package minimal;

import io.pulumi.Config;
import io.pulumi.Stack;

public final class MyStack extends Stack {
    public MyStack() {
        var config = Config.of();
        var name = config.require("name");
        var secret = config.require("secret");
        System.out.println(String.format("Hello, %s!", name));
        System.out.println(String.format("Psst, %s", secret));
    }
}
