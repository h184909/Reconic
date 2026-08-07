package no.reconic.generator.export;

import no.reconic.generator.dns.DnsObservation;
import no.reconic.generator.domain.DomainCandidate;
import no.reconic.generator.finance.FinancialObservation;
import no.reconic.generator.intelligence.ProviderSignal;
import no.reconic.generator.intelligence.PublicInfrastructureObservation;
import no.reconic.generator.intelligence.TechnologyObservation;
import no.reconic.generator.model.CompanyCandidate;
import no.reconic.generator.model.CompanyDiscoveryResult;
import no.reconic.generator.scoring.OpportunityAssessment;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class CsvExportService {

    private static final char DELIMITER = ';';
    private static final String LINE_ENDING = "\r\n";
    private static final byte[] UTF8_BOM = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

    private final ValidationSampleService validationSampleService;

    public CsvExportService(ValidationSampleService validationSampleService) {
        this.validationSampleService = validationSampleService;
    }

    public byte[] exportAll(CompanyDiscoveryResult result) {
        return write(result == null ? List.of() : result.candidates());
    }

    public byte[] exportValidationSample(CompanyDiscoveryResult result) {
        List<CompanyCandidate> candidates = result == null
                ? List.of()
                : validationSampleService.createSample(result.candidates());
        return write(candidates);
    }

    private byte[] write(List<CompanyCandidate> candidates) {
        StringBuilder csv = new StringBuilder();
        appendRow(csv, List.of(
                "organizationNumber",
                "companyName",
                "employees",
                "segment",
                "naceCode",
                "naceDescription",
                "municipalityNumber",
                "municipalityName",
                "address",
                "registeredWebsite",
                "registeredEmail",
                "phone",
                "entityType",
                "parentOrganizationNumber",
                "generatedDomain",
                "domainSource",
                "domainConfidence",
                "requiresDomainVerification",
                "domainExplanation",
                "dnsStatus",
                "mxRecords",
                "spfRecords",
                "dmarcRecord",
                "dmarcPolicy",
                "nameServers",
                "dnsErrors",
                "emailPlatform",
                "emailPlatformConfidence",
                "emailGateway",
                "emailGatewayConfidence",
                "dmarcPosture",
                "spfPosture",
                "spfAllMechanism",
                "spfRedirectTarget",
                "spfSignals",
                "providerSignals",
                "providerRoles",
                "providerEvidence",
                "technologyEvidence",
                "mtaStsStatus",
                "mtaStsRecord",
                "tlsRptStatus",
                "tlsRptRecord",
                "dnssecStatus",
                "autodiscoverTarget",
                "certificateTransparencyStatus",
                "certificateNames",
                "publicLookupWarnings",
                "publicInfrastructureEvidence",
                "financialStatus",
                "financialSourceOrganizationNumber",
                "financialSourceIsParent",
                "financialYear",
                "financialPeriodEnd",
                "financialCurrency",
                "operatingRevenue",
                "operatingResult",
                "operatingMarginPercent",
                "preTaxResult",
                "annualResult",
                "equity",
                "assets",
                "equityRatioPercent",
                "debt",
                "currentAssets",
                "currentLiabilities",
                "currentRatio",
                "revenuePerEmployee",
                "financialWarning",
                "opportunityScore",
                "opportunityPriority",
                "marketFitScore",
                "technicalOpportunityScore",
                "providerLandscapeScore",
                "dataConfidenceScore",
                "priorityCapped",
                "priorityExplanation",
                "sharedDomain",
                "sharedDomainCount",
                "reasonsToContact",
                "uncertaintyWarnings",
                "scoreEvidence",
                "manualEmailPlatform",
                "emailPlatformCorrect",
                "manualProviderRelationship",
                "providerSignalCorrect",
                "technologyComment",
                "manualDomain",
                "isCorrect",
                "comment",
                "wouldContact",
                "priorityCorrect",
                "reasonsUseful",
                "manualPriority",
                "salesComment"
        ));

        for (CompanyCandidate candidate : candidates) {
            DomainCandidate domain = candidate.domainCandidate();
            DnsObservation dns = candidate.dnsObservation();
            TechnologyObservation technology = candidate.technologyObservation();
            PublicInfrastructureObservation publicInfra = technology == null
                    ? PublicInfrastructureObservation.empty()
                    : technology.publicInfrastructure();
            OpportunityAssessment opportunity = candidate.opportunityAssessment();
            FinancialObservation financial = candidate.financialObservation();

            appendRow(csv, List.of(
                    value(candidate.organizationNumber()),
                    value(candidate.name()),
                    Integer.toString(candidate.employees()),
                    candidate.segment() == null ? "" : candidate.segment().getDisplayName(),
                    value(candidate.naceCode()),
                    value(candidate.naceDescription()),
                    value(candidate.municipalityNumber()),
                    value(candidate.municipalityName()),
                    value(candidate.address()),
                    value(candidate.website()),
                    value(candidate.email()),
                    value(candidate.phone()),
                    candidate.entityType() == null ? "" : candidate.entityType().getDisplayName(),
                    value(candidate.parentOrganizationNumber()),
                    domain == null ? "" : value(domain.domain()),
                    domain == null ? "" : domain.source().getDisplayName(),
                    domain == null ? "" : domain.confidence().getDisplayName(),
                    domain != null && domain.requiresVerification() ? "true" : "false",
                    domain == null ? "" : value(domain.explanation()),
                    dns == null ? "" : dns.status().getDisplayName(),
                    dns == null ? "" : String.join(" | ", dns.mxRecords()),
                    dns == null ? "" : String.join(" | ", dns.spfRecords()),
                    dns == null ? "" : value(dns.dmarcRecord()),
                    dns == null ? "" : value(dns.dmarcPolicy()),
                    dns == null ? "" : String.join(" | ", dns.nameServers()),
                    dns == null ? "" : String.join(" | ", dns.lookupErrors()),
                    technology == null ? "" : technology.emailPlatform().getDisplayName(),
                    technology == null ? "" : technology.emailPlatformConfidence().getDisplayName(),
                    technology == null ? "" : technology.emailGateway().getDisplayName(),
                    technology == null ? "" : technology.emailGatewayConfidence().getDisplayName(),
                    technology == null ? "" : technology.dmarcPosture().getDisplayName(),
                    technology == null ? "" : technology.spfPosture().getDisplayName(),
                    technology == null ? "" : value(technology.spfAllMechanism()),
                    technology == null ? "" : value(technology.spfRedirectTarget()),
                    technology == null ? "" : technology.spfSignalsDisplay(),
                    technology == null ? "" : providerSummary(technology.providerSignals()),
                    technology == null ? "" : providerRoles(technology.providerSignals()),
                    technology == null ? "" : technology.providerEvidenceDisplay(),
                    technology == null ? "" : technology.evidenceDisplay(),
                    publicInfra.mtaStsStatus().getDisplayName(),
                    value(publicInfra.mtaStsRecord()),
                    publicInfra.tlsRptStatus().getDisplayName(),
                    value(publicInfra.tlsRptRecord()),
                    publicInfra.dnssecStatus().getDisplayName(),
                    value(publicInfra.autodiscoverTarget()),
                    publicInfra.certificateTransparencyStatus().getDisplayName(),
                    publicInfra.certificateNamesDisplay(),
                    publicInfra.warningsDisplay(),
                    publicInfra.evidenceDisplay(),
                    financial == null ? "" : financial.status().getDisplayName(),
                    financial == null ? "" : value(financial.sourceOrganizationNumber()),
                    financial != null && financial.sourceIsParent() ? "true" : "false",
                    financial == null || financial.fiscalYear() == null ? "" : financial.fiscalYear().toString(),
                    financial == null ? "" : value(financial.periodEnd()),
                    financial == null ? "" : value(financial.currency()),
                    financial == null ? "" : decimal(financial.operatingRevenue()),
                    financial == null ? "" : decimal(financial.operatingResult()),
                    financial == null ? "" : decimal(financial.operatingMarginPercent()),
                    financial == null ? "" : decimal(financial.preTaxResult()),
                    financial == null ? "" : decimal(financial.annualResult()),
                    financial == null ? "" : decimal(financial.equity()),
                    financial == null ? "" : decimal(financial.assets()),
                    financial == null ? "" : decimal(financial.equityRatioPercent()),
                    financial == null ? "" : decimal(financial.debt()),
                    financial == null ? "" : decimal(financial.currentAssets()),
                    financial == null ? "" : decimal(financial.currentLiabilities()),
                    financial == null ? "" : decimal(financial.currentRatio()),
                    financial == null ? "" : decimal(financial.revenuePerEmployee(candidate.employees())),
                    financial == null ? "" : value(financial.warning()),
                    opportunity == null ? "" : Integer.toString(opportunity.opportunityScore()),
                    opportunity == null ? "" : opportunity.priority().getDisplayName(),
                    opportunity == null ? "" : Integer.toString(opportunity.marketFitScore()),
                    opportunity == null ? "" : Integer.toString(opportunity.technicalOpportunityScore()),
                    opportunity == null ? "" : Integer.toString(opportunity.providerLandscapeScore()),
                    opportunity == null ? "" : Integer.toString(opportunity.dataConfidenceScore()),
                    opportunity != null && opportunity.priorityCapped() ? "true" : "false",
                    opportunity == null ? "" : value(opportunity.priorityExplanation()),
                    opportunity != null && opportunity.isSharedDomain() ? "true" : "false",
                    opportunity == null ? "" : Integer.toString(opportunity.sharedDomainCount()),
                    opportunity == null ? "" : opportunity.reasonsDisplay(),
                    opportunity == null ? "" : opportunity.warningsDisplay(),
                    opportunity == null ? "" : opportunity.evidenceDisplay(),
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    ""
            ));
        }

        byte[] content = csv.toString().getBytes(StandardCharsets.UTF_8);
        byte[] result = new byte[UTF8_BOM.length + content.length];
        System.arraycopy(UTF8_BOM, 0, result, 0, UTF8_BOM.length);
        System.arraycopy(content, 0, result, UTF8_BOM.length, content.length);
        return result;
    }

    private String providerSummary(List<ProviderSignal> signals) {
        if (signals == null || signals.isEmpty()) {
            return "";
        }
        return String.join(" | ", signals.stream()
                .map(signal -> signal.provider()
                        + " [" + signal.role().getDisplayName() + "; "
                        + signal.sourcesDisplay() + "; "
                        + signal.confidence().getDisplayName() + "]")
                .toList());
    }

    private String providerRoles(List<ProviderSignal> signals) {
        if (signals == null || signals.isEmpty()) {
            return "";
        }
        return String.join(" | ", signals.stream()
                .map(signal -> signal.provider() + ": " + signal.role().getDisplayName())
                .toList());
    }

    private void appendRow(StringBuilder csv, List<String> values) {
        for (int index = 0; index < values.size(); index++) {
            if (index > 0) {
                csv.append(DELIMITER);
            }
            csv.append(escape(values.get(index)));
        }
        csv.append(LINE_ENDING);
    }

    private String escape(String value) {
        String safe = value == null ? "" : value;
        boolean quote = safe.indexOf(DELIMITER) >= 0
                || safe.contains("\"")
                || safe.contains("\n")
                || safe.contains("\r");

        if (!quote) {
            return safe;
        }
        return "\"" + safe.replace("\"", "\"\"") + "\"";
    }

    private String decimal(java.math.BigDecimal value) {
        return value == null ? "" : value.stripTrailingZeros().toPlainString();
    }

    private String value(String value) {
        return value == null ? "" : value;
    }
}
