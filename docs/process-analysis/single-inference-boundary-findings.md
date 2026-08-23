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

### Illustrative Capability-Narrowness Case

This section records an illustrative observed case, not a controlled repeated experiment and not quantitative evidence.

User description:

```text
Tenemos un equipo de tres personas que procesa devoluciones. Entran alrededor de ciento veinte por semana. Cada una lleva entre ocho y doce minutos de trabajo, aunque el cliente suele recibir la resolución recién a las 48 horas. En temporada alta puede llegar a duplicarse el volumen.
```

The live model interpretation correctly recognized, conceptually:

- team size = 3;
- approximate volume near 120 per WEEK;
- manual effort = RANGE 8..12 MINUTE per return;
- client resolution after approximately 48 HOUR as elapsed or cycle time, not manual effort;
- seasonal volume may reach approximately 2 x baseline.

The public P06 result nevertheless had:

```text
clarificationId = null
clarificationQuestions = []
reasoning.establishedInputs = []
calculation = null
decision = null
```

This should not be classified automatically as a bug. It demonstrates a different architectural driver from P0.3.1.

P0.3.1 demonstrated:

```text
same language -> varying semantic interpretations
```

The returns example demonstrates:

```text
good or rich semantic interpretation -> capability contract too narrow to consume all of it
```

Semantic interpretation is not the same thing as analytical or capability projection. A semantic interpretation can be useful and still contain information a specific analytical capability cannot currently consume.

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
        -> CandidateEvidence
        -> one or more semantic hypotheses
        -> admissibility validation
        -> admissible hypothesis space
        -> established / stable knowledge
        -> capability projection
        -> robust reasoning / targeted clarification
```

Using notation:

```text
x -> {H1, H2, ..., Hn} -> A
```

where Hi is a semantic hypothesis and A is the set of admissible hypotheses after deterministic validation.

This is a conceptual architecture decision. It does not define Java records, persistence tables, APIs, or model-calling strategy.

The target boundary for P0.5 to refine is:

```text
Natural Language
        -> Candidate Evidence
        -> Semantic Hypotheses
        -> Admissibility
        -> Admissible Hypothesis Space
        -> Established / Stable Knowledge
        -> Capability Projection
        -> Deterministic Reasoning
```

Bounded semantic sampling and active human clarification are lateral mechanisms around this pipeline, not unbounded control loops. The diagram is conceptual; it is not a final implementation design.

### Epistemic Separation

The future architecture must preserve this conceptual separation:

```text
CandidateEvidence != SemanticHypothesis != EstablishedKnowledge != CapabilityProjection
```

CandidateEvidence is the canonical architectural term for the role previously described as an extracted candidate. It is evidence-oriented and must preserve source grounding and provenance. This is a conceptual architecture name, not an assertion that a Java type named CandidateEvidence exists.

CandidateEvidence is a source-grounded piece of potentially relevant structure detected in the user description, without necessarily assigning its final business semantic role.

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

SemanticHypothesis is one coherent semantic interpretation assigning CandidateEvidence to domain meaning and relations. For the same temporal expression, alternatives may include PROCESS_ELAPSED_TIME or, only where justified by source wording, MANUAL_EFFORT_PER_BUSINESS_ITEM.

EstablishedKnowledge is knowledge the system may treat as authoritative for deterministic reasoning according to explicit grounding, provenance, and establishment rules. Deterministic validation may establish structural consistency, contract compatibility, admissibility, dimensional validity, or provenance consistency, but validation alone does not make an LLM-generated semantic hypothesis true.

CapabilityProjection is a capability-specific, deterministic projection of available established or stable semantic knowledge into the inputs that a particular analytical capability knows how to consume.

Examples include:

- P06 operational-burden projection;
- future Technology Fit projection;
- future capacity-analysis projection.

P06 is one consumer of a capability-specific projection from a broader semantic knowledge model. The general semantic model must not be designed around P06.

### Finite Backend Domain

The backend must not attempt to encode a rule for every possible natural-language formulation. Natural language is open-ended.

Backend rules must operate on canonical semantic concepts, relations, dimensions, and invariants, not on particular linguistic phrases.

Anti-pattern examples:

```text
if text says "per week" ...
if text says "less than one day" ...
if text says "high season" ...
if text says "four or five" ...
```

These do not scale.

Desired rule level examples:

```text
RANGE -> lowerBound <= upperBound
QUANTITY / WEEK -> dimensionally distinct from QUANTITY / MONTH
MANUAL_EFFORT -> active human work
PROCESS_ELAPSED_TIME -> not manual effort
SOURCE_STATED -> requires appropriate grounding/provenance
DERIVED_FACT -> requires explicit premises
```

These names are conceptual examples unless already present in the repository. The generic principle is rules about concepts, not rules about phrases.

The deterministic backend should operate over the finite canonical semantic domain required by the analytical capabilities currently implemented:

```text
open linguistic space
        -> candidate extraction / semantic interpretation
        -> finite composable semantic vocabulary
        -> deterministic reasoning
