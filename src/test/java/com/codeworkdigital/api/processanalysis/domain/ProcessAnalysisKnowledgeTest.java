package com.codeworkdigital.api.processanalysis.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProcessAnalysisKnowledgeTest {

    @Test
    void keepsKnownFactsInferencesEvidenceGapsAndConstraintsDistinct() {
        ProcessAnalysisScope processWide = ProcessAnalysisScope.processWide();
        ProcessKnownFact fact = new ProcessKnownFact("Orders are reviewed before fulfillment", processWide);
        ProcessInference inference = new ProcessInference("Routing may depend on request category", processWide);
        ProcessEvidenceGap gap = new ProcessEvidenceGap(
                "How often are orders revised after review?",
                ProcessEvidenceSource.EMPIRICAL,
                "Whether revision handling needs separate analysis",
                processWide);
        ProcessConstraint constraint = new ProcessConstraint(
                "Only approved staff may release refunds",
                "governance rule",
                "Whether automated release is allowed",
                processWide);

        ProcessAnalysisKnowledge knowledge =
                new ProcessAnalysisKnowledge(List.of(fact), List.of(inference), List.of(gap), List.of(constraint));

        assertThat(knowledge.knownFacts()).containsExactly(fact);
        assertThat(knowledge.inferences()).containsExactly(inference);
        assertThat(knowledge.evidenceGaps()).containsExactly(gap);
        assertThat(knowledge.constraints()).containsExactly(constraint);
    }

    @Test
    void acceptsSelfReportedEvidenceGaps() {
        ProcessEvidenceGap gap = new ProcessEvidenceGap(
                "Who decides the manual route?",
                ProcessEvidenceSource.SELF_REPORTED,
                "Whether routing authority is explicit",
                ProcessAnalysisScope.processWide());

        assertThat(gap.source()).isEqualTo(ProcessEvidenceSource.SELF_REPORTED);
    }

    @Test
    void acceptsEmpiricalEvidenceGaps() {
        ProcessEvidenceGap gap = new ProcessEvidenceGap(
                "How many submissions reenter validation?",
                ProcessEvidenceSource.EMPIRICAL,
                "Whether reentry volume changes process risk",
                ProcessAnalysisScope.processWide());

        assertThat(gap.source()).isEqualTo(ProcessEvidenceSource.EMPIRICAL);
    }

    @Test
    void acceptsUnknownEvidenceSourceWhereAppropriate() {
        ProcessEvidenceGap gap = new ProcessEvidenceGap(
                "What determines fulfillment path selection?",
                ProcessEvidenceSource.UNKNOWN,
                "Whether branching criteria can be specified",
                ProcessAnalysisScope.processWide());

        assertThat(gap.source()).isEqualTo(ProcessEvidenceSource.UNKNOWN);
    }

    @Test
    void rejectsBlankOrNullRequiredValues() {
        ProcessAnalysisScope processWide = ProcessAnalysisScope.processWide();

        assertThatThrownBy(() -> new ProcessKnownFact(" ", processWide))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ProcessKnownFact("fact", null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ProcessInference(" ", processWide))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ProcessInference(null, processWide))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ProcessInference("inference", null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ProcessEvidenceGap(null, ProcessEvidenceSource.UNKNOWN, "decision", processWide))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ProcessEvidenceGap("question", null, "decision", processWide))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ProcessEvidenceGap("question", ProcessEvidenceSource.UNKNOWN, " ", processWide))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ProcessEvidenceGap("question", ProcessEvidenceSource.UNKNOWN, "decision", null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ProcessConstraint("constraint", "policy", " ", processWide))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ProcessConstraint("constraint", "policy", "decision", null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ProcessOperation(null, "title", "description"))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ProcessInformationFlow("source", "target", " "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ProcessControlFlow("source", "target", null))
                .isInstanceOf(NullPointerException.class);
        assertThatCode(() -> new ProcessControlFlow("source", "target", ""))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsNullCollectionMembers() {
        ProcessAnalysisScope processWide = ProcessAnalysisScope.processWide();
        ProcessKnownFact fact = new ProcessKnownFact("Orders are reviewed", processWide);
        ProcessInference inference = new ProcessInference("Routing may depend on request category", processWide);
        ProcessEvidenceGap gap = new ProcessEvidenceGap(
                "What is missing?",
                ProcessEvidenceSource.UNKNOWN,
                "Which decision may change",
                processWide);
        ProcessConstraint constraint = new ProcessConstraint(
                "Refunds require approval",
                "internal commercial policy",
                "Whether refund release can proceed",
                processWide);

        assertThatThrownBy(() -> new ProcessAnalysisKnowledge(
                        listWithNull(fact),
                        List.of(inference),
                        List.of(gap),
                        List.of(constraint)))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ProcessAnalysisKnowledge(
                        List.of(fact),
                        listWithNull(inference),
                        List.of(gap),
                        List.of(constraint)))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ProcessAnalysisKnowledge(
                        List.of(fact),
                        List.of(inference),
                        listWithNull(gap),
                        List.of(constraint)))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ProcessAnalysisKnowledge(
                        List.of(fact),
                        List.of(inference),
                        List.of(gap),
                        listWithNull(constraint)))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void defensivelyCopiesCollections() {
        ProcessAnalysisScope processWide = ProcessAnalysisScope.processWide();
        ProcessKnownFact fact = new ProcessKnownFact("Orders are reviewed", processWide);
        ProcessInference inference = new ProcessInference("Routing may depend on request category", processWide);
        ProcessEvidenceGap gap = new ProcessEvidenceGap(
                "How often does reentry occur?",
                ProcessEvidenceSource.EMPIRICAL,
                "Whether reentry should be modeled separately",
                processWide);
        ProcessConstraint constraint = new ProcessConstraint(
                "Customer data must remain in approved systems",
                "legal requirement",
                "Whether system boundary can change",
                processWide);
        List<ProcessKnownFact> knownFacts = new ArrayList<>(List.of(fact));
        List<ProcessInference> inferences = new ArrayList<>(List.of(inference));
        List<ProcessEvidenceGap> evidenceGaps = new ArrayList<>(List.of(gap));
        List<ProcessConstraint> constraints = new ArrayList<>(List.of(constraint));

        ProcessAnalysisKnowledge knowledge =
                new ProcessAnalysisKnowledge(knownFacts, inferences, evidenceGaps, constraints);

        knownFacts.clear();
        inferences.clear();
        evidenceGaps.clear();
        constraints.clear();

        assertThat(knowledge.knownFacts()).containsExactly(fact);
        assertThat(knowledge.inferences()).containsExactly(inference);
        assertThat(knowledge.evidenceGaps()).containsExactly(gap);
        assertThat(knowledge.constraints()).containsExactly(constraint);
        assertThatThrownBy(() -> knowledge.knownFacts().add(new ProcessKnownFact("new fact", processWide)))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> knowledge.inferences().add(new ProcessInference("new inference", processWide)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void acceptsEmptyKnowledgeCollections() {
        ProcessAnalysisKnowledge knowledge = new ProcessAnalysisKnowledge(List.of(), List.of(), List.of(), List.of());

        assertThat(knowledge.knownFacts()).isEmpty();
        assertThat(knowledge.inferences()).isEmpty();
        assertThat(knowledge.evidenceGaps()).isEmpty();
        assertThat(knowledge.constraints()).isEmpty();
    }

    private static <T> List<T> listWithNull(T item) {
        List<T> items = new ArrayList<>();
        items.add(item);
        items.add(null);
        return items;
    }
}
