package no.reconic.controller;

import jakarta.validation.Valid;
import no.reconic.generator.brreg.BrregClientException;
import no.reconic.generator.model.IndustrySegment;
import no.reconic.generator.model.Municipality;
import no.reconic.generator.service.CompanyDiscoveryService;
import no.reconic.generator.web.LeadSearchForm;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class HomeController {

    private final CompanyDiscoveryService companyDiscoveryService;

    public HomeController(CompanyDiscoveryService companyDiscoveryService) {
        this.companyDiscoveryService = companyDiscoveryService;
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
            Model model
    ) {
        if (bindingResult.hasErrors()) {
            return "index";
        }

        try {
            model.addAttribute("result", companyDiscoveryService.discover(searchForm));
        } catch (BrregClientException exception) {
            model.addAttribute("searchError", exception.getMessage());
        }

        return "index";
    }
}
