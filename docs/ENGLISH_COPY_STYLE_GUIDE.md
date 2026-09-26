# Heimdall English Copy and Terminology Guide

This guide is the English copy baseline for the Thor lower-screen UI. It favors short, direct labels that remain readable at 1240 x 1080 without changing layout, text size, hit targets, or interaction behavior.

## 0.3.0 Audit Scope

- Baseline: `2026-09-22-hardware-monitor-thor-accepted` at `9d96ce9`.
- Audited source: all 1,055 English `<string>` entries, the plural entries, and their Simplified-Chinese counterparts.
- Direct-string parity before and after the copy pass: `1,055 / 1,055`.
- Hardcoded English UI text: only example field hints (`https://example.com/v1` and `sk-...`) remain outside resources.
- Explicit line breaks reviewed: 14. Structural status rows and dialog paragraph separators remain intentional; compact status and empty-state copy was shortened.
- Static risk reduction: English strings over 40 characters changed from 302 to 280; strings over 80 characters changed from 89 to 76.

Character count is a triage signal, not a rendering guarantee. Final fit still requires English testing on Thor because the Android emulator does not reproduce Thor font metrics.

## Voice

- Lead with the action or state: `Save`, `Stopped`, `Add File`, `No maps yet`.
- Prefer one familiar verb over a descriptive sentence in buttons.
- Use active voice and concrete nouns.
- Remove repeated context when the card or section title already supplies it.
- Keep technical implementation terms in Details, Diagnostics, or error recovery text.
- Do not shorten safety, privacy, destructive-action, or accessibility copy until meaning becomes ambiguous.

## Compact Copy Budgets

These are review targets, not automatic truncation rules.

| Surface | Target |
| --- | --- |
| 42–52 dp text button | 18 characters or fewer |
| Icon-and-label action | 14 characters or fewer |
| Status chip or trailing state | 18 characters or fewer |
| Fixed-height row title | 24 characters or fewer |
| Single-line helper strip | 45 characters or fewer |
| Settings summary | Prefer 80 characters or fewer |
| Dialog body / accessibility description | No hard cap; preserve meaning |

If a compact label exceeds its target, first remove repeated context, then choose a shorter canonical term. Do not reduce text size or hit area to rescue copy.

## Canonical Terms

| Use | Meaning and rule | Avoid |
| --- | --- | --- |
| `Home` | Runtime page in the bottom navigation | Main, Main Screen, main-screen page |
| `Grid` | Saved arrangement system; capitalize as a Heimdall concept | layout when specifically naming the Grid |
| `Edit Grid` | Entry point for module placement and size | Main Layout, Edit modules |
| `Profile` | Player-owned configuration unit; always capitalized | preset, config profile |
| `module` | Generic item placed in the Grid; lowercase in sentences | widget as a universal synonym |
| `widget` | Data or service-backed module, lowercase in sentences | capitalized Widget in prose |
| `Macros` | Settings category and module family | Macro Buttons |
| `Macro` | One executable player action | macro command |
| `Basic Touch` | Accessibility-based compatible touch capability | Accessibility mode |
| `Advanced Controls` | Shizuku-backed capability group | advanced input, native mode |
| `Touch Drag` | Sustained upper-screen drag mode | Compatible Touch |
| `Enhanced Touch` | Mapping-compatible touch route | enhanced mode |
| `Precision Aim` | Fine right-stick control mode | relative aim |
| `Right Stick` | Virtual right-stick control mode | Virtual Right Stick |
| `Virtual Mouse` | Relative mouse control mode | Mouse Pointer as the feature name |
| `PC Keyboard` | Profile-owned keyboard mapping feature | PC keyboard, keyboard mapping as its title |
| `Full Keyboard` | Temporary full US ANSI surface | Virtual Keyboard |
| `Keypad` | Compact Grid keyboard module | Keyboard Pad in visible UI |
| `Quick Actions` | Structured action module | shortcuts |
| `Magnifier` | Live upper-screen reference module | Zoom except the compact Grid add label |
| `Canvas` | Profile-owned local media module | image widget |
| `Translation` | OCR translation feature and editor title | Translation Widget as a page title |
| `Hardware Monitor` | Passive CPU/RAM module | Monitor except the compact Grid add label |
| `Connections` | Settings category for Basic Touch and Advanced Controls | Connection & Permissions |
| `Game Context` | Optional detected game/console identity | game recognition context |
| `Interactive Map` | Embedded web map type | browser map |
| `app-wide` | Setting shared across Profiles | App-global, global app setting |
| `upper screen` / `lower screen` | Noun form | upper-screen when used as a noun |
| `upper-screen` / `lower-screen` | Adjective form | upper screen used before a noun |

