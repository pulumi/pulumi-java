package io.pulumi.core;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.pulumi.core.internal.TypedInputOutput;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

class InputsTest {

    @Test
    void testMapConcat() {
        final var outV2 = Output.of("V2");
        final var outV3 = Output.of("V3");
        final var outV3wrong = Output.of("V3_wrong");
        final InputMap<String> map1 = InputMap.<String>builder()
                .put("K1", "V1")
                .put("K2", outV2)
                .put("K3", outV3wrong)
                .build();

        final var map2 = InputMap.<String>builder()
                .put("K3", outV3)
                .put("K4", "V4")
                .build();

        var result = map1.concat(map2);
        var data = InputOutputTests.waitFor(result);

        assertThat(data.isEmpty()).isFalse();
        assertThat(data.isSecret()).isFalse();
        assertThat(data.isKnown()).isTrue();
        assertThat(data.getValueNullable()).hasSize(4);
        for (var i = 1; i <= 4; i++) {
            assertThat(data.getValueNullable()).containsEntry("K" + i, "V" + i);
        }

        // Check that the input maps haven't changed
        InputOutputTests.waitFor(
                Output.tuple(map1, outV3wrong.toInput()).applyVoid(t -> {
                    assertThat(t.t1).hasSize(3);
                    assertThat(t.t1).contains(entry("K3", t.t2));
                }),
                Output.tuple(map2, outV3.toInput()).applyVoid(t -> {
                    assertThat(t.t1).hasSize(2);
                    assertThat(t.t1).contains(entry("K3", t.t2));
                })
        );
    }

    @Test
    void testInputMapEitherInitializer() {
        var sample = SampleArgs.builder()
                .add(Map.of(
                        "left", Either.leftOf("testValue"),
                        "right", Either.rightOf(123)
                ))
                .build();

        var data = InputOutputTests.waitFor(sample.dict);
        assertThat(data.getValueNullable()).hasSize(2);
        assertThat(data.getValueNullable()).containsValue(Either.leftOf("testValue"));
        assertThat(data.getValueNullable()).containsValue(Either.rightOf(123));
    }

    @Test
    void testInputListEitherInitializer() {
        var sample = SampleArgs.builder()
                .add(List.of(
                        Either.leftOf("testValue"),
                        Either.rightOf(123)
                ))
                .build();
        var data = InputOutputTests.waitFor(sample.list);
        assertThat(data.getValueNullable()).hasSize(2);
        assertThat(data.getValueNullable()).containsOnlyOnce(Either.leftOf("testValue"));
        assertThat(data.getValueNullable()).containsOnlyOnce(Either.rightOf(123));
    }

    private static final class SampleArgs {

        public final InputList<Either<String, Integer>> list;
        public final InputMap<Either<String, Integer>> dict;

        private SampleArgs(InputList<Either<String, Integer>> list, InputMap<Either<String, Integer>> dict) {
            this.list = list;
            this.dict = dict;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {
            private final InputList.Builder<Either<String, Integer>> list = InputList.builder();
            private final InputMap.Builder<Either<String, Integer>> dict = InputMap.builder();

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
}