package com.walkmates.lab1;

import com.sun.jdi.connect.IllegalConnectorArgumentsException;
import com.walkmates.model.Seeker;
import com.walkmates.model.TrustTier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Lab 1, Part B — specification-based tests for {@link Seeker}.
 *
 * <p>Design your tests on paper first (equivalence partitions, boundary values, decision table)
 * from {@code docs/REQUIREMENTS.md} FR-1.1 / FR-1.3 / FR-1.2, then implement them here. One
 * worked example is provided; the {@code TODO}s are yours.</p>
 */
class SeekerSpecBasedTest {

    // ---- Worked example: boundary value at the maximum single top-up (FR-1.3) ----
    @Test
    @DisplayName("Top-up exactly at the 5000 SEK single-transaction maximum is accepted")
    void topUpAtSingleMaximumIsAccepted() {
        Seeker seeker = new Seeker("sam@example.com", "Sam", "0707654321");

        seeker.addFunds(Seeker.MAX_SINGLE_TOP_UP); // 5000.00, the boundary value

        assertThat(seeker.getBalance()).isEqualTo(Seeker.MAX_SINGLE_TOP_UP);
    }

    @Test
    @DisplayName("Well-formed email accepted")
    void validEmailAtConstruction() {
        Seeker seeker = new Seeker("william@example.com", "William", "0701234567");
        assertThat(seeker.getEmail()).isEqualTo("william@example.com");
    }

