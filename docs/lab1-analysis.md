# Lab 1 Analysis

**Pair:** William Olin | Gargaar Ahmed

## Activity 1.1 - Quality-attribute analysis

Three features from [`REQUIREMENTS.md`](REQUIREMENTS.md), each with the two ISO/IEC
25010 characteristics:

# Wallet top-ups (FR-1.3)
Characteristic number 1 is the functional correctness where the min/max/rounding rules (10.00 which is min, 5 000.00 
single transaction cap, and a 20 000.00 balance cap, round-half-up to 2 decimal points) are exact numeric contracts where
any deviation is real money handled wrong. Characteristic number 2 is security where the balance must never go negative
and must never be pushed past its cap. A seeker is an untrusted actor supplying the top-up amount, so the checks are the
onbly thing stopping wallet-balance manipulation.

# Booking decision rule (FR-4.4)
Characteristic number 1 the functional correctness is the five-condition compound date for the decision. Listing state, tier
limit, provider capacity, duration and wallet balance, that all must accept/reject exactly the right requests and if one
condition's boundary is wrong and the rules silently misbehaves. Characteristic number 2 the reliability where trust-tier limits
and provider capacity exist to bound concurrent load and exposure. The system must consistently enforce them under repeated
booking attempts or the limits advertises earnest real guarantees.

# AI match explanation FR-5
Characteristic number 1 the reliability where fR-5.2 explicitly requires a deterministic fallback on LLM failure or a timeout
with no exception leaking to the callers. The feature must degrade gracefully rather than breaking the booking flow.
The second characteristic is the security where the prompt builder must keep provider-supplied listing text inside explicit delimits
separated from the standing instruction, as a prompt-injection mitigation.
 
### Testable quality requirement (for #2, the booking decision rule)

For every trust tier *T* ∈ {NEW, VERIFIED, TRUSTED, PRO_SITTER} with tier limit
`T.getMaxConcurrentBookings()`, and for a Seeker in tier *T* who already holds exactly
`T.getMaxConcurrentBookings()` Bookings in an active state (REQUESTED, CONFIRMED, or
IN_PROGRESS), a request to create one additional Booking for that Seeker **SHALL** be rejected
(`BookingService.BookingRejectedException`), and the Seeker's active-booking count **SHALL
NEVER** exceed `T.getMaxConcurrentBookings()` after any `createBooking` call.

This is directly checkable: seed a Seeker at each tier with `active == max` active bookings, call
`createBooking`, and assert its throws.

---

## Activity 1.2 — Bug analysis (error → fault → failure)

> *"A NEW seeker who already has one active booking was able to create a second one. The system
> let it through; the seeker now has two active bookings, which should be impossible for their
> tier."*

### 1. The chain

- **Human error:** when coding FR-4.4 rule 2 ("the Seeker's active Bookings **<** the trust-tier
  max"), the developer translated the *acceptance* condition (`active < max`) into a *rejection*
  check using the wrong relational operator — reaching for "reject if active is over the limit"
  (`active > max`) instead of negating the acceptance condition correctly (`active >= max`). It's
  a classic boundary/off-by-one slip: `>` and `>=` look interchangeable unless you deliberately
  test the equal-to-the-limit case.
- **Fault:** the coded condition is permissive exactly at the boundary `active == max`, so a
  Seeker already sitting at their tier limit is not rejected.
- **Failure:** a NEW-tier Seeker (limit 1) who already has one active Booking sends a second
  `createBooking` request; the system accepts and confirms it instead of throwing
  `BookingRejectedException`. The Seeker ends up with two simultaneously active Bookings, which
  FR-1.2/FR-4.4 say must be impossible for that tier.

### 2. Responsible code

```java
long seekerActive = activeBookingCountForSeeker(seekerId);
if (seekerActive > seeker.getMaxConcurrentBookings()) {
    throw new BookingRejectedException("Seeker booking limit reached for tier " + seeker.getTrustTier());
}
```

The comparison operator is `>` where it must be `>=`. The spec requires `active < max` to *accept*, i.e. reject whenever
`active >= max`. With `>`, the boundary case `active == max` evaluates to `false` and the request is wrongly accepted. 
Concretely for a NEW seeker (`max == 1`): after one active booking, `seekerActive == 1`; the check
`1 > 1` is `false`, so no exception is thrown and a second active booking is created — the tier
limit is off by exactly one. (The same off-by-one would under-reject for every tier, not just
NEW; NEW is simply the tier where it's cheapest to trigger, since its limit is 1.)

### 3. Test level and technique

Unit test on `BookingService.createBooking`, in isolation, with `SeekerRepository`,
`ListingRepository`, `ProviderRepository`, and `BookingRepository` mocked/stubbed (the class is
explicitly designed for this — see its Javadoc). Arrange a NEW-tier Seeker whose
`BookingRepository` stub already returns one Booking in an active status (e.g. `CONFIRMED`) for
that seeker, call `createBooking` again, and assert it throws `BookingRejectedException`.

Boundary value on the FR-4.4 rule-2 condition for the case is `seekerActive == max` (here `1 == 1`), not just an interior "well below the limit" case
or a wildly-over-the-limit case. A decision-table test covering all five FR-4.4 conditions would
also expose it, provided the table includes the "active count exactly at the tier max" row rather
than only "clearly under" / "clearly over" rows. An integration or system-level test exercising
two real booking requests end-to-end would also *reveal* the failure, but a unit test at the
boundary is the cheapest and most precise place to have caught the fault, since it isolates the
one faulty condition instead of the whole booking flow.