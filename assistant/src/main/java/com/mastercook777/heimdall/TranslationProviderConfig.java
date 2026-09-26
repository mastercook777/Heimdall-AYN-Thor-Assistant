package com.mastercook777.heimdall;

public final class TranslationProviderConfig {
    public static final String PROVIDER_SILICONFLOW = "siliconflow";
    public static final String PROVIDER_CUSTOM = "custom_openai";
    public static final String REGION_CHINA = "china";
    public static final String REGION_GLOBAL = "global";
    public static final String SILICONFLOW_CHINA_BASE_URL = "https://api.siliconflow.cn/v1";
    public static final String SILICONFLOW_GLOBAL_BASE_URL = "https://api.siliconflow.com/v1";
    public static final String SILICONFLOW_CHINA_MODEL = "tencent/Hunyuan-MT-7B";
    public static final String SILICONFLOW_GLOBAL_MODEL = "Qwen/Qwen3-8B";

    public String provider = PROVIDER_SILICONFLOW;
    public String region = REGION_CHINA;
    public String apiKey = "";
    public String customBaseUrl = "";
    public String customModel = "";

    public TranslationProviderConfig copy() {
        TranslationProviderConfig result = new TranslationProviderConfig();
        result.provider = provider;
        result.region = region;
        result.apiKey = apiKey;
        result.customBaseUrl = customBaseUrl;
        result.customModel = customModel;
        result.sanitize();
        return result;
    }

    public void sanitize() {
        provider = PROVIDER_CUSTOM.equals(provider) ? PROVIDER_CUSTOM : PROVIDER_SILICONFLOW;
        region = REGION_GLOBAL.equals(region) ? REGION_GLOBAL : REGION_CHINA;
        apiKey = safe(apiKey);
        customBaseUrl = safe(customBaseUrl);
        customModel = safe(customModel);
    }

    public String resolvedBaseUrl() {
        sanitize();
        if (PROVIDER_CUSTOM.equals(provider)) return customBaseUrl;
        return REGION_GLOBAL.equals(region)
                ? SILICONFLOW_GLOBAL_BASE_URL : SILICONFLOW_CHINA_BASE_URL;
    }

    public String resolvedModel() {
        sanitize();
        if (PROVIDER_CUSTOM.equals(provider)) return customModel;
        return REGION_GLOBAL.equals(region)
                ? SILICONFLOW_GLOBAL_MODEL : SILICONFLOW_CHINA_MODEL;
    }

    public boolean isComplete() {
        sanitize();
        String baseUrl = resolvedBaseUrl();
        return apiKey.length() > 0 && resolvedModel().length() > 0
                && baseUrl.startsWith("https://");
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
