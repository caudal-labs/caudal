package io.caudal.server.service;

import io.caudal.core.Event;
import io.caudal.core.SpaceSnapshot;
import io.caudal.server.dto.EventRequest;
import io.caudal.server.persistence.PersistenceService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class RecoveryService {

    private static final Logger log = LoggerFactory.getLogger(RecoveryService.class);

    private final PersistenceService persistence;
    private final SpaceManager spaceManager;
    private final JdbcTemplate jdbc;
    private final Timer replayTimer;

    public RecoveryService(PersistenceService persistence, SpaceManager spaceManager,
                           JdbcTemplate jdbc, MeterRegistry registry) {
        this.persistence = persistence;
        this.spaceManager = spaceManager;
        this.jdbc = jdbc;
        this.replayTimer = registry.timer("caudal.recovery.replay");
    }

    @EventListener(ApplicationReadyEvent.class)
    public void recover() {
        replayTimer.record(() -> {
            List<String> spaceIds = jdbc.queryForList(
                    "SELECT DISTINCT space_id FROM snapshots UNION SELECT DISTINCT space_id FROM event_log",
                    String.class
            );

            for (String spaceId : spaceIds) {
                try {
                    recoverSpace(spaceId);
                } catch (Exception e) {
                    log.error("Failed to recover space={}", spaceId, e);
                }
            }

            log.info("Recovery complete: {} space(s) restored", spaceIds.size());
        });
    }

    private void recoverSpace(String spaceId) {
        SpaceSnapshot snap = persistence.loadLatestSnapshot(spaceId);
        Instant replayAfter;

        if (snap != null) {
            spaceManager.restoreSpace(snap);
            replayAfter = spaceManager.clock().toInstant(snap.bucket());
            log.info("Loaded snapshot for space={} bucket={}", spaceId, snap.bucket());
        } else {
            replayAfter = Instant.EPOCH;
        }

        List<EventRequest.EventItem> walEvents = persistence.replayWalAfter(spaceId, replayAfter);
        if (!walEvents.isEmpty()) {
            List<Event> coreEvents = walEvents.stream()
                    .map(e -> new Event(
                            e.src(), e.dst(), e.intensity(), e.type(),
                            e.timestamp() != null ? Instant.parse(e.timestamp()) : null,
                            e.attrs() != null ? e.attrs() : Map.of()
                    ))
                    .toList();

            spaceManager.applyEvents(spaceId, coreEvents);
            log.info("Replayed {} WAL events for space={}", walEvents.size(), spaceId);
        }
    }
}
