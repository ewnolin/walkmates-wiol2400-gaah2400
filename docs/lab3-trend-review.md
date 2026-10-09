# Lab 3 — Research trend mini-review: testing AI systems without an oracle

**Pair:** William Olin | Gargaar Ahmed

**Trend chosen:** testing AI systems (and, inside it, the test oracle problem)

**Topic we would implement in WalkMates:** layered evaluation ("evals") of the AI explanation

Part A of this lab tests `MatchExplanationService`, whose output comes from a language model.
There is no exact expected string for a model's answer, so the question behind every test we
wrote was: *what can we still check, and how strongly?* This note compares a practitioner source
with academic and standards sources on that question (Part B.1) and then picks one topic from
that literature and explains how and why it could be implemented in WalkMates (Part B.2).

## Sources

| # | Type | Source |
|---|---|---|
| P1 | Practitioner | Hamel Husain, *Your AI Product Needs Evals*, 29 March 2024. <https://hamel.dev/blog/posts/evals/> |
| P2 | Practitioner / community guidance | OWASP Gen AI Security Project, *LLM01:2025 Prompt Injection*. <https://genai.owasp.org/llmrisk/llm01-prompt-injection/> |
| A1 | Academic | T. Y. Chen, F.-C. Kuo, H. Liu, P.-L. Poon, D. Towey, T. H. Tse and Z. Q. Zhou, *Metamorphic Testing: A Review of Challenges and Opportunities*, ACM Computing Surveys 51(1), Article 4, January 2018. DOI 10.1145/3143561 |
| S1 | Standard (abstract only) | ISO/IEC TR 29119-11:2020, *Software and systems engineering — Software testing — Part 11: Guidelines on the testing of AI-based systems*. <https://www.iso.org/standard/79016.html> |

We used the web versions of P1 and P2 and a public reading copy of A1. For S1 we could only
read the public abstract page, because the standard itself is paywalled, so we do not claim
anything about its detailed content.

## B.1 What each source adds

**P1 (practitioner)** argues that LLM products fail mostly because teams have no evaluation
system of their own. Its lowest level is plain *unit tests and assertions*: cheap, fast checks
that run on every code change, scoped by feature and by scenario (for example "only one listing
matches", "several match", "none match"). Test inputs should be hard but realistic, and tests
should grow from real failures. 

**P2 (practitioner / community)** describes prompt injection as input that changes a model's
behaviour in ways the developer did not intend, including *indirect* injection where untrusted
external content carries the instructions. Its mitigations include constraining the model's
role, **segregating and labelling untrusted content**, filtering input and output, human approval
for risky actions and regular adversarial testing. It states plainly that it is unclear whether
fool-proof prevention exists, so the measures only reduce the impact.

**A1 (academic)** defines the *test oracle problem*: it is difficult or impossible to verify
whether the result of a test is correct. Metamorphic testing (MT) addresses this by running a
source test case, deriving follow-up test cases with a *metamorphic relation* (a necessary
property that links several inputs and their outputs), and checking the relation instead of the
exact output.

**S1 (standard, abstract)** describes AI-based systems as complex, often poorly specified and
potentially non-deterministic, and names the test oracle problem as the main challenge when
testing them. The abstract also mentions black-box guidelines, white-box testing for neural
networks and options for test environments and scenarios.

### Comparison: practitioner vs academic/standards

- **They agree on the problem.** P1 starts from "you cannot judge outputs with a simple
  equality check" and S1 and A1 name the same thing, the oracle problem. All of them move the
  check from *the exact answer* to *properties that must hold*.
- **They differ in what a failed check means.** P1 treats assertion pass rates as a product
  decision (some failures are tolerable) and builds tests from observed failures. A1 treats a
  violated metamorphic relation as evidence of a fault, because the relation is a *necessary
  property* of the program. Which of the two applies depends on what is being tested: for the
  model's free text a tolerance makes sense, for deterministic code around it it does not.
- **They differ in where tests come from.** P1: from real user failures and hard synthetic
  inputs. A1: derived systematically from a relation and a source input. P2: from an attacker's
  point of view (adversarial testing).
- **What the practitioner source adds that the academic one does not:** an order of work (cheap
  assertions first, human review later) and the idea that the pass bar is a product decision.
  **What the academic source adds:** a principled way to get a check without an oracle, and an
  honest account of how far that check can be trusted.

## B.2 Topic to implement in WalkMates: a layered evaluation of the live explanation

### Where Part A stops
Our Part A tests cover everything around the model that is deterministic: the prompt, the fallback,
the ranking and the HTTP contract. They say nothing about whether the text a real model produces
is any good, because in the repository the model is a mock or the canned `StubLlmClient`. That
gap is where the literature points next: P1's second level and P2's
adversarial testing, both on top of the cheap assertions we already have.

### Why it fits WalkMates
- The explanation is shown to a seeker who is deciding whether to book a stranger's animal-care
  task, so an explanation that invents facts not in the listing, repeats a provider's instructions
  or is simply off-topic is a real quality and trust problem, not only a cosmetic one.
- The provider's description is free text and goes into the prompt. Our injection tests only show
  how the prompt is *built* (P2 says this cannot prove resistance), so a test that sends such text
  through a real model is the only way to learn whether the defence holds in practice.
- The model, its version and the prompt can change without any Java code changing. A fixed set of
  scenarios turns that into a visible before/after instead of a silent change.
- 

### How it could be implemented
1. **Scenario set (P1: scenarios, hard inputs).** A small JSON file in `src/test/resources/eval/`
   with about 20 cases: each combines a trust tier, a listing type and a description. Include normal
   listings, an empty description, a very long one, a Swedish one, and injection variants (the plain
   one from Activity 5.4, the delimiter break-out and the title-injection that our two disabled
   tests describe).
2. **Level 1 checks (cheap assertions on real output).** For every case: the answer is not blank,
   is at most two sentences (the prompt asks for 1-2), does not contain the prompt's own markers
   (`LISTING_DESCRIPTION_DATA`, "untrusted USER DATA"), and for the injection cases is not just
   "YES" and does not follow the planted instruction. Each case runs several times and we require a
   pass rate (for example at least 95 %) instead of 100 %, which is P1's point that the bar is a
   product decision.

### Limits and costs
- Needs an API key and a small budget, and real traces must not contain real user data.
