package io.caudal.server.service;

import io.caudal.core.*;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Service
public class SpaceManager {

    private final MemoryEngine engine;
    private final BucketClock clock;
    private final SpaceConfig defaultConfig;
    private final Map<String, SpaceState> spaces = new ConcurrentHashMap<>();
    private final Map<String, ReadWriteLock> locks = new ConcurrentHashMap<>();

    public SpaceManager(MemoryEngine engine, BucketClock clock, SpaceConfig defaultConfig) {
        this.engine = engine;
        this.clock = clock;
        this.defaultConfig = defaultConfig;
    }

    public long applyEvents(String spaceId, List<Event> events) {
        long bucket = clock.nowBucket();
        ReadWriteLock lock = lockFor(spaceId);
        lock.writeLock().lock();
        try {
            SpaceState space = spaces.computeIfAbsent(spaceId, id -> new SpaceState(id, defaultConfig));
            engine.applyEvents(space, events, bucket);
        } finally {
            lock.writeLock().unlock();
        }
        return bucket;
    }

    public long applyEventsAndModulations(String spaceId, List<Event> events, List<Modulation> modulations) {
        long bucket = clock.nowBucket();
        ReadWriteLock lock = lockFor(spaceId);
        lock.writeLock().lock();
        try {
            SpaceState space = spaces.computeIfAbsent(spaceId, id -> new SpaceState(id, defaultConfig));
            engine.applyEvents(space, events, bucket);
            if (modulations != null && !modulations.isEmpty()) {
                engine.applyModulations(space, modulations);
            }
        } finally {
            lock.writeLock().unlock();
        }
        return bucket;
    }

    public void applyModulations(String spaceId, List<Modulation> modulations) {
        ReadWriteLock lock = lockFor(spaceId);
        lock.writeLock().lock();
        try {
            SpaceState space = spaces.computeIfAbsent(spaceId, id -> new SpaceState(id, defaultConfig));
            engine.applyModulations(space, modulations);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public List<FocusItem> focus(String spaceId, int k) {
        long bucket = clock.nowBucket();
        ReadWriteLock lock = lockFor(spaceId);
        lock.readLock().lock();
        try {
            SpaceState space = spaces.get(spaceId);
            if (space == null) {
                return List.of();
            }
            return engine.focus(space, k, bucket);
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<NextHopItem> next(String spaceId, String src, int k) {
        long bucket = clock.nowBucket();
        ReadWriteLock lock = lockFor(spaceId);
        lock.readLock().lock();
        try {
            SpaceState space = spaces.get(spaceId);
            if (space == null) {
                return List.of();
            }
            return engine.next(space, src, k, bucket);
        } finally {
            lock.readLock().unlock();
        }
    }

    public PathwayResult pathways(String spaceId, String start, int ants, int maxSteps,
        int k, Long seed) {
        long bucket = clock.nowBucket();
        ReadWriteLock lock = lockFor(spaceId);
        lock.readLock().lock();
        try {
            SpaceState space = spaces.get(spaceId);
            if (space == null) {
                return new PathwayResult(List.of(), List.of());
            }
            return engine.pathways(space, start, ants, maxSteps, k, seed, bucket);
        } finally {
            lock.readLock().unlock();
        }
    }

    public SpaceSnapshot snapshot(String spaceId) {
        long bucket = clock.nowBucket();
        ReadWriteLock lock = lockFor(spaceId);
        lock.readLock().lock();
        try {
            SpaceState space = spaces.get(spaceId);
            if (space == null) {
                return null;
            }
            return engine.snapshot(space, bucket);
        } finally {
            lock.readLock().unlock();
        }
    }

    public void restoreSpace(SpaceSnapshot snap) {
        ReadWriteLock lock = lockFor(snap.spaceId());
        lock.writeLock().lock();
        try {
            spaces.put(snap.spaceId(), engine.restore(snap, defaultConfig));
        } finally {
            lock.writeLock().unlock();
        }
    }

    public List<String> spaceIds() {
        return List.copyOf(spaces.keySet());
    }

    public SpaceState getSpaceState(String spaceId) {
        ReadWriteLock lock = lockFor(spaceId);
        lock.readLock().lock();
        try {
            return spaces.get(spaceId);
        } finally {
            lock.readLock().unlock();
        }
    }

    public BucketClock clock() {
        return clock;
    }

    private ReadWriteLock lockFor(String spaceId) {
        return locks.computeIfAbsent(spaceId, id -> new ReentrantReadWriteLock());
    }
}
