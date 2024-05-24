package generated_program;

import com.pulumi.Context;
import com.pulumi.Pulumi;
import com.pulumi.alpha.Resource;
import com.pulumi.alpha.ResourceArgs;

public class App {
    public static void main(String[] args) {
        Pulumi.run(App::stack);
    }

    public static void stack(Context ctx) {
        final var res = new Resource("res", ResourceArgs.builder().value(true).build());
    }
}
