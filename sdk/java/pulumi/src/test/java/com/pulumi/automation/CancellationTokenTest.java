// Copyright 2026, Pulumi Corporation

package com.pulumi.automation;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CancellationTokenTest {
    @Test
    void testNoneIsNeverCanceled() {
        assertThat(CancellationToken.none().isCancellationRequested()).isFalse();
    }

    @Test
    void testCancelIsObservableThroughSourceAndToken() {
        try (var source = new CancellationTokenSource()) {
            var token = source.token();
            assertThat(source.isCancellationRequested()).isFalse();
            assertThat(token.isCancellationRequested()).isFalse();

            source.cancel();

            assertThat(source.isCancellationRequested()).isTrue();
            assertThat(token.isCancellationRequested()).isTrue();
        }
    }

    @Test
    void testCallbackFiresOnCancel() {
        try (var source = new CancellationTokenSource()) {
            var calls = new AtomicInteger();
            source.token().register(calls::incrementAndGet);

            assertThat(calls.get()).isEqualTo(0);

            source.cancel();
            assertThat(calls.get()).isEqualTo(1);

            // cancel is idempotent, callbacks only fire once
            source.cancel();
            assertThat(calls.get()).isEqualTo(1);
        }
    }

    @Test
    void testCallbackFiresImmediatelyWhenAlreadyCanceled() {
        try (var source = new CancellationTokenSource()) {
            source.cancel();

            var calls = new AtomicInteger();
            source.token().register(calls::incrementAndGet);
            assertThat(calls.get()).isEqualTo(1);
        }
    }

    @Test
    void testClosedRegistrationDoesNotFire() {
        try (var source = new CancellationTokenSource()) {
            var calls = new AtomicInteger();
            var registration = source.token().register(calls::incrementAndGet);
            registration.close();

            source.cancel();
            assertThat(calls.get()).isEqualTo(0);
        }
    }

    @Test
    void testCallbacksFireInRegistrationOrder() {
        try (var source = new CancellationTokenSource()) {
            var order = new StringBuilder();
            source.token().register(() -> order.append("a"));
            source.token().register(() -> order.append("b"));

            source.cancel();
            assertThat(order.toString()).isEqualTo("ab");
        }
    }
}
