package io.pulumi.resources;

import io.pulumi.core.Input;
import io.pulumi.core.internal.annotations.InputImport;
import org.junit.jupiter.api.Test;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ResourceArgsTest {
    public static class ComplexResourceArgs1 extends ResourceArgs {
        @InputImport
        @Nullable
        public Input<String> s = null;

        @InputImport(name = "array")
        @Nullable
        private Input<List<Boolean>> array = null;

        public Input<List<Boolean>> getArray() {
            return this.array == null ? Input.ofList() : this.array;
        }

        public void setArray(@Nullable Input<List<Boolean>> value) {
            this.array = value;
        }
    }

    @Test
    void testComplexResourceArgs1_emptyValues() {
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
        private Input<List<Boolean>> array = null;

        @InputImport(name = "map", json = true)
        @Nullable
        private Input<Map<String, Integer>> map = null;

        public Input<List<Boolean>> getArray() {
            return this.array == null ? Input.ofList() : this.array;
        }

        public void setArray(@Nullable Input<List<Boolean>> value) {
            this.array = value;
        }

        public Input<Map<String, Integer>> getMap() {
            return this.map == null ? Input.ofMap() : this.map;
        }

        public void setMap(@Nullable Input<Map<String, Integer>> value) {
            this.map = value;
        }
    }

    @Test
    void testJsonMap() {
        var args = new JsonResourceArgs1();
        args.setArray(Input.ofList(true, false));
        args.setMap(Input.ofMap("k1", 1, "k2", 2));
        var map = args.internalTypedOptionalToMapAsync().join();

        assertThat(map).containsKey("array");
        assertThat(map).containsKey("map");

        assertThat(map.get("array")).isPresent().contains("[true,false]");
        assertThat(map.get("map")).isPresent().contains("{\"k1\":1.0,\"k2\":2.0}");
    }
}