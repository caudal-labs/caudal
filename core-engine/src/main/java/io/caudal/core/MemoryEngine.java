package io.caudal.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class MemoryEngine {

    public void applyEvents(SpaceState space, List<Event> events, long currentBucket) {
        SpaceConfig config = space.config();

        for (Event event : events) {
            EdgeKey key = new EdgeKey(event.src(), event.dst());
            EdgeState edge = space.getOrCreateEdge(key, currentBucket);

            applyDecay(edge, currentBucket, config.decayPerBucket());

            edge.setScore(edge.score() + event.weight() * config.depositScale());
            edge.setLastUpdatedBucket(currentBucket);
            edge.incrementRawCount();
        }

        prune(space, currentBucket);
    }

    public List<FocusItem> focus(SpaceState space, int k, long currentBucket) {
        Map<String, Double> nodeScores = new HashMap<>();

        for (var entry : space.edges().entrySet()) {
            EdgeState edge = entry.getValue();
            double decayed = decayedScore(edge, currentBucket, space.config().decayPerBucket());

            nodeScores.merge(edge.src(), decayed, Double::sum);
            nodeScores.merge(edge.dst(), decayed, Double::sum);
        }

        return nodeScores.entrySet().stream()
            .map(e -> new FocusItem(e.getKey(), e.getValue()))
            .sorted()
            .limit(k)
            .toList();
    }

    public List<NextHopItem> next(SpaceState space, String src, int k, long currentBucket) {
        List<EdgeKey> outgoing = space.getOutgoing(src);
        if (outgoing.isEmpty()) {
            return List.of();
        }

        return outgoing.stream()
            .map(key -> {
                EdgeState edge = space.getEdge(key);
                double decayed = decayedScore(edge, currentBucket, space.config().decayPerBucket());
                return new NextHopItem(key.dst(), decayed);
            })
            .sorted()
            .limit(k)
            .toList();
    }

    public PathwayResult pathways(SpaceState space, String start, int ants, int maxSteps,
        int k, Long seed, long currentBucket) {
        Random rng = seed != null ? new Random(seed) : new Random();
        double alpha = space.config().alpha();
        Map<List<String>, Integer> pathCounts = new HashMap<>();
        Map<String, Integer> entityCounts = new HashMap<>();

        for (int ant = 0; ant < ants; ant++) {
            List<String> path = new ArrayList<>();
            String current = start;
            path.add(current);

            for (int step = 0; step < maxSteps; step++) {
                List<EdgeKey> outgoing = space.getOutgoing(current);
                if (outgoing.isEmpty()) {
                    break;
                }

                List<Double> weights = new ArrayList<>(outgoing.size());
                double totalWeight = 0;
                for (EdgeKey key : outgoing) {
                    EdgeState edge = space.getEdge(key);
                    double w = Math.pow(
                        decayedScore(edge, currentBucket, space.config().decayPerBucket()),
                        alpha
                    );
                    weights.add(w);
                    totalWeight += w;
                }

                if (totalWeight <= 0) {
                    break;
                }

                double r = rng.nextDouble() * totalWeight;
                double cumulative = 0;
                int chosen = 0;
                for (int i = 0; i < weights.size(); i++) {
                    cumulative += weights.get(i);
                    if (r <= cumulative) {
                        chosen = i;
                        break;
                    }
                }

                current = outgoing.get(chosen).dst();
                path.add(current);
                entityCounts.merge(current, 1, Integer::sum);
            }

            pathCounts.merge(path, 1, Integer::sum);
        }

        List<PathwayResult.Path> topPaths = pathCounts.entrySet().stream()
            .map(e -> new PathwayResult.Path(e.getKey(), (double) e.getValue() / ants))
            .sorted()
            .limit(k)
            .toList();

        List<FocusItem> topEntities = entityCounts.entrySet().stream()
            .map(e -> new FocusItem(e.getKey(), (double) e.getValue() / ants))
            .sorted()
            .limit(k)
            .toList();

        return new PathwayResult(topPaths, topEntities);
    }

    // --- Snapshot support ---

    public SpaceSnapshot snapshot(SpaceState space, long currentBucket) {
        List<SpaceSnapshot.EdgeData> edgeData = space.edges().values().stream()
            .map(es -> {
                double decayed = decayedScore(es, currentBucket, space.config().decayPerBucket());
                return new SpaceSnapshot.EdgeData(
                    es.src(), es.dst(), decayed, currentBucket, es.rawCount()
                );
            })
            .toList();

        return new SpaceSnapshot(space.spaceId(), currentBucket, edgeData);
    }

    public SpaceState restore(SpaceSnapshot snap, SpaceConfig config) {
        SpaceState space = new SpaceState(snap.spaceId(), config);
        for (SpaceSnapshot.EdgeData ed : snap.edges()) {
            EdgeKey key = new EdgeKey(ed.src(), ed.dst());
            EdgeState edge = space.getOrCreateEdge(key, ed.lastUpdatedBucket());
            edge.setScore(ed.score());
            edge.setLastUpdatedBucket(ed.lastUpdatedBucket());
        }
        return space;
    }

    // --- Internal helpers ---

    static void applyDecay(EdgeState edge, long currentBucket, double decayPerBucket) {
        long delta = currentBucket - edge.lastUpdatedBucket();
        if (delta > 0) {
            edge.setScore(edge.score() * Math.pow(1.0 - decayPerBucket, delta));
        }
    }

    static double decayedScore(EdgeState edge, long currentBucket, double decayPerBucket) {
        long delta = currentBucket - edge.lastUpdatedBucket();
        if (delta <= 0) {
            return edge.score();
        }
        return edge.score() * Math.pow(1.0 - decayPerBucket, delta);
    }

    private void prune(SpaceState space, long currentBucket) {
        SpaceConfig config = space.config();
        double decayPerBucket = config.decayPerBucket();
        double minScore = config.minScoreToKeep();

        // Phase 1: drop edges below minimum score threshold
        List<EdgeKey> toRemove = new ArrayList<>();
        for (var entry : space.edges().entrySet()) {
            double decayed = decayedScore(entry.getValue(), currentBucket, decayPerBucket);
            if (decayed < minScore) {
                toRemove.add(entry.getKey());
            }
        }
        toRemove.forEach(space::removeEdge);

        // Phase 2: if still above cap, evict lowest-score edges
        if (space.edgeCount() > config.maxEdges()) {
            List<Map.Entry<EdgeKey, EdgeState>> sorted = space.edges().entrySet().stream()
                .sorted(Comparator.comparingDouble(
                    e -> decayedScore(e.getValue(), currentBucket, decayPerBucket)))
                .toList();

            int toEvict = space.edgeCount() - config.maxEdges();
            for (int i = 0; i < toEvict && i < sorted.size(); i++) {
                space.removeEdge(sorted.get(i).getKey());
            }
        }
    }
}
