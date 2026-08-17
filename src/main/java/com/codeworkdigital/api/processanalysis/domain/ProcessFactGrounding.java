package com.codeworkdigital.api.processanalysis.domain;

/**
 * Describes why a proposition is admissible as an established fact in the process-analysis knowledge model.
 */
public enum ProcessFactGrounding {

    /**
     * Explicitly supported by source material available to the analysis.
     *
     * This does not assert independent verification or objective truth; it only means the available source material
     * states or explicitly supports the proposition.
     */
    SOURCE_STATED,

    /**
     * Established through observation, inspection, measurement, or another direct empirical determination.
     */
    EMPIRICALLY_ESTABLISHED,

    /**
     * Derived deterministically from already established information, without probabilistic, heuristic, or
     * semantically uncertain inference.
     */
    DETERMINISTICALLY_DERIVED
}
