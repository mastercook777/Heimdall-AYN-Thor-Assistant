#!/usr/bin/env python3
"""Verify ZIP and ELF alignment for every native library in an APK."""

from __future__ import annotations

import argparse
import struct
import sys
import zipfile
from pathlib import Path


PT_LOAD = 1
ABI_ELF_PAGE_SIZES = {
    "arm64-v8a": 16384,
    "armeabi-v7a": 4096,
    "x86": 4096,
    "x86_64": 16384,
}


def local_data_offset(apk_file, info: zipfile.ZipInfo) -> int:
    apk_file.seek(info.header_offset)
    header = apk_file.read(30)
    if len(header) != 30:
        raise ValueError(f"truncated local header for {info.filename}")
    signature, = struct.unpack_from("<I", header, 0)
    if signature != 0x04034B50:
        raise ValueError(f"invalid local header signature for {info.filename}")
    name_length, extra_length = struct.unpack_from("<HH", header, 26)
    return info.header_offset + 30 + name_length + extra_length


def elf_load_segments(payload: bytes, name: str) -> list[tuple[int, int, int]]:
    if len(payload) < 64 or payload[:4] != b"\x7fELF":
        raise ValueError(f"{name} is not an ELF shared library")

    elf_class = payload[4]
    data_encoding = payload[5]
    if data_encoding == 1:
        endian = "<"
    elif data_encoding == 2:
        endian = ">"
    else:
        raise ValueError(f"{name} has unsupported ELF data encoding {data_encoding}")

    if elf_class == 1:
        phoff = struct.unpack_from(endian + "I", payload, 28)[0]
        phentsize = struct.unpack_from(endian + "H", payload, 42)[0]
        phnum = struct.unpack_from(endian + "H", payload, 44)[0]

        def read_program_header(offset: int) -> tuple[int, int, int, int]:
            return (
                struct.unpack_from(endian + "I", payload, offset)[0],
                struct.unpack_from(endian + "I", payload, offset + 4)[0],
                struct.unpack_from(endian + "I", payload, offset + 8)[0],
                struct.unpack_from(endian + "I", payload, offset + 28)[0],
            )

    elif elf_class == 2:
        phoff = struct.unpack_from(endian + "Q", payload, 32)[0]
        phentsize = struct.unpack_from(endian + "H", payload, 54)[0]
        phnum = struct.unpack_from(endian + "H", payload, 56)[0]

        def read_program_header(offset: int) -> tuple[int, int, int, int]:
            return (
                struct.unpack_from(endian + "I", payload, offset)[0],
                struct.unpack_from(endian + "Q", payload, offset + 8)[0],
                struct.unpack_from(endian + "Q", payload, offset + 16)[0],
                struct.unpack_from(endian + "Q", payload, offset + 48)[0],
            )

    else:
        raise ValueError(f"{name} has unsupported ELF class {elf_class}")

    if phentsize <= 0 or phnum <= 0 or phoff + phentsize * phnum > len(payload):
        raise ValueError(f"{name} has an invalid ELF program-header table")

    segments: list[tuple[int, int, int]] = []
    for index in range(phnum):
        offset = phoff + index * phentsize
        p_type, p_offset, p_vaddr, p_align = read_program_header(offset)
        if p_type == PT_LOAD:
            segments.append((p_offset, p_vaddr, p_align))
    if not segments:
        raise ValueError(f"{name} has no PT_LOAD segments")
    return segments


def verify(apk_path: Path, page_size: int, legacy_page_size: int) -> int:
    failures: list[str] = []
    rows: list[tuple[str, int, str, int, int, int]] = []

    with apk_path.open("rb") as apk_file, zipfile.ZipFile(apk_path) as archive:
        corrupt_member = archive.testzip()
        if corrupt_member:
            failures.append(f"ZIP CRC verification failed at {corrupt_member}")

        libraries = sorted(
            [
                info
                for info in archive.infolist()
                if info.filename.startswith("lib/") and info.filename.endswith(".so")
            ],
            key=lambda info: info.filename,
        )
        if not libraries:
            failures.append("APK contains no lib/<abi>/*.so entries")

        for info in libraries:
            try:
                data_offset = local_data_offset(apk_file, info)
                payload = archive.read(info)
                segments = elf_load_segments(payload, info.filename)
            except (OSError, struct.error, ValueError, zipfile.BadZipFile) as error:
                failures.append(str(error))
                continue

            if info.compress_type != zipfile.ZIP_STORED:
                failures.append(
                    f"{info.filename} is compressed (method={info.compress_type})"
                )
            if data_offset % page_size != 0:
                failures.append(
                    f"{info.filename} ZIP data offset {data_offset} is not "
                    f"{page_size}-byte aligned"
                )

            path_parts = info.filename.split("/")
            abi = path_parts[1] if len(path_parts) > 2 else "unknown"
            if abi not in ABI_ELF_PAGE_SIZES:
                failures.append(f"{info.filename} uses an unsupported ABI path: {abi}")
                continue
            elf_page_size = ABI_ELF_PAGE_SIZES[abi]
            expected_page_size = page_size if elf_page_size == 16384 else legacy_page_size
            if elf_page_size != expected_page_size:
                failures.append(
                    f"{info.filename} ABI policy expected {expected_page_size}, "
                    f"configured {elf_page_size}"
                )
            minimum_load_alignment = min(segment[2] for segment in segments)
            for p_offset, p_vaddr, p_align in segments:
                if p_align < elf_page_size:
                    failures.append(
                        f"{info.filename} PT_LOAD alignment {p_align} is below "
                        f"{elf_page_size}"
                    )
                if p_offset % elf_page_size != p_vaddr % elf_page_size:
                    failures.append(
                        f"{info.filename} PT_LOAD offset/vaddr are not congruent "
                        f"for {elf_page_size}-byte pages"
                    )
            rows.append(
                (
                    info.filename,
                    info.file_size,
                    "STORED" if info.compress_type == zipfile.ZIP_STORED else str(info.compress_type),
                    data_offset,
                    elf_page_size,
                    minimum_load_alignment,
                )
            )

    print("Native library alignment report")
    print(
        "path\tsize\tcompression\tzipDataOffset\texpectedElfPage"
        "\tminimumPtLoadAlignment"
    )
    for name, size, compression, data_offset, elf_page_size, alignment in rows:
        print(
            f"{name}\t{size}\t{compression}\t{data_offset}\t{elf_page_size}\t{alignment}"
        )

    if failures:
        for failure in failures:
            print(f"ERROR: {failure}", file=sys.stderr)
        return 1
    print(
        f"Verified {len(rows)} native libraries: ZIP {page_size}-byte alignment; "
        f"64-bit ELF {page_size}-byte and 32-bit ELF {legacy_page_size}-byte alignment"
    )
    return 0


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("apk", type=Path)
    parser.add_argument("--page-size", type=int, default=16384)
    parser.add_argument("--legacy-page-size", type=int, default=4096)
    args = parser.parse_args()
    if args.page_size <= 0 or args.page_size & (args.page_size - 1):
        parser.error("--page-size must be a positive power of two")
    if args.legacy_page_size <= 0 or args.legacy_page_size & (args.legacy_page_size - 1):
        parser.error("--legacy-page-size must be a positive power of two")
    if not args.apk.is_file():
        parser.error(f"APK does not exist: {args.apk}")
    return verify(args.apk.resolve(), args.page_size, args.legacy_page_size)


if __name__ == "__main__":
    raise SystemExit(main())
