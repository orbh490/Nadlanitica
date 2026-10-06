package com.realestate.analytics.controller;

import com.realestate.analytics.service.LandTenderService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/land-tenders")
class LandTenderController {

    private final LandTenderService service;

    LandTenderController(LandTenderService service) {
        this.service = service;
    }

    @GetMapping("/self-build")
    List<LandTenderService.LandTenderSummary> selfBuildTenders() {
        return service.activeSelfBuildTenders();
    }

    @GetMapping("/city-results")
    List<LandTenderService.CityLandResult> cityResults() {
        return service.cityLandResults();
    }

    @GetMapping("/{id}")
    LandTenderService.LandTenderDetail tenderDetail(@PathVariable long id) {
        return service.tenderDetail(id);
    }

    @GetMapping(value = "/{id}/booklet", produces = MediaType.APPLICATION_PDF_VALUE)
    byte[] booklet(@PathVariable long id) {
        return service.bookletPdf(id);
    }

    @GetMapping("/{id}/discounts")
    LandTenderService.BookletDiscountInfo discounts(@PathVariable long id) {
        return service.bookletDiscounts(id);
    }

    @GetMapping("/{id}/discount-suggestion")
    LandTenderService.DiscountSuggestion discountSuggestion(@PathVariable long id, @RequestParam int populationCode) {
        return service.discountSuggestion(id, populationCode);
    }
}
