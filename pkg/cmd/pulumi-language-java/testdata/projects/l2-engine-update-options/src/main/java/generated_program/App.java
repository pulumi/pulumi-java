package generated_program;

import com.pulumi.Context;
import com.pulumi.Pulumi;
import com.pulumi.simple.Resource;
import com.pulumi.simple.ResourceArgs;

public class App {
    public static void main(String[] args) {
        Pulumi.run(App::stack);
    }

    public static void stack(Context ctx) {
        final var target = new Resource("target", ResourceArgs.builder().value(true).build());

        final var other = new Resource("other", ResourceArgs.builder().value(true).build());
    }
}
