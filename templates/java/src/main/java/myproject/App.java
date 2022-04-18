package myproject;

import com.pulumi.Pulumi;
import com.pulumi.core.Output;

public class App {
    public static void main(String[] args) {
        int exitCode = Pulumi.run(ctx -> {
            ctx.export("exampleOutput", Output.of("example"));
            return ctx.exports();
        });
        System.exit(exitCode);
    }
}
