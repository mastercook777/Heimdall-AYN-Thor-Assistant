package com.mastercook777.heimdall;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ActivityOptions;
import android.content.ClipData;
import android.content.BroadcastReceiver;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.hardware.display.DisplayManager;
import android.media.AudioManager;
import android.media.projection.MediaProjectionManager;
import android.media.projection.MediaProjectionConfig;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import android.graphics.pdf.PdfRenderer;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.Display;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.animation.PathInterpolator;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AssistantActivity extends Activity {
    private static final PathInterpolator UI_EASE_OUT =
            new PathInterpolator(0.2f, 0f, 0f, 1f);
    private static final long TOUCH_SURFACE_PRESS_IN_MS = 70L;
    private static final long TOUCH_SURFACE_PRESS_OUT_MS = 110L;
    private static final long PANEL_ENTER_MS = 160L;
    private static final long PANEL_EXIT_MS = 110L;
    private static final long CONTENT_TRANSITION_MS = 160L;
    private static final int CONTENT_ENTER_TRANSLATION_DP = 32;
    private static final long SETTINGS_DOCK_EXIT_MS = 120L;
    private static final long SETTINGS_DOCK_ENTER_MS = 140L;
    private static final long SETTINGS_PAGE_ENTER_MS = 160L;
    private static final long SETTINGS_PAGE_EXIT_MS = 110L;
    private static final int SETTINGS_PAGE_ENTER_TRANSLATION_DP = 32;
    private static final int SETTINGS_PAGE_EXIT_TRANSLATION_DP = 24;
    private static final int SETTINGS_TRANSITION_NONE = 0;
    private static final int SETTINGS_TRANSITION_ENTERING = 1;
    private static final int SETTINGS_TRANSITION_EXITING = 2;
    private static final int REQUEST_GUIDE_FILE = 2101;
    private static final int REQUEST_MAP_FILE = 2102;
    private static final int REQUEST_EXPORT_PROFILES = 2103;
    private static final int REQUEST_IMPORT_PROFILES = 2104;
    private static final int REQUEST_GUIDE_TEXT_FILE = 2105;
    private static final int REQUEST_PROFILE_ICON = 2106;
    private static final int REQUEST_MACRO_ICON = 2107;
    private static final int REQUEST_SCREEN_RECORDING = 2108;
    private static final int REQUEST_RECORD_AUDIO = 2109;
    private static final int REQUEST_MAGNIFIER_PROJECTION = 2110;
    private static final int REQUEST_CANVAS_IMAGE = 2111;
    private static final int REQUEST_DIAGNOSTIC_EXPORT = 2112;
    private static final int MAGNIFIER_REGION_START_MAX_ATTEMPTS = 12;
    private static final long MAGNIFIER_REGION_START_RETRY_MS = 150L;
    private static final int BG = HeimdallUi.COLOR_BG;
    private static final int PANEL = HeimdallUi.COLOR_SURFACE;
    private static final int PANEL_ALT = HeimdallUi.COLOR_SURFACE_RAISED;
    private static final int PANEL_SOFT = HeimdallUi.COLOR_SURFACE_SOFT;
    private static final int PRIMARY = HeimdallUi.COLOR_ACCENT;
    private static final int PRIMARY_DARK = HeimdallUi.COLOR_ACCENT_DARK;
    private static final int TEXT = HeimdallUi.COLOR_TEXT;
    private static final int MUTED = HeimdallUi.COLOR_TEXT_MUTED;
    private static final int BORDER = HeimdallUi.COLOR_BORDER;
    private static final int DANGER = HeimdallUi.COLOR_DANGER;
    private static final int DANGER_BG = HeimdallUi.COLOR_DANGER_BG;
    private static final int GRID_EDITOR_COLUMNS = 6;
    private static final int GRID_EDITOR_ROWS = 8;
    private static final int SCREEN_MAIN = 0;
    private static final int SCREEN_MAP = 1;
    private static final int SCREEN_GUIDE = 2;
    private static final int SCREEN_SETTINGS = 3;
    private static final int MAP_VIEW_LOCAL = 0;
    private static final int MAP_VIEW_INTERACTIVE = 1;
    private static final int SETTINGS_LAYOUT = 0;
    private static final int SETTINGS_TOUCHPAD = 1;
    private static final int SETTINGS_MACRO = 2;
    private static final int SETTINGS_INPUT = 3;
    private static final int SETTINGS_PROFILE = 4;
    private static final int SETTINGS_MAGNIFIER = 5;
    private static final int SETTINGS_APPEARANCE = 6;
    private static final int SETTINGS_DIAGNOSTICS = 7;
    private static final String STATE_ACTIVE_SCREEN = "heimdall.active_screen";
    private static final String STATE_SETTINGS_SECTION = "heimdall.settings_section";
    private static final String STATE_SETTINGS_SCROLL_Y = "heimdall.settings_scroll_y";
    private static final String STATE_LAYOUT_DRAFT = "heimdall.layout_draft";
    private static final String STATE_TOUCHPAD_DRAFT = "heimdall.touchpad_draft";
    private static final String STATE_MACRO_MAPPING_PROTECTION_DRAFT =
            "heimdall.macro_mapping_protection_draft";
    private static final String STATE_MAGNIFIER_DRAFT = "heimdall.magnifier_draft";
    private static final String STATE_THEME_DRAFT = "heimdall.theme_draft";
    private static final String STATE_COMPATIBILITY_DRAFT = "heimdall.compatibility_draft";
    private static final String STATE_TOUCHPAD_ADVANCED = "heimdall.touchpad_advanced";
    private static final String STATE_INPUT_DIAGNOSTICS = "heimdall.input_diagnostics";
    private static final String STATE_PROFILE_DETAILS = "heimdall.profile_details";
    private static final String STATE_PROFILE_INPUTS_PRESENT = "heimdall.profile_inputs_present";
    private static final String STATE_PROFILE_NAME_DRAFT = "heimdall.profile_name_draft";
    private static final String STATE_PROFILE_PACKAGE_DRAFT = "heimdall.profile_package_draft";
    private static final String STATE_PROFILE_ROM_DRAFT = "heimdall.profile_rom_draft";
    private static final String STATE_PROFILE_DEFAULT_DRAFT = "heimdall.profile_default_draft";

    private List<GameProfile> profiles;
    private GameProfile selectedProfile;
    private int selectedProfileIndex;
    private int activeScreen = SCREEN_MAIN;
    private int activeSettingsSection = SETTINGS_TOUCHPAD;
    private WidgetLayout draftWidgetLayout;
    private ScrollView settingsContentScroll;
    private LinearLayout settingsContentContainer;
    private int settingsContentScrollY;
    private ProfileIconView profileIconView;
    private TextView profileTitle;
    private TextView systemTimeText;
    private ImageView systemNetworkIcon;
    private BatteryStatusView systemBatteryIcon;
    private TextView systemBatteryText;
    private String lastSystemTime = "";
    private String lastNetworkLabel = "";
    private String lastBatteryLabel = "";
    private TextView statusText;
    private final List<MacroGridBinding> macroGrids = new ArrayList<>();
    private final List<MacroModuleEditorBinding> settingsMacroModuleEditors = new ArrayList<>();
    private CheckBox settingsMacroMappingProtectionInput;
    private Boolean settingsMacroMappingProtectionDraft;
    private final List<View> transientAnimatedViews = new ArrayList<>();
    private final List<View> closingAnimatedPanels = new ArrayList<>();
    private final List<PanelOverlay> panelOverlays = new ArrayList<>();
    private final List<DockNavButton> dockNavButtons = new ArrayList<>();
    private FrameLayout overlayHost;
    private FrameLayout contentHost;
    private DockNavBar dockNavBar;
    private LinearLayout bottomDock;
    private View currentContentPage;
    private int contentTransitionGeneration;
    private int settingsTransitionGeneration;
    private int settingsTransitionState = SETTINGS_TRANSITION_NONE;
    private boolean closeSettingsAfterEnter;
    private AlertDialog widgetGridDialog;
    private LinearLayout profileList;
    private TouchPadView touchPadView;
    private int targetDisplayId = Display.DEFAULT_DISPLAY;
    private int targetDisplayWidth;
    private int targetDisplayHeight;
    private int targetDisplayDensityDpi;
    private TouchpadSettings touchpadSettings;
    private String settingsTouchpadMode;
    private TouchpadSettings settingsTouchpadDraft;
    private SliderInput settingsSensitivityInput;
    private SliderInput settingsIntervalInput;
    private SliderInput settingsMinDeltaInput;
    private SliderInput settingsAnchorXInput;
    private SliderInput settingsAnchorYInput;
    private SliderInput settingsStrokeInput;
    private SliderInput settingsShizukuTouchSensitivityXInput;
    private SliderInput settingsShizukuTouchSensitivityYInput;
    private SliderInput settingsShizukuTouchFrameInput;
    private SliderInput settingsShizukuTouchMinDeltaInput;
    private SliderInput settingsShizukuTouchCurveInput;
    private SliderInput settingsShizukuTouchSmoothingInput;
    private SliderInput settingsRightStickSensitivityInput;
    private SliderInput settingsRightStickDeadzoneInput;
    private SliderInput settingsRightStickCurveInput;
    private SliderInput settingsRightStickMaxInput;
    private SliderInput settingsRightStickRadiusInput;
    private SliderInput settingsRightStickRecenterInput;
    private String settingsRightStickCenterMode;
    private SliderInput settingsRelativeMouseSensitivityInput;
    private SliderInput settingsRelativeMouseMaxOutputInput;
    private SliderInput settingsRelativeMousePulseInput;
    private CheckBox settingsRelativeMouseInvertYInput;
    private String settingsRelativeMouseAcceleration;
    private EditText settingsProfileNameInput;
    private EditText settingsProfilePackageInput;
    private EditText settingsProfileRomInput;
    private CheckBox settingsProfileDefaultInput;
    private WidgetLayout.Item settingsMagnifierDraft;
    private String settingsThemeDraft;
    private Boolean settingsPerformanceCompatibilityDraft;
    private boolean showTouchpadAdvancedSettings;
    private boolean showInputDiagnostics;
    private boolean showProfileDetectionDetails;
    private TextView pendingGuideFileSummary;
    private String[] pendingGuideFileUriDraft;
    private EditText pendingGuideFileTitleInput;
    private GuideFileTitleState pendingGuideFileTitleState;
    private List<GameProfile> pendingProfileExportProfiles;
    private ProfileBundleStore.PreparedImport pendingProfileImport;
    private ProfileBundleStore.Request pendingProfileBundleRequest;
    private int pendingProfileIconIndex = -1;
    private String[] pendingMacroIconKey;
    private ImageView pendingMacroIconPreview;
    private Macro pendingMacroIconMacro;
    private boolean editingInteractiveMapInline;
    private int activeMapViewerMode = MAP_VIEW_LOCAL;
    private WebView activeMapWebView;
    private TextView activeMapWebStatus;
    private boolean activeMapWebError;
    private Bitmap activeLocalMapBitmap;
    private final List<Bitmap> activeLocalMapThumbnails = new ArrayList<>();
    private int activeLocalMapIndex;
    private boolean mapViewerFullscreen;
    private String mapWebCurrentUrl;
    private final ThorPerformanceCompatibility thorPerformanceCompatibility =
            new ThorPerformanceCompatibility();
    private final HeimdallStabilityDiagnostics stabilityDiagnostics =
            new HeimdallStabilityDiagnostics(thorPerformanceCompatibility);
    private final ThorGameFocusProtection thorGameFocusProtection =
            new ThorGameFocusProtection();
    private final ThorTextInputFocusLease thorTextInputFocusLease =
            new ThorTextInputFocusLease();
    private final ThorImeSessionInitializer thorImeSessionInitializer =
            new ThorImeSessionInitializer();
    private ViewTreeObserver.OnGlobalFocusChangeListener textInputFocusListener;
    private ViewTreeObserver.OnGlobalLayoutListener imeVisibilityListener;
    private boolean textInputFocused;
    private boolean imeVisibleForPerformanceCompatibility;
    private View fullscreenMapControls;
    private View fullscreenMapReveal;
    private View fullscreenGuideControls;
    private View fullscreenGuideReveal;
    private MapMarker editingMapMarkerInline;
    private boolean creatingMapMarkerInline;
    private GuideEntry editingGuideInline;
    private String editingGuideTypeInline;
    private GuideEntry viewingGuideInline;
    private GuideTextReaderView activeGuideReaderView;
    private GuideEntry activeGuideReaderEntry;
    private String activeGuideReaderFingerprint = "";
    private GuideTextDocument cachedGuideTextDocument;
    private GuideEntry cachedGuideTextEntry;
    private int guideTextLoadGeneration;
    private boolean guideReaderFullscreen;
    private EditText pendingGuideTextInput;
    private List<MacroStep> activeDraftSteps;
    private LinearLayout activeStepsList;
    private Macro activeEditingMacro;
    private boolean captureInProgress;
    private GamepadRecordingSession activeGamepadRecordingSession;
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private final InputBridge.Callback inputStatusCallback = new InputBridge.Callback() {
        @Override
        public void onStatus(String message) {
            showAction(message);
        }

        @Override
        public void onError(String message) {
            showErrorAction(message);
        }
    };
    private final ForegroundAppTracker.Listener foregroundAppListener =
            snapshot -> uiHandler.post(() -> maybeAutoSwitchProfile(snapshot));
    private boolean profileAwarenessActive;
    private boolean activityStarted;
    private final Runnable profileAwarenessTicker = new Runnable() {
        @Override
        public void run() {
            if (!profileAwarenessActive) {
                return;
            }
            long started = DebugPerformanceDiagnostics.beginTask("App-aware upper-window scan");
            if (ForegroundAppTracker.isEnabled(AssistantActivity.this)) {
                ThorAccessibilityService service = ThorAccessibilityService.getInstance();
                if (service != null) {
                    service.refreshForegroundApp();
                }
                maybeAutoSwitchProfile(ForegroundAppTracker.latest());
            }
            DebugPerformanceDiagnostics.endTask("App-aware upper-window scan", started);
            uiHandler.postDelayed(this, 1200L);
        }
    };
    private final List<QuickActionButtonView> quickScreenshotButtons = new ArrayList<>();
    private final List<QuickActionButtonView> quickRecordButtons = new ArrayList<>();
    private final List<UpperScreenMagnifierView> magnifierViews = new ArrayList<>();
    private final List<CanvasWidgetView> canvasViews = new ArrayList<>();
    private String pendingRecordingProfileName;
    private WidgetLayout.Item pendingMagnifierProjectionItem;
    private WidgetLayout.Item pendingMagnifierRegionAfterProjectionItem;
    private float pendingMagnifierRegionAfterProjectionAspectRatio;
    private int pendingMagnifierRegionStartAttempts;
    private WidgetLayout.Item pendingMagnifierRegionItem;
    private GameProfile pendingMagnifierRegionProfile;
    private boolean magnifierRegionCaptureInProgress;
    private boolean pendingMagnifierRegionUsesDraft;
    private boolean magnifierWasFrozenBeforeRegionCapture;
    private GameProfile pendingCanvasImportProfile;
    private WidgetLayout.Item pendingCanvasImportItem;
    private int pendingCanvasImportFrameWidth = 1;
    private int pendingCanvasImportFrameHeight = 1;
    private CanvasAssetStore.Request pendingCanvasImportRequest;
    private boolean canvasOverlayActive;
    private final Runnable hideFullscreenMapControls = new Runnable() {
        @Override
        public void run() {
            setFullscreenMapControlsVisible(false);
        }
    };
    private final Runnable hideFullscreenGuideControls = new Runnable() {
        @Override
        public void run() {
            setFullscreenGuideControlsVisible(false);
        }
    };
    private boolean statusReceiverRegistered;
    private final BroadcastReceiver statusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateSystemStatus();
        }
    };
    private final BroadcastReceiver captureReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (CoordinateCaptureActivity.ACTION_REGION_PREVIEW.equals(intent.getAction())) {
                handleMagnifierRegionPreview(intent);
                return;
            }
            if (CoordinateCaptureActivity.ACTION_REGION_CANCELLED.equals(intent.getAction())) {
                cancelMagnifierRegionCapture();
                return;
            }
            if (CoordinateCaptureActivity.ACTION_REGION_CAPTURED.equals(intent.getAction())) {
                handleMagnifierRegionCaptured(intent);
                return;
            }
            if (!CoordinateCaptureActivity.ACTION_CAPTURED.equals(intent.getAction())) {
                return;
            }
            if (activeDraftSteps == null && activeEditingMacro == null) {
                showAction(getString(R.string.action_capture_editor_closed));
                return;
            }
            String step = intent.getStringExtra(CoordinateCaptureActivity.EXTRA_STEP);
            if (step != null && step.length() > 0) {
                MacroStep parsedStep = MacroStep.parse(step);
                if (activeDraftSteps == null) {
                    activeDraftSteps = new ArrayList<>();
                    if (activeEditingMacro != null) {
                        activeDraftSteps.addAll(activeEditingMacro.steps);
                    }
                }
                activeDraftSteps.add(parsedStep);
                if (activeStepsList != null) {
                    renderStepList(activeDraftSteps, activeStepsList);
                }
                if (activeEditingMacro != null) {
                    activeEditingMacro.steps.clear();
                    activeEditingMacro.steps.addAll(activeDraftSteps);
                    ProfileStore.saveProfiles(AssistantActivity.this, profiles);
                    renderSelectedProfile();
                }
                captureInProgress = false;
                if (activeStepsList == null) {
                    activeDraftSteps = null;
                    activeEditingMacro = null;
                }
                showAction(getString(R.string.action_capture_step_added, step));
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DebugPerformanceDiagnostics.initialize(this);
        HeimdallStabilityDiagnostics.reportPreviousExit(this);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        enterImmersiveMode();

        profiles = ProfileStore.loadProfiles(this);
        selectedProfileIndex = ProfileStore.loadSelectedIndex(this, profiles.size());
        selectedProfile = profiles.get(selectedProfileIndex);
        touchpadSettings = selectedProfile.safeTouchpadSettings();
        restoreUiInstanceState(savedInstanceState);
        registerCaptureReceiver();
        updateTargetDisplayInfo();
        setContentView(createLayout());
        restoreVisibleProfileDraft(savedInstanceState);
        renderProfiles();
        renderSelectedProfile();
        startStatusUpdates();
        DebugPerformanceDiagnostics.attachRootObservers(this);
        installTextInputCompatibilityObservers();
        getWindow().getDecorView().post(() -> thorPerformanceCompatibility.apply(
                this, DebugPerformanceDiagnostics.isCompositionProbe()));
        if (activeScreen == SCREEN_SETTINGS && settingsContentScroll != null) {
            int restoreY = Math.max(0, settingsContentScrollY);
            settingsContentScroll.post(() -> settingsContentScroll.scrollTo(0, restoreY));
        }
        if (isWindowFocusRequired()) {
            updateGameFocusProtection();
        } else {
            initializeLowerDisplayImeSession();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (activeScreen == SCREEN_SETTINGS && activeSettingsSection == SETTINGS_TOUCHPAD
                && settingsTouchpadDraft != null) {
            applySettingsTouchpadInputs();
        }
        if (settingsContentScroll != null) {
            settingsContentScrollY = settingsContentScroll.getScrollY();
        }
        if (settingsMacroMappingProtectionInput != null) {
            settingsMacroMappingProtectionDraft =
                    settingsMacroMappingProtectionInput.isChecked();
        }
        outState.putInt(STATE_ACTIVE_SCREEN, activeScreen);
        outState.putInt(STATE_SETTINGS_SECTION, activeSettingsSection);
        outState.putInt(STATE_SETTINGS_SCROLL_Y, settingsContentScrollY);
        putJsonState(outState, STATE_LAYOUT_DRAFT, draftWidgetLayout);
        putJsonState(outState, STATE_TOUCHPAD_DRAFT, settingsTouchpadDraft);
        if (settingsMacroMappingProtectionDraft != null) {
            outState.putBoolean(STATE_MACRO_MAPPING_PROTECTION_DRAFT,
                    settingsMacroMappingProtectionDraft);
        }
        putJsonState(outState, STATE_MAGNIFIER_DRAFT, settingsMagnifierDraft);
        if (settingsThemeDraft != null) {
            outState.putString(STATE_THEME_DRAFT, settingsThemeDraft);
        }
        if (settingsPerformanceCompatibilityDraft != null) {
            outState.putBoolean(STATE_COMPATIBILITY_DRAFT,
                    settingsPerformanceCompatibilityDraft);
        }
        outState.putBoolean(STATE_TOUCHPAD_ADVANCED, showTouchpadAdvancedSettings);
        outState.putBoolean(STATE_INPUT_DIAGNOSTICS, showInputDiagnostics);
        outState.putBoolean(STATE_PROFILE_DETAILS, showProfileDetectionDetails);
        if (settingsProfileNameInput != null && settingsProfilePackageInput != null
                && settingsProfileRomInput != null && settingsProfileDefaultInput != null) {
            outState.putBoolean(STATE_PROFILE_INPUTS_PRESENT, true);
            outState.putString(STATE_PROFILE_NAME_DRAFT,
                    settingsProfileNameInput.getText().toString());
            outState.putString(STATE_PROFILE_PACKAGE_DRAFT,
                    settingsProfilePackageInput.getText().toString());
            outState.putString(STATE_PROFILE_ROM_DRAFT,
                    settingsProfileRomInput.getText().toString());
            outState.putBoolean(STATE_PROFILE_DEFAULT_DRAFT,
                    settingsProfileDefaultInput.isChecked());
        }
        super.onSaveInstanceState(outState);
    }

    private void restoreUiInstanceState(Bundle state) {
        if (state == null) {
            return;
        }
        activeScreen = state.getInt(STATE_ACTIVE_SCREEN, SCREEN_MAIN);
        activeSettingsSection = state.getInt(STATE_SETTINGS_SECTION, SETTINGS_TOUCHPAD);
        settingsContentScrollY = state.getInt(STATE_SETTINGS_SCROLL_Y, 0);
        try {
            String layoutJson = state.getString(STATE_LAYOUT_DRAFT);
            if (layoutJson != null) {
                draftWidgetLayout = WidgetLayout.fromJson(new JSONObject(layoutJson));
            }
            String touchpadJson = state.getString(STATE_TOUCHPAD_DRAFT);
            if (touchpadJson != null) {
                settingsTouchpadDraft = TouchpadSettings.fromJson(
                        new JSONObject(touchpadJson), selectedProfile.safeTouchpadSettings());
                settingsTouchpadMode = settingsTouchpadDraft.mode;
            }
            String magnifierJson = state.getString(STATE_MAGNIFIER_DRAFT);
            if (magnifierJson != null) {
                settingsMagnifierDraft = WidgetLayout.Item.fromJson(
                        new JSONObject(magnifierJson));
            }
        } catch (JSONException ignored) {
            // A malformed transient bundle must never overwrite persisted Profile data.
        }
        settingsThemeDraft = state.getString(STATE_THEME_DRAFT);
        if (state.containsKey(STATE_MACRO_MAPPING_PROTECTION_DRAFT)) {
            settingsMacroMappingProtectionDraft = state.getBoolean(
                    STATE_MACRO_MAPPING_PROTECTION_DRAFT);
        }
        if (state.containsKey(STATE_COMPATIBILITY_DRAFT)) {
            settingsPerformanceCompatibilityDraft =
                    state.getBoolean(STATE_COMPATIBILITY_DRAFT);
        }
        showTouchpadAdvancedSettings = state.getBoolean(STATE_TOUCHPAD_ADVANCED, false);
        showInputDiagnostics = state.getBoolean(STATE_INPUT_DIAGNOSTICS, false);
        showProfileDetectionDetails = state.getBoolean(STATE_PROFILE_DETAILS, false);
    }

    private void restoreVisibleProfileDraft(Bundle state) {
        if (state == null || !state.getBoolean(STATE_PROFILE_INPUTS_PRESENT, false)
                || settingsProfileNameInput == null || settingsProfilePackageInput == null
                || settingsProfileRomInput == null || settingsProfileDefaultInput == null) {
            return;
        }
        settingsProfileNameInput.setText(state.getString(STATE_PROFILE_NAME_DRAFT, ""));
        settingsProfilePackageInput.setText(state.getString(STATE_PROFILE_PACKAGE_DRAFT, ""));
        settingsProfileRomInput.setText(state.getString(STATE_PROFILE_ROM_DRAFT, ""));
        settingsProfileDefaultInput.setChecked(
                state.getBoolean(STATE_PROFILE_DEFAULT_DRAFT, false));
    }

    private void putJsonState(Bundle state, String key, WidgetLayout value) {
        if (value == null) {
            return;
        }
        try {
            state.putString(key, value.toJson().toString());
        } catch (JSONException ignored) {
            // Transient state is best-effort; persisted Profile data remains untouched.
        }
    }

    private void putJsonState(Bundle state, String key, TouchpadSettings value) {
        if (value == null) {
            return;
        }
        try {
            state.putString(key, value.toJson().toString());
        } catch (JSONException ignored) {
            // Transient state is best-effort; persisted Profile data remains untouched.
        }
    }

    private void putJsonState(Bundle state, String key, WidgetLayout.Item value) {
        if (value == null) {
            return;
        }
        try {
            state.putString(key, value.toJson().toString());
        } catch (JSONException ignored) {
            // Transient state is best-effort; persisted Profile data remains untouched.
        }
    }

    @Override
    protected void onDestroy() {
        flushGuideReadingPosition();
        for (View view : new ArrayList<>(transientAnimatedViews)) {
            view.animate().cancel();
        }
        transientAnimatedViews.clear();
        closingAnimatedPanels.clear();
        endGamepadRecordingSession(activeGamepadRecordingSession);
        releaseMagnifierViews();
        releaseCanvasViews();
        cancelPendingCanvasImport();
        cancelPendingProfileBundleWork();
        stopMagnifierProjection();
        releaseMapWebView();
        releaseLocalMapBitmap();
        releaseLocalMapThumbnails();
        uiHandler.removeCallbacks(hideFullscreenMapControls);
        uiHandler.removeCallbacks(hideFullscreenGuideControls);
        stopStatusUpdates();
        unregisterReceiver(captureReceiver);
        ThorAccessibilityService.setDiagnosticScanningSuspended(false);
        thorTextInputFocusLease.cancel();
        thorImeSessionInitializer.cancel();
        removeTextInputCompatibilityObservers();
        stabilityDiagnostics.stop();
        thorPerformanceCompatibility.release();
        DebugPerformanceDiagnostics.shutdown();
        super.onDestroy();
    }

    @Override
    protected void onStart() {
        super.onStart();
        activityStarted = true;
        stabilityDiagnostics.start(this);
        getWindow().getDecorView().post(() -> {
            if (activityStarted && !isFinishing() && !isDestroyed()) {
                thorPerformanceCompatibility.apply(this,
                        DebugPerformanceDiagnostics.isCompositionProbe());
            }
        });
        if (DebugPerformanceDiagnostics.isStaticUi()) {
            ThorAccessibilityService.setDiagnosticScanningSuspended(true);
            return;
        }
        ThorAccessibilityService.setDiagnosticScanningSuspended(false);
        startStatusUpdates();
        updateProfileAwarenessRegistration();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateGameFocusProtection(true);
        if (!DebugPerformanceDiagnostics.isStaticUi()) {
            resumeMagnifierViews();
        }
        if (activeMapWebView != null) {
            activeMapWebView.onResume();
        }
    }

    @Override
    protected void onPause() {
        saveGuideReadingPosition();
        if (!magnifierRegionCaptureInProgress) {
            pauseMagnifierViews();
        }
        resetRightStickIfNeeded();
        InputBridge.cancelShizukuTouchpadDrag(this);
        if (activeMapWebView != null) {
            activeMapWebView.onPause();
        }
        super.onPause();
    }

    @Override
    protected void onStop() {
        activityStarted = false;
        stabilityDiagnostics.stop();
        thorPerformanceCompatibility.release();
        profileAwarenessActive = false;
        uiHandler.removeCallbacks(profileAwarenessTicker);
        DebugPerformanceDiagnostics.unregisterRepeatingTask(
                "App-aware upper-window scan");
        ForegroundAppTracker.clearListener(foregroundAppListener);
        stopStatusUpdates();
        super.onStop();
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        stabilityDiagnostics.onTrimMemory(this, level);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        stabilityDiagnostics.onLowMemory(this);
    }

    private void installTextInputCompatibilityObservers() {
        View decor = getWindow().getDecorView();
        if (decor == null || textInputFocusListener != null || imeVisibilityListener != null) {
            return;
        }
        textInputFocusListener = (oldFocus, newFocus) -> {
            boolean focused = newFocus instanceof EditText
                    || thorTextInputFocusLease.isHoldingWindowFocus();
            if (textInputFocused != focused) {
                textInputFocused = focused;
                updatePerformanceCompatibilityTextInputPause();
            }
        };
        imeVisibilityListener = () -> {
            boolean visible = isImeVisible(decor);
            if (imeVisibleForPerformanceCompatibility != visible) {
                boolean wasVisible = imeVisibleForPerformanceCompatibility;
                imeVisibleForPerformanceCompatibility = visible;
                updatePerformanceCompatibilityTextInputPause();
                if (wasVisible && !visible) {
                    releaseTextInputFocusThen(null);
                }
            }
        };
        ViewTreeObserver observer = decor.getViewTreeObserver();
        observer.addOnGlobalFocusChangeListener(textInputFocusListener);
        observer.addOnGlobalLayoutListener(imeVisibilityListener);
    }

    private boolean isImeVisible(View decor) {
        boolean visibleFromInsets = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsets insets = decor.getRootWindowInsets();
            visibleFromInsets = insets != null
                    && insets.isVisible(WindowInsets.Type.ime());
        }

        Rect visibleFrame = new Rect();
        decor.getWindowVisibleDisplayFrame(visibleFrame);
        int rootHeight = decor.getRootView() == null ? 0 : decor.getRootView().getHeight();
        int hiddenHeight = rootHeight <= 0 ? 0 : rootHeight - visibleFrame.bottom;
        boolean visibleFromFrame = rootHeight > 0 && hiddenHeight > rootHeight * 0.18f;
        return visibleFromInsets || visibleFromFrame;
    }

    private void removeTextInputCompatibilityObservers() {
        View decor = getWindow().getDecorView();
        if (decor != null) {
            ViewTreeObserver observer = decor.getViewTreeObserver();
            if (observer.isAlive()) {
                if (textInputFocusListener != null) {
                    observer.removeOnGlobalFocusChangeListener(textInputFocusListener);
                }
                if (imeVisibilityListener != null) {
                    observer.removeOnGlobalLayoutListener(imeVisibilityListener);
                }
            }
        }
        textInputFocusListener = null;
        imeVisibilityListener = null;
        textInputFocused = false;
        imeVisibleForPerformanceCompatibility = false;
    }

    private void updatePerformanceCompatibilityTextInputPause() {
        boolean paused = textInputFocused || imeVisibleForPerformanceCompatibility;
        thorPerformanceCompatibility.setPausedForTextInput(this, paused);
    }

    private void updateGameFocusProtection() {
        updateGameFocusProtection(false);
    }

    private void updateGameFocusProtection(boolean force) {
        if (thorImeSessionInitializer.isHoldingWindowFocus()) {
            thorGameFocusProtection.apply(this, false);
            return;
        }
        boolean protectGameFocus = !isWindowFocusRequired();
        if (force) {
            thorGameFocusProtection.reapply(this, protectGameFocus);
        } else {
            thorGameFocusProtection.apply(this, protectGameFocus);
        }
    }

    private boolean isWindowFocusRequired() {
        return thorTextInputFocusLease.isHoldingWindowFocus();
    }

    private void bindTextInputFocus(EditText input) {
        if (input == null) {
            return;
        }
        input.setOnTouchListener((view, event) -> {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                requestTextInputFocus((EditText) view);
            }
            return false;
        });
    }

    private void bindTapToEditTextInputFocus(EditText input) {
        if (input == null) {
            return;
        }
        final int touchSlop = ViewConfiguration.get(this).getScaledTouchSlop();
        input.setOnTouchListener(new View.OnTouchListener() {
            private float downX;
            private float downY;
            private boolean trackingGesture;
            private boolean dragging;

            @Override
            public boolean onTouch(View view, MotionEvent event) {
                EditText editText = (EditText) view;
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        if (isWindowFocusRequired()) {
                            requestTextInputFocus(editText);
                            trackingGesture = false;
                            return false;
                        }
                        downX = event.getX();
                        downY = event.getY();
                        dragging = false;
                        trackingGesture = true;
                        break;
                    case MotionEvent.ACTION_MOVE:
                        if (trackingGesture && !dragging) {
                            float deltaX = event.getX() - downX;
                            float deltaY = event.getY() - downY;
                            dragging = deltaX * deltaX + deltaY * deltaY
                                    > touchSlop * touchSlop;
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                        if (trackingGesture && !dragging) {
                            requestTextInputFocus(editText);
                        }
                        trackingGesture = false;
                        break;
                    case MotionEvent.ACTION_CANCEL:
                        trackingGesture = false;
                        break;
                    default:
                        break;
                }
                return false;
            }
        });
    }

    private void requestTextInputFocus(EditText input) {
        thorTextInputFocusLease.acquire(this, thorGameFocusProtection, input);
        if (thorTextInputFocusLease.isHoldingWindowFocus() && !textInputFocused) {
            textInputFocused = true;
            updatePerformanceCompatibilityTextInputPause();
        }
    }

    private boolean releaseTextInputFocusThen(Runnable afterRelease) {
        return thorTextInputFocusLease.release(
                this,
                thorGameFocusProtection,
                () -> {
                    textInputFocused = false;
                    updatePerformanceCompatibilityTextInputPause();
                },
                afterRelease);
    }

    private void runAfterTextInputFocusRelease(Runnable action) {
        if (!releaseTextInputFocusThen(action) && action != null) {
            action.run();
        }
    }

    private void initializeLowerDisplayImeSession() {
        if (isFinishing() || isDestroyed() || overlayHost == null
                || isWindowFocusRequired()) {
            return;
        }
        thorImeSessionInitializer.initialize(
                this,
                overlayHost,
                () -> thorGameFocusProtection.apply(this, false),
                initialized -> {
                    if (!isFinishing() && !isDestroyed()) {
                        updateGameFocusProtection();
                    }
                });
    }

    private void updateProfileAwarenessRegistration() {
        uiHandler.removeCallbacks(profileAwarenessTicker);
        DebugPerformanceDiagnostics.unregisterRepeatingTask(
                "App-aware upper-window scan");
        ForegroundAppTracker.clearListener(foregroundAppListener);
        profileAwarenessActive = activityStarted
                && !DebugPerformanceDiagnostics.isStaticUi()
                && ForegroundAppTracker.isEnabled(this);
        if (!profileAwarenessActive) {
            return;
        }
        ForegroundAppTracker.setListener(foregroundAppListener);
        ThorAccessibilityService service = ThorAccessibilityService.getInstance();
        if (service != null) {
            service.refreshForegroundApp();
        }
        maybeAutoSwitchProfile(ForegroundAppTracker.latest());
        DebugPerformanceDiagnostics.registerRepeatingTask(
                "App-aware upper-window scan", 1200L);
        uiHandler.postDelayed(profileAwarenessTicker, 1200L);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        uiHandler.post(() -> updateGameFocusProtection(true));
        if (requestCode == REQUEST_MAGNIFIER_PROJECTION) {
            if (resultCode == RESULT_OK && data != null && pendingMagnifierProjectionItem != null) {
                startApprovedMagnifierProjection(resultCode, data, pendingMagnifierProjectionItem);
            } else {
                clearPendingMagnifierRegionAfterProjection();
                showAction(getString(R.string.action_magnifier_permission_cancelled));
            }
            pendingMagnifierProjectionItem = null;
            return;
        }
        if (requestCode == REQUEST_SCREEN_RECORDING) {
            if (resultCode == RESULT_OK && data != null) {
                startApprovedScreenRecording(resultCode, data);
            } else {
                pendingRecordingProfileName = null;
                showAction(getString(R.string.action_recording_permission_cancelled));
                updateQuickRecordButtons();
            }
            return;
        }
        if (requestCode == REQUEST_EXPORT_PROFILES) {
            if (resultCode == RESULT_OK && data != null && data.getData() != null) {
                writeProfileExport(data.getData());
            } else {
                pendingProfileExportProfiles = null;
            }
            return;
        }
        if (requestCode == REQUEST_DIAGNOSTIC_EXPORT) {
            if (resultCode == RESULT_OK && data != null && data.getData() != null) {
                writeDiagnosticReport(data.getData());
            }
            return;
        }
        if (requestCode == REQUEST_IMPORT_PROFILES) {
            if (resultCode == RESULT_OK && data != null && data.getData() != null) {
                readProfileImport(data.getData());
            }
            return;
        }
        if (requestCode == REQUEST_PROFILE_ICON) {
            if (resultCode == RESULT_OK && data != null && data.getData() != null
                    && pendingProfileIconIndex >= 0 && pendingProfileIconIndex < profiles.size()) {
                Uri uri = data.getData();
                try {
                    getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                } catch (Exception ignored) {
                }
                profiles.get(pendingProfileIconIndex).iconUri = uri.toString();
                ProfileStore.saveProfiles(this, profiles);
                rebuildContent();
                showAction(getString(R.string.action_profile_icon_updated));
            }
            pendingProfileIconIndex = -1;
            return;
        }
        if (requestCode == REQUEST_MACRO_ICON) {
            if (resultCode == RESULT_OK && data != null && data.getData() != null
                    && pendingMacroIconKey != null && pendingMacroIconPreview != null) {
                try {
                    MacroIconRepository.MacroIconOption imported =
                            MacroIconRepository.importUserIcon(this, data.getData());
                    pendingMacroIconKey[0] = imported.key;
                    updateMacroIconPreview(pendingMacroIconPreview, pendingMacroIconMacro,
                            pendingMacroIconKey[0]);
                showAction(getString(R.string.action_macro_icon_imported));
                } catch (Exception ignored) {
                showErrorAction(getString(R.string.error_macro_icon_import));
                }
            }
            pendingMacroIconKey = null;
            pendingMacroIconPreview = null;
            pendingMacroIconMacro = null;
            return;
        }
        if (requestCode == REQUEST_CANVAS_IMAGE) {
            GameProfile targetProfile = pendingCanvasImportProfile;
            WidgetLayout.Item targetItem = pendingCanvasImportItem;
            int targetFrameWidth = pendingCanvasImportFrameWidth;
            int targetFrameHeight = pendingCanvasImportFrameHeight;
            pendingCanvasImportProfile = null;
            pendingCanvasImportItem = null;
            pendingCanvasImportFrameWidth = 1;
            pendingCanvasImportFrameHeight = 1;
            if (resultCode == RESULT_OK && data != null && data.getData() != null
                    && isCanvasTargetValid(targetProfile, targetItem)) {
                importCanvasImage(targetProfile, targetItem, data.getData(),
                        targetFrameWidth, targetFrameHeight);
            }
            return;
        }
        if (requestCode == REQUEST_GUIDE_FILE) {
            if (resultCode == RESULT_OK && data != null && data.getData() != null) {
                Uri uri = data.getData();
                try {
                    getContentResolver().takePersistableUriPermission(uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION);
                } catch (Exception ignored) {
                }
                String displayName = documentDisplayName(uri);
                if (pendingGuideFileUriDraft != null) {
                    pendingGuideFileUriDraft[0] = uri.toString();
                }
                if (pendingGuideFileSummary != null) {
                    pendingGuideFileSummary.setText(nonEmpty(displayName,
                            getString(R.string.guide_default_file)));
                }
                if (pendingGuideFileTitleInput != null
                        && pendingGuideFileTitleState != null
                        && !pendingGuideFileTitleState.userEdited) {
                    String automaticTitle = nonEmpty(stripFileExtension(displayName),
                            getString(R.string.guide_default_file));
                    pendingGuideFileTitleState.applyingAutomaticTitle = true;
                    pendingGuideFileTitleInput.setText(automaticTitle);
                    pendingGuideFileTitleInput.setSelection(
                            pendingGuideFileTitleInput.getText().length());
                    pendingGuideFileTitleState.applyingAutomaticTitle = false;
                }
                showAction(getString(R.string.action_guide_file_selected));
            }
            clearPendingGuideFileSelection();
            return;
        }
        if (requestCode == REQUEST_GUIDE_TEXT_FILE) {
            if (resultCode == RESULT_OK && data != null && data.getData() != null) {
                Uri uri = data.getData();
                try {
                    getContentResolver().takePersistableUriPermission(uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION);
                } catch (Exception ignored) {
                }
                String imported = readTextDocument(uri, 60000);
                if (pendingGuideTextInput != null && imported.length() > 0) {
                    pendingGuideTextInput.setText(imported);
                    pendingGuideTextInput.setSelection(Math.min(
                            pendingGuideTextInput.getText().length(), imported.length()));
                    showAction(getString(R.string.action_note_file_imported));
                } else {
                    showErrorAction(getString(R.string.error_note_file_read));
                }
            }
            pendingGuideTextInput = null;
            return;
        }
        if (requestCode == REQUEST_MAP_FILE && resultCode == RESULT_OK
                && data != null && data.getData() != null) {
            Uri uri = data.getData();
            try {
                getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (Exception ignored) {
            }
            String title = documentDisplayName(uri);
            selectedProfile.safeMaps().add(new MapEntry(
                    nonEmpty(stripFileExtension(title),
                            getString(R.string.map_untitled)), uri.toString()));
            activeLocalMapIndex = selectedProfile.safeMaps().size() - 1;
            selectedProfile.syncLegacyMapFields();
            activeMapViewerMode = MAP_VIEW_LOCAL;
            ProfileStore.saveProfiles(this, profiles);
                showAction(getString(R.string.action_map_added));
            if (activeScreen == SCREEN_MAP) {
                rebuildContent();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQUEST_RECORD_AUDIO) {
            return;
        }
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            launchScreenRecordingConsent();
        } else {
            pendingRecordingProfileName = null;
            showErrorAction(getString(R.string.permission_record_audio_required));
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        thorTextInputFocusLease.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            enterImmersiveMode();
            if (!DebugPerformanceDiagnostics.isStaticUi()) {
                updateBridgeStatus();
                updateSystemStatus();
                if (activeScreen == SCREEN_SETTINGS
                        && activeSettingsSection == SETTINGS_INPUT
                        && settingsContentContainer != null) {
                    uiHandler.post(this::refreshSettingsContent);
                }
            }
        } else {
            resetRightStickIfNeeded();
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (DebugPerformanceDiagnostics.isStaticUi()) {
            return true;
        }
        return super.dispatchTouchEvent(event);
    }

    @Override
    public void onBackPressed() {
        if (dismissTopPanelOverlay()) {
            return;
        }
        if (guideReaderFullscreen) {
            closeGuideFullscreen();
            return;
        }
        if (mapViewerFullscreen) {
            closeMapFullscreen();
            return;
        }
        if (activeScreen == SCREEN_MAP && activeMapViewerMode == MAP_VIEW_INTERACTIVE
                && activeMapWebView != null && activeMapWebView.canGoBack()) {
            activeMapWebView.goBack();
            return;
        }
        if (activeScreen == SCREEN_SETTINGS) {
            closeSettingsPanel();
            return;
        }
        if (activeScreen == SCREEN_GUIDE
                && (viewingGuideInline != null || editingGuideTypeInline != null)) {
            if (viewingGuideInline != null) {
                closeGuideReader();
                return;
            }
            editingGuideInline = null;
            editingGuideTypeInline = null;
            showGuidePanel();
            return;
        }
        if (activeScreen != SCREEN_MAIN) {
            activeScreen = SCREEN_MAIN;
            rebuildContent();
            return;
        }
        super.onBackPressed();
    }

    private void resetRightStickIfNeeded() {
        if (touchPadView != null) {
            String mode = TouchpadSettings.normalizeMode(touchpadSettings.mode);
            if (!TouchpadSettings.MODE_RIGHT_STICK.equals(mode)
                    && !TouchpadSettings.MODE_RELATIVE_MOUSE.equals(mode)) {
                return;
            }
            touchPadView.recenterFromActivity();
        }
    }

    private void registerCaptureReceiver() {
        IntentFilter filter = new IntentFilter(CoordinateCaptureActivity.ACTION_CAPTURED);
        filter.addAction(CoordinateCaptureActivity.ACTION_REGION_CAPTURED);
        filter.addAction(CoordinateCaptureActivity.ACTION_REGION_PREVIEW);
        filter.addAction(CoordinateCaptureActivity.ACTION_REGION_CANCELLED);
        registerReceiver(captureReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
    }

    private void startStatusUpdates() {
        updateSystemStatus();
        if (DebugPerformanceDiagnostics.isStaticUi() || statusReceiverRegistered) {
            return;
        }
        IntentFilter filter = new IntentFilter(Intent.ACTION_TIME_TICK);
        filter.addAction(Intent.ACTION_TIME_CHANGED);
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(statusReceiver, filter);
        statusReceiverRegistered = true;
    }

    private void stopStatusUpdates() {
        if (!statusReceiverRegistered) {
            return;
        }
        unregisterReceiver(statusReceiver);
        statusReceiverRegistered = false;
    }

    private void updateSystemStatus() {
        if (systemTimeText == null || systemBatteryText == null || systemNetworkIcon == null || systemBatteryIcon == null) {
            return;
        }
        String time = new SimpleDateFormat("h:mm a", Locale.US).format(new Date());
        if (!time.equals(lastSystemTime)) {
            systemTimeText.setText(time);
            lastSystemTime = time;
        }
        String network = networkLabel();
        if (!network.equals(lastNetworkLabel)) {
            systemNetworkIcon.setImageResource(networkIconForLabel(network));
            systemNetworkIcon.setAlpha("No net".equals(network) ? 0.55f : 1f);
            lastNetworkLabel = network;
        }
        BatterySnapshot battery = batterySnapshot();
        systemBatteryIcon.setBattery(battery.percent, battery.charging);
        String batteryLabel = battery.label();
        if (!batteryLabel.equals(lastBatteryLabel)) {
            systemBatteryText.setText(batteryLabel);
            lastBatteryLabel = batteryLabel;
        }
    }

    private String batteryLabel() {
        return batterySnapshot().label();
    }

    private BatterySnapshot batterySnapshot() {
        Intent intent = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (intent == null) {
            return new BatterySnapshot(-1, false);
        }
        int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        if (level < 0 || scale <= 0) {
            return new BatterySnapshot(-1, false);
        }
        int percent = Math.round(level * 100f / scale);
        int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        boolean charging = status == BatteryManager.BATTERY_STATUS_CHARGING
                || status == BatteryManager.BATTERY_STATUS_FULL;
        return new BatterySnapshot(percent, charging);
    }

    private int networkIconForLabel(String label) {
        if ("Wi-Fi".equals(label)) {
            return R.drawable.ic_status_wifi;
        }
        return R.drawable.ic_status_wifi_off;
    }

    private int batteryIconFor(int percent, boolean charging) {
        if (charging) {
            return R.drawable.ic_status_battery_charging;
        }
        if (percent < 0) {
            return R.drawable.ic_status_battery_25;
        }
        if (percent <= 30) {
            return R.drawable.ic_status_battery_25;
        }
        if (percent <= 60) {
            return R.drawable.ic_status_battery_50;
        }
        if (percent <= 85) {
            return R.drawable.ic_status_battery_75;
        }
        return R.drawable.ic_status_battery;
    }

    private String networkLabel() {
        ConnectivityManager manager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        if (manager == null) {
            return "No net";
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            NetworkCapabilities capabilities = manager.getNetworkCapabilities(manager.getActiveNetwork());
            if (capabilities == null) {
                return "No net";
            }
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                return "Wi-Fi";
            }
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                return "Cell";
            }
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                return "LAN";
            }
            return "Net";
        }
        return manager.getActiveNetworkInfo() != null && manager.getActiveNetworkInfo().isConnected()
                ? "Net" : "No net";
    }

    private static final class BatterySnapshot {
        final int percent;
        final boolean charging;

        BatterySnapshot(int percent, boolean charging) {
            this.percent = percent;
            this.charging = charging;
        }

        String label() {
            if (percent < 0) {
                return "--%";
            }
            return percent + "%" + (charging ? "+" : "");
        }
    }

    private void enterImmersiveMode() {
        Window window = getWindow();
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        int systemBackground = HeimdallUi.background(this);
        window.setStatusBarColor(systemBackground);
        window.setNavigationBarColor(systemBackground);
        window.getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }

    private View createLayout() {
        FrameLayout host = new FrameLayout(this);
        overlayHost = host;
        contentHost = null;
        currentContentPage = null;
        dockNavBar = null;
        bottomDock = null;
        dockNavButtons.clear();
        applySystemGestureExclusion(host);
        if (mapViewerFullscreen && activeScreen == SCREEN_MAP) {
            host.addView(createFullscreenMapLayout(), new FrameLayout.LayoutParams(-1, -1));
            return host;
        }
        if (guideReaderFullscreen && activeScreen == SCREEN_GUIDE
                && isInlineTextGuide(viewingGuideInline)) {
            host.addView(createFullscreenGuideLayout(), new FrameLayout.LayoutParams(-1, -1));
            return host;
        }
        LinearLayout root = new TrackedLinearLayout("Root background");
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(HeimdallUi.background(this));
        root.setPadding(dp(6), dp(6), dp(6), dp(6));
        applySystemGestureExclusion(root);

        LinearLayout header = new TrackedLinearLayout("Header");
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setOrientation(LinearLayout.HORIZONTAL);
        HeimdallUi.applyHeaderPanel(this, header);
        LinearLayout.LayoutParams headerParams = new LinearLayout.LayoutParams(-1, dp(HeimdallUi.HEIGHT_HEADER));
        headerParams.setMargins(0, 0, 0, dp(6));
        root.addView(header, headerParams);

        LinearLayout profileContext = new LinearLayout(this);
        profileContext.setOrientation(LinearLayout.HORIZONTAL);
        profileContext.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
        header.addView(profileContext, new LinearLayout.LayoutParams(0, -1, 1));

        profileIconView = new ProfileIconView(this);
        profileIconView.setElevation(HeimdallUi.isPearl(this) ? 0f : dp(3));
        profileIconView.setOnClickListener(v -> showProfileQuickPicker());
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(36), dp(36));
        iconParams.setMargins(0, 0, dp(10), 0);
        profileContext.addView(profileIconView, iconParams);

        profileTitle = text("", 15, PRIMARY, true);
        profileTitle.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
        profileTitle.setSingleLine(true);
        profileTitle.setEllipsize(android.text.TextUtils.TruncateAt.END);
        profileTitle.setOnClickListener(v -> showProfileQuickPicker());
        profileContext.addView(profileTitle, new LinearLayout.LayoutParams(0, -1, 1));

        ImageView headerBrandMark = new ImageView(this);
        headerBrandMark.setImageResource(HeimdallUi.isPearl(this)
                ? R.drawable.ic_heimdall_header_mark_freya
                : R.drawable.ic_heimdall_header_mark_blue);
        headerBrandMark.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        headerBrandMark.setClickable(false);
        headerBrandMark.setFocusable(false);
        headerBrandMark.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        LinearLayout.LayoutParams brandMarkParams = new LinearLayout.LayoutParams(dp(20), dp(20));
        brandMarkParams.setMargins(dp(8), 0, dp(8), 0);
        header.addView(headerBrandMark, brandMarkParams);

        LinearLayout systemStatus = new LinearLayout(this);
        systemStatus.setOrientation(LinearLayout.HORIZONTAL);
        systemStatus.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);
        header.addView(systemStatus, new LinearLayout.LayoutParams(0, -1, 1));

        systemTimeText = text("", 13, TEXT, true);
        systemTimeText.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);
        systemStatus.addView(systemTimeText, new LinearLayout.LayoutParams(-2, -1));

        systemNetworkIcon = new ImageView(this);
        systemNetworkIcon.setImageResource(R.drawable.ic_status_wifi);
        systemNetworkIcon.setColorFilter(HeimdallUi.textColor(this));
        LinearLayout.LayoutParams networkParams = new LinearLayout.LayoutParams(dp(18), dp(18));
        networkParams.setMargins(dp(10), 0, dp(6), 0);
        systemStatus.addView(systemNetworkIcon, networkParams);

        systemBatteryIcon = new BatteryStatusView(this);
        LinearLayout.LayoutParams batteryIconParams = new LinearLayout.LayoutParams(dp(20), dp(20));
        batteryIconParams.setMargins(dp(2), 0, dp(4), 0);
        systemStatus.addView(systemBatteryIcon, batteryIconParams);

        systemBatteryText = text("", 13, TEXT, true);
        systemBatteryText.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);
        systemStatus.addView(systemBatteryText, new LinearLayout.LayoutParams(-2, -1));

        contentHost = new FrameLayout(this);
        currentContentPage = createContentPage(activeScreen);
        LinearLayout.LayoutParams contentParams = new LinearLayout.LayoutParams(-1, 0, 1);
        contentParams.setMargins(0, 0, 0, dp(6));
        contentHost.addView(currentContentPage, new FrameLayout.LayoutParams(-1, -1));
        root.addView(contentHost, contentParams);

        bottomDock = new TrackedLinearLayout("Bottom Dock");
        bottomDock.setOrientation(LinearLayout.HORIZONTAL);
        HeimdallUi.applyBottomDockPanel(this, bottomDock);
        root.addView(bottomDock, new LinearLayout.LayoutParams(-1, dp(HeimdallUi.HEIGHT_DOCK)));

        dockNavBar = new DockNavBar(this, dockIndexForScreen(activeScreen));
        bottomDock.addView(dockNavBar, new LinearLayout.LayoutParams(0, -1, 3));

        LinearLayout bottomBar = new LinearLayout(this);
        bottomBar.setOrientation(LinearLayout.HORIZONTAL);
        dockNavBar.addView(bottomBar, new FrameLayout.LayoutParams(-1, -1));

        addDockItem(bottomBar, navIconButton(getString(R.string.nav_home),
                R.drawable.ic_overview, SCREEN_MAIN,
                () -> switchPlayScreen(SCREEN_MAIN)));
        addDockItem(bottomBar, navIconButton(getString(R.string.nav_map),
                R.drawable.ic_map, SCREEN_MAP, this::showMapPanel));
        addDockItem(bottomBar, navIconButton(getString(R.string.nav_guide),
                R.drawable.ic_guide, SCREEN_GUIDE, this::showGuidePanel));

        FrameLayout settingsSlot = new FrameLayout(this);
        bottomDock.addView(settingsSlot, new LinearLayout.LayoutParams(0, -1, 1));
        Button settingsButton = navIconButton("", R.drawable.ic_settings, SCREEN_SETTINGS,
                this::showSettingsPanel);
        settingsButton.setContentDescription(getString(R.string.nav_settings));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            settingsButton.setTooltipText(getString(R.string.nav_settings));
        }
        settingsSlot.addView(settingsButton, new FrameLayout.LayoutParams(-1, -1));

        View settingsDivider = new View(this);
        settingsDivider.setBackgroundColor(HeimdallUi.isPearl(this)
                ? 0x40657386
                : 0x405F7C9A);
        FrameLayout.LayoutParams dividerParams = new FrameLayout.LayoutParams(dp(1), -1);
        dividerParams.gravity = Gravity.LEFT;
        dividerParams.setMargins(0, dp(10), 0, dp(10));
        settingsSlot.addView(settingsDivider, dividerParams);
        updateDockNavSelection(false);
        if (activeScreen == SCREEN_SETTINGS) {
            bottomDock.setVisibility(View.GONE);
        }

        applyFlatUiPolicy(root);
        host.addView(root, new FrameLayout.LayoutParams(-1, -1));
        return host;
    }

    private void addDockItem(LinearLayout bottomBar, Button button) {
        bottomBar.addView(button, new LinearLayout.LayoutParams(0, -1, 1));
    }

    private View createContentPage(int screen) {
        FrameLayout page = new FrameLayout(this);
        page.setTag(Integer.valueOf(screen));
        page.addView(createMainContent(), new FrameLayout.LayoutParams(-1, -1));
        return page;
    }

    private View createMainContent() {
        releaseMagnifierViews();
        releaseCanvasViews();
        if (activeScreen == SCREEN_SETTINGS) {
            macroGrids.clear();
            touchPadView = null;
            statusText = null;
            return createSettingsHomePanel();
        }
        if (activeScreen == SCREEN_MAP) {
            macroGrids.clear();
            touchPadView = null;
            statusText = null;
            return createMapPage();
        }
        if (activeScreen == SCREEN_GUIDE) {
            macroGrids.clear();
            touchPadView = null;
            statusText = null;
            return createGuidePage();
        }
        WidgetHostLayout widgetGrid = new WidgetHostLayout(this, currentWidgetLayout());
        addWidgetsToGrid(widgetGrid, currentWidgetLayout());
        return widgetGrid;
    }

    private WidgetLayout currentWidgetLayout() {
        if (draftWidgetLayout != null) {
            draftWidgetLayout.sanitize();
            return draftWidgetLayout;
        }
        return selectedProfile.safeWidgetLayout();
    }

    private WidgetLayout editableWidgetLayout() {
        if (draftWidgetLayout == null) {
            draftWidgetLayout = selectedProfile.safeWidgetLayout().copy();
        }
        draftWidgetLayout.sanitize();
        return draftWidgetLayout;
    }

    private void addWidgetsToGrid(WidgetHostLayout grid, WidgetLayout layout) {
        macroGrids.clear();
        quickScreenshotButtons.clear();
        quickRecordButtons.clear();
        touchPadView = null;
        statusText = null;
        for (WidgetLayout.Item item : layout.items) {
            View widget = createWidget(item);
            if (widget == null) {
                continue;
            }
            grid.addWidget(widget, item);
        }
        if (magnifierViews.isEmpty() && UpperScreenProjectionService.isRunning()) {
            stopMagnifierProjection();
        }
    }

    private View createWidget(WidgetLayout.Item item) {
        String type = item.type;
        if (WidgetLayout.TYPE_MACRO_GROUP.equals(type)) {
            return createMacroWidget(item);
        }
        if (WidgetLayout.TYPE_TOUCHPAD.equals(type)) {
            touchPadView = new TouchPadView(this);
            return touchPadView;
        }
        if (WidgetLayout.TYPE_STATUS.equals(type)) {
            return createStatusWidget();
        }
        if (WidgetLayout.TYPE_CANVAS.equals(type)) {
            CanvasWidgetView view = new CanvasWidgetView(this, item,
                    !hasUnsavedWidgetLayout(), new CanvasWidgetView.Listener() {
                        @Override
                        public void onChooseImage(WidgetLayout.Item requestedItem,
                                int frameWidth, int frameHeight) {
                            chooseCanvasImage(requestedItem, frameWidth, frameHeight);
                        }

                        @Override
                        public void onFullscreen(WidgetLayout.Item requestedItem,
                                int frameWidth, int frameHeight) {
                            showCanvasFullscreen(requestedItem, frameWidth, frameHeight);
                        }

                        @Override
                        public void onOptions(WidgetLayout.Item requestedItem,
                                int frameWidth, int frameHeight) {
                            showCanvasOptions(requestedItem, frameWidth, frameHeight);
                        }

                        @Override
                        public void onDraftInteractionBlocked() {
                            showErrorAction(getString(R.string.canvas_save_layout_first));
                        }
                    });
            canvasViews.add(view);
            return view;
        }
        if (WidgetLayout.TYPE_QUICK_ACTIONS.equals(type)) {
            return createQuickActionsWidget();
        }
        if (WidgetLayout.TYPE_MAGNIFIER.equals(type)) {
            UpperScreenMagnifierView view = new UpperScreenMagnifierView(this, item,
                    new UpperScreenMagnifierView.ActionListener() {
                        @Override
                        public void onProjectionRequested(WidgetLayout.Item requestedItem) {
                            requestMagnifierProjection(requestedItem);
                        }

                        @Override
                        public void onRegionRequested(WidgetLayout.Item requestedItem,
                                float targetAspectRatio) {
                            startMagnifierRegionCapture(requestedItem, targetAspectRatio);
                        }

                        @Override
                        public void onStopRequested() {
                            if (!UpperScreenProjectionService.isActiveOrStarting()) {
                                return;
                            }
                            stopMagnifierProjection();
            showAction(getString(R.string.action_magnifier_stopped));
                        }
                    });
            magnifierViews.add(view);
            if (profileAwarenessActive) {
                view.resume();
            }
            return view;
        }
        return null;
    }

    private View createMacroWidget(WidgetLayout.Item item) {
        LinearLayout macroGrid;
        macroGrid = new TrackedLinearLayout("Macro module");
        macroGrid.setOrientation(LinearLayout.VERTICAL);
        macroGrids.add(new MacroGridBinding(macroGrid, item));
        return macroGrid;
    }

    private View createStatusWidget() {
        statusText = new TrackedTextView("Status module");
        statusText.setTextSize(HeimdallUi.TYPE_HELP);
        statusText.setTextColor(HeimdallUi.mutedTextColor(this));
        statusText.setGravity(Gravity.CENTER_VERTICAL);
        statusText.setPadding(dp(HeimdallUi.SPACE_3), dp(6), dp(HeimdallUi.SPACE_3), dp(6));
        statusText.setMaxLines(3);
        HeimdallUi.applyInfoPill(this, statusText);
        return statusText;
    }

    private View createQuickActionsWidget() {
        LinearLayout root = new TrackedLinearLayout("Quick Actions");
        root.setOrientation(LinearLayout.VERTICAL);
        HeimdallUi.applyQuickActionPanel(this, root);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        root.addView(actions, new LinearLayout.LayoutParams(-1, 0, 1));
        QuickActionButtonView screenshot = addQuickActionButton(actions,
                R.drawable.ic_camera, getString(R.string.quick_action_screenshot),
                QuickActionButtonView.STATE_IDLE, this::captureUpperScreen);
        quickScreenshotButtons.add(screenshot);
        View actionDivider = new View(this);
        actionDivider.setBackgroundColor(HeimdallUi.quickActionDivider(this));
        LinearLayout.LayoutParams actionDividerParams =
                new LinearLayout.LayoutParams(dp(1), -1);
        actionDividerParams.setMargins(0, dp(7), 0, dp(7));
        actions.addView(actionDivider, actionDividerParams);
        boolean projectionBusy = ScreenRecordingService.isRecording()
                || UpperScreenProjectionService.isActiveOrStarting();
        QuickActionButtonView record = addQuickActionButton(actions,
                projectionBusy ? R.drawable.ic_stop : R.drawable.ic_video,
                quickRecordActionDescription(), quickRecordVisualState(),
                this::toggleUpperScreenRecording);
        quickRecordButtons.add(record);

        View volumeDivider = new View(this);
        volumeDivider.setBackgroundColor(HeimdallUi.quickActionDivider(this));
        LinearLayout.LayoutParams volumeDividerParams =
                new LinearLayout.LayoutParams(-1, dp(1));
        volumeDividerParams.setMargins(dp(7), 0, dp(7), 0);
        root.addView(volumeDivider, volumeDividerParams);

        LinearLayout volumeRow = new LinearLayout(this);
        volumeRow.setOrientation(LinearLayout.HORIZONTAL);
        volumeRow.setGravity(Gravity.CENTER_VERTICAL);
        volumeRow.setPadding(dp(9), 0, dp(9), 0);
        LinearLayout.LayoutParams volumeParams = new LinearLayout.LayoutParams(-1, dp(38));
        root.addView(volumeRow, volumeParams);

        ImageView volumeIcon = new ImageView(this);
        volumeIcon.setImageResource(R.drawable.ic_volume_up);
        volumeIcon.setColorFilter(HeimdallUi.isPearl(this) ? 0xFF536274 : 0xFFBFD0E2);
        volumeIcon.setContentDescription(getString(R.string.quick_action_media_volume));
        volumeRow.addView(volumeIcon, new LinearLayout.LayoutParams(dp(24), dp(24)));

        AudioManager audio = (AudioManager) getSystemService(AUDIO_SERVICE);
        QuickVolumeSeekBar volume = new QuickVolumeSeekBar(this);
        int max = audio == null ? 15 : Math.max(1, audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
        int current = audio == null ? 0 : audio.getStreamVolume(AudioManager.STREAM_MUSIC);
        volume.setMax(max);
        volume.setProgress(current);
        volumeIcon.setAlpha(0.55f + 0.45f * current / (float) max);
        volume.setContentDescription(getString(R.string.quick_action_adjust_media_volume));
        volume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && audio != null) {
                    audio.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0);
                }
                volumeIcon.setAlpha(0.55f + 0.45f * progress / (float) max);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        LinearLayout.LayoutParams sliderParams = new LinearLayout.LayoutParams(0, dp(38), 1);
        sliderParams.setMargins(dp(7), 0, 0, 0);
        volumeRow.addView(volume, sliderParams);
        return root;
    }

    private void captureUpperScreen() {
        resetQuickScreenshotFeedback();
        updateTargetDisplayInfo();
        final String profileName = selectedProfile == null ? "Profile" : selectedProfile.name;
        ThorAccessibilityService.captureDisplay(this, targetDisplayId,
                new ThorAccessibilityService.ScreenshotCallback() {
                    @Override
                    public void onCaptured(Bitmap bitmap) {
                        new Thread(() -> saveScreenshot(bitmap, profileName),
                                "heimdall-screenshot-save").start();
                    }

                    @Override
                    public void onError(String message) {
                        runOnUiThread(() -> {
                            showQuickScreenshotResult(false);
                            showErrorAction(message);
                        });
                    }
                });
    }

    private void saveScreenshot(Bitmap bitmap, String profileName) {
        Uri uri = null;
        boolean saved = false;
        try {
            uri = CaptureStorage.createImage(getContentResolver(), profileName);
            if (uri != null) {
                try (OutputStream output = getContentResolver().openOutputStream(uri, "w")) {
                    saved = output != null && bitmap.compress(Bitmap.CompressFormat.PNG, 100, output);
                }
            }
            if (saved) {
                CaptureStorage.publish(getContentResolver(), uri);
            } else {
                CaptureStorage.discard(getContentResolver(), uri);
            }
        } catch (Throwable ignored) {
            CaptureStorage.discard(getContentResolver(), uri);
        } finally {
            bitmap.recycle();
        }
        final boolean result = saved;
        runOnUiThread(() -> {
            showQuickScreenshotResult(result);
            if (result) {
                showAction(getString(R.string.screenshot_saved));
            } else {
                showErrorAction(getString(R.string.screenshot_save_failed));
            }
        });
    }

    private void resetQuickScreenshotFeedback() {
        for (QuickActionButtonView button : quickScreenshotButtons) {
            button.clearTransientResult();
        }
    }

    private void showQuickScreenshotResult(boolean success) {
        for (QuickActionButtonView button : quickScreenshotButtons) {
            button.showTransientResult(success);
        }
    }

    private void toggleUpperScreenRecording() {
        if (ScreenRecordingService.isRecording()) {
            Intent stop = new Intent(this, ScreenRecordingService.class);
            stop.setAction(ScreenRecordingService.ACTION_STOP);
            startService(stop);
            uiHandler.postDelayed(() -> {
                updateQuickRecordButtons();
                String message = nonEmpty(ScreenRecordingService.lastStatus(),
                        getString(R.string.recording_stopped));
                if (ScreenRecordingService.lastStatusIsError()) {
                    showErrorAction(message);
                } else {
                    showAction(message);
                }
            }, 500);
            return;
        }
        if (UpperScreenProjectionService.isActiveOrStarting()) {
            stopMagnifierProjection();
            pauseMagnifierViews();
            return;
        }
        updateTargetDisplayInfo();
        if (targetDisplayId != Display.DEFAULT_DISPLAY) {
            showErrorAction(getString(R.string.error_projection_route_unconfirmed));
            return;
        }
        pendingRecordingProfileName = selectedProfile == null ? "Profile" : selectedProfile.name;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                && checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO);
            return;
        }
        launchScreenRecordingConsent();
    }

    private void launchScreenRecordingConsent() {
        MediaProjectionManager manager =
                (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        if (manager == null) {
            showErrorAction(getString(R.string.error_recording_unsupported));
            pendingRecordingProfileName = null;
            return;
        }
        Intent consent;
        if (Build.VERSION.SDK_INT >= 34) {
            consent = manager.createScreenCaptureIntent(
                    MediaProjectionConfig.createConfigForDefaultDisplay());
        } else {
            consent = manager.createScreenCaptureIntent();
        }
        startActivityForResult(consent, REQUEST_SCREEN_RECORDING);
    }

    private void startApprovedScreenRecording(int resultCode, Intent resultData) {
        // The consent UI may change display focus. Resolve the default upper display again
        // so the encoder surface always matches the display MediaProjection will capture.
        updateTargetDisplayInfo();
        Intent start = new Intent(this, ScreenRecordingService.class);
        start.setAction(ScreenRecordingService.ACTION_START);
        start.putExtra(ScreenRecordingService.EXTRA_RESULT_CODE, resultCode);
        start.putExtra(ScreenRecordingService.EXTRA_RESULT_DATA, resultData);
        start.putExtra(ScreenRecordingService.EXTRA_PROFILE_NAME,
                nonEmpty(pendingRecordingProfileName, "Profile"));
        start.putExtra(ScreenRecordingService.EXTRA_WIDTH, targetDisplayWidth);
        start.putExtra(ScreenRecordingService.EXTRA_HEIGHT, targetDisplayHeight);
        start.putExtra(ScreenRecordingService.EXTRA_DENSITY, targetDisplayDensityDpi);
        pendingRecordingProfileName = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(start);
        } else {
            startService(start);
        }
        uiHandler.postDelayed(() -> {
            updateQuickRecordButtons();
            showAction(nonEmpty(ScreenRecordingService.lastStatus(),
                    ScreenRecordingService.isRecording()
                            ? getString(R.string.recording_upper_screen)
                            : getString(R.string.recording_start_failed)));
        }, 700);
    }

    private void updateQuickRecordButtons() {
        boolean recording = ScreenRecordingService.isRecording();
        boolean magnifierActive = UpperScreenProjectionService.isActiveOrStarting();
        boolean projectionBusy = recording || magnifierActive;
        for (QuickActionButtonView button : quickRecordButtons) {
            button.setActionIcon(projectionBusy ? R.drawable.ic_stop : R.drawable.ic_video);
            button.setVisualState(quickRecordVisualState());
            button.setContentDescription(quickRecordActionDescription());
        }
    }

    private String quickRecordActionDescription() {
        if (ScreenRecordingService.isRecording()) {
            return getString(R.string.recording_stop_upper_screen);
        }
        if (UpperScreenProjectionService.isActiveOrStarting()) {
            return getString(R.string.magnifier_stop_upper_screen);
        }
        return getString(R.string.recording_start_upper_screen);
    }

    private int quickRecordVisualState() {
        if (ScreenRecordingService.isRecording()) {
            return QuickActionButtonView.STATE_RECORDING_STOP;
        }
        if (UpperScreenProjectionService.isActiveOrStarting()) {
            return QuickActionButtonView.STATE_MAGNIFIER_STOP;
        }
        return QuickActionButtonView.STATE_IDLE;
    }

    private void requestMagnifierProjection(WidgetLayout.Item item) {
        if (ScreenRecordingService.isRecording()) {
            clearPendingMagnifierRegionAfterProjection();
            showErrorAction(getString(R.string.error_recording_magnifier_conflict));
            return;
        }
        updateTargetDisplayInfo();
        if (targetDisplayId != Display.DEFAULT_DISPLAY) {
            clearPendingMagnifierRegionAfterProjection();
            showErrorAction(getString(R.string.error_magnifier_default_display_only));
            return;
        }
        UpperScreenProjectionService.setRegion(item.magnifierLeft, item.magnifierTop,
                item.magnifierRight, item.magnifierBottom);
        UpperScreenProjectionService.setTuning(
                WidgetLayout.magnifierTargetAspectRatio(item),
                item.magnifierFps, item.magnifierZoom);
        if (UpperScreenProjectionService.isActiveOrStarting()) {
            resumeMagnifierViews();
            schedulePendingMagnifierRegionAfterProjection();
            return;
        }
        MediaProjectionManager manager =
                (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        if (manager == null) {
            clearPendingMagnifierRegionAfterProjection();
            showErrorAction(getString(R.string.error_magnifier_unsupported));
            return;
        }
        pendingMagnifierProjectionItem = item;
        Intent consent;
        if (Build.VERSION.SDK_INT >= 34) {
            consent = manager.createScreenCaptureIntent(
                    MediaProjectionConfig.createConfigForDefaultDisplay());
        } else {
            consent = manager.createScreenCaptureIntent();
        }
        startActivityForResult(consent, REQUEST_MAGNIFIER_PROJECTION);
    }

    private void startApprovedMagnifierProjection(int resultCode, Intent resultData,
            WidgetLayout.Item item) {
        updateTargetDisplayInfo();
        UpperScreenProjectionService.setRegion(item.magnifierLeft, item.magnifierTop,
                item.magnifierRight, item.magnifierBottom);
        UpperScreenProjectionService.setTuning(
                WidgetLayout.magnifierTargetAspectRatio(item),
                item.magnifierFps, item.magnifierZoom);
        Intent start = new Intent(this, UpperScreenProjectionService.class);
        start.setAction(UpperScreenProjectionService.ACTION_START);
        start.putExtra(UpperScreenProjectionService.EXTRA_RESULT_CODE, resultCode);
        start.putExtra(UpperScreenProjectionService.EXTRA_RESULT_DATA, resultData);
        start.putExtra(UpperScreenProjectionService.EXTRA_WIDTH, targetDisplayWidth);
        start.putExtra(UpperScreenProjectionService.EXTRA_HEIGHT, targetDisplayHeight);
        start.putExtra(UpperScreenProjectionService.EXTRA_DENSITY, targetDisplayDensityDpi);
        UpperScreenProjectionService.markStarting();
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(start);
            } else {
                startService(start);
            }
        } catch (Throwable ignored) {
            UpperScreenProjectionService.cancelStarting();
            clearPendingMagnifierRegionAfterProjection();
            showErrorAction(getString(R.string.error_magnifier_start));
            return;
        }
        updateQuickRecordButtons();
        uiHandler.postDelayed(() -> {
            resumeMagnifierViews();
            updateQuickRecordButtons();
            continuePendingMagnifierRegionAfterProjection();
        }, 500L);
    }

    private void stopMagnifierProjection() {
        if (!UpperScreenProjectionService.isActiveOrStarting()) {
            return;
        }
        Intent stop = new Intent(this, UpperScreenProjectionService.class);
        stop.setAction(UpperScreenProjectionService.ACTION_STOP);
        startService(stop);
        uiHandler.postDelayed(this::updateQuickRecordButtons, 350L);
    }

    private void startMagnifierRegionCapture(WidgetLayout.Item item, float targetAspectRatio) {
        if (!UpperScreenProjectionService.isRunning()) {
            pendingMagnifierRegionAfterProjectionItem = item;
            pendingMagnifierRegionAfterProjectionAspectRatio =
                    Math.max(0.2f, Math.min(5f, targetAspectRatio));
            pendingMagnifierRegionStartAttempts = 0;
            requestMagnifierProjection(item);
            return;
        }
        pendingMagnifierRegionItem = item;
        pendingMagnifierRegionProfile = selectedProfile;
        pendingMagnifierRegionUsesDraft = draftWidgetLayout != null
                && draftWidgetLayout.items.contains(item);
        magnifierWasFrozenBeforeRegionCapture = UpperScreenProjectionService.isFrozen();
        magnifierRegionCaptureInProgress = true;
        captureInProgress = true;
        Intent intent = CoordinateCaptureActivity.createIntent(
                this, CoordinateCaptureActivity.MODE_REGION);
        intent.putExtra(CoordinateCaptureActivity.EXTRA_TARGET_ASPECT,
                Math.max(0.2f, Math.min(5f, targetAspectRatio)));
        intent.putExtra(CoordinateCaptureActivity.EXTRA_REGION_SHAPE,
                WidgetLayout.normalizeMagnifierShape(item.magnifierShape));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK
                | Intent.FLAG_ACTIVITY_NO_ANIMATION);
        Display targetDisplay = findCaptureDisplay();
        if (targetDisplay == null) {
            magnifierRegionCaptureInProgress = false;
            captureInProgress = false;
            pendingMagnifierRegionItem = null;
            pendingMagnifierRegionProfile = null;
            pendingMagnifierRegionUsesDraft = false;
            magnifierWasFrozenBeforeRegionCapture = false;
            showErrorAction(getString(R.string.error_upper_screen_not_found));
            return;
        }
        ActivityOptions options = ActivityOptions.makeBasic();
        options.setLaunchDisplayId(targetDisplay.getDisplayId());
        UpperScreenProjectionService.setTuning(
                Math.max(0.2f, Math.min(5f, targetAspectRatio)), item.magnifierFps, 1f);
        UpperScreenProjectionService.setFrozen(true);
        try {
            startActivity(intent, options.toBundle());
        } catch (Throwable t) {
            UpperScreenProjectionService.setFrozen(magnifierWasFrozenBeforeRegionCapture);
            magnifierWasFrozenBeforeRegionCapture = false;
            magnifierRegionCaptureInProgress = false;
            captureInProgress = false;
            pendingMagnifierRegionItem = null;
            pendingMagnifierRegionProfile = null;
            pendingMagnifierRegionUsesDraft = false;
            String reason = t.getClass().getSimpleName();
            String detail = t.getMessage();
            if (detail != null && !detail.trim().isEmpty()) {
                detail = detail.trim();
                reason += ": " + detail.substring(0, Math.min(80, detail.length()));
            }
            showErrorAction(getString(R.string.error_upper_screen_region_start, reason));
        }
    }

    private void schedulePendingMagnifierRegionAfterProjection() {
        if (pendingMagnifierRegionAfterProjectionItem == null) {
            return;
        }
        uiHandler.postDelayed(this::continuePendingMagnifierRegionAfterProjection,
                MAGNIFIER_REGION_START_RETRY_MS);
    }

    private void continuePendingMagnifierRegionAfterProjection() {
        WidgetLayout.Item item = pendingMagnifierRegionAfterProjectionItem;
        if (item == null) {
            return;
        }
        if (!UpperScreenProjectionService.isRunning()) {
            pendingMagnifierRegionStartAttempts++;
            if (pendingMagnifierRegionStartAttempts < MAGNIFIER_REGION_START_MAX_ATTEMPTS
                    && UpperScreenProjectionService.isActiveOrStarting()) {
                uiHandler.postDelayed(this::continuePendingMagnifierRegionAfterProjection,
                        MAGNIFIER_REGION_START_RETRY_MS);
                return;
            }
            clearPendingMagnifierRegionAfterProjection();
            showErrorAction(getString(R.string.error_magnifier_start));
            return;
        }
        float targetAspectRatio = pendingMagnifierRegionAfterProjectionAspectRatio;
        clearPendingMagnifierRegionAfterProjection();
        startMagnifierRegionCapture(item, targetAspectRatio);
    }

    private void clearPendingMagnifierRegionAfterProjection() {
        pendingMagnifierRegionAfterProjectionItem = null;
        pendingMagnifierRegionAfterProjectionAspectRatio = 0f;
        pendingMagnifierRegionStartAttempts = 0;
    }

    private void handleMagnifierRegionPreview(Intent intent) {
        WidgetLayout.Item item = pendingMagnifierRegionItem;
        if (item == null || pendingMagnifierRegionProfile == null) {
            return;
        }
        float left = intent.getFloatExtra(
                CoordinateCaptureActivity.EXTRA_REGION_LEFT, item.magnifierLeft);
        float top = intent.getFloatExtra(
                CoordinateCaptureActivity.EXTRA_REGION_TOP, item.magnifierTop);
        float right = intent.getFloatExtra(
                CoordinateCaptureActivity.EXTRA_REGION_RIGHT, item.magnifierRight);
        float bottom = intent.getFloatExtra(
                CoordinateCaptureActivity.EXTRA_REGION_BOTTOM, item.magnifierBottom);
        float aspect = intent.getFloatExtra(
                CoordinateCaptureActivity.EXTRA_TARGET_ASPECT,
                WidgetLayout.magnifierTargetAspectRatio(item));
        UpperScreenProjectionService.setRegion(left, top, right, bottom);
        UpperScreenProjectionService.setTuning(aspect, item.magnifierFps, 1f);
    }

    private void cancelMagnifierRegionCapture() {
        WidgetLayout.Item item = pendingMagnifierRegionItem;
        if (item != null) {
            UpperScreenProjectionService.setRegion(item.magnifierLeft, item.magnifierTop,
                    item.magnifierRight, item.magnifierBottom);
            UpperScreenProjectionService.setTuning(
                    WidgetLayout.magnifierTargetAspectRatio(item),
                    item.magnifierFps, item.magnifierZoom);
        }
        finishMagnifierRegionCapture();
    }

    private void handleMagnifierRegionCaptured(Intent intent) {
        WidgetLayout.Item item = pendingMagnifierRegionItem;
        GameProfile profile = pendingMagnifierRegionProfile;
        if (item == null || profile == null) {
            return;
        }
        commitMagnifierRegion(
                intent.getFloatExtra(CoordinateCaptureActivity.EXTRA_REGION_LEFT,
                        item.magnifierLeft),
                intent.getFloatExtra(CoordinateCaptureActivity.EXTRA_REGION_TOP,
                        item.magnifierTop),
                intent.getFloatExtra(CoordinateCaptureActivity.EXTRA_REGION_RIGHT,
                        item.magnifierRight),
                intent.getFloatExtra(CoordinateCaptureActivity.EXTRA_REGION_BOTTOM,
                        item.magnifierBottom),
                intent.getFloatExtra(CoordinateCaptureActivity.EXTRA_TARGET_ASPECT,
                        WidgetLayout.magnifierTargetAspectRatio(item)));
    }

    private void commitMagnifierRegion(float left, float top, float right, float bottom,
            float aspectRatio) {
        WidgetLayout.Item item = pendingMagnifierRegionItem;
        GameProfile profile = pendingMagnifierRegionProfile;
        if (item == null || profile == null) {
            return;
        }
        item.magnifierLeft = left;
        item.magnifierTop = top;
        item.magnifierRight = right;
        item.magnifierBottom = bottom;
        item.magnifierAspectRatio = aspectRatio;
        if (pendingMagnifierRegionUsesDraft) {
            draftWidgetLayout.sanitize();
        } else {
            profile.safeWidgetLayout().sanitize();
            ProfileStore.saveProfiles(this, profiles);
        }
        UpperScreenProjectionService.setRegion(item.magnifierLeft, item.magnifierTop,
                item.magnifierRight, item.magnifierBottom);
        UpperScreenProjectionService.setTuning(
                WidgetLayout.magnifierTargetAspectRatio(item),
                item.magnifierFps, item.magnifierZoom);
        finishMagnifierRegionCapture();
        rebuildContent();
    }

    private void finishMagnifierRegionCapture() {
        UpperScreenProjectionService.setFrozen(magnifierWasFrozenBeforeRegionCapture);
        magnifierWasFrozenBeforeRegionCapture = false;
        magnifierRegionCaptureInProgress = false;
        pendingMagnifierRegionItem = null;
        pendingMagnifierRegionProfile = null;
        pendingMagnifierRegionUsesDraft = false;
        captureInProgress = false;
        resumeMagnifierViews();
    }

    private void resumeMagnifierViews() {
        for (UpperScreenMagnifierView view : magnifierViews) {
            view.resume();
        }
    }

    private void pauseMagnifierViews() {
        for (UpperScreenMagnifierView view : magnifierViews) {
            view.pause();
        }
    }

    private void releaseMagnifierViews() {
        for (UpperScreenMagnifierView view : magnifierViews) {
            view.release();
        }
        magnifierViews.clear();
    }

    private void releaseCanvasViews() {
        for (CanvasWidgetView view : canvasViews) {
            view.release();
        }
        canvasViews.clear();
    }

    private void chooseCanvasImage(WidgetLayout.Item item, int frameWidth, int frameHeight) {
        if (hasUnsavedWidgetLayout()) {
            showErrorAction(getString(R.string.canvas_save_layout_first));
            return;
        }
        WidgetLayout.Item targetItem = resolveCanvasProfileItem(item);
        if (targetItem == null) {
            showErrorAction(getString(R.string.canvas_unavailable));
            return;
        }
        cancelPendingCanvasImport();
        pendingCanvasImportProfile = selectedProfile;
        pendingCanvasImportItem = targetItem;
        pendingCanvasImportFrameWidth = Math.max(1, frameWidth);
        pendingCanvasImportFrameHeight = Math.max(1, frameHeight);
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES,
                new String[]{"image/jpeg", "image/png", "image/webp"});
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivityForResult(intent, REQUEST_CANVAS_IMAGE);
        } catch (Exception ex) {
            cancelPendingCanvasImport();
            showErrorAction(getString(R.string.error_open_image_picker));
        }
    }

    private void importCanvasImage(GameProfile profile, WidgetLayout.Item item, Uri uri,
            int frameWidth, int frameHeight) {
        if (!isCanvasTargetValid(profile, item) || profile != selectedProfile) {
            return;
        }
        showAction(getString(R.string.canvas_importing));
        pendingCanvasImportRequest = CanvasAssetStore.importAsync(this, uri,
                new CanvasAssetStore.ImportCallback() {
                    @Override
                    public void onImported(String assetId) {
                        pendingCanvasImportRequest = null;
                        if (!isCanvasTargetValid(profile, item)
                                || selectedProfile != profile || isFinishing() || isDestroyed()) {
                            return;
                        }
                        CanvasConfig draft = new CanvasConfig();
                        draft.assetId = assetId;
                        if (item.canvasConfig != null) {
                            draft.shape = item.canvasConfig.shape;
                        }
                        showCanvasCompositionEditor(profile, item, draft, true,
                                frameWidth, frameHeight);
                    }

                    @Override
                    public void onError(CanvasAssetStore.ImportError error) {
                        pendingCanvasImportRequest = null;
                        showErrorAction(getString(canvasImportErrorString(error)));
                    }
                });
    }

    private int canvasImportErrorString(CanvasAssetStore.ImportError error) {
        if (error == CanvasAssetStore.ImportError.TOO_LARGE) {
            return R.string.canvas_import_too_large;
        }
        if (error == CanvasAssetStore.ImportError.ANIMATED) {
            return R.string.canvas_animated_unsupported;
        }
        if (error == CanvasAssetStore.ImportError.UNSUPPORTED) {
            return R.string.canvas_format_unsupported;
        }
        if (error == CanvasAssetStore.ImportError.DECODE) {
            return R.string.canvas_decode_error;
        }
        return R.string.canvas_import_failed;
    }

    private void showCanvasCompositionEditor(GameProfile profile, WidgetLayout.Item item,
            CanvasConfig config, boolean initialFill, int frameWidth, int frameHeight) {
        if (!isCanvasTargetValid(profile, item) || selectedProfile != profile
                || config == null || !config.hasAsset()) {
            showErrorAction(getString(R.string.canvas_unavailable));
            return;
        }
        final PanelOverlay[] overlayHolder = new PanelOverlay[1];
        final CanvasCompositionEditor[] editorHolder = new CanvasCompositionEditor[1];
        CanvasCompositionEditor editor = new CanvasCompositionEditor(this, config, initialFill,
                frameWidth, frameHeight,
                new CanvasCompositionEditor.Listener() {
                    @Override
                    public void onDone(CanvasConfig result) {
                        if (overlayHolder[0] == null) {
                            return;
                        }
                        dismissPanelAnimated(overlayHolder[0], () -> {
                            if (!isCanvasTargetValid(profile, item)
                                    || selectedProfile != profile) {
                                return;
                            }
                            item.canvasConfig = result.copy();
                            ProfileStore.saveProfiles(AssistantActivity.this, profiles);
                            rebuildContent();
                            showAction(getString(initialFill
                                    ? R.string.canvas_saved_first_hint
                                    : R.string.canvas_composition_saved));
                        });
                    }

                    @Override
                    public void onCancel() {
                        if (overlayHolder[0] != null) {
                            dismissPanelAnimated(overlayHolder[0]);
                        }
                    }
                });
        editorHolder[0] = editor;
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(-1, -1, Gravity.CENTER);
        params.setMargins(dp(8), dp(8), dp(8), dp(8));
        canvasOverlayActive = true;
        overlayHolder[0] = showPanelOverlay(editor, params, () -> {
            canvasOverlayActive = false;
            if (editorHolder[0] != null) {
                editorHolder[0].release();
            }
        });
        if (overlayHolder[0] == null) {
            canvasOverlayActive = false;
            editor.release();
        }
    }

    private void showCanvasFullscreen(WidgetLayout.Item item, int frameWidth, int frameHeight) {
        if (hasUnsavedWidgetLayout()) {
            showErrorAction(getString(R.string.canvas_save_layout_first));
            return;
        }
        WidgetLayout.Item targetItem = resolveCanvasProfileItem(item);
        if (targetItem == null || targetItem.canvasConfig == null
                || !targetItem.canvasConfig.hasAsset()) {
            showErrorAction(getString(R.string.canvas_unavailable));
            return;
        }
        GameProfile profile = selectedProfile;
        CanvasConfig config = targetItem.canvasConfig.copy();
        final PanelOverlay[] overlayHolder = new PanelOverlay[1];
        final FullscreenImageViewer[] viewerHolder = new FullscreenImageViewer[1];
        FullscreenImageViewer viewer = new FullscreenImageViewer(this, config.assetId,
                new FullscreenImageViewer.Listener() {
                    @Override
                    public void onClose() {
                        if (overlayHolder[0] != null) {
                            dismissPanelAnimated(overlayHolder[0]);
                        }
                    }

                    @Override
                    public void onEditComposition() {
                        if (overlayHolder[0] != null) {
                            dismissPanelAnimated(overlayHolder[0], () ->
                                    showCanvasCompositionEditor(profile, targetItem,
                                            targetItem.canvasConfig, false,
                                            frameWidth, frameHeight));
                        }
                    }
                });
        viewerHolder[0] = viewer;
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(-1, -1, Gravity.CENTER);
        canvasOverlayActive = true;
        overlayHolder[0] = showPanelOverlay(viewer, params, () -> {
            canvasOverlayActive = false;
            if (viewerHolder[0] != null) {
                viewerHolder[0].release();
            }
        });
        if (overlayHolder[0] == null) {
            canvasOverlayActive = false;
            viewer.release();
        }
    }

    private void showCanvasOptions(WidgetLayout.Item item, int frameWidth, int frameHeight) {
        if (hasUnsavedWidgetLayout()) {
            showErrorAction(getString(R.string.canvas_save_layout_first));
            return;
        }
        WidgetLayout.Item targetItem = resolveCanvasProfileItem(item);
        if (targetItem == null || targetItem.canvasConfig == null
                || !targetItem.canvasConfig.hasAsset()) {
            return;
        }
        GameProfile profile = selectedProfile;
        LinearLayout shell = new LinearLayout(this);
        shell.setOrientation(LinearLayout.VERTICAL);
        shell.setPadding(dp(14), dp(12), dp(14), dp(12));
        shell.setBackground(HeimdallUi.isPearl(this)
                ? HeimdallUi.cncFlush(this, HeimdallUi.RADIUS_PANEL)
                : HeimdallUi.glass(this, 0xFA0B111B, 0xFF070A10,
                        0x886A829C, 0x44344150, HeimdallUi.RADIUS_PANEL, 2));
        TextView title = text(getString(R.string.canvas_options),
                HeimdallUi.TYPE_EDITOR_TITLE, TEXT, true);
        shell.addView(title, new LinearLayout.LayoutParams(-1, dp(38)));

        final PanelOverlay[] overlayHolder = new PanelOverlay[1];
        boolean circular = targetItem.canvasConfig.isCircular();
        shell.addView(canvasOptionButton(circular
                ? R.string.canvas_use_rectangle
                : R.string.canvas_use_circle, () ->
                dismissCanvasOptionThen(overlayHolder[0], () ->
                        setCanvasShape(profile, targetItem, !circular))));
        shell.addView(canvasOptionButton(R.string.canvas_edit_composition, () ->
                dismissCanvasOptionThen(overlayHolder[0], () ->
                        showCanvasCompositionEditor(profile, targetItem,
                                targetItem.canvasConfig, false,
                                frameWidth, frameHeight))));
        shell.addView(canvasOptionButton(R.string.canvas_replace_image, () ->
                dismissCanvasOptionThen(overlayHolder[0], () -> chooseCanvasImage(targetItem,
                        frameWidth, frameHeight))));
        shell.addView(canvasOptionButton(R.string.canvas_view_fullscreen, () ->
                dismissCanvasOptionThen(overlayHolder[0], () -> showCanvasFullscreen(targetItem,
                        frameWidth, frameHeight))));
        shell.addView(canvasOptionButton(R.string.canvas_remove_image, () ->
                dismissCanvasOptionThen(overlayHolder[0], () -> confirmRemoveCanvasImage(
                        profile, targetItem))));
        shell.addView(canvasOptionButton(R.string.common_close, () -> {
            if (overlayHolder[0] != null) {
                dismissPanelAnimated(overlayHolder[0]);
            }
        }));

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                settingsOverlayWidth(520), -2, Gravity.CENTER);
        params.setMargins(dp(12), dp(12), dp(12), dp(12));
        canvasOverlayActive = true;
        overlayHolder[0] = showPanelOverlay(shell, params,
                () -> canvasOverlayActive = false);
        if (overlayHolder[0] == null) {
            canvasOverlayActive = false;
        }
    }

    private Button canvasOptionButton(int labelRes, Runnable action) {
        Button button = editorButton(getString(labelRes), action);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(46));
        params.setMargins(0, dp(3), 0, dp(3));
        button.setLayoutParams(params);
        return button;
    }

    private void dismissCanvasOptionThen(PanelOverlay overlay, Runnable action) {
        if (overlay != null) {
            dismissPanelAnimated(overlay, action);
        } else {
            action.run();
        }
    }

    private void setCanvasShape(GameProfile profile, WidgetLayout.Item item, boolean circular) {
        if (!isCanvasTargetValid(profile, item) || selectedProfile != profile
                || item.canvasConfig == null) {
            return;
        }
        item.canvasConfig.shape = circular
                ? CanvasConfig.SHAPE_CIRCLE : CanvasConfig.SHAPE_RECTANGLE;
        item.canvasConfig.normalize();
        ProfileStore.saveProfiles(this, profiles);
        rebuildContent();
        showAction(getString(circular
                ? R.string.canvas_circle_enabled : R.string.canvas_rectangle_enabled));
    }

    private void confirmRemoveCanvasImage(GameProfile profile, WidgetLayout.Item item) {
        canvasOverlayActive = true;
        PanelOverlay overlay = showSettingsDecisionPanel(getString(R.string.canvas_remove_image),
                getString(R.string.canvas_remove_confirm), null, null,
                getString(R.string.canvas_remove_image), () -> {
                    if (!isCanvasTargetValid(profile, item) || selectedProfile != profile) {
                        return;
                    }
                    CanvasConfig cleared = new CanvasConfig();
                    if (item.canvasConfig != null) {
                        cleared.shape = item.canvasConfig.shape;
                    }
                    item.canvasConfig = cleared;
                    ProfileStore.saveProfiles(this, profiles);
                    rebuildContent();
                    showAction(getString(R.string.canvas_image_removed));
                }, () -> canvasOverlayActive = false);
        if (overlay == null) {
            canvasOverlayActive = false;
        }
    }

    private boolean isCanvasTargetValid(GameProfile profile, WidgetLayout.Item item) {
        return profile != null && item != null
                && WidgetLayout.TYPE_CANVAS.equals(item.type)
                && profiles != null && profiles.contains(profile)
                && profile.safeWidgetLayout().items.contains(item);
    }

    private WidgetLayout.Item resolveCanvasProfileItem(WidgetLayout.Item requestedItem) {
        if (selectedProfile == null || requestedItem == null || hasUnsavedWidgetLayout()) {
            return null;
        }
        WidgetLayout profileLayout = selectedProfile.safeWidgetLayout();
        WidgetLayout.Item profileItem = profileLayout.items.contains(requestedItem)
                ? requestedItem : null;
        boolean requestedFromEquivalentDraft = draftWidgetLayout != null
                && draftWidgetLayout.items.contains(requestedItem);
        if (profileItem == null && requestedFromEquivalentDraft) {
            int itemIndex = draftWidgetLayout.items.indexOf(requestedItem);
            if (itemIndex >= 0 && itemIndex < profileLayout.items.size()) {
                WidgetLayout.Item candidate = profileLayout.items.get(itemIndex);
                if (WidgetLayout.TYPE_CANVAS.equals(candidate.type)) {
                    profileItem = candidate;
                }
            }
        }
        if (profileItem == null || !WidgetLayout.TYPE_CANVAS.equals(profileItem.type)) {
            return null;
        }
        if (requestedFromEquivalentDraft) {
            draftWidgetLayout = null;
        }
        return profileItem;
    }

    private void cancelPendingCanvasImport() {
        if (pendingCanvasImportRequest != null) {
            pendingCanvasImportRequest.cancel();
            pendingCanvasImportRequest = null;
        }
        pendingCanvasImportProfile = null;
        pendingCanvasImportItem = null;
        pendingCanvasImportFrameWidth = 1;
        pendingCanvasImportFrameHeight = 1;
    }

    private QuickActionButtonView addQuickActionButton(LinearLayout row, int icon,
                                                       String description, int visualState,
                                                       Runnable action) {
        QuickActionButtonView button = new QuickActionButtonView(this);
        button.setActionIcon(icon);
        button.setContentDescription(description);
        button.setVisualState(visualState);
        button.setOnClickListener(v -> action.run());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -1, 1);
        row.addView(button, params);
        return button;
    }

    private void applySystemGestureExclusion(View view) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return;
        }
        view.post(() -> {
            int width = view.getWidth();
            int height = view.getHeight();
            if (width <= 0 || height <= 0) {
                return;
            }
            view.setSystemGestureExclusionRects(Collections.singletonList(new Rect(0, 0, width, height)));
        });
        view.addOnLayoutChangeListener((changedView, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            int width = right - left;
            int height = bottom - top;
            if (width > 0 && height > 0) {
                changedView.setSystemGestureExclusionRects(Collections.singletonList(new Rect(0, 0, width, height)));
            }
        });
    }

    private void renderProfiles() {
        if (profileList == null) {
            return;
        }
        profileList.removeAllViews();
        for (GameProfile profile : profiles) {
            int index = profiles.indexOf(profile);
            Button button = new Button(this);
            button.setText(profile.name);
            button.setTextColor(HeimdallUi.textColor(this));
            button.setTextSize(12);
            button.setAllCaps(false);
            if (profile == selectedProfile) {
                HeimdallUi.applySelectedButton(this, button);
            } else {
                HeimdallUi.applySecondaryButton(this, button);
            }
            button.setOnClickListener(v -> {
                selectProfile(index);
            });
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(142), dp(46));
            params.setMargins(0, dp(4), dp(6), dp(4));
            profileList.addView(button, params);
        }
    }

    private void selectProfile(int index) {
        if (index < 0 || index >= profiles.size()) {
            return;
        }
        flushGuideReadingPosition();
        invalidateGuideTextCache();
        draftWidgetLayout = null;
        settingsTouchpadDraft = null;
        settingsMagnifierDraft = null;
        settingsMacroMappingProtectionInput = null;
        settingsMacroMappingProtectionDraft = null;
        viewingGuideInline = null;
        editingGuideInline = null;
        editingGuideTypeInline = null;
        editingInteractiveMapInline = false;
        editingMapMarkerInline = null;
        creatingMapMarkerInline = false;
        mapViewerFullscreen = false;
        guideReaderFullscreen = false;
        mapWebCurrentUrl = null;
        activeLocalMapIndex = 0;
        selectedProfileIndex = index;
        selectedProfile = profiles.get(index);
        touchpadSettings = selectedProfile.safeTouchpadSettings();
        ProfileStore.saveSelectedIndex(this, selectedProfileIndex);
        rebuildContent();
    }

    private void maybeAutoSwitchProfile(ForegroundAppTracker.Snapshot snapshot) {
        if (!ForegroundAppTracker.isEnabled(this)
                || snapshot == null
                || !snapshot.isUpperOrUnknownDisplay()
                || activeScreen == SCREEN_SETTINGS
                || hasUnsavedWidgetLayout()
                || activeDraftSteps != null
                || captureInProgress
                || magnifierRegionCaptureInProgress
                || canvasOverlayActive
                || pendingCanvasImportProfile != null
                || pendingCanvasImportRequest != null
                || (widgetGridDialog != null && widgetGridDialog.isShowing())) {
            return;
        }
        int match = ProfileAutoSwitchResolver.resolve(profiles, selectedProfileIndex, snapshot);
        if (match != ProfileAutoSwitchResolver.NO_MATCH && match != selectedProfileIndex) {
            selectProfile(match);
        }
    }

    private boolean hasUnsavedWidgetLayout() {
        if (draftWidgetLayout == null || selectedProfile == null) {
            return false;
        }
        try {
            return !draftWidgetLayout.toJson().toString().equals(
                    selectedProfile.safeWidgetLayout().toJson().toString());
        } catch (JSONException ignored) {
            return true;
        }
    }

    private void renderSelectedProfile() {
        if (profileTitle != null) {
        setTextIfChanged(profileTitle,
                getString(R.string.profile_title_dropdown, selectedProfile.name));
        }
        if (profileIconView != null) {
            profileIconView.setProfile(selectedProfile);
        }
        updateBridgeStatus();
        if (macroGrids.isEmpty()) {
            return;
        }
        for (MacroGridBinding binding : macroGrids) {
            binding.grid.removeAllViews();
        }

        selectedProfile.setMacroCount(selectedProfile.macroCount);
        selectedProfile.normalizeLayout();
        for (MacroGridBinding binding : macroGrids) {
            renderMacroBinding(binding);
        }
    }

    private void renderMacroBinding(MacroGridBinding binding) {
        WidgetLayout.Item item = binding.item;
        ensureMacroCapacity(item.macroStart + item.macroCount);
        int total = Math.max(1, Math.min(item.macroCount, selectedProfile.macros.size() - item.macroStart));
        int columns = Math.min(item.macroColumns, Math.max(1, total));
        int rowsNeeded = (int) Math.ceil(total / (float) columns);
        int rows = item.macroRows <= 0 ? rowsNeeded : Math.max(rowsNeeded, item.macroRows);
        for (int row = 0; row < rows; row++) {
                LinearLayout rowLine = new LinearLayout(this);
                rowLine.setOrientation(LinearLayout.HORIZONTAL);
            binding.grid.addView(rowLine, new LinearLayout.LayoutParams(-1, 0, 1));
            populateMacroRow(rowLine, item, row, rows, columns, total);
        }
    }

    private void populateMacroRow(LinearLayout line, WidgetLayout.Item item, int row, int rows, int columns, int total) {
        for (int col = 0; col < columns; col++) {
            int macroIndex = macroIndexForCell(item, row, col, rows, columns);
            if (macroIndex >= 0 && macroIndex < total) {
                addMacroButtonToLine(line, item.macroStart + macroIndex,
                        item.macroIconOnly);
            } else {
                line.addView(new View(this), new LinearLayout.LayoutParams(0, -1, 1));
            }
        }
    }

    private int macroIndexForCell(WidgetLayout.Item item, int row, int col, int rows, int columns) {
        if (item.macroRightHandPriority) {
            return (columns - 1 - col) * rows + row;
        }
        return row * columns + col;
    }

    private void addMacroButtonToLine(LinearLayout line, int macroIndex, boolean iconOnly) {
        Button button = macroButton(selectedProfile.macros.get(macroIndex), macroIndex, iconOnly);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -1, 1);
        params.setMargins(dp(5), dp(5), dp(5), dp(5));
        line.addView(button, params);
    }

    private void ensureMacroCapacity(int count) {
        int before = selectedProfile.macroCount;
        int required = Math.max(count, requiredMacroCountFromWidgets());
        selectedProfile.setMacroCount(required);
        if (selectedProfile.macroCount != before && draftWidgetLayout == null) {
            ProfileStore.saveProfiles(this, profiles);
        }
    }

    private int requiredMacroCountFromWidgets() {
        int required = 1;
        WidgetLayout layout = currentWidgetLayout();
        for (WidgetLayout.Item item : layout.items) {
            if (WidgetLayout.TYPE_MACRO_GROUP.equals(item.type)) {
                required = Math.max(required, item.macroStart + item.macroCount);
            }
        }
        return required;
    }

    private Button macroButton(Macro macro, int index, boolean iconOnly) {
        MacroButtonView button = new MacroButtonView(this);
        button.setMacroLabel(macro.label);
        button.setMacroLabelVisible(!iconOnly);
        button.setText("");
        button.setAllCaps(false);
        int macroPriority = macroPriorityFor(macro, index);
        MacroIconRepository.MacroIconOption icon = MacroIconRepository.resolve(this, macro);
        styleMacroButton(button, macroPriority, false, index, icon);
        button.addOnLayoutChangeListener((view, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) ->
                styleMacroButton(button, macroPriority, false, index, icon));
        button.setOnClickListener(v -> {
            button.cancelPendingMacroLongPress();
            runMacro(macro);
        });
        button.setMacroLongPressAction(() -> showMacroEditor(macro));
        return button;
    }

    private void styleMacroButton(Button button, int priority, boolean focused, int index,
                                  MacroIconRepository.MacroIconOption icon) {
        HeimdallUi.applyMacroButton(this, button, priority, focused, index);
        int color = HeimdallUi.isPearl(this)
                ? (focused ? 0xFF9B4C12 : (priority == HeimdallUi.MACRO_UTILITY ? 0xCC697687 : 0xFF344457))
                : (focused ? 0xFFFFFFFF : (priority == HeimdallUi.MACRO_UTILITY ? 0xCCB8C5D4 : 0xFFE6EDF3));
        installMacroIcon(button, icon, color, priority);
    }

    private void installMacroIcon(Button button, MacroIconRepository.MacroIconOption icon, int color, int priority) {
        int height = button.getHeight();
        int width = button.getWidth();
        float ratio = priority == HeimdallUi.MACRO_UTILITY
                ? HeimdallUi.MACRO_ICON_SHARE_UTILITY
                : HeimdallUi.MACRO_ICON_SHARE_STANDARD;
        int size = height > 0 && width > 0
                ? Math.max(dp(HeimdallUi.MACRO_ICON_MIN),
                        Math.min(dp(HeimdallUi.MACRO_ICON_MAX),
                                Math.round(Math.min(width, height) * ratio)))
                : dp(HeimdallUi.MACRO_ICON_MIN);
        if (button instanceof MacroButtonView) {
            ((MacroButtonView) button).setMacroIcon(icon, color, size);
            return;
        }
        setTopIcon(button, icon, color, size, dp(5));
    }

    private int macroPriorityFor(Macro macro, int index) {
        String role = macro == null ? Macro.ROLE_SECONDARY : macro.normalizedRole();
        if (Macro.ROLE_PRIMARY.equals(role)) {
            return HeimdallUi.MACRO_PRIMARY;
        }
        if (Macro.ROLE_UTILITY.equals(role)) {
            return HeimdallUi.MACRO_UTILITY;
        }
        return HeimdallUi.MACRO_SECONDARY;
    }

    private void runMacro(Macro macro) {
        if (statusText != null) {
            statusText.setText(getString(R.string.status_macro_running, macro.label));
        }
        InputBridge.dispatch(this, macro, isEnhancedTouchModeActive(),
                shouldProtectThorMappingFromControllerMacros(), inputStatusCallback);
    }

    private boolean isEnhancedTouchModeActive() {
        return selectedProfile != null
                && selectedProfile.touchpadSettings != null
                && TouchpadSettings.MODE_SHIZUKU_TOUCH.equals(
                        selectedProfile.touchpadSettings.mode);
    }

    private boolean shouldProtectThorMappingFromControllerMacros() {
        return isEnhancedTouchModeActive()
                && selectedProfile.protectThorMappingDuringEnhancedTouch;
    }

    private void updateBridgeStatus() {
        if (statusText == null || selectedProfile == null) {
            return;
        }
        setTextIfChanged(statusText, getString(R.string.main_status_summary,
                InputBridge.isReady(this) ? getString(R.string.common_ready)
                        : getString(R.string.input_not_enabled),
                displayGameName(), selectedProfile.mapMarkers.size(),
                selectedProfile.guides.size()));
    }

    private String displayGameName() {
        String value = selectedProfile == null || selectedProfile.name == null ? "" : selectedProfile.name.trim();
        return value.length() == 0 ? getString(R.string.common_unnamed_profile) : value;
    }

    private void showMacroPicker() {
        selectedProfile.setMacroCount(selectedProfile.macroCount);
        int count = Math.min(selectedProfile.macroCount, selectedProfile.macros.size());
        LinearLayout shell = new LinearLayout(this);
        shell.setOrientation(LinearLayout.VERTICAL);
        shell.setBackground(HeimdallUi.isPearl(this)
                ? HeimdallUi.cncFlush(this, 14)
                : HeimdallUi.glass(this, 0xFA0B111B, 0xFF070A10,
                        0x886A829C, 0x44344150, 14, 2));

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(16), dp(6), dp(16), dp(4));
        shell.addView(header, new LinearLayout.LayoutParams(-1, dp(66)));

        TextView title = text(getString(R.string.macro_picker_title),
                HeimdallUi.TYPE_EDITOR_TITLE, TEXT, true);
        header.addView(title, new LinearLayout.LayoutParams(-1, dp(30)));
        TextView subtitle = text(getString(R.string.macro_picker_subtitle,
                        nonEmpty(selectedProfile.name,
                                getString(R.string.common_profile_fallback)),
                        getResources().getQuantityString(R.plurals.macro_count,
                                count, count)),
                11, MUTED, false);
        header.addView(subtitle, new LinearLayout.LayoutParams(-1, dp(22)));

        View headerDivider = new View(this);
        headerDivider.setBackgroundColor(HeimdallUi.isPearl(this) ? 0x287B8792 : 0x445F7C9A);
        shell.addView(headerDivider, new LinearLayout.LayoutParams(-1, dp(1)));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(false);
        scroll.setScrollbarFadingEnabled(false);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(dp(12), dp(8), dp(12), dp(8));
        scroll.addView(list, new ScrollView.LayoutParams(-1, -2));
        shell.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        final PanelOverlay[] overlayHolder = new PanelOverlay[1];
        for (int rowStart = 0; rowStart < count; rowStart += 2) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            list.addView(row, new LinearLayout.LayoutParams(-1, dp(74)));
            for (int column = 0; column < 2; column++) {
                int index = rowStart + column;
                LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(0, dp(66), 1);
                cardParams.setMargins(column == 0 ? 0 : dp(4), dp(4),
                        column == 0 ? dp(4) : 0, dp(4));
                if (index >= count) {
                    row.addView(new View(this), cardParams);
                    continue;
                }
                Macro macro = selectedProfile.macros.get(index);
                View card = macroEditorPickerCard(macro, index, () -> {
                    Runnable openEditor = () -> showMacroEditor(macro);
                    if (overlayHolder[0] != null) {
                        dismissPanelAnimated(overlayHolder[0], openEditor);
                    } else {
                        openEditor.run();
                    }
                });
                row.addView(card, cardParams);
            }
        }

        if (count == 0) {
            TextView empty = text(getString(R.string.macro_picker_empty),
                    13, MUTED, false);
            empty.setGravity(Gravity.CENTER);
            list.addView(empty, new LinearLayout.LayoutParams(-1, dp(72)));
        }

        View footerDivider = new View(this);
        footerDivider.setBackgroundColor(HeimdallUi.isPearl(this) ? 0x287B8792 : 0x445F7C9A);
        shell.addView(footerDivider, new LinearLayout.LayoutParams(-1, dp(1)));
        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setPadding(dp(12), dp(4), dp(12), dp(4));
        shell.addView(actions, new LinearLayout.LayoutParams(-1, dp(58)));
        actions.addView(editorButton(getString(R.string.common_close), () -> {
            if (overlayHolder[0] != null) {
                dismissPanelAnimated(overlayHolder[0]);
            }
        }));

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                settingsOverlayWidth(760), settingsOverlayHeight(760), Gravity.CENTER);
        params.setMargins(dp(12), dp(12), dp(12), dp(12));
        overlayHolder[0] = showPanelOverlay(shell, params, null);
    }

    private View macroEditorPickerCard(Macro macro, int index, Runnable action) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(8), dp(5), dp(10), dp(5));
        card.setBackground(HeimdallUi.isPearl(this)
                ? HeimdallUi.pearlMenuControl(this, HeimdallUi.RADIUS_CARD, false, false)
                : HeimdallUi.glass(this, 0xB20F1622, 0xC9090E16,
                        0x665F7C9A, 0x33344150, HeimdallUi.RADIUS_CARD, 1));
        card.setClickable(true);
        card.setFocusable(true);
        card.setOnClickListener(view -> action.run());

        ImageView icon = new ImageView(this);
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        icon.setPadding(dp(7), dp(7), dp(7), dp(7));
        updateMacroIconPreview(icon, macro, macro.iconKey);
        card.addView(icon, new LinearLayout.LayoutParams(dp(48), dp(48)));

        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        labels.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams labelsParams = new LinearLayout.LayoutParams(0, -1, 1);
        labelsParams.setMargins(dp(7), 0, dp(6), 0);
        card.addView(labels, labelsParams);

        String label = nonEmpty(macro.label, getString(R.string.common_macro_fallback));
        TextView name = text(label, 13, TEXT, true);
        name.setSingleLine(true);
        name.setEllipsize(android.text.TextUtils.TruncateAt.END);
        labels.addView(name, new LinearLayout.LayoutParams(-1, dp(28)));

        int stepCount = macro.steps == null ? 0 : macro.steps.size();
        String role = macro.normalizedRole();
        String roleLabel = Macro.ROLE_PRIMARY.equals(role)
                ? getString(R.string.macro_role_high)
                : (Macro.ROLE_UTILITY.equals(role)
                        ? getString(R.string.macro_role_low)
                        : getString(R.string.macro_role_medium));
        TextView meta = text(getString(R.string.macro_picker_card_meta, index + 1,
                        getResources().getQuantityString(R.plurals.step_count,
                                stepCount, stepCount), roleLabel),
                10, MUTED, false);
        meta.setSingleLine(true);
        meta.setEllipsize(android.text.TextUtils.TruncateAt.END);
        labels.addView(meta, new LinearLayout.LayoutParams(-1, dp(22)));

        TextView edit = text(getString(R.string.macro_picker_edit), 11,
                HeimdallUi.accent(this), true);
        edit.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);
        edit.setSingleLine(true);
        card.addView(edit, new LinearLayout.LayoutParams(dp(54), -1));
        card.setContentDescription(getString(R.string.macro_edit_content_description,
                label, getResources().getQuantityString(R.plurals.step_count,
                        stepCount, stepCount), roleLabel));
        return card;
    }

    private void showProfileQuickPicker() {
        final PanelOverlay[] holder = new PanelOverlay[1];
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(14), dp(10), dp(14), dp(10));
        panel.setBackground(HeimdallUi.isPearl(this)
                ? HeimdallUi.surfacePanel(this, HeimdallUi.RADIUS_PANEL)
                : HeimdallUi.glass(this, 0xFA0B111B, 0xFF070A10,
                        0x886A829C, 0x44344150, 14, 2));

        TextView title = text(getString(R.string.profile_quick_switch_title),
                HeimdallUi.TYPE_PAGE_TITLE, TEXT, true);
        panel.addView(title, new LinearLayout.LayoutParams(-1, dp(32)));

        LinearLayout current = new LinearLayout(this);
        current.setOrientation(LinearLayout.HORIZONTAL);
        current.setGravity(Gravity.CENTER_VERTICAL);
        current.setPadding(dp(8), dp(4), dp(6), dp(4));
        current.setBackground(HeimdallUi.isPearl(this)
                ? HeimdallUi.cncControl(this, HeimdallUi.RADIUS_CARD, false, false)
                : HeimdallUi.insetPanel(this, HeimdallUi.RADIUS_CARD));
        LinearLayout.LayoutParams currentParams = new LinearLayout.LayoutParams(-1, dp(48));
        currentParams.setMargins(0, 0, 0, dp(6));
        panel.addView(current, currentParams);

        ProfileIconView currentIcon = new ProfileIconView(this);
        currentIcon.setProfile(selectedProfile);
        current.addView(currentIcon, new LinearLayout.LayoutParams(dp(34), dp(34)));

        TextView currentText = text(getString(R.string.profile_current_name,
                        nonEmpty(selectedProfile.name,
                                getString(R.string.common_profile_fallback))),
                12, TEXT, true);
        currentText.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
        currentText.setSingleLine(true);
        currentText.setEllipsize(android.text.TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams currentTextParams = new LinearLayout.LayoutParams(0, -1, 1);
        currentTextParams.setMargins(dp(8), 0, dp(4), 0);
        current.addView(currentText, currentTextParams);

        Button changeIcon = compactButton(getString(R.string.profile_change_icon), () -> {
            if (holder[0] != null) {
                dismissPanelAnimated(holder[0], this::chooseProfileIcon);
            } else {
                chooseProfileIcon();
            }
        });
        changeIcon.setSingleLine(true);
        changeIcon.setTextSize(10);
        LinearLayout.LayoutParams changeIconParams = new LinearLayout.LayoutParams(dp(84), dp(40));
        changeIconParams.setMargins(dp(3), 0, dp(3), 0);
        current.addView(changeIcon, changeIconParams);

        Button resetIcon = compactButton(getString(R.string.profile_default_icon), () -> {
            Runnable reset = () -> {
                selectedProfile.iconUri = "";
                ProfileStore.saveProfiles(this, profiles);
                rebuildContent();
            };
            if (holder[0] != null) {
                dismissPanelAnimated(holder[0], reset);
            } else {
                reset.run();
            }
        });
        resetIcon.setEnabled(selectedProfile.iconUri != null && selectedProfile.iconUri.trim().length() > 0);
        resetIcon.setAlpha(resetIcon.isEnabled() ? 1f : 0.45f);
        resetIcon.setSingleLine(true);
        resetIcon.setTextSize(10);
        LinearLayout.LayoutParams resetIconParams = new LinearLayout.LayoutParams(dp(84), dp(40));
        resetIconParams.setMargins(dp(3), 0, 0, 0);
        current.addView(resetIcon, resetIconParams);

        ScrollView scroll = new ScrollView(this);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(list, new ScrollView.LayoutParams(-1, -2));
        panel.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        for (int i = 0; i < profiles.size(); i++) {
            final int index = i;
            GameProfile profile = profiles.get(i);
            boolean selected = i == selectedProfileIndex;
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(8), dp(4), dp(8), dp(4));
            row.setBackground(HeimdallUi.isPearl(this)
                    ? HeimdallUi.pearlMenuControl(this, HeimdallUi.RADIUS_CARD, selected, false)
                    : HeimdallUi.glass(this,
                            selected ? 0xB2111A26 : 0xA6101722,
                            selected ? 0xD0080D14 : 0xC9080C12,
                            selected ? 0xAA70B7FF : 0x665F7C9A,
                            selected ? 0x55445A72 : 0x33344150,
                            HeimdallUi.RADIUS_CARD, 1));
            row.setOnClickListener(view -> {
                if (holder[0] != null) {
                    dismissPanelAnimated(holder[0], () -> {
                        selectProfile(index);
                        showAction(getString(R.string.profile_switched, selectedProfile.name));
                    });
                } else {
                    selectProfile(index);
                    showAction(getString(R.string.profile_switched, selectedProfile.name));
                }
            });
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, dp(52));
            rowParams.setMargins(0, 0, 0, dp(4));
            list.addView(row, rowParams);

            ProfileIconView icon = new ProfileIconView(this);
            icon.setProfile(profile);
            row.addView(icon, new LinearLayout.LayoutParams(dp(36), dp(36)));

            TextView detail = text(nonEmpty(profile.name, "Profile"),
                    13, selected ? TEXT : MUTED, selected);
            detail.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
            detail.setSingleLine(true);
            detail.setEllipsize(android.text.TextUtils.TruncateAt.END);
            LinearLayout.LayoutParams detailParams = new LinearLayout.LayoutParams(0, -1, 1);
            detailParams.setMargins(dp(9), 0, dp(8), 0);
            row.addView(detail, detailParams);

            if (selected) {
                TextView currentBadge = text(getString(R.string.common_current), 10,
                        HeimdallUi.accent(this), true);
                currentBadge.setGravity(Gravity.CENTER);
                row.addView(currentBadge, new LinearLayout.LayoutParams(dp(42), -1));
            }
        }

        Button close = editorButton(getString(R.string.common_close), () -> {
            if (holder[0] != null) {
                dismissPanelAnimated(holder[0]);
            }
        });
        LinearLayout.LayoutParams closeParams = new LinearLayout.LayoutParams(-1, dp(42));
        closeParams.setMargins(0, dp(4), 0, 0);
        panel.addView(close, closeParams);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                settingsOverlayWidth(650), settingsOverlayHeight(760), Gravity.CENTER);
        params.setMargins(dp(12), dp(12), dp(12), dp(12));
        holder[0] = showPanelOverlay(panel, params, null);
    }

    private void chooseProfileIcon() {
        if (releaseTextInputFocusThen(this::chooseProfileIcon)) {
            return;
        }
        pendingProfileIconIndex = selectedProfileIndex;
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        try {
            startActivityForResult(intent, REQUEST_PROFILE_ICON);
        } catch (Exception ex) {
            pendingProfileIconIndex = -1;
            showErrorAction(getString(R.string.error_open_image_picker));
        }
    }

    private void showSettingsPanel() {
        if (activeScreen == SCREEN_SETTINGS || settingsTransitionState != SETTINGS_TRANSITION_NONE) {
            return;
        }
        flushGuideReadingPosition();
        if (guideReaderFullscreen) {
            leaveGuideFullscreenState();
        }
        cancelContentTransitions();
        clearPanelOverlays();
        settingsContentScrollY = 0;
        if (!shouldAnimateUi() || contentHost == null || currentContentPage == null
                || bottomDock == null || bottomDock.getHeight() <= 0) {
            activeScreen = SCREEN_SETTINGS;
            rebuildContent();
            return;
        }

        int generation = ++settingsTransitionGeneration;
        settingsTransitionState = SETTINGS_TRANSITION_ENTERING;
        closeSettingsAfterEnter = false;
        setDockNavButtonsEnabled(false);
        activeScreen = SCREEN_SETTINGS;
        updateGameFocusProtection();

        View dock = bottomDock;
        dock.animate().cancel();
        dock.setVisibility(View.VISIBLE);
        dock.setTranslationY(0f);
        trackAnimatedView(dock);
        dock.animate()
                .translationY(dock.getHeight() + dp(8))
                .setDuration(SETTINGS_DOCK_EXIT_MS)
                .setInterpolator(UI_EASE_OUT)
                .withEndAction(() -> installSettingsPageAfterDockExit(generation, dock))
                .start();
    }

    private void installSettingsPageAfterDockExit(int generation, View dock) {
        finishAnimatedView(dock);
        if (!isSettingsTransitionCurrent(generation, SETTINGS_TRANSITION_ENTERING)
                || contentHost == null || currentContentPage == null || bottomDock != dock) {
            return;
        }

        removeStaleContentPages();
        View outgoingPage = currentContentPage;
        int outgoingScreen = contentPageScreen(outgoingPage, SCREEN_MAIN);

        settingsContentScroll = null;
        settingsContentContainer = null;
        View incomingPage = createContentPage(SCREEN_SETTINGS);
        applyFlatUiPolicy(incomingPage);
        incomingPage.setVisibility(View.INVISIBLE);
        incomingPage.setTranslationY(dp(SETTINGS_PAGE_ENTER_TRANSLATION_DP));
        contentHost.addView(incomingPage, new FrameLayout.LayoutParams(-1, -1));
        currentContentPage = incomingPage;

        bottomDock.setVisibility(View.GONE);
        bottomDock.setTranslationY(0f);
        contentHost.requestLayout();
        ViewTreeObserver observer = contentHost.getViewTreeObserver();
        observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                if (contentHost != null && contentHost.getViewTreeObserver().isAlive()) {
                    contentHost.getViewTreeObserver().removeOnPreDrawListener(this);
                }
                if (!isSettingsTransitionCurrent(generation, SETTINGS_TRANSITION_ENTERING)
                        || incomingPage.getParent() != contentHost) {
                    return true;
                }

                outgoingPage.setVisibility(View.INVISIBLE);
                removeContentPage(outgoingPage);
                cleanupContentPage(outgoingScreen);
                incomingPage.setVisibility(View.VISIBLE);
                incomingPage.setAlpha(1f);
                trackAnimatedView(incomingPage);
                incomingPage.postOnAnimation(() -> {
                    if (!isSettingsTransitionCurrent(generation, SETTINGS_TRANSITION_ENTERING)
                            || incomingPage.getParent() != contentHost) {
                        finishAnimatedView(incomingPage);
                        return;
                    }
                    incomingPage.animate().cancel();
                    incomingPage.animate()
                            .translationY(0f)
                            .setDuration(SETTINGS_PAGE_ENTER_MS)
                            .setInterpolator(UI_EASE_OUT)
                            .withEndAction(() -> finishSettingsPageEnter(generation, incomingPage))
                            .start();
                });
                return true;
            }
        });
    }

    private void finishSettingsPageEnter(int generation, View incomingPage) {
        finishAnimatedView(incomingPage);
        if (!isSettingsTransitionCurrent(generation, SETTINGS_TRANSITION_ENTERING)) {
            return;
        }
        incomingPage.setTranslationY(0f);
        settingsTransitionState = SETTINGS_TRANSITION_NONE;
        setDockNavButtonsEnabled(true);
        if (closeSettingsAfterEnter) {
            closeSettingsAfterEnter = false;
            closeSettingsPanel();
        }
    }

    private void closeSettingsPanel() {
        if (releaseTextInputFocusThen(this::closeSettingsPanel)) {
            return;
        }
        if (settingsTransitionState == SETTINGS_TRANSITION_ENTERING) {
            closeSettingsAfterEnter = true;
            return;
        }
        if (settingsTransitionState == SETTINGS_TRANSITION_EXITING) {
            return;
        }
        if (activeScreen != SCREEN_SETTINGS) {
            return;
        }
        if (!shouldAnimateUi() || contentHost == null || currentContentPage == null
                || bottomDock == null) {
            activeScreen = SCREEN_MAIN;
            rebuildContent();
            uiHandler.post(() -> maybeAutoSwitchProfile(ForegroundAppTracker.latest()));
            return;
        }

        int generation = ++settingsTransitionGeneration;
        settingsTransitionState = SETTINGS_TRANSITION_EXITING;
        setDockNavButtonsEnabled(false);
        View outgoingPage = currentContentPage;
        outgoingPage.animate().cancel();
        trackAnimatedView(outgoingPage);
        outgoingPage.animate()
                .translationY(dp(SETTINGS_PAGE_EXIT_TRANSLATION_DP))
                .setDuration(SETTINGS_PAGE_EXIT_MS)
                .setInterpolator(UI_EASE_OUT)
                .withEndAction(() -> installMainPageAfterSettingsExit(generation, outgoingPage))
                .start();
    }

    private void installMainPageAfterSettingsExit(int generation, View outgoingPage) {
        finishAnimatedView(outgoingPage);
        if (!isSettingsTransitionCurrent(generation, SETTINGS_TRANSITION_EXITING)
                || contentHost == null || currentContentPage != outgoingPage || bottomDock == null) {
            return;
        }

        activeScreen = SCREEN_MAIN;
        settingsContentScroll = null;
        settingsContentContainer = null;
        View incomingPage = createContentPage(SCREEN_MAIN);
        applyFlatUiPolicy(incomingPage);
        incomingPage.setVisibility(View.INVISIBLE);
        contentHost.addView(incomingPage, new FrameLayout.LayoutParams(-1, -1));
        currentContentPage = incomingPage;
        renderSelectedProfile();
        updateDockNavSelection(false);

        bottomDock.animate().cancel();
        bottomDock.setTranslationY(dp(HeimdallUi.HEIGHT_DOCK + 8));
        bottomDock.setVisibility(View.VISIBLE);
        contentHost.requestLayout();
        ViewTreeObserver observer = contentHost.getViewTreeObserver();
        observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                if (contentHost != null && contentHost.getViewTreeObserver().isAlive()) {
                    contentHost.getViewTreeObserver().removeOnPreDrawListener(this);
                }
                if (!isSettingsTransitionCurrent(generation, SETTINGS_TRANSITION_EXITING)
                        || incomingPage.getParent() != contentHost) {
                    return true;
                }

                outgoingPage.setVisibility(View.INVISIBLE);
                removeContentPage(outgoingPage);
                incomingPage.setVisibility(View.VISIBLE);
                incomingPage.setAlpha(1f);
                View dock = bottomDock;
                trackAnimatedView(dock);
                dock.postOnAnimation(() -> {
                    if (!isSettingsTransitionCurrent(generation, SETTINGS_TRANSITION_EXITING)
                            || dock != bottomDock || dock.getParent() == null) {
                        finishAnimatedView(dock);
                        return;
                    }
                    dock.animate().cancel();
                    dock.animate()
                            .translationY(0f)
                            .setDuration(SETTINGS_DOCK_ENTER_MS)
                            .setInterpolator(UI_EASE_OUT)
                            .withEndAction(() -> finishSettingsPageExit(generation, dock))
                            .start();
                });
                return true;
            }
        });
    }

    private void finishSettingsPageExit(int generation, View dock) {
        finishAnimatedView(dock);
        if (!isSettingsTransitionCurrent(generation, SETTINGS_TRANSITION_EXITING)) {
            return;
        }
        dock.setTranslationY(0f);
        settingsTransitionState = SETTINGS_TRANSITION_NONE;
        updateGameFocusProtection();
        setDockNavButtonsEnabled(true);
        uiHandler.post(() -> maybeAutoSwitchProfile(ForegroundAppTracker.latest()));
    }

    private boolean isSettingsTransitionCurrent(int generation, int state) {
        return generation == settingsTransitionGeneration && settingsTransitionState == state;
    }

    private void setDockNavButtonsEnabled(boolean enabled) {
        for (DockNavButton button : dockNavButtons) {
            button.setEnabled(enabled);
        }
    }

    private void openSettingsSection(int section) {
        activeScreen = SCREEN_SETTINGS;
        updateGameFocusProtection();
        if (activeSettingsSection != section) {
            settingsContentScroll = null;
            settingsContentScrollY = 0;
        }
        if (activeSettingsSection != section && activeSettingsSection == SETTINGS_TOUCHPAD) {
            settingsTouchpadDraft = null;
        }
        if (activeSettingsSection != section && activeSettingsSection == SETTINGS_MAGNIFIER) {
            settingsMagnifierDraft = null;
        }
        if (activeSettingsSection != section && activeSettingsSection == SETTINGS_MACRO) {
            settingsMacroMappingProtectionInput = null;
            settingsMacroMappingProtectionDraft = null;
        }
        activeSettingsSection = section;
        if (section == SETTINGS_TOUCHPAD) {
            ensureSettingsTouchpadDraft();
        }
        if (section == SETTINGS_MAGNIFIER) {
            ensureSettingsMagnifierDraft();
        }
        if (section == SETTINGS_MACRO && settingsMacroMappingProtectionDraft == null) {
            settingsMacroMappingProtectionDraft =
                    selectedProfile.protectThorMappingDuringEnhancedTouch;
        }
        if (section == SETTINGS_APPEARANCE) {
            settingsThemeDraft = HeimdallUi.theme(this);
            settingsPerformanceCompatibilityDraft =
                    ThorPerformanceCompatibility.isEnabled(this);
        }
        rebuildContent();
    }

    private View createSettingsHomePanel() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(6), dp(6), dp(6), dp(6));
        panel.setBackground(HeimdallUi.isPearl(this)
                ? HeimdallUi.cncFlush(this, HeimdallUi.RADIUS_PANEL)
                : HeimdallUi.surfacePanel(this, HeimdallUi.RADIUS_PANEL));

        LinearLayout shell = new LinearLayout(this);
        shell.setOrientation(LinearLayout.HORIZONTAL);
        panel.addView(shell, new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout rail = new LinearLayout(this);
        rail.setOrientation(LinearLayout.VERTICAL);
        rail.setPadding(0, 0, dp(8), 0);
        shell.addView(rail, new LinearLayout.LayoutParams(dp(214), -1));

        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.VERTICAL);
        rail.addView(nav, new LinearLayout.LayoutParams(-1, 0, 1));

        nav.addView(settingsCategoryButton(getString(R.string.settings_category_main),
                R.drawable.ic_overview, SETTINGS_LAYOUT));
        nav.addView(settingsCategoryButton(getString(R.string.settings_category_controls),
                R.drawable.ic_touchpad, SETTINGS_TOUCHPAD));
        nav.addView(settingsCategoryButton(getString(R.string.settings_category_macros),
                R.drawable.ic_macro, SETTINGS_MACRO));
        nav.addView(settingsCategoryButton(getString(R.string.settings_category_magnifier),
                R.drawable.ic_fullscreen, SETTINGS_MAGNIFIER));
        nav.addView(settingsCategoryButton(getString(R.string.settings_category_appearance),
                R.drawable.ic_settings, SETTINGS_APPEARANCE));
        nav.addView(settingsCategoryButton(getString(R.string.settings_category_connection),
                R.drawable.ic_bridge, SETTINGS_INPUT));
        nav.addView(settingsCategoryButton(getString(R.string.common_profile_fallback),
                R.drawable.ic_profile, SETTINGS_PROFILE));
        nav.addView(settingsCategoryButton(getString(R.string.settings_category_diagnostics),
                R.drawable.ic_guide, SETTINGS_DIAGNOSTICS));

        LinearLayout exitSection = new LinearLayout(this);
        exitSection.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams exitSectionParams = new LinearLayout.LayoutParams(
                -1, dp(HeimdallUi.HEIGHT_SETTINGS_FOOTER + 6));
        rail.addView(exitSection, exitSectionParams);

        View exitDivider = new View(this);
        exitDivider.setBackgroundColor(HeimdallUi.isPearl(this)
                ? 0x287B8792 : 0x445F7C9A);
        LinearLayout.LayoutParams exitDividerParams = new LinearLayout.LayoutParams(-1, dp(1));
        exitDividerParams.setMargins(dp(10), 0, dp(10), 0);
        exitSection.addView(exitDivider, exitDividerParams);

        Button exitAction = settingsExitAction();
        LinearLayout.LayoutParams exitActionParams = new LinearLayout.LayoutParams(
                -1, dp(HeimdallUi.HEIGHT_SETTINGS_FOOTER));
        exitActionParams.setMargins(0, dp(5), 0, 0);
        exitSection.addView(exitAction, exitActionParams);

        LinearLayout rightColumn = new LinearLayout(this);
        rightColumn.setOrientation(LinearLayout.VERTICAL);
        shell.addView(rightColumn, new LinearLayout.LayoutParams(0, -1, 1));

        ScrollView contentScroll = new ScrollView(this);
        settingsContentScroll = contentScroll;
        contentScroll.setFillViewport(true);
        contentScroll.setBackground(HeimdallUi.isPearl(this)
                ? HeimdallUi.cncFlush(this, 12)
                : rounded(0xFF0B1018, HeimdallUi.border(this), 12));
        rightColumn.addView(contentScroll, new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout content = new LinearLayout(this);
        settingsContentContainer = content;
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(12), dp(16), dp(12));
        contentScroll.addView(content, new ScrollView.LayoutParams(-1, -2));
        populateSettingsContent(content);

        LinearLayout footer = new LinearLayout(this);
        footer.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams footerParams = new LinearLayout.LayoutParams(
                -1, dp(HeimdallUi.HEIGHT_SETTINGS_FOOTER));
        footerParams.setMargins(0, dp(6), 0, 0);
        rightColumn.addView(footer, footerParams);

        footer.addView(settingsFooterButton(getString(R.string.common_back),
                this::closeSettingsPanel, false, true));
        boolean canReset = activeSettingsSection != SETTINGS_PROFILE
                && activeSettingsSection != SETTINGS_DIAGNOSTICS;
        String resetLabel = !canReset
                ? getString(R.string.settings_no_reset)
                : (activeSettingsSection == SETTINGS_INPUT
                        ? getString(R.string.settings_use_basic_connection)
                        : getString(R.string.settings_restore_defaults));
        footer.addView(settingsFooterButton(resetLabel,
                () -> runAfterTextInputFocusRelease(this::resetActiveSettingsSection),
                false, canReset));
        boolean canSave = activeSettingsSection != SETTINGS_INPUT
                && activeSettingsSection != SETTINGS_DIAGNOSTICS;
        String saveLabel = canSave ? getString(R.string.common_save)
                : getString(activeSettingsSection == SETTINGS_DIAGNOSTICS
                        ? R.string.settings_no_save
                        : R.string.settings_selection_applies_immediately);
        footer.addView(settingsFooterButton(saveLabel,
                () -> runAfterTextInputFocusRelease(this::saveActiveSettingsSection),
                canSave, canSave));

        return panel;
    }

    private void exitHeimdall() {
        resetRightStickIfNeeded();
        finishAndRemoveTask();
    }

    private Button settingsExitAction() {
        Button button = actionButton(getString(R.string.common_exit_heimdall),
                () -> runAfterTextInputFocusRelease(this::exitHeimdall));
        HeimdallUi.applySecondaryButton(this, button);
        button.setTextColor(HeimdallUi.mutedTextColor(this));
        button.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        button.setSingleLine(true);
        button.setEllipsize(android.text.TextUtils.TruncateAt.END);
        button.setPadding(dp(14), 0, dp(8), 0);
        button.setCompoundDrawablePadding(dp(11));

        boolean pearl = HeimdallUi.isPearl(this);
        ColorStateList iconColors = new ColorStateList(
                new int[][]{
                        new int[]{-android.R.attr.state_enabled},
                        new int[]{android.R.attr.state_pressed},
                        new int[]{}
                },
                new int[]{
                        pearl ? 0x668A5964 : 0x66D27E82,
                        pearl ? 0xFFA75159 : 0xFFEA7175,
                        pearl ? 0xFF8A5964 : 0xFFD27E82
                });
        setLeftIcon(button, R.drawable.ic_power, iconColors, dp(20));
        return button;
    }

    private View settingsCategoryButton(String title, int iconRes, int section) {
        boolean selected = activeSettingsSection == section;
        FrameLayout shell = new FrameLayout(this);
        shell.setClickable(true);
        shell.setFocusable(true);
        shell.setOnClickListener(v -> openSettingsSection(section));
        if (selected) {
            shell.setBackground(HeimdallUi.isPearl(this)
                    ? HeimdallUi.pearlMenuControl(this, HeimdallUi.RADIUS_BUTTON, false, false)
                    : HeimdallUi.glass(this, 0xA9121A26, 0xB9080D14,
                            0x5570B7FF, 0x22344150, HeimdallUi.RADIUS_BUTTON, 1));
        } else {
            shell.setBackgroundColor(Color.TRANSPARENT);
        }

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14), 0, dp(8), 0);
        shell.addView(row, new FrameLayout.LayoutParams(-1, -1));

        ImageView icon = new ImageView(this);
        icon.setImageResource(iconRes);
        icon.setColorFilter(selected ? HeimdallUi.accent(this) : HeimdallUi.mutedTextColor(this));
        icon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        row.addView(icon, new LinearLayout.LayoutParams(dp(22), dp(22)));

        TextView label = text(title, 13, selected ? TEXT : MUTED, selected);
        label.setSingleLine(true);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(0, -1, 1);
        labelParams.setMargins(dp(11), 0, 0, 0);
        row.addView(label, labelParams);

        if (selected) {
            View marker = new View(this);
            marker.setBackgroundColor(HeimdallUi.accent(this));
            FrameLayout.LayoutParams markerParams = new FrameLayout.LayoutParams(dp(3), dp(28));
            markerParams.gravity = Gravity.LEFT | Gravity.CENTER_VERTICAL;
            shell.addView(marker, markerParams);
        }
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, 0, 1);
        params.setMargins(0, dp(2), 0, dp(2));
        shell.setLayoutParams(params);
        return shell;
    }

    private Button settingsFooterButton(String label, Runnable action,
                                        boolean primary, boolean enabled) {
        Button button = editorButton(label, action);
        if (primary) {
            HeimdallUi.applyPrimaryActionButton(this, button);
        }
        button.setEnabled(enabled);
        button.setAlpha(enabled ? 1f : 0.45f);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -1, 1);
        params.setMargins(dp(4), dp(3), dp(4), dp(3));
        button.setLayoutParams(params);
        return button;
    }

    private void populateSettingsContent(LinearLayout content) {
        TextView title = text(settingsSectionTitle(), 16, TEXT, true);
        title.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
        content.addView(title, new LinearLayout.LayoutParams(-1, dp(32)));

        TextView summary = text(settingsSectionSummary(), 11, MUTED, false);
        summary.setGravity(Gravity.TOP | Gravity.LEFT);
        summary.setMinHeight(dp(34));
        LinearLayout.LayoutParams summaryParams = new LinearLayout.LayoutParams(-1, -2);
        summaryParams.setMargins(0, 0, 0, dp(6));
        content.addView(summary, summaryParams);

        if (activeSettingsSection == SETTINGS_LAYOUT) {
            populateLayoutSettingsContent(content);
        } else if (activeSettingsSection == SETTINGS_TOUCHPAD) {
            populateTouchpadSettingsContent(content);
        } else if (activeSettingsSection == SETTINGS_MACRO) {
            populateMacroSettingsContent(content);
        } else if (activeSettingsSection == SETTINGS_MAGNIFIER) {
            populateMagnifierSettingsContent(content);
        } else if (activeSettingsSection == SETTINGS_INPUT) {
            populateInputSettingsContent(content);
        } else if (activeSettingsSection == SETTINGS_APPEARANCE) {
            populateAppearanceSettingsContent(content);
        } else if (activeSettingsSection == SETTINGS_DIAGNOSTICS) {
            populateDiagnosticsSettingsContent(content);
        } else {
            populateProfileSettingsContent(content);
        }
    }

    private void populateAppearanceSettingsContent(LinearLayout content) {
        if (settingsThemeDraft == null) {
            settingsThemeDraft = HeimdallUi.theme(this);
        }
        addSettingsLabel(content, getString(R.string.settings_theme));
        LinearLayout row = settingsActionRow(content);
        row.addView(themeChoiceButton("Heimdall Blue", HeimdallUi.THEME_DARK));
        row.addView(themeChoiceButton("Freya White", HeimdallUi.THEME_PEARL));
        addSettingsHelp(content, getString(R.string.settings_theme_help));

        addSettingsLabel(content, getString(R.string.settings_language));
        LinearLayout languageRow = settingsActionRow(content);
        languageRow.addView(languageChoiceButton(R.string.language_system_default,
                AppLanguageManager.LANGUAGE_SYSTEM));
        languageRow.addView(languageChoiceButton(R.string.language_english,
                AppLanguageManager.LANGUAGE_ENGLISH));
        languageRow.addView(languageChoiceButton(R.string.language_simplified_chinese,
                AppLanguageManager.LANGUAGE_SIMPLIFIED_CHINESE));
        addSettingsHelp(content, getString(R.string.settings_language_help));

        if (settingsPerformanceCompatibilityDraft == null) {
            settingsPerformanceCompatibilityDraft =
                    ThorPerformanceCompatibility.isEnabled(this);
        }
        CheckBox compatibility = new CheckBox(this);
        compatibility.setText(getString(R.string.settings_performance_compatibility));
        compatibility.setTextSize(12);
        compatibility.setEnabled(ThorPerformanceCompatibility.isSupported());
        compatibility.setChecked(settingsPerformanceCompatibilityDraft);
        styleCheckBox(compatibility);
        compatibility.setOnCheckedChangeListener((button, checked) ->
                settingsPerformanceCompatibilityDraft = checked);
        content.addView(compatibility, new LinearLayout.LayoutParams(-1, dp(42)));
        addSettingsHelp(content,
                getString(R.string.settings_performance_compatibility_help));
    }

    private Button themeChoiceButton(String label, String theme) {
        Button button = actionButton(label, () -> {
            settingsThemeDraft = theme;
            refreshSettingsContent();
        });
        HeimdallUi.applyChoiceButton(this, button, theme.equals(settingsThemeDraft));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -1, 1);
        params.setMargins(dp(3), dp(3), dp(3), dp(3));
        button.setLayoutParams(params);
        return button;
    }

    private Button languageChoiceButton(int labelRes, String languageTag) {
        Button button = actionButton(getString(labelRes), () ->
                AppLanguageManager.setLanguage(this, languageTag));
        HeimdallUi.applyChoiceButton(this, button,
                languageTag.equals(AppLanguageManager.currentLanguage(this)));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -1, 1);
        params.setMargins(dp(3), dp(3), dp(3), dp(3));
        button.setLayoutParams(params);
        return button;
    }

    private void populateLayoutSettingsContent(LinearLayout content) {
        LinearLayout row1 = settingsActionRow(content);
        row1.addView(editorButton(getString(R.string.settings_layout_balanced),
                () -> previewWidgetLayout(WidgetLayout.defaultLayout())));
        row1.addView(editorButton(getString(R.string.settings_layout_controls_first),
                () -> previewWidgetLayout(WidgetLayout.fpsLayout())));
        row1.addView(editorButton(getString(R.string.settings_layout_macros_first),
                () -> previewWidgetLayout(WidgetLayout.macroFocusLayout())));

        LinearLayout row2 = settingsActionRow(content);
        row2.addView(editorButton(getString(R.string.settings_customize_modules), () -> {
            activeScreen = SCREEN_MAIN;
            editableWidgetLayout();
            showWidgetGridEditor();
        }));
    }

    private void populateTouchpadSettingsContent(LinearLayout content) {
        clearSettingsTouchpadInputs();
        TouchpadSettings draft = ensureSettingsTouchpadDraft();
        settingsTouchpadMode = TouchpadSettings.normalizeMode(draft.mode);
        LinearLayout modeRow = settingsActionRow(content);
        modeRow.addView(touchpadModeButton(getString(R.string.touch_mode_compatible),
                TouchpadSettings.MODE_TOUCH_DRAG));
        modeRow.addView(touchpadModeButton(getString(R.string.touch_mode_precision_aim),
                TouchpadSettings.MODE_RELATIVE_MOUSE));

        LinearLayout modeRow2 = settingsActionRow(content);
        modeRow2.addView(touchpadModeButton(getString(R.string.touch_mode_virtual_right_stick),
                TouchpadSettings.MODE_RIGHT_STICK));
        modeRow2.addView(touchpadModeButton(getString(R.string.touch_mode_enhanced),
                TouchpadSettings.MODE_SHIZUKU_TOUCH));

        if (!relativeMouseBackendAvailable()) {
            addSettingsHelp(content,
                    getString(R.string.touch_precision_requires_controller));
        }

        if (TouchpadSettings.MODE_RELATIVE_MOUSE.equals(settingsTouchpadMode)) {
            settingsRelativeMouseSensitivityInput = settingsSliderInput(content,
                    getString(R.string.touch_aim_sensitivity), 0.2f, 4f,
                    draft.relativeMouseSensitivity, 10, "x");
            settingsRelativeMouseMaxOutputInput = settingsSliderInput(content,
                    getString(R.string.touch_max_output), 10f, 100f,
                    Math.round(draft.relativeMouseMaxOutputPercent * 100f), 1, "%");

            addSettingsLabel(content, getString(R.string.touch_response_curve));
            settingsRelativeMouseAcceleration =
                    TouchpadSettings.normalizeRelativeMouseAcceleration(draft.relativeMouseAcceleration);
            LinearLayout accelerationRow = settingsActionRow(content);
            accelerationRow.addView(relativeMouseAccelerationButton(
                    getString(R.string.touch_curve_off),
                    TouchpadSettings.RELATIVE_MOUSE_ACCELERATION_OFF));
            accelerationRow.addView(relativeMouseAccelerationButton(
                    getString(R.string.touch_curve_gentle),
                    TouchpadSettings.RELATIVE_MOUSE_ACCELERATION_LOW));
            accelerationRow.addView(relativeMouseAccelerationButton(
                    getString(R.string.touch_curve_balanced),
                    TouchpadSettings.RELATIVE_MOUSE_ACCELERATION_MEDIUM));
            accelerationRow.addView(relativeMouseAccelerationButton(
                    getString(R.string.touch_curve_fast),
                    TouchpadSettings.RELATIVE_MOUSE_ACCELERATION_HIGH));

            settingsRelativeMouseInvertYInput = new CheckBox(this);
            settingsRelativeMouseInvertYInput.setText(
                    getString(R.string.touch_invert_vertical));
            settingsRelativeMouseInvertYInput.setTextSize(12);
            styleCheckBox(settingsRelativeMouseInvertYInput);
            settingsRelativeMouseInvertYInput.setChecked(draft.relativeMouseInvertY);
            content.addView(settingsRelativeMouseInvertYInput,
                    new LinearLayout.LayoutParams(-1, dp(42)));

            addTouchpadAdvancedToggle(content);
            if (showTouchpadAdvancedSettings) {
                settingsRelativeMousePulseInput = settingsSliderInput(content,
                    getString(R.string.touch_response_duration), 8f, 24f,
                        draft.relativeMousePulseDurationMs, 1, "ms");
            }
            addSettingsHelp(content, getString(R.string.touch_precision_help));
        } else if (TouchpadSettings.MODE_RIGHT_STICK.equals(settingsTouchpadMode)) {
            settingsRightStickCenterMode = TouchpadSettings.normalizeRightStickCenterMode(draft.rightStickCenterMode);
            addSettingsLabel(content, getString(R.string.touch_stick_center));
            LinearLayout centerModeRow = settingsActionRow(content);
            centerModeRow.addView(rightStickCenterModeButton(
                    getString(R.string.touch_center_dynamic),
                    TouchpadSettings.RIGHT_STICK_CENTER_FLOAT));
            centerModeRow.addView(rightStickCenterModeButton(
                    getString(R.string.touch_center_fixed),
                    TouchpadSettings.RIGHT_STICK_CENTER_STATIC));
            settingsRightStickSensitivityInput = settingsSliderInput(content,
                    getString(R.string.touch_sensitivity), 0.2f, 4f,
                    draft.rightStickSensitivity, 10, "x");
            settingsRightStickDeadzoneInput = settingsSliderInput(content,
                    getString(R.string.touch_deadzone), 0f, 40f,
                    Math.round(draft.rightStickDeadzone * 100f), 1, "%");
            settingsRightStickCurveInput = settingsSliderInput(content,
                    getString(R.string.touch_feel_curve), 0.5f, 3f,
                    draft.rightStickCurve, 10, "");
            settingsRightStickMaxInput = settingsSliderInput(content,
                    getString(R.string.touch_max_output), 10f, 100f,
                    Math.round(draft.rightStickMaxOutput * 100f), 1, "%");
            addTouchpadAdvancedToggle(content);
            if (showTouchpadAdvancedSettings) {
            settingsRightStickRadiusInput = settingsSliderInput(content,
                    getString(R.string.touch_operation_radius), 18f, 80f,
                    Math.round(draft.rightStickRadius * 100f), 1, "%");
            settingsRightStickRecenterInput = settingsSliderInput(content,
                    getString(R.string.touch_recenter_strength), 1f, 8f,
                    draft.rightStickRecenterBursts, 1, "");
            settingsIntervalInput = settingsSliderInput(content,
                    getString(R.string.touch_response_interval), 0f, 80f,
                    draft.intervalMs, 1, "ms");
            }
            addSettingsHelp(content, getString(R.string.touch_right_stick_help));
        } else if (TouchpadSettings.MODE_SHIZUKU_TOUCH.equals(settingsTouchpadMode)) {
            settingsShizukuTouchSensitivityXInput = settingsSliderInput(content,
                    getString(R.string.touch_horizontal_sensitivity), 0.2f, 12f,
                    draft.shizukuTouchSensitivityX, 10, "x");
            settingsShizukuTouchSensitivityYInput = settingsSliderInput(content,
                    getString(R.string.touch_vertical_sensitivity), 0.2f, 12f,
                    draft.shizukuTouchSensitivityY, 10, "x");
            settingsShizukuTouchSmoothingInput = settingsSliderInput(content,
                    getString(R.string.touch_smoothing), 0f, 90f,
                    Math.round(draft.shizukuTouchSmoothing * 100f), 1, "%");
            addTouchpadAdvancedToggle(content);
            if (showTouchpadAdvancedSettings) {
            settingsShizukuTouchFrameInput = settingsSliderInput(content,
                    getString(R.string.touch_response_interval), 4f, 33f,
                    draft.shizukuTouchFrameMs, 1, "ms");
            settingsShizukuTouchMinDeltaInput = settingsSliderInput(content,
                    getString(R.string.touch_minimum_movement), 0f, 20f,
                    draft.shizukuTouchMinDelta, 10, "px");
            settingsShizukuTouchCurveInput = settingsSliderInput(content,
                    getString(R.string.touch_feel_curve), 0.5f, 3f,
                    draft.shizukuTouchCurve, 10, "");
            settingsAnchorXInput = settingsSliderInput(content,
                    getString(R.string.touch_start_x), 5f, 95f,
                    Math.round(draft.anchorX * 100f), 1, "%");
            settingsAnchorYInput = settingsSliderInput(content,
                    getString(R.string.touch_start_y), 5f, 95f,
                    Math.round(draft.anchorY * 100f), 1, "%");
            }
            addSettingsHelp(content, getString(R.string.touch_enhanced_help));
        } else if (TouchpadSettings.MODE_TOUCH_DRAG.equals(settingsTouchpadMode)) {
            settingsSensitivityInput = settingsSliderInput(content,
                    getString(R.string.touch_sensitivity), 0.2f, 12f,
                    draft.sensitivity, 10, "x");
            addTouchpadAdvancedToggle(content);
            if (showTouchpadAdvancedSettings) {
            settingsIntervalInput = settingsSliderInput(content,
                    getString(R.string.touch_response_interval), 0f, 160f,
                    draft.intervalMs, 1, "ms");
            settingsMinDeltaInput = settingsSliderInput(content,
                    getString(R.string.touch_minimum_movement), 0f, 20f,
                    draft.minDelta, 10, "px");
            settingsAnchorXInput = settingsSliderInput(content,
                    getString(R.string.touch_start_x), 5f, 95f,
                    Math.round(draft.anchorX * 100f), 1, "%");
            settingsAnchorYInput = settingsSliderInput(content,
                    getString(R.string.touch_start_y), 5f, 95f,
                    Math.round(draft.anchorY * 100f), 1, "%");
            settingsStrokeInput = settingsSliderInput(content,
                    getString(R.string.touch_press_duration), 1f, 80f,
                    draft.strokeMs, 1, "ms");
            }
            addSettingsHelp(content, getString(R.string.touch_compatible_help));
        } else {
            addSettingsHelp(content, getString(R.string.touch_no_extra_tuning,
                    localizedTouchpadModeLabel(settingsTouchpadMode)));
        }
    }

    private void populateMacroSettingsContent(LinearLayout content) {
        settingsMacroModuleEditors.clear();
        if (settingsMacroMappingProtectionDraft == null) {
            settingsMacroMappingProtectionDraft =
                    selectedProfile.protectThorMappingDuringEnhancedTouch;
        }
        addSettingsLabel(content, getString(
                R.string.macro_settings_enhanced_touch_controller_policy));
        settingsMacroMappingProtectionInput = new CheckBox(this);
        settingsMacroMappingProtectionInput.setText(
                R.string.macro_settings_protect_thor_mapping);
        settingsMacroMappingProtectionInput.setTextSize(12);
        styleCheckBox(settingsMacroMappingProtectionInput);
        settingsMacroMappingProtectionInput.setChecked(
                settingsMacroMappingProtectionDraft);
        settingsMacroMappingProtectionInput.setOnCheckedChangeListener((button, checked) ->
                settingsMacroMappingProtectionDraft = checked);
        content.addView(settingsMacroMappingProtectionInput,
                new LinearLayout.LayoutParams(-1, dp(42)));
        addSettingsHelp(content, getString(
                R.string.macro_settings_protect_thor_mapping_help));

        WidgetLayout layout = editableWidgetLayout();
        List<WidgetLayout.Item> items = new ArrayList<>();
        for (WidgetLayout.Item item : layout.items) {
            if (WidgetLayout.TYPE_MACRO_GROUP.equals(item.type)) {
                items.add(item);
            }
        }
        if (items.isEmpty()) {
            TextView empty = text(getString(R.string.macro_settings_empty),
                    12, MUTED, false);
            content.addView(empty, new LinearLayout.LayoutParams(-1, dp(48)));
            return;
        }
        for (int i = 0; i < items.size(); i++) {
            WidgetLayout.Item item = items.get(i);
            addMacroModuleEditorCard(content, item, i);
        }
        LinearLayout row = settingsActionRow(content);
        row.addView(editorButton(getString(R.string.macro_settings_edit_buttons),
                this::showMacroPicker));
        addSettingsHelp(content, getString(R.string.macro_settings_help));
    }

    private void addMacroModuleEditorCard(LinearLayout content, WidgetLayout.Item item, int moduleIndex) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(10), dp(8), dp(10), dp(8));
        card.setBackground(HeimdallUi.isPearl(this)
                ? HeimdallUi.pearlMenuPanel(this, 10)
                : HeimdallUi.fieldPanel(this, 10));
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(-1, dp(204));
        cardParams.setMargins(0, dp(5), 0, dp(7));
        content.addView(card, cardParams);

        TextView title = text(getString(R.string.macro_settings_group, moduleIndex + 1),
                13, TEXT, true);
        card.addView(title, new LinearLayout.LayoutParams(-1, dp(24)));

        LinearLayout rangeRow = new LinearLayout(this);
        rangeRow.setOrientation(LinearLayout.HORIZONTAL);
        card.addView(rangeRow, new LinearLayout.LayoutParams(-1, dp(46)));

        NumberStepper startInput = compactNumberStepper(1, 24, item.macroStart + 1);
        rangeRow.addView(labeledNumberStepper(
                getString(R.string.macro_settings_first_button), startInput),
                new LinearLayout.LayoutParams(0, -1, 1));

        NumberStepper countInput = compactNumberStepper(1, 24, item.macroCount);
        rangeRow.addView(labeledNumberStepper(
                getString(R.string.macro_settings_button_count), countInput),
                new LinearLayout.LayoutParams(0, -1, 1));

        LinearLayout layoutRow = new LinearLayout(this);
        layoutRow.setOrientation(LinearLayout.HORIZONTAL);
        card.addView(layoutRow, new LinearLayout.LayoutParams(-1, dp(46)));

        NumberStepper columnsInput = compactNumberStepper(1, 4, item.macroColumns);
        layoutRow.addView(labeledNumberStepper(getString(R.string.common_columns),
                columnsInput), new LinearLayout.LayoutParams(0, -1, 1));

        int defaultRows = item.macroRows <= 0
                ? Math.max(1, (int) Math.ceil(item.macroCount / (float) Math.max(1, item.macroColumns)))
                : item.macroRows;
        NumberStepper rowsInput = compactNumberStepper(1, 6, defaultRows);
        layoutRow.addView(labeledNumberStepper(getString(R.string.common_rows),
                rowsInput), new LinearLayout.LayoutParams(0, -1, 1));

        CheckBox rightInput = new CheckBox(this);
        rightInput.setText(getString(R.string.macro_settings_align_right));
        rightInput.setTextSize(12);
        styleCheckBox(rightInput);
        rightInput.setChecked(item.macroRightHandPriority);
        card.addView(rightInput, new LinearLayout.LayoutParams(-1, dp(38)));

        CheckBox iconOnlyInput = new CheckBox(this);
        iconOnlyInput.setText(getString(R.string.macro_settings_icon_only));
        iconOnlyInput.setTextSize(12);
        styleCheckBox(iconOnlyInput);
        iconOnlyInput.setChecked(item.macroIconOnly);
        card.addView(iconOnlyInput, new LinearLayout.LayoutParams(-1, dp(38)));

        settingsMacroModuleEditors.add(new MacroModuleEditorBinding(
                item, startInput, countInput, columnsInput, rowsInput, rightInput,
                iconOnlyInput));
    }

    private void populateInputSettingsContent(LinearLayout content) {
        InputBackendDiagnostics.Snapshot diagnostics = showInputDiagnostics
                ? InputBackendDiagnostics.inspect(this) : null;
        NativeGamepadPath.Device nativeDevice = NativeGamepadPath.resolveDevice();
        boolean accessibilityReady = ThorAccessibilityService.isReady();
        boolean shizukuReady = ShizukuNativeController.isReady();
        boolean nativeReady = nativeDevice != null && shizukuReady;
        InputBridge.BackendOption accessibilityOption = null;
        InputBridge.BackendOption shizukuOption = null;
        InputBridge.BackendOption[] options = InputBridge.backendOptions(this);
        for (InputBridge.BackendOption option : options) {
            if (InputBridge.BACKEND_ACCESSIBILITY.equals(option.id)) {
                accessibilityOption = option;
            } else if (InputBridge.BACKEND_SHIZUKU.equals(option.id)) {
                shizukuOption = option;
            }
        }
        String selectedBackend = InputBridge.selectedBackendId(this);
        boolean accessibilitySelected = InputBridge.BACKEND_ACCESSIBILITY.equals(selectedBackend);
        boolean shizukuSelected = InputBridge.BACKEND_SHIZUKU.equals(selectedBackend);

        addSettingsInfoCard(content, getString(R.string.connection_capabilities),
                getString(R.string.connection_capability_summary,
                        getString(accessibilityReady ? R.string.connection_available
                                : R.string.connection_basic_required),
                        getString(nativeReady ? R.string.connection_available
                                : R.string.connection_controller_required)),
                accessibilityReady && nativeReady
                        ? HeimdallUi.SEMANTIC_SUCCESS : HeimdallUi.SEMANTIC_WARNING);

        addSettingsLabel(content, getString(R.string.connection_methods));
        if (accessibilityOption != null) {
            InputBridge.BackendOption option = accessibilityOption;
            addConnectionSettingsOption(content, getString(R.string.connection_basic_touch),
                    getString(R.string.connection_basic_touch_summary),
                    accessibilitySelected, accessibilityReady,
                    accessibilitySelected
                            ? getString(accessibilityReady
                                    ? R.string.connection_manage_permission
                                    : R.string.connection_enable)
                            : getString(R.string.connection_use_this),
                    true, () -> {
                        if (!accessibilitySelected) {
                            selectInputBackendFromSettings(option);
                            if (accessibilityReady) {
                                return;
                            }
                        }
                        showAction(getString(accessibilityReady
                                ? R.string.connection_accessibility_manage
                                : R.string.connection_accessibility_enable));
                        InputBridge.openSettings(this);
                    });
        }
        if (shizukuOption != null) {
            InputBridge.BackendOption option = shizukuOption;
            String actionLabel = shizukuSelected && shizukuReady
                    ? getString(nativeReady ? R.string.connection_connected
                            : R.string.connection_waiting_controller)
                    : getString(shizukuReady ? R.string.connection_use_this
                            : R.string.connection_connect);
            addConnectionSettingsOption(content,
                    getString(R.string.connection_controller_enhancement),
                    getString(R.string.connection_controller_summary),
                    shizukuSelected, nativeReady, actionLabel,
                    !(shizukuSelected && shizukuReady), () -> {
                        if (!ShizukuNativeController.isBinderAlive()) {
                            showErrorAction(getString(R.string.connection_start_shizuku));
                            return;
                        }
                        if (!shizukuSelected || !ShizukuNativeController.isPermissionGranted()) {
                            selectInputBackendFromSettings(option);
                            return;
                        }
                        showAction(getString(R.string.connection_controller_connected));
                    });
        }

        LinearLayout detailsRow = settingsActionRow(content);
        detailsRow.addView(editorButton(showInputDiagnostics
                ? getString(R.string.connection_collapse_technical_details)
                : getString(R.string.connection_technical_details), () -> {
            showInputDiagnostics = !showInputDiagnostics;
            refreshSettingsContent();
        }));

        if (showInputDiagnostics) {
            addSettingsInfoCard(content, getString(R.string.connection_current_route),
                    getString(R.string.connection_route_summary,
                            InputBridge.backendName(this),
                            NativeGamepadPath.statusLabel(this, shizukuReady)),
                    HeimdallUi.SEMANTIC_NEUTRAL);
            addSettingsInfoCard(content, getString(R.string.connection_controller_device),
                    NativeGamepadPath.debugLabel(this, shizukuReady)
                            + "\n" + NativeGamepadPath.permissionHint(this, shizukuReady),
                    nativeReady ? HeimdallUi.SEMANTIC_NEUTRAL : HeimdallUi.SEMANTIC_WARNING);
            TextView diagnosticView = text(inputDiagnosticText(diagnostics), 11, TEXT, false);
            diagnosticView.setGravity(Gravity.TOP | Gravity.LEFT);
            diagnosticView.setPadding(dp(8), dp(6), dp(8), dp(6));
            diagnosticView.setBackground(HeimdallUi.insetPanel(this, 8));
            content.addView(diagnosticView, new LinearLayout.LayoutParams(-1, dp(124)));

            LinearLayout diagnosticRow = settingsActionRow(content);
            diagnosticRow.addView(editorButton(
                    getString(R.string.connection_refresh_details), () -> {
                diagnosticView.setText(inputDiagnosticText(InputBackendDiagnostics.inspect(this)));
                showDebugAction(getString(R.string.connection_details_refreshed));
            }));
        }
    }

    private void populateDiagnosticsSettingsContent(LinearLayout content) {
        addSettingsInfoCard(content, getString(R.string.diagnostics_last_exit),
                HeimdallStabilityDiagnostics.previousExitSummary(this),
                HeimdallUi.SEMANTIC_NEUTRAL);
        addSettingsLabel(content, getString(R.string.diagnostics_export_title));
        addSettingsHelp(content, getString(R.string.diagnostics_export_help));
        addSettingsHelp(content, getString(R.string.diagnostics_privacy_help));
        LinearLayout row = settingsActionRow(content);
        row.addView(editorButton(getString(R.string.diagnostics_export_action),
                this::requestDiagnosticExport));
    }

    private void requestDiagnosticExport() {
        String timestamp = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US)
                .format(new Date());
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TITLE, "heimdall-diagnostics-" + timestamp + ".txt");
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        try {
            startActivityForResult(intent, REQUEST_DIAGNOSTIC_EXPORT);
        } catch (Exception error) {
            showErrorAction(getString(R.string.diagnostics_export_picker_error));
        }
    }

    private void writeDiagnosticReport(Uri uri) {
        showAction(getString(R.string.diagnostics_export_preparing));
        new Thread(() -> {
            boolean success = false;
            try (OutputStream output = getContentResolver().openOutputStream(uri, "w")) {
                if (output != null) {
                    output.write(stabilityDiagnostics.buildReport(this)
                            .getBytes(StandardCharsets.UTF_8));
                    output.flush();
                    success = true;
                }
            } catch (Exception error) {
                android.util.Log.w(HeimdallStabilityDiagnostics.TAG,
                        "diagnostic export failed", error);
            }
            boolean exported = success;
            runOnUiThread(() -> {
                if (!isFinishing() && !isDestroyed()) {
                    if (exported) {
                        showAction(getString(R.string.diagnostics_export_complete));
                    } else {
                        showErrorAction(getString(R.string.diagnostics_export_error));
                    }
                }
            });
        }, "heimdall-diagnostic-export").start();
    }

    private void populateProfileSettingsContent(LinearLayout content) {
        ThorAccessibilityService detectionService = ThorAccessibilityService.getInstance();
        if (detectionService != null && ForegroundAppTracker.isEnabled(this)) {
            detectionService.refreshForegroundApp();
        }
        CheckBox autoSwitchInput = new CheckBox(this);
        autoSwitchInput.setText(getString(R.string.profile_auto_switch));
        autoSwitchInput.setTextSize(12);
        styleCheckBox(autoSwitchInput);
        autoSwitchInput.setChecked(ForegroundAppTracker.isEnabled(this));
        autoSwitchInput.setOnCheckedChangeListener((button, checked) -> {
            ForegroundAppTracker.setEnabled(this, checked);
            updateProfileAwarenessRegistration();
            if (checked) {
                maybeAutoSwitchProfile(ForegroundAppTracker.latest());
            }
        });
        content.addView(autoSwitchInput, new LinearLayout.LayoutParams(-1, dp(42)));

        ForegroundAppTracker.Snapshot observed = ForegroundAppTracker.latest();
        TextView detectionStatus = addSettingsInfoCard(content,
                getString(R.string.profile_upper_screen_binding),
                profileDetectionSummary(observed),
                observed == null ? HeimdallUi.SEMANTIC_WARNING : HeimdallUi.SEMANTIC_SUCCESS);
        final TextView[] detectionDetails = new TextView[1];
        LinearLayout detectionActions = settingsActionRow(content);
        detectionActions.addView(editorButton(
                getString(R.string.profile_use_current_upper_screen), () -> {
            ThorAccessibilityService service = ThorAccessibilityService.getInstance();
            if (service != null) {
                service.refreshForegroundApp();
            }
            ForegroundAppTracker.Snapshot latest = ForegroundAppTracker.latest();
            detectionStatus.setText(profileDetectionSummary(latest));
            if (detectionDetails[0] != null) {
                detectionDetails[0].setText(profileDetectionText(latest));
            }
            if (latest == null) {
            showErrorAction(getString(R.string.profile_no_upper_screen_app));
                return;
            }
            settingsProfilePackageInput.setText(latest.packageName);
            if (hasUsableRomTitle(latest)) {
                settingsProfileRomInput.setText(latest.windowTitle);
            }
        }));
        detectionActions.addView(editorButton(showProfileDetectionDetails
                ? getString(R.string.profile_collapse_recognition_details)
                : getString(R.string.profile_recognition_details),
                this::toggleProfileDetectionDetails));
        if (showProfileDetectionDetails) {
            detectionDetails[0] = addSettingsInfoCard(content,
                    getString(R.string.profile_recognition_details),
                    profileDetectionText(observed), HeimdallUi.SEMANTIC_NEUTRAL);
        }

        addSettingsLabel(content, getString(R.string.profile_name));

        settingsProfileNameInput = settingsEditText(selectedProfile.name);
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(-1, dp(42));
        nameParams.setMargins(0, 0, 0, dp(8));
        content.addView(settingsProfileNameInput, nameParams);

        addSettingsLabel(content, getString(R.string.profile_associated_game));

        settingsProfilePackageInput = settingsEditText(selectedProfile.packageHint);
        settingsProfilePackageInput.setHint(getString(R.string.profile_package_hint));
        settingsProfilePackageInput.setHintTextColor(HeimdallUi.mutedTextColor(this));
        LinearLayout.LayoutParams packageParams = new LinearLayout.LayoutParams(-1, dp(42));
        packageParams.setMargins(0, 0, 0, dp(8));
        content.addView(settingsProfilePackageInput, packageParams);

        LinearLayout recentAppRow = settingsActionRow(content);
        recentAppRow.addView(editorButton(getString(R.string.profile_choose_recent_app), () ->
                showRecentAppPicker(settingsProfilePackageInput)));

        addSettingsLabel(content, getString(R.string.profile_emulator_game_optional));

        settingsProfileRomInput = settingsEditText(selectedProfile.romContextHint);
        settingsProfileRomInput.setHint(getString(R.string.profile_rom_hint));
        settingsProfileRomInput.setHintTextColor(HeimdallUi.mutedTextColor(this));
        LinearLayout.LayoutParams romParams = new LinearLayout.LayoutParams(-1, dp(42));
        romParams.setMargins(0, 0, 0, dp(4));
        content.addView(settingsProfileRomInput, romParams);

        settingsProfileDefaultInput = new CheckBox(this);
        settingsProfileDefaultInput.setText(getString(R.string.profile_default_for_app));
        settingsProfileDefaultInput.setTextSize(12);
        styleCheckBox(settingsProfileDefaultInput);
        settingsProfileDefaultInput.setChecked(selectedProfile.defaultForPackage);
        content.addView(settingsProfileDefaultInput, new LinearLayout.LayoutParams(-1, dp(40)));

        addSettingsHelp(content, getString(R.string.profile_binding_help));

        LinearLayout manageRow = settingsActionRow(content);
        manageRow.addView(editorButton(getString(R.string.profile_create), () -> {
            addBlankProfile();
            rebuildContent();
        }));

        addSettingsLabel(content, getString(R.string.profile_data_management));
        LinearLayout ioRow = settingsActionRow(content);
        ioRow.addView(editorButton(getString(R.string.profile_export),
                this::showProfileExportDialog));
        ioRow.addView(editorButton(getString(R.string.profile_import),
                () -> showProfileImportDialog(null)));

        addSettingsLabel(content, getString(R.string.profile_list));
        for (int i = 0; i < profiles.size(); i++) {
            content.addView(settingsProfileRow(profiles.get(i), i));
        }
        addProfileSnapshotSettings(content);
    }

    private void toggleProfileDetectionDetails() {
        String name = settingsProfileNameInput == null ? null
                : settingsProfileNameInput.getText().toString();
        String packageHint = settingsProfilePackageInput == null ? null
                : settingsProfilePackageInput.getText().toString();
        String romHint = settingsProfileRomInput == null ? null
                : settingsProfileRomInput.getText().toString();
        Boolean defaultForPackage = settingsProfileDefaultInput == null ? null
                : settingsProfileDefaultInput.isChecked();
        showProfileDetectionDetails = !showProfileDetectionDetails;
        refreshSettingsContent();
        if (name != null && settingsProfileNameInput != null) {
            settingsProfileNameInput.setText(name);
        }
        if (packageHint != null && settingsProfilePackageInput != null) {
            settingsProfilePackageInput.setText(packageHint);
        }
        if (romHint != null && settingsProfileRomInput != null) {
            settingsProfileRomInput.setText(romHint);
        }
        if (defaultForPackage != null && settingsProfileDefaultInput != null) {
            settingsProfileDefaultInput.setChecked(defaultForPackage);
        }
    }

    private View settingsProfileRow(GameProfile profile, int index) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, 0, 0, 0);

        boolean selected = index == selectedProfileIndex;
        Button switchButton = actionButton((selected ? "\u25cf " : "\u25cb ")
                + nonEmpty(profile.name, "Profile"), () -> selectProfile(index));
        switchButton.setTextSize(13);
        switchButton.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
        switchButton.setSingleLine(true);
        switchButton.setEllipsize(android.text.TextUtils.TruncateAt.END);
        switchButton.setPadding(dp(12), 0, dp(12), 0);
        switchButton.setTextColor(selected ? HeimdallUi.accent(this) : HeimdallUi.textColor(this));
        HeimdallUi.applyChoiceButton(this, switchButton, selected);
        LinearLayout.LayoutParams switchParams = new LinearLayout.LayoutParams(0, dp(48), 1);
        switchParams.setMargins(0, dp(5), dp(6), dp(5));
        row.addView(switchButton, switchParams);

        Button copyButton = compactButton(getString(R.string.common_copy), () -> {
            addProfileFrom(profile);
            rebuildContent();
        });
        LinearLayout.LayoutParams copyParams = new LinearLayout.LayoutParams(dp(58), dp(48));
        copyParams.setMargins(0, dp(5), dp(4), dp(5));
        row.addView(copyButton, copyParams);

        Button deleteButton = compactButton(getString(R.string.common_delete),
                () -> confirmDeleteProfile(index));
        deleteButton.setEnabled(profiles.size() > 1);
        deleteButton.setAlpha(deleteButton.isEnabled() ? 1f : 0.45f);
        LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(dp(58), dp(48));
        deleteParams.setMargins(0, dp(5), 0, dp(5));
        row.addView(deleteButton, deleteParams);

        return row;
    }

    private void confirmDeleteProfile(int index) {
        if (profiles.size() <= 1) {
            showErrorAction(getString(R.string.profile_keep_one));
            return;
        }
        if (index < 0 || index >= profiles.size()) {
            return;
        }
        String name = nonEmpty(profiles.get(index).name,
                getString(R.string.common_profile_fallback));
        showSettingsDecisionPanel(getString(R.string.profile_delete_title),
                getString(R.string.profile_delete_message, name),
                null, null, getString(R.string.common_delete), () -> {
                    deleteProfile(index);
                    refreshSettingsContent();
                });
    }

    private void addProfileSnapshotSettings(LinearLayout content) {
        List<ProfileSnapshotStore.SnapshotInfo> snapshots =
                ProfileSnapshotStore.listSnapshots(this);
        String body = getString(R.string.profile_backup_summary,
                getResources().getQuantityString(R.plurals.restore_point_count,
                        snapshots.size(), snapshots.size()));
        addSettingsInfoCard(content, getString(R.string.profile_backup_restore), body,
                snapshots.isEmpty() ? HeimdallUi.SEMANTIC_WARNING : HeimdallUi.SEMANTIC_SUCCESS);

        if (snapshots.isEmpty()) {
            addSettingsHelp(content, getString(R.string.profile_no_restore_points));
            return;
        }
        for (ProfileSnapshotStore.SnapshotInfo snapshot : snapshots) {
            content.addView(settingsProfileSnapshotRow(snapshot));
        }
    }

    private View settingsProfileSnapshotRow(ProfileSnapshotStore.SnapshotInfo snapshot) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
                .format(new Date(snapshot.createdAt));
        String subtitle = snapshot.readable
                ? getResources().getQuantityString(R.plurals.profile_count,
                        snapshot.profileCount, snapshot.profileCount)
                : getString(R.string.profile_snapshot_raw_only);
        LinearLayout summary = new LinearLayout(this);
        summary.setOrientation(LinearLayout.VERTICAL);
        summary.setGravity(Gravity.CENTER_VERTICAL);
        summary.setPadding(dp(12), 0, dp(12), 0);
        summary.setBackground(HeimdallUi.fieldPanel(this, 9));
        TextView summaryTitle = text(snapshotReasonLabel(snapshot.reason) + "  " + timestamp,
                12, TEXT, true);
        summary.addView(summaryTitle, new LinearLayout.LayoutParams(-1, dp(26)));
        TextView summarySubtitle = text(subtitle, 11, MUTED, false);
        summary.addView(summarySubtitle, new LinearLayout.LayoutParams(-1, dp(22)));
        summary.setAlpha(snapshot.readable ? 1f : 0.55f);
        LinearLayout.LayoutParams summaryParams = new LinearLayout.LayoutParams(0, dp(58), 1);
        summaryParams.setMargins(0, dp(5), dp(6), dp(5));
        row.addView(summary, summaryParams);

        Button restore = compactButton(getString(R.string.common_restore),
                () -> confirmRestoreProfileSnapshot(snapshot));
        restore.setEnabled(snapshot.readable);
        restore.setAlpha(snapshot.readable ? 1f : 0.45f);
        LinearLayout.LayoutParams restoreParams = new LinearLayout.LayoutParams(dp(58), dp(48));
        restoreParams.setMargins(0, dp(10), 0, dp(10));
        row.addView(restore, restoreParams);
        return row;
    }

    private String snapshotReasonLabel(String reason) {
        if (ProfileSnapshotStore.REASON_DELETE.equals(reason)) {
            return getString(R.string.profile_snapshot_before_delete);
        }
        if (ProfileSnapshotStore.REASON_REPLACE_ALL.equals(reason)) {
            return getString(R.string.profile_snapshot_before_replace);
        }
        if (ProfileSnapshotStore.REASON_RESTORE.equals(reason)) {
            return getString(R.string.profile_snapshot_before_restore);
        }
        if (ProfileSnapshotStore.REASON_SANITIZE.equals(reason)) {
            return getString(R.string.profile_snapshot_before_repair);
        }
        return getString(R.string.profile_snapshot_original_protection);
    }

    private void confirmRestoreProfileSnapshot(ProfileSnapshotStore.SnapshotInfo snapshot) {
        if (snapshot == null || !snapshot.readable) {
            showErrorAction(getString(R.string.profile_snapshot_incomplete));
            return;
        }
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
                .format(new Date(snapshot.createdAt));
        showSettingsDecisionPanel(getString(R.string.profile_restore_title),
                getString(R.string.profile_restore_message,
                        snapshotReasonLabel(snapshot.reason) + "  " + timestamp,
                        getResources().getQuantityString(R.plurals.profile_count,
                                snapshot.profileCount, snapshot.profileCount),
                        getResources().getQuantityString(R.plurals.profile_count,
                                profiles.size(), profiles.size())),
                null, null, getString(R.string.common_restore),
                () -> restoreProfileSnapshot(snapshot));
    }

    private void restoreProfileSnapshot(ProfileSnapshotStore.SnapshotInfo snapshot) {
        List<GameProfile> restored;
        try {
            restored = ProfileSnapshotStore.readSnapshot(this, snapshot);
        } catch (Exception ex) {
            showErrorAction(getString(R.string.profile_restore_read_failed));
            return;
        }
        if (!protectProfilesBefore(ProfileSnapshotStore.REASON_RESTORE)) {
            return;
        }
        profiles.clear();
        profiles.addAll(restored);
        selectedProfileIndex = Math.max(0,
                Math.min(snapshot.selectedIndex, profiles.size() - 1));
        finishProfileImport(getString(R.string.profile_snapshot_restored,
                getResources().getQuantityString(R.plurals.profile_count,
                        restored.size(), restored.size())));
    }

    private void populateMagnifierSettingsContent(LinearLayout content) {
        WidgetLayout.Item draft = ensureSettingsMagnifierDraft();
        if (draft == null) {
            addSettingsInfoCard(content, getString(R.string.magnifier_missing_title),
                    getString(R.string.magnifier_missing_help),
                    HeimdallUi.SEMANTIC_WARNING);
            LinearLayout addRow = settingsActionRow(content);
            addRow.addView(editorButton(getString(R.string.settings_customize_modules), () -> {
                activeScreen = SCREEN_MAIN;
                editableWidgetLayout();
                showWidgetGridEditor();
            }));
            return;
        }

        addSettingsLabel(content, getString(R.string.magnifier_shape));
        LinearLayout shapeRow = settingsActionRow(content);
        shapeRow.addView(magnifierShapeButton(
                getString(R.string.magnifier_shape_rectangle),
                WidgetLayout.MAGNIFIER_SHAPE_RECTANGLE));
        shapeRow.addView(magnifierShapeButton(
                getString(R.string.magnifier_shape_circle),
                WidgetLayout.MAGNIFIER_SHAPE_CIRCLE));

        addSettingsLabel(content, getString(R.string.magnifier_frame_rate));
        LinearLayout fpsRow = settingsActionRow(content);
        fpsRow.addView(magnifierFpsButton("15 FPS", 15));
        fpsRow.addView(magnifierFpsButton("30 FPS", 30));
        fpsRow.addView(magnifierFpsButton("60 FPS", 60));

        addSettingsLabel(content, getString(R.string.magnifier_zoom));
        LinearLayout zoomRow = settingsActionRow(content);
        zoomRow.addView(magnifierZoomButton("1.0x", 1f));
        zoomRow.addView(magnifierZoomButton("1.2x", 1.2f));
        zoomRow.addView(magnifierZoomButton("1.5x", 1.5f));
        zoomRow.addView(magnifierZoomButton("2.0x", 2f));

        String projectionStatus = getString(UpperScreenProjectionService.isRunning()
                ? (UpperScreenProjectionService.isFrozen()
                        ? R.string.magnifier_status_paused
                        : R.string.magnifier_status_running)
                : R.string.magnifier_status_stopped);
        addSettingsInfoCard(content, getString(R.string.magnifier_current_status),
                getString(R.string.magnifier_status_summary, projectionStatus,
                        draft.magnifierFps, magnifierZoomLabel(draft.magnifierZoom)),
                UpperScreenProjectionService.isRunning()
                        ? HeimdallUi.SEMANTIC_SUCCESS : HeimdallUi.SEMANTIC_NEUTRAL);

        if (UpperScreenProjectionService.isActiveOrStarting()) {
            LinearLayout actionRow = settingsActionRow(content);
            actionRow.addView(editorButton(getString(R.string.magnifier_stop), () -> {
                stopMagnifierProjection();
                uiHandler.postDelayed(this::refreshSettingsContent, 300L);
            }));
        }

        addSettingsHelp(content, getString(WidgetLayout.isCircularMagnifier(draft)
                ? R.string.magnifier_settings_help_circle
                : R.string.magnifier_settings_help_rectangle));
    }

    private WidgetLayout.Item ensureSettingsMagnifierDraft() {
        if (settingsMagnifierDraft == null) {
            WidgetLayout.Item current = currentWidgetLayout().findItem(WidgetLayout.TYPE_MAGNIFIER);
            if (current != null) {
                settingsMagnifierDraft = current.copy();
            }
        }
        return settingsMagnifierDraft;
    }

    private Button magnifierFpsButton(String label, int fps) {
        Button button = editorButton(label, () -> {
            WidgetLayout.Item draft = ensureSettingsMagnifierDraft();
            if (draft != null) {
                draft.magnifierFps = WidgetLayout.normalizeMagnifierFps(fps);
                refreshSettingsContent();
            }
        });
        WidgetLayout.Item draft = ensureSettingsMagnifierDraft();
        HeimdallUi.applyChoiceButton(this, button,
                draft != null && draft.magnifierFps == fps);
        return button;
    }

    private Button magnifierShapeButton(String label, String shape) {
        String normalizedShape = WidgetLayout.normalizeMagnifierShape(shape);
        Button button = editorButton(label, () -> {
            WidgetLayout.Item draft = ensureSettingsMagnifierDraft();
            if (draft != null) {
                draft.magnifierShape = normalizedShape;
                refreshSettingsContent();
            }
        });
        WidgetLayout.Item draft = ensureSettingsMagnifierDraft();
        HeimdallUi.applyChoiceButton(this, button, draft != null
                && normalizedShape.equals(
                        WidgetLayout.normalizeMagnifierShape(draft.magnifierShape)));
        return button;
    }

    private Button magnifierZoomButton(String label, float zoom) {
        Button button = editorButton(label, () -> {
            WidgetLayout.Item draft = ensureSettingsMagnifierDraft();
            if (draft != null) {
                draft.magnifierZoom = WidgetLayout.normalizeMagnifierZoom(zoom);
                refreshSettingsContent();
            }
        });
        WidgetLayout.Item draft = ensureSettingsMagnifierDraft();
        HeimdallUi.applyChoiceButton(this, button,
                draft != null && Math.abs(draft.magnifierZoom - zoom) < 0.01f);
        return button;
    }

    private String magnifierZoomLabel(float zoom) {
        return String.format(java.util.Locale.US, "%.1fx",
                WidgetLayout.normalizeMagnifierZoom(zoom));
    }

    private boolean applySettingsMagnifierDraft() {
        if (settingsMagnifierDraft == null) {
            return false;
        }
        WidgetLayout.Item target = currentWidgetLayout().findItem(WidgetLayout.TYPE_MAGNIFIER);
        if (target == null) {
            return false;
        }
        String oldShape = WidgetLayout.normalizeMagnifierShape(target.magnifierShape);
        String newShape = WidgetLayout.normalizeMagnifierShape(
                settingsMagnifierDraft.magnifierShape);
        target.magnifierFps = WidgetLayout.normalizeMagnifierFps(
                settingsMagnifierDraft.magnifierFps);
        target.magnifierZoom = WidgetLayout.normalizeMagnifierZoom(
                settingsMagnifierDraft.magnifierZoom);
        target.magnifierShape = newShape;
        boolean shapeChanged = !oldShape.equals(newShape);
        if (shapeChanged && WidgetLayout.MAGNIFIER_SHAPE_CIRCLE.equals(newShape)
                && UpperScreenProjectionService.isFrozen()
                && !magnifierRegionCaptureInProgress) {
            UpperScreenProjectionService.setFrozen(false);
        }
        UpperScreenProjectionService.setTuning(
                WidgetLayout.magnifierTargetAspectRatio(target),
                target.magnifierFps, target.magnifierZoom);
        return shapeChanged;
    }

    private void applyProfileSettingsInputs() {
        if (settingsProfileNameInput == null || settingsProfilePackageInput == null
                || settingsProfileRomInput == null || settingsProfileDefaultInput == null) {
            return;
        }
        selectedProfile.name = nonEmpty(settingsProfileNameInput.getText().toString(), selectedProfile.name);
        selectedProfile.mode = "\u901a\u7528";
        selectedProfile.packageHint = settingsProfilePackageInput.getText().toString().trim();
        selectedProfile.romContextHint = settingsProfileRomInput.getText().toString().trim();
        selectedProfile.defaultForPackage = settingsProfileDefaultInput.isChecked()
                && selectedProfile.packageHint.length() > 0;
        if (selectedProfile.defaultForPackage) {
            for (GameProfile profile : profiles) {
                if (profile != selectedProfile
                        && selectedProfile.packageHint.equalsIgnoreCase(profile.packageHint)) {
                    profile.defaultForPackage = false;
                }
            }
        }
    }

    private String profileDetectionText(ForegroundAppTracker.Snapshot snapshot) {
        if (snapshot == null) {
            return getString(R.string.profile_detection_unavailable_detail);
        }
        String title = snapshot.windowTitle.length() == 0
                ? getString(R.string.profile_detection_window_title_unavailable)
                : snapshot.windowTitle;
        String activity = snapshot.className.length() == 0
                ? getString(R.string.profile_detection_activity_unknown)
                : snapshot.className;
        return getString(R.string.profile_detection_detail,
                snapshot.packageName, title, activity);
    }

    private String profileDetectionSummary(ForegroundAppTracker.Snapshot snapshot) {
        if (snapshot == null) {
            return getString(R.string.profile_detection_unavailable_summary);
        }
        if (hasUsableRomTitle(snapshot)) {
            return getString(R.string.profile_detection_app_and_game_summary);
        }
        return getString(R.string.profile_detection_app_only_summary);
    }

    private static boolean hasUsableRomTitle(ForegroundAppTracker.Snapshot snapshot) {
        return snapshot != null && snapshot.windowTitle.length() > 0
                && snapshot.isUpperOrUnknownDisplay();
    }

    private LinearLayout settingsActionRow(LinearLayout content) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        content.addView(row, new LinearLayout.LayoutParams(-1, dp(48)));
        return row;
    }

    private void addSettingsLabel(LinearLayout content, String label) {
        TextView title = text(label, 12, MUTED, true);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(24));
        params.setMargins(0, dp(2), 0, 0);
        content.addView(title, params);
    }

    private void addConnectionSettingsOption(LinearLayout content, String title, String summary,
                                             boolean selected, boolean ready,
                                             String actionLabel, boolean actionEnabled,
                                             Runnable action) {
        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        copy.setPadding(dp(12), dp(9), dp(12), dp(9));
        copy.setBackground(HeimdallUi.fieldPanel(this, 10));
        LinearLayout.LayoutParams copyParams = new LinearLayout.LayoutParams(-1, -2);
        copyParams.setMargins(0, dp(4), 0, 0);
        content.addView(copy, copyParams);

        int stateRes = selected
                ? (ready ? R.string.connection_state_current_ready
                        : R.string.connection_state_current_setup)
                : (ready ? R.string.connection_state_available_ready
                        : R.string.connection_state_available_setup);
        TextView titleView = text(getString(R.string.connection_option_title,
                        title, getString(stateRes)), 12,
                selected ? HeimdallUi.accent(this) : TEXT, true);
        copy.addView(titleView, new LinearLayout.LayoutParams(-1, -2));
        TextView summaryView = text(summary, 11, MUTED, false);
        LinearLayout.LayoutParams summaryParams = new LinearLayout.LayoutParams(-1, -2);
        summaryParams.setMargins(0, dp(3), 0, 0);
        copy.addView(summaryView, summaryParams);

        Button button = editorButton(actionLabel, action);
        button.setEnabled(actionEnabled);
        button.setAlpha(actionEnabled ? 1f : 0.45f);
        button.setSingleLine(false);
        button.setMinHeight(dp(52));
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(-1, -2);
        buttonParams.setMargins(0, dp(4), 0, dp(8));
        content.addView(button, buttonParams);
    }

    private void addTouchpadAdvancedToggle(LinearLayout content) {
        LinearLayout row = settingsActionRow(content);
        row.addView(editorButton(showTouchpadAdvancedSettings
                ? getString(R.string.touch_collapse_advanced_tuning)
                : getString(R.string.touch_advanced_tuning), () -> {
            applySettingsTouchpadInputs();
            showTouchpadAdvancedSettings = !showTouchpadAdvancedSettings;
            refreshSettingsContent();
        }));
    }

    private EditText settingsEditText(String value) {
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setText(value == null ? "" : value);
        input.setTextColor(HeimdallUi.textColor(this));
        input.setTextSize(12);
        input.setBackground(HeimdallUi.fieldPanel(this, 8));
        input.setPadding(dp(10), 0, dp(10), 0);
        bindTextInputFocus(input);
        return input;
    }

    private Button touchpadModeButton(String label, String mode) {
        Button button = editorButton(label, () -> selectTouchpadModeDraft(mode));
        boolean selected = mode.equals(settingsTouchpadMode);
        HeimdallUi.applyChoiceButton(this, button, selected);
        if (TouchpadSettings.MODE_RELATIVE_MOUSE.equals(mode)
                && !relativeMouseBackendAvailable()) {
            button.setEnabled(false);
            button.setAlpha(0.45f);
            button.setContentDescription(getString(
                    R.string.touch_precision_requires_controller_description));
        }
        return button;
    }

    private String localizedTouchpadModeLabel(String mode) {
        String normalized = TouchpadSettings.normalizeMode(mode);
        if (TouchpadSettings.MODE_RELATIVE_MOVE.equals(normalized)) {
            return getString(R.string.touch_mode_relative_move);
        }
        if (TouchpadSettings.MODE_MOUSE_POINTER.equals(normalized)) {
            return getString(R.string.touch_mode_mouse_pointer);
        }
        if (TouchpadSettings.MODE_RIGHT_STICK.equals(normalized)) {
            return getString(R.string.touch_mode_virtual_right_stick);
        }
        if (TouchpadSettings.MODE_SHIZUKU_TOUCH.equals(normalized)) {
            return getString(R.string.touch_mode_enhanced);
        }
        if (TouchpadSettings.MODE_RELATIVE_MOUSE.equals(normalized)) {
            return getString(R.string.touch_mode_precision_aim);
        }
        return getString(R.string.touch_mode_compatible);
    }

    private TouchpadSettings ensureSettingsTouchpadDraft() {
        if (settingsTouchpadDraft == null) {
            settingsTouchpadDraft = selectedProfile.safeTouchpadSettings().copy();
        }
        return settingsTouchpadDraft;
    }

    private void selectTouchpadModeDraft(String mode) {
        if (!touchpadModeAvailable(mode)) {
            if (TouchpadSettings.MODE_RELATIVE_MOUSE.equals(mode)) {
                showErrorAction(getString(R.string.action_controller_enhancement_required));
                return;
            }
            showErrorAction(getString(R.string.action_connection_setup_required));
            return;
        }
        applySettingsTouchpadInputs();
        ensureSettingsTouchpadDraft().mode = mode;
        settingsTouchpadMode = TouchpadSettings.normalizeMode(mode);
        refreshSettingsContent();
    }

    private void clearSettingsTouchpadInputs() {
        settingsSensitivityInput = null;
        settingsIntervalInput = null;
        settingsMinDeltaInput = null;
        settingsAnchorXInput = null;
        settingsAnchorYInput = null;
        settingsStrokeInput = null;
        settingsShizukuTouchSensitivityXInput = null;
        settingsShizukuTouchSensitivityYInput = null;
        settingsShizukuTouchFrameInput = null;
        settingsShizukuTouchMinDeltaInput = null;
        settingsShizukuTouchCurveInput = null;
        settingsShizukuTouchSmoothingInput = null;
        settingsRightStickSensitivityInput = null;
        settingsRightStickDeadzoneInput = null;
        settingsRightStickCurveInput = null;
        settingsRightStickMaxInput = null;
        settingsRightStickRadiusInput = null;
        settingsRightStickRecenterInput = null;
        settingsRightStickCenterMode = null;
        settingsRelativeMouseSensitivityInput = null;
        settingsRelativeMouseMaxOutputInput = null;
        settingsRelativeMousePulseInput = null;
        settingsRelativeMouseInvertYInput = null;
        settingsRelativeMouseAcceleration = null;
    }

    private void addSettingsHelp(LinearLayout content, String message) {
        TextView help = text(message, 11, MUTED, false);
        help.setGravity(Gravity.TOP | Gravity.LEFT);
        help.setMinHeight(dp(34));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(0, dp(4), 0, dp(8));
        content.addView(help, params);
    }

    private TextView addSettingsInfoCard(LinearLayout content, String title, String body, int semantic) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(10), dp(8), dp(10), dp(8));
        card.setMinimumHeight(dp(70));
        HeimdallUi.applySemanticPanel(this, card, semantic);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(0, dp(4), 0, dp(8));
        content.addView(card, params);

        TextView titleView = text(title, 12, TEXT, true);
        card.addView(titleView, new LinearLayout.LayoutParams(-1, dp(22)));

        TextView bodyView = text(body, 11, MUTED, false);
        bodyView.setGravity(Gravity.TOP | Gravity.LEFT);
        card.addView(bodyView, new LinearLayout.LayoutParams(-1, -2));
        return bodyView;
    }

    private void showSettingsDecisionPanel(String title, String message,
                                           String secondaryLabel, Runnable secondaryAction,
                                           String primaryLabel, Runnable primaryAction) {
        showSettingsDecisionPanel(title, message, secondaryLabel, secondaryAction,
                primaryLabel, primaryAction, null);
    }

    private PanelOverlay showSettingsDecisionPanel(String title, String message,
                                           String secondaryLabel, Runnable secondaryAction,
                                           String primaryLabel, Runnable primaryAction,
                                           Runnable onDismiss) {
        LinearLayout shell = new LinearLayout(this);
        shell.setOrientation(LinearLayout.VERTICAL);
        shell.setBackground(HeimdallUi.isPearl(this)
                ? HeimdallUi.cncFlush(this, 14)
                : HeimdallUi.glass(this, 0xFA0B111B, 0xFF070A10,
                        0x886A829C, 0x44344150, 14, 2));
        shell.setPadding(dp(16), dp(14), dp(16), dp(12));

        TextView titleView = text(title, HeimdallUi.TYPE_EDITOR_TITLE, TEXT, true);
        shell.addView(titleView, new LinearLayout.LayoutParams(-1, dp(36)));

        TextView messageView = text(message, 12, MUTED, false);
        messageView.setGravity(Gravity.TOP | Gravity.LEFT);
        messageView.setLineSpacing(0f, 1.12f);
        LinearLayout.LayoutParams messageParams = new LinearLayout.LayoutParams(-1, -2);
        messageParams.setMargins(0, dp(4), 0, dp(14));
        shell.addView(messageView, messageParams);

        View divider = new View(this);
        divider.setBackgroundColor(HeimdallUi.isPearl(this) ? 0x287B8792 : 0x445F7C9A);
        shell.addView(divider, new LinearLayout.LayoutParams(-1, dp(1)));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setPadding(0, dp(5), 0, 0);
        shell.addView(actions, new LinearLayout.LayoutParams(-1, dp(56)));

        final PanelOverlay[] overlayHolder = new PanelOverlay[1];
        actions.addView(editorButton(getString(R.string.common_cancel), () -> {
            if (overlayHolder[0] != null) {
                dismissPanelAnimated(overlayHolder[0]);
            }
        }));
        if (secondaryLabel != null && secondaryAction != null) {
            actions.addView(editorButton(secondaryLabel, () -> {
                if (overlayHolder[0] != null) {
                    dismissPanelAnimated(overlayHolder[0], secondaryAction);
                }
            }));
        }
        if (primaryLabel != null && primaryAction != null) {
            Button primary = editorButton(primaryLabel, () -> {
                if (overlayHolder[0] != null) {
                    dismissPanelAnimated(overlayHolder[0], primaryAction);
                }
            });
            HeimdallUi.applyPrimaryActionButton(this, primary);
            actions.addView(primary);
        }

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                settingsOverlayWidth(620), -2, Gravity.CENTER);
        params.setMargins(dp(12), dp(12), dp(12), dp(12));
        overlayHolder[0] = showPanelOverlay(shell, params, onDismiss);
        return overlayHolder[0];
    }

    private void showInformationPanel(String title, String message) {
        LinearLayout shell = new LinearLayout(this);
        shell.setOrientation(LinearLayout.VERTICAL);
        shell.setBackground(HeimdallUi.isPearl(this)
                ? HeimdallUi.cncFlush(this, 14)
                : HeimdallUi.glass(this, 0xFA0B111B, 0xFF070A10,
                        0x886A829C, 0x44344150, 14, 2));
        shell.setPadding(dp(16), dp(14), dp(16), dp(12));

        TextView titleView = text(title, HeimdallUi.TYPE_EDITOR_TITLE, TEXT, true);
        shell.addView(titleView, new LinearLayout.LayoutParams(-1, dp(36)));

        ScrollView scroll = new ScrollView(this);
        TextView messageView = text(message, 13, MUTED, false);
        messageView.setGravity(Gravity.TOP | Gravity.LEFT);
        messageView.setLineSpacing(0f, 1.12f);
        messageView.setPadding(0, dp(6), 0, dp(12));
        scroll.addView(messageView, new ScrollView.LayoutParams(-1, -2));
        shell.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        View divider = new View(this);
        divider.setBackgroundColor(HeimdallUi.isPearl(this) ? 0x287B8792 : 0x445F7C9A);
        shell.addView(divider, new LinearLayout.LayoutParams(-1, dp(1)));

        final PanelOverlay[] overlayHolder = new PanelOverlay[1];
        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setPadding(0, dp(5), 0, 0);
        shell.addView(actions, new LinearLayout.LayoutParams(-1, dp(56)));
        actions.addView(editorButton(getString(R.string.common_close), () -> {
            if (overlayHolder[0] != null) {
                dismissPanelAnimated(overlayHolder[0]);
            }
        }));

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                settingsOverlayWidth(620), settingsOverlayHeight(520), Gravity.CENTER);
        params.setMargins(dp(12), dp(12), dp(12), dp(12));
        overlayHolder[0] = showPanelOverlay(shell, params, null);
    }

    private int settingsOverlayWidth(int preferredDp) {
        int available = getResources().getDisplayMetrics().widthPixels - dp(32);
        return Math.max(dp(280), Math.min(dp(preferredDp), available));
    }

    private int settingsOverlayHeight(int preferredDp) {
        int available = getResources().getDisplayMetrics().heightPixels - dp(64);
        return Math.max(dp(320), Math.min(dp(preferredDp), available));
    }

    private Button rightStickCenterModeButton(String label, String mode) {
        Button button = editorButton(label, () -> selectRightStickCenterModeDraft(mode));
        boolean selected = mode.equals(settingsRightStickCenterMode);
        HeimdallUi.applyChoiceButton(this, button, selected);
        return button;
    }

    private void selectRightStickCenterModeDraft(String mode) {
        applySettingsTouchpadInputs();
        settingsRightStickCenterMode = TouchpadSettings.normalizeRightStickCenterMode(mode);
        ensureSettingsTouchpadDraft().rightStickCenterMode = settingsRightStickCenterMode;
        refreshSettingsContent();
    }

    private Button relativeMouseAccelerationButton(String label, String acceleration) {
        Button button = editorButton(label, () -> selectRelativeMouseAccelerationDraft(acceleration));
        button.setTextSize(HeimdallUi.TYPE_BUTTON_COMPACT);
        button.setPadding(dp(HeimdallUi.SPACE_1), 0, dp(HeimdallUi.SPACE_1), 0);
        boolean selected = acceleration.equals(settingsRelativeMouseAcceleration);
        HeimdallUi.applyChoiceButton(this, button, selected);
        return button;
    }

    private void selectRelativeMouseAccelerationDraft(String acceleration) {
        applySettingsTouchpadInputs();
        settingsRelativeMouseAcceleration =
                TouchpadSettings.normalizeRelativeMouseAcceleration(acceleration);
        ensureSettingsTouchpadDraft().relativeMouseAcceleration = settingsRelativeMouseAcceleration;
        refreshSettingsContent();
    }

    private void applySettingsTouchpadInputs() {
        TouchpadSettings draft = ensureSettingsTouchpadDraft();
        if (settingsTouchpadMode != null) {
            draft.mode = settingsTouchpadMode;
        }
        if (settingsRightStickCenterMode != null) {
            draft.rightStickCenterMode = settingsRightStickCenterMode;
        }
        if (settingsSensitivityInput != null) {
            draft.sensitivity = settingsSensitivityInput.floatValue();
        }
        if (settingsIntervalInput != null) {
            draft.intervalMs = settingsIntervalInput.intValue();
        }
        if (settingsMinDeltaInput != null) {
            draft.minDelta = settingsMinDeltaInput.floatValue();
        }
        if (settingsAnchorXInput != null) {
            draft.anchorX = settingsAnchorXInput.floatValue() / 100f;
        }
        if (settingsAnchorYInput != null) {
            draft.anchorY = settingsAnchorYInput.floatValue() / 100f;
        }
        if (settingsStrokeInput != null) {
            draft.strokeMs = settingsStrokeInput.intValue();
        }
        if (settingsShizukuTouchSensitivityXInput != null) {
            draft.shizukuTouchSensitivityX = settingsShizukuTouchSensitivityXInput.floatValue();
        }
        if (settingsShizukuTouchSensitivityYInput != null) {
            draft.shizukuTouchSensitivityY = settingsShizukuTouchSensitivityYInput.floatValue();
        }
        if (settingsShizukuTouchFrameInput != null) {
            draft.shizukuTouchFrameMs = settingsShizukuTouchFrameInput.intValue();
        }
        if (settingsShizukuTouchMinDeltaInput != null) {
            draft.shizukuTouchMinDelta = settingsShizukuTouchMinDeltaInput.floatValue();
        }
        if (settingsShizukuTouchCurveInput != null) {
            draft.shizukuTouchCurve = settingsShizukuTouchCurveInput.floatValue();
        }
        if (settingsShizukuTouchSmoothingInput != null) {
            draft.shizukuTouchSmoothing = settingsShizukuTouchSmoothingInput.floatValue() / 100f;
        }
        if (settingsRightStickSensitivityInput != null) {
            draft.rightStickSensitivity = settingsRightStickSensitivityInput.floatValue();
        }
        if (settingsRightStickDeadzoneInput != null) {
            draft.rightStickDeadzone = settingsRightStickDeadzoneInput.floatValue() / 100f;
        }
        if (settingsRightStickCurveInput != null) {
            draft.rightStickCurve = settingsRightStickCurveInput.floatValue();
        }
        if (settingsRightStickMaxInput != null) {
            draft.rightStickMaxOutput = settingsRightStickMaxInput.floatValue() / 100f;
        }
        if (settingsRightStickRadiusInput != null) {
            draft.rightStickRadius = settingsRightStickRadiusInput.floatValue() / 100f;
        }
        if (settingsRightStickRecenterInput != null) {
            draft.rightStickRecenterBursts = settingsRightStickRecenterInput.intValue();
        }
        if (settingsRelativeMouseSensitivityInput != null) {
            draft.relativeMouseSensitivity = settingsRelativeMouseSensitivityInput.floatValue();
        }
        if (settingsRelativeMouseMaxOutputInput != null) {
            draft.relativeMouseMaxOutputPercent =
                    settingsRelativeMouseMaxOutputInput.floatValue() / 100f;
        }
        if (settingsRelativeMousePulseInput != null) {
            draft.relativeMousePulseDurationMs = settingsRelativeMousePulseInput.intValue();
        }
        if (settingsRelativeMouseInvertYInput != null) {
            draft.relativeMouseInvertY = settingsRelativeMouseInvertYInput.isChecked();
        }
        if (settingsRelativeMouseAcceleration != null) {
            draft.relativeMouseAcceleration =
                    TouchpadSettings.normalizeRelativeMouseAcceleration(settingsRelativeMouseAcceleration);
        }
    }

    private void applySettingsMacroModuleInputs() {
        int required = 1;
        for (MacroModuleEditorBinding binding : settingsMacroModuleEditors) {
            int start = Math.max(0, binding.startInput.value() - 1);
            int count = Math.max(1, binding.countInput.value());
            int columns = Math.max(1, binding.columnsInput.value());
            int rows = Math.max(1, binding.rowsInput.value());
            int maxCount = Math.max(1, 24 - start);
            count = Math.min(count, maxCount);
            columns = Math.min(columns, Math.max(1, count));
            rows = Math.max(rows, (int) Math.ceil(count / (float) columns));
            if (rows > 6) {
                columns = Math.min(4, Math.max(columns, (int) Math.ceil(count / 6f)));
                rows = Math.max(1, (int) Math.ceil(count / (float) columns));
            }
            rows = Math.min(rows, 6);
            binding.item.macroStart = start;
            binding.item.macroCount = count;
            binding.item.macroColumns = columns;
            binding.item.macroRows = rows;
            binding.item.macroRightHandPriority = binding.rightInput.isChecked();
            binding.item.macroIconOnly = binding.iconOnlyInput.isChecked();
            binding.item.hasMacroConfig = true;
            required = Math.max(required, start + count);
        }
        currentWidgetLayout().sanitize();
        selectedProfile.setMacroCount(required);
    }

    private void previewWidgetLayout(WidgetLayout layout) {
        draftWidgetLayout = layout;
        activeScreen = SCREEN_MAIN;
        rebuildContent();
        showAction(getString(R.string.grid_layout_previewed));
    }

    private void setTouchpadModeFromSettings(String mode) {
        selectTouchpadModeDraft(mode);
    }

    private void selectInputBackendFromSettings(InputBridge.BackendOption option) {
        if (!option.available) {
            showErrorAction(getString(InputBridge.BACKEND_SHIZUKU.equals(option.id)
                    ? R.string.connection_start_shizuku
                    : R.string.settings_connection_unavailable));
            return;
        }
        if (InputBridge.setSelectedBackendId(this, option.id)) {
            if (InputBridge.BACKEND_SHIZUKU.equals(option.id) && !ShizukuNativeController.isPermissionGranted()) {
                ShizukuNativeController.requestPermission();
            }
            updateBridgeStatus();
            refreshSettingsContent();
            showAction(getString(R.string.settings_connection_using,
                    getString(InputBridge.BACKEND_SHIZUKU.equals(option.id)
                            ? R.string.connection_controller_enhancement
                            : R.string.connection_basic_touch)));
        }
    }

    private String settingsSectionTitle() {
        if (activeSettingsSection == SETTINGS_LAYOUT) {
            return getString(R.string.settings_category_main);
        }
        if (activeSettingsSection == SETTINGS_TOUCHPAD) {
            return getString(R.string.settings_category_controls);
        }
        if (activeSettingsSection == SETTINGS_MACRO) {
            return getString(R.string.settings_category_macros);
        }
        if (activeSettingsSection == SETTINGS_MAGNIFIER) {
            return getString(R.string.settings_category_magnifier);
        }
        if (activeSettingsSection == SETTINGS_INPUT) {
            return getString(R.string.settings_category_connection);
        }
        if (activeSettingsSection == SETTINGS_APPEARANCE) {
            return getString(R.string.settings_category_appearance);
        }
        if (activeSettingsSection == SETTINGS_DIAGNOSTICS) {
            return getString(R.string.settings_category_diagnostics);
        }
        return "Profile";
    }

    private String settingsSectionSummary() {
        if (activeSettingsSection == SETTINGS_LAYOUT) {
            return getString(draftWidgetLayout == null
                    ? R.string.settings_summary_layout_saved
                    : R.string.settings_summary_layout_draft);
        }
        if (activeSettingsSection == SETTINGS_TOUCHPAD) {
            return getString(R.string.settings_summary_touchpad);
        }
        if (activeSettingsSection == SETTINGS_MACRO) {
            return getString(R.string.settings_summary_macros);
        }
        if (activeSettingsSection == SETTINGS_MAGNIFIER) {
            WidgetLayout.Item draft = ensureSettingsMagnifierDraft();
            if (draft == null) {
                return getString(R.string.settings_summary_magnifier_missing);
            }
            return getString(R.string.settings_summary_magnifier);
        }
        if (activeSettingsSection == SETTINGS_INPUT) {
            return getString(R.string.settings_summary_connection);
        }
        if (activeSettingsSection == SETTINGS_APPEARANCE) {
            return getString(R.string.settings_summary_appearance);
        }
        if (activeSettingsSection == SETTINGS_DIAGNOSTICS) {
            return getString(R.string.settings_summary_diagnostics);
        }
        return getString(R.string.settings_summary_profile);
    }

    private void resetActiveSettingsSection() {
        if (activeSettingsSection == SETTINGS_LAYOUT) {
            draftWidgetLayout = WidgetLayout.defaultLayout();
            showDebugAction(getString(R.string.grid_layout_reset_preview));
            refreshSettingsContent();
            return;
        } else if (activeSettingsSection == SETTINGS_TOUCHPAD) {
            settingsTouchpadDraft = new TouchpadSettings();
            settingsTouchpadMode = settingsTouchpadDraft.mode;
            clearSettingsTouchpadInputs();
            showDebugAction(getString(R.string.settings_reset_touchpad_draft));
            refreshSettingsContent();
            return;
        } else if (activeSettingsSection == SETTINGS_MACRO) {
            resetMacroModulesToDefault();
            settingsMacroMappingProtectionDraft = true;
            settingsMacroMappingProtectionInput = null;
            showDebugAction(getString(R.string.settings_reset_macro_draft));
            refreshSettingsContent();
            return;
        } else if (activeSettingsSection == SETTINGS_MAGNIFIER) {
            WidgetLayout.Item current = currentWidgetLayout().findItem(WidgetLayout.TYPE_MAGNIFIER);
            settingsMagnifierDraft = current == null ? null : current.copy();
            if (settingsMagnifierDraft != null) {
                settingsMagnifierDraft.magnifierFps = 30;
                settingsMagnifierDraft.magnifierScaleMode = WidgetLayout.MAGNIFIER_SCALE_FILL;
                settingsMagnifierDraft.magnifierShape =
                        WidgetLayout.MAGNIFIER_SHAPE_RECTANGLE;
                settingsMagnifierDraft.magnifierZoom = 1f;
            }
            showDebugAction(getString(R.string.settings_reset_magnifier_draft));
            refreshSettingsContent();
            return;
        } else if (activeSettingsSection == SETTINGS_INPUT) {
            InputBridge.setSelectedBackendId(this, InputBridge.BACKEND_ACCESSIBILITY);
            showDebugAction(getString(R.string.settings_reset_connection_basic));
            refreshSettingsContent();
            return;
        } else if (activeSettingsSection == SETTINGS_APPEARANCE) {
            settingsThemeDraft = HeimdallUi.THEME_DARK;
            settingsPerformanceCompatibilityDraft = false;
            showDebugAction(getString(R.string.settings_reset_appearance_draft));
            refreshSettingsContent();
            return;
        }
        ProfileStore.saveProfiles(this, profiles);
        showDebugAction(getString(R.string.settings_section_reset, settingsSectionTitle()));
        refreshSettingsContent();
    }

    private void saveActiveSettingsSection() {
        boolean savingLayout = activeSettingsSection == SETTINGS_LAYOUT;
        boolean magnifierShapeChanged = false;
        if (activeSettingsSection == SETTINGS_LAYOUT && draftWidgetLayout != null) {
            selectedProfile.widgetLayout = draftWidgetLayout.copy();
            draftWidgetLayout = null;
        }
        if (activeSettingsSection == SETTINGS_TOUCHPAD) {
            applySettingsTouchpadInputs();
            selectedProfile.touchpadSettings = ensureSettingsTouchpadDraft().copy();
            touchpadSettings = selectedProfile.safeTouchpadSettings();
            settingsTouchpadDraft = null;
        }
        if (activeSettingsSection == SETTINGS_MACRO) {
            applySettingsMacroModuleInputs();
            if (settingsMacroMappingProtectionInput != null) {
                settingsMacroMappingProtectionDraft =
                        settingsMacroMappingProtectionInput.isChecked();
            }
            selectedProfile.protectThorMappingDuringEnhancedTouch =
                    settingsMacroMappingProtectionDraft == null
                            || settingsMacroMappingProtectionDraft;
            if (draftWidgetLayout != null) {
                selectedProfile.widgetLayout = draftWidgetLayout.copy();
                draftWidgetLayout = null;
            }
        }
        if (activeSettingsSection == SETTINGS_MAGNIFIER) {
            magnifierShapeChanged = applySettingsMagnifierDraft();
            settingsMagnifierDraft = null;
        }
        if (activeSettingsSection == SETTINGS_PROFILE) {
            applyProfileSettingsInputs();
        }
        boolean themeChanged = activeSettingsSection == SETTINGS_APPEARANCE;
        if (themeChanged) {
            HeimdallUi.setTheme(this, settingsThemeDraft);
            boolean compatibilityEnabled =
                    Boolean.TRUE.equals(settingsPerformanceCompatibilityDraft);
            ThorPerformanceCompatibility.setEnabled(this, compatibilityEnabled);
            thorPerformanceCompatibility.apply(this, compatibilityEnabled
                    || DebugPerformanceDiagnostics.isCompositionProbe());
            enterImmersiveMode();
        }
        ProfileStore.saveProfiles(this, profiles);
        showDebugAction(savingLayout
                ? getString(R.string.grid_layout_saved)
                : getString(R.string.settings_section_saved, settingsSectionTitle()));
        updateBridgeStatus();
        if (themeChanged || magnifierShapeChanged) {
            rebuildContent();
        } else {
            refreshSettingsContent();
        }
    }

    private void resetMacroModulesToDefault() {
        WidgetLayout layout = editableWidgetLayout();
        int start = 0;
        for (WidgetLayout.Item item : layout.items) {
            if (WidgetLayout.TYPE_MACRO_GROUP.equals(item.type)) {
                item.macroStart = start;
                item.macroCount = 4;
                item.macroColumns = 2;
                item.macroRows = 2;
                item.macroRightHandPriority = true;
                item.macroIconOnly = false;
                start += item.macroCount;
            }
        }
    }

    private void showWidgetLayoutPresetPicker() {
        String[] labels = {
                getString(R.string.grid_preset_balanced),
                getString(R.string.grid_preset_controls),
                getString(R.string.grid_preset_macros),
                getString(R.string.grid_preset_edit)
        };
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.grid_preset_title))
                .setItems(labels, (dialog, which) -> {
                    if (which == 1) {
                        draftWidgetLayout = WidgetLayout.fpsLayout();
                    } else if (which == 2) {
                        draftWidgetLayout = WidgetLayout.macroFocusLayout();
                    } else if (which == 3) {
                        activeScreen = SCREEN_MAIN;
                        editableWidgetLayout();
                        showWidgetGridEditor();
                        return;
                    } else {
                        draftWidgetLayout = WidgetLayout.defaultLayout();
                    }
                    activeScreen = SCREEN_MAIN;
                    rebuildContent();
                    showAction(getString(R.string.grid_preset_previewed,
                            widgetLayoutPresetLabel(currentWidgetLayout().preset)));
                })
                .setNegativeButton(getString(R.string.common_cancel), null)
                .show();
    }

    private String widgetLayoutPresetLabel(String preset) {
        if (WidgetLayout.PRESET_FPS.equals(preset)) {
            return getString(R.string.grid_preset_name_controls);
        }
        if (WidgetLayout.PRESET_MACRO_FOCUS.equals(preset)) {
            return getString(R.string.grid_preset_name_macros);
        }
        if (WidgetLayout.PRESET_CUSTOM.equals(preset)) {
            return getString(R.string.grid_preset_name_custom);
        }
        return getString(R.string.grid_preset_name_balanced);
    }

    private void showWidgetGridEditor() {
        if (widgetGridDialog != null && widgetGridDialog.isShowing()) {
            widgetGridDialog.dismiss();
        }
        normalizeGridEditorLayout();
        LinearLayout editor = new LinearLayout(this);
        editor.setOrientation(LinearLayout.VERTICAL);
        editor.setPadding(dp(16), dp(12), dp(16), dp(18));
        editor.setLayoutParams(new LinearLayout.LayoutParams(-1, -1));
        editor.setBackgroundColor(HeimdallUi.background(this));
        applySystemGestureExclusion(editor);

        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        titleRow.setPadding(dp(4), 0, dp(2), 0);
        editor.addView(titleRow, new LinearLayout.LayoutParams(-1, dp(32)));

        TextView title = text(getString(R.string.grid_editor_title,
                GRID_EDITOR_COLUMNS, GRID_EDITOR_ROWS), 13, TEXT, true);
        title.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
        titleRow.addView(title, new LinearLayout.LayoutParams(0, -1, 1));

        Button closeButton = gridCloseButton(() -> {
            if (widgetGridDialog != null) {
                widgetGridDialog.dismiss();
            }
        });
        closeButton.setContentDescription(getString(R.string.grid_editor_close));
        LinearLayout.LayoutParams closeParams = new LinearLayout.LayoutParams(dp(36), dp(24));
        closeParams.setMargins(0, dp(5), 0, dp(3));
        titleRow.addView(closeButton, closeParams);

        TextView help = text(getString(R.string.grid_editor_help), 10, MUTED, false);
        help.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
        help.setPadding(dp(4), 0, 0, 0);
        editor.addView(help, new LinearLayout.LayoutParams(-1, dp(18)));

        GridEditorView gridEditor = new GridEditorView(this);
        applySystemGestureExclusion(gridEditor);
        editor.addView(gridEditor, new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        editor.addView(actions, new LinearLayout.LayoutParams(-1, dp(38)));
        actions.addView(editorButton(getString(R.string.grid_editor_add_macro),
                () -> addWidgetFromEditor(WidgetLayout.TYPE_MACRO_GROUP, gridEditor)));
        actions.addView(editorButton(getString(R.string.grid_editor_add_touch),
                () -> addWidgetFromEditor(WidgetLayout.TYPE_TOUCHPAD, gridEditor)));
        actions.addView(editorButton(getString(R.string.grid_editor_add_magnifier),
                () -> addWidgetFromEditor(WidgetLayout.TYPE_MAGNIFIER, gridEditor)));
        actions.addView(editorButton(getString(R.string.grid_editor_add_canvas),
                () -> addWidgetFromEditor(WidgetLayout.TYPE_CANVAS, gridEditor)));
        actions.addView(editorButton(getString(R.string.grid_editor_add_quick_actions),
                () -> addWidgetFromEditor(WidgetLayout.TYPE_QUICK_ACTIONS, gridEditor)));

        LinearLayout editActions = new LinearLayout(this);
        editActions.setOrientation(LinearLayout.HORIZONTAL);
        editor.addView(editActions, new LinearLayout.LayoutParams(-1, dp(38)));
        editActions.addView(editorButton(getString(R.string.grid_editor_copy),
                () -> duplicateSelectedWidget(gridEditor)));
        editActions.addView(editorButton(getString(R.string.grid_editor_delete),
                () -> deleteSelectedWidget(gridEditor)));
        editActions.addView(editorButton(getString(R.string.grid_editor_reset), () -> {
            draftWidgetLayout = WidgetLayout.defaultLayout();
            gridEditor.invalidate();
            showDebugAction(getString(R.string.grid_editor_reset_preview));
        }));

        widgetGridDialog = new AlertDialog.Builder(this)
                .setView(editor)
                .create();
        widgetGridDialog.setCanceledOnTouchOutside(false);
        widgetGridDialog.setOnKeyListener((dialog, keyCode, event) -> keyCode == KeyEvent.KEYCODE_BACK);
        widgetGridDialog.setOnDismissListener(dialog -> {
            rebuildContent();
            if (widgetGridDialog == dialog) {
                widgetGridDialog = null;
            }
        });
        widgetGridDialog.show();
        Window window = widgetGridDialog.getWindow();
        if (window != null) {
            window.getDecorView().setPadding(0, 0, 0, 0);
            applySystemGestureExclusion(window.getDecorView());
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
        }
    }

    private void normalizeGridEditorLayout() {
        WidgetLayout layout = editableWidgetLayout();
        layout.columns = GRID_EDITOR_COLUMNS;
        layout.rows = GRID_EDITOR_ROWS;
        layout.sanitize();
    }

    private int widgetPreviewColor(String type) {
        if (HeimdallUi.isPearl(this)) {
            if (WidgetLayout.TYPE_TOUCHPAD.equals(type)) {
                return 0xFFD6DEE6;
            }
            if (WidgetLayout.TYPE_MACRO_GROUP.equals(type)) {
                return 0xFFE3DFE8;
            }
            if (WidgetLayout.TYPE_QUICK_ACTIONS.equals(type)) {
                return 0xFFDCE3E9;
            }
            if (WidgetLayout.TYPE_MAGNIFIER.equals(type)) {
                return 0xFFD6E1E3;
            }
            if (WidgetLayout.TYPE_CANVAS.equals(type)) {
                return 0xFFD4D9DE;
            }
            return 0xFFDCE5DF;
        }
        if (WidgetLayout.TYPE_TOUCHPAD.equals(type)) {
            return 0xFF15243A;
        }
        if (WidgetLayout.TYPE_MACRO_GROUP.equals(type)) {
            return 0xFF241D3A;
        }
        if (WidgetLayout.TYPE_QUICK_ACTIONS.equals(type)) {
            return 0xFF17263B;
        }
        if (WidgetLayout.TYPE_MAGNIFIER.equals(type)) {
            return 0xFF142A35;
        }
        if (WidgetLayout.TYPE_CANVAS.equals(type)) {
            return 0xFF101820;
        }
        return 0xFF142A22;
    }

    private String widgetTypeLabel(String type) {
        if (WidgetLayout.TYPE_TOUCHPAD.equals(type)) {
            return getString(R.string.grid_widget_touch);
        }
        if (WidgetLayout.TYPE_MACRO_GROUP.equals(type)) {
            return getString(R.string.grid_widget_macro);
        }
        if (WidgetLayout.TYPE_STATUS.equals(type)) {
            return getString(R.string.grid_widget_status);
        }
        if (WidgetLayout.TYPE_CANVAS.equals(type)) {
            return getString(R.string.canvas_name);
        }
        if (WidgetLayout.TYPE_QUICK_ACTIONS.equals(type)) {
            return getString(R.string.grid_widget_quick_actions);
        }
        if (WidgetLayout.TYPE_MAGNIFIER.equals(type)) {
            return getString(R.string.grid_widget_magnifier);
        }
        return getString(R.string.grid_widget_module);
    }

    private String widgetEditorMeta(WidgetLayout.Item item) {
        String size = item.w + " x " + item.h;
        if (!WidgetLayout.TYPE_MACRO_GROUP.equals(item.type)) {
            return size;
        }
        int first = item.macroStart + 1;
        int last = item.macroStart + item.macroCount;
        return size + "  M" + first + "-" + last;
    }

    private void addWidgetFromEditor(String type, GridEditorView gridEditor) {
        WidgetLayout layout = editableWidgetLayout();
        if (WidgetLayout.TYPE_MAGNIFIER.equals(type)
                && layout.findItem(WidgetLayout.TYPE_MAGNIFIER) != null) {
            showErrorAction(getString(R.string.grid_editor_magnifier_limit));
            return;
        }
        int[] size = defaultWidgetSize(type);
        WidgetLayout.Item item = firstAvailableWidgetItem(layout, type, size[0], size[1]);
        if (item == null) {
            showErrorAction(getString(R.string.grid_editor_no_space_add));
            return;
        }
        configureNewWidgetItem(layout, item);
        layout.items.add(item);
        layout.preset = WidgetLayout.PRESET_CUSTOM;
        layout.sanitize();
        gridEditor.selectItem(layout.items.size() - 1);
        gridEditor.invalidate();
        showDebugAction(getString(R.string.grid_editor_added, widgetTypeLabel(type)));
    }

    private void configureNewWidgetItem(WidgetLayout layout, WidgetLayout.Item item) {
        if (!WidgetLayout.TYPE_MACRO_GROUP.equals(item.type)) {
            return;
        }
        int start = nextMacroStart(layout);
        item.macroStart = start;
        item.macroCount = 4;
        item.macroColumns = 2;
        item.macroRows = 2;
        item.macroRightHandPriority = true;
        ensureMacroCapacity(start + item.macroCount);
    }

    private int nextMacroStart(WidgetLayout layout) {
        int next = 0;
        for (WidgetLayout.Item item : layout.items) {
            if (WidgetLayout.TYPE_MACRO_GROUP.equals(item.type)) {
                next = Math.max(next, item.macroStart + item.macroCount);
            }
        }
        return Math.max(0, Math.min(23, next));
    }

    private void duplicateSelectedWidget(GridEditorView gridEditor) {
        WidgetLayout layout = editableWidgetLayout();
        int selected = gridEditor.selectedIndex();
        if (selected < 0 || selected >= layout.items.size()) {
            showErrorAction(getString(R.string.grid_editor_select_copy));
            return;
        }
        WidgetLayout.Item source = layout.items.get(selected);
        if (!WidgetLayout.TYPE_MACRO_GROUP.equals(source.type)) {
            showErrorAction(getString(R.string.grid_editor_copy_macro_only));
            return;
        }
        WidgetLayout.Item copy = firstAvailableWidgetItem(layout, source.type, source.w, source.h);
        if (copy == null) {
            showErrorAction(getString(R.string.grid_editor_no_space_copy));
            return;
        }
        copy.macroStart = source.macroStart;
        copy.macroCount = source.macroCount;
        copy.macroColumns = source.macroColumns;
        copy.macroRows = source.macroRows;
        copy.macroRightHandPriority = source.macroRightHandPriority;
        copy.macroIconOnly = source.macroIconOnly;
        layout.items.add(copy);
        layout.preset = WidgetLayout.PRESET_CUSTOM;
        layout.sanitize();
        gridEditor.selectItem(layout.items.size() - 1);
        gridEditor.invalidate();
        showDebugAction(getString(R.string.grid_editor_copied));
    }

    private void deleteSelectedWidget(GridEditorView gridEditor) {
        WidgetLayout layout = editableWidgetLayout();
        int selected = gridEditor.selectedIndex();
        if (selected < 0 || selected >= layout.items.size()) {
            showErrorAction(getString(R.string.grid_editor_select_delete));
            return;
        }
        layout.items.remove(selected);
        layout.preset = WidgetLayout.PRESET_CUSTOM;
        layout.sanitize();
        gridEditor.selectItem(-1);
        gridEditor.invalidate();
        showDebugAction(getString(R.string.grid_editor_deleted));
    }

    private void saveWidgetLayoutFromEditor(GridEditorView gridEditor) {
        WidgetLayout layout = editableWidgetLayout();
        layout.preset = WidgetLayout.PRESET_CUSTOM;
        layout.sanitize();
        gridEditor.invalidate();
    }

    private int singleOverlapIndex(WidgetLayout layout, WidgetLayout.Item candidate, int ignoreIndex) {
        int overlap = -1;
        for (int i = 0; i < layout.items.size(); i++) {
            if (i == ignoreIndex) {
                continue;
            }
            if (rectsOverlap(candidate, layout.items.get(i))) {
                if (overlap >= 0) {
                    return -2;
                }
                overlap = i;
            }
        }
        return overlap;
    }

    private boolean canPlaceWidget(WidgetLayout layout, WidgetLayout.Item candidate, int ignoreA, int ignoreB) {
        for (int i = 0; i < layout.items.size(); i++) {
            if (i == ignoreA || i == ignoreB) {
                continue;
            }
            if (rectsOverlap(candidate, layout.items.get(i))) {
                return false;
            }
        }
        return true;
    }

    private boolean rectsOverlap(WidgetLayout.Item a, WidgetLayout.Item b) {
        return a.x < b.x + b.w
                && a.x + a.w > b.x
                && a.y < b.y + b.h
                && a.y + a.h > b.y;
    }

    private final class WidgetHostLayout extends ViewGroup {
        private final WidgetLayout layout;
        private final List<WidgetLayout.Item> childItems = new ArrayList<>();
        private final int gap = dp(6);

        WidgetHostLayout(Context context, WidgetLayout layout) {
            super(context);
            this.layout = layout;
        }

        void addWidget(View child, WidgetLayout.Item item) {
            childItems.add(item);
            addView(child, new ViewGroup.LayoutParams(0, 0));
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            int width = MeasureSpec.getSize(widthMeasureSpec);
            int height = MeasureSpec.getSize(heightMeasureSpec);
            float cellW = width / (float) Math.max(1, layout.columns);
            float cellH = height / (float) Math.max(1, layout.rows);
            for (int i = 0; i < getChildCount(); i++) {
                WidgetLayout.Item item = childItems.get(i);
                int childW = Math.max(1, Math.round(item.w * cellW) - gap * 2);
                int childH = Math.max(1, Math.round(item.h * cellH) - gap * 2);
                int childWSpec = MeasureSpec.makeMeasureSpec(childW, MeasureSpec.EXACTLY);
                int childHSpec = MeasureSpec.makeMeasureSpec(childH, MeasureSpec.EXACTLY);
                getChildAt(i).measure(childWSpec, childHSpec);
            }
            setMeasuredDimension(width, height);
        }

        @Override
        protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
            int width = right - left;
            int height = bottom - top;
            float cellW = width / (float) Math.max(1, layout.columns);
            float cellH = height / (float) Math.max(1, layout.rows);
            for (int i = 0; i < getChildCount(); i++) {
                WidgetLayout.Item item = childItems.get(i);
                int childLeft = Math.round(item.x * cellW) + gap;
                int childTop = Math.round(item.y * cellH) + gap;
                int childRight = Math.round((item.x + item.w) * cellW) - gap;
                int childBottom = Math.round((item.y + item.h) * cellH) - gap;
                getChildAt(i).layout(childLeft, childTop, childRight, childBottom);
            }
        }
    }

    private final class GridEditorView extends View {
        private static final int MODE_NONE = 0;
        private static final int MODE_MOVE = 1;
        private static final int MODE_RESIZE = 2;

        private final Paint editorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF editorRect = new RectF();
        private int selectedIndex = -1;
        private int mode = MODE_NONE;
        private int originalX;
        private int originalY;
        private int originalW;
        private int originalH;
        private float downX;
        private float downY;
        private int grabCellX;
        private int grabCellY;

        GridEditorView(Context context) {
            super(context);
            setBackground(HeimdallUi.insetPanel(AssistantActivity.this, 10));
            setContentDescription(getString(R.string.grid_editor_canvas_description));
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            int availableW = Math.max(1, MeasureSpec.getSize(widthMeasureSpec));
            int availableH = Math.max(1, MeasureSpec.getSize(heightMeasureSpec));
            if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.EXACTLY) {
                setMeasuredDimension(availableW, availableH);
                return;
            }
            int targetH = Math.round(availableW * realGridAspectHeight());
            int measuredH = Math.min(availableH, targetH);
            setMeasuredDimension(availableW, measuredH);
        }

        private float realGridAspectHeight() {
            DisplayMetrics metrics = getResources().getDisplayMetrics();
            float width = Math.max(1f, metrics.widthPixels - dp(8));
            float height = Math.max(1f, metrics.heightPixels
                    - dp(8)
                    - dp(44)
                    - dp(6)
                    - dp(50)
                    - dp(6));
            return height / width;
        }

        int selectedIndex() {
            return selectedIndex;
        }

        void selectItem(int index) {
            selectedIndex = index;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            WidgetLayout layout = currentWidgetLayout();
            float cellW = getWidth() / (float) layout.columns;
            float cellH = getHeight() / (float) layout.rows;

            editorPaint.setStyle(Paint.Style.STROKE);
            editorPaint.setStrokeWidth(dp(1));
            editorPaint.setColor(HeimdallUi.isPearl(AssistantActivity.this) ? 0x335D6975 : 0x334EA1FF);
            for (int col = 1; col < layout.columns; col++) {
                float x = col * cellW;
                canvas.drawLine(x, 0, x, getHeight(), editorPaint);
            }
            for (int row = 1; row < layout.rows; row++) {
                float y = row * cellH;
                canvas.drawLine(0, y, getWidth(), y, editorPaint);
            }

            editorPaint.setStyle(Paint.Style.STROKE);
            editorPaint.setStrokeWidth(dp(2));
            editorPaint.setColor(HeimdallUi.isPearl(AssistantActivity.this) ? 0x886D7B88 : 0x664EA1FF);
            canvas.drawRect(0, 0, getWidth(), getHeight(), editorPaint);

            if (layout.items.isEmpty()) {
                editorPaint.setStyle(Paint.Style.FILL);
                editorPaint.setTextAlign(Paint.Align.CENTER);
                editorPaint.setTextSize(dp(15));
                editorPaint.setColor(HeimdallUi.mutedTextColor(AssistantActivity.this));
                canvas.drawText(getString(R.string.grid_editor_empty),
                        getWidth() * 0.5f, getHeight() * 0.5f, editorPaint);
                editorPaint.setTextAlign(Paint.Align.LEFT);
                return;
            }

            for (int i = 0; i < layout.items.size(); i++) {
                drawEditorItem(canvas, layout.items.get(i), i, cellW, cellH);
            }
        }

        private void drawEditorItem(Canvas canvas, WidgetLayout.Item item, int index, float cellW, float cellH) {
            float gap = dp(4);
            editorRect.set(item.x * cellW + gap,
                    item.y * cellH + gap,
                    (item.x + item.w) * cellW - gap,
                    (item.y + item.h) * cellH - gap);
            boolean selected = index == selectedIndex;

            editorPaint.setStyle(Paint.Style.FILL);
            editorPaint.setColor(selected ? selectedWidgetColor(item.type) : widgetPreviewColor(item.type));
            canvas.drawRoundRect(editorRect, dp(10), dp(10), editorPaint);

            editorPaint.setStyle(Paint.Style.STROKE);
            editorPaint.setStrokeWidth(selected ? dp(3) : dp(1));
            editorPaint.setColor(selected
                    ? HeimdallUi.accent(AssistantActivity.this)
                    : (HeimdallUi.isPearl(AssistantActivity.this) ? 0x886D7B88 : 0x884EA1FF));
            canvas.drawRoundRect(editorRect, dp(10), dp(10), editorPaint);

            editorPaint.setStyle(Paint.Style.FILL);
            editorPaint.setColor(HeimdallUi.textColor(AssistantActivity.this));
            editorPaint.setTextSize(dp(15));
            editorPaint.setTextAlign(Paint.Align.LEFT);
            canvas.drawText(widgetTypeLabel(item.type), editorRect.left + dp(10), editorRect.top + dp(24), editorPaint);
            editorPaint.setColor(HeimdallUi.mutedTextColor(AssistantActivity.this));
            editorPaint.setTextSize(dp(12));
            canvas.drawText(widgetEditorMeta(item), editorRect.left + dp(10), editorRect.top + dp(44), editorPaint);

            if (selected) {
                float handle = dp(24);
                editorPaint.setStyle(Paint.Style.FILL);
                editorPaint.setColor(HeimdallUi.isPearl(AssistantActivity.this) ? 0xDDF08A2A : 0xDD4EA1FF);
                canvas.drawRoundRect(editorRect.right - handle, editorRect.bottom - handle,
                        editorRect.right, editorRect.bottom, dp(8), dp(8), editorPaint);
                editorPaint.setColor(0xFFFFFFFF);
                editorPaint.setStrokeWidth(dp(2));
                canvas.drawLine(editorRect.right - dp(7), editorRect.bottom - dp(18),
                        editorRect.right - dp(18), editorRect.bottom - dp(7), editorPaint);
            }
        }

        private int selectedWidgetColor(String type) {
            if (HeimdallUi.isPearl(AssistantActivity.this)) {
                return 0xFFE7E1D9;
            }
            if (WidgetLayout.TYPE_TOUCHPAD.equals(type)) {
                return 0xCC1F4D78;
            }
            if (WidgetLayout.TYPE_MACRO_GROUP.equals(type)) {
                return 0xCC4A3278;
            }
            if (WidgetLayout.TYPE_MAGNIFIER.equals(type)) {
                return 0xCC20566A;
            }
            if (WidgetLayout.TYPE_CANVAS.equals(type)) {
                return 0xCC283B4C;
            }
            return 0xCC1D5A3B;
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            WidgetLayout layout = editableWidgetLayout();
            if (layout.items.isEmpty()) {
                return true;
            }
            float cellW = getWidth() / (float) layout.columns;
            float cellH = getHeight() / (float) layout.rows;
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    getParent().requestDisallowInterceptTouchEvent(true);
                    downX = event.getX();
                    downY = event.getY();
                    selectedIndex = hitTest(layout, downX, downY, cellW, cellH);
                    if (selectedIndex < 0) {
                        mode = MODE_NONE;
                        invalidate();
                        return true;
                    }
                    WidgetLayout.Item item = layout.items.get(selectedIndex);
                    originalX = item.x;
                    originalY = item.y;
                    originalW = item.w;
                    originalH = item.h;
                    grabCellX = Math.max(0, Math.min(item.w - 1, (int) ((downX - item.x * cellW) / cellW)));
                    grabCellY = Math.max(0, Math.min(item.h - 1, (int) ((downY - item.y * cellH) / cellH)));
                    mode = hitResizeHandle(item, downX, downY, cellW, cellH) ? MODE_RESIZE : MODE_MOVE;
                    invalidate();
                    return true;
                case MotionEvent.ACTION_MOVE:
                    if (selectedIndex < 0 || selectedIndex >= layout.items.size()) {
                        return true;
                    }
                    updateDraggedItem(layout, layout.items.get(selectedIndex), event.getX(), event.getY(), cellW, cellH);
                    invalidate();
                    return true;
                case MotionEvent.ACTION_UP:
                    getParent().requestDisallowInterceptTouchEvent(false);
                    finishDrag(layout);
                    mode = MODE_NONE;
                    invalidate();
                    return true;
                case MotionEvent.ACTION_CANCEL:
                    getParent().requestDisallowInterceptTouchEvent(false);
                    restoreOriginal(layout);
                    mode = MODE_NONE;
                    invalidate();
                    return true;
                default:
                    return true;
            }
        }

        private int hitTest(WidgetLayout layout, float x, float y, float cellW, float cellH) {
            for (int i = layout.items.size() - 1; i >= 0; i--) {
                WidgetLayout.Item item = layout.items.get(i);
                if (x >= item.x * cellW
                        && x <= (item.x + item.w) * cellW
                        && y >= item.y * cellH
                        && y <= (item.y + item.h) * cellH) {
                    return i;
                }
            }
            return -1;
        }

        private boolean hitResizeHandle(WidgetLayout.Item item, float x, float y, float cellW, float cellH) {
            float right = (item.x + item.w) * cellW;
            float bottom = (item.y + item.h) * cellH;
            float handle = dp(34);
            return x >= right - handle && x <= right && y >= bottom - handle && y <= bottom;
        }

        private void updateDraggedItem(WidgetLayout layout, WidgetLayout.Item item, float x, float y, float cellW, float cellH) {
            if (mode == MODE_RESIZE) {
                int right = Math.max(item.x + 1, Math.min(layout.columns, (int) Math.ceil(x / cellW)));
                int bottom = Math.max(item.y + 1, Math.min(layout.rows, (int) Math.ceil(y / cellH)));
                item.w = Math.max(1, Math.min(layout.columns - item.x, right - item.x));
                item.h = Math.max(1, Math.min(layout.rows - item.y, bottom - item.y));
                return;
            }
            if (mode == MODE_MOVE) {
                int nextX = Math.round(x / cellW) - grabCellX;
                int nextY = Math.round(y / cellH) - grabCellY;
                item.x = Math.max(0, Math.min(layout.columns - item.w, nextX));
                item.y = Math.max(0, Math.min(layout.rows - item.h, nextY));
            }
        }

        private void finishDrag(WidgetLayout layout) {
            if (selectedIndex < 0 || selectedIndex >= layout.items.size()) {
                return;
            }
            WidgetLayout.Item item = layout.items.get(selectedIndex);
            if (mode == MODE_RESIZE) {
                if (widgetOverlaps(layout, item, selectedIndex)) {
                    restoreOriginal(layout);
            showErrorAction(getString(R.string.grid_editor_resize_overlap));
                    return;
                }
                saveWidgetLayoutFromEditor(this);
                showDebugAction(getString(R.string.grid_editor_resized));
                return;
            }

            int overlap = singleOverlapIndex(layout, item, selectedIndex);
            if (overlap == -1) {
                saveWidgetLayoutFromEditor(this);
                showDebugAction(getString(R.string.grid_editor_moved));
                return;
            }
            if (overlap >= 0 && trySwap(layout, selectedIndex, overlap)) {
                saveWidgetLayoutFromEditor(this);
                showDebugAction(getString(R.string.grid_editor_swapped));
                return;
            }
            restoreOriginal(layout);
            showErrorAction(getString(R.string.grid_editor_position_conflict));
        }

        private boolean trySwap(WidgetLayout layout, int movingIndex, int targetIndex) {
            WidgetLayout.Item moving = layout.items.get(movingIndex);
            WidgetLayout.Item target = layout.items.get(targetIndex);
            WidgetLayout.Item movingAtTarget = new WidgetLayout.Item(moving.type, target.x, target.y, moving.w, moving.h);
            WidgetLayout.Item targetAtOriginal = new WidgetLayout.Item(target.type, originalX, originalY, target.w, target.h);
            clampWidgetItem(layout, movingAtTarget);
            clampWidgetItem(layout, targetAtOriginal);
            if (movingAtTarget.x != target.x || movingAtTarget.y != target.y
                    || targetAtOriginal.x != originalX || targetAtOriginal.y != originalY) {
                return false;
            }
            if (!canPlaceWidget(layout, movingAtTarget, movingIndex, targetIndex)
                    || !canPlaceWidget(layout, targetAtOriginal, movingIndex, targetIndex)) {
                return false;
            }
            moving.x = movingAtTarget.x;
            moving.y = movingAtTarget.y;
            target.x = targetAtOriginal.x;
            target.y = targetAtOriginal.y;
            return true;
        }

        private void restoreOriginal(WidgetLayout layout) {
            if (selectedIndex < 0 || selectedIndex >= layout.items.size()) {
                return;
            }
            WidgetLayout.Item item = layout.items.get(selectedIndex);
            item.x = originalX;
            item.y = originalY;
            item.w = originalW;
            item.h = originalH;
        }
    }

    private int[] defaultWidgetSize(String type) {
        if (WidgetLayout.TYPE_TOUCHPAD.equals(type)) {
            return new int[]{3, 4};
        }
        if (WidgetLayout.TYPE_MACRO_GROUP.equals(type)) {
            return new int[]{2, 4};
        }
        if (WidgetLayout.TYPE_QUICK_ACTIONS.equals(type)) {
            return new int[]{2, 2};
        }
        if (WidgetLayout.TYPE_MAGNIFIER.equals(type)) {
            return new int[]{3, 3};
        }
        if (WidgetLayout.TYPE_CANVAS.equals(type)) {
            return new int[]{3, 3};
        }
        return new int[]{2, 1};
    }

    private WidgetLayout.Item firstAvailableWidgetItem(WidgetLayout layout, String type, int width, int height) {
        for (int y = 0; y <= layout.rows - height; y++) {
            for (int x = 0; x <= layout.columns - width; x++) {
                WidgetLayout.Item candidate = new WidgetLayout.Item(type, x, y, width, height);
                if (!widgetOverlaps(layout, candidate, -1)) {
                    return candidate;
                }
            }
        }
        return null;
    }

    private void clampWidgetItem(WidgetLayout layout, WidgetLayout.Item item) {
        item.x = Math.max(0, Math.min(layout.columns - 1, item.x));
        item.y = Math.max(0, Math.min(layout.rows - 1, item.y));
        item.w = Math.max(1, Math.min(layout.columns - item.x, item.w));
        item.h = Math.max(1, Math.min(layout.rows - item.y, item.h));
    }

    private boolean widgetOverlaps(WidgetLayout layout, WidgetLayout.Item candidate, int ignoreIndex) {
        for (int i = 0; i < layout.items.size(); i++) {
            if (i == ignoreIndex) {
                continue;
            }
            WidgetLayout.Item item = layout.items.get(i);
            boolean separated = candidate.x + candidate.w <= item.x
                    || item.x + item.w <= candidate.x
                    || candidate.y + candidate.h <= item.y
                    || item.y + item.h <= candidate.y;
            if (!separated) {
                return true;
            }
        }
        return false;
    }

    private String inputDiagnosticText(InputBackendDiagnostics.Snapshot snapshot) {
        boolean shizukuReady = ShizukuNativeController.isReady();
        String shizuku = snapshot.shizukuInstalled
                ? getString(R.string.diagnostic_detected_value, snapshot.shizukuPackageName)
                : getString(R.string.diagnostic_not_detected);
        String root = snapshot.suFound
                ? getString(R.string.diagnostic_possible_value, snapshot.suPath)
                : getString(R.string.diagnostic_not_detected);
        return getString(R.string.diagnostic_backend_title) + "\n"
                + getString(R.string.diagnostic_accessibility, yesNo(snapshot.accessibilityReady)) + "\n"
                + getString(R.string.diagnostic_controller,
                NativeGamepadPath.statusLabel(this, shizukuReady)) + "\n"
                + NativeGamepadPath.debugLabel(this, shizukuReady) + "\n"
                + getString(R.string.diagnostic_shizuku, shizuku) + "\n"
                + getString(R.string.diagnostic_shizuku_status,
                ShizukuNativeController.statusLabel()) + "\n"
                + getString(R.string.diagnostic_root, root) + "\n"
                + getString(R.string.diagnostic_uinput, uinputDiagnosticLabel(snapshot)) + "\n"
                + getString(R.string.diagnostic_uinput_open,
                yesNo(snapshot.uinputOpenProbeOk), snapshot.uinputOpenProbeMessage) + "\n"
                + getString(R.string.diagnostic_recommendation, snapshot.recommendedRoute);
    }

    private String uinputDiagnosticLabel(InputBackendDiagnostics.Snapshot snapshot) {
        if (!snapshot.uinputExists) {
            return getString(R.string.diagnostic_not_detected);
        }
        String direct = getString(R.string.diagnostic_uinput_direct,
                yesNo(snapshot.uinputReadable), yesNo(snapshot.uinputWritable));
        if (ShizukuNativeController.isReady()) {
            return getString(R.string.diagnostic_uinput_shizuku, direct);
        }
        return direct;
    }

    private String yesNo(boolean value) {
        return getString(value ? R.string.common_yes : R.string.common_no);
    }

    private void showMapPanel() {
        switchPlayScreen(SCREEN_MAP);
    }

    private void openMapFullscreen(int mode) {
        if (mode == MAP_VIEW_LOCAL && selectedLocalMap() == null) {
            showErrorAction(getString(R.string.map_select_local_first));
            return;
        }
        if (mode == MAP_VIEW_INTERACTIVE
                && normalizeInteractiveMapUrl(selectedProfile.interactiveMapUrl).length() == 0) {
            showErrorAction(getString(R.string.map_add_interactive_first));
            return;
        }
        if (mode == MAP_VIEW_INTERACTIVE && activeMapWebView != null) {
            String current = activeMapWebView.getUrl();
            if (current != null && (current.startsWith("https://") || current.startsWith("http://"))) {
                mapWebCurrentUrl = current;
            }
        }
        activeMapViewerMode = mode;
        mapViewerFullscreen = true;
        rebuildContent();
    }

    private View createFullscreenMapLayout() {
        profileIconView = null;
        profileTitle = null;
        systemTimeText = null;
        systemNetworkIcon = null;
        systemBatteryIcon = null;
        systemBatteryText = null;
        statusText = null;
        profileList = null;
        touchPadView = null;
        macroGrids.clear();

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(HeimdallUi.isPearl(this) ? 0xFFD4DCE3 : BG);
        applySystemGestureExclusion(root);

        if (activeMapViewerMode == MAP_VIEW_LOCAL) {
            addFullscreenLocalMap(root);
        } else {
            addFullscreenInteractiveMap(root);
        }
        return root;
    }

    private void addFullscreenLocalMap(FrameLayout root) {
        MapEntry map = selectedLocalMap();
        if (map == null) {
            closeMapFullscreen();
            return;
        }
        Bitmap bitmap = loadLocalMapBitmap(map.uri, map.title,
                Math.max(dp(2400), getResources().getDisplayMetrics().widthPixels * 3));
        activeLocalMapBitmap = bitmap;
        if (bitmap == null) {
            TextView error = text(getString(R.string.map_read_error),
                    14, DANGER, false);
            error.setGravity(Gravity.CENTER);
            root.addView(error, new FrameLayout.LayoutParams(-1, -1));
            LinearLayout errorToolbar = fullscreenMapToolbar(
                    nonEmpty(map.title, getString(R.string.map_local)));
            addMapIconTool(errorToolbar, R.drawable.ic_open_external,
                    getString(R.string.common_open_external), this::openProfileMap);
            addMapIconTool(errorToolbar, R.drawable.ic_fullscreen_exit,
                    getString(R.string.common_exit_fullscreen), this::closeMapFullscreen);
            installFullscreenMapControls(root, errorToolbar, error);
            return;
        }

        ZoomableMapView viewer = new ZoomableMapView(this);
        viewer.setImageBitmap(bitmap);
        viewer.setBackgroundColor(0xFF030507);
        root.addView(viewer, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout toolbar = fullscreenMapToolbar(
                nonEmpty(map.title, getString(R.string.map_local)));
        addMapIconTool(toolbar, R.drawable.ic_refresh,
                getString(R.string.map_reset_zoom), viewer::resetZoom);
        addMapIconTool(toolbar, R.drawable.ic_open_external,
                getString(R.string.common_open_external), this::openProfileMap);
        addMapIconTool(toolbar, R.drawable.ic_fullscreen_exit,
                getString(R.string.common_exit_fullscreen), this::closeMapFullscreen);
        installFullscreenMapControls(root, toolbar, viewer);
    }

    private void addFullscreenInteractiveMap(FrameLayout root) {
        LinearLayout toolbar = fullscreenMapToolbar(
                nonEmpty(selectedProfile.interactiveMapTitle,
                        getString(R.string.map_interactive)));
        addMapIconTool(toolbar, R.drawable.ic_arrow_back,
                getString(R.string.common_previous), () -> {
            if (activeMapWebView != null && activeMapWebView.canGoBack()) {
                activeMapWebView.goBack();
            }
        });
        addMapIconTool(toolbar, R.drawable.ic_arrow_forward,
                getString(R.string.common_next), () -> {
            if (activeMapWebView != null && activeMapWebView.canGoForward()) {
                activeMapWebView.goForward();
            }
        });
        addMapIconTool(toolbar, R.drawable.ic_refresh,
                getString(R.string.common_refresh), () -> {
            if (activeMapWebView != null) {
                activeMapWebView.reload();
            }
        });
        addMapIconTool(toolbar, R.drawable.ic_open_external,
                getString(R.string.common_open_external), this::openInteractiveMapExternally);
        addMapIconTool(toolbar, R.drawable.ic_fullscreen_exit,
                getString(R.string.common_exit_fullscreen), this::closeMapFullscreen);

        try {
            activeMapWebView = buildInteractiveMapWebView();
            root.addView(activeMapWebView, new FrameLayout.LayoutParams(-1, -1));
            activeMapWebStatus = floatingMapStatus();
            FrameLayout.LayoutParams statusParams = new FrameLayout.LayoutParams(-2, dp(30));
            statusParams.gravity = Gravity.LEFT | Gravity.BOTTOM;
            statusParams.setMargins(dp(10), 0, dp(10), dp(10));
            root.addView(activeMapWebStatus, statusParams);
            installFullscreenMapControls(root, toolbar, activeMapWebView);
            loadInteractiveMap(activeMapWebView);
        } catch (Exception ex) {
            activeMapWebView = null;
            TextView error = text(getString(R.string.map_embedded_browser_unavailable),
                    14, DANGER, false);
            error.setGravity(Gravity.CENTER);
            root.addView(error, new FrameLayout.LayoutParams(-1, -1));
            installFullscreenMapControls(root, toolbar, error);
        }
    }

    private LinearLayout fullscreenMapToolbar(String title) {
        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setOrientation(LinearLayout.HORIZONTAL);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.setPadding(dp(8), dp(3), dp(4), dp(3));
        toolbar.setBackground(HeimdallUi.isPearl(this)
                ? HeimdallUi.cncFlush(this, HeimdallUi.RADIUS_PANEL)
                : HeimdallUi.glass(this, 0xDD111824, 0xEE080C12,
                        HeimdallUi.COLOR_SYSTEM_BORDER_TOP, HeimdallUi.COLOR_SYSTEM_BORDER_BOTTOM,
                        HeimdallUi.RADIUS_PANEL, 2));
        TextView titleView = text(title, 13, TEXT, true);
        titleView.setSingleLine(true);
        toolbar.addView(titleView, new LinearLayout.LayoutParams(0, -1, 1));
        return toolbar;
    }

    private void addMapIconTool(LinearLayout toolbar, int iconRes, String description, Runnable action) {
        ImageButton button = compactMapIconButton(iconRes, description, action);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(42), dp(42));
        params.setMargins(dp(2), 0, dp(2), 0);
        toolbar.addView(button, params);
    }

    private ImageButton compactMapIconButton(int iconRes, String description, Runnable action) {
        ImageButton button = new ImageButton(this);
        button.setImageResource(iconRes);
        button.setColorFilter(HeimdallUi.textColor(this));
        button.setContentDescription(description);
        button.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        button.setPadding(dp(10), dp(10), dp(10), dp(10));
        button.setBackground(HeimdallUi.isPearl(this)
                ? HeimdallUi.pearlMenuControl(this, HeimdallUi.RADIUS_BUTTON, false, false)
                : HeimdallUi.glass(this, 0xB20F1622, 0xD0090E16,
                        0x665F7C9A, 0x33344150, HeimdallUi.RADIUS_BUTTON, 1));
        button.setOnClickListener(v -> {
            action.run();
            if (mapViewerFullscreen) {
                showFullscreenMapControls();
            }
        });
        return button;
    }

    private void installFullscreenMapControls(FrameLayout root, LinearLayout toolbar, View content) {
        FrameLayout.LayoutParams toolbarParams = new FrameLayout.LayoutParams(-1, dp(50));
        toolbarParams.gravity = Gravity.TOP;
        toolbarParams.setMargins(dp(8), dp(8), dp(8), 0);
        root.addView(toolbar, toolbarParams);
        fullscreenMapControls = toolbar;

        ImageButton reveal = compactMapIconButton(R.drawable.ic_toolbar_reveal,
                getString(R.string.map_show_navigation), this::showFullscreenMapControls);
        reveal.setBackground(HeimdallUi.isPearl(this)
                ? HeimdallUi.pearlMenuControl(this, 8, false, false)
                : HeimdallUi.glass(this, 0xA6111824, 0xC9080C12,
                        HeimdallUi.COLOR_SYSTEM_BORDER_TOP, HeimdallUi.COLOR_SYSTEM_BORDER_BOTTOM, 8, 1));
        FrameLayout.LayoutParams revealParams = new FrameLayout.LayoutParams(dp(46), dp(24));
        revealParams.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        revealParams.setMargins(0, dp(3), 0, 0);
        root.addView(reveal, revealParams);
        fullscreenMapReveal = reveal;

        content.setOnTouchListener((view, event) -> {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN
                    && (event.getY() <= dp(26) || event.getY() >= view.getHeight() - dp(26))) {
                showFullscreenMapControls();
            }
            return false;
        });
        toolbar.setOnTouchListener((view, event) -> {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                showFullscreenMapControls();
            }
            return false;
        });
        showFullscreenMapControls();
    }

    private void showFullscreenMapControls() {
        setFullscreenMapControlsVisible(true);
        uiHandler.removeCallbacks(hideFullscreenMapControls);
        uiHandler.postDelayed(hideFullscreenMapControls, 3000);
    }

    private void setFullscreenMapControlsVisible(boolean visible) {
        if (fullscreenMapControls != null) {
            fullscreenMapControls.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
        if (fullscreenMapReveal != null) {
            fullscreenMapReveal.setVisibility(visible ? View.GONE : View.VISIBLE);
        }
    }

    private void closeMapFullscreen() {
        uiHandler.removeCallbacks(hideFullscreenMapControls);
        if (activeMapWebView != null) {
            String current = activeMapWebView.getUrl();
            if (current != null && (current.startsWith("https://") || current.startsWith("http://"))) {
                mapWebCurrentUrl = current;
            }
        }
        fullscreenMapControls = null;
        fullscreenMapReveal = null;
        mapViewerFullscreen = false;
        rebuildContent();
    }

    private View createMapPage() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackground(HeimdallUi.isPearl(this)
                ? HeimdallUi.cncFlush(this, 12)
                : HeimdallUi.surfacePanel(this, 12));
        root.setPadding(dp(8), dp(8), dp(8), dp(8));

        LinearLayout modes = new LinearLayout(this);
        modes.setOrientation(LinearLayout.HORIZONTAL);
        modes.setPadding(dp(3), dp(3), dp(3), dp(3));
        modes.setBackground(HeimdallUi.isPearl(this)
                ? new ColorDrawable(Color.TRANSPARENT)
                : HeimdallUi.insetPanel(this, HeimdallUi.RADIUS_BUTTON));
        LinearLayout.LayoutParams modeParams = new LinearLayout.LayoutParams(-1, dp(46));
        modeParams.setMargins(0, dp(2), 0, dp(8));
        root.addView(modes, modeParams);

        Button local = editorButton(getString(R.string.map_local), () -> {
            activeMapViewerMode = MAP_VIEW_LOCAL;
            editingInteractiveMapInline = false;
            rebuildContent();
        });
        HeimdallUi.applySectionButton(this, local, activeMapViewerMode == MAP_VIEW_LOCAL);
        modes.addView(local, new LinearLayout.LayoutParams(0, -1, 1));

        Button interactive = editorButton(getString(R.string.map_interactive), () -> {
            activeMapViewerMode = MAP_VIEW_INTERACTIVE;
            editingMapMarkerInline = null;
            creatingMapMarkerInline = false;
            rebuildContent();
        });
        HeimdallUi.applySectionButton(this, interactive, activeMapViewerMode == MAP_VIEW_INTERACTIVE);
        LinearLayout.LayoutParams interactiveParams = new LinearLayout.LayoutParams(0, -1, 1);
        interactiveParams.setMargins(dp(4), 0, 0, 0);
        modes.addView(interactive, interactiveParams);

        View content = activeMapViewerMode == MAP_VIEW_INTERACTIVE
                ? createInteractiveMapPage()
                : createLocalMapPage();
        root.addView(content, new LinearLayout.LayoutParams(-1, 0, 1));
        return root;
    }

    private View createLocalMapPage() {
        List<MapEntry> maps = selectedProfile.safeMaps();
        activeLocalMapIndex = maps.isEmpty()
                ? 0
                : Math.max(0, Math.min(activeLocalMapIndex, maps.size() - 1));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(6), 0, dp(6), dp(4));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams actionsParams = new LinearLayout.LayoutParams(-1, dp(42));
        actionsParams.setMargins(0, 0, 0, dp(4));
        root.addView(actions, actionsParams);
        actions.addView(mapLibraryAction(getString(R.string.map_add), R.drawable.ic_add,
                true, this::chooseMapFile));
        actions.addView(mapLibraryAction(getString(R.string.map_rename), R.drawable.ic_edit,
                !maps.isEmpty(), this::renameSelectedLocalMap));
        actions.addView(mapLibraryAction(getString(R.string.map_delete), R.drawable.ic_trash,
                !maps.isEmpty(), this::deleteSelectedLocalMap));

        ScrollView scroller = new ScrollView(this);
        scroller.setFillViewport(true);
        scroller.setVerticalScrollBarEnabled(false);
        LinearLayout.LayoutParams scrollerParams = new LinearLayout.LayoutParams(-1, 0, 1);
        scrollerParams.setMargins(0, 0, 0, dp(5));
        root.addView(scroller, scrollerParams);

        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(2);
        grid.setAlignmentMode(GridLayout.ALIGN_BOUNDS);
        grid.setUseDefaultMargins(false);
        grid.setPadding(0, 0, 0, dp(4));
        scroller.addView(grid, new ScrollView.LayoutParams(-1, -2));

        if (maps.isEmpty()) {
            TextView empty = text(getString(R.string.map_empty), 13, MUTED, false);
            empty.setGravity(Gravity.CENTER);
            empty.setBackground(HeimdallUi.isPearl(this)
                    ? HeimdallUi.pearlMenuPanel(this, 10)
                    : HeimdallUi.insetPanel(this, 10));
            GridLayout.LayoutParams emptyParams = new GridLayout.LayoutParams(
                    GridLayout.spec(0), GridLayout.spec(0, 2, 1f));
            emptyParams.width = 0;
            emptyParams.height = dp(180);
            emptyParams.setMargins(dp(4), dp(4), dp(4), dp(4));
            grid.addView(empty, emptyParams);
        } else {
            for (int i = 0; i < maps.size(); i++) {
                final int index = i;
                View tile = createLocalMapTile(maps.get(i), index, index == activeLocalMapIndex);
                GridLayout.LayoutParams tileParams = new GridLayout.LayoutParams(
                        GridLayout.spec(i / 2), GridLayout.spec(i % 2, 1f));
                tileParams.width = 0;
                tileParams.height = dp(188);
                tileParams.setMargins(dp(4), dp(4), dp(4), dp(4));
                grid.addView(tile, tileParams);
            }
        }

        return root;
    }

    private View createLocalMapTile(MapEntry map, int index, boolean selected) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(4), dp(4), dp(4), dp(4));
        card.setBackground(HeimdallUi.isPearl(this)
                ? HeimdallUi.pearlMenuControl(this, HeimdallUi.RADIUS_CARD, selected, false)
                : HeimdallUi.glass(this,
                        selected ? 0xB2111A26 : 0xA6101722,
                        selected ? 0xD0080D14 : 0xC9080C12,
                        selected ? 0xBB70B7FF : 0x665F7C9A,
                        selected ? 0x55445A72 : 0x33344150,
                        HeimdallUi.RADIUS_CARD, selected ? 2 : 1));

        FrameLayout preview = new FrameLayout(this);
        preview.setBackgroundColor(0xFF05070A);
        card.addView(preview, new LinearLayout.LayoutParams(-1, 0, 1));

        Bitmap thumbnail = loadLocalMapBitmap(map.uri, map.title, dp(480));
        if (thumbnail != null) {
            activeLocalMapThumbnails.add(thumbnail);
            ImageView image = new ImageView(this);
            image.setImageBitmap(thumbnail);
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            preview.addView(image, new FrameLayout.LayoutParams(-1, -1));
        } else {
            TextView unavailable = text(getString(R.string.map_preview_unavailable),
                    12, MUTED, false);
            unavailable.setGravity(Gravity.CENTER);
            preview.addView(unavailable, new FrameLayout.LayoutParams(-1, -1));
        }

        if (selected) {
            ImageView check = new ImageView(this);
            check.setImageResource(R.drawable.ic_check);
            check.setColorFilter(HeimdallUi.isPearl(this) ? 0xFF9B4C12 : 0xFFD7EEFF);
            check.setPadding(dp(4), dp(4), dp(4), dp(4));
            check.setBackground(HeimdallUi.isPearl(this)
                    ? HeimdallUi.pearlMenuControl(this, HeimdallUi.RADIUS_SMALL, true, false)
                    : HeimdallUi.glass(this, 0xD0121C29, 0xE0080D14,
                            0xAA70B7FF, 0x55445A72, HeimdallUi.RADIUS_SMALL, 1));
            FrameLayout.LayoutParams checkParams = new FrameLayout.LayoutParams(dp(24), dp(24));
            checkParams.gravity = Gravity.TOP | Gravity.RIGHT;
            checkParams.setMargins(0, dp(6), dp(6), 0);
            preview.addView(check, checkParams);
        }

        TextView title = text(nonEmpty(map.title,
                getString(R.string.map_numbered, index + 1)), 12, TEXT, true);
        title.setSingleLine(true);
        title.setPadding(dp(6), 0, dp(6), 0);
        card.addView(title, new LinearLayout.LayoutParams(-1, dp(34)));
        card.setOnClickListener(view -> {
            activeLocalMapIndex = index;
            openMapFullscreen(MAP_VIEW_LOCAL);
        });
        return card;
    }

    private Button mapLibraryAction(String label, int iconRes, boolean enabled, Runnable action) {
        Button button = actionButton(label, action);
        HeimdallUi.applySecondaryButton(this, button);
        button.setTextSize(HeimdallUi.TYPE_BUTTON_COMPACT);
        button.setGravity(Gravity.CENTER);
        button.setMinHeight(0);
        button.setMinimumHeight(0);
        button.setPadding(dp(7), 0, dp(7), 0);
        button.setCompoundDrawablePadding(dp(4));
        setLeftIcon(button, iconRes,
                enabled ? HeimdallUi.textColor(this) : HeimdallUi.mutedTextColor(this), dp(15));
        button.setEnabled(enabled);
        button.setAlpha(enabled ? 1f : (HeimdallUi.isPearl(this) ? 0.55f : 0.45f));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -1, 1);
        params.setMargins(dp(3), dp(2), dp(3), dp(2));
        button.setLayoutParams(params);
        return button;
    }

    private Bitmap loadLocalMapBitmap(String uriString, String fileName, int maxSide) {
        if (uriString == null || uriString.trim().length() == 0) {
            return null;
        }
        Uri uri = Uri.parse(uriString.contains("://") ? uriString : "file://" + uriString);
        if (isPdfDocument(uriString, fileName)) {
            return renderPdfFirstPage(uri, maxSide);
        }
        if (isImageDocument(uriString, fileName)) {
            return decodeImagePreview(uri, maxSide);
        }
        return null;
    }

    private View createInteractiveMapPage() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(4), 0, dp(4), dp(4));

        String savedUrl = selectedProfile.interactiveMapUrl == null
                ? ""
                : selectedProfile.interactiveMapUrl.trim();
        if (editingInteractiveMapInline || savedUrl.length() == 0) {
            addInteractiveMapEditor(panel);
            if (savedUrl.length() == 0) {
            TextView empty = text(getString(R.string.map_interactive_empty),
                        13, MUTED, false);
                empty.setGravity(Gravity.TOP | Gravity.LEFT);
                empty.setPadding(dp(12), dp(12), dp(12), dp(12));
                empty.setBackground(HeimdallUi.insetPanel(this, 10));
                panel.addView(empty, new LinearLayout.LayoutParams(-1, dp(92)));
            }
            return panel;
        }

        LinearLayout toolbar = fullscreenMapToolbar(
                nonEmpty(selectedProfile.interactiveMapTitle,
                        getString(R.string.map_interactive)));
        addMapIconTool(toolbar, R.drawable.ic_arrow_back,
                getString(R.string.common_previous), () -> {
            if (activeMapWebView != null && activeMapWebView.canGoBack()) {
                activeMapWebView.goBack();
            }
        });
        addMapIconTool(toolbar, R.drawable.ic_arrow_forward,
                getString(R.string.common_next), () -> {
            if (activeMapWebView != null && activeMapWebView.canGoForward()) {
                activeMapWebView.goForward();
            }
        });
        addMapIconTool(toolbar, R.drawable.ic_refresh,
                getString(R.string.common_refresh), () -> {
            if (activeMapWebView != null) {
                activeMapWebView.reload();
            }
        });
        addMapIconTool(toolbar, R.drawable.ic_fullscreen,
                getString(R.string.common_fullscreen),
                () -> openMapFullscreen(MAP_VIEW_INTERACTIVE));
        addMapIconTool(toolbar, R.drawable.ic_edit,
                getString(R.string.map_edit_interactive), () -> {
            editingInteractiveMapInline = true;
            rebuildContent();
        });
        addMapIconTool(toolbar, R.drawable.ic_open_external,
                getString(R.string.common_open_external), this::openInteractiveMapExternally);
        panel.addView(toolbar, new LinearLayout.LayoutParams(-1, dp(50)));

        FrameLayout browserFrame = new FrameLayout(this);
        LinearLayout.LayoutParams browserParams = new LinearLayout.LayoutParams(-1, 0, 1);
        browserParams.setMargins(0, dp(4), 0, 0);
        panel.addView(browserFrame, browserParams);

        try {
            activeMapWebView = buildInteractiveMapWebView();
            browserFrame.addView(activeMapWebView, new FrameLayout.LayoutParams(-1, -1));
            activeMapWebStatus = floatingMapStatus();
            FrameLayout.LayoutParams statusParams = new FrameLayout.LayoutParams(-2, dp(30));
            statusParams.gravity = Gravity.LEFT | Gravity.BOTTOM;
            statusParams.setMargins(dp(8), 0, dp(8), dp(8));
            browserFrame.addView(activeMapWebStatus, statusParams);
            loadInteractiveMap(activeMapWebView);
        } catch (Exception ex) {
            activeMapWebView = null;
            TextView error = text(getString(R.string.map_embedded_browser_unavailable),
                    13, DANGER, false);
            error.setGravity(Gravity.CENTER);
            browserFrame.addView(error, new FrameLayout.LayoutParams(-1, -1));
        }
        return panel;
    }

    private TextView floatingMapStatus() {
        TextView status = text(getString(R.string.map_loading), 11, MUTED, false);
        status.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
        status.setPadding(dp(10), 0, dp(10), 0);
        status.setBackground(HeimdallUi.surfacePanel(this, 8));
        return status;
    }

    private void addInteractiveMapEditor(LinearLayout panel) {
        LinearLayout card = inlineEditorCard(panel,
                selectedProfile.interactiveMapUrl == null || selectedProfile.interactiveMapUrl.trim().length() == 0
                        ? getString(R.string.map_add_interactive)
                        : getString(R.string.map_edit_interactive));
        EditText titleInput = inlineEditText(
                nonEmpty(selectedProfile.interactiveMapTitle,
                        getString(R.string.map_interactive)),
                getString(R.string.map_name));
        card.addView(titleInput, blockParams(42, 0, 8));
        EditText urlInput = inlineEditText(
                selectedProfile.interactiveMapUrl == null ? "" : selectedProfile.interactiveMapUrl,
                "https://map.example.com");
        urlInput.setSingleLine(true);
        card.addView(urlInput, blockParams(42, 0, 8));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        card.addView(actions, new LinearLayout.LayoutParams(-1, dp(48)));
        if (selectedProfile.interactiveMapUrl != null
                && selectedProfile.interactiveMapUrl.trim().length() > 0) {
            actions.addView(editorButton(getString(R.string.common_cancel), () -> {
                editingInteractiveMapInline = false;
                rebuildContent();
            }));
        }
        actions.addView(editorButton(getString(R.string.map_save_and_open), () -> {
            String normalized = normalizeInteractiveMapUrl(urlInput.getText().toString());
            if (normalized.length() == 0) {
            showErrorAction(getString(R.string.map_invalid_url));
                return;
            }
            selectedProfile.interactiveMapTitle = nonEmpty(titleInput.getText().toString(),
                    getString(R.string.map_interactive));
            selectedProfile.interactiveMapUrl = normalized;
            ProfileStore.saveProfiles(this, profiles);
            editingInteractiveMapInline = false;
            activeMapViewerMode = MAP_VIEW_INTERACTIVE;
            mapWebCurrentUrl = null;
            rebuildContent();
        }));
    }

    private WebView buildInteractiveMapWebView() {
        WebView webView = new WebView(this);
        webView.setBackgroundColor(0xFF05070A);
        webView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setAllowFileAccessFromFileURLs(false);
        settings.setAllowUniversalAccessFromFileURLs(false);
        settings.setMediaPlaybackRequiresUserGesture(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            settings.setSafeBrowsingEnabled(true);
        }
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int progress) {
                if (progress < 100) {
                updateMapWebStatus(getString(R.string.map_loading_progress, progress), false);
                }
            }
        });
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                activeMapWebError = false;
                updateMapWebStatus(getString(R.string.map_loading), false);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return handleInteractiveMapNavigation(request.getUrl());
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return handleInteractiveMapNavigation(Uri.parse(url));
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                if (url != null && (url.startsWith("https://") || url.startsWith("http://"))) {
                    mapWebCurrentUrl = url;
                }
                if (!activeMapWebError) {
                updateMapWebStatus(getString(R.string.map_loaded), false);
                }
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame()) {
                    activeMapWebError = true;
                updateMapWebStatus(getString(R.string.map_load_failed), true);
                }
            }
        });
        return webView;
    }

    private void loadInteractiveMap(WebView webView) {
        String baseUrl = normalizeInteractiveMapUrl(selectedProfile.interactiveMapUrl);
        String url = normalizeInteractiveMapUrl(mapWebCurrentUrl);
        webView.loadUrl(url.length() == 0 ? baseUrl : url);
    }

    private boolean handleInteractiveMapNavigation(Uri uri) {
        String scheme = uri == null ? "" : nonEmpty(uri.getScheme(), "").toLowerCase(Locale.US);
        if ("http".equals(scheme) || "https".equals(scheme)) {
            return false;
        }
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } catch (Exception ex) {
                updateMapWebStatus(getString(R.string.map_link_open_failed), true);
        }
        return true;
    }

    private String normalizeInteractiveMapUrl(String raw) {
        String value = raw == null ? "" : raw.trim();
        if (value.length() == 0) {
            return "";
        }
        if (!value.contains("://")) {
            value = "https://" + value;
        }
        try {
            Uri uri = Uri.parse(value);
            String scheme = nonEmpty(uri.getScheme(), "").toLowerCase(Locale.US);
            if (("http".equals(scheme) || "https".equals(scheme))
                    && uri.getHost() != null && uri.getHost().trim().length() > 0) {
                return uri.toString();
            }
        } catch (Exception ignored) {
        }
        return "";
    }

    private void openInteractiveMapExternally() {
        String url = normalizeInteractiveMapUrl(selectedProfile.interactiveMapUrl);
        if (url.length() == 0) {
            showErrorAction(getString(R.string.map_add_interactive_first));
            return;
        }
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception ex) {
            showErrorAction(getString(R.string.map_external_browser_failed));
        }
    }

    private void updateMapWebStatus(String message, boolean error) {
        if (activeMapWebStatus == null) {
            return;
        }
        activeMapWebStatus.setText(message);
        activeMapWebStatus.setTextColor(error ? DANGER : MUTED);
    }

    private void chooseMapFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/*", "application/pdf", "text/html"});
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        try {
            startActivityForResult(intent, REQUEST_MAP_FILE);
        } catch (Exception ex) {
            showErrorAction(getString(R.string.error_system_file_picker));
        }
    }

    private void openProfileMap() {
        MapEntry map = selectedLocalMap();
        if (map == null || map.uri.trim().length() == 0) {
            showErrorAction(getString(R.string.map_choose_file_first));
            return;
        }
        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri uri = Uri.parse(map.uri.trim());
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        boolean pdf = isPdfDocument(map.uri, map.title);
        if (pdf) {
            intent.setDataAndType(uri, "application/pdf");
        } else {
            intent.setDataAndType(uri, "*/*");
        }
        startExternalActivity(intent, getString(R.string.map_open_chooser),
                R.string.map_open_failed, pdf);
    }

    private MapEntry selectedLocalMap() {
        List<MapEntry> maps = selectedProfile.safeMaps();
        if (maps.isEmpty()) {
            activeLocalMapIndex = 0;
            return null;
        }
        activeLocalMapIndex = Math.max(0, Math.min(activeLocalMapIndex, maps.size() - 1));
        return maps.get(activeLocalMapIndex);
    }

    private void renameSelectedLocalMap() {
        MapEntry map = selectedLocalMap();
        if (map == null) {
            return;
        }

        LinearLayout shell = new LinearLayout(this);
        shell.setOrientation(LinearLayout.VERTICAL);
        shell.setPadding(dp(16), dp(14), dp(16), dp(12));
        shell.setBackground(HeimdallUi.isPearl(this)
                ? HeimdallUi.cncFlush(this, 14)
                : HeimdallUi.glass(this, 0xFA0B111B, 0xFF070A10,
                        0x886A829C, 0x44344150, 14, 2));

        TextView title = text(getString(R.string.map_rename_title),
                HeimdallUi.TYPE_EDITOR_TITLE, TEXT, true);
        shell.addView(title, new LinearLayout.LayoutParams(-1, dp(38)));

        TextView label = text(getString(R.string.map_name),
                HeimdallUi.TYPE_LABEL, MUTED, true);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(-1, dp(26));
        labelParams.setMargins(0, dp(4), 0, 0);
        shell.addView(label, labelParams);

        EditText input = inlineEditText(nonEmpty(map.title,
                getString(R.string.common_map_fallback)), getString(R.string.map_name));
        input.setSingleLine(true);
        input.setSelectAllOnFocus(true);
        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(-1, dp(48));
        inputParams.setMargins(0, 0, 0, dp(14));
        shell.addView(input, inputParams);

        View divider = new View(this);
        divider.setBackgroundColor(HeimdallUi.isPearl(this) ? 0x287B8792 : 0x445F7C9A);
        shell.addView(divider, new LinearLayout.LayoutParams(-1, dp(1)));

        final PanelOverlay[] overlayHolder = new PanelOverlay[1];
        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setPadding(0, dp(5), 0, 0);
        shell.addView(actions, new LinearLayout.LayoutParams(-1, dp(56)));
        actions.addView(editorButton(getString(R.string.common_cancel), () -> {
            if (overlayHolder[0] != null) {
                dismissPanelAnimated(overlayHolder[0]);
            }
        }));
        Button save = editorButton(getString(R.string.common_save), () -> {
            if (overlayHolder[0] == null) {
                return;
            }
            dismissPanelAnimated(overlayHolder[0], () -> {
                map.title = nonEmpty(input.getText().toString(),
                        getString(R.string.common_map_fallback));
                selectedProfile.syncLegacyMapFields();
                ProfileStore.saveProfiles(this, profiles);
                rebuildContent();
            });
        });
        HeimdallUi.applyPrimaryActionButton(this, save);
        actions.addView(save);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                settingsOverlayWidth(560), -2, Gravity.CENTER);
        params.setMargins(dp(12), dp(12), dp(12), dp(12));
        overlayHolder[0] = showPanelOverlay(shell, params, null);
        requestTextInputFocus(input);
    }

    private void deleteSelectedLocalMap() {
        MapEntry map = selectedLocalMap();
        if (map == null) {
            return;
        }
        showSettingsDecisionPanel(
                getString(R.string.map_delete_title),
                getString(R.string.map_delete_message,
                        nonEmpty(map.title, getString(R.string.common_map_fallback))),
                null,
                null,
                getString(R.string.common_delete),
                () -> {
                    selectedProfile.safeMaps().remove(activeLocalMapIndex);
                    activeLocalMapIndex = Math.max(0,
                            Math.min(activeLocalMapIndex, selectedProfile.safeMaps().size() - 1));
                    selectedProfile.syncLegacyMapFields();
                    ProfileStore.saveProfiles(this, profiles);
                    rebuildContent();
                });
    }

    private String documentDisplayName(Uri uri) {
        Cursor cursor = null;
        try {
            cursor = getContentResolver().query(uri,
                    new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (index >= 0) {
                    String name = cursor.getString(index);
                    if (name != null && name.trim().length() > 0) {
                        return name.trim();
                    }
                }
            }
        } catch (Exception ignored) {
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return "";
    }

    private String stripFileExtension(String name) {
        String value = name == null ? "" : name.trim();
        int dot = value.lastIndexOf('.');
        return dot > 0 ? value.substring(0, dot) : value;
    }

    private String markerLabel(MapMarker marker) {
        String position = nonEmpty(marker.position,
                getString(R.string.map_marker_position_missing));
        return getString(R.string.map_marker_summary,
                nonEmpty(marker.title, getString(R.string.map_marker_fallback)), position);
    }

    private void showMapMarkerDetail(MapMarker marker) {
        showInformationPanel(
                nonEmpty(marker.title, getString(R.string.map_marker_title)),
                getString(R.string.map_marker_detail,
                        nonEmpty(marker.position, getString(R.string.common_not_set)),
                        nonEmpty(marker.note, getString(R.string.map_marker_no_note))));
    }

    private void showMapMarkerEditor(AlertDialog[] parentDialogHolder, MapMarker editingMarker) {
        editingMapMarkerInline = editingMarker;
        creatingMapMarkerInline = editingMarker == null;
        showMapPanel();
    }

    private void addMapMarkerInlineEditor(LinearLayout panel, MapMarker editingMarker) {
        boolean editing = editingMarker != null;
        LinearLayout card = inlineEditorCard(panel, getString(editing
                ? R.string.map_marker_edit : R.string.map_marker_add));

        TextView titleLabel = text(getString(R.string.map_marker_name), 12, MUTED, true);
        card.addView(titleLabel, new LinearLayout.LayoutParams(-1, dp(22)));
        EditText titleInput = inlineEditText(editing ? editingMarker.title : "",
                getString(R.string.map_marker_name_hint));
        card.addView(titleInput, blockParams(42, 0, 8));

        TextView positionLabel = text(getString(R.string.map_marker_position),
                12, MUTED, true);
        card.addView(positionLabel, new LinearLayout.LayoutParams(-1, dp(22)));
        EditText positionInput = inlineEditText(editing ? editingMarker.position : "",
                getString(R.string.map_marker_position_hint));
        card.addView(positionInput, blockParams(42, 0, 8));

        TextView noteLabel = text(getString(R.string.map_marker_note), 12, MUTED, true);
        card.addView(noteLabel, new LinearLayout.LayoutParams(-1, dp(22)));
        EditText noteInput = inlineEditText(editing ? editingMarker.note : "",
                getString(R.string.map_marker_note_hint));
        noteInput.setSingleLine(false);
        noteInput.setMinLines(4);
        noteInput.setGravity(Gravity.TOP | Gravity.LEFT);
        card.addView(noteInput, blockParams(112, 0, 8));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        card.addView(actions, new LinearLayout.LayoutParams(-1, dp(48)));
        actions.addView(editorButton(getString(R.string.common_cancel), () -> {
            editingMapMarkerInline = null;
            creatingMapMarkerInline = false;
            rebuildContent();
        }));
        actions.addView(editorButton(getString(R.string.common_save), () -> {
            String title = nonEmpty(titleInput.getText().toString(),
                    getString(R.string.map_marker_fallback));
            String position = positionInput.getText().toString().trim();
            String note = noteInput.getText().toString().trim();
            if (editing) {
                editingMarker.title = title;
                editingMarker.position = position;
                editingMarker.note = note;
            } else {
                selectedProfile.mapMarkers.add(new MapMarker(title, note, position));
            }
            ProfileStore.saveProfiles(this, profiles);
            editingMapMarkerInline = null;
            creatingMapMarkerInline = false;
            showMapPanel();
        }));
    }

    private void copyMapMarker(MapMarker marker) {
        ClipboardManager manager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        if (manager == null) {
            showErrorAction(getString(R.string.map_marker_copy_failed));
            return;
        }
        String content = getString(R.string.map_marker_clipboard_content,
                nonEmpty(marker.title, getString(R.string.map_marker_fallback)),
                nonEmpty(marker.position, getString(R.string.map_marker_position_missing)),
                nonEmpty(marker.note, ""));
        manager.setPrimaryClip(ClipData.newPlainText(
                nonEmpty(marker.title, getString(R.string.map_marker_title)), content));
        showAction(getString(R.string.map_marker_copied));
    }

    private void showGuidePanel() {
        switchPlayScreen(SCREEN_GUIDE);
    }

    private View createGuidePage() {
        if (viewingGuideInline != null) {
            return createGuideReaderPage(viewingGuideInline);
        }
        if (editingGuideTypeInline != null) {
            return createGuideEditorPage(editingGuideTypeInline, editingGuideInline);
        }
        ScrollView scroller = new ScrollView(this);
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(8);
        panel.setPadding(pad, pad, pad, pad);
        panel.setBackground(HeimdallUi.isPearl(this)
                ? HeimdallUi.cncFlush(this, 12)
                : HeimdallUi.surfacePanel(this, 12));
        scroller.addView(panel, new ScrollView.LayoutParams(-1, -2));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams actionParams = new LinearLayout.LayoutParams(-1, dp(46));
        actionParams.setMargins(0, dp(2), 0, dp(8));
        panel.addView(actions, actionParams);
        actions.addView(pageActionButton(getString(R.string.guide_add_note), R.drawable.ic_edit,
                () -> showGuideEditor(null, GuideEntry.TYPE_NOTE, null)));
        actions.addView(pageActionButton(getString(R.string.guide_add_link), R.drawable.ic_open_external,
                () -> showGuideEditor(null, GuideEntry.TYPE_LINK, null)));
        actions.addView(pageActionButton(getString(R.string.guide_add_file), R.drawable.ic_add,
                () -> showGuideEditor(null, GuideEntry.TYPE_FILE, null)));

        if (selectedProfile.guides.isEmpty()) {
            TextView empty = text(getString(R.string.guide_empty), 13, MUTED, false);
            empty.setGravity(Gravity.CENTER);
            empty.setBackground(HeimdallUi.isPearl(this)
                    ? HeimdallUi.pearlMenuPanel(this, HeimdallUi.RADIUS_CARD)
                    : HeimdallUi.insetPanel(this, HeimdallUi.RADIUS_CARD));
            panel.addView(empty, new LinearLayout.LayoutParams(-1, dp(96)));
        } else {
            for (int i = 0; i < selectedProfile.guides.size(); i++) {
                final int index = i;
                GuideEntry guide = selectedProfile.guides.get(i);
                LinearLayout row = listActionRow(guideLabel(guide),
                        () -> openGuide(guide),
                        () -> showGuideEditor(null, guide.type, guide));
            row.addView(iconButton(R.drawable.ic_copy, getString(R.string.guide_copy_content), false,
                        () -> copyGuideContent(guide)), iconButtonParams());

            row.addView(iconButton(R.drawable.ic_edit, getString(R.string.guide_edit), false,
                        () -> showGuideEditor(null, guide.type, guide)), iconButtonParams());

            row.addView(iconButton(R.drawable.ic_trash, getString(R.string.guide_delete), true, () -> {
                    selectedProfile.guides.remove(index);
                    ProfileStore.saveProfiles(this, profiles);
                    if (editingGuideInline == guide) {
                        editingGuideInline = null;
                        editingGuideTypeInline = null;
                    }
                    if (viewingGuideInline == guide) {
                        flushGuideReadingPosition();
                        viewingGuideInline = null;
                        guideReaderFullscreen = false;
                        invalidateGuideTextCache();
                    }
                    showGuidePanel();
                }), iconButtonParams());
                panel.addView(row, new LinearLayout.LayoutParams(-1, dp(56)));
            }
        }

        return scroller;
    }

    private String guideLabel(GuideEntry guide) {
        String prefix;
        if (GuideEntry.TYPE_LINK.equals(guide.type)) {
            prefix = getString(R.string.guide_type_link);
        } else if (GuideEntry.TYPE_FILE.equals(guide.type)) {
            prefix = getString(R.string.guide_type_file);
        } else {
            prefix = getString(R.string.guide_type_note);
        }
        return getString(R.string.guide_row_summary, prefix,
                nonEmpty(guide.title, getString(R.string.common_guide_fallback)));
    }

    private void showGuideEditor(AlertDialog[] parentDialogHolder, String type, GuideEntry editingGuide) {
        flushGuideReadingPosition();
        invalidateGuideTextCache();
        guideReaderFullscreen = false;
        viewingGuideInline = null;
        editingGuideInline = editingGuide;
        editingGuideTypeInline = GuideEntry.normalizeType(type);
        showGuidePanel();
    }

    private View createGuideEditorPage(String type, GuideEntry editingGuide) {
        boolean editing = editingGuide != null;
        boolean fileType = GuideEntry.TYPE_FILE.equals(type);
        boolean noteType = GuideEntry.TYPE_NOTE.equals(type);

        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(8);
        panel.setPadding(pad, pad, pad, pad);
        panel.setBackground(HeimdallUi.isPearl(this)
                ? HeimdallUi.cncFlush(this, 12)
                : HeimdallUi.surfacePanel(this, 12));

        LinearLayout editor = new LinearLayout(this);
        editor.setOrientation(LinearLayout.VERTICAL);
        editor.setPadding(dp(10), dp(8), dp(10), dp(8));
        editor.setBackground(HeimdallUi.isPearl(this)
                ? HeimdallUi.pearlMenuPanel(this, 10)
                : HeimdallUi.insetPanel(this, 10));
        panel.addView(editor, new LinearLayout.LayoutParams(-1, 0, 1));

        TextView editorTitle = text(getString(editing
                        ? R.string.guide_edit_title : R.string.guide_add_title),
                13, PRIMARY, true);
        editorTitle.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        editor.addView(editorTitle, new LinearLayout.LayoutParams(-1, dp(30)));

        String initialTitle = editing
                ? (editingGuide.title == null ? "" : editingGuide.title)
                : (fileType ? "" : defaultGuideTitle(type));
        EditText titleInput = inlineEditText(initialTitle,
                getString(R.string.guide_title));

        final GuideFileTitleState fileTitleState;
        if (fileType) {
            fileTitleState = new GuideFileTitleState();
            fileTitleState.userEdited = editing && initialTitle.trim().length() > 0;
            titleInput.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence value, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence value, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable value) {
                    if (!fileTitleState.applyingAutomaticTitle) {
                        fileTitleState.userEdited = true;
                    }
                }
            });
        } else {
            fileTitleState = null;
        }

        final String[] fileUriDraft = {fileType && editing && editingGuide.content != null
                ? editingGuide.content : ""};
        final EditText contentInput = fileType ? null : inlineEditText(
                editing && editingGuide.content != null ? editingGuide.content : "",
                guideHint(type));

        if (noteType) {
            bindTapToEditTextInputFocus(contentInput);
            LinearLayout titleRow = new LinearLayout(this);
            titleRow.setOrientation(LinearLayout.HORIZONTAL);
            titleRow.setGravity(Gravity.CENTER_VERTICAL);
            editor.addView(titleRow, new LinearLayout.LayoutParams(-1, dp(48)));

            TextView titleLabel = text(getString(R.string.guide_title), 12, MUTED, true);
            titleLabel.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
            titleRow.addView(titleLabel, new LinearLayout.LayoutParams(dp(58), -1));

            LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, -1, 1);
            titleParams.setMargins(0, dp(3), dp(6), dp(3));
            titleRow.addView(titleInput, titleParams);

            Button importButton = actionButton(getString(R.string.guide_import_text),
                    () -> chooseGuideTextFile(contentInput));
            HeimdallUi.applySecondaryButton(this, importButton);
            importButton.setTextSize(HeimdallUi.TYPE_BUTTON_COMPACT);
            LinearLayout.LayoutParams importParams = new LinearLayout.LayoutParams(dp(180), -1);
            importParams.setMargins(0, dp(3), 0, dp(3));
            titleRow.addView(importButton, importParams);

            TextView contentLabel = text(guideContentLabel(type), 12, MUTED, true);
            contentLabel.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
            editor.addView(contentLabel, new LinearLayout.LayoutParams(-1, dp(26)));

            contentInput.setSingleLine(false);
            contentInput.setMinLines(7);
            contentInput.setGravity(Gravity.TOP | Gravity.START);
            LinearLayout.LayoutParams contentParams = new LinearLayout.LayoutParams(-1, 0, 1);
            contentParams.setMargins(0, 0, 0, dp(8));
            editor.addView(contentInput, contentParams);
        } else {
            ScrollView formScroll = new ScrollView(this);
            formScroll.setFillViewport(true);
            editor.addView(formScroll, new LinearLayout.LayoutParams(-1, 0, 1));

            LinearLayout form = new LinearLayout(this);
            form.setOrientation(LinearLayout.VERTICAL);
            form.setPadding(0, dp(2), 0, dp(8));
            formScroll.addView(form, new ScrollView.LayoutParams(-1, -2));

            TextView titleLabel = text(getString(R.string.guide_title), 12, MUTED, true);
            form.addView(titleLabel, new LinearLayout.LayoutParams(-1, dp(26)));
            form.addView(titleInput, blockParams(42, 0, 8));

            TextView contentLabel = text(guideContentLabel(type), 12, MUTED, true);
            form.addView(contentLabel, new LinearLayout.LayoutParams(-1, dp(26)));

            if (fileType) {
                TextView fileSummary = guideFileSelectionSummary(fileUriDraft[0],
                        editing ? editingGuide.title : "");
                form.addView(fileSummary, blockParams(54, 0, 8));

                LinearLayout fileActions = new LinearLayout(this);
                fileActions.setOrientation(LinearLayout.HORIZONTAL);
                form.addView(fileActions, new LinearLayout.LayoutParams(-1, dp(48)));
                fileActions.addView(editorButton(getString(R.string.guide_choose_file),
                        () -> chooseGuideFile(fileSummary, fileUriDraft,
                                titleInput, fileTitleState)));
            } else {
                contentInput.setSingleLine(false);
                contentInput.setMinLines(3);
                contentInput.setGravity(Gravity.TOP | Gravity.START);
                form.addView(contentInput, blockParams(84, 0, 8));
            }

            TextView help = text(guideHelp(type), 12, 0xFF5F6368, false);
            help.setTextColor(MUTED);
            form.addView(help, blockParams(52, 0, 8));
        }

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        editor.addView(actions, new LinearLayout.LayoutParams(-1, dp(48)));
        actions.addView(editorButton(getString(R.string.common_cancel), () -> {
            editingGuideInline = null;
            editingGuideTypeInline = null;
            showGuidePanel();
        }));
        actions.addView(editorButton(getString(R.string.common_save), () -> {
            String content = fileType ? fileUriDraft[0].trim()
                    : contentInput.getText().toString().trim();
            if (content.length() == 0) {
                showErrorAction(getString(R.string.guide_content_required));
                return;
            }
            if (editing) {
                boolean contentChanged = !GuideEntry.normalizeType(type).equals(editingGuide.type)
                        || !content.equals(editingGuide.content);
                editingGuide.title = nonEmpty(titleInput.getText().toString(), defaultGuideTitle(type));
                editingGuide.type = GuideEntry.normalizeType(type);
                editingGuide.content = content;
                if (contentChanged) {
                    editingGuide.clearReadingPosition();
                }
            } else {
                selectedProfile.guides.add(new GuideEntry(
                        nonEmpty(titleInput.getText().toString(), defaultGuideTitle(type)),
                        type,
                        content));
            }
            ProfileStore.saveProfiles(this, profiles);
            editingGuideInline = null;
            editingGuideTypeInline = null;
            showAction(getString(editing ? R.string.guide_updated : R.string.guide_saved));
            showGuidePanel();
        }));
        return panel;
    }

    private TextView guideFileSelectionSummary(String uriString, String fallbackTitle) {
        String displayName = "";
        if (uriString != null && uriString.trim().length() > 0) {
            try {
                displayName = documentDisplayName(Uri.parse(uriString.trim()));
            } catch (Exception ignored) {
            }
        }
        TextView summary = text(nonEmpty(displayName,
                nonEmpty(fallbackTitle, getString(R.string.guide_no_file))),
                12, TEXT, false);
        summary.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        summary.setPadding(dp(10), dp(6), dp(10), dp(6));
        summary.setMaxLines(2);
        summary.setEllipsize(android.text.TextUtils.TruncateAt.END);
        summary.setBackground(HeimdallUi.fieldPanel(this, 8));
        return summary;
    }

    private void copyGuideContent(GuideEntry guide) {
        ClipboardManager manager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        if (manager == null) {
            showErrorAction(getString(R.string.map_marker_copy_failed));
            return;
        }
        String content = GuideEntry.TYPE_LINK.equals(guide.type) ? guideUri(guide).toString() : guide.content;
        manager.setPrimaryClip(ClipData.newPlainText(
                nonEmpty(guide.title, getString(R.string.common_guide_fallback)), content));
        showAction(getString(R.string.guide_copied));
    }

    private void chooseGuideFile(TextView fileSummary, String[] fileUriDraft,
            EditText titleInput, GuideFileTitleState titleState) {
        if (releaseTextInputFocusThen(() -> chooseGuideFile(
                fileSummary, fileUriDraft, titleInput, titleState))) {
            return;
        }
        pendingGuideFileSummary = fileSummary;
        pendingGuideFileUriDraft = fileUriDraft;
        pendingGuideFileTitleInput = titleInput;
        pendingGuideFileTitleState = titleState;
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"application/pdf", "text/plain", "text/markdown", "text/html", "image/*"});
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        try {
            startActivityForResult(intent, REQUEST_GUIDE_FILE);
        } catch (Exception ex) {
            clearPendingGuideFileSelection();
            showErrorAction(getString(R.string.error_system_file_picker));
        }
    }

    private void clearPendingGuideFileSelection() {
        pendingGuideFileSummary = null;
        pendingGuideFileUriDraft = null;
        pendingGuideFileTitleInput = null;
        pendingGuideFileTitleState = null;
    }

    private void chooseGuideTextFile(EditText targetInput) {
        if (releaseTextInputFocusThen(() -> chooseGuideTextFile(targetInput))) {
            return;
        }
        pendingGuideTextInput = targetInput;
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"text/plain", "text/markdown", "text/*"});
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        try {
            startActivityForResult(intent, REQUEST_GUIDE_TEXT_FILE);
        } catch (Exception ex) {
            showErrorAction(getString(R.string.error_system_file_picker));
        }
    }

    private String defaultGuideTitle(String type) {
        if (GuideEntry.TYPE_LINK.equals(type)) {
            return getString(R.string.guide_default_link);
        }
        if (GuideEntry.TYPE_FILE.equals(type)) {
            return getString(R.string.guide_default_file);
        }
        return getString(R.string.guide_default_note);
    }

    private String guideContentLabel(String type) {
        if (GuideEntry.TYPE_LINK.equals(type)) {
            return getString(R.string.guide_field_link);
        }
        if (GuideEntry.TYPE_FILE.equals(type)) {
            return getString(R.string.guide_field_file);
        }
        return getString(R.string.guide_field_note);
    }

    private String guideHint(String type) {
        if (GuideEntry.TYPE_LINK.equals(type)) {
            return "https://...";
        }
        if (GuideEntry.TYPE_FILE.equals(type)) {
            return getString(R.string.guide_file_hint);
        }
        return getString(R.string.guide_note_hint);
    }

    private String guideHelp(String type) {
        if (GuideEntry.TYPE_LINK.equals(type)) {
            return getString(R.string.guide_link_help);
        }
        if (GuideEntry.TYPE_FILE.equals(type)) {
            return getString(R.string.guide_file_help);
        }
        return getString(R.string.guide_note_help);
    }

    private void openGuide(GuideEntry guide) {
        flushGuideReadingPosition();
        invalidateGuideTextCache();
        guideReaderFullscreen = false;
        if (GuideEntry.TYPE_NOTE.equals(guide.type)) {
            viewingGuideInline = guide;
            editingGuideInline = null;
            editingGuideTypeInline = null;
            showGuidePanel();
            return;
        }

        if (GuideEntry.TYPE_FILE.equals(guide.type)
                && isPdfDocument(guide.content, guide.title)) {
            openGuideExternally(guide);
            return;
        }

        if (GuideEntry.TYPE_FILE.equals(guide.type)
                && canPreviewInline(guide.content, guide.title)) {
            viewingGuideInline = guide;
            editingGuideInline = null;
            editingGuideTypeInline = null;
            showGuidePanel();
            return;
        }

        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri uri = guideUri(guide);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        boolean pdf = false;
        if (GuideEntry.TYPE_FILE.equals(guide.type)) {
            pdf = isPdfDocument(guide.content, guide.title);
            if (pdf) {
                intent.setDataAndType(uri, "application/pdf");
            } else {
                intent.setDataAndType(uri, "*/*");
            }
        } else {
            intent.setData(uri);
        }
        startExternalActivity(intent, getString(R.string.guide_open_chooser),
                R.string.guide_open_failed, pdf);
    }

    private View createGuideReaderPage(GuideEntry guide) {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(8), dp(8), dp(8), dp(8));
        panel.setBackground(HeimdallUi.isPearl(this)
                ? HeimdallUi.cncFlush(this, 12)
                : HeimdallUi.surfacePanel(this, 12));

        if (isInlineTextGuide(guide)) {
            panel.addView(createGuideReaderToolbar(guide, false),
                    new LinearLayout.LayoutParams(-1, dp(48)));
            GuideTextReaderView reader = createGuideTextReaderView(guide);
            reader.setBackground(HeimdallUi.isPearl(this)
                    ? HeimdallUi.pearlMenuPanel(this, 8)
                    : HeimdallUi.rounded(this, HeimdallUi.COLOR_SURFACE_INSET, 0, 8, 0));
            LinearLayout.LayoutParams readerParams = new LinearLayout.LayoutParams(-1, 0, 1);
            readerParams.setMargins(0, dp(4), 0, 0);
            panel.addView(reader, readerParams);
            loadGuideText(reader, guide);
        } else if (GuideEntry.TYPE_FILE.equals(guide.type)) {
            panel.addView(createGuideReaderToolbar(guide, false),
                    new LinearLayout.LayoutParams(-1, dp(48)));
            LinearLayout content = new LinearLayout(this);
            content.setOrientation(LinearLayout.VERTICAL);
            content.setPadding(dp(8), dp(8), dp(8), dp(8));
            panel.addView(content, new LinearLayout.LayoutParams(-1, 0, 1));
            addDocumentPreview(content,
                    nonEmpty(guide.title, getString(R.string.guide_default_file)),
                    guide.content, guide.title, false);
        } else {
            LinearLayout content = new LinearLayout(this);
            content.setOrientation(LinearLayout.VERTICAL);
            content.setPadding(dp(8), dp(8), dp(8), dp(8));
            panel.addView(content, new LinearLayout.LayoutParams(-1, 0, 1));
            addTextPreview(content, guideUri(guide).toString(), 86);
        }
        return panel;
    }

    private boolean isInlineTextGuide(GuideEntry guide) {
        return guide != null && (GuideEntry.TYPE_NOTE.equals(guide.type)
                || (GuideEntry.TYPE_FILE.equals(guide.type)
                && isTextDocument(guide.content, guide.title)));
    }

    private LinearLayout createGuideReaderToolbar(GuideEntry guide, boolean fullscreen) {
        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setOrientation(LinearLayout.HORIZONTAL);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.setPadding(dp(4), dp(3), dp(4), dp(3));
        if (fullscreen) {
            toolbar.setBackground(HeimdallUi.isPearl(this)
                    ? HeimdallUi.cncFlush(this, HeimdallUi.RADIUS_PANEL)
                    : HeimdallUi.glass(this, 0xDD111824, 0xEE080C12,
                            HeimdallUi.COLOR_SYSTEM_BORDER_TOP,
                            HeimdallUi.COLOR_SYSTEM_BORDER_BOTTOM,
                            HeimdallUi.RADIUS_PANEL, 2));
        } else {
            toolbar.setBackgroundColor(Color.TRANSPARENT);
        }
        addGuideIconTool(toolbar, R.drawable.ic_arrow_back,
                fullscreen ? getString(R.string.common_exit_fullscreen)
                        : getString(R.string.guide_close_reader),
                fullscreen ? this::closeGuideFullscreen : this::closeGuideReader);
        TextView title = text(nonEmpty(guide.title, getString(R.string.common_guide_fallback)),
                13, TEXT, true);
        title.setSingleLine(true);
        title.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        toolbar.addView(title, new LinearLayout.LayoutParams(0, -1, 1));

        if (isInlineTextGuide(guide)) {
            String currentLayout = GuideEntry.READER_MODE_ORIGINAL.equals(guide.readerMode)
                    ? getString(R.string.guide_layout_original)
                    : getString(R.string.guide_layout_reading);
            addGuideIconTool(toolbar, R.drawable.ic_reader_layout,
                    getString(R.string.guide_layout_action, currentLayout),
                    () -> showGuideReaderOptions(guide));
            addGuideIconTool(toolbar, R.drawable.ic_bookmark,
                    getString(R.string.guide_bookmark_action),
                    () -> showGuideBookmarks(guide));

            if (!fullscreen) {
                addGuideIconTool(toolbar, R.drawable.ic_fullscreen,
                        getString(R.string.guide_enter_fullscreen), this::openGuideFullscreen);
            }
        }
        if (!GuideEntry.TYPE_NOTE.equals(guide.type)) {
            addGuideIconTool(toolbar, R.drawable.ic_open_external,
                    getString(R.string.common_open_external), () -> openGuideExternally(guide));
        }
        return toolbar;
    }

    private void addGuideIconTool(LinearLayout toolbar, int iconRes, String description,
            Runnable action) {
        ImageButton button = compactMapIconButton(iconRes, description, action);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(42), dp(40));
        params.setMargins(dp(2), 0, dp(2), 0);
        toolbar.addView(button, params);
    }

    private GuideTextReaderView createGuideTextReaderView(GuideEntry guide) {
        boolean original = GuideEntry.READER_MODE_ORIGINAL.equals(guide.readerMode);
        GuideTextReaderView reader = new GuideTextReaderView(this, original);
        reader.showMessage(getString(R.string.guide_loading_text), MUTED);
        return reader;
    }

    private void loadGuideText(GuideTextReaderView reader, GuideEntry guide) {
        flushGuideReadingPosition();
        activeGuideReaderView = reader;
        activeGuideReaderEntry = guide;
        activeGuideReaderFingerprint = "";
        if (cachedGuideTextEntry == guide && cachedGuideTextDocument != null) {
            attachGuideTextDocument(reader, guide, cachedGuideTextDocument);
            return;
        }
        int generation = ++guideTextLoadGeneration;
        GuideTextLoader.load(this, guide, result -> {
            if (generation != guideTextLoadGeneration || viewingGuideInline != guide
                    || activeGuideReaderView != reader) {
                return;
            }
            if (result.document == null) {
                int message = result.failure == GuideTextLoader.Failure.TOO_LARGE
                        ? R.string.guide_text_too_large : R.string.guide_text_read_failed;
                reader.showMessage(getString(message), DANGER);
                return;
            }
            cachedGuideTextEntry = guide;
            cachedGuideTextDocument = result.document;
            attachGuideTextDocument(reader, guide, result.document);
        });
    }

    private void attachGuideTextDocument(GuideTextReaderView reader, GuideEntry guide,
            GuideTextDocument document) {
        activeGuideReaderFingerprint = document.fingerprint;
        boolean restore = guide.hasReadingPositionFor(document.fingerprint);
        GuideTextReaderView.Position position = new GuideTextReaderView.Position(
                restore ? guide.readerAnchor : 0,
                restore ? guide.readerAnchorTop : 0,
                restore ? guide.readerHorizontalColumn : 0,
                restore ? guide.readerViewportWidth : 0);
        reader.setDocument(document, position);
    }

    private void closeGuideReader() {
        flushGuideReadingPosition();
        guideReaderFullscreen = false;
        viewingGuideInline = null;
        invalidateGuideTextCache();
        rebuildContent();
    }

    private void invalidateGuideTextCache() {
        guideTextLoadGeneration++;
        cachedGuideTextEntry = null;
        cachedGuideTextDocument = null;
    }

    private void showGuideReaderOptions(GuideEntry guide) {
        LinearLayout shell = new LinearLayout(this);
        shell.setOrientation(LinearLayout.VERTICAL);
        shell.setPadding(dp(14), dp(12), dp(14), dp(12));
        shell.setBackground(guideReaderOverlayPanel());
        TextView title = text(getString(R.string.guide_layout_title), 15, TEXT, true);
        shell.addView(title, new LinearLayout.LayoutParams(-1, dp(38)));

        PanelOverlay[] holder = new PanelOverlay[1];
        LinearLayout modeRow = new LinearLayout(this);
        modeRow.setOrientation(LinearLayout.HORIZONTAL);
        modeRow.addView(guideReaderOptionButton(getString(R.string.guide_layout_reading),
                GuideEntry.READER_MODE_READING.equals(guide.readerMode),
                () -> applyGuideReaderOptions(holder[0], guide,
                        GuideEntry.READER_MODE_READING)));
        modeRow.addView(guideReaderOptionButton(getString(R.string.guide_layout_original),
                GuideEntry.READER_MODE_ORIGINAL.equals(guide.readerMode),
                () -> applyGuideReaderOptions(holder[0], guide,
                        GuideEntry.READER_MODE_ORIGINAL)));
        shell.addView(modeRow, new LinearLayout.LayoutParams(-1, dp(52)));

        TextView note = text(getString(R.string.guide_layout_help), 11, MUTED, false);
        note.setGravity(Gravity.TOP | Gravity.START);
        note.setPadding(dp(4), dp(6), dp(4), 0);
        shell.addView(note, new LinearLayout.LayoutParams(-1, 0, 1));
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(dp(440), dp(170));
        params.gravity = Gravity.CENTER;
        holder[0] = showPanelOverlay(shell, params, null);
    }

    private Button guideReaderOptionButton(String label, boolean selected, Runnable action) {
        Button button = editorButton(label, action);
        HeimdallUi.applyChoiceButton(this, button, selected);
        return button;
    }

    private void applyGuideReaderOptions(PanelOverlay overlay, GuideEntry guide,
            String mode) {
        Runnable apply = () -> {
            flushGuideReadingPosition();
            if (guide.updateReaderPresentation(mode)) {
                ProfileStore.saveProfiles(this, profiles);
            }
            rebuildContent();
        };
        if (overlay != null) {
            dismissPanelAnimated(overlay, apply);
        } else {
            apply.run();
        }
    }

    private void showGuideBookmarks(GuideEntry guide) {
        if (guide == null || viewingGuideInline != guide) {
            return;
        }
        LinearLayout shell = new LinearLayout(this);
        shell.setOrientation(LinearLayout.VERTICAL);
        shell.setPadding(dp(14), dp(12), dp(14), dp(12));
        shell.setBackground(guideReaderOverlayPanel());

        PanelOverlay[] holder = new PanelOverlay[1];
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = text(getString(R.string.guide_bookmarks), 15, TEXT, true);
        header.addView(title, new LinearLayout.LayoutParams(0, -1, 1));
        ImageButton add = compactMapIconButton(R.drawable.ic_add,
                getString(R.string.guide_bookmark_add),
                () -> addGuideBookmark(holder[0], guide));
        header.addView(add, new LinearLayout.LayoutParams(dp(42), dp(40)));
        shell.addView(header, new LinearLayout.LayoutParams(-1, dp(48)));

        ScrollView scroller = new ScrollView(this);
        scroller.setFillViewport(true);
        scroller.setVerticalScrollBarEnabled(true);
        scroller.setScrollbarFadingEnabled(false);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(0, dp(4), 0, dp(4));
        scroller.addView(list, new ScrollView.LayoutParams(-1, -2));
        shell.addView(scroller, new LinearLayout.LayoutParams(-1, 0, 1));

        if (guide.bookmarks.isEmpty()) {
            TextView empty = text(getString(R.string.guide_bookmark_empty), 13, MUTED, false);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(dp(18), dp(18), dp(18), dp(18));
            list.addView(empty, new LinearLayout.LayoutParams(-1, dp(110)));
        } else {
            List<GuideEntry.Bookmark> ordered = new ArrayList<>(guide.bookmarks);
            String fingerprint = activeGuideReaderFingerprint;
            Collections.sort(ordered, (left, right) -> {
                boolean leftCurrent = left.belongsTo(fingerprint);
                boolean rightCurrent = right.belongsTo(fingerprint);
                if (leftCurrent != rightCurrent) {
                    return leftCurrent ? -1 : 1;
                }
                return Integer.compare(left.anchor, right.anchor);
            });
            for (GuideEntry.Bookmark bookmark : ordered) {
                list.addView(guideBookmarkRow(holder, guide, bookmark),
                        new LinearLayout.LayoutParams(-1, dp(56)));
            }
        }

        Button close = editorButton(getString(R.string.common_close), () -> {
            if (holder[0] != null) {
                dismissPanelAnimated(holder[0]);
            }
        });
        LinearLayout.LayoutParams closeParams = new LinearLayout.LayoutParams(-1, dp(44));
        closeParams.setMargins(0, dp(8), 0, 0);
        shell.addView(close, closeParams);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                settingsOverlayWidth(600), settingsOverlayHeight(620), Gravity.CENTER);
        params.setMargins(dp(12), dp(12), dp(12), dp(12));
        holder[0] = showPanelOverlay(shell, params, null);
    }

    private Drawable guideReaderOverlayPanel() {
        if (HeimdallUi.isPearl(this)) {
            return HeimdallUi.cncFlush(this, 12);
        }
        return HeimdallUi.glass(this, 0xFF111824, 0xFF080C12,
                HeimdallUi.COLOR_SYSTEM_BORDER_TOP,
                HeimdallUi.COLOR_SYSTEM_BORDER_BOTTOM, 12, 1);
    }

    private View guideBookmarkRow(PanelOverlay[] holder, GuideEntry guide,
            GuideEntry.Bookmark bookmark) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(2), 0, dp(2));

        boolean current = bookmark.belongsTo(activeGuideReaderFingerprint);
        String fallback = getString(R.string.guide_bookmark_default,
                Math.max(1, guide.bookmarks.indexOf(bookmark) + 1));
        String label = nonEmpty(bookmark.label, fallback);
        Button open = new Button(this);
        open.setAllCaps(false);
        open.setSingleLine(true);
        open.setText(current ? label : getString(R.string.guide_bookmark_stale, label));
        open.setTextSize(13);
        open.setTextColor(current ? HeimdallUi.textColor(this) : HeimdallUi.mutedTextColor(this));
        open.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        open.setBackground(HeimdallUi.isPearl(this)
                ? HeimdallUi.pearlMenuControl(this, 8, false, !current)
                : HeimdallUi.surfacePanel(this, 8));
        open.setMinHeight(0);
        open.setMinWidth(0);
        open.setPadding(dp(10), 0, dp(8), 0);
        open.setContentDescription(getString(R.string.guide_bookmark_open, label));
        open.setOnClickListener(view -> {
            if (!bookmark.belongsTo(activeGuideReaderFingerprint)) {
                showErrorAction(getString(R.string.guide_bookmark_stale_message));
                return;
            }
            jumpToGuideBookmark(holder[0], guide, bookmark, label);
        });
        row.addView(open, new LinearLayout.LayoutParams(0, dp(52), 1));

        ImageButton delete = iconButton(R.drawable.ic_trash,
                getString(R.string.guide_bookmark_delete), true,
                () -> deleteGuideBookmark(holder[0], guide, bookmark));
        LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(dp(58), dp(48));
        deleteParams.setMargins(dp(4), dp(2), 0, dp(2));
        row.addView(delete, deleteParams);
        return row;
    }

    private void addGuideBookmark(PanelOverlay overlay, GuideEntry guide) {
        if (activeGuideReaderView == null || activeGuideReaderEntry != guide
                || activeGuideReaderFingerprint.length() == 0
                || cachedGuideTextEntry != guide || cachedGuideTextDocument == null) {
            showErrorAction(getString(R.string.guide_bookmark_loading));
            return;
        }
        if (guide.bookmarks.size() >= GuideEntry.MAX_BOOKMARKS) {
            showErrorAction(getString(R.string.guide_bookmark_limit));
            return;
        }
        GuideTextReaderView.Position position = activeGuideReaderView.capturePosition();
        if (guide.hasBookmarkAt(activeGuideReaderFingerprint, position.anchor)) {
            showAction(getString(R.string.guide_bookmark_duplicate));
            return;
        }
        String heading = cachedGuideTextDocument.nearbyMarkdownHeading(position.anchor);
        String label = nonEmpty(heading, getString(R.string.guide_bookmark_default,
                guide.bookmarks.size() + 1));
        GuideEntry.Bookmark bookmark = guide.addBookmark(label, position.anchor,
                position.anchorTop, position.horizontalColumn, position.viewportWidth,
                activeGuideReaderFingerprint);
        if (bookmark == null) {
            showErrorAction(getString(R.string.guide_bookmark_limit));
            return;
        }
        ProfileStore.saveProfiles(this, profiles);
        Runnable refresh = () -> {
            showAction(getString(R.string.guide_bookmark_added));
            showGuideBookmarks(guide);
        };
        if (overlay != null) {
            dismissPanelAnimated(overlay, refresh);
        } else {
            refresh.run();
        }
    }

    private void jumpToGuideBookmark(PanelOverlay overlay, GuideEntry guide,
            GuideEntry.Bookmark bookmark, String label) {
        GuideTextReaderView reader = activeGuideReaderView;
        if (reader == null || activeGuideReaderEntry != guide
                || !bookmark.belongsTo(activeGuideReaderFingerprint)) {
            showErrorAction(getString(R.string.guide_bookmark_stale_message));
            return;
        }
        GuideTextReaderView.Position position = new GuideTextReaderView.Position(
                bookmark.anchor, bookmark.anchorTop, bookmark.horizontalColumn,
                bookmark.viewportWidth);
        Runnable jump = () -> {
            reader.scrollToPosition(position);
            if (guide.updateReadingPosition(0, bookmark.anchor, bookmark.anchorTop,
                    bookmark.horizontalColumn, bookmark.viewportWidth,
                    bookmark.fingerprint)) {
                ProfileStore.saveProfiles(this, profiles);
            }
            showAction(getString(R.string.guide_bookmark_open, label));
        };
        if (overlay != null) {
            dismissPanelAnimated(overlay, jump);
        } else {
            jump.run();
        }
    }

    private void deleteGuideBookmark(PanelOverlay overlay, GuideEntry guide,
            GuideEntry.Bookmark bookmark) {
        if (!guide.removeBookmark(bookmark)) {
            return;
        }
        ProfileStore.saveProfiles(this, profiles);
        Runnable refresh = () -> {
            showAction(getString(R.string.guide_bookmark_deleted));
            showGuideBookmarks(guide);
        };
        if (overlay != null) {
            dismissPanelAnimated(overlay, refresh);
        } else {
            refresh.run();
        }
    }

    private void openGuideFullscreen() {
        if (!isInlineTextGuide(viewingGuideInline)) {
            return;
        }
        flushGuideReadingPosition();
        guideReaderFullscreen = true;
        rebuildContent();
    }

    private View createFullscreenGuideLayout() {
        profileIconView = null;
        profileTitle = null;
        systemTimeText = null;
        systemNetworkIcon = null;
        systemBatteryIcon = null;
        systemBatteryText = null;
        statusText = null;
        profileList = null;
        touchPadView = null;
        macroGrids.clear();

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(HeimdallUi.isPearl(this) ? 0xFFD4DCE3 : BG);
        applySystemGestureExclusion(root);
        GuideEntry guide = viewingGuideInline;
        if (!isInlineTextGuide(guide)) {
            guideReaderFullscreen = false;
            return root;
        }
        GuideTextReaderView reader = createGuideTextReaderView(guide);
        root.addView(reader, new FrameLayout.LayoutParams(-1, -1));
        LinearLayout toolbar = createGuideReaderToolbar(guide, true);
        installFullscreenGuideControls(root, toolbar, reader);
        loadGuideText(reader, guide);
        return root;
    }

    private void installFullscreenGuideControls(FrameLayout root, LinearLayout toolbar,
            View content) {
        FrameLayout.LayoutParams toolbarParams = new FrameLayout.LayoutParams(-1, dp(50));
        toolbarParams.gravity = Gravity.TOP;
        toolbarParams.setMargins(dp(8), dp(8), dp(8), 0);
        root.addView(toolbar, toolbarParams);
        fullscreenGuideControls = toolbar;

        ImageButton reveal = compactMapIconButton(R.drawable.ic_toolbar_reveal,
                getString(R.string.guide_show_toolbar), this::showFullscreenGuideControls);
        reveal.setBackground(HeimdallUi.isPearl(this)
                ? HeimdallUi.pearlMenuControl(this, 8, false, false)
                : HeimdallUi.glass(this, 0xA6111824, 0xC9080C12,
                        HeimdallUi.COLOR_SYSTEM_BORDER_TOP,
                        HeimdallUi.COLOR_SYSTEM_BORDER_BOTTOM, 8, 1));
        FrameLayout.LayoutParams revealParams = new FrameLayout.LayoutParams(dp(46), dp(24));
        revealParams.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        revealParams.setMargins(0, dp(3), 0, 0);
        root.addView(reveal, revealParams);
        fullscreenGuideReveal = reveal;

        content.setOnTouchListener((view, event) -> {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN
                    && (event.getY() <= dp(26)
                    || event.getY() >= view.getHeight() - dp(26))) {
                showFullscreenGuideControls();
            }
            return false;
        });
        toolbar.setOnTouchListener((view, event) -> {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                showFullscreenGuideControls();
            }
            return false;
        });
        showFullscreenGuideControls();
    }

    private void showFullscreenGuideControls() {
        setFullscreenGuideControlsVisible(true);
        uiHandler.removeCallbacks(hideFullscreenGuideControls);
        uiHandler.postDelayed(hideFullscreenGuideControls, 3000);
    }

    private void setFullscreenGuideControlsVisible(boolean visible) {
        if (fullscreenGuideControls != null) {
            fullscreenGuideControls.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
        if (fullscreenGuideReveal != null) {
            fullscreenGuideReveal.setVisibility(visible ? View.GONE : View.VISIBLE);
        }
    }

    private void closeGuideFullscreen() {
        uiHandler.removeCallbacks(hideFullscreenGuideControls);
        flushGuideReadingPosition();
        fullscreenGuideControls = null;
        fullscreenGuideReveal = null;
        guideReaderFullscreen = false;
        rebuildContent();
    }

    private void leaveGuideFullscreenState() {
        uiHandler.removeCallbacks(hideFullscreenGuideControls);
        fullscreenGuideControls = null;
        fullscreenGuideReveal = null;
        guideReaderFullscreen = false;
    }

    private void addDocumentPreview(LinearLayout parent, String title, String uriString,
            String fileName, boolean mapPreview) {
        LinearLayout card = mapPreview
                ? inlineEditorCard(parent, getString(R.string.guide_preview_title, title))
                : parent;
        String raw = uriString == null ? "" : uriString.trim();
        if (raw.length() == 0) {
            addTextPreview(card, getString(R.string.guide_no_file), 64);
            return;
        }
        Uri uri = Uri.parse(raw.contains("://") ? raw : "file://" + raw);
        if (isPdfDocument(raw, fileName)) {
            Bitmap page = renderPdfFirstPage(uri, Math.max(dp(480), getResources().getDisplayMetrics().widthPixels - dp(48)));
            if (page != null) {
                addBitmapPreview(card, page, 260);
                addPreviewCaption(card, getString(R.string.guide_pdf_first_page));
            } else {
                addTextPreview(card, getString(R.string.guide_pdf_preview_failed), 72);
            }
            return;
        }
        if (isImageDocument(raw, fileName)) {
            Bitmap bitmap = decodeImagePreview(uri, Math.max(dp(720), getResources().getDisplayMetrics().widthPixels));
            if (bitmap != null) {
                addBitmapPreview(card, bitmap, 260);
            } else {
                addTextPreview(card, getString(R.string.guide_image_preview_failed), 72);
            }
            return;
        }
        if (isTextDocument(raw, fileName)) {
            String text = readTextDocument(uri, 60000);
            addReaderText(card, text.length() == 0
                    ? getString(R.string.guide_text_read_failed) : text);
            return;
        }
        addTextPreview(card, getString(R.string.guide_embedded_unsupported, raw), 96);
    }

    private void addBitmapPreview(LinearLayout parent, Bitmap bitmap, int heightDp) {
        ImageView image = new ImageView(this);
        image.setImageBitmap(bitmap);
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        image.setAdjustViewBounds(true);
        image.setBackground(rounded(HeimdallUi.isPearl(this) ? 0xFFD7DEE5 : 0xFF05070A,
                HeimdallUi.border(this), 8));
        image.setPadding(dp(4), dp(4), dp(4), dp(4));
        parent.addView(image, blockParams(heightDp, 4, 8));
    }

    private void addTextPreview(LinearLayout parent, String value, int heightDp) {
        TextView body = text(nonEmpty(value, getString(R.string.common_no_content)),
                12, TEXT, false);
        body.setGravity(Gravity.TOP | Gravity.START);
        body.setPadding(dp(10), dp(8), dp(10), dp(8));
        body.setBackground(HeimdallUi.insetPanel(this, 8));
        body.setTextIsSelectable(true);
        parent.addView(body, blockParams(heightDp, 4, 8));
    }

    private void addPreviewCaption(LinearLayout parent, String value) {
        TextView caption = text(nonEmpty(value, getString(R.string.common_no_content)),
                12, MUTED, false);
        caption.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        caption.setPadding(dp(10), dp(2), dp(10), dp(2));
        parent.addView(caption, blockParams(34, 0, 6));
    }

    private TextView addReaderText(LinearLayout parent, String value) {
        TextView body = text(nonEmpty(value, getString(R.string.common_no_content)),
                13, TEXT, false);
        body.setGravity(Gravity.TOP | Gravity.START);
        body.setPadding(dp(10), dp(10), dp(10), dp(14));
        body.setLineSpacing(dp(2), 1f);
        body.setTextIsSelectable(true);
        parent.addView(body, new LinearLayout.LayoutParams(-1, -2));
        return body;
    }

    private void flushGuideReadingPosition() {
        if (activeGuideReaderView == null || activeGuideReaderEntry == null) {
            return;
        }
        saveGuideReadingPosition();
        activeGuideReaderView = null;
        activeGuideReaderEntry = null;
        activeGuideReaderFingerprint = "";
    }

    private void saveGuideReadingPosition() {
        GuideTextReaderView reader = activeGuideReaderView;
        GuideEntry guide = activeGuideReaderEntry;
        if (reader == null || guide == null || activeGuideReaderFingerprint.length() == 0) {
            return;
        }
        GuideTextReaderView.Position position = reader.capturePosition();
        boolean changed = guide.updateReadingPosition(0, position.anchor,
                position.anchorTop, position.horizontalColumn, position.viewportWidth,
                activeGuideReaderFingerprint);
        if (changed) {
            ProfileStore.saveProfiles(this, profiles);
        }
    }

    private Bitmap decodeImagePreview(Uri uri, int maxSide) {
        try {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            InputStream boundsStream = getContentResolver().openInputStream(uri);
            if (boundsStream != null) {
                BitmapFactory.decodeStream(boundsStream, null, bounds);
                boundsStream.close();
            }
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 1;
            int largest = Math.max(bounds.outWidth, bounds.outHeight);
            while (largest / options.inSampleSize > maxSide) {
                options.inSampleSize *= 2;
            }
            InputStream imageStream = getContentResolver().openInputStream(uri);
            if (imageStream == null) {
                return null;
            }
            Bitmap bitmap = BitmapFactory.decodeStream(imageStream, null, options);
            imageStream.close();
            return bitmap;
        } catch (Exception ignored) {
            return null;
        }
    }

    private Bitmap renderPdfFirstPage(Uri uri, int maxWidth) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return null;
        }
        try {
            ParcelFileDescriptor descriptor = getContentResolver().openFileDescriptor(uri, "r");
            if (descriptor == null) {
                return null;
            }
            PdfRenderer renderer = new PdfRenderer(descriptor);
            if (renderer.getPageCount() == 0) {
                renderer.close();
                descriptor.close();
                return null;
            }
            PdfRenderer.Page page = renderer.openPage(0);
            float scale = Math.min(2.0f, Math.max(0.4f, maxWidth / (float) Math.max(1, page.getWidth())));
            int width = Math.max(1, Math.round(page.getWidth() * scale));
            int height = Math.max(1, Math.round(page.getHeight() * scale));
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            bitmap.eraseColor(0xFFFFFFFF);
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
            page.close();
            renderer.close();
            descriptor.close();
            return bitmap;
        } catch (Exception ignored) {
            return null;
        }
    }

    private String readTextDocument(Uri uri, int maxBytes) {
        try {
            InputStream input = getContentResolver().openInputStream(uri);
            if (input == null) {
                return "";
            }
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int total = 0;
            int readLimit = Math.max(1, maxBytes) + 1;
            int read;
            while ((read = input.read(buffer)) != -1 && total < readLimit) {
                int count = Math.min(read, readLimit - total);
                output.write(buffer, 0, count);
                total += count;
            }
            input.close();
            boolean truncated = total > maxBytes;
            byte[] bytes = output.toByteArray();
            if (truncated) {
                bytes = Arrays.copyOf(bytes, maxBytes);
            }
            String text = GuideTextDecoder.decode(bytes, truncated);
            if (truncated) {
                text += getString(R.string.guide_preview_truncated);
            }
            return text;
        } catch (Exception ignored) {
            return "";
        }
    }

    private boolean canPreviewInline(String uriString, String fileName) {
        return isImageDocument(uriString, fileName)
                || isTextDocument(uriString, fileName);
    }

    private boolean isPdfDocument(String uriString, String fileName) {
        String mime = mimeType(uriString);
        return "application/pdf".equals(mime) || looksLikePdf(uriString) || looksLikePdf(fileName);
    }

    private boolean isImageDocument(String uriString, String fileName) {
        String mime = mimeType(uriString);
        String lower = ((uriString == null ? "" : uriString) + " " + (fileName == null ? "" : fileName)).toLowerCase(Locale.US);
        return (mime != null && mime.startsWith("image/"))
                || lower.contains(".png") || lower.contains(".jpg") || lower.contains(".jpeg")
                || lower.contains(".webp") || lower.contains(".gif");
    }

    private boolean isTextDocument(String uriString, String fileName) {
        String mime = mimeType(uriString);
        String lower = ((uriString == null ? "" : uriString) + " " + (fileName == null ? "" : fileName)).toLowerCase(Locale.US);
        return (mime != null && mime.startsWith("text/"))
                || lower.contains(".txt") || lower.contains(".md") || lower.contains(".markdown");
    }

    private String mimeType(String uriString) {
        if (uriString == null || uriString.trim().length() == 0) {
            return null;
        }
        try {
            return getContentResolver().getType(Uri.parse(uriString.trim()));
        } catch (Exception ignored) {
            return null;
        }
    }

    private void openGuideExternally(GuideEntry guide) {
        if (GuideEntry.TYPE_NOTE.equals(guide.type)) {
            showAction(getString(R.string.guide_read_in_heimdall));
            return;
        }
        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri uri = guideUri(guide);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        boolean pdf = false;
        if (GuideEntry.TYPE_FILE.equals(guide.type)) {
            pdf = isPdfDocument(guide.content, guide.title);
            if (pdf) {
                intent.setDataAndType(uri, "application/pdf");
            } else {
                intent.setDataAndType(uri, "*/*");
            }
        } else {
            intent.setData(uri);
        }
        startExternalActivity(intent, getString(R.string.guide_open_chooser),
                R.string.guide_open_failed, pdf);
    }

    private void startExternalActivity(Intent intent, String chooserTitle,
            int errorMessageRes, boolean preferDefaultHandler) {
        if (preferDefaultHandler) {
            try {
                startActivity(intent);
                return;
            } catch (Exception ignored) {
            }
        }
        try {
            startActivity(Intent.createChooser(intent, chooserTitle));
        } catch (Exception ex) {
            showErrorAction(getString(errorMessageRes));
        }
    }

    private Uri guideUri(GuideEntry guide) {
        String raw = guide.content == null ? "" : guide.content.trim();
        if (GuideEntry.TYPE_LINK.equals(guide.type) && !raw.contains("://")) {
            raw = "https://" + raw;
        }
        if (GuideEntry.TYPE_FILE.equals(guide.type) && !raw.contains("://")) {
            raw = "file://" + raw;
        }
        return Uri.parse(raw);
    }

    private boolean looksLikePdf(String value) {
        return value != null && value.toLowerCase(Locale.ROOT).endsWith(".pdf");
    }

    private void showRecentAppPicker(EditText packageInput) {
        List<String> packages = InputBridge.getRecentPackageNames();
        if (packages.isEmpty()) {
                showErrorAction(getString(R.string.profile_recent_apps_empty));
            return;
        }
        LinearLayout shell = new LinearLayout(this);
        shell.setOrientation(LinearLayout.VERTICAL);
        shell.setBackground(HeimdallUi.isPearl(this)
                ? HeimdallUi.cncFlush(this, 14)
                : HeimdallUi.glass(this, 0xFA0B111B, 0xFF070A10,
                        0x886A829C, 0x44344150, 14, 2));
        shell.setPadding(dp(14), dp(12), dp(14), dp(10));

        TextView title = text(getString(R.string.profile_recent_app_picker_title),
                HeimdallUi.TYPE_EDITOR_TITLE, TEXT, true);
        shell.addView(title, new LinearLayout.LayoutParams(-1, dp(34)));
        TextView subtitle = text(getString(R.string.profile_recent_app_picker_subtitle),
                11, MUTED, false);
        shell.addView(subtitle, new LinearLayout.LayoutParams(-1, dp(28)));

        ScrollView scroll = new ScrollView(this);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(list, new ScrollView.LayoutParams(-1, -2));
        shell.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        final PanelOverlay[] overlayHolder = new PanelOverlay[1];
        for (String packageName : packages) {
            String appName = appLabelForPackage(packageName);
            list.addView(settingsMenuButton(appName, packageName, () -> {
                packageInput.setText(packageName);
                packageInput.setSelection(packageInput.getText().length());
                        showAction(getString(R.string.profile_app_selected, appName));
                if (overlayHolder[0] != null) {
                    dismissPanelAnimated(overlayHolder[0]);
                }
            }));
        }

        View divider = new View(this);
        divider.setBackgroundColor(HeimdallUi.isPearl(this) ? 0x287B8792 : 0x445F7C9A);
        shell.addView(divider, new LinearLayout.LayoutParams(-1, dp(1)));
        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        shell.addView(actions, new LinearLayout.LayoutParams(-1, dp(54)));
        actions.addView(editorButton(getString(R.string.common_cancel), () -> {
            if (overlayHolder[0] != null) {
                dismissPanelAnimated(overlayHolder[0]);
            }
        }));

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                settingsOverlayWidth(660), settingsOverlayHeight(620), Gravity.CENTER);
        params.setMargins(dp(12), dp(12), dp(12), dp(12));
        overlayHolder[0] = showPanelOverlay(shell, params, null);
    }

    private String appLabelForPackage(String packageName) {
        try {
            ApplicationInfo info = getPackageManager().getApplicationInfo(packageName, 0);
            CharSequence label = getPackageManager().getApplicationLabel(info);
            return nonEmpty(label == null ? null : label.toString(), packageName);
        } catch (PackageManager.NameNotFoundException ignored) {
            return packageName;
        }
    }

    private void showProfileExportDialog() {
        showSettingsDecisionPanel(getString(R.string.profile_export),
                getString(R.string.profile_export_summary),
                getString(R.string.profile_export_current), () -> exportProfiles(false),
                getString(R.string.profile_export_all), () -> exportProfiles(true));
    }

    private void showProfileImportDialog(AlertDialog[] parentDialogHolder) {
        if (releaseTextInputFocusThen(() -> showProfileImportDialog(parentDialogHolder))) {
            return;
        }
        if (parentDialogHolder != null && parentDialogHolder[0] != null) {
            parentDialogHolder[0].dismiss();
        }
        cancelPendingProfileBundleWork();
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{
                ProfileBundleStore.MIME_TYPE, "application/json", "application/octet-stream"});
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        try {
            startActivityForResult(intent, REQUEST_IMPORT_PROFILES);
        } catch (Exception ex) {
            showErrorAction(getString(R.string.error_profile_import_picker));
        }
    }

    private void exportProfiles(boolean allProfiles) {
        if (releaseTextInputFocusThen(() -> exportProfiles(allProfiles))) {
            return;
        }
        cancelPendingProfileBundleWork();
        List<GameProfile> exportList = new ArrayList<>();
        if (allProfiles) {
            exportList.addAll(profiles);
        } else {
            exportList.add(selectedProfile);
        }
        String timestamp = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(new Date());
        try {
            pendingProfileExportProfiles = ProfileStore.profilesFromJson(
                    ProfileStore.profilesToJson(exportList));
        } catch (JSONException ex) {
            pendingProfileExportProfiles = null;
            showErrorAction(getString(R.string.error_profile_export_empty));
            return;
        }
        String filename = allProfiles
                ? "heimdall-profiles-" + timestamp + ProfileBundleStore.FILE_EXTENSION
                : "heimdall-profile-" + safeFilename(selectedProfile.name) + "-" + timestamp
                        + ProfileBundleStore.FILE_EXTENSION;
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(ProfileBundleStore.MIME_TYPE);
        intent.putExtra(Intent.EXTRA_TITLE, filename);
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        try {
            startActivityForResult(intent, REQUEST_EXPORT_PROFILES);
        } catch (Exception ex) {
            pendingProfileExportProfiles = null;
            showErrorAction(getString(R.string.error_profile_export_picker));
        }
    }

    private void writeProfileExport(Uri uri) {
        if (pendingProfileExportProfiles == null || pendingProfileExportProfiles.isEmpty()) {
            showErrorAction(getString(R.string.error_profile_export_empty));
            return;
        }
        List<GameProfile> exportProfiles = pendingProfileExportProfiles;
        pendingProfileExportProfiles = null;
        cancelPendingProfileBundleRequest();
        showAction(getString(R.string.profile_export_preparing));
        final ProfileBundleStore.Request[] holder = new ProfileBundleStore.Request[1];
        holder[0] = ProfileBundleStore.exportAsync(this, exportProfiles,
                new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(new Date()), uri,
                new ProfileBundleStore.ExportCallback() {
                    @Override
                    public void onExported(int assetCount, long assetBytes) {
                        if (pendingProfileBundleRequest != holder[0]) {
                            return;
                        }
                        pendingProfileBundleRequest = null;
                        showAction(getString(R.string.profile_export_complete,
                                assetCount, android.text.format.Formatter.formatShortFileSize(
                                        AssistantActivity.this, assetBytes)));
                    }

                    @Override
                    public void onError(ProfileBundleStore.Failure failure) {
                        if (pendingProfileBundleRequest != holder[0]) {
                            return;
                        }
                        pendingProfileBundleRequest = null;
                        showErrorAction(profileBundleFailureMessage(failure));
                    }
                });
        pendingProfileBundleRequest = holder[0];
    }

    private void readProfileImport(Uri uri) {
        try {
            getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (Exception ignored) {
        }
        cancelPendingProfileBundleWork();
        showAction(getString(R.string.profile_import_validating));
        final ProfileBundleStore.Request[] holder = new ProfileBundleStore.Request[1];
        holder[0] = ProfileBundleStore.prepareImportAsync(this, uri,
                new ProfileBundleStore.ImportCallback() {
                    @Override
                    public void onPrepared(ProfileBundleStore.PreparedImport preparedImport) {
                        if (pendingProfileBundleRequest != holder[0]) {
                            preparedImport.close();
                            return;
                        }
                        pendingProfileBundleRequest = null;
                        pendingProfileImport = preparedImport;
                        showProfileImportConfirm(preparedImport);
                    }

                    @Override
                    public void onError(ProfileBundleStore.Failure failure) {
                        if (pendingProfileBundleRequest != holder[0]) {
                            return;
                        }
                        pendingProfileBundleRequest = null;
                        showErrorAction(profileBundleFailureMessage(failure));
                    }
                });
        pendingProfileBundleRequest = holder[0];
    }

    private void showProfileImportConfirm(ProfileBundleStore.PreparedImport preparedImport) {
        List<GameProfile> imported = preparedImport.profiles;
        String firstName = imported.isEmpty() ? "" : imported.get(0).name;
        String importedCount = getResources().getQuantityString(R.plurals.profile_count,
                imported.size(), imported.size());
        String currentCount = getResources().getQuantityString(R.plurals.profile_count,
                profiles.size(), profiles.size());
        String message = getString(R.string.profile_import_read_summary, importedCount)
                + (firstName == null || firstName.length() == 0 ? ""
                : "\n" + getString(R.string.profile_import_first_name, firstName))
                + "\n" + (preparedImport.legacyJson
                ? getString(R.string.profile_import_legacy_summary)
                : getString(R.string.profile_import_asset_summary,
                        preparedImport.assetCount,
                        android.text.format.Formatter.formatShortFileSize(
                                this, preparedImport.assetBytes)))
                + "\n\n" + getString(R.string.profile_import_choice_summary, currentCount);
        showSettingsDecisionPanel(getString(R.string.profile_import), message,
                getString(R.string.profile_import_replace_all),
                () -> installPreparedProfiles(preparedImport, true),
                getString(R.string.profile_import_append),
                () -> installPreparedProfiles(preparedImport, false));
    }

    private void installPreparedProfiles(ProfileBundleStore.PreparedImport preparedImport,
            boolean replaceAll) {
        if (pendingProfileImport != preparedImport || preparedImport == null) {
            showErrorAction(getString(R.string.error_profile_import_read));
            return;
        }
        pendingProfileImport = null;
        cancelPendingProfileBundleRequest();
        showAction(getString(R.string.profile_import_installing));
        final ProfileBundleStore.Request[] holder = new ProfileBundleStore.Request[1];
        holder[0] = ProfileBundleStore.installAsync(this, preparedImport,
                new ProfileBundleStore.InstallCallback() {
                    @Override
                    public void onInstalled(List<GameProfile> imported) {
                        if (pendingProfileBundleRequest != holder[0]) {
                            return;
                        }
                        pendingProfileBundleRequest = null;
                        if (replaceAll) {
                            replaceProfiles(imported);
                        } else {
                            appendProfiles(imported);
                        }
                    }

                    @Override
                    public void onError(ProfileBundleStore.Failure failure) {
                        if (pendingProfileBundleRequest != holder[0]) {
                            return;
                        }
                        pendingProfileBundleRequest = null;
                        showErrorAction(profileBundleFailureMessage(failure));
                    }
                });
        pendingProfileBundleRequest = holder[0];
    }

    private String profileBundleFailureMessage(ProfileBundleStore.Failure failure) {
        if (failure == null) {
            return getString(R.string.error_profile_import_read);
        }
        String detail = nonEmpty(failure.detail, getString(R.string.profile_asset_unknown));
        if (failure.code == ProfileBundleStore.ErrorCode.MISSING_ASSET) {
            return getString(R.string.error_profile_bundle_missing_asset, detail);
        }
        if (failure.code == ProfileBundleStore.ErrorCode.UNSUPPORTED_ASSET) {
            return getString(R.string.error_profile_bundle_unsupported_asset, detail);
        }
        if (failure.code == ProfileBundleStore.ErrorCode.TOO_LARGE) {
            return getString(R.string.error_profile_bundle_too_large, detail);
        }
        if (failure.code == ProfileBundleStore.ErrorCode.UNSUPPORTED_VERSION) {
            return getString(R.string.error_profile_bundle_version, detail);
        }
        if (failure.code == ProfileBundleStore.ErrorCode.UNSAFE_PATH) {
            return getString(R.string.error_profile_bundle_unsafe_path, detail);
        }
        if (failure.code == ProfileBundleStore.ErrorCode.CORRUPT
                || failure.code == ProfileBundleStore.ErrorCode.INVALID_FORMAT) {
            return getString(R.string.error_profile_bundle_corrupt, detail);
        }
        if (failure.code == ProfileBundleStore.ErrorCode.EMPTY) {
            return getString(R.string.error_profile_import_format);
        }
        return getString(R.string.error_profile_bundle_storage, detail);
    }

    private void cancelPendingProfileBundleRequest() {
        if (pendingProfileBundleRequest != null) {
            pendingProfileBundleRequest.cancel();
            pendingProfileBundleRequest = null;
        }
    }

    private void cancelPendingProfileBundleWork() {
        cancelPendingProfileBundleRequest();
        pendingProfileExportProfiles = null;
        if (pendingProfileImport != null) {
            pendingProfileImport.close();
            pendingProfileImport = null;
        }
    }

    private void appendProfiles(List<GameProfile> imported) {
        for (GameProfile profile : imported) {
            profile.name = uniqueProfileName(profile.name);
            profiles.add(profile);
        }
        selectedProfileIndex = Math.max(0, profiles.size() - imported.size());
        finishProfileImport(getString(R.string.profile_import_appended,
                getResources().getQuantityString(R.plurals.profile_count,
                        imported.size(), imported.size())));
    }

    private void replaceProfiles(List<GameProfile> imported) {
        if (!protectProfilesBefore(ProfileSnapshotStore.REASON_REPLACE_ALL)) {
            return;
        }
        profiles.clear();
        profiles.addAll(imported);
        selectedProfileIndex = 0;
        finishProfileImport(getString(R.string.profile_import_replaced,
                getResources().getQuantityString(R.plurals.profile_count,
                        imported.size(), imported.size())));
    }

    private boolean protectProfilesBefore(String reason) {
        if (ProfileSnapshotStore.createSnapshot(this, profiles, selectedProfileIndex, reason)) {
            return true;
        }
        showErrorAction(getString(R.string.error_profile_snapshot_save));
        return false;
    }

    private void finishProfileImport(String message) {
        selectedProfile = profiles.get(selectedProfileIndex);
        draftWidgetLayout = null;
        settingsTouchpadDraft = null;
        settingsMacroMappingProtectionInput = null;
        settingsMacroMappingProtectionDraft = null;
        touchpadSettings = selectedProfile.safeTouchpadSettings();
        ProfileStore.saveSelectedIndex(this, selectedProfileIndex);
        ProfileStore.saveProfiles(this, profiles);
        renderProfiles();
        renderSelectedProfile();
        rebuildContent();
        showAction(message);
    }

    private String uniqueProfileName(String rawName) {
        String base = nonEmpty(rawName, getString(R.string.profile_imported_fallback)).trim();
        String candidate = base;
        int index = 2;
        while (profileNameExists(candidate)) {
            candidate = getString(R.string.profile_import_name_suffix, base, index);
            index++;
        }
        return candidate;
    }

    private boolean profileNameExists(String name) {
        for (GameProfile profile : profiles) {
            if (name.equals(profile.name)) {
                return true;
            }
        }
        return false;
    }

    private String safeFilename(String rawName) {
        String name = nonEmpty(rawName, "profile").trim();
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            boolean safe = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')
                    || (c >= '0' && c <= '9') || c == '-' || c == '_';
            builder.append(safe ? c : '-');
        }
        String result = builder.toString();
        return result.length() == 0 ? "profile" : result;
    }

    private boolean touchpadModeAvailable(String mode) {
        String normalized = TouchpadSettings.normalizeMode(mode);
        if (TouchpadSettings.MODE_TOUCH_DRAG.equals(normalized)) {
            return true;
        }
        if (TouchpadSettings.MODE_RIGHT_STICK.equals(normalized)) {
            return true;
        }
        if (TouchpadSettings.MODE_SHIZUKU_TOUCH.equals(normalized)) {
            return InputBridge.BACKEND_SHIZUKU.equals(InputBridge.selectedBackendId(this))
                    && ShizukuNativeController.isReady();
        }
        if (TouchpadSettings.MODE_RELATIVE_MOUSE.equals(normalized)) {
            return relativeMouseBackendAvailable();
        }
        if (TouchpadSettings.MODE_RELATIVE_MOVE.equals(normalized)) {
            return InputBridge.supportsRelativeMove(this);
        }
        if (TouchpadSettings.MODE_MOUSE_POINTER.equals(normalized)) {
            return InputBridge.supportsMouseMode(this);
        }
        return false;
    }

    private boolean relativeMouseBackendAvailable() {
        return InputBridge.BACKEND_SHIZUKU.equals(InputBridge.selectedBackendId(this))
                && ShizukuNativeController.isReady()
                && NativeGamepadPath.resolveDevice() != null;
    }

    private SliderInput sliderInput(LinearLayout parent, String label, float min, float max, float value, int scale, String suffix) {
        return sliderInput(parent, label, min, max, value, scale, suffix, 0xFF202124, 0xFF5F6368);
    }

    private SliderInput settingsSliderInput(LinearLayout parent, String label, float min, float max, float value, int scale, String suffix) {
        return sliderInput(parent, label, min, max, value, scale, suffix, TEXT, MUTED);
    }

    private SliderInput sliderInput(LinearLayout parent, String label, float min, float max, float value, int scale, String suffix, int titleColor, int valueColor) {
        SliderInput input = new SliderInput(min, max, value, scale, suffix);

        LinearLayout labelRow = new LinearLayout(this);
        labelRow.setOrientation(LinearLayout.HORIZONTAL);
        labelRow.setGravity(Gravity.CENTER_VERTICAL);
        parent.addView(labelRow, new LinearLayout.LayoutParams(-1, dp(28)));

        TextView title = text(label, 13, titleColor, true);
        labelRow.addView(title, new LinearLayout.LayoutParams(0, -1, 1));

        input.valueLabel = text("", 13, valueColor, true);
        input.valueLabel.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);
        labelRow.addView(input.valueLabel, new LinearLayout.LayoutParams(dp(88), -1));

        input.seekBar = new SeekBar(this);
        input.seekBar.setMax(input.maxUnits - input.minUnits);
        input.seekBar.setProgress(input.currentUnits - input.minUnits);
        styleSettingsSeekBar(input.seekBar);
        input.seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                input.currentUnits = input.minUnits + progress;
                input.updateLabel();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        input.updateLabel();
        parent.addView(input.seekBar, new LinearLayout.LayoutParams(-1, dp(42)));
        return input;
    }

    private void styleSettingsSeekBar(SeekBar seekBar) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return;
        }
        int active = HeimdallUi.isPearl(this) ? 0xFFF08A2A : HeimdallUi.accent(this);
        int track = HeimdallUi.isPearl(this) ? 0xFF909AA2 : 0xFF445A72;
        seekBar.setProgressTintList(ColorStateList.valueOf(active));
        seekBar.setProgressBackgroundTintList(ColorStateList.valueOf(track));
        seekBar.setThumbTintList(ColorStateList.valueOf(active));
        seekBar.setSplitTrack(false);
    }

    private void rebuildContent() {
        if (releaseTextInputFocusThen(this::rebuildContent)) {
            return;
        }
        flushGuideReadingPosition();
        cancelSettingsTransition();
        cancelContentTransitions();
        clearPanelOverlays();
        if (settingsContentScroll != null) {
            settingsContentScrollY = settingsContentScroll.getScrollY();
        }
        settingsContentScroll = null;
        settingsContentContainer = null;
        releaseMapWebView();
        releaseLocalMapBitmap();
        releaseLocalMapThumbnails();
        setContentView(createLayout());
        lastSystemTime = "";
        lastNetworkLabel = "";
        lastBatteryLabel = "";
        renderProfiles();
        renderSelectedProfile();
        updateSystemStatus();
        DebugPerformanceDiagnostics.attachRootObservers(this);
        updateGameFocusProtection();
        if (activeScreen == SCREEN_SETTINGS && settingsContentScroll != null) {
            int restoreY = Math.max(0, settingsContentScrollY);
            settingsContentScroll.post(() -> settingsContentScroll.scrollTo(0, restoreY));
        }
    }

    private void switchPlayScreen(int targetScreen) {
        if (releaseTextInputFocusThen(() -> switchPlayScreen(targetScreen))) {
            return;
        }
        flushGuideReadingPosition();
        if (guideReaderFullscreen && targetScreen != SCREEN_GUIDE) {
            leaveGuideFullscreenState();
        }
        if (settingsTransitionState != SETTINGS_TRANSITION_NONE) {
            return;
        }
        if (targetScreen == SCREEN_SETTINGS) {
            showSettingsPanel();
            return;
        }
        if (!isPlayScreen(targetScreen)) {
            return;
        }
        if (contentHost == null || currentContentPage == null
                || activeScreen == SCREEN_SETTINGS || mapViewerFullscreen) {
            activeScreen = targetScreen;
            rebuildContent();
            return;
        }
        if (activeScreen == targetScreen) {
            refreshCurrentPlayContent(targetScreen);
            return;
        }

        clearPanelOverlays();

        View outgoingPage = currentContentPage;
        int outgoingScreen = contentPageScreen(outgoingPage, activeScreen);
        removeAllContentPages();

        activeScreen = targetScreen;
        settingsContentScroll = null;
        settingsContentContainer = null;
        View incomingPage = createContentPage(targetScreen);
        applyFlatUiPolicy(incomingPage);
        currentContentPage = incomingPage;
        contentHost.addView(incomingPage, new FrameLayout.LayoutParams(-1, -1));
        updateGameFocusProtection();
        updateDockNavSelection(shouldAnimateUi());
        renderSelectedProfile();

        if (!shouldAnimateUi()) {
            incomingPage.setAlpha(1f);
            incomingPage.setTranslationX(0f);
            cleanupContentPage(outgoingScreen);
            return;
        }

        int generation = ++contentTransitionGeneration;
        int direction = targetScreen > outgoingScreen ? 1 : -1;
        incomingPage.setAlpha(1f);
        incomingPage.setTranslationX(direction * dp(CONTENT_ENTER_TRANSLATION_DP));
        trackAnimatedView(incomingPage);

        cleanupContentPage(outgoingScreen);
        incomingPage.animate()
                .translationX(0f)
                .setDuration(CONTENT_TRANSITION_MS)
                .setInterpolator(UI_EASE_OUT)
                .withEndAction(() -> {
                    if (generation == contentTransitionGeneration && incomingPage.getParent() != null) {
                        incomingPage.setAlpha(1f);
                        incomingPage.setTranslationX(0f);
                    }
                    finishAnimatedView(incomingPage);
                })
                .start();
    }

    private void refreshCurrentPlayContent(int screen) {
        if (releaseTextInputFocusThen(() -> refreshCurrentPlayContent(screen))) {
            return;
        }
        flushGuideReadingPosition();
        if (contentHost == null || currentContentPage == null || !isPlayScreen(screen)) {
            activeScreen = screen;
            rebuildContent();
            return;
        }
        clearPanelOverlays();
        removeStaleContentPages();
        View oldPage = currentContentPage;
        int oldScreen = contentPageScreen(oldPage, screen);
        oldPage.animate().cancel();
        finishAnimatedView(oldPage);
        removeContentPage(oldPage);
        cleanupContentPage(oldScreen);

        activeScreen = screen;
        View newPage = createContentPage(screen);
        applyFlatUiPolicy(newPage);
        currentContentPage = newPage;
        contentHost.addView(newPage, new FrameLayout.LayoutParams(-1, -1));
        updateGameFocusProtection();
        updateDockNavSelection(false);
        renderSelectedProfile();
    }

    private void cancelSettingsTransition() {
        settingsTransitionGeneration++;
        settingsTransitionState = SETTINGS_TRANSITION_NONE;
        closeSettingsAfterEnter = false;
        if (bottomDock != null) {
            bottomDock.animate().cancel();
            bottomDock.setTranslationY(0f);
            finishAnimatedView(bottomDock);
        }
        if (currentContentPage != null) {
            currentContentPage.animate().cancel();
            currentContentPage.setAlpha(1f);
            currentContentPage.setTranslationY(0f);
            currentContentPage.setVisibility(View.VISIBLE);
            finishAnimatedView(currentContentPage);
        }
        setDockNavButtonsEnabled(true);
    }

    private boolean isPlayScreen(int screen) {
        return screen == SCREEN_MAIN || screen == SCREEN_MAP || screen == SCREEN_GUIDE;
    }

    private void cancelContentTransitions() {
        contentTransitionGeneration++;
        if (contentHost == null) {
            return;
        }
        for (int i = contentHost.getChildCount() - 1; i >= 0; i--) {
            View child = contentHost.getChildAt(i);
            child.animate().cancel();
            finishAnimatedView(child);
        }
    }

    private void removeStaleContentPages() {
        if (contentHost == null) {
            return;
        }
        for (int i = contentHost.getChildCount() - 1; i >= 0; i--) {
            View child = contentHost.getChildAt(i);
            child.animate().cancel();
            finishAnimatedView(child);
            if (child != currentContentPage) {
                int screen = contentPageScreen(child, -1);
                contentHost.removeViewAt(i);
                cleanupContentPage(screen);
            }
        }
    }

    private void removeAllContentPages() {
        if (contentHost == null) {
            return;
        }
        for (int i = contentHost.getChildCount() - 1; i >= 0; i--) {
            View child = contentHost.getChildAt(i);
            child.animate().cancel();
            finishAnimatedView(child);
            contentHost.removeViewAt(i);
        }
        currentContentPage = null;
    }

    private void removeContentPage(View page) {
        if (contentHost != null && page != null && page.getParent() == contentHost) {
            contentHost.removeView(page);
        }
    }

    private int contentPageScreen(View page, int fallback) {
        Object tag = page == null ? null : page.getTag();
        if (tag instanceof Integer) {
            return (Integer) tag;
        }
        return fallback;
    }

    private void cleanupContentPage(int screen) {
        if (screen == SCREEN_MAP) {
            releaseMapWebView();
            releaseLocalMapBitmap();
            releaseLocalMapThumbnails();
        } else if (screen == SCREEN_MAIN) {
            releaseMagnifierViews();
        }
    }

    private void updateDockNavSelection(boolean animateIndicator) {
        for (DockNavButton button : dockNavButtons) {
            boolean selected = activeScreen == button.navScreen();
            HeimdallUi.applyNavButton(this, button, selected);
            button.setNavIcon(button.navIconRes(), selected);
        }
        if (dockNavBar != null) {
            dockNavBar.setSelectedIndex(dockIndexForScreen(activeScreen),
                    animateIndicator && shouldAnimateUi());
        }
    }

    private void refreshSettingsContent() {
        if (releaseTextInputFocusThen(this::refreshSettingsContent)) {
            return;
        }
        if (activeScreen != SCREEN_SETTINGS
                || settingsContentScroll == null
                || settingsContentContainer == null) {
            rebuildContent();
            return;
        }
        int restoreY = settingsContentScroll.getScrollY();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            settingsContentContainer.suppressLayout(true);
        }
        try {
            settingsContentContainer.removeAllViews();
            populateSettingsContent(settingsContentContainer);
        } finally {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                settingsContentContainer.suppressLayout(false);
            }
        }
        settingsContentContainer.requestLayout();
        DebugPerformanceDiagnostics.countRequestLayout("Settings content");
        renderSelectedProfile();
        updateSystemStatus();
        settingsContentScroll.post(() -> settingsContentScroll.scrollTo(0, restoreY));
    }

    private void releaseMapWebView() {
        if (activeMapWebView == null) {
            activeMapWebStatus = null;
            activeMapWebError = false;
            return;
        }
        try {
            activeMapWebView.stopLoading();
            activeMapWebView.setWebChromeClient(null);
            activeMapWebView.setWebViewClient(null);
            activeMapWebView.loadUrl("about:blank");
            activeMapWebView.removeAllViews();
            activeMapWebView.destroy();
        } catch (Exception ignored) {
        }
        activeMapWebView = null;
        activeMapWebStatus = null;
        activeMapWebError = false;
    }

    private void releaseLocalMapBitmap() {
        if (activeLocalMapBitmap != null && !activeLocalMapBitmap.isRecycled()) {
            activeLocalMapBitmap.recycle();
        }
        activeLocalMapBitmap = null;
    }

    private void releaseLocalMapThumbnails() {
        for (Bitmap thumbnail : activeLocalMapThumbnails) {
            if (thumbnail != null && !thumbnail.isRecycled()) {
                thumbnail.recycle();
            }
        }
        activeLocalMapThumbnails.clear();
    }

    private void setTextIfChanged(TextView view, String value) {
        if (view == null) {
            return;
        }
        String next = value == null ? "" : value;
        if (!next.contentEquals(view.getText())) {
            view.setText(next);
        }
    }

    private void applyFlatUiPolicy(View view) {
        if (!DebugPerformanceDiagnostics.isFlatUi() || view == null) {
            return;
        }
        view.setElevation(0f);
        if (view.getLayerType() == View.LAYER_TYPE_SOFTWARE) {
            view.setLayerType(View.LAYER_TYPE_NONE, null);
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                applyFlatUiPolicy(group.getChildAt(i));
            }
        }
    }

    private final class TrackedLinearLayout extends LinearLayout {
        private String component;

        TrackedLinearLayout(String value) {
            super(AssistantActivity.this);
            component = value;
        }

        @Override
        protected void dispatchDraw(Canvas canvas) {
            DebugPerformanceDiagnostics.countDraw(component);
            super.dispatchDraw(canvas);
        }

        @Override
        public void invalidate() {
            if (component != null) {
                DebugPerformanceDiagnostics.countInvalidate(component);
            }
            super.invalidate();
        }

        @Override
        public void postInvalidate() {
            if (component != null) {
                DebugPerformanceDiagnostics.countPostInvalidate(component);
            }
            super.postInvalidate();
        }

        @Override
        public void postInvalidateOnAnimation() {
            if (component != null) {
                DebugPerformanceDiagnostics.countPostInvalidateOnAnimation(component);
            }
            super.postInvalidateOnAnimation();
        }

        @Override
        public void requestLayout() {
            if (component != null) {
                DebugPerformanceDiagnostics.countRequestLayout(component);
            }
            super.requestLayout();
        }
    }

    private final class TrackedTextView extends TextView {
        private String component;

        TrackedTextView(String value) {
            super(AssistantActivity.this);
            component = value;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            DebugPerformanceDiagnostics.countDraw(component);
            super.onDraw(canvas);
        }

        @Override
        public void invalidate() {
            if (component != null) {
                DebugPerformanceDiagnostics.countInvalidate(component);
            }
            super.invalidate();
        }

        @Override
        public void postInvalidate() {
            if (component != null) {
                DebugPerformanceDiagnostics.countPostInvalidate(component);
            }
            super.postInvalidate();
        }

        @Override
        public void postInvalidateOnAnimation() {
            if (component != null) {
                DebugPerformanceDiagnostics.countPostInvalidateOnAnimation(component);
            }
            super.postInvalidateOnAnimation();
        }

        @Override
        public void requestLayout() {
            if (component != null) {
                DebugPerformanceDiagnostics.countRequestLayout(component);
            }
            super.requestLayout();
        }
    }

    private NumberStepper compactNumberStepper(int min, int max, int selected) {
        return new NumberStepper(min, max, selected);
    }

    private LinearLayout labeledNumberStepper(String label, NumberStepper stepper) {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.HORIZONTAL);
        container.setGravity(Gravity.CENTER_VERTICAL);
        container.setPadding(0, 0, dp(4), 0);
        TextView title = text(label, 10, MUTED, true);
        title.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
        title.setSingleLine(true);
        container.addView(title, new LinearLayout.LayoutParams(dp(50), -1));
        container.addView(stepper, new LinearLayout.LayoutParams(0, dp(38), 1));
        return container;
    }

    private final class NumberStepper extends LinearLayout {
        private final int min;
        private final int max;
        private int current;
        private final TextView valueLabel;
        private final TextView decrement;
        private final TextView increment;

        NumberStepper(int min, int max, int selected) {
            super(AssistantActivity.this);
            this.min = min;
            this.max = Math.max(min, max);
            current = Math.max(min, Math.min(this.max, selected));
            setOrientation(HORIZONTAL);
            setGravity(Gravity.CENTER);
            setPadding(dp(2), dp(2), dp(2), dp(2));
            setBackground(HeimdallUi.fieldPanel(AssistantActivity.this, 7));

            decrement = stepButton("-");
            valueLabel = text(String.valueOf(current), 12, TEXT, true);
            valueLabel.setGravity(Gravity.CENTER);
            valueLabel.setSingleLine(true);
            increment = stepButton("+");

            addView(decrement, new LinearLayout.LayoutParams(dp(30), -1));
            addView(valueLabel, new LinearLayout.LayoutParams(0, -1, 1));
            addView(increment, new LinearLayout.LayoutParams(dp(30), -1));
            decrement.setOnClickListener(v -> setValue(current - 1));
            increment.setOnClickListener(v -> setValue(current + 1));
            refresh();
        }

        private TextView stepButton(String label) {
            TextView button = text(label, 16, TEXT, true);
            button.setGravity(Gravity.CENTER);
            button.setSingleLine(true);
            button.setBackgroundColor(Color.TRANSPARENT);
            button.setClickable(true);
            button.setFocusable(true);
            return button;
        }

        private void setValue(int value) {
            int clamped = Math.max(min, Math.min(max, value));
            if (clamped == current) {
                return;
            }
            current = clamped;
            refresh();
        }

        private void refresh() {
            valueLabel.setText(String.valueOf(current));
            decrement.setAlpha(current > min ? 1f : 0.28f);
            increment.setAlpha(current < max ? 1f : 0.28f);
            decrement.setEnabled(current > min);
            increment.setEnabled(current < max);
        }

        int value() {
            return current;
        }
    }

    private int dockIndexForScreen(int screen) {
        if (screen <= SCREEN_MAIN) {
            return 0;
        }
        if (screen >= SCREEN_SETTINGS) {
            return 3;
        }
        return screen;
    }

    private final class QuickVolumeSeekBar extends SeekBar {
        private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        QuickVolumeSeekBar(Context context) {
            super(context);
            setPadding(dp(7), 0, dp(7), 0);
            setThumb(new ColorDrawable(Color.TRANSPARENT));
            setProgressDrawable(new ColorDrawable(Color.TRANSPARENT));
            setSplitTrack(false);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            DebugPerformanceDiagnostics.countDraw("Quick Actions volume");
            int width = getWidth();
            int height = getHeight();
            if (width <= 0 || height <= 0) {
                return;
            }
            float left = getPaddingLeft();
            float right = width - getPaddingRight();
            float centerY = height / 2f;
            boolean pearl = HeimdallUi.isPearl(AssistantActivity.this);
            float trackHalf = dp(HeimdallUi.VOLUME_TRACK_HEIGHT) / 2f;
            float fraction = getMax() <= 0 ? 0f : getProgress() / (float) getMax();
            float thumbX = left + (right - left) * fraction;

            trackPaint.setStyle(Paint.Style.FILL);
            if (pearl) {
                trackPaint.setColor(0x44526170);
                canvas.drawRoundRect(left, centerY - trackHalf - dp(1), right,
                        centerY + trackHalf + dp(1), trackHalf + dp(1), trackHalf + dp(1), trackPaint);
            }
            trackPaint.setColor(HeimdallUi.volumeTrack(AssistantActivity.this));
            canvas.drawRoundRect(left, centerY - trackHalf, right, centerY + trackHalf,
                    trackHalf, trackHalf, trackPaint);

            if (thumbX > left) {
                trackPaint.setColor(HeimdallUi.volumeActive(AssistantActivity.this));
                canvas.drawRoundRect(left, centerY - trackHalf, thumbX, centerY + trackHalf,
                        trackHalf, trackHalf, trackPaint);
            }

            float thumbRadius = dp(HeimdallUi.VOLUME_THUMB_SIZE) / 2f;
            if (pearl) {
                trackPaint.setColor(0x55455260);
                canvas.drawCircle(thumbX, centerY + dp(1), thumbRadius + dp(1), trackPaint);
            }
            if (isPressed()) {
                trackPaint.setColor(pearl ? 0x44F08A2A : 0x4455B7E8);
                canvas.drawCircle(thumbX, centerY, thumbRadius + dp(4), trackPaint);
            }
            trackPaint.setColor(pearl
                    ? (isPressed() ? 0xFFFFC17A : 0xFFF08A2A)
                    : (isPressed() ? 0xFFBDEBFF : 0xFF8ACAF0));
            canvas.drawCircle(thumbX, centerY, thumbRadius, trackPaint);
            trackPaint.setStyle(Paint.Style.STROKE);
            trackPaint.setStrokeWidth(dp(1));
            trackPaint.setColor(pearl ? 0xCCFFF4E8 : 0xCCEFF9FF);
            canvas.drawCircle(thumbX, centerY, thumbRadius, trackPaint);
            if (pearl) {
                trackPaint.setStyle(Paint.Style.FILL);
                trackPaint.setColor(0xAAFFF5E8);
                canvas.drawCircle(thumbX - thumbRadius * 0.3f, centerY - thumbRadius * 0.3f,
                        Math.max(1f, thumbRadius * 0.16f), trackPaint);
            }
        }
    }

    private final class BatteryStatusView extends View {
        private final Paint batteryPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path batteryBolt = new Path();
        private int percent = -1;
        private boolean charging;

        BatteryStatusView(Context context) {
            super(context);
        }

        void setBattery(int value, boolean isCharging) {
            if (percent == value && charging == isCharging) {
                return;
            }
            percent = value;
            charging = isCharging;
            invalidate();
        }

        @Override
        public void invalidate() {
            DebugPerformanceDiagnostics.countInvalidate("Header battery");
            super.invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            DebugPerformanceDiagnostics.countDraw("Header battery");
            super.onDraw(canvas);
            float width = getWidth();
            float height = getHeight();
            if (width <= 0 || height <= 0) {
                return;
            }
            boolean pearl = HeimdallUi.isPearl(AssistantActivity.this);
            float shellLeft = dp(2);
            float shellTop = height * 0.27f;
            float shellRight = width - dp(4);
            float shellBottom = height * 0.73f;
            float radius = dp(2);
            batteryPaint.setShader(null);
            batteryPaint.setStyle(Paint.Style.STROKE);
            batteryPaint.setStrokeWidth(dp(1));
            batteryPaint.setColor(pearl ? 0xFF596774 : 0xFFD9E8F8);
            canvas.drawRoundRect(shellLeft, shellTop, shellRight, shellBottom,
                    radius, radius, batteryPaint);
            batteryPaint.setStyle(Paint.Style.FILL);
            canvas.drawRoundRect(shellRight + dp(1), height * 0.39f, width - dp(1),
                    height * 0.61f, dp(1), dp(1), batteryPaint);

            float level = percent < 0 ? 0.25f : Math.max(0.06f, Math.min(1f, percent / 100f));
            float inset = dp(2);
            float fillRight = shellLeft + inset + (shellRight - shellLeft - inset * 2f) * level;
            batteryPaint.setColor(pearl ? 0xFFF08A2A : 0xFF70B7FF);
            canvas.drawRoundRect(shellLeft + inset, shellTop + inset, fillRight,
                    shellBottom - inset, dp(1), dp(1), batteryPaint);
            if (charging) {
                batteryPaint.setColor(pearl ? 0xFFFEF4E8 : 0xFFF4FAFF);
                batteryBolt.reset();
                float cx = (shellLeft + shellRight) / 2f;
                float cy = height / 2f;
                batteryBolt.moveTo(cx + dp(1), cy - dp(4));
                batteryBolt.lineTo(cx - dp(2), cy);
                batteryBolt.lineTo(cx, cy);
                batteryBolt.lineTo(cx - dp(1), cy + dp(4));
                batteryBolt.lineTo(cx + dp(3), cy - dp(1));
                batteryBolt.lineTo(cx + dp(1), cy - dp(1));
                batteryBolt.close();
                canvas.drawPath(batteryBolt, batteryPaint);
            }
        }
    }

    private final class QuickActionButtonView extends ImageButton {
        static final int STATE_IDLE = 0;
        static final int STATE_MAGNIFIER_STOP = 1;
        static final int STATE_RECORDING_STOP = 2;
        private static final int RESULT_NONE = 0;
        private static final int RESULT_SUCCESS = 1;
        private static final int RESULT_ERROR = 2;
        private static final long RESULT_VISIBLE_MS = 800L;
        private static final long RESULT_ENTER_MS = 70L;
        private static final long RESULT_EXIT_MS = 110L;
        private static final long RESULT_HOLD_MS =
                RESULT_VISIBLE_MS - RESULT_ENTER_MS - RESULT_EXIT_MS;
        private static final float RESULT_TINT_OPACITY = 0.68f;

        private int visualState = STATE_IDLE;
        private int actionIconRes;
        private int transientResult = RESULT_NONE;
        private float resultProgress;
        private ValueAnimator resultAnimator;
        private final Runnable beginResultExit = this::startResultExit;
        private final Runnable clearResult = this::clearTransientResult;

        QuickActionButtonView(Context context) {
            super(context);
        }

        void setActionIcon(int iconRes) {
            actionIconRes = iconRes;
            if (transientResult == RESULT_NONE) {
                setImageResource(iconRes);
            }
        }

        void setVisualState(int value) {
            visualState = value;
            applyStateColor();
            setBackgroundColor(Color.TRANSPARENT);
            invalidate();
        }

        void showTransientResult(boolean success) {
            cancelResultAnimation();
            removeCallbacks(clearResult);
            removeCallbacks(beginResultExit);
            transientResult = success ? RESULT_SUCCESS : RESULT_ERROR;
            setImageResource(actionIconRes);
            resultProgress = shouldAnimateUi() ? 0f : 1f;
            applyStateColor();
            invalidate();
            if (shouldAnimateUi()) {
                animateResultProgress(1f, RESULT_ENTER_MS,
                        () -> postDelayed(beginResultExit, RESULT_HOLD_MS));
            } else {
                postDelayed(clearResult, RESULT_VISIBLE_MS);
            }
        }

        void clearTransientResult() {
            removeCallbacks(clearResult);
            removeCallbacks(beginResultExit);
            cancelResultAnimation();
            if (transientResult == RESULT_NONE) {
                return;
            }
            finishTransientResult();
        }

        private void startResultExit() {
            if (transientResult == RESULT_NONE) {
                return;
            }
            animateResultProgress(0f, RESULT_EXIT_MS, this::finishTransientResult);
        }

        private void finishTransientResult() {
            resultProgress = 0f;
            transientResult = RESULT_NONE;
            if (actionIconRes != 0) {
                setImageResource(actionIconRes);
            }
            applyStateColor();
            invalidate();
        }

        private void animateResultProgress(float target, long duration, Runnable endAction) {
            cancelResultAnimation();
            ValueAnimator animator = ValueAnimator.ofFloat(resultProgress, target);
            resultAnimator = animator;
            animator.setDuration(duration);
            animator.setInterpolator(UI_EASE_OUT);
            animator.addUpdateListener(valueAnimator -> {
                resultProgress = (float) valueAnimator.getAnimatedValue();
                applyStateColor();
                invalidate();
            });
            animator.addListener(new AnimatorListenerAdapter() {
                private boolean cancelled;

                @Override
                public void onAnimationCancel(Animator animation) {
                    cancelled = true;
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    if (resultAnimator == animation) {
                        resultAnimator = null;
                    }
                    if (!cancelled && endAction != null) {
                        endAction.run();
                    }
                }
            });
            animator.start();
        }

        private void cancelResultAnimation() {
            if (resultAnimator != null) {
                resultAnimator.cancel();
                resultAnimator = null;
            }
        }

        @Override
        public void invalidate() {
            DebugPerformanceDiagnostics.countInvalidate("Quick Action button");
            super.invalidate();
        }

        private int baseStateColor(boolean pearl) {
            if (visualState == STATE_RECORDING_STOP) {
                return pearl ? 0xFFE0525C : 0xFFFF6B7A;
            }
            if (visualState == STATE_MAGNIFIER_STOP) {
                return pearl ? 0xFFE77F1F : 0xFF70B7FF;
            }
            return pearl ? 0xFF536274 : 0xFFD9E8F8;
        }

        private int resultColor(boolean pearl) {
            if (transientResult == RESULT_SUCCESS) {
                return HeimdallUi.COLOR_SUCCESS;
            }
            if (transientResult == RESULT_ERROR) {
                // Match the existing delete-control red in each theme.
                return pearl ? 0xFFB34A4F : HeimdallUi.COLOR_DANGER;
            }
            return baseStateColor(pearl);
        }

        private int blendColor(int from, int to, float progress) {
            float amount = Math.max(0f, Math.min(1f, progress));
            return Color.argb(
                    Math.round(Color.alpha(from) + (Color.alpha(to) - Color.alpha(from)) * amount),
                    Math.round(Color.red(from) + (Color.red(to) - Color.red(from)) * amount),
                    Math.round(Color.green(from) + (Color.green(to) - Color.green(from)) * amount),
                    Math.round(Color.blue(from) + (Color.blue(to) - Color.blue(from)) * amount));
        }

        private void applyStateColor() {
            boolean pearl = HeimdallUi.isPearl(AssistantActivity.this);
            setColorFilter(blendColor(baseStateColor(pearl), resultColor(pearl),
                    resultProgress * RESULT_TINT_OPACITY));
        }

        @Override
        protected void drawableStateChanged() {
            super.drawableStateChanged();
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            DebugPerformanceDiagnostics.countDraw("Quick Action button");
            int width = getWidth();
            int height = getHeight();
            if (width <= 0 || height <= 0) {
                return;
            }
            Drawable icon = getDrawable();
            if (icon != null) {
                int size = Math.min(dp(HeimdallUi.QUICK_ACTION_ICON_SIZE),
                        Math.max(dp(22), Math.min(width, height) - dp(18)));
                int left = (width - size) / 2;
                int top = (height - size) / 2 + (isPressed() ? dp(1) : 0);
                icon.setAlpha(255);
                icon.setBounds(left, top, left + size, top + size);
                icon.draw(canvas);
            }
        }

        @Override
        protected void onDetachedFromWindow() {
            removeCallbacks(clearResult);
            removeCallbacks(beginResultExit);
            cancelResultAnimation();
            resultProgress = 0f;
            transientResult = RESULT_NONE;
            super.onDetachedFromWindow();
        }
    }

    private static final class GuideFileTitleState {
        boolean userEdited;
        boolean applyingAutomaticTitle;
    }

    private static final class MacroGridBinding {
        final LinearLayout grid;
        final WidgetLayout.Item item;

        MacroGridBinding(LinearLayout grid, WidgetLayout.Item item) {
            this.grid = grid;
            this.item = item;
        }
    }

    private static final class MacroModuleEditorBinding {
        final WidgetLayout.Item item;
        final NumberStepper startInput;
        final NumberStepper countInput;
        final NumberStepper columnsInput;
        final NumberStepper rowsInput;
        final CheckBox rightInput;
        final CheckBox iconOnlyInput;

        MacroModuleEditorBinding(WidgetLayout.Item item, NumberStepper startInput,
                                 NumberStepper countInput, NumberStepper columnsInput,
                                 NumberStepper rowsInput, CheckBox rightInput,
                                 CheckBox iconOnlyInput) {
            this.item = item;
            this.startInput = startInput;
            this.countInput = countInput;
            this.columnsInput = columnsInput;
            this.rowsInput = rowsInput;
            this.rightInput = rightInput;
            this.iconOnlyInput = iconOnlyInput;
        }
    }

    private static final class PanelOverlay {
        final FrameLayout scrim;
        final View panel;
        final Runnable onDismiss;

        PanelOverlay(FrameLayout scrim, View panel, Runnable onDismiss) {
            this.scrim = scrim;
            this.panel = panel;
            this.onDismiss = onDismiss;
        }
    }

    private static final class GamepadRecordingSession {
        final List<MacroStep> draftSteps;
        final LinearLayout stepsList;
        final int replaceIndex;
        final TextView headerTitle;
        final TextView headerSubtitle;
        final LinearLayout body;
        final LinearLayout actions;
        PanelOverlay overlay;
        NativeGamepadPath.Device device;
        Runnable ticker;
        long startedAtMs;
        boolean cancelled;
        boolean testing;
        String sequence;
        Button resultRetakeButton;
        Button resultTestButton;
        Button resultCommitButton;

        GamepadRecordingSession(List<MacroStep> draftSteps, LinearLayout stepsList,
                int replaceIndex, TextView headerTitle, TextView headerSubtitle,
                LinearLayout body, LinearLayout actions) {
            this.draftSteps = draftSteps;
            this.stepsList = stepsList;
            this.replaceIndex = replaceIndex;
            this.headerTitle = headerTitle;
            this.headerSubtitle = headerSubtitle;
            this.body = body;
            this.actions = actions;
        }
    }

    private final class SliderInput {
        private final int minUnits;
        private final int maxUnits;
        private final int scale;
        private final String suffix;
        private int currentUnits;
        private SeekBar seekBar;
        private TextView valueLabel;

        SliderInput(float min, float max, float value, int scale, String suffix) {
            this.scale = scale;
            this.suffix = suffix == null ? "" : suffix;
            this.minUnits = Math.round(min * scale);
            this.maxUnits = Math.round(max * scale);
            int units = Math.round(value * scale);
            this.currentUnits = Math.max(minUnits, Math.min(maxUnits, units));
        }

        int intValue() {
            return Math.round(floatValue());
        }

        float floatValue() {
            return currentUnits / (float) scale;
        }

        void updateLabel() {
            if (valueLabel == null) {
                return;
            }
            String value;
            if (scale == 1) {
                value = String.valueOf(currentUnits);
            } else {
                value = String.format(Locale.US, "%.1f", floatValue());
            }
            valueLabel.setText(value + suffix);
        }
    }

    private void addProfileFrom(GameProfile source) {
        List<Macro> macros = new ArrayList<>();
        for (Macro macro : source.macros) {
            List<MacroStep> steps = new ArrayList<>();
            for (MacroStep step : macro.steps) {
                steps.add(new MacroStep(step.type, step.value));
            }
            Macro copy = new Macro(macro.label, steps);
            copy.highlighted = macro.highlighted;
            copy.role = macro.normalizedRole();
            copy.iconKey = macro.iconKey;
            macros.add(copy);
        }
        GameProfile profile = new GameProfile(getString(R.string.profile_copy_name, source.name),
                getString(R.string.profile_generic_game),
                source.packageHint, source.macroCount, macros);
        profile.romContextHint = "";
        profile.defaultForPackage = false;
        profile.iconUri = source.iconUri;
        for (GuideEntry guide : source.guides) {
            profile.guides.add(guide.copy());
        }
        for (MapEntry map : source.safeMaps()) {
            profile.maps.add(new MapEntry(map.title, map.uri));
        }
        profile.syncLegacyMapFields();
        profile.interactiveMapTitle = source.interactiveMapTitle;
        profile.interactiveMapUrl = source.interactiveMapUrl;
        for (MapMarker marker : source.mapMarkers) {
            profile.mapMarkers.add(new MapMarker(marker.title, marker.note, marker.position));
        }
        profile.macroColumns = source.macroColumns;
        profile.macroRows = source.macroRows;
        profile.rightHandPriority = source.rightHandPriority;
        profile.protectThorMappingDuringEnhancedTouch =
                source.protectThorMappingDuringEnhancedTouch;
        profile.touchpadSettings = source.safeTouchpadSettings().copy();
        profile.widgetLayout = source.safeWidgetLayout().copy();
        profile.normalizeLayout();
        profiles.add(profile);
        selectedProfileIndex = profiles.size() - 1;
        selectedProfile = profile;
        draftWidgetLayout = null;
        settingsTouchpadDraft = null;
        settingsMacroMappingProtectionInput = null;
        settingsMacroMappingProtectionDraft = null;
        touchpadSettings = selectedProfile.safeTouchpadSettings();
        ProfileStore.saveSelectedIndex(this, selectedProfileIndex);
        ProfileStore.saveProfiles(this, profiles);
        renderProfiles();
        renderSelectedProfile();
        showAction(getString(R.string.profile_copied, profile.name));
    }

    private void addBlankProfile() {
        List<Macro> macros = new ArrayList<>();
        int defaultCount = 4;
        for (int i = 0; i < defaultCount; i++) {
            macros.add(new Macro(getString(R.string.macro_default_name, i + 1),
                    ProfileStore.steps("wait:80ms")));
        }
        GameProfile profile = new GameProfile(getString(R.string.profile_new_name),
                getString(R.string.profile_generic_game), "", defaultCount, macros);
        profile.macroColumns = 4;
        profile.macroRows = 0;
        profile.rightHandPriority = true;
        profile.normalizeLayout();
        profiles.add(profile);
        selectedProfileIndex = profiles.size() - 1;
        selectedProfile = profile;
        draftWidgetLayout = null;
        settingsTouchpadDraft = null;
        settingsMacroMappingProtectionInput = null;
        settingsMacroMappingProtectionDraft = null;
        touchpadSettings = selectedProfile.safeTouchpadSettings();
        ProfileStore.saveSelectedIndex(this, selectedProfileIndex);
        ProfileStore.saveProfiles(this, profiles);
        renderProfiles();
        renderSelectedProfile();
        showAction(getString(R.string.profile_created));
    }

    private void deleteProfile(int index) {
        if (profiles.size() <= 1) {
            showErrorAction(getString(R.string.profile_keep_one));
            return;
        }
        if (index < 0 || index >= profiles.size()) {
            return;
        }
        if (!protectProfilesBefore(ProfileSnapshotStore.REASON_DELETE)) {
            return;
        }
        String removedName = profiles.get(index).name;
        profiles.remove(index);
        if (selectedProfileIndex >= profiles.size()) {
            selectedProfileIndex = profiles.size() - 1;
        }
        if (index < selectedProfileIndex) {
            selectedProfileIndex--;
        }
        selectedProfile = profiles.get(selectedProfileIndex);
        draftWidgetLayout = null;
        settingsTouchpadDraft = null;
        settingsMacroMappingProtectionInput = null;
        settingsMacroMappingProtectionDraft = null;
        touchpadSettings = selectedProfile.safeTouchpadSettings();
        ProfileStore.saveSelectedIndex(this, selectedProfileIndex);
        ProfileStore.saveProfiles(this, profiles);
        renderProfiles();
        renderSelectedProfile();
        showAction(getString(R.string.profile_deleted_recoverable, removedName));
    }

    private String nonEmpty(String value, String fallback) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.length() == 0 ? fallback : trimmed;
    }

    private void showMacroEditor(Macro macro) {
        List<MacroStep> draftSteps = new ArrayList<>();
        for (MacroStep step : macro.steps) {
            draftSteps.add(new MacroStep(step.type, step.value));
        }
        String[] draftIconKey = {macro.iconKey};
        String[] draftRole = {macro.normalizedRole()};

        LinearLayout editorShell = new LinearLayout(this);
        editorShell.setOrientation(LinearLayout.VERTICAL);
        editorShell.setBackground(HeimdallUi.isPearl(this)
                ? HeimdallUi.cncFlush(this, 14)
                : HeimdallUi.glass(this, 0xFA0B111B, 0xFF070A10,
                        0x886A829C, 0x44344150, 14, 2));

        LinearLayout editorHeader = new LinearLayout(this);
        editorHeader.setOrientation(LinearLayout.HORIZONTAL);
        editorHeader.setGravity(Gravity.CENTER_VERTICAL);
        editorHeader.setPadding(dp(16), 0, dp(16), 0);
        editorShell.addView(editorHeader, new LinearLayout.LayoutParams(-1, dp(58)));

        LinearLayout heading = new LinearLayout(this);
        heading.setOrientation(LinearLayout.VERTICAL);
        heading.setGravity(Gravity.CENTER_VERTICAL);
        editorHeader.addView(heading, new LinearLayout.LayoutParams(0, -1, 1));

        TextView titleView = text(getString(R.string.macro_editor_title),
                HeimdallUi.TYPE_EDITOR_TITLE, TEXT, true);
        heading.addView(titleView, new LinearLayout.LayoutParams(-1, dp(30)));
        TextView editorSubtitle = text(getString(R.string.macro_editor_subtitle,
                nonEmpty(macro.label, getString(R.string.common_macro_fallback)),
                getResources().getQuantityString(R.plurals.step_count,
                        draftSteps.size(), draftSteps.size())), 11, MUTED, false);
        heading.addView(editorSubtitle, new LinearLayout.LayoutParams(-1, dp(20)));

        View headerDivider = new View(this);
        headerDivider.setBackgroundColor(HeimdallUi.isPearl(this) ? 0x287B8792 : 0x445F7C9A);
        editorShell.addView(headerDivider, new LinearLayout.LayoutParams(-1, dp(1)));

        LinearLayout editorBody = new LinearLayout(this);
        editorBody.setOrientation(LinearLayout.HORIZONTAL);
        editorBody.setPadding(dp(14), dp(12), dp(14), dp(10));
        editorShell.addView(editorBody, new LinearLayout.LayoutParams(-1, 0, 1));

        ScrollView basicsScroll = new ScrollView(this);
        basicsScroll.setFillViewport(false);
        basicsScroll.setScrollbarFadingEnabled(false);
        editorBody.addView(basicsScroll, new LinearLayout.LayoutParams(0, -1, 38));

        LinearLayout basics = new LinearLayout(this);
        basics.setOrientation(LinearLayout.VERTICAL);
        basics.setPadding(dp(2), 0, dp(12), dp(4));
        basicsScroll.addView(basics, new ScrollView.LayoutParams(-1, -2));

        TextView basicsLabel = text(getString(R.string.macro_editor_basic_info),
                HeimdallUi.TYPE_SECTION_TITLE, TEXT, true);
        basics.addView(basicsLabel, new LinearLayout.LayoutParams(-1, dp(30)));

        TextView nameLabel = text(getString(R.string.macro_editor_button_name),
                HeimdallUi.TYPE_LABEL, MUTED, true);
        basics.addView(nameLabel, new LinearLayout.LayoutParams(-1, dp(26)));

        EditText labelInput = new EditText(this);
        labelInput.setSingleLine(true);
        labelInput.setText(macro.label);
        styleDarkEditText(labelInput);
        basics.addView(labelInput, new LinearLayout.LayoutParams(-1, dp(50)));

        TextView roleLabel = text(getString(R.string.macro_editor_emphasis),
                HeimdallUi.TYPE_LABEL, MUTED, true);
        basics.addView(roleLabel, new LinearLayout.LayoutParams(-1, dp(28)));

        LinearLayout roleRow = new LinearLayout(this);
        roleRow.setOrientation(LinearLayout.HORIZONTAL);
        String[] roleValues = {Macro.ROLE_PRIMARY, Macro.ROLE_SECONDARY, Macro.ROLE_UTILITY};
        String[] roleLabels = {getString(R.string.macro_role_short_high),
                getString(R.string.macro_role_short_medium),
                getString(R.string.macro_role_short_low)};
        Button[] roleButtons = new Button[roleValues.length];
        for (int i = 0; i < roleValues.length; i++) {
            final String role = roleValues[i];
            Button roleButton = editorButton(roleLabels[i], () -> {
                draftRole[0] = role;
                for (int j = 0; j < roleButtons.length; j++) {
                    HeimdallUi.applyMacroRoleChoiceButton(this, roleButtons[j], j,
                            roleValues[j].equals(draftRole[0]));
                }
            });
            roleButton.setTextSize(11);
            roleButton.setSingleLine(true);
            roleButton.setPadding(dp(2), 0, dp(2), 0);
            HeimdallUi.applyMacroRoleChoiceButton(this, roleButton, i, role.equals(draftRole[0]));
            roleButtons[i] = roleButton;
            LinearLayout.LayoutParams roleParams = new LinearLayout.LayoutParams(0, -1, 1);
            roleParams.setMargins(i == 0 ? 0 : dp(3), 0, 0, 0);
            roleRow.addView(roleButton, roleParams);
        }
        LinearLayout.LayoutParams roleRowParams = new LinearLayout.LayoutParams(-1, dp(46));
        roleRowParams.setMargins(0, 0, 0, dp(4));
        basics.addView(roleRow, roleRowParams);

        TextView iconLabel = text(getString(R.string.macro_editor_icon),
                HeimdallUi.TYPE_LABEL, MUTED, true);
        basics.addView(iconLabel, new LinearLayout.LayoutParams(-1, dp(28)));

        LinearLayout iconRow = new LinearLayout(this);
        iconRow.setOrientation(LinearLayout.HORIZONTAL);
        iconRow.setGravity(Gravity.CENTER_VERTICAL);
        basics.addView(iconRow, new LinearLayout.LayoutParams(-1, dp(112)));

        ImageView iconPreview = new ImageView(this);
        iconPreview.setScaleType(ImageView.ScaleType.FIT_CENTER);
        iconPreview.setPadding(dp(14), dp(14), dp(14), dp(14));
        iconPreview.setBackground(HeimdallUi.isPearl(this)
                ? HeimdallUi.cncShallowInset(this, 10)
                : HeimdallUi.insetPanel(this, 10));
        updateMacroIconPreview(iconPreview, macro, draftIconKey[0]);
        LinearLayout.LayoutParams previewParams = new LinearLayout.LayoutParams(dp(84), dp(84));
        previewParams.setMargins(0, dp(4), dp(10), dp(4));
        iconRow.addView(iconPreview, previewParams);

        LinearLayout iconActions = new LinearLayout(this);
        iconActions.setOrientation(LinearLayout.VERTICAL);
        iconRow.addView(iconActions, new LinearLayout.LayoutParams(0, -1, 1));
        Button chooseIconButton = editorButton(getString(R.string.macro_editor_choose_icon), () ->
                showMacroIconPicker(macro, draftIconKey, iconPreview));
        iconActions.addView(chooseIconButton, new LinearLayout.LayoutParams(-1, 0, 1));
        Button defaultIconButton = editorButton(getString(R.string.macro_editor_use_default), () -> {
            draftIconKey[0] = null;
            updateMacroIconPreview(iconPreview, macro, null);
        });
        iconActions.addView(defaultIconButton, new LinearLayout.LayoutParams(-1, 0, 1));

        View columnDivider = new View(this);
        columnDivider.setBackgroundColor(HeimdallUi.isPearl(this) ? 0x287B8792 : 0x33445A72);
        LinearLayout.LayoutParams columnDividerParams = new LinearLayout.LayoutParams(dp(1), -1);
        columnDividerParams.setMargins(dp(2), dp(2), dp(12), dp(2));
        editorBody.addView(columnDivider, columnDividerParams);

        LinearLayout stepsPanel = new LinearLayout(this);
        stepsPanel.setOrientation(LinearLayout.VERTICAL);
        editorBody.addView(stepsPanel, new LinearLayout.LayoutParams(0, -1, 62));

        TextView stepsLabel = text(getString(R.string.macro_editor_steps),
                HeimdallUi.TYPE_SECTION_TITLE, TEXT, true);
        stepsPanel.addView(stepsLabel, new LinearLayout.LayoutParams(-1, dp(30)));

        ScrollView stepScroll = new ScrollView(this);
        stepScroll.setFillViewport(false);
        stepScroll.setScrollbarFadingEnabled(false);
        stepScroll.setBackground(HeimdallUi.isPearl(this)
                ? HeimdallUi.cncShallowInset(this, 10)
                : HeimdallUi.insetPanel(this, 10));
        LinearLayout stepsList = new LinearLayout(this);
        stepsList.setOrientation(LinearLayout.VERTICAL);
        stepsList.setPadding(dp(6), dp(4), dp(6), dp(4));
        stepsList.setTag(Boolean.TRUE);
        stepScroll.addView(stepsList, new ScrollView.LayoutParams(-1, -2));
        LinearLayout.LayoutParams stepScrollParams = new LinearLayout.LayoutParams(-1, 0, 1);
        stepScrollParams.setMargins(0, 0, 0, dp(6));
        stepsPanel.addView(stepScroll, stepScrollParams);
        renderStepList(draftSteps, stepsList);

        LinearLayout captureRow = new LinearLayout(this);
        captureRow.setOrientation(LinearLayout.HORIZONTAL);
        TextView addLabel = text(getString(R.string.macro_editor_add_step),
                HeimdallUi.TYPE_LABEL, MUTED, true);
        stepsPanel.addView(addLabel, new LinearLayout.LayoutParams(-1, dp(24)));

        LinearLayout gamepadRow = new LinearLayout(this);
        gamepadRow.setOrientation(LinearLayout.HORIZONTAL);
        stepsPanel.addView(gamepadRow, new LinearLayout.LayoutParams(-1, dp(48)));
        Button gamepadRecordButton = editorButton(
                getString(R.string.macro_editor_record_controller), () ->
                showGamepadRecordingPanel(draftSteps, stepsList, -1));
        setLeftIcon(gamepadRecordButton, R.drawable.ic_macro_gamepad,
                HeimdallUi.textColor(this), dp(18));
        gamepadRecordButton.setCompoundDrawablePadding(dp(7));
        gamepadRow.addView(gamepadRecordButton);

        stepsPanel.addView(captureRow, new LinearLayout.LayoutParams(-1, dp(46)));

        captureRow.addView(editorButton(getString(R.string.macro_editor_capture_tap),
                () -> startCapture(draftSteps, stepsList, CoordinateCaptureActivity.MODE_TAP)));
        captureRow.addView(editorButton(getString(R.string.macro_editor_capture_hold),
                () -> startCapture(draftSteps, stepsList, CoordinateCaptureActivity.MODE_HOLD)));
        captureRow.addView(editorButton(getString(R.string.macro_editor_capture_swipe),
                () -> startCapture(draftSteps, stepsList, CoordinateCaptureActivity.MODE_SWIPE)));

        LinearLayout waitRow = new LinearLayout(this);
        waitRow.setOrientation(LinearLayout.HORIZONTAL);
        stepsPanel.addView(waitRow, new LinearLayout.LayoutParams(-1, dp(46)));

        EditText waitInput = new EditText(this);
        waitInput.setSingleLine(true);
        waitInput.setText("80");
        waitInput.setTextSize(13);
        waitInput.setGravity(Gravity.CENTER);
        styleDarkEditText(waitInput);
        waitRow.addView(waitInput, new LinearLayout.LayoutParams(0, -1, 1));
        waitRow.addView(editorButton(getString(R.string.macro_editor_add_wait),
                () -> addWaitStep(draftSteps, stepsList, waitInput)));
        waitRow.addView(editorButton(getString(R.string.macro_editor_clear), () -> {
            draftSteps.clear();
            renderStepList(draftSteps, stepsList);
        }));

        int helpText = shouldProtectThorMappingFromControllerMacros()
                ? R.string.macro_editor_help_enhanced_touch
                : (isEnhancedTouchModeActive()
                        ? R.string.macro_editor_help_enhanced_touch_controller_enabled
                        : R.string.macro_editor_help);
        TextView help = text(getString(helpText),
                HeimdallUi.TYPE_META, MUTED, false);
        stepsPanel.addView(help, new LinearLayout.LayoutParams(-1, dp(32)));

        final PanelOverlay[] overlayHolder = new PanelOverlay[1];
        View footerDivider = new View(this);
        footerDivider.setBackgroundColor(HeimdallUi.isPearl(this) ? 0x287B8792 : 0x445F7C9A);
        editorShell.addView(footerDivider, new LinearLayout.LayoutParams(-1, dp(1)));

        LinearLayout actionRow = new LinearLayout(this);
        actionRow.setOrientation(LinearLayout.HORIZONTAL);
        actionRow.setGravity(Gravity.CENTER_VERTICAL);
        actionRow.setPadding(dp(12), dp(4), dp(12), dp(4));
        editorShell.addView(actionRow, new LinearLayout.LayoutParams(-1, dp(62)));

        Button cancelButton = editorButton(getString(R.string.common_cancel), () -> {
            if (overlayHolder[0] != null) {
                dismissPanelAnimated(overlayHolder[0]);
            }
        });
        actionRow.addView(cancelButton);

        Button saveButton = editorButton(getString(R.string.common_save), () -> {
            String label = labelInput.getText().toString().trim();
            macro.label = label.length() == 0 ? "Macro" : label;
            macro.role = Macro.normalizeRole(draftRole[0]);
            macro.highlighted = Macro.ROLE_PRIMARY.equals(macro.role);
            macro.iconKey = draftIconKey[0] == null || draftIconKey[0].trim().length() == 0
                    ? null : draftIconKey[0].trim();
            macro.steps.clear();
            macro.steps.addAll(draftSteps);
            if (macro.steps.isEmpty()) {
                macro.steps.add(new MacroStep(MacroStep.TYPE_WAIT, "80ms"));
            }
            ProfileStore.saveProfiles(this, profiles);
            renderSelectedProfile();
            showAction(getString(R.string.macro_saved, macro.label));
            if (overlayHolder[0] != null) {
                dismissPanelAnimated(overlayHolder[0]);
            }
        });
        HeimdallUi.applyPrimaryActionButton(this, saveButton);
        actionRow.addView(saveButton);

        activeDraftSteps = draftSteps;
        activeStepsList = stepsList;
        activeEditingMacro = macro;
        captureInProgress = false;

        FrameLayout.LayoutParams editorParams = new FrameLayout.LayoutParams(-1, -1);
        editorParams.setMargins(dp(8), dp(8), dp(8), dp(8));
        overlayHolder[0] = showPanelOverlay(editorShell, editorParams, () -> {
            activeStepsList = null;
            if (!captureInProgress) {
                activeDraftSteps = null;
                activeEditingMacro = null;
            }
        });
    }

    private void styleDarkEditText(EditText input) {
        input.setTextColor(HeimdallUi.textColor(this));
        input.setHintTextColor(HeimdallUi.mutedTextColor(this));
        input.setBackground(HeimdallUi.fieldPanel(this, 9));
        input.setPadding(dp(10), 0, dp(10), 0);
        bindTextInputFocus(input);
    }

    private boolean shouldAnimateUi() {
        return !DebugPerformanceDiagnostics.isFlatUi() && ValueAnimator.areAnimatorsEnabled();
    }

    private void trackAnimatedView(View view) {
        if (!transientAnimatedViews.contains(view)) {
            transientAnimatedViews.add(view);
        }
    }

    private void finishAnimatedView(View view) {
        transientAnimatedViews.remove(view);
    }

    private void preparePanelForDeferredEnter(View panel) {
        if (panel == null) {
            return;
        }
        panel.setAlpha(0f);
        panel.setVisibility(View.INVISIBLE);
    }

    private void animatePanelIn(View panel) {
        if (panel == null) {
            return;
        }
        panel.animate().cancel();
        finishAnimatedView(panel);
        closingAnimatedPanels.remove(panel);
        panel.setScaleX(1f);
        panel.setScaleY(1f);
        if (!shouldAnimateUi()) {
            panel.setAlpha(1f);
            panel.setVisibility(View.VISIBLE);
            return;
        }
        panel.setAlpha(0f);
        trackAnimatedView(panel);
        ViewTreeObserver observer = panel.getViewTreeObserver();
        observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                if (panel.getViewTreeObserver().isAlive()) {
                    panel.getViewTreeObserver().removeOnPreDrawListener(this);
                }
                if (panel.getParent() == null || closingAnimatedPanels.contains(panel)) {
                    finishAnimatedView(panel);
                    return true;
                }
                panel.setVisibility(View.VISIBLE);
                panel.setAlpha(0f);
                panel.postOnAnimation(() -> startPanelEnterAnimation(panel));
                return true;
            }
        });
        panel.requestLayout();
    }

    private void startPanelEnterAnimation(View panel) {
        if (panel.getParent() == null || closingAnimatedPanels.contains(panel)) {
            finishAnimatedView(panel);
            return;
        }
        panel.animate().cancel();
        panel.setScaleX(1f);
        panel.setScaleY(1f);
        panel.animate()
                .alpha(1f)
                .setDuration(PANEL_ENTER_MS)
                .setInterpolator(UI_EASE_OUT)
                .withEndAction(() -> {
                    if (panel.getParent() != null && !closingAnimatedPanels.contains(panel)) {
                        panel.setAlpha(1f);
                        panel.setVisibility(View.VISIBLE);
                    }
                    finishAnimatedView(panel);
                })
                .start();
    }

    private void clearDismissedPanelAnimation(View panel) {
        panel.animate().cancel();
        panel.setAlpha(1f);
        panel.setVisibility(View.VISIBLE);
        finishAnimatedView(panel);
        closingAnimatedPanels.remove(panel);
    }

    private PanelOverlay showPanelOverlay(View panel, FrameLayout.LayoutParams panelParams, Runnable onDismiss) {
        if (overlayHost == null || panel == null) {
            if (onDismiss != null) {
                onDismiss.run();
            }
            return null;
        }
        FrameLayout scrim = new FrameLayout(this);
        scrim.setClickable(true);
        scrim.setFocusable(false);
        scrim.setBackgroundColor(HeimdallUi.isPearl(this) ? 0x8A3A4048 : 0xB8000000);
        scrim.setAlpha(0f);
        scrim.setVisibility(View.INVISIBLE);
        applySystemGestureExclusion(scrim);
        preparePanelForDeferredEnter(panel);
        scrim.addView(panel, panelParams);

        PanelOverlay overlay = new PanelOverlay(scrim, panel, onDismiss);
        panelOverlays.add(overlay);
        updateGameFocusProtection();
        overlayHost.addView(scrim, new FrameLayout.LayoutParams(-1, -1));
        animatePanelOverlayIn(overlay);
        return overlay;
    }

    private void animatePanelOverlayIn(PanelOverlay overlay) {
        if (overlay == null) {
            return;
        }
        View scrim = overlay.scrim;
        View panel = overlay.panel;
        scrim.animate().cancel();
        if (!shouldAnimateUi()) {
            scrim.setAlpha(1f);
            scrim.setVisibility(View.VISIBLE);
            panel.setAlpha(1f);
            panel.setVisibility(View.VISIBLE);
            return;
        }
        trackAnimatedView(scrim);
        ViewTreeObserver observer = scrim.getViewTreeObserver();
        observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                if (scrim.getViewTreeObserver().isAlive()) {
                    scrim.getViewTreeObserver().removeOnPreDrawListener(this);
                }
                if (scrim.getParent() == null
                        || !panelOverlays.contains(overlay)
                        || closingAnimatedPanels.contains(panel)) {
                    finishAnimatedView(scrim);
                    return true;
                }
                scrim.setVisibility(View.VISIBLE);
                scrim.setAlpha(0f);
                panel.setVisibility(View.VISIBLE);
                panel.setAlpha(0f);
                scrim.postOnAnimation(() -> {
                    if (scrim.getParent() == null
                            || !panelOverlays.contains(overlay)
                            || closingAnimatedPanels.contains(panel)) {
                        finishAnimatedView(scrim);
                        return;
                    }
                    scrim.animate()
                            .alpha(1f)
                            .setDuration(PANEL_ENTER_MS)
                            .setInterpolator(UI_EASE_OUT)
                            .withEndAction(() -> finishAnimatedView(scrim))
                            .start();
                    startPanelEnterAnimation(panel);
                });
                return true;
            }
        });
        scrim.requestLayout();
    }

    private boolean dismissTopPanelOverlay() {
        if (panelOverlays.isEmpty()) {
            return false;
        }
        dismissPanelAnimated(panelOverlays.get(panelOverlays.size() - 1));
        return true;
    }

    private void dismissPanelAnimated(PanelOverlay overlay) {
        dismissPanelAnimated(overlay, null);
    }

    private void dismissPanelAnimated(PanelOverlay overlay, Runnable afterDismiss) {
        if (overlay == null || !panelOverlays.contains(overlay) || closingAnimatedPanels.contains(overlay.panel)) {
            return;
        }
        if (thorTextInputFocusLease.isActiveInputInside(overlay.panel)
                && releaseTextInputFocusThen(() -> dismissPanelAnimated(overlay, afterDismiss))) {
            return;
        }
        if (!shouldAnimateUi()) {
            removePanelOverlay(overlay, afterDismiss);
            return;
        }
        closingAnimatedPanels.add(overlay.panel);
        trackAnimatedView(overlay.scrim);
        trackAnimatedView(overlay.panel);
        overlay.scrim.animate().cancel();
        overlay.panel.animate().cancel();
        overlay.scrim.setVisibility(View.VISIBLE);
        overlay.panel.setVisibility(View.VISIBLE);
        overlay.scrim.animate()
                .alpha(0f)
                .setDuration(PANEL_EXIT_MS)
                .setInterpolator(UI_EASE_OUT)
                .start();
        overlay.panel.animate()
                .alpha(0f)
                .setDuration(PANEL_EXIT_MS)
                .setInterpolator(UI_EASE_OUT)
                .withEndAction(() -> removePanelOverlay(overlay, afterDismiss))
                .start();
    }

    private void removePanelOverlay(PanelOverlay overlay, Runnable afterDismiss) {
        if (overlay == null) {
            return;
        }
        overlay.scrim.animate().cancel();
        overlay.panel.animate().cancel();
        finishAnimatedView(overlay.scrim);
        finishAnimatedView(overlay.panel);
        closingAnimatedPanels.remove(overlay.panel);
        panelOverlays.remove(overlay);
        updateGameFocusProtection();
        if (overlay.scrim.getParent() instanceof ViewGroup) {
            ((ViewGroup) overlay.scrim.getParent()).removeView(overlay.scrim);
        }
        if (overlay.onDismiss != null) {
            overlay.onDismiss.run();
        }
        if (afterDismiss != null) {
            afterDismiss.run();
        }
    }

    private void clearPanelOverlays() {
        for (PanelOverlay overlay : new ArrayList<>(panelOverlays)) {
            removePanelOverlay(overlay, null);
        }
        panelOverlays.clear();
    }

    private void updateMacroIconPreview(ImageView preview, Macro macro, String iconKey) {
        MacroIconRepository.MacroIconOption option = iconKey == null || iconKey.trim().length() == 0
                ? MacroIconRepository.defaultOption(this)
                : MacroIconRepository.findByKey(this, iconKey);
        if (option == null) {
            option = MacroIconRepository.defaultOption(this);
        }
        Drawable drawable = option.load(this);
        if (drawable == null) {
            drawable = MacroIconRepository.defaultOption(this).load(this);
            option = MacroIconRepository.defaultOption(this);
        }
        if (drawable != null) {
            drawable = drawable.mutate();
            if (option.tintable) {
                drawable.setTint(HeimdallUi.textColor(this));
            } else {
                drawable.clearColorFilter();
            }
        }
        preview.setImageDrawable(drawable);
    }
    private void showMacroIconPicker(Macro macro, String[] draftIconKey, ImageView iconPreview) {
        List<MacroIconRepository.MacroIconOption> builtInOptions =
                MacroIconRepository.builtInOptions(this);
        List<MacroIconRepository.MacroIconOption> customOptions = MacroIconRepository.customOptions(this);

        LinearLayout pickerShell = new LinearLayout(this);
        pickerShell.setOrientation(LinearLayout.VERTICAL);
        pickerShell.setPadding(dp(14), dp(12), dp(14), dp(12));
        pickerShell.setBackground(HeimdallUi.isPearl(this)
                ? HeimdallUi.cncRaised(this, 12, false, false)
                : HeimdallUi.glass(this, 0xF00B111B, 0xFA070A10,
                        0x884EA1FF, 0x44344150, 12, 1));

        TextView title = text(getString(R.string.macro_icon_picker_title),
                HeimdallUi.TYPE_PAGE_TITLE, TEXT, true);
        pickerShell.addView(title, new LinearLayout.LayoutParams(-1, dp(42)));

        final PanelOverlay[] overlayHolder = new PanelOverlay[1];
        Button importButton = editorButton(getString(R.string.macro_icon_import), () -> {
            if (overlayHolder[0] != null) {
                dismissPanelAnimated(overlayHolder[0],
                        () -> chooseMacroIconFile(macro, draftIconKey, iconPreview));
            } else {
                chooseMacroIconFile(macro, draftIconKey, iconPreview);
            }
        });
        HeimdallUi.applySecondaryButton(this, importButton);
        LinearLayout.LayoutParams importParams = new LinearLayout.LayoutParams(-1, dp(46));
        importParams.setMargins(0, 0, 0, dp(8));
        pickerShell.addView(importButton, importParams);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setVerticalScrollBarEnabled(true);
        scroll.setScrollbarFadingEnabled(false);
        scroll.setSmoothScrollingEnabled(true);
        scroll.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);
        pickerShell.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(0, 0, 0, dp(8));
        scroll.addView(panel, new ScrollView.LayoutParams(-1, -2));

        TextView builtInLabel = text(getString(R.string.macro_icon_builtin),
                HeimdallUi.TYPE_LABEL, MUTED, true);
        panel.addView(builtInLabel, new LinearLayout.LayoutParams(-1, dp(24)));

        LinearLayout builtInList = new LinearLayout(this);
        builtInList.setOrientation(LinearLayout.VERTICAL);
        panel.addView(builtInList, new LinearLayout.LayoutParams(-1, -2));

        if (builtInOptions.isEmpty() && customOptions.isEmpty()) {
            TextView empty = text(getString(R.string.macro_icon_empty), 12, MUTED, false);
            panel.addView(empty, new LinearLayout.LayoutParams(-1, dp(48)));
        }

        LinearLayout customList = null;
        if (!customOptions.isEmpty()) {
        TextView customLabel = text(getString(R.string.macro_icon_custom),
                HeimdallUi.TYPE_LABEL, MUTED, true);
            LinearLayout.LayoutParams customLabelParams = new LinearLayout.LayoutParams(-1, dp(28));
            customLabelParams.setMargins(0, dp(8), 0, 0);
            panel.addView(customLabel, customLabelParams);
            customList = new LinearLayout(this);
            customList.setOrientation(LinearLayout.VERTICAL);
            panel.addView(customList, new LinearLayout.LayoutParams(-1, -2));
        }

        addMacroIconRows(builtInList, builtInOptions, draftIconKey, iconPreview,
                macro, overlayHolder);
        if (customList != null) {
            addMacroIconRows(customList, customOptions, draftIconKey, iconPreview,
                    macro, overlayHolder);
        }

        View footerDivider = new View(this);
        footerDivider.setBackgroundColor(HeimdallUi.isPearl(this) ? 0x287B8792 : 0x445F7C9A);
        pickerShell.addView(footerDivider, new LinearLayout.LayoutParams(-1, dp(1)));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setPadding(0, dp(4), 0, 0);
        pickerShell.addView(actions, new LinearLayout.LayoutParams(-1, dp(58)));
        actions.addView(editorButton(getString(R.string.common_close), () -> {
            if (overlayHolder[0] != null) {
                dismissPanelAnimated(overlayHolder[0]);
            }
        }));

        DisplayMetrics metrics = getResources().getDisplayMetrics();
        FrameLayout.LayoutParams pickerParams = new FrameLayout.LayoutParams(
                Math.min(dp(620), metrics.widthPixels - dp(48)),
                Math.min(dp(760), metrics.heightPixels - dp(96)),
                Gravity.CENTER);
        overlayHolder[0] = showPanelOverlay(pickerShell, pickerParams, null);
    }

    private void chooseMacroIconFile(Macro macro, String[] draftIconKey, ImageView iconPreview) {
        if (releaseTextInputFocusThen(
                () -> chooseMacroIconFile(macro, draftIconKey, iconPreview))) {
            return;
        }
        pendingMacroIconMacro = macro;
        pendingMacroIconKey = draftIconKey;
        pendingMacroIconPreview = iconPreview;
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/png", "image/webp"});
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivityForResult(intent, REQUEST_MACRO_ICON);
        } catch (Exception ignored) {
            pendingMacroIconMacro = null;
            pendingMacroIconKey = null;
            pendingMacroIconPreview = null;
            showErrorAction(getString(R.string.error_open_image_picker));
        }
    }

    private void addMacroIconRows(LinearLayout list,
                                  List<MacroIconRepository.MacroIconOption> options,
                                  String[] draftIconKey,
                                  ImageView iconPreview,
                                  Macro macro,
                                  PanelOverlay[] overlayHolder) {
        final int columns = 3;
        for (int start = 0; start < options.size(); start += columns) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            list.addView(row, new LinearLayout.LayoutParams(-1, dp(114)));
            for (int column = 0; column < columns; column++) {
                int index = start + column;
                if (index >= options.size()) {
                    row.addView(new View(this), new LinearLayout.LayoutParams(0, -1, 1));
                    continue;
                }
                MacroIconRepository.MacroIconOption option = options.get(index);
                View cell = macroIconPickerCell(option, draftIconKey[0], () -> {
                    draftIconKey[0] = option.key;
                    updateMacroIconPreview(iconPreview, macro, draftIconKey[0]);
                    if (overlayHolder[0] != null) {
                        dismissPanelAnimated(overlayHolder[0]);
                    }
                });
                LinearLayout.LayoutParams cellParams = new LinearLayout.LayoutParams(0, -1, 1);
                cellParams.setMargins(dp(4), dp(4), dp(4), dp(4));
                row.addView(cell, cellParams);
            }
        }
    }

    private View macroIconPickerCell(MacroIconRepository.MacroIconOption option, String selectedKey, Runnable action) {
        String effectiveSelectedKey = selectedKey == null || selectedKey.trim().length() == 0
                ? MacroIconRepository.defaultOption(this).key : selectedKey;
        boolean selected = sameIconKey(option.key, effectiveSelectedKey);
        FrameLayout frame = new FrameLayout(this);
        frame.setBackground(HeimdallUi.isPearl(this)
                ? HeimdallUi.cncRaised(this, 10, selected, false)
                : HeimdallUi.glass(this,
                        selected ? 0xC3111A28 : 0xB00E1624,
                        selected ? 0xD7080D16 : 0xC2070A10,
                        selected ? 0xCC62C6FF : 0x665F7C9A,
                        selected ? 0x884EA1FF : 0x22344150,
                        10, selected ? 2 : 1));
        frame.setPadding(dp(6), dp(6), dp(6), dp(6));
        frame.setOnClickListener(v -> action.run());
        frame.setContentDescription(option.displayName);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER);
        frame.addView(content, new FrameLayout.LayoutParams(-1, -1));

        ImageView image = new ImageView(this);
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        Drawable drawable = option.load(this);
        if (drawable != null) {
            drawable = drawable.mutate();
            if (option.tintable) {
                drawable.setTint(HeimdallUi.textColor(this));
            } else {
                drawable.clearColorFilter();
            }
        }
        image.setImageDrawable(drawable);
        content.addView(image, new LinearLayout.LayoutParams(dp(46), dp(46)));

        TextView label = text(option.displayName, HeimdallUi.TYPE_META, selected ? TEXT : MUTED, selected);
        label.setGravity(Gravity.CENTER);
        label.setSingleLine(true);
        content.addView(label, new LinearLayout.LayoutParams(-1, dp(26)));

        if (selected) {
            TextView check = text("\u2713", 12, TEXT, true);
            check.setGravity(Gravity.CENTER);
            check.setBackground(HeimdallUi.isPearl(this)
                    ? HeimdallUi.cncRaised(this, 8, true, false)
                    : HeimdallUi.glass(this, 0xD0142740, 0xE609111D,
                            0xCC70B7FF, 0x664EA1FF, 8, 1));
            FrameLayout.LayoutParams checkParams = new FrameLayout.LayoutParams(dp(20), dp(20),
                    Gravity.TOP | Gravity.RIGHT);
            frame.addView(check, checkParams);
        }

        return frame;
    }

    private boolean sameIconKey(String left, String right) {
        String l = left == null ? "" : left.trim();
        String r = right == null ? "" : right.trim();
        return l.equals(r);
    }

    private void startCapture(List<MacroStep> draftSteps, LinearLayout stepsList, String mode) {
        activeDraftSteps = draftSteps;
        activeStepsList = stepsList;
        captureInProgress = true;
        Intent intent = CoordinateCaptureActivity.createIntent(this, mode);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK
                | Intent.FLAG_ACTIVITY_NO_ANIMATION);
        Display targetDisplay = findCaptureDisplay();
        if (targetDisplay != null) {
            ActivityOptions options = ActivityOptions.makeBasic();
            options.setLaunchDisplayId(targetDisplay.getDisplayId());
            showAction(getString(R.string.capture_upper_display,
                    targetDisplay.getDisplayId()));
            startActivity(intent, options.toBundle());
        } else {
            showAction(getString(R.string.capture_current_display_fallback));
            startActivity(intent);
        }
    }

    private Display findCaptureDisplay() {
        DisplayManager manager = (DisplayManager) getSystemService(DISPLAY_SERVICE);
        if (manager == null) {
            return null;
        }
        // Thor's upper panel is the Android default display. Do not infer it by excluding
        // the Activity display: that deprecated value can change after MediaProjection UI.
        Display upper = manager.getDisplay(Display.DEFAULT_DISPLAY);
        if (upper != null && upper.isValid()) {
            return upper;
        }
        Display current = getWindow().getDecorView().getDisplay();
        int currentId = current == null ? Display.DEFAULT_DISPLAY : current.getDisplayId();
        Display[] displays = manager.getDisplays();
        Display best = null;
        int bestArea = -1;
        DisplayMetrics metrics = new DisplayMetrics();
        for (Display display : displays) {
            if (display.getDisplayId() == currentId) {
                continue;
            }
            display.getRealMetrics(metrics);
            int area = metrics.widthPixels * metrics.heightPixels;
            if (area > bestArea) {
                best = display;
                bestArea = area;
            }
        }
        return best;
    }

    private void updateTargetDisplayInfo() {
        Display display = null;
        DisplayManager manager = (DisplayManager) getSystemService(DISPLAY_SERVICE);
        if (manager != null) {
            display = manager.getDisplay(Display.DEFAULT_DISPLAY);
        }
        if (display == null) {
            display = findCaptureDisplay();
        }
        if (display == null) {
            display = getWindowManager().getDefaultDisplay();
        }
        if (display == null) {
            targetDisplayId = Display.DEFAULT_DISPLAY;
            targetDisplayWidth = 0;
            targetDisplayHeight = 0;
            targetDisplayDensityDpi = getResources().getDisplayMetrics().densityDpi;
            return;
        }
        DisplayMetrics metrics = new DisplayMetrics();
        display.getRealMetrics(metrics);
        targetDisplayId = display.getDisplayId();
        int width = metrics.widthPixels;
        int height = metrics.heightPixels;
        try {
            Display.Mode mode = display.getMode();
            if (mode != null && mode.getPhysicalWidth() > 0 && mode.getPhysicalHeight() > 0) {
                int physicalWidth = mode.getPhysicalWidth();
                int physicalHeight = mode.getPhysicalHeight();
                boolean metricsLandscape = metrics.widthPixels >= metrics.heightPixels;
                boolean modeLandscape = physicalWidth >= physicalHeight;
                if (metricsLandscape != modeLandscape) {
                    int swap = physicalWidth;
                    physicalWidth = physicalHeight;
                    physicalHeight = swap;
                }
                width = physicalWidth;
                height = physicalHeight;
            }
        } catch (Throwable ignored) {
        }
        targetDisplayWidth = width;
        targetDisplayHeight = height;
        targetDisplayDensityDpi = metrics.densityDpi;
    }

    private void addWaitStep(List<MacroStep> draftSteps, LinearLayout stepsList, EditText waitInput) {
        String raw = waitInput.getText().toString().trim().replace("ms", "");
        long ms;
        try {
            ms = Math.max(0L, Long.parseLong(raw));
        } catch (NumberFormatException ignored) {
            ms = 80L;
            waitInput.setText("80");
        }
        draftSteps.add(new MacroStep(MacroStep.TYPE_WAIT, ms + "ms"));
        renderStepList(draftSteps, stepsList);
    }

    private void showGamepadRecordingPanel(List<MacroStep> draftSteps,
            LinearLayout stepsList, int replaceIndex) {
        if (activeGamepadRecordingSession != null
                && activeGamepadRecordingSession.overlay != null) {
            dismissPanelAnimated(activeGamepadRecordingSession.overlay);
        }

        LinearLayout shell = new LinearLayout(this);
        shell.setOrientation(LinearLayout.VERTICAL);
        shell.setBackground(HeimdallUi.isPearl(this)
                ? HeimdallUi.cncFlush(this, 14)
                : HeimdallUi.glass(this, 0xFA0B111B, 0xFF070A10,
                        0x886A829C, 0x44344150, 14, 2));

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(18), 0, dp(18), 0);
        shell.addView(header, new LinearLayout.LayoutParams(-1, dp(66)));

        TextView headerTitle = text(getString(R.string.gamepad_record_title),
                HeimdallUi.TYPE_EDITOR_TITLE, TEXT, true);
        header.addView(headerTitle, new LinearLayout.LayoutParams(-1, dp(32)));
        TextView headerSubtitle = text(getString(R.string.gamepad_record_prepare),
                11, MUTED, false);
        header.addView(headerSubtitle, new LinearLayout.LayoutParams(-1, dp(22)));

        View headerDivider = new View(this);
        headerDivider.setBackgroundColor(HeimdallUi.isPearl(this) ? 0x287B8792 : 0x445F7C9A);
        shell.addView(headerDivider, new LinearLayout.LayoutParams(-1, dp(1)));

        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(18), dp(16), dp(18), dp(12));
        ScrollView bodyScroll = new ScrollView(this);
        bodyScroll.setFillViewport(true);
        bodyScroll.addView(body, new ScrollView.LayoutParams(-1, -2));
        shell.addView(bodyScroll, new LinearLayout.LayoutParams(-1, 0, 1));

        View footerDivider = new View(this);
        footerDivider.setBackgroundColor(HeimdallUi.isPearl(this) ? 0x287B8792 : 0x445F7C9A);
        shell.addView(footerDivider, new LinearLayout.LayoutParams(-1, dp(1)));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER_VERTICAL);
        actions.setPadding(dp(12), dp(4), dp(12), dp(4));
        shell.addView(actions, new LinearLayout.LayoutParams(-1, dp(62)));

        GamepadRecordingSession session = new GamepadRecordingSession(
                draftSteps, stepsList, replaceIndex, headerTitle, headerSubtitle, body, actions);
        activeGamepadRecordingSession = session;
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                settingsOverlayWidth(720), settingsOverlayHeight(520), Gravity.CENTER);
        params.setMargins(dp(12), dp(12), dp(12), dp(12));
        session.overlay = showPanelOverlay(shell, params, () -> endGamepadRecordingSession(session));
        if (session.overlay == null) {
            endGamepadRecordingSession(session);
            return;
        }
        renderGamepadRecordingPrepare(session);
    }

    private void renderGamepadRecordingPrepare(GamepadRecordingSession session) {
        if (!isActiveGamepadRecordingSession(session)) {
            return;
        }
        stopGamepadRecordingTicker(session);
        session.sequence = null;
        session.testing = false;
        session.device = NativeGamepadPath.resolveDevice();
        resetGamepadRecordingPanel(session, getString(R.string.gamepad_record_title),
                getString(R.string.gamepad_record_prepare));

        boolean ready = gamepadCaptureRouteReady(session.device);
        String deviceName = session.device == null ? ""
                : nonEmpty(session.device.name, "Controller");
        String title;
        String body;
        int semantic;
        if (session.device == null) {
            title = getString(R.string.gamepad_no_controller_title);
            body = getString(R.string.gamepad_no_controller_body);
            semantic = HeimdallUi.SEMANTIC_WARNING;
        } else if (InputBridge.BACKEND_SHIZUKU.equals(InputBridge.selectedBackendId(this))) {
            if (ready) {
            title = getString(R.string.gamepad_enhancement_ready_title, deviceName);
            body = getString(R.string.gamepad_record_ready_body);
                semantic = HeimdallUi.SEMANTIC_SUCCESS;
            } else {
            title = getString(R.string.gamepad_enhancement_not_ready_title);
            body = getString(R.string.gamepad_enhancement_not_ready_body, deviceName);
                semantic = HeimdallUi.SEMANTIC_WARNING;
            }
        } else if (ready) {
            title = getString(R.string.gamepad_direct_ready_title, deviceName);
            body = getString(R.string.gamepad_record_ready_body);
            semantic = HeimdallUi.SEMANTIC_SUCCESS;
        } else {
            title = getString(R.string.gamepad_channel_unavailable_title);
            body = getString(R.string.gamepad_channel_unavailable_body, deviceName);
            semantic = HeimdallUi.SEMANTIC_WARNING;
        }
        addGamepadStateCard(session.body, title, body, semantic);

        TextView hint = text(getString(R.string.gamepad_record_hint),
                11, MUTED, false);
        hint.setGravity(Gravity.TOP | Gravity.LEFT);
        LinearLayout.LayoutParams hintParams = new LinearLayout.LayoutParams(-1, -2);
        hintParams.setMargins(dp(2), dp(12), dp(2), 0);
        session.body.addView(hint, hintParams);

        addGamepadPanelAction(session, getString(R.string.common_cancel), false,
                () -> dismissGamepadRecordingPanel(session));
        if (ready) {
            addGamepadPanelAction(session, getString(R.string.gamepad_start_recording), true,
                    () -> startGamepadRecording(session));
        } else {
            addGamepadPanelAction(session, getString(R.string.gamepad_check_again), true,
                    () -> renderGamepadRecordingPrepare(session));
        }
    }

    private boolean gamepadCaptureRouteReady(NativeGamepadPath.Device device) {
        if (device == null) {
            return false;
        }
        if (InputBridge.BACKEND_SHIZUKU.equals(InputBridge.selectedBackendId(this))) {
            return ShizukuNativeController.isReady();
        }
        return device.readable;
    }

    private void startGamepadRecording(GamepadRecordingSession session) {
        if (!isActiveGamepadRecordingSession(session)) {
            return;
        }
        if (session.testing) {
            showAction(getString(R.string.gamepad_wait_for_test));
            return;
        }
        session.device = NativeGamepadPath.resolveDevice();
        if (!gamepadCaptureRouteReady(session.device)) {
            renderGamepadRecordingPrepare(session);
            return;
        }
        final String capturePath = session.device.path;
        session.startedAtMs = SystemClock.elapsedRealtime();
        session.sequence = null;
        renderGamepadRecordingActive(session);

        new Thread(() -> {
            String sequence;
            try {
                sequence = InputBridge.captureNativeGamepadSequence(this, capturePath, 4000);
            } catch (Throwable t) {
                sequence = t.getClass().getSimpleName() + ": "
                        + nonEmpty(t.getMessage(), "native controller record failed");
            }
            String finalSequence = sequence;
            runOnUiThread(() -> finishGamepadRecording(session, finalSequence));
        }, "native-controller-sequence-capture").start();
    }

    private void renderGamepadRecordingActive(GamepadRecordingSession session) {
        resetGamepadRecordingPanel(session, getString(R.string.gamepad_record_title),
                getString(R.string.gamepad_recording_title));
        LinearLayout card = addGamepadStateCard(session.body,
                getString(R.string.gamepad_recording_title),
                getString(R.string.gamepad_recording_body),
                HeimdallUi.SEMANTIC_RECORDING);
        TextView countdown = text(getString(R.string.gamepad_recording_countdown, 4f),
                24, TEXT, true);
        countdown.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
        LinearLayout.LayoutParams countdownParams = new LinearLayout.LayoutParams(-1, dp(48));
        countdownParams.setMargins(0, dp(4), 0, 0);
        card.addView(countdown, countdownParams);

        TextView hint = text(getString(R.string.gamepad_recording_hint),
                11, MUTED, false);
        LinearLayout.LayoutParams hintParams = new LinearLayout.LayoutParams(-1, -2);
        hintParams.setMargins(dp(2), dp(12), dp(2), 0);
        session.body.addView(hint, hintParams);

        addGamepadPanelAction(session, getString(R.string.gamepad_cancel_recording), false,
                () -> dismissGamepadRecordingPanel(session));

        session.ticker = new Runnable() {
            @Override
            public void run() {
                if (!isActiveGamepadRecordingSession(session) || session.cancelled) {
                    return;
                }
                long elapsed = SystemClock.elapsedRealtime() - session.startedAtMs;
                long remaining = Math.max(0L, 4000L - elapsed);
                countdown.setText(getString(R.string.gamepad_recording_countdown,
                        remaining / 1000f));
                if (remaining > 0L) {
                    uiHandler.postDelayed(this, 100L);
                }
            }
        };
        uiHandler.post(session.ticker);
    }

    private void finishGamepadRecording(GamepadRecordingSession session, String sequence) {
        if (!isActiveGamepadRecordingSession(session) || session.cancelled) {
            return;
        }
        stopGamepadRecordingTicker(session);
        GamepadSequencePolicy.Inspection inspection =
                GamepadSequencePolicy.inspect(sequence);
        if (inspection.hasUnreleasedSystemNavigationKey) {
            renderGamepadRecordingError(session,
                    getString(R.string.native_controller_replay_missing_release));
            return;
        }
        GamepadSequenceSummary summary = gamepadSummary(sequence);
        if (summary.valid && sequence != null && sequence.startsWith("seq:")) {
            session.sequence = sequence;
            renderGamepadRecordingResult(session, summary);
            return;
        }
        renderGamepadRecordingError(session, sequence);
    }

    private String gamepadSystemNavigationLabel(int scanCode) {
        if (scanCode == GamepadSequencePolicy.KEY_BACK) {
            return getString(R.string.gamepad_system_key_back);
        }
        if (scanCode == GamepadSequencePolicy.KEY_RECENT_APPS
                || scanCode == GamepadSequencePolicy.KEY_APPSELECT) {
            return getString(R.string.gamepad_system_key_recents);
        }
        return getString(R.string.gamepad_system_key_home);
    }

    private void renderGamepadRecordingResult(GamepadRecordingSession session,
            GamepadSequenceSummary summary) {
        resetGamepadRecordingPanel(session, getString(R.string.gamepad_record_complete),
                getString(R.string.gamepad_confirm_recording));
        LinearLayout resultCard = addGamepadStateCard(session.body,
                summary.title, summary.subtitle, HeimdallUi.SEMANTIC_SUCCESS);
        resultCard.setPadding(dp(16), dp(14), dp(16), dp(14));

        if (summary.hitEventLimit) {
            addGamepadStateCard(session.body,
                    getString(R.string.gamepad_many_inputs_title),
                    getString(R.string.gamepad_many_inputs_body),
                    HeimdallUi.SEMANTIC_WARNING);
        }

        GamepadSequencePolicy.Inspection inspection =
                GamepadSequencePolicy.inspect(session.sequence);
        if (inspection.containsSystemNavigationKey()) {
            addGamepadStateCard(session.body,
                    getString(R.string.gamepad_system_navigation_warning_title),
                    getString(R.string.gamepad_system_navigation_warning_body,
                            gamepadSystemNavigationLabel(
                                    inspection.systemNavigationScanCode)),
                    HeimdallUi.SEMANTIC_WARNING);
        }

        TextView testStatus = text(getString(R.string.gamepad_test_before_add),
                11, MUTED, false);
        LinearLayout.LayoutParams testParams = new LinearLayout.LayoutParams(-1, -2);
        testParams.setMargins(dp(2), dp(10), dp(2), 0);
        session.body.addView(testStatus, testParams);

        addGamepadPanelAction(session, getString(R.string.common_cancel), false,
                () -> dismissGamepadRecordingPanel(session));
        session.resultRetakeButton = addGamepadPanelAction(session,
                getString(R.string.gamepad_rerecord), false,
                () -> startGamepadRecording(session));
        session.resultTestButton = addGamepadPanelAction(session,
                getString(R.string.gamepad_test), false,
                () -> testGamepadSequence(session, testStatus));
        session.resultCommitButton = addGamepadPanelAction(session,
                getString(session.replaceIndex >= 0 ? R.string.gamepad_replace_step
                        : R.string.gamepad_add_to_macro),
                true, () -> addRecordedGamepadStep(session, summary));
    }

    private void renderGamepadRecordingError(GamepadRecordingSession session, String detail) {
        boolean noEvents = detail == null || detail.trim().length() == 0
                || "NO_EVENTS".equals(detail.trim());
        resetGamepadRecordingPanel(session,
                getString(noEvents ? R.string.gamepad_no_input_title
                        : R.string.gamepad_record_failed_title),
                getString(R.string.gamepad_retry_subtitle));
        String title = noEvents ? getString(R.string.gamepad_no_input_title)
                : friendlyGamepadCaptureErrorTitle(detail);
        String body = noEvents
                ? getString(R.string.gamepad_no_input_body)
                : getString(R.string.gamepad_record_failed_body);
        addGamepadStateCard(session.body, title, body,
                noEvents ? HeimdallUi.SEMANTIC_WARNING : HeimdallUi.SEMANTIC_ERROR);
        if (!noEvents) {
            addGamepadTechnicalDetails(session.body, detail);
        }
        addGamepadPanelAction(session, getString(R.string.common_cancel), false,
                () -> dismissGamepadRecordingPanel(session));
        addGamepadPanelAction(session, getString(R.string.gamepad_rerecord), true,
                () -> startGamepadRecording(session));
    }

    private String friendlyGamepadCaptureErrorTitle(String detail) {
        String lower = detail == null ? "" : detail.toLowerCase(Locale.US);
        if (lower.contains("shizuku") || lower.contains("user service")) {
            return getString(R.string.gamepad_error_connection_lost);
        }
        if (lower.contains("not found") || lower.contains("\u672a\u627e\u5230")) {
            return getString(R.string.gamepad_error_no_controller);
        }
        if (lower.contains("permission") || lower.contains("\u6743\u9650")
                || lower.contains("not readable") || lower.contains("\u4e0d\u53ef\u8bfb")) {
            return getString(R.string.gamepad_error_channel_unavailable);
        }
        return getString(R.string.gamepad_error_generic);
    }

    private void resetGamepadRecordingPanel(GamepadRecordingSession session,
            String title, String subtitle) {
        session.headerTitle.setText(title);
        session.headerSubtitle.setText(subtitle);
        session.body.removeAllViews();
        session.actions.removeAllViews();
        session.resultRetakeButton = null;
        session.resultTestButton = null;
        session.resultCommitButton = null;
    }

    private LinearLayout addGamepadStateCard(LinearLayout parent, String title,
            String message, int semantic) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        HeimdallUi.applySemanticPanel(this, card, semantic);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(0, 0, 0, dp(8));
        parent.addView(card, params);

        TextView titleView = text(title, 18, TEXT, true);
        titleView.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
        if (semantic == HeimdallUi.SEMANTIC_RECORDING) {
            titleView.setTextColor(0xFFFF6B7A);
        }
        card.addView(titleView, new LinearLayout.LayoutParams(-1, -2));
        if (message != null && message.length() > 0) {
            TextView messageView = text(message, 12, MUTED, false);
            messageView.setGravity(Gravity.TOP | Gravity.LEFT);
            messageView.setLineSpacing(0f, 1.12f);
            LinearLayout.LayoutParams messageParams = new LinearLayout.LayoutParams(-1, -2);
            messageParams.setMargins(0, dp(6), 0, 0);
            card.addView(messageView, messageParams);
        }
        return card;
    }

    private void addGamepadTechnicalDetails(LinearLayout parent, String detail) {
        TextView detailView = text(detail, 10, MUTED, false);
        detailView.setVisibility(View.GONE);
        detailView.setTextIsSelectable(true);
        detailView.setPadding(dp(10), dp(8), dp(10), dp(8));
        detailView.setBackground(HeimdallUi.fieldPanel(this, 8));
        TextView toggle = text(getString(R.string.gamepad_view_technical_details),
                11, MUTED, true);
        toggle.setClickable(true);
        toggle.setFocusable(true);
        toggle.setPadding(dp(2), dp(6), dp(2), dp(6));
        toggle.setOnClickListener(v -> {
            boolean show = detailView.getVisibility() != View.VISIBLE;
            detailView.setVisibility(show ? View.VISIBLE : View.GONE);
            toggle.setText(show ? R.string.gamepad_collapse_technical_details
                    : R.string.gamepad_view_technical_details);
        });
        parent.addView(toggle, new LinearLayout.LayoutParams(-1, dp(34)));
        LinearLayout.LayoutParams detailParams = new LinearLayout.LayoutParams(-1, -2);
        detailParams.setMargins(0, 0, 0, dp(8));
        parent.addView(detailView, detailParams);
    }

    private Button addGamepadPanelAction(GamepadRecordingSession session,
            String label, boolean primary, Runnable action) {
        Button button = editorButton(label, action);
        if (primary) {
            HeimdallUi.applyPrimaryActionButton(this, button);
        }
        session.actions.addView(button);
        return button;
    }

    private void testGamepadSequence(GamepadRecordingSession session, TextView status) {
        if (!isActiveGamepadRecordingSession(session) || session.testing
                || session.sequence == null) {
            return;
        }
        if (shouldProtectThorMappingFromControllerMacros()) {
            status.setTextColor(HeimdallUi.COLOR_DANGER);
            status.setText(R.string.gamepad_test_enhanced_touch_blocked);
            showErrorAction(getString(R.string.macro_enhanced_touch_controller_blocked));
            return;
        }
        session.testing = true;
        setGamepadResultActionsEnabled(session, false);
        status.setTextColor(MUTED);
        status.setText(R.string.gamepad_testing_replay);
        new Thread(() -> {
            String result;
            try {
                result = NativeGamepadPath.userFacingError(this,
                        InputBridge.replayNativeGamepadSequence(this, session.sequence));
            } catch (Throwable t) {
                result = t.getClass().getSimpleName() + ": "
                        + nonEmpty(t.getMessage(), "native controller replay failed");
            }
            String finalResult = result;
            runOnUiThread(() -> {
                if (!isActiveGamepadRecordingSession(session)) {
                    return;
                }
                session.testing = false;
                setGamepadResultActionsEnabled(session, true);
                if (nativeGamepadReplaySucceeded(finalResult)) {
                    status.setTextColor(HeimdallUi.COLOR_SUCCESS);
                    status.setText(R.string.gamepad_test_complete);
                } else {
                    status.setTextColor(HeimdallUi.COLOR_DANGER);
                    status.setText(R.string.gamepad_test_failed);
                }
            });
        }, "gamepad-sequence-preview").start();
    }

    private void setGamepadResultActionsEnabled(GamepadRecordingSession session,
            boolean enabled) {
        setGamepadResultActionEnabled(session.resultRetakeButton, enabled);
        setGamepadResultActionEnabled(session.resultTestButton, enabled);
        setGamepadResultActionEnabled(session.resultCommitButton, enabled);
    }

    private void setGamepadResultActionEnabled(Button button, boolean enabled) {
        if (button == null) {
            return;
        }
        button.setEnabled(enabled);
        button.setAlpha(enabled ? 1f : 0.48f);
    }

    private void addRecordedGamepadStep(GamepadRecordingSession session,
            GamepadSequenceSummary summary) {
        if (!isActiveGamepadRecordingSession(session) || session.testing
                || session.sequence == null) {
            return;
        }
        MacroStep step = new MacroStep(MacroStep.TYPE_GAMEPAD, session.sequence);
        boolean replaced = session.replaceIndex >= 0
                && session.replaceIndex < session.draftSteps.size();
        if (replaced) {
            session.draftSteps.set(session.replaceIndex, step);
        } else {
            session.draftSteps.add(step);
        }
        renderStepList(session.draftSteps, session.stepsList);
        showAction(getString(replaced ? R.string.gamepad_step_replaced
                : R.string.gamepad_step_added, summary.title));
        dismissGamepadRecordingPanel(session);
    }

    private void dismissGamepadRecordingPanel(GamepadRecordingSession session) {
        if (session != null && session.overlay != null) {
            dismissPanelAnimated(session.overlay);
        }
    }

    private boolean isActiveGamepadRecordingSession(GamepadRecordingSession session) {
        return session != null && !session.cancelled
                && activeGamepadRecordingSession == session
                && session.overlay != null && panelOverlays.contains(session.overlay);
    }

    private void stopGamepadRecordingTicker(GamepadRecordingSession session) {
        if (session != null && session.ticker != null) {
            uiHandler.removeCallbacks(session.ticker);
            session.ticker = null;
        }
    }

    private void endGamepadRecordingSession(GamepadRecordingSession session) {
        if (session == null) {
            return;
        }
        session.cancelled = true;
        session.testing = false;
        stopGamepadRecordingTicker(session);
        if (activeGamepadRecordingSession == session) {
            activeGamepadRecordingSession = null;
        }
    }

    private GamepadSequenceSummary gamepadSummary(String sequence) {
        NativeGamepadPath.Device device = NativeGamepadPath.resolveDevice();
        int axisX = device == null ? -1 : device.rightStickAxisX;
        int axisY = device == null ? -1 : device.rightStickAxisY;
        return GamepadSequenceSummary.summarize(this, sequence, axisX, axisY);
    }

    private boolean nativeGamepadReplaySucceeded(String result) {
        String lower = result == null ? "" : result.toLowerCase(Locale.US);
        return lower.contains("sequence ok") || lower.contains("combo ok");
    }

    private void renderStepList(List<MacroStep> steps, LinearLayout stepsList) {
        updateStepListHeight(steps, stepsList);
        stepsList.removeAllViews();
        if (steps.isEmpty()) {
            TextView empty = text(getString(R.string.macro_steps_empty), 13, MUTED, false);
            empty.setGravity(Gravity.CENTER);
            stepsList.addView(empty, new LinearLayout.LayoutParams(-1, dp(44)));
            return;
        }
        for (int i = 0; i < steps.size(); i++) {
            final int index = i;
            MacroStep step = steps.get(i);
            if (MacroStep.TYPE_GAMEPAD.equals(step.type)
                    && step.value != null && step.value.startsWith("seq:")) {
                GamepadSequenceSummary summary = gamepadSummary(step.value);
                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(dp(4), dp(3), 0, dp(3));

                LinearLayout labels = new LinearLayout(this);
                labels.setOrientation(LinearLayout.VERTICAL);
                labels.setGravity(Gravity.CENTER_VERTICAL);
                labels.setPadding(0, 0, dp(6), 0);
                row.addView(labels, new LinearLayout.LayoutParams(0, -1, 1));

                TextView title = text((i + 1) + ". " + summary.title, 13, TEXT, true);
                title.setMaxLines(2);
                title.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
                labels.addView(title, new LinearLayout.LayoutParams(-1, dp(36)));
                TextView subtitle = text(summary.subtitle, 10, MUTED, false);
                subtitle.setMaxLines(2);
                labels.addView(subtitle, new LinearLayout.LayoutParams(-1, dp(36)));

                row.addView(gamepadStepAction(getString(R.string.gamepad_rerecord), () ->
                                showGamepadRecordingPanel(steps, stepsList, index)),
                        new LinearLayout.LayoutParams(dp(60), dp(48)));
                row.addView(gamepadStepAction(getString(R.string.gamepad_delete_short), () -> {
                    steps.remove(index);
                    renderStepList(steps, stepsList);
                }), new LinearLayout.LayoutParams(dp(60), dp(48)));
                stepsList.addView(row, new LinearLayout.LayoutParams(-1, dp(80)));
                if (i < steps.size() - 1) {
                    View divider = new View(this);
                    divider.setBackgroundColor(HeimdallUi.isPearl(this)
                            ? 0x207B8792 : 0x28445A72);
                    stepsList.addView(divider, new LinearLayout.LayoutParams(-1, dp(1)));
                }
                continue;
            }
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, dp(2), 0, dp(2));

            TextView label = text((i + 1) + ". " + stepDisplayText(steps.get(i)), 13, TEXT, false);
            row.addView(label, new LinearLayout.LayoutParams(0, dp(42), 1));

            Button delete = new Button(this);
            delete.setText(R.string.gamepad_delete_short);
            delete.setTextSize(12);
            delete.setAllCaps(false);
            HeimdallUi.applySecondaryButton(this, delete);
            delete.setOnClickListener(v -> {
                steps.remove(index);
                renderStepList(steps, stepsList);
            });
            row.addView(delete, new LinearLayout.LayoutParams(dp(58), dp(42)));
            stepsList.addView(row, new LinearLayout.LayoutParams(-1, dp(46)));
        }
    }

    private Button gamepadStepAction(String label, Runnable action) {
        Button button = actionButton(label, action);
        button.setTextSize(11);
        button.setPadding(dp(2), 0, dp(2), 0);
        HeimdallUi.applySecondaryButton(this, button);
        return button;
    }

    private String stepDisplayText(MacroStep step) {
        if (step == null) {
            return "";
        }
        if (MacroStep.TYPE_GAMEPAD.equals(step.type) && step.value != null && step.value.startsWith("seq:")) {
            GamepadSequenceSummary summary = gamepadSummary(step.value);
            return summary.title + "  \u00b7  " + summary.subtitle;
        }
        return step.toString();
    }

    private void updateStepListHeight(List<MacroStep> steps, LinearLayout stepsList) {
        if (stepsList == null || !(stepsList.getParent() instanceof ScrollView)) {
            return;
        }
        if (Boolean.TRUE.equals(stepsList.getTag())) {
            return;
        }
        ScrollView scrollView = (ScrollView) stepsList.getParent();
        ViewGroup.LayoutParams params = scrollView.getLayoutParams();
        if (params == null) {
            return;
        }
        params.height = stepListHeight(steps);
        scrollView.setLayoutParams(params);
    }

    private int stepListHeight(List<MacroStep> steps) {
        int count = steps == null ? 0 : steps.size();
        int visibleRows = Math.max(1, Math.min(4, count));
        if (count == 0) {
            return dp(54);
        }
        int heightDp = 8;
        for (int i = 0; i < visibleRows; i++) {
            MacroStep step = steps.get(i);
            boolean gamepadSequence = MacroStep.TYPE_GAMEPAD.equals(step.type)
                    && step.value != null && step.value.startsWith("seq:");
            heightDp += gamepadSequence ? 80 : 46;
            if (gamepadSequence && i < count - 1) {
                heightDp += 1;
            }
        }
        return dp(heightDp);
    }

    private void showAction(String message) {
        if (statusText != null) {
            statusText.setText(productStatusMessage(message));
        }
    }

    private void showErrorAction(String message) {
        String visible = message == null || message.trim().length() == 0
                ? getString(R.string.status_updated) : message.trim();
        if (statusText != null) {
            statusText.setText(productStatusMessage(visible));
        }
        Toast.makeText(this, visible, Toast.LENGTH_SHORT).show();
    }

    private void showDebugAction(String message) {
        if (statusText != null) {
            statusText.setText(productStatusMessage(message));
        }
    }

    private String productStatusMessage(String message) {
        if (message == null || message.trim().length() == 0) {
            return getString(R.string.status_updated);
        }
        String value = message.trim();
        if (value.length() > 48) {
            return value.substring(0, 45) + "...";
        }
        return value;
    }

    private LinearLayout panel() {
        LinearLayout view = new LinearLayout(this);
        HeimdallUi.applyModulePanel(this, view);
        return view;
    }

    private Button actionButton(String label, Runnable action) {
        return HeimdallUi.baseButton(this, label, action);
    }

    private Button toolButton(String label, Runnable action) {
        Button button = actionButton(label, action);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -1, 1);
        params.setMargins(dp(3), 0, dp(3), 0);
        button.setLayoutParams(params);
        return button;
    }

    private Button toolIconButton(String label, int iconRes, Runnable action) {
        Button button = actionButton(label, action);
        button.setTextSize(12);
        button.setGravity(Gravity.CENTER);
        button.setCompoundDrawablesWithIntrinsicBounds(iconRes, 0, 0, 0);
        button.setCompoundDrawablePadding(dp(5));
        button.setPadding(dp(8), 0, dp(6), 0);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -1, 1);
        params.setMargins(dp(2), 0, dp(2), 0);
        button.setLayoutParams(params);
        return button;
    }

    private Button navIconButton(String label, int iconRes, int screen, Runnable action) {
        DockNavButton button = new DockNavButton(this);
        button.setNavDestination(screen, iconRes);
        button.setText(label);
        button.setAllCaps(false);
        button.setOnClickListener(v -> action.run());
        boolean selected = activeScreen == screen;
        HeimdallUi.applyNavButton(this, button, selected);
        button.setNavIcon(iconRes, selected);
        dockNavButtons.add(button);
        return button;
    }

    private void setTopIcon(Button button, MacroIconRepository.MacroIconOption option,
                            int color, int sizePx, int paddingPx) {
        Drawable icon = option == null ? null : option.load(this);
        if (icon == null) {
            button.setCompoundDrawables(null, null, null, null);
            return;
        }
        icon = icon.mutate();
        if (option.tintable) {
            icon.setTint(color);
        } else {
            icon.clearColorFilter();
        }
        icon.setBounds(0, 0, sizePx, sizePx);
        button.setCompoundDrawables(null, icon, null, null);
        button.setCompoundDrawablePadding(paddingPx);
    }

    private void setLeftIcon(Button button, int iconRes, int color, int sizePx) {
        Drawable icon = getDrawable(iconRes);
        if (icon == null) {
            button.setCompoundDrawables(null, null, null, null);
            return;
        }
        icon = icon.mutate();
        icon.setTint(color);
        icon.setBounds(0, 0, sizePx, sizePx);
        button.setCompoundDrawables(icon, null, null, null);
    }

    private void setLeftIcon(Button button, int iconRes, ColorStateList colors, int sizePx) {
        Drawable icon = getDrawable(iconRes);
        if (icon == null) {
            button.setCompoundDrawables(null, null, null, null);
            return;
        }
        icon = icon.mutate();
        icon.setTintList(colors);
        icon.setBounds(0, 0, sizePx, sizePx);
        button.setCompoundDrawables(icon, null, null, null);
    }

    private Button editorButton(String label, Runnable action) {
        Button button = actionButton(label, action);
        HeimdallUi.applySecondaryButton(this, button);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -1, 1);
        params.setMargins(dp(3), dp(4), dp(3), dp(4));
        button.setLayoutParams(params);
        return button;
    }

    private Button pageActionButton(String label, int iconRes, Runnable action) {
        Button button = actionButton(label, action);
        HeimdallUi.applySecondaryButton(this, button);
        button.setTextSize(HeimdallUi.TYPE_BUTTON_COMPACT);
        button.setGravity(Gravity.CENTER);
        button.setCompoundDrawablePadding(dp(6));
        setLeftIcon(button, iconRes, HeimdallUi.textColor(this), dp(16));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -1, 1);
        params.setMargins(dp(3), dp(2), dp(3), dp(2));
        button.setLayoutParams(params);
        return button;
    }

    private LinearLayout inlineEditorCard(LinearLayout parent, String title) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(10), dp(8), dp(10), dp(8));
        card.setBackground(HeimdallUi.isPearl(this)
                ? HeimdallUi.pearlMenuPanel(this, 10)
                : HeimdallUi.insetPanel(this, 10));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(0, dp(6), 0, dp(10));
        parent.addView(card, params);

        TextView titleView = text(title, 13, PRIMARY, true);
        card.addView(titleView, new LinearLayout.LayoutParams(-1, dp(26)));
        return card;
    }

    private EditText inlineEditText(String value, String hint) {
        EditText input = settingsEditText(value);
        input.setHint(hint);
        input.setHintTextColor(HeimdallUi.mutedTextColor(this));
        return input;
    }

    private void styleCheckBox(CheckBox checkBox) {
        checkBox.setTextColor(HeimdallUi.textColor(this));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            int checked = HeimdallUi.accent(this);
            int unchecked = HeimdallUi.isPearl(this) ? 0xFF788693 : 0xFF7F91A6;
            checkBox.setButtonTintList(new ColorStateList(
                    new int[][]{
                            new int[]{android.R.attr.state_checked},
                            new int[]{}
                    },
                    new int[]{checked, unchecked}));
        }
    }

    private LinearLayout.LayoutParams blockParams(int heightDp, int topDp, int bottomDp) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(heightDp));
        params.setMargins(0, dp(topDp), 0, dp(bottomDp));
        return params;
    }

    private Button settingsMenuButton(String title, String subtitle, Runnable action) {
        Button button = actionButton(title + "\n" + subtitle, action);
        button.setTextSize(13);
        button.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
        button.setTextColor(HeimdallUi.textColor(this));
        HeimdallUi.applySecondaryButton(this, button);
        button.setPadding(dp(12), 0, dp(12), 0);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(58));
        params.setMargins(0, dp(5), 0, dp(5));
        button.setLayoutParams(params);
        return button;
    }

    private void dismissDialog(AlertDialog[] holder) {
        if (holder != null && holder.length > 0 && holder[0] != null && holder[0].isShowing()) {
            holder[0].dismiss();
        }
    }

    private Button gridCloseButton(Runnable action) {
        Button button = actionButton("\u00d7", action);
        button.setTextSize(16);
        button.setTextColor(HeimdallUi.textColor(this));
        button.setGravity(Gravity.CENTER);
        button.setIncludeFontPadding(false);
        HeimdallUi.applySecondaryButton(this, button);
        button.setPadding(0, 0, 0, 0);
        return button;
    }

    private Button compactButton(String label, Runnable action) {
        Button button = actionButton(label, action);
        button.setTextColor(HeimdallUi.textColor(this));
        button.setTextSize(11);
        HeimdallUi.applySecondaryButton(this, button);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(58), dp(48));
        params.setMargins(dp(3), dp(4), 0, dp(4));
        button.setLayoutParams(params);
        return button;
    }

    private LinearLayout listActionRow(String label, Runnable openAction, Runnable editAction) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(2), 0, dp(2));

        Button open = new Button(this);
        open.setAllCaps(false);
        open.setText(label);
        open.setTextSize(13);
        open.setTextColor(HeimdallUi.textColor(this));
        open.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
        open.setBackground(HeimdallUi.isPearl(this)
                ? HeimdallUi.pearlMenuControl(this, 8, false, false)
                : HeimdallUi.surfacePanel(this, 8));
        open.setMinHeight(0);
        open.setMinWidth(0);
        open.setPadding(dp(10), 0, dp(8), 0);
        open.setOnClickListener(v -> openAction.run());
        open.setOnLongClickListener(v -> {
            editAction.run();
            return true;
        });
        row.addView(open, new LinearLayout.LayoutParams(0, dp(52), 1));
        return row;
    }

    private ImageButton iconButton(int iconRes, String description, boolean danger, Runnable action) {
        ImageButton button = new ImageButton(this);
        button.setImageResource(iconRes);
        int iconColor = danger
                ? (HeimdallUi.isPearl(this) ? 0xFFB34A4F : DANGER)
                : HeimdallUi.textColor(this);
        button.setColorFilter(iconColor);
        if (HeimdallUi.isPearl(this)) {
            button.setBackground(HeimdallUi.pearlMenuControl(this, 8, false, false));
        } else {
            button.setBackground(danger
                    ? rounded(DANGER_BG, DANGER, 8)
                    : HeimdallUi.surfacePanel(this, 8));
        }
        button.setContentDescription(description);
        button.setScaleType(ImageButton.ScaleType.CENTER);
        button.setPadding(dp(16), dp(16), dp(16), dp(16));
        button.setMinimumWidth(dp(48));
        button.setMinimumHeight(dp(48));
        button.setOnClickListener(v -> action.run());
        return button;
    }

    private LinearLayout.LayoutParams iconButtonParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(48), dp(52));
        params.setMargins(dp(3), 0, 0, 0);
        return params;
    }

    private TextView text(String value, int sp, int color, boolean bold) {
        return HeimdallUi.text(this, value, sp, color, bold);
    }

    private int dp(int value) {
        return HeimdallUi.dp(this, value);
    }

    private GradientDrawable rounded(int color, int strokeColor, int radiusDp) {
        return HeimdallUi.rounded(this, color, strokeColor, radiusDp);
    }

    private final class TouchPadView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rect = new RectF();
        private final Path surfaceClip = new Path();
        private float x = -1;
        private float y = -1;
        private float lastX = -1;
        private float lastY = -1;
        private float originX = -1;
        private float originY = -1;
        private float currentStickX;
        private float currentStickY;
        private float shizukuSmoothedDx;
        private float shizukuSmoothedDy;
        private float drawnSurfaceRadius;
        private float surfacePressProgress;
        private long lastDispatchTime;
        private boolean relativeMouseErrorShown;
        private long touchDragGestureToken;
        private boolean touchDragStarted;
        private boolean touchDragAcceptingMoves;
        private boolean touchDragErrorShown;
        private InputBridge.Callback touchDragCallback;
        private boolean rightStickGestureStarted;
        private boolean rightStickGestureActive;
        private boolean rightStickErrorShown;
        private ValueAnimator surfacePressAnimator;
        private final Runnable relativeMouseNeutralReset = new Runnable() {
            @Override
            public void run() {
                emitRelativeMouseStick(0f, 0f);
                invalidate();
            }
        };

        TouchPadView(Activity activity) {
            super(activity);
            setBackground(HeimdallUi.isPearl(AssistantActivity.this)
                    ? null
                    : HeimdallUi.glass(AssistantActivity.this,
                            0x550D1420, 0x77070A10,
                            0x00000000, 0x00000000,
                            HeimdallUi.RADIUS_MODULE, 0));
            applySystemGestureExclusion(this);
            requestAdvancedInputPreparation();
        }

        private void requestAdvancedInputPreparation() {
            String mode = TouchpadSettings.normalizeMode(touchpadSettings.mode);
            boolean shizukuControllerMode = (TouchpadSettings.MODE_RELATIVE_MOUSE.equals(mode)
                    || TouchpadSettings.MODE_RIGHT_STICK.equals(mode))
                    && InputBridge.BACKEND_SHIZUKU.equals(
                            InputBridge.selectedBackendId(AssistantActivity.this));
            if ((TouchpadSettings.MODE_SHIZUKU_TOUCH.equals(mode) || shizukuControllerMode)
                    && ShizukuNativeController.isPermissionGranted()) {
                ShizukuNativeController.requestServiceBinding(AssistantActivity.this);
            }
        }

        private boolean shizukuControllerServicePrepared() {
            if (!InputBridge.BACKEND_SHIZUKU.equals(
                    InputBridge.selectedBackendId(AssistantActivity.this))) {
                return true;
            }
            if (ShizukuNativeController.isServiceBound()) {
                return true;
            }
            boolean bindingRequested = ShizukuNativeController.requestServiceBinding(
                    AssistantActivity.this);
            if (bindingRequested && ShizukuNativeController.isServiceBinding()) {
                showAction(getString(R.string.shizuku_controller_preparing));
            } else if (!ShizukuNativeController.isServiceBound()) {
                showErrorAction(getString(R.string.shizuku_controller_route_required));
            }
            return ShizukuNativeController.isServiceBound();
        }

        @Override
        public void invalidate() {
            DebugPerformanceDiagnostics.countInvalidate("Touch surface");
            super.invalidate();
        }

        @Override
        public void requestLayout() {
            DebugPerformanceDiagnostics.countRequestLayout("Touch surface");
            super.requestLayout();
        }

        void recenterFromActivity() {
            String mode = TouchpadSettings.normalizeMode(touchpadSettings.mode);
            if (TouchpadSettings.MODE_RELATIVE_MOUSE.equals(mode)) {
                cancelRelativeMousePulse(true);
            } else if (TouchpadSettings.MODE_RIGHT_STICK.equals(mode)) {
                recenterRightStick(false);
            } else {
                cancelTouchDragGesture(mode);
            }
            rightStickGestureStarted = false;
            rightStickGestureActive = false;
            currentStickX = 0f;
            currentStickY = 0f;
            x = -1;
            y = -1;
            lastX = -1;
            lastY = -1;
            originX = -1;
            originY = -1;
            shizukuSmoothedDx = 0f;
            shizukuSmoothedDy = 0f;
            resetSurfacePressFeedback();
            invalidate();
        }

        private void setSurfacePressed(boolean pressed) {
            float target = pressed ? 1f : 0f;
            if (surfacePressAnimator != null) {
                surfacePressAnimator.cancel();
                surfacePressAnimator = null;
            }
            if (!shouldAnimateUi() || Math.abs(surfacePressProgress - target) < 0.001f) {
                surfacePressProgress = target;
                invalidate();
                return;
            }
            ValueAnimator animator = ValueAnimator.ofFloat(surfacePressProgress, target);
            surfacePressAnimator = animator;
            animator.setDuration(pressed
                    ? TOUCH_SURFACE_PRESS_IN_MS : TOUCH_SURFACE_PRESS_OUT_MS);
            animator.setInterpolator(UI_EASE_OUT);
            animator.addUpdateListener(valueAnimator -> {
                surfacePressProgress = (float) valueAnimator.getAnimatedValue();
                invalidate();
            });
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    if (surfacePressAnimator == animation) {
                        surfacePressAnimator = null;
                    }
                }
            });
            animator.start();
        }

        private void resetSurfacePressFeedback() {
            if (surfacePressAnimator != null) {
                surfacePressAnimator.cancel();
                surfacePressAnimator = null;
            }
            surfacePressProgress = 0f;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            DebugPerformanceDiagnostics.countDraw("Touch surface");
            super.onDraw(canvas);
            drawnSurfaceRadius = dp(HeimdallUi.INPUT_SURFACE_RADIUS);
            int surfaceInset = dp(HeimdallUi.INPUT_SURFACE_INSET);
            rect.set(surfaceInset, surfaceInset,
                    getWidth() - surfaceInset, getHeight() - surfaceInset);
            String mode = TouchpadSettings.normalizeMode(touchpadSettings.mode);
            boolean rightStickMode = TouchpadSettings.MODE_RIGHT_STICK.equals(mode);
            boolean shizukuTouchMode = TouchpadSettings.MODE_SHIZUKU_TOUCH.equals(mode);
            boolean relativeMouseMode =
                    TouchpadSettings.MODE_RELATIVE_MOUSE.equals(mode);

            if (DebugPerformanceDiagnostics.isFlatUi()) {
                drawFlatControlSurface(canvas, rightStickMode);
                return;
            }

            drawControlSurface(canvas, rightStickMode, shizukuTouchMode);

            if (rightStickMode) {
                drawRightStick(canvas);
            } else if (relativeMouseMode) {
                drawPrecisionAimReticle(canvas);
            } else {
                drawTouchPadDot(canvas);
            }
        }

        private void drawFlatControlSurface(Canvas canvas, boolean rightStickMode) {
            paint.setShader(null);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(0xFF202A34);
            canvas.drawRect(rect, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(1));
            paint.setColor(0xFF6E7E8E);
            canvas.drawRect(rect, paint);
            float centerX = rightStickMode ? stickCenterX() : rect.centerX();
            float centerY = rightStickMode ? stickCenterY() : rect.centerY();
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(0xFF9AA8B8);
            canvas.drawCircle(centerX, centerY, dp(4), paint);
            if (x >= 0f && y >= 0f) {
                paint.setColor(HeimdallUi.accent(AssistantActivity.this));
                canvas.drawCircle(x, y, dp(rightStickMode ? 22 : 12), paint);
            }
        }

        private void drawPrecisionAimReticle(Canvas canvas) {
            float centerX = rect.centerX();
            float centerY = rect.centerY();
            float radius = dp(24);
            float gap = dp(7);
            float arm = dp(34);
            paint.setShader(null);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeWidth(dp(x >= 0 ? 2 : 1));
            boolean pearl = HeimdallUi.isPearl(AssistantActivity.this);
            paint.setColor(pearl
                    ? (x >= 0 ? 0xFFF08A2A : 0xCCF7F9FB)
                    : (x >= 0 ? 0xEE70B7FF : 0x996A9DDB));
            canvas.drawCircle(centerX, centerY, radius, paint);
            paint.setColor(pearl
                    ? (x >= 0 ? 0xFFFFB05C : 0xAAF7F9FB)
                    : (x >= 0 ? 0xCC9ED0FF : 0x665A8DFF));
            canvas.drawCircle(centerX, centerY, dp(5), paint);
            canvas.drawLine(centerX - arm, centerY, centerX - gap, centerY, paint);
            canvas.drawLine(centerX + gap, centerY, centerX + arm, centerY, paint);
            canvas.drawLine(centerX, centerY - arm, centerX, centerY - gap, paint);
            canvas.drawLine(centerX, centerY + gap, centerX, centerY + arm, paint);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(pearl ? 0xFFF08A2A : 0xE6E6EDF3);
            canvas.drawCircle(centerX, centerY, dp(2), paint);
            if (x >= 0 && y >= 0) {
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(dp(1));
                paint.setColor(pearl ? 0x99F08A2A : 0x9970B7FF);
                canvas.drawCircle(x, y, dp(12), paint);
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(pearl ? 0xCCF08A2A : 0xCC70B7FF);
                canvas.drawCircle(x, y, dp(3), paint);
            }
            paint.setStrokeCap(Paint.Cap.BUTT);
        }

        private void drawControlSurface(Canvas canvas, boolean rightStickMode, boolean shizukuTouchMode) {
            float pressProgress = clampFloat(surfacePressProgress, 0f, 1f);
            boolean pearl = HeimdallUi.isPearl(AssistantActivity.this);
            if (pearl) {
                drawPearlInputFrame(canvas);
            }
            int idleTop = pearl ? 0xFF717B81
                    : (rightStickMode ? 0x34243D5B
                            : (shizukuTouchMode ? 0x32234640 : 0x34243D5B));
            int activeTop = pearl ? 0xFF7C868C
                    : (rightStickMode ? 0x4A265C94 : 0x4A245A78);
            int idleBottom = pearl ? 0xFF566168 : 0x8A070A10;
            int activeBottom = pearl ? 0xFF626C72 : 0xA3070A10;
            int top = blendColor(idleTop, activeTop, pressProgress);
            int bottom = blendColor(idleBottom, activeBottom, pressProgress);
            float radius = surfaceRadius();
            paint.setStyle(Paint.Style.FILL);
            paint.setShader(new LinearGradient(0, rect.top, 0, rect.bottom, top, bottom, Shader.TileMode.CLAMP));
            canvas.drawRoundRect(rect, radius, radius, paint);
            paint.setShader(null);

            drawSurfaceDepth(canvas, pressProgress);
            drawControlTexture(canvas, rightStickMode, shizukuTouchMode);
            drawSurfaceGlow(canvas, pressProgress);
            drawFocusCorners(canvas, pressProgress);
        }

        private void drawPearlInputFrame(Canvas canvas) {
            float outerRadius = dp(HeimdallUi.RADIUS_MODULE);
            RectF shadow = new RectF(rect);
            shadow.inset(dp(1) / 2f, dp(1) / 2f);
            shadow.offset(0f, dp(1));
            paint.setStyle(Paint.Style.FILL);
            paint.setShader(null);
            paint.setColor(0x32000000);
            canvas.drawRoundRect(shadow, outerRadius, outerRadius, paint);

            RectF shell = new RectF(rect);
            float shellInset = dp(3) / 4f;
            shell.inset(shellInset, shellInset);
            float shellRadius = Math.max(0f, outerRadius - shellInset);
            paint.setShader(new LinearGradient(shell.left, shell.top, shell.right, shell.bottom,
                    new int[]{0xFFDCE1E4, 0xFF929CA3, 0xFF56616A},
                    new float[]{0f, 0.50f, 1f}, Shader.TileMode.CLAMP));
            canvas.drawRoundRect(shell, shellRadius, shellRadius, paint);

            RectF rim = new RectF(shell);
            float rimInset = dp(2);
            rim.inset(rimInset, rimInset);
            float rimRadius = Math.max(0f, shellRadius - rimInset);
            paint.setShader(new LinearGradient(rim.left, rim.top, rim.right, rim.bottom,
                    new int[]{0xFFFFFFFF, 0xFFFFFFFF, 0xFFFDFDFC, 0xFFD9DEE1},
                    new float[]{0f, 0.58f, 0.84f, 1f}, Shader.TileMode.CLAMP));
            canvas.drawRoundRect(rim, rimRadius, rimRadius, paint);

            RectF separator = new RectF(rim);
            float separatorInset = dp(3);
            separator.inset(separatorInset, separatorInset);
            float separatorRadius = Math.max(0f, rimRadius - separatorInset);
            paint.setShader(new LinearGradient(separator.left, separator.top,
                    separator.right, separator.bottom,
                    new int[]{0xFF7F898F, 0xFF626D74, 0xFF465159},
                    new float[]{0f, 0.55f, 1f}, Shader.TileMode.CLAMP));
            canvas.drawRoundRect(separator, separatorRadius, separatorRadius, paint);
            paint.setShader(null);

            rect.set(separator);
            float faceInset = dp(3) / 4f;
            rect.inset(faceInset, faceInset);
            drawnSurfaceRadius = Math.max(0f, separatorRadius - faceInset);
        }

        private float surfaceRadius() {
            return drawnSurfaceRadius > 0f
                    ? drawnSurfaceRadius : dp(HeimdallUi.INPUT_SURFACE_RADIUS);
        }

        private void drawSurfaceDepth(Canvas canvas, float pressProgress) {
            float radius = Math.max(rect.width(), rect.height()) * 0.72f;
            paint.setStyle(Paint.Style.FILL);
            boolean pearl = HeimdallUi.isPearl(AssistantActivity.this);
            int idleCenter = pearl ? 0x0FFFFFFF : 0x145A8DFF;
            int activeCenter = pearl ? 0x20F08A2A : 0x244EA1FF;
            paint.setShader(new RadialGradient(rect.centerX(), rect.centerY(), radius,
                    pearl
                            ? new int[]{blendColor(idleCenter, activeCenter, pressProgress),
                                    0x00000000, 0x22404A52}
                            : new int[]{blendColor(idleCenter, activeCenter, pressProgress),
                                    0x00070A10, 0x2E000000},
                    new float[]{0f, 0.62f, 1f}, Shader.TileMode.CLAMP));
            float corner = surfaceRadius();
            canvas.drawRoundRect(rect, corner, corner, paint);
            paint.setShader(null);
        }

        private void drawSurfaceGlow(Canvas canvas, float pressProgress) {
            paint.setShader(null);
            paint.setStyle(Paint.Style.STROKE);
            boolean pearl = HeimdallUi.isPearl(AssistantActivity.this);
            if (pearl) {
                if (pressProgress <= 0.001f) {
                    return;
                }
                float glowWidth = dp(4);
                RectF glow = new RectF(rect);
                glow.inset(glowWidth / 2f, glowWidth / 2f);
                paint.setStrokeWidth(glowWidth);
                paint.setColor(scaleColorAlpha(0x55F08A2A, pressProgress));
                float glowRadius = Math.max(0f,
                        surfaceRadius() - glowWidth / 2f);
                canvas.drawRoundRect(glow, glowRadius, glowRadius, paint);

                RectF edge = new RectF(rect);
                edge.inset(dp(1) / 2f, dp(1) / 2f);
                paint.setStrokeWidth(dp(1));
                paint.setColor(blendColor(0x84657078, 0xD8F08A2A, pressProgress));
                canvas.drawRoundRect(edge,
                        Math.max(0f, surfaceRadius() - dp(1) / 2f),
                        Math.max(0f, surfaceRadius() - dp(1) / 2f),
                        paint);
                return;
            }
            float wideStroke = lerp(dp(7) / 2f, dp(5), pressProgress);
            RectF glowRect = new RectF(rect);
            glowRect.inset(wideStroke / 2f, wideStroke / 2f);
            paint.setStrokeWidth(wideStroke);
            paint.setColor(blendColor(
                    HeimdallUi.inputGlow(AssistantActivity.this, false),
                    HeimdallUi.inputGlow(AssistantActivity.this, true),
                    pressProgress));
            float glowRadius = Math.max(0f,
                    surfaceRadius() - wideStroke / 2f);
            canvas.drawRoundRect(glowRect, glowRadius, glowRadius, paint);

            float hairline = dp(1);
            RectF edgeRect = new RectF(rect);
            edgeRect.inset(hairline / 2f, hairline / 2f);
            paint.setStrokeWidth(hairline);
            paint.setColor(blendColor(
                    HeimdallUi.inputEdge(AssistantActivity.this, false),
                    HeimdallUi.inputEdge(AssistantActivity.this, true),
                    pressProgress));
            float edgeRadius = Math.max(0f,
                    surfaceRadius() - hairline / 2f);
            canvas.drawRoundRect(edgeRect, edgeRadius, edgeRadius, paint);
            paint.setShader(null);
        }

        private void drawFocusCorners(Canvas canvas, float pressProgress) {
            float length = lerp(dp(28), dp(36), pressProgress);
            float inset = dp(12);
            float left = rect.left + inset;
            float top = rect.top + inset;
            float right = rect.right - inset;
            float bottom = rect.bottom - inset;
            paint.setShader(null);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(lerp(dp(1), dp(2), pressProgress));
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setColor(HeimdallUi.isPearl(AssistantActivity.this)
                    ? blendColor(0xAAF08A2A, 0xFFF08A2A, pressProgress)
                    : blendColor(0x886A9DDB, 0xEE70B7FF, pressProgress));
            canvas.drawLine(left, top, left + length, top, paint);
            canvas.drawLine(left, top, left, top + length, paint);
            canvas.drawLine(right, top, right - length, top, paint);
            canvas.drawLine(right, top, right, top + length, paint);
            canvas.drawLine(left, bottom, left + length, bottom, paint);
            canvas.drawLine(left, bottom, left, bottom - length, paint);
            canvas.drawLine(right, bottom, right - length, bottom, paint);
            canvas.drawLine(right, bottom, right, bottom - length, paint);
            paint.setStrokeCap(Paint.Cap.BUTT);
        }

        private void drawControlTexture(Canvas canvas, boolean rightStickMode, boolean shizukuTouchMode) {
            canvas.save();
            surfaceClip.reset();
            float radius = surfaceRadius();
            surfaceClip.addRoundRect(rect, radius, radius, Path.Direction.CW);
            canvas.clipPath(surfaceClip);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(1f);
            paint.setColor(HeimdallUi.isPearl(AssistantActivity.this)
                    ? HeimdallUi.inputTexture(AssistantActivity.this)
                    : (shizukuTouchMode ? 0x244AE0A9
                            : (rightStickMode ? 0x265A8DFF : HeimdallUi.inputTexture(AssistantActivity.this))));
            int step = dp(14);
            for (int xLine = -getHeight(); xLine < getWidth(); xLine += step) {
                canvas.drawLine(xLine, rect.bottom, xLine + getHeight(), rect.top, paint);
            }
            paint.setColor(HeimdallUi.inputTextureAlt(AssistantActivity.this));
            int offset = dp(7);
            for (int xLine = -getHeight() + offset; xLine < getWidth(); xLine += step) {
                canvas.drawLine(xLine, rect.top, xLine + getHeight(), rect.bottom, paint);
            }
            canvas.restore();
        }

        private void drawTouchPadDot(Canvas canvas) {
            if (x < 0 || y < 0) {
                drawInputCenterPoint(canvas);
                return;
            }
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(HeimdallUi.isPearl(AssistantActivity.this) ? 0x22F08A2A : 0x224EA1FF);
            canvas.drawCircle(x, y, dp(44), paint);
            paint.setColor(HeimdallUi.isPearl(AssistantActivity.this) ? 0xCCF08A2A : 0xCC70B7FF);
            canvas.drawCircle(x, y, dp(18), paint);
            paint.setColor(HeimdallUi.textColor(AssistantActivity.this));
            canvas.drawCircle(x, y, dp(3), paint);
        }

        private void drawInputCenterPoint(Canvas canvas) {
            float centerX = rect.centerX();
            float centerY = rect.centerY();
            paint.setShader(null);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(HeimdallUi.isPearl(AssistantActivity.this) ? 0x18F08A2A : 0x1255B7E8);
            canvas.drawCircle(centerX, centerY, dp(28), paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(1));
            paint.setColor(HeimdallUi.isPearl(AssistantActivity.this) ? 0x88F7F9FB : 0x775A8DFF);
            canvas.drawCircle(centerX, centerY, dp(20), paint);
            paint.setColor(HeimdallUi.isPearl(AssistantActivity.this) ? 0xAAF08A2A : 0x996A9DDB);
            canvas.drawCircle(centerX, centerY, dp(7), paint);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(HeimdallUi.inputCenter(AssistantActivity.this));
            canvas.drawCircle(centerX, centerY, dp(3), paint);
        }

        private void drawRightStick(Canvas canvas) {
            float centerX = stickCenterX();
            float centerY = stickCenterY();
            float radius = rightStickRadiusPixels();
            float deadRadius = radius * touchpadSettings.rightStickDeadzone;
            float capX = x >= 0 ? x : centerX;
            float capY = y >= 0 ? y : centerY;
            float dx = capX - centerX;
            float dy = capY - centerY;
            float distance = (float) Math.sqrt(dx * dx + dy * dy);
            if (distance > radius && distance > 0f) {
                float scale = radius / distance;
                capX = centerX + dx * scale;
                capY = centerY + dy * scale;
            }

            boolean pearl = HeimdallUi.isPearl(AssistantActivity.this);
            boolean active = x >= 0 && y >= 0;

            if (pearl) {
                paint.setShader(null);
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(0x1E000000);
                canvas.drawCircle(centerX, centerY + dp(2), radius + dp(2), paint);
                paint.setShader(new RadialGradient(centerX - radius * 0.18f,
                        centerY - radius * 0.20f, radius * 1.15f,
                        new int[]{0xFFD4D9DC, 0xFF8F999F, 0xFF4E5961},
                        new float[]{0f, 0.70f, 1f}, Shader.TileMode.CLAMP));
                canvas.drawCircle(centerX, centerY, radius + dp(1), paint);
                paint.setShader(null);
            }

            paint.setStyle(Paint.Style.FILL);
            paint.setShader(new RadialGradient(
                    pearl ? centerX - radius * 0.16f : centerX,
                    pearl ? centerY - radius * 0.18f : centerY,
                    pearl ? radius * 1.22f : radius,
                    pearl
                            ? new int[]{0xFF747F87, 0xFF4B545B, 0xFF2A3136}
                            : new int[]{0xFF1B2536, 0xFF0A0D13, 0xFF030407},
                    new float[]{0f, 0.72f, 1f}, Shader.TileMode.CLAMP));
            canvas.drawCircle(centerX, centerY, radius, paint);
            paint.setShader(null);

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(pearl ? dp(1) : dp(1));
            if (pearl) {
                paint.setShader(new LinearGradient(centerX - radius, centerY - radius,
                        centerX + radius, centerY + radius,
                        new int[]{0xE8FFFFFF, 0x9AAAB3B9, 0x88434C54},
                        new float[]{0f, 0.54f, 1f}, Shader.TileMode.CLAMP));
            } else {
                paint.setColor(0x665A7494);
            }
            canvas.drawCircle(centerX, centerY, radius, paint);
            paint.setShader(null);
            if (pearl) {
                paint.setStrokeWidth(dp(1));
                paint.setColor(0x4CFFFFFF);
                canvas.drawCircle(centerX, centerY, radius * 0.78f, paint);
            }
            paint.setStrokeWidth(dp(1));
            paint.setColor(pearl ? 0x668D989F : 0x339AA8B8);
            canvas.drawCircle(centerX, centerY, deadRadius, paint);

            paint.setStrokeWidth(dp(2));
            paint.setColor(pearl
                    ? (active ? 0xB8F08A2A : 0x4079858D)
                    : 0x6670B7FF);
            if (active) {
                canvas.drawLine(centerX, centerY, capX, capY, paint);
            }

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(pearl ? 0x3D000000 : 0x66000000);
            canvas.drawCircle(capX, capY + dp(3), dp(34), paint);
            paint.setShader(new RadialGradient(capX - dp(10), capY - dp(12), dp(42),
                    pearl
                            ? new int[]{0xFF838E96, 0xFF434B52, 0xFF20262B}
                            : new int[]{0xFF566171, 0xFF242A33, 0xFF0D1015},
                    new float[]{0f, 0.55f, 1f}, Shader.TileMode.CLAMP));
            canvas.drawCircle(capX, capY, dp(31), paint);
            paint.setShader(null);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(pearl ? dp(2) : dp(1));
            if (pearl && !active) {
                paint.setShader(new LinearGradient(capX - dp(31), capY - dp(31),
                        capX + dp(31), capY + dp(31),
                        new int[]{0xE6FFFFFF, 0xAA9AA5AD, 0x88515B63},
                        new float[]{0f, 0.55f, 1f}, Shader.TileMode.CLAMP));
            } else {
                paint.setColor(pearl ? 0xD8F08A2A : 0xAA4EA1FF);
            }
            canvas.drawCircle(capX, capY, dp(31), paint);
            paint.setShader(null);

        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            x = event.getX();
            y = event.getY();
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) {
                setSurfacePressed(true);
            } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                setSurfacePressed(false);
            }
            String mode = TouchpadSettings.normalizeMode(touchpadSettings.mode);
            boolean shizukuTouchMode = TouchpadSettings.MODE_SHIZUKU_TOUCH.equals(mode);
            if (TouchpadSettings.MODE_RIGHT_STICK.equals(mode)) {
                return handleRightStickTouch(event);
            }
            if (TouchpadSettings.MODE_RELATIVE_MOUSE.equals(mode)) {
                return handleRelativeMouseTouch(event);
            }
            if (!TouchpadSettings.MODE_TOUCH_DRAG.equals(mode) && !shizukuTouchMode) {
                if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            showErrorAction(getString(R.string.touchpad_mode_route_unavailable,
                            localizedTouchpadModeLabel(touchpadSettings.mode)));
                }
                if (event.getActionMasked() == MotionEvent.ACTION_UP || event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
                    x = -1;
                    y = -1;
                    lastX = -1;
                    lastY = -1;
                }
                invalidate();
                return true;
            }
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                updateTargetDisplayInfo();
                lastX = x;
                lastY = y;
                originX = x;
                originY = y;
                lastDispatchTime = 0;
                shizukuSmoothedDx = 0f;
                shizukuSmoothedDy = 0f;
                long gestureToken = ++touchDragGestureToken;
                touchDragErrorShown = false;
                touchDragCallback = createTouchDragCallback(gestureToken);
                boolean started;
                if (shizukuTouchMode) {
                    started = InputBridge.startShizukuTouchpadDrag(AssistantActivity.this,
                            targetDisplayId, targetDisplayWidth, targetDisplayHeight,
                            touchpadSettings.anchorX, touchpadSettings.anchorY, touchDragCallback);
                } else {
                    started = InputBridge.startTouchpadDrag(AssistantActivity.this,
                            targetDisplayId, targetDisplayWidth, targetDisplayHeight,
                            touchpadSettings.anchorX, touchpadSettings.anchorY, touchDragCallback);
                }
                touchDragStarted = started;
                touchDragAcceptingMoves = started;
                if (!started) {
                    clearUnavailableGestureVisual();
                }
                invalidate();
                return true;
            }

            if (event.getActionMasked() == MotionEvent.ACTION_MOVE) {
                if (!touchDragAcceptingMoves) {
                    x = -1f;
                    y = -1f;
                    return true;
                }
                long now = event.getEventTime();
                float dx = x - lastX;
                float dy = y - lastY;
                if (shizukuTouchMode) {
                    if (Math.abs(dx) >= touchpadSettings.shizukuTouchMinDelta
                            || Math.abs(dy) >= touchpadSettings.shizukuTouchMinDelta) {
                        float outDx = transformShizukuTouchDelta(dx, touchpadSettings.shizukuTouchSensitivityX);
                        float outDy = transformShizukuTouchDelta(dy, touchpadSettings.shizukuTouchSensitivityY);
                        float smoothing = clampFloat(touchpadSettings.shizukuTouchSmoothing, 0f, 0.9f);
                        shizukuSmoothedDx = shizukuSmoothedDx * smoothing + outDx * (1f - smoothing);
                        shizukuSmoothedDy = shizukuSmoothedDy * smoothing + outDy * (1f - smoothing);
                        boolean moved = InputBridge.moveShizukuTouchpadDrag(AssistantActivity.this,
                                shizukuSmoothedDx, shizukuSmoothedDy, touchpadSettings.shizukuTouchFrameMs,
                                touchDragCallback);
                        if (!moved) {
                            touchDragAcceptingMoves = false;
                            clearUnavailableGestureVisual();
                        }
                        lastX = x;
                        lastY = y;
                    }
                } else if (now - lastDispatchTime >= touchpadSettings.intervalMs
                        && (Math.abs(dx) >= touchpadSettings.minDelta || Math.abs(dy) >= touchpadSettings.minDelta)) {
                    float scale = touchpadSettings.sensitivity;
                    boolean moved = InputBridge.moveTouchpadDrag(AssistantActivity.this,
                            dx * scale, dy * scale, touchpadSettings.strokeMs, touchDragCallback);
                    if (!moved) {
                        touchDragAcceptingMoves = false;
                        clearUnavailableGestureVisual();
                    }
                    lastDispatchTime = now;
                    lastX = x;
                    lastY = y;
                }
                invalidate();
                return true;
            }

            if (event.getActionMasked() == MotionEvent.ACTION_UP || event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
                boolean shouldEndDrag = touchDragStarted;
                InputBridge.Callback callback = touchDragCallback;
                if (event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
                    // Leaving the local touch surface is a normal recenter path. Release the
                    // upper-screen drag, but ignore the Accessibility cancellation callback.
                    touchDragGestureToken++;
                }
                touchDragStarted = false;
                touchDragAcceptingMoves = false;
                touchDragCallback = null;
                x = -1;
                y = -1;
                lastX = -1;
                lastY = -1;
                originX = -1;
                originY = -1;
                shizukuSmoothedDx = 0f;
                shizukuSmoothedDy = 0f;
                if (shouldEndDrag && callback != null) {
                    if (shizukuTouchMode) {
                        InputBridge.endShizukuTouchpadDrag(AssistantActivity.this,
                                touchpadSettings.strokeMs, callback);
                    } else {
                        InputBridge.endTouchpadDrag(AssistantActivity.this,
                                touchpadSettings.strokeMs, callback);
                    }
                }
            }
            invalidate();
            return true;
        }

        private InputBridge.Callback createTouchDragCallback(long gestureToken) {
            return new InputBridge.Callback() {
                @Override
                public void onStatus(String message) {
                    uiHandler.post(() -> {
                        if (gestureToken == touchDragGestureToken) {
                            inputStatusCallback.onStatus(message);
                        }
                    });
                }

                @Override
                public void onError(String message) {
                    uiHandler.post(() -> {
                        if (gestureToken != touchDragGestureToken) {
                            return;
                        }
                        touchDragAcceptingMoves = false;
                        clearUnavailableGestureVisual();
                        if (!touchDragErrorShown) {
                            touchDragErrorShown = true;
                            inputStatusCallback.onError(message);
                        }
                    });
                }

                @Override
                public void onGestureCancelled() {
                    uiHandler.post(() -> {
                        if (gestureToken != touchDragGestureToken) {
                            return;
                        }
                        touchDragAcceptingMoves = false;
                        clearUnavailableGestureVisual();
                    });
                }
            };
        }

        private void cancelTouchDragGesture(String mode) {
            boolean shouldCancel = touchDragStarted;
            InputBridge.Callback callback = touchDragCallback;
            touchDragGestureToken++;
            touchDragStarted = false;
            touchDragAcceptingMoves = false;
            touchDragCallback = null;
            if (!shouldCancel) {
                return;
            }
            if (TouchpadSettings.MODE_SHIZUKU_TOUCH.equals(mode)) {
                InputBridge.cancelShizukuTouchpadDrag(AssistantActivity.this);
            } else if (callback != null) {
                InputBridge.endTouchpadDrag(AssistantActivity.this,
                        touchpadSettings.strokeMs, callback);
            }
        }

        private void clearUnavailableGestureVisual() {
            currentStickX = 0f;
            currentStickY = 0f;
            x = -1f;
            y = -1f;
            lastX = -1f;
            lastY = -1f;
            originX = -1f;
            originY = -1f;
            setSurfacePressed(false);
            invalidate();
        }

        private float transformShizukuTouchDelta(float delta, float sensitivity) {
            float sign = Math.signum(delta);
            float normalized = Math.abs(delta) / 24f;
            float curved = (float) Math.pow(normalized, touchpadSettings.shizukuTouchCurve) * 24f;
            return sign * curved * sensitivity;
        }

        private boolean handleRelativeMouseTouch(MotionEvent event) {
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) {
                uiHandler.removeCallbacks(relativeMouseNeutralReset);
                relativeMouseErrorShown = false;
                lastX = x;
                lastY = y;
                originX = x;
                originY = y;
                if (!relativeMouseBackendAvailable()) {
                    showErrorAction(getString(R.string.shizuku_controller_route_required));
                    x = -1f;
                    y = -1f;
                } else if (!shizukuControllerServicePrepared()) {
                    lastX = -1f;
                    lastY = -1f;
                    originX = -1f;
                    originY = -1f;
                    x = -1f;
                    y = -1f;
                }
                invalidate();
                return true;
            }
            if (action == MotionEvent.ACTION_MOVE) {
                if (!relativeMouseBackendAvailable() || lastX < 0f || lastY < 0f) {
                    return true;
                }
                float dx = x - lastX;
                float dy = y - lastY;
                lastX = x;
                lastY = y;
                RelativeMouseMapper.Output output =
                        RelativeMouseMapper.map(dx, dy, touchpadSettings);
                if (!output.isNeutral()) {
                    uiHandler.removeCallbacks(relativeMouseNeutralReset);
                    boolean sent = emitRelativeMouseStick(output.x, output.y);
                    if (sent) {
                        uiHandler.postDelayed(relativeMouseNeutralReset,
                                Math.max(8, touchpadSettings.relativeMousePulseDurationMs));
                    }
                }
                invalidate();
                return true;
            }
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                cancelRelativeMousePulse(true);
                x = -1f;
                y = -1f;
                lastX = -1f;
                lastY = -1f;
                originX = -1f;
                originY = -1f;
                invalidate();
                return true;
            }
            return true;
        }

        private void cancelRelativeMousePulse(boolean forceNeutral) {
            uiHandler.removeCallbacks(relativeMouseNeutralReset);
            if (forceNeutral && relativeMouseBackendAvailable()) {
                emitRelativeMouseStick(0f, 0f);
            }
        }

        private boolean emitRelativeMouseStick(float stickX, float stickY) {
            if (!relativeMouseBackendAvailable()) {
                if (!relativeMouseErrorShown) {
                    relativeMouseErrorShown = true;
                    showErrorAction(getString(R.string.shizuku_controller_route_required));
                }
                return false;
            }
            try {
                NativeGamepadPath.Device device = NativeGamepadPath.requireDevice();
                String result = ShizukuNativeController.emitRightStick(
                        AssistantActivity.this, device, stickX, stickY);
                boolean failed = !NativeGamepadPath.operationSucceeded(result);
                if (failed && !relativeMouseErrorShown) {
                    relativeMouseErrorShown = true;
                    showErrorAction(getString(R.string.native_controller_write_failed));
                }
                return !failed;
            } catch (Throwable t) {
                if (!relativeMouseErrorShown) {
                    relativeMouseErrorShown = true;
                    showErrorAction(getString(R.string.native_controller_write_failed));
                }
                return false;
            }
        }

        @Override
        protected void onDetachedFromWindow() {
            resetSurfacePressFeedback();
            String mode = TouchpadSettings.normalizeMode(touchpadSettings.mode);
            if (TouchpadSettings.MODE_RELATIVE_MOUSE.equals(mode)) {
                cancelRelativeMousePulse(true);
            } else if (TouchpadSettings.MODE_RIGHT_STICK.equals(mode)) {
                if (rightStickGestureStarted) {
                    recenterRightStick(false);
                }
            } else {
                cancelTouchDragGesture(mode);
            }
            rightStickGestureStarted = false;
            rightStickGestureActive = false;
            uiHandler.removeCallbacks(relativeMouseNeutralReset);
            super.onDetachedFromWindow();
        }

        private float clampFloat(float value, float min, float max) {
            return Math.max(min, Math.min(max, value));
        }

        private float lerp(float start, float end, float fraction) {
            return start + (end - start) * clampFloat(fraction, 0f, 1f);
        }

        private int blendColor(int start, int end, float fraction) {
            float amount = clampFloat(fraction, 0f, 1f);
            return Color.argb(
                    Math.round(lerp(Color.alpha(start), Color.alpha(end), amount)),
                    Math.round(lerp(Color.red(start), Color.red(end), amount)),
                    Math.round(lerp(Color.green(start), Color.green(end), amount)),
                    Math.round(lerp(Color.blue(start), Color.blue(end), amount)));
        }

        private int scaleColorAlpha(int color, float fraction) {
            return Color.argb(
                    Math.round(Color.alpha(color) * clampFloat(fraction, 0f, 1f)),
                    Color.red(color), Color.green(color), Color.blue(color));
        }

        private boolean handleRightStickTouch(MotionEvent event) {
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) {
                rightStickErrorShown = false;
                if (!shizukuControllerServicePrepared()) {
                    rightStickGestureStarted = false;
                    rightStickGestureActive = false;
                    clearUnavailableGestureVisual();
                    return true;
                }
                rightStickGestureStarted = true;
                rightStickGestureActive = true;
                if (TouchpadSettings.RIGHT_STICK_CENTER_STATIC.equals(
                        TouchpadSettings.normalizeRightStickCenterMode(touchpadSettings.rightStickCenterMode))) {
                    originX = staticStickCenterX();
                    originY = staticStickCenterY();
                } else {
                    originX = x;
                    originY = y;
                }
                currentStickX = 0f;
                currentStickY = 0f;
                lastDispatchTime = 0;
                if (!recenterRightStick(false)
                        || !updateRightStickFromTouch(event.getEventTime(), true)) {
                    rightStickGestureActive = false;
                    clearUnavailableGestureVisual();
                }
                invalidate();
                return true;
            }
            if (action == MotionEvent.ACTION_MOVE) {
                if (!rightStickGestureActive) {
                    x = -1f;
                    y = -1f;
                } else if (!updateRightStickFromTouch(event.getEventTime(), false)) {
                    rightStickGestureActive = false;
                    clearUnavailableGestureVisual();
                }
                invalidate();
                return true;
            }
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                if (rightStickGestureStarted) {
                    recenterRightStick(false);
                }
                rightStickGestureStarted = false;
                rightStickGestureActive = false;
                currentStickX = 0f;
                currentStickY = 0f;
                x = -1;
                y = -1;
                lastX = -1;
                lastY = -1;
                originX = -1;
                originY = -1;
                invalidate();
                return true;
            }
            return true;
        }

        private boolean updateRightStickFromTouch(long eventTime, boolean force) {
            if (!force && eventTime - lastDispatchTime < touchpadSettings.intervalMs) {
                return true;
            }
            float radius = rightStickRadiusPixels();
            float stickX = applyRightStickCurve((x - stickCenterX()) / radius);
            float stickY = applyRightStickCurve((y - stickCenterY()) / radius);
            currentStickX = stickX;
            currentStickY = stickY;
            boolean sent = emitRightStick(stickX, stickY, false);
            lastDispatchTime = eventTime;
            return sent;
        }

        private float stickCenterX() {
            return originX >= 0 ? originX : staticStickCenterX();
        }

        private float stickCenterY() {
            return originY >= 0 ? originY : staticStickCenterY();
        }

        private float staticStickCenterX() {
            return getWidth() * 0.5f;
        }

        private float staticStickCenterY() {
            return getHeight() * 0.5f;
        }

        private float rightStickRadiusPixels() {
            return Math.max(dp(36), Math.min(getWidth(), getHeight()) * touchpadSettings.rightStickRadius);
        }

        private float applyRightStickCurve(float value) {
            float scaled = clampUnit(value * touchpadSettings.rightStickSensitivity);
            float magnitude = Math.abs(scaled);
            if (magnitude <= touchpadSettings.rightStickDeadzone) {
                return 0f;
            }
            float normalized = (magnitude - touchpadSettings.rightStickDeadzone)
                    / Math.max(0.001f, 1f - touchpadSettings.rightStickDeadzone);
            float curved = (float) Math.pow(normalized, touchpadSettings.rightStickCurve);
            return Math.signum(scaled) * clampUnit(curved * touchpadSettings.rightStickMaxOutput);
        }

        private boolean recenterRightStick(boolean showOk) {
            int count = Math.max(1, touchpadSettings.rightStickRecenterBursts);
            for (int i = 0; i < count; i++) {
                if (!emitRightStick(0f, 0f, showOk && i == count - 1)) {
                    return false;
                }
                if (i < count - 1) {
                    try {
                        Thread.sleep(4L);
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                        return false;
                    }
                }
            }
            return true;
        }

        private boolean emitRightStick(float stickX, float stickY, boolean showOk) {
            try {
                NativeGamepadPath.Device device = NativeGamepadPath.requireDevice();
                String result = InputBridge.emitNativeRightStick(
                        AssistantActivity.this, device, stickX, stickY);
                if (!NativeGamepadPath.operationSucceeded(result)) {
                    showRightStickErrorOnce();
                    return false;
                } else if (showOk) {
                    showAction(getString(R.string.native_controller_step_complete));
                }
                return true;
            } catch (Throwable t) {
                showRightStickErrorOnce();
                return false;
            }
        }

        private void showRightStickErrorOnce() {
            if (!rightStickErrorShown) {
                rightStickErrorShown = true;
                showErrorAction(getString(R.string.native_controller_write_failed));
            }
        }

        private float clampUnit(float value) {
            if (value < -1f) {
                return -1f;
            }
            if (value > 1f) {
                return 1f;
            }
            return value;
        }

    }

}
