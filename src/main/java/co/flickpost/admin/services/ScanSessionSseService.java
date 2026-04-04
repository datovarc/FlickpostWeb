package co.flickpost.admin.services;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class ScanSessionSseService {

    private static final Logger logger = LogManager.getLogger(ScanSessionSseService.class);
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(0L);
        emitters.add(emitter);

        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> {
            emitters.remove(emitter);
            emitter.complete();
        });
        emitter.onError(error -> emitters.remove(emitter));

        try {
            emitter.send(SseEmitter.event().name("connected").data(Map.of("status", "ok")));
        } catch (IOException e) {
            emitters.remove(emitter);
            emitter.completeWithError(e);
        }

        return emitter;
    }

    public void publishPackageIngested(String trackingNumber) {
        Iterator<SseEmitter> iterator = emitters.iterator();
        while (iterator.hasNext()) {
            SseEmitter emitter = iterator.next();
            try {
                emitter.send(SseEmitter.event()
                        .name("package-ingested")
                        .data(Map.of("trackingNumber", trackingNumber)));
            } catch (Exception e) {
                logger.debug("Removing failed SSE emitter after package-ingested publish", e);
                emitters.remove(emitter);
                emitter.complete();
            }
        }
    }

    public void publishSessionStopped() {
        Iterator<SseEmitter> iterator = emitters.iterator();
        while (iterator.hasNext()) {
            SseEmitter emitter = iterator.next();
            try {
                emitter.send(SseEmitter.event()
                        .name("session-stopped")
                        .data(Map.of("active", false)));
            } catch (Exception e) {
                logger.debug("Removing failed SSE emitter after session-stopped publish", e);
                emitters.remove(emitter);
                emitter.complete();
            }
        }
    }
}
