package com.codeworkdigital.api.processanalysis.application;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class ProcessEffortClarificationResolver {

    private final ProcessEffortClarificationAnswerMaterializer clarificationAnswerMaterializer;
    private final ProcessEffortEstablishedKnowledgeComposer establishedKnowledgeComposer;
    private final ProcessEffortPerReportingPeriodMaterializer effortPerReportingPeriodMaterializer;
    private final ProcessEffortMaterialityAssessmentEvaluator effortMaterialityAssessmentEvaluator;

    public ProcessEffortClarificationResolver(
            ProcessEffortClarificationAnswerMaterializer clarificationAnswerMaterializer,
            ProcessEffortEstablishedKnowledgeComposer establishedKnowledgeComposer,
            ProcessEffortPerReportingPeriodMaterializer effortPerReportingPeriodMaterializer,
            ProcessEffortMaterialityAssessmentEvaluator effortMaterialityAssessmentEvaluator) {
        this.clarificationAnswerMaterializer =
                Objects.requireNonNull(clarificationAnswerMaterializer, "clarificationAnswerMaterializer");
        this.establishedKnowledgeComposer =
                Objects.requireNonNull(establishedKnowledgeComposer, "establishedKnowledgeComposer");
        this.effortPerReportingPeriodMaterializer =
                Objects.requireNonNull(effortPerReportingPeriodMaterializer, "effortPerReportingPeriodMaterializer");
        this.effortMaterialityAssessmentEvaluator =
                Objects.requireNonNull(effortMaterialityAssessmentEvaluator, "effortMaterialityAssessmentEvaluator");
    }

    public ProcessEffortClarificationResolution resolve(
            ProcessAnalysisResult baseline,
            List<ProcessEffortMaterialityClarificationAnswer> answers) {
        Objects.requireNonNull(baseline, "baseline");
        Objects.requireNonNull(answers, "answers");

        requireEligibleBaseline(baseline);
        requireCompleteAnswerSet(baseline.materialityEvidenceGaps(), answers);

        ProcessEffortClarificationKnowledge clarificationKnowledge = clarificationAnswerMaterializer.materialize(
                baseline.materialityEvidenceGaps(),
                baseline.effortEvidence(),
                answers);
        requireClarificationKnowledgeForAllGaps(baseline.materialityEvidenceGaps(), clarificationKnowledge);

        ProcessEffortEstablishedKnowledge establishedKnowledge = establishedKnowledgeComposer.compose(
                baseline.sourceKnowledge(),
                clarificationKnowledge);
        ProcessEffortDerivedResult derivedResult = effortPerReportingPeriodMaterializer.materialize(establishedKnowledge)
                .orElseThrow(() -> new IllegalStateException(
                        "complete actionable clarification answers did not produce deterministic effort burden"));
        ProcessEffortMaterialityAssessment materialityAssessment =
                effortMaterialityAssessmentEvaluator.assess(Optional.of(derivedResult));
        if (materialityAssessment.status() == ProcessEffortMaterialityAssessmentStatus.NOT_ESTABLISHED) {
            throw new IllegalStateException(
                    "complete actionable clarification answers did not establish effort materiality");
        }

        return new ProcessEffortClarificationResolution(
                clarificationKnowledge,
                establishedKnowledge,
                derivedResult,
                materialityAssessment);
    }

    private static void requireEligibleBaseline(ProcessAnalysisResult baseline) {
        if (!baseline.understanding().isProcessIdentified()) {
            throw new IllegalArgumentException("baseline must identify a process");
        }
        ProcessEffortMaterialityAssessment assessment = baseline.materialityAssessment()
                .orElseThrow(() -> new IllegalArgumentException("baseline materiality assessment is required"));
        if (assessment.status() != ProcessEffortMaterialityAssessmentStatus.NOT_ESTABLISHED) {
            throw new IllegalArgumentException("baseline materiality assessment must be not established");
        }
        if (baseline.materialityEvidenceGaps().isEmpty()) {
            throw new IllegalArgumentException("baseline must have actionable materiality evidence gaps");
        }
    }

    private static void requireCompleteAnswerSet(
            List<ProcessEffortMaterialityEvidenceGap> gaps,
            List<ProcessEffortMaterialityClarificationAnswer> answers) {
        Set<ProcessEffortMaterialityEvidenceGapKind> gapKinds = gapKinds(gaps);
        Set<ProcessEffortMaterialityEvidenceGapKind> answerKinds = answerKinds(answers);
        if (!answerKinds.equals(gapKinds)) {
            throw new IllegalArgumentException("clarification answers must exactly match actionable gap kinds");
        }
    }

    private static Set<ProcessEffortMaterialityEvidenceGapKind> gapKinds(
            List<ProcessEffortMaterialityEvidenceGap> gaps) {
        Set<ProcessEffortMaterialityEvidenceGapKind> kinds =
                EnumSet.noneOf(ProcessEffortMaterialityEvidenceGapKind.class);
        for (ProcessEffortMaterialityEvidenceGap gap : gaps) {
            ProcessEffortMaterialityEvidenceGapKind kind =
                    Objects.requireNonNull(gap, "gap").kind();
            if (!kinds.add(kind)) {
                throw new IllegalArgumentException("duplicate actionable gap kind: " + kind);
            }
        }
        return kinds;
    }

    private static Set<ProcessEffortMaterialityEvidenceGapKind> answerKinds(
            List<ProcessEffortMaterialityClarificationAnswer> answers) {
        Map<ProcessEffortMaterialityEvidenceGapKind, ProcessEffortMaterialityClarificationAnswer> byKind =
                new EnumMap<>(ProcessEffortMaterialityEvidenceGapKind.class);
        for (ProcessEffortMaterialityClarificationAnswer answer : answers) {
            ProcessEffortMaterialityClarificationAnswer existing =
                    byKind.putIfAbsent(Objects.requireNonNull(answer, "answer").kind(), answer);
            if (existing != null) {
                throw new IllegalArgumentException("duplicate clarification answer kind: " + answer.kind());
            }
        }
        return byKind.keySet();
    }

    private static void requireClarificationKnowledgeForAllGaps(
            List<ProcessEffortMaterialityEvidenceGap> gaps,
            ProcessEffortClarificationKnowledge clarificationKnowledge) {
        Set<ProcessEffortMaterialityEvidenceGapKind> materializedKinds =
                EnumSet.noneOf(ProcessEffortMaterialityEvidenceGapKind.class);
        clarificationKnowledge.knownFacts().forEach(fact -> {
            if (fact.id().equals(ProcessEffortSourceKnowledgeMapper.VOLUME_FACT_ID)) {
                materializedKinds.add(ProcessEffortMaterialityEvidenceGapKind.VOLUME_PER_REPORTING_PERIOD);
            }
            if (fact.id().equals(ProcessEffortSourceKnowledgeMapper.EFFORT_FACT_ID)) {
                materializedKinds.add(ProcessEffortMaterialityEvidenceGapKind.EFFORT_PER_BUSINESS_ITEM);
            }
        });
        if (!materializedKinds.equals(gapKinds(gaps))) {
            throw new IllegalStateException(
                    "complete actionable clarification answers did not materialize required knowledge");
        }
    }
}
