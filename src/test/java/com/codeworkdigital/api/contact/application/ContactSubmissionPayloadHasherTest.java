package com.codeworkdigital.api.contact.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.codeworkdigital.api.contact.domain.ContactLocale;
import com.codeworkdigital.api.contact.domain.ContactSource;
import org.junit.jupiter.api.Test;

class ContactSubmissionPayloadHasherTest {

    private static final String EXPECTED_VECTOR =
            "7e8efa0a429a05304de89079e4dff69040f1c5ed72827ff62e6f45847dd42b98";

    private final ContactSubmissionPayloadHasher hasher = new ContactSubmissionPayloadHasher();
    private final ContactSubmissionNormalizer normalizer = new ContactSubmissionNormalizer();

    @Test
    void producesLowercaseSha256Hex() {
        String hash = hasher.hash(payload());

        assertThat(hash).hasSize(64);
        assertThat(hash).matches("[0-9a-f]{64}");
    }

    @Test
    void isDeterministicAndMatchesFixedVector() {
        assertThat(hasher.hash(payload())).isEqualTo(hasher.hash(payload()));
        assertThat(hasher.hash(payload())).isEqualTo(EXPECTED_VECTOR);
    }

    @Test
    void sameNormalizedValuesProduceSameHash() {
        NormalizedContactSubmissionPayload first = payload();
        NormalizedContactSubmissionPayload second = new NormalizedContactSubmissionPayload(
                ContactSource.HOME,
                ContactLocale.ES,
                "Ada Lovelace",
                "Ada@Example.TEST",
                null,
                "CodeWork",
                "Line\nTwo");

        assertThat(hasher.hash(first)).isEqualTo(hasher.hash(second));
    }

    @Test
    void everyIncludedFieldAffectsTheHash() {
        assertThat(hasher.hash(withSource(ContactSource.CONTACT_PAGE))).isNotEqualTo(EXPECTED_VECTOR);
        assertThat(hasher.hash(withLocale(ContactLocale.EN))).isNotEqualTo(EXPECTED_VECTOR);
        assertThat(hasher.hash(withName("Grace Hopper"))).isNotEqualTo(EXPECTED_VECTOR);
        assertThat(hasher.hash(withEmail("grace@example.test"))).isNotEqualTo(EXPECTED_VECTOR);
        assertThat(hasher.hash(withPhone("+39 123"))).isNotEqualTo(EXPECTED_VECTOR);
        assertThat(hasher.hash(withCompanyOrProject("Different"))).isNotEqualTo(EXPECTED_VECTOR);
        assertThat(hasher.hash(withMessage("Different"))).isNotEqualTo(EXPECTED_VECTOR);
    }

    @Test
    void nullIsNotAmbiguousWithEmptyString() {
        assertThat(hasher.hash(withPhone(null))).isNotEqualTo(hasher.hash(withPhone("")));
    }

    @Test
    void lengthPrefixPreventsDelimiterStyleAmbiguity() {
        NormalizedContactSubmissionPayload first = withNameAndEmail("ab", "c");
        NormalizedContactSubmissionPayload second = withNameAndEmail("a", "bc");

        assertThat(hasher.hash(first)).isNotEqualTo(hasher.hash(second));
    }

    @Test
    void usesExplicitUtf8AndKeepsFixedLfVector() {
        assertThat(hasher.hash(withMessage("Line\nTwo"))).isEqualTo(EXPECTED_VECTOR);
    }

    @Test
    void normalizationMakesEquivalentLineEndingsProduceTheSameHash() {
        String lf = hasher.hash(normalizer.normalize(commandWithMessage("Line\nTwo")));
        String crlf = hasher.hash(normalizer.normalize(commandWithMessage("Line\r\nTwo")));
        String cr = hasher.hash(normalizer.normalize(commandWithMessage("Line\rTwo")));

        assertThat(crlf).isEqualTo(lf);
        assertThat(cr).isEqualTo(lf);
    }

    private NormalizedContactSubmissionPayload payload() {
        return new NormalizedContactSubmissionPayload(
                ContactSource.HOME,
                ContactLocale.ES,
                "Ada Lovelace",
                "Ada@Example.TEST",
                null,
                "CodeWork",
                "Line\nTwo");
    }

    private NormalizedContactSubmissionPayload withSource(ContactSource source) {
        NormalizedContactSubmissionPayload payload = payload();
        return new NormalizedContactSubmissionPayload(source, payload.locale(), payload.name(), payload.email(),
                payload.phone(), payload.companyOrProject(), payload.message());
    }

    private NormalizedContactSubmissionPayload withLocale(ContactLocale locale) {
        NormalizedContactSubmissionPayload payload = payload();
        return new NormalizedContactSubmissionPayload(payload.source(), locale, payload.name(), payload.email(),
                payload.phone(), payload.companyOrProject(), payload.message());
    }

    private NormalizedContactSubmissionPayload withName(String name) {
        NormalizedContactSubmissionPayload payload = payload();
        return new NormalizedContactSubmissionPayload(payload.source(), payload.locale(), name, payload.email(),
                payload.phone(), payload.companyOrProject(), payload.message());
    }

    private NormalizedContactSubmissionPayload withEmail(String email) {
        NormalizedContactSubmissionPayload payload = payload();
        return new NormalizedContactSubmissionPayload(payload.source(), payload.locale(), payload.name(), email,
                payload.phone(), payload.companyOrProject(), payload.message());
    }

    private NormalizedContactSubmissionPayload withPhone(String phone) {
        NormalizedContactSubmissionPayload payload = payload();
        return new NormalizedContactSubmissionPayload(payload.source(), payload.locale(), payload.name(), payload.email(),
                phone, payload.companyOrProject(), payload.message());
    }

    private NormalizedContactSubmissionPayload withCompanyOrProject(String companyOrProject) {
        NormalizedContactSubmissionPayload payload = payload();
        return new NormalizedContactSubmissionPayload(payload.source(), payload.locale(), payload.name(), payload.email(),
                payload.phone(), companyOrProject, payload.message());
    }

    private NormalizedContactSubmissionPayload withMessage(String message) {
        NormalizedContactSubmissionPayload payload = payload();
        return new NormalizedContactSubmissionPayload(payload.source(), payload.locale(), payload.name(), payload.email(),
                payload.phone(), payload.companyOrProject(), message);
    }

    private NormalizedContactSubmissionPayload withNameAndEmail(String name, String email) {
        NormalizedContactSubmissionPayload payload = payload();
        return new NormalizedContactSubmissionPayload(payload.source(), payload.locale(), name, email,
                payload.phone(), payload.companyOrProject(), payload.message());
    }

    private SubmitContactSubmissionCommand commandWithMessage(String message) {
        return new SubmitContactSubmissionCommand(
                java.util.UUID.randomUUID(),
                ContactSource.HOME,
                ContactLocale.ES,
                "Ada Lovelace",
                "Ada@Example.TEST",
                null,
                "CodeWork",
                message);
    }
}
