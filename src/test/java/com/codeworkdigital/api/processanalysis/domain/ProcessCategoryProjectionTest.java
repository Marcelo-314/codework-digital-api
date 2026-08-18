package com.codeworkdigital.api.processanalysis.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ProcessCategoryProjectionTest {

    @Test
    void categoryDomainIdAcceptsValidValue() {
        ProcessCategoryDomainId id = new ProcessCategoryDomainId("request-type");

        assertThat(id.value()).isEqualTo("request-type");
    }

    @Test
    void categoryDomainIdRejectsNullValue() {
        assertThatThrownBy(() -> new ProcessCategoryDomainId(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void categoryDomainIdRejectsBlankValue() {
        assertThatThrownBy(() -> new ProcessCategoryDomainId(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("value must not be blank");
    }

    @Test
    void categoryDomainIdUsesStructuralEquality() {
        assertThat(new ProcessCategoryDomainId("request-type"))
                .isEqualTo(new ProcessCategoryDomainId("request-type"))
                .isNotEqualTo(new ProcessCategoryDomainId("customer-tier"));
    }

    @Test
    void categoryMemberIdAcceptsValidValue() {
        ProcessCategoryMemberId id = new ProcessCategoryMemberId("complaint");

        assertThat(id.value()).isEqualTo("complaint");
    }

    @Test
    void categoryMemberIdRejectsNullValue() {
        assertThatThrownBy(() -> new ProcessCategoryMemberId(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void categoryMemberIdRejectsBlankValue() {
        assertThatThrownBy(() -> new ProcessCategoryMemberId(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("value must not be blank");
    }

    @Test
    void categoryMemberIdUsesStructuralEquality() {
        assertThat(new ProcessCategoryMemberId("complaint"))
                .isEqualTo(new ProcessCategoryMemberId("complaint"))
                .isNotEqualTo(new ProcessCategoryMemberId("premium"));
    }

    @Test
    void categoryProjectionAcceptsValidDomainAndMember() {
        ProcessCategoryProjection projection = projection("request-type", "complaint");

        assertThat(projection.domainId()).isEqualTo(new ProcessCategoryDomainId("request-type"));
        assertThat(projection.memberId()).isEqualTo(new ProcessCategoryMemberId("complaint"));
    }

    @Test
    void categoryProjectionRejectsNullDomain() {
        assertThatThrownBy(() -> new ProcessCategoryProjection(null, new ProcessCategoryMemberId("complaint")))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void categoryProjectionRejectsNullMember() {
        assertThatThrownBy(() -> new ProcessCategoryProjection(new ProcessCategoryDomainId("request-type"), null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void sameDomainAndSameMemberAreEqual() {
        assertThat(projection("request-type", "complaint"))
                .isEqualTo(projection("request-type", "complaint"));
    }

    @Test
    void sameMemberTokenInDifferentDomainsIsNotEqual() {
        assertThat(projection("request-type", "premium"))
                .isNotEqualTo(projection("customer-tier", "premium"));
    }

    @Test
    void differentMembersInSameDomainAreNotEqual() {
        assertThat(projection("request-type", "order"))
                .isNotEqualTo(projection("request-type", "complaint"));
    }

    private static ProcessCategoryProjection projection(String domainId, String memberId) {
        return new ProcessCategoryProjection(
                new ProcessCategoryDomainId(domainId),
                new ProcessCategoryMemberId(memberId));
    }
}
