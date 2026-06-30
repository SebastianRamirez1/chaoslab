package com.chaoslab.domain.resilience;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ResiliencePolicyTest {

    @Test
    void noneIsPassThrough() {
        ResiliencePolicy none = ResiliencePolicy.none();

        assertThat(none.hasTimeout()).isFalse();
        assertThat(none.hasBreaker()).isFalse();
        assertThat(none.maxAttempts()).isEqualTo(1);
    }

    @Test
    void exposesConfiguredPoliciesAndReusesBreakerPerTarget() {
        ResiliencePolicy policy = new ResiliencePolicy(200, 3, 5, 3_000L);

        assertThat(policy.hasTimeout()).isTrue();
        assertThat(policy.timeoutMillis()).isEqualTo(200);
        assertThat(policy.maxAttempts()).isEqualTo(3);
        assertThat(policy.hasBreaker()).isTrue();
        assertThat(policy.breakerFor("api-1")).isSameAs(policy.breakerFor("api-1"));
        assertThat(policy.breakerFor("api-1")).isNotSameAs(policy.breakerFor("api-2"));
    }

    @Test
    void rejectsInvalidConfiguration() {
        assertThatThrownBy(() -> new ResiliencePolicy(0, 0, 0, 0L)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ResiliencePolicy(-1, 1, 0, 0L)).isInstanceOf(IllegalArgumentException.class);
    }
}
