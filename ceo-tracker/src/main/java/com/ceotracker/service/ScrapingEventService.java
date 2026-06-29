package com.ceotracker.service;

import com.ceotracker.entity.CeoContact;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class ScrapingEventService {

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(0L);
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));
        return emitter;
    }

    public void sendContact(CeoContact contact) {
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                    .name("contact")
                    .data(contact));
            } catch (Exception e) {
                emitters.remove(emitter);
            }
        }
    }

    public void sendProgress(String scraperName, int count) {
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                    .name("progress")
                    .data(String.format("{\"scraper\":\"%s\",\"count\":%d}", scraperName, count)));
            } catch (Exception e) {
                emitters.remove(emitter);
            }
        }
    }

    public void sendTotal(int total) {
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                    .name("total")
                    .data(String.valueOf(total)));
            } catch (Exception e) {
                emitters.remove(emitter);
            }
        }
    }

    public void sendDone() {
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                    .name("done")
                    .data("OK"));
                emitter.complete();
            } catch (Exception e) {
                emitters.remove(emitter);
            }
        }
    }
}
