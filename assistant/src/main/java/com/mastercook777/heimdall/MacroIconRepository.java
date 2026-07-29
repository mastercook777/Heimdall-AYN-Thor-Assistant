package com.mastercook777.heimdall;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

final class MacroIconRepository {
    private static final String ASSET_DIR = "macro_icons";
    private static final String ASSET_PREFIX = "asset:";
    private static final String USER_PREFIX = "user:";
    private static final String BUILTIN_PREFIX = "builtin:";
    private static final String USER_DIR = "macro_icons";
    private static final int NORMALIZED_SIZE = 256;
    private static final int CONTENT_SIZE = 240;
    private static final int MAX_SOURCE_SIDE = 4096;
    private static final long MAX_SOURCE_BYTES = 8L * 1024L * 1024L;

    private MacroIconRepository() {
    }

    static MacroIconOption resolve(Context context, Macro macro) {
        if (macro != null && macro.iconKey != null && macro.iconKey.trim().length() > 0) {
            MacroIconOption option = findByKey(context, macro.iconKey.trim());
            if (option != null) {
                return option;
            }
        }
        return defaultOption(context);
    }

    static MacroIconOption findByKey(Context context, String key) {
        if (key == null || key.trim().length() == 0) {
            return null;
        }
        String normalized = key.trim();
        for (MacroIconOption option : builtInOptions(context)) {
            if (option.key.equals(normalized)) {
                return option;
            }
        }
        if (normalized.startsWith(ASSET_PREFIX) || normalized.startsWith(USER_PREFIX)) {
            for (MacroIconOption option : customOptions(context)) {
                if (option.key.equals(normalized)) {
                    return option;
                }
            }
        }
        return null;
    }

    static MacroIconOption defaultOption(Context context) {
        return MacroIconOption.resource(BUILTIN_PREFIX + "default",
                R.drawable.ic_macro_default, context.getString(R.string.macro_icon_default));
    }

    static List<MacroIconOption> builtInOptions(Context context) {
        List<MacroIconOption> options = new ArrayList<>();
        options.add(defaultOption(context));
        options.add(MacroIconOption.resource(BUILTIN_PREFIX + "combo",
                R.drawable.ic_macro_combo, context.getString(R.string.macro_icon_combo)));
        options.add(MacroIconOption.resource(BUILTIN_PREFIX + "save",
                R.drawable.ic_macro_save, context.getString(R.string.macro_icon_save)));
        options.add(MacroIconOption.resource(BUILTIN_PREFIX + "load",
                R.drawable.ic_macro_load, context.getString(R.string.macro_icon_load)));
        options.add(MacroIconOption.resource(BUILTIN_PREFIX + "fast_forward",
                R.drawable.ic_macro_fast_forward,
                context.getString(R.string.macro_icon_fast_forward)));
        options.add(MacroIconOption.resource(BUILTIN_PREFIX + "peak_left",
                R.drawable.ic_macro_peak_left, context.getString(R.string.macro_icon_peek_left)));
        options.add(MacroIconOption.resource(BUILTIN_PREFIX + "peak_right",
                R.drawable.ic_macro_peak_right, context.getString(R.string.macro_icon_peek_right)));
        options.add(MacroIconOption.resource(BUILTIN_PREFIX + "shoulder_l1",
                R.drawable.ic_macro_l1, "L1"));
        options.add(MacroIconOption.resource(BUILTIN_PREFIX + "shoulder_r1",
                R.drawable.ic_macro_r1, "R1"));
        options.add(MacroIconOption.resource(BUILTIN_PREFIX + "skill",
                R.drawable.ic_macro_skill, context.getString(R.string.macro_icon_skill)));
        options.add(MacroIconOption.resource(BUILTIN_PREFIX + "ultimate",
                R.drawable.ic_macro_ultimate, context.getString(R.string.macro_icon_ultimate)));
        options.add(MacroIconOption.resource(BUILTIN_PREFIX + "operator_utility",
                R.drawable.ic_macro_utility, "Utility"));
        options.add(MacroIconOption.resource(BUILTIN_PREFIX + "gamepad",
                R.drawable.ic_macro_gamepad, context.getString(R.string.macro_icon_gamepad)));
        options.add(MacroIconOption.resource(BUILTIN_PREFIX + "mount",
                R.drawable.ic_macro_mount, context.getString(R.string.macro_icon_mount)));
        options.add(MacroIconOption.resource(BUILTIN_PREFIX + "vehicle",
                R.drawable.ic_macro_vehicle, context.getString(R.string.macro_icon_vehicle)));
        options.add(MacroIconOption.resource(BUILTIN_PREFIX + "backpack",
                R.drawable.ic_macro_backpack, context.getString(R.string.macro_icon_backpack)));
        options.add(MacroIconOption.resource(BUILTIN_PREFIX + "healing",
                R.drawable.ic_macro_healing, context.getString(R.string.macro_icon_healing)));
        options.add(MacroIconOption.resource(BUILTIN_PREFIX + "throwable",
                R.drawable.ic_macro_throwable, context.getString(R.string.macro_icon_throwable)));
        options.add(MacroIconOption.resource(BUILTIN_PREFIX + "pet",
                R.drawable.ic_macro_pet, context.getString(R.string.macro_icon_pet)));
        options.add(MacroIconOption.resource(BUILTIN_PREFIX + "quest",
                R.drawable.ic_macro_quest, context.getString(R.string.macro_icon_quest)));
        options.add(MacroIconOption.resource(BUILTIN_PREFIX + "interact",
                R.drawable.ic_macro_interact, context.getString(R.string.macro_icon_interact)));
        options.add(MacroIconOption.resource(BUILTIN_PREFIX + "crosshair",
                R.drawable.ic_macro_crosshair, context.getString(R.string.macro_icon_crosshair)));
        options.add(MacroIconOption.resource(BUILTIN_PREFIX + "reload",
                R.drawable.ic_macro_reload, context.getString(R.string.macro_icon_reload)));
        options.add(MacroIconOption.resource(BUILTIN_PREFIX + "sprint",
                R.drawable.ic_macro_sprint, context.getString(R.string.macro_icon_sprint)));
        options.add(MacroIconOption.resource(BUILTIN_PREFIX + "defense",
                R.drawable.ic_macro_defense, context.getString(R.string.macro_icon_defense)));
        options.add(MacroIconOption.resource(BUILTIN_PREFIX + "map",
                R.drawable.ic_macro_map, context.getString(R.string.macro_icon_map)));
        options.add(MacroIconOption.resource(BUILTIN_PREFIX + "pause",
                R.drawable.ic_macro_pause, context.getString(R.string.macro_icon_pause)));
        options.add(MacroIconOption.resource(BUILTIN_PREFIX + "menu",
                R.drawable.ic_macro_menu, context.getString(R.string.macro_icon_menu)));
        options.add(MacroIconOption.resource(BUILTIN_PREFIX + "direction_up",
                R.drawable.ic_macro_direction_up, context.getString(R.string.macro_icon_direction_up)));
        options.add(MacroIconOption.resource(BUILTIN_PREFIX + "direction_down",
                R.drawable.ic_macro_direction_down, context.getString(R.string.macro_icon_direction_down)));
        options.add(MacroIconOption.resource(BUILTIN_PREFIX + "direction_left",
                R.drawable.ic_macro_direction_left, context.getString(R.string.macro_icon_direction_left)));
        options.add(MacroIconOption.resource(BUILTIN_PREFIX + "direction_right",
                R.drawable.ic_macro_direction_right, context.getString(R.string.macro_icon_direction_right)));
        options.add(MacroIconOption.resource(BUILTIN_PREFIX + "previous",
                R.drawable.ic_macro_previous, context.getString(R.string.macro_icon_previous)));
        options.add(MacroIconOption.resource(BUILTIN_PREFIX + "next",
                R.drawable.ic_macro_next, context.getString(R.string.macro_icon_next)));
        return Collections.unmodifiableList(options);
    }

    static List<MacroIconOption> customOptions(Context context) {
        List<MacroIconOption> options = new ArrayList<>();
        File directory = userDirectory(context);
        File[] userFiles = directory.listFiles((dir, name) -> name != null
                && name.toLowerCase(Locale.US).endsWith(".png"));
        if (userFiles != null) {
            Arrays.sort(userFiles, (left, right) -> left.getName().compareToIgnoreCase(right.getName()));
            for (int i = 0; i < userFiles.length; i++) {
                File file = userFiles[i];
                options.add(MacroIconOption.file(USER_PREFIX + file.getName(), file.getAbsolutePath(),
                        context.getString(R.string.macro_icon_custom_number, i + 1)));
            }
        }

        String[] files;
        try {
            files = context.getAssets().list(ASSET_DIR);
        } catch (IOException ignored) {
            files = null;
        }
        if (files == null || files.length == 0) {
            return options;
        }
        Arrays.sort(files, String.CASE_INSENSITIVE_ORDER);
        for (String file : files) {
            if (file == null || !file.toLowerCase(Locale.US).endsWith(".png")) {
                continue;
            }
            String assetPath = ASSET_DIR + "/" + file;
            String key = ASSET_PREFIX + file;
            options.add(MacroIconOption.asset(key, assetPath, displayNameFor(file)));
        }
        return options;
    }

    static MacroIconOption importUserIcon(Context context, Uri uri) throws IOException {
        if (context == null || uri == null) {
            throw new IOException("Missing icon source");
        }
        try (AssetFileDescriptor descriptor = context.getContentResolver().openAssetFileDescriptor(uri, "r")) {
            if (descriptor != null && descriptor.getLength() > MAX_SOURCE_BYTES) {
                throw new IOException("Icon exceeds 8 MB");
            }
        }
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream stream = context.getContentResolver().openInputStream(uri)) {
            if (stream == null) {
                throw new IOException("Unable to open icon");
            }
            BitmapFactory.decodeStream(stream, null, bounds);
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0
                || bounds.outWidth > MAX_SOURCE_SIDE || bounds.outHeight > MAX_SOURCE_SIDE) {
            throw new IOException("Icon must be a PNG or WebP no larger than 4096px");
        }

