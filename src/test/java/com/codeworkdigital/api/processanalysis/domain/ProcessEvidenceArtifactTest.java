package com.codeworkdigital.api.processanalysis.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ProcessEvidenceArtifactTest {

    @Test
    void createsSourceMaterialArtifact() {
        ProcessEvidenceArtifact artifact = new ProcessEvidenceArtifact(
                new ProcessEvidenceArtifactId("source-1"),
                ProcessEvidenceArtifactKind.SOURCE_MATERIAL,
                "Material that states each order requires manual review");

        assertThat(artifact.id()).isEqualTo(new ProcessEvidenceArtifactId("source-1"));
        assertThat(artifact.kind()).isEqualTo(ProcessEvidenceArtifactKind.SOURCE_MATERIAL);
        assertThat(artifact.description()).isEqualTo("Material that states each order requires manual review");
    }

    @Test
    void createsEmpiricalResultArtifact() {
        ProcessEvidenceArtifact artifact = new ProcessEvidenceArtifact(
                new ProcessEvidenceArtifactId("measurement-1"),
                ProcessEvidenceArtifactKind.EMPIRICAL_RESULT,
                "Observed operation duration sample with a 2.4 minute mean");

        assertThat(artifact.kind()).isEqualTo(ProcessEvidenceArtifactKind.EMPIRICAL_RESULT);
    }

    @Test
    void artifactIdentityIsStableDomainReference() {
        ProcessEvidenceArtifactId id = new ProcessEvidenceArtifactId("artifact-1");

        assertThat(id).isEqualTo(new ProcessEvidenceArtifactId("artifact-1"));
        assertThat(id).isNotEqualTo(new ProcessEvidenceArtifactId("artifact-2"));
    }

    @Test
    void distinguishesSourceMaterialFromEmpiricalResultWithinTheSameAbstraction() {
        ProcessEvidenceArtifactId sourceId = new ProcessEvidenceArtifactId("source-1");
        ProcessEvidenceArtifactId empiricalId = new ProcessEvidenceArtifactId("empirical-1");

        ProcessEvidenceArtifact source = new ProcessEvidenceArtifact(
                sourceId,
                ProcessEvidenceArtifactKind.SOURCE_MATERIAL,
                "Source material stating the rule");
        ProcessEvidenceArtifact empirical = new ProcessEvidenceArtifact(
                empiricalId,
                ProcessEvidenceArtifactKind.EMPIRICAL_RESULT,
                "Observed measurements establishing average handling time");

        assertThat(source.kind()).isEqualTo(ProcessEvidenceArtifactKind.SOURCE_MATERIAL);
        assertThat(empirical.kind()).isEqualTo(ProcessEvidenceArtifactKind.EMPIRICAL_RESULT);
        assertThat(source).isNotEqualTo(empirical);
    }

    @Test
    void rejectsNullOrBlankRequiredValues() {
        ProcessEvidenceArtifactId id = new ProcessEvidenceArtifactId("artifact-1");

        assertThatThrownBy(() -> new ProcessEvidenceArtifactId(null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ProcessEvidenceArtifactId(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("value must not be blank");
        assertThatThrownBy(() -> new ProcessEvidenceArtifact(null, ProcessEvidenceArtifactKind.SOURCE_MATERIAL, "source"))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ProcessEvidenceArtifact(id, null, "source"))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ProcessEvidenceArtifact(id, ProcessEvidenceArtifactKind.SOURCE_MATERIAL, null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ProcessEvidenceArtifact(id, ProcessEvidenceArtifactKind.SOURCE_MATERIAL, " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("description must not be blank");
    }
}
