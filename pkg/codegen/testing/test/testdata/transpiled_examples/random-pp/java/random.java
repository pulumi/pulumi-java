package generated_program;

import com.pulumi.Context;
import com.pulumi.Pulumi;
import com.pulumi.core.Output;
import com.pulumi.random.RandomPassword;
import com.pulumi.random.RandomPasswordArgs;
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
        var randomPassword = new RandomPassword("randomPassword", RandomPasswordArgs.builder()        
            .length(16)
            .special(true)
            .overrideSpecial("_%@")
            .build());

        ctx.export("password", randomPassword.result());
    }
}
