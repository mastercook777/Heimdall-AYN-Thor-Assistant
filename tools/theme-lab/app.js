(() => {
  "use strict";

  const FORMAT = "heimdall.theme-colorway";
  const SCHEMA_VERSION = 1;
  const STORAGE_KEY = "heimdall.theme-lab.colorways.v1";

  const TOKEN_GROUPS = [
    {
      id: "surface",
      label: "Surface",
      tokens: [
        ["pageBackground", "Background"],
        ["flatPageBackground", "Flat page background"],
        ["surfaceBase", "Base surface"],
        ["surfaceRaised", "Raised surface"],
        ["surfaceControl", "Control surface"],
        ["surfaceInset", "Inset surface"],
        ["surfaceField", "Field surface"]
      ]
    },
    {
      id: "structure",
      label: "Structure",
      tokens: [
        ["edgeStrong", "Structure primary"],
        ["edgeNeutral", "Border primary"],
        ["inputEdgeIdle", "Border quiet"],
        ["quickActionDivider", "Divider"]
      ]
    },
    {
      id: "text",
      label: "Text",
      tokens: [
        ["textPrimary", "Primary text"],
        ["textSecondary", "Secondary text"],
        ["textDisabled", "Disabled text"]
      ]
    },
    {
      id: "accent",
      label: "Accent",
      tokens: [
        ["accent", "Accent primary"],
        ["accentGradientEnd", "Accent soft"],
        ["accentStrong", "Accent strong"],
        ["focus", "Focus"],
        ["selection", "Selection"],
        ["activeInput", "Active input"]
      ]
    }
  ];

  const TOKENS = TOKEN_GROUPS.flatMap(group => group.tokens.map(([key, label]) => ({
    key,
    label,
    group: group.id
  })));
  const TOKEN_KEYS = TOKENS.map(token => token.key);
  const TOKEN_BY_KEY = new Map(TOKENS.map(token => [token.key, token]));

  const FAMILY_CONTRACTS = {
    heimdall: {
      label: "Heimdall",
      description: "Restrained dark glass · soft depth · rounded construction",
      semantic: {
        success: "#FF5FD18A",
        warning: "#FFD8A13A",
        error: "#FFFF6B6B",
        recording: "#FFFF6B7A"
      }
    },
    freya: {
      label: "Freya",
      description: "Pearl CNC · vertical light · machined control hierarchy",
      semantic: {
        success: "#FF2F8B59",
        warning: "#FFC46B20",
        error: "#FFB34A4F",
        recording: "#FFE0525C"
      }
    }
  };

  const BUILTIN_PRESETS = [
    createPreset("heimdall.blue", "Heimdall Blue", "heimdall", [
      "#FF070A10", "#FF0E141B", "#CC101722", "#D1172131", "#FF0F1622", "#990B1018", "#FF101824",
      "#FF3C4D63", "#FF2B3748", "#AA6A9DDB", "#334E6074",
      "#FFE6EDF3", "#FF9AA8B8", "#8C9AA8B8",
      "#FF4EA1FF", "#FF8B5CFF", "#FF70B7FF", "#FF3F9DFF", "#FF4EA1FF", "#FF70B7FF"
    ]),
    createPreset("heimdall.amber", "Heimdall Amber", "heimdall", [
      "#FF090B0D", "#FF090B0D", "#FF090B0D", "#FF121619", "#FF101417", "#FF07090A", "#FF0C1013",
      "#FF4B4B45", "#FF303538", "#AA725528", "#33303538",
      "#FFEEECE6", "#FFA5A59F", "#8CA5A59F",
      "#FFD99A2B", "#FF725528", "#FFE7A436", "#FFE7A436", "#FFD99A2B", "#FFF0B84C"
    ]),
    createPreset("freya.white", "Freya White", "freya", [
      "#FFE3E6E7", "#FFE5E7E8", "#FFEEF0EF", "#FFF6F5F3", "#FFE5E8E9", "#FFD8DCDD", "#FFF8F8F6",
      "#FF9EABB8", "#FF9EABB8", "#CC9AA7B4", "#66929EAA",
      "#FF263547", "#FF596A7D", "#8C596A7D",
      "#FFF08A2A", "#FFFFA044", "#FFFFA044", "#FFF08A2A", "#FFF08A2A", "#FFF08A2A"
    ]),
    createPreset("freya.rosewood", "Freya Rosewood", "freya", [
      "#FFF1E7E2", "#FFF1E7E2", "#FFF6EEEA", "#FFFBF5F1", "#FFE9DDD7", "#FFD9CCC6", "#FFFFF9F5",
      "#FF806055", "#FFB8A49B", "#CCA18479", "#FFD0BFB8",
      "#FF49332D", "#FF785E55", "#FFA4938C",
      "#FFC65D7B", "#FFD97993", "#FFD97993", "#FFD16A87", "#FFC65D7B", "#FFD97993"
    ])
  ];

  const PREVIEW_STATES = [
    ["idle", "Idle"],
    ["focus", "Focus"],
    ["press", "Press"],
    ["running", "Running"],
    ["recording", "Recording", true],
    ["warning", "Warning", true],
    ["error", "Error", true]
  ];

  const dom = {
    familySelect: byId("familySelect"),
    familyContract: byId("familyContract"),
    displayNameInput: byId("displayNameInput"),
    idInput: byId("idInput"),
    presetSelect: byId("presetSelect"),
    loadPresetButton: byId("loadPresetButton"),
    duplicateButton: byId("duplicateButton"),
    saveAsButton: byId("saveAsButton"),
    resetDraftButton: byId("resetDraftButton"),
    dirtyBadge: byId("dirtyBadge"),
    stateSelector: byId("stateSelector"),
    tokenEditor: byId("tokenEditor"),
    tokenCount: byId("tokenCount"),
    selectedTokenLabel: byId("selectedTokenLabel"),
    selectedTokenValue: byId("selectedTokenValue"),
    hslEditor: byId("hslEditor"),
    groupAdjustments: byId("groupAdjustments"),
    setAButton: byId("setAButton"),
    setBButton: byId("setBButton"),
    showAButton: byId("showAButton"),
    showBButton: byId("showBButton"),
    returnDraftButton: byId("returnDraftButton"),
    compareSummary: byId("compareSummary"),
    compareNote: byId("compareNote"),
    importButton: byId("importButton"),
    exportButton: byId("exportButton"),
    importInput: byId("importInput"),
    previewViewport: byId("previewViewport"),
    previewWrap: byId("previewWrap"),
    themePreview: byId("themePreview"),
    previewSourceBadge: byId("previewSourceBadge"),
    contrastStatus: byId("contrastStatus"),
    scaleLabel: byId("scaleLabel"),
    saveDialog: byId("saveDialog"),
    saveForm: byId("saveForm"),
    saveNameInput: byId("saveNameInput"),
    saveIdInput: byId("saveIdInput"),
    saveError: byId("saveError"),
    cancelSaveButton: byId("cancelSaveButton"),
    toast: byId("toast")
  };

  let customPresets = loadCustomPresets();
  let draft = clone(BUILTIN_PRESETS[0]);
  let loadedSnapshot = clone(draft);
  let selectedToken = "pageBackground";
  let previewState = "idle";
  let previewOverride = null;
  let compareSlots = { A: null, B: null };
  let toastTimer = null;
  let saveMode = "save";
  const tokenControls = new Map();
  const groupStates = new Map();

  initialize();

  function initialize() {
    buildStateSelector();
    buildTokenEditor();
    buildGroupAdjustments();
    bindEvents();
    refreshPresetSelect(draft.id);
    beginGroupBaselines();
    syncAll();
    const resizeObserver = new ResizeObserver(fitPreview);
    resizeObserver.observe(dom.previewViewport);
    fitPreview();
  }

  function createPreset(id, displayName, family, values) {
    if (values.length !== TOKEN_KEYS.length) {
      throw new Error(`Preset ${id} has ${values.length} colors; expected ${TOKEN_KEYS.length}`);
    }
    const colors = {};
    TOKEN_KEYS.forEach((key, index) => { colors[key] = normalizeHex(values[index]); });
    return { format: FORMAT, schemaVersion: SCHEMA_VERSION, id, family, displayName, colors, builtin: true };
  }

  function byId(id) { return document.getElementById(id); }
  function clone(value) { return JSON.parse(JSON.stringify(value)); }

  function allPresets() {
    return [...BUILTIN_PRESETS, ...customPresets].sort((a, b) => {
      if (a.builtin !== b.builtin) return a.builtin ? -1 : 1;
      return a.displayName.localeCompare(b.displayName);
    });
  }

  function presetsForFamily(family) { return allPresets().filter(preset => preset.family === family); }

  function buildStateSelector() {
    PREVIEW_STATES.forEach(([id, label, semantic]) => {
      const button = document.createElement("button");
      button.type = "button";
      button.className = `state-chip${semantic ? " semantic" : ""}`;
      button.dataset.state = id;
      button.setAttribute("role", "radio");
      button.setAttribute("aria-checked", id === previewState ? "true" : "false");
      button.textContent = label;
      button.addEventListener("click", () => {
        previewState = id;
        dom.stateSelector.querySelectorAll(".state-chip").forEach(chip => {
          chip.setAttribute("aria-checked", chip.dataset.state === id ? "true" : "false");
        });
        renderPreview();
      });
      dom.stateSelector.appendChild(button);
    });
  }

  function buildTokenEditor() {
    dom.tokenCount.textContent = `${TOKENS.length} tokens`;
    TOKEN_GROUPS.forEach(group => {
      const section = document.createElement("section");
      section.className = "token-group";
      const heading = document.createElement("h3");
      heading.textContent = group.label;
      section.appendChild(heading);

      group.tokens.forEach(([key, label]) => {
        const row = document.createElement("div");
        row.className = "token-row";
        row.dataset.token = key;
        row.tabIndex = 0;

        const picker = document.createElement("input");
        picker.type = "color";
        picker.setAttribute("aria-label", `${label} color picker`);

        const copy = document.createElement("span");
        copy.className = "token-name";
        const strong = document.createElement("strong");
        strong.textContent = label;
        const small = document.createElement("small");
        small.textContent = key;
        copy.append(strong, small);

        const hex = document.createElement("input");
        hex.className = "token-hex";
        hex.type = "text";
        hex.maxLength = 9;
        hex.spellcheck = false;
        hex.setAttribute("aria-label", `${label} hex value`);

        const select = () => selectToken(key);
        row.addEventListener("click", event => { if (event.target !== hex && event.target !== picker) select(); });
        row.addEventListener("keydown", event => { if (event.key === "Enter" || event.key === " ") select(); });
        picker.addEventListener("input", () => {
          prepareDirectEdit(key);
          const old = parseHex(draft.colors[key]);
          const rgb = parseHex(picker.value);
          draft.colors[key] = formatHex(rgb.r, rgb.g, rgb.b, old.a, old.explicitAlpha);
          selectToken(key, false);
          markEdited();
        });
        hex.addEventListener("focus", select);
        hex.addEventListener("change", () => {
          const normalized = safeNormalizeHex(hex.value);
          if (!normalized) {
            hex.classList.add("invalid");
            hex.value = draft.colors[key];
            showToast("Use #RRGGBB or #AARRGGBB.", true);
            return;
          }
          prepareDirectEdit(key);
          hex.classList.remove("invalid");
          draft.colors[key] = normalized;
          selectToken(key, false);
          markEdited();
        });

        row.append(picker, copy, hex);
        section.appendChild(row);
        tokenControls.set(key, { row, picker, hex });
      });
      dom.tokenEditor.appendChild(section);
    });
  }

  function buildGroupAdjustments() {
    [
      { id: "surface", label: "Surface group", keys: TOKEN_GROUPS.find(group => group.id === "surface").tokens.map(([key]) => key) },
      { id: "accent", label: "Accent group", keys: TOKEN_GROUPS.find(group => group.id === "accent").tokens.map(([key]) => key) }
    ].forEach(group => {
      const panel = document.createElement("div");
      panel.className = "group-adjustment";
      const header = document.createElement("div");
      header.className = "group-adjustment-header";
      const title = document.createElement("strong");
      title.textContent = group.label;
      const actions = document.createElement("div");
      actions.className = "group-adjustment-actions";
      const reset = miniButton("Reset");
      const apply = miniButton("Apply");
      actions.append(reset, apply);
      header.append(title, actions);
      panel.appendChild(header);

      const controls = {};
      [
        ["h", "Hue", -180, 180, 1, "°"],
        ["s", "Saturation", -40, 40, 1, "%"],
        ["l", "Lightness", -30, 30, 1, "%"]
      ].forEach(([key, label, min, max, step, suffix]) => {
        const row = document.createElement("div");
        row.className = "slider-row";
        const name = document.createElement("label");
        name.textContent = label;
        const input = document.createElement("input");
        input.type = "range";
        input.min = min;
        input.max = max;
        input.step = step;
        input.value = 0;
        const output = document.createElement("output");
        output.textContent = `0${suffix}`;
        input.addEventListener("input", () => {
          output.textContent = `${Number(input.value) > 0 ? "+" : ""}${input.value}${suffix}`;
          applyGroupPreview(group.id);
        });
        row.append(name, input, output);
        panel.appendChild(row);
        controls[key] = { input, output, suffix };
      });

      groupStates.set(group.id, { keys: group.keys, base: {}, controls });
      reset.addEventListener("click", () => resetGroup(group.id));
      apply.addEventListener("click", () => commitGroup(group.id, true));
      dom.groupAdjustments.appendChild(panel);
    });
  }

  function miniButton(label) {
    const button = document.createElement("button");
    button.type = "button";
    button.className = "mini-button";
    button.textContent = label;
    return button;
  }

  function bindEvents() {
    dom.familySelect.addEventListener("change", () => {
      const family = dom.familySelect.value;
      const canonicalDefault = family === "heimdall" ? "heimdall.blue" : "freya.white";
      const next = allPresets().find(preset => preset.id === canonicalDefault)
        || presetsForFamily(family)[0];
      loadPreset(next);
      showToast(`${FAMILY_CONTRACTS[family].label} family loaded. Material contract remains locked.`);
    });

    dom.displayNameInput.addEventListener("input", () => {
      draft.displayName = dom.displayNameInput.value;
      markEdited();
    });
    dom.idInput.addEventListener("input", () => {
      draft.id = dom.idInput.value.trim();
      markEdited();
    });

    dom.loadPresetButton.addEventListener("click", () => {
      const preset = allPresets().find(item => item.id === dom.presetSelect.value);
      if (preset) loadPreset(preset);
    });
    dom.presetSelect.addEventListener("dblclick", () => dom.loadPresetButton.click());
    dom.duplicateButton.addEventListener("click", () => openSaveDialog("duplicate"));
    dom.saveAsButton.addEventListener("click", () => openSaveDialog("save"));
    dom.resetDraftButton.addEventListener("click", () => {
      draft = clone(loadedSnapshot);
      previewOverride = null;
      beginGroupBaselines();
      syncAll();
      showToast("Draft reset to the last loaded colorway.");
    });

    dom.setAButton.addEventListener("click", () => setCompareSlot("A"));
    dom.setBButton.addEventListener("click", () => setCompareSlot("B"));
    dom.showAButton.addEventListener("click", () => showCompareSlot("A"));
    dom.showBButton.addEventListener("click", () => showCompareSlot("B"));
    dom.returnDraftButton.addEventListener("click", () => {
      previewOverride = null;
      renderPreview();
      updateCompareUi();
    });

    dom.importButton.addEventListener("click", () => dom.importInput.click());
    dom.importInput.addEventListener("change", importFile);
    dom.exportButton.addEventListener("click", exportColorway);

    dom.cancelSaveButton.addEventListener("click", () => dom.saveDialog.close());
    dom.saveForm.addEventListener("submit", event => {
      event.preventDefault();
      saveCustomPreset();
    });
  }

  function selectToken(key, rebuild = true) {
    selectedToken = key;
    tokenControls.forEach((controls, tokenKey) => controls.row.classList.toggle("selected", tokenKey === key));
    if (rebuild) renderHslEditor();
  }

  function renderHslEditor() {
    const token = TOKEN_BY_KEY.get(selectedToken);
    const rgba = parseHex(draft.colors[selectedToken]);
    const hsl = rgbToHsl(rgba.r, rgba.g, rgba.b);
    dom.selectedTokenLabel.textContent = token.label;
    dom.selectedTokenValue.textContent = draft.colors[selectedToken];
    dom.hslEditor.replaceChildren();

    [
      ["h", "Hue", 0, 360, 1, "°"],
      ["s", "Saturation", 0, 100, 1, "%"],
      ["l", "Lightness", 0, 100, 1, "%"]
    ].forEach(([key, label, min, max, step, suffix]) => {
      const row = document.createElement("div");
      row.className = "slider-row";
      const name = document.createElement("label");
      name.textContent = label;
      const input = document.createElement("input");
      input.type = "range";
      input.min = min;
      input.max = max;
      input.step = step;
      input.value = Math.round(hsl[key]);
      const output = document.createElement("output");
      output.textContent = `${Math.round(hsl[key])}${suffix}`;
      input.addEventListener("input", () => {
        prepareDirectEdit(selectedToken);
        const current = parseHex(draft.colors[selectedToken]);
        const next = rgbToHsl(current.r, current.g, current.b);
        next[key] = Number(input.value);
        const rgb = hslToRgb(next.h, next.s, next.l);
        draft.colors[selectedToken] = formatHex(rgb.r, rgb.g, rgb.b, current.a, current.explicitAlpha);
        output.textContent = `${input.value}${suffix}`;
        markEdited(false);
        syncTokenInputs();
        dom.selectedTokenValue.textContent = draft.colors[selectedToken];
      });
      row.append(name, input, output);
      dom.hslEditor.appendChild(row);
    });
  }

  function prepareDirectEdit(key) {
    const group = TOKEN_BY_KEY.get(key)?.group;
    if (groupStates.has(group)) commitGroup(group, false);
    previewOverride = null;
  }

  function beginGroupBaselines() {
    groupStates.forEach(state => {
      state.base = Object.fromEntries(state.keys.map(key => [key, draft.colors[key]]));
      resetGroupControls(state);
    });
  }

  function applyGroupPreview(groupId) {
    const state = groupStates.get(groupId);
    const shifts = Object.fromEntries(Object.entries(state.controls).map(([key, control]) => [key, Number(control.input.value)]));
    state.keys.forEach(key => {
      const rgba = parseHex(state.base[key]);
      const hsl = rgbToHsl(rgba.r, rgba.g, rgba.b);
      hsl.h = wrapHue(hsl.h + shifts.h);
      hsl.s = clamp(hsl.s + shifts.s, 0, 100);
      hsl.l = clamp(hsl.l + shifts.l, 0, 100);
      const rgb = hslToRgb(hsl.h, hsl.s, hsl.l);
      draft.colors[key] = formatHex(rgb.r, rgb.g, rgb.b, rgba.a, rgba.explicitAlpha);
    });
    previewOverride = null;
    markEdited(false);
    syncTokenInputs();
    renderHslEditor();
  }

  function commitGroup(groupId, announce) {
    const state = groupStates.get(groupId);
    state.base = Object.fromEntries(state.keys.map(key => [key, draft.colors[key]]));
    resetGroupControls(state);
    if (announce) showToast(`${groupId === "surface" ? "Surface" : "Accent"} adjustment applied to the draft.`);
  }

  function resetGroup(groupId) {
    const state = groupStates.get(groupId);
    state.keys.forEach(key => { draft.colors[key] = state.base[key]; });
    resetGroupControls(state);
    previewOverride = null;
    markEdited(false);
    syncTokenInputs();
    renderHslEditor();
  }

  function resetGroupControls(state) {
    Object.values(state.controls).forEach(control => {
      control.input.value = 0;
      control.output.textContent = `0${control.suffix}`;
    });
  }

  function refreshPresetSelect(selectedId) {
    dom.presetSelect.replaceChildren();
    const builtInGroup = document.createElement("optgroup");
    builtInGroup.label = "Registry baselines";
    BUILTIN_PRESETS.filter(preset => preset.family === draft.family).forEach(preset => builtInGroup.append(optionFor(preset)));
    dom.presetSelect.appendChild(builtInGroup);
    const saved = customPresets.filter(preset => preset.family === draft.family);
    if (saved.length) {
      const savedGroup = document.createElement("optgroup");
      savedGroup.label = "Saved in this browser";
      saved.forEach(preset => savedGroup.append(optionFor(preset)));
      dom.presetSelect.appendChild(savedGroup);
    }
    if ([...dom.presetSelect.options].some(option => option.value === selectedId)) {
      dom.presetSelect.value = selectedId;
    }
  }

  function optionFor(preset) {
    const option = document.createElement("option");
    option.value = preset.id;
    option.textContent = preset.displayName;
    return option;
  }

  function loadPreset(preset) {
    if (!preset) return;
    draft = clone(preset);
    loadedSnapshot = clone(preset);
    previewOverride = null;
    selectedToken = "pageBackground";
    previewState = "idle";
    dom.stateSelector.querySelectorAll(".state-chip").forEach(chip => chip.setAttribute("aria-checked", chip.dataset.state === "idle" ? "true" : "false"));
    beginGroupBaselines();
    refreshPresetSelect(preset.id);
    syncAll();
  }

  function syncAll() {
    dom.familySelect.value = draft.family;
    dom.familyContract.textContent = FAMILY_CONTRACTS[draft.family].description;
    dom.displayNameInput.value = draft.displayName;
    dom.idInput.value = draft.id;
    syncTokenInputs();
    selectToken(selectedToken);
    updateDirtyState();
    updateCompareUi();
    renderPreview();
  }

  function syncTokenInputs() {
    TOKEN_KEYS.forEach(key => {
      const value = draft.colors[key];
      const controls = tokenControls.get(key);
      controls.picker.value = pickerHex(value);
      controls.hex.value = value;
      controls.hex.classList.remove("invalid");
    });
  }

  function markEdited(rebuildHsl = true) {
    previewOverride = null;
    if (rebuildHsl) renderHslEditor();
    updateDirtyState();
    updateCompareUi();
    renderPreview();
  }

  function updateDirtyState() {
    const isDirty = JSON.stringify(exportShape(draft)) !== JSON.stringify(exportShape(loadedSnapshot));
    dom.dirtyBadge.textContent = isDirty ? "Draft" : "Saved";
    dom.dirtyBadge.classList.toggle("dirty", isDirty);
  }

  function setCompareSlot(slot) {
    compareSlots[slot] = clone(draft);
    previewOverride = null;
    updateCompareUi();
    renderPreview();
    showToast(`${draft.displayName} captured as ${slot}.`);
  }

  function showCompareSlot(slot) {
    if (!compareSlots[slot]) return;
    previewOverride = { slot, theme: clone(compareSlots[slot]) };
    updateCompareUi();
    renderPreview();
  }

  function updateCompareUi() {
    dom.showAButton.disabled = !compareSlots.A;
    dom.showBButton.disabled = !compareSlots.B;
    dom.returnDraftButton.disabled = !previewOverride;
    dom.showAButton.textContent = compareSlots.A ? `Preview A · ${compareSlots.A.displayName}` : "Preview A";
    dom.showBButton.textContent = compareSlots.B ? `Preview B · ${compareSlots.B.displayName}` : "Preview B";
    if (!compareSlots.A && !compareSlots.B) {
      dom.compareSummary.textContent = "No snapshots";
      dom.compareNote.textContent = "Capture two drafts to compare their fixed fixtures.";
      return;
    }
    if (!(compareSlots.A && compareSlots.B)) {
      dom.compareSummary.textContent = "1 snapshot";
      dom.compareNote.textContent = "Capture the second slot to calculate token differences.";
      return;
    }
    const differences = TOKEN_KEYS.filter(key => compareSlots.A.colors[key] !== compareSlots.B.colors[key]).length;
    const sameFamily = compareSlots.A.family === compareSlots.B.family;
    dom.compareSummary.textContent = `${differences} token${differences === 1 ? "" : "s"} differ`;
    dom.compareNote.textContent = sameFamily
      ? `Valid same-family comparison: ${FAMILY_CONTRACTS[compareSlots.A.family].label} material stays fixed.`
      : "Cross-family preview is material reference only; it is not a colorway parity judgment.";
  }

  function renderPreview() {
    const theme = previewOverride?.theme || draft;
    const family = FAMILY_CONTRACTS[theme.family];
    const style = dom.themePreview.style;
    const variables = {
      "--p-bg": theme.colors.pageBackground,
      "--p-flat-bg": theme.colors.flatPageBackground,
      "--p-surface-base": cssColor(theme.colors.surfaceBase),
      "--p-surface-raised": cssColor(theme.colors.surfaceRaised),
      "--p-surface-control": cssColor(theme.colors.surfaceControl),
      "--p-surface-inset": cssColor(theme.colors.surfaceInset),
      "--p-surface-field": cssColor(theme.colors.surfaceField),
      "--p-text": cssColor(theme.colors.textPrimary),
      "--p-text-secondary": cssColor(theme.colors.textSecondary),
      "--p-text-disabled": cssColor(theme.colors.textDisabled),
      "--p-edge": cssColor(theme.colors.edgeNeutral),
      "--p-edge-strong": cssColor(theme.colors.edgeStrong),
      "--p-divider": cssColor(theme.colors.quickActionDivider),
      "--p-accent": cssColor(theme.colors.accent),
      "--p-accent-soft": cssColor(theme.colors.accentGradientEnd),
      "--p-accent-strong": cssColor(theme.colors.accentStrong),
      "--p-focus": cssColor(theme.colors.focus),
      "--p-selection": cssColor(theme.colors.selection),
      "--p-active-input": cssColor(theme.colors.activeInput),
      "--s-success": cssColor(family.semantic.success),
      "--s-warning": cssColor(family.semantic.warning),
      "--s-error": cssColor(family.semantic.error),
      "--s-recording": cssColor(family.semantic.recording),
      "--s-disabled": cssColor(theme.colors.textDisabled)
    };
    Object.entries(variables).forEach(([name, value]) => style.setProperty(name, value));
    dom.themePreview.classList.toggle("family-heimdall", theme.family === "heimdall");
    dom.themePreview.classList.toggle("family-freya", theme.family === "freya");
    dom.themePreview.dataset.state = previewState;
    dom.previewSourceBadge.textContent = previewOverride ? `${previewOverride.slot} · ${theme.displayName}` : `Draft · ${theme.displayName}`;
    dom.previewSourceBadge.title = theme.id;
    updateSemanticCopy();
    updateContrastStatus(theme);
  }

  function updateSemanticCopy() {
    const label = dom.themePreview.querySelector(".semantic-label");
    const title = dom.themePreview.querySelector(".state-overlay-title");
    const copy = dom.themePreview.querySelector(".state-overlay-copy");
    const messages = {
      idle: ["Ready", "", ""],
      focus: ["Focused", "", ""],
      press: ["Pressed", "", ""],
      running: ["Running", "", ""],
      recording: ["Recording", "Recording active", "Semantic red remains locked"],
      warning: ["Attention", "Warning state", "Semantic amber remains locked"],
      error: ["Unavailable", "Error state", "Semantic red remains locked"]
    };
    const [header, overlayTitle, overlayCopy] = messages[previewState];
    label.textContent = header;
    title.textContent = overlayTitle;
    copy.textContent = overlayCopy;
  }

  function updateContrastStatus(theme) {
    const page = opaqueRgb(theme.colors.pageBackground, { r: 0, g: 0, b: 0 });
    const surface = opaqueRgb(theme.colors.surfaceBase, page);
    const pairs = [
      [theme.colors.textPrimary, page, 4.5],
      [theme.colors.textPrimary, surface, 4.5],
      [theme.colors.textSecondary, page, 3.0],
      [theme.colors.textSecondary, surface, 3.0]
    ];
    const passed = pairs.filter(([foreground, background, minimum]) => contrastRatio(opaqueRgb(foreground, background), background) >= minimum).length;
    dom.contrastStatus.textContent = `${passed}/${pairs.length} text checks pass`;
    dom.contrastStatus.classList.toggle("good", passed === pairs.length);
    dom.contrastStatus.classList.toggle("warning", passed !== pairs.length);
  }

  function fitPreview() {
    const horizontalPadding = 44;
    const verticalPadding = 44;
    const width = Math.max(260, dom.previewViewport.clientWidth - horizontalPadding);
    const height = Math.max(240, dom.previewViewport.clientHeight - verticalPadding);
    const scale = Math.min(width / 1240, height / 1080, 1);
    dom.previewWrap.style.width = `${1240 * scale}px`;
    dom.previewWrap.style.height = `${1080 * scale}px`;
    dom.themePreview.style.transform = `scale(${scale})`;
    dom.scaleLabel.textContent = `${Math.round(scale * 100)}%`;
  }

  function openSaveDialog(mode) {
    saveMode = mode;
    const suffix = mode === "duplicate" ? ".copy" : "";
    dom.saveNameInput.value = mode === "duplicate" ? `${draft.displayName} Copy` : draft.displayName;
    dom.saveIdInput.value = `${draft.id}${suffix}`;
    dom.saveError.textContent = "";
    dom.saveDialog.showModal();
    dom.saveNameInput.focus();
    dom.saveNameInput.select();
  }

  function saveCustomPreset() {
    const displayName = dom.saveNameInput.value.trim();
    const id = dom.saveIdInput.value.trim().toLowerCase();
    const error = identityError(id, displayName, draft.family);
    if (error) {
      dom.saveError.textContent = error;
      return;
    }
    if (BUILTIN_PRESETS.some(preset => preset.id === id)) {
      dom.saveError.textContent = "Registry baseline IDs are immutable. Choose a new ID.";
      return;
    }
    const saved = clone(draft);
    saved.id = id;
    saved.displayName = displayName;
    delete saved.builtin;
    const existingIndex = customPresets.findIndex(preset => preset.id === id);
    if (existingIndex >= 0) customPresets.splice(existingIndex, 1, saved);
    else customPresets.push(saved);
    persistCustomPresets();
    draft = clone(saved);
    loadedSnapshot = clone(saved);
    beginGroupBaselines();
    refreshPresetSelect(saved.id);
    dom.saveDialog.close();
    syncAll();
    showToast(`${displayName} saved in this browser${saveMode === "duplicate" ? " as a duplicate" : ""}.`);
  }

  function identityError(id, displayName, family) {
    if (!displayName) return "Display name is required.";
    if (!new RegExp(`^${family}\\.[a-z0-9][a-z0-9.-]*$`).test(id)) {
      return `ID must start with ${family}. and contain lowercase letters, digits, dots, or hyphens.`;
    }
    return "";
  }

  function exportColorway() {
    const id = draft.id.trim().toLowerCase();
    const displayName = draft.displayName.trim();
    const error = identityError(id, displayName, draft.family);
    if (error) {
      showToast(error, true);
      return;
    }
    draft.id = id;
    draft.displayName = displayName;
    const payload = exportShape(draft, true);
    const blob = new Blob([`${JSON.stringify(payload, null, 2)}\n`], { type: "application/json" });
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = `${id}.colorway.json`;
    document.body.appendChild(link);
    link.click();
    link.remove();
    URL.revokeObjectURL(url);
    syncAll();
    showToast(`${displayName} exported with ${TOKEN_KEYS.length} color tokens.`);
  }

  function exportShape(theme, includeSchema = false) {
    const shape = {
      format: FORMAT,
      schemaVersion: SCHEMA_VERSION,
      id: theme.id,
      family: theme.family,
      displayName: theme.displayName,
      colors: Object.fromEntries(TOKEN_KEYS.map(key => [key, theme.colors[key]]))
    };
    if (includeSchema) return { $schema: "./theme-colorway.schema.json", ...shape };
    return shape;
  }

  async function importFile(event) {
    const file = event.target.files?.[0];
    event.target.value = "";
    if (!file) return;
    try {
      const raw = JSON.parse(await file.text());
      const result = normalizeImported(raw);
      draft = result.theme;
      loadedSnapshot = clone(draft);
      previewOverride = null;
      selectedToken = "pageBackground";
      beginGroupBaselines();
      refreshPresetSelect();
      syncAll();
      const suffix = result.filled.length ? ` ${result.filled.length} missing token(s) used the ${FAMILY_CONTRACTS[draft.family].label} baseline.` : "";
      showToast(`${draft.displayName} imported.${suffix}`);
    } catch (error) {
      showToast(`Import failed: ${error.message}`, true);
    }
  }

  function normalizeImported(raw) {
    if (!raw || typeof raw !== "object" || Array.isArray(raw)) throw new Error("The JSON root must be an object.");
    if (raw.format && raw.format !== FORMAT) throw new Error(`Unsupported format: ${raw.format}`);
    if (raw.schemaVersion !== undefined && Number(raw.schemaVersion) !== SCHEMA_VERSION) throw new Error(`Unsupported schemaVersion: ${raw.schemaVersion}`);
    const family = String(raw.family || "").toLowerCase();
    if (!FAMILY_CONTRACTS[family]) throw new Error("family must be heimdall or freya.");
    const id = String(raw.id || "").trim().toLowerCase();
    const displayName = String(raw.displayName || "").trim();
    const identity = identityError(id, displayName, family);
    if (identity) throw new Error(identity);
    if (!raw.colors || typeof raw.colors !== "object" || Array.isArray(raw.colors)) throw new Error("colors must be an object.");

    const baseline = clone(BUILTIN_PRESETS.find(preset => preset.family === family));
    const filled = [];
    TOKEN_KEYS.forEach(key => {
      if (raw.colors[key] === undefined) {
        filled.push(key);
        return;
      }
      const normalized = safeNormalizeHex(raw.colors[key]);
      if (!normalized) throw new Error(`${key} must use #RRGGBB or #AARRGGBB.`);
      baseline.colors[key] = normalized;
    });
    baseline.id = id;
    baseline.displayName = displayName;
    delete baseline.builtin;
    return { theme: baseline, filled };
  }

  function loadCustomPresets() {
    try {
      const value = JSON.parse(localStorage.getItem(STORAGE_KEY) || "[]");
      if (!Array.isArray(value)) return [];
      return value.map(item => normalizeImported(item).theme);
    } catch (_) {
      return [];
    }
  }

  function persistCustomPresets() {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(customPresets.map(preset => exportShape(preset))));
    } catch (error) {
      showToast(`Could not save browser preset: ${error.message}`, true);
    }
  }

  function showToast(message, isError = false) {
    clearTimeout(toastTimer);
    dom.toast.textContent = message;
    dom.toast.classList.toggle("error", isError);
    dom.toast.classList.add("show");
    toastTimer = setTimeout(() => dom.toast.classList.remove("show"), 3200);
  }

  function normalizeHex(value) {
    const normalized = safeNormalizeHex(value);
    if (!normalized) throw new Error(`Invalid color: ${value}`);
    return normalized;
  }

  function safeNormalizeHex(value) {
    if (typeof value !== "string") return null;
    let hex = value.trim().toUpperCase();
    if (/^#[0-9A-F]{3}$/.test(hex)) {
      hex = `#${hex[1]}${hex[1]}${hex[2]}${hex[2]}${hex[3]}${hex[3]}`;
    }
    return /^#[0-9A-F]{6}([0-9A-F]{2})?$/.test(hex) ? hex : null;
  }

  function parseHex(value) {
    const hex = normalizeHex(value).slice(1);
    const explicitAlpha = hex.length === 8;
    const offset = explicitAlpha ? 2 : 0;
    return {
      a: explicitAlpha ? parseInt(hex.slice(0, 2), 16) / 255 : 1,
      r: parseInt(hex.slice(offset, offset + 2), 16),
      g: parseInt(hex.slice(offset + 2, offset + 4), 16),
      b: parseInt(hex.slice(offset + 4, offset + 6), 16),
      explicitAlpha
    };
  }

  function pickerHex(value) {
    const rgba = parseHex(value);
    return `#${byte(rgba.r)}${byte(rgba.g)}${byte(rgba.b)}`;
  }

  function cssColor(value) {
    const rgba = parseHex(value);
    if (rgba.a >= 0.999) return pickerHex(value);
    return `rgba(${rgba.r}, ${rgba.g}, ${rgba.b}, ${Number(rgba.a.toFixed(3))})`;
  }

  function formatHex(r, g, b, alpha = 1, explicitAlpha = false) {
    const rgb = `${byte(r)}${byte(g)}${byte(b)}`;
    return explicitAlpha || alpha < 0.999 ? `#${byte(alpha * 255)}${rgb}` : `#${rgb}`;
  }

  function byte(value) { return Math.round(clamp(value, 0, 255)).toString(16).padStart(2, "0").toUpperCase(); }
  function clamp(value, min, max) { return Math.min(max, Math.max(min, value)); }
  function wrapHue(value) { return ((value % 360) + 360) % 360; }

  function rgbToHsl(r, g, b) {
    r /= 255; g /= 255; b /= 255;
    const max = Math.max(r, g, b);
    const min = Math.min(r, g, b);
    let h = 0;
    let s = 0;
    const l = (max + min) / 2;
    const delta = max - min;
    if (delta !== 0) {
      s = delta / (1 - Math.abs(2 * l - 1));
      if (max === r) h = 60 * (((g - b) / delta) % 6);
      else if (max === g) h = 60 * ((b - r) / delta + 2);
      else h = 60 * ((r - g) / delta + 4);
    }
    return { h: wrapHue(h), s: s * 100, l: l * 100 };
  }

  function hslToRgb(h, s, l) {
    h = wrapHue(h);
    s = clamp(s, 0, 100) / 100;
    l = clamp(l, 0, 100) / 100;
    const c = (1 - Math.abs(2 * l - 1)) * s;
    const x = c * (1 - Math.abs((h / 60) % 2 - 1));
    const m = l - c / 2;
    let rgb;
    if (h < 60) rgb = [c, x, 0];
    else if (h < 120) rgb = [x, c, 0];
    else if (h < 180) rgb = [0, c, x];
    else if (h < 240) rgb = [0, x, c];
    else if (h < 300) rgb = [x, 0, c];
    else rgb = [c, 0, x];
    return { r: (rgb[0] + m) * 255, g: (rgb[1] + m) * 255, b: (rgb[2] + m) * 255 };
  }

  function opaqueRgb(value, backdrop) {
    if (typeof value === "object" && value.r !== undefined && value.a === undefined) return value;
    const rgba = typeof value === "string" ? parseHex(value) : value;
    const background = backdrop || { r: 255, g: 255, b: 255 };
    return {
      r: rgba.r * rgba.a + background.r * (1 - rgba.a),
      g: rgba.g * rgba.a + background.g * (1 - rgba.a),
      b: rgba.b * rgba.a + background.b * (1 - rgba.a)
    };
  }

  function contrastRatio(a, b) {
    const lighter = Math.max(luminance(a), luminance(b));
    const darker = Math.min(luminance(a), luminance(b));
    return (lighter + .05) / (darker + .05);
  }

  function luminance(rgb) {
    const channels = [rgb.r, rgb.g, rgb.b].map(value => {
      const channel = value / 255;
      return channel <= .03928 ? channel / 12.92 : Math.pow((channel + .055) / 1.055, 2.4);
    });
    return .2126 * channels[0] + .7152 * channels[1] + .0722 * channels[2];
  }
})();
