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
        ProcessKnownFact fact = new ProcessKnownFact(
                factId("fact-1"),
                "Orders are reviewed before fulfillment",
                ProcessFactGrounding.SOURCE_STATED,
                processWide,
                List.of(),
                List.of(artifactId("source-1")));
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
    void acceptsSourceStatedKnownFact() {
        ProcessKnownFact fact = new ProcessKnownFact(
                factId("fact-1"),
                "Requests arrive through a form",
                ProcessFactGrounding.SOURCE_STATED,
                ProcessAnalysisScope.processWide(),
                List.of(),
                List.of(artifactId("source-1")));

        assertThat(fact.id()).isEqualTo(factId("fact-1"));
        assertThat(fact.grounding()).isEqualTo(ProcessFactGrounding.SOURCE_STATED);
        assertThat(fact.evidenceArtifactIds()).containsExactly(artifactId("source-1"));
    }

    @Test
    void acceptsEmpiricallyEstablishedKnownFact() {
        ProcessKnownFact fact = new ProcessKnownFact(
                factId("fact-1"),
                "The measured monthly average is 3,986 requests",
                ProcessFactGrounding.EMPIRICALLY_ESTABLISHED,
                ProcessAnalysisScope.processWide(),
                List.of(),
                List.of(artifactId("measurement-1")));

        assertThat(fact.grounding()).isEqualTo(ProcessFactGrounding.EMPIRICALLY_ESTABLISHED);
        assertThat(fact.evidenceArtifactIds()).containsExactly(artifactId("measurement-1"));
    }

    @Test
    void acceptsDeterministicallyDerivedKnownFactWithPremiseFacts() {
        ProcessKnownFact premise = sourceFact("fact-0", "4,000 requests are handled monthly", "source-1");
        ProcessKnownFact fact = new ProcessKnownFact(
                factId("fact-1"),
                "4,000 requests at 2 minutes each is 8,000 minutes",
                ProcessFactGrounding.DETERMINISTICALLY_DERIVED,
                ProcessAnalysisScope.processWide(),
                List.of(factId("fact-0")),
                List.of());

        ProcessAnalysisKnowledge knowledge =
                new ProcessAnalysisKnowledge(List.of(premise, fact), List.of(), List.of(), List.of());

        assertThat(knowledge.knownFacts()).containsExactly(premise, fact);
        assertThat(knowledge.inferences()).isEmpty();
        assertThat(fact.grounding()).isEqualTo(ProcessFactGrounding.DETERMINISTICALLY_DERIVED);
        assertThat(fact.premiseFactIds()).containsExactly(factId("fact-0"));
        assertThat(fact.evidenceArtifactIds()).isEmpty();
    }

    @Test
    void sourceStatedKnownFactRequiresEvidenceArtifactIds() {
        assertThatThrownBy(() -> new ProcessKnownFact(
                        factId("fact-1"),
                        "Requests arrive through a form",
                        ProcessFactGrounding.SOURCE_STATED,
                        ProcessAnalysisScope.processWide(),
                        List.of(),
                        List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SOURCE_STATED known facts require evidence artifact ids");
    }

    @Test
    void empiricallyEstablishedKnownFactRequiresEvidenceArtifactIds() {
        assertThatThrownBy(() -> new ProcessKnownFact(
                        factId("fact-1"),
                        "The measured monthly average is 3,986 requests",
                        ProcessFactGrounding.EMPIRICALLY_ESTABLISHED,
                        ProcessAnalysisScope.processWide(),
                        List.of(),
                        List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("EMPIRICALLY_ESTABLISHED known facts require evidence artifact ids");
    }

    @Test
    void deterministicallyDerivedKnownFactRejectsEvidenceArtifactIds() {
        assertThatThrownBy(() -> new ProcessKnownFact(
                        factId("fact-1"),
                        "4,000 requests at 2 minutes each is 8,000 minutes",
                        ProcessFactGrounding.DETERMINISTICALLY_DERIVED,
                        ProcessAnalysisScope.processWide(),
                        List.of(factId("fact-0")),
                        List.of(artifactId("artifact-1"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("DETERMINISTICALLY_DERIVED known facts do not accept evidence artifact ids");
    }

    @Test
    void knownFactRejectsDuplicateEvidenceArtifactIds() {
        assertThatThrownBy(() -> new ProcessKnownFact(
                        factId("fact-1"),
                        "Requests arrive through a form",
                        ProcessFactGrounding.SOURCE_STATED,
                        ProcessAnalysisScope.processWide(),
                        List.of(),
                        List.of(artifactId("source-1"), artifactId("source-1"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("known fact evidence artifact id must be unique: source-1");
    }

    @Test
    void knownFactDefensivelyCopiesAndReturnsImmutableEvidenceArtifactIds() {
        List<ProcessEvidenceArtifactId> artifactIds = new ArrayList<>(List.of(artifactId("source-1")));

        ProcessKnownFact fact = new ProcessKnownFact(
                factId("fact-1"),
                "Requests arrive through a form",
                ProcessFactGrounding.SOURCE_STATED,
                ProcessAnalysisScope.processWide(),
                List.of(),
                artifactIds);

        artifactIds.clear();

        assertThat(fact.evidenceArtifactIds()).containsExactly(artifactId("source-1"));
        assertThatThrownBy(() -> fact.evidenceArtifactIds().add(artifactId("source-2")))
                .isInstanceOf(UnsupportedOperationException.class);
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

        assertThatThrownBy(() -> new ProcessKnownFact(
                        factId("fact-1"),
                        " ",
                        ProcessFactGrounding.SOURCE_STATED,
                        processWide,
                        List.of(),
                        List.of(artifactId("source-1"))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ProcessKnownFact(
                        null,
                        "fact",
                        ProcessFactGrounding.SOURCE_STATED,
                        processWide,
                        List.of(),
                        List.of(artifactId("source-1"))))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ProcessKnownFact(
                        factId("fact-1"),
                        "fact",
                        null,
                        processWide,
                        List.of(),
                        List.of(artifactId("source-1"))))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ProcessKnownFact(
                        factId("fact-1"),
                        "fact",
                        ProcessFactGrounding.SOURCE_STATED,
                        null,
                        List.of(),
                        List.of(artifactId("source-1"))))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ProcessKnownFact(
                        factId("fact-1"),
                        "fact",
                        ProcessFactGrounding.SOURCE_STATED,
                        processWide,
                        null,
                        List.of(artifactId("source-1"))))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ProcessKnownFact(
                        factId("fact-1"),
                        "fact",
                        ProcessFactGrounding.SOURCE_STATED,
                        processWide,
                        listWithNull(factId("fact-0")),
                        List.of(artifactId("source-1"))))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ProcessKnownFact(
                        factId("fact-1"),
                        "fact",
                        ProcessFactGrounding.SOURCE_STATED,
                        processWide,
                        List.of(),
                        null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ProcessKnownFact(
                        factId("fact-1"),
                        "fact",
                        ProcessFactGrounding.SOURCE_STATED,
                        processWide,
                        List.of(),
                        listWithNull(artifactId("source-1"))))
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
        ProcessKnownFact fact = new ProcessKnownFact(
                factId("fact-1"),
                "Orders are reviewed",
                ProcessFactGrounding.SOURCE_STATED,
                processWide,
                List.of(),
                List.of(artifactId("source-1")));
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
        ProcessKnownFact fact = new ProcessKnownFact(
                factId("fact-1"),
                "Orders are reviewed",
                ProcessFactGrounding.SOURCE_STATED,
                processWide,
                List.of(),
                List.of(artifactId("source-1")));
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
        assertThatThrownBy(() -> knowledge.knownFacts().add(new ProcessKnownFact(
                        factId("fact-2"),
                        "new fact",
                        ProcessFactGrounding.SOURCE_STATED,
                        processWide,
                        List.of(),
                        List.of(artifactId("source-2")))))
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

    @Test
    void acceptsMultipleKnownFactsWithDifferentIds() {
        ProcessKnownFact first = sourceFact("fact-1", "Orders are reviewed", "source-1");
        ProcessKnownFact second = sourceFact("fact-2", "Orders are approved", "source-2");

        ProcessAnalysisKnowledge knowledge =
                new ProcessAnalysisKnowledge(List.of(first, second), List.of(), List.of(), List.of());

        assertThat(knowledge.knownFacts()).containsExactly(first, second);
    }

    @Test
    void rejectsDuplicateKnownFactIdWhenFactsDiffer() {
        ProcessKnownFact first = sourceFact("fact-1", "Orders are reviewed", "source-1");
        ProcessKnownFact second = sourceFact("fact-1", "Orders are approved", "source-2");

        assertThatThrownBy(() -> new ProcessAnalysisKnowledge(List.of(first, second), List.of(), List.of(), List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("known fact id must be unique: fact-1");
    }

    @Test
    void rejectsDuplicateKnownFactIdWhenFactsAreStructurallyIdentical() {
        ProcessKnownFact fact = sourceFact("fact-1", "Orders are reviewed", "source-1");

        assertThatThrownBy(() -> new ProcessAnalysisKnowledge(List.of(fact, fact), List.of(), List.of(), List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("known fact id must be unique: fact-1");
    }

    @Test
    void sourceStatedKnownFactRejectsPremiseFactIds() {
        assertThatThrownBy(() -> new ProcessKnownFact(
                        factId("fact-1"),
                        "Requests arrive through a form",
                        ProcessFactGrounding.SOURCE_STATED,
                        ProcessAnalysisScope.processWide(),
                        List.of(factId("fact-0")),
                        List.of(artifactId("source-1"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SOURCE_STATED known facts do not accept premise fact ids");
    }

    @Test
    void empiricallyEstablishedKnownFactRejectsPremiseFactIds() {
        assertThatThrownBy(() -> new ProcessKnownFact(
                        factId("fact-1"),
                        "The measured monthly average is 3,986 requests",
                        ProcessFactGrounding.EMPIRICALLY_ESTABLISHED,
                        ProcessAnalysisScope.processWide(),
                        List.of(factId("fact-0")),
                        List.of(artifactId("measurement-1"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("EMPIRICALLY_ESTABLISHED known facts do not accept premise fact ids");
    }

    @Test
    void deterministicallyDerivedKnownFactRequiresPremiseFactIds() {
        assertThatThrownBy(() -> new ProcessKnownFact(
                        factId("fact-1"),
                        "4,000 requests at 2 minutes each is 8,000 minutes",
                        ProcessFactGrounding.DETERMINISTICALLY_DERIVED,
                        ProcessAnalysisScope.processWide(),
                        List.of(),
                        List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("DETERMINISTICALLY_DERIVED known facts require premise fact ids");
    }

    @Test
    void deterministicallyDerivedKnownFactAcceptsMultipleDistinctPremiseFactIds() {
        ProcessKnownFact fact = derivedFact(
                "fact-3",
                "The total effort is 8,000 minutes",
                List.of(factId("fact-1"), factId("fact-2")));

        assertThat(fact.premiseFactIds()).containsExactly(factId("fact-1"), factId("fact-2"));
    }

    @Test
    void knownFactRejectsDuplicatePremiseFactIds() {
        assertThatThrownBy(() -> derivedFact(
                        "fact-3",
                        "The total effort is 8,000 minutes",
                        List.of(factId("fact-1"), factId("fact-1"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("known fact premise fact id must be unique: fact-1");
    }

    @Test
    void knownFactDefensivelyCopiesAndReturnsImmutablePremiseFactIds() {
        List<ProcessKnownFactId> premiseFactIds = new ArrayList<>(List.of(factId("fact-1")));

        ProcessKnownFact fact = derivedFact("fact-2", "Derived fact", premiseFactIds);

        premiseFactIds.clear();

        assertThat(fact.premiseFactIds()).containsExactly(factId("fact-1"));
        assertThatThrownBy(() -> fact.premiseFactIds().add(factId("fact-3")))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void acceptsKnownFactReferencingExistingPremise() {
        ProcessKnownFact premise = sourceFact("fact-1", "Orders are reviewed", "source-1");
        ProcessKnownFact derived = derivedFact("fact-2", "Reviewed orders are ready", List.of(factId("fact-1")));

        ProcessAnalysisKnowledge knowledge =
                new ProcessAnalysisKnowledge(List.of(premise, derived), List.of(), List.of(), List.of());

        assertThat(knowledge.knownFacts()).containsExactly(premise, derived);
    }

    @Test
    void rejectsUnknownPremiseFactId() {
        ProcessKnownFact derived = derivedFact("fact-2", "Reviewed orders are ready", List.of(factId("missing-fact")));

        assertThatThrownBy(() -> new ProcessAnalysisKnowledge(List.of(derived), List.of(), List.of(), List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("known fact references an unknown premise fact: missing-fact");
    }

    @Test
    void rejectsSelfReferencingPremiseFactId() {
        ProcessKnownFact derived = derivedFact("fact-1", "Derived fact", List.of(factId("fact-1")));

        assertThatThrownBy(() -> new ProcessAnalysisKnowledge(List.of(derived), List.of(), List.of(), List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("known fact cannot reference itself as a premise: fact-1");
    }

    @Test
    void rejectsDirectTwoNodeKnownFactPremiseCycle() {
        ProcessKnownFact first = derivedFact("fact-1", "First derived fact", List.of(factId("fact-2")));
        ProcessKnownFact second = derivedFact("fact-2", "Second derived fact", List.of(factId("fact-1")));

        assertThatThrownBy(() -> new ProcessAnalysisKnowledge(List.of(first, second), List.of(), List.of(), List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cyclic known-fact premise relationship");
    }

    @Test
    void rejectsIndirectKnownFactPremiseCycle() {
        ProcessKnownFact first = derivedFact("fact-1", "First derived fact", List.of(factId("fact-3")));
        ProcessKnownFact second = derivedFact("fact-2", "Second derived fact", List.of(factId("fact-1")));
        ProcessKnownFact third = derivedFact("fact-3", "Third derived fact", List.of(factId("fact-2")));

        assertThatThrownBy(
                        () -> new ProcessAnalysisKnowledge(List.of(first, second, third), List.of(), List.of(), List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cyclic known-fact premise relationship");
    }

    @Test
    void acceptsAcyclicChainOfDerivedKnownFacts() {
        ProcessKnownFact source = sourceFact("fact-1", "Orders are reviewed", "source-1");
        ProcessKnownFact firstDerived = derivedFact("fact-2", "Reviewed orders are ready", List.of(factId("fact-1")));
        ProcessKnownFact secondDerived = derivedFact("fact-3", "Ready orders can ship", List.of(factId("fact-2")));

        ProcessAnalysisKnowledge knowledge = new ProcessAnalysisKnowledge(
                List.of(source, firstDerived, secondDerived),
                List.of(),
                List.of(),
                List.of());

        assertThat(knowledge.knownFacts()).containsExactly(source, firstDerived, secondDerived);
    }

    @Test
    void knownFactListOrderDoesNotAffectPremiseReferenceValidity() {
        ProcessKnownFact source = sourceFact("fact-1", "Orders are reviewed", "source-1");
        ProcessKnownFact derived = derivedFact("fact-2", "Reviewed orders are ready", List.of(factId("fact-1")));

        ProcessAnalysisKnowledge knowledge =
                new ProcessAnalysisKnowledge(List.of(derived, source), List.of(), List.of(), List.of());

        assertThat(knowledge.knownFacts()).containsExactly(derived, source);
    }

    private static <T> List<T> listWithNull(T item) {
        List<T> items = new ArrayList<>();
        items.add(item);
        items.add(null);
        return items;
    }

    private static ProcessEvidenceArtifactId artifactId(String value) {
        return new ProcessEvidenceArtifactId(value);
    }

    private static ProcessKnownFactId factId(String value) {
        return new ProcessKnownFactId(value);
    }

    private static ProcessKnownFact sourceFact(String id, String statement, String artifactId) {
        return new ProcessKnownFact(
                factId(id),
                statement,
                ProcessFactGrounding.SOURCE_STATED,
                ProcessAnalysisScope.processWide(),
                List.of(),
                List.of(artifactId(artifactId)));
    }

    private static ProcessKnownFact derivedFact(
            String id,
            String statement,
            List<ProcessKnownFactId> premiseFactIds) {
        return new ProcessKnownFact(
                factId(id),
                statement,
                ProcessFactGrounding.DETERMINISTICALLY_DERIVED,
                ProcessAnalysisScope.processWide(),
                premiseFactIds,
                List.of());
    }
}
