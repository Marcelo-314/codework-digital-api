package com.codeworkdigital.api.processanalysis.application;

import com.codeworkdigital.api.processanalysis.domain.ProcessBusinessItemPerReportingPeriodUnit;
import com.codeworkdigital.api.processanalysis.domain.ProcessEffortDurationUnit;
import com.codeworkdigital.api.processanalysis.domain.ProcessEffortPerBusinessItemUnit;
import com.codeworkdigital.api.processanalysis.domain.ProcessEffortPerReportingPeriodUnit;
import com.codeworkdigital.api.processanalysis.domain.ProcessEvidenceArtifactId;
import com.codeworkdigital.api.processanalysis.domain.ProcessKnownFact;
import com.codeworkdigital.api.processanalysis.domain.ProcessKnownFactId;
import com.codeworkdigital.api.processanalysis.domain.ProcessQuantityProjection;
import com.codeworkdigital.api.processanalysis.domain.ProcessReportingPeriodUnit;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class ProcessEffortReasoningProjector {

    private ProcessEffortReasoningProjector() {
    }

    public static Reasoning project(ProcessAnalysisResult result) {
        Objects.requireNonNull(result, "result");
        return project(
                establishedKnowledge(result.sourceKnowledge()),
                result.derivedResult(),
                result.materialityAssessment());
    }

    public static Reasoning project(ProcessEffortClarificationResolution resolution) {
        Objects.requireNonNull(resolution, "resolution");
        return project(
                resolution.establishedKnowledge(),
                Optional.of(resolution.derivedResult()),
                Optional.of(resolution.materialityAssessment()));
    }

    private static Reasoning project(
            ProcessEffortEstablishedKnowledge establishedKnowledge,
            Optional<ProcessEffortDerivedResult> derivedResult,
            Optional<ProcessEffortMaterialityAssessment> materialityAssessment) {
        return new Reasoning(
                establishedInputs(establishedKnowledge),
                calculation(derivedResult).orElse(null),
                decision(materialityAssessment).orElse(null));
    }

    private static ProcessEffortEstablishedKnowledge establishedKnowledge(ProcessEffortSourceKnowledge sourceKnowledge) {
        return new ProcessEffortEstablishedKnowledge(
                sourceKnowledge.evidenceBase(),
                sourceKnowledge.knownFacts());
    }

    private static List<EstablishedInput> establishedInputs(ProcessEffortEstablishedKnowledge establishedKnowledge) {
        List<EstablishedInput> inputs = new ArrayList<>();
        input(
                establishedKnowledge,
                ProcessEffortSourceKnowledgeMapper.VOLUME_FACT_ID,
                InputCode.VOLUME_PER_REPORTING_PERIOD,
                QuantityUnit.BUSINESS_ITEM_PER_MONTH)
                .ifPresent(inputs::add);
        input(
                establishedKnowledge,
                ProcessEffortSourceKnowledgeMapper.EFFORT_FACT_ID,
                InputCode.EFFORT_PER_BUSINESS_ITEM,
                QuantityUnit.MINUTE_PER_BUSINESS_ITEM)
                .ifPresent(inputs::add);
        return inputs;
    }

    private static Optional<EstablishedInput> input(
            ProcessEffortEstablishedKnowledge establishedKnowledge,
            ProcessKnownFactId factId,
            InputCode code,
            QuantityUnit publicUnit) {
        return establishedKnowledge.knownFacts().stream()
                .filter(fact -> fact.id().equals(factId))
                .findFirst()
                .map(fact -> new EstablishedInput(
                        code,
                        quantity(fact, publicUnit),
                        source(fact, establishedKnowledge)));
    }

    private static Quantity quantity(ProcessKnownFact fact, QuantityUnit publicUnit) {
        ProcessQuantityProjection projection = fact.computableProjection()
                .filter(ProcessQuantityProjection.class::isInstance)
                .map(ProcessQuantityProjection.class::cast)
                .orElseThrow(() -> new IllegalStateException("established reasoning input requires quantity projection"));
        requireSupportedInputUnit(projection, publicUnit);
        return new Quantity(projection.magnitude(), publicUnit);
    }

    private static void requireSupportedInputUnit(ProcessQuantityProjection projection, QuantityUnit publicUnit) {
        switch (publicUnit) {
            case BUSINESS_ITEM_PER_MONTH -> {
                if (!(projection.unit() instanceof ProcessBusinessItemPerReportingPeriodUnit unit)
                        || unit.reportingPeriod() != ProcessReportingPeriodUnit.MONTH) {
                    throw new IllegalStateException("volume reasoning input requires business item per month");
                }
            }
            case MINUTE_PER_BUSINESS_ITEM -> {
                if (!(projection.unit() instanceof ProcessEffortPerBusinessItemUnit unit)
                        || unit.effortDuration() != ProcessEffortDurationUnit.MINUTE) {
                    throw new IllegalStateException("effort reasoning input requires minute per business item");
                }
            }
            case MINUTE_PER_MONTH -> throw new IllegalStateException("unsupported established input unit");
        }
    }

    private static InputSource source(
            ProcessKnownFact fact,
            ProcessEffortEstablishedKnowledge establishedKnowledge) {
        ProcessEvidenceArtifactId artifactId = fact.evidenceArtifactIds().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("established reasoning input requires evidence artifact"));
        if (artifactId.equals(ProcessEffortSourceKnowledgeMapper.SOURCE_ARTIFACT_ID)) {
            return InputSource.PROCESS_DESCRIPTION;
        }
        boolean clarificationArtifact = establishedKnowledge.evidenceBase().artifacts().stream()
                .anyMatch(artifact -> artifact.id().equals(artifactId))
                && (artifactId.equals(ProcessEffortClarificationAnswerMaterializer.VOLUME_CLARIFICATION_ARTIFACT_ID)
                || artifactId.equals(ProcessEffortClarificationAnswerMaterializer.EFFORT_CLARIFICATION_ARTIFACT_ID));
        if (clarificationArtifact) {
            return InputSource.CLARIFICATION_ANSWER;
        }
        throw new IllegalStateException("unsupported established reasoning input source");
    }

    private static Optional<Calculation> calculation(Optional<ProcessEffortDerivedResult> derivedResult) {
        return derivedResult.map(result -> {
            ProcessQuantityProjection projection = result.resultFact().computableProjection()
                    .filter(ProcessQuantityProjection.class::isInstance)
                    .map(ProcessQuantityProjection.class::cast)
                    .orElseThrow(() -> new IllegalStateException("reasoning calculation requires quantity projection"));
            requireMinutePerMonth(projection, "reasoning calculation result");
            return new Calculation(Operation.MULTIPLY, new Quantity(projection.magnitude(), QuantityUnit.MINUTE_PER_MONTH));
        });
    }

    private static Optional<Decision> decision(Optional<ProcessEffortMaterialityAssessment> materialityAssessment) {
        return materialityAssessment
                .filter(assessment -> assessment.status() != ProcessEffortMaterialityAssessmentStatus.NOT_ESTABLISHED)
                .map(assessment -> {
                    ProcessQuantityProjection threshold = assessment.threshold().projection();
                    requireMinutePerMonth(threshold, "reasoning decision threshold");
                    return new Decision(
                            "LAB_OPERATIONAL_BURDEN_THRESHOLD",
                            new Quantity(threshold.magnitude(), QuantityUnit.MINUTE_PER_MONTH),
                            comparison(assessment),
                            outcome(assessment.status()));
                });
    }

    private static void requireMinutePerMonth(ProcessQuantityProjection projection, String name) {
        if (!(projection.unit() instanceof ProcessEffortPerReportingPeriodUnit unit)
                || unit.effortDuration() != ProcessEffortDurationUnit.MINUTE
                || unit.reportingPeriod() != ProcessReportingPeriodUnit.MONTH) {
            throw new IllegalStateException(name + " requires minute per month");
        }
    }

    private static Comparison comparison(ProcessEffortMaterialityAssessment assessment) {
        ProcessQuantityProjection burden = assessment.establishedOperationalBurden()
                .orElseThrow(() -> new IllegalStateException("established reasoning decision requires burden"));
        BigDecimal threshold = assessment.threshold().projection().magnitude();
        int relation = burden.magnitude().compareTo(threshold);
        return switch (assessment.status()) {
            case NO_MATERIAL_JUSTIFICATION_IDENTIFIED -> {
                if (relation >= 0) {
                    throw new IllegalStateException("below-threshold outcome requires burden below threshold");
                }
                yield Comparison.BELOW_THRESHOLD;
            }
            case OPPORTUNITY_IDENTIFIED -> {
                if (relation < 0) {
                    throw new IllegalStateException("opportunity outcome requires burden at or above threshold");
                }
                yield Comparison.AT_OR_ABOVE_THRESHOLD;
            }
            case NOT_ESTABLISHED -> throw new IllegalStateException("not established has no public reasoning decision");
        };
    }

    private static DecisionOutcome outcome(ProcessEffortMaterialityAssessmentStatus status) {
        return switch (status) {
            case NO_MATERIAL_JUSTIFICATION_IDENTIFIED -> DecisionOutcome.NO_MATERIAL_JUSTIFICATION_IDENTIFIED;
            case OPPORTUNITY_IDENTIFIED -> DecisionOutcome.OPPORTUNITY_IDENTIFIED;
            case NOT_ESTABLISHED -> throw new IllegalStateException("not established has no public reasoning outcome");
        };
    }

    public record Reasoning(
            List<EstablishedInput> establishedInputs,
            Calculation calculation,
            Decision decision) {

        public Reasoning {
            establishedInputs = List.copyOf(establishedInputs);
        }
    }

    public record EstablishedInput(
            InputCode code,
            Quantity quantity,
            InputSource source) {
    }

    public record Calculation(
            Operation operation,
            Quantity result) {
    }

    public record Decision(
            String criterion,
            Quantity threshold,
            Comparison comparison,
            DecisionOutcome outcome) {
    }

    public record Quantity(
            BigDecimal magnitude,
            QuantityUnit unit) {
    }

    public enum InputCode {
        VOLUME_PER_REPORTING_PERIOD,
        EFFORT_PER_BUSINESS_ITEM
    }

    public enum InputSource {
        PROCESS_DESCRIPTION,
        CLARIFICATION_ANSWER
    }

    public enum QuantityUnit {
        BUSINESS_ITEM_PER_MONTH,
        MINUTE_PER_BUSINESS_ITEM,
        MINUTE_PER_MONTH
    }

    public enum Operation {
        MULTIPLY
    }

    public enum Comparison {
        BELOW_THRESHOLD,
        AT_OR_ABOVE_THRESHOLD
    }

    public enum DecisionOutcome {
        NO_MATERIAL_JUSTIFICATION_IDENTIFIED,
        OPPORTUNITY_IDENTIFIED
    }
}
