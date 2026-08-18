package com.codeworkdigital.api.processanalysis.domain;

import java.util.Objects;

/**
 * Domain-scoped symbolic category projection for a known fact.
 *
 * Equality is defined by the domain/member pair. The same member token in two distinct domains is not the same category
 * value. This does not prove that the domain is exhaustive or validate membership against a declared finite domain.
 */
public record ProcessCategoryProjection(
        ProcessCategoryDomainId domainId,
        ProcessCategoryMemberId memberId) implements ProcessComputableProjection {

    public ProcessCategoryProjection {
        domainId = Objects.requireNonNull(domainId, "domainId");
        memberId = Objects.requireNonNull(memberId, "memberId");
    }
}
