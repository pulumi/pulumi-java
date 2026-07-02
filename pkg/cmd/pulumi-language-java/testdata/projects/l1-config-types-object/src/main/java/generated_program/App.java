package generated_program;

import com.pulumi.Context;
import com.pulumi.Pulumi;
import com.pulumi.core.Output;
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
    }

    public static class AnObjectConfig {
        private java.util.List<Boolean> prop;
        public java.util.List<Boolean> prop() { return prop; }
    }
}
