# Lab Reflection — WalkMates

**Lab:** 2  **Pair:** William Olin | Gargaar Ahmed  

**Analysis:** see [`lab2-analysis.md`](file:///C:/Users/Garga/Miun/DV033G/walkmates-wiol2400-gaah2400/lab2-analysis.md) (Activities 3.1–4.3).

---

### 1. What we did

For part A, we measured the structural coverage of `PricingCalculator` with JaCoCo, starting from **83% line coverage 
and 50% branch coverage**. We added tests for the free `SHELTER_VOLUNTEER` branch and the overnight surcharge branch, 
which increased the coverage to **92% line coverage and 70% branch coverage**. We then added a boundary-value test for 
exactly 480 minutes because FR-4.3 requires the surcharge only when the duration is strictly greater than 480 minutes. 
For part B, we used PIT mutation testing, added a regression test for the booking-limit boundary, and used Mockito to 
isolate `BookingService` and `SeekerService`. We tested successful, failed and timed-out wallet top-ups, and verified 
that a successful booking sends a confirmation notification.

### 2. What we found

What we found was that the supplied `PricingCalculator` implementation contained a boundary defect: it used `>= 480` 
instead of `> 480`. Our 600-minute test covered the surcharge branch but did not expose this fault, because both comparisons 
apply the surcharge at 600 minutes. The exact 480-minute test failed with **716.80 expected but 860.16 actual**, 
which showed that the surcharge was incorrectly applied at the boundary. We also found the same type of off-by-one 
problem in the booking-limit logic, where `>` allowed a `NEW` seeker with exactly one active booking to create another 
booking even though the limit was one. Both production comparisons were corrected and the final full test suite passed 
with **34 tests, 0 failures and 0 errors**.

### 3. AI use (be honest — it doesn't lower your grade)

What we used AI for was helping us work through the Lab 2 activities step by step, understand the JaCoCo and PIT results, 
and construct the structural and Mockito tests. When tests failed, we checked the actual output and source code rather 
than assuming the suggested test or expected value was correct. For example, our first expected value for the 600-minute 
overnight test was wrong, and we corrected it after calculating the pricing manually. We also accidentally changed the 
production comparison to an invalid `=` operator while fixing the boundary defect, which caused a compilation error. 
We then corrected it to `>` and reran the tests. The important part was using the actual test and build results to verify each change.

### 4. Judgment

We had to decide which structural tests were necessary to cover the important branches and which boundary values were
needed to distinguish correct behaviour from the seeded faults. In particular, we had to judge that a duration clearly 
above 480 minutes was not enough and that exactly 480 minutes was necessary to distinguish `>=` from `>`. We also had to 
decide which dependencies to mock in the service tests so that `BookingService` and `SeekerService` could be tested in isolation. 
For regression selection, we selected the existing `PricingCalculator` tests that exercise the normal pricing path, 
the free-listing branch, the overnight path and the 480-minute boundary.

### 5. What we'd test next

If we had another hour, we would investigate the remaining PIT surviving mutations more deeply and add targeted tests 
where they are relevant to the Lab 2 requirements. We would also expand the Mockito tests with additional failure paths 
in `BookingService`, such as provider capacity or insufficient balance, and run the complete regression suite after each change. 
This would give us more evidence that the tests are not only covering the code but also detecting incorrect implementations of the service decisions.