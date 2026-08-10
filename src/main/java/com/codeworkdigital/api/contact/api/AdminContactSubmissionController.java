package com.codeworkdigital.api.contact.api;

import com.codeworkdigital.api.contact.domain.ContactSubmission;
import com.codeworkdigital.api.contact.domain.ContactSubmissionRepository;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/admin/contact-submissions")
public class AdminContactSubmissionController {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final ContactSubmissionRepository repository;

    public AdminContactSubmissionController(ContactSubmissionRepository repository) {
        this.repository = repository;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public AdminContactSubmissionsPageResponse list(
            @RequestParam(defaultValue = "" + DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + DEFAULT_SIZE) int size) {
        int boundedSize = validateAndBoundSize(size);
        long offset = offsetFor(page, boundedSize);
        long totalElements = repository.count();
        List<AdminContactSubmissionResponse> content = repository.findPageByCreatedAtDesc(boundedSize, offset)
                .stream()
                .map(this::toResponse)
                .toList();

        return new AdminContactSubmissionsPageResponse(
                content,
                page,
                boundedSize,
                totalElements,
                totalPages(totalElements, boundedSize));
    }

    private int validateAndBoundSize(int size) {
        if (size < 1) {
            throw new InvalidAdminPaginationException("size");
        }
        return Math.min(size, MAX_SIZE);
    }

    private long offsetFor(int page, int size) {
        if (page < 0) {
            throw new InvalidAdminPaginationException("page");
        }
        return Math.multiplyExact((long) page, size);
    }

    private int totalPages(long totalElements, int size) {
        return (int) Math.ceil((double) totalElements / size);
    }

    private AdminContactSubmissionResponse toResponse(ContactSubmission submission) {
        return new AdminContactSubmissionResponse(
                submission.id(),
                submission.source().name(),
                submission.locale().name().toLowerCase(),
                submission.name(),
                submission.email(),
                submission.phone(),
                submission.companyOrProject(),
                submission.message(),
                submission.status().name(),
                submission.createdAt(),
                submission.updatedAt());
    }
}
