package myproject;

import io.pulumi.Stack;
import io.pulumi.core.Output;
import io.pulumi.core.annotations.OutputExport;

import java.util.List;
import java.util.Map;

public final class MyStack extends Stack {

    @OutputExport(type = String.class)
    private final Output<String> myOutput;

    public MyStack() {
        this.myOutput = Output.of("hello, world");
    }
}
