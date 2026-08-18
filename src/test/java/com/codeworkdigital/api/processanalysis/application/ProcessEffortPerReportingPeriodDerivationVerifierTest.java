package com.codeworkdigital.api.processanalysis.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codeworkdigital.api.processanalysis.domain.ProcessAnalysisKnowledge;
import com.codeworkdigital.api.processanalysis.domain.ProcessAnalysisScope;
import com.codeworkdigital.api.processanalysis.domain.ProcessBusinessItemPerReportingPeriodUnit;
import com.codeworkdigital.api.processanalysis.domain.ProcessBusinessItemUnitId;
import com.codeworkdigital.api.processanalysis.domain.ProcessComputableProjection;
import com.codeworkdigital.api.processanalysis.domain.ProcessEffortDurationUnit;
import com.codeworkdigital.api.processanalysis.domain.ProcessEffortPerBusinessItemUnit;
import com.codeworkdigital.api.processanalysis.domain.ProcessEffortPerReportingPeriodDerivation;
import com.codeworkdigital.api.processanalysis.domain.ProcessEffortPerReportingPeriodUnit;
import com.codeworkdigital.api.processanalysis.domain.ProcessEvidenceArtifactId;
import com.codeworkdigital.api.processanalysis.domain.ProcessFactGrounding;
import com.codeworkdigital.api.processanalysis.domain.ProcessKnownFact;
import com.codeworkdigital.api.processanalysis.domain.ProcessKnownFactId;
import com.codeworkdigital.api.processanalysis.domain.ProcessQuantityProjection;
import com.codeworkdigital.api.processanalysis.domain.ProcessQuantityUnitExpression;
import com.codeworkdigital.api.processanalysis.domain.ProcessReportingPeriodUnit;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ProcessEffortPerReportingPeriodDerivationVerifierTest {

    private final ProcessEffortPerReportingPeriodDerivationVerifier verifier =
            new ProcessEffortPerReportingPeriodDerivationVerifier();

    @Test
    void verifiesValidP06bEffortPerReportingPeriodMagnitude() {
        ProcessEffortPerReportingPeriodDerivation derivation = standardDerivation();
        ProcessAnalysisKnowledge knowledge = knowledge("4000", "2", "8000", derivation);

        assertThat(verifier.verify(knowledge, derivation)).isTrue();
    }

    @Test
    void returnsFalseForInvalidDeclaredResultMagnitude() {
        ProcessEffortPerReportingPeriodDerivation derivation = standardDerivation();
        ProcessAnalysisKnowledge knowledge = knowledge("4000", "2", "7999", derivation);

        assertThatCode(() -> knowledge("4000", "2", "7999", derivation)).doesNotThrowAnyException();
        assertThat(verifier.verify(knowledge, derivation)).isFalse();
    }

    @Test
    void verifiesValidP06aEffortPerReportingPeriodMagnitude() {
        ProcessEffortPerReportingPeriodDerivation derivation = standardDerivation();
        ProcessAnalysisKnowledge knowledge = knowledge("4", "2", "8", derivation);

        assertThat(verifier.verify(knowledge, derivation)).isTrue();
    }

    @Test
    void verifiesZeroMagnitude() {
        ProcessEffortPerReportingPeriodDerivation derivation = standardDerivation();
        ProcessAnalysisKnowledge knowledge = knowledge("0", "2", "0", derivation);

        assertThat(verifier.verify(knowledge, derivation)).isTrue();
    }

    @Test
    void verifiesExactDecimalMagnitude() {
        ProcessEffortPerReportingPeriodDerivation derivation = standardDerivation();
        ProcessAnalysisKnowledge knowledge = knowledge("4", "2.4", "9.6", derivation);

        assertThat(verifier.verify(knowledge, derivation)).isTrue();
    }

    @Test
    void returnsFalseForDecimalMismatch() {
        ProcessEffortPerReportingPeriodDerivation derivation = standardDerivation();
        ProcessAnalysisKnowledge knowledge = knowledge("4", "2.4", "9.5", derivation);

        assertThat(verifier.verify(knowledge, derivation)).isFalse();
    }

    @Test
    void comparesBigDecimalMagnitudesNumericallyAcrossScales() {
        ProcessEffortPerReportingPeriodDerivation derivation = standardDerivation();
        ProcessAnalysisKnowledge knowledge = knowledge("4.0", "2.00", "8.000", derivation);

        assertThat(verifier.verify(knowledge, derivation)).isTrue();
    }

    @Test
    void rejectsNullKnowledge() {
        assertThatThrownBy(() -> verifier.verify(null, standardDerivation()))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejectsNullDerivation() {
        assertThatThrownBy(() -> verifier.verify(knowledge("4000", "2", "8000", standardDerivation()), null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejectsDerivationThatIsNotDeclaredByKnowledge() {
        ProcessEffortPerReportingPeriodDerivation declaredDerivation =
                new ProcessEffortPerReportingPeriodDerivation(factId("volume"), factId("effort"), factId("result"));
        ProcessEffortPerReportingPeriodDerivation undeclaredDerivation =
                new ProcessEffortPerReportingPeriodDerivation(
                        factId("other-volume"),
                        factId("other-effort"),
                        factId("other-result"));
        ProcessAnalysisKnowledge knowledge = new ProcessAnalysisKnowledge(
                List.of(
                        sourceQuantityFact("volume", "4000", requestsPerMonth("request")),
                        sourceQuantityFact("effort", "2", minutesPerBusinessItem("request")),
                        derivedQuantityFact(
                                "result",
                                "8000",
                                minutesPerMonth(),
                                List.of(factId("volume"), factId("effort"))),
                        sourceQuantityFact("other-volume", "4", requestsPerMonth("request")),
                        sourceQuantityFact("other-effort", "2", minutesPerBusinessItem("request")),
                        derivedQuantityFact(
                                "other-result",
                                "8",
                                minutesPerMonth(),
                                List.of(factId("other-volume"), factId("other-effort")))),
                List.of(),
                List.of(),
                List.of(),
                List.of(declaredDerivation));

        assertThatThrownBy(() -> verifier.verify(knowledge, undeclaredDerivation))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("deterministic derivation must be declared");
    }

    @Test
    void acceptsSourceStatedOperands() {
        ProcessEffortPerReportingPeriodDerivation derivation = standardDerivation();
        ProcessAnalysisKnowledge knowledge = knowledge(
                List.of(
                        sourceQuantityFact("volume", "4000", requestsPerMonth("request")),
                        sourceQuantityFact("effort", "2", minutesPerBusinessItem("request")),
                        derivedQuantityFact(
                                "result",
                                "8000",
                                minutesPerMonth(),
                                List.of(factId("volume"), factId("effort")))),
                derivation);

        assertThat(verifier.verify(knowledge, derivation)).isTrue();
    }

    private static ProcessAnalysisKnowledge knowledge(
            String volumeMagnitude,
            String effortMagnitude,
            String resultMagnitude,
            ProcessEffortPerReportingPeriodDerivation derivation) {
        return knowledge(
                List.of(
                        sourceQuantityFact("volume", volumeMagnitude, requestsPerMonth("request")),
                        sourceQuantityFact("effort", effortMagnitude, minutesPerBusinessItem("request")),
                        derivedQuantityFact(
                                "result",
                                resultMagnitude,
                                minutesPerMonth(),
                                List.of(factId("volume"), factId("effort")))),
                derivation);
    }

    private static ProcessAnalysisKnowledge knowledge(
            List<ProcessKnownFact> knownFacts,
            ProcessEffortPerReportingPeriodDerivation derivation) {
        return new ProcessAnalysisKnowledge(knownFacts, List.of(), List.of(), List.of(), List.of(derivation));
    }

    private static ProcessEffortPerReportingPeriodDerivation standardDerivation() {
        return new ProcessEffortPerReportingPeriodDerivation(factId("volume"), factId("effort"), factId("result"));
    }

    private static ProcessKnownFact sourceQuantityFact(
            String id,
            String magnitude,
            ProcessQuantityUnitExpression unit) {
        return knownFact(
                id,
                "Source quantity fact " + id,
                ProcessFactGrounding.SOURCE_STATED,
                List.of(),
                List.of(artifactId(id)),
                quantity(magnitude, unit));
    }

    private static ProcessKnownFact derivedQuantityFact(
            String id,
            String magnitude,
            ProcessQuantityUnitExpression unit,
            List<ProcessKnownFactId> premiseFactIds) {
        return knownFact(
                id,
                "Derived quantity fact " + id,
                ProcessFactGrounding.DETERMINISTICALLY_DERIVED,
                premiseFactIds,
                List.of(),
                quantity(magnitude, unit));
    }

    private static ProcessKnownFact knownFact(
            String id,
            String statement,
            ProcessFactGrounding grounding,
            List<ProcessKnownFactId> premiseFactIds,
            List<ProcessEvidenceArtifactId> evidenceArtifactIds,
            ProcessComputableProjection computableProjection) {
        return new ProcessKnownFact(
                factId(id),
                statement,
                grounding,
                ProcessAnalysisScope.processWide(),
                premiseFactIds,
                evidenceArtifactIds,
                Optional.of(computableProjection));
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
