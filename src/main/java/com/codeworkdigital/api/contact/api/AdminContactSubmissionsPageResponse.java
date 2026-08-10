package com.codeworkdigital.api.contact.api;

import java.util.List;

public record AdminContactSubmissionsPageResponse(
        List<AdminContactSubmissionResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {
}
