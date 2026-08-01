package no.reconic.generator.model;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public record CompanyDiscoveryResult(
        Instant generatedAt,
        int fetchedCount,
        int candidateCount,
        int filteredOutCount,
        List<CompanyCandidate> candidates,
        Map<IndustrySegment, Long> segmentCounts
) {
    private static final DateTimeFormatter DISPLAY_FORMAT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

    public CompanyDiscoveryResult {
        candidates = List.copyOf(candidates);
        segmentCounts = Map.copyOf(segmentCounts);
    }

    public String generatedAtDisplay() {
        return DISPLAY_FORMAT.format(generatedAt.atZone(ZoneId.systemDefault()));
    }
}
