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
        final var aNumber = config.requireDouble("aNumber");
        ctx.export("theNumber", aNumber + 1.25);
        final var optionalNumber = config.getDouble("optionalNumber").orElse(41.5);
        ctx.export("defaultNumber", optionalNumber + 1.2);
        final var anInt = config.requireInteger("anInt");
        ctx.export("theInteger", anInt + 4);
        final var optionalInt = config.getInteger("optionalInt").orElse(1);
        ctx.export("defaultInteger", optionalInt + 2);
        final var aString = config.require("aString");
        ctx.export("theString", String.format("%s World", aString));
        final var optionalString = config.get("optionalString").orElse("defaultStringValue");
        ctx.export("defaultString", optionalString);
        final var aBool = config.requireBoolean("aBool");
        ctx.export("theBool", !aBool && true);
        final var optionalBool = config.getBoolean("optionalBool").orElse(false);
        ctx.export("defaultBool", optionalBool);
    }
}
