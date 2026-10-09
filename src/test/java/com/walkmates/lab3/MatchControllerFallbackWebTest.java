package com.walkmates.lab3;

import com.walkmates.model.Listing;
import com.walkmates.model.ListingType;
import com.walkmates.model.Seeker;
import com.walkmates.repository.ListingRepository;
import com.walkmates.repository.SeekerRepository;
import com.walkmates.service.ai.LlmClient;
import com.walkmates.service.ai.MatchExplanationService;
import com.walkmates.web.MatchController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Lab 3, Activity 5.5 extension — the HTTP fallback path.
 *
 * <p>Here the <em>real</em> {@link MatchExplanationService} is wired into the controller and only
 * the {@link LlmClient} behind it is mocked. That lets us check the end-to-end promise of FR-5.2
 * at the interface: when the model fails, times out or returns nothing, the endpoint still
 * answers 200 with the deterministic fallback text and never exposes an error to the caller.</p>
 */
@WebMvcTest(MatchController.class)
@Import(MatchExplanationService.class)
class MatchControllerFallbackWebTest {

    private static final String FALLBACK =
            "This DOG_WALK opportunity \"Walk Rex\" is a good fit for a NEW seeker.";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private SeekerRepository seekers;
    @MockitoBean
    private ListingRepository listings;
    @MockitoBean
    private LlmClient llm;

    @BeforeEach
    void seekerAndListingExist() {
        Seeker seeker = new Seeker("p@example.com", "Pat", "0701112233");
        Listing listing = new Listing("provider-1", "Walk Rex", "Friendly dog", ListingType.DOG_WALK);
        when(seekers.findById("s1")).thenReturn(Optional.of(seeker));
        when(listings.findById("l1")).thenReturn(Optional.of(listing));
    }

    @Test
    @DisplayName("explain returns 200 with the fallback text when the LLM call fails")
    void fallbackWhenTheModelFails() throws Exception {
        when(llm.complete(anyString())).thenThrow(new LlmClient.LlmException("provider down"));

        mvc.perform(get("/api/match/s1/explain").param("listingId", "l1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.explanation").value(FALLBACK));
    }

    @Test
    @DisplayName("explain returns 200 with the fallback text when the LLM call times out")
    void fallbackWhenTheModelTimesOut() throws Exception {
        when(llm.complete(anyString())).thenThrow(new LlmClient.LlmTimeoutException("took too long"));

        mvc.perform(get("/api/match/s1/explain").param("listingId", "l1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.explanation").value(FALLBACK));
    }

    @Test
    @DisplayName("explain returns 200 with the fallback text when the LLM answers with a blank text")
    void fallbackWhenTheModelAnswersBlank() throws Exception {
        when(llm.complete(anyString())).thenReturn("   ");

        mvc.perform(get("/api/match/s1/explain").param("listingId", "l1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.explanation").value(FALLBACK));
    }

    @Test
    @DisplayName("explain returns the model's answer when the LLM call succeeds")
    void modelAnswerIsPassedThrough() throws Exception {
        when(llm.complete(anyString())).thenReturn("A calm walk suits you.");

        mvc.perform(get("/api/match/s1/explain").param("listingId", "l1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.explanation").value("A calm walk suits you."));
    }
}
