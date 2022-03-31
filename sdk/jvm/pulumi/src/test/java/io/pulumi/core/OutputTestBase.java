package io.pulumi.core;

import io.pulumi.core.internal.Internal;
import io.pulumi.core.internal.OutputBuilder;
import io.pulumi.deployment.Deployment;
import io.pulumi.deployment.MocksTest;
import io.pulumi.deployment.internal.TestOptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static io.pulumi.deployment.internal.DeploymentTests.DeploymentMockBuilder;
import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class OutputTestBase {
    abstract boolean isPreview();

    private Deployment deployment;
    private OutputBuilder output;

    @BeforeAll
    public void mockSetup() {
        deployment = DeploymentMockBuilder.builder()
                .setMocks(new MocksTest.MyMocks())
                .setOptions(new TestOptions(isPreview()))
                .buildMockInstance()
                .getDeployment();
        output = OutputBuilder.forDeployment(deployment);
    }

    @Test
    void testApplyCanRunOnKnownValue() {
        var o1 = output.of(0);
        var o2 = o1.applyValue(a -> a + 1);
        var data = OutputTests.waitFor(o2);
        assertThat(data.isKnown()).isTrue();
        assertThat(data.getValueNullable()).isNotNull().isEqualTo(1);
    }

    @Test
    void testApplyCanRunOnKnownAwaitableValue() {
        var o1 = output.of(0);
        var o2 = o1.applyFuture(a -> CompletableFuture.completedFuture("inner"));
        var data = OutputTests.waitFor(o2);
        assertThat(data.isKnown()).isTrue();
        assertThat(data.getValueNullable()).isNotNull().isEqualTo("inner");
    }

    @Test
    void testApplyCanRunOnKnownKnownOutputValue() {
        var o1 = output.of(0);
        var o2 = o1.applyValue(a -> "inner");
        var data = OutputTests.waitFor(o2);
        assertThat(data.isKnown()).isTrue();
        assertThat(data.getValueNullable()).isNotNull().isEqualTo("inner");
    }

    @Test
    void testApplyFloatsUnknown() {
        var ctx = OutputTests.testContext();
        var o1 = output.of(0);
        var o2 = o1.apply(a -> OutputTests.unknown(ctx.deployment));
        var data = OutputTests.waitFor(o2);
        assertThat(data.isKnown()).isFalse();
        assertThat(data.getValueNullable()).isNull();
    }

    @Test
    void testApplyValueTerminatesOnUnknown() {
        var ctx = OutputTests.testContext();
        var runCounter = new AtomicInteger(0);
        Output<Integer> o1 = OutputTests.unknown(ctx.deployment);
        var o2 = o1.applyValue(a ->
        {
            runCounter.incrementAndGet();
            return (a + 1);
        });
        var data = OutputTests.waitFor(o2);
        assertThat(data.isKnown()).isFalse();
        assertThat(data.getValueNullable()).isNull();
        assertThat(runCounter.get()).isEqualTo(0);
    }

    @Test
    void testApplyFutureTerminatesOnUnknown() {
        var ctx = OutputTests.testContext();
        var o1 = OutputTests.unknown(ctx.deployment);
        var runCounter = new AtomicInteger(0);
        var o2 = o1.applyFuture(a ->
        {
            runCounter.incrementAndGet();
            return CompletableFuture.completedFuture("inner");
        });
        var data = OutputTests.waitFor(o2);
        assertThat(data.isKnown()).isFalse();
        assertThat(data.getValueNullable()).isNull();
        assertThat(runCounter.get()).isEqualTo(0);
    }

    @Test
    void testApplyTerminatesOnUnkonwn() {
        var ctx = OutputTests.testContext();
        var o1 = OutputTests.unknown(ctx.deployment);
        var runCounter = new AtomicInteger(0);
        var o2 = o1.apply(a ->
        {
            runCounter.incrementAndGet();
            return output.of("inner");
        });
        var data = OutputTests.waitFor(o2);
        assertThat(data.isKnown()).isFalse();
        assertThat(data.getValueNullable()).isNull();
        assertThat(runCounter.get()).isEqualTo(0);
    }

    @Test
    void testApplyPreservesSecretOnKnown() {
        var o1 = output.ofSecret(0);
        var o2 = o1.applyValue(a -> a + 1);
        var data = OutputTests.waitFor(o2);
        assertThat(data.isKnown()).isTrue();
        assertThat(data.isSecret()).isTrue();
        assertThat(data.getValueNullable()).isNotNull().isEqualTo(1);
    }

    @Test
    void testApplyPreservesSecretOnKnownAwaitable() {
        var o1 = output.ofSecret(0);
        var o2 = o1.applyFuture(a -> CompletableFuture.completedFuture("inner"));
        var data = OutputTests.waitFor(o2);
        assertThat(data.isKnown()).isTrue();
        assertThat(data.isSecret()).isTrue();
        assertThat(data.getValueNullable()).isNotNull().isEqualTo("inner");
    }

    @Test
    void testApplyPreservesSecretOnKnownKnownOutput() {
        var o1 = output.ofSecret(0);
        var o2 = o1.apply(a -> output.of("inner"));
        var data = OutputTests.waitFor(o2);
        assertThat(data.isKnown()).isTrue();
        assertThat(data.isSecret()).isTrue();
        assertThat(data.getValueNullable()).isNotNull().isEqualTo("inner");
    }

    @Test
    void testApplyPreservesSecretOnKnownUnknownOutput() {
        var ctx = OutputTests.testContext();
        var o1 = output.ofSecret(0);
        var o2 = o1.apply(a -> OutputTests.unknown(ctx.deployment));
        var data = OutputTests.waitFor(o2);
        assertThat(data.isKnown()).isFalse();
        assertThat(data.isSecret()).isTrue();
        assertThat(data.getValueNullable()).isNull();
    }

    @Test
    void testApplyPreservesSecretOnUnknown() {
        var ctx = OutputTests.testContext();
        Output<Integer> o1 = OutputTests.unknownSecret(ctx.deployment);
        var o2 = o1.applyValue(a -> a + 1);
        var data = OutputTests.waitFor(o2);
        assertThat(data.isKnown()).isFalse();
        assertThat(data.isSecret()).isTrue();
        assertThat(data.getValueNullable()).isNull();
    }

    @Test
    void testApplyPreservesSecretOnUnknownAwaitable() {
        var ctx = OutputTests.testContext();
        Output<Integer> o1 = OutputTests.unknownSecret(ctx.deployment);
        var o2 = o1.applyFuture(a -> CompletableFuture.completedFuture("inner"));
        var data = OutputTests.waitFor(o2);
        assertThat(data.isKnown()).isFalse();
        assertThat(data.isSecret()).isTrue();
        assertThat(data.getValueNullable()).isNull();
    }

    @Test
    void testApplyPropagatesSecretOnKnownKnownOutput() {
        var o1 = output.of(0);
        var o2 = o1.apply(a -> output.ofSecret("inner"));
        var data = OutputTests.waitFor(o2);
        assertThat(data.isKnown()).isTrue();
        assertThat(data.isSecret()).isTrue();
        assertThat(data.getValueNullable()).isNotNull().isEqualTo("inner");
    }

    @Test
    void testApplyPropagatesSecretOnKnownUnknownOutput() {
        var ctx = OutputTests.testContext();
        var o1 = output.of(0);
        var o2 = o1.apply(a -> OutputTests.unknownSecret(ctx.deployment));
        var data = OutputTests.waitFor(o2);
        assertThat(data.isKnown()).isFalse();
        assertThat(data.isSecret()).isTrue();
        assertThat(data.getValueNullable()).isNull();
    }

    @Test
    void testApplyPreservesSecretOnUnknownKnownOutput() {
        var ctx = OutputTests.testContext();
        var o1 = OutputTests.unknownSecret(ctx.deployment);
        var o2 = o1.apply(a -> output.of("inner"));
        var data = OutputTests.waitFor(o2);
        assertThat(data.isKnown()).isFalse();
        assertThat(data.isSecret()).isTrue();
        assertThat(data.getValueNullable()).isNull();
    }

    @Test
    void testApplyPreservesSecretOnUnknownUnknownOutput() {
        var ctx = OutputTests.testContext();
        var o1 = OutputTests.unknownSecret(ctx.deployment);
        var o2 = o1.apply(a -> OutputTests.unknown(ctx.deployment));
        var data = OutputTests.waitFor(o2);
        assertThat(data.isKnown()).isFalse();
        assertThat(data.isSecret()).isTrue();
        assertThat(data.getValueNullable()).isNull();
    }

    @Test
    void testApplyPropagatesSecretOnUnknownKnownOutput() {
        var ctx = OutputTests.testContext();
        var o1 = OutputTests.unknown(ctx.deployment);
        var o2 = o1.apply(a -> output.ofSecret("inner"));
        var data = OutputTests.waitFor(o2);
        assertThat(data.isKnown()).isFalse();
        assertThat(data.isSecret()).isFalse();
        assertThat(data.getValueNullable()).isNull();
    }

    @Test
    void testApplyPropagatesSecretOnUnknownUnknownOutput() {
        var ctx = OutputTests.testContext();
        var o1 = OutputTests.unknown(ctx.deployment);
        var o2 = o1.apply(a -> OutputTests.unknownSecret(ctx.deployment));
        var data = OutputTests.waitFor(o2);
        assertThat(data.isKnown()).isFalse();
        assertThat(data.isSecret()).isFalse();
        assertThat(data.getValueNullable()).isNull();
    }

    @Test
    void testApplyTupleHandlesEmpty() {
        var out = output.tuple(output.empty(), output.empty());
        var data = OutputTests.waitFor(out);
        assertThat(data.isKnown()).isTrue();
        assertThat(data.getValueNullable()).isNotNull().isEqualTo(Tuples.of(null, null));
    }

    @Test
    void testApplyTupleHandlesUnknown() {
        var ctx = OutputTests.testContext();
        var out = output.tuple(
                OutputTests.unknown(ctx.deployment),
                OutputTests.unknown(ctx.deployment));
        var data = OutputTests.waitFor(out);
        assertThat(data.isKnown()).isFalse();
        assertThat(data.getValueNullable()).isNull();
    }

    @Test
    void testAllParamsOutputs() {
        var o1 = output.of(1);
        var o2 = output.of(2);
        var o3 = output.all(o1, o2);
        var data = OutputTests.waitFor(o3);
        assertThat(data.getValueNullable()).containsExactly(1, 2);
    }

    @Test
    void testIsSecretAsyncOnKnownOutput() {
        var o1 = output.ofSecret(0);
        var o2 = output.of(1);
        var isSecret1 = Internal.of(o1).isSecret().join();
        var isSecret2 = Internal.of(o2).isSecret().join();
        assertThat(isSecret1).isTrue();
        assertThat(isSecret2).isFalse();
    }

    @Test
    void testIsSecretAsyncOnAwaitableOutput() {
        var o1 = output.ofSecret(0).applyFuture(a -> CompletableFuture.completedFuture("inner1"));
        var o2 = output.of(1).applyFuture(a -> CompletableFuture.completedFuture("inner2"));
        var isSecret1 = Internal.of(o1).isSecret().join();
        var isSecret2 = Internal.of(o2).isSecret().join();
        assertThat(isSecret1).isTrue();
        assertThat(isSecret2).isFalse();
    }

    @Test
    void testUnsecretOnKnownSecretValue() {
        var secret = output.ofSecret(1);
        var notSecret = secret.asPlaintext();
        var notSecretData = Internal.of(notSecret).getDataAsync().join();
        assertThat(notSecretData.isSecret()).isFalse();
        assertThat(notSecretData.getValueNullable()).isNotNull().isEqualTo(1);
    }

    @Test
    void testUnsecretOnAwaitableSecretValue() {
        var secret = output.ofSecret(1).applyFuture(a -> CompletableFuture.completedFuture("inner"));
        var notSecret = secret.asPlaintext();
        var notSecretData = Internal.of(notSecret).getDataAsync().join();
        assertThat(notSecretData.isSecret()).isFalse();
        assertThat(notSecretData.getValueNullable()).isNotNull().isEqualTo("inner");
    }

    @Test
    void testUnsecretOnNonSecretValue() {
        var secret = output.of(2);
        var notSecret = secret.asPlaintext();
        var notSecretData = Internal.of(notSecret).getDataAsync().join();
        assertThat(notSecretData.isSecret()).isFalse();
        assertThat(notSecretData.getValueNullable()).isNotNull().isEqualTo(2);
    }
}
