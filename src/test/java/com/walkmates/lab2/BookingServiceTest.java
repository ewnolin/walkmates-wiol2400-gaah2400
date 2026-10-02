package com.walkmates.lab2;

import com.walkmates.model.Booking;
import com.walkmates.model.Listing;
import com.walkmates.model.ListingType;
import com.walkmates.model.Seeker;
import com.walkmates.model.TrustTier;
import com.walkmates.model.Provider;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

import com.walkmates.repository.BookingRepository;
import com.walkmates.repository.ListingRepository;
import com.walkmates.repository.ProviderRepository;
import com.walkmates.repository.SeekerRepository;
import com.walkmates.service.BookingService;
import com.walkmates.service.NotificationService;
import com.walkmates.service.PricingCalculator;
import org.junit.jupiter.api.Test;

class BookingServiceTest {

    private final SeekerRepository seekers = mock(SeekerRepository.class);
    private final ListingRepository listings = mock(ListingRepository.class);
    private final ProviderRepository providers = mock(ProviderRepository.class);
    private final BookingRepository bookings = mock(BookingRepository.class);
    private final PricingCalculator pricing = mock(PricingCalculator.class);
    private final NotificationService notifications = mock(NotificationService.class);

    private final BookingService bookingService = new BookingService(
            seekers,
            listings,
            providers,
            bookings,
            pricing,
            notifications
    );

    @Test
    void rejectsBookingWhenSeekerIsExactlyAtConcurrentBookingLimit() {
        Seeker seeker = new Seeker("new@example.com", "Alex", "0701234567");
        seeker.setTrustTier(TrustTier.NEW);

        Listing listing = new Listing(
                "provider-1",
                "Dog walk",
                "Walk a dog",
                ListingType.DOG_WALK
        );

        Booking activeBooking = new Booking("seeker-1", "existing-listing", 60);

        when(seekers.findById("seeker-1")).thenReturn(Optional.of(seeker));
        when(listings.findById(anyString())).thenReturn(Optional.of(listing));
        when(bookings.findBySeekerId("seeker-1"))
                .thenReturn(List.of(activeBooking));

        Provider provider = new Provider("Provider", 59.33, 18.07);

        when(providers.findById(listing.getProviderId()))
                .thenReturn(Optional.of(provider));

        assertThrows(
                BookingService.BookingRejectedException.class,
                () -> bookingService.createBooking("seeker-1", "listing-1", 60)
        );
    }

    @Test
    void successfulBookingSendsConfirmationNotification() {
        Seeker seeker = new Seeker(
                "verified@example.com",
                "Alex",
                "0701234567"
        );
        seeker.setTrustTier(TrustTier.VERIFIED);
        seeker.addFunds(1000.00);

        Listing listing = new Listing(
                "provider-1",
                "Dog walk",
                "Walk a dog",
                ListingType.DOG_WALK
        );

        Provider provider = new Provider("Provider", 59.33, 18.07);

        when(seekers.findById("seeker-1"))
                .thenReturn(Optional.of(seeker));
        when(listings.findById(anyString()))
                .thenReturn(Optional.of(listing));
        when(bookings.findBySeekerId("seeker-1"))
                .thenReturn(List.of());
        when(providers.findById(listing.getProviderId()))
                .thenReturn(Optional.of(provider));
        when(listings.findByProviderId(listing.getProviderId()))
                .thenReturn(List.of());
        when(pricing.priceFor(any(Booking.class), any(Listing.class), any(Seeker.class)))
                .thenReturn(100.00);

        Booking result = bookingService.createBooking(
                "seeker-1",
                listing.getId(),
                60
        );

        assertEquals(100.00, result.getPrice());

        verify(notifications).sendBookingConfirmed(seeker, result);
    }
}