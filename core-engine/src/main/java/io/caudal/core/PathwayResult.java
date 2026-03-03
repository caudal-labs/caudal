package io.caudal.core;

import java.util.List;

public record PathwayResult(
        List<Path> paths,
        List<FocusItem> topEntities
) {

    public record Path(List<String> nodes, double score) implements Comparable<Path> {
        @Override
        public int compareTo(Path other) {
            return Double.compare(other.score, this.score); // descending
        }
    }
}
