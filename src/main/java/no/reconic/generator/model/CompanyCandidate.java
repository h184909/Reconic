package no.reconic.generator.model;

import no.reconic.generator.dns.DnsObservation;
import no.reconic.generator.domain.DomainCandidate;
import no.reconic.generator.intelligence.TechnologyObservation;
import no.reconic.generator.scoring.OpportunityAssessment;

public record CompanyCandidate(
        String organizationNumber,
        String name,
        int employees,
        IndustrySegment segment,
        String naceCode,
        String naceDescription,
        String municipalityNumber,
        String municipalityName,
        String address,
        String website,
        String email,
        String phone,
        EntityType entityType,
        String parentOrganizationNumber,
        DomainCandidate domainCandidate,
        DnsObservation dnsObservation,
        TechnologyObservation technologyObservation,
        OpportunityAssessment opportunityAssessment
) {
    public CompanyCandidate(
            String organizationNumber,
            String name,
            int employees,
            IndustrySegment segment,
            String naceCode,
            String naceDescription,
            String municipalityNumber,
            String municipalityName,
            String address,
            String website,
            String email,
            String phone,
            EntityType entityType,
            String parentOrganizationNumber,
            DomainCandidate domainCandidate,
            DnsObservation dnsObservation,
            TechnologyObservation technologyObservation
    ) {
        this(
                organizationNumber,
                name,
                employees,
                segment,
                naceCode,
                naceDescription,
                municipalityNumber,
                municipalityName,
                address,
                website,
                email,
                phone,
                entityType,
                parentOrganizationNumber,
                domainCandidate,
                dnsObservation,
                technologyObservation,
                OpportunityAssessment.empty()
        );
    }

    public CompanyCandidate(
            String organizationNumber,
            String name,
            int employees,
            IndustrySegment segment,
            String naceCode,
            String naceDescription,
            String municipalityNumber,
            String municipalityName,
            String address,
            String website,
            String email,
            String phone,
            EntityType entityType,
            String parentOrganizationNumber,
            DomainCandidate domainCandidate,
            DnsObservation dnsObservation
    ) {
        this(
                organizationNumber,
                name,
                employees,
                segment,
                naceCode,
                naceDescription,
                municipalityNumber,
                municipalityName,
                address,
                website,
                email,
                phone,
                entityType,
                parentOrganizationNumber,
                domainCandidate,
                dnsObservation,
                TechnologyObservation.empty(),
                OpportunityAssessment.empty()
        );
    }

    public CompanyCandidate {
        technologyObservation = technologyObservation == null
                ? TechnologyObservation.empty()
                : technologyObservation;
        opportunityAssessment = opportunityAssessment == null
                ? OpportunityAssessment.empty()
                : opportunityAssessment;
    }

    public String websiteUrl() {
        if (website == null || website.isBlank()) {
            return null;
        }

        String trimmed = website.trim();
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed;
        }
        return "https://" + trimmed;
    }

    public String naceDisplay() {
        if (naceCode == null || naceCode.isBlank()) {
            return naceDescription;
        }
        if (naceDescription == null || naceDescription.isBlank()) {
            return naceCode;
        }
        return naceCode + " " + naceDescription;
    }

    public CompanyCandidate withDnsObservation(DnsObservation observation) {
        return new CompanyCandidate(
                organizationNumber,
                name,
                employees,
                segment,
                naceCode,
                naceDescription,
                municipalityNumber,
                municipalityName,
                address,
                website,
                email,
                phone,
                entityType,
                parentOrganizationNumber,
                domainCandidate,
                observation,
                TechnologyObservation.empty(),
                OpportunityAssessment.empty()
        );
    }

    public CompanyCandidate withTechnologyObservation(TechnologyObservation observation) {
        return new CompanyCandidate(
                organizationNumber,
                name,
                employees,
                segment,
                naceCode,
                naceDescription,
                municipalityNumber,
                municipalityName,
                address,
                website,
                email,
                phone,
                entityType,
                parentOrganizationNumber,
                domainCandidate,
                dnsObservation,
                observation,
                OpportunityAssessment.empty()
        );
    }

    public CompanyCandidate withOpportunityAssessment(OpportunityAssessment assessment) {
        return new CompanyCandidate(
                organizationNumber,
                name,
                employees,
                segment,
                naceCode,
                naceDescription,
                municipalityNumber,
                municipalityName,
                address,
                website,
                email,
                phone,
                entityType,
                parentOrganizationNumber,
                domainCandidate,
                dnsObservation,
                technologyObservation,
                assessment
        );
    }
}
