package com.codeworkdigital.api.contact.api;

import com.codeworkdigital.api.contact.application.AdminContactSubmissionQueryService;
import com.codeworkdigital.api.contact.application.AdminContactSubmissionSummary;
import com.codeworkdigital.api.contact.application.ListAdminContactSubmissionsQuery;
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

    private final AdminContactSubmissionQueryService queryService;

    public AdminContactSubmissionController(AdminContactSubmissionQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public AdminContactSubmissionsPageResponse list(
            @RequestParam(defaultValue = "" + DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + DEFAULT_SIZE) int size) {
        var result = queryService.list(new ListAdminContactSubmissionsQuery(page, size));

        return new AdminContactSubmissionsPageResponse(
                result.content().stream().map(this::toResponse).toList(),
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages());
    }

    private AdminContactSubmissionResponse toResponse(AdminContactSubmissionSummary submission) {
        return new AdminContactSubmissionResponse(
                submission.id(),
                submission.source(),
                submission.locale(),
                submission.name(),
                submission.email(),
                submission.phone(),
                submission.companyOrProject(),
                submission.message(),
                submission.status(),
                submission.createdAt(),
                submission.updatedAt());
    }
}
