package com.graminsaathi.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.graminsaathi.dto.request.ApplicantProfile;
import com.graminsaathi.model.User;
import com.graminsaathi.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ProfileServiceTest {

    private ProfileService profileService;
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        profileService = new ProfileService(userRepository, new ObjectMapper());
    }

    @Test
    void getApplicantProfileIsEmptyNotNullWhenNothingSaved() {
        User user = new User();

        ApplicantProfile profile = profileService.getApplicantProfile(user);

        assertNotNull(profile);
        assertNull(profile.getAge());
        assertNull(profile.getCategory());
    }

    @Test
    void getApplicantProfileIsEmptyForANullUser() {
        assertNotNull(profileService.getApplicantProfile(null));
    }

    @Test
    void mergeLetsOverridesWinButKeepsBaseFieldsTheyDontTouch() {
        ApplicantProfile base = ApplicantProfile.builder().age(30).category("general").isSc(false).build();
        ApplicantProfile overrides = ApplicantProfile.builder().age(45).isSc(true).build();

        ApplicantProfile merged = profileService.merge(base, overrides);

        assertEquals(45, merged.getAge()); // overrides wins
        assertEquals("general", merged.getCategory()); // untouched by overrides, kept from base
        assertEquals(Boolean.TRUE, merged.getIsSc()); // overrides wins, including a boolean flip
    }

    @Test
    void mergeWithNullOverridesReturnsBaseUnchanged() {
        ApplicantProfile base = ApplicantProfile.builder().age(30).build();

        assertSame(base, profileService.merge(base, null));
    }

    @Test
    void mergeWithNullBaseReturnsOverrides() {
        ApplicantProfile overrides = ApplicantProfile.builder().age(45).build();

        assertSame(overrides, profileService.merge(null, overrides));
    }

    @Test
    void saveApplicantProfileMergesOntoWhatWasAlreadySavedAndPersists() {
        User user = new User();
        user.setApplicantProfileJson("{\"age\":30,\"category\":\"general\"}");

        ApplicantProfile saved = profileService.saveApplicantProfile(user, ApplicantProfile.builder().age(45).build());

        assertEquals(45, saved.getAge());
        assertEquals("general", saved.getCategory()); // preserved from the earlier save, not wiped
        assertNotNull(user.getApplicantProfileJson());
        assertTrue(user.getApplicantProfileJson().contains("45"));
        verify(userRepository).save(user);
    }
}
