package no.reconic.generator.web;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

class LeadExplorerTemplateTest {

    @Test
    void templateContainsExplorerControlsAndRowMetadata() throws IOException {
        String html = resource("/templates/index.html");

        assertTrue(html.contains("id=\"leadExplorer\""));
        assertTrue(html.contains("id=\"filterProvider\""));
        assertTrue(html.contains("data-domain-confidence"));
        assertTrue(html.contains("data-dnssec"));
        assertTrue(html.contains("provider-filter-token"));
    }

    @Test
    void templateLoadsDedicatedExplorerAssets() throws IOException {
        String html = resource("/templates/index.html");

        assertTrue(html.contains("@{/css/lead-explorer.css}"));
        assertTrue(html.contains("@{/js/lead-explorer.js}"));
    }

    @Test
    void explorerScriptSupportsInstantFilteringPresetAndFilteredExport() throws IOException {
        String javascript = resource("/static/js/lead-explorer.js");

        assertTrue(javascript.contains("function applyFilters()"));
        assertTrue(javascript.contains("function applyColdCallPreset()"));
        assertTrue(javascript.contains("function exportFilteredCsv()"));
        assertTrue(javascript.contains("providerSummaryChips"));
    }

    private String resource(String path) throws IOException {
        try (InputStream input = getClass().getResourceAsStream(path)) {
            if (input == null) {
                throw new IOException("Mangler testressurs: " + path);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
