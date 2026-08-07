package no.reconic.generator.finance;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FinancialEnrichmentServiceTest {

    @Test
    void parsesOfficialAnnualAccountKeyFigures() {
        Map<String, Object> account = account(
                "2025-12-31",
                "NOK",
                50_000_000L,
                5_000_000L,
                4_200_000L
        );

        FinancialObservation observation =
                FinancialEnrichmentService.parseObservation(account, "999999999");

        assertEquals(FinancialLookupStatus.SUCCESS, observation.status());
        assertEquals(2025, observation.fiscalYear());
        assertEquals("NOK", observation.currency());
        assertEquals(new BigDecimal("50000000"), observation.operatingRevenue());
        assertEquals(new BigDecimal("5000000"), observation.operatingResult());
        assertEquals(new BigDecimal("4200000"), observation.annualResult());
    }

    @Test
    void selectsLatestAccountWhenResponseContainsSeveralYears() {
        Map<String, Object> older = account(
                "2024-12-31",
                "NOK",
                30_000_000L,
                1_000_000L,
                800_000L
        );
        Map<String, Object> newer = account(
                "2025-12-31",
                "NOK",
                40_000_000L,
                2_000_000L,
                1_500_000L
        );

        Map<String, Object> response = Map.of("data", List.of(older, newer));

        FinancialObservation observation =
                FinancialEnrichmentService.parseObservation(response, "999999999");

        assertEquals(2025, observation.fiscalYear());
        assertEquals(new BigDecimal("40000000"), observation.operatingRevenue());
    }

    @Test
    void returnsNotAvailableForUnexpectedOrEmptyResponse() {
        FinancialObservation observation =
                FinancialEnrichmentService.parseObservation(
                        Map.of("data", List.of()),
                        "999999999"
                );

        assertEquals(FinancialLookupStatus.NOT_AVAILABLE, observation.status());
        assertFalse(observation.hasData());
    }

    private Map<String, Object> account(
            String periodEnd,
            String currency,
            long revenue,
            long operatingResult,
            long annualResult
    ) {
        return Map.of(
                "regnskapsperiode", Map.of(
                        "fraDato", periodEnd.substring(0, 4) + "-01-01",
                        "tilDato", periodEnd
                ),
                "valuta", currency,
                "resultatregnskapResultat", Map.of(
                        "aarsresultat", annualResult,
                        "ordinaertResultatFoerSkattekostnad", annualResult,
                        "driftsresultat", Map.of(
                                "driftsresultat", operatingResult,
                                "driftsinntekter", Map.of(
                                        "sumDriftsinntekter", revenue
                                )
                        )
                ),
                "egenkapitalGjeld", Map.of(
                        "egenkapital", Map.of("sumEgenkapital", 10_000_000L),
                        "gjeldOversikt", Map.of(
                                "sumGjeld", 15_000_000L,
                                "kortsiktigGjeld", Map.of(
                                        "sumKortsiktigGjeld", 5_000_000L
                                )
                        )
                ),
                "eiendeler", Map.of(
                        "sumEiendeler", 25_000_000L,
                        "omloepsmidler", Map.of(
                                "sumOmloepsmidler", 8_000_000L
                        )
                ),
                "revisjon", Map.of(
                        "fravalgRevisjon", false,
                        "ikkeRevidertAarsregnskap", false
                )
        );
    }
}
