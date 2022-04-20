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
        var dbCluster = new Cluster("dbCluster", ClusterArgs.builder()        
            .masterPassword(Output.ofSecret("foobar"))
            .build());

        return ctx.exports();
    }
}
