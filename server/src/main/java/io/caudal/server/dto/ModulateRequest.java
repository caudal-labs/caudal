package io.caudal.server.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record ModulateRequest(
    @NotBlank String space,
    @NotEmpty @Valid List<ModulationItem> modulations
) {}
