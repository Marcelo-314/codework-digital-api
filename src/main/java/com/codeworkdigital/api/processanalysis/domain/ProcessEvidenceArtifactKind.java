package com.codeworkdigital.api.processanalysis.domain;

/**
 * Classifies the epistemic role an artifact can play when supporting a fact.
 */
public enum ProcessEvidenceArtifactKind {

    /**
     * Material available to the analysis that states or explicitly supports a proposition.
     *
     * This preserves SOURCE_STATED semantics: the proposition is source-supported, not independently verified.
     */
    SOURCE_MATERIAL,

    /**
     * A direct empirical determination such as an observation, inspection, measurement, or measurement set.
     */
    EMPIRICAL_RESULT
}
