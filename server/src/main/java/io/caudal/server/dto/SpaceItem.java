package io.caudal.server.dto;

public record SpaceItem(String space, int entityCount, int edgeCount, long eventCount) {
}
