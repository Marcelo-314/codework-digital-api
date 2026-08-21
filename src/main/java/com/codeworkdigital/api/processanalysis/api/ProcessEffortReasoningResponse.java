package com.codeworkdigital.api.processanalysis.api;

import com.codeworkdigital.api.processanalysis.application.ProcessEffortReasoningProjector;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

public record ProcessEffortReasoningResponse(
        List<EstablishedInput> establishedInputs,
        Calculation calculation,
        Decision decision) {

    public ProcessEffortReasoningResponse {
        establishedInputs = List.copyOf(establishedInputs);
    }

    static ProcessEffortReasoningResponse from(ProcessEffortReasoningProjector.Reasoning reasoning) {
        Objects.requireNonNull(reasoning, "reasoning");
        return new ProcessEffortReasoningResponse(
                reasoning.establishedInputs().stream()
                        .map(EstablishedInput::from)
                        .toList(),
                reasoning.calculation() == null ? null : Calculation.from(reasoning.calculation()),
                reasoning.decision() == null ? null : Decision.from(reasoning.decision()));
    }

    public record EstablishedInput(
            InputCode code,
            BigDecimal magnitude,
            QuantityUnit unit,
            InputSource source) {

        static EstablishedInput from(ProcessEffortReasoningProjector.EstablishedInput input) {
            return new EstablishedInput(
                    InputCode.valueOf(input.code().name()),
                    input.quantity().magnitude(),
                    QuantityUnit.valueOf(input.quantity().unit().name()),
                    InputSource.valueOf(input.source().name()));
        }
    }

    public record Calculation(
            Operation operation,
            Quantity result) {

        static Calculation from(ProcessEffortReasoningProjector.Calculation calculation) {
            return new Calculation(
                    Operation.valueOf(calculation.operation().name()),
                    Quantity.from(calculation.result()));
        }
    }

    public record Decision(
            String criterion,
            Quantity threshold,
            Comparison comparison,
            ProcessEffortMaterialityOutcomeResponse outcome) {

        static Decision from(ProcessEffortReasoningProjector.Decision decision) {
            return new Decision(
                    decision.criterion(),
                    Quantity.from(decision.threshold()),
                    Comparison.valueOf(decision.comparison().name()),
                    ProcessEffortMaterialityOutcomeResponse.valueOf(decision.outcome().name()));
        }
    }

    public record Quantity(
            BigDecimal magnitude,
            QuantityUnit unit) {

        static Quantity from(ProcessEffortReasoningProjector.Quantity quantity) {
            return new Quantity(
                    quantity.magnitude(),
                    QuantityUnit.valueOf(quantity.unit().name()));
        }
    }

    public enum InputCode {
        VOLUME_PER_REPORTING_PERIOD,
        EFFORT_PER_BUSINESS_ITEM
    }

    public enum InputSource {
        PROCESS_DESCRIPTION,
        CLARIFICATION_ANSWER
    }

    public enum QuantityUnit {
        BUSINESS_ITEM_PER_MONTH,
        MINUTE_PER_BUSINESS_ITEM,
        MINUTE_PER_MONTH
    }

    public enum Operation {
        MULTIPLY
    }

    public enum Comparison {
        BELOW_THRESHOLD,
        AT_OR_ABOVE_THRESHOLD
    }
}
