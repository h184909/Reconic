package no.reconic.generator.export;

import no.reconic.generator.domain.DomainConfidence;
import no.reconic.generator.model.CompanyCandidate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class ValidationSampleService {

    private static final int DEFAULT_SAMPLE_SIZE = 50;
    private static final int HIGH_CONFIDENCE_TARGET = 30;
    private static final int MEDIUM_CONFIDENCE_TARGET = 20;

    private final SecureRandom random = new SecureRandom();

    public List<CompanyCandidate> createSample(List<CompanyCandidate> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }

        List<CompanyCandidate> withDomain = candidates.stream()
                .filter(candidate -> candidate.domainCandidate() != null)
                .filter(candidate -> candidate.domainCandidate().hasDomain())
                .toList();

        if (withDomain.size() <= DEFAULT_SAMPLE_SIZE) {
            List<CompanyCandidate> all = new ArrayList<>(withDomain);
            Collections.shuffle(all, random);
            return List.copyOf(all);
        }

        List<CompanyCandidate> high = shuffled(withDomain.stream()
                .filter(candidate -> candidate.domainCandidate().confidence() == DomainConfidence.HIGH)
                .toList());
        List<CompanyCandidate> medium = shuffled(withDomain.stream()
                .filter(candidate -> candidate.domainCandidate().confidence() == DomainConfidence.MEDIUM)
                .toList());

        Set<CompanyCandidate> selected = new LinkedHashSet<>();
        addUpTo(selected, high, HIGH_CONFIDENCE_TARGET);
        addUpTo(selected, medium, MEDIUM_CONFIDENCE_TARGET);

        if (selected.size() < DEFAULT_SAMPLE_SIZE) {
            List<CompanyCandidate> remaining = shuffled(withDomain.stream()
                    .filter(candidate -> !selected.contains(candidate))
                    .toList());
            addUpTo(selected, remaining, DEFAULT_SAMPLE_SIZE - selected.size());
        }

        List<CompanyCandidate> sample = new ArrayList<>(selected);
        Collections.shuffle(sample, random);
        return List.copyOf(sample);
    }

    private List<CompanyCandidate> shuffled(List<CompanyCandidate> values) {
        List<CompanyCandidate> result = new ArrayList<>(values);
        Collections.shuffle(result, random);
        return result;
    }

    private void addUpTo(Set<CompanyCandidate> target, List<CompanyCandidate> source, int count) {
        int remaining = Math.max(0, count);
        for (CompanyCandidate candidate : source) {
            if (remaining == 0) {
                return;
            }
            if (target.add(candidate)) {
                remaining--;
            }
        }
    }
}
