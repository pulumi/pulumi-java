package io.pulumi.core;

import com.google.common.collect.ImmutableSet;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.pulumi.core.Tuples.Tuple2;
import io.pulumi.core.Tuples.Tuple3;
import io.pulumi.core.internal.Internal;
import io.pulumi.core.internal.OutputBuilder;
import io.pulumi.core.internal.OutputData;
import io.pulumi.core.internal.OutputInternal;
import io.pulumi.deployment.*;
import io.pulumi.deployment.internal.DeploymentTests;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class OutputTests {

    @CanIgnoreReturnValue
    public static <T> OutputData<T> waitFor(Output<T> io) {
        return Internal.of(io).getDataAsync().join();
    }

    @CanIgnoreReturnValue
    public static <T1, T2>
    Tuple2<OutputData<T1>, OutputData<T2>>
    waitFor(Output<T1> io1, Output<T2> io2) {
        return Tuples.of(
                Internal.of(io1).getDataAsync().join(),
                Internal.of(io2).getDataAsync().join()
        );
    }

    @CanIgnoreReturnValue
    public static <T1, T2, T3>
    Tuple3<OutputData<T1>, OutputData<T2>, OutputData<T3>>
    waitFor(Output<T1> io1, Output<T2> io2, Output<T3> io3) {
        return Tuples.of(
                Internal.of(io1).getDataAsync().join(),
                Internal.of(io2).getDataAsync().join(),
                Internal.of(io3).getDataAsync().join()
        );
    }

    public static <T> Output<T> unknown(Deployment deployment) {
        return new OutputInternal<>(deployment, OutputData.ofNullable(ImmutableSet.of(), null, false, false));
    }

    public static <T> Output<T> unknownSecret(Deployment deployment) {
        return new OutputInternal<>(deployment, OutputData.ofNullable(ImmutableSet.of(), null, false, true));
    }

    public static TestContext testContext() {
        return new TestContext();
    }

    public static class TestContext {
        public final OutputBuilder output;
        public final Deployment deployment;

        public TestContext() {
            var mock = DeploymentTests.DeploymentMockBuilder.builder()
                    .setMocks(new MocksThatAlwaysThrow())
                    .buildSpyInstance();
            this.deployment = mock.getDeployment();
            this.output = OutputBuilder.forDeployment(this.deployment);
        }
    }

    public static class MocksThatAlwaysThrow implements Mocks {
        @Override
        public CompletableFuture<Tuple2<Optional<String>, Object>> newResourceAsync(MockResourceArgs args) {
            return CompletableFuture.failedFuture(new IllegalStateException(
                    "MocksThatAlwaysThrow do not support resources"));
        }

        @Override
        public CompletableFuture<Map<String, Object>> callAsync(MockCallArgs args) {
            return CompletableFuture.failedFuture(new IllegalStateException(
                    "MocksThatAlwaysThrow do not support calls"));
        }
    }
}
