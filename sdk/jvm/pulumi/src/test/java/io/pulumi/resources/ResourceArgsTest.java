package io.pulumi.resources;

import io.pulumi.core.Input;
import io.pulumi.core.InputList;
import io.pulumi.core.InputMap;
import io.pulumi.core.internal.annotations.InputImport;
import org.junit.jupiter.api.Test;

import javax.annotation.Nullable;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ResourceArgsTest {
    public static class ComplexResourceArgs1 extends ResourceArgs {
        @InputImport
        @Nullable
        public Input<String> s = null;

        @InputImport(name = "array")
        @Nullable
        private InputList<Boolean> array = null;

        public InputList<Boolean> getArray() {
            return this.array == null ? InputList.of() : this.array;
        }

        public void setArray(@Nullable InputList<Boolean> value) {
            this.array = value;
        }
    }

    @Test
    void testComplexResourceArgs1_nullValues() {
        var args = new ComplexResourceArgs1();
        var map = args.internalTypedOptionalToMapAsync().join();

        assertThat(map).containsKey("s");
        assertThat(map).containsKey("array");

        assertThat(map).containsEntry("s", Optional.empty());
        assertThat(map).containsEntry("array", Optional.empty());
    }

    public static class JsonResourceArgs1 extends ResourceArgs {
        @InputImport(name = "array", json = true)
        @Nullable
        private InputList<Boolean> array = null;

        @InputImport(name = "map", json = true)
        @Nullable
        private InputMap<Integer> map = null;

        public InputList<Boolean> getArray() {
            return this.array == null ? InputList.of() : this.array;
        }

        public void setArray(@Nullable InputList<Boolean> value) {
            this.array = value;
        }

        public InputMap<Integer> getMap() {
            return this.map == null ? InputMap.of() : this.map;
        }

        public void setMap(@Nullable InputMap<Integer> value) {
            this.map = value;
        }
    }

    @Test
    void testJsonMap() {
        var args = new JsonResourceArgs1();
        args.setArray(InputList.of(true, false));
        args.setMap(InputMap.of("k1", 1, "k2", 2));
        var map = args.internalTypedOptionalToMapAsync().join();

        assertThat(map).containsKey("array");
        assertThat(map).containsKey("map");

        assertThat(map.get("array")).isPresent().contains("[true,false]");
        assertThat(map.get("map")).isPresent().contains("{\"k1\":1.0,\"k2\":2.0}");
    }
}