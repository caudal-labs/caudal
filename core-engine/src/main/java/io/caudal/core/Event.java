package io.caudal.core;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

public record Event(
    String src,
    String dst,
    double intensity,
    String type,
    Instant timestamp,
    Map<String, String> attrs
) {

    public Event {
        Objects.requireNonNull(src, "src must not be null");
        Objects.requireNonNull(dst, "dst must not be null");
        if (intensity <= 0) {
            throw new IllegalArgumentException("intensity must be positive");
        }
        if (attrs == null) {
            attrs = Map.of();
        }
    }

    public Event(String src, String dst, double intensity) {
        this(src, dst, intensity, null, null, Map.of());
    }
}
