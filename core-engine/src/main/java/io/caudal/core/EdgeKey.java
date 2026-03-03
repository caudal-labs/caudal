package io.caudal.core;

import java.util.Objects;

public record EdgeKey(String src, String dst) {

    public EdgeKey {
        Objects.requireNonNull(src, "src must not be null");
        Objects.requireNonNull(dst, "dst must not be null");
    }
}
