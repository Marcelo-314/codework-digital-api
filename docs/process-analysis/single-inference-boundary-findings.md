# P0.4 - Single-Inference Boundary Findings

This document records the bounded Process Analysis Lab findings from P0.3.1 and P0.3.2. It closes the current single-inference investigation and establishes architectural constraints for the next foundation work.

It is not an implementation plan for multi-inference, candidate evidence, semantic hypotheses, new APIs, prompt changes, or production behavior.

## Current System Boundary

The current Process Analysis Lab pipeline is:

```text
natural-language process description
        -> single LLM structured inference
        -> typed ProcessEffortEvidence
        -> deterministic validation
        -> established knowledge / evidence gaps
        -> human clarification where currently supported
        -> deterministic derivation and policy
        -> public reasoning from backend authoritative results
```

The current implementation has important strengths that must be preserved:

- deterministic calculations are performed by backend domain code, not by the LLM;
- explicit provenance distinguishes source-stated values from clarification answers;
- exact facts are distinguished from non-exact evidence such as APPROXIMATE, RANGE, ABSENT, and UNSUPPORTED_UNIT;
- malformed or non-admissible evidence fails closed before deterministic derivation;
- human clarification can establish new source-stated values for currently supported quantitative gaps;
- the frontend does not recalculate P06 burden or policy outcomes;
- public reasoning is derived from backend authoritative results.

The current implementation is not a failure. It provided the controlled experimental boundary that made the next architecture questions visible.

## Observed Evidence

### P0.3.1 Baseline

P0.3.1 ran 10 live executions of the same canonical Spanish input:

```text
Recibimos solicitudes internas de compra a través de un formulario en la intranet. El formulario tiene campos obligatorios: centro de costo, categoría (elegida de una lista de tres opciones), monto y descripción. Cuando alguien la envía, el sistema la deriva automáticamente al responsable de esa categoría, que aprueba o rechaza desde la misma herramienta. Llegan unas cuatro o cinco por mes. El circuito tarda menos de un día y no hemos tenido reclamos ni errores de derivación.
```

The preserved diagnostic observed:

- 8/10 canonical P06 signatures:
  - volume = RANGE, minMagnitude = 4, maxMagnitude = 5, reportingPeriod = MONTH;
  - effort = ABSENT;
  - same business-item identity;
  - gaps emitted: VOLUME_PER_REPORTING_PERIOD and EFFORT_PER_BUSINESS_ITEM.
- 1/10 malformed semantic signature:
  - volume.status = APPROXIMATE;
  - magnitude = null;
  - minMagnitude = 4;
  - maxMagnitude = 5;
  - effort = ABSENT;
  - gaps = none.
- 1/10 semantically incorrect effort signature:
  - volume = valid RANGE, minMagnitude = 4, maxMagnitude = 5, reportingPeriod = MONTH;
  - effort = UNSUPPORTED_UNIT where the source only stated elapsed process time;
  - gaps = none.

The diagnostic also observed sameNonblankBusinessItemRef = true in all preserved signatures. Business-item identity did not explain the observed failures in that sample. The preserved diagnostic had 0 transport/model failures.

These counts are stability observations under one experiment. 8/10 does not mean an 80% probability of correctness.

### P0.3.2 Prompt Contract Hardening

P0.3.2 changed only the semantic prompt contract:

- explicit bounded or two-value expressions are RANGE;
- explicit range semantics take precedence over approximation wording;
- APPROXIMATE is a single approximate scalar;
- manual effort is distinct from elapsed, cycle, lead, turnaround, waiting, and SLA time;
- elapsed process duration alone does not establish manual effort.

The first post-change diagnostic produced 10/10 canonical P06 signatures.

A later traceability microfix changed the prompt version only:

```text
process-analysis-understanding-v4
        -> process-analysis-understanding-v5
```

The final prompt version is process-analysis-understanding-v5. The final v5 canonical diagnostic observed:

- 10 executions;
- 10 successful calls;
- 0 transport/model failures;
- 1 distinct P06 signature;
- 10/10 canonical gap emission.

The canonical signature was:

```text
volume:
  status = RANGE
  magnitude = null
  minMagnitude = 4
  maxMagnitude = 5
  reportingPeriod = MONTH

effort:
  status = ABSENT

sameNonblankBusinessItemRef = true

gaps:
  VOLUME_PER_REPORTING_PERIOD
  EFFORT_PER_BUSINESS_ITEM
```

10/10 does not prove universal determinism. It is a bounded empirical stability observation for the canonical case and final v5 prompt.

The final v5 diagnostic also observed approximately:

- min latency: 4917 ms;
- p50 latency: 5578 ms;
- max latency: 6810 ms.

These are experimental values for that preserved remote inference path, not universal service latency.

## What Structured Output Guarantees

Structured Output can guarantee schema-level properties such as:

