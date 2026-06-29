package com.ceotracker.service.scraper;

import com.ceotracker.entity.CeoContact;

import java.util.List;

public abstract class BaseScraper {

    protected final String name;
    protected final String sourceType;

    protected BaseScraper(String name, String sourceType) {
        this.name = name;
        this.sourceType = sourceType;
    }

    public abstract List<CeoContact> scrape();

    public String getName() { return name; }
    public String getSourceType() { return sourceType; }
}
