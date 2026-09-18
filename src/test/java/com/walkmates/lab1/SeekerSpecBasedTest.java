package com.walkmates.lab1;

import com.sun.jdi.connect.IllegalConnectorArgumentsException;
import com.walkmates.model.Seeker;
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
        String longString = "a".repeat(244) + "@email.com";
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
        Seeker seeker = new Seeker("william@email.com", "William", "+46701234567");
        assertThat(seeker.getPhoneNumber()).isEqualTo("+46701234567");
    }

    @Test
    @DisplayName("Phone number in an invalid format is rejected")
    void invalidPhoneIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new Seeker("william@email.com", "William", "12345"));
    }

    // TODO (BVA): just-below / at / just-above the 10.00 minimum top-up (FR-1.3).
    // TODO (BVA): a top-up that would push the balance above 20000.00 is rejected (FR-1.3).
    // TODO (Decision table): expected fee + max-bookings for each trust tier (FR-1.2).

    @Test
    @DisplayName("TODO: replace me — invalid email is rejected at registration")
    void invalidEmailIsRejected() {
        // Example of the shape; expand into your full EP set.
        assertThrows(IllegalArgumentException.class,
                () -> new Seeker("not-an-email", "Sam", "0707654321"));
    }
}
