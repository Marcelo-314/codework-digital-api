package com.codeworkdigital.api.processanalysis.domain;

/**
 * Machine-usable projection of a {@link ProcessKnownFact} needed by supported deterministic analysis.
 *
 * This is not the whole proposition, generic application data, arbitrary JSON, an expression, or a deterministic
 * relation.
 */
public sealed interface ProcessComputableProjection permits ProcessCategoryProjection {
}
