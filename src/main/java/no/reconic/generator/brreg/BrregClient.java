package no.reconic.generator.brreg;

import no.reconic.generator.brreg.dto.BrregCompanyDto;
import no.reconic.generator.brreg.dto.BrregEmbeddedDto;
import no.reconic.generator.brreg.dto.BrregPageDto;
import no.reconic.generator.brreg.dto.BrregResponseDto;
import no.reconic.generator.model.Municipality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class BrregClient {

    private static final Logger log = LoggerFactory.getLogger(BrregClient.class);
    private static final String BASE_URL = "https://data.brreg.no/enhetsregisteret/api";
    private static final int PAGE_SIZE = 1_000;
    private static final int MAX_PAGES_PER_MUNICIPALITY = 100;

    private final RestClient restClient;

    public BrregClient() {
        this.restClient = RestClient.builder()
                .baseUrl(BASE_URL)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.USER_AGENT, "Reconic/0.2 development")
                .build();
    }

    public List<BrregCompanyDto> fetchMainUnits(
            Collection<Municipality> municipalities,
            int minEmployees,
            int maxEmployees
    ) {
        return fetchForMunicipalities("enheter", municipalities, minEmployees, maxEmployees, true);
    }

    public List<BrregCompanyDto> fetchSubunits(
            Collection<Municipality> municipalities,
            int minEmployees,
            int maxEmployees
    ) {
        return fetchForMunicipalities("underenheter", municipalities, minEmployees, maxEmployees, false);
    }

    private List<BrregCompanyDto> fetchForMunicipalities(
            String endpoint,
            Collection<Municipality> municipalities,
            int minEmployees,
            int maxEmployees,
            boolean mainUnits
    ) {
        Map<String, BrregCompanyDto> uniqueCompanies = new LinkedHashMap<>();

        for (Municipality municipality : municipalities) {
            log.info("Henter {} for {} ({}–{} ansatte)", endpoint, municipality.getDisplayName(), minEmployees, maxEmployees);
            List<BrregCompanyDto> companies = fetchAllPages(
                    endpoint,
                    municipality,
                    minEmployees,
                    maxEmployees,
                    mainUnits
            );

            for (BrregCompanyDto company : companies) {
                if (company != null && company.organisasjonsnummer() != null) {
                    uniqueCompanies.putIfAbsent(company.organisasjonsnummer(), company);
                }
            }
        }

        return List.copyOf(uniqueCompanies.values());
    }

    private List<BrregCompanyDto> fetchAllPages(
            String endpoint,
            Municipality municipality,
            int minEmployees,
            int maxEmployees,
            boolean mainUnits
    ) {
        List<BrregCompanyDto> companies = new ArrayList<>();
        int pageNumber = 0;
        int totalPages = 1;

        do {
            BrregResponseDto response = fetchPage(
                    endpoint,
                    municipality,
                    minEmployees,
                    maxEmployees,
                    mainUnits,
                    pageNumber
            );

            List<BrregCompanyDto> pageCompanies = extractCompanies(response, mainUnits);
            companies.addAll(pageCompanies);
            totalPages = extractTotalPages(response);
            log.debug("Brreg {} / {} side {} av {}: {} treff", endpoint, municipality.getDisplayName(), pageNumber + 1, totalPages, pageCompanies.size());
            pageNumber++;
        } while (pageNumber < totalPages && pageNumber < MAX_PAGES_PER_MUNICIPALITY);

        if (pageNumber >= MAX_PAGES_PER_MUNICIPALITY && pageNumber < totalPages) {
            throw new BrregClientException(
                    "Brønnøysund-søket returnerte flere sider enn sikkerhetsgrensen for "
                            + municipality.getDisplayName() + ". Begrens søket før du prøver igjen."
            );
        }

        return companies;
    }

    private BrregResponseDto fetchPage(
            String endpoint,
            Municipality municipality,
            int minEmployees,
            int maxEmployees,
            boolean mainUnits,
            int pageNumber
    ) {
        try {
            return restClient.get()
                    .uri(uriBuilder -> {
                        var builder = uriBuilder
                                .path("/" + endpoint)
                                .queryParam("kommunenummer", municipality.getNumber())
                                .queryParam("fraAntallAnsatte", minEmployees)
                                .queryParam("tilAntallAnsatte", maxEmployees)
                                .queryParam("size", PAGE_SIZE)
                                .queryParam("page", pageNumber)
                                .queryParam("sort", "organisasjonsnummer,ASC");

                        if (mainUnits) {
                            builder.queryParam("organisasjonsform", "AS");
                        }

                        return builder.build();
                    })
                    .retrieve()
                    .body(BrregResponseDto.class);
        } catch (RestClientResponseException exception) {
            throw new BrregClientException(
                    "Brønnøysund svarte med HTTP " + exception.getStatusCode().value()
                            + " for " + municipality.getDisplayName() + ".",
                    exception
            );
        } catch (ResourceAccessException exception) {
            throw new BrregClientException(
                    "Kunne ikke koble til Brønnøysundregistrene. Kontroller internettforbindelsen og prøv igjen.",
                    exception
            );
        } catch (RuntimeException exception) {
            throw new BrregClientException(
                    "Uventet feil under henting fra Brønnøysundregistrene.",
                    exception
            );
        }
    }

    private List<BrregCompanyDto> extractCompanies(BrregResponseDto response, boolean mainUnits) {
        if (response == null) {
            return List.of();
        }

        BrregEmbeddedDto embedded = response.embedded();
        if (embedded == null) {
            return List.of();
        }

        List<BrregCompanyDto> companies = mainUnits ? embedded.enheter() : embedded.underenheter();
        return companies == null ? List.of() : companies;
    }

    private int extractTotalPages(BrregResponseDto response) {
        if (response == null) {
            return 1;
        }

        BrregPageDto page = response.page();
        if (page == null || page.totalPages() == null || page.totalPages() < 1) {
            return 1;
        }

        return page.totalPages();
    }
}
