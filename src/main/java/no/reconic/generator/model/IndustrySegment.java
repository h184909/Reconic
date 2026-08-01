package no.reconic.generator.model;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public enum IndustrySegment {
    CONSTRUCTION("Bygg/anlegg", List.of("41", "42", "43")),
    INDUSTRY("Industri", List.of("10", "16", "20", "22", "23", "24", "25", "28", "33")),
    LEGAL_ACCOUNTING("Advokat/regnskap", List.of("69.1", "69.2")),
    CONSULTING_TECHNICAL("Rådgivning/teknisk", List.of("71.1", "71.2")),
    HEALTH_CARE("Helse og omsorg", List.of("86", "87", "88")),
    TRANSPORT_LOGISTICS("Transport/logistikk", List.of("49", "50", "52", "53")),
    WHOLESALE_TRADE("Engros/handel", List.of("45", "46"));

    private final String displayName;
    private final List<String> nacePrefixes;

    IndustrySegment(String displayName, List<String> nacePrefixes) {
        this.displayName = displayName;
        this.nacePrefixes = List.copyOf(nacePrefixes);
    }

    public String getDisplayName() {
        return displayName;
    }

    public List<String> getNacePrefixes() {
        return nacePrefixes;
    }

    public boolean matches(String naceCode) {
        if (naceCode == null || naceCode.isBlank()) {
            return false;
        }

        String normalized = naceCode.trim();
        return nacePrefixes.stream().anyMatch(normalized::startsWith);
    }

    public static Optional<IndustrySegment> fromNaceCode(String naceCode) {
        return Arrays.stream(values())
                .filter(segment -> segment.matches(naceCode))
                .findFirst();
    }
}
