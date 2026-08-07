package no.reconic.generator.intelligence;

import no.reconic.generator.domain.DomainCandidate;
import no.reconic.generator.model.CompanyCandidate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.InitialDirContext;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PublicInfrastructureService {

    private static final Logger log = LoggerFactory.getLogger(PublicInfrastructureService.class);
    private static final Pattern RDAP_DELEGATION_SIGNED =
            Pattern.compile("\"delegationSigned\"\\s*:\\s*(true|false)", Pattern.CASE_INSENSITIVE);
    private static final Pattern RDAP_EVENT =
            Pattern.compile("\"eventAction\"\\s*:\\s*\"([^\"]+)\"\\s*,\\s*\"eventDate\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern CT_NAME_VALUE =
            Pattern.compile("\"name_value\"\\s*:\\s*\"((?:\\\\.|[^\"])*)\"");

    private final boolean noridEnabled;
    private final boolean certificateTransparencyEnabled;
    private final int maxConcurrency;
    private final HttpClient httpClient;

    public PublicInfrastructureService(
            @Value("${reconic.public-intelligence.norid.enabled:false}") boolean noridEnabled,
            @Value("${reconic.public-intelligence.ct.enabled:false}") boolean certificateTransparencyEnabled,
            @Value("${reconic.public-intelligence.max-concurrency:16}") int maxConcurrency
    ) {
        this.noridEnabled = noridEnabled;
        this.certificateTransparencyEnabled = certificateTransparencyEnabled;
        this.maxConcurrency = Math.max(1, Math.min(maxConcurrency, 32));
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public List<CompanyCandidate> enrich(List<CompanyCandidate> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }

        try (var executor = Executors.newFixedThreadPool(maxConcurrency)) {
            List<java.util.concurrent.Future<CompanyCandidate>> futures = new ArrayList<>();
            for (CompanyCandidate candidate : candidates) {
                futures.add(executor.submit(() -> enrichOne(candidate)));
            }

            List<CompanyCandidate> result = new ArrayList<>(futures.size());
            for (var future : futures) {
                try {
                    result.add(future.get());
                } catch (Exception exception) {
                    log.warn("Offentlig infrastrukturanalyse feilet for én kandidat: {}", exception.getMessage());
                }
            }
            return List.copyOf(result);
        }
    }

    CompanyCandidate enrichOne(CompanyCandidate candidate) {
        if (candidate == null) {
            return null;
        }
        DomainCandidate domainCandidate = candidate.domainCandidate();
        if (domainCandidate == null || !domainCandidate.hasDomain()) {
            return candidate;
        }

        PublicInfrastructureObservation observation = analyze(domainCandidate.domain());
        TechnologyObservation technology = candidate.technologyObservation() == null
                ? TechnologyObservation.empty()
                : candidate.technologyObservation();

        return candidate.withTechnologyObservation(technology.withPublicInfrastructure(observation));
    }

    public PublicInfrastructureObservation analyze(String domain) {
        String normalizedDomain = normalizeDomain(domain);
        if (normalizedDomain == null) {
            return PublicInfrastructureObservation.empty();
        }

        List<String> warnings = new ArrayList<>();
        List<String> evidence = new ArrayList<>();

        List<String> mtaStsRecords = dnsRecords("_mta-sts." + normalizedDomain, "TXT", warnings);
        String mtaStsRecord = firstMatching(mtaStsRecords, "v=stsv1");
        PublicSignalStatus mtaStsStatus = statusFromDnsResult(mtaStsRecords, mtaStsRecord);
        if (mtaStsRecord != null) {
            evidence.add("MTA-STS TXT funnet: " + mtaStsRecord);
        }

        List<String> tlsRptRecords = dnsRecords("_smtp._tls." + normalizedDomain, "TXT", warnings);
        String tlsRptRecord = firstMatching(tlsRptRecords, "v=tlsrptv1");
        PublicSignalStatus tlsRptStatus = statusFromDnsResult(tlsRptRecords, tlsRptRecord);
        if (tlsRptRecord != null) {
            evidence.add("TLS-RPT TXT funnet: " + tlsRptRecord);
        }

        List<String> dsRecords = dnsRecords(normalizedDomain, "DS", warnings);
        PublicSignalStatus dnssecStatus = dsRecords == null
                ? PublicSignalStatus.UNKNOWN
                : (dsRecords.isEmpty() ? PublicSignalStatus.MISSING : PublicSignalStatus.PRESENT);
        if (dnssecStatus == PublicSignalStatus.PRESENT) {
            evidence.add("DNSSEC DS-post observert");
        }

        List<String> autodiscover = dnsRecords("autodiscover." + normalizedDomain, "CNAME", warnings);
        String autodiscoverTarget = autodiscover == null || autodiscover.isEmpty()
                ? null
                : cleanDnsValue(autodiscover.getFirst());
        if (autodiscoverTarget != null) {
            evidence.add("Autodiscover CNAME: " + autodiscoverTarget);
        }

        NoridResult norid = queryNorid(normalizedDomain, warnings);
        if (norid.status() == PublicSignalStatus.PRESENT) {
            evidence.add("Norid RDAP bekrefter domenet");
            if (norid.dnssec() != null) {
                evidence.add("Norid DNSSEC: " + (norid.dnssec() ? "aktivert" : "ikke aktivert"));
            }
        }

        CtResult ct = queryCertificateTransparency(normalizedDomain, warnings);
        if (ct.status() == PublicSignalStatus.PRESENT && !ct.names().isEmpty()) {
            evidence.add("Certificate Transparency: " + ct.names().size() + " relevante sertifikatnavn");
        }

        return new PublicInfrastructureObservation(
                mtaStsStatus,
                mtaStsRecord,
                tlsRptStatus,
                tlsRptRecord,
                dnssecStatus,
                autodiscoverTarget,
                norid.status(),
                norid.dnssec(),
                norid.createdAt(),
                norid.updatedAt(),
                ct.status(),
                ct.names(),
                warnings,
                evidence
        );
    }

    private PublicSignalStatus statusFromDnsResult(List<String> rawRecords, String matchingRecord) {
        if (rawRecords == null) {
            return PublicSignalStatus.UNKNOWN;
        }
        return matchingRecord == null ? PublicSignalStatus.MISSING : PublicSignalStatus.PRESENT;
    }

    private NoridResult queryNorid(String domain, List<String> warnings) {
        if (!domain.endsWith(".no") || !noridEnabled) {
            return new NoridResult(PublicSignalStatus.SKIPPED, null, null, null);
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://rdap.norid.no/domain/" + domain))
                    .timeout(Duration.ofSeconds(5))
                    .header("Accept", "application/rdap+json, application/json")
                    .header("User-Agent", "Reconic/0.5.2 development")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 404) {
                return new NoridResult(PublicSignalStatus.MISSING, null, null, null);
            }
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                warnings.add("Norid RDAP svarte HTTP " + response.statusCode() + " for " + domain);
                return new NoridResult(PublicSignalStatus.UNKNOWN, null, null, null);
            }
            return parseNoridRdap(response.body());
        } catch (Exception exception) {
            warnings.add("Norid RDAP feilet for " + domain + ": " + shortMessage(exception));
            return new NoridResult(PublicSignalStatus.UNKNOWN, null, null, null);
        }
    }

    private CtResult queryCertificateTransparency(String domain, List<String> warnings) {
        if (!certificateTransparencyEnabled) {
            return new CtResult(PublicSignalStatus.SKIPPED, List.of());
        }

        try {
            String query = "%25." + domain;
            String url = "https://crt.sh/?q=" + URLEncoder.encode(query, StandardCharsets.UTF_8) + "&output=json";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(6))
                    .header("Accept", "application/json")
                    .header("User-Agent", "Reconic/0.5.2 development")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                warnings.add("Certificate Transparency svarte HTTP " + response.statusCode() + " for " + domain);
                return new CtResult(PublicSignalStatus.UNKNOWN, List.of());
            }

            List<String> names = parseCertificateNames(response.body(), domain);
            return new CtResult(
                    names.isEmpty() ? PublicSignalStatus.MISSING : PublicSignalStatus.PRESENT,
                    names
            );
        } catch (Exception exception) {
            warnings.add("Certificate Transparency feilet for " + domain + ": " + shortMessage(exception));
            return new CtResult(PublicSignalStatus.UNKNOWN, List.of());
        }
    }

    List<String> dnsRecords(String name, String type, List<String> warnings) {
        Hashtable<String, String> environment = new Hashtable<>();
        environment.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.dns.DnsContextFactory");
        environment.put("com.sun.jndi.dns.timeout.initial", "2000");
        environment.put("com.sun.jndi.dns.timeout.retries", "1");

        InitialDirContext context = null;
        try {
            context = new InitialDirContext(environment);
            Attributes attributes = context.getAttributes(name, new String[]{type});
            Attribute attribute = attributes.get(type);
            if (attribute == null) {
                return List.of();
            }

            List<String> values = new ArrayList<>();
            NamingEnumeration<?> enumeration = attribute.getAll();
            while (enumeration.hasMore()) {
                values.add(cleanDnsValue(String.valueOf(enumeration.next())));
            }
            return List.copyOf(values);
        } catch (NamingException exception) {
            if (isNormalMissingRecord(exception)) {
                return List.of();
            }
            warnings.add(type + " for " + name + " feilet: " + shortMessage(exception));
            return null;
        } finally {
            if (context != null) {
                try {
                    context.close();
                } catch (NamingException ignored) {
                    // Best-effort cleanup only.
                }
            }
        }
    }

    static NoridResult parseNoridRdap(String json) {
        if (json == null || json.isBlank()) {
            return new NoridResult(PublicSignalStatus.UNKNOWN, null, null, null);
        }

        Boolean dnssec = null;
        Matcher dnssecMatcher = RDAP_DELEGATION_SIGNED.matcher(json);
        if (dnssecMatcher.find()) {
            dnssec = Boolean.parseBoolean(dnssecMatcher.group(1));
        }

        String createdAt = null;
        String updatedAt = null;
        Matcher eventMatcher = RDAP_EVENT.matcher(json);
        while (eventMatcher.find()) {
            String action = eventMatcher.group(1).toLowerCase(Locale.ROOT);
            String date = eventMatcher.group(2);
            if ((action.contains("registration") || action.contains("created")) && createdAt == null) {
                createdAt = date;
            }
            if (action.contains("last changed") || action.contains("changed") || action.contains("update")) {
                updatedAt = date;
            }
        }

        return new NoridResult(PublicSignalStatus.PRESENT, dnssec, createdAt, updatedAt);
    }

    static List<String> parseCertificateNames(String json, String domain) {
        if (json == null || json.isBlank() || domain == null || domain.isBlank()) {
            return List.of();
        }

        String normalizedDomain = domain.toLowerCase(Locale.ROOT);
        Set<String> names = new LinkedHashSet<>();
        Matcher matcher = CT_NAME_VALUE.matcher(json);
        while (matcher.find()) {
            String value = unescapeJsonString(matcher.group(1));
            for (String rawName : value.split("\\n")) {
                String name = rawName.trim().toLowerCase(Locale.ROOT);
                if (name.startsWith("*.")) {
                    name = name.substring(2);
                }
                if (name.equals(normalizedDomain) || name.endsWith("." + normalizedDomain)) {
                    names.add(name);
                }
                if (names.size() >= 25) {
                    break;
                }
            }
            if (names.size() >= 25) {
                break;
            }
        }

        return names.stream()
                .sorted(Comparator.comparingInt(String::length).thenComparing(String::compareTo))
                .toList();
    }

    private String firstMatching(List<String> values, String fragment) {
        if (values == null) {
            return null;
        }
        String expected = fragment.toLowerCase(Locale.ROOT);
        return values.stream()
                .filter(value -> value != null && value.toLowerCase(Locale.ROOT).contains(expected))
                .findFirst()
                .orElse(null);
    }

    private static String normalizeDomain(String domain) {
        if (domain == null) {
            return null;
        }
        String normalized = domain.trim().toLowerCase(Locale.ROOT);
        while (normalized.endsWith(".")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized.isBlank() ? null : normalized;
    }

    private static String cleanDnsValue(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value.trim();
        if (cleaned.startsWith("\"") && cleaned.endsWith("\"") && cleaned.length() >= 2) {
            cleaned = cleaned.substring(1, cleaned.length() - 1);
        }
        cleaned = cleaned.replace("\" \"", "");
        while (cleaned.endsWith(".")) {
            cleaned = cleaned.substring(0, cleaned.length() - 1);
        }
        return cleaned.trim();
    }

    private static boolean isNormalMissingRecord(NamingException exception) {
        String message = exception.getMessage();
        if (message == null) {
            return false;
        }
        String lower = message.toLowerCase(Locale.ROOT);
        return lower.contains("name not found")
                || lower.contains("dns name not found")
                || lower.contains("no attributes");
    }

    private static String shortMessage(Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }
        return message.length() <= 180 ? message : message.substring(0, 180);
    }

    private static String unescapeJsonString(String value) {
        return value
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }

    record NoridResult(
            PublicSignalStatus status,
            Boolean dnssec,
            String createdAt,
            String updatedAt
    ) {
    }

    record CtResult(
            PublicSignalStatus status,
            List<String> names
    ) {
        CtResult {
            names = names == null ? List.of() : List.copyOf(names);
        }
    }
}
