package com.pulumi.core;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.pulumi.core.internal.ContextAwareCompletableFuture;
import com.pulumi.test.internal.PulumiTestInternal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.logging.Level;

import static com.pulumi.test.internal.PulumiTestInternal.extractOutputData;
import static com.pulumi.test.internal.PulumiTestInternal.logger;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;

public class OutputStaticsTest {
    @AfterEach
    public void cleanup() {
        PulumiTestInternal.cleanup();
    }

    @BeforeEach
    public void setup() {
        PulumiTestInternal.builder()
                .standardLogger(logger(Level.OFF))
                .build();
    }

    @Test
    void testListConcatNull() {
        var result = Output.concatList(null, null);
        var data = extractOutputData(result);

        assertThat(data.isSecret()).isFalse();
        assertThat(data.isKnown()).isTrue();
        assertThat(data.getValueNullable()).isNotNull().isEmpty();
    }

    @Test
    void testListConcatCoercesNullToEmpty() {
        var nullList = Output.ofNullable((List<Object>)null);
        var result = Output.concatList(nullList, nullList);
        var data = extractOutputData(result);

        assertThat(data.isSecret()).isFalse();
        assertThat(data.isKnown()).isTrue();
        assertThat(data.getValueNullable()).isNotNull().isEmpty();
    }

    @Test
    void testListConcatSimple() {
        final var outV2 = Output.of("V2");
        final var outV3 = Output.of("V3");
        final var list1 = Output.<String>listBuilder()
                .add("V1")
                .add(outV2)
                .build();

        final Output<List<String>> list2 = Output.<String>listBuilder()
                .add(outV3)
                .add("V4")
                .build();

        var result = Output.concatList(list1, list2);
        var data = extractOutputData(result);

        assertThat(data.isSecret()).isFalse();
        assertThat(data.isKnown()).isTrue();
        assertThat(data.getValueNullable()).hasSize(4);
        assertThat(data.getValueNullable()).containsOnly(
                "V1",
                extractOutputData(outV2).getValueNullable(),
                extractOutputData(outV3).getValueNullable(),
                "V4"
        );

        // Check that the input maps haven't changed
        extractOutputData(
                Output.tuple(list1, outV2)
                        .applyValue(t -> {
                    assertThat(t.t1).hasSize(2);
                    assertThat(t.t1).contains("V1", t.t2);
                    return (Void) null;
                })
        );
        extractOutputData(
                Output.tuple(list2, outV3).applyValue(t -> {
                    assertThat(t.t1).hasSize(2);
                    assertThat(t.t1).containsOnly(t.t2, "V4");
                    return (Void) null;
                })
        );
    }

    @Test
    void testMapConcatNull() {
        var result = Output.concatMap(null, null);
        var data = extractOutputData(result);

        assertThat(data.isSecret()).isFalse();
        assertThat(data.isKnown()).isTrue();
        assertThat(data.getValueNullable()).isNotNull().isEmpty();
    }

    @Test
    void testMapConcatNullAsEmpty() {
        var result = Output.concatMap(
                Output.ofNullable((Map<String,Object>)null),
                Output.ofNullable((Map<String,Object>)null));
        var data = extractOutputData(result);
        assertThat(data.isSecret()).isFalse();
        assertThat(data.isKnown()).isTrue();
        assertThat(data.getValueNullable()).isNotNull().isEmpty();
    }

    @Test
    void testMapConcatSimple() {
        final var outV2 = Output.of("V2");
        final var outV3 = Output.of("V3");
        final var outV3wrong = Output.of("V3_wrong");
        final var map1 = Output.<String>mapBuilder()
                .put("K1", "V1")
                .put("K2", outV2)
                .put("K3", outV3wrong)
                .build();

        final var map2 = Output.<String>mapBuilder()
                .put("K3", outV3)
                .put("K4", "V4")
                .build();

        var result = Output.concatMap(map1, map2);
        var data = extractOutputData(result);

        assertThat(data.isSecret()).isFalse();
        assertThat(data.isKnown()).isTrue();
        assertThat(data.getValueNullable()).hasSize(4);
        for (var i = 1; i <= 4; i++) {
            assertThat(data.getValueNullable()).containsEntry("K" + i, "V" + i);
        }

        // Check that the input maps haven't changed
        extractOutputData(
                Output.tuple(map1, outV3wrong).applyValue(t -> {
                    assertThat(t.t1).hasSize(3);
                    assertThat(t.t1).contains(entry("K3", t.t2));
                    return (Void) null;
                })
        );
        extractOutputData(
                Output.tuple(map2, outV3).applyValue(t -> {
                    assertThat(t.t1).hasSize(2);
                    assertThat(t.t1).contains(entry("K3", t.t2));
                    return (Void) null;
                })
        );
    }

