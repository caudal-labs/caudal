package io.caudal.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class MemoryEngineTest {

    private MemoryEngine engine;

    @BeforeEach
    void setUp() {
        engine = new MemoryEngine();
    }

    // --- Lazy decay math ---

    @Nested
    class DecayTests {

        @Test
        void noDecayWhenSameBucket() {
            EdgeState edge = new EdgeState("a", "b", 1.0, 10);
            MemoryEngine.applyDecay(edge, 10, 0.05);
            assertThat(edge.score()).isEqualTo(1.0);
        }

        @Test
        void singleBucketDecay() {
            EdgeState edge = new EdgeState("a", "b", 1.0, 10);
            MemoryEngine.applyDecay(edge, 11, 0.05);
            assertThat(edge.score()).isCloseTo(0.95, within(1e-10));
        }

        @Test
        void multiBucketDecay_powerLaw() {
            EdgeState edge = new EdgeState("a", "b", 1.0, 10);
            MemoryEngine.applyDecay(edge, 20, 0.05);
            // (1 - 0.05)^10 = 0.95^10
            assertThat(edge.score()).isCloseTo(Math.pow(0.95, 10), within(1e-10));
        }

        @Test
        void decayTowardsZero_highDecay() {
            EdgeState edge = new EdgeState("a", "b", 1.0, 0);
            MemoryEngine.applyDecay(edge, 100, 0.1);
            assertThat(edge.score()).isCloseTo(Math.pow(0.9, 100), within(1e-15));
            assertThat(edge.score()).isLessThan(1e-4);
        }
    }

    // --- Reinforcement ---

    @Nested
    class ReinforcementTests {

        @Test
        void reinforcement_decaysThenAdds() {
            SpaceConfig config = new SpaceConfig(1000, 10000, 1e-9, 0.1, 1.0, 1.0);
            SpaceState space = new SpaceState("test", config);

            // First event at bucket 0
            engine.applyEvents(space, List.of(new Event("a", "b", 1.0)), 0);
            assertThat(space.getEdge(new EdgeKey("a", "b")).score()).isEqualTo(1.0);

            // Second event at bucket 5 — should decay first, then add
            engine.applyEvents(space, List.of(new Event("a", "b", 1.0)), 5);
            double expected = 1.0 * Math.pow(0.9, 5) + 1.0;
            assertThat(space.getEdge(new EdgeKey("a", "b")).score())
                    .isCloseTo(expected, within(1e-10));
        }

        @Test
        void reinforcement_newEdge() {
            SpaceState space = new SpaceState("test", SpaceConfig.DEFAULT);
            engine.applyEvents(space, List.of(new Event("x", "y", 2.5)), 0);

            EdgeState edge = space.getEdge(new EdgeKey("x", "y"));
            assertThat(edge).isNotNull();
            assertThat(edge.score()).isEqualTo(2.5);
            assertThat(edge.rawCount()).isEqualTo(1);
        }

        @Test
        void reinforcement_intensityScaling() {
            SpaceConfig config = new SpaceConfig(1000, 10000, 1e-9, 0.05, 2.0, 1.0);
            SpaceState space = new SpaceState("test", config);

            engine.applyEvents(space, List.of(new Event("a", "b", 3.0)), 0);
            // 3.0 * depositScale(2.0) = 6.0
            assertThat(space.getEdge(new EdgeKey("a", "b")).score()).isEqualTo(6.0);
        }
    }

    // --- Ranking ---

    @Nested
    class RankingTests {

        @Test
        void focus_ranksDescending() {
            SpaceState space = new SpaceState("test", SpaceConfig.DEFAULT);
            engine.applyEvents(space, List.of(
                    new Event("a", "b", 3.0),
                    new Event("c", "d", 1.0),
                    new Event("a", "e", 5.0)
            ), 0);

            List<FocusItem> items = engine.focus(space, 10, 0);
            assertThat(items).hasSizeGreaterThanOrEqualTo(3);
            // scores should be in descending order
            for (int i = 1; i < items.size(); i++) {
                assertThat(items.get(i - 1).score()).isGreaterThanOrEqualTo(items.get(i).score());
            }
        }

        @Test
        void focus_respectsK() {
            SpaceState space = new SpaceState("test", SpaceConfig.DEFAULT);
            engine.applyEvents(space, List.of(
                    new Event("a", "b", 1.0),
                    new Event("c", "d", 2.0),
                    new Event("e", "f", 3.0)
            ), 0);

            assertThat(engine.focus(space, 2, 0)).hasSize(2);
        }

        @Test
        void focus_stableTieBreak() {
            SpaceState space = new SpaceState("test", SpaceConfig.DEFAULT);
            engine.applyEvents(space, List.of(
                    new Event("alpha", "beta", 1.0),
                    new Event("gamma", "delta", 1.0)
            ), 0);

            List<FocusItem> run1 = engine.focus(space, 10, 0);
            List<FocusItem> run2 = engine.focus(space, 10, 0);
            assertThat(run1).isEqualTo(run2);
        }

        @Test
        void next_ranksDescending() {
            SpaceState space = new SpaceState("test", SpaceConfig.DEFAULT);
            engine.applyEvents(space, List.of(
                    new Event("a", "b", 3.0),
                    new Event("a", "c", 1.0),
                    new Event("a", "d", 5.0)
            ), 0);

            List<NextHopItem> items = engine.next(space, "a", 10, 0);
            assertThat(items).hasSize(3);
            assertThat(items.get(0).id()).isEqualTo("d");
            assertThat(items.get(1).id()).isEqualTo("b");
            assertThat(items.get(2).id()).isEqualTo("c");
        }

        @Test
        void next_noOutgoing_returnsEmpty() {
            SpaceState space = new SpaceState("test", SpaceConfig.DEFAULT);
            assertThat(engine.next(space, "nonexistent", 10, 0)).isEmpty();
        }
    }

    // --- Pruning ---

    @Nested
    class PruningTests {

        @Test
        void pruning_removesEdgesBelowThreshold() {
            SpaceConfig config = new SpaceConfig(1000, 10000, 0.5, 0.5, 1.0, 1.0);
            SpaceState space = new SpaceState("test", config);

            engine.applyEvents(space, List.of(new Event("a", "b", 1.0)), 0);
            assertThat(space.edgeCount()).isEqualTo(1);

            // After decay of 2 buckets: 1.0 * 0.5^2 = 0.25 < threshold 0.5
            // Sending a tiny event to trigger pruning
            engine.applyEvents(space, List.of(new Event("c", "d", 0.01)), 2);
            // a->b should have been pruned (decayed score = 0.25, below 0.5 threshold)
            assertThat(space.getEdge(new EdgeKey("a", "b"))).isNull();
        }

        @Test
        void pruning_respectsCapEviction() {
            SpaceConfig config = new SpaceConfig(1000, 3, 1e-9, 0.0, 1.0, 1.0);
            SpaceState space = new SpaceState("test", config);

            engine.applyEvents(space, List.of(
                    new Event("a", "b", 1.0),
                    new Event("c", "d", 2.0),
                    new Event("e", "f", 3.0)
            ), 0);
            assertThat(space.edgeCount()).isEqualTo(3);

            // Adding one more should trigger cap eviction
            engine.applyEvents(space, List.of(new Event("g", "h", 4.0)), 0);
            assertThat(space.edgeCount()).isLessThanOrEqualTo(3);
            // Lowest score edge (a->b) should have been evicted
            assertThat(space.getEdge(new EdgeKey("a", "b"))).isNull();
        }
    }

    // --- Pathways ---

    @Nested
    class PathwayTests {

        @Test
        void pathways_deterministic_withSeed() {
            SpaceState space = new SpaceState("test", SpaceConfig.DEFAULT);
            engine.applyEvents(space, List.of(
                    new Event("a", "b", 2.0),
                    new Event("b", "c", 3.0),
                    new Event("a", "d", 1.0),
                    new Event("d", "e", 1.0)
            ), 0);

            PathwayResult r1 = engine.pathways(space, "a", 100, 5, 10, 42L, 0);
            PathwayResult r2 = engine.pathways(space, "a", 100, 5, 10, 42L, 0);

            assertThat(r1.paths()).isEqualTo(r2.paths());
            assertThat(r1.topEntities()).isEqualTo(r2.topEntities());
        }

        @Test
        void pathways_noOutgoing_returnsSingleNodePaths() {
            SpaceState space = new SpaceState("test", SpaceConfig.DEFAULT);
            PathwayResult result = engine.pathways(space, "isolated", 10, 5, 5, 42L, 0);
            assertThat(result.paths()).isNotEmpty();
            assertThat(result.paths().getFirst().nodes()).containsExactly("isolated");
        }

        @Test
        void pathways_prefersHigherScoreEdges() {
            SpaceState space = new SpaceState("test", SpaceConfig.DEFAULT);
            engine.applyEvents(space, List.of(
                    new Event("a", "high", 100.0),
                    new Event("a", "low", 0.001)
            ), 0);

            PathwayResult result = engine.pathways(space, "a", 1000, 1, 10, 42L, 0);
            // Most ants should go to "high"
            FocusItem topEntity = result.topEntities().getFirst();
            assertThat(topEntity.id()).isEqualTo("high");
            assertThat(topEntity.score()).isGreaterThan(0.9);
        }
    }

    // --- Late events policy ---

    @Nested
    class LateEventTests {

        @Test
        void lateEvent_appliedAsOfCurrentBucket() {
            SpaceState space = new SpaceState("test", SpaceConfig.DEFAULT);

            // Event arrives at bucket 10 regardless of its original timestamp
            engine.applyEvents(space, List.of(new Event("a", "b", 1.0)), 10);
            EdgeState edge = space.getEdge(new EdgeKey("a", "b"));
            assertThat(edge.lastUpdatedBucket()).isEqualTo(10);
        }
    }

    // --- Snapshot / Restore ---

    @Nested
    class SnapshotTests {

        @Test
        void snapshotAndRestore_preservesState() {
            SpaceState space = new SpaceState("test", SpaceConfig.DEFAULT);
            engine.applyEvents(space, List.of(
                    new Event("a", "b", 2.0),
                    new Event("c", "d", 3.0)
            ), 5);

            SpaceSnapshot snap = engine.snapshot(space, 5);
            SpaceState restored = engine.restore(snap, SpaceConfig.DEFAULT);

            assertThat(restored.edgeCount()).isEqualTo(space.edgeCount());
            assertThat(restored.spaceId()).isEqualTo("test");

            List<FocusItem> originalFocus = engine.focus(space, 10, 5);
            List<FocusItem> restoredFocus = engine.focus(restored, 10, 5);
            assertThat(restoredFocus).isEqualTo(originalFocus);
        }
    }
}
