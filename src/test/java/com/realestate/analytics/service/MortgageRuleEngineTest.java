package com.realestate.analytics.service;

import com.realestate.analytics.model.ApartmentCategory;
import com.realestate.analytics.model.BuyerProfile;
import com.realestate.analytics.model.Property;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MortgageRuleEngineTest {

    private final MortgageRuleEngine engine = new MortgageRuleEngine();

    @Test
    void singleApartmentRequiresQuarterEquity() {
        BuyerProfile buyer = new BuyerProfile(0, 0, ApartmentCategory.SINGLE);
        Property property = new Property(1_500_000, 1_400_000, false);
        assertThat(engine.calculateRequiredEquity(buyer, property)).isEqualTo(350_000.0); // 25% of purchase price
    }

    @Test
    void replacementApartmentRequires30Percent() {
        BuyerProfile buyer = new BuyerProfile(0, 0, ApartmentCategory.REPLACEMENT);
        Property property = new Property(1_500_000, 1_400_000, false);
        assertThat(engine.calculateRequiredEquity(buyer, property)).isEqualTo(420_000.0); // 30% of purchase price
    }

    @Test
    void investmentApartmentRequiresHalfEquity() {
        BuyerProfile buyer = new BuyerProfile(0, 0, ApartmentCategory.INVESTMENT);
        Property property = new Property(1_500_000, 1_400_000, false);
        assertThat(engine.calculateRequiredEquity(buyer, property)).isEqualTo(700_000.0); // 50% of purchase price
    }

    @Test
    void subsidizedApartmentUnderCapUsesMarketValueAsBasis() {
        BuyerProfile buyer = new BuyerProfile(0, 0, ApartmentCategory.SINGLE);
        Property property = new Property(1_500_000, 1_400_000, true); // market value under 1.8M cap
        assertThat(engine.calculateRequiredEquity(buyer, property)).isEqualTo(375_000.0); // 25% of market value
    }

    @Test
    void subsidizedApartmentOverCapUsesPurchasePriceOrCapWhicheverHigher() {
        BuyerProfile buyer = new BuyerProfile(0, 0, ApartmentCategory.SINGLE);
        Property property = new Property(2_500_000, 1_600_000, true); // market value exceeds 1.8M cap
        // basis = max(1.8M, 1.6M) = 1.8M -> required = 25% of 1.8M
        assertThat(engine.calculateRequiredEquity(buyer, property)).isEqualTo(450_000.0);
    }

    @Test
    void minimumCashFloorAppliesWhenLtvRequirementIsLower() {
        BuyerProfile buyer = new BuyerProfile(0, 0, ApartmentCategory.SINGLE);
        Property property = new Property(100_000, 90_000, false); // 25% would be only 22,500
        assertThat(engine.calculateRequiredEquity(buyer, property)).isEqualTo(100_000.0);
    }

    @Test
    void subsidizedMinimumCashFloorIsLower() {
        BuyerProfile buyer = new BuyerProfile(0, 0, ApartmentCategory.SINGLE);
        Property property = new Property(100_000, 90_000, true);
        assertThat(engine.calculateRequiredEquity(buyer, property)).isEqualTo(60_000.0);
    }

    @Test
    void canAffordComparesAvailableEquityToRequirement() {
        BuyerProfile affordable = new BuyerProfile(400_000, 0, ApartmentCategory.SINGLE);
        BuyerProfile notAffordable = new BuyerProfile(300_000, 0, ApartmentCategory.SINGLE);
        Property property = new Property(1_500_000, 1_400_000, false);
        assertThat(engine.canAfford(affordable, property)).isTrue();
        assertThat(engine.canAfford(notAffordable, property)).isFalse();
    }
}
