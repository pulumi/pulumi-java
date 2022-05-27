//DEPS com.pulumi:pulumi:${env.PULUMI_JAVA_SDK_VERSION:0.1.0}

import com.pulumi.Pulumi;

public class main {
    public static void main(String[] args) {
        Pulumi.run(ctx -> {
            var log = ctx.log();
            var config = ctx.config();
            var name = config.require("name");
            var secret = config.require("secret");
            log.info("Hello, %s!%n", name);
            log.info("Psst, %s%n", secret);
        });
    }
}
