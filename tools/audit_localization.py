#!/usr/bin/env python3
"""Check paired Android copy structure and list changed copy for human review."""

import argparse
from collections import Counter
from pathlib import Path
import re
import subprocess
import xml.etree.ElementTree as ET


ROOT = Path(__file__).resolve().parents[1]
FILES = {
    "en": Path("assistant/src/main/res/values/strings.xml"),
    "zh": Path("assistant/src/main/res/values-zh-rCN/strings.xml"),
}
FORMAT = re.compile(r"%(?:([1-9]\d*)\$)?([-#+ 0,(<]*)(?:\d+)?(?:\.\d+)?([a-zA-Z%])")


def resources(raw):
    result = {}
    for element in ET.fromstring(raw):
        name = element.get("name")
        if not name:
            continue
        if element.tag == "string":
            result[("string", name)] = {"value": "".join(element.itertext())}
        elif element.tag == "plurals":
            result[("plurals", name)] = {
                item.get("quantity"): "".join(item.itertext())
                for item in element.findall("item")
            }
    return result


def placeholders(value):
    return Counter((index or "next", kind) for index, _, kind in FORMAT.findall(value)
                   if kind != "%")


def check_pair(en, zh):
    errors = []
    for key in sorted(en.keys() ^ zh.keys()):
        errors.append(f"Missing counterpart: {key}")
    for key in sorted(en.keys() & zh.keys()):
        a, b = en[key], zh[key]
        if key[0] == "plurals" and ("other" not in a or "other" not in b):
            errors.append(f"Plural fallback missing: {key}")
        for quantity in a.keys() & b.keys():
            if placeholders(a[quantity]) != placeholders(b[quantity]):
                errors.append(f"Format placeholders differ: {key} / {quantity}")
    return errors


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--base", help="Git ref used to show added or changed copy")
    args = parser.parse_args()
    current = {language: resources((ROOT / path).read_bytes())
               for language, path in FILES.items()}
    errors = check_pair(current["en"], current["zh"])
    print(f"Resource pairs: {len(current['en'])} English, {len(current['zh'])} Chinese")
    if args.base:
        old = {
            language: resources(subprocess.check_output(
                ["git", "show", f"{args.base}:{path.as_posix()}"], cwd=ROOT))
            for language, path in FILES.items()
        }
        changed = sorted(key for key in current["en"]
                         if old["en"].get(key) != current["en"].get(key)
                         or old["zh"].get(key) != current["zh"].get(key))
        added = sum(key not in old["en"] for key in changed)
        print(f"Review this delta: {len(changed)} changed resources ({added} new)")
        for kind, name in changed:
            value = current["en"][(kind, name)]
            length = max(len(text) for text in value.values())
            print(f"  {kind}:{name} | longest English item {length} chars")
    for error in errors:
        print(f"ERROR: {error}")
    if errors:
        raise SystemExit(1)
    print("Keys, plural quantities, and format placeholders match.")


if __name__ == "__main__":
    main()
