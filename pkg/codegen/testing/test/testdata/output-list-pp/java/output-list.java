package generated_program;

import com.pulumi.Context;
import com.pulumi.Pulumi;
import com.pulumi.core.Output;
import com.pulumi.random.RandomPet;
import com.pulumi.random.RandomShuffle;
import com.pulumi.random.RandomShuffleArgs;
import com.pulumi.infra.Bucket;
import com.pulumi.infra.InfraFunctions;
import com.pulumi.infra.inputs.GetPolicyDocumentArgs;
import com.pulumi.infra.inputs.GetPolicyDocumentStatementArgs;
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
        var pet = new RandomPet("pet");

        // A list whose elements are all plain values still generates plain varargs.
        var plainList = new RandomShuffle("plainList", RandomShuffleArgs.builder()
            .inputs(            
                "a",
                "b")
            .build());

        // A list mixing outputs and plain values has no matching builder overload as varargs,
        // so it has to be combined with Output.all().
        var mixedList = new RandomShuffle("mixedList", RandomShuffleArgs.builder()
            .inputs(Output.all(            
                pet.id(),
                pet.id().applyValue(_id -> String.format("%s-suffix", _id)),
                Output.of("literal")))
            .build());

        // A single-element list is generated the same way: the element is still an output.
        var singleOutput = new RandomShuffle("singleOutput", RandomShuffleArgs.builder()
            .inputs(Output.all(pet.id()))
            .build());

        // The same applies to a list nested inside invoke arguments, which is what makes the
        // S3 bucket policy example in the registry docs fail to compile.
        var sourceBucket = new Bucket("sourceBucket");

        final var policyDocument = InfraFunctions.getPolicyDocument(GetPolicyDocumentArgs.builder()
            .statements(GetPolicyDocumentStatementArgs.builder()
                .actions("s3:GetObject")
                .resources(Output.all(                
                    sourceBucket.bucket(),
                    sourceBucket.bucket().applyValue(_bucket -> String.format("%s/*", _bucket))))
                .build())
            .build());

    }
}
