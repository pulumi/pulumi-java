package generated_program;

import java.util.*;
import java.io.*;
import java.nio.*;
import com.pulumi.*;

public class App {
    public static void main(String[] args) {
        Pulumi.run(App::stack);
    }

    public static void stack(Context ctx) {
        var dbCluster = new Cluster("dbCluster", ClusterArgs.builder()        
            .masterPassword(Output.ofSecret("foobar"))
            .build());

    }
}
