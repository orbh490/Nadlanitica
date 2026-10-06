package com.realestate.analytics.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PeripheryGrantCitiesTest {

    private final PeripheryGrantCities cities = new PeripheryGrantCities();

    @Test
    void recognizesKnownPeripheryCity() {
        assertThat(cities.isEligible("שדרות")).isTrue();
        assertThat(cities.isEligible("דימונה")).isTrue();
    }

    @Test
    void doesNotFlagCentralCities() {
        assertThat(cities.isEligible("תל אביב")).isFalse();
        assertThat(cities.isEligible("רמת גן")).isFalse();
    }

    @Test
    void handlesNullSafely() {
        assertThat(cities.isEligible(null)).isFalse();
    }
}
