package com.codeworkdigital.api.processanalysis.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProcessAnalysisScopeTest {

    @Test
    void createsProcessWideEmptyScope() {
        ProcessAnalysisScope scope = ProcessAnalysisScope.processWide();

        assertThat(scope.operationIds()).isEmpty();
        assertThat(scope.isProcessWide()).isTrue();
    }

    @Test
    void createsOneOperationScope() {
        ProcessAnalysisScope scope = ProcessAnalysisScope.operation("validate");

        assertThat(scope.operationIds()).containsExactly("validate");
        assertThat(scope.isProcessWide()).isFalse();
    }

    @Test
    void createsMultiOperationScopePreservingOrder() {
        ProcessAnalysisScope scope = ProcessAnalysisScope.operations(List.of("validate", "review", "revise"));

        assertThat(scope.operationIds()).containsExactly("validate", "review", "revise");
    }

    @Test
    void rejectsDuplicateIds() {
        assertThatThrownBy(() -> ProcessAnalysisScope.operations(List.of("validate", "review", "validate")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("operation IDs must be unique inside the scope");
    }

    @Test
    void rejectsBlankIds() {
        assertThatThrownBy(() -> ProcessAnalysisScope.operation(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("operationId must not be blank");
    }

    @Test
    void rejectsNullMembers() {
        List<String> operationIds = new ArrayList<>();
        operationIds.add("validate");
        operationIds.add(null);

        assertThatThrownBy(() -> ProcessAnalysisScope.operations(operationIds))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void defensivelyCopiesAndReturnsImmutableCollection() {
        List<String> operationIds = new ArrayList<>(List.of("validate", "review"));

        ProcessAnalysisScope scope = ProcessAnalysisScope.operations(operationIds);

        operationIds.clear();

        assertThat(scope.operationIds()).containsExactly("validate", "review");
        assertThatThrownBy(() -> scope.operationIds().add("revise"))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
