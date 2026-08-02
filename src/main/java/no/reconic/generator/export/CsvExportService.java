package no.reconic.generator.export;

import no.reconic.generator.dns.DnsObservation;
import no.reconic.generator.domain.DomainCandidate;
import no.reconic.generator.model.CompanyCandidate;
import no.reconic.generator.model.CompanyDiscoveryResult;
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
                "manualDomain",
                "isCorrect",
                "comment"
        ));

        for (CompanyCandidate candidate : candidates) {
            DomainCandidate domain = candidate.domainCandidate();
            DnsObservation dns = candidate.dnsObservation();

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

    private String value(String value) {
        return value == null ? "" : value;
    }
}