    @Test
    void testOutputMapEitherInitializer() {
        var sample = SampleArgs.builder()
                .add(Map.of(
                        "left", Either.ofLeft("testValue"),
                        "right", Either.ofRight(123)
                ))
                .build();

        var data = extractOutputData(sample.dict);
        assertThat(data.getValueNullable()).hasSize(2);
        assertThat(data.getValueNullable()).containsValue(Either.ofLeft("testValue"));
        assertThat(data.getValueNullable()).containsValue(Either.ofRight(123));
    }

    @Test
    void testOutputListEitherInitializer() {
        var sample = SampleArgs.builder()
                .add(List.of(
                        Either.ofLeft("testValue"),
                        Either.ofRight(123)
                ))
                .build();
        var data = extractOutputData(sample.list);
        assertThat(data.getValueNullable()).hasSize(2);
        assertThat(data.getValueNullable()).containsOnlyOnce(Either.ofLeft("testValue"));
        assertThat(data.getValueNullable()).containsOnlyOnce(Either.ofRight(123));
    }

    private static final class SampleArgs {

        public final Output<List<Either<String, Integer>>> list;
        public final Output<Map<String, Either<String, Integer>>> dict;

        private SampleArgs(Output<List<Either<String, Integer>>> list, Output<Map<String, Either<String, Integer>>> dict) {
            this.list = list;
            this.dict = dict;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {
            private final Output.ListBuilder<Either<String, Integer>> list = Output.listBuilder();
            private final Output.MapBuilder<Either<String, Integer>> dict = Output.mapBuilder();

            @CanIgnoreReturnValue
            public Builder add(List<Either<String, Integer>> list) {
                this.list.addAll(list);
                return this;
            }

            @CanIgnoreReturnValue
            public Builder add(Map<String, Either<String, Integer>> map) {
                this.dict.putAll(map);
                return this;
            }

            public SampleArgs build() {
                return new SampleArgs(this.list.build(), this.dict.build());
            }
        }
    }

    @Test
    void testNullableSecretifyOutput() {
        Output<String> res0_ = Output.ofNullable((String) null);
        Output<String> res0 = res0_.asSecret();
        var data0 = extractOutputData(res0);
        assertThat(data0.getValueNullable()).isEqualTo(null);
        assertThat(data0.isSecret()).isTrue();
        assertThat(data0.isKnown()).isTrue();

        // stringify should not modify the original Output
        var data0_ = extractOutputData(res0_);
        assertThat(data0_.isSecret()).isFalse();

        Output<String> res1 = Output.of("test1").asSecret();
        var data1 = extractOutputData(res1);
        assertThat(data1.getValueNullable()).isEqualTo("test1");
        assertThat(data1.isSecret()).isTrue();
        assertThat(data1.isKnown()).isTrue();
    }

    /**
     * If the user creates nulls through APIs we do not maintain that is fair game to throw NPE
     */
    @Test
    void testExpectedNPEs() {

        assertThatThrownBy(() ->
                extractOutputData(
                        Output.<Integer>ofNullable(null)
                                .applyValue(x -> x + 1)
                )
        ).isInstanceOf(CompletionException.class).hasCauseInstanceOf(NullPointerException.class);

        assertThatThrownBy(() ->
                extractOutputData(
                        Output.<Integer>of(ContextAwareCompletableFuture.supplyAsync(() -> null))
                                .applyValue(x -> x + 1)
                )
        ).isInstanceOf(CompletionException.class).hasCauseInstanceOf(NullPointerException.class);

        assertThatThrownBy(() ->
                extractOutputData(
                        Output.of(1)
                                .apply(__ -> Output.<Integer>ofNullable(null))
                                .applyValue(x -> x + 1)
                )
        ).isInstanceOf(CompletionException.class).hasCauseInstanceOf(NullPointerException.class);

        assertThatThrownBy(() ->
                extractOutputData(
                        Output.of(1)
                                .apply(__ -> Output.of(ContextAwareCompletableFuture.<Integer>supplyAsync(() -> null)))
                                .applyValue(x -> x + 1)
                )
        ).isInstanceOf(CompletionException.class).hasCauseInstanceOf(NullPointerException.class);
    }
}
