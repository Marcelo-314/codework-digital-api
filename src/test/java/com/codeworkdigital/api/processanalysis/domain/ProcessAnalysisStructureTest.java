package com.codeworkdigital.api.processanalysis.domain;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class ProcessAnalysisStructureTest {

    @Test
    void acceptsProcessWideKnowledge() {
        ProcessAnalysisKnowledge knowledge = new ProcessAnalysisKnowledge(
                List.of(new ProcessKnownFact(
                        factId("fact-1"),
                        "Orders are reviewed",
                        ProcessFactGrounding.SOURCE_STATED,
                        ProcessAnalysisScope.processWide(),
                        List.of(),
                        List.of(artifactId("source-1")))),
                List.of(new ProcessInference(
                        "Routing may depend on request category",
                        ProcessAnalysisScope.processWide())),
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

        assertThatCode(() -> new ProcessAnalysisStructure(graph(), sourceEvidenceBase(), knowledge))
                .doesNotThrowAnyException();
    }

    @Test
    void acceptsOperationScopedKnownFact() {
        ProcessAnalysisKnowledge knowledge = new ProcessAnalysisKnowledge(
                List.of(new ProcessKnownFact(
                        factId("fact-1"),
                        "Validation happens before review",
                        ProcessFactGrounding.SOURCE_STATED,
                        ProcessAnalysisScope.operation("validate"),
                        List.of(),
                        List.of(artifactId("source-1")))),
                List.of(),
                List.of(),
                List.of());

        assertThatCode(() -> new ProcessAnalysisStructure(graph(), sourceEvidenceBase(), knowledge))
                .doesNotThrowAnyException();
    }

    @Test
    void acceptsOperationScopedEvidenceGap() {
        ProcessAnalysisKnowledge knowledge = new ProcessAnalysisKnowledge(
                List.of(),
                List.of(),
                List.of(new ProcessEvidenceGap(
                        "How often does review request revision?",
                        ProcessEvidenceSource.EMPIRICAL,
                        "Whether revision should be modeled separately",
                        ProcessAnalysisScope.operation("review"))),
                List.of());

        assertThatCode(() -> new ProcessAnalysisStructure(graph(), emptyEvidenceBase(), knowledge))
                .doesNotThrowAnyException();
    }

    @Test
    void acceptsOperationScopedConstraint() {
        ProcessAnalysisKnowledge knowledge = new ProcessAnalysisKnowledge(
                List.of(),
                List.of(),
                List.of(),
                List.of(new ProcessConstraint(
                        "Revision must be approved by review",
                        "platform/state rule",
                        "Whether revision can skip review",
                        ProcessAnalysisScope.operation("revise"))));

        assertThatCode(() -> new ProcessAnalysisStructure(graph(), emptyEvidenceBase(), knowledge))
                .doesNotThrowAnyException();
    }

    @Test
    void acceptsMultiOperationScope() {
        ProcessAnalysisKnowledge knowledge = new ProcessAnalysisKnowledge(
                List.of(new ProcessKnownFact(
                        factId("fact-1"),
                        "Review and revision can repeat",
                        ProcessFactGrounding.SOURCE_STATED,
                        ProcessAnalysisScope.operations(List.of("review", "revise", "validate")),
                        List.of(),
                        List.of(artifactId("source-1")))),
                List.of(),
                List.of(),
                List.of());

        assertThatCode(() -> new ProcessAnalysisStructure(graph(), sourceEvidenceBase(), knowledge))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsUnknownOperationReferencedByKnownFact() {
        ProcessAnalysisKnowledge knowledge = new ProcessAnalysisKnowledge(
                List.of(new ProcessKnownFact(
                        factId("fact-1"),
                        "Escalation happens",
                        ProcessFactGrounding.SOURCE_STATED,
                        ProcessAnalysisScope.operation("escalate"),
                        List.of(),
                        List.of(artifactId("source-1")))),
                List.of(),
                List.of(),
                List.of());

        assertThatThrownBy(() -> new ProcessAnalysisStructure(graph(), sourceEvidenceBase(), knowledge))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("known fact scope references an unknown operation: escalate");
    }

    @Test
    void rejectsUnknownOperationReferencedByEvidenceGap() {
        ProcessAnalysisKnowledge knowledge = new ProcessAnalysisKnowledge(
                List.of(),
                List.of(),
                List.of(new ProcessEvidenceGap(
                        "Who owns escalation?",
                        ProcessEvidenceSource.UNKNOWN,
                        "Whether escalation changes ownership",
                        ProcessAnalysisScope.operation("escalate"))),
                List.of());

        assertThatThrownBy(() -> new ProcessAnalysisStructure(graph(), emptyEvidenceBase(), knowledge))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("evidence gap scope references an unknown operation: escalate");
    }

    @Test
    void rejectsUnknownOperationReferencedByConstraint() {
        ProcessAnalysisKnowledge knowledge = new ProcessAnalysisKnowledge(
                List.of(),
                List.of(),
                List.of(),
                List.of(new ProcessConstraint(
                        "Escalation requires approval",
                        "governance rule",
                        "Whether escalation can be automated",
                        ProcessAnalysisScope.operation("escalate"))));

        assertThatThrownBy(() -> new ProcessAnalysisStructure(graph(), emptyEvidenceBase(), knowledge))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("constraint scope references an unknown operation: escalate");
    }

    @Test
    void acceptsProcessWideInference() {
        ProcessAnalysisKnowledge knowledge = new ProcessAnalysisKnowledge(
                List.of(),
                List.of(new ProcessInference(
                        "Routing may depend on request category",
                        ProcessAnalysisScope.processWide())),
                List.of(),
                List.of());

        assertThatCode(() -> new ProcessAnalysisStructure(graph(), emptyEvidenceBase(), knowledge))
                .doesNotThrowAnyException();
    }

    @Test
    void acceptsOperationScopedInference() {
        ProcessAnalysisKnowledge knowledge = new ProcessAnalysisKnowledge(
                List.of(),
                List.of(new ProcessInference(
                        "Review may trigger revision",
                        ProcessAnalysisScope.operation("review"))),
                List.of(),
                List.of());

        assertThatCode(() -> new ProcessAnalysisStructure(graph(), emptyEvidenceBase(), knowledge))
                .doesNotThrowAnyException();
    }

    @Test
    void acceptsMultiOperationInferenceScopeWhenEveryOperationExists() {
        ProcessAnalysisKnowledge knowledge = new ProcessAnalysisKnowledge(
                List.of(),
                List.of(new ProcessInference(
                        "Review and revision may form a reentry region",
                        ProcessAnalysisScope.operations(List.of("review", "revise", "validate")))),
                List.of(),
                List.of());

        assertThatCode(() -> new ProcessAnalysisStructure(graph(), emptyEvidenceBase(), knowledge))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsUnknownOperationReferencedByInference() {
        ProcessAnalysisKnowledge knowledge = new ProcessAnalysisKnowledge(
                List.of(),
                List.of(new ProcessInference(
                        "Escalation may depend on review outcome",
                        ProcessAnalysisScope.operation("escalate"))),
                List.of(),
                List.of());

        assertThatThrownBy(() -> new ProcessAnalysisStructure(graph(), emptyEvidenceBase(), knowledge))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("inference scope references an unknown operation: escalate");
    }

    @Test
    void acceptsValidEvidenceBase() {
        ProcessAnalysisKnowledge knowledge = new ProcessAnalysisKnowledge(List.of(), List.of(), List.of(), List.of());
        ProcessEvidenceBase evidenceBase = new ProcessEvidenceBase(List.of(new ProcessEvidenceArtifact(
                new ProcessEvidenceArtifactId("source-1"),
                ProcessEvidenceArtifactKind.SOURCE_MATERIAL,
                "Source material stating the review rule")));

        assertThatCode(() -> new ProcessAnalysisStructure(graph(), evidenceBase, knowledge))
                .doesNotThrowAnyException();
    }

    @Test
    void acceptsSourceStatedFactReferencingExistingSourceMaterial() {
        ProcessAnalysisKnowledge knowledge = new ProcessAnalysisKnowledge(
                List.of(new ProcessKnownFact(
                        factId("fact-1"),
                        "Orders require manual review",
                        ProcessFactGrounding.SOURCE_STATED,
                        ProcessAnalysisScope.processWide(),
                        List.of(),
                        List.of(artifactId("source-1")))),
                List.of(),
                List.of(),
                List.of());

        assertThatCode(() -> new ProcessAnalysisStructure(graph(), sourceEvidenceBase(), knowledge))
                .doesNotThrowAnyException();
    }

    @Test
    void acceptsEmpiricallyEstablishedFactReferencingExistingEmpiricalResult() {
        ProcessAnalysisKnowledge knowledge = new ProcessAnalysisKnowledge(
                List.of(new ProcessKnownFact(
                        factId("fact-1"),
                        "Observed average handling time is 2.4 minutes",
                        ProcessFactGrounding.EMPIRICALLY_ESTABLISHED,
                        ProcessAnalysisScope.processWide(),
                        List.of(),
                        List.of(artifactId("measurement-1")))),
                List.of(),
                List.of(),
                List.of());

        assertThatCode(() -> new ProcessAnalysisStructure(graph(), empiricalEvidenceBase(), knowledge))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsKnownFactReferencingUnknownEvidenceArtifact() {
        ProcessAnalysisKnowledge knowledge = new ProcessAnalysisKnowledge(
                List.of(new ProcessKnownFact(
                        factId("fact-1"),
                        "Orders require manual review",
                        ProcessFactGrounding.SOURCE_STATED,
                        ProcessAnalysisScope.processWide(),
                        List.of(),
                        List.of(artifactId("missing-source")))),
                List.of(),
                List.of(),
                List.of());

        assertThatThrownBy(() -> new ProcessAnalysisStructure(graph(), sourceEvidenceBase(), knowledge))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("known fact references an unknown evidence artifact: missing-source");
    }

    @Test
    void rejectsSourceStatedFactReferencingEmpiricalResult() {
        ProcessAnalysisKnowledge knowledge = new ProcessAnalysisKnowledge(
                List.of(new ProcessKnownFact(
                        factId("fact-1"),
                        "Orders require manual review",
                        ProcessFactGrounding.SOURCE_STATED,
                        ProcessAnalysisScope.processWide(),
                        List.of(),
                        List.of(artifactId("measurement-1")))),
                List.of(),
                List.of(),
                List.of());

        assertThatThrownBy(() -> new ProcessAnalysisStructure(graph(), empiricalEvidenceBase(), knowledge))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SOURCE_STATED")
                .hasMessageContaining("measurement-1")
                .hasMessageContaining("EMPIRICAL_RESULT");
    }

    @Test
    void rejectsEmpiricallyEstablishedFactReferencingSourceMaterial() {
        ProcessAnalysisKnowledge knowledge = new ProcessAnalysisKnowledge(
                List.of(new ProcessKnownFact(
                        factId("fact-1"),
                        "Observed average handling time is 2.4 minutes",
                        ProcessFactGrounding.EMPIRICALLY_ESTABLISHED,
                        ProcessAnalysisScope.processWide(),
                        List.of(),
                        List.of(artifactId("source-1")))),
                List.of(),
                List.of(),
                List.of());

        assertThatThrownBy(() -> new ProcessAnalysisStructure(graph(), sourceEvidenceBase(), knowledge))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("EMPIRICALLY_ESTABLISHED")
                .hasMessageContaining("source-1")
                .hasMessageContaining("SOURCE_MATERIAL");
    }

    @Test
    void rejectsNullEvidenceBase() {
        ProcessAnalysisKnowledge knowledge = new ProcessAnalysisKnowledge(List.of(), List.of(), List.of(), List.of());

        assertThatThrownBy(() -> new ProcessAnalysisStructure(graph(), null, knowledge))
                .isInstanceOf(NullPointerException.class);
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

    private static ProcessEvidenceBase emptyEvidenceBase() {
        return new ProcessEvidenceBase(List.of());
    }

    private static ProcessEvidenceBase sourceEvidenceBase() {
        return new ProcessEvidenceBase(List.of(new ProcessEvidenceArtifact(
                artifactId("source-1"),
                ProcessEvidenceArtifactKind.SOURCE_MATERIAL,
                "Source material stating the review rule")));
    }

    private static ProcessEvidenceBase empiricalEvidenceBase() {
        return new ProcessEvidenceBase(List.of(new ProcessEvidenceArtifact(
                artifactId("measurement-1"),
                ProcessEvidenceArtifactKind.EMPIRICAL_RESULT,
                "Measured operation duration sample")));
    }

    private static ProcessEvidenceArtifactId artifactId(String value) {
        return new ProcessEvidenceArtifactId(value);
    }

    private static ProcessKnownFactId factId(String value) {
        return new ProcessKnownFactId(value);
    }
}
