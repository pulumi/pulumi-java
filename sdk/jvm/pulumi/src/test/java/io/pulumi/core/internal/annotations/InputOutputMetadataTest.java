package io.pulumi.core.internal.annotations;

import io.pulumi.core.Either;
import io.pulumi.core.Input;
import io.pulumi.core.Output;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class InputOutputMetadataTest {

    private static class Tester {

        @OutputExport(name = "complex1", type = Either.class, parameters = {Integer.class, String.class})
        private final Output<Either<Integer, String>> complex1 = Output.of(Either.ofLeft(1));

        @OutputExport(name = "complex2", type = Either.class, parameters = {Integer.class, String.class})
        private final Output<Either<Integer, String>> complex2 = Output.of(Either.ofRight("1"));

        @OutputExport(name = "foo", type = String.class)
        private final Output<String> explicitFoo = Output.of("");

        @OutputExport(type = String.class)
        private final Output<String> implicitFoo = Output.of("");

        @OutputExport(type = Map.class, parameters = {String.class, Integer.class})
        private final Output<Map<String, Integer>> implicitBaz = Output.of(Map.of());

        @InputImport(name = "bar")
        private final Input<String> explicitBar = Input.of("");

        @InputImport
        private final Input<String> implicitBar = Input.of("");

        @InputImport(name = "", required = true, json = true)
        public final Input<Map<String, Integer>> inputMap = Input.ofMap("k1", 1, "k2", 2);

        @InputImport
        public final Input<List<Boolean>> inputList = Input.ofList(true, false);
    }

    @Test
    void testInputInfos() {
        var infos = InputMetadata.of(Tester.class);
        assertThat(infos).hasSize(4);
    }

    @Test
    void testOutputInfos() {
        var infos = OutputMetadata.of(Tester.class);
        assertThat(infos).hasSize(5);
    }

}