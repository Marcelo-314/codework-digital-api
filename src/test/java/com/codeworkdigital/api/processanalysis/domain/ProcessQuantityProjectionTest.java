package com.codeworkdigital.api.processanalysis.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ProcessQuantityProjectionTest {

    @Test
    void businessItemUnitIdAcceptsValidId() {
        ProcessBusinessItemUnitId id = new ProcessBusinessItemUnitId("request");

        assertThat(id.value()).isEqualTo("request");
    }

    @Test
    void businessItemUnitIdRejectsNullValue() {
        assertThatThrownBy(() -> new ProcessBusinessItemUnitId(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void businessItemUnitIdRejectsBlankValue() {
        assertThatThrownBy(() -> new ProcessBusinessItemUnitId(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("value must not be blank");
    }

    @Test
    void sameBusinessItemUnitIdsAreEqual() {
        assertThat(new ProcessBusinessItemUnitId("request"))
                .isEqualTo(new ProcessBusinessItemUnitId("request"));
    }

    @Test
    void distinctBusinessItemUnitIdsAreUnequal() {
        assertThat(new ProcessBusinessItemUnitId("request"))
                .isNotEqualTo(new ProcessBusinessItemUnitId("invoice"));
    }

    @Test
    void businessItemPerReportingPeriodUnitAcceptsValidComponents() {
        ProcessBusinessItemPerReportingPeriodUnit unit =
                new ProcessBusinessItemPerReportingPeriodUnit(request(), ProcessReportingPeriodUnit.MONTH);

        assertThat(unit.businessItemId()).isEqualTo(request());
        assertThat(unit.reportingPeriod()).isEqualTo(ProcessReportingPeriodUnit.MONTH);
    }

    @Test
    void businessItemPerReportingPeriodUnitRejectsNullComponents() {
        assertThatThrownBy(() -> new ProcessBusinessItemPerReportingPeriodUnit(null, ProcessReportingPeriodUnit.MONTH))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ProcessBusinessItemPerReportingPeriodUnit(request(), null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void effortPerBusinessItemUnitAcceptsValidComponents() {
        ProcessEffortPerBusinessItemUnit unit =
                new ProcessEffortPerBusinessItemUnit(ProcessEffortDurationUnit.MINUTE, request());

        assertThat(unit.effortDuration()).isEqualTo(ProcessEffortDurationUnit.MINUTE);
        assertThat(unit.businessItemId()).isEqualTo(request());
    }

    @Test
    void effortPerBusinessItemUnitRejectsNullComponents() {
        assertThatThrownBy(() -> new ProcessEffortPerBusinessItemUnit(null, request()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ProcessEffortPerBusinessItemUnit(ProcessEffortDurationUnit.MINUTE, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void effortPerReportingPeriodUnitAcceptsValidComponents() {
        ProcessEffortPerReportingPeriodUnit unit =
                new ProcessEffortPerReportingPeriodUnit(
                        ProcessEffortDurationUnit.MINUTE,
                        ProcessReportingPeriodUnit.MONTH);

        assertThat(unit.effortDuration()).isEqualTo(ProcessEffortDurationUnit.MINUTE);
        assertThat(unit.reportingPeriod()).isEqualTo(ProcessReportingPeriodUnit.MONTH);
    }

    @Test
    void effortPerReportingPeriodUnitRejectsNullComponents() {
        assertThatThrownBy(() -> new ProcessEffortPerReportingPeriodUnit(null, ProcessReportingPeriodUnit.MONTH))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ProcessEffortPerReportingPeriodUnit(ProcessEffortDurationUnit.MINUTE, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void requestBasedExpressionDoesNotEqualInvoiceBasedEquivalent() {
        assertThat(new ProcessBusinessItemPerReportingPeriodUnit(request(), ProcessReportingPeriodUnit.MONTH))
                .isNotEqualTo(new ProcessBusinessItemPerReportingPeriodUnit(invoice(), ProcessReportingPeriodUnit.MONTH));
        assertThat(new ProcessEffortPerBusinessItemUnit(ProcessEffortDurationUnit.MINUTE, request()))
                .isNotEqualTo(new ProcessEffortPerBusinessItemUnit(ProcessEffortDurationUnit.MINUTE, invoice()));
    }

    @Test
    void structurallyIdenticalUnitExpressionsAreEqual() {
        assertThat(new ProcessBusinessItemPerReportingPeriodUnit(request(), ProcessReportingPeriodUnit.MONTH))
                .isEqualTo(new ProcessBusinessItemPerReportingPeriodUnit(request(), ProcessReportingPeriodUnit.MONTH));
        assertThat(new ProcessEffortPerBusinessItemUnit(ProcessEffortDurationUnit.MINUTE, request()))
                .isEqualTo(new ProcessEffortPerBusinessItemUnit(ProcessEffortDurationUnit.MINUTE, request()));
        assertThat(new ProcessEffortPerReportingPeriodUnit(
                        ProcessEffortDurationUnit.MINUTE,
                        ProcessReportingPeriodUnit.MONTH))
                .isEqualTo(new ProcessEffortPerReportingPeriodUnit(
                        ProcessEffortDurationUnit.MINUTE,
                        ProcessReportingPeriodUnit.MONTH));
    }

    @Test
    void quantityProjectionAcceptsValidPositiveMagnitude() {
        ProcessQuantityProjection projection = quantity("4000", requestsPerMonth());

        assertThat(projection.magnitude()).isEqualByComparingTo("4000");
        assertThat(projection.unit()).isEqualTo(requestsPerMonth());
    }

    @Test
    void quantityProjectionAcceptsZero() {
        ProcessQuantityProjection projection = quantity("0", requestsPerMonth());

        assertThat(projection.magnitude()).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    void quantityProjectionRejectsNegativeMagnitude() {
        assertThatThrownBy(() -> quantity("-1", requestsPerMonth()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("magnitude must be greater than or equal to zero");
    }

    @Test
    void quantityProjectionRejectsNullMagnitude() {
        assertThatThrownBy(() -> new ProcessQuantityProjection(null, requestsPerMonth()))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void quantityProjectionRejectsNullUnit() {
        assertThatThrownBy(() -> new ProcessQuantityProjection(new BigDecimal("1"), null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void decimalScaleIsCanonicalizedForStructuralEquality() {
        assertThat(quantity("2", minutesPerRequest()))
                .isEqualTo(quantity("2.0", minutesPerRequest()))
                .isEqualTo(quantity("2.00", minutesPerRequest()));
    }

    @Test
    void zeroScaleIsCanonicalizedForStructuralEquality() {
        assertThat(quantity("0", requestsPerMonth()))
                .isEqualTo(quantity("0.0", requestsPerMonth()))
                .isEqualTo(quantity("0.00", requestsPerMonth()));
    }

    @Test
    void distinctMagnitudesRemainUnequal() {
        assertThat(quantity("2", minutesPerRequest()))
                .isNotEqualTo(quantity("3", minutesPerRequest()));
    }

    @Test
    void sameMagnitudeWithDistinctUnitsRemainsUnequal() {
        assertThat(quantity("2", minutesPerRequest()))
                .isNotEqualTo(quantity("2", requestsPerMonth()));
    }

    @Test
    void representsP06QuantitiesWithoutMultiplyingThem() {
        ProcessQuantityProjection monthlyVolume = quantity("4000", requestsPerMonth());
        ProcessQuantityProjection manualEffort = quantity("2", minutesPerRequest());
        ProcessQuantityProjection monthlyEffort = quantity("8000", minutesPerMonth());

        assertThat(monthlyVolume.unit()).isInstanceOfSatisfying(
                ProcessBusinessItemPerReportingPeriodUnit.class,
                unit -> {
                    assertThat(unit.businessItemId()).isEqualTo(request());
                    assertThat(unit.reportingPeriod()).isEqualTo(ProcessReportingPeriodUnit.MONTH);
                });
        assertThat(manualEffort.unit()).isInstanceOfSatisfying(
                ProcessEffortPerBusinessItemUnit.class,
                unit -> {
                    assertThat(unit.effortDuration()).isEqualTo(ProcessEffortDurationUnit.MINUTE);
                    assertThat(unit.businessItemId()).isEqualTo(request());
                });
        assertThat(monthlyEffort.unit()).isInstanceOfSatisfying(
                ProcessEffortPerReportingPeriodUnit.class,
                unit -> {
                    assertThat(unit.effortDuration()).isEqualTo(ProcessEffortDurationUnit.MINUTE);
                    assertThat(unit.reportingPeriod()).isEqualTo(ProcessReportingPeriodUnit.MONTH);
                });
    }

    private static ProcessQuantityProjection quantity(String magnitude, ProcessQuantityUnitExpression unit) {
        return new ProcessQuantityProjection(new BigDecimal(magnitude), unit);
    }

    private static ProcessBusinessItemPerReportingPeriodUnit requestsPerMonth() {
        return new ProcessBusinessItemPerReportingPeriodUnit(request(), ProcessReportingPeriodUnit.MONTH);
    }

    private static ProcessEffortPerBusinessItemUnit minutesPerRequest() {
        return new ProcessEffortPerBusinessItemUnit(ProcessEffortDurationUnit.MINUTE, request());
    }

    private static ProcessEffortPerReportingPeriodUnit minutesPerMonth() {
        return new ProcessEffortPerReportingPeriodUnit(
                ProcessEffortDurationUnit.MINUTE,
                ProcessReportingPeriodUnit.MONTH);
    }

    private static ProcessBusinessItemUnitId request() {
        return new ProcessBusinessItemUnitId("request");
    }

    private static ProcessBusinessItemUnitId invoice() {
        return new ProcessBusinessItemUnitId("invoice");
    }
}
