package generated_program;

import com.pulumi.Context;
import com.pulumi.Pulumi;
import com.pulumi.core.Output;
import com.pulumi.nestedcollections.Foo;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

public class App {
    public static void main(String[] args) {
        Pulumi.run(App::stack);
    }

    public static void stack(Context ctx) {
        // A resource with deeply nested collection output properties: a list of lists of lists
        // of an object type and a map of maps of maps of strings.
        var foo = new Foo("foo");

        ctx.export("secondProp", foo.conditionSets().applyValue(_conditionSets -> _conditionSets.get(0).get(0).get(1).prop()));
        ctx.export("leaf", foo.privateEndpoint().applyValue(_privateEndpoint -> _privateEndpoint.get("outer").get("inner").get("leaf")));
    }
}
