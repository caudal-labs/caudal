package io.caudal.server.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record EventRequest(
    @NotBlank String space,
    @NotEmpty @Valid List<EventItem> events,
    @Valid List<ModulationItem> modulations
) {

    public record EventItem(
        @NotBlank String src,
        @NotBlank String dst,
        double intensity,
        String type,
        String timestamp,
        java.util.Map<String, String> attrs
    ) {

        public EventItem {
            if (intensity <= 0) {
                intensity = 1.0;
            }
        }
    }
}
