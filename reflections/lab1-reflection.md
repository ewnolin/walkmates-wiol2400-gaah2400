# Lab Reflection — WalkMates

> One per lab. Keep it **short and specific** — this is graded for *understanding*, not length.
> Half a page to a page is plenty. Bullet points are fine.

**Lab:** 1
**Pair:** William Olin | Gargaar Ahmed
**Repo commit/tag:** (link — Labs 1–3; write `N/A` for Lab 4)

**Analysis:** see [`../docs/lab1-analysis.md`](../docs/lab1-analysis.md) (Activities 1.1 and 1.2).

---

### 1. What we did
A few sentences: which tests/artifacts you produced and why those, against which requirements
(cite rule IDs, e.g. FR-1.3, FR-4.4).



### 2. What we found
The most interesting thing you learned or uncovered — a boundary bug, a surviving mutant, a
covered-but-buggy path, a fallback that didn't behave, a metamorphic relation that broke.



### 3. AI use (be honest — it doesn't lower your grade)
- What did you use AI for in this lab?
- **What did the AI suggest vs. what you kept or changed — and why?** (the key question)
- Anything the AI produced that you suspected was wrong or weak? How did you check?



### 4. Judgment
Where did *you* have to decide something the tools/AI couldn't decide for you? (e.g. which
equivalence classes matter, whether coverage was "enough", whether a mutant was equivalent.)



### 5. What we'd test next
If you had another hour, what's the next test or risk you'd go after?

---

### Table created

| #  | Attribute | Class | Representive Input | Expected outcome |
|---|---|---|---|---|
| 1 | Email | Valid | "william@example.com" | Seeker constructed |
| 2 | Email | Invalid -- malformed (no domain) | "notemail" | `IllegalArgumentException` |
| 3 | Email | Invalid -- too long | _Check implementation_ | `IllegalArgumentException` |
| 4 | Display name | Valid | "William Olin" | Seeker constructed |
| 5 | Display name | Invalid -- Too short | "A" | `IllegalArgumentException` |
| 6 | Display name | Invalid -- disallowed char | "GAR1" | `IllegalArgumentException` |
| 7 | Phone | Valid | "0701234567" | Seeker constructed |
| 8 | Phone | Valid | "+46701234567" | Seeker constructed |
| 9 | Phone | Invalid | "12345" | `IllegalArgumentException` |
| 10 | Wallet top-up | Valid | 100.00 | Balance increases by `100.00` |
| 11 | Wallet top-up | Invalid -- below minimum | 5.00 | `IllegalArgumentException` |