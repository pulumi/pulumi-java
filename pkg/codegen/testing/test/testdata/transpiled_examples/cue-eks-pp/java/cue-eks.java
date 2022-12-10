package generated_program;

import com.pulumi.Context;
import com.pulumi.Pulumi;
import com.pulumi.core.Output;
import com.pulumi.eks.Cluster;
import com.pulumi.eks.ClusterArgs;
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
        var rawkode = new Cluster("rawkode", ClusterArgs.builder()        
            .instanceType("t2.medium")
            .desiredCapacity(2)
            .minSize(1)
            .maxSize(2)
            .build());

        var stack72 = new Cluster("stack72", ClusterArgs.builder()        
            .instanceType("t2.medium")
            .desiredCapacity(4)
            .minSize(1)
            .maxSize(8)
            .build());

    }
}
