package sqs;

import io.pulumi.Stack;

import io.pulumi.aws.sqs.*;
import io.pulumi.core.Output;
import io.pulumi.core.annotations.Export;

public final class MyStack extends Stack {

    @Export(type = String.class)
    private final Output<String> queueUrl;

    public MyStack() {
        final var queue = new Queue("foo", QueueArgs.builder().build());
        this.queueUrl = queue.getUrl();
    }

}
