package com.mastercook777.heimdall;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

final class TranslationSettingsStore {
    private static final String PREFS = "heimdall_translation_settings";
    private static final String KEY_ALIAS = "heimdall_translation_api_key_v1";
    private static final String ANDROID_KEY_STORE = "AndroidKeyStore";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";

    private TranslationSettingsStore() {}

    static TranslationProviderConfig load(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        TranslationProviderConfig config = new TranslationProviderConfig();
        config.provider = prefs.getString("provider", TranslationProviderConfig.PROVIDER_SILICONFLOW);
        config.region = prefs.getString("region", TranslationProviderConfig.REGION_CHINA);
        config.customBaseUrl = prefs.getString("custom_base", "");
        config.customModel = prefs.getString("custom_model", "");
        config.apiKey = decrypt(prefs.getString("api_key_ciphertext", ""));
        config.sanitize();
        return config;
    }

    static boolean save(Context context, TranslationProviderConfig value) {
        TranslationProviderConfig config = value == null
                ? new TranslationProviderConfig() : value.copy();
        String encrypted = encrypt(config.apiKey);
        if (config.apiKey.length() > 0 && encrypted.length() == 0) return false;
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString("provider", config.provider)
                .putString("region", config.region)
                .putString("custom_base", config.customBaseUrl)
                .putString("custom_model", config.customModel)
                .putString("api_key_ciphertext", encrypted)
                .commit();
    }

    private static String encrypt(String plaintext) {
        if (plaintext == null || plaintext.length() == 0) return "";
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey());
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return Base64.encodeToString(cipher.getIV(), Base64.NO_WRAP) + "."
                    + Base64.encodeToString(ciphertext, Base64.NO_WRAP);
        } catch (Exception ignored) {
            return "";
        }
    }

    private static String decrypt(String saved) {
        if (saved == null || saved.length() == 0) return "";
        try {
            String[] parts = saved.split("\\.", 2);
            if (parts.length != 2) return "";
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), new GCMParameterSpec(128,
                    Base64.decode(parts[0], Base64.NO_WRAP)));
            return new String(cipher.doFinal(Base64.decode(parts[1], Base64.NO_WRAP)),
                    StandardCharsets.UTF_8);
        } catch (Exception ignored) {
            return "";
        }
    }

    private static SecretKey getOrCreateKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
        keyStore.load(null);
        java.security.Key saved = keyStore.getKey(KEY_ALIAS, null);
        if (saved instanceof SecretKey) return (SecretKey) saved;
        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEY_STORE);
        generator.init(new KeyGenParameterSpec.Builder(KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build());
        return generator.generateKey();
    }
}
