package io.caudal.server;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "caudal")
public record CaudalProperties(
    long bucketSizeSeconds,
    int maxNodes,
    int maxEdges,
    double minScoreToKeep,
    double decayPerBucket,
    double depositScale,
    double alpha,
    long snapshotIntervalSeconds,
    int snapshotsToKeep,
    int walRetentionDays,
    Pathways pathways
) {

    public record Pathways(ModeConfig fast, ModeConfig balanced, ModeConfig deep) {

        public Pathways {
            if (fast == null) {
                fast = new ModeConfig(50, 4);
            }
            if (balanced == null) {
                balanced = new ModeConfig(200, 6);
            }
            if (deep == null) {
                deep = new ModeConfig(500, 10);
            }
        }

        public ModeConfig resolve(String mode) {
            return switch (mode) {
                case "fast" -> fast;
                case "deep" -> deep;
                default -> balanced;
            };
        }
    }

    public record ModeConfig(int ants, int maxSteps) {}

    public CaudalProperties {
        if (bucketSizeSeconds <= 0) {
            bucketSizeSeconds = 30;
        }
        if (maxNodes <= 0) {
            maxNodes = 10_000;
        }
        if (maxEdges <= 0) {
            maxEdges = 100_000;
        }
        if (minScoreToKeep <= 0) {
            minScoreToKeep = 1e-6;
        }
        if (decayPerBucket <= 0) {
            decayPerBucket = 0.0253;
        }
        if (depositScale <= 0) {
            depositScale = 1.0;
        }
        if (alpha <= 0) {
            alpha = 1.0;
        }
        if (snapshotIntervalSeconds <= 0) {
            snapshotIntervalSeconds = 300;
        }
        if (snapshotsToKeep <= 0) {
            snapshotsToKeep = 3;
        }
        if (walRetentionDays <= 0) {
            walRetentionDays = 7;
        }
        if (pathways == null) {
            pathways = new Pathways(null, null, null);
        }
    }
}
