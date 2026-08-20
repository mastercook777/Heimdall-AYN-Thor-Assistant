package com.mastercook777.heimdall;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
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
import java.util.Collections;
import java.util.List;
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
            testVirtualKeyboardTransportContract();
            testKeyboardPadModelContract();
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
