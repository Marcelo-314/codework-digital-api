package com.codeworkdigital.api.contact.application;

import com.codeworkdigital.api.contact.domain.ContactSubmission;
import com.codeworkdigital.api.contact.domain.ContactSubmissionRepository;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class AdminContactSubmissionQueryService {

    private static final int MAX_SIZE = 100;

    private final ContactSubmissionRepository repository;

    public AdminContactSubmissionQueryService(ContactSubmissionRepository repository) {
        this.repository = repository;
    }

    public AdminContactSubmissionListResult list(ListAdminContactSubmissionsQuery query) {
        int boundedSize = validateAndBoundSize(query.size());
        long offset = offsetFor(query.page(), boundedSize);
        long totalElements = repository.count();
        List<AdminContactSubmissionSummary> content = repository.findPageByCreatedAtDesc(boundedSize, offset)
                .stream()
                .map(this::toSummary)
                .toList();

        return new AdminContactSubmissionListResult(
                content,
                query.page(),
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

    private AdminContactSubmissionSummary toSummary(ContactSubmission submission) {
        return new AdminContactSubmissionSummary(
                submission.id(),
                submission.source().name(),
                submission.locale().name().toLowerCase(Locale.ROOT),
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
