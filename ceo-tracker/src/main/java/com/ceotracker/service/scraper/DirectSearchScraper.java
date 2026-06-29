package com.ceotracker.service.scraper;

import com.ceotracker.entity.CeoContact;
import java.util.ArrayList;
import java.util.List;

public class DirectSearchScraper extends BaseScraper {
    public DirectSearchScraper() { super("Direct Search", "RECHERCHE_CEO"); }
    @Override public List<CeoContact> scrape() { return new ArrayList<>(); }
}
