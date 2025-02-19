package generated_program;

import com.pulumi.Context;
import com.pulumi.Pulumi;
import com.pulumi.core.Output;
import com.pulumi.simple.Resource;
import com.pulumi.simple.ResourceArgs;
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
        var class_ = new Resource("class", ResourceArgs.builder()
            .value(true)
            .build());

        ctx.export("class", class_);
        var export = new Resource("export", ResourceArgs.builder()
            .value(true)
            .build());

        ctx.export("export", export);
        var mod = new Resource("mod", ResourceArgs.builder()
            .value(true)
            .build());

        ctx.export("mod", mod);
        var import_ = new Resource("import", ResourceArgs.builder()
            .value(true)
            .build());

        // TODO(pulumi/pulumi#18246): Pcl should support scoping based on resource type just like HCL does in TF so we can uncomment this.
        // output "import" {
        //   value = Resource["import"]
        // }
        var object = new Resource("object", ResourceArgs.builder()
            .value(true)
            .build());

        ctx.export("object", object);
        var self = new Resource("self", ResourceArgs.builder()
            .value(true)
            .build());

        ctx.export("self", self);
        var this_ = new Resource("this", ResourceArgs.builder()
            .value(true)
            .build());

        ctx.export("this", this_);
        var if_ = new Resource("if", ResourceArgs.builder()
            .value(true)
            .build());

        ctx.export("if", if_);
    }
}
