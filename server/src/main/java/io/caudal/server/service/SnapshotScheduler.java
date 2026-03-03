package io.caudal.server.service;

import io.caudal.core.SpaceSnapshot;
import io.caudal.server.CaudalProperties;
import io.caudal.server.persistence.PersistenceService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SnapshotScheduler {

    private static final Logger log = LoggerFactory.getLogger(SnapshotScheduler.class);

    private final SpaceManager spaceManager;
    private final PersistenceService persistence;
    private final CaudalProperties props;
    private final Timer snapshotTimer;

    public SnapshotScheduler(SpaceManager spaceManager, PersistenceService persistence,
                             CaudalProperties props, MeterRegistry registry) {
        this.spaceManager = spaceManager;
        this.persistence = persistence;
        this.props = props;
        this.snapshotTimer = registry.timer("caudal.snapshot.duration");
    }

    @Scheduled(fixedDelayString = "${caudal.snapshot-interval-seconds:300}000")
    public void snapshotAll() {
        snapshotTimer.record(() -> {
            for (String spaceId : spaceManager.spaceIds()) {
                try {
                    SpaceSnapshot snap = spaceManager.snapshot(spaceId);
                    if (snap != null) {
                        persistence.saveSnapshot(spaceId, snap);
                        persistence.pruneOldSnapshots(spaceId, props.snapshotsToKeep());

                        Instant walCutoff = Instant.now().minus(Duration.ofDays(props.walRetentionDays()));
                        persistence.pruneOldWal(spaceId, walCutoff);
                    }
                } catch (Exception e) {
                    log.error("Failed to snapshot space={}", spaceId, e);
                }
            }
        });
    }
}
