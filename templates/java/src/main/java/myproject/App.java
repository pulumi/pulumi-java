package myproject;

import io.pulumi.Pulumi;
import io.pulumi.core.Output;

public class App {
    public static void main(String[] args) {
        int exitCode = Pulumi.run(ctx -> {
            ctx.export("exampleOutput", Output.of("example"));
            return ctx.exports();
        });
        System.exit(exitCode);
    }
}
