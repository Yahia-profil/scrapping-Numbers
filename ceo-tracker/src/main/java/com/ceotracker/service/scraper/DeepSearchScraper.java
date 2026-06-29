package com.ceotracker.service.scraper;

import com.ceotracker.entity.CeoContact;
import java.util.ArrayList;
import java.util.List;

public class DeepSearchScraper extends BaseScraper {
    public DeepSearchScraper() { super("Deep Search", "RECHERCHE_APPROFONDIE"); }
    @Override public List<CeoContact> scrape() { return new ArrayList<>(); }
}