Compact Grid add labels are deliberate exceptions: `Pad`, `Zoom`, `Translate`, `Actions`, and `Monitor` may be used where the full feature name would compete with the module preview.

## Capitalization

- Use sentence case for dialog titles, field labels, status, and actions.
- Preserve title case for product concepts in the terminology table.
- Capitalize `Profile`, `Grid`, `Canvas`, and named control modes.
- Keep generic `module`, `widget`, `app`, `screen`, `button`, and `provider` lowercase.
- Use `API`, `OCR`, `PDF`, `URL`, `HTTP`, `HTTPS`, `RAM`, `CPU`, and `FPS` in uppercase.

## Buttons and States

- Button labels should be direct verbs: `Save`, `Edit`, `Replace`, `Refresh`, `Set Up`.
- Do not combine alternatives with `/` when one action occurs.
- A destructive confirmation may use a short button only when the dialog body names the consequence.
- State text should report the state, not narrate it: `Mirroring`, `Paused on last frame`, `Stopped`.
- Use `unavailable` when the route cannot be used and `not enabled` when setup is optional but incomplete.

## Line Breaks

- Do not add a hard line break merely to force a preferred visual wrap.
- Hard breaks are allowed for stable key/value status rows, a title plus dynamic value, or a deliberate dialog paragraph boundary.
- Shorten both sides of a required break so neither line depends on clipping.
- Empty-state copy should use at most two short lines: state first, next action second.
- Dynamic values must retain enough room for localized or user-provided content; shortening the static prefix takes priority.

## Review Checklist

1. Does the string use the canonical term for its feature?
2. Is repeated page, card, or button context removable?
3. Does a button begin with a clear action?
4. Is a hard line break structural rather than decorative?
5. Are format placeholders unchanged and correctly ordered?
6. Does the copy remain honest about unavailable capabilities and save boundaries?
7. Does the English UI fit on Thor in both Heimdall and Freya families at the owner's normal font scale?

## Required Thor Pass Before 0.3.0

Check the English UI on the exact candidate APK at 1240 x 1080, in at least one Heimdall-family and one Freya-family colorway:

- First Setup and Startup Readiness.
- Settings category rail, summaries, toggles, and numeric steppers.
- Edit Grid, Grid draft bar, and add-module labels.
- Macro picker/editor and controller composer.
- Connections and Profile management.
- Map, Guide, Magnifier, Translation, Canvas, Quick Actions, Keypad, and Full Keyboard.
- Dialog titles/actions, long dynamic Profile names, and long macro labels.

Record visual overflow, unintended wrapping, clipped baselines, and forced two-line buttons separately. Build or emulator success is not Thor text-fit acceptance.

## Thor Verification Record

On 2026-09-25, the owner completed an English-interface pass on Thor and reported that the interface was acceptable for the current 0.3.0 preparation. The final focused Grid correction keeps the compact `Translate` entry on one line without changing its outer bounds or hit target.

- Accepted Debug APK SHA-256: `B501861F4E622767D30E409E12A7690EF8167F7A455EA23AC8AA80760C1628B1`
- Scope: current English copy, terminology, and observed text fit, including the Grid `Translate` correction.
- Status: provisionally accepted for continued 0.3.0 preparation; this is not release-candidate, signing, Tag, or publication approval.