- required field presence;
- allowed enum values;
- primitive and container types;
- shape constraints encoded in the JSON schema.

Structured Output does not by itself establish:

- correct semantic role;
- correct relation between a value and a domain concept;
- cross-field epistemic consistency unless it is encoded and enforceable;
- truth of extracted propositions;
- stability across independent stochastic executions.

The P0.3.1 example:

```text
status = APPROXIMATE
magnitude = null
minMagnitude = 4
maxMagnitude = 5
```

was compatible with the model-boundary schema, but inconsistent with the deterministic P06 domain contract for APPROXIMATE and RANGE.

## Architectural Decisions

### LLM Outputs Are Hypotheses

LLM outputs are semantic hypotheses, not established facts.

The Process Analysis architecture must evolve conceptually from:

```text
description -> one model interpretation -> executable evidence
```

toward:

```text
description
        -> candidate evidence
        -> one or more semantic hypotheses
        -> admissibility validation
        -> admissible hypothesis space
        -> robust reasoning / targeted clarification
```

Using notation:

```text
x -> {H1, H2, ..., Hn} -> A
```

where Hi is a semantic hypothesis and A is the set of admissible hypotheses after deterministic validation.

This is a conceptual architecture decision. It does not define Java records, persistence tables, APIs, or model-calling strategy.

### Epistemic Separation

The future architecture must preserve this conceptual separation:

```text
ExtractedCandidate != SemanticHypothesis != EstablishedKnowledge
```

ExtractedCandidate is a source-grounded piece of potentially relevant structure detected in the user description, without necessarily assigning its final business semantic role.

Example:

```text
source:
"menos de un día"

candidate meaning:
temporal expression
LESS_THAN
1
DAY
```

This is not automatically manual effort.

SemanticHypothesis is one coherent semantic interpretation assigning candidate evidence to domain meaning and relations. For the same temporal expression, alternatives may include PROCESS_ELAPSED_TIME or, only where justified by source wording, MANUAL_EFFORT_PER_BUSINESS_ITEM.

EstablishedKnowledge is knowledge the system may treat as authoritative for deterministic reasoning according to explicit provenance, validation, or human-establishment rules.

### Finite Backend Domain

The backend must not attempt to encode a rule for every possible natural-language formulation. Natural language is open-ended.

The deterministic backend should operate over the finite canonical semantic domain required by the analytical capabilities currently implemented:

```text
open linguistic space
        -> semantic interpretation
        -> finite canonical domain
        -> deterministic reasoning
```

This is not a proposal for a global enterprise ontology or generic workflow engine. Domain vocabulary should grow only when an analytical capability requires it.

### Multiple Inferences

The previous "exactly one model inference" constraint is no longer a permanent architectural invariant. It was valuable as a bounded experimental constraint.

The future architecture may use multiple bounded semantic inferences.

This does not authorize:

- unbounded agent loops;
- recursive self-chat;
- majority-vote truth;
- retries until a desired result appears;
- uncontrolled cost or latency growth.

The sampling strategy is not designed in P0.4. It belongs to P0.6.

### Frequency Is Not Probability

If an interpretation appears k times in n samples, k/n is an observed stability frequency under that experiment.

It must not automatically be represented as:

- confidence;
- probability of truth;
- calibrated posterior probability.

Frequency may inform stability diagnostics, additional-sampling decisions, and disagreement detection. It does not establish truth by itself.

## Concepts For The Next Foundation

### Analytical Equivalence

Two hypotheses may differ descriptively yet be equivalent for a specific analytical capability.

Notation:

```text
Hi ~P06 Hj
```

when both hypotheses induce the same P06-relevant consequences.

Example:

```text
Receive -> Route -> Decide
```

and:

```text
Receive -> Classify -> Route -> Decide
```

may be different process decompositions. If the difference does not change P06 evidence or reasoning, it may be irrelevant for P06.

The future architecture should not require global textual or structural identity between hypotheses. Full equivalence rules belong to P0.7.

### Material Disagreement

Not every semantic disagreement requires clarification.

The future system must distinguish descriptive variation from analytically material disagreement.

A disagreement is material for an analytical capability when alternative admissible interpretations can change that capability's conclusion or required inputs.

This concept will later drive active clarification. It is not implemented here.

### Robust Reasoning

For an analytical function f and admissible hypothesis space A:

```text
if all admissible hypotheses produce the same relevant result:

f(A) = {r}
```

then r is robust with respect to the remaining admissible semantic uncertainty.

The system does not necessarily need to identify one uniquely true Hi before concluding r.

If:

```text
|f(A)| > 1
```

then the unresolved uncertainty is analytically material and more information may be required.

This is a conceptual target, not authorization to change P06 semantics in P0.4.

### Active Clarification

Current clarification primarily handles missing or non-exact quantitative values.

Future clarification may include:

