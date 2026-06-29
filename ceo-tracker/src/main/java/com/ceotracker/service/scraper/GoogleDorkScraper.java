package com.ceotracker.service.scraper;

import com.ceotracker.entity.CeoContact;
import java.util.ArrayList;
import java.util.List;

public class GoogleDorkScraper extends BaseScraper {
    public GoogleDorkScraper() { super("Google Dorks", "RECHERCHE_WEB"); }
    @Override public List<CeoContact> scrape() { return new ArrayList<>(); }
}
