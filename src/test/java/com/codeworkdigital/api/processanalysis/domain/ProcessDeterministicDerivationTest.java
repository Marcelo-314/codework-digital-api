package com.codeworkdigital.api.processanalysis.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ProcessDeterministicDerivationTest {

    @Test
    void acceptsValidP06EffortPerReportingPeriodDerivation() {
        ProcessKnownFact volume = sourceQuantityFact("volume", "4000", requestsPerMonth("request"));
        ProcessKnownFact effort = sourceQuantityFact("effort", "2", minutesPerBusinessItem("request"));
        ProcessKnownFact result = derivedQuantityFact(
                "result",
                "8000",
                minutesPerMonth(),
                List.of(factId("volume"), factId("effort")));
        ProcessEffortPerReportingPeriodDerivation derivation =
                new ProcessEffortPerReportingPeriodDerivation(factId("volume"), factId("effort"), factId("result"));

        ProcessAnalysisKnowledge knowledge = knowledge(List.of(volume, effort, result), List.of(derivation));

        assertThat(knowledge.deterministicDerivations()).containsExactly(derivation);
    }

    @Test
    void acceptsArithmeticallyIncorrectResultMagnitudeWhenStructureIsValid() {
        ProcessKnownFact volume = sourceQuantityFact("volume", "4000", requestsPerMonth("request"));
        ProcessKnownFact effort = sourceQuantityFact("effort", "2", minutesPerBusinessItem("request"));
        ProcessKnownFact result = derivedQuantityFact(
                "result",
                "7999",
                minutesPerMonth(),
                List.of(factId("volume"), factId("effort")));

        assertThatCode(() -> knowledge(
                        List.of(volume, effort, result),
                        List.of(new ProcessEffortPerReportingPeriodDerivation(
                                factId("volume"),
                                factId("effort"),
                                factId("result")))))
                .doesNotThrowAnyException();
    }

    @Test
    void derivationRejectsNullFactIds() {
        assertThatThrownBy(() -> new ProcessEffortPerReportingPeriodDerivation(null, factId("effort"), factId("result")))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ProcessEffortPerReportingPeriodDerivation(factId("volume"), null, factId("result")))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ProcessEffortPerReportingPeriodDerivation(factId("volume"), factId("effort"), null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void derivationRejectsSameFactUsedForVolumeAndEffort() {
        assertThatThrownBy(() -> new ProcessEffortPerReportingPeriodDerivation(
                        factId("fact"),
                        factId("fact"),
                        factId("result")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("deterministic derivation fact ids must be distinct");
    }

    @Test
    void derivationRejectsResultEqualToVolume() {
        assertThatThrownBy(() -> new ProcessEffortPerReportingPeriodDerivation(
                        factId("volume"),
                        factId("effort"),
                        factId("volume")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("deterministic derivation fact ids must be distinct");
    }

    @Test
    void derivationRejectsResultEqualToEffort() {
        assertThatThrownBy(() -> new ProcessEffortPerReportingPeriodDerivation(
                        factId("volume"),
                        factId("effort"),
                        factId("effort")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("deterministic derivation fact ids must be distinct");
    }

    @Test
    void rejectsUnknownVolumeFact() {
        ProcessKnownFact effort = sourceQuantityFact("effort", "2", minutesPerBusinessItem("request"));
        ProcessKnownFact result = derivedQuantityFact(
                "result",
                "8000",
                minutesPerMonth(),
                List.of(factId("effort"), factId("other-volume")));
        ProcessKnownFact otherVolume = sourceQuantityFact("other-volume", "4000", requestsPerMonth("request"));

        assertThatThrownBy(() -> knowledge(
                        List.of(effort, result, otherVolume),
                        List.of(new ProcessEffortPerReportingPeriodDerivation(
                                factId("missing-volume"),
                                factId("effort"),
                                factId("result")))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unknown volumeFactId");
    }

    @Test
    void rejectsUnknownEffortFact() {
        ProcessKnownFact volume = sourceQuantityFact("volume", "4000", requestsPerMonth("request"));
        ProcessKnownFact result = derivedQuantityFact(
                "result",
                "8000",
                minutesPerMonth(),
                List.of(factId("volume"), factId("other-effort")));
        ProcessKnownFact otherEffort = sourceQuantityFact("other-effort", "2", minutesPerBusinessItem("request"));

        assertThatThrownBy(() -> knowledge(
                        List.of(volume, result, otherEffort),
                        List.of(new ProcessEffortPerReportingPeriodDerivation(
                                factId("volume"),
                                factId("missing-effort"),
                                factId("result")))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unknown effortPerBusinessItemFactId");
    }

    @Test
    void rejectsUnknownResultFact() {
        ProcessKnownFact volume = sourceQuantityFact("volume", "4000", requestsPerMonth("request"));
        ProcessKnownFact effort = sourceQuantityFact("effort", "2", minutesPerBusinessItem("request"));

        assertThatThrownBy(() -> knowledge(
                        List.of(volume, effort),
                        List.of(new ProcessEffortPerReportingPeriodDerivation(
                                factId("volume"),
                                factId("effort"),
                                factId("missing-result")))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unknown resultFactId");
    }

    @Test
    void rejectsSourceStatedResultFact() {
        ProcessKnownFact volume = sourceQuantityFact("volume", "4000", requestsPerMonth("request"));
        ProcessKnownFact effort = sourceQuantityFact("effort", "2", minutesPerBusinessItem("request"));
        ProcessKnownFact result = sourceQuantityFact("result", "8000", minutesPerMonth());

        assertThatThrownBy(() -> knowledge(
                        List.of(volume, effort, result),
                        List.of(new ProcessEffortPerReportingPeriodDerivation(
                                factId("volume"),
                                factId("effort"),
                                factId("result")))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("result fact must be DETERMINISTICALLY_DERIVED");
    }

    @Test
    void rejectsEmpiricallyEstablishedResultFact() {
        ProcessKnownFact volume = sourceQuantityFact("volume", "4000", requestsPerMonth("request"));
        ProcessKnownFact effort = sourceQuantityFact("effort", "2", minutesPerBusinessItem("request"));
        ProcessKnownFact result = empiricalQuantityFact("result", "8000", minutesPerMonth());

        assertThatThrownBy(() -> knowledge(
                        List.of(volume, effort, result),
                        List.of(new ProcessEffortPerReportingPeriodDerivation(
                                factId("volume"),
                                factId("effort"),
                                factId("result")))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("result fact must be DETERMINISTICALLY_DERIVED");
    }

    @Test
    void rejectsMissingOperandPremise() {
        ProcessKnownFact volume = sourceQuantityFact("volume", "4000", requestsPerMonth("request"));
        ProcessKnownFact effort = sourceQuantityFact("effort", "2", minutesPerBusinessItem("request"));
        ProcessKnownFact result = derivedQuantityFact("result", "8000", minutesPerMonth(), List.of(factId("volume")));

        assertThatThrownBy(() -> validDerivationKnowledge(volume, effort, result))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("result premises must match declared operand facts");
    }

    @Test
    void rejectsExtraPremise() {
        ProcessKnownFact volume = sourceQuantityFact("volume", "4000", requestsPerMonth("request"));
        ProcessKnownFact effort = sourceQuantityFact("effort", "2", minutesPerBusinessItem("request"));
        ProcessKnownFact extra = sourceQuantityFact("extra", "1", requestsPerMonth("request"));
        ProcessKnownFact result = derivedQuantityFact(
                "result",
                "8000",
                minutesPerMonth(),
                List.of(factId("volume"), factId("effort"), factId("extra")));

        assertThatThrownBy(() -> knowledge(
                        List.of(volume, effort, extra, result),
                        List.of(new ProcessEffortPerReportingPeriodDerivation(
                                factId("volume"),
                                factId("effort"),
                                factId("result")))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("result premises must match declared operand facts");
    }

    @Test
    void rejectsPremiseIdsThatDifferFromDeclaredOperands() {
        ProcessKnownFact volume = sourceQuantityFact("volume", "4000", requestsPerMonth("request"));
        ProcessKnownFact effort = sourceQuantityFact("effort", "2", minutesPerBusinessItem("request"));
        ProcessKnownFact other = sourceQuantityFact("other", "3", minutesPerBusinessItem("request"));
        ProcessKnownFact result = derivedQuantityFact(
                "result",
                "8000",
                minutesPerMonth(),
                List.of(factId("volume"), factId("other")));

        assertThatThrownBy(() -> knowledge(
                        List.of(volume, effort, other, result),
                        List.of(new ProcessEffortPerReportingPeriodDerivation(
                                factId("volume"),
                                factId("effort"),
                                factId("result")))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("result premises must match declared operand facts");
    }

    @Test
    void rejectsMissingVolumeProjection() {
        ProcessKnownFact volume = sourceFact("volume");
        ProcessKnownFact effort = sourceQuantityFact("effort", "2", minutesPerBusinessItem("request"));
        ProcessKnownFact result = derivedQuantityFact(
                "result",
                "8000",
                minutesPerMonth(),
                List.of(factId("volume"), factId("effort")));

        assertThatThrownBy(() -> validDerivationKnowledge(volume, effort, result))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("volumeFactId requires a computable projection");
    }

    @Test
    void rejectsMissingEffortProjection() {
        ProcessKnownFact volume = sourceQuantityFact("volume", "4000", requestsPerMonth("request"));
        ProcessKnownFact effort = sourceFact("effort");
        ProcessKnownFact result = derivedQuantityFact(
                "result",
                "8000",
                minutesPerMonth(),
                List.of(factId("volume"), factId("effort")));

        assertThatThrownBy(() -> validDerivationKnowledge(volume, effort, result))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("effortPerBusinessItemFactId requires a computable projection");
    }

    @Test
    void rejectsMissingResultProjection() {
        ProcessKnownFact volume = sourceQuantityFact("volume", "4000", requestsPerMonth("request"));
        ProcessKnownFact effort = sourceQuantityFact("effort", "2", minutesPerBusinessItem("request"));
        ProcessKnownFact result = derivedFact("result", List.of(factId("volume"), factId("effort")));

        assertThatThrownBy(() -> validDerivationKnowledge(volume, effort, result))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("resultFactId requires a computable projection");
    }

    @Test
    void rejectsCategoryProjectionInVolumeRole() {
        ProcessKnownFact volume = sourceCategoryFact("volume");
        ProcessKnownFact effort = sourceQuantityFact("effort", "2", minutesPerBusinessItem("request"));
        ProcessKnownFact result = derivedQuantityFact(
                "result",
                "8000",
                minutesPerMonth(),
                List.of(factId("volume"), factId("effort")));

        assertThatThrownBy(() -> validDerivationKnowledge(volume, effort, result))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("volumeFactId requires a quantity projection");
    }

    @Test
    void rejectsCategoryProjectionInEffortRole() {
        ProcessKnownFact volume = sourceQuantityFact("volume", "4000", requestsPerMonth("request"));
        ProcessKnownFact effort = sourceCategoryFact("effort");
        ProcessKnownFact result = derivedQuantityFact(
                "result",
                "8000",
                minutesPerMonth(),
                List.of(factId("volume"), factId("effort")));

        assertThatThrownBy(() -> validDerivationKnowledge(volume, effort, result))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("effortPerBusinessItemFactId requires a quantity projection");
    }

    @Test
    void rejectsCategoryProjectionInResultRole() {
        ProcessKnownFact volume = sourceQuantityFact("volume", "4000", requestsPerMonth("request"));
        ProcessKnownFact effort = sourceQuantityFact("effort", "2", minutesPerBusinessItem("request"));
        ProcessKnownFact result = derivedCategoryFact("result", List.of(factId("volume"), factId("effort")));

        assertThatThrownBy(() -> validDerivationKnowledge(volume, effort, result))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("resultFactId requires a quantity projection");
    }

    @Test
    void rejectsWrongQuantityUnitForVolumeRole() {
        ProcessKnownFact volume = sourceQuantityFact("volume", "4000", minutesPerMonth());
        ProcessKnownFact effort = sourceQuantityFact("effort", "2", minutesPerBusinessItem("request"));
        ProcessKnownFact result = derivedQuantityFact(
                "result",
                "8000",
                minutesPerMonth(),
                List.of(factId("volume"), factId("effort")));

        assertThatThrownBy(() -> validDerivationKnowledge(volume, effort, result))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("volumeFactId requires unit ProcessBusinessItemPerReportingPeriodUnit");
    }

    @Test
    void rejectsWrongQuantityUnitForEffortRole() {
        ProcessKnownFact volume = sourceQuantityFact("volume", "4000", requestsPerMonth("request"));
        ProcessKnownFact effort = sourceQuantityFact("effort", "2", requestsPerMonth("request"));
        ProcessKnownFact result = derivedQuantityFact(
                "result",
                "8000",
                minutesPerMonth(),
                List.of(factId("volume"), factId("effort")));

        assertThatThrownBy(() -> validDerivationKnowledge(volume, effort, result))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("effortPerBusinessItemFactId requires unit ProcessEffortPerBusinessItemUnit");
    }

    @Test
    void rejectsWrongQuantityUnitForResultRole() {
        ProcessKnownFact volume = sourceQuantityFact("volume", "4000", requestsPerMonth("request"));
        ProcessKnownFact effort = sourceQuantityFact("effort", "2", minutesPerBusinessItem("request"));
        ProcessKnownFact result = derivedQuantityFact(
                "result",
                "8000",
                requestsPerMonth("request"),
                List.of(factId("volume"), factId("effort")));

        assertThatThrownBy(() -> validDerivationKnowledge(volume, effort, result))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("resultFactId requires unit ProcessEffortPerReportingPeriodUnit");
    }

    @Test
    void rejectsDifferentBusinessItemUnits() {
        ProcessKnownFact volume = sourceQuantityFact("volume", "4000", requestsPerMonth("request"));
        ProcessKnownFact effort = sourceQuantityFact("effort", "2", minutesPerBusinessItem("invoice"));
        ProcessKnownFact result = derivedQuantityFact(
                "result",
                "8000",
                minutesPerMonth(),
                List.of(factId("volume"), factId("effort")));

        assertThatThrownBy(() -> validDerivationKnowledge(volume, effort, result))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("business item units must match");
    }

    private static ProcessAnalysisKnowledge validDerivationKnowledge(
            ProcessKnownFact volume,
            ProcessKnownFact effort,
            ProcessKnownFact result) {
        return knowledge(
                List.of(volume, effort, result),
                List.of(new ProcessEffortPerReportingPeriodDerivation(
                        factId("volume"),
                        factId("effort"),
                        factId("result"))));
    }

    private static ProcessAnalysisKnowledge knowledge(
            List<ProcessKnownFact> knownFacts,
            List<ProcessDeterministicDerivation> derivations) {
        return new ProcessAnalysisKnowledge(knownFacts, List.of(), List.of(), List.of(), derivations);
    }

    private static ProcessKnownFact sourceFact(String id) {
        return new ProcessKnownFact(
                factId(id),
                "Source fact " + id,
                ProcessFactGrounding.SOURCE_STATED,
                ProcessAnalysisScope.processWide(),
                List.of(),
                List.of(artifactId(id)));
    }

    private static ProcessKnownFact sourceQuantityFact(
            String id,
            String magnitude,
            ProcessQuantityUnitExpression unit) {
        return new ProcessKnownFact(
                factId(id),
                "Source quantity fact " + id,
                ProcessFactGrounding.SOURCE_STATED,
                ProcessAnalysisScope.processWide(),
                List.of(),
                List.of(artifactId(id)),
                Optional.of(quantity(magnitude, unit)));
    }

    private static ProcessKnownFact empiricalQuantityFact(
            String id,
            String magnitude,
            ProcessQuantityUnitExpression unit) {
        return new ProcessKnownFact(
                factId(id),
                "Empirical quantity fact " + id,
                ProcessFactGrounding.EMPIRICALLY_ESTABLISHED,
                ProcessAnalysisScope.processWide(),
                List.of(),
                List.of(artifactId(id)),
                Optional.of(quantity(magnitude, unit)));
    }

    private static ProcessKnownFact sourceCategoryFact(String id) {
        return new ProcessKnownFact(
                factId(id),
                "Source category fact " + id,
                ProcessFactGrounding.SOURCE_STATED,
                ProcessAnalysisScope.processWide(),
                List.of(),
                List.of(artifactId(id)),
                Optional.of(new ProcessCategoryProjection(
                        new ProcessCategoryDomainId("request-type"),
                        new ProcessCategoryMemberId("request"))));
    }

    private static ProcessKnownFact derivedFact(String id, List<ProcessKnownFactId> premiseFactIds) {
        return new ProcessKnownFact(
                factId(id),
                "Derived fact " + id,
                ProcessFactGrounding.DETERMINISTICALLY_DERIVED,
                ProcessAnalysisScope.processWide(),
                premiseFactIds,
                List.of());
    }

    private static ProcessKnownFact derivedQuantityFact(
            String id,
            String magnitude,
            ProcessQuantityUnitExpression unit,
            List<ProcessKnownFactId> premiseFactIds) {
        return new ProcessKnownFact(
                factId(id),
                "Derived quantity fact " + id,
                ProcessFactGrounding.DETERMINISTICALLY_DERIVED,
                ProcessAnalysisScope.processWide(),
                premiseFactIds,
                List.of(),
                Optional.of(quantity(magnitude, unit)));
    }

    private static ProcessKnownFact derivedCategoryFact(String id, List<ProcessKnownFactId> premiseFactIds) {
        return new ProcessKnownFact(
                factId(id),
                "Derived category fact " + id,
                ProcessFactGrounding.DETERMINISTICALLY_DERIVED,
                ProcessAnalysisScope.processWide(),
                premiseFactIds,
                List.of(),
                Optional.of(new ProcessCategoryProjection(
                        new ProcessCategoryDomainId("routing-destination"),
                        new ProcessCategoryMemberId("support"))));
    }

    private static ProcessQuantityProjection quantity(String magnitude, ProcessQuantityUnitExpression unit) {
        return new ProcessQuantityProjection(new BigDecimal(magnitude), unit);
    }

    private static ProcessBusinessItemPerReportingPeriodUnit requestsPerMonth(String businessItemId) {
        return new ProcessBusinessItemPerReportingPeriodUnit(
                new ProcessBusinessItemUnitId(businessItemId),
                ProcessReportingPeriodUnit.MONTH);
    }

    private static ProcessEffortPerBusinessItemUnit minutesPerBusinessItem(String businessItemId) {
        return new ProcessEffortPerBusinessItemUnit(
                ProcessEffortDurationUnit.MINUTE,
                new ProcessBusinessItemUnitId(businessItemId));
    }

    private static ProcessEffortPerReportingPeriodUnit minutesPerMonth() {
        return new ProcessEffortPerReportingPeriodUnit(
                ProcessEffortDurationUnit.MINUTE,
                ProcessReportingPeriodUnit.MONTH);
    }

    private static ProcessKnownFactId factId(String value) {
        return new ProcessKnownFactId(value);
    }

    private static ProcessEvidenceArtifactId artifactId(String value) {
        return new ProcessEvidenceArtifactId("source-" + value);
    }
}
