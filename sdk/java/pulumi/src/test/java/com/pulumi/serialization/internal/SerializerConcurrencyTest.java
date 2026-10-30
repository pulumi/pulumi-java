package com.pulumi.serialization.internal;

import com.google.common.collect.ImmutableSet;
import com.pulumi.Log;
import com.pulumi.core.Output;
import com.pulumi.core.internal.OutputData;
import com.pulumi.core.internal.OutputInternal;
import com.pulumi.deployment.internal.EngineLogger;
import com.pulumi.resources.Resource;
import org.junit.jupiter.api.RepeatedTest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class SerializerConcurrencyTest {

    /**
     * Outputs nested in a single property can resolve on different threads, and each
     * resolution records its resources in {@link Serializer#dependentResources}.
     * Regression test for https://github.com/pulumi/pulumi-java/issues/2358.
     */
    @RepeatedTest(5)
    void collectsDependentResourcesFromOutputsResolvedConcurrently() throws Exception {
        final int count = 2_000;
        var executor = Executors.newFixedThreadPool(8);
        try {
            var start = new CountDownLatch(1);
            var resources = new ArrayList<Resource>(count);
            var outputs = new ArrayList<Output<String>>(count);
            for (int i = 0; i < count; i++) {
                var resource = mock(Resource.class);
                resources.add(resource);
                var value = "value-" + i;
                var data = CompletableFuture.supplyAsync(() -> {
                    try {
                        start.await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException(e);
                    }
                    return OutputData.of(ImmutableSet.of(resource), value);
                }, executor);
                outputs.add(new OutputInternal<>(data));
            }

            var serializer = new Serializer(new Log(EngineLogger.ignore()));
            var result = serializer.serializeAsync("prop", List.copyOf(outputs), true);
            start.countDown();
            result.get(30, TimeUnit.SECONDS);

            assertThat(serializer.dependentResources)
                    .hasSize(count)
                    .containsExactlyInAnyOrderElementsOf(resources);
            assertThat(ImmutableSet.copyOf(serializer.dependentResources)).hasSize(count);
        } finally {
            executor.shutdownNow();
        }
    }
}
