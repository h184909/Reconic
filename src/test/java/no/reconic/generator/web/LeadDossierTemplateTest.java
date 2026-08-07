package no.reconic.generator.web;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

class LeadDossierTemplateTest {

    @Test
    void templateContainsDossierDialogAndPerLeadTemplate() throws IOException {
        String html = resource("/templates/index.html");

        assertTrue(html.contains("id=\"leadDossierDialog\""));
        assertTrue(html.contains("class=\"lead-dossier-template\""));
        assertTrue(html.contains("data-open-dossier"));
        assertTrue(html.contains("Lead dossier"));
    }

    @Test
    void dossierContainsSalesFinancialTechnicalAndProviderSections() throws IOException {
        String html = resource("/templates/index.html");

        assertTrue(html.contains("Cold-call brief"));
        assertTrue(html.contains("Betalingsevne / størrelse"));
        assertTrue(html.contains("Observerbar konfigurasjon"));
        assertTrue(html.contains("Mulige leverandørsignaler"));
        assertTrue(html.contains("Vis rå tekniske observasjoner"));
    }

    @Test
    void javascriptSupportsOpenCloseAndCopyBrief() throws IOException {
        String javascript = resource("/static/js/lead-explorer.js");

        assertTrue(javascript.contains("function openDossier(row)"));
        assertTrue(javascript.contains("function closeDossier()"));
        assertTrue(javascript.contains("function dossierBriefText()"));
        assertTrue(javascript.contains("async function copyDossierBrief()"));
    }

    @Test
    void ringReadyPresetIncludesThirtyMillionRevenueThreshold() throws IOException {
        String javascript = resource("/static/js/lead-explorer.js");

        assertTrue(javascript.contains("els.revenueMin.value = \"30\""));
        assertTrue(javascript.contains("applyRingReadyPreset"));
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
