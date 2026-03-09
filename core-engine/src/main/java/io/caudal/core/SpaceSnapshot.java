package io.caudal.core;

import java.util.List;

public record SpaceSnapshot(
        String spaceId,
        long bucket,
        List<EdgeData> edges,
        List<ModulationData> modulations,
        long eventCounter
) {

    public SpaceSnapshot(String spaceId, long bucket, List<EdgeData> edges) {
        this(spaceId, bucket, edges, List.of(), 0);
    }

    public record EdgeData(
            String src,
            String dst,
            double score,
            long lastUpdatedBucket,
            long rawCount
    ) {}

    public record ModulationData(
            String entity,
            double attention,
            long decay,
            long appliedAtEventCount
    ) {}
}
