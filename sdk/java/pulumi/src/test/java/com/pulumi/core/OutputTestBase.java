package com.pulumi.core;

import com.pulumi.core.internal.Internal;
import com.pulumi.deployment.MocksTest;
import com.pulumi.deployment.internal.TestOptions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static com.pulumi.deployment.internal.DeploymentTests.DeploymentMockBuilder;
import static com.pulumi.deployment.internal.DeploymentTests.cleanupDeploymentMocks;
import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class OutputTestBase {
    abstract boolean isPreview();

    @BeforeAll
    public void mockSetup() {
        DeploymentMockBuilder.builder()
                .setMocks(new MocksTest.MyMocks())
                .setOptions(new TestOptions(isPreview()))
                .setMockGlobalInstance();
    }

    @AfterAll
    void cleanup() {
        cleanupDeploymentMocks();
    }

    @Test
    void testApplyCanRunOnKnownValue() {
        var o1 = Output.of(0);
        var o2 = o1.applyValue(a -> a + 1);
        var data = OutputTests.waitFor(o2);
        assertThat(data.isKnown()).isTrue();
        assertThat(data.getValueNullable()).isNotNull().isEqualTo(1);
    }

    @Test
    void testApplyCanRunOnKnownAwaitableValue() {
        var o1 = Output.of(0);
        var o2 = o1.apply(a -> Output.of(CompletableFuture.completedFuture("inner")));
        var data = OutputTests.waitFor(o2);
        assertThat(data.isKnown()).isTrue();
        assertThat(data.getValueNullable()).isNotNull().isEqualTo("inner");
    }

    @Test
    void testApplyCanRunOnKnownKnownOutputValue() {
        var o1 = Output.of(0);
        var o2 = o1.applyValue(a -> "inner");
        var data = OutputTests.waitFor(o2);
        assertThat(data.isKnown()).isTrue();
        assertThat(data.getValueNullable()).isNotNull().isEqualTo("inner");
    }

    @Test
    void testApplyFloatsUnknown() {
        var o1 = Output.of(0);
        var o2 = o1.apply(a -> OutputTests.unknown());
        var data = OutputTests.waitFor(o2);
        assertThat(data.isKnown()).isFalse();
        assertThat(data.getValueNullable()).isNull();
    }

    @Test
    void testApplyValueTerminatesOnUnknown() {
        var runCounter = new AtomicInteger(0);
        Output<Integer> o1 = OutputTests.unknown();
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
        var o1 = OutputTests.unknown();
        var runCounter = new AtomicInteger(0);
        var o2 = o1.apply(a ->
        {
            runCounter.incrementAndGet();
            return Output.of(CompletableFuture.completedFuture("inner"));
        });
        var data = OutputTests.waitFor(o2);
        assertThat(data.isKnown()).isFalse();
        assertThat(data.getValueNullable()).isNull();
        assertThat(runCounter.get()).isEqualTo(0);
    }

    @Test
    void testApplyTerminatesOnUnkonwn() {
        var o1 = OutputTests.unknown();
        var runCounter = new AtomicInteger(0);
        var o2 = o1.apply(a ->
        {
            runCounter.incrementAndGet();
            return Output.of("inner");
        });
        var data = OutputTests.waitFor(o2);
        assertThat(data.isKnown()).isFalse();
        assertThat(data.getValueNullable()).isNull();
        assertThat(runCounter.get()).isEqualTo(0);
    }

    @Test
    void testApplyPreservesSecretOnKnown() {
        var o1 = Output.ofSecret(0);
        var o2 = o1.applyValue(a -> a + 1);
        var data = OutputTests.waitFor(o2);
        assertThat(data.isKnown()).isTrue();
        assertThat(data.isSecret()).isTrue();
        assertThat(data.getValueNullable()).isNotNull().isEqualTo(1);
    }

    @Test
    void testApplyPreservesSecretOnKnownAwaitable() {
        var o1 = Output.ofSecret(0);
        var o2 = o1.apply(a -> Output.of(CompletableFuture.completedFuture("inner")));
        var data = OutputTests.waitFor(o2);
        assertThat(data.isKnown()).isTrue();
        assertThat(data.isSecret()).isTrue();
        assertThat(data.getValueNullable()).isNotNull().isEqualTo("inner");
    }

    @Test
    void testApplyPreservesSecretOnKnownKnownOutput() {
        var o1 = Output.ofSecret(0);
        var o2 = o1.apply(a -> Output.of("inner"));
        var data = OutputTests.waitFor(o2);
        assertThat(data.isKnown()).isTrue();
        assertThat(data.isSecret()).isTrue();
        assertThat(data.getValueNullable()).isNotNull().isEqualTo("inner");
    }

    @Test
    void testApplyPreservesSecretOnKnownUnknownOutput() {
        var o1 = Output.ofSecret(0);
        var o2 = o1.apply(a -> OutputTests.unknown());
        var data = OutputTests.waitFor(o2);
        assertThat(data.isKnown()).isFalse();
        assertThat(data.isSecret()).isTrue();
        assertThat(data.getValueNullable()).isNull();
    }

    @Test
    void testApplyPreservesSecretOnUnknown() {
        Output<Integer> o1 = OutputTests.unknownSecret();
        var o2 = o1.applyValue(a -> a + 1);
        var data = OutputTests.waitFor(o2);
        assertThat(data.isKnown()).isFalse();
        assertThat(data.isSecret()).isTrue();
        assertThat(data.getValueNullable()).isNull();
    }

    @Test
    void testApplyPreservesSecretOnUnknownAwaitable() {
        Output<Integer> o1 = OutputTests.unknownSecret();
        var o2 = o1.apply(a -> Output.of(CompletableFuture.completedFuture("inner")));
        var data = OutputTests.waitFor(o2);
        assertThat(data.isKnown()).isFalse();
        assertThat(data.isSecret()).isTrue();
        assertThat(data.getValueNullable()).isNull();
    }

    @Test
    void testApplyPropagatesSecretOnKnownKnownOutput() {
        var o1 = Output.of(0);
        var o2 = o1.apply(a -> Output.ofSecret("inner"));
        var data = OutputTests.waitFor(o2);
        assertThat(data.isKnown()).isTrue();
        assertThat(data.isSecret()).isTrue();
        assertThat(data.getValueNullable()).isNotNull().isEqualTo("inner");
    }

    @Test
    void testApplyPropagatesSecretOnKnownUnknownOutput() {
        var o1 = Output.of(0);
        var o2 = o1.apply(a -> OutputTests.unknownSecret());
        var data = OutputTests.waitFor(o2);
        assertThat(data.isKnown()).isFalse();
        assertThat(data.isSecret()).isTrue();
        assertThat(data.getValueNullable()).isNull();
    }

    @Test
    void testApplyPreservesSecretOnUnknownKnownOutput() {
        var o1 = OutputTests.unknownSecret();
        var o2 = o1.apply(a -> Output.of("inner"));
        var data = OutputTests.waitFor(o2);
        assertThat(data.isKnown()).isFalse();
        assertThat(data.isSecret()).isTrue();
        assertThat(data.getValueNullable()).isNull();
    }

    @Test
    void testApplyPreservesSecretOnUnknownUnknownOutput() {
        var o1 = OutputTests.unknownSecret();
        var o2 = o1.apply(a -> OutputTests.unknown());
        var data = OutputTests.waitFor(o2);
        assertThat(data.isKnown()).isFalse();
        assertThat(data.isSecret()).isTrue();
        assertThat(data.getValueNullable()).isNull();
    }

    @Test
    void testApplyPropagatesSecretOnUnknownKnownOutput() {
        var o1 = OutputTests.unknown();
        var o2 = o1.apply(a -> Output.ofSecret("inner"));
        var data = OutputTests.waitFor(o2);
        assertThat(data.isKnown()).isFalse();
        assertThat(data.isSecret()).isFalse();
        assertThat(data.getValueNullable()).isNull();
    }

    @Test
    void testApplyPropagatesSecretOnUnknownUnknownOutput() {
        var o1 = OutputTests.unknown();
        var o2 = o1.apply(a -> OutputTests.unknownSecret());
        var data = OutputTests.waitFor(o2);
        assertThat(data.isKnown()).isFalse();
        assertThat(data.isSecret()).isFalse();
        assertThat(data.getValueNullable()).isNull();
    }

    @Test
    void testApplyTupleHandlesNulls() {
        var output = Output.tuple(Output.ofNullable(null), Output.ofNullable(null));
        var data = OutputTests.waitFor(output);
        assertThat(data.isKnown()).isTrue();
        assertThat(data.getValueNullable()).isNotNull().isEqualTo(Tuples.of(null, null));
    }

    @Test
    void testApplyTupleHandlesUnknown() {
        var output = Output.tuple(OutputTests.unknown(), OutputTests.unknown());
        var data = OutputTests.waitFor(output);
        assertThat(data.isKnown()).isFalse();
        assertThat(data.getValueNullable()).isNull();
    }

    @Test
    void testAllParamsOutputs() {
        var o1 = Output.of(1);
        var o2 = Output.of(2);
        var o3 = Output.all(o1, o2);
        var data = OutputTests.waitFor(o3);
        assertThat(data.getValueNullable()).containsExactly(1, 2);
    }

    @Test
    void testIsSecretAsyncOnKnownOutput() {
        var o1 = Output.ofSecret(0);
        var o2 = Output.of(1);
        var isSecret1 = Internal.of(o1).isSecret().join();
        var isSecret2 = Internal.of(o2).isSecret().join();
        assertThat(isSecret1).isTrue();
        assertThat(isSecret2).isFalse();
    }

    @Test
    void testIsSecretAsyncOnAwaitableOutput() {
        var o1 = Output.ofSecret(0).apply(a -> Output.of(CompletableFuture.completedFuture("inner1")));
        var o2 = Output.of(1).apply(a -> Output.of(CompletableFuture.completedFuture("inner2")));
        var isSecret1 = Internal.of(o1).isSecret().join();
        var isSecret2 = Internal.of(o2).isSecret().join();
        assertThat(isSecret1).isTrue();
        assertThat(isSecret2).isFalse();
    }

    @Test
    void testUnsecretOnKnownSecretValue() {
        var secret = Output.ofSecret(1);
        var notSecret = secret.asPlaintext();
        var notSecretData = Internal.of(notSecret).getDataAsync().join();
        assertThat(notSecretData.isSecret()).isFalse();
        assertThat(notSecretData.getValueNullable()).isNotNull().isEqualTo(1);
    }

    @Test
    void testUnsecretOnAwaitableSecretValue() {
        var secret = Output.ofSecret(1).apply(a -> Output.of(CompletableFuture.completedFuture("inner")));
        var notSecret = secret.asPlaintext();
        var notSecretData = Internal.of(notSecret).getDataAsync().join();
        assertThat(notSecretData.isSecret()).isFalse();
        assertThat(notSecretData.getValueNullable()).isNotNull().isEqualTo("inner");
    }

    @Test
    void testUnsecretOnNonSecretValue() {
        var secret = Output.of(2);
        var notSecret = secret.asPlaintext();
        var notSecretData = Internal.of(notSecret).getDataAsync().join();
        assertThat(notSecretData.isSecret()).isFalse();
        assertThat(notSecretData.getValueNullable()).isNotNull().isEqualTo(2);
    }
}
