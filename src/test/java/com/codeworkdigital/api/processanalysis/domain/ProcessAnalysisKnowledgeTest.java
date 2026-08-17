package com.codeworkdigital.api.processanalysis.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProcessAnalysisKnowledgeTest {

    @Test
    void keepsKnownFactsEvidenceGapsAndConstraintsDistinct() {
        ProcessKnownFact fact = new ProcessKnownFact("Orders are reviewed before fulfillment");
        ProcessEvidenceGap gap = new ProcessEvidenceGap(
                "How often are orders revised after review?",
                ProcessEvidenceSource.EMPIRICAL,
                "Whether revision handling needs separate analysis");
        ProcessConstraint constraint = new ProcessConstraint(
                "Only approved staff may release refunds",
                "governance rule",
                "Whether automated release is allowed");

        ProcessAnalysisKnowledge knowledge =
                new ProcessAnalysisKnowledge(List.of(fact), List.of(gap), List.of(constraint));

        assertThat(knowledge.knownFacts()).containsExactly(fact);
        assertThat(knowledge.evidenceGaps()).containsExactly(gap);
        assertThat(knowledge.constraints()).containsExactly(constraint);
    }

    @Test
    void acceptsSelfReportedEvidenceGaps() {
        ProcessEvidenceGap gap = new ProcessEvidenceGap(
                "Who decides the manual route?",
                ProcessEvidenceSource.SELF_REPORTED,
                "Whether routing authority is explicit");

        assertThat(gap.source()).isEqualTo(ProcessEvidenceSource.SELF_REPORTED);
    }

    @Test
    void acceptsEmpiricalEvidenceGaps() {
        ProcessEvidenceGap gap = new ProcessEvidenceGap(
                "How many submissions reenter validation?",
                ProcessEvidenceSource.EMPIRICAL,
                "Whether reentry volume changes process risk");

        assertThat(gap.source()).isEqualTo(ProcessEvidenceSource.EMPIRICAL);
    }

    @Test
    void acceptsUnknownEvidenceSourceWhereAppropriate() {
        ProcessEvidenceGap gap = new ProcessEvidenceGap(
                "What determines fulfillment path selection?",
                ProcessEvidenceSource.UNKNOWN,
                "Whether branching criteria can be specified");

        assertThat(gap.source()).isEqualTo(ProcessEvidenceSource.UNKNOWN);
    }

    @Test
    void rejectsBlankOrNullRequiredValues() {
        assertThatThrownBy(() -> new ProcessKnownFact(" "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ProcessEvidenceGap(null, ProcessEvidenceSource.UNKNOWN, "decision"))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ProcessEvidenceGap("question", null, "decision"))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ProcessEvidenceGap("question", ProcessEvidenceSource.UNKNOWN, " "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ProcessConstraint("constraint", "policy", " "))
                .isInstanceOf(IllegalArgumentException.class);
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
        ProcessKnownFact fact = new ProcessKnownFact("Orders are reviewed");
        ProcessEvidenceGap gap = new ProcessEvidenceGap(
                "What is missing?",
                ProcessEvidenceSource.UNKNOWN,
                "Which decision may change");
        ProcessConstraint constraint = new ProcessConstraint(
                "Refunds require approval",
                "internal commercial policy",
                "Whether refund release can proceed");

        assertThatThrownBy(() -> new ProcessAnalysisKnowledge(
                        listWithNull(fact),
                        List.of(gap),
                        List.of(constraint)))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ProcessAnalysisKnowledge(
                        List.of(fact),
                        listWithNull(gap),
                        List.of(constraint)))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ProcessAnalysisKnowledge(
                        List.of(fact),
                        List.of(gap),
                        listWithNull(constraint)))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void defensivelyCopiesCollections() {
        ProcessKnownFact fact = new ProcessKnownFact("Orders are reviewed");
        ProcessEvidenceGap gap = new ProcessEvidenceGap(
                "How often does reentry occur?",
                ProcessEvidenceSource.EMPIRICAL,
                "Whether reentry should be modeled separately");
        ProcessConstraint constraint = new ProcessConstraint(
                "Customer data must remain in approved systems",
                "legal requirement",
                "Whether system boundary can change");
        List<ProcessKnownFact> knownFacts = new ArrayList<>(List.of(fact));
        List<ProcessEvidenceGap> evidenceGaps = new ArrayList<>(List.of(gap));
        List<ProcessConstraint> constraints = new ArrayList<>(List.of(constraint));

        ProcessAnalysisKnowledge knowledge =
                new ProcessAnalysisKnowledge(knownFacts, evidenceGaps, constraints);

        knownFacts.clear();
        evidenceGaps.clear();
        constraints.clear();

        assertThat(knowledge.knownFacts()).containsExactly(fact);
        assertThat(knowledge.evidenceGaps()).containsExactly(gap);
        assertThat(knowledge.constraints()).containsExactly(constraint);
        assertThatThrownBy(() -> knowledge.knownFacts().add(new ProcessKnownFact("new fact")))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void acceptsEmptyKnowledgeCollections() {
        ProcessAnalysisKnowledge knowledge = new ProcessAnalysisKnowledge(List.of(), List.of(), List.of());

        assertThat(knowledge.knownFacts()).isEmpty();
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
