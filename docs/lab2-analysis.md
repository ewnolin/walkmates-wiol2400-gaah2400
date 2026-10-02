# Lab 2 Analysis

**Pair:** William Olin | Gargaar Ahmed

## Activity 3.1 - Structural Coverage Baseline

The initial JaCoCo coverage for `PricingCalculator` was **83% line coverage** and **50% branch coverage**. There were **5 of 15 lines** and **5 of 10 branches** not covered.

The existing structural test exercised a normal 60-minute walk, but it did not exercise all of the pricing decisions. In particular, the free `SHELTER_VOLUNTEER` branch and the overnight surcharge branch needed explicit tests.

The baseline showed why line coverage alone was not enough for this component. The pricing decision contains branches where different inputs can reach different calculations, so the tests need to exercise both sides of those conditions.

---

## Activity 3.2 - Branch Coverage Improvement

### 1. Free `SHELTER_VOLUNTEER` Branch

A test was added for a `SHELTER_VOLUNTEER` listing because this listing type is always free.

```
java
@Test
@DisplayName("SHELTER_VOLUNTEER listing is always free")
void shelterVolunteerIsFree() {
    Booking booking = new Booking("seeker-1", "listing-1", 60);

    double price = pricing.priceFor(
            booking,
            listing(ListingType.SHELTER_VOLUNTEER),
            seeker(TrustTier.VERIFIED)
    );

    assertThat(price).isEqualTo(0.00);
}
```

After adding this test, the `PricingCalculator` coverage increased to **86% line coverage** and **60% branch coverage**.

The number of missed lines decreased from 5 to 4, and the number of missed branches decreased from 5 to 4.

### 2. Overnight Surcharge Branch

A 600-minute `DOG_WALK` test was added to exercise the overnight surcharge branch.

```
java
@Test
@DisplayName("600 min DOG_WALK applies the 20% overnight surcharge")
void overnightWalkIncludesSurcharge() {
    Booking booking = new Booking("seeker-1", "listing-1", 600);

    double price = pricing.priceFor(
            booking,
            listing(ListingType.DOG_WALK),
            seeker(TrustTier.VERIFIED)
    );

    assertThat(price).isEqualTo(1075.20);
}
```

The expected value was calculated as:

- 600 minutes = 10 hours
- Base cost = 10 × 80 = 800 SEK
- Overnight surcharge = 20% × 800 = 160 SEK
- Subtotal = 960 SEK
- VERIFIED platform fee = 12% × 960 = 115.20 SEK
- Final price = 1075.20 SEK

After adding this test, coverage increased to **92% line coverage** and **70% branch coverage**.

The number of missed lines decreased to 3 of 15, and missed branches decreased to 3 of 10.

The existing 60-minute test already exercised the non-overnight path.

---

## Activity 3.3 - Boundary-Value Test

The important boundary for the overnight rule is exactly **480 minutes**.

The requirement says that the surcharge applies only when the duration is **strictly greater than** 480 minutes.

A test was therefore added for exactly 480 minutes:

```java
@Test
@DisplayName("Exactly 480 minutes must NOT receive the overnight surcharge")
void exactly480MinutesHasNoOvernightSurcharge() {
    Booking booking = new Booking("seeker-1", "listing-1", 480);

    double price = pricing.priceFor(
            booking,
            listing(ListingType.DOG_WALK),
            seeker(TrustTier.VERIFIED)
    );

    assertThat(price).isEqualTo(716.80);
}
```

The expected value is:

- 480 minutes = 8 hours
- Base cost = 8 × 80 = 640 SEK
- No overnight surcharge
- VERIFIED platform fee = 12% × 640 = 76.80 SEK
- Final price = 716.80 SEK

### 1. Observed Failure

Before fixing the production code, the test failed.

The implementation used:

```
java
if (booking.getDurationMinutes() >= OVERNIGHT_THRESHOLD_MINUTES) {
    overnightExtra = baseCost * OVERNIGHT_SURCHARGE_RATE;
}
```

For exactly 480 minutes, the actual result was **860.16 SEK** instead of **716.80 SEK**.

The difference comes from the 20% surcharge being applied at the boundary:

- Base cost = 640 SEK
- Incorrect surcharge = 128 SEK
- Subtotal = 768 SEK
- VERIFIED fee = 92.16 SEK
- Actual result = 860.16 SEK

This was a real failure caused by the comparison operator.

### 2. Fault

The fault was the use of:

```
>=
```

instead of:

```
>
```

The 600-minute overnight test alone would not have found this defect because both `>=` and `>` produce the surcharge for 600 minutes.

The exact 480-minute test is therefore important because it distinguishes the two operators at the boundary.

### 3. Fix

The production code was corrected to:

```
java
if (booking.getDurationMinutes() > OVERNIGHT_THRESHOLD_MINUTES) {
    overnightExtra = baseCost * OVERNIGHT_SURCHARGE_RATE;
}
```

The focused structural tests then passed.

A subsequent full test run passed with:

- **34 tests**
- **0 failures**
- **0 errors**
- **BUILD SUCCESS**

This demonstrates that the boundary test detected a defect that branch coverage by itself would not necessarily reveal.

---

## Activity 4.1 - Mutation Testing

PIT mutation testing was executed with:

```
mvn clean test org.pitest:pitest-maven:mutationCoverage
```

The overall PIT result was:

- **194 mutations generated**
- **67 mutations killed**
- **119 mutations with no coverage**
- **8 surviving mutations**
- **89% test strength**

