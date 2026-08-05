package com.codeworkdigital.api.contact.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.codeworkdigital.api.contact.domain.ContactLocale;
import com.codeworkdigital.api.contact.domain.ContactSource;
import java.text.Normalizer;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ContactSubmissionNormalizerTest {

    private static final String TEST_TURNSTILE_TOKEN = "test-turnstile-token";

    private final ContactSubmissionNormalizer normalizer = new ContactSubmissionNormalizer();

    @Test
    void normalizesUnicodeToNfc() {
        NormalizedContactSubmissionPayload decomposed = normalizer.normalize(command("Cafe\u0301", "Message"));
        NormalizedContactSubmissionPayload composed = normalizer.normalize(command("Caf\u00e9", "Message"));

        assertThat(decomposed.name()).isEqualTo(composed.name());
        assertThat(Normalizer.isNormalized(decomposed.name(), Normalizer.Form.NFC)).isTrue();
    }

    @Test
    void normalizesNameWhitespaceWhilePreservingAccentsAndCase() {
        NormalizedContactSubmissionPayload payload = normalizer.normalize(command(" \t Mar\u00eda   DE\u00a0la   Cruz \n", "Message"));

        assertThat(payload.name()).isEqualTo("Mar\u00eda DE la Cruz");
    }

    @Test
    void trimsEmailAndPreservesCaseAndInterior() {
        NormalizedContactSubmissionPayload payload = normalizer.normalize(commandWithEmail("  Ada.Lovelace+Test@Example.COM  "));

        assertThat(payload.email()).isEqualTo("Ada.Lovelace+Test@Example.COM");
    }

    @Test
    void normalizesPhoneAndConvertsBlankToNull() {
        NormalizedContactSubmissionPayload payload = normalizer.normalize(commandWithOptionalFields(
                " \t +39\u00a0(123)   456-789 \n",
                null));
        NormalizedContactSubmissionPayload blank = normalizer.normalize(commandWithOptionalFields(" \t \n", null));

        assertThat(payload.phone()).isEqualTo("+39 (123) 456-789");
        assertThat(blank.phone()).isNull();
    }

    @Test
    void normalizesCompanyOrProjectAndConvertsBlankToNull() {
        NormalizedContactSubmissionPayload payload = normalizer.normalize(commandWithOptionalFields(
                null,
                "  CodeWork\u00a0  Digital / Proyecto  "));
        NormalizedContactSubmissionPayload blank = normalizer.normalize(commandWithOptionalFields(null, " \n\t "));

        assertThat(payload.companyOrProject()).isEqualTo("CodeWork Digital / Proyecto");
        assertThat(blank.companyOrProject()).isNull();
    }

    @Test
    void normalizesMessageLineEndingsAndPreservesInternalFormatting() {
        NormalizedContactSubmissionPayload payload = normalizer.normalize(command(
                "Ada",
                " \tLine  one\r\nLine   two\rLine three\n\nLine four  \n "));

        assertThat(payload.message()).isEqualTo("Line  one\nLine   two\nLine three\n\nLine four");
    }

    @Test
    void preservesEmojiAndSupplementaryCharacters() {
        NormalizedContactSubmissionPayload payload = normalizer.normalize(command("Ada \uD83D\uDE80", "Message \uD83D\uDE80"));

        assertThat(payload.name()).isEqualTo("Ada \uD83D\uDE80");
        assertThat(payload.message()).isEqualTo("Message \uD83D\uDE80");
    }

    private SubmitContactSubmissionCommand command(String name, String message) {
        return new SubmitContactSubmissionCommand(
                UUID.randomUUID(),
                ContactSource.HOME,
                ContactLocale.ES,
                name,
                "ada@example.test",
                null,
                null,
                message,
                TEST_TURNSTILE_TOKEN);
    }

    private SubmitContactSubmissionCommand commandWithEmail(String email) {
        return new SubmitContactSubmissionCommand(
                UUID.randomUUID(),
                ContactSource.HOME,
                ContactLocale.ES,
                "Ada",
                email,
                null,
                null,
                "Message",
                TEST_TURNSTILE_TOKEN);
    }

    private SubmitContactSubmissionCommand commandWithOptionalFields(String phone, String companyOrProject) {
        return new SubmitContactSubmissionCommand(
                UUID.randomUUID(),
                ContactSource.HOME,
                ContactLocale.ES,
                "Ada",
                "ada@example.test",
                phone,
                companyOrProject,
                "Message",
                TEST_TURNSTILE_TOKEN);
    }
}
