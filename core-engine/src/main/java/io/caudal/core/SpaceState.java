package io.caudal.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class SpaceState {

    private final String spaceId;
    private final SpaceConfig config;
    private final Map<EdgeKey, EdgeState> edges;
    private final Map<String, List<EdgeKey>> outgoing;

    public SpaceState(String spaceId, SpaceConfig config) {
        this.spaceId = spaceId;
        this.config = config;
        this.edges = new HashMap<>();
        this.outgoing = new HashMap<>();
    }

    public String spaceId() {
        return spaceId;
    }

    public SpaceConfig config() {
        return config;
    }

    public Map<EdgeKey, EdgeState> edges() {
        return Collections.unmodifiableMap(edges);
    }

    public int edgeCount() {
        return edges.size();
    }

    public int nodeCount() {
        return outgoing.size();
    }

    public EdgeState getEdge(EdgeKey key) {
        return edges.get(key);
    }

    public EdgeState getOrCreateEdge(EdgeKey key, long currentBucket) {
        return edges.computeIfAbsent(key, k -> {
            outgoing.computeIfAbsent(k.src(), s -> new ArrayList<>()).add(k);
            return new EdgeState(k.src(), k.dst(), 0.0, currentBucket);
        });
    }

    public List<EdgeKey> getOutgoing(String src) {
        return outgoing.getOrDefault(src, List.of());
    }

    public void removeEdge(EdgeKey key) {
        EdgeState removed = edges.remove(key);
        if (removed != null) {
            List<EdgeKey> out = outgoing.get(key.src());
            if (out != null) {
                out.remove(key);
                if (out.isEmpty()) {
                    outgoing.remove(key.src());
                }
            }
        }
    }

    public Map<String, List<EdgeKey>> outgoing() {
        return Collections.unmodifiableMap(outgoing);
    }
}
