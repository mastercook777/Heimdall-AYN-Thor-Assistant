package com.mastercook777.heimdall;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

public final class GuideTextDecoder {
    private static final byte[] UTF8_BOM = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
    private static final byte[] UTF16_LE_BOM = {(byte) 0xFF, (byte) 0xFE};
    private static final byte[] UTF16_BE_BOM = {(byte) 0xFE, (byte) 0xFF};
    private static final int MAX_TRUNCATED_TAIL_BYTES = 3;

    private GuideTextDecoder() {
    }

    public static String decode(byte[] bytes, boolean truncated) throws CharacterCodingException {
        byte[] source = bytes == null ? new byte[0] : bytes;
        if (startsWith(source, UTF8_BOM)) {
            return decodeStrict(source, UTF8_BOM.length,
                    source.length - UTF8_BOM.length, StandardCharsets.UTF_8, truncated);
        }
        if (startsWith(source, UTF16_LE_BOM)) {
            return decodeStrict(source, UTF16_LE_BOM.length,
                    source.length - UTF16_LE_BOM.length, StandardCharsets.UTF_16LE, truncated);
        }
        if (startsWith(source, UTF16_BE_BOM)) {
            return decodeStrict(source, UTF16_BE_BOM.length,
                    source.length - UTF16_BE_BOM.length, StandardCharsets.UTF_16BE, truncated);
        }

        try {
            return decodeStrict(source, 0, source.length, StandardCharsets.UTF_8, truncated);
        } catch (CharacterCodingException utf8Failure) {
            return decodeStrict(source, 0, source.length,
                    Charset.forName("GB18030"), truncated);
        }
    }

    private static String decodeStrict(byte[] source, int offset, int length,
            Charset charset, boolean truncated) throws CharacterCodingException {
        CharacterCodingException firstFailure = null;
        int maxTrim = truncated ? Math.min(MAX_TRUNCATED_TAIL_BYTES, length) : 0;
        for (int trim = 0; trim <= maxTrim; trim++) {
            CharsetDecoder decoder = charset.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT);
            try {
                String decoded = decoder.decode(
                        ByteBuffer.wrap(source, offset, length - trim)).toString();
                if (isProbablyText(decoded)) {
                    return decoded;
                }
                if (firstFailure == null) {
                    firstFailure = new CharacterCodingException();
                }
            } catch (CharacterCodingException failure) {
                if (firstFailure == null) {
                    firstFailure = failure;
                }
            }
        }
        throw firstFailure == null ? new CharacterCodingException() : firstFailure;
    }

    private static boolean isProbablyText(String text) {
        for (int index = 0; index < text.length();) {
            int codePoint = text.codePointAt(index);
            index += Character.charCount(codePoint);
            if (codePoint == 0 || codePoint == 0xFFFE || codePoint == 0xFFFF) {
                return false;
            }
            if (Character.isISOControl(codePoint)
                    && codePoint != '\n' && codePoint != '\r'
                    && codePoint != '\t' && codePoint != '\f') {
                return false;
            }
        }
        return true;
    }

    private static boolean startsWith(byte[] source, byte[] prefix) {
        if (source.length < prefix.length) {
            return false;
        }
        for (int index = 0; index < prefix.length; index++) {
            if (source[index] != prefix[index]) {
                return false;
            }
        }
        return true;
    }
}
