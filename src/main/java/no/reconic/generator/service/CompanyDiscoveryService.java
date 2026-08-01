package no.reconic.generator.service;

import no.reconic.generator.brreg.BrregClient;
import no.reconic.generator.brreg.dto.BrregAddressDto;
import no.reconic.generator.brreg.dto.BrregCompanyDto;
import no.reconic.generator.brreg.dto.BrregIndustryCodeDto;
import no.reconic.generator.model.CompanyCandidate;
import no.reconic.generator.model.CompanyDiscoveryResult;
import no.reconic.generator.model.EntityType;
import no.reconic.generator.model.IndustrySegment;
import no.reconic.generator.model.Municipality;
import no.reconic.generator.web.LeadSearchForm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CompanyDiscoveryService {

    private static final Logger log = LoggerFactory.getLogger(CompanyDiscoveryService.class);

    private final BrregClient brregClient;

    public CompanyDiscoveryService(BrregClient brregClient) {
        this.brregClient = brregClient;
    }

    public CompanyDiscoveryResult discover(LeadSearchForm form) {
        Set<Municipality> municipalities = parseMunicipalities(form.getMunicipalityNumbers());
        Set<IndustrySegment> selectedSegments = parseSegments(form.getSegmentNames());

        List<TypedCompany> fetchedCompanies = new ArrayList<>();
        brregClient.fetchMainUnits(municipalities, form.getMinEmployees(), form.getMaxEmployees())
                .forEach(company -> fetchedCompanies.add(new TypedCompany(company, EntityType.MAIN_UNIT)));

        if (form.isIncludeSubunits()) {
            brregClient.fetchSubunits(municipalities, form.getMinEmployees(), form.getMaxEmployees())
                    .forEach(company -> fetchedCompanies.add(new TypedCompany(company, EntityType.SUBUNIT)));
        }

        Map<String, CompanyCandidate> uniqueCandidates = new LinkedHashMap<>();

        for (TypedCompany typedCompany : fetchedCompanies) {
            BrregCompanyDto company = typedCompany.company();

            if (!isActive(company)) {
                continue;
            }

            IndustrySegment segment = findSegment(company);
            if (segment == null || !selectedSegments.contains(segment)) {
                continue;
            }

            CompanyCandidate candidate = toCandidate(company, typedCompany.entityType(), segment);
            if (candidate.organizationNumber() != null) {
                uniqueCandidates.putIfAbsent(candidate.organizationNumber(), candidate);
            }
        }

        List<CompanyCandidate> candidates = uniqueCandidates.values().stream()
                .sorted(Comparator.comparingInt(CompanyCandidate::employees)
                        .reversed()
                        .thenComparing(CompanyCandidate::name, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();

        Map<IndustrySegment, Long> segmentCounts = candidates.stream()
                .collect(Collectors.groupingBy(
                        CompanyCandidate::segment,
                        () -> new EnumMap<>(IndustrySegment.class),
                        Collectors.counting()
                ));

        int fetchedCount = fetchedCompanies.size();
        log.info("Kandidatsøk ferdig: {} rå treff, {} kandidater", fetchedCount, candidates.size());
        return new CompanyDiscoveryResult(
                Instant.now(),
                fetchedCount,
                candidates.size(),
                fetchedCount - candidates.size(),
                candidates,
                segmentCounts
        );
    }

    private Set<Municipality> parseMunicipalities(List<String> municipalityNumbers) {
        return municipalityNumbers.stream()
                .map(Municipality::fromNumber)
                .flatMap(java.util.Optional::stream)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<IndustrySegment> parseSegments(List<String> segmentNames) {
        return segmentNames.stream()
                .map(this::parseSegment)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private IndustrySegment parseSegment(String name) {
        try {
            return IndustrySegment.valueOf(name);
        } catch (IllegalArgumentException | NullPointerException ignored) {
            return null;
        }
    }

    private boolean isActive(BrregCompanyDto company) {
        return !Boolean.TRUE.equals(company.konkurs())
                && !Boolean.TRUE.equals(company.underAvvikling())
                && (company.slettedato() == null || company.slettedato().isBlank());
    }

    private IndustrySegment findSegment(BrregCompanyDto company) {
        BrregIndustryCodeDto industryCode = company.naeringskode1();
        if (industryCode == null) {
            return null;
        }
        return IndustrySegment.fromNaceCode(industryCode.kode()).orElse(null);
    }

    private CompanyCandidate toCandidate(
            BrregCompanyDto company,
            EntityType entityType,
            IndustrySegment segment
    ) {
        BrregAddressDto address = selectAddress(company, entityType);
        BrregIndustryCodeDto industryCode = company.naeringskode1();

        return new CompanyCandidate(
                company.organisasjonsnummer(),
                company.navn(),
                company.antallAnsatte() == null ? 0 : company.antallAnsatte(),
                segment,
                industryCode == null ? null : industryCode.kode(),
                industryCode == null ? null : industryCode.beskrivelse(),
                address == null ? null : address.kommunenummer(),
                address == null ? null : address.kommune(),
                formatAddress(address),
                company.hjemmeside(),
                company.epostadresse(),
                company.telefon(),
                entityType,
                company.overordnetEnhet()
        );
    }

    private BrregAddressDto selectAddress(BrregCompanyDto company, EntityType entityType) {
        if (entityType == EntityType.SUBUNIT && company.beliggenhetsadresse() != null) {
            return company.beliggenhetsadresse();
        }
        if (company.forretningsadresse() != null) {
            return company.forretningsadresse();
        }
        return company.beliggenhetsadresse();
    }

    private String formatAddress(BrregAddressDto address) {
        if (address == null) {
            return null;
        }

        List<String> parts = new ArrayList<>();
        if (address.adresse() != null) {
            address.adresse().stream()
                    .filter(value -> value != null && !value.isBlank())
                    .forEach(parts::add);
        }

        String postalLine = java.util.stream.Stream.of(address.postnummer(), address.poststed())
                .filter(value -> value != null && !value.isBlank())
                .collect(Collectors.joining(" "));

        if (!postalLine.isBlank()) {
            parts.add(postalLine);
        }

        return parts.isEmpty() ? null : String.join(", ", parts);
    }

    private record TypedCompany(BrregCompanyDto company, EntityType entityType) {
    }
}
