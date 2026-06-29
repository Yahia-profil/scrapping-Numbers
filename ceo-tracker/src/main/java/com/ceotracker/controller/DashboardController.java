package com.ceotracker.controller;

import com.ceotracker.entity.CeoContact;
import com.ceotracker.service.CeoContactService;
import com.ceotracker.service.ScrapingEventService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Controller
public class DashboardController {

    private final CeoContactService ceoContactService;
    private final ScrapingEventService eventService;

    public DashboardController(CeoContactService ceoContactService, ScrapingEventService eventService) {
        this.ceoContactService = ceoContactService;
        this.eventService = eventService;
    }

    @GetMapping("/")
    public String dashboard(Model model) {
        List<CeoContact> contacts = ceoContactService.getAll();
        model.addAttribute("contacts", contacts);
        model.addAttribute("total", contacts.size());
        model.addAttribute("sourceTypes", ceoContactService.getSourceTypes());
        return "dashboard";
    }

    @GetMapping("/stream")
    public SseEmitter stream() {
        return eventService.subscribe();
    }

    @GetMapping("/scrape")
    public String triggerScrape() {
        CompletableFuture.runAsync(() -> ceoContactService.runScraping());
        return "redirect:/";
    }

    @GetMapping("/export/csv")
    public ResponseEntity<byte[]> exportCsv() {
        List<CeoContact> contacts = ceoContactService.getAll();
        StringBuilder sb = new StringBuilder("Entreprise;Téléphone;Source;Lien\n");
        for (CeoContact c : contacts) {
            sb.append(String.join(";",
                escapeCsv(c.getCompanyName()),
                c.getPhoneNumber(),
                escapeCsv(c.getSourceType() != null ? c.getSourceType() : ""),
                escapeCsv(c.getSourceUrl() != null ? c.getSourceUrl() : "")
            )).append("\n");
        }
        byte[] bytes = sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv; charset=UTF-8"));
        headers.setContentDispositionFormData("attachment", "contacts_entreprises.csv");
        return ResponseEntity.ok().headers(headers).body(bytes);
    }

    private String escapeCsv(String s) {
        if (s == null) return "";
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }

    @GetMapping("/stats")
    public String stats(Model model) {
        long total = ceoContactService.count();
        model.addAttribute("total", total);
        return "stats";
    }
}
