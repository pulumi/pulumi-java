package com.pulumi.example.provider;

import com.pulumi.core.annotations.Import;
import com.pulumi.core.annotations.Export;
import com.pulumi.core.Output;
import com.pulumi.random.RandomString;
import com.pulumi.random.RandomStringArgs;
import com.pulumi.resources.ComponentResource;
import com.pulumi.resources.ComponentResourceOptions;
import com.pulumi.resources.CustomResourceOptions;
import com.pulumi.resources.ResourceArgs;
import com.pulumi.std.inputs.AbsArgs;
import com.pulumi.std.StdFunctions;
import com.pulumi.std.inputs.AbsPlainArgs;
import java.util.concurrent.CompletableFuture;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;

class SampleAsyncArgs extends ResourceArgs {
    @Import(name="length", required=true)
    private Output<Integer> length;

    public Output<Integer> length() {
        return this.length;
    }

    private SampleAsyncArgs() {}

    public SampleAsyncArgs(Output<Integer> length) {
        this.length = length;
    }
}

// This component is a test case for a number of async operations to make sure they work
// correctly for our context-aware completable future implementation.
class SampleAsync extends ComponentResource {
    @Export(name="value", refs={String.class}, tree="[0]")
    public final Output<String> value;

    public SampleAsync(String name, SampleAsyncArgs args, ComponentResourceOptions opts) {
        super("javap:index:SampleAsync", name, null, opts);

        var resOpts = CustomResourceOptions.builder()
            .parent(this)
            .build();

        // First async operation for length
        var asyncLength = Output.of(CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(2000);
                return -5.0;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Async operation interrupted", e);
            }
        }));

        // Invoke a remote function.
        var absLength = StdFunctions.abs(AbsArgs.builder().input(asyncLength).build());
        var absLengthInt = absLength.applyValue(d -> d.result().intValue());

        // Invoke a remote function with a plain value.
        var absPlainLength = StdFunctions.absPlain(AbsPlainArgs.builder().input(-1.0).build());
        var absPlainLengthInt = Output.of(absPlainLength).applyValue(d -> d.result().intValue());

        var totalLength = Output.tuple(args.length(), absLengthInt, absPlainLengthInt).applyValue(values -> values.t1 + values.t2 + values.t3);

        // Create the random string with modified length.
        var randomString = new RandomString(name + "1", 
            RandomStringArgs.builder()
                .length(totalLength)
                .special(false)
                .build(), resOpts);


        // Make HTTP request based on the input length
        this.value = Output.tuple(randomString.result(), totalLength)
            .applyValue(values -> {
                String randomStr = values.t1;
                Integer todoId = (values.t2 % 10) + 1;

                var randomString2 = new RandomString(name + "2", 
                    RandomStringArgs.builder()
                        .length(totalLength)
                        .special(false)
                        .build(), resOpts);
                
                try {
                    HttpClient client = HttpClient.newHttpClient();
                    HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://jsonplaceholder.typicode.com/todos/" + todoId))
                        .build();
                    
                    HttpResponse<String> response = client.send(request, 
                        HttpResponse.BodyHandlers.ofString());
                    return randomStr + " - Todo #" + todoId + ": " + response.body();
                } catch (Exception e) {
                    throw new RuntimeException("HTTP request failed", e);
                }
            });
    }
}
