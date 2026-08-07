package no.reconic.generator.finance;

import no.reconic.generator.model.CompanyCandidate;
import no.reconic.generator.model.EntityType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

@Service
public class FinancialEnrichmentService {

    private static final Logger log = LoggerFactory.getLogger(FinancialEnrichmentService.class);
    private static final String BASE_URL =
            "https://data.brreg.no/regnskapsregisteret/regnskap";

    private final boolean enabled;
    private final boolean onlyWithDomain;
    private final int maxConcurrency;
    private final RestClient restClient;

    /*
     * Annual-account key figures change slowly. Keeping successful/not-available
     * observations in memory avoids repeating the same public lookup when the user
     * runs several market searches in one Reconic session.
     */
    private final ConcurrentHashMap<String, FinancialObservation> cache = new ConcurrentHashMap<>();

    public FinancialEnrichmentService(
            @Value("${reconic.finance.enabled:true}") boolean enabled,
            @Value("${reconic.finance.only-with-domain:true}") boolean onlyWithDomain,
            @Value("${reconic.finance.max-concurrency:6}") int maxConcurrency
    ) {
        this.enabled = enabled;
        this.onlyWithDomain = onlyWithDomain;
        this.maxConcurrency = Math.max(1, Math.min(maxConcurrency, 12));

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(4))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(8));

        this.restClient = RestClient.builder()
                .baseUrl(BASE_URL)
                .requestFactory(requestFactory)
                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("User-Agent", "Reconic/0.7 development")
                .build();
    }

    public List<CompanyCandidate> enrich(List<CompanyCandidate> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }

        if (!enabled) {
            return candidates.stream()
                    .map(candidate -> candidate.withFinancialObservation(
                            FinancialObservation.skipped("Økonomiberikelse er deaktivert.")
                    ))
                    .toList();
        }

        try (var executor = Executors.newFixedThreadPool(maxConcurrency)) {
            List<java.util.concurrent.Future<CompanyCandidate>> futures = new ArrayList<>();
            for (CompanyCandidate candidate : candidates) {
                futures.add(executor.submit(() -> enrichOne(candidate)));
            }

            List<CompanyCandidate> result = new ArrayList<>(futures.size());
            for (int index = 0; index < futures.size(); index++) {
                try {
                    result.add(futures.get(index).get());
                } catch (Exception exception) {
                    CompanyCandidate original = candidates.get(index);
                    log.warn(
                            "Regnskapsberikelse feilet uventet for {}: {}",
                            original.organizationNumber(),
                            exception.getMessage()
                    );
                    result.add(original.withFinancialObservation(
                            FinancialObservation.failed(
                                    lookupOrganizationNumber(original),
                                    "Uventet feil under regnskapsberikelse."
                            )
                    ));
                }
            }
            return List.copyOf(result);
        }
    }

    CompanyCandidate enrichOne(CompanyCandidate candidate) {
        if (candidate == null) {
            return null;
        }

        if (onlyWithDomain
                && (candidate.domainCandidate() == null || !candidate.domainCandidate().hasDomain())) {
            return candidate.withFinancialObservation(
                    FinancialObservation.skipped(
                            "Ikke hentet fordi kandidaten mangler verifiserbart domene. "
                                    + "Dette reduserer unødvendige API-oppslag."
                    )
            );
        }

        String lookupOrganizationNumber = lookupOrganizationNumber(candidate);
        if (lookupOrganizationNumber == null || lookupOrganizationNumber.length() != 9) {
            return candidate.withFinancialObservation(
                    FinancialObservation.notAvailable(
                            lookupOrganizationNumber,
                            "Mangler gyldig organisasjonsnummer for regnskapsoppslag."
                    )
            );
        }

        FinancialObservation observation = cache.computeIfAbsent(
                lookupOrganizationNumber,
                this::lookup
        );

        // Do not keep transient technical failures cached for the whole application session.
        if (observation.status() == FinancialLookupStatus.FAILED) {
            cache.remove(lookupOrganizationNumber, observation);
        }

        boolean parentSource = candidate.entityType() == EntityType.SUBUNIT
                && candidate.parentOrganizationNumber() != null
                && !candidate.parentOrganizationNumber().isBlank()
                && lookupOrganizationNumber.equals(candidate.parentOrganizationNumber());

        return candidate.withFinancialObservation(observation.withSourceIsParent(parentSource));
    }

    private FinancialObservation lookup(String organizationNumber) {
        try {
            Object response = restClient.get()
                    .uri("/{organizationNumber}", organizationNumber)
                    .retrieve()
                    .body(Object.class);

            return parseObservation(response, organizationNumber);
        } catch (RestClientResponseException exception) {
            int status = exception.getStatusCode().value();
            if (status == 404 || status == 204) {
                return FinancialObservation.notAvailable(
                        organizationNumber,
                        "Ingen åpne nøkkeltall fra Regnskapsregisteret."
                );
            }

            log.debug(
                    "Regnskapsregisteret svarte HTTP {} for {}",
                    status,
                    organizationNumber
            );
            return FinancialObservation.failed(
                    organizationNumber,
                    "Regnskapsregisteret svarte HTTP " + status + "."
            );
        } catch (Exception exception) {
            log.debug(
                    "Regnskapsoppslag feilet for {}: {}",
                    organizationNumber,
                    exception.getMessage()
            );
            return FinancialObservation.failed(
                    organizationNumber,
                    "Regnskapsoppslag feilet: " + shortMessage(exception)
            );
        }
    }

    static FinancialObservation parseObservation(Object response, String organizationNumber) {
        if (response == null) {
            return FinancialObservation.notAvailable(
                    organizationNumber,
                    "Ingen åpne nøkkeltall fra Regnskapsregisteret."
            );
        }

        List<Map<?, ?>> accounts = new ArrayList<>();
        collectAccounts(response, accounts);

        if (accounts.isEmpty()) {
            return FinancialObservation.notAvailable(
                    organizationNumber,
                    "Ingen åpne nøkkeltall fra Regnskapsregisteret."
            );
        }

        Map<?, ?> account = accounts.stream()
                .max(Comparator.comparing(
                        FinancialEnrichmentService::periodEndForSort,
                        Comparator.nullsFirst(Comparator.naturalOrder())
                ))
                .orElse(accounts.getFirst());

        String periodEnd = stringAt(account, "regnskapsperiode", "tilDato");
        Integer fiscalYear = yearFromPeriodEnd(periodEnd);

        return new FinancialObservation(
                FinancialLookupStatus.SUCCESS,
                organizationNumber,
                false,
                fiscalYear,
                periodEnd,
                stringAt(account, "valuta"),
                decimalAt(
                        account,
                        "resultatregnskapResultat",
                        "driftsresultat",
                        "driftsinntekter",
                        "sumDriftsinntekter"
                ),
                decimalAt(
                        account,
                        "resultatregnskapResultat",
                        "driftsresultat",
                        "driftsresultat"
                ),
                decimalAt(
                        account,
                        "resultatregnskapResultat",
                        "ordinaertResultatFoerSkattekostnad"
                ),
                decimalAt(account, "resultatregnskapResultat", "aarsresultat"),
                decimalAt(account, "egenkapitalGjeld", "egenkapital", "sumEgenkapital"),
                decimalAt(account, "eiendeler", "sumEiendeler"),
                decimalAt(account, "egenkapitalGjeld", "gjeldOversikt", "sumGjeld"),
                decimalAt(account, "eiendeler", "omloepsmidler", "sumOmloepsmidler"),
                decimalAt(
                        account,
                        "egenkapitalGjeld",
                        "gjeldOversikt",
                        "kortsiktigGjeld",
                        "sumKortsiktigGjeld"
                ),
                booleanAt(account, "revisjon", "ikkeRevidertAarsregnskap"),
                booleanAt(account, "revisjon", "fravalgRevisjon"),
                null
        );
    }

    private static void collectAccounts(Object node, List<Map<?, ?>> accounts) {
        if (node instanceof Map<?, ?> map) {
            if (map.containsKey("resultatregnskapResultat")
                    && map.containsKey("regnskapsperiode")) {
                accounts.add(map);
            }

            for (Object value : map.values()) {
                collectAccounts(value, accounts);
            }
            return;
        }

        if (node instanceof Collection<?> collection) {
            for (Object value : collection) {
                collectAccounts(value, accounts);
            }
        }
    }

    private String lookupOrganizationNumber(CompanyCandidate candidate) {
        if (candidate == null) {
            return null;
        }
        if (candidate.entityType() == EntityType.SUBUNIT
                && candidate.parentOrganizationNumber() != null
                && !candidate.parentOrganizationNumber().isBlank()) {
            return candidate.parentOrganizationNumber().trim();
        }
        return candidate.organizationNumber() == null
                ? null
                : candidate.organizationNumber().trim();
    }

    private static LocalDate periodEndForSort(Map<?, ?> account) {
        String value = stringAt(account, "regnskapsperiode", "tilDato");
        if (value == null) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Integer yearFromPeriodEnd(String periodEnd) {
        if (periodEnd == null || periodEnd.length() < 4) {
            return null;
        }
        try {
            return Integer.valueOf(periodEnd.substring(0, 4));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static Object valueAt(Map<?, ?> map, String... path) {
        Object current = map;
        for (String key : path) {
            if (!(current instanceof Map<?, ?> currentMap)) {
                return null;
            }
            current = currentMap.get(key);
            if (current == null) {
                return null;
            }
        }
        return current;
    }

    private static String stringAt(Map<?, ?> map, String... path) {
        Object value = valueAt(map, path);
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    private static BigDecimal decimalAt(Map<?, ?> map, String... path) {
        Object value = valueAt(map, path);
        if (value == null) {
            return null;
        }
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static Boolean booleanAt(Map<?, ?> map, String... path) {
        Object value = valueAt(map, path);
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value);
        if ("true".equalsIgnoreCase(text)) {
            return true;
        }
        if ("false".equalsIgnoreCase(text)) {
            return false;
        }
        return null;
    }

    private static String shortMessage(Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }
        return message.length() <= 180 ? message : message.substring(0, 180);
    }
}
