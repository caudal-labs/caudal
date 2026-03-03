package io.caudal.server.dto;

import java.util.List;

public record FocusResponse(List<ScoredItem> items, String asOf) {

    public record ScoredItem(String id, double score) {}
}
