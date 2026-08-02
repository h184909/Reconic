package no.reconic.controller;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import no.reconic.generator.brreg.BrregClientException;
import no.reconic.generator.export.CsvExportService;
import no.reconic.generator.model.CompanyDiscoveryResult;
import no.reconic.generator.model.IndustrySegment;
import no.reconic.generator.model.Municipality;
import no.reconic.generator.service.CompanyDiscoveryService;
import no.reconic.generator.web.LeadSearchForm;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Controller
public class HomeController {

    private static final String LAST_RESULT_SESSION_KEY = "reconicLastDiscoveryResult";
    private static final DateTimeFormatter FILE_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final CompanyDiscoveryService companyDiscoveryService;
    private final CsvExportService csvExportService;

    public HomeController(
            CompanyDiscoveryService companyDiscoveryService,
            CsvExportService csvExportService
    ) {
        this.companyDiscoveryService = companyDiscoveryService;
        this.csvExportService = csvExportService;
    }

    @ModelAttribute("municipalities")
    public Municipality[] municipalities() {
        return Municipality.values();
    }

    @ModelAttribute("segments")
    public IndustrySegment[] segments() {
        return IndustrySegment.values();
    }

    @GetMapping("/")
    public String home(Model model) {
        if (!model.containsAttribute("searchForm")) {
            model.addAttribute("searchForm", new LeadSearchForm());
        }
        return "index";
    }

    @PostMapping("/search")
    public String search(
            @Valid @ModelAttribute("searchForm") LeadSearchForm searchForm,
            BindingResult bindingResult,
            Model model,
            HttpSession session
    ) {
        if (bindingResult.hasErrors()) {
            return "index";
        }

        try {
            CompanyDiscoveryResult result = companyDiscoveryService.discover(searchForm);
            model.addAttribute("result", result);
            session.setAttribute(LAST_RESULT_SESSION_KEY, result);
        } catch (BrregClientException exception) {
            model.addAttribute("searchError", exception.getMessage());
        }

        return "index";
    }

    @GetMapping("/export/all.csv")
    public ResponseEntity<byte[]> exportAll(HttpSession session) {
        CompanyDiscoveryResult result = lastResult(session);
        if (result == null) {
            return missingResultResponse();
        }
        return csvResponse(
                csvExportService.exportAll(result),
                "reconic-all-" + timestamp() + ".csv"
        );
    }

    @GetMapping("/export/validation.csv")
    public ResponseEntity<byte[]> exportValidation(HttpSession session) {
        CompanyDiscoveryResult result = lastResult(session);
        if (result == null) {
            return missingResultResponse();
        }
        return csvResponse(
                csvExportService.exportValidationSample(result),
                "reconic-validation-50-" + timestamp() + ".csv"
        );
    }

    private CompanyDiscoveryResult lastResult(HttpSession session) {
        Object value = session.getAttribute(LAST_RESULT_SESSION_KEY);
        return value instanceof CompanyDiscoveryResult result ? result : null;
    }

    private ResponseEntity<byte[]> csvResponse(byte[] content, String filename) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(content);
    }

    private ResponseEntity<byte[]> missingResultResponse() {
        byte[] message = "Kjør et kandidatsøk før du eksporterer.".getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .contentType(MediaType.TEXT_PLAIN)
                .body(message);
    }

    private String timestamp() {
        return FILE_TIMESTAMP.format(LocalDateTime.now());
    }
}
