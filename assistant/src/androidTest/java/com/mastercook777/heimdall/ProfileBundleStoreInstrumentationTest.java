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
            testControllerSequenceSafetyPolicy();
            testComposedControllerSequenceContract();
            testMacroCloneAndDispatchGateContract();
            testInterruptedComposedReplayReleasesHeldState();
            testVirtualKeyboardTransportContract();
            testKeyboardPadModelContract();
            testQuickActionsModelContract();
            testCanvasRuntimeDecodePolicy();
            testProfileIconDecodePolicy();
            testUserMacroIconDeletionContract();
            testGameContextIdentityAndResolverContract();
            testGameContextAppOnlyFallbackAndManualSelectionGuardContract();
            testRomContextProbeAdapterTargetsAndFiltering();
            testEdenGameContextIdentityAndResolverContract();
            testPpssppPositiveLaunchGameContextContract();
            testRetroArchTwoLevelGameContextContract();
            testAssistantActivitySingleTaskContract();
            testSecondaryDisplayLaunchRouterContract();
            testUpperDisplaySingleTouchHandoffCoordinateContract();
            testUpperDisplayStartedLifecycleHandoffContract();
            testStartupCapabilityDefaultContract();
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
        assertEquals(1200, CanvasImageLoader.runtimeDecodeMaxSide(
                600, 400, Float.NaN));
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

    public void testRomContextProbeAdapterTargetsAndFiltering() {
        assertEquals("retroarch", RomContextProbeCollector.targetName(
                RomContextProbeCollector.TARGET_RETROARCH));
        assertEquals("ppsspp", RomContextProbeCollector.targetName(
                RomContextProbeCollector.TARGET_PPSSPP));
        assertEquals("eden", RomContextProbeCollector.targetName(
                RomContextProbeCollector.TARGET_EDEN));
        String filtered = RomContextProbeCollector.filterIdentityCandidates(
                "I Eden: [EmulationActivity] Begin ROM swap: data=content://games/Game.xci\n"
                        + "I Noise: unrelated renderer frame\n");
        assertTrue(filtered.contains("content://games/Game.xci"));
        assertFalse(filtered.contains("unrelated renderer frame"));
        String ppssppStop = RomContextProbeCollector.filterIdentityCandidates(
                "I Loader: REQUEST_GAME_STOP: running exit callback before shutdown.\n");
        assertTrue(ppssppStop.contains("REQUEST_GAME_STOP"));
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
        profile.protectThorMappingDuringEnhancedTouch = false;
        profile.touchpadSettings.mode = TouchpadSettings.MODE_VIRTUAL_MOUSE;
        profile.touchpadSettings.virtualMouseSensitivity = 1.35f;
        profile.touchpadSettings.virtualMouseInvertY = true;
        profile.touchpadSettings.virtualMouseScrollDistance = 48f;
        profile.touchpadSettings.virtualMouseFullGestureArea = true;
        profile.iconUri = iconSource.toString();
        profile.maps.add(new MapEntry("同名攻略", mapSource.toString()));
        profile.maps.add(new MapEntry("PDF 地图", pdfSource.toString()));
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