The raw killed/generated ratio is approximately **35%**, but this number needs to be interpreted together with the 119 mutations that had no coverage.

The relevant `PricingCalculator` boundary mutation was killed by the new 480-minute boundary test. The overnight conditional mutation was also killed by the structural tests.

This provides evidence that the new tests are not only executing the code but are capable of detecting specific changes to the pricing conditions.

The mutation results also show that simply increasing line or branch coverage does not guarantee that every possible faulty implementation will be detected.

---

## Activity 4.1 - Booking Limit Regression

The booking-limit fault from Lab 1 was tested again using Mockito isolation.

The original implementation contained:

```
java
long seekerActive = activeBookingCountForSeeker(seekerId);
if (seekerActive > seeker.getMaxConcurrentBookings()) {
    throw new BookingRejectedException(
            "Seeker booking limit reached for tier " + seeker.getTrustTier());
}
```

The test used a `NEW` seeker whose maximum number of concurrent bookings is one, and supplied exactly one active booking.

### 1. Observed Failure

With the faulty `>` comparison, the focused test failed because no `BookingRejectedException` was thrown.

The important boundary was:

```
seekerActive == max
1 == 1
```

The faulty comparison evaluates:

```
1 > 1
```

as `false`, so the booking was incorrectly allowed.

### 2. Fix

The production condition was changed to:

```
java
if (seekerActive >= seeker.getMaxConcurrentBookings()) {
    throw new BookingRejectedException(
            "Seeker booking limit reached for tier " + seeker.getTrustTier());
}
```

The focused regression test then passed:

- **1 test**
- **0 failures**
- **0 errors**
- **BUILD SUCCESS**

This reproduces the boundary analysis from Lab 1 and demonstrates that the regression test catches the exact off-by-one fault.

---

## Activity 4.2 - Component Isolation with Mockito

Mockito was used to isolate `BookingService` and `SeekerService` from their repositories and external service seams.

### SeekerService

Three tests were implemented for `SeekerService.topUp`.

#### 1. Successful Top-Up

The payment service is mocked to succeed.

The test verifies that:

- the wallet is credited with **100 SEK**;
- the payment service is called with the correct arguments;
- the seeker is saved.

The test passed successfully.

#### 2. Payment Decline

The mocked payment service throws:

```
PaymentService.PaymentException
```

The test verifies that:

- the exception is propagated;
- the wallet remains at **0.00 SEK**;
- the seeker is not saved.

This demonstrates that the wallet is not credited when the external payment charge fails.

#### 3. Payment Timeout

The mocked payment service throws:

```
PaymentService.PaymentTimeoutException
```

The test verifies that:

- the timeout exception is propagated;
- the wallet remains at **0.00 SEK**;
- the seeker is not saved.

All three tests passed:

- **3 tests**
- **0 failures**
- **0 errors**
- **BUILD SUCCESS**

### BookingService

Two Mockito-based tests were implemented.

The first tests the exact concurrent-booking boundary from the previous activity.

The second tests a successful booking and verifies the notification interaction:

```
java
verify(notifications).sendBookingConfirmed(seeker, result);
```

The two tests passed:

- **2 tests**
- **0 failures**
- **0 errors**
- **BUILD SUCCESS**

The use of mocks means that the tests focus on the service's decisions and interactions rather than depending on real repository or notification implementations.

---

## Activity 4.3 - Regression Selection

The repository did not contain the `feature/weekend-surcharge` branch mentioned in the lab instructions, so a simulated change to `PricingCalculator` was used for regression selection.

Assume that a weekend surcharge is added to the pricing calculation.

The following tests should be selected for regression testing:

- `shortWalkHasBasePriceAndVerifiedFee`
- `shelterVolunteerIsFree`
- `overnightWalkIncludesSurcharge`
- `exactly480MinutesHasNoOvernightSurcharge`

### Reason for Selection

- `shortWalkHasBasePriceAndVerifiedFee` exercises the normal non-overnight pricing path.
- `shelterVolunteerIsFree` protects the separate free-listing branch.
- `overnightWalkIncludesSurcharge` exercises the positive overnight-surcharge path.
- `exactly480MinutesHasNoOvernightSurcharge` protects the most important boundary, where the surcharge must not apply.

The selected tests therefore cover the main existing pricing behaviors that could be affected by a change to `PricingCalculator`, including the boundary that previously exposed the `>=` defect.

---

## Overall Analysis

The Lab 2 work showed the difference between structural coverage and effective test design.

The initial `PricingCalculator` result was **83% line coverage** and **50% branch coverage**. Adding tests for the 
free-listing and overnight branches increased the observed coverage to **92% line coverage** and **70% branch coverage**.

However, the most important finding was the **480-minute boundary**. The overnight test at 600 minutes exercised the 
surcharge branch but could not distinguish between `>=` and `>`. Only the exact boundary test exposed the supplied fault.

The same principle was demonstrated again with the booking-limit regression. Testing a seeker exactly at the trust-tier 
limit exposed the difference between `>` and `>=`.

Mutation testing supported this observation because the relevant pricing boundary mutation was killed by the new structural tests.

Finally, Mockito isolation allowed the service tests to exercise payment success, payment failure, payment timeout, 
booking-limit rejection, and successful-booking notification without relying on real external components.

The main lesson from Lab 2 is therefore that **coverage measures which code is executed, while boundary and mutation 
testing provide evidence that tests can detect incorrect implementations of that code**.