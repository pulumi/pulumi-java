package s3site;

import io.pulumi.Pulumi;

public class App {
    public static void main(String[] args) {
        int exitCode = Pulumi.runStack(MyStack::new);
        System.exit(exitCode);
    }
}
