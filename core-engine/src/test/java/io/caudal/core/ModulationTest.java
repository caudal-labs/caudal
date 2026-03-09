package io.caudal.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Test;

class ModulationTest {

    @Test
    void persistent_modulation_neverDecays() {
        Modulation mod = new Modulation("x", 0.1, 0, 0);
        assertThat(mod.effectiveAttention(0)).isEqualTo(0.1);
        assertThat(mod.effectiveAttention(1000)).isEqualTo(0.1);
        assertThat(mod.effectiveAttention(1_000_000)).isEqualTo(0.1);
    }

    @Test
    void decaying_modulation_halflife() {
        Modulation mod = new Modulation("x", 0.0, 100, 0);
        // At 0 events: full suppression
        assertThat(mod.effectiveAttention(0)).isCloseTo(0.0, within(1e-10));
        // At 100 events: 1.0 + (0.0 - 1.0) * 0.5^(100/100) = 0.5
        assertThat(mod.effectiveAttention(100)).isCloseTo(0.5, within(1e-10));
        // At 200 events: 1.0 + (0.0 - 1.0) * 0.5^(200/100) = 0.75
        assertThat(mod.effectiveAttention(200)).isCloseTo(0.75, within(1e-10));
    }

    @Test
    void amplifying_modulation_decays_toward_neutral() {
        Modulation mod = new Modulation("x", 5.0, 50, 0);
        // At 0 events: should be 5.0
        assertThat(mod.effectiveAttention(0)).isCloseTo(5.0, within(1e-10));
        // At 50 events: 1.0 + (5.0 - 1.0) * 0.5^(50/50) = 1.0 + 2.0 = 3.0
        assertThat(mod.effectiveAttention(50)).isCloseTo(3.0, within(1e-10));
    }

    @Test
    void isNeutral_detectsDecayedModulation() {
        Modulation mod = new Modulation("x", 0.0, 10, 0);
        assertThat(mod.isNeutral(0, 0.001)).isFalse();
        assertThat(mod.isNeutral(1000, 0.001)).isTrue();
    }

    @Test
    void negativeAttention_rejected() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Modulation("x", -1.0, 0, 0));
    }

    @Test
    void nullEntity_rejected() {
        org.assertj.core.api.Assertions.assertThatNullPointerException()
                .isThrownBy(() -> new Modulation(null, 1.0, 0, 0));
    }
}
