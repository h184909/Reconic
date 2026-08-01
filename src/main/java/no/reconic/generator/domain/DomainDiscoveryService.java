package no.reconic.generator.domain;

import org.springframework.stereotype.Component;

import java.net.IDN;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class DomainDiscoveryService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "(?i)(?:^|[<\\s])([a-z0-9.!#$%&'*+/=?^_`{|}~-]+)@([a-z0-9\\p{L}.-]+)(?:>|\\s|$)"
    );

    private static final Pattern DOMAIN_PATTERN = Pattern.compile(
            "(?=.{1,253}$)(?:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\\.)+"
                    + "[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?"
    );

    private static final Pattern IPV4_PATTERN = Pattern.compile(
            "^(?:25[0-5]|2[0-4]\\d|1?\\d?\\d)(?:\\.(?:25[0-5]|2[0-4]\\d|1?\\d?\\d)){3}$"
    );

    private static final Set<String> FREE_EMAIL_DOMAINS = Set.of(
            "gmail.com", "googlemail.com", "hotmail.com", "hotmail.no", "outlook.com",
            "live.com", "live.no", "msn.com", "yahoo.com", "yahoo.no", "icloud.com",
            "me.com", "online.no", "broadpark.no", "lyse.net", "start.no", "proton.me",
            "protonmail.com"
    );

    private static final Set<String> NON_OWNED_WEBSITE_DOMAINS = Set.of(
            "facebook.com", "linkedin.com", "instagram.com", "twitter.com", "x.com",
            "youtube.com", "tiktok.com", "proff.no", "purehelp.no", "gulesider.no",
            "1881.no", "google.com", "google.no", "wixsite.com", "wordpress.com",
            "webflow.io", "squarespace.com", "github.io", "myshopify.com"
    );

    private static final Set<String> MULTI_LABEL_PUBLIC_SUFFIXES = Set.of(
            "co.uk", "org.uk", "me.uk", "ac.uk", "gov.uk",
            "com.au", "net.au", "org.au",
            "co.nz", "org.nz", "net.nz",
            "co.jp", "co.za", "com.br"
    );

    public DomainCandidate discover(String registeredWebsite, String registeredEmail) {
        NormalizationResult websiteResult = normalizeWebsite(registeredWebsite);
        if (websiteResult.domain() != null) {
            return new DomainCandidate(
                    websiteResult.domain(),
                    DomainSource.REGISTERED_WEBSITE,
                    DomainConfidence.HIGH,
                    false,
                    "Domenet er normalisert fra virksomhetens registrerte hjemmeside."
            );
        }

        NormalizationResult emailResult = normalizeEmail(registeredEmail);
        if (emailResult.domain() != null) {
            String explanation = registeredWebsite == null || registeredWebsite.isBlank()
                    ? "Ingen gyldig hjemmeside var registrert. Domenet er hentet fra virksomhetens registrerte e-postadresse."
                    : "Den registrerte hjemmesiden kunne ikke brukes (" + websiteResult.reason()
                    + "). Domenet er derfor hentet fra registrert e-postadresse.";

            return new DomainCandidate(
                    emailResult.domain(),
                    DomainSource.REGISTERED_EMAIL,
                    DomainConfidence.MEDIUM,
                    true,
                    explanation
            );
        }

        return DomainCandidate.none(buildMissingExplanation(
                registeredWebsite,
                websiteResult.reason(),
                registeredEmail,
                emailResult.reason()
        ));
    }

    NormalizationResult normalizeWebsite(String value) {
        if (value == null || value.isBlank()) {
            return NormalizationResult.invalid("ingen hjemmeside registrert");
        }

        String input = stripWrappingCharacters(value.trim());
        if (input.isBlank()) {
            return NormalizationResult.invalid("tom hjemmesideverdi");
        }

        if (input.regionMatches(true, 0, "mailto:", 0, 7)) {
            return NormalizationResult.invalid("hjemmesidefeltet inneholder en e-postadresse");
        }

        String withScheme = ensureScheme(input);
        String host;
        try {
            URI uri = new URI(withScheme);
            host = extractHost(uri);
        } catch (URISyntaxException | IllegalArgumentException exception) {
            return NormalizationResult.invalid("ugyldig nettadresse");
        }

        String normalized = normalizeHost(host);
        if (normalized == null) {
            return NormalizationResult.invalid("kunne ikke finne et gyldig domene i hjemmesiden");
        }

        String registrableDomain = toRegistrableDomain(normalized);
        if (isNonOwnedWebsiteDomain(registrableDomain)) {
            return NormalizationResult.invalid("hjemmesiden peker til en delt plattform eller katalog");
        }

        return NormalizationResult.valid(registrableDomain);
    }

    NormalizationResult normalizeEmail(String value) {
        if (value == null || value.isBlank()) {
            return NormalizationResult.invalid("ingen e-postadresse registrert");
        }

        Matcher matcher = EMAIL_PATTERN.matcher(value.trim());
        if (!matcher.find()) {
            return NormalizationResult.invalid("ugyldig e-postadresse");
        }

        String normalized = normalizeHost(matcher.group(2));
        if (normalized == null) {
            return NormalizationResult.invalid("ugyldig e-postdomene");
        }

        String registrableDomain = toRegistrableDomain(normalized);
        if (FREE_EMAIL_DOMAINS.contains(registrableDomain)) {
            return NormalizationResult.invalid("e-postadressen bruker en privat eller gratis e-postleverandør");
        }
        if (isNonOwnedWebsiteDomain(registrableDomain)) {
            return NormalizationResult.invalid("e-postdomenet tilhører en delt plattform");
        }

        return NormalizationResult.valid(registrableDomain);
    }

    private String buildMissingExplanation(
            String website,
            String websiteReason,
            String email,
            String emailReason
    ) {
        if ((website == null || website.isBlank()) && (email == null || email.isBlank())) {
            return "Brønnøysund har verken registrert hjemmeside eller e-postadresse for virksomheten.";
        }

        return "Domene ikke funnet. Hjemmeside: " + safeReason(websiteReason)
                + ". E-post: " + safeReason(emailReason) + ".";
    }

    private String safeReason(String reason) {
        return reason == null || reason.isBlank() ? "ikke tilgjengelig" : reason;
    }

    private String stripWrappingCharacters(String value) {
        String result = value;
        while (result.length() >= 2
                && ((result.startsWith("\"") && result.endsWith("\""))
                || (result.startsWith("'") && result.endsWith("'")))) {
            result = result.substring(1, result.length() - 1).trim();
        }
        return result;
    }

    private String ensureScheme(String value) {
        if (value.startsWith("//")) {
            return "https:" + value;
        }
        if (value.matches("(?i)^[a-z][a-z0-9+.-]*://.*")) {
            return value;
        }
        return "https://" + value;
    }

    private String extractHost(URI uri) {
        if (uri.getHost() != null) {
            return uri.getHost();
        }

        String authority = uri.getRawAuthority();
        if (authority == null || authority.isBlank()) {
            return null;
        }

        int atIndex = authority.lastIndexOf('@');
        if (atIndex >= 0) {
            authority = authority.substring(atIndex + 1);
        }

        if (authority.startsWith("[")) {
            int closingBracket = authority.indexOf(']');
            return closingBracket > 0 ? authority.substring(1, closingBracket) : null;
        }

        int colonIndex = authority.lastIndexOf(':');
        return colonIndex > 0 ? authority.substring(0, colonIndex) : authority;
    }

    private String normalizeHost(String rawHost) {
        if (rawHost == null || rawHost.isBlank()) {
            return null;
        }

        String host = rawHost.trim().toLowerCase(Locale.ROOT);
        while (host.endsWith(".")) {
            host = host.substring(0, host.length() - 1);
        }
        host = host.replaceFirst("^www\\d*\\.", "");

        if (host.isBlank() || host.equals("localhost") || IPV4_PATTERN.matcher(host).matches() || host.contains(":")) {
            return null;
        }

        try {
            host = IDN.toASCII(host, IDN.USE_STD3_ASCII_RULES).toLowerCase(Locale.ROOT);
        } catch (IllegalArgumentException exception) {
            return null;
        }

        if (!DOMAIN_PATTERN.matcher(host).matches()) {
            return null;
        }
        return host;
    }

    private String toRegistrableDomain(String host) {
        String[] labels = host.split("\\.");
        if (labels.length <= 2) {
            return host;
        }

        String lastTwo = labels[labels.length - 2] + "." + labels[labels.length - 1];
        if (MULTI_LABEL_PUBLIC_SUFFIXES.contains(lastTwo) && labels.length >= 3) {
            return labels[labels.length - 3] + "." + lastTwo;
        }
        return lastTwo;
    }

    private boolean isNonOwnedWebsiteDomain(String domain) {
        return NON_OWNED_WEBSITE_DOMAINS.contains(domain);
    }

    record NormalizationResult(String domain, String reason) {
        static NormalizationResult valid(String domain) {
            return new NormalizationResult(domain, null);
        }

        static NormalizationResult invalid(String reason) {
            return new NormalizationResult(null, reason);
        }
    }
}
