package com.walkmates.lab2;

import com.walkmates.model.Seeker;
import com.walkmates.repository.SeekerRepository;
import com.walkmates.service.NotificationService;
import com.walkmates.service.PaymentService;
import com.walkmates.service.SeekerService;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class SeekerServiceTest {

    private final SeekerRepository seekers = mock(SeekerRepository.class);
    private final PaymentService payments = mock(PaymentService.class);
    private final NotificationService notifications = mock(NotificationService.class);

    private final SeekerService seekerService =
            new SeekerService(seekers, payments, notifications);

    @Test
    void successfulTopUpCreditsWalletAndSavesSeeker() throws Exception {
        Seeker seeker = new Seeker(
                "alex@example.com",
                "Alex",
                "0701234567"
        );

        when(seekers.findById("seeker-1"))
                .thenReturn(Optional.of(seeker));
        when(seekers.save(seeker))
                .thenReturn(seeker);

        seekerService.topUp("seeker-1", "card-1", 100.00);

        assertThat(seeker.getBalance()).isEqualTo(100.00);

        verify(payments).charge("seeker-1", "card-1", 100.00);
        verify(seekers).save(seeker);
    }

    @Test
    void failedTopUpDoesNotCreditWallet() throws Exception {
        Seeker seeker = new Seeker(
                "alex@example.com",
                "Alex",
                "0701234567"
        );

        when(seekers.findById("seeker-1"))
                .thenReturn(Optional.of(seeker));

        doThrow(new PaymentService.PaymentException("Payment declined"))
                .when(payments)
                .charge("seeker-1", "card-1", 100.00);

        assertThatThrownBy(() ->
                seekerService.topUp("seeker-1", "card-1", 100.00)
        ).isInstanceOf(PaymentService.PaymentException.class);

        assertThat(seeker.getBalance()).isEqualTo(0.00);

        verify(payments).charge("seeker-1", "card-1", 100.00);
        verify(seekers, never()).save(any(Seeker.class));
    }

    @Test
    void timedOutTopUpDoesNotCreditWallet() throws Exception {
        Seeker seeker = new Seeker(
                "alex@example.com",
                "Alex",
                "0701234567"
        );

        when(seekers.findById("seeker-1"))
                .thenReturn(Optional.of(seeker));

        doThrow(new PaymentService.PaymentTimeoutException("Payment gateway timed out"))
                .when(payments)
                .charge("seeker-1", "card-1", 100.00);

        assertThatThrownBy(() ->
                seekerService.topUp("seeker-1", "card-1", 100.00)
        ).isInstanceOf(PaymentService.PaymentTimeoutException.class);

        assertThat(seeker.getBalance()).isEqualTo(0.00);

        verify(payments).charge("seeker-1", "card-1", 100.00);
        verify(seekers, never()).save(any(Seeker.class));
    }
}