package minimal;

import io.pulumi.deployment.Deployment;

public class App {
    public static void main(String[] args) {
        Integer exitCode = Deployment.runAsyncStack(MyStack.class).join();
        System.exit(exitCode);
    }
}