- factual clarification;
- semantic clarification;
- subject or identity clarification;
- process-boundary clarification.

Example semantic clarification:

```text
When you say the circuit takes less than one day, do you mean total elapsed time or active human work dedicated to each request?
```

The future goal is not to ask about every uncertainty. It is to ask about uncertainty that is material to the current analytical conclusion.

Implementation belongs to a later increment.

## Candidate Producers And Preprocessing

Candidate extraction may eventually have multiple producers. Conceptually possible producers include:

- deterministic browser preprocessing;
- optional small browser-local model;
- deterministic backend preprocessing;
- remote semantic model;
- explicit user input.

This document does not commit to implementing all of them.

CandidateEvidence must eventually be producer-independent in semantic meaning while preserving provenance.

A browser-local model, if explored later, is:

- a preprocessing or candidate producer;
- not an authoritative analyst;
- not a source of established facts merely because it ran locally.

Because browser state is user-controlled, frontend-produced candidates must remain untrusted input to backend validation.

The original description should remain available to the backend together with preprocessed candidates. P0.4 does not define the API shape.

## Source Grounding

Future candidate evidence should remain grounded in the original source text whenever feasible.

Examples of future-checkable properties:

- a source span exists in the original description;
- numeric values claimed as extracted are derivable from the grounded span;
- preprocessing does not silently introduce unsupported factual values.

P0.4 does not implement source-span machinery. This is a design constraint for P0.5.

## Latency And Cost Constraints

The final P0.3.2 v5 live diagnostic observed approximately 4917 ms minimum latency, 5578 ms p50 latency, and 6810 ms maximum latency for the current remote inference path.

Future multi-inference design must consider:

- bounded sample size;
- possible controlled concurrency;
- adaptive sampling or early stopping;
- API and model cost;
- request latency;
- failure handling.

Sequential 10-call execution must not be treated as the target product design merely because it was useful for diagnostics.

## What P0.3.x Remains Valid

The next architecture must not casually discard the domain guarantees already produced by P0.3.x:

- ProcessEffortEvidence semantics already proven useful;
- exact vs approximate/range distinction;
- source-stated vs clarification provenance;
- deterministic burden derivation;
- canonical threshold policy;
- structured clarification lifecycle;
- public reasoning;
- fail-closed validation;
- one-shot continuation semantics.

Some representations may later move behind a new semantic boundary, but these guarantees remain assets. P0.3.x does not need to be rewritten as a conclusion of P0.4.

## Non-Goals And Anti-Patterns

P0.4 explicitly rejects:

1. giant prompt accumulation as the primary scalability strategy;
2. backend if/else rules for arbitrary language formulations;
3. majority vote = truth;
4. sample frequency = calibrated probability;
5. unbounded autonomous agents;
6. automatic midpoint/min/max selection as factual truth;
7. browser-produced evidence trusted as authoritative;
8. global ontology construction before analytical need exists;
9. replacing deterministic calculations with LLM arithmetic;
10. prematurely implementing Scenario Model on the unstable semantic boundary.

## Open Questions For P0.5+

P0.5:

- What is the minimum CandidateEvidence representation?
- What information must remain source-grounded?
- What provenance model is sufficient?
- How is CandidateEvidence distinct from current ProcessEffortEvidence?
- What constitutes a SemanticHypothesis?
- Does one hypothesis cover the complete process or capability-specific evidence?
- Which concepts belong in the first finite semantic vocabulary?

P0.6:

- How many inferences are initially sampled?
- Can samples run concurrently?
- What are safe cost and latency ceilings?
- What constitutes an early-stop condition?

P0.7:

- What makes a hypothesis admissible?
- What is equivalence for P06?
- What disagreements are material?

P0.8:

- How is the smallest useful clarification selected?
- When should another bounded model inference be preferred over a human question?

P0.9:

- What does robust reasoning mean for exact values versus intervals or ranges?
- Which conclusions may be asserted across multiple admissible hypotheses?

## Roadmap

- P0.1 Public Reasoning View V1: DONE
- P0.2 Structured Clarification Interaction: DONE
- P0.3 Quantitative Evidence Gap Semantics V2: DONE
- P0.3.1 P06 Live Extraction Stability: DONE, baseline 8/10 canonical signature
- P0.3.2 P06 Evidence Contract Hardening: DONE, final prompt v5, bounded canonical sample 10/10
- P0.4 Single-Inference Boundary Findings: CURRENT
- P0.5 Candidate Evidence & Semantic Hypothesis Foundation
- P0.6 Bounded Multi-Inference Sampling
- P0.7 Admissibility, Analytical Equivalence & Material Disagreement
- P0.8 Active Clarification
- P0.9 Robust Reasoning over the Admissible Hypothesis Space
- P1 Scenario Model: POSTPONED until semantic foundation is established

Later:

- Process Operation Graph
- Opportunity Map
- Technology Fit redesign
