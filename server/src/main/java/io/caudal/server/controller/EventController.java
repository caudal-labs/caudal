package io.caudal.server.controller;

import io.caudal.core.Event;
import io.caudal.core.Modulation;
import io.caudal.server.api.EventsApi;
import io.caudal.server.dto.EventRequest;
import io.caudal.server.dto.EventResponse;
import io.caudal.server.dto.ModulateRequest;
import io.caudal.server.dto.ModulateResponse;
import io.caudal.server.persistence.PersistenceService;
import io.caudal.server.service.SpaceManager;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class EventController implements EventsApi {

    private final SpaceManager spaceManager;
    private final PersistenceService persistence;
    private final Timer ingestTimer;
    private final Counter ingestCounter;

    public EventController(SpaceManager spaceManager, PersistenceService persistence,
                           MeterRegistry registry) {
        this.spaceManager = spaceManager;
        this.persistence = persistence;
        this.ingestTimer = registry.timer("caudal.events.ingest");
        this.ingestCounter = registry.counter("caudal.events.ingested");
    }

    @Override
    public ResponseEntity<EventResponse> ingestEvents(EventRequest request) {
        return ingestTimer.record(() -> {
            List<Event> coreEvents = request.events().stream()
                    .map(item -> new Event(
                            item.src(),
                            item.dst(),
                            item.intensity(),
                            item.type(),
                            item.timestamp() != null ? Instant.parse(item.timestamp()) : null,
                            item.attrs() != null ? item.attrs() : Map.of()
                    ))
                    .toList();

            List<Modulation> coreModulations = null;
            if (request.modulations() != null && !request.modulations().isEmpty()) {
                coreModulations = request.modulations().stream()
                        .map(m -> new Modulation(m.entity(), m.attention(), m.decay(), 0))
                        .toList();
            }

            long bucket = spaceManager.applyEventsAndModulations(
                    request.space(), coreEvents, coreModulations);

            persistence.appendWal(request.space(), request.events());

            ingestCounter.increment(coreEvents.size());

            String asOf = spaceManager.clock().toInstant(bucket).toString();
            return ResponseEntity.accepted().body(new EventResponse(coreEvents.size(), asOf));
        });
    }

    @Override
    public ResponseEntity<ModulateResponse> modulate(ModulateRequest request) {
        List<Modulation> coreModulations = request.modulations().stream()
                .map(m -> new Modulation(m.entity(), m.attention(), m.decay(), 0))
                .toList();

        spaceManager.applyModulations(request.space(), coreModulations);

        String asOf = spaceManager.clock().toInstant(spaceManager.clock().nowBucket()).toString();
        return ResponseEntity.ok(new ModulateResponse(coreModulations.size(), asOf));
    }
}
