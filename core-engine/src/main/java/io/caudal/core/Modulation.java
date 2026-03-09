package io.caudal.core;

import java.util.Objects;

public record Modulation(
    String entity,
    double attention,
    long decay,
    long appliedAtEventCount
) {

    public Modulation {
        Objects.requireNonNull(entity, "entity must not be null");
        if (attention < 0) {
            throw new IllegalArgumentException("attention must be non-negative");
        }
    }

    public double effectiveAttention(long currentEventCount) {
        if (decay <= 0) {
            return attention;
        }
        long delta = currentEventCount - appliedAtEventCount;
        if (delta <= 0) {
            return attention;
        }
        return 1.0 + (attention - 1.0) * Math.pow(0.5, (double) delta / decay);
    }

    public boolean isNeutral(long currentEventCount, double epsilon) {
        return Math.abs(effectiveAttention(currentEventCount) - 1.0) < epsilon;
    }
}
