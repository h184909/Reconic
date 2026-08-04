package no.reconic.generator.scoring;

import no.reconic.generator.dns.DnsLookupStatus;
import no.reconic.generator.dns.DnsObservation;
import no.reconic.generator.domain.DomainConfidence;
import no.reconic.generator.intelligence.DmarcPosture;
import no.reconic.generator.intelligence.EmailPlatform;
import no.reconic.generator.intelligence.ProviderRole;
import no.reconic.generator.intelligence.ProviderSignal;
import no.reconic.generator.intelligence.SignalConfidence;
import no.reconic.generator.intelligence.SpfPosture;
import no.reconic.generator.intelligence.TechnologyObservation;
import no.reconic.generator.model.CompanyCandidate;
import no.reconic.generator.model.IndustrySegment;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class OpportunityScoringService {

    public List<CompanyCandidate> enrich(List<CompanyCandidate> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }
        return candidates.stream()
                .map(candidate -> candidate == null
                        ? null
                        : candidate.withOpportunityAssessment(score(candidate)))
                .toList();
    }

    public OpportunityAssessment score(CompanyCandidate candidate) {
        if (candidate == null) {
            return OpportunityAssessment.empty();
        }

        List<String> reasons = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<String> evidence = new ArrayList<>();

        int marketFit = marketFit(candidate, reasons, evidence);
        int technicalOpportunity = technicalOpportunity(candidate, reasons, warnings, evidence);
        int providerLandscape = providerLandscape(candidate, reasons, warnings, evidence);
        int total = Math.min(100, marketFit + technicalOpportunity + providerLandscape);
        int dataConfidence = dataConfidence(candidate, warnings, evidence);

        if (reasons.isEmpty()) {
            reasons.add("Ingen sterke opportunity-signaler er observert ennå; kandidaten krever manuell vurdering.");
        }

        return new OpportunityAssessment(
                total,
                OpportunityPriority.fromScore(total),
                marketFit,
                technicalOpportunity,
                providerLandscape,
                dataConfidence,
                reasons,
                warnings,
                evidence
        );
    }

    private int marketFit(
            CompanyCandidate candidate,
            List<String> reasons,
            List<String> evidence
    ) {
        int employeeScore = employeeScore(candidate.employees());
        int segmentScore = segmentScore(candidate.segment());
        int score = Math.min(35, employeeScore + segmentScore);

        if (employeeScore >= 20) {
            reasons.add(candidate.employees() + " ansatte er innenfor Reconic sin foreløpige kjernemålgruppe.");
        } else {
            reasons.add(candidate.employees() + " ansatte gir en moderat markedsmatch.");
        }
        if (candidate.segment() != null) {
            evidence.add("Bransjesegment: " + candidate.segment().getDisplayName());
        }
        evidence.add("Markedsmatch: ansatte " + employeeScore + " poeng, bransje " + segmentScore + " poeng");
        return score;
    }

    private int technicalOpportunity(
            CompanyCandidate candidate,
            List<String> reasons,
            List<String> warnings,
            List<String> evidence
    ) {
        if (candidate.domainCandidate() == null || !candidate.domainCandidate().hasDomain()) {
            warnings.add("Tekniske opportunity-signaler kunne ikke beregnes fordi virksomhetsdomene mangler.");
            return 0;
        }

        TechnologyObservation technology = candidate.technologyObservation();
        if (technology == null) {
            warnings.add("Teknologianalysen mangler.");
            return 0;
        }

        int score = 0;
        switch (technology.dmarcPosture()) {
            case MISSING -> {
                score += 15;
                reasons.add("Ingen offentlig DMARC-policy ble observert; dette er et konkret område å undersøke.");
            }
            case MONITORING -> {
                score += 10;
                reasons.add("DMARC står i overvåkingsmodus (p=none), uten håndheving.");
            }
            case INVALID -> {
                score += 12;
                reasons.add("DMARC-posten kunne ikke tolkes som en kjent policy.");
            }
            case UNKNOWN -> warnings.add("DMARC-status er ukjent på grunn av manglende eller teknisk usikker data.");
            case QUARANTINE, REJECT -> evidence.add("DMARC håndheves med "
                    + technology.dmarcPosture().getDisplayName().toLowerCase() + ".");
        }

        switch (technology.spfPosture()) {
            case MULTIPLE -> {
                score += 14;
                reasons.add("Flere SPF-poster ble observert og bør undersøkes som mulig konfigurasjonsproblem.");
            }
            case PASS_ALL -> {
                score += 14;
                reasons.add("SPF tillater alle avsendere (+all), et tydelig teknisk kontrollpunkt.");
            }
            case MISSING -> {
                score += 9;
                reasons.add("Ingen offentlig SPF-policy ble observert.");
            }
            case NEUTRAL -> {
                score += 7;
                reasons.add("SPF avsluttes med neutral (?all), som gir et mulig forbedringsområde.");
            }
            case SOFT_FAIL -> {
                score += 5;
                reasons.add("SPF bruker softfail (~all), som kan være verdt å gjennomgå.");
            }
            case REDIRECTED -> {
                score += 2;
                evidence.add("SPF-policy delegeres til " + technology.spfRedirectTarget() + ".");
            }
            case PRESENT -> score += 3;
            case UNKNOWN -> warnings.add("SPF-status er ukjent på grunn av manglende eller teknisk usikker data.");
            case HARD_FAIL -> evidence.add("SPF avsluttes med hardfail (-all).");
        }

        if (technology.emailPlatform() == EmailPlatform.MICROSOFT_365) {
            score += 1;
            evidence.add("E-postplattform: Microsoft 365.");
            if (technology.dmarcPosture() == DmarcPosture.MISSING
                    || technology.dmarcPosture() == DmarcPosture.MONITORING) {
                score += 4;
                reasons.add("Microsoft 365 kombinert med svakere DMARC-håndheving gir en konkret samtalestarter.");
            }
        } else if (technology.emailPlatform() == EmailPlatform.GOOGLE_WORKSPACE) {
            score += 1;
            evidence.add("E-postplattform: Google Workspace.");
        } else if (technology.emailPlatform() == EmailPlatform.OTHER) {
            score += 2;
            evidence.add("E-postplattformen matcher ikke en foreløpig kjent standardsignatur.");
        }

        return Math.min(45, score);
    }

    private int providerLandscape(
            CompanyCandidate candidate,
            List<String> reasons,
            List<String> warnings,
            List<String> evidence
    ) {
        TechnologyObservation technology = candidate.technologyObservation();
        if (technology == null || candidate.domainCandidate() == null || !candidate.domainCandidate().hasDomain()) {
            return 0;
        }

        List<ProviderSignal> allSignals = technology.providerSignals();
        List<ProviderSignal> mspSignals = technology.mspCandidateSignals();

        if (mspSignals.isEmpty()) {
            if (allSignals.isEmpty()) {
                reasons.add("Ingen kjent leverandørsignatur ble funnet i offentlig MX, SPF eller NS.");
                evidence.add("Fravær av signatur er ikke bevis på at virksomheten mangler IT-leverandør.");
                return 16;
            }

            String roles = String.join(", ", allSignals.stream()
                    .map(signal -> signal.role().getDisplayName())
                    .distinct()
                    .toList());
            reasons.add("Bare infrastrukturspor ble funnet (" + roles
                    + "); ingen tydelig MSP-signatur er observert.");
            evidence.add("Infrastruktursignaler: " + String.join(", ", allSignals.stream()
                    .map(ProviderSignal::provider)
                    .toList()));
            return 12;
        }

        if (mspSignals.size() == 1) {
            ProviderSignal signal = mspSignals.getFirst();
            reasons.add("Mulig konkurrent-/driftssignal: " + signal.provider()
                    + " (" + signal.sourcesDisplay() + ").");
            evidence.add("Leverandørrolle: " + signal.role().getDisplayName()
                    + ", konfidens " + signal.confidence().getDisplayName().toLowerCase() + ".");
            return signal.confidence() == SignalConfidence.HIGH ? 7 : 9;
        }

        warnings.add("Flere mulige MSP-signaturer ble observert; leverandørbildet kan være delt eller historisk.");
        evidence.add("Mulige MSP-signaler: " + String.join(", ", mspSignals.stream()
                .map(ProviderSignal::provider)
                .toList()));
        return 6;
    }

    private int dataConfidence(
            CompanyCandidate candidate,
            List<String> warnings,
            List<String> evidence
    ) {
        int score = 0;

        DomainConfidence domainConfidence = candidate.domainCandidate() == null
                ? DomainConfidence.NONE
                : candidate.domainCandidate().confidence();
        score += switch (domainConfidence) {
            case HIGH -> 35;
            case MEDIUM -> 22;
            case LOW -> 10;
            case NONE -> 0;
        };
        if (domainConfidence == DomainConfidence.MEDIUM) {
            warnings.add("Domenet er hentet fra registrert e-post og bør verifiseres manuelt.");
        } else if (domainConfidence == DomainConfidence.LOW) {
            warnings.add("Domenet har lav konfidens.");
        } else if (domainConfidence == DomainConfidence.NONE) {
            warnings.add("Virksomhetsdomene er ikke funnet.");
        }

        DnsObservation dns = candidate.dnsObservation();
        DnsLookupStatus dnsStatus = dns == null ? DnsLookupStatus.SKIPPED : dns.status();
        score += switch (dnsStatus) {
            case SUCCESS -> 30;
            case PARTIAL -> 18;
            case FAILED -> 5;
            case SKIPPED -> 0;
        };
        if (dnsStatus == DnsLookupStatus.PARTIAL) {
            warnings.add("DNS-analysen ble bare delvis fullført.");
        } else if (dnsStatus == DnsLookupStatus.FAILED) {
            warnings.add("DNS-analysen feilet.");
        }

        if (present(candidate.website()) || present(candidate.email())) {
            score += 5;
        }
        if (present(candidate.phone())) {
            score += 5;
        }
        if (present(candidate.address())) {
            score += 5;
        }
        if (present(candidate.organizationNumber())) {
            score += 5;
        }

        TechnologyObservation technology = candidate.technologyObservation();
        if (technology != null) {
            score += switch (technology.emailPlatformConfidence()) {
                case HIGH -> 7;
                case MEDIUM -> 4;
                case LOW -> 2;
                case NONE -> 0;
            };
            if (technology.dmarcPosture() != DmarcPosture.UNKNOWN) {
                score += 4;
            }
            if (technology.spfPosture() != SpfPosture.UNKNOWN) {
                score += 4;
            }
        }

        int clamped = Math.min(100, score);
        evidence.add("Datatillit: domene " + domainConfidence.getDisplayName()
                + ", DNS " + dnsStatus.getDisplayName() + ".");
        return clamped;
    }

    private int employeeScore(int employees) {
        if (employees >= 40 && employees <= 79) {
            return 24;
        }
        if (employees >= 80 && employees <= 120) {
            return 22;
        }
        if (employees >= 25 && employees <= 39) {
            return 20;
        }
        if (employees >= 121 && employees <= 200) {
            return 16;
        }
        if (employees >= 10 && employees <= 24) {
            return 10;
        }
        if (employees > 200) {
            return 12;
        }
        return 4;
    }

    private int segmentScore(IndustrySegment segment) {
        if (segment == null) {
            return 0;
        }
        return switch (segment) {
            case LEGAL_ACCOUNTING -> 11;
            case CONSULTING_TECHNICAL, INDUSTRY -> 10;
            case CONSTRUCTION -> 9;
            case TRANSPORT_LOGISTICS -> 8;
            case WHOLESALE_TRADE -> 7;
            case HEALTH_CARE -> 6;
        };
    }

    private boolean present(String value) {
        return value != null && !value.isBlank();
    }
}
