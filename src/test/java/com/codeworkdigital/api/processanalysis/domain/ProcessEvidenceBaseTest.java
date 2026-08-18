package com.codeworkdigital.api.processanalysis.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProcessEvidenceBaseTest {

    @Test
    void acceptsEmptyEvidenceBase() {
        ProcessEvidenceBase evidenceBase = new ProcessEvidenceBase(List.of());

        assertThat(evidenceBase.artifacts()).isEmpty();
    }

    @Test
    void acceptsMultipleArtifactsWithDifferentIds() {
        ProcessEvidenceArtifact source = artifact(
                "source-1",
                ProcessEvidenceArtifactKind.SOURCE_MATERIAL,
                "Source material stating the review rule");
        ProcessEvidenceArtifact measurement = artifact(
                "measurement-1",
                ProcessEvidenceArtifactKind.EMPIRICAL_RESULT,
                "Measured operation duration sample");

        ProcessEvidenceBase evidenceBase = new ProcessEvidenceBase(List.of(source, measurement));

        assertThat(evidenceBase.artifacts()).containsExactly(source, measurement);
    }

    @Test
    void defensivelyCopiesAndReturnsImmutableCollection() {
        ProcessEvidenceArtifact source = artifact(
                "source-1",
                ProcessEvidenceArtifactKind.SOURCE_MATERIAL,
                "Source material stating the review rule");
        List<ProcessEvidenceArtifact> artifacts = new ArrayList<>(List.of(source));

        ProcessEvidenceBase evidenceBase = new ProcessEvidenceBase(artifacts);

        artifacts.clear();

        assertThat(evidenceBase.artifacts()).containsExactly(source);
        assertThatThrownBy(() -> evidenceBase.artifacts().add(artifact(
                        "source-2",
                        ProcessEvidenceArtifactKind.SOURCE_MATERIAL,
                        "Other source material")))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void rejectsDuplicateArtifactIdEvenWhenKindOrDescriptionDiffer() {
        ProcessEvidenceArtifact source = artifact(
                "artifact-1",
                ProcessEvidenceArtifactKind.SOURCE_MATERIAL,
                "Source material stating the rule");
        ProcessEvidenceArtifact empirical = artifact(
                "artifact-1",
                ProcessEvidenceArtifactKind.EMPIRICAL_RESULT,
                "Measured operation duration sample");

        assertThatThrownBy(() -> new ProcessEvidenceBase(List.of(source, empirical)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("evidence artifact id must be unique: artifact-1");
    }

    @Test
    void rejectsDuplicateArtifactIdEvenWhenArtifactsAreStructurallyIdentical() {
        ProcessEvidenceArtifact source = artifact(
                "artifact-1",
                ProcessEvidenceArtifactKind.SOURCE_MATERIAL,
                "Source material stating the rule");

        assertThatThrownBy(() -> new ProcessEvidenceBase(List.of(source, source)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("evidence artifact id must be unique: artifact-1");
    }

    @Test
    void rejectsNullValues() {
        assertThatThrownBy(() -> new ProcessEvidenceBase(null))
                .isInstanceOf(NullPointerException.class);

        List<ProcessEvidenceArtifact> artifacts = new ArrayList<>();
        artifacts.add(artifact(
                "source-1",
                ProcessEvidenceArtifactKind.SOURCE_MATERIAL,
                "Source material stating the review rule"));
        artifacts.add(null);

        assertThatThrownBy(() -> new ProcessEvidenceBase(artifacts))
                .isInstanceOf(NullPointerException.class);
    }

    private static ProcessEvidenceArtifact artifact(
            String id,
            ProcessEvidenceArtifactKind kind,
            String description) {
        return new ProcessEvidenceArtifact(new ProcessEvidenceArtifactId(id), kind, description);
    }
}
