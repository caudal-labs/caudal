package io.caudal.server.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public record ModulationItem(
    @NotBlank String entity,
    @PositiveOrZero double attention,
    long decay
) {

    public ModulationItem {
        if (decay < 0) {
            decay = 0;
        }
    }
}
