package generated_program;

import com.pulumi.Context;
import com.pulumi.Pulumi;
import com.pulumi.core.Output;
import static com.pulumi.codegen.internal.Serialization.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

public class App {
    public static void main(String[] args) {
        Pulumi.run(App::stack);
    }

    public static void stack(Context ctx) {
        final var config = ctx.config();
        final var aMap = config.requireObject("aMap", com.pulumi.core.TypeShape.map(String.class, Integer.class));
        ctx.export("theMap", Map.ofEntries(
            Map.entry("a", aMap.get("a") + 1),
            Map.entry("b", aMap.get("b") + 1)
        ));
        final var anObject = config.requireObject("anObject", AnObjectConfig.class);
        ctx.export("theObject", anObject.prop().get(0));
        final var anyObject = config.requireObject("anyObject", com.pulumi.core.TypeShape.map(String.class, Object.class));
        ctx.export("theThing", ((Number) anyObject.get("a")).doubleValue() + ((Number) anyObject.get("b")).doubleValue());
        final var optionalUntypedObject = config.getObject("optionalUntypedObject", com.pulumi.core.TypeShape.map(String.class, Object.class)).orElse(Map.of("key", "value"));
        ctx.export("defaultUntypedObject", optionalUntypedObject);
        final var optionalList = config.getObject("optionalList", com.pulumi.core.TypeShape.list(String.class)).orElse(null);
        final var optionalMap = config.getObject("optionalMap", com.pulumi.core.TypeShape.map(String.class, String.class)).orElse(null);
        final var optionalObject = config.getObject("optionalObject", OptionalObjectConfig.class).orElse(null);
        ctx.export("optionalList", optionalList == null ? "null" : serializeJson(
            optionalList));
        ctx.export("optionalMap", optionalMap == null ? "null" : serializeJson(
            optionalMap));
        ctx.export("optionalObject", optionalObject == null ? "null" : serializeJson(
            optionalObject));
    }

    public static class AnObjectConfig {
        private java.util.List<Boolean> prop;
        public java.util.List<Boolean> prop() { return prop; }
    }

    public static class OptionalObjectConfig {
        private Integer other;
        public Integer other() { return other; }
        private String prop;
        public String prop() { return prop; }
    }
}
