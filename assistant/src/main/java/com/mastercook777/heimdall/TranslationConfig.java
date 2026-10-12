package com.mastercook777.heimdall;

import org.json.JSONException;
import org.json.JSONObject;

public final class TranslationConfig {
    public static final String SCRIPT_LATIN = "latin";
    public static final String SCRIPT_JAPANESE = "japanese";
    public static final String SCRIPT_CHINESE = "chinese";
    public static final String SCRIPT_KOREAN = "korean";

    public static final String LANGUAGE_CHINESE_SIMPLIFIED = "zh-CN";
    public static final String LANGUAGE_CHINESE_TRADITIONAL = "zh-TW";
    public static final String LANGUAGE_ENGLISH = "en";
    public static final String LANGUAGE_JAPANESE = "ja";
    public static final String LANGUAGE_KOREAN = "ko";

    public static final String TEXT_SIZE_COMPACT = "compact";
    public static final String TEXT_SIZE_STANDARD = "standard";
    public static final String TEXT_SIZE_LARGE = "large";
    public static final String TEXT_SIZE_EXTRA_LARGE = "extra_large";
    public static final String LINE_SPACING_COMPACT = "compact";
    public static final String LINE_SPACING_STANDARD = "standard";
    public static final String LINE_SPACING_RELAXED = "relaxed";

    public float regionLeft = 0.10f;
    public float regionTop = 0.65f;
    public float regionRight = 0.90f;
    public float regionBottom = 0.90f;
    public String ocrScript = SCRIPT_JAPANESE;
    public String targetLanguage = LANGUAGE_CHINESE_SIMPLIFIED;
    public String textSizePreset = TEXT_SIZE_COMPACT;
    public String lineSpacingPreset = LINE_SPACING_STANDARD;

    public TranslationConfig copy() {
        TranslationConfig result = new TranslationConfig();
        result.regionLeft = regionLeft;
        result.regionTop = regionTop;
        result.regionRight = regionRight;
        result.regionBottom = regionBottom;
        result.ocrScript = ocrScript;
        result.targetLanguage = targetLanguage;
        result.textSizePreset = textSizePreset;
        result.lineSpacingPreset = lineSpacingPreset;
        result.sanitize();
        return result;
    }

    public void sanitize() {
        regionLeft = clamp(regionLeft, 0f, 0.98f);
        regionTop = clamp(regionTop, 0f, 0.98f);
        regionRight = clamp(regionRight, regionLeft + 0.02f, 1f);
        regionBottom = clamp(regionBottom, regionTop + 0.02f, 1f);
        ocrScript = normalizeScript(ocrScript);
        targetLanguage = normalizeTargetLanguage(targetLanguage);
        textSizePreset = normalizeTextSizePreset(textSizePreset);
        lineSpacingPreset = normalizeLineSpacingPreset(lineSpacingPreset);
    }

    public JSONObject toJson() throws JSONException {
        sanitize();
        JSONObject object = new JSONObject();
        object.put("regionLeft", regionLeft);
        object.put("regionTop", regionTop);
        object.put("regionRight", regionRight);
        object.put("regionBottom", regionBottom);
        object.put("ocrScript", ocrScript);
        object.put("targetLanguage", targetLanguage);
        object.put("textSizePreset", textSizePreset);
        object.put("lineSpacingPreset", lineSpacingPreset);
        return object;
    }

    public static TranslationConfig fromJson(JSONObject object) {
        TranslationConfig result = new TranslationConfig();
        if (object != null) {
            result.regionLeft = (float) object.optDouble("regionLeft", result.regionLeft);
            result.regionTop = (float) object.optDouble("regionTop", result.regionTop);
            result.regionRight = (float) object.optDouble("regionRight", result.regionRight);
            result.regionBottom = (float) object.optDouble("regionBottom", result.regionBottom);
            result.ocrScript = object.optString("ocrScript", result.ocrScript);
            result.targetLanguage = object.optString("targetLanguage", result.targetLanguage);
            result.textSizePreset = object.optString(
                    "textSizePreset", result.textSizePreset);
            result.lineSpacingPreset = object.optString(
                    "lineSpacingPreset", result.lineSpacingPreset);
        }
        result.sanitize();
        return result;
    }

    public static String normalizeScript(String value) {
        if (SCRIPT_LATIN.equals(value) || SCRIPT_CHINESE.equals(value)
                || SCRIPT_KOREAN.equals(value)) {
            return value;
        }
        return SCRIPT_JAPANESE;
    }

    public static String normalizeTargetLanguage(String value) {
        if (LANGUAGE_CHINESE_TRADITIONAL.equals(value)
                || LANGUAGE_ENGLISH.equals(value)
                || LANGUAGE_JAPANESE.equals(value)
                || LANGUAGE_KOREAN.equals(value)) {
            return value;
        }
        return LANGUAGE_CHINESE_SIMPLIFIED;
    }

    public static String normalizeTextSizePreset(String value) {
        if (TEXT_SIZE_COMPACT.equals(value) || TEXT_SIZE_LARGE.equals(value)
                || TEXT_SIZE_EXTRA_LARGE.equals(value)) {
            return value;
        }
        return TEXT_SIZE_STANDARD;
    }

    public static String normalizeLineSpacingPreset(String value) {
        if (LINE_SPACING_COMPACT.equals(value) || LINE_SPACING_RELAXED.equals(value)) {
            return value;
        }
        return LINE_SPACING_STANDARD;
    }

    public int textSizeSp() {
        String normalized = normalizeTextSizePreset(textSizePreset);
        if (TEXT_SIZE_COMPACT.equals(normalized)) return 14;
        if (TEXT_SIZE_LARGE.equals(normalized)) return 18;
        if (TEXT_SIZE_EXTRA_LARGE.equals(normalized)) return 22;
        return 16;
    }

    public int lineSpacingExtraDp() {
        String normalized = normalizeLineSpacingPreset(lineSpacingPreset);
        if (LINE_SPACING_COMPACT.equals(normalized)) return 0;
        if (LINE_SPACING_RELAXED.equals(normalized)) return 5;
        return 2;
    }

    public static String sourceCode(String script) {
        String normalized = normalizeScript(script);
        if (SCRIPT_JAPANESE.equals(normalized)) return "JP";
        if (SCRIPT_CHINESE.equals(normalized)) return "\u4e2d\u6587";
        if (SCRIPT_KOREAN.equals(normalized)) return "KO";
        return "EN";
    }

    public static String targetCode(String language) {
        String normalized = normalizeTargetLanguage(language);
        if (LANGUAGE_JAPANESE.equals(normalized)) return "JP";
        if (LANGUAGE_KOREAN.equals(normalized)) return "KO";
        if (LANGUAGE_ENGLISH.equals(normalized)) return "EN";
        if (LANGUAGE_CHINESE_TRADITIONAL.equals(normalized)) return "\u7e41\u4e2d";
        return "\u7b80\u4e2d";
    }

    public static String targetPromptName(String language) {
        String normalized = normalizeTargetLanguage(language);
        if (LANGUAGE_CHINESE_TRADITIONAL.equals(normalized)) return "Traditional Chinese";
        if (LANGUAGE_ENGLISH.equals(normalized)) return "English";
        if (LANGUAGE_JAPANESE.equals(normalized)) return "Japanese";
        if (LANGUAGE_KOREAN.equals(normalized)) return "Korean";
        return "Simplified Chinese";
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