```

This is not a proposal for a global enterprise ontology or generic workflow engine. Domain vocabulary should grow only when an analytical capability requires it.

Complexity should emerge from composition of a finite semantic vocabulary, not from accumulation of linguistic special cases. The vocabulary may conceptually contain primitives such as quantity, range, bound, period, duration, condition, relation, actor, action, subject, and unit, but P0.4 does not define a complete ontology.

A good future semantic model should represent new business-language cases primarily by combining existing primitives. It should not require a new semantic type or backend rule for every new wording encountered.

The future semantic foundation must be broader than P06 while remaining finite and incrementally scoped. A concept discovered in one use case must not automatically be modeled as a P06-specific type if it is semantically more general. For example, "48 hours until resolution" should be representable as temporal semantic information even if P06 cannot use it as manual effort.

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

## Semantic Knowledge And Capability Projection

Semantic interpretation is distinct from capability projection:

```text
Semantic Interpretation != Capability Projection
```

The semantic model may preserve information such as:

- team size = 3;
- volume near 120 per WEEK;
- manual effort in [8, 12] MINUTE per item;
- elapsed resolution time near 48 HOUR;
- conditional peak-volume relation near 2 x baseline.

P06 may currently consume only the subset compatible with its dimensional and epistemic contract. The system must not discard useful semantic knowledge merely because P06 cannot currently project it. Likewise, P06 must not silently coerce unsupported knowledge merely to force a calculation.

Prohibited implicit behavior includes:

- silently treating WEEK as MONTH;
- silently choosing 4 weeks/month;
- silently choosing 52/12 weeks/month;
- converting a range to its midpoint;
- using elapsed time as manual effort;
- interpreting team size as available FTE capacity without required premises.

A future capability may legitimately consume information that P06 currently does not. This is why capability projection must remain a distinct layer downstream of semantic interpretation, admissibility, and established or stable knowledge.

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

The CapabilityProjection layer must fit into this target:

```text
Admissible Hypotheses
        -> Stable / Established Knowledge
        -> Capability Projection(s)
        -> f(A) or capability-specific robust consequence
```

P0.4 does not resolve whether robust reasoning operates directly on hypotheses, on projected capability states, or through another intermediate representation. That remains an open P0.7/P0.9 design question.

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

CandidateEvidence extraction may eventually have multiple producers. Conceptually possible producers include:

- BROWSER_DETERMINISTIC;
- BROWSER_LOCAL_MODEL;
- BACKEND_DETERMINISTIC;
- REMOTE_MODEL;
- USER.

These names are conceptual examples, not required enums.

This document does not commit to implementing all of them.

CandidateEvidence must eventually be producer-independent in semantic meaning while preserving producer provenance.

A browser-local model, if explored later, is:

- a preprocessing or candidate producer;
- not an authoritative analyst;
- not a source of established facts merely because it ran locally.

Because browser state is user-controlled, frontend-produced candidates must remain untrusted input to backend validation.

The original description should remain available to the backend together with preprocessed candidates. P0.4 does not define the API shape.

P0.5 must design CandidateEvidence to be sufficiently neutral and composable that language cases such as:

- 120 per week;
- approximately 120;
- between 100 and 140;
- up to 120;
- 30 percent more on Fridays;
- up to 2 x volume in high season;

do not inherently require one dedicated candidate type per phrase. The exact semantic primitives remain open for P0.5.

## Source Grounding

Future CandidateEvidence should remain grounded in the original source text whenever feasible.

```text
CandidateEvidence -> grounded source span
```

Examples of future-checkable properties:

- a source span exists in the original description;
- numeric values claimed as extracted are derivable from the grounded span;
- preprocessing does not silently introduce unsupported factual values.

This requirement applies to source-grounded candidate evidence. It does not require every semantic hypothesis itself to be a literal substring; semantic hypotheses interpret candidate evidence.

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

- What is the minimum compositional CandidateEvidence representation?
- Which semantic primitives are necessary initially?
- Which primitives are generic and which are capability-specific?
- How is CandidateEvidence source-grounded?
- What information must remain source-grounded?
- What provenance is preserved?
- How is CandidateEvidence distinct from current ProcessEffortEvidence?
- How are candidate relations represented without creating phrase-specific types?
- What constitutes a SemanticHypothesis?
- Is a hypothesis process-wide, capability-specific, or can both views coexist?
- Which concepts belong in the first finite semantic vocabulary?
- What is the minimum EstablishedKnowledge model?
- What exactly is a CapabilityProjection?
- What information may remain established even when no current capability can consume it?
- How do we prevent P06 requirements from defining the general semantic model?

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
- P0.4 Single-Inference Boundary Findings: CURRENT / closing
- P0.5 Candidate Evidence & Semantic Hypothesis Foundation: includes CandidateEvidence, source grounding/provenance, finite compositional vocabulary, and semantic knowledge vs capability projection separation
- P0.6 Bounded Multi-Inference Sampling
- P0.7 Admissibility, Analytical Equivalence & Material Disagreement
- P0.8 Active Clarification
- P0.9 Robust Reasoning over the Admissible Hypothesis Space
- P1 Scenario Model: POSTPONED until semantic foundation is established

Later:

- Process Operation Graph
- Opportunity Map
- Technology Fit redesign
