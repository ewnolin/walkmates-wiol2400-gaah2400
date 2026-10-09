package com.walkmates.lab3;

import com.walkmates.model.Listing;
import com.walkmates.model.ListingStatus;
import com.walkmates.model.ListingType;
import com.walkmates.model.Seeker;
import com.walkmates.repository.ListingRepository;
import com.walkmates.repository.SeekerRepository;
import com.walkmates.service.ai.MatchExplanationService;
import com.walkmates.web.MatchController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Lab 3, Part A (interface rung) — testing the AI feature through its HTTP boundary with
 * {@code MockMvc}, with the service/repositories mocked. This is the "test the interface, not a
 * live model" example. One worked test is provided (the 404 path); extend it to the success and
 * fallback paths.
 */
@WebMvcTest(MatchController.class)
class MatchControllerWebTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private SeekerRepository seekers;
    @MockitoBean
    private ListingRepository listings;
    @MockitoBean
    private MatchExplanationService matchExplanation;

    private Seeker seeker() {
        return new Seeker("p@example.com", "Pat", "0701112233");
    }

    private Listing listing() {
        return new Listing("provider-1", "Walk Rex", "Friendly dog", ListingType.DOG_WALK);
    }

    @Test
    @DisplayName("GET explain returns 404 when the seeker does not exist")
    void explainReturns404WhenSeekerMissing() throws Exception {
        when(seekers.findById("missing")).thenReturn(Optional.empty());
        when(listings.findById("l1")).thenReturn(Optional.empty());

        mvc.perform(get("/api/match/missing/explain").param("listingId", "l1"))
                .andExpect(status().isNotFound());

        verifyNoInteractions(matchExplanation);
    }

    @Test
    @DisplayName("GET explain returns 404 when the seeker exists but the listing does not")
    void explainReturns404WhenListingMissing() throws Exception {
        when(seekers.findById("s1")).thenReturn(Optional.of(seeker()));
        when(listings.findById("missing")).thenReturn(Optional.empty());

        mvc.perform(get("/api/match/s1/explain").param("listingId", "missing"))
                .andExpect(status().isNotFound());

        verifyNoInteractions(matchExplanation);
    }

    @Test
    @DisplayName("GET explain returns 200 and a JSON body with the explanation")
    void explainReturns200WithJsonBody() throws Exception {
        Seeker seeker = seeker();
        Listing listing = listing();
        when(seekers.findById("s1")).thenReturn(Optional.of(seeker));
        when(listings.findById("l1")).thenReturn(Optional.of(listing));
        when(matchExplanation.explainMatch(seeker, listing)).thenReturn("A calm walk suits you.");

        mvc.perform(get("/api/match/s1/explain").param("listingId", "l1"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.seekerId").value("s1"))
                .andExpect(jsonPath("$.listingId").value("l1"))
                .andExpect(jsonPath("$.explanation").value("A calm walk suits you."));
    }

    @Test
    @DisplayName("GET explain still returns 200 when the service answers with its fallback text")
    void explainReturns200WhenServiceReturnsFallbackText() throws Exception {
        Seeker seeker = seeker();
        Listing listing = listing();
        String fallback = "This DOG_WALK opportunity \"Walk Rex\" is a good fit for a NEW seeker.";
        when(seekers.findById("s1")).thenReturn(Optional.of(seeker));
        when(listings.findById("l1")).thenReturn(Optional.of(listing));
        when(matchExplanation.explainMatch(seeker, listing)).thenReturn(fallback);

        mvc.perform(get("/api/match/s1/explain").param("listingId", "l1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.explanation").value(fallback));
    }

    @Test
    @DisplayName("GET explain without the listingId parameter is a bad request")
    void explainRequiresTheListingIdParameter() throws Exception {
        mvc.perform(get("/api/match/s1/explain"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(matchExplanation);
    }

    @Test
    @DisplayName("GET best returns 200 with the recommended listing and its explanation")
    void bestReturns200WithTheRecommendedListing() throws Exception {
        Seeker seeker = seeker();
        Listing listing = listing();
        when(seekers.findById("s1")).thenReturn(Optional.of(seeker));
        when(listings.findAll()).thenReturn(List.of(listing));
        when(matchExplanation.recommendBestMatch(eq(seeker), any())).thenReturn(listing);
        when(matchExplanation.explainMatch(seeker, listing)).thenReturn("A calm walk suits you.");

        mvc.perform(get("/api/match/s1/best"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.seekerId").value("s1"))
                .andExpect(jsonPath("$.listingId").value(listing.getId()))
                .andExpect(jsonPath("$.explanation").value("A calm walk suits you."));
    }

    @Test
    @DisplayName("GET best only offers available listings to the recommender")
    void bestPassesOnlyAvailableListingsToTheService() throws Exception {
        Seeker seeker = seeker();
        Listing available = listing();
        Listing booked = new Listing("provider-1", "Booked walk", "Taken", ListingType.DOG_WALK);
        booked.transitionTo(ListingStatus.BOOKED);
        when(seekers.findById("s1")).thenReturn(Optional.of(seeker));
        when(listings.findAll()).thenReturn(List.of(available, booked));

        mvc.perform(get("/api/match/s1/best"));

        verify(matchExplanation).recommendBestMatch(eq(seeker), eq(List.of(available)));
    }

    @Test
    @DisplayName("GET best returns 204 when there is no listing to recommend")
    void bestReturns204WhenThereIsNoCandidate() throws Exception {
        Seeker seeker = seeker();
        when(seekers.findById("s1")).thenReturn(Optional.of(seeker));
        when(listings.findAll()).thenReturn(List.of());
        when(matchExplanation.recommendBestMatch(eq(seeker), any())).thenReturn(null);

        mvc.perform(get("/api/match/s1/best"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("GET best returns 404 when the seeker does not exist")
    void bestReturns404WhenSeekerMissing() throws Exception {
        when(seekers.findById("missing")).thenReturn(Optional.empty());

        mvc.perform(get("/api/match/missing/best"))
                .andExpect(status().isNotFound());

        verifyNoInteractions(matchExplanation);
    }
}
