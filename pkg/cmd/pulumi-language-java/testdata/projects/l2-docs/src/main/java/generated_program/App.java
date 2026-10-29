package generated_program;

import com.pulumi.Context;
import com.pulumi.Pulumi;
import com.pulumi.core.Output;
import com.pulumi.enum_.Res;
import com.pulumi.enum_.ResArgs;
import com.pulumi.docs.Resource;
import com.pulumi.docs.ResourceArgs;
import com.pulumi.docs.DocsFunctions;
import com.pulumi.docs.inputs.FunArgs;
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
        var enumRes = new Res("enumRes", ResArgs.builder()
            .intEnum(com.pulumi.enum_.enums.IntEnum.IntOne)
            .stringEnum(com.pulumi.enum_.enums.StringEnum.StringOne)
            .build());

        var res = new Resource("res", ResourceArgs.builder()
            .in(DocsFunctions.fun(FunArgs.builder()
                .in(false)
                .build()).applyValue(_invoke -> _invoke.out()))
            .externalEnum(com.pulumi.enum_.enums.StringEnum.StringOne)
            .build());

    }
}
