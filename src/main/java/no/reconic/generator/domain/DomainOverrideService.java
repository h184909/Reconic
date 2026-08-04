package no.reconic.generator.domain;

import no.reconic.generator.model.CompanyCandidate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.IDN;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class DomainOverrideService {

    private static final Logger log = LoggerFactory.getLogger(DomainOverrideService.class);
    private static final String RESOURCE_PATH = "domain-overrides.csv";
    private static final Pattern DOMAIN_PATTERN = Pattern.compile(
            "(?=.{1,253}$)(?:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\\.)+"
                    + "[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?"
    );

    private final Map<String, DomainOverride> overrides;

    public DomainOverrideService() {
        this(loadOverrides());
    }

    DomainOverrideService(Map<String, DomainOverride> overrides) {
        this.overrides = overrides == null ? Map.of() : Map.copyOf(overrides);
    }

    public List<CompanyCandidate> apply(List<CompanyCandidate> candidates) {
        if (candidates == null || candidates.isEmpty() || overrides.isEmpty()) {
            return candidates == null ? List.of() : List.copyOf(candidates);
        }
        return candidates.stream()
                .map(this::apply)
                .toList();
    }

    CompanyCandidate apply(CompanyCandidate candidate) {
        if (candidate == null || candidate.organizationNumber() == null) {
            return candidate;
        }

        DomainOverride override = overrides.get(candidate.organizationNumber().trim());
        if (override == null) {
            return candidate;
        }

        DomainCandidate previous = candidate.domainCandidate();
        String previousDomain = previous == null ? null : previous.domain();
        String explanation = "Manuell domenefasit brukt for organisasjonsnummer "
                + candidate.organizationNumber() + ". " + override.reason();
        if (previousDomain != null && !previousDomain.equalsIgnoreCase(override.domain())) {
            explanation += " Automatisk forslag var " + previousDomain + ".";
        }

        return candidate.withDomainCandidate(new DomainCandidate(
                override.domain(),
                DomainSource.MANUAL_OVERRIDE,
                DomainConfidence.HIGH,
                false,
                explanation
        ));
    }

    public int overrideCount() {
        return overrides.size();
    }

    private static Map<String, DomainOverride> loadOverrides() {
        ClassPathResource resource = new ClassPathResource(RESOURCE_PATH);
        if (!resource.exists()) {
            return Map.of();
        }

        Map<String, DomainOverride> result = new LinkedHashMap<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                resource.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            boolean firstLine = true;
            while ((line = reader.readLine()) != null) {
                String trimmed = removeBom(line).trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                if (firstLine && trimmed.toLowerCase(Locale.ROOT).startsWith("organizationnumber;")) {
                    firstLine = false;
                    continue;
                }
                firstLine = false;

                String[] parts = trimmed.split(";", 3);
                if (parts.length < 2) {
                    log.warn("Ignorerer ugyldig rad i {}: {}", RESOURCE_PATH, trimmed);
                    continue;
                }

                String organizationNumber = parts[0].trim();
                String domain = normalizeDomain(parts[1]);
                String reason = parts.length == 3 && !parts[2].isBlank()
                        ? parts[2].trim()
                        : "Bekreftet manuelt.";

                if (!organizationNumber.matches("\\d{9}") || domain == null) {
                    log.warn("Ignorerer ugyldig domeneoverride i {}: {}", RESOURCE_PATH, trimmed);
                    continue;
                }
                result.put(organizationNumber, new DomainOverride(domain, reason));
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Kunne ikke lese " + RESOURCE_PATH, exception);
        }
        return result;
    }

    private static String normalizeDomain(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String domain = value.trim().toLowerCase(Locale.ROOT);
        domain = domain.replaceFirst("(?i)^https?://", "");
        domain = domain.replaceFirst("(?i)^www\\d*\\.", "");
        int slash = domain.indexOf('/');
        if (slash >= 0) {
            domain = domain.substring(0, slash);
        }
        while (domain.endsWith(".")) {
            domain = domain.substring(0, domain.length() - 1);
        }
        try {
            domain = IDN.toASCII(domain, IDN.USE_STD3_ASCII_RULES).toLowerCase(Locale.ROOT);
        } catch (IllegalArgumentException exception) {
            return null;
        }
        return DOMAIN_PATTERN.matcher(domain).matches() ? domain : null;
    }

    private static String removeBom(String value) {
        return value != null && value.startsWith("\uFEFF") ? value.substring(1) : value;
    }

    record DomainOverride(String domain, String reason) {
        DomainOverride {
            domain = normalizeDomain(domain);
            reason = reason == null || reason.isBlank() ? "Bekreftet manuelt." : reason.trim();
            if (domain == null) {
                throw new IllegalArgumentException("Override-domene må være gyldig");
            }
        }
    }
}
