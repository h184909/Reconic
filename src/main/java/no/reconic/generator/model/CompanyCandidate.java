package no.reconic.generator.model;

import no.reconic.generator.domain.DomainCandidate;

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
        DomainCandidate domainCandidate
) {
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
}
