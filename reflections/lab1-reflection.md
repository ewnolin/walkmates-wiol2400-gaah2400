# Lab Reflection — WalkMates

> One per lab. Keep it **short and specific** — this is graded for *understanding*, not length.
> Half a page to a page is plenty. Bullet points are fine.

**Lab:** 1
**Pair:** William Olin | Gargaar Ahmed
**Repo commit/tag:** (link — Labs 1–3; write `N/A` for Lab 4)

**Analysis:** see [`lab1-analysis.md`](../lab1-analysis.md) (Activities 1.1 and 1.2).

---

### 1. What we did
For part A, we analyzed three features, wallet top-ups (FR-1.3), booking decision rule (FR-4.4)
and the AI match-explanation service (FR-5) against ISO/IEC 25010 quality characteristics, and 
traced the FR-4.4 rule-2 booking-limit bug from the given failure report. For part B we implemented
`SeekerSpecBasedTest` with tests on email, display name, phone number and wallet top-up amount
(FR-1.1, FR-1.3), boundary-value tests on the 10.00 minimum, 5 000.00 single transaction maximum
and 20 000.00 balance cap (FR-1.3), and a decision-table covering set for all four trust tiers.

### 2. What we found
What we found was a undocumented bug in the Seeker's phone regex which only accepts 7 digits after 
the international prefix `+467`, but FR-1.1 specifies `+467XXXXXXXX`, matching the natural international
form of a real Swedish number (e.g. `0701234567`-> `+46701234567`). We only caught iy because our test
used the spec-correct value and the constructor threw where we expected it to succeed.

### 3. AI use (be honest — it doesn't lower your grade)
What we used AI for was identify this error documented in the section above. When it failed unexpectedly, we didn't
assume our test was wrong and wen t back into Seeker.java and the source confined the regex itself was short a digit
versus FR-1.1. Something we caught ourselves was one of our own boundary tests (`emailTooLongIsRejected`) 
initially landed exactly at 254 char-limit instead of over it, because we didn't calculate the domain without adjusting
the repeating 'a's. This was an off by one error which made the test instead of failing it was successful in creating a 
Seeker which meant the outcome wasn't what we expected.

### 4. Judgment
We had to decide which EP classes were worth their own row beyond the minimum ask, example in treating
the two valid phone formats as distinct classes rather one generic phone number representative. Treating
email length violation and format violation as two separate invalid classes, since they trip different `if`
branches in `Seeker` itself. We also had to judge, once our own tests failed, whether that pointed was a real
code bug or not meant re-deriving the expected digit count.

### 5. What we'd test next
If we had another hour we would test the other seeded faults the lab mentions but doesn't ask us to hunt 
yet. Some turning both Activity 1.2 findings into real, isolated tests rather than only reasoned through analysis.


---
