package com.codeworkdigital.api.processanalysis.domain;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class ProcessAnalysisStructureTest {

    @Test
    void acceptsProcessWideKnowledge() {
        ProcessAnalysisKnowledge knowledge = new ProcessAnalysisKnowledge(
                List.of(new ProcessKnownFact("Orders are reviewed", ProcessAnalysisScope.processWide())),
                List.of(new ProcessEvidenceGap(
                        "What changes routing?",
                        ProcessEvidenceSource.SELF_REPORTED,
                        "Whether routing criteria are explicit",
                        ProcessAnalysisScope.processWide())),
                List.of(new ProcessConstraint(
                        "Refunds require approval",
                        "governance rule",
                        "Whether release can be automated",
                        ProcessAnalysisScope.processWide())));

        assertThatCode(() -> new ProcessAnalysisStructure(graph(), knowledge))
                .doesNotThrowAnyException();
    }

    @Test
    void acceptsOperationScopedKnownFact() {
        ProcessAnalysisKnowledge knowledge = new ProcessAnalysisKnowledge(
                List.of(new ProcessKnownFact("Validation happens before review", ProcessAnalysisScope.operation("validate"))),
                List.of(),
                List.of());

        assertThatCode(() -> new ProcessAnalysisStructure(graph(), knowledge))
                .doesNotThrowAnyException();
    }

    @Test
    void acceptsOperationScopedEvidenceGap() {
        ProcessAnalysisKnowledge knowledge = new ProcessAnalysisKnowledge(
                List.of(),
                List.of(new ProcessEvidenceGap(
                        "How often does review request revision?",
                        ProcessEvidenceSource.EMPIRICAL,
                        "Whether revision should be modeled separately",
                        ProcessAnalysisScope.operation("review"))),
                List.of());

        assertThatCode(() -> new ProcessAnalysisStructure(graph(), knowledge))
                .doesNotThrowAnyException();
    }

    @Test
    void acceptsOperationScopedConstraint() {
        ProcessAnalysisKnowledge knowledge = new ProcessAnalysisKnowledge(
                List.of(),
                List.of(),
                List.of(new ProcessConstraint(
                        "Revision must be approved by review",
                        "platform/state rule",
                        "Whether revision can skip review",
                        ProcessAnalysisScope.operation("revise"))));

        assertThatCode(() -> new ProcessAnalysisStructure(graph(), knowledge))
                .doesNotThrowAnyException();
    }

    @Test
    void acceptsMultiOperationScope() {
        ProcessAnalysisKnowledge knowledge = new ProcessAnalysisKnowledge(
                List.of(new ProcessKnownFact(
                        "Review and revision can repeat",
                        ProcessAnalysisScope.operations(List.of("review", "revise", "validate")))),
                List.of(),
                List.of());

        assertThatCode(() -> new ProcessAnalysisStructure(graph(), knowledge))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsUnknownOperationReferencedByKnownFact() {
        ProcessAnalysisKnowledge knowledge = new ProcessAnalysisKnowledge(
                List.of(new ProcessKnownFact("Escalation happens", ProcessAnalysisScope.operation("escalate"))),
                List.of(),
                List.of());

        assertThatThrownBy(() -> new ProcessAnalysisStructure(graph(), knowledge))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("known fact scope references an unknown operation: escalate");
    }

    @Test
    void rejectsUnknownOperationReferencedByEvidenceGap() {
        ProcessAnalysisKnowledge knowledge = new ProcessAnalysisKnowledge(
                List.of(),
                List.of(new ProcessEvidenceGap(
                        "Who owns escalation?",
                        ProcessEvidenceSource.UNKNOWN,
                        "Whether escalation changes ownership",
                        ProcessAnalysisScope.operation("escalate"))),
                List.of());

        assertThatThrownBy(() -> new ProcessAnalysisStructure(graph(), knowledge))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("evidence gap scope references an unknown operation: escalate");
    }

    @Test
    void rejectsUnknownOperationReferencedByConstraint() {
        ProcessAnalysisKnowledge knowledge = new ProcessAnalysisKnowledge(
                List.of(),
                List.of(),
                List.of(new ProcessConstraint(
                        "Escalation requires approval",
                        "governance rule",
                        "Whether escalation can be automated",
                        ProcessAnalysisScope.operation("escalate"))));

        assertThatThrownBy(() -> new ProcessAnalysisStructure(graph(), knowledge))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("constraint scope references an unknown operation: escalate");
    }

    private static ProcessOperationGraph graph() {
        return new ProcessOperationGraph(
                List.of(operation("validate"), operation("review"), operation("revise")),
                List.of(
                        new ProcessInformationFlow("validate", "review", "validation result"),
                        new ProcessInformationFlow("review", "revise", "review notes"),
                        new ProcessInformationFlow("revise", "validate", "revised submission")),
                List.of(
                        new ProcessControlFlow("validate", "review"),
                        new ProcessControlFlow("review", "revise", "needs revision"),
                        new ProcessControlFlow("revise", "validate")));
    }

    private static ProcessOperation operation(String id) {
        return new ProcessOperation(id, id, id + " operation");
    }
}
