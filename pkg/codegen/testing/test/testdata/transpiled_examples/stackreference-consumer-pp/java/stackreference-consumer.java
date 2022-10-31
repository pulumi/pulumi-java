package generated_program;

import com.pulumi.Context;
import com.pulumi.Pulumi;
import com.pulumi.core.Output;
import com.pulumi.pulumi.pulumi.StackReference;
import com.pulumi.pulumi.pulumi.StackReferenceArgs;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

public class App {
    public static void main(String[] args) {
        Pulumi.run(App::stack);
    }

    public static void stack(Context ctx) {
        var stackRef = new StackReference("stackRef", StackReferenceArgs.builder()        
            .name("PLACEHOLDER_ORG_NAME/stackreference-producer/PLACEHOLDER_STACK_NAME")
            .build());

        ctx.export("referencedImageName", stackRef.outputs().applyValue(outputs -> outputs.imageName()));
    }
}