        BitmapFactory.Options decode = new BitmapFactory.Options();
        decode.inPreferredConfig = Bitmap.Config.ARGB_8888;
        decode.inSampleSize = 1;
        while (Math.max(bounds.outWidth, bounds.outHeight) / decode.inSampleSize > 1024) {
            decode.inSampleSize *= 2;
        }
        Bitmap source;
        try (InputStream stream = context.getContentResolver().openInputStream(uri)) {
            source = stream == null ? null : BitmapFactory.decodeStream(stream, null, decode);
        }
        if (source == null) {
            throw new IOException("Unable to decode icon");
        }

        Rect visible = visibleBounds(source);
        if (visible.isEmpty()) {
            source.recycle();
            throw new IOException("Icon has no visible pixels");
        }
        Bitmap normalized = Bitmap.createBitmap(NORMALIZED_SIZE, NORMALIZED_SIZE, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(normalized);
        canvas.drawColor(Color.TRANSPARENT);
        float scale = Math.min(CONTENT_SIZE / (float) visible.width(), CONTENT_SIZE / (float) visible.height());
        float width = visible.width() * scale;
        float height = visible.height() * scale;
        RectF target = new RectF((NORMALIZED_SIZE - width) / 2f, (NORMALIZED_SIZE - height) / 2f,
                (NORMALIZED_SIZE + width) / 2f, (NORMALIZED_SIZE + height) / 2f);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);
        canvas.drawBitmap(source, visible, target, paint);
        source.recycle();

        File directory = userDirectory(context);
        if (!directory.exists() && !directory.mkdirs()) {
            normalized.recycle();
            throw new IOException("Unable to create icon storage");
        }
        String fileName = "icon_" + System.currentTimeMillis() + ".png";
        File output = new File(directory, fileName);
        boolean saved;
        try (FileOutputStream stream = new FileOutputStream(output)) {
            saved = normalized.compress(Bitmap.CompressFormat.PNG, 100, stream);
        } finally {
            normalized.recycle();
        }
        if (!saved) {
            output.delete();
            throw new IOException("Unable to save icon");
        }
        return MacroIconOption.file(USER_PREFIX + fileName, output.getAbsolutePath(),
                context.getString(R.string.macro_icon_custom));
    }

    private static Rect visibleBounds(Bitmap bitmap) {
        int minX = bitmap.getWidth();
        int minY = bitmap.getHeight();
        int maxX = -1;
        int maxY = -1;
        for (int y = 0; y < bitmap.getHeight(); y++) {
            for (int x = 0; x < bitmap.getWidth(); x++) {
                if (Color.alpha(bitmap.getPixel(x, y)) <= 16) {
                    continue;
                }
                minX = Math.min(minX, x);
                minY = Math.min(minY, y);
                maxX = Math.max(maxX, x);
                maxY = Math.max(maxY, y);
            }
        }
        return maxX < minX || maxY < minY ? new Rect() : new Rect(minX, minY, maxX + 1, maxY + 1);
    }

    private static File userDirectory(Context context) {
        return new File(context.getFilesDir(), USER_DIR);
    }

    private static String displayNameFor(String fileName) {
        String name = fileName.replace(".png", "").replace('_', ' ').trim();
        if (name.length() == 0) {
            return "Icon";
        }
        StringBuilder builder = new StringBuilder();
        String[] parts = name.split("\\s+");
        for (String part : parts) {
            if (part.length() == 0) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                builder.append(part.substring(1));
            }
        }
        return builder.toString();
    }

    static final class MacroIconOption {
        final String key;
        final String assetPath;
        final String filePath;
        final int drawableRes;
        final String displayName;
        final boolean tintable;

        private MacroIconOption(String key, String assetPath, String filePath, int drawableRes,
                                String displayName, boolean tintable) {
            this.key = key;
            this.assetPath = assetPath;
            this.filePath = filePath;
            this.drawableRes = drawableRes;
            this.displayName = displayName;
            this.tintable = tintable;
        }

        static MacroIconOption resource(String key, int drawableRes, String displayName) {
            return new MacroIconOption(key, null, null, drawableRes, displayName, true);
        }

        static MacroIconOption asset(String key, String assetPath, String displayName) {
            return new MacroIconOption(key, assetPath, null, 0, displayName, false);
        }

        static MacroIconOption file(String key, String filePath, String displayName) {
            return new MacroIconOption(key, null, filePath, 0, displayName, false);
        }

        Drawable load(Context context) {
            if (assetPath != null) {
                try (InputStream stream = context.getAssets().open(assetPath)) {
                    return Drawable.createFromStream(stream, assetPath);
                } catch (IOException ignored) {
                    return null;
                }
            }
            if (filePath != null) {
                return Drawable.createFromPath(filePath);
            }
            if (drawableRes == 0) {
                return null;
            }
            return context.getDrawable(drawableRes);
        }
    }
}
