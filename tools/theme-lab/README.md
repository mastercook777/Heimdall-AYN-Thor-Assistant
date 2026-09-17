# Heimdall Theme Lab

Theme Lab is a dependency-free, browser-local colorway workbench for the two existing Heimdall material families. It is deliberately separate from the Android app and does not read or write Android preferences, Profiles, Grid data, or registry source.

Open `index.html` directly in a current Chromium, Edge, Chrome, or Firefox browser. No local server or package install is required.

## V1 contract

- Family is fixed to `heimdall` or `freya`. The selected family owns material language, geometry, radii, light direction, depth, component construction, and state presentation.
- Colorway owns only the exported color tokens. The token keys intentionally match current `ThemePalette` names where a direct mapping already exists.
- Success, Warning, Error, Recording, destructive, and other product meanings are semantic-owned. The fixture shows the relevant semantic states, but those colors are not editable or exported.
- Canvas/media content stays content-neutral. Its outer frame participates in the family preview; its content pixels do not inherit the colorway.
- The Preview DOM, copy, geometry, icons, and state targets are fixed at `1240 × 1080`. The browser scales that exact stage to fit the available workspace.
- A/B is a rapid visual toggle between two snapshots. A same-family comparison is a valid colorway comparison. A cross-family comparison is only a material reference because family geometry and construction differ.
- Saved presets use browser `localStorage`. Export JSON is the portable source of truth.

## Requirement review and V1 additions

The original brief was feasible but needed four contracts to avoid producing a disposable mockup tool:

1. A versioned interchange format. Exports identify `heimdall.theme-colorway` and `schemaVersion: 1`; `theme-colorway.schema.json` defines the complete file.
2. Explicit ownership. Family-owned values and semantic colors are absent from the exported `colors` object, so ordinary colorway edits cannot silently redefine them.
3. Import validation and partial-import behavior. Invalid families, IDs, or colors fail closed. A partial V1 color object is allowed in the UI and fills omitted fields from that family's registered baseline, with a visible notice; every export is complete.
4. Alpha-aware color handling. The editor accepts `#RRGGBB` and Android-style `#AARRGGBB`. The native browser picker edits RGB while retaining an existing alpha channel.

The Lab also adds lightweight text-contrast diagnostics and non-destructive Surface/Accent HSL group adjustments. These are review aids, not automated approval.

## Presets and mapping boundary

The four initial presets mirror the accepted registry palette values for:

- `heimdall.blue`
- `heimdall.amber`
- `freya.white`
- `freya.rosewood`

V1 covers the cross-component palette roles needed for fast colorway direction work. It does not export the larger `ThemeComponentColors`, `ThemeKeyboardColors`, `ThemeGlassColors`, or `ThemeCncColors` graphs. Those production-specific roles still require a deliberate Android mapping pass when a draft is promoted.

An exported file therefore means “complete Theme Lab V1 colorway,” not “registration-ready Android `ThemeDefinition`.” Promotion should map the V1 fields, populate every additional governed Android role, preserve the shared family `ThemeMaterialSpec`, add contract assertions, and then use production components plus exact-hash Thor testing as the authority.

## Files

- `index.html` — editor shell and the fixed canonical fixture.
- `styles.css` — Lab UI, immutable family material contracts, and the fixed preview layout.
- `app.js` — presets, token editing, HSL transforms, validation, A/B, local save, import, and export.
- `theme-colorway.schema.json` — portable Colorway V1 JSON Schema.

## Non-goals

- No Android app changes or source generation.
- No production theme persistence.
- No radius, geometry, material, layout, animation, or hit-area editor.
- No complete production renderer simulation.
- No claim that browser preview equals Android compositing or Thor visual acceptance.
