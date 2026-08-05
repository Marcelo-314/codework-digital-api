package com.codeworkdigital.api.contact.application;

import java.text.Normalizer;
import org.springframework.stereotype.Component;

@Component
public class ContactSubmissionNormalizer {

    public NormalizedContactSubmissionPayload normalize(SubmitContactSubmissionCommand command) {
        return new NormalizedContactSubmissionPayload(
                command.source(),
                command.locale(),
                normalizeCollapsed(command.name()),
                normalizeTrimmed(command.email()),
                normalizeOptionalCollapsed(command.phone()),
                normalizeOptionalCollapsed(command.companyOrProject()),
                normalizeMessage(command.message()));
    }

    private String normalizeCollapsed(String value) {
        return collapseInternalWhitespace(stripUnicodeWhitespace(normalizeNfc(value)));
    }

    private String normalizeOptionalCollapsed(String value) {
        if (value == null) {
            return null;
        }
        String normalized = normalizeCollapsed(value);
        return normalized.isEmpty() ? null : normalized;
    }

    private String normalizeTrimmed(String value) {
        return stripUnicodeWhitespace(normalizeNfc(value));
    }

    private String normalizeMessage(String value) {
        return stripUnicodeWhitespace(normalizeNfc(value).replace("\r\n", "\n").replace('\r', '\n'));
    }

    private String normalizeNfc(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFC);
    }

    private String stripUnicodeWhitespace(String value) {
        int start = 0;
        int end = value.length();
        while (start < end) {
            int codePoint = value.codePointAt(start);
            if (!isUnicodeWhitespace(codePoint)) {
                break;
            }
            start += Character.charCount(codePoint);
        }
        while (start < end) {
            int codePoint = value.codePointBefore(end);
            if (!isUnicodeWhitespace(codePoint)) {
                break;
            }
            end -= Character.charCount(codePoint);
        }
        return value.substring(start, end);
    }

    private String collapseInternalWhitespace(String value) {
        StringBuilder result = new StringBuilder(value.length());
        boolean inWhitespace = false;
        for (int offset = 0; offset < value.length(); ) {
            int codePoint = value.codePointAt(offset);
            if (isUnicodeWhitespace(codePoint)) {
                inWhitespace = true;
            } else {
                if (inWhitespace && !result.isEmpty()) {
                    result.append(' ');
                }
                result.appendCodePoint(codePoint);
                inWhitespace = false;
            }
            offset += Character.charCount(codePoint);
        }
        return result.toString();
    }

    private boolean isUnicodeWhitespace(int codePoint) {
        return Character.isWhitespace(codePoint) || Character.isSpaceChar(codePoint);
    }
}
