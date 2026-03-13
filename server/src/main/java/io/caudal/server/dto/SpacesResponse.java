package io.caudal.server.dto;

import java.util.List;

public record SpacesResponse(List<SpaceItem> spaces, String asOf) {
}
