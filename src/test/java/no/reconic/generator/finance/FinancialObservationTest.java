package no.reconic.generator.finance;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class FinancialObservationTest {

    @Test
    void calculatesOperatingMarginAndEquityRatio() {
        FinancialObservation observation = observation(
                new BigDecimal("50000000"),
                new BigDecimal("5000000"),
                new BigDecimal("12000000"),
                new BigDecimal("40000000")
        );

        assertEquals(new BigDecimal("10.0"), observation.operatingMarginPercent());
        assertEquals(new BigDecimal("30.0"), observation.equityRatioPercent());
    }

    @Test
    void calculatesRevenuePerEmployee() {
        FinancialObservation observation = observation(
                new BigDecimal("50000000"),
                new BigDecimal("5000000"),
                new BigDecimal("12000000"),
                new BigDecimal("40000000")
        );

        assertEquals(new BigDecimal("1000000"), observation.revenuePerEmployee(50));
        assertNull(observation.revenuePerEmployee(0));
    }

    @Test
    void reportsProfitabilityWithoutInventingData() {
        FinancialObservation positive = observation(
                new BigDecimal("50000000"),
                new BigDecimal("1000000"),
                null,
                null
        );
        FinancialObservation missing = FinancialObservation.notAvailable(
                "999999999",
                "Ingen data"
        );

        assertTrue(positive.hasPositiveOperatingResult());
        assertFalse(positive.hasNegativeOperatingResult());
        assertFalse(missing.hasData());
        assertNull(missing.operatingMarginPercent());
    }

    private FinancialObservation observation(
            BigDecimal revenue,
            BigDecimal operatingResult,
            BigDecimal equity,
            BigDecimal assets
    ) {
        return new FinancialObservation(
                FinancialLookupStatus.SUCCESS,
                "999999999",
                false,
                2025,
                "2025-12-31",
                "NOK",
                revenue,
                operatingResult,
                operatingResult,
                operatingResult,
                equity,
                assets,
                null,
                null,
                null,
                false,
                false,
                null
        );
    }
}
