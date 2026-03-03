package io.caudal.core;

public record SpaceConfig(
    int maxNodes,
    int maxEdges,
    double minScoreToKeep,
    double decayPerBucket,
    double depositScale,
    double alpha
) {

    public static final SpaceConfig DEFAULT = new SpaceConfig(
        10_000,     // maxNodes
        100_000,    // maxEdges
        1e-6,       // minScoreToKeep
        0.0253,     // decayPerBucket (2.53% decay per 30s bucket)
        1.0,        // depositScale
        1.0         // alpha (pathways exponent)
    );

    public SpaceConfig {
        if (maxNodes <= 0) {
            throw new IllegalArgumentException("maxNodes must be positive");
        }
        if (maxEdges <= 0) {
            throw new IllegalArgumentException("maxEdges must be positive");
        }
        if (decayPerBucket < 0 || decayPerBucket > 1) {
            throw new IllegalArgumentException("decayPerBucket must be in [0,1]");
        }
        if (depositScale <= 0) {
            throw new IllegalArgumentException("depositScale must be positive");
        }
        if (alpha <= 0) {
            throw new IllegalArgumentException("alpha must be positive");
        }
    }
}
