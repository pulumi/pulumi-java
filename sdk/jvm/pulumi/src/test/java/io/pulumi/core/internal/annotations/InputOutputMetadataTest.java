package io.pulumi.core.internal.annotations;

import io.pulumi.core.*;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InputOutputMetadataTest {

    private static class Tester {

        @OutputExport(name = "complex1", type = Either.class, parameters = {Integer.class, String.class})
        private final Output<Either<Integer, String>> complex1 = Output.of(Either.leftOf(1));

        @OutputExport(name = "complex2", type = Either.class, parameters = {Integer.class, String.class})
        private final Output<Either<Integer, String>> complex2 = Output.of(Either.rightOf("1"));

        @OutputExport(name = "foo", type = String.class)
        private final Output<String> explicitFoo = Output.of("");

        @OutputExport(type = String.class)
        private final Output<String> implicitFoo = Output.of("");

        @InputImport(name = "bar")
        private final Input<String> explicitBar = Input.of("");

        @InputImport
        private final Input<String> implicitBar = Input.of("");

        @InputImport(name = "", required = true, json = true)
        public final InputMap<Integer> inputMap = InputMap.of("k1", 1, "k2", 2);

        @InputImport
        public final InputList<Boolean> inputList = InputList.of(true, false);
    }

    @Test
    void testInputInfos() {
        var infos = InputMetadata.of(Tester.class);
        assertThat(infos).hasSize(4);
    }

    @Test
    void testOutputInfos() {
        var infos = OutputMetadata.of(Tester.class);
        assertThat(infos).hasSize(4);
        System.out.println(infos);
    }

}