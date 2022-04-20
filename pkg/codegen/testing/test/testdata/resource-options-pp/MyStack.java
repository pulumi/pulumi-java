package generated_program;

import java.util.*;
import java.io.*;
import java.nio.*;
import com.pulumi.*;

public class App {
    public static void main(String[] args) {
        int exitCode = Pulumi.run(App::stack);
        System.exit(exitCode);
    }

    public static Exports stack(Context ctx) {
        var provider = new Provider("provider", ProviderArgs.builder()        
            .region("us-west-2")
            .build());

        var bucket1 = new Bucket("bucket1");

        return ctx.exports();
    }
}
