package com.codeworkdigital.api.processanalysis.domain;

/**
 * Quantity-unit form for the P06 process-analysis intervention-value case.
 *
 * This is not a generic expression AST, dimensional model, or unit algebra system.
 */
public sealed interface ProcessQuantityUnitExpression permits
        ProcessBusinessItemPerReportingPeriodUnit,
        ProcessEffortPerBusinessItemUnit,
        ProcessEffortPerReportingPeriodUnit {
}
