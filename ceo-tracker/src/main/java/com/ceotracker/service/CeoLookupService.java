package com.ceotracker.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class CeoLookupService {
    private static final Logger log = LoggerFactory.getLogger(CeoLookupService.class);
    public CeoLookupService() { log.debug("CeoLookupService loaded (inactive)"); }
}
