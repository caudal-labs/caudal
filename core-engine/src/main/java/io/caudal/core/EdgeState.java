package io.caudal.core;

public final class EdgeState {

    private final String src;
    private final String dst;
    private double score;
    private long lastUpdatedBucket;
    private long rawCount;

    public EdgeState(String src, String dst, double score, long lastUpdatedBucket) {
        this.src = src;
        this.dst = dst;
        this.score = score;
        this.lastUpdatedBucket = lastUpdatedBucket;
        this.rawCount = 0;
    }

    public String src() {
        return src;
    }

    public String dst() {
        return dst;
    }

    public double score() {
        return score;
    }

    public long lastUpdatedBucket() {
        return lastUpdatedBucket;
    }

    public long rawCount() {
        return rawCount;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public void setLastUpdatedBucket(long bucket) {
        this.lastUpdatedBucket = bucket;
    }

    public void incrementRawCount() {
        this.rawCount++;
    }
}
