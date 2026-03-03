package io.caudal.core;

import java.time.Instant;

public final class BucketClock {

    private final long bucketSizeSeconds;

    public BucketClock(long bucketSizeSeconds) {
        if (bucketSizeSeconds <= 0) {
            throw new IllegalArgumentException("bucketSizeSeconds must be positive");
        }
        this.bucketSizeSeconds = bucketSizeSeconds;
    }

    public long toBucket(Instant instant) {
        return instant.getEpochSecond() / bucketSizeSeconds;
    }

    public long nowBucket() {
        return toBucket(Instant.now());
    }

    public Instant toInstant(long bucket) {
        return Instant.ofEpochSecond(bucket * bucketSizeSeconds);
    }

    public long bucketSizeSeconds() {
        return bucketSizeSeconds;
    }
}
