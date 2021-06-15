package io.pulumi.core;

import io.pulumi.test.internal.TestOptions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;

import static io.pulumi.deployment.internal.DeploymentTests.*;
import static org.assertj.core.api.Assertions.assertThat;

public class OutputsNormalTest {

    private static DeploymentMock mock;

    @BeforeAll
    public static void mockSetup() {
        mock = DeploymentMockBuilder.builder()
                .setOptions(new TestOptions(false))
                .setMockGlobalInstance();
    }

    @AfterAll
    static void cleanup() {
        cleanupDeploymentMocks();
    }

    @AfterEach
    public void printInternalErrorCount() {
        printErrorCount(mock.logger);
    }

    @Test
    void testApplyCanRunOnKnownValue() {
        var o1 = Output.of(0);
        var o2 = o1.applyValue(a -> a + 1);
        var data = InputOutputTests.waitFor(o2);
        assertThat(data.isKnown()).isTrue();
        assertThat(data.getValueNullable()).isNotNull();
        assertThat(data.getValueNullable()).isEqualTo(1);
    }

    @Test
    void testApplyCanRunOnKnownAwaitableValue() {
        var o1 = Output.of(0);
        var o2 = o1.applyFuture(a -> CompletableFuture.completedFuture("inner"));
        var data = InputOutputTests.waitFor(o2);
        assertThat(data.isKnown()).isTrue();
        assertThat(data.getValueNullable()).isNotNull();
        assertThat(data.getValueNullable()).isEqualTo("inner");
    }

    @Test
    void testApplyCanRunOnKnownKnownOutputValue() {
        var o1 = Output.of(0);
        var o2 = o1.applyValue(a -> "inner");
        var data = InputOutputTests.waitFor(o2);
        assertThat(data.isKnown()).isTrue();
        assertThat(data.getValueNullable()).isNotNull();
        assertThat(data.getValueNullable()).isEqualTo("inner");
    }

    @Test
    void testApplyCanRunOnKnownUnknownOutputValue() {
        var o1 = Output.of(0);
        var o2 = o1.apply(a -> InputOutputTests.unknown("inner"));
        var data = InputOutputTests.waitFor(o2);
        assertThat(data.isKnown()).isFalse();
        assertThat(data.getValueNullable()).isNotNull();
        assertThat(data.getValueNullable()).isEqualTo("inner");
    }

    @Test
    void testApplyProducesKnownOnUnknown() {
        var o1 = InputOutputTests.unknown(0);
        var o2 = o1.applyValue(a -> a + 1);
        var data = InputOutputTests.waitFor(o2);
        assertThat(data.isKnown()).isFalse();
        assertThat(data.getValueNullable()).isNotNull();
        assertThat(data.getValueNullable()).isEqualTo(1);
    }

    @Test
    void testApplyProducesKnownOnUnknownAwaitable() {
        var o1 = InputOutputTests.unknown(0);
        var o2 = o1.applyFuture(a -> CompletableFuture.completedFuture("inner"));
        var data = InputOutputTests.waitFor(o2);
        assertThat(data.isKnown()).isFalse();
        assertThat(data.getValueNullable()).isNotNull();
        assertThat(data.getValueNullable()).isEqualTo("inner");
    }

    @Test
    void testApplyProducesKnownOnUnknownKnownOutput() {
        var o1 = InputOutputTests.unknown(0);
        var o2 = o1.apply(a -> Output.of("inner"));
        var data = InputOutputTests.waitFor(o2);
        assertThat(data.isKnown()).isFalse();
        assertThat(data.getValueNullable()).isNotNull();
        assertThat(data.getValueNullable()).isEqualTo("inner");
    }

    @Test
    void testApplyProducesUnknownOnUnknownUnknownOutput() {
        var o1 = InputOutputTests.unknown(0);
        var o2 = o1.apply(a -> InputOutputTests.unknown("inner"));
        var data = InputOutputTests.waitFor(o2);
        assertThat(data.isKnown()).isFalse();
        assertThat(data.getValueNullable()).isNotNull();
        assertThat(data.getValueNullable()).isEqualTo("inner");
    }

    @Test
    void testApplyPreservesSecretOnKnown() {
        var o1 = Output.ofSecret(0);
        var o2 = o1.applyValue(a -> a + 1);
        var data = InputOutputTests.waitFor(o2);
        assertThat(data.isKnown()).isTrue();
        assertThat(data.isSecret()).isTrue();
        assertThat(data.getValueNullable()).isNotNull();
        assertThat(data.getValueNullable()).isEqualTo(1);
    }

    @Test
    void testApplyPreservesSecretOnKnownAwaitable() {
        var o1 = Output.ofSecret(0);
        var o2 = o1.applyFuture(a -> CompletableFuture.completedFuture("inner"));
        var data = InputOutputTests.waitFor(o2);
        assertThat(data.isKnown()).isTrue();
        assertThat(data.isSecret()).isTrue();
        assertThat(data.getValueNullable()).isNotNull();
        assertThat(data.getValueNullable()).isEqualTo("inner");
    }

