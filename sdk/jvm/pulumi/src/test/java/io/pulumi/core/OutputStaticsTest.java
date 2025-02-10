package io.pulumi.core;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.pulumi.core.internal.OutputBuilder;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

public class OutputStaticsTest {
    private static final OutputBuilder output = OutputTests.testContext().output;

    @Test
    void testListConcatNull() {
        var result = output.concatList(null, null);
        var data = OutputTests.waitFor(result);

        assertThat(data.isSecret()).isFalse();
        assertThat(data.isKnown()).isTrue();
        assertThat(data.getValueNullable()).isNotNull().isEmpty();
    }

    @Test
    void testListConcatEmpty() {
        var result = output.concatList(output.empty(), output.empty());
        var data = OutputTests.waitFor(result);

        assertThat(data.isSecret()).isFalse();
        assertThat(data.isKnown()).isTrue();
        assertThat(data.getValueNullable()).isNotNull().isEmpty();
    }

    @Test
    void testListConcatSimple() {
        final var outV2 = output.of("V2");
        final var outV3 = output.of("V3");
        final var list1 = output.<String>listBuilder()
                .add("V1")
                .add(outV2)
                .build();

        final Output<List<String>> list2 = output.<String>listBuilder()
                .add(outV3)
                .add("V4")
                .build();

        var result = output.concatList(list1, list2);
        var data = OutputTests.waitFor(result);

        assertThat(data.isEmpty()).isFalse();
        assertThat(data.isSecret()).isFalse();
        assertThat(data.isKnown()).isTrue();
        assertThat(data.getValueNullable()).hasSize(4);
        assertThat(data.getValueNullable()).containsOnly(
                "V1",
                OutputTests.waitFor(outV2).getValueNullable(),
                OutputTests.waitFor(outV3).getValueNullable(),
                "V4"
        );

        // Check that the input maps haven't changed
        OutputTests.waitFor(
                output.tuple(list1, outV2).applyVoid(t -> {
                    assertThat(t.t1).hasSize(2);
                    assertThat(t.t1).contains("V1", t.t2);
                }),
                output.tuple(list2, outV3).applyVoid(t -> {
                    assertThat(t.t1).hasSize(2);
                    assertThat(t.t1).containsOnly(t.t2, "V4");
                })
        );
    }

    @Test
    void testMapConcatNull() {
        var result = output.concatMap(null, null);
        var data = OutputTests.waitFor(result);

        assertThat(data.isSecret()).isFalse();
        assertThat(data.isKnown()).isTrue();
        assertThat(data.getValueNullable()).isNotNull().isEmpty();
    }

    @Test
    void testMapConcatEmpty() {
        var result = output.concatMap(output.empty(), output.empty());
        var data = OutputTests.waitFor(result);

        assertThat(data.isEmpty()).isFalse();
        assertThat(data.isSecret()).isFalse();
        assertThat(data.isKnown()).isTrue();
        assertThat(data.getValueNullable()).isNotNull().isEmpty();
    }

    @Test
    void testMapConcatSimple() {
        final var outV2 = output.of("V2");
        final var outV3 = output.of("V3");
        final var outV3wrong = output.of("V3_wrong");
        final var map1 = output.<String>mapBuilder()
                .put("K1", "V1")
                .put("K2", outV2)
                .put("K3", outV3wrong)
                .build();

        final var map2 = output.<String>mapBuilder()
                .put("K3", outV3)
                .put("K4", "V4")
                .build();

        var result = output.concatMap(map1, map2);
        var data = OutputTests.waitFor(result);

        assertThat(data.isEmpty()).isFalse();
        assertThat(data.isSecret()).isFalse();
        assertThat(data.isKnown()).isTrue();
        assertThat(data.getValueNullable()).hasSize(4);
        for (var i = 1; i <= 4; i++) {
            assertThat(data.getValueNullable()).containsEntry("K" + i, "V" + i);
        }

        // Check that the input maps haven't changed
        OutputTests.waitFor(
                output.tuple(map1, outV3wrong).applyVoid(t -> {
                    assertThat(t.t1).hasSize(3);
                    assertThat(t.t1).contains(entry("K3", t.t2));
                }),
                output.tuple(map2, outV3).applyVoid(t -> {
                    assertThat(t.t1).hasSize(2);
                    assertThat(t.t1).contains(entry("K3", t.t2));
                })
        );
    }