    @Test
    @DisplayName("Malformed email with no domain is rejected")
    void invalidEmailWithNoDomainIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new Seeker("notemail", "William", "0701234567"));
    }

    @Test
    @DisplayName("Email over 254 chars is rejected")
    void emailTooLongIsRejected() {
        String longString = "a".repeat(245) + "@email.com";
        assertThrows(IllegalArgumentException.class,
                () -> new Seeker(longString, "William", "0701234567"));
    }

    @Test
    @DisplayName("Valid display name is accepted")
    void validDisplayNameIsAccepted() {
        Seeker seeker = new Seeker("william@email.com", "William Olin", "0701234567");
        assertThat(seeker.getDisplayName()).isEqualTo("William Olin");
    }

    @Test
    @DisplayName("Display name shorter than 2 characters is rejected")
    void displayNameTooShortIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new Seeker("william@email.com", "A", "0701234567"));
    }

    @Test
    @DisplayName("Display name with disallowed characters is rejected")
    void displayNameInvalidCharactersIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new Seeker("william@email.com", "Gar1", "0701234567"));
    }

    @Test
    @DisplayName("Swedish-format phone number is accepted")
    void validSwedishPhoneIsAccepted() {
        Seeker seeker = new Seeker("william@email.com", "William", "0701234567");
        assertThat(seeker.getPhoneNumber()).isEqualTo("0701234567");
    }

    @Test
    @DisplayName("International-format phone number is accepted")
    void validInternationalPhoneIsAccepted() {
        Seeker seeker = new Seeker("william@email.com", "William", "+4671234567");
        assertThat(seeker.getPhoneNumber()).isEqualTo("+4671234567");
    }

    @Test
    @DisplayName("Phone number in an invalid format is rejected")
    void invalidPhoneIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new Seeker("william@email.com", "William", "12345"));
    }

    @Test
    @DisplayName("A valid top-up amount is accepted")
    void validTopUpIsAccepted() {
        Seeker seeker = new Seeker("william@email.com", "William", "0701234567");
        seeker.addFunds(100.00);
        assertThat(seeker.getBalance()).isEqualTo(100.00);
    }

    @Test
    @DisplayName("A top-up below the minimum is rejected and balance is unchanged")
    void topUpBelowMinimumIsRejected() {
        Seeker seeker = new Seeker("william@email.com", "William", "0701234567");
        assertThrows(IllegalArgumentException.class, () -> seeker.addFunds(5.00));
        assertThat(seeker.getBalance()).isEqualTo(0.00);
    }

    @Test
    @DisplayName("Top-up just below the 10.00 minimum is rejected")
    void topUpJustBelowMinimumIsRejected() {
        Seeker seeker = new Seeker("william@email.com", "William", "0701234567");
        assertThrows(IllegalArgumentException.class, () -> seeker.addFunds(9.99));
        assertThat(seeker.getBalance()).isEqualTo(0.00);
    }

    @Test
    @DisplayName("Top-up exactly at the 10.00 minimum is accepted")
    void topUpAtMinimumIsAccepted() {
        Seeker seeker = new Seeker("william@email.com", "William", "0701234567");
        seeker.addFunds(10.00);
        assertThat(seeker.getBalance()).isEqualTo(10.00);
    }

    @Test
    @DisplayName("Top-up just above the 10.00 minimum is accepted")
    void topUpJustAboveMinimumIsAccepted() {
        Seeker seeker = new Seeker("william@email.com", "William", "0701234567");
        seeker.addFunds(10.01);
        assertThat(seeker.getBalance()).isEqualTo(10.01);
    }

    @Test
    @DisplayName("Top-up just below the 5000 single-transaction maximum is accepted")
    void topUpJustBelowSingleMaximumIsAccepted() {
        Seeker seeker = new Seeker("william@email.com", "William", "0701234567");
        seeker.addFunds(4999.99);
        assertThat(seeker.getBalance()).isEqualTo(4999.99);
    }

    @Test
    @DisplayName("Top-up just above the 5000 single-transaction maximum is rejected")
    void topUpJustAboveSingleMaximumIsRejected() {
        Seeker seeker = new Seeker("william@email.com", "William", "0701234567");
        assertThrows(IllegalArgumentException.class, () -> seeker.addFunds(5000.01));
        assertThat(seeker.getBalance()).isEqualTo(0.00);
    }

    @Test
    @DisplayName("BVA: top-up that lands just below the 20000 balance cap is accepted")
    void topUpJustBelowBalanceCapIsAccepted() {
        Seeker seeker = new Seeker("william@email.com", "William", "0701234567");
        seeker.addFunds(5000.00); // 5000.00
        seeker.addFunds(5000.00); // 10000.00
        seeker.addFunds(5000.00); // 15000.00
        seeker.addFunds(4999.99); // 19999.99 — just below the cap

        assertThat(seeker.getBalance()).isEqualTo(19999.99);
    }

    @Test
    @DisplayName("BVA: top-up that lands exactly at the 20000 balance cap is accepted")
    void topUpAtBalanceCapIsAccepted() {
        Seeker seeker = new Seeker("william@email.com", "William", "0701234567");
        seeker.addFunds(5000.00); // 5000.00
        seeker.addFunds(5000.00); // 10000.00
        seeker.addFunds(5000.00); // 15000.00
        seeker.addFunds(5000.00); // 20000.00 — exactly at the cap

        assertThat(seeker.getBalance()).isEqualTo(20000.00);
    }

    @Test
    @DisplayName("BVA: top-up that would push balance just above the 20000 cap is rejected")
    void topUpJustAboveBalanceCapIsRejected() {
        Seeker seeker = new Seeker("william@email.com", "William", "0701234567");
        seeker.addFunds(5000.00); // 5000.00
        seeker.addFunds(5000.00); // 10000.00
        seeker.addFunds(5000.00); // 15000.00
        seeker.addFunds(4990.00); // 19990.00
        assertThrows(IllegalArgumentException.class, () -> seeker.addFunds(10.01));
        assertThat(seeker.getBalance()).isEqualTo(19990.00); // unchanged
    }

    @Test
    @DisplayName("NEW tier allows 1 booking at 15% fee")
    void newTierLimitsAreCorrect() {
        Seeker seeker = new Seeker("william@email.com", "William", "0701234567");
        seeker.setTrustTier(TrustTier.NEW); // already the default, set explicitly for clarity

        assertThat(seeker.getMaxConcurrentBookings()).isEqualTo(1);
        assertThat(seeker.getTrustTier().getPlatformFee()).isEqualTo(0.15);
    }

    @Test
    @DisplayName("VERIFIED tier allows 3 bookings at 12% fee")
    void verifiedTierLimitsAreCorrect() {
        Seeker seeker = new Seeker("william@email.com", "William", "0701234567");
        seeker.setTrustTier(TrustTier.VERIFIED);

        assertThat(seeker.getMaxConcurrentBookings()).isEqualTo(3);
        assertThat(seeker.getTrustTier().getPlatformFee()).isEqualTo(0.12);
    }

    @Test
    @DisplayName("TRUSTED tier allows 5 bookings at 8% fee")
    void trustedTierLimitsAreCorrect() {
        Seeker seeker = new Seeker("william@email.com", "William", "0701234567");
        seeker.setTrustTier(TrustTier.TRUSTED);

        assertThat(seeker.getMaxConcurrentBookings()).isEqualTo(5);
        assertThat(seeker.getTrustTier().getPlatformFee()).isEqualTo(0.08);
    }

    @Test
    @DisplayName("PRO_SITTER tier allows 10 bookings at 5% fee")
    void proSitterTierLimitsAreCorrect() {
        Seeker seeker = new Seeker("william@email.com", "William", "0701234567");
        seeker.setTrustTier(TrustTier.PRO_SITTER);

        assertThat(seeker.getMaxConcurrentBookings()).isEqualTo(10);
        assertThat(seeker.getTrustTier().getPlatformFee()).isEqualTo(0.05);
    }
}
