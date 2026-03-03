package io.caudal.server.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record EventRequest(
    @NotBlank String space,
    @NotEmpty @Valid List<EventItem> events
) {

    public record EventItem(
        @NotBlank String src,
        @NotBlank String dst,
        double weight,
        String type,
        String timestamp,
        java.util.Map<String, String> attrs
    ) {

        public EventItem {
            if (weight <= 0) {
                weight = 1.0;
            }
        }
    }
}
