package io.caudal.server.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.caudal.core.SpaceSnapshot;
import io.caudal.server.dto.EventRequest;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class PersistenceService {

    private static final Logger log = LoggerFactory.getLogger(PersistenceService.class);

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;
    private final Counter walCounter;
    private final Counter snapshotBytesCounter;

    public PersistenceService(JdbcTemplate jdbc, ObjectMapper objectMapper, MeterRegistry registry) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
        this.walCounter = registry.counter("caudal.wal.appended");
        this.snapshotBytesCounter = registry.counter("caudal.snapshot.bytes");
    }

    public void appendWal(String spaceId, List<EventRequest.EventItem> events) {
        String sql = "INSERT INTO event_log (space_id, event_timestamp, src, dst, intensity, type, attrs) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?)";

        List<Object[]> batchArgs = events.stream()
            .map(e -> new Object[]{
                spaceId,
                e.timestamp(),
                e.src(),
                e.dst(),
                e.intensity(),
                e.type(),
                e.attrs() != null ? toJson(e.attrs()) : null
            })
            .toList();

        jdbc.batchUpdate(sql, batchArgs);
        walCounter.increment(events.size());
    }

    public void saveSnapshot(String spaceId, SpaceSnapshot snapshot) {
        try {
            byte[] compressed = compress(objectMapper.writeValueAsBytes(snapshot));
            jdbc.update(
                "INSERT INTO snapshots (space_id, bucket, payload, codec, version) VALUES (?, ?, ?, 'json+gzip', 1)",
                spaceId, snapshot.bucket(), compressed
            );
            snapshotBytesCounter.increment(compressed.length);
            log.info("Saved snapshot for space={} bucket={} size={}B", spaceId, snapshot.bucket(), compressed.length);
        } catch (IOException e) {
            throw new RuntimeException("Failed to serialize snapshot", e);
        }
    }

    public SpaceSnapshot loadLatestSnapshot(String spaceId) {
        List<byte[]> results = jdbc.query(
            "SELECT payload FROM snapshots WHERE space_id = ? ORDER BY created_at DESC LIMIT 1",
            (rs, rowNum) -> rs.getBytes("payload"),
            spaceId
        );
        if (results.isEmpty()) {
            return null;
        }

        try {
            byte[] decompressed = decompress(results.getFirst());
            return objectMapper.readValue(decompressed, SpaceSnapshot.class);
        } catch (IOException e) {
            throw new RuntimeException("Failed to deserialize snapshot", e);
        }
    }

    public long latestSnapshotBucket(String spaceId) {
        List<Long> results = jdbc.query(
            "SELECT bucket FROM snapshots WHERE space_id = ? ORDER BY created_at DESC LIMIT 1",
            (rs, rowNum) -> rs.getLong("bucket"),
            spaceId
        );
        return results.isEmpty() ? -1 : results.getFirst();
    }

    public List<EventRequest.EventItem> replayWalAfter(String spaceId, Instant after) {
        return jdbc.query(
            "SELECT src, dst, intensity, type, event_timestamp, attrs FROM event_log " +
                "WHERE space_id = ? AND arrived_at > ? ORDER BY arrived_at",
            (rs, rowNum) -> new EventRequest.EventItem(
                rs.getString("src"),
                rs.getString("dst"),
                rs.getDouble("intensity"),
                rs.getString("type"),
                rs.getString("event_timestamp"),
                null
            ),
            spaceId, Timestamp.from(after)
        );
    }

    public void pruneOldSnapshots(String spaceId, int keep) {
        jdbc.update(
            "DELETE FROM snapshots WHERE space_id = ? AND id NOT IN " +
                "(SELECT id FROM snapshots WHERE space_id = ? ORDER BY created_at DESC LIMIT ?)",
            spaceId, spaceId, keep
        );
    }

    public void pruneOldWal(String spaceId, Instant before) {
        jdbc.update("DELETE FROM event_log WHERE space_id = ? AND arrived_at < ?",
            spaceId, Timestamp.from(before));
    }

    public void deleteSpaceData(String spaceId) {
        jdbc.update("DELETE FROM event_log WHERE space_id = ?", spaceId);
        jdbc.update("DELETE FROM snapshots WHERE space_id = ?", spaceId);
        log.info("Deleted all persistence data for space={}", spaceId);
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (IOException e) {
            return "{}";
        }
    }

    private byte[] compress(byte[] data) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(baos)) {
            gzip.write(data);
        }
        return baos.toByteArray();
    }

    private byte[] decompress(byte[] data) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        try (GZIPInputStream gzip = new GZIPInputStream(bais)) {
            return gzip.readAllBytes();
        }
    }
}
