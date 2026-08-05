package com.codeworkdigital.api.contact.application;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.springframework.stereotype.Component;

@Component
public class ContactSubmissionPayloadHasher {

    private static final byte[] PREFIX = "cwd-contact-submission:v1".getBytes(StandardCharsets.UTF_8);

    public String hash(NormalizedContactSubmissionPayload payload) {
        MessageDigest digest = sha256();
        digest.update(PREFIX);
        update(digest, payload.source().name());
        update(digest, payload.locale().name());
        update(digest, payload.name());
        update(digest, payload.email());
        update(digest, payload.phone());
        update(digest, payload.companyOrProject());
        update(digest, payload.message());
        return HexFormat.of().formatHex(digest.digest());
    }

    private MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private void update(MessageDigest digest, String value) {
        if (value == null) {
            digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(-1).array());
            return;
        }
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(bytes.length).array());
        digest.update(bytes);
    }
}
