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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PublicInfrastructureService {

    private static final Logger log = LoggerFactory.getLogger(PublicInfrastructureService.class);

    private static final String DNSSEC_DOH_BASE_URL = "https://dns.google/resolve";

    private static final Pattern CT_NAME_VALUE =
            Pattern.compile("\"name_value\"\\s*:\\s*\"((?:\\\\.|[^\"])*)\"");

    private static final Pattern DNS_JSON_STATUS =
            Pattern.compile("\"Status\"\\s*:\\s*(\\d+)");
    private static final Pattern DNS_JSON_AD =
            Pattern.compile("\"AD\"\\s*:\\s*(true|false)", Pattern.CASE_INSENSITIVE);
    private static final Pattern DNS_JSON_ANSWER =
            Pattern.compile("\"Answer\"\\s*:\\s*\\[(.*?)]", Pattern.DOTALL);
    private static final Pattern DNS_JSON_OBJECT =
            Pattern.compile("\\{(.*?)}", Pattern.DOTALL);
    private static final Pattern DNS_JSON_TYPE =
            Pattern.compile("\"type\"\\s*:\\s*(\\d+)");
    private static final Pattern DNS_JSON_DATA =
            Pattern.compile("\"data\"\\s*:\\s*\"((?:\\\\.|[^\"])*)\"");

    private final boolean dnssecDohEnabled;
    private final boolean certificateTransparencyEnabled;
    private final int maxConcurrency;
    private final HttpClient httpClient;

    public PublicInfrastructureService(
            @Value("${reconic.public-intelligence.dnssec-doh.enabled:true}") boolean dnssecDohEnabled,
            @Value("${reconic.public-intelligence.ct.enabled:false}") boolean certificateTransparencyEnabled,
            @Value("${reconic.public-intelligence.max-concurrency:16}") int maxConcurrency
    ) {
        this.dnssecDohEnabled = dnssecDohEnabled;
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

        // Shared/conglomerate domains are common. Analyze a domain once per search,
        // then reuse the immutable observation for all candidates on that domain.
        var perSearchCache = new ConcurrentHashMap<String, PublicInfrastructureObservation>();

        try (var executor = Executors.newFixedThreadPool(maxConcurrency)) {
            List<java.util.concurrent.Future<CompanyCandidate>> futures = new ArrayList<>();
            for (CompanyCandidate candidate : candidates) {
                futures.add(executor.submit(() -> enrichOne(candidate, perSearchCache)));
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

    CompanyCandidate enrichOne(
            CompanyCandidate candidate,
            ConcurrentHashMap<String, PublicInfrastructureObservation> perSearchCache
    ) {
        if (candidate == null) {
            return null;
        }

        DomainCandidate domainCandidate = candidate.domainCandidate();
        if (domainCandidate == null || !domainCandidate.hasDomain()) {
            return candidate;
        }

        String normalizedDomain = normalizeDomain(domainCandidate.domain());
        if (normalizedDomain == null) {
            return candidate;
        }

        PublicInfrastructureObservation observation =
                perSearchCache.computeIfAbsent(normalizedDomain, this::analyze);

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

        // JNDI DNS in the JDK does not reliably support DS records.
        // v0.5.2.1 therefore uses a DNS-over-HTTPS DS query instead.
        DnssecResult dnssec = queryDnssec(normalizedDomain, warnings);
        if (dnssec.status() == PublicSignalStatus.PRESENT) {
            evidence.add("DNSSEC DS-post observert via DNS-over-HTTPS");
            if (Boolean.TRUE.equals(dnssec.authenticatedData())) {
                evidence.add("DNS-over-HTTPS-resolveren markerte svaret som DNSSEC-validert");
            }
        }

        List<String> autodiscover = dnsRecords("autodiscover." + normalizedDomain, "CNAME", warnings);
        String autodiscoverTarget = autodiscover == null || autodiscover.isEmpty()
                ? null
                : cleanDnsValue(autodiscover.getFirst());
        if (autodiscoverTarget != null) {
            evidence.add("Autodiscover CNAME: " + autodiscoverTarget);
        }

        /*
         * Norid is deliberately NOT queried by Reconic.
         *
         * Norid's public lookup terms prohibit commercial use of lookup data,
         * including targeted advertising. Reconic is a lead-intelligence tool,
         * so automatic Norid enrichment would be the wrong data source for this use.
         *
         * The existing observation fields remain SKIPPED for CSV/model backwards
         * compatibility with v0.5.2.
         */

        CtResult ct = queryCertificateTransparency(normalizedDomain, warnings);
        if (ct.status() == PublicSignalStatus.PRESENT && !ct.names().isEmpty()) {
            evidence.add("Certificate Transparency: " + ct.names().size() + " relevante sertifikatnavn");
        }

        return new PublicInfrastructureObservation(
                mtaStsStatus,
                mtaStsRecord,
                tlsRptStatus,
                tlsRptRecord,
                dnssec.status(),
                autodiscoverTarget,
                PublicSignalStatus.SKIPPED,
                null,
                null,
                null,
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

    private DnssecResult queryDnssec(String domain, List<String> warnings) {
        if (!dnssecDohEnabled) {
            return new DnssecResult(PublicSignalStatus.SKIPPED, List.of(), null);
        }

        try {
            String url = DNSSEC_DOH_BASE_URL
                    + "?name=" + URLEncoder.encode(domain, StandardCharsets.UTF_8)
                    + "&type=DS"
                    + "&do=1"
                    + "&cd=0"
                    + "&edns_client_subnet=0.0.0.0%2F0";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(5))
                    .header("Accept", "application/json")
                    .header("User-Agent", "Reconic/0.5.2.1 development")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                warnings.add("DNSSEC DoH svarte HTTP " + response.statusCode() + " for " + domain);
                return new DnssecResult(PublicSignalStatus.UNKNOWN, List.of(), null);
            }

            DnssecResult result = parseDnssecDoh(response.body());
            if (result.status() == PublicSignalStatus.UNKNOWN) {
                warnings.add("DNSSEC DoH ga et uventet DNS-svar for " + domain);
            }
            return result;
        } catch (Exception exception) {
            warnings.add("DNSSEC DoH feilet for " + domain + ": " + shortMessage(exception));
            return new DnssecResult(PublicSignalStatus.UNKNOWN, List.of(), null);
        }
    }

    static DnssecResult parseDnssecDoh(String json) {
        if (json == null || json.isBlank()) {
            return new DnssecResult(PublicSignalStatus.UNKNOWN, List.of(), null);
        }

        Matcher statusMatcher = DNS_JSON_STATUS.matcher(json);
        if (!statusMatcher.find()) {
            return new DnssecResult(PublicSignalStatus.UNKNOWN, List.of(), null);
        }

        int dnsStatus;
        try {
            dnsStatus = Integer.parseInt(statusMatcher.group(1));
        } catch (NumberFormatException exception) {
            return new DnssecResult(PublicSignalStatus.UNKNOWN, List.of(), null);
        }

        Boolean authenticatedData = null;
        Matcher adMatcher = DNS_JSON_AD.matcher(json);
        if (adMatcher.find()) {
            authenticatedData = Boolean.parseBoolean(adMatcher.group(1));
        }

        // NOERROR = 0. NXDOMAIN = 3. Other DNS rcodes are treated as technical uncertainty.
        if (dnsStatus == 3) {
            return new DnssecResult(PublicSignalStatus.MISSING, List.of(), authenticatedData);
        }
        if (dnsStatus != 0) {
            return new DnssecResult(PublicSignalStatus.UNKNOWN, List.of(), authenticatedData);
        }

        List<String> dsRecords = new ArrayList<>();
        Matcher answerMatcher = DNS_JSON_ANSWER.matcher(json);
        if (answerMatcher.find()) {
            String answerBody = answerMatcher.group(1);
            Matcher objectMatcher = DNS_JSON_OBJECT.matcher(answerBody);

            while (objectMatcher.find()) {
                String object = objectMatcher.group(1);

                Matcher typeMatcher = DNS_JSON_TYPE.matcher(object);
                if (!typeMatcher.find()) {
                    continue;
                }

                int type;
                try {
                    type = Integer.parseInt(typeMatcher.group(1));
                } catch (NumberFormatException exception) {
                    continue;
                }

                // DNS RR type 43 = DS.
                if (type != 43) {
                    continue;
                }

                Matcher dataMatcher = DNS_JSON_DATA.matcher(object);
                if (dataMatcher.find()) {
                    dsRecords.add(unescapeJsonString(dataMatcher.group(1)).trim());
                }
            }
        }

        return new DnssecResult(
                dsRecords.isEmpty() ? PublicSignalStatus.MISSING : PublicSignalStatus.PRESENT,
                List.copyOf(dsRecords),
                authenticatedData
        );
    }

    private CtResult queryCertificateTransparency(String domain, List<String> warnings) {
        if (!certificateTransparencyEnabled) {
            return new CtResult(PublicSignalStatus.SKIPPED, List.of());
        }

        try {
            String query = "%25." + domain;
            String url = "https://crt.sh/?q="
                    + URLEncoder.encode(query, StandardCharsets.UTF_8)
                    + "&output=json";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(6))
                    .header("Accept", "application/json")
                    .header("User-Agent", "Reconic/0.5.2.1 development")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                warnings.add("Certificate Transparency svarte HTTP "
                        + response.statusCode() + " for " + domain);
                return new CtResult(PublicSignalStatus.UNKNOWN, List.of());
            }

            List<String> names = parseCertificateNames(response.body(), domain);
            return new CtResult(
                    names.isEmpty() ? PublicSignalStatus.MISSING : PublicSignalStatus.PRESENT,
                    names
            );
        } catch (Exception exception) {
            warnings.add("Certificate Transparency feilet for "
                    + domain + ": " + shortMessage(exception));
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

    record DnssecResult(
            PublicSignalStatus status,
            List<String> dsRecords,
            Boolean authenticatedData
    ) {
        DnssecResult {
            status = status == null ? PublicSignalStatus.UNKNOWN : status;
            dsRecords = dsRecords == null ? List.of() : List.copyOf(dsRecords);
        }
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
