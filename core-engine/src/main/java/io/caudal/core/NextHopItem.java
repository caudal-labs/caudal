package io.caudal.core;

public record NextHopItem(String id, double score) implements Comparable<NextHopItem> {

    @Override
    public int compareTo(NextHopItem other) {
        int cmp = Double.compare(other.score, this.score); // descending
        return cmp != 0 ? cmp : this.id.compareTo(other.id); // stable tie-break
    }
}
