package com.ficn.panel.parser;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class KubernetesQuantityTest {

    @Test
    void parsesMemoryQuantities() {
        assertThat(KubernetesQuantity.toBytes("128Mi")).isEqualTo(128L * 1024 * 1024);
        assertThat(KubernetesQuantity.toBytes("1Gi")).isEqualTo(1024L * 1024 * 1024);
        assertThat(KubernetesQuantity.toBytes("1000")).isEqualTo(1000L);
    }

    @Test
    void parsesCpuQuantities() {
        assertThat(KubernetesQuantity.toCores("250m")).isCloseTo(0.25, within(1e-9));
        assertThat(KubernetesQuantity.toCores("2")).isCloseTo(2D, within(1e-9));
        assertThat(KubernetesQuantity.milliCpu(0.25)).isEqualTo("250m");
    }
}
