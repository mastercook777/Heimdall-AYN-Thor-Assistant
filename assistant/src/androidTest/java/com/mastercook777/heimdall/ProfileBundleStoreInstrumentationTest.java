package com.mastercook777.heimdall;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public final class ProfileBundleStoreInstrumentationTest extends Instrumentation {
    private Context target;

    @Override
    public void onCreate(Bundle arguments) {
        super.onCreate(arguments);
        start();
    }

    @Override
    public void onStart() {
        Bundle result = new Bundle();
        try {
            target = getTargetContext();
            assertNotNull(target);
            testHardwareMonitorModelContract();
            testControllerSequenceSafetyPolicy();
            testComposedControllerSequenceContract();
            testMacroCloneAndDispatchGateContract();
            testInterruptedComposedReplayReleasesHeldState();
            testVirtualKeyboardTransportContract();
            testKeyboardPadModelContract();
            testQuickActionsModelContract();
            testThemeRegistryContract();
            testProfileThemeBindingContract();
            testWidgetLayoutIdentityContract();
            testTranslationWidgetModelContract();
            testCanvasRuntimeDecodePolicy();
            testCanvasExtremeAspectFillPolicy();
            testCanvasAnimationContract();
            testProfileIconDecodePolicy();
            testUserMacroIconDeletionContract();
            testGameContextIdentityAndResolverContract();
            testGameContextAppOnlyFallbackAndManualSelectionGuardContract();
            testForegroundObservationClearAndUnicodeExportFilenameContract();
            testGameContextUserServiceLifetimeContract();
            testEdenGameContextIdentityAndResolverContract();
            testGameNativeCurrentStateGameContextContract();
            testPpssppPositiveLaunchGameContextContract();
            testRetroArchTwoLevelGameContextContract();
            testAssistantActivitySingleTaskContract();
            testSecondaryDisplayLaunchRouterContract();
            testUpperDisplaySingleTouchHandoffCoordinateContract();
            testUpperDisplayStartedLifecycleHandoffContract();
            testStartupCapabilityDefaultContract();
            testAdvancedControlsStateContract();
            testInteractiveMapBrowserSettingsContract();
            testSelfContainedRoundTripAfterSourcesAreDeleted();
            testCorruptMissingUnsafeAndOversizedBundlesFailClosed();
            testLegacyProfileJsonRemainsImportable();
            result.putString("result",
                    "Profile bundle and controller sequence safety checks passed");
            finish(Activity.RESULT_OK, result);
        } catch (Throwable failure) {
            result.putString("result", failure.toString());
            result.putString("stack", android.util.Log.getStackTraceString(failure));
            finish(Activity.RESULT_CANCELED, result);
        }
    }

    public void testVirtualKeyboardTransportContract() {
        assertTrue(VirtualKeyboardDispatcher.isSupportedKeyCode(1));
        assertTrue(VirtualKeyboardDispatcher.isSupportedKeyCode(255));
        assertFalse(VirtualKeyboardDispatcher.isSupportedKeyCode(0));
        assertFalse(VirtualKeyboardDispatcher.isSupportedKeyCode(256));
        assertEquals(ShizukuNativeUserService.TRANSACTION_RELEASE_VIRTUAL_MOUSE + 1,
                ShizukuNativeUserService.TRANSACTION_OPEN_VIRTUAL_KEYBOARD);
        assertEquals(ShizukuNativeUserService.TRANSACTION_OPEN_VIRTUAL_KEYBOARD + 1,
                ShizukuNativeUserService.TRANSACTION_EMIT_VIRTUAL_KEYBOARD);
        assertEquals(ShizukuNativeUserService.TRANSACTION_EMIT_VIRTUAL_KEYBOARD + 1,
                ShizukuNativeUserService.TRANSACTION_RELEASE_VIRTUAL_KEYBOARD_KEYS);
        assertEquals(ShizukuNativeUserService.TRANSACTION_RELEASE_VIRTUAL_KEYBOARD_KEYS + 1,
                ShizukuNativeUserService.TRANSACTION_RELEASE_VIRTUAL_KEYBOARD);
    }

    public void testThemeRegistryContract() {
        HeimdallUi.clearActiveProfileTheme();
        List<ThemeDefinition> themes = ThemeRegistry.selectableThemes();
        assertEquals(6, themes.size());
        assertEquals(ThemeRegistry.ID_HEIMDALL_BLUE, themes.get(0).id);
        assertEquals(ThemeFamily.HEIMDALL, themes.get(0).family);
        assertEquals(ThemeRegistry.ID_HEIMDALL_AMBER, themes.get(1).id);
        assertEquals(ThemeFamily.HEIMDALL, themes.get(1).family);
        assertEquals(ThemeRegistry.ID_HEIMDALL_NOCTURNE, themes.get(2).id);
        assertEquals(ThemeFamily.HEIMDALL, themes.get(2).family);
        assertEquals(ThemeRegistry.ID_FREYA_WHITE, themes.get(3).id);
        assertEquals(ThemeFamily.FREYA, themes.get(3).family);
        assertEquals(ThemeRegistry.ID_FREYA_ROSEWOOD, themes.get(4).id);
        assertEquals(ThemeFamily.FREYA, themes.get(4).family);
        assertEquals(ThemeRegistry.ID_FREYA_CELADON, themes.get(5).id);
        assertEquals(ThemeFamily.FREYA, themes.get(5).family);
        assertTrue(themes.get(0).displayNameRes != 0);
        assertTrue(themes.get(1).displayNameRes != 0);
        assertTrue(themes.get(2).displayNameRes != 0);
        assertTrue(themes.get(3).displayNameRes != 0);
        assertTrue(themes.get(4).displayNameRes != 0);
        assertTrue(themes.get(5).displayNameRes != 0);

        assertEquals(ThemeRegistry.ID_HEIMDALL_BLUE,
                ThemeRegistry.resolve(" dark ").id());
        assertEquals(ThemeRegistry.ID_FREYA_WHITE,
                ThemeRegistry.resolve("PEARL").id());
        assertEquals(ThemeRegistry.ID_HEIMDALL_BLUE,
                ThemeRegistry.resolve("future.unknown").id());
        assertTrue(ThemeRegistry.isKnown(ThemeRegistry.ID_HEIMDALL_BLUE));
        assertTrue(ThemeRegistry.isKnown(ThemeRegistry.ID_HEIMDALL_AMBER));
        assertTrue(ThemeRegistry.isKnown(ThemeRegistry.ID_HEIMDALL_NOCTURNE));
        assertTrue(ThemeRegistry.isKnown(ThemeRegistry.LEGACY_PEARL));
        assertTrue(ThemeRegistry.isKnown(ThemeRegistry.ID_FREYA_ROSEWOOD));
        assertTrue(ThemeRegistry.isKnown(ThemeRegistry.ID_FREYA_CELADON));
        assertFalse(ThemeRegistry.isKnown("freya.rose"));
        assertFalse(ThemeRegistry.isKnown("future.unknown"));

        ResolvedTheme blue = ThemeRegistry.resolve(ThemeRegistry.ID_HEIMDALL_BLUE);
        ResolvedTheme amber = ThemeRegistry.resolve(ThemeRegistry.ID_HEIMDALL_AMBER);
        ResolvedTheme nocturne = ThemeRegistry.resolve(ThemeRegistry.ID_HEIMDALL_NOCTURNE);
        ResolvedTheme white = ThemeRegistry.resolve(ThemeRegistry.ID_FREYA_WHITE);
        ResolvedTheme rosewood = ThemeRegistry.resolve(ThemeRegistry.ID_FREYA_ROSEWOOD);
        ResolvedTheme celadon = ThemeRegistry.resolve(ThemeRegistry.ID_FREYA_CELADON);
        assertEquals(1, blue.materials.mediaFrameContentInsetDp);
        assertEquals(6, white.materials.mediaFrameContentInsetDp);
        assertEquals(0xFF4EA1FF, blue.palette.accent);
        assertEquals(0xFF090B0D, amber.palette.pageBackground);
        assertEquals(0xFF090B0D, amber.palette.surfaceBase);
        assertEquals(0xFF121619, amber.palette.surfaceRaised);
        assertEquals(0xFF07090A, amber.palette.surfaceInset);
        assertEquals(0xFF101417, amber.palette.surfaceControl);
        assertEquals(0xFF0C1013, amber.palette.surfaceField);
        assertEquals(0xFFEEECE6, amber.palette.textPrimary);
        assertEquals(0xFFA5A59F, amber.palette.textSecondary);
        assertEquals(0xFF303538, amber.palette.edgeNeutral);
        assertEquals(0xFF4B4B45, amber.palette.edgeStrong);
        assertEquals(0xFFD99A2B, amber.palette.accent);
        assertEquals(0xFFE7A436, amber.palette.focus);
        assertEquals(0xFFD99A2B, amber.palette.selection);
        assertEquals(0xFFF0B84C, amber.palette.activeInput);
        assertTrue(blue.materials == amber.materials);
        assertEquals(0xFF0E0C10, nocturne.palette.pageBackground);
        assertEquals(0xFF0E0C10, nocturne.palette.flatPageBackground);
        assertEquals(0xFF16131B, nocturne.palette.surfaceBase);
        assertEquals(0xFF231C29, nocturne.palette.surfaceRaised);
        assertEquals(0xFF09080C, nocturne.palette.surfaceInset);
        assertEquals(0xFF1E1823, nocturne.palette.surfaceControl);
        assertEquals(0xFF131016, nocturne.palette.surfaceField);
        assertEquals(0xFFF1EDF4, nocturne.palette.textPrimary);
        assertEquals(0xFFAAA3B0, nocturne.palette.textSecondary);
        assertEquals(0x8CAAA3B0, nocturne.palette.textDisabled);
        assertEquals(0xFF33293C, nocturne.palette.edgeNeutral);
        assertEquals(0xFF50385F, nocturne.palette.edgeStrong);
        assertEquals(0xFF809D74, nocturne.palette.accent);
        assertEquals(0xFF4F5F4C, nocturne.palette.accentGradientEnd);
        assertEquals(0xFF9BC089, nocturne.palette.accentStrong);
        assertEquals(0xFF9BC089, nocturne.palette.focus);
        assertEquals(0xFF809D74, nocturne.palette.selection);
        assertEquals(0xFFADD297, nocturne.palette.activeInput);
        assertEquals(0xAA557A4F, nocturne.palette.inputEdgeIdle);
        assertEquals(0x3333293C, nocturne.palette.quickActionDivider);
        assertTrue(blue.materials == nocturne.materials);
        assertTrue(amber.materials == nocturne.materials);
        assertTrue(blue.glassColors != null);
        assertTrue(amber.glassColors != null);
        assertTrue(nocturne.glassColors != null);
        assertTrue(nocturne.cncColors == null);
        assertTrue(amber.cncColors == null);
        assertEquals(0xCC111824, blue.glassColors.surface(
                ThemeGlassColors.SYSTEM_CHROME).top);
        assertEquals(0xCC121619, amber.glassColors.surface(
                ThemeGlassColors.SYSTEM_CHROME).top);
        assertEquals(0xA8101417, amber.glassColors.surface(
                ThemeGlassColors.QUICK_ACTIONS).top);
        assertEquals(0xC0121619, amber.glassColors.surface(
                ThemeGlassColors.MACRO_PRIMARY).top);
        assertEquals(0xD00C1013, amber.glassColors.surface(
                ThemeGlassColors.MACRO_PRIMARY).bottom);
        assertEquals(0x88D99A2B, amber.glassColors.surface(
                ThemeGlassColors.MACRO_PRIMARY).edgeTop);
        assertEquals(0x44303538, amber.glassColors.surface(
                ThemeGlassColors.MACRO_PRIMARY).edgeBottom);
        assertEquals(0xFFE7A436, amber.glassColors.surface(
                ThemeGlassColors.MACRO_FOCUSED).edgeTop);
        assertEquals(0x760C1013, amber.glassColors.surface(
                ThemeGlassColors.FIELD).top);
        assertEquals(0xFFD99A2B, amber.componentColors.quickVolumeThumbIdle);
        assertEquals(0xFF101417, amber.componentColors.inputFaceIdleTop);
        assertEquals(0xFF0C1013, amber.componentColors.inputFaceIdleBottom);
        assertEquals(0x66303538, amber.glassColors.rightStickWellEdge);
        assertEquals(0xAAF0B84C, amber.glassColors.rightStickCapActiveEdge);
        assertEquals(0xFF0B1018, blue.glassColors.surface(
                ThemeGlassColors.SETTINGS_CONTENT).top);
        assertEquals(0xFF2B3748, blue.glassColors.surface(
                ThemeGlassColors.SETTINGS_CONTENT).edgeTop);
        assertEquals(0xFF0C1013, amber.glassColors.surface(
                ThemeGlassColors.SETTINGS_CONTENT).top);
        assertEquals(0xFF303538, amber.glassColors.surface(
                ThemeGlassColors.SETTINGS_CONTENT).edgeTop);
        assertEquals(0xCC231C29, nocturne.glassColors.surface(
                ThemeGlassColors.SYSTEM_CHROME).top);
        assertEquals(0xA81E1823, nocturne.glassColors.surface(
                ThemeGlassColors.QUICK_ACTIONS).top);
        assertEquals(0xC0231C29, nocturne.glassColors.surface(
                ThemeGlassColors.MACRO_PRIMARY).top);
        assertEquals(0xD0131016, nocturne.glassColors.surface(
                ThemeGlassColors.MACRO_PRIMARY).bottom);
        assertEquals(0x88809D74, nocturne.glassColors.surface(
                ThemeGlassColors.MACRO_PRIMARY).edgeTop);
        assertEquals(0xFF9BC089, nocturne.glassColors.surface(
                ThemeGlassColors.MACRO_FOCUSED).edgeTop);
        assertEquals(0xFF131016, nocturne.glassColors.surface(
                ThemeGlassColors.SETTINGS_CONTENT).top);
        assertEquals(0xFF33293C, nocturne.glassColors.surface(
                ThemeGlassColors.SETTINGS_CONTENT).edgeTop);
        assertEquals(0xFF809D74, nocturne.componentColors.quickActionIconIdle);
        assertEquals(0xFF809D74, nocturne.componentColors.quickVolumeThumbIdle);
        assertEquals(0xFF1E1823, nocturne.componentColors.inputFaceIdleTop);
        assertEquals(0xFF131016, nocturne.componentColors.inputFaceIdleBottom);
        assertEquals(nocturne.componentColors.inputFaceIdleTop,
                nocturne.componentColors.inputFaceActiveTop);
        assertEquals(nocturne.componentColors.inputFaceIdleBottom,
                nocturne.componentColors.inputFaceActiveBottom);
        assertEquals(0x6633293C, nocturne.glassColors.rightStickWellEdge);
        assertEquals(0xAAADD297, nocturne.glassColors.rightStickCapActiveEdge);
        assertEquals(0xFF809D74, nocturne.componentColors.batteryFill);
        assertEquals(0xFFADD297, nocturne.componentColors.keyboard.lockedText);
        assertEquals(0xFFF08A2A, white.palette.accent);
        assertEquals(0xFFF1E7E2, rosewood.palette.pageBackground);
        assertEquals(0xFFF6EEEA, rosewood.palette.surfaceBase);
        assertEquals(0xFFFBF5F1, rosewood.palette.surfaceRaised);
        assertEquals(0xFFD9CCC6, rosewood.palette.surfaceInset);
        assertEquals(0xFF49332D, rosewood.palette.textPrimary);
        assertEquals(0xFFC65D7B, rosewood.palette.accent);
        assertEquals(0xFFD16A87, rosewood.palette.focus);
        assertEquals(0xFFD97993, rosewood.palette.activeInput);
        assertEquals(0xFFEEEAE3, celadon.palette.pageBackground);
        assertEquals(0xFFEEEAE3, celadon.palette.flatPageBackground);
        assertEquals(0xFFECE8E1, celadon.palette.surfaceBase);
        assertEquals(0xFFF7F4EE, celadon.palette.surfaceRaised);
        assertEquals(0xFFD1D1C7, celadon.palette.surfaceInset);
        assertEquals(0xFFE6E2DA, celadon.palette.surfaceControl);
        assertEquals(0xFFE1DED7, celadon.palette.surfaceField);
        assertEquals(0xFF2E3431, celadon.palette.textPrimary);
        assertEquals(0xFF68716C, celadon.palette.textSecondary);
        assertEquals(0x8C68716C, celadon.palette.textDisabled);
        assertEquals(0xFFB7BEB9, celadon.palette.edgeNeutral);
        assertEquals(0xFF8F9A95, celadon.palette.edgeStrong);
        assertEquals(0xFF6F947F, celadon.palette.accent);
        assertEquals(0xFF9EB4A8, celadon.palette.accentGradientEnd);
        assertEquals(0xFF557B67, celadon.palette.accentStrong);
        assertEquals(0xFF557B67, celadon.palette.focus);
        assertEquals(0xFF6F947F, celadon.palette.selection);
        assertEquals(0xFF416B57, celadon.palette.activeInput);
        assertEquals(0xAA829B8E, celadon.palette.inputEdgeIdle);
        assertEquals(0x33949C97, celadon.palette.quickActionDivider);
        assertTrue(white.materials == rosewood.materials);
        assertTrue(white.materials == celadon.materials);
        assertTrue(rosewood.materials == celadon.materials);
        assertTrue(white.glassColors == null);
        assertTrue(celadon.glassColors == null);
        ThemeCncColors.Surface whiteRaised = white.cncColors.surface(ThemeCncColors.RAISED);
        assertEquals(0xFFF7F6F4, whiteRaised.faceTop);
        assertEquals(0xFFF0F1EF, whiteRaised.faceBottom);
        assertEquals(0xFFF9FAF9, whiteRaised.shellColors()[0]);
        assertEquals(0xFFFFFFFF, whiteRaised.rimColors()[0]);
        assertEquals(0xFFFFC17A, white.componentColors.quickVolumeThumbPressed);
        assertEquals(0xFFF08A2A, white.componentColors.batteryFill);
        assertEquals(0xFFFEF4E8, white.componentColors.batteryChargingBolt);
        assertEquals(0xFF717B81, white.componentColors.inputFaceIdleTop);
        assertEquals(0xFF566168, white.componentColors.inputFaceIdleBottom);
        assertEquals(0xFF7C868C, white.componentColors.inputFaceActiveTop);
        assertEquals(0xFF626C72, white.componentColors.inputFaceActiveBottom);
        assertEquals(0x22404A52, white.componentColors.inputDepthOuter);
        assertEquals(0xFFFBFAF8, white.componentColors.keyboard.keycapFaceTop);
        assertEquals(0xFFF5F4F1, white.componentColors.keyboard.keycapFaceBottom);
        assertEquals(0xFFE8DDD8, rosewood.cncColors.surface(
                ThemeCncColors.SHALLOW_INSET).roleColor);
        assertEquals(0xFFF4ECE8, rosewood.cncColors.surface(
                ThemeCncColors.CONTROL).roleColor);
        assertEquals(0xFFE9DDD7, rosewood.cncColors.surface(
                ThemeCncColors.FIELD).roleColor);
        assertEquals(0xFFD5C7C0, rosewood.cncColors.surface(
                ThemeCncColors.INPUT_FRAME).roleColor);
        assertEquals(0xFF76574D, rosewood.componentColors.batteryShell);
        assertEquals(0xFFC65D7B, rosewood.componentColors.batteryFill);
        assertEquals(0xFFFBF5F1, rosewood.componentColors.batteryChargingBolt);
        assertEquals(0xFF806F69, rosewood.componentColors.inputFaceIdleTop);
        assertEquals(0xFF66544E, rosewood.componentColors.inputFaceIdleBottom);
        assertEquals(rosewood.componentColors.inputFaceIdleTop,
                rosewood.componentColors.inputFaceActiveTop);
        assertEquals(rosewood.componentColors.inputFaceIdleBottom,
                rosewood.componentColors.inputFaceActiveBottom);
        assertEquals(0x2256443F, rosewood.componentColors.inputDepthOuter);
        assertEquals(0xFFECE8E1, celadon.cncColors.surface(
                ThemeCncColors.FLUSH).roleColor);
        assertEquals(0xFFF7F4EE, celadon.cncColors.surface(
                ThemeCncColors.RAISED).roleColor);
        assertEquals(0xFFD1D1C7, celadon.cncColors.surface(
                ThemeCncColors.INSET).roleColor);
        assertEquals(0xFFE6E2DA, celadon.cncColors.surface(
                ThemeCncColors.CONTROL).roleColor);
        assertEquals(0xFFE1DED7, celadon.cncColors.surface(
                ThemeCncColors.FIELD).roleColor);
        assertEquals(0xFF68716C, celadon.componentColors.batteryShell);
        assertEquals(0xFF6F947F, celadon.componentColors.batteryFill);
        assertEquals(0xFFF7F4EE, celadon.componentColors.batteryChargingBolt);
        assertEquals(0xFF71847A, celadon.componentColors.inputFaceIdleTop);
        assertEquals(0xFF586A61, celadon.componentColors.inputFaceIdleBottom);
        assertEquals(celadon.componentColors.inputFaceIdleTop,
                celadon.componentColors.inputFaceActiveTop);
        assertEquals(celadon.componentColors.inputFaceIdleBottom,
                celadon.componentColors.inputFaceActiveBottom);
        assertEquals(0x22545F5A, celadon.componentColors.inputDepthOuter);
        assertEquals(0xFF557B67, celadon.componentColors.keyboard.lockedText);
        assertEquals(0xE04EA1FF, blue.componentColors.dockIndicator);
        assertEquals(0xE0F08A2A, white.componentColors.dockIndicator);
        assertEquals(0xFF18212B, blue.componentColors.flatSurfaceFill);
        assertEquals(0xFFE4E6E7, white.componentColors.flatSurfaceFill);
        assertEquals(0x445F7C9A, blue.componentColors.structuralDivider);
        assertEquals(0x287B8792, white.componentColors.structuralDivider);
        assertEquals(0xFFD9E8F8, blue.componentColors.quickActionIconIdle);
        assertEquals(0xFF536274, white.componentColors.quickActionIconIdle);
        assertEquals(0xFF070A10, blue.componentColors.fullscreenBackground);
        assertEquals(0xFFD4DCE3, white.componentColors.fullscreenBackground);
        assertEquals(0xCC70B7FF, blue.componentColors.touchPointCore);
        assertEquals(0xCCF08A2A, white.componentColors.touchPointCore);
        assertEquals(0xAA70B7FF, blue.componentColors.canvasPressedEdge);
        assertEquals(0xC8F08A2A, white.componentColors.canvasPressedEdge);
        assertEquals(0xFF5FD18A, blue.componentColors.statusLampSuccess);
        assertEquals(0xFF5FD18A, amber.componentColors.statusLampSuccess);
        assertEquals(0xFF5FD18A, white.componentColors.statusLampSuccess);
        assertEquals(0xFFFF6B6B, blue.componentColors.statusLampError);
        assertEquals(0xFFFF6B6B, amber.componentColors.statusLampError);
        assertEquals(0xFFFF6B6B, white.componentColors.statusLampError);
        assertEquals(0xFFE6EDF3, blue.componentColors.keyboard.lockedText);
        assertEquals(0xFFE77F1F, white.componentColors.keyboard.lockedText);
        assertEquals(0xFF15243A, blue.componentColors.gridPreview(
                ThemeComponentColors.GRID_TOUCHPAD));
        assertEquals(0xFFD6DEE6, white.componentColors.gridPreview(
                ThemeComponentColors.GRID_TOUCHPAD));
        assertEquals(0xCC1D5A3B, blue.componentColors.gridSelected(
                ThemeComponentColors.GRID_QUICK_ACTIONS));
        assertEquals(0xFFE7E1D9, white.componentColors.gridSelected(
                ThemeComponentColors.GRID_QUICK_ACTIONS));
        assertFalse(blue.semanticStates.error.foreground
                == blue.semanticStates.recording.foreground);
        assertFalse(white.semanticStates.error.foreground
                == white.semanticStates.recording.foreground);
        assertEquals(blue.semanticStates.success.foreground,
                amber.semanticStates.success.foreground);
        assertEquals(blue.semanticStates.warning.foreground,
                amber.semanticStates.warning.foreground);
        assertEquals(blue.semanticStates.error.foreground,
                amber.semanticStates.error.foreground);
        assertEquals(blue.semanticStates.recording.foreground,
                amber.semanticStates.recording.foreground);
        assertEquals(blue.semanticStates.success.foreground,
                nocturne.semanticStates.success.foreground);
        assertEquals(blue.semanticStates.warning.foreground,
                nocturne.semanticStates.warning.foreground);
        assertEquals(blue.semanticStates.error.foreground,
                nocturne.semanticStates.error.foreground);
        assertEquals(blue.semanticStates.recording.foreground,
                nocturne.semanticStates.recording.foreground);
        assertFalse(amber.palette.accent == amber.semanticStates.error.foreground);
        assertFalse(amber.palette.accent == amber.semanticStates.recording.foreground);
        assertFalse(nocturne.palette.accent == nocturne.semanticStates.error.foreground);
        assertFalse(nocturne.palette.accent == nocturne.semanticStates.recording.foreground);
        assertEquals(0xFF3F8A66, rosewood.semanticStates.success.foreground);
        assertEquals(0xFFB4772E, rosewood.semanticStates.warning.foreground);
        assertEquals(0xFFC8493E, rosewood.semanticStates.error.foreground);
        assertEquals(0xFFD43D4B, rosewood.semanticStates.recording.foreground);
        assertFalse(rosewood.palette.accent == rosewood.semanticStates.error.foreground);
        assertFalse(rosewood.palette.accent == rosewood.semanticStates.recording.foreground);
        assertFalse(rosewood.semanticStates.error.foreground
                == rosewood.semanticStates.recording.foreground);
        assertEquals(rosewood.semanticStates.success.foreground,
                celadon.semanticStates.success.foreground);
        assertEquals(rosewood.semanticStates.warning.foreground,
                celadon.semanticStates.warning.foreground);
        assertEquals(rosewood.semanticStates.error.foreground,
                celadon.semanticStates.error.foreground);
        assertEquals(rosewood.semanticStates.recording.foreground,
                celadon.semanticStates.recording.foreground);
        assertFalse(celadon.palette.accent == celadon.semanticStates.error.foreground);
        assertFalse(celadon.palette.accent == celadon.semanticStates.recording.foreground);
        assertFalse(celadon.semanticStates.error.foreground
                == celadon.semanticStates.recording.foreground);

        android.content.SharedPreferences preferences =
                target.getSharedPreferences("heimdall_ui", Context.MODE_PRIVATE);
        boolean hadTheme = preferences.contains("theme");
        String previousTheme = preferences.getString("theme", null);
        try {
            assertTrue(preferences.edit().putString("theme", ThemeRegistry.LEGACY_PEARL)
                    .commit());
            assertEquals(ThemeRegistry.ID_FREYA_WHITE, HeimdallUi.theme(target));
            assertEquals(ThemeRegistry.LEGACY_PEARL,
                    preferences.getString("theme", null));

            assertTrue(preferences.edit().putString("theme", "future.unknown").commit());
            assertEquals(ThemeRegistry.ID_HEIMDALL_BLUE, HeimdallUi.theme(target));
            assertEquals("future.unknown", preferences.getString("theme", null));

            HeimdallUi.setTheme(target, ThemeRegistry.LEGACY_PEARL);
            assertEquals(ThemeRegistry.ID_FREYA_WHITE,
                    preferences.getString("theme", null));

            HeimdallUi.setTheme(target, ThemeRegistry.ID_FREYA_ROSEWOOD);
            assertEquals(ThemeRegistry.ID_FREYA_ROSEWOOD,
                    preferences.getString("theme", null));

            HeimdallUi.setTheme(target, ThemeRegistry.ID_HEIMDALL_AMBER);
            assertEquals(ThemeRegistry.ID_HEIMDALL_AMBER,
                    preferences.getString("theme", null));

            HeimdallUi.setTheme(target, ThemeRegistry.ID_HEIMDALL_NOCTURNE);
            assertEquals(ThemeRegistry.ID_HEIMDALL_NOCTURNE,
                    preferences.getString("theme", null));

            HeimdallUi.setTheme(target, ThemeRegistry.ID_FREYA_CELADON);
            assertEquals(ThemeRegistry.ID_FREYA_CELADON,
                    preferences.getString("theme", null));
        } finally {
            HeimdallUi.clearActiveProfileTheme();
            android.content.SharedPreferences.Editor restore = preferences.edit();
            if (hadTheme) {
                restore.putString("theme", previousTheme);
            } else {
                restore.remove("theme");
            }
            assertTrue(restore.commit());
        }
    }

    public void testProfileThemeBindingContract() throws Exception {
        JSONObject legacyJson = new JSONObject();
        legacyJson.put("name", "Legacy theme");
        legacyJson.put("mode", "generic");
        GameProfile legacy = GameProfile.fromJson(legacyJson);
        assertEquals("", legacy.normalizedThemeId());
        assertFalse(legacy.toJson().has("themeId"));
        assertEquals(ThemeRegistry.ID_FREYA_WHITE,
                legacy.effectiveThemeId(ThemeRegistry.LEGACY_PEARL));

        legacy.setThemeId(ThemeRegistry.ID_FREYA_CELADON);
        JSONObject saved = legacy.toJson();
        assertEquals(ThemeRegistry.ID_FREYA_CELADON, saved.getString("themeId"));
        assertEquals(ThemeRegistry.ID_FREYA_CELADON,
                GameProfile.fromJson(saved).normalizedThemeId());

        legacy.setThemeId(ThemeRegistry.LEGACY_PEARL);
        assertEquals(ThemeRegistry.ID_FREYA_WHITE, legacy.normalizedThemeId());
        legacy.setThemeId("future.unknown");
        assertEquals("", legacy.normalizedThemeId());

        android.content.SharedPreferences preferences =
                target.getSharedPreferences("heimdall_ui", Context.MODE_PRIVATE);
        boolean hadTheme = preferences.contains("theme");
        String previousTheme = preferences.getString("theme", null);
        try {
            HeimdallUi.clearActiveProfileTheme();
            assertTrue(preferences.edit().putString("theme",
                    ThemeRegistry.ID_HEIMDALL_BLUE).commit());
            assertEquals(ThemeRegistry.ID_HEIMDALL_BLUE, HeimdallUi.globalTheme(target));
            assertEquals(ThemeRegistry.ID_HEIMDALL_BLUE, HeimdallUi.theme(target));

            HeimdallUi.setActiveProfileTheme(ThemeRegistry.ID_FREYA_CELADON);
            assertEquals(ThemeRegistry.ID_FREYA_CELADON, HeimdallUi.theme(target));
            assertEquals(ThemeRegistry.ID_HEIMDALL_BLUE, HeimdallUi.globalTheme(target));
            assertEquals(ThemeRegistry.ID_HEIMDALL_BLUE,
                    preferences.getString("theme", null));

            HeimdallUi.clearActiveProfileTheme();
            assertEquals(ThemeRegistry.ID_HEIMDALL_BLUE, HeimdallUi.theme(target));
        } finally {
            HeimdallUi.clearActiveProfileTheme();
            android.content.SharedPreferences.Editor restore = preferences.edit();
            if (hadTheme) {
                restore.putString("theme", previousTheme);
            } else {
                restore.remove("theme");
            }
            assertTrue(restore.commit());
        }
    }

    public void testInteractiveMapBrowserSettingsContract() throws Exception {
        assertEquals(InteractiveMapBrowserSettings.MODE_MOBILE,
                InteractiveMapBrowserSettings.normalize(null));
        assertEquals(InteractiveMapBrowserSettings.MODE_MOBILE,
                InteractiveMapBrowserSettings.normalize("unknown"));
        assertEquals(InteractiveMapBrowserSettings.MODE_DESKTOP,
                InteractiveMapBrowserSettings.normalize(" DESKTOP "));

        String mobileUserAgent = "Mozilla/5.0 (Linux; Android 15; Thor Build/AP3A; wv) "
                + "AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 "
                + "Chrome/140.0.0.0 Mobile Safari/537.36";
        String desktopUserAgent = InteractiveMapBrowserSettings.desktopUserAgent(mobileUserAgent);
        assertTrue(desktopUserAgent.contains("(X11; Linux x86_64)"));
        assertTrue(desktopUserAgent.contains("Chrome/140.0.0.0"));
        assertFalse(desktopUserAgent.contains("Android"));
        assertFalse(desktopUserAgent.contains("Version/4.0"));
        assertFalse(desktopUserAgent.contains("Mobile"));

        JSONObject legacy = new JSONObject();
        legacy.put("name", "Legacy map");
        legacy.put("mode", "generic");
        GameProfile legacyProfile = GameProfile.fromJson(legacy);
        assertEquals(InteractiveMapBrowserSettings.MODE_MOBILE,
                legacyProfile.interactiveMapBrowserMode);

        legacyProfile.interactiveMapTitle = "World map";
        legacyProfile.interactiveMapUrl = "https://example.com/map";
        legacyProfile.interactiveMapBrowserMode = InteractiveMapBrowserSettings.MODE_DESKTOP;
        GameProfile restored = GameProfile.fromJson(legacyProfile.toJson());
        assertEquals(InteractiveMapBrowserSettings.MODE_DESKTOP,
                restored.interactiveMapBrowserMode);
        assertEquals("https://example.com/map", restored.interactiveMapUrl);
    }

    public void testUpperDisplaySingleTouchHandoffCoordinateContract() {
        Point thorUpper =
                UpperDisplaySingleTouchHandoff.resolveMirroredLowerRightPoint(1920, 1080);
        assertNotNull(thorUpper);
        assertEquals(1918, thorUpper.x);
        assertEquals(1079, thorUpper.y);
        assertNull(UpperDisplaySingleTouchHandoff.resolveMirroredLowerRightPoint(2, 1080));
        assertNull(UpperDisplaySingleTouchHandoff.resolveMirroredLowerRightPoint(1920, 1));
    }

    public void testAssistantActivitySingleTaskContract() throws Exception {
        ActivityInfo assistantInfo = target.getPackageManager().getActivityInfo(
                new ComponentName(target, AssistantActivity.class), 0);
        ActivityInfo routerInfo = target.getPackageManager().getActivityInfo(
                new ComponentName(target, HeimdallLaunchActivity.class), 0);
        ActivityInfo handoffInfo = target.getPackageManager().getActivityInfo(
                new ComponentName(target, UpperDisplayFocusHandoffActivity.class), 0);
        assertEquals(ActivityInfo.LAUNCH_SINGLE_TASK, assistantInfo.launchMode);
        assertEquals(target.getPackageName(), assistantInfo.taskAffinity);
        assertEquals(target.getPackageName() + ".launch_router", routerInfo.taskAffinity);
        assertEquals(target.getPackageName() + ".focushandoff", handoffInfo.taskAffinity);
        assertFalse(handoffInfo.exported);
        assertTrue((handoffInfo.flags & ActivityInfo.FLAG_EXCLUDE_FROM_RECENTS) != 0);
        assertTrue((handoffInfo.flags & ActivityInfo.FLAG_NO_HISTORY) != 0);
        assertFalse(assistantInfo.taskAffinity.equals(routerInfo.taskAffinity));
    }

    public void testUpperDisplayStartedLifecycleHandoffContract() {
        UpperDisplayStartedLifecycleHandoff handoff =
                new UpperDisplayStartedLifecycleHandoff();
        int firstGeneration = handoff.claim();
        assertEquals(0, firstGeneration);
        assertTrue(handoff.isCurrent(firstGeneration));
        assertEquals(UpperDisplayStartedLifecycleHandoff.NO_CLAIM, handoff.claim());

        handoff.rearmAfterStop();
        assertFalse(handoff.isCurrent(firstGeneration));
        int secondGeneration = handoff.claim();
        assertEquals(1, secondGeneration);
        assertTrue(handoff.isCurrent(secondGeneration));
        assertEquals(UpperDisplayStartedLifecycleHandoff.NO_CLAIM, handoff.claim());
    }

    public void testSecondaryDisplayLaunchRouterContract() {
        assertEquals(4, HeimdallLaunchRouter.selectTargetDisplayId(
                0, new int[] {0, 4},
                new int[] {android.view.Display.STATE_ON,
                        android.view.Display.STATE_ON}));
        assertEquals(4, HeimdallLaunchRouter.selectTargetDisplayId(
                4, new int[] {0, 4, 7},
                new int[] {android.view.Display.STATE_ON,
                        android.view.Display.STATE_ON,
                        android.view.Display.STATE_ON}));
        assertEquals(7, HeimdallLaunchRouter.selectTargetDisplayId(
                0, new int[] {0, 4, 7},
                new int[] {android.view.Display.STATE_ON,
                        android.view.Display.STATE_OFF,
                        android.view.Display.STATE_ON}));
        assertEquals(android.view.Display.INVALID_DISPLAY,
                HeimdallLaunchRouter.selectTargetDisplayId(
                        0, new int[] {0},
                        new int[] {android.view.Display.STATE_ON}));
        assertEquals(android.view.Display.INVALID_DISPLAY,
                HeimdallLaunchRouter.selectTargetDisplayId(
                        0, new int[] {0, 4},
                        new int[] {android.view.Display.STATE_ON}));
    }

    public void testStartupCapabilityDefaultContract() {
        assertTrue(ForegroundAppTracker.shouldAutoEnable(false, true, true));
        assertFalse(ForegroundAppTracker.shouldAutoEnable(true, true, true));
        assertFalse(ForegroundAppTracker.shouldAutoEnable(false, false, true));
        assertFalse(ForegroundAppTracker.shouldAutoEnable(false, true, false));
    }

    public void testKeyboardPadModelContract() throws Exception {
        WidgetLayout layout = WidgetLayout.defaultLayout();
        WidgetLayout.Item item = new WidgetLayout.Item(
                WidgetLayout.TYPE_KEYBOARD_PAD, 0, 0, 3, 4);
        KeyboardPad pad = KeyboardPad.defaultPad();
        pad.columns = 4;
        pad.rows = 3;
        KeyboardPad.Key key = pad.keys.get(0);
        key.binding.linuxKeyCode = KeyboardKeyCatalog.KEY_ENTER;
        key.binding.ctrl = true;
        key.behavior = KeyboardPad.BEHAVIOR_PRESS;
        key.display.label = "ACCEPT";
        key.display.iconKey = "builtin:interact";
        key.geometry.x = 2;
        key.geometry.y = 1;
        key.geometry.w = 2;
        key.geometry.h = 1;
        item.keyboardPad = pad;
        KeyboardPad.Key actionKey = pad.keys.get(1);
        actionKey.actionType = KeyboardPad.ACTION_HEIMDALL;
        actionKey.heimdallAction = HeimdallActionCatalog.ACTION_OPEN_VIRTUAL_KEYBOARD;
        actionKey.display.label = "Keyboard";
        layout.items.add(item);

        WidgetLayout restored = WidgetLayout.fromJson(layout.toJson());
        WidgetLayout.Item restoredItem = restored.findItem(WidgetLayout.TYPE_KEYBOARD_PAD);
        assertNotNull(restoredItem);
        KeyboardPad.Key restoredKey = restoredItem.safeKeyboardPad().keys.get(0);
        assertEquals(KeyboardKeyCatalog.KEY_ENTER, restoredKey.binding.linuxKeyCode);
        assertTrue(restoredKey.binding.ctrl);
        assertEquals(KeyboardPad.BEHAVIOR_PRESS, restoredKey.behavior);
        assertEquals("ACCEPT", restoredKey.display.label);
        assertEquals("builtin:interact", restoredKey.display.iconKey);
        assertEquals(2, restoredKey.geometry.x);
        assertEquals(1, restoredKey.geometry.y);
        assertEquals(2, restoredKey.geometry.w);
        KeyboardPad.Key restoredActionKey = restoredItem.safeKeyboardPad().keys.get(1);
        assertEquals(KeyboardPad.ACTION_HEIMDALL, restoredActionKey.actionType);
        assertEquals(HeimdallActionCatalog.ACTION_OPEN_VIRTUAL_KEYBOARD,
                restoredActionKey.heimdallAction);
        assertTrue(restoredActionKey.isHeimdallAction());

        JSONObject legacyPadItem = new JSONObject();
        legacyPadItem.put("type", WidgetLayout.TYPE_KEYBOARD_PAD);
        legacyPadItem.put("x", 0);
        legacyPadItem.put("y", 0);
        legacyPadItem.put("w", 3);
        legacyPadItem.put("h", 4);
        WidgetLayout.Item restoredLegacyItem = WidgetLayout.Item.fromJson(legacyPadItem);
        assertEquals(4, restoredLegacyItem.safeKeyboardPad().keys.size());
        assertEquals(KeyboardPad.LAYOUT_HORIZONTAL,
                restoredLegacyItem.safeKeyboardPad().layoutMode);
        assertEquals(KeyboardPad.BEHAVIOR_WHILE_HELD,
                restoredLegacyItem.safeKeyboardPad().keys.get(0).behavior);
        assertEquals(KeyboardPad.ACTION_KEYBOARD_BINDING,
                restoredLegacyItem.safeKeyboardPad().keys.get(0).actionType);

        KeyboardPad compact = KeyboardPad.defaultPad();
        compact.resizeKeyCount(3);
        assertEquals(3, compact.keys.size());
        assertEquals(3, compact.columns);
        assertEquals(1, compact.rows);
        compact.resizeKeyCount(8);
        assertEquals(8, compact.keys.size());
        assertEquals(3, compact.columns);
        assertEquals(3, compact.rows);

        compact.setLayoutMode(KeyboardPad.LAYOUT_VERTICAL);
        assertEquals(KeyboardPad.LAYOUT_VERTICAL, compact.layoutMode);
        assertEquals(1, compact.columns);
        assertEquals(8, compact.rows);
        for (int index = 0; index < compact.keys.size(); index++) {
            assertEquals(0, compact.keys.get(index).geometry.x);
            assertEquals(index, compact.keys.get(index).geometry.y);
        }
        compact.resizeKeyCount(12);
        assertEquals(1, compact.columns);
        assertEquals(12, compact.rows);

        KeyboardPad restoredVertical = KeyboardPad.fromJson(compact.toJson());
        assertEquals(KeyboardPad.LAYOUT_VERTICAL, restoredVertical.layoutMode);
        assertEquals(1, restoredVertical.columns);
        assertEquals(12, restoredVertical.rows);
        restoredVertical.setLayoutMode(KeyboardPad.LAYOUT_HORIZONTAL);
        assertEquals(4, restoredVertical.columns);
        assertEquals(3, restoredVertical.rows);
    }

    public void testQuickActionsModelContract() throws Exception {
        JSONObject legacyItemJson = new JSONObject();
        legacyItemJson.put("type", WidgetLayout.TYPE_QUICK_ACTIONS);
        legacyItemJson.put("x", 0);
        legacyItemJson.put("y", 0);
        legacyItemJson.put("w", 3);
        legacyItemJson.put("h", 3);
        WidgetLayout.Item legacyItem = WidgetLayout.Item.fromJson(legacyItemJson);
        QuickActionsConfig legacy = legacyItem.safeQuickActions();
        assertEquals(2, legacy.actions.size());
        assertEquals(HeimdallActionCatalog.ACTION_SCREENSHOT, legacy.actionAt(0));
        assertEquals(HeimdallActionCatalog.ACTION_SCREEN_RECORDING, legacy.actionAt(1));
        assertTrue(legacy.mediaVolume);

        WidgetLayout.Item newItem = new WidgetLayout.Item(
                WidgetLayout.TYPE_QUICK_ACTIONS, 0, 0, 3, 3);
        assertEquals(HeimdallActionCatalog.ACTION_OPEN_VIRTUAL_KEYBOARD,
                newItem.safeQuickActions().actionAt(2));
        newItem.safeQuickActions().setActionAt(0,
                HeimdallActionCatalog.ACTION_OPEN_VIRTUAL_KEYBOARD);
        newItem.safeQuickActions().setActionAt(1,
                HeimdallActionCatalog.ACTION_SCREENSHOT);
        newItem.safeQuickActions().setActionAt(2, HeimdallActionCatalog.ACTION_NONE);
        newItem.safeQuickActions().mediaVolume = false;
        WidgetLayout.Item restored = WidgetLayout.Item.fromJson(newItem.toJson());
        assertEquals(2, restored.safeQuickActions().actions.size());
        assertEquals(HeimdallActionCatalog.ACTION_OPEN_VIRTUAL_KEYBOARD,
                restored.safeQuickActions().actionAt(0));
        assertEquals(HeimdallActionCatalog.ACTION_SCREENSHOT,
                restored.safeQuickActions().actionAt(1));
        assertFalse(restored.safeQuickActions().mediaVolume);
        assertEquals(HeimdallActionCatalog.ACTION_NONE,
                HeimdallActionCatalog.normalizeQuickAction("arbitrary_intent"));
    }

    public void testWidgetLayoutIdentityContract() throws Exception {
        WidgetLayout layout = WidgetLayout.defaultLayout();
        layout.sanitize();
        assertEquals(WidgetLayout.CURRENT_SCHEMA_VERSION, layout.schemaVersion);
        assertEquals(12, layout.columns);
        assertEquals(8, layout.rows);
        assertEquals(WidgetLayout.CURRENT_SCHEMA_VERSION,
                layout.toJson().getInt("schemaVersion"));

        WidgetLayout.Item source = layout.items.get(0);
        assertTrue(source.itemId.trim().length() > 0);
        assertEquals(source.itemId, source.copy().itemId);
        assertFalse(source.itemId.equals(new WidgetLayout.Item(
                source.type, source.x, source.y, source.w, source.h).itemId));

        WidgetLayout restored = WidgetLayout.fromJson(layout.toJson());
        assertEquals(source.itemId, restored.items.get(0).itemId);
        assertEquals(source, layout.findItemById(source.itemId));

        JSONObject legacyJson = layout.toJson();
        legacyJson.remove("schemaVersion");
        JSONArray legacyItems = legacyJson.getJSONArray("items");
        for (int i = 0; i < legacyItems.length(); i++) {
            legacyItems.getJSONObject(i).remove("itemId");
        }
        WidgetLayout legacyFirst = WidgetLayout.fromJson(legacyJson);
        WidgetLayout legacySecond = WidgetLayout.fromJson(legacyJson);
        assertEquals(WidgetLayout.CURRENT_SCHEMA_VERSION, legacyFirst.schemaVersion);
        assertEquals(legacyFirst.items.get(0).itemId, legacySecond.items.get(0).itemId);
        assertFalse(legacyFirst.items.get(0).itemId.equals(legacyFirst.items.get(1).itemId));

        JSONObject sixByEightJson = new JSONObject();
        sixByEightJson.put("preset", WidgetLayout.PRESET_CUSTOM);
        sixByEightJson.put("columns", 6);
        sixByEightJson.put("rows", 8);
        JSONArray sixByEightItems = new JSONArray();
        JSONObject legacyMagnifier = new JSONObject();
        legacyMagnifier.put("type", WidgetLayout.TYPE_MAGNIFIER);
        legacyMagnifier.put("x", 1);
        legacyMagnifier.put("y", 2);
        legacyMagnifier.put("w", 3);
        legacyMagnifier.put("h", 4);
        legacyMagnifier.put("magnifierShape", WidgetLayout.MAGNIFIER_SHAPE_CIRCLE);
        sixByEightItems.put(legacyMagnifier);
        sixByEightJson.put("items", sixByEightItems);
        WidgetLayout migrated = WidgetLayout.fromJson(sixByEightJson);
        WidgetLayout.Item migratedMagnifier = migrated.items.get(0);
        assertEquals(12, migrated.columns);
        assertEquals(8, migrated.rows);
        assertEquals(2, migratedMagnifier.x);
        assertEquals(2, migratedMagnifier.y);
        assertEquals(6, migratedMagnifier.w);
        assertEquals(4, migratedMagnifier.h);
        assertEquals(WidgetLayout.MAGNIFIER_SHAPE_CIRCLE,
                migratedMagnifier.magnifierShape);

        WidgetLayout equivalentPreset = WidgetLayout.defaultLayout();
        assertTrue(layout.hasSameContent(equivalentPreset));
        assertTrue(equivalentPreset.adoptItemIdsFromEquivalent(layout));
        assertEquals(layout.items.get(0).itemId, equivalentPreset.items.get(0).itemId);
        equivalentPreset.items.get(0).x += 1;
        assertFalse(layout.hasSameContent(equivalentPreset));

        JSONObject duplicateIdJson = layout.toJson();
        JSONArray duplicateItems = duplicateIdJson.getJSONArray("items");
        duplicateItems.getJSONObject(1).put("itemId",
                duplicateItems.getJSONObject(0).getString("itemId"));
        WidgetLayout repaired = WidgetLayout.fromJson(duplicateIdJson);
        assertFalse(repaired.items.get(0).itemId.equals(repaired.items.get(1).itemId));
    }

    public void testTranslationWidgetModelContract() throws Exception {
        WidgetLayout layout = new WidgetLayout();
        layout.items.clear();
        WidgetLayout.Item first = new WidgetLayout.Item(
                WidgetLayout.TYPE_TRANSLATION, 0, 0, 6, 3);
        first.translationConfig.regionLeft = -1f;
        first.translationConfig.regionRight = 2f;
        first.translationConfig.ocrScript = TranslationConfig.SCRIPT_KOREAN;
        first.translationConfig.targetLanguage = TranslationConfig.LANGUAGE_ENGLISH;
        layout.items.add(first);
        layout.items.add(new WidgetLayout.Item(
                WidgetLayout.TYPE_TRANSLATION, 6, 0, 6, 3));
        layout.sanitize();
        assertEquals(1, layout.items.size());
        assertEquals(WidgetLayout.TYPE_TRANSLATION, layout.items.get(0).type);
        assertEquals(0f, layout.items.get(0).safeTranslation().regionLeft);
        assertEquals(1f, layout.items.get(0).safeTranslation().regionRight);

        WidgetLayout restored = WidgetLayout.fromJson(layout.toJson());
        assertEquals(TranslationConfig.SCRIPT_KOREAN,
                restored.items.get(0).safeTranslation().ocrScript);
        assertEquals(TranslationConfig.LANGUAGE_ENGLISH,
                restored.items.get(0).safeTranslation().targetLanguage);

        TranslationTextStabilizer stabilizer = new TranslationTextStabilizer();
        assertEquals(null, stabilizer.accept("  Hello\n world "));
        assertTrue(stabilizer.isAwaitingConfirmation());
        assertEquals("Hello world", stabilizer.accept("Hello   world"));
        assertFalse(stabilizer.isAwaitingConfirmation());
        stabilizer.markTranslated("Hello world");
        assertEquals(null, stabilizer.accept("Hello world"));
        assertFalse(stabilizer.isAwaitingConfirmation());
        assertTrue(TranslationTextStabilizer.isSimilar("Subtitle!", "Subtitle!", 0.90f));
        assertEquals(null, stabilizer.accept("Hello there"));
        assertTrue(stabilizer.isAwaitingConfirmation());
        assertEquals("Hello there", stabilizer.accept("Hello there"));

        TranslationTextStabilizer punctuationJitter = new TranslationTextStabilizer();
        assertEquals(null, punctuationJitter.accept("\u300c\u884c\u3053\u3046\u3002\u300d"));
        assertEquals("\u884c\u3053\u3046", punctuationJitter.accept("\u884c\u3053\u3046"));

        TranslationTextStabilizer characterJitter = new TranslationTextStabilizer();
        assertEquals(null, characterJitter.accept("\u5f7c\u306f\u3053\u3053\u306b\u3044\u308b"));
        assertEquals("\u5f7c\u306f\u3053\u3053\u306b\u3044\u308d",
                characterJitter.accept("\u5f7c\u306f\u3053\u3053\u306b\u3044\u308d"));

        TranslationTextStabilizer growingSubtitle = new TranslationTextStabilizer();
        assertEquals(null, growingSubtitle.accept("The"));
        assertEquals(null, growingSubtitle.accept("The hero"));
        assertTrue(growingSubtitle.isAwaitingConfirmation());
        assertEquals("The hero", growingSubtitle.accept("The hero"));

        assertEquals("Speaker\nDialogue line",
                TranslationTextStabilizer.normalize(
                        "  Speaker  \r\n\r\n Dialogue   line "));
        assertEquals("Speaker Dialogue line",
                TranslationApiClient.prepareSourceText("Speaker\nDialogue line"));

        assertTrue(TranslationTextStabilizer.isTranslationDuplicate(
                "\u30c7\u30a3\u30b1\n\u304a\u30fc\u3044 \u30d5\u30ea\u30c3\u30c8!",
                "\u30c7\u30a3\u30b1\n\u304a\u30fc\u3044\u3001\u30d5\u30ea\u30c3\u30c8！"));
        assertTrue(TranslationTextStabilizer.isTranslationDuplicate(
                "Name\nDialogue line", "Dialogue line\nName"));
        assertFalse(TranslationTextStabilizer.isTranslationDuplicate(
                "Open the door", "Return to the village"));
        TranslationRuntimeController.Snapshot runtimeSnapshot =
                new TranslationRuntimeController.Snapshot(
                        "  Same subtitle  ", "  Same translation  ");
        assertEquals("Same subtitle", runtimeSnapshot.sourceText);
        assertEquals("Same translation", runtimeSnapshot.translatedText);
        TranslationTextStabilizer resumedStabilizer = new TranslationTextStabilizer();
        resumedStabilizer.markTranslated(runtimeSnapshot.sourceText);
        assertEquals(null, resumedStabilizer.accept("Same subtitle"));
        assertEquals(null, resumedStabilizer.accept("Same subtitle"));
        assertFalse(TranslationRuntimeController.shouldSurfaceOcrError(1));
        assertFalse(TranslationRuntimeController.shouldSurfaceOcrError(2));
        assertTrue(TranslationRuntimeController.shouldSurfaceOcrError(3));
        assertFalse(TranslationRuntimeController.shouldReplaceActiveRequest(
                "Noisy OCR", "Current subtitle", false));
        assertFalse(TranslationRuntimeController.shouldReplaceActiveRequest(
                "Current subtitle!", "Current subtitle", true));
        assertTrue(TranslationRuntimeController.shouldReplaceActiveRequest(
                "Next subtitle", "Current subtitle", true));
        assertEquals(1800L, HeimdallInteraction.EDIT_LONG_PRESS_TIMEOUT_MS);

        TranslationProviderConfig provider = new TranslationProviderConfig();
        provider.region = TranslationProviderConfig.REGION_CHINA;
        assertEquals(TranslationProviderConfig.SILICONFLOW_CHINA_BASE_URL,
                provider.resolvedBaseUrl());
        assertEquals(TranslationProviderConfig.SILICONFLOW_CHINA_MODEL,
                provider.resolvedModel());
        provider.region = TranslationProviderConfig.REGION_GLOBAL;
        assertEquals(TranslationProviderConfig.SILICONFLOW_GLOBAL_BASE_URL,
                provider.resolvedBaseUrl());
        assertEquals(TranslationProviderConfig.SILICONFLOW_GLOBAL_MODEL,
                provider.resolvedModel());

        GameProfile profile = new GameProfile("Translation", "General", "", 1,
                Collections.singletonList(new Macro("M1", ProfileStore.steps("wait:80ms"))));
        profile.widgetLayout = layout;
        String profileJson = profile.toJson().toString();
        assertTrue(profileJson.contains("profileId"));
        assertFalse(profileJson.contains("apiKey"));
        assertFalse(profileJson.contains("Hunyuan-MT-7B"));

        JSONObject duplicate = profile.toJson();
        JSONArray profiles = new JSONArray();
        profiles.put(duplicate);
        profiles.put(new JSONObject(duplicate.toString()));
        List<GameProfile> imported = ProfileStore.profilesFromJson(profiles.toString());
        assertFalse(imported.get(0).safeProfileId().equals(imported.get(1).safeProfileId()));
    }

    public void testHardwareMonitorModelContract() throws Exception {
        WidgetLayout layout = new WidgetLayout();
        layout.items.clear();
        layout.items.add(new WidgetLayout.Item(
                WidgetLayout.TYPE_HARDWARE_MONITOR, 0, 0, 4, 2));
        layout.items.add(new WidgetLayout.Item(
                WidgetLayout.TYPE_HARDWARE_MONITOR, 4, 0, 4, 2));
        layout.sanitize();
        assertEquals(1, layout.items.size());
        assertEquals(WidgetLayout.TYPE_HARDWARE_MONITOR, layout.items.get(0).type);
        WidgetLayout restored = WidgetLayout.fromJson(layout.toJson());
        assertEquals(WidgetLayout.TYPE_HARDWARE_MONITOR, restored.items.get(0).type);

        assertEquals(Float.valueOf(58.125f),
                HardwareMonitorSampler.convertMilliCelsius("58125"));
        assertNull(HardwareMonitorSampler.convertMilliCelsius("battery"));
        assertNull(HardwareMonitorSampler.convertMilliCelsius("200000"));

        File thermalRoot = new File(target.getCacheDir(),
                "hardware-monitor-contract-" + System.nanoTime());
        File batteryZone = new File(thermalRoot, "thermal_zone1");
        File cpuZone = new File(thermalRoot, "thermal_zone48");
        assertTrue(batteryZone.mkdirs());
        assertTrue(cpuZone.mkdirs());
        File batteryType = new File(batteryZone, "type");
        File batteryTemp = new File(batteryZone, "temp");
        File cpuType = new File(cpuZone, "type");
        File cpuTemp = new File(cpuZone, "temp");
        writeFile(batteryType, "battery\n".getBytes(StandardCharsets.US_ASCII));
        writeFile(batteryTemp, "31000\n".getBytes(StandardCharsets.US_ASCII));
        writeFile(cpuType, "cpu-0-1\n".getBytes(StandardCharsets.US_ASCII));
        writeFile(cpuTemp, "58125\n".getBytes(StandardCharsets.US_ASCII));
        assertEquals(cpuTemp.getCanonicalPath(),
                HardwareMonitorSampler.findCpuTemperatureFile(thermalRoot).getCanonicalPath());
        assertEquals(Float.valueOf(58.125f),
                HardwareMonitorSampler.readTemperatureCelsius(cpuTemp));

        File duplicateZone = new File(thermalRoot, "thermal_zone49");
        assertTrue(duplicateZone.mkdirs());
        File duplicateType = new File(duplicateZone, "type");
        File duplicateTemp = new File(duplicateZone, "temp");
        writeFile(duplicateType, "cpu-0-1\n".getBytes(StandardCharsets.US_ASCII));
        writeFile(duplicateTemp, "59000\n".getBytes(StandardCharsets.US_ASCII));
        assertNull(HardwareMonitorSampler.findCpuTemperatureFile(thermalRoot));

        assertTrue(duplicateTemp.delete());
        assertTrue(duplicateType.delete());
        assertTrue(duplicateZone.delete());
        assertTrue(cpuTemp.delete());
        assertTrue(cpuType.delete());
        assertTrue(cpuZone.delete());
        assertTrue(batteryTemp.delete());
        assertTrue(batteryType.delete());
        assertTrue(batteryZone.delete());
        assertTrue(thermalRoot.delete());
    }

    public void testProfileIconDecodePolicy() {
        Rect landscape = ProfileIconView.centeredSquareBounds(4096, 512);
        assertEquals(512, landscape.width());
        assertEquals(512, landscape.height());
        assertEquals(1792, landscape.left);
        assertEquals(0, landscape.top);
        assertEquals(2, ProfileIconView.sampleSizeForTarget(
                landscape.width(), 256));

        Rect portrait = ProfileIconView.centeredSquareBounds(512, 4096);
        assertEquals(512, portrait.width());
        assertEquals(512, portrait.height());
        assertEquals(0, portrait.left);
        assertEquals(1792, portrait.top);
        assertEquals(1, ProfileIconView.sampleSizeForTarget(240, 256));
        assertTrue(ProfileIconView.centeredSquareBounds(0, 512).isEmpty());
    }

    public void testUserMacroIconDeletionContract() throws Exception {
        assertEquals(4, MacroIconRepository.importSampleSize(4096, 4096));
        assertEquals(2, MacroIconRepository.importSampleSize(4096, 512));
        assertEquals(1, MacroIconRepository.importSampleSize(4096, 64));

        File directory = new File(target.getFilesDir(), "macro_icons");
        assertTrue(directory.exists() || directory.mkdirs());
        File icon = new File(directory, "delete_contract_" + System.nanoTime() + ".png");
        writeFile(icon, pngFixture());
        String key = "user:" + icon.getName();

        assertTrue(MacroIconRepository.isUserIconKey(key));
        assertFalse(MacroIconRepository.isUserIconKey("asset:" + icon.getName()));
        assertFalse(MacroIconRepository.isUserIconKey("user:../" + icon.getName()));
        assertFalse(MacroIconRepository.deleteUserIcon(
                target, "user:../" + icon.getName()));
        assertTrue(icon.isFile());
        assertTrue(MacroIconRepository.deleteUserIcon(target, key));
        assertFalse(icon.exists());
        assertFalse(MacroIconRepository.deleteUserIcon(target, key));
    }

    public void testCanvasRuntimeDecodePolicy() {
        assertEquals(256, CanvasImageLoader.runtimeDecodeMaxSide(0, 0, 1f));
        assertEquals(1200, CanvasImageLoader.runtimeDecodeMaxSide(600, 400, 1f));
        assertEquals(1500, CanvasImageLoader.runtimeDecodeMaxSide(250, 200, 3f));
        assertEquals(2000, CanvasImageLoader.runtimeDecodeMaxSide(250, 200, 4f));
        assertEquals(2048, CanvasImageLoader.runtimeDecodeMaxSide(600, 400, 2f));
        assertEquals(2048, CanvasImageLoader.runtimeDecodeMaxSide(250, 200, 8f));
        assertEquals(2048, CanvasImageLoader.runtimeDecodeMaxSide(250, 200, 24f));
        assertEquals(1200, CanvasImageLoader.runtimeDecodeMaxSide(
                600, 400, Float.NaN));
    }

    public void testCanvasExtremeAspectFillPolicy() throws Exception {
        assertEquals(Float.valueOf(1f), Float.valueOf(
                CanvasCompositionMath.coverZoom(1200, 100, 1200, 100)));
        assertEquals(Float.valueOf(12f), Float.valueOf(
                CanvasCompositionMath.coverZoom(1200, 100, 1000, 1000)));
        assertEquals(Float.valueOf(24f), Float.valueOf(
                CanvasCompositionMath.coverZoom(1200, 100, 100, 200)));
        assertEquals(Float.valueOf(CanvasConfig.MAX_GESTURE_ZOOM), Float.valueOf(
                CanvasCompositionMath.gestureMaximumZoom(1200, 100, 1200, 100)));
        assertEquals(Float.valueOf(24f), Float.valueOf(
                CanvasCompositionMath.gestureMaximumZoom(1200, 100, 1000, 1000)));
        assertEquals(Float.valueOf(48f), Float.valueOf(
                CanvasCompositionMath.gestureMaximumZoom(1200, 100, 100, 200)));

        CanvasConfig extremeFill = new CanvasConfig();
        extremeFill.zoom = 24f;
        CanvasConfig restored = CanvasConfig.fromJson(extremeFill.toJson());
        assertEquals(Float.valueOf(24f), Float.valueOf(restored.zoom));

        extremeFill.zoom = Float.MAX_VALUE;
        extremeFill.normalize();
        assertEquals(Float.valueOf(CanvasConfig.MAX_COMPOSITION_ZOOM),
                Float.valueOf(extremeFill.zoom));
    }

    public void testAdvancedControlsStateContract() {
        assertEquals(InputBridge.AdvancedControlsState.SHIZUKU_STOPPED,
                InputBridge.resolveAdvancedControlsState(false,
                        true, true, true, false));
        assertEquals(InputBridge.AdvancedControlsState.AUTHORIZATION_REQUIRED,
                InputBridge.resolveAdvancedControlsState(true,
                        false, true, true, false));
        assertEquals(InputBridge.AdvancedControlsState.AUTHORIZED,
                InputBridge.resolveAdvancedControlsState(true,
                        true, false, true, false));
        assertEquals(InputBridge.AdvancedControlsState.AUTHORIZED,
                InputBridge.resolveAdvancedControlsState(true,
                        true, true, false, false));
        assertEquals(InputBridge.AdvancedControlsState.PREPARING,
                InputBridge.resolveAdvancedControlsState(true,
                        true, true, false, true));
        assertEquals(InputBridge.AdvancedControlsState.READY,
                InputBridge.resolveAdvancedControlsState(true,
                        true, true, true, true));
    }

    public void testCanvasAnimationContract() throws Exception {
        CanvasConfig animated = new CanvasConfig();
        animated.assetId = "animated.gif";
        animated.animated = true;
        CanvasConfig restored = CanvasConfig.fromJson(animated.toJson());
        assertTrue(restored.animated);
        assertFalse(restored.video);

        CanvasConfig video = new CanvasConfig();
        video.assetId = "loop.mp4";
        video.animated = true;
        video.video = true;
        CanvasConfig restoredVideo = CanvasConfig.fromJson(video.toJson());
        assertTrue(restoredVideo.animated);
        assertTrue(restoredVideo.video);

        WidgetLayout layout = new WidgetLayout();
        WidgetLayout.Item first = new WidgetLayout.Item(
                WidgetLayout.TYPE_CANVAS, 0, 0, 2, 2);
        first.canvasConfig = restored;
        WidgetLayout.Item second = new WidgetLayout.Item(
                WidgetLayout.TYPE_CANVAS, 2, 0, 2, 2);
        layout.items.add(first);
        layout.items.add(second);
        assertFalse(CanvasAnimationPolicy.canAssign(layout, second, true));
        assertTrue(CanvasAnimationPolicy.canAssign(layout, first, true));
        assertTrue(CanvasAnimationPolicy.canAssign(layout, second, false));

        File gif = new File(target.getCacheDir(),
                "canvas-animation-contract-" + System.nanoTime() + ".gif");
        writeFile(gif, android.util.Base64.decode(
                "R0lGODlhAQABAIAAAAAAAP///ywAAAAAAQABAAACAUwAOw==",
                android.util.Base64.DEFAULT));
        CanvasAssetStore.AssetInfo info = CanvasAssetStore.inspectStoredAsset(gif);
        assertEquals("gif", info.extension);
        assertTrue(info.animated);
        assertFalse(info.video);
        assertEquals(1, info.width);
        assertEquals(1, info.height);
        assertTrue(gif.delete());
    }

    public void testGameContextIdentityAndResolverContract() throws Exception {
        String uriA = "content://com.android.externalstorage.documents/document/"
                + "primary%3ARoms%2FPS2%2FGame%20A.iso";
        String uriB = "content://com.android.externalstorage.documents/document/"
                + "primary%3ARoms%2FPS2%2FGame%20B.iso";
        String line = "1756270000.100 12467 12500 I EmulationThread: "
                + "Starting emulation thread (" + uriA + ")";
        assertEquals(uriA, AetherSx2GameContext.extractUri(line));
        assertEquals(1756270000100L, AetherSx2GameContext.extractObservedAt(line));
        assertEquals(0L, AetherSx2GameContext.extractObservedAt(
                "EmulationThread: Starting emulation thread (" + uriA + ")"));
        assertEquals("", AetherSx2GameContext.extractUri(
                "GameLauncher: ROM URI " + uriA));
        assertEquals(AetherSx2GameContext.ACTIVITY_EMULATION,
                AetherSx2GameContext.classifyResumedActivityLine(
                        "topResumedActivity=ActivityRecord{4fe4dc0 u0 "
                                + "xyz.aethersx2.android/.EmulationActivity} t1996}"));
        assertEquals(AetherSx2GameContext.ACTIVITY_MAIN,
                AetherSx2GameContext.classifyResumedActivityLine(
                        "Resumed: ActivityRecord{6a10d33 u0 "
                                + "xyz.aethersx2.android/.MainActivity} t2004}"));
        assertEquals(AetherSx2GameContext.ACTIVITY_UNKNOWN,
                AetherSx2GameContext.classifyResumedActivityLine(
                        "baseActivity={xyz.aethersx2.android/"
                                + "xyz.aethersx2.android.MainActivity}"));
        assertEquals(500L, ShizukuGameContextUserService.activityCacheDurationMillis(
                AetherSx2GameContext.ACTIVITY_MAIN));
        assertEquals(3000L, ShizukuGameContextUserService.activityCacheDurationMillis(
                AetherSx2GameContext.ACTIVITY_EMULATION));
        assertTrue(ShizukuGameContextUserService.shouldClearForActivity(
                AetherSx2GameContext.ACTIVITY_MAIN, 2000L, 1500L));
        assertFalse(ShizukuGameContextUserService.shouldClearForActivity(
                AetherSx2GameContext.ACTIVITY_MAIN, 2000L, 2500L));
        assertFalse(ShizukuGameContextUserService.shouldClearForActivity(
                AetherSx2GameContext.ACTIVITY_EMULATION, 2000L, 1500L));

        GameContextSnapshot contextA = AetherSx2GameContext.snapshot(12467, uriA, 100L);
        GameContextSnapshot contextARepeat = AetherSx2GameContext.snapshot(12467, uriA, 200L);
        GameContextSnapshot contextB = AetherSx2GameContext.snapshot(12467, uriB, 300L);
        assertEquals(GameContextSnapshot.State.ACTIVE, contextA.state);
        assertEquals(contextA.identityKey, contextARepeat.identityKey);
        assertFalse(contextA.identityKey.equals(contextB.identityKey));
        assertEquals("Game A.iso", contextA.label);

        GameProfile first = new GameProfile("A", "generic",
                AetherSx2GameContext.PACKAGE_NAME, Collections.emptyList());
        GameProfile second = new GameProfile("B", "generic",
                AetherSx2GameContext.PACKAGE_NAME, Collections.emptyList());
        first.gameContextBinding = bindingFrom(contextA);
        second.gameContextBinding = bindingFrom(contextB);
        first.romContextHint = "Game B";
        List<GameProfile> profiles = Arrays.asList(first, second);
        ForegroundAppTracker.Snapshot foreground = new ForegroundAppTracker.Snapshot(
                AetherSx2GameContext.PACKAGE_NAME,
                AetherSx2GameContext.PACKAGE_NAME + ".EmulationActivity",
                "Game B", 0, 300L);
        assertEquals(0, ProfileAutoSwitchResolver.resolve(profiles, 1,
                foreground, contextA));
        assertEquals(1, ProfileAutoSwitchResolver.resolve(profiles, 1,
                foreground, GameContextSnapshot.UNKNOWN));

        first.gameContextBinding = new GameContextBinding();
        second.gameContextBinding = new GameContextBinding();
        second.romContextHint = "Game B";
        assertEquals(ProfileAutoSwitchResolver.NO_MATCH,
                ProfileAutoSwitchResolver.resolve(profiles, -1,
                        foreground, GameContextSnapshot.UNKNOWN));
        first.gameContextBinding = bindingFrom(contextA);
        second.gameContextBinding = bindingFrom(contextA);
        assertEquals(1, ProfileAutoSwitchResolver.resolve(profiles, 1,
                foreground, contextA));
        assertEquals(ProfileAutoSwitchResolver.NO_MATCH,
                ProfileAutoSwitchResolver.resolve(profiles, -1, foreground, contextA));

        JSONObject json = first.toJson();
        GameProfile restored = GameProfile.fromJson(json);
        assertEquals(first.gameContextBinding.identityKey,
                restored.gameContextBinding.identityKey);
        assertFalse(json.toString().contains("content://"));
        assertEquals("Game B", restored.romContextHint);

        JSONObject legacy = new JSONObject();
        legacy.put("name", "Legacy");
        legacy.put("mode", "generic");
        legacy.put("packageHint", AetherSx2GameContext.PACKAGE_NAME);
        legacy.put("romContextHint", "old hint");
        GameProfile restoredLegacy = GameProfile.fromJson(legacy);
        assertEquals("old hint", restoredLegacy.romContextHint);
        assertFalse(restoredLegacy.safeGameContextBinding().isBound());
    }

    public void testGameContextAppOnlyFallbackAndManualSelectionGuardContract() {
        String uriA = "content://com.android.externalstorage.documents/document/"
                + "primary%3AROMs%2Fpsp%2FGame%20A.iso";
        String uriB = "content://com.android.externalstorage.documents/document/"
                + "primary%3AROMs%2Fpsp%2FGame%20B.iso";
        String uriC = "content://com.android.externalstorage.documents/document/"
                + "primary%3AROMs%2Fpsp%2FGame%20C.iso";
        GameContextSnapshot contextA = PpssppGameContext.snapshot(
                PpssppGameContext.PACKAGE_FREE, 4400, uriA, 100L);
        GameContextSnapshot contextB = PpssppGameContext.snapshot(
                PpssppGameContext.PACKAGE_FREE, 4400, uriB, 200L);
        GameContextSnapshot contextC = PpssppGameContext.snapshot(
                PpssppGameContext.PACKAGE_FREE, 4400, uriC, 300L);
        ForegroundAppTracker.Snapshot foreground = new ForegroundAppTracker.Snapshot(
                PpssppGameContext.PACKAGE_FREE,
                PpssppGameContext.PACKAGE_FREE + ".PpssppActivity", "", 0, 300L);

        GameProfile exactA = new GameProfile("PSP A", "generic",
                PpssppGameContext.PACKAGE_FREE, Collections.emptyList());
        GameProfile exactB = new GameProfile("PSP B", "generic",
                PpssppGameContext.PACKAGE_FREE, Collections.emptyList());
        GameProfile appOnly = new GameProfile("PSP Default", "generic",
                PpssppGameContext.PACKAGE_FREE, Collections.emptyList());
        exactA.gameContextBinding = bindingFrom(contextA);
        exactB.gameContextBinding = bindingFrom(contextB);

        List<GameProfile> profiles = Arrays.asList(exactA, exactB, appOnly);
        assertEquals(1, ProfileAutoSwitchResolver.resolve(
                profiles, 2, foreground, contextB));
        assertEquals(2, ProfileAutoSwitchResolver.resolve(
                profiles, 0, foreground, contextC));
        assertEquals(0, ProfileAutoSwitchResolver.resolve(
                profiles, 0, foreground, GameContextSnapshot.UNKNOWN));

        GameProfile secondAppOnly = new GameProfile("PSP Alternate", "generic",
                PpssppGameContext.PACKAGE_FREE, Collections.emptyList());
        secondAppOnly.defaultForPackage = true;
        profiles = Arrays.asList(exactA, exactB, appOnly, secondAppOnly);
        assertEquals(3, ProfileAutoSwitchResolver.resolve(
                profiles, 0, foreground, contextC));
        assertEquals(2, ProfileAutoSwitchResolver.resolve(
                profiles, 2, foreground, contextC));
        secondAppOnly.defaultForPackage = false;
        assertEquals(ProfileAutoSwitchResolver.NO_MATCH,
                ProfileAutoSwitchResolver.resolve(profiles, 0, foreground, contextC));

        GameProfile duplicateA = new GameProfile("PSP A duplicate", "generic",
                PpssppGameContext.PACKAGE_FREE, Collections.emptyList());
        duplicateA.gameContextBinding = bindingFrom(contextA);
        profiles = Arrays.asList(exactA, duplicateA, appOnly);
        assertEquals(ProfileAutoSwitchResolver.NO_MATCH,
                ProfileAutoSwitchResolver.resolve(profiles, 2, foreground, contextA));
        assertEquals(0, ProfileAutoSwitchResolver.resolve(
                profiles, 0, foreground, contextA));

        ManualProfileSelectionGuard guard = new ManualProfileSelectionGuard();
        guard.record(foreground, contextB);
        assertTrue(guard.shouldSuppress(foreground, contextB));
        assertTrue(guard.shouldSuppress(foreground, GameContextSnapshot.UNKNOWN));
        assertTrue(guard.shouldSuppress(foreground, GameContextSnapshot.none(
                PpssppGameContext.PACKAGE_FREE, 4400, PpssppGameContext.DETECTOR_ID)));
        GameContextSnapshot relaunchedB = PpssppGameContext.snapshot(
                PpssppGameContext.PACKAGE_FREE, 4400, uriB, 400L);
        assertFalse(guard.shouldSuppress(foreground, relaunchedB));

        guard.record(foreground, contextB);
        assertFalse(guard.shouldSuppress(foreground, contextA));
        guard.record(foreground, GameContextSnapshot.UNKNOWN);
        assertFalse(guard.shouldSuppress(foreground, contextC));

        guard.record(foreground, contextB);
        ForegroundAppTracker.Snapshot otherApp = new ForegroundAppTracker.Snapshot(
                "com.example.frontend", "com.example.frontend.MainActivity", "", 0, 500L);
        assertFalse(guard.shouldSuppress(otherApp, contextB));
    }

    public void testForegroundObservationClearAndUnicodeExportFilenameContract() {
        ForegroundAppTracker.Snapshot previous = ForegroundAppTracker.latest();
        ForegroundAppTracker.Snapshot game = new ForegroundAppTracker.Snapshot(
                "com.example.game", "com.example.game.MainActivity", "", 0, 100L);
        ForegroundAppTracker.publish(game);
        assertEquals("com.example.game", ForegroundAppTracker.latest().packageName);
        ForegroundAppTracker.clear();
        assertNull(ForegroundAppTracker.latest());
        assertEquals(ProfileAutoSwitchResolver.NO_MATCH,
                ProfileAutoSwitchResolver.resolve(Collections.emptyList(), 0,
                        ForegroundAppTracker.latest()));
        assertTrue(ThorAccessibilityService.isLauncherPackage("com.android.launcher3"));
        assertFalse(ThorAccessibilityService.isLauncherPackage("com.example.game"));
        if (previous != null) {
            ForegroundAppTracker.publish(previous);
        }

        assertEquals("\u585e\u5c14\u8fbe\u4f20\u8bf4",
                AssistantActivity.safeFilename("\u585e\u5c14\u8fbe\u4f20\u8bf4"));
        assertEquals("Game-\u4e2d\u6587-2",
                AssistantActivity.safeFilename("Game \u4e2d\u6587/2"));
        assertEquals("profile", AssistantActivity.safeFilename("  "));
    }

    public void testEdenGameContextIdentityAndResolverContract() throws Exception {
        String titleA = "0100FF500E34A000";
        String titleB = "01007300020FA000";
        String lineA = "1787890435.204 30590 4696 I YuzuNative: [550.480865] "
                + "Loader <Info> core/loader/nca.cpp:113:Load: Set pointer buffer size "
                + "for ProgramID 0x" + titleA + " (Heap size: 0x6e6f69)";
        String lineB = "1787890458.772 30590 4910 I YuzuNative: [574.048586] "
                + "Core <Info> core/game_settings.cpp:137:LoadOverrides: Applied game settings "
                + "for title ID " + titleB + " on OS 4";
        String labelLine = "1787890457.456 30590 30590 I YuzuNative: [572.733132] "
                + "Frontend <Info> main/jni/native_log.cpp:22:Log_info: "
                + "[EmulationFragment] Starting view setup for game: ASTRAL CHAIN";
        assertEquals(titleA.toLowerCase(Locale.ROOT),
                EdenGameContext.extractTitleId(lineA));
        assertEquals(titleB.toLowerCase(Locale.ROOT),
                EdenGameContext.extractTitleId(lineB));
        assertEquals("ASTRAL CHAIN", EdenGameContext.extractTitleLabel(labelLine));
        assertEquals("", EdenGameContext.extractTitleId(
                "I Frontend: ProgramID 0x" + titleA));
        assertEquals("", EdenGameContext.extractTitleId(
                lineA.replace("YuzuNative:", "OtherTag:")));

        assertTrue(ShizukuGameContextController.isSupportedPackage(
                AetherSx2GameContext.PACKAGE_NAME));
        assertTrue(ShizukuGameContextController.isSupportedPackage(
                EdenGameContext.PACKAGE_MAIN));
        assertTrue(ShizukuGameContextController.isSupportedPackage(
                EdenGameContext.PACKAGE_NIGHTLY));
        assertFalse(ShizukuGameContextController.isSupportedPackage(
                "com.miHoYo.Yuanshen"));
        assertEquals("YuzuNative", ShizukuGameContextUserService.detectorLogTag(
                EdenGameContext.PACKAGE_MAIN));
        assertEquals("EmulationThread", ShizukuGameContextUserService.detectorLogTag(
                AetherSx2GameContext.PACKAGE_NAME));
        assertTrue(ShizukuGameContextUserService.processCommandMatchesPackage(
                EdenGameContext.PACKAGE_MAIN, EdenGameContext.PACKAGE_MAIN));
        assertTrue(ShizukuGameContextUserService.processCommandMatchesPackage(
                EdenGameContext.PACKAGE_MAIN, EdenGameContext.PACKAGE_MAIN + ":worker"));
        assertFalse(ShizukuGameContextUserService.processCommandMatchesPackage(
                EdenGameContext.PACKAGE_MAIN, EdenGameContext.PACKAGE_MAIN + ".spoof"));
        assertFalse(ShizukuGameContextUserService.processCommandMatchesPackage(
                EdenGameContext.PACKAGE_MAIN, ""));
        assertEquals(EdenGameContext.ACTIVITY_EMULATION,
                ShizukuGameContextUserService.classifyActivityLine(
                        EdenGameContext.PACKAGE_MAIN,
                        "topResumedActivity=ActivityRecord{1ab169a u0 "
                                + EdenGameContext.PACKAGE_MAIN
                                + "/org.yuzu.yuzu_emu.activities.EmulationActivity} t2142}"));
        assertEquals(EdenGameContext.ACTIVITY_MAIN,
                ShizukuGameContextUserService.classifyActivityLine(
                        EdenGameContext.PACKAGE_MAIN,
                        "Resumed: ActivityRecord{c524c0a u0 "
                                + EdenGameContext.PACKAGE_MAIN
                                + "/org.yuzu.yuzu_emu.ui.main.MainActivity} t2142}"));

        GameContextSnapshot contextA = EdenGameContext.snapshot(
                EdenGameContext.PACKAGE_MAIN, 30590, titleA,
                "Xenoblade Chronicles Definitive Edition", 100L);
        GameContextSnapshot contextARepeat = EdenGameContext.snapshot(
                EdenGameContext.PACKAGE_MAIN, 30590, titleA.toLowerCase(Locale.ROOT),
                "Xenoblade Chronicles Definitive Edition", 200L);
        GameContextSnapshot contextB = EdenGameContext.snapshot(
                EdenGameContext.PACKAGE_MAIN, 30590, titleB,
                "ASTRAL CHAIN", 300L);
        assertEquals(GameContextSnapshot.State.ACTIVE, contextA.state);
        assertEquals(GameContextBinding.KIND_EMULATOR_TITLE_ID, contextA.kind);
        assertEquals(contextA.identityKey, contextARepeat.identityKey);
        assertFalse(contextA.identityKey.equals(contextB.identityKey));
        assertEquals("ASTRAL CHAIN", contextB.label);

        GameProfile first = new GameProfile("Xenoblade", "generic",
                EdenGameContext.PACKAGE_MAIN, Collections.emptyList());
        GameProfile second = new GameProfile("Astral Chain", "generic",
                EdenGameContext.PACKAGE_MAIN, Collections.emptyList());
        first.gameContextBinding = bindingFrom(contextA);
        second.gameContextBinding = bindingFrom(contextB);
        assertTrue(first.gameContextBinding.isBound());
        JSONObject serialized = first.toJson();
        assertFalse(serialized.toString().contains(titleA));
        assertFalse(serialized.toString().contains("content://"));
        GameProfile restored = GameProfile.fromJson(serialized);
        assertTrue(restored.safeGameContextBinding().isBound());
        assertEquals(GameContextBinding.KIND_EMULATOR_TITLE_ID,
                restored.safeGameContextBinding().kind);

        ForegroundAppTracker.Snapshot foreground = new ForegroundAppTracker.Snapshot(
                EdenGameContext.PACKAGE_MAIN,
                "org.yuzu.yuzu_emu.activities.EmulationActivity", "", 0, 300L);
        assertEquals(1, ProfileAutoSwitchResolver.resolve(
                Arrays.asList(first, second), 0, foreground, contextB));
    }

    public void testGameContextUserServiceLifetimeContract() throws Exception {
        assertEquals("game_context_v15",
                ShizukuGameContextController.SERVICE_PROCESS_SUFFIX);
        assertEquals("heimdall_game_context_v15",
                ShizukuGameContextController.SERVICE_TAG);
        assertEquals(14, ShizukuGameContextController.SERVICE_VERSION);
        assertEquals("heimdall_native_controller_v13",
                ShizukuNativeController.SERVICE_TAG);
        assertEquals(13, ShizukuNativeController.SERVICE_VERSION);
        assertTrue(ShizukuUserServiceLifecycle.isDestroyTransaction(16_777_115));
        assertFalse(ShizukuUserServiceLifecycle.isDestroyTransaction(
                IBinder.FIRST_CALL_TRANSACTION));
    }

    public void testGameNativeCurrentStateGameContextContract() throws Exception {
        String steamLaunch = "1789021668.833 18060 14959 I app.gamenative: "
                + "I: ID: STEAM_1656780";
        String customLaunch = "1789022020.037 17056 20312 I app.gamenative: "
                + "I: ID: CUSTOM_GAME_674942539";
        String gogLaunch = "1789031683.064 17056 9565 I app.gamenative: "
                + "I: ID: GOG_1420716694";
        String epicLaunch = "1789033029.732 17056 16151 I app.gamenative: "
                + "I: ID: EPIC_175";
        String steamExit = "1789021899.472 18060 18060 I Exit    : "
                + "I: Exiting, getting feedback for appId: STEAM_1656780";
        String gogExit = "1789032062.706 17056 17056 I Exit    : "
                + "I: Exiting, getting feedback for appId: GOG_1420716694";
        String epicExit = "1789033181.789 17056 17056 I Exit    : "
                + "I: Exiting, getting feedback for appId: EPIC_175";
        String steamLabel = "1789021669.283 18060 14959 I XServerScreen: "
                + "I: Initiated CPU pinning for: Hero's Hour.exe";
        String customLabel = "1789022020.086 17056 20312 I XServerScreen: "
                + "I: Initiated CPU pinning for: A:\\sora_1st.exe";
        String gogLabel = "1789031683.107 17056 9565 I XServerScreen: "
                + "I: Initiated CPU pinning for: ddtrilogy.exe";
        String epicLabel = "1789033029.796 17056 16151 I XServerScreen: "
                + "I: Initiated CPU pinning for: AstroDuel2_EOS.exe";

        assertEquals("STEAM_1656780", GameNativeGameContext.extractLaunchId(steamLaunch));
        assertEquals("CUSTOM_GAME_674942539",
                GameNativeGameContext.extractLaunchId(customLaunch));
        assertEquals("GOG_1420716694", GameNativeGameContext.extractLaunchId(gogLaunch));
        assertEquals("EPIC_175", GameNativeGameContext.extractLaunchId(epicLaunch));
        assertEquals("STEAM_1656780", GameNativeGameContext.extractExitId(steamExit));
        assertEquals("GOG_1420716694", GameNativeGameContext.extractExitId(gogExit));
        assertEquals("EPIC_175", GameNativeGameContext.extractExitId(epicExit));
        assertEquals("Hero's Hour", GameNativeGameContext.extractGameLabel(steamLabel));
        assertEquals("sora_1st", GameNativeGameContext.extractGameLabel(customLabel));
        assertEquals("ddtrilogy", GameNativeGameContext.extractGameLabel(gogLabel));
        assertEquals("AstroDuel2_EOS", GameNativeGameContext.extractGameLabel(epicLabel));
        assertEquals("", GameNativeGameContext.extractLaunchId(
                steamLaunch.replace("app.gamenative:", "OtherTag:")));
        assertEquals("", GameNativeGameContext.extractLaunchId(
                steamLaunch.replace("STEAM_1656780", "STEAM_bad")));
        assertEquals("", GameNativeGameContext.extractLaunchId(
                gogLaunch.replace("GOG_1420716694", "AMAZON_1420716694")));
        assertEquals("", GameNativeGameContext.extractLaunchId(
                epicLaunch.replace("EPIC_175", "EPIC_0")));
        assertEquals("", GameNativeGameContext.extractExitId(
                steamExit.replace("Exit    :", "app.gamenative:")));

        assertTrue(GameNativeGameContext.supportsPackage("app.gamenative"));
        assertFalse(GameNativeGameContext.supportsPackage("gamehub.lite"));
        assertTrue(ShizukuGameContextController.isSupportedPackage("app.gamenative"));
        assertEquals("app.gamenative", ShizukuGameContextUserService.detectorLogTag(
                GameNativeGameContext.PACKAGE_NAME));
        assertFalse(ShizukuGameContextUserService.shouldClearForPackage(
                GameNativeGameContext.PACKAGE_NAME,
                AetherSx2GameContext.ACTIVITY_MAIN, 2000L, 1500L));

        assertTrue(GameNativeGameContext.isBaselineSameUidCommand(
                GameNativeGameContext.PACKAGE_NAME, "app.gamenative"));
        assertTrue(GameNativeGameContext.isBaselineSameUidCommand(
                GameNativeGameContext.PACKAGE_NAME,
                "logcat -v threadtime *:E -T 09-10 12:45:10.045"));
        assertFalse(GameNativeGameContext.isBaselineSameUidCommand(
                GameNativeGameContext.PACKAGE_NAME,
                "C:\\Program Files (x86)\\Steam\\steamapps\\common\\Hero's Hour"
                        + "\\Hero's Hour.exe"));
        assertFalse(GameNativeGameContext.isBaselineSameUidCommand(
                GameNativeGameContext.PACKAGE_NAME, "A:\\sora_1st.exe"));

        GameContextSnapshot steam = GameNativeGameContext.snapshot(
                GameNativeGameContext.STATE_ACTIVE, 18060, "STEAM_1656780",
                "Hero's Hour", 100L);
        GameContextSnapshot steamRepeat = GameNativeGameContext.snapshot(
                GameNativeGameContext.STATE_ACTIVE, 17056, "STEAM_1656780",
                "Hero's Hour", 200L);
        GameContextSnapshot custom = GameNativeGameContext.snapshot(
                GameNativeGameContext.STATE_ACTIVE, 17056, "CUSTOM_GAME_674942539",
                "sora_1st", 300L);
        GameContextSnapshot sameNumericCustom = GameNativeGameContext.snapshot(
                GameNativeGameContext.STATE_ACTIVE, 17056, "CUSTOM_GAME_1656780",
                "Local game", 400L);
        GameContextSnapshot gog = GameNativeGameContext.snapshot(
                GameNativeGameContext.STATE_ACTIVE, 17056, "GOG_1420716694",
                "ddtrilogy", 450L);
        GameContextSnapshot sameNumericGog = GameNativeGameContext.snapshot(
                GameNativeGameContext.STATE_ACTIVE, 17056, "GOG_1656780",
                "", 475L);
        GameContextSnapshot epic = GameNativeGameContext.snapshot(
                GameNativeGameContext.STATE_ACTIVE, 17056, "EPIC_175",
                "AstroDuel2_EOS", 480L);
        GameContextSnapshot sameNumericEpic = GameNativeGameContext.snapshot(
                GameNativeGameContext.STATE_ACTIVE, 17056, "EPIC_1656780",
                "", 490L);
        GameContextSnapshot none = GameNativeGameContext.snapshot(
                GameNativeGameContext.STATE_NONE, 17056, "", "", 500L);
        assertEquals(GameContextSnapshot.State.ACTIVE, steam.state);
        assertEquals(GameContextBinding.KIND_PC_GAME, steam.kind);
        assertEquals("Hero's Hour", steam.label);
        assertEquals(steam.identityKey, steamRepeat.identityKey);
        assertFalse(steam.identityKey.equals(custom.identityKey));
        assertFalse(steam.identityKey.equals(sameNumericCustom.identityKey));
        assertFalse(steam.identityKey.equals(sameNumericGog.identityKey));
        assertFalse(custom.identityKey.equals(gog.identityKey));
        assertEquals("GOG game", sameNumericGog.label);
        assertFalse(steam.identityKey.equals(sameNumericEpic.identityKey));
        assertFalse(gog.identityKey.equals(epic.identityKey));
        assertEquals("Epic game", sameNumericEpic.label);
        assertEquals(GameContextSnapshot.State.NONE, none.state);
        assertEquals(GameContextSnapshot.State.UNKNOWN,
                GameNativeGameContext.snapshot(GameNativeGameContext.STATE_ACTIVE,
                        17056, "STEAM_bad", "Bad", 600L).state);

        GameProfile steamProfile = new GameProfile("Hero's Hour", "generic",
                GameNativeGameContext.PACKAGE_NAME, Collections.emptyList());
        GameProfile customProfile = new GameProfile("Trails", "generic",
                GameNativeGameContext.PACKAGE_NAME, Collections.emptyList());
        GameProfile gogProfile = new GameProfile("Double Dragon", "generic",
                GameNativeGameContext.PACKAGE_NAME, Collections.emptyList());
        GameProfile epicProfile = new GameProfile("Astro Duel 2", "generic",
                GameNativeGameContext.PACKAGE_NAME, Collections.emptyList());
        steamProfile.gameContextBinding = bindingFrom(steam);
        customProfile.gameContextBinding = bindingFrom(custom);
        gogProfile.gameContextBinding = bindingFrom(gog);
        epicProfile.gameContextBinding = bindingFrom(epic);
        List<GameProfile> profiles = Arrays.asList(
                steamProfile, customProfile, gogProfile, epicProfile);
        ForegroundAppTracker.Snapshot foreground = new ForegroundAppTracker.Snapshot(
                GameNativeGameContext.PACKAGE_NAME,
                GameNativeGameContext.PACKAGE_NAME + ".MainActivityAliasAlt", "", 0, 300L);
        assertEquals(1, ProfileAutoSwitchResolver.resolve(
                profiles, 0, foreground, custom));
        assertEquals(2, ProfileAutoSwitchResolver.resolve(
                profiles, 0, foreground, gog));
        assertEquals(3, ProfileAutoSwitchResolver.resolve(
                profiles, 0, foreground, epic));

        JSONObject serialized = customProfile.toJson();
        assertFalse(serialized.toString().contains("674942539"));
        assertFalse(serialized.toString().contains("A:\\"));
        assertEquals(GameContextBinding.KIND_PC_GAME,
                GameProfile.fromJson(serialized).safeGameContextBinding().kind);
        JSONObject serializedGog = gogProfile.toJson();
        assertFalse(serializedGog.toString().contains("1420716694"));
        assertFalse(serializedGog.toString().contains("GOG_"));
        JSONObject serializedEpic = epicProfile.toJson();
        assertFalse(serializedEpic.toString().contains("EPIC_175"));
        assertFalse(serializedEpic.toString().contains("EPIC_"));

        GameProfile duplicate = new GameProfile("Trails alternate", "generic",
                GameNativeGameContext.PACKAGE_NAME, Collections.emptyList());
        duplicate.gameContextBinding = bindingFrom(custom);
        assertEquals(ProfileAutoSwitchResolver.NO_MATCH,
                ProfileAutoSwitchResolver.resolve(
                        Arrays.asList(customProfile, duplicate), -1, foreground, custom));
    }

    public void testPpssppPositiveLaunchGameContextContract() throws Exception {
        String uriA = "content://com.android.externalstorage.documents/tree/"
                + "primary%3AROMs/document/primary%3AROMs%2Fpsp%2FGame%20A.iso";
        String uriB = "content://com.android.externalstorage.documents/tree/"
                + "primary%3AROMs/document/primary%3AROMs%2Fpsp%2FGame%20B.cso";
        String lineA = "1787890363.684 3720 3803 I PPSSPP  : [BOOT] Booted "
                + uriA + "...";
        String lineB = "1787899443.571 22246 22305 I PPSSPP  : [BOOT] Booted "
                + uriB + "...";

        assertEquals(uriA, PpssppGameContext.extractUri(lineA));
        assertEquals(uriB, PpssppGameContext.extractUri(lineB));
        assertEquals("", PpssppGameContext.extractUri(
                lineA.replace("I PPSSPP", "I OtherTag")));
        assertEquals("", PpssppGameContext.extractUri(
                "1787890363.684 3720 3803 I PPSSPP  : [BOOT] Loading " + uriA));
        assertTrue(PpssppGameContext.supportsPackage(PpssppGameContext.PACKAGE_FREE));
        assertTrue(PpssppGameContext.supportsPackage(PpssppGameContext.PACKAGE_GOLD));
        assertFalse(PpssppGameContext.supportsPackage("org.ppsspp.fork"));
        assertTrue(ShizukuGameContextController.isSupportedPackage(
                PpssppGameContext.PACKAGE_FREE));
        assertEquals("PPSSPP", ShizukuGameContextUserService.detectorLogTag(
                PpssppGameContext.PACKAGE_FREE));

        GameContextSnapshot contextA = PpssppGameContext.snapshot(
                PpssppGameContext.PACKAGE_FREE, 3720, uriA, 100L);
        GameContextSnapshot contextARepeat = PpssppGameContext.snapshot(
                PpssppGameContext.PACKAGE_FREE, 3720, uriA, 200L);
        GameContextSnapshot contextB = PpssppGameContext.snapshot(
                PpssppGameContext.PACKAGE_FREE, 3720, uriB, 300L);
        assertEquals(GameContextSnapshot.State.ACTIVE, contextA.state);
        assertEquals(GameContextBinding.KIND_SAF_DOCUMENT, contextA.kind);
        assertEquals(contextA.identityKey, contextARepeat.identityKey);
        assertFalse(contextA.identityKey.equals(contextB.identityKey));
        assertEquals("Game A.iso", contextA.label);
        assertEquals(GameContextSnapshot.State.UNKNOWN,
                PpssppGameContext.snapshot(PpssppGameContext.PACKAGE_FREE,
                        -1, uriA, 100L).state);

        assertFalse(ShizukuGameContextUserService.shouldClearForPackage(
                PpssppGameContext.PACKAGE_FREE,
                AetherSx2GameContext.ACTIVITY_MAIN, 2000L, 1500L));
        assertTrue(ShizukuGameContextUserService.shouldClearForPackage(
                AetherSx2GameContext.PACKAGE_NAME,
                AetherSx2GameContext.ACTIVITY_MAIN, 2000L, 1500L));

        GameProfile first = new GameProfile("PSP A", "generic",
                PpssppGameContext.PACKAGE_FREE, Collections.emptyList());
        GameProfile second = new GameProfile("PSP B", "generic",
                PpssppGameContext.PACKAGE_FREE, Collections.emptyList());
        first.gameContextBinding = bindingFrom(contextA);
        second.gameContextBinding = bindingFrom(contextB);
        List<GameProfile> profiles = Arrays.asList(first, second);
        ForegroundAppTracker.Snapshot foreground = new ForegroundAppTracker.Snapshot(
                PpssppGameContext.PACKAGE_FREE,
                PpssppGameContext.PACKAGE_FREE + ".PpssppActivity", "", 0, 300L);
        assertEquals(1, ProfileAutoSwitchResolver.resolve(
                profiles, 0, foreground, contextB));
        assertEquals(1, ProfileAutoSwitchResolver.resolve(
                profiles, 1, foreground, GameContextSnapshot.UNKNOWN));

        JSONObject serialized = second.toJson();
        assertFalse(serialized.toString().contains("content://"));
        assertTrue(GameProfile.fromJson(serialized).safeGameContextBinding().isBound());
    }

    public void testRetroArchTwoLevelGameContextContract() throws Exception {
        String gameAPath = "/storage/emulated/0/ROMs/GBA/Kirby.gba";
        String gameBPath = "/storage/emulated/0/ROMs/GBA/Metroid.gba";
        String gameZipPath = "/storage/emulated/0/ROMs/Archives/Archive.zip";
        String gbaFolderZipPath = "/storage/emulated/0/ROMs/GBA/Archive.zip";
        JSONObject historyA = new JSONObject();
        JSONArray itemsA = new JSONArray();
        JSONObject itemA = new JSONObject();
        itemA.put("path", gameAPath);
        itemA.put("label", "Kirby & the Amazing Mirror");
        itemA.put("core_path", "/data/user/0/com.retroarch.aarch64/cores/mgba.so");
        itemA.put("core_name", "mGBA");
        itemA.put("db_name", "");
        itemsA.put(itemA);
        historyA.put("items", itemsA);

        RetroArchGameContext.LaunchRecord recordA =
                RetroArchGameContext.parseHistory(historyA.toString(), 10_000L);
        assertNotNull(recordA);
        assertEquals("nintendo_game_boy_advance", recordA.platformCode);
        assertEquals("Nintendo Game Boy Advance", recordA.platformLabel);

        JSONObject historyB = new JSONObject(historyA.toString());
        historyB.getJSONArray("items").getJSONObject(0).put("path", gameBPath);
        historyB.getJSONArray("items").getJSONObject(0).put("label", "Metroid Fusion");
        RetroArchGameContext.LaunchRecord recordB =
                RetroArchGameContext.parseHistory(historyB.toString(), 20_000L);
        assertNotNull(recordB);

        JSONObject ambiguousHistory = new JSONObject(historyA.toString());
        ambiguousHistory.getJSONArray("items").getJSONObject(0).put("path", gameZipPath);
        RetroArchGameContext.LaunchRecord ambiguous =
                RetroArchGameContext.parseHistory(ambiguousHistory.toString(), 30_000L);
        assertNotNull(ambiguous);
        assertEquals("", ambiguous.platformCode);

        ambiguousHistory.getJSONArray("items").getJSONObject(0).put("db_name", "DETECT");
        RetroArchGameContext.LaunchRecord detectMarker =
                RetroArchGameContext.parseHistory(ambiguousHistory.toString(), 35_000L);
        assertNotNull(detectMarker);
        assertEquals("", detectMarker.platformCode);

        ambiguousHistory.getJSONArray("items").getJSONObject(0)
                .put("db_name", "Nintendo - Game Boy Advance.lpl");
        RetroArchGameContext.LaunchRecord databaseResolved =
                RetroArchGameContext.parseHistory(ambiguousHistory.toString(), 40_000L);
        assertNotNull(databaseResolved);
        assertEquals(recordA.platformCode, databaseResolved.platformCode);

        ambiguousHistory.getJSONArray("items").getJSONObject(0).put("db_name", "");
        ambiguousHistory.getJSONArray("items").getJSONObject(0).put("path", gbaFolderZipPath);
        RetroArchGameContext.LaunchRecord folderResolved =
                RetroArchGameContext.parseHistory(ambiguousHistory.toString(), 45_000L);
        assertNotNull(folderResolved);
        assertEquals("nintendo_game_boy_advance", folderResolved.platformCode);
        assertFalse(folderResolved.platformAffectsContentIdentity);

        RetroArchGameContext.LaunchRecord legacyUnknownFolderRecord =
                new RetroArchGameContext.LaunchRecord(gbaFolderZipPath, "Archive",
                        "", "", false, 45_000L);
        GameContextSnapshot legacyUnknownFolderContext = RetroArchGameContext.snapshot(
                RetroArchGameContext.PACKAGE_AARCH64, 7000, legacyUnknownFolderRecord);
        GameContextSnapshot folderResolvedContext = RetroArchGameContext.snapshot(
                RetroArchGameContext.PACKAGE_AARCH64, 7000, folderResolved);
        assertEquals(legacyUnknownFolderContext.identityKey,
                folderResolvedContext.identityKey);

        ambiguousHistory.getJSONArray("items").getJSONObject(0)
                .put("path", "/storage/emulated/0/ROMs/SFC/Chrono Trigger.zip");
        RetroArchGameContext.LaunchRecord sfcFolder =
                RetroArchGameContext.parseHistory(ambiguousHistory.toString(), 46_000L);
        assertNotNull(sfcFolder);
        assertEquals("nintendo_snes", sfcFolder.platformCode);

        ambiguousHistory.getJSONArray("items").getJSONObject(0)
                .put("path", "/storage/emulated/0/ROMs/Neo Geo/Metal Slug.zip");
        RetroArchGameContext.LaunchRecord neoGeoFolder =
                RetroArchGameContext.parseHistory(ambiguousHistory.toString(), 47_000L);
        assertNotNull(neoGeoFolder);
        assertEquals("snk_neo_geo", neoGeoFolder.platformCode);

        ambiguousHistory.getJSONArray("items").getJSONObject(0)
                .put("path", "/storage/emulated/0/ROMs/FBAlpha/1941.zip");
        RetroArchGameContext.LaunchRecord fbAlphaFolder =
                RetroArchGameContext.parseHistory(ambiguousHistory.toString(), 48_000L);
        assertNotNull(fbAlphaFolder);
        assertEquals("finalburn_alpha", fbAlphaFolder.platformCode);

        ambiguousHistory.getJSONArray("items").getJSONObject(0)
                .put("path", "/storage/emulated/0/ROMs/megadrive/Sonic.zip");
        RetroArchGameContext.LaunchRecord megaDriveFolder =
                RetroArchGameContext.parseHistory(ambiguousHistory.toString(), 48_100L);
        assertNotNull(megaDriveFolder);
        assertEquals("sega_mega_drive_genesis", megaDriveFolder.platformCode);

        ambiguousHistory.getJSONArray("items").getJSONObject(0)
                .put("path", "/storage/emulated/0/ROMs/psx/Ridge Racer.chd");
        RetroArchGameContext.LaunchRecord playStationFolder =
                RetroArchGameContext.parseHistory(ambiguousHistory.toString(), 48_200L);
        assertNotNull(playStationFolder);
        assertEquals("sony_playstation", playStationFolder.platformCode);

        ambiguousHistory.getJSONArray("items").getJSONObject(0)
                .put("path", "/storage/emulated/0/ROMs/dreamcast/Crazy Taxi.chd");
        RetroArchGameContext.LaunchRecord dreamcastFolder =
                RetroArchGameContext.parseHistory(ambiguousHistory.toString(), 48_300L);
        assertNotNull(dreamcastFolder);
        assertEquals("sega_dreamcast", dreamcastFolder.platformCode);

        ambiguousHistory.getJSONArray("items").getJSONObject(0)
                .put("path", "/storage/emulated/0/ROMs/mame/1942.zip");
        RetroArchGameContext.LaunchRecord mameFolder =
                RetroArchGameContext.parseHistory(ambiguousHistory.toString(), 48_400L);
        assertNotNull(mameFolder);
        assertEquals("arcade_mame", mameFolder.platformCode);

        ambiguousHistory.getJSONArray("items").getJSONObject(0)
                .put("path", "/storage/emulated/0/ROMs/n64/Hacks/Mario.z64.zip");
        RetroArchGameContext.LaunchRecord nestedN64Folder =
                RetroArchGameContext.parseHistory(ambiguousHistory.toString(), 48_500L);
        assertNotNull(nestedN64Folder);
        assertEquals("nintendo_64", nestedN64Folder.platformCode);

        ambiguousHistory.getJSONArray("items").getJSONObject(0)
                .put("path", "/storage/emulated/0/ROMs/tg-cd/Dracula X.chd");
        RetroArchGameContext.LaunchRecord turboGrafxCdFolder =
                RetroArchGameContext.parseHistory(ambiguousHistory.toString(), 48_600L);
        assertNotNull(turboGrafxCdFolder);
        assertEquals("nec_pc_engine_cd", turboGrafxCdFolder.platformCode);

        ambiguousHistory.getJSONArray("items").getJSONObject(0).put("path",
                "content://provider/document/primary%3AROMs%2FSFC%2FGame.zip");
        RetroArchGameContext.LaunchRecord encodedSfcFolder =
                RetroArchGameContext.parseHistory(ambiguousHistory.toString(), 49_000L);
        assertNotNull(encodedSfcFolder);
        assertEquals("nintendo_snes", encodedSfcFolder.platformCode);

        ambiguousHistory.getJSONArray("items").getJSONObject(0)
                .put("path", "/storage/emulated/0/ROMs/my-sfc-backups/Game.zip");
        RetroArchGameContext.LaunchRecord nearMatchFolder =
                RetroArchGameContext.parseHistory(ambiguousHistory.toString(), 49_500L);
        assertNotNull(nearMatchFolder);
        assertEquals("", nearMatchFolder.platformCode);

        ambiguousHistory.getJSONArray("items").getJSONObject(0)
                .put("path", "/storage/emulated/0/ROMs/steam/Game.zip");
        RetroArchGameContext.LaunchRecord nonRomFrontendFolder =
                RetroArchGameContext.parseHistory(ambiguousHistory.toString(), 49_600L);
        assertNotNull(nonRomFrontendFolder);
        assertEquals("", nonRomFrontendFolder.platformCode);

        assertTrue(RetroArchGameContext.supportsPackage(
                RetroArchGameContext.PACKAGE_MAIN));
        assertTrue(RetroArchGameContext.supportsPackage(
                RetroArchGameContext.PACKAGE_AARCH64));
        assertTrue(RetroArchGameContext.supportsPackage(
                RetroArchGameContext.PACKAGE_RA32));
        assertFalse(RetroArchGameContext.supportsPackage("org.retroarch"));
        assertTrue(ShizukuGameContextController.isSupportedPackage(
                RetroArchGameContext.PACKAGE_AARCH64));
        assertEquals("RetroArch", ShizukuGameContextUserService.detectorLogTag(
                RetroArchGameContext.PACKAGE_AARCH64));
        assertTrue(RetroArchGameContext.isLaunchEvidenceFresh(10_000L, 11_500L));
        assertFalse(RetroArchGameContext.isLaunchEvidenceFresh(10_000L, 13_000L));
        assertFalse(ShizukuGameContextUserService.shouldClearForPackage(
                RetroArchGameContext.PACKAGE_AARCH64,
                AetherSx2GameContext.ACTIVITY_MAIN, 50_000L, 40_000L));

        GameContextSnapshot contextA = RetroArchGameContext.snapshot(
                RetroArchGameContext.PACKAGE_AARCH64, 7000, recordA);
        GameContextSnapshot contextB = RetroArchGameContext.snapshot(
                RetroArchGameContext.PACKAGE_AARCH64, 7000, recordB);
        GameContextSnapshot contextZip = RetroArchGameContext.snapshot(
                RetroArchGameContext.PACKAGE_AARCH64, 7000, ambiguous);
        assertEquals(GameContextSnapshot.State.ACTIVE, contextA.state);
        assertEquals(RetroArchGameContext.KIND_CONTENT, contextA.kind);
        assertTrue(contextA.hasPlatformIdentity());
        assertEquals(contextA.platformIdentityKey, contextB.platformIdentityKey);
        assertFalse(contextA.identityKey.equals(contextB.identityKey));
        assertFalse(contextZip.hasPlatformIdentity());

        RetroArchGameContext.LaunchRecord recordARelaunch =
                RetroArchGameContext.parseHistory(historyA.toString(), 50_000L);
        GameContextSnapshot contextARelaunch = RetroArchGameContext.snapshot(
                RetroArchGameContext.PACKAGE_AARCH64, 7000, recordARelaunch);
        assertFalse(contextA.sameIdentity(contextARelaunch));

        GameProfile exactA = new GameProfile("Kirby", "generic",
                RetroArchGameContext.PACKAGE_AARCH64, Collections.emptyList());
        GameProfile gbaDefault = new GameProfile("GBA", "generic",
                RetroArchGameContext.PACKAGE_AARCH64, Collections.emptyList());
        GameProfile appDefault = new GameProfile("RetroArch", "generic",
                RetroArchGameContext.PACKAGE_AARCH64, Collections.emptyList());
        exactA.gameContextBinding = bindingFrom(contextA);
        gbaDefault.gameContextBinding = platformBindingFrom(contextA);
        List<GameProfile> profiles = Arrays.asList(exactA, gbaDefault, appDefault);
        ForegroundAppTracker.Snapshot foreground = new ForegroundAppTracker.Snapshot(
                RetroArchGameContext.PACKAGE_AARCH64,
                "com.retroarch.browser.retroactivity.RetroActivityFuture", "", 0, 50_000L);
        assertEquals(0, ProfileAutoSwitchResolver.resolve(
                profiles, 2, foreground, contextA));
        assertEquals(1, ProfileAutoSwitchResolver.resolve(
                profiles, 0, foreground, contextB));
        assertEquals(2, ProfileAutoSwitchResolver.resolve(
                profiles, 0, foreground, contextZip));

        GameProfile duplicateGba = new GameProfile("GBA alternate", "generic",
                RetroArchGameContext.PACKAGE_AARCH64, Collections.emptyList());
        duplicateGba.gameContextBinding = platformBindingFrom(contextA);
        profiles = Arrays.asList(exactA, gbaDefault, duplicateGba, appDefault);
        assertEquals(ProfileAutoSwitchResolver.NO_MATCH,
                ProfileAutoSwitchResolver.resolve(profiles, 3, foreground, contextB));
        assertEquals(1, ProfileAutoSwitchResolver.resolve(
                profiles, 1, foreground, contextB));

        JSONObject serialized = gbaDefault.toJson();
        assertFalse(serialized.toString().contains(gameAPath));
        assertEquals(RetroArchGameContext.KIND_PLATFORM,
                GameProfile.fromJson(serialized).safeGameContextBinding().kind);

        assertEquals(987654L, ShizukuGameContextUserService.parseProcessStartTicks(
                "123 (retro arch) S 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 987654 20"));
        assertEquals(1787900000L, ShizukuGameContextUserService.parseBootEpochSeconds(
                "cpu 1 2 3 4\nbtime 1787900000\nprocesses 5\n"));
    }

    private static GameContextBinding bindingFrom(GameContextSnapshot snapshot) {
        GameContextBinding binding = new GameContextBinding();
        binding.kind = snapshot.kind;
        binding.identityKey = snapshot.identityKey;
        binding.label = snapshot.label;
        return binding;
    }

    private static GameContextBinding platformBindingFrom(GameContextSnapshot snapshot) {
        GameContextBinding binding = new GameContextBinding();
        binding.kind = snapshot.platformKind;
        binding.identityKey = snapshot.platformIdentityKey;
        binding.label = snapshot.platformLabel;
        return binding;
    }

    public void testControllerSequenceSafetyPolicy() {
        GamepadSequencePolicy.Inspection ordinary = GamepadSequencePolicy.inspect(
                "seq:1,304,1,0;1,304,0,80;1,316,1,20;1,316,0,40");
        assertFalse(ordinary.containsSystemNavigationKey());
        assertFalse(ordinary.hasUnreleasedSystemNavigationKey);
        assertFalse(ordinary.exceedsReplayLimits());
        assertEquals(4, ordinary.eventCount);
        assertTrue(ordinary.replayTimeoutMs() >= 3_000L);

        assertSystemNavigationDetected(GamepadSequencePolicy.KEY_BACK);
        assertSystemNavigationDetected(GamepadSequencePolicy.KEY_HOME);
        assertSystemNavigationDetected(GamepadSequencePolicy.KEY_RECENT_APPS);
        assertSystemNavigationDetected(GamepadSequencePolicy.KEY_APPSELECT);

        GamepadSequencePolicy.Inspection malformed = GamepadSequencePolicy.inspect(
                "seq:not-an-event;1,304,1,0;1,304,0,20");
        assertFalse(malformed.containsSystemNavigationKey());
        assertEquals(2, malformed.eventCount);

        StringBuilder overlong = new StringBuilder("seq:");
        for (int i = 0; i < 21; i++) {
            if (i > 0) overlong.append(';');
            overlong.append("1,304,1,500");
        }
        assertTrue(GamepadSequencePolicy.inspect(overlong.toString())
                .exceedsReplayLimits());
    }

    public void testComposedControllerSequenceContract() {
        List<GamepadSequenceComposer.Frame> frames = new ArrayList<>();
        frames.add(new GamepadSequenceComposer.Frame(-1, 0, Collections.emptyList()));
        frames.add(new GamepadSequenceComposer.Frame(-1, 1, Collections.emptyList()));
        frames.add(new GamepadSequenceComposer.Frame(0, 1, Collections.emptyList()));
        frames.add(new GamepadSequenceComposer.Frame(1, 1, Collections.emptyList()));
        frames.add(new GamepadSequenceComposer.Frame(1, 0,
                java.util.Arrays.asList(GamepadSequenceComposer.BTN_A,
                        GamepadSequenceComposer.BTN_B)));

        String keyDpad = GamepadSequenceComposer.build(frames,
                GamepadSequenceComposer.DPAD_KEYS, 40, 60);
        assertEquals("seq:1,546,1,0;0,0,0,0;1,545,1,40;0,0,0,0;"
                + "1,546,0,40;0,0,0,0;1,547,1,40;0,0,0,0;"
                + "1,304,1,40;1,305,1,0;1,545,0,0;0,0,0,0;"
                + "1,304,0,60;1,305,0,0;1,547,0,0;0,0,0,0", keyDpad);
        assertTrue(GamepadSequenceComposer.isComposedSequence(keyDpad));
        assertFalse(GamepadSequenceComposer.isComposedSequence(
                "seq:1,304,1,0;1,304,0,60"));
        String recordedAnalogWithSyn =
                "seq:3,0,123,0;0,0,0,0;3,0,0,40;0,0,0,0";
        assertNull(GamepadSequenceComposer.parseEditable(recordedAnalogWithSyn));
        assertFalse(GamepadSequenceComposer.isComposedSequence(recordedAnalogWithSyn));
        GamepadSequenceSummary keySummary = GamepadSequenceSummary.summarize(
                target, keyDpad, 2, 5);
        assertTrue(keySummary.valid);
        assertTrue(keySummary.title.contains("\u2199"));
        assertTrue(keySummary.title.contains("\u2198"));
        assertTrue(keySummary.title.contains("A + B"));
        assertEquals(220, (int) keySummary.replayDurationMs);
        List<String> emittedFrames = new ArrayList<>();
        String replayResult = ShizukuGamepadSequenceReplay.replay(keyDpad, sequence -> {
            emittedFrames.add(sequence);
            return "ok";
        });
        assertTrue(replayResult.contains("sequence ok"));
        assertEquals(6, emittedFrames.size());
        assertEquals("seq:1,546,1,0;0,0,0,0", emittedFrames.get(0));
        assertEquals("seq:1,304,1,0;1,305,1,0;1,545,0,0;0,0,0,0",
                emittedFrames.get(4));
        assertEquals("seq:1,304,0,0;1,305,0,0;1,547,0,0;0,0,0,0",
                emittedFrames.get(5));
        List<String> failedFrames = new ArrayList<>();
        String failedReplay = ShizukuGamepadSequenceReplay.replay(keyDpad, sequence -> {
            failedFrames.add(sequence);
            return failedFrames.size() == 2 ? "write failed" : "ok";
        });
        assertEquals("write failed", failedReplay);
        assertEquals(3, failedFrames.size());
        assertEquals("seq:1,546,0,0;1,545,0,0;0,0,0,0", failedFrames.get(2));
        GamepadSequencePolicy.Inspection inspection =
                GamepadSequencePolicy.inspect(keyDpad);
        assertFalse(inspection.containsSystemNavigationKey());
        assertFalse(inspection.exceedsReplayLimits());
        assertEquals(220, (int) inspection.replayDurationMs);

        GamepadSequenceComposer.EditableSequence restored =
                GamepadSequenceComposer.parseEditable(keyDpad);
        assertNotNull(restored);
        assertEquals(frames.size(), restored.frames.size());
        assertEquals(40, restored.frameIntervalMs);
        assertEquals(60, restored.finalHoldMs);
        assertEquals(keyDpad, GamepadSequenceComposer.build(restored.frames,
                GamepadSequenceComposer.DPAD_KEYS,
                restored.frameIntervalMs, restored.finalHoldMs));

        List<GamepadSequenceComposer.Frame> chargeFrames = new ArrayList<>();
        chargeFrames.add(new GamepadSequenceComposer.Frame(-1, 0,
                Collections.emptyList(), 1_200));
        chargeFrames.add(new GamepadSequenceComposer.Frame(1, 0,
                Collections.singletonList(GamepadSequenceComposer.BTN_A)));
        String chargeMove = GamepadSequenceComposer.build(chargeFrames,
                GamepadSequenceComposer.DPAD_KEYS, 40, 60);
        assertTrue(chargeMove.contains("0,0,0,500;0,0,0,500;0,0,0,200"));
        GamepadSequenceComposer.EditableSequence restoredCharge =
                GamepadSequenceComposer.parseEditable(chargeMove);
        assertNotNull(restoredCharge);
        assertEquals(2, restoredCharge.frames.size());
        assertEquals(1_200, restoredCharge.frames.get(0).holdOverrideMs);
        assertEquals(chargeMove, GamepadSequenceComposer.build(restoredCharge.frames,
                GamepadSequenceComposer.DPAD_KEYS,
                restoredCharge.frameIntervalMs, restoredCharge.finalHoldMs));

        List<GamepadSequenceComposer.Frame> mirrored =
                GamepadSequenceComposer.mirrorHorizontally(restoredCharge.frames);
        assertEquals(1, mirrored.get(0).directionX);
        assertEquals(-1, mirrored.get(1).directionX);
        assertEquals(1_200, mirrored.get(0).holdOverrideMs);
        assertEquals(restoredCharge.frames.get(1).buttons, mirrored.get(1).buttons);
        List<GamepadSequenceComposer.Frame> mirroredTwice =
                GamepadSequenceComposer.mirrorHorizontally(mirrored);
        assertEquals(restoredCharge.frames.get(0).directionX,
                mirroredTwice.get(0).directionX);
        assertEquals(restoredCharge.frames.get(1).directionX,
                mirroredTwice.get(1).directionX);

        String hatDpad = GamepadSequenceComposer.build(frames,
                GamepadSequenceComposer.DPAD_HAT, 40, 60);
        assertTrue(hatDpad.startsWith("seq:3,16,-1,0;0,0,0,0;3,17,1,40"));
        assertTrue(hatDpad.contains("3,16,0,40"));
        assertTrue(hatDpad.endsWith("3,16,0,0;0,0,0,0"));
        assertEquals("", GamepadSequenceComposer.build(Collections.emptyList(),
                GamepadSequenceComposer.DPAD_KEYS, 40, 60));
        assertEquals(GamepadSequenceComposer.MIN_TIMING_MS,
                GamepadSequenceComposer.clampTiming(0));
        assertEquals(GamepadSequenceComposer.MAX_TIMING_MS,
                GamepadSequenceComposer.clampTiming(900));
        assertEquals(GamepadSequenceComposer.MAX_HOLD_MS,
                GamepadSequenceComposer.clampHold(20_000));
    }

    public void testMacroCloneAndDispatchGateContract() {
        List<GamepadSequenceComposer.Frame> heldFrames = new ArrayList<>();
        heldFrames.add(new GamepadSequenceComposer.Frame(-1, 0,
                Collections.singletonList(GamepadSequenceComposer.BTN_A), 1_200));
        String heldSequence = GamepadSequenceComposer.build(heldFrames,
                GamepadSequenceComposer.DPAD_KEYS, 40, 60);

        Macro source = new Macro("Charge Right", java.util.Arrays.asList(
                new MacroStep(MacroStep.TYPE_WAIT, "30ms"),
                new MacroStep(MacroStep.TYPE_GAMEPAD, heldSequence)));
        source.role = Macro.ROLE_PRIMARY;
        source.highlighted = true;
        source.iconKey = "builtin:ultimate";
        assertTrue(source.hasCancellableControllerHold());

        Macro targetMacro = new Macro("Old target",
                Collections.singletonList(new MacroStep(MacroStep.TYPE_WAIT, "80ms")));
        targetMacro.role = Macro.ROLE_UTILITY;
        targetMacro.iconKey = "builtin:map";
        targetMacro.overwriteFrom(source);
        assertEquals("Charge Right", targetMacro.label);
        assertEquals(Macro.ROLE_PRIMARY, targetMacro.role);
        assertTrue(targetMacro.highlighted);
        assertEquals("builtin:ultimate", targetMacro.iconKey);
        assertEquals(2, targetMacro.steps.size());
        assertTrue(targetMacro.steps.get(0) != source.steps.get(0));
        assertEquals(source.steps.get(1).value, targetMacro.steps.get(1).value);
        assertTrue(targetMacro.hasCancellableControllerHold());
        source.label = "Changed source";
        source.steps.clear();
        assertEquals("Charge Right", targetMacro.label);
        assertEquals(2, targetMacro.steps.size());

        MacroDispatchGate<String> gate = new MacroDispatchGate<>();
        Object ordinary = new Object();
        MacroDispatchGate.Result<String> ordinaryStart =
                gate.onTap(ordinary, false, "ordinary-backend");
        assertEquals(MacroDispatchGate.Decision.START, ordinaryStart.decision);
        for (int tap = 0; tap < 1_000; tap++) {
            assertEquals(MacroDispatchGate.Decision.IGNORE_REPEAT,
                    gate.onTap(ordinary, false, "unused").decision);
        }
        assertEquals(MacroDispatchGate.Decision.BUSY,
                gate.onTap(new Object(), false, "unused").decision);
        gate.finish(ordinaryStart.id + 1);
        assertTrue(gate.hasActive());
        gate.finish(ordinaryStart.id);
        assertFalse(gate.hasActive());

        Object charge = new Object();
        MacroDispatchGate.Result<String> chargeStart =
                gate.onTap(charge, true, "charge-backend");
        assertEquals(MacroDispatchGate.Decision.START, chargeStart.decision);
        MacroDispatchGate.Result<String> cancel =
                gate.onTap(charge, true, "unused");
        assertEquals(MacroDispatchGate.Decision.CANCEL, cancel.decision);
        assertEquals("charge-backend", cancel.payload);
        assertEquals(MacroDispatchGate.Decision.CANCEL_PENDING,
                gate.onTap(charge, true, "unused").decision);
        gate.finish(chargeStart.id);
        assertFalse(gate.hasActive());
        assertEquals(MacroDispatchGate.Decision.START,
                gate.onTap(new Object(), false, "next").decision);
    }

    public void testInterruptedComposedReplayReleasesHeldState() throws Exception {
        List<GamepadSequenceComposer.Frame> heldFrames = Collections.singletonList(
                new GamepadSequenceComposer.Frame(-1, 0,
                        Collections.singletonList(GamepadSequenceComposer.BTN_A), 1_200));
        String sequence = GamepadSequenceComposer.build(heldFrames,
                GamepadSequenceComposer.DPAD_KEYS, 40, 60);
        List<String> emitted = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch pressed = new CountDownLatch(1);
        String[] result = {null};
        Thread replay = new Thread(() -> result[0] =
                ShizukuGamepadSequenceReplay.replay(sequence, frame -> {
                    emitted.add(frame);
                    if (frame.contains("1,546,1,0")
                            && frame.contains("1,304,1,0")) {
                        pressed.countDown();
                    }
                    return "ok";
                }), "controller-cancel-contract");
        replay.start();
        assertTrue(pressed.await(1, TimeUnit.SECONDS));
        Thread.sleep(20L);
        replay.interrupt();
        replay.join(1_000L);
        assertFalse(replay.isAlive());
        assertTrue(result[0] != null && result[0].contains("interrupted"));
        assertTrue(emitted.size() >= 2);
        String release = emitted.get(emitted.size() - 1);
        assertTrue(release.contains("1,304,0,0"));
        assertTrue(release.contains("1,546,0,0"));
        assertTrue(release.endsWith("0,0,0,0"));
    }

    private static void assertSystemNavigationDetected(int scanCode) {
        GamepadSequencePolicy.Inspection inspection = GamepadSequencePolicy.inspect(
                "seq:1," + scanCode + ",1,0;1," + scanCode + ",0,40");
        assertTrue(inspection.containsSystemNavigationKey());
        assertFalse(inspection.hasUnreleasedSystemNavigationKey);
        assertEquals(scanCode, inspection.systemNavigationScanCode);

        GamepadSequencePolicy.Inspection missingRelease =
                GamepadSequencePolicy.inspect("seq:1," + scanCode + ",1,0");
        assertTrue(missingRelease.hasUnreleasedSystemNavigationKey);
    }

    public void testSelfContainedRoundTripAfterSourcesAreDeleted() throws Exception {
        byte[] png = pngFixture();
        Uri iconSource = fixture("icon-source", "雷神.png", png);
        Uri mapSource = fixture("map-source", "地图.html",
                "<html><body>地图版本 A</body></html>".getBytes(StandardCharsets.UTF_8));
        Uri guideSource = fixture("guide-source", "攻略.md",
                "指南版本 B".getBytes(StandardCharsets.UTF_8));
        Uri guideSource2 = fixture("guide-source-2", "攻略.md",
                "指南版本 C".getBytes(StandardCharsets.UTF_8));
        Uri pdfSource = fixture("pdf-source", "世界地图.pdf",
                "%PDF-1.4\n% Heimdall fixture\n".getBytes(StandardCharsets.US_ASCII));

        File macroDirectory = new File(target.getFilesDir(), "macro_icons");
        assertTrue(macroDirectory.exists() || macroDirectory.mkdirs());
        File macroSource = new File(macroDirectory, "roundtrip_original.png");
        writeFile(macroSource, png);
        String digest = ProfileAssetStore.sha256(macroSource);
        String canvasId = CanvasAssetStore.installBundledAsset(
                target, macroSource, digest, "png");

        Macro macro = new Macro("中文宏",
                Collections.singletonList(new MacroStep(MacroStep.TYPE_WAIT, "80ms")));
        macro.iconKey = "user:" + macroSource.getName();
        GameProfile profile = new GameProfile("中文 Profile", "通用", "",
                Collections.singletonList(macro));
        profile.setThemeId(ThemeRegistry.ID_FREYA_CELADON);
        profile.protectThorMappingDuringEnhancedTouch = false;
        profile.touchpadSettings.mode = TouchpadSettings.MODE_VIRTUAL_MOUSE;
        profile.touchpadSettings.virtualMouseSensitivity = 1.35f;
        profile.touchpadSettings.virtualMouseInvertY = true;
        profile.touchpadSettings.virtualMouseScrollDistance = 48f;
        profile.touchpadSettings.virtualMouseFullGestureArea = true;
        profile.iconUri = iconSource.toString();
        profile.maps.add(new MapEntry("同名攻略", mapSource.toString()));
        profile.maps.add(new MapEntry("PDF 地图", pdfSource.toString()));
        profile.interactiveMapTitle = "在线地图";
        profile.interactiveMapUrl = "https://example.com/interactive-map";
        profile.interactiveMapBrowserMode = InteractiveMapBrowserSettings.MODE_DESKTOP;
        GuideEntry bookmarkedGuide = new GuideEntry("同名攻略", GuideEntry.TYPE_FILE,
                guideSource.toString());
        bookmarkedGuide.addBookmark("Boss route", 240, 18, 12, 900,
                "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef");
        profile.guides.add(bookmarkedGuide);
        profile.guides.add(new GuideEntry("同名攻略二", GuideEntry.TYPE_FILE,
                guideSource2.toString()));
        WidgetLayout layout = new WidgetLayout();
        WidgetLayout.Item canvas = new WidgetLayout.Item(WidgetLayout.TYPE_CANVAS, 0, 0, 4, 4);
        canvas.canvasConfig.assetId = canvasId;
        layout.items.add(canvas);
        profile.widgetLayout = layout;

        Uri bundle = ProfileBundleTestProvider.uri("roundtrip-bundle", "迁移包.heimdall-profile");
        ExportResult exported = export(Collections.singletonList(profile), bundle);
        assertNull(exported.failure);
        assertEquals(5, exported.assetCount);
        assertTrue(exported.assetBytes > png.length);

        target.getContentResolver().delete(iconSource, null, null);
        target.getContentResolver().delete(mapSource, null, null);
        target.getContentResolver().delete(guideSource, null, null);
        target.getContentResolver().delete(guideSource2, null, null);
        target.getContentResolver().delete(pdfSource, null, null);
        assertTrue(macroSource.delete());
        File oldCanvas = CanvasAssetStore.resolve(target, canvasId);
        assertNotNull(oldCanvas);
        assertTrue(oldCanvas.delete());

        PrepareResult preparedResult = prepare(bundle);
        assertNull(preparedResult.failure);
        assertNotNull(preparedResult.prepared);
        assertFalse(preparedResult.prepared.legacyJson);
        assertEquals(5, preparedResult.prepared.assetCount);
        InstallResult installed = install(preparedResult.prepared);
        assertNull(installed.failure);
        assertEquals(1, installed.profiles.size());

        GameProfile restored = installed.profiles.get(0);
        assertEquals("中文 Profile", restored.name);
        assertEquals(ThemeRegistry.ID_FREYA_CELADON, restored.normalizedThemeId());
        assertFalse(restored.protectThorMappingDuringEnhancedTouch);
        assertEquals(TouchpadSettings.MODE_VIRTUAL_MOUSE,
                restored.touchpadSettings.mode);
        assertEquals(Float.valueOf(1.35f),
                Float.valueOf(restored.touchpadSettings.virtualMouseSensitivity));
        assertTrue(restored.touchpadSettings.virtualMouseInvertY);
        assertEquals(Float.valueOf(48f),
                Float.valueOf(restored.touchpadSettings.virtualMouseScrollDistance));
        assertTrue(restored.touchpadSettings.virtualMouseFullGestureArea);
        assertTrue(restored.iconUri.startsWith("content://"
                + target.getPackageName() + ".profile-assets/"));
        assertReadable(Uri.parse(restored.iconUri));
        assertEquals(2, restored.maps.size());
        assertEquals("https://example.com/interactive-map", restored.interactiveMapUrl);
        assertEquals(InteractiveMapBrowserSettings.MODE_DESKTOP,
                restored.interactiveMapBrowserMode);
        assertReadable(Uri.parse(restored.maps.get(0).uri));
        assertReadable(Uri.parse(restored.maps.get(1).uri));
        assertEquals(2, restored.guides.size());
        assertReadable(Uri.parse(restored.guides.get(0).content));
        assertReadable(Uri.parse(restored.guides.get(1).content));
        assertEquals(1, restored.guides.get(0).bookmarks.size());
        assertEquals("Boss route", restored.guides.get(0).bookmarks.get(0).label);
        assertEquals(240, restored.guides.get(0).bookmarks.get(0).anchor);
        assertTrue(restored.macros.get(0).iconKey.startsWith("user:bundle_"));
        assertNotNull(MacroIconRepository.findByKey(target, restored.macros.get(0).iconKey));
        String restoredCanvasId = restored.widgetLayout.items.get(0).canvasConfig.assetId;
        assertNotNull(CanvasAssetStore.resolve(target, restoredCanvasId));
    }

    public void testCorruptMissingUnsafeAndOversizedBundlesFailClosed() throws Exception {
        Uri source = fixture("fault-source", "图标.png", pngFixture());
        Macro macro = new Macro("Macro",
                Collections.singletonList(new MacroStep(MacroStep.TYPE_WAIT, "80ms")));
        GameProfile profile = new GameProfile("Fault fixture", "通用", "",
                Collections.singletonList(macro));
        profile.iconUri = source.toString();
        Uri bundle = ProfileBundleTestProvider.uri("fault-bundle", "fault.heimdall-profile");
        assertNull(export(Collections.singletonList(profile), bundle).failure);
        byte[] valid = read(bundle);

        assertPrepareFailure(rewrite(valid, Rewrite.CORRUPT_ASSET), "fault-corrupt",
                ProfileBundleStore.ErrorCode.CORRUPT);
        assertPrepareFailure(rewrite(valid, Rewrite.MISSING_ASSET), "fault-missing",
                ProfileBundleStore.ErrorCode.MISSING_ASSET);
        assertPrepareFailure(rewrite(valid, Rewrite.UNSAFE_PATH), "fault-unsafe",
                ProfileBundleStore.ErrorCode.UNSAFE_PATH);
        assertPrepareFailure(rewrite(valid, Rewrite.OVERSIZED_ASSET), "fault-large",
                ProfileBundleStore.ErrorCode.TOO_LARGE);

        Uri missing = ProfileBundleTestProvider.uri("missing-source", "missing.png");
        profile.iconUri = missing.toString();
        Uri failedExport = ProfileBundleTestProvider.uri("failed-export", "failed.zip");
        assertEquals(ProfileBundleStore.ErrorCode.MISSING_ASSET,
                export(Collections.singletonList(profile), failedExport).failure.code);
    }

    public void testLegacyProfileJsonRemainsImportable() throws Exception {
        Macro macro = new Macro("Legacy",
                Collections.singletonList(new MacroStep(MacroStep.TYPE_WAIT, "80ms")));
        GameProfile profile = new GameProfile("旧版 Profile", "通用", "",
                Collections.singletonList(macro));
        JSONObject legacyProfile = profile.toJson();
        legacyProfile.remove("protectThorMappingDuringEnhancedTouch");
        JSONObject legacyTouchpad = legacyProfile.getJSONObject("touchpadSettings");
        legacyTouchpad.remove("virtual_mouse_sensitivity");
        legacyTouchpad.remove("virtual_mouse_invert_y");
        legacyTouchpad.remove("virtual_mouse_scroll_distance");
        legacyTouchpad.remove("virtual_mouse_full_gesture_area");
        JSONArray legacy = new JSONArray().put(legacyProfile);
        Uri source = fixture("legacy-json", "旧版.json",
                legacy.toString().getBytes(StandardCharsets.UTF_8));
        PrepareResult result = prepare(source);
        assertNull(result.failure);
        assertNotNull(result.prepared);
        assertTrue(result.prepared.legacyJson);
        assertEquals("旧版 Profile", result.prepared.profiles.get(0).name);
        assertEquals("", result.prepared.profiles.get(0).normalizedThemeId());
        assertTrue(result.prepared.profiles.get(0)
                .protectThorMappingDuringEnhancedTouch);
        TouchpadSettings restoredTouchpad = result.prepared.profiles.get(0).touchpadSettings;
        assertEquals(Float.valueOf(1f), Float.valueOf(restoredTouchpad.virtualMouseSensitivity));
        assertFalse(restoredTouchpad.virtualMouseInvertY);
        assertEquals(Float.valueOf(36f), Float.valueOf(restoredTouchpad.virtualMouseScrollDistance));
        assertFalse(restoredTouchpad.virtualMouseFullGestureArea);
        result.prepared.close();
    }

    private ExportResult export(List<GameProfile> profiles, Uri destination) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        ExportResult result = new ExportResult();
        ProfileBundleStore.exportAsync(target, profiles, "2026-08-03T00:00:00Z", destination,
                new ProfileBundleStore.ExportCallback() {
                    @Override
                    public void onExported(int assetCount, long assetBytes) {
                        result.assetCount = assetCount;
                        result.assetBytes = assetBytes;
                        latch.countDown();
                    }

                    @Override
                    public void onError(ProfileBundleStore.Failure failure) {
                        result.failure = failure;
                        latch.countDown();
                    }
                });
        assertTrue("Export timed out", latch.await(30, TimeUnit.SECONDS));
        return result;
    }

    private PrepareResult prepare(Uri source) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        PrepareResult result = new PrepareResult();
        ProfileBundleStore.prepareImportAsync(target, source,
                new ProfileBundleStore.ImportCallback() {
                    @Override
                    public void onPrepared(ProfileBundleStore.PreparedImport preparedImport) {
                        result.prepared = preparedImport;
                        latch.countDown();
                    }

                    @Override
                    public void onError(ProfileBundleStore.Failure failure) {
                        result.failure = failure;
                        latch.countDown();
                    }
                });
        assertTrue("Import preparation timed out", latch.await(30, TimeUnit.SECONDS));
        return result;
    }

    private InstallResult install(ProfileBundleStore.PreparedImport prepared) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        InstallResult result = new InstallResult();
        ProfileBundleStore.installAsync(target, prepared, new ProfileBundleStore.InstallCallback() {
            @Override
            public void onInstalled(List<GameProfile> profiles) {
                result.profiles = profiles;
                latch.countDown();
            }

            @Override
            public void onError(ProfileBundleStore.Failure failure) {
                result.failure = failure;
                latch.countDown();
            }
        });
        assertTrue("Asset installation timed out", latch.await(30, TimeUnit.SECONDS));
        return result;
    }

    private void assertPrepareFailure(byte[] bytes, String token,
            ProfileBundleStore.ErrorCode expected) throws Exception {
        Uri uri = fixture(token, token + ".heimdall-profile", bytes);
        PrepareResult result = prepare(uri);
        assertNull(result.prepared);
        assertNotNull(result.failure);
        if (expected != result.failure.code) {
            throw new AssertionError("Expected " + expected + " but was "
                    + result.failure.code + ", detail=" + result.failure.detail
                    + ", cause=" + result.failure.getCause());
        }
    }

    private Uri fixture(String token, String displayName, byte[] bytes) throws Exception {
        Uri uri = ProfileBundleTestProvider.uri(token, displayName);
        try (OutputStream output = target.getContentResolver().openOutputStream(uri, "wt")) {
            assertNotNull(output);
            output.write(bytes);
        }
        return uri;
    }

    private byte[] read(Uri uri) throws Exception {
        try (InputStream input = target.getContentResolver().openInputStream(uri);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            assertNotNull(input);
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            return output.toByteArray();
        }
    }

    private void assertReadable(Uri uri) throws Exception {
        try (InputStream input = target.getContentResolver().openInputStream(uri)) {
            assertNotNull(input);
            assertTrue(input.read() >= 0);
        }
    }

    private static byte[] pngFixture() throws Exception {
        Bitmap bitmap = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888);
        bitmap.eraseColor(Color.rgb(74, 59, 96));
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output));
        bitmap.recycle();
        return output.toByteArray();
    }

    private static void writeFile(File file, byte[] bytes) throws Exception {
        try (FileOutputStream output = new FileOutputStream(file)) {
            output.write(bytes);
            output.flush();
            output.getFD().sync();
        }
    }

    private static byte[] rewrite(byte[] source, Rewrite rewrite) throws Exception {
        ByteArrayOutputStream destination = new ByteArrayOutputStream();
        boolean changed = false;
        try (ZipInputStream input = new ZipInputStream(new ByteArrayInputStream(source));
             ZipOutputStream output = new ZipOutputStream(destination)) {
            ZipEntry entry;
            while ((entry = input.getNextEntry()) != null) {
                byte[] content = readAll(input);
                boolean asset = entry.getName().startsWith("assets/");
                if (rewrite == Rewrite.MISSING_ASSET && asset && !changed) {
                    changed = true;
                    continue;
                }
                if (rewrite == Rewrite.CORRUPT_ASSET && asset && !changed) {
                    content[0] ^= 0x01;
                    changed = true;
                }
                if (rewrite == Rewrite.OVERSIZED_ASSET
                        && "manifest.json".equals(entry.getName())) {
                    JSONObject manifest = new JSONObject(new String(content, StandardCharsets.UTF_8));
                    JSONObject first = manifest.getJSONArray("assets").getJSONObject(0);
                    first.put("size", 129L * 1024L * 1024L);
                    content = manifest.toString().getBytes(StandardCharsets.UTF_8);
                    changed = true;
                }
                output.putNextEntry(new ZipEntry(entry.getName()));
                output.write(content);
                output.closeEntry();
            }
            if (rewrite == Rewrite.UNSAFE_PATH) {
                output.putNextEntry(new ZipEntry("../escape.txt"));
                output.write('x');
                output.closeEntry();
                changed = true;
            }
        }
        assertTrue(changed);
        return destination.toByteArray();
    }

    private static byte[] readAll(InputStream input) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int count;
        while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
        return output.toByteArray();
    }

    private enum Rewrite { CORRUPT_ASSET, MISSING_ASSET, UNSAFE_PATH, OVERSIZED_ASSET }

    private static final class ExportResult {
        int assetCount;
        long assetBytes;
        ProfileBundleStore.Failure failure;
    }

    private static final class PrepareResult {
        ProfileBundleStore.PreparedImport prepared;
        ProfileBundleStore.Failure failure;
    }

    private static final class InstallResult {
        List<GameProfile> profiles = new ArrayList<>();
        ProfileBundleStore.Failure failure;
    }

    private static void assertTrue(boolean value) {
        if (!value) throw new AssertionError("Expected true");
    }

    private static void assertTrue(String message, boolean value) {
        if (!value) throw new AssertionError(message);
    }

    private static void assertFalse(boolean value) {
        if (value) throw new AssertionError("Expected false");
    }

    private static void assertNull(Object value) {
        if (value != null) throw new AssertionError("Expected null but was " + value);
    }

    private static void assertNotNull(Object value) {
        if (value == null) throw new AssertionError("Expected non-null value");
    }

    private static void assertEquals(Object expected, Object actual) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError("Expected " + expected + " but was " + actual);
        }
    }

    private static void assertEquals(int expected, int actual) {
        if (expected != actual) {
            throw new AssertionError("Expected " + expected + " but was " + actual);
        }
    }
}
