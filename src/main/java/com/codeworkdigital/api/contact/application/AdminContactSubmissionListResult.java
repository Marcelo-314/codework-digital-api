package com.codeworkdigital.api.contact.application;

import java.util.List;

public record AdminContactSubmissionListResult(
        List<AdminContactSubmissionSummary> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {
}
