package com.ceotracker.controller;

import com.ceotracker.entity.CeoContact;
import com.ceotracker.service.CeoContactService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

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
                            @RequestParam(name = "ville", defaultValue = "CASABLANCA") String ville) {
        List<CeoContact> contacts = ceoContactService.getByCity(ville);
        model.addAttribute("contacts", contacts);
        model.addAttribute("total", contacts.size());
        model.addAttribute("ville", ville);
        model.addAttribute("villes", ceoContactService.getAvailableCities());
        return "dashboard";
    }

    @GetMapping("/status")
    public String updateStatus(@RequestParam Long id, @RequestParam String status,
                               @RequestParam(defaultValue = "CASABLANCA") String ville) {
        ceoContactService.updateStatus(id, status);
        return "redirect:/?ville=" + ville;
    }

    @GetMapping("/export/csv")
    public ResponseEntity<byte[]> exportCsv(@RequestParam(name = "ville", defaultValue = "CASABLANCA") String ville) {
        List<CeoContact> contacts = ceoContactService.getByCity(ville);
        // UTF-8 BOM for Excel compatibility
        byte[] bom = new byte[]{(byte)0xEF, (byte)0xBB, (byte)0xBF};
        StringBuilder sb = new StringBuilder("Entreprise;Contact;Fonction;GSM;Statut\n");
        for (CeoContact c : contacts) {
            sb.append(String.join(";",
                escapeCsv(c.getCompanyName()),
                escapeCsv(c.getCeoName() != null ? c.getCeoName() : ""),
                escapeCsv(c.getJobTitle() != null ? c.getJobTitle() : ""),
                escapeCsv(c.getPhoneNumber()),
                escapeCsv(c.getStatus())
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
