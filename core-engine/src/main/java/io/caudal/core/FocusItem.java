package io.caudal.core;

public record FocusItem(String id, double score) implements Comparable<FocusItem> {

    @Override
    public int compareTo(FocusItem other) {
        int cmp = Double.compare(other.score, this.score); // descending
        return cmp != 0 ? cmp : this.id.compareTo(other.id); // stable tie-break
    }
}
