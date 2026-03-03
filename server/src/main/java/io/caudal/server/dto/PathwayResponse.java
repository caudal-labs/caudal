package io.caudal.server.dto;

import java.util.List;

public record PathwayResponse(
        List<PathItem> paths,
        List<FocusResponse.ScoredItem> topEntities,
        String asOf
) {

    public record PathItem(List<String> nodes, double score) {}
}
