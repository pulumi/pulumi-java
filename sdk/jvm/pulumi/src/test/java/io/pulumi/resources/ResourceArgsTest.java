package io.pulumi.resources;

import io.pulumi.Log;
import io.pulumi.core.Output;
import io.pulumi.core.annotations.InputImport;
import io.pulumi.core.internal.Internal;
import io.pulumi.deployment.internal.DeploymentTests;
import org.junit.jupiter.api.Test;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static io.pulumi.core.OutputTests.waitFor;
import static org.assertj.core.api.Assertions.assertThat;

class ResourceArgsTest {

    private final static Log log = DeploymentTests.mockLog();

    public static class ComplexResourceArgs1 extends ResourceArgs {
        @InputImport
        @Nullable
        public Output<String> s = null;

        @InputImport(name = "array")
        @Nullable
        private Output<List<Boolean>> array = null;

        public Output<List<Boolean>> getArray() {
            return this.array == null ? Output.ofList() : this.array;
        }

        public void setArray(@Nullable Output<List<Boolean>> value) {
            this.array = value;
        }
    }

    @Test
    void testComplexResourceArgs1_emptyValues() {
        var args = new ComplexResourceArgs1();
        var map = Internal.from(args).toOptionalMapAsync(log).join();

        assertThat(map).containsKey("s");
        assertThat(map).containsKey("array");

        assertThat(map).containsEntry("s", Optional.empty());
        assertThat(map).containsEntry("array", Optional.empty());
    }

    @Test
    void testComplexResourceArgs1_simpleValues() {
        var args = new ComplexResourceArgs1();
        args.s = Output.of("s");
        args.array = Output.of(List.of(true, false));
        var map = Internal.from(args).toOptionalMapAsync(log).join();

        assertThat(map).containsKey("s");
        assertThat(map).containsKey("array");

        var s = map.get("s").get();
        assertThat(s).isInstanceOf(Output.class);
        assertThat(waitFor((Output) s)).isEqualTo(waitFor(Output.of("s")));

        var array = map.get("array").get();
        assertThat(array).isInstanceOf(Output.class);
        assertThat(waitFor((Output) array)).isEqualTo(waitFor(Output.of(List.of(true, false))));
    }

    public static class JsonResourceArgs1 extends ResourceArgs {
        @InputImport(name = "array", json = true)
        @Nullable
        private Output<List<Boolean>> array = null;

        @InputImport(name = "map", json = true)
        @Nullable
        private Output<Map<String, Integer>> map = null;

        public Output<List<Boolean>> getArray() {
            return this.array == null ? Output.ofList() : this.array;
        }

        public void setArray(@Nullable Output<List<Boolean>> value) {
            this.array = value;
        }

        public Output<Map<String, Integer>> getMap() {
            return this.map == null ? Output.ofMap() : this.map;
        }

        public void setMap(@Nullable Output<Map<String, Integer>> value) {
            this.map = value;
        }
    }

    @Test
    void testJsonMap() {
        var args = new JsonResourceArgs1();
        args.setArray(Output.ofList(true, false));
        args.setMap(Output.ofMap("k1", 1, "k2", 2));
        var map = Internal.from(args).toOptionalMapAsync(log).join();

        assertThat(map).containsKey("array");
        assertThat(map).containsKey("map");

        assertThat(map.get("array")).isPresent().contains("[true,false]");
        assertThat(map.get("map")).isPresent().contains("{\"k1\":1.0,\"k2\":2.0}");
    }
}