package io.caudal.server.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record EventRequest(
        @NotBlank String space,
        @NotEmpty @Valid List<EventItem> events,
        @Valid List<ModulationItem> modulations
) {

    public record EventItem(
            @NotBlank String src,
            @NotBlank String dst,
            @Positive(message = "intensity must be > 0") Double intensity,
            String type,
            String timestamp,
            java.util.Map<String, String> attrs
    ) {

        public EventItem {
            if (intensity == null) {
                intensity = 1.0;
            }
        }
    }
}
