package io.caudal.core;

import java.util.List;

public record SpaceSnapshot(
        String spaceId,
        long bucket,
        List<EdgeData> edges
) {

    public record EdgeData(
            String src,
            String dst,
            double score,
            long lastUpdatedBucket,
            long rawCount
    ) {}
}
