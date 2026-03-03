package io.caudal.server.dto;

import jakarta.validation.constraints.NotBlank;

public record PathwayRequest(@NotBlank String space, @NotBlank String start, int k, String mode) {

    public PathwayRequest {
        if (k <= 0) {
            k = 10;
        }
        if (mode == null || mode.isBlank()) {
            mode = "balanced";
        }
    }
}
