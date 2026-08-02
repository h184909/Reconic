package no.reconic.generator.dns;

import no.reconic.generator.model.CompanyCandidate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class DnsEnrichmentService {

    private static final Logger log = LoggerFactory.getLogger(DnsEnrichmentService.class);
    private static final Pattern QUOTED_TXT_CHUNK = Pattern.compile("\\\"([^\\\"]*)\\\"");
    private static final Pattern DMARC_POLICY = Pattern.compile("(?i)(?:^|;)\\s*p\\s*=\\s*([^;\\s]+)");
    private static final int MAX_CONCURRENT_DOMAINS = 24;

    private final DnsResolver dnsResolver;

    public DnsEnrichmentService(DnsResolver dnsResolver) {
        this.dnsResolver = dnsResolver;
    }

    public List<CompanyCandidate> enrich(List<CompanyCandidate> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }

        try (ExecutorService executor = Executors.newFixedThreadPool(MAX_CONCURRENT_DOMAINS)) {
            List<CompletableFuture<CompanyCandidate>> futures = candidates.stream()
                    .map(candidate -> CompletableFuture.supplyAsync(() -> safeEnrich(candidate), executor))
                    .toList();

            return futures.stream()
                    .map(CompletableFuture::join)
                    .toList();
        }
    }


    private CompanyCandidate safeEnrich(CompanyCandidate candidate) {
        try {
            return enrich(candidate);
        } catch (RuntimeException exception) {
            if (candidate == null) {
                return null;
            }
            String domain = candidate.domainCandidate() == null ? null : candidate.domainCandidate().domain();
            log.warn("Uventet DNS-feil for {}: {}", domain, rootMessage(exception));
            return candidate.withDnsObservation(DnsObservation.failed(domain, rootMessage(exception)));
        }
    }

    CompanyCandidate enrich(CompanyCandidate candidate) {
        if (candidate == null || candidate.domainCandidate() == null || !candidate.domainCandidate().hasDomain()) {
            return candidate == null ? null : candidate.withDnsObservation(DnsObservation.skipped(null));
        }

        String domain = candidate.domainCandidate().domain();
        List<String> errors = new ArrayList<>();
        int successfulQueries = 0;

        LookupResult mxLookup = lookup(domain, "MX", errors);
        successfulQueries += mxLookup.successful() ? 1 : 0;

        LookupResult txtLookup = lookup(domain, "TXT", errors);
        successfulQueries += txtLookup.successful() ? 1 : 0;

        LookupResult dmarcLookup = lookup("_dmarc." + domain, "TXT", errors);
        successfulQueries += dmarcLookup.successful() ? 1 : 0;

        LookupResult nsLookup = lookup(domain, "NS", errors);
        successfulQueries += nsLookup.successful() ? 1 : 0;

        List<String> mxRecords = mxLookup.values().stream()
                .map(this::normalizeMx)
                .filter(value -> !value.isBlank())
                .sorted(Comparator.comparingInt((String value) -> mxPriority(value))
                        .thenComparing(String.CASE_INSENSITIVE_ORDER))
                .toList();

        List<String> spfRecords = txtLookup.values().stream()
                .map(this::normalizeTxt)
                .filter(value -> value.regionMatches(true, 0, "v=spf1", 0, 6))
                .distinct()
                .toList();

        String dmarcRecord = dmarcLookup.values().stream()
                .map(this::normalizeTxt)
                .filter(value -> value.regionMatches(true, 0, "v=dmarc1", 0, 8))
                .findFirst()
                .orElse(null);

        List<String> nameServers = nsLookup.values().stream()
                .map(this::normalizeHostRecord)
                .filter(value -> !value.isBlank())
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();

        DnsLookupStatus status;
        if (successfulQueries == 4) {
            status = DnsLookupStatus.SUCCESS;
        } else if (successfulQueries == 0) {
            status = DnsLookupStatus.FAILED;
        } else {
            status = DnsLookupStatus.PARTIAL;
        }

        DnsObservation observation = new DnsObservation(
                domain,
                status,
                mxRecords,
                spfRecords,
                dmarcRecord,
                extractDmarcPolicy(dmarcRecord),
                nameServers,
                errors
        );

        log.debug(
                "DNS {}: status={}, MX={}, SPF={}, DMARC={}, NS={}, errors={}",
                domain,
                status,
                mxRecords.size(),
                spfRecords.size(),
                dmarcRecord != null,
                nameServers.size(),
                errors.size()
        );

        return candidate.withDnsObservation(observation);
    }

    private LookupResult lookup(String name, String type, List<String> errors) {
        try {
            return new LookupResult(dnsResolver.lookup(name, type), true);
        } catch (RuntimeException exception) {
            errors.add(type + " for " + name + ": " + rootMessage(exception));
            return new LookupResult(List.of(), false);
        }
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        String message = current.getMessage();
        return message == null || message.isBlank() ? current.getClass().getSimpleName() : message;
    }

    private String normalizeMx(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.trim().replaceAll("\\s+", " ");
        int lastSpace = normalized.lastIndexOf(' ');
        if (lastSpace > 0) {
            String priority = normalized.substring(0, lastSpace).trim();
            String host = normalizeHostRecord(normalized.substring(lastSpace + 1));
            if (host.isBlank()) {
                return "";
            }
            return priority + " " + host;
        }
        return normalizeHostRecord(normalized);
    }

    private int mxPriority(String value) {
        if (value == null) {
            return Integer.MAX_VALUE;
        }
        int space = value.indexOf(' ');
        if (space <= 0) {
            return Integer.MAX_VALUE;
        }
        try {
            return Integer.parseInt(value.substring(0, space));
        } catch (NumberFormatException ignored) {
            return Integer.MAX_VALUE;
        }
    }

    private String normalizeTxt(String raw) {
        if (raw == null) {
            return "";
        }

        Matcher matcher = QUOTED_TXT_CHUNK.matcher(raw);
        StringBuilder joined = new StringBuilder();
        while (matcher.find()) {
            joined.append(matcher.group(1));
        }

        String value = joined.length() == 0 ? raw : joined.toString();
        return value.trim().replaceAll("\\s+", " ");
    }

    private String normalizeHostRecord(String raw) {
        if (raw == null) {
            return "";
        }
        String value = raw.trim().toLowerCase(Locale.ROOT);
        while (value.endsWith(".")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    private String extractDmarcPolicy(String record) {
        if (record == null) {
            return null;
        }
        Matcher matcher = DMARC_POLICY.matcher(record);
        return matcher.find() ? matcher.group(1).toLowerCase(Locale.ROOT) : null;
    }

    private record LookupResult(List<String> values, boolean successful) {
        private LookupResult {
            values = values == null ? List.of() : List.copyOf(values);
        }
    }
}
