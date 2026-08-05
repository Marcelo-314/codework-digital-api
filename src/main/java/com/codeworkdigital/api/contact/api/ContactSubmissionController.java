package com.codeworkdigital.api.contact.api;

import com.codeworkdigital.api.contact.application.ContactSubmissionApplicationService;
import com.codeworkdigital.api.contact.application.SubmitContactSubmissionCommand;
import com.codeworkdigital.api.contact.application.SubmitContactSubmissionResult;
import com.codeworkdigital.api.contact.domain.ContactLocale;
import com.codeworkdigital.api.contact.domain.ContactSource;
import jakarta.validation.Valid;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/v1/contact-submissions")
public class ContactSubmissionController {

    private static final Pattern CANONICAL_UUID = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    private final ContactSubmissionApplicationService applicationService;

    public ContactSubmissionController(ContactSubmissionApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ContactSubmissionResponse> submit(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody ContactSubmissionRequest request) {
        SubmitContactSubmissionResult result = applicationService.submit(new SubmitContactSubmissionCommand(
                parseIdempotencyKey(idempotencyKey),
                mapSource(request.source()),
                mapLocale(request.locale()),
                request.name(),
                request.email(),
                request.phone(),
                request.companyOrProject(),
                request.message()));

        ContactSubmissionResponse response = new ContactSubmissionResponse(
                result.submissionId(),
                result.status().name(),
                result.receivedAt());
        return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK).body(response);
    }

    private UUID parseIdempotencyKey(String value) {
        if (value == null || value.isEmpty() || !CANONICAL_UUID.matcher(value).matches()) {
            throw new InvalidIdempotencyKeyException();
        }
        try {
            UUID uuid = UUID.fromString(value);
            if (!uuid.toString().equalsIgnoreCase(value)) {
                throw new InvalidIdempotencyKeyException();
            }
            return uuid;
        } catch (IllegalArgumentException exception) {
            throw new InvalidIdempotencyKeyException();
        }
    }

    private ContactSource mapSource(String source) {
        return switch (source) {
            case "HOME" -> ContactSource.HOME;
            case "CONTACT_PAGE" -> ContactSource.CONTACT_PAGE;
            default -> throw new UnsupportedContactValueException("source");
        };
    }

    private ContactLocale mapLocale(String locale) {
        return switch (locale) {
            case "es" -> ContactLocale.ES;
            case "en" -> ContactLocale.EN;
            case "it" -> ContactLocale.IT;
            default -> throw new UnsupportedContactValueException("locale");
        };
    }
}
