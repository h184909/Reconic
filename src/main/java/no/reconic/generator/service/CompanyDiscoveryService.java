package no.reconic.generator.service;

import no.reconic.generator.brreg.BrregClient;
import no.reconic.generator.brreg.dto.BrregAddressDto;
import no.reconic.generator.brreg.dto.BrregCompanyDto;
import no.reconic.generator.brreg.dto.BrregIndustryCodeDto;
import no.reconic.generator.dns.DnsEnrichmentService;
import no.reconic.generator.dns.DnsLookupStatus;
import no.reconic.generator.dns.DnsObservation;
import no.reconic.generator.domain.DomainCandidate;
import no.reconic.generator.domain.DomainConfidence;
import no.reconic.generator.domain.DomainDiscoveryService;
import no.reconic.generator.domain.DomainSource;
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
    private final DomainDiscoveryService domainDiscoveryService;
    private final DnsEnrichmentService dnsEnrichmentService;

    public CompanyDiscoveryService(
            BrregClient brregClient,
            DomainDiscoveryService domainDiscoveryService,
            DnsEnrichmentService dnsEnrichmentService
    ) {
        this.brregClient = brregClient;
        this.domainDiscoveryService = domainDiscoveryService;
        this.dnsEnrichmentService = dnsEnrichmentService;
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

        List<CompanyCandidate> sortedCandidates = uniqueCandidates.values().stream()
                .sorted(Comparator.comparingInt(CompanyCandidate::employees)
                        .reversed()
                        .thenComparing(CompanyCandidate::name, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();

        List<CompanyCandidate> candidates = dnsEnrichmentService.enrich(sortedCandidates);

        Map<IndustrySegment, Long> segmentCounts = candidates.stream()
                .collect(Collectors.groupingBy(
                        CompanyCandidate::segment,
                        () -> new EnumMap<>(IndustrySegment.class),
                        Collectors.counting()
                ));

        int domainCount = (int) candidates.stream()
                .map(CompanyCandidate::domainCandidate)
                .filter(Objects::nonNull)
                .filter(DomainCandidate::hasDomain)
                .count();
        int websiteDomainCount = countBySource(candidates, DomainSource.REGISTERED_WEBSITE);
        int emailDomainCount = countBySource(candidates, DomainSource.REGISTERED_EMAIL);
        int highConfidenceCount = countByConfidence(candidates, DomainConfidence.HIGH);
        int mediumConfidenceCount = countByConfidence(candidates, DomainConfidence.MEDIUM);

        int dnsAttemptedCount = countDnsStatus(candidates, DnsLookupStatus.SUCCESS)
                + countDnsStatus(candidates, DnsLookupStatus.PARTIAL)
                + countDnsStatus(candidates, DnsLookupStatus.FAILED);
        int dnsSuccessCount = countDnsStatus(candidates, DnsLookupStatus.SUCCESS);
        int dnsPartialCount = countDnsStatus(candidates, DnsLookupStatus.PARTIAL);
        int dnsFailureCount = countDnsStatus(candidates, DnsLookupStatus.FAILED);
        int mxCount = countDnsPresence(candidates, DnsObservation::hasMx);
        int spfCount = countDnsPresence(candidates, DnsObservation::hasSpf);
        int dmarcCount = countDnsPresence(candidates, DnsObservation::hasDmarc);
        int nameServerCount = countDnsPresence(candidates, DnsObservation::hasNameServers);

        int fetchedCount = fetchedCompanies.size();
        log.info(
                "Kandidatsøk ferdig: {} rå treff, {} kandidater, {} domener, DNS: {} ok / {} delvis / {} feilet",
                fetchedCount,
                candidates.size(),
                domainCount,
                dnsSuccessCount,
                dnsPartialCount,
                dnsFailureCount
        );

        return new CompanyDiscoveryResult(
                Instant.now(),
                fetchedCount,
                candidates.size(),
                fetchedCount - candidates.size(),
                candidates,
                segmentCounts,
                domainCount,
                candidates.size() - domainCount,
                websiteDomainCount,
                emailDomainCount,
                highConfidenceCount,
                mediumConfidenceCount,
                dnsAttemptedCount,
                dnsSuccessCount,
                dnsPartialCount,
                dnsFailureCount,
                mxCount,
                spfCount,
                dmarcCount,
                nameServerCount
        );
    }

    private int countBySource(List<CompanyCandidate> candidates, DomainSource source) {
        return (int) candidates.stream()
                .map(CompanyCandidate::domainCandidate)
                .filter(Objects::nonNull)
                .filter(candidate -> candidate.source() == source)
                .count();
    }

    private int countByConfidence(List<CompanyCandidate> candidates, DomainConfidence confidence) {
        return (int) candidates.stream()
                .map(CompanyCandidate::domainCandidate)
                .filter(Objects::nonNull)
                .filter(candidate -> candidate.confidence() == confidence)
                .count();
    }

    private int countDnsStatus(List<CompanyCandidate> candidates, DnsLookupStatus status) {
        return (int) candidates.stream()
                .map(CompanyCandidate::dnsObservation)
                .filter(Objects::nonNull)
                .filter(observation -> observation.status() == status)
                .count();
    }

    private int countDnsPresence(
            List<CompanyCandidate> candidates,
            java.util.function.Predicate<DnsObservation> predicate
    ) {
        return (int) candidates.stream()
                .map(CompanyCandidate::dnsObservation)
                .filter(Objects::nonNull)
                .filter(predicate)
                .count();
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
        DomainCandidate domainCandidate = domainDiscoveryService.discover(
                company.hjemmeside(),
                company.epostadresse()
        );

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
                company.overordnetEnhet(),
                domainCandidate,
                DnsObservation.skipped(domainCandidate.domain())
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
