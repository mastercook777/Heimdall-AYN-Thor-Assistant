package com.mastercook777.heimdall;

import android.graphics.Bitmap;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions;
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions;
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

final class TranslationOcrEngine {
    interface Callback {
        void onSuccess(String text);
        void onError();
    }

    private TextRecognizer latin;
    private TextRecognizer japanese;
    private TextRecognizer chinese;
    private TextRecognizer korean;

    void recognize(Bitmap region, TranslationConfig config, Callback callback) {
        if (region == null || region.isRecycled()) {
            callback.onError();
            return;
        }
        recognizer(config.ocrScript).process(InputImage.fromBitmap(region, 0))
                .addOnSuccessListener(text -> {
                    region.recycle();
                    callback.onSuccess(text == null ? "" : text.getText());
                })
                .addOnFailureListener(error -> {
                    region.recycle();
                    callback.onError();
                });
    }

    void close() {
        if (latin != null) latin.close();
        if (japanese != null) japanese.close();
        if (chinese != null) chinese.close();
        if (korean != null) korean.close();
        latin = null;
        japanese = null;
        chinese = null;
        korean = null;
    }

    private TextRecognizer recognizer(String script) {
        String normalized = TranslationConfig.normalizeScript(script);
        if (TranslationConfig.SCRIPT_CHINESE.equals(normalized)) {
            if (chinese == null) chinese = TextRecognition.getClient(
                    new ChineseTextRecognizerOptions.Builder().build());
            return chinese;
        }
        if (TranslationConfig.SCRIPT_KOREAN.equals(normalized)) {
            if (korean == null) korean = TextRecognition.getClient(
                    new KoreanTextRecognizerOptions.Builder().build());
            return korean;
        }
        if (TranslationConfig.SCRIPT_JAPANESE.equals(normalized)) {
            if (japanese == null) japanese = TextRecognition.getClient(
                    new JapaneseTextRecognizerOptions.Builder().build());
            return japanese;
        }
        if (latin == null) latin = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        return latin;
    }

}