    @Test
    void testApplyPreservesSecretOnKnownKnownOutput() {
        var o1 = Output.ofSecret(0);
        var o2 = o1.apply(a -> Output.of("inner"));
        var data = InputOutputTests.waitFor(o2);
        assertThat(data.isKnown()).isTrue();
        assertThat(data.isSecret()).isTrue();
        assertThat(data.getValueNullable()).isNotNull();
        assertThat(data.getValueNullable()).isEqualTo("inner");
    }

    @Test
    void testApplyPreservesSecretOnKnownUnknownOutput() {
        var o1 = Output.ofSecret(0);
        var o2 = o1.apply(a -> InputOutputTests.unknown("inner"));
        var data = InputOutputTests.waitFor(o2);
        assertThat(data.isKnown()).isFalse();
        assertThat(data.isSecret()).isTrue();
        assertThat(data.getValueNullable()).isNotNull();
        assertThat(data.getValueNullable()).isEqualTo("inner");
    }

    @Test
    void testApplyPreservesSecretOnUnknown() {
        var o1 = InputOutputTests.unknownSecret(0);
        var o2 = o1.applyValue(a -> a + 1);
        var data = InputOutputTests.waitFor(o2);
        assertThat(data.isKnown()).isFalse();
        assertThat(data.isSecret()).isTrue();
        assertThat(data.getValueNullable()).isNotNull();
        assertThat(data.getValueNullable()).isEqualTo(1);
    }

    @Test
    void testApplyPreservesSecretOnUnknownAwaitable() {
        var o1 = InputOutputTests.unknownSecret(0);
        var o2 = o1.applyFuture(a -> CompletableFuture.completedFuture("inner"));
        var data = InputOutputTests.waitFor(o2);
        assertThat(data.isKnown()).isFalse();
        assertThat(data.isSecret()).isTrue();
        assertThat(data.getValueNullable()).isNotNull();
        assertThat(data.getValueNullable()).isEqualTo("inner");
    }

    @Test
    void testApplyPreservesSecretOnUnknownKnownOutput() {
        var o1 = InputOutputTests.unknownSecret(0);
        var o2 = o1.apply(a -> Output.of("inner"));
        var data = InputOutputTests.waitFor(o2);
        assertThat(data.isKnown()).isFalse();
        assertThat(data.isSecret()).isTrue();
        assertThat(data.getValueNullable()).isNotNull();
        assertThat(data.getValueNullable()).isEqualTo("inner");
    }

    @Test
    void testApplyPreservesSecretOnUnknownUnknownOutput() {
        var o1 = InputOutputTests.unknownSecret(0);
        var o2 = o1.apply(a -> InputOutputTests.unknown("inner"));
        var data = InputOutputTests.waitFor(o2);
        assertThat(data.isKnown()).isFalse();
        assertThat(data.isSecret()).isTrue();
        assertThat(data.getValueNullable()).isNotNull();
        assertThat(data.getValueNullable()).isEqualTo("inner");
    }

    @Test
    void testApplyPropagatesSecretOnKnownKnownOutput() {
        var o1 = Output.of(0);
        var o2 = o1.apply(a -> Output.ofSecret("inner"));
        var data = InputOutputTests.waitFor(o2);
        assertThat(data.isKnown()).isTrue();
        assertThat(data.isSecret()).isTrue();
        assertThat(data.getValueNullable()).isNotNull();
        assertThat(data.getValueNullable()).isEqualTo("inner");
    }


    @Test
    void testApplyPropagatesSecretOnKnownUnknownOutput() {
        var o1 = Output.of(0);
        var o2 = o1.apply(a -> InputOutputTests.unknownSecret("inner"));
        var data = InputOutputTests.waitFor(o2);
        assertThat(data.isKnown()).isFalse();
        assertThat(data.isSecret()).isTrue();
        assertThat(data.getValueNullable()).isNotNull();
        assertThat(data.getValueNullable()).isEqualTo("inner");
    }

    @Test
    void testApplyPropagatesSecretOnUnknownKnownOutput() {
        var o1 = InputOutputTests.unknown(0);
        var o2 = o1.apply(a -> Output.ofSecret("inner"));
        var data = InputOutputTests.waitFor(o2);
        assertThat(data.isKnown()).isFalse();
        assertThat(data.isSecret()).isTrue();
        assertThat(data.getValueNullable()).isNotNull();
        assertThat(data.getValueNullable()).isEqualTo("inner");
    }

    @Test
    void testApplyPropagatesSecretOnUnknownUnknownOutput() {
        var o1 = InputOutputTests.unknown(0);
        var o2 = o1.apply(a -> InputOutputTests.unknownSecret("inner"));
        var data = InputOutputTests.waitFor(o2);
        assertThat(data.isKnown()).isFalse();
        assertThat(data.isSecret()).isTrue();
        assertThat(data.getValueNullable()).isNotNull();
        assertThat(data.getValueNullable()).isEqualTo("inner");
    }

    @Test
    void testCreateUnknownRunsValueFactory() {
        var output = InputOutputTests.unknown(() -> CompletableFuture.completedFuture("value"));
        var data = InputOutputTests.waitFor(output);
        assertThat(data.isKnown()).isFalse();
        assertThat(data.getValueNullable()).isNotNull();
        assertThat(data.getValueNullable()).isEqualTo("value");
    }
}
