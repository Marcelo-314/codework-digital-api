package com.codeworkdigital.api.processanalysis.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProcessOperationGraphTest {

    @Test
    void acceptsValidSimpleLinearGraph() {
        ProcessOperation receive = operation("receive");
        ProcessOperation classify = operation("classify");
        ProcessOperation route = operation("route");

        ProcessOperationGraph graph = new ProcessOperationGraph(
                List.of(receive, classify, route),
                List.of(
                        new ProcessInformationFlow("receive", "classify", "received request details"),
                        new ProcessInformationFlow("classify", "route", "classification result")),
                List.of(
                        new ProcessControlFlow("receive", "classify"),
                        new ProcessControlFlow("classify", "route")));

        assertThat(graph.operations()).containsExactly(receive, classify, route);
        assertThat(graph.informationFlows()).hasSize(2);
        assertThat(graph.controlFlows()).hasSize(2);
    }

    @Test
    void acceptsFanOutBranching() {
        ProcessOperation validate = operation("validate");
        ProcessOperation fulfillDigital = operation("fulfill-digital");
        ProcessOperation fulfillManual = operation("fulfill-manual");

        assertThatCode(() -> new ProcessOperationGraph(
                        List.of(validate, fulfillDigital, fulfillManual),
                        List.of(
                                new ProcessInformationFlow("validate", "fulfill-digital", "validated order"),
                                new ProcessInformationFlow("validate", "fulfill-manual", "validated order")),
                        List.of(
                                new ProcessControlFlow("validate", "fulfill-digital", "digital path"),
                                new ProcessControlFlow("validate", "fulfill-manual", "manual path"))))
                .doesNotThrowAnyException();
    }

    @Test
    void acceptsCycleAndReentry() {
        ProcessOperation validate = operation("validate");
        ProcessOperation review = operation("review");
        ProcessOperation revise = operation("revise");

        assertThatCode(() -> new ProcessOperationGraph(
                        List.of(validate, review, revise),
                        List.of(
                                new ProcessInformationFlow("validate", "review", "validation result"),
                                new ProcessInformationFlow("review", "revise", "review notes"),
                                new ProcessInformationFlow("revise", "validate", "revised submission")),
                        List.of(
                                new ProcessControlFlow("validate", "review"),
                                new ProcessControlFlow("review", "revise", "needs revision"),
                                new ProcessControlFlow("revise", "validate"))))
                .doesNotThrowAnyException();
    }

    @Test
    void acceptsSingleOperationGraphWithoutEdges() {
        ProcessOperation fulfill = operation("fulfill");

        ProcessOperationGraph graph = new ProcessOperationGraph(List.of(fulfill), List.of(), List.of());

        assertThat(graph.operations()).containsExactly(fulfill);
        assertThat(graph.informationFlows()).isEmpty();
        assertThat(graph.controlFlows()).isEmpty();
    }

    @Test
    void rejectsDuplicateOperationIds() {
        ProcessOperation first = new ProcessOperation("validate", "Validate", "Validate request");
        ProcessOperation duplicate = new ProcessOperation("validate", "Validate again", "Repeat validation");

        assertThatThrownBy(() -> new ProcessOperationGraph(List.of(first, duplicate), List.of(), List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("operation IDs must be unique");
    }

    @Test
    void rejectsInformationFlowReferencingUnknownOperation() {
        ProcessOperation receive = operation("receive");

        assertThatThrownBy(() -> new ProcessOperationGraph(
                        List.of(receive),
                        List.of(new ProcessInformationFlow("receive", "classify", "request details")),
                        List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("information flow targetOperationId references an unknown operation");
    }

    @Test
    void rejectsControlFlowReferencingUnknownOperation() {
        ProcessOperation receive = operation("receive");

        assertThatThrownBy(() -> new ProcessOperationGraph(
                        List.of(receive),
                        List.of(),
                        List.of(new ProcessControlFlow("receive", "route"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("control flow targetOperationId references an unknown operation");
    }

    @Test
    void rejectsNullCollectionMembers() {
        ProcessOperation receive = operation("receive");

        assertThatThrownBy(() -> new ProcessOperationGraph(
                        listWithNull(receive),
                        List.of(),
                        List.of()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ProcessOperationGraph(
                        List.of(receive),
                        listWithNull(new ProcessInformationFlow("receive", "receive", "request details")),
                        List.of()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ProcessOperationGraph(
                        List.of(receive),
                        List.of(),
                        listWithNull(new ProcessControlFlow("receive", "receive"))))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void defensivelyCopiesCollections() {
        ProcessOperation receive = operation("receive");
        ProcessOperation classify = operation("classify");
        List<ProcessOperation> operations = new ArrayList<>(List.of(receive, classify));
        List<ProcessInformationFlow> informationFlows = new ArrayList<>(
                List.of(new ProcessInformationFlow("receive", "classify", "request details")));
        List<ProcessControlFlow> controlFlows = new ArrayList<>(List.of(new ProcessControlFlow("receive", "classify")));

        ProcessOperationGraph graph = new ProcessOperationGraph(operations, informationFlows, controlFlows);

        operations.clear();
        informationFlows.clear();
        controlFlows.clear();

        assertThat(graph.operations()).containsExactly(receive, classify);
        assertThat(graph.informationFlows()).hasSize(1);
        assertThat(graph.controlFlows()).hasSize(1);
        assertThatThrownBy(() -> graph.operations().add(operation("route")))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    private static ProcessOperation operation(String id) {
        return new ProcessOperation(id, id, id + " operation");
    }

    private static <T> List<T> listWithNull(T item) {
        List<T> items = new ArrayList<>();
        items.add(item);
        items.add(null);
        return items;
    }
}
