package io.caudal.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class BucketClockTest {

    @Test
    void toBucket_mapsCorrectly() {
        BucketClock clock = new BucketClock(60);
        assertThat(clock.toBucket(Instant.ofEpochSecond(0))).isEqualTo(0);
        assertThat(clock.toBucket(Instant.ofEpochSecond(59))).isEqualTo(0);
        assertThat(clock.toBucket(Instant.ofEpochSecond(60))).isEqualTo(1);
        assertThat(clock.toBucket(Instant.ofEpochSecond(119))).isEqualTo(1);
        assertThat(clock.toBucket(Instant.ofEpochSecond(120))).isEqualTo(2);
    }

    @Test
    void toBucket_boundaries_with300SecondBuckets() {
        BucketClock clock = new BucketClock(300);
        assertThat(clock.toBucket(Instant.ofEpochSecond(299))).isEqualTo(0);
        assertThat(clock.toBucket(Instant.ofEpochSecond(300))).isEqualTo(1);
        assertThat(clock.toBucket(Instant.ofEpochSecond(600))).isEqualTo(2);
    }

    @Test
    void toInstant_roundTrips() {
        BucketClock clock = new BucketClock(60);
        long bucket = 42;
        assertThat(clock.toInstant(bucket)).isEqualTo(Instant.ofEpochSecond(42 * 60));
        assertThat(clock.toBucket(clock.toInstant(bucket))).isEqualTo(bucket);
    }

    @Test
    void constructor_rejectsNonPositive() {
        assertThatIllegalArgumentException().isThrownBy(() -> new BucketClock(0));
        assertThatIllegalArgumentException().isThrownBy(() -> new BucketClock(-1));
    }
}
