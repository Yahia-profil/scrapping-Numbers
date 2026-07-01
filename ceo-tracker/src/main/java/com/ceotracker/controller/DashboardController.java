package com.ceotracker.controller;

import com.ceotracker.entity.CeoContact;
import com.ceotracker.service.CeoContactService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Controller
public class DashboardController {

    private final CeoContactService ceoContactService;

    public DashboardController(CeoContactService ceoContactService) {
        this.ceoContactService = ceoContactService;
    }

    @GetMapping("/")
    public String dashboard(Model model,
                            @RequestParam(name = "ville", defaultValue = "CASABLANCA") String ville,
                            @RequestParam(name = "activite", defaultValue = "TOUTES") String activite,
                            @RequestParam(name = "statut", defaultValue = "TOUS") String statut,
                            @RequestParam(name = "q", defaultValue = "") String q) {
        List<CeoContact> contacts = ceoContactService.search(ville, activite, q, statut);
        model.addAttribute("contacts", contacts);
        model.addAttribute("total", contacts.size());
        model.addAttribute("ville", ville);
        model.addAttribute("activite", activite);
        model.addAttribute("statut", statut);
        model.addAttribute("q", q);
        model.addAttribute("villes", ceoContactService.getAvailableCities());
        model.addAttribute("activites", ceoContactService.getAvailableActivities());
        return "dashboard";
    }

    @GetMapping("/search")
    public String searchRows(Model model,
                             @RequestParam(name = "ville", defaultValue = "CASABLANCA") String ville,
                             @RequestParam(name = "activite", defaultValue = "TOUTES") String activite,
                             @RequestParam(name = "statut", defaultValue = "TOUS") String statut,
                             @RequestParam(name = "q", defaultValue = "") String q) {
        List<CeoContact> contacts = ceoContactService.search(ville, activite, q, statut);
        model.addAttribute("contacts", contacts);
        model.addAttribute("nombre", contacts.size());
        model.addAttribute("ville", ville);
        model.addAttribute("activite", activite);
        model.addAttribute("statut", statut);
        model.addAttribute("q", q);
        return "fragments :: tableRows";
    }

    @PostMapping("/notes")
    @ResponseBody
    public String saveNotes(@RequestParam Long id, @RequestParam String notes) {
        ceoContactService.updateNotes(id, notes);
        return "ok";
    }

    @GetMapping("/status")
    public String updateStatus(@RequestParam Long id, @RequestParam String status,
                               @RequestParam(defaultValue = "CASABLANCA") String ville,
                               @RequestParam(defaultValue = "TOUTES") String activite,
                               @RequestParam(defaultValue = "TOUS") String statut,
                               @RequestParam(defaultValue = "") String q) {
        ceoContactService.updateStatus(id, status);
        String params = "?ville=" + ville + "&activite=" + activite + "&statut=" + statut;
        if (!q.isEmpty()) params += "&q=" + q;
        return "redirect:/" + params;
    }

    @GetMapping("/export/csv")
    public ResponseEntity<byte[]> exportCsv(@RequestParam(name = "ville", defaultValue = "CASABLANCA") String ville,
                                            @RequestParam(name = "activite", defaultValue = "TOUTES") String activite,
                                            @RequestParam(name = "statut", defaultValue = "TOUS") String statut,
                                            @RequestParam(name = "q", defaultValue = "") String q) {
        List<CeoContact> contacts = ceoContactService.search(ville, activite, q, statut);
        byte[] bom = new byte[]{(byte)0xEF, (byte)0xBB, (byte)0xBF};
        StringBuilder sb = new StringBuilder("Entreprise;Contact;Fonction;Activit\u00e9;GSM;Statut;Notes\n");
        for (CeoContact c : contacts) {
            sb.append(String.join(";",
                escapeCsv(c.getCompanyName()),
                escapeCsv(c.getCeoName() != null ? c.getCeoName() : ""),
                escapeCsv(c.getJobTitle() != null ? c.getJobTitle() : ""),
                escapeCsv(c.getActivity() != null ? c.getActivity() : ""),
                escapeCsv(c.getPhoneNumber()),
                escapeCsv(c.getStatus()),
                escapeCsv(c.getNotes())
            )).append("\n");
        }
        byte[] content = sb.toString().getBytes(StandardCharsets.UTF_8);
        byte[] bytes = new byte[bom.length + content.length];
        System.arraycopy(bom, 0, bytes, 0, bom.length);
        System.arraycopy(content, 0, bytes, bom.length, content.length);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv; charset=UTF-8"));
        headers.setContentDispositionFormData("attachment", "contacts_" + ville + ".csv");
        return ResponseEntity.ok().headers(headers).body(bytes);
    }

    private String escapeCsv(String s) {
        if (s == null) return "";
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }
}
