package generated_program;

import com.pulumi.Context;
import com.pulumi.Pulumi;
import com.pulumi.core.Output;
import com.pulumi.enum_.Res;
import com.pulumi.enum_.ResArgs;
import com.pulumi.extenumref.Sink;
import com.pulumi.extenumref.SinkArgs;
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
        var myRes = new Res("myRes", ResArgs.builder()
            .intEnum(com.pulumi.enum_.enums.IntEnum.IntOne)
            .stringEnum(com.pulumi.enum_.enums.StringEnum.StringOne)
            .build());

        var mySink = new Sink("mySink", SinkArgs.builder()
            .stringEnum(com.pulumi.enum_.enums.StringEnum.StringTwo)
            .build());

    }
}
