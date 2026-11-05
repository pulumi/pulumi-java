package generated_program;

import com.pulumi.Context;
import com.pulumi.Pulumi;
import com.pulumi.core.Output;
import com.pulumi.enumlist.EnumlistFunctions;
import com.pulumi.enumlist.inputs.MixArgs;
import com.pulumi.enumlist.Palette;
import com.pulumi.enumlist.PaletteArgs;
import com.pulumi.enumlist.inputs.SwatchArgs;
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
        final var mixed = EnumlistFunctions.mix(MixArgs.builder()
            .colors(            
                com.pulumi.enumlist.enums.Color.Red,
                com.pulumi.enumlist.enums.Color.Green)
            .build());

        var palette = new Palette("palette", PaletteArgs.builder()
            .primary(com.pulumi.enumlist.enums.Color.Red)
            .colors(            
                com.pulumi.enumlist.enums.Color.Red,
                com.pulumi.enumlist.enums.Color.Green)
            .swatches(SwatchArgs.builder()
                .colors(com.pulumi.enumlist.enums.Color.Green)
                .build())
            .build());

        ctx.export("result", mixed.applyValue(_mixed -> _mixed.result()));
    }
}