    @Test
    void testoutputMapEitherInitializer() {
        var sample = SampleArgs.builder()
                .add(Map.of(
                        "left", Either.ofLeft("testValue"),
                        "right", Either.ofRight(123)
                ))
                .build();

        var data = OutputTests.waitFor(sample.dict);
        assertThat(data.getValueNullable()).hasSize(2);
        assertThat(data.getValueNullable()).containsValue(Either.ofLeft("testValue"));
        assertThat(data.getValueNullable()).containsValue(Either.ofRight(123));
    }

    @Test
    void testoutputListEitherInitializer() {
        var sample = SampleArgs.builder()
                .add(List.of(
                        Either.ofLeft("testValue"),
                        Either.ofRight(123)
                ))
                .build();
        var data = OutputTests.waitFor(sample.list);
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
            private final OutputBuilder.ListBuilder<Either<String, Integer>> list = output.listBuilder();
            private final OutputBuilder.MapBuilder<Either<String, Integer>> dict = output.mapBuilder();

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
    public void testNullableSecretifyoutput() {
        Output<String> res0_ = output.ofNullable((String) null);
        Output<String> res0 = res0_.asSecret();
        var data0 = OutputTests.waitFor(res0);
        assertThat(data0.getValueNullable()).isEqualTo(null);
        assertThat(data0.isSecret()).isTrue();
        assertThat(data0.isPresent()).isFalse();
        assertThat(data0.isKnown()).isTrue();

        // stringify should not modify the original output
        var data0_ = OutputTests.waitFor(res0_);
        assertThat(data0_.isSecret()).isFalse();

        Output<String> res1 = output.ofNullable("test1").asSecret();
        var data1 = OutputTests.waitFor(res1);
        assertThat(data1.getValueNullable()).isEqualTo("test1");
        assertThat(data1.isSecret()).isTrue();
        assertThat(data1.isPresent()).isTrue();
        assertThat(data1.isKnown()).isTrue();

        Output<String> res2 = output.ofNullable(output.of("test2")).asSecret();
        var data2 = OutputTests.waitFor(res2);
        assertThat(data2.getValueNullable()).isEqualTo("test2");
        assertThat(data2.isSecret()).isTrue();
        assertThat(data2.isPresent()).isTrue();
        assertThat(data2.isKnown()).isTrue();
    }

    /**
     * If the user creates nulls through APIs we do not maintain that is fair game to throw NPE
     */
    @Test
    void testExpectedNPEs() {
        assertThatThrownBy(() ->
                OutputTests.waitFor(
                        output.<Integer>empty().applyValue(x -> x + 1)
                )
        ).isInstanceOf(CompletionException.class).hasCauseInstanceOf(NullPointerException.class);

        assertThatThrownBy(() ->
                OutputTests.waitFor(
                        output.<Integer>ofNullable(null)
                                .applyValue(x -> x + 1)
                )
        ).isInstanceOf(CompletionException.class).hasCauseInstanceOf(NullPointerException.class);

        assertThatThrownBy(() ->
                OutputTests.waitFor(
                        output.<Integer>of(CompletableFuture.supplyAsync(() -> null))
                                .applyValue(x -> x + 1)
                )
        ).isInstanceOf(CompletionException.class).hasCauseInstanceOf(NullPointerException.class);

        assertThatThrownBy(() ->
                OutputTests.waitFor(
                        output.of(1)
                                .applyOptional(__ -> Optional.<Integer>ofNullable(null))
                                .applyValue(x -> x + 1)
                )
        ).isInstanceOf(CompletionException.class).hasCauseInstanceOf(NullPointerException.class);

        assertThatThrownBy(() ->
                OutputTests.waitFor(
                        output.of(1)
                                .apply(__ -> output.<Integer>ofNullable(null))
                                .applyValue(x -> x + 1)
                )
        ).isInstanceOf(CompletionException.class).hasCauseInstanceOf(NullPointerException.class);

        assertThatThrownBy(() ->
                OutputTests.waitFor(
                        output.of(1)
                                .applyFuture(__ -> CompletableFuture.<Integer>supplyAsync(() -> null))
                                .applyValue(x -> x + 1)
                )
        ).isInstanceOf(CompletionException.class).hasCauseInstanceOf(NullPointerException.class);
    }
}
