package no.reconic.generator.intelligence;

import no.reconic.generator.dns.DnsObservation;
import no.reconic.generator.model.CompanyCandidate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TechnologyAnalysisService {

    private static final Pattern SPF_ALL_MECHANISM = Pattern.compile("(?i)(?:^|\\s)([+~?\\-]?)all(?:\\s|$)");
    private static final Pattern SPF_REDIRECT = Pattern.compile("(?i)(?:^|\\s)redirect=([^\\s]+)");

    private static final List<ProviderDefinition> PROVIDERS = List.of(
            new ProviderDefinition("Hjelseth", ProviderRole.MSP_CANDIDATE, List.of("hjelseth.com")),
            new ProviderDefinition("Upheads", ProviderRole.MSP_CANDIDATE, List.of("upheads.no", "upheads.com", "upheads.org")),
            new ProviderDefinition("ITsjefen", ProviderRole.MSP_CANDIDATE, List.of("itsjefen.net")),
            new ProviderDefinition("Intility", ProviderRole.MSP_CANDIDATE, List.of("intility.com")),
            new ProviderDefinition("Netpower", ProviderRole.MSP_CANDIDATE, List.of("netpower.no")),
            new ProviderDefinition("ECIT", ProviderRole.MSP_CANDIDATE, List.of("ecitinfra.no", "ecit.com")),
            new ProviderDefinition("Telenor", ProviderRole.CONNECTIVITY_PROVIDER, List.of("telenor.net", "online.no")),
            new ProviderDefinition("Altibox", ProviderRole.CONNECTIVITY_PROVIDER, List.of("altibox.no")),
            new ProviderDefinition("GlobalConnect", ProviderRole.CONNECTIVITY_PROVIDER, List.of("globalconnect.no", "broadnet.no")),
            new ProviderDefinition("Domeneshop", ProviderRole.DNS_PROVIDER, List.of("hyp.net", "domeneshop.no")),
            new ProviderDefinition("one.com", ProviderRole.DNS_PROVIDER, List.of("brand.one.com"))
    );

    public List<CompanyCandidate> enrich(List<CompanyCandidate> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }
        return candidates.stream()
                .map(candidate -> candidate == null
                        ? null
                        : candidate.withTechnologyObservation(analyze(candidate.dnsObservation())))
                .toList();
    }

    public TechnologyObservation analyze(DnsObservation dns) {
        if (dns == null) {
            return TechnologyObservation.empty();
        }

        List<String> mx = lower(dns.mxRecords());
        List<String> spf = lower(dns.spfRecords());
        List<String> evidence = new ArrayList<>();

        EmailGateway gateway = detectGateway(mx);
        SignalConfidence gatewayConfidence = gateway == EmailGateway.NONE
                ? SignalConfidence.NONE
                : SignalConfidence.HIGH;
        if (gateway != EmailGateway.NONE) {
            evidence.add("MX indikerer gateway: " + gateway.getDisplayName());
        }

        PlatformResult platformResult = detectPlatform(mx, spf, queryFailed(dns, "MX for "));
        if (platformResult.platform() != EmailPlatform.UNKNOWN
                && platformResult.platform() != EmailPlatform.NONE) {
            evidence.add(platformResult.evidence());
        }

        DmarcPosture dmarcPosture = detectDmarc(dns);
        SpfResult spfResult = detectSpf(dns);
        if (spfResult.redirectTarget() != null) {
            evidence.add("SPF delegerer policy med redirect=" + spfResult.redirectTarget());
        }
        List<ProviderSignal> providerSignals = detectProviders(dns);

        return new TechnologyObservation(
                platformResult.platform(),
                platformResult.confidence(),
                gateway,
                gatewayConfidence,
                dmarcPosture,
                spfResult.posture(),
                spfResult.allMechanism(),
                spfResult.redirectTarget(),
                spfResult.signals(),
                providerSignals,
                evidence
        );
    }

    private PlatformResult detectPlatform(List<String> mx, List<String> spf, boolean mxLookupFailed) {
        boolean microsoftInMx = containsAny(mx, "mail.protection.outlook.com");
        boolean googleInMx = containsAny(mx, "aspmx.l.google.com", "googlemail.com");
        boolean microsoftInSpf = containsAny(spf, "spf.protection.outlook.com");
        boolean googleInSpf = containsAny(spf, "_spf.google.com");

        if (microsoftInMx) {
            return new PlatformResult(
                    EmailPlatform.MICROSOFT_365,
                    SignalConfidence.HIGH,
                    "MX peker til Microsoft 365"
            );
        }
        if (googleInMx) {
            return new PlatformResult(
                    EmailPlatform.GOOGLE_WORKSPACE,
                    SignalConfidence.HIGH,
                    "MX peker til Google Workspace"
            );
        }
        if (microsoftInSpf) {
            return new PlatformResult(
                    EmailPlatform.MICROSOFT_365,
                    SignalConfidence.MEDIUM,
                    "SPF inkluderer Microsoft 365; dette kan være utgående e-post bak en separat gateway"
            );
        }
        if (googleInSpf) {
            return new PlatformResult(
                    EmailPlatform.GOOGLE_WORKSPACE,
                    SignalConfidence.MEDIUM,
                    "SPF inkluderer Google Workspace; dette kan være utgående e-post bak en separat gateway"
            );
        }
        if (mx.isEmpty()) {
            return mxLookupFailed
                    ? new PlatformResult(EmailPlatform.UNKNOWN, SignalConfidence.NONE, "MX-oppslaget feilet teknisk")
                    : new PlatformResult(EmailPlatform.NONE, SignalConfidence.NONE, "Ingen MX funnet");
        }
        return new PlatformResult(
                EmailPlatform.OTHER,
                SignalConfidence.LOW,
                "MX finnes, men matcher ingen kjent plattformsignatur"
        );
    }

    private EmailGateway detectGateway(List<String> mx) {
        if (containsAny(mx, "mimecast.com", "mimecast.co", "mimecast.net")) {
            return EmailGateway.MIMECAST;
        }
        if (containsAny(mx, "ppe-hosted.com", "pphosted.com", "proofpoint.com")) {
            return EmailGateway.PROOFPOINT;
        }
        if (containsAny(mx, "iphmx.com")) {
            return EmailGateway.CISCO_EMAIL_SECURITY;
        }
        if (containsAny(mx, "online.no", "telenor.net")) {
            return EmailGateway.TELENOR;
        }
        if (containsAny(mx, "altibox.no")) {
            return EmailGateway.ALTIBOX;
        }
        return EmailGateway.NONE;
    }

    private DmarcPosture detectDmarc(DnsObservation dns) {
        if (!dns.hasDmarc()) {
            return queryFailed(dns, "TXT for _dmarc.")
                    ? DmarcPosture.UNKNOWN
                    : DmarcPosture.MISSING;
        }
        String policy = dns.dmarcPolicy();
        if (policy == null || policy.isBlank()) {
            return DmarcPosture.INVALID;
        }
        return switch (policy.toLowerCase(Locale.ROOT)) {
            case "none" -> DmarcPosture.MONITORING;
            case "quarantine" -> DmarcPosture.QUARANTINE;
            case "reject" -> DmarcPosture.REJECT;
            default -> DmarcPosture.INVALID;
        };
    }

    private SpfResult detectSpf(DnsObservation dns) {
        List<String> records = dns.spfRecords();
        if (records == null || records.isEmpty()) {
            SpfPosture posture = queryFailed(dns, "TXT for " + dns.domain())
                    ? SpfPosture.UNKNOWN
                    : SpfPosture.MISSING;
            return new SpfResult(posture, null, null, List.of());
        }

        Set<String> signals = new LinkedHashSet<>();
        List<String> lowerRecords = lower(records);
        if (containsAny(lowerRecords, "spf.protection.outlook.com")) {
            signals.add("Microsoft 365");
        }
        if (containsAny(lowerRecords, "_spf.google.com")) {
            signals.add("Google Workspace");
        }
        if (containsAny(lowerRecords, "mimecast.com")) {
            signals.add("Mimecast");
        }
        if (containsAny(lowerRecords, "proofpoint.com", "pphosted.com", "ppe-hosted.com")) {
            signals.add("Proofpoint");
        }
        if (containsAny(lowerRecords, "iphmx.com")) {
            signals.add("Cisco Email Security");
        }
        for (ProviderDefinition provider : PROVIDERS) {
            if (containsAny(lowerRecords, provider.patterns().toArray(String[]::new))) {
                signals.add(provider.name());
            }
        }

        String allMechanism = findAllMechanism(records.getFirst());
        String redirectTarget = findRedirectTarget(records.getFirst());
        if (redirectTarget != null) {
            signals.add("SPF redirect: " + redirectTarget);
        }

        if (records.size() > 1) {
            return new SpfResult(SpfPosture.MULTIPLE, allMechanism, redirectTarget, List.copyOf(signals));
        }

        SpfPosture posture;
        if (allMechanism != null) {
            posture = switch (allMechanism) {
                case "-all" -> SpfPosture.HARD_FAIL;
                case "~all" -> SpfPosture.SOFT_FAIL;
                case "?all" -> SpfPosture.NEUTRAL;
                case "+all", "all" -> SpfPosture.PASS_ALL;
                default -> SpfPosture.PRESENT;
            };
        } else if (redirectTarget != null) {
            posture = SpfPosture.REDIRECTED;
        } else {
            posture = SpfPosture.PRESENT;
        }
        return new SpfResult(posture, allMechanism, redirectTarget, List.copyOf(signals));
    }

    private String findAllMechanism(String record) {
        if (record == null) {
            return null;
        }
        Matcher matcher = SPF_ALL_MECHANISM.matcher(record);
        String last = null;
        while (matcher.find()) {
            String qualifier = matcher.group(1);
            last = (qualifier == null || qualifier.isBlank() ? "" : qualifier) + "all";
        }
        return last;
    }

    private String findRedirectTarget(String record) {
        if (record == null) {
            return null;
        }
        Matcher matcher = SPF_REDIRECT.matcher(record);
        return matcher.find() ? matcher.group(1).trim().toLowerCase(Locale.ROOT) : null;
    }

    private List<ProviderSignal> detectProviders(DnsObservation dns) {
        Map<String, ProviderAccumulator> accumulators = new LinkedHashMap<>();
        for (ProviderDefinition definition : PROVIDERS) {
            ProviderAccumulator accumulator = new ProviderAccumulator(definition.name(), definition.role());
            collectMatches(accumulator, SignalSource.MX, dns.mxRecords(), definition.patterns());
            collectMatches(accumulator, SignalSource.SPF, dns.spfRecords(), definition.patterns());
            collectMatches(accumulator, SignalSource.NS, dns.nameServers(), definition.patterns());
            if (!accumulator.sources.isEmpty()) {
                accumulators.put(definition.name(), accumulator);
            }
        }

        return accumulators.values().stream()
                .map(ProviderAccumulator::toSignal)
                .sorted(Comparator
                        .comparingInt((ProviderSignal signal) -> signal.role().getSortOrder())
                        .thenComparing(signal -> signal.confidence() == SignalConfidence.HIGH ? 0 : 1)
                        .thenComparing(ProviderSignal::provider, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private void collectMatches(
            ProviderAccumulator accumulator,
            SignalSource source,
            List<String> values,
            List<String> patterns
    ) {
        if (values == null) {
            return;
        }
        for (String raw : values) {
            String value = raw == null ? "" : raw.toLowerCase(Locale.ROOT);
            for (String pattern : patterns) {
                if (value.contains(pattern)) {
                    accumulator.sources.add(source);
                    accumulator.evidence.add(source.name() + ": " + raw);
                    break;
                }
            }
        }
    }

    private boolean queryFailed(DnsObservation dns, String prefix) {
        if (dns == null || dns.lookupErrors() == null) {
            return false;
        }
        String expected = prefix.toLowerCase(Locale.ROOT);
        return dns.lookupErrors().stream()
                .filter(value -> value != null)
                .map(value -> value.toLowerCase(Locale.ROOT))
                .anyMatch(value -> value.startsWith(expected));
    }

    private boolean containsAny(List<String> values, String... fragments) {
        for (String value : values) {
            for (String fragment : fragments) {
                if (value.contains(fragment.toLowerCase(Locale.ROOT))) {
                    return true;
                }
            }
        }
        return false;
    }

    private List<String> lower(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(value -> value.toLowerCase(Locale.ROOT))
                .toList();
    }

    private record PlatformResult(
            EmailPlatform platform,
            SignalConfidence confidence,
            String evidence
    ) {
    }

    private record SpfResult(
            SpfPosture posture,
            String allMechanism,
            String redirectTarget,
            List<String> signals
    ) {
    }

    private record ProviderDefinition(
            String name,
            ProviderRole role,
            List<String> patterns
    ) {
    }

    private static final class ProviderAccumulator {
        private final String provider;
        private final ProviderRole role;
        private final Set<SignalSource> sources = EnumSet.noneOf(SignalSource.class);
        private final Set<String> evidence = new LinkedHashSet<>();

        private ProviderAccumulator(String provider, ProviderRole role) {
            this.provider = provider;
            this.role = role;
        }

        private ProviderSignal toSignal() {
            SignalConfidence confidence = sources.size() >= 2
                    ? SignalConfidence.HIGH
                    : SignalConfidence.MEDIUM;
            return new ProviderSignal(
                    provider,
                    role,
                    List.copyOf(sources),
                    confidence,
                    List.copyOf(evidence)
            );
        }
    }
}
