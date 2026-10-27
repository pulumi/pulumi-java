package com.pulumi.multiarg;

import com.pulumi.core.Output;
import com.pulumi.multiarg.inputs.LookupValue;
import com.pulumi.multiarg.outputs.LookupResult;
import com.pulumi.test.Mocks;
import com.pulumi.test.TestOptions;
import com.pulumi.test.internal.PulumiTestInternal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import static com.pulumi.test.PulumiTest.extractValue;
import static org.assertj.core.api.Assertions.assertThat;

class MultiArgTest {

    @AfterEach
    void cleanup() {
        PulumiTestInternal.cleanup();
    }

    @Test
    void testOutputInvokeWithObjectArguments() {
        var mocks = new RecordingMocks();
        var result = run(mocks, () -> MultiargFunctions.lookup(
                value("a"), List.of(value("b")), Map.of("k", value("c"))));

        assertThat(result).isEqualTo("ok");
        assertThat(mocks.args).isEqualTo(Map.of(
                "value", Map.of("name", "a"),
                "list", List.of(Map.of("name", "b")),
                "map", Map.of("k", Map.of("name", "c"))));
    }

    @Test
    void testPlainInvokeWithObjectArguments() {
        var mocks = new RecordingMocks();
        var result = run(mocks, () -> Output.of(MultiargFunctions.lookupPlain(
                value("a"), List.of(value("b")), Map.of("k", value("c")))));

        assertThat(result).isEqualTo("ok");
        assertThat(mocks.args).isEqualTo(Map.of(
                "value", Map.of("name", "a"),
                "list", List.of(Map.of("name", "b")),
                "map", Map.of("k", Map.of("name", "c"))));
    }

    @Test
    void testOutputInvokeWithOmittedOptionalArguments() {
        var mocks = new RecordingMocks();
        var result = run(mocks, () -> MultiargFunctions.lookup(value("a"), null, null));

        assertThat(result).isEqualTo("ok");
        assertThat(mocks.args).isEqualTo(Map.of("value", Map.of("name", "a")));
    }

    private static LookupValue value(String name) {
        return LookupValue.builder().name(name).build();
    }

    private static String run(Mocks mocks, Supplier<Output<LookupResult>> invoke) {
        var test = PulumiTestInternal.builder()
                .options(TestOptions.builder().preview(true).build())
                .mocks(mocks)
                .build();
        var result = test.runTest(ctx -> ctx.export("result", invoke.get().applyValue(r -> r.result())))
                .throwOnError();
        return extractValue(result.output("result", String.class));
    }

    private static class RecordingMocks implements Mocks {
        Map<String, Object> args;

        @Override
        public CompletableFuture<ResourceResult> newResourceAsync(ResourceArgs args) {
            throw new IllegalArgumentException(String.format("Unknown resource '%s'", args.type));
        }

        @Override
        public CompletableFuture<Map<String, Object>> callAsync(CallArgs args) {
            assertThat(args.token).isEqualTo("multiarg:index:lookup");
            this.args = args.args;
            return CompletableFuture.completedFuture(Map.of("result", "ok"));
        }
    }
}
