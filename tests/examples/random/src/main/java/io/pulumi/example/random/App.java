package io.pulumi.example.random;

import io.pulumi.Pulumi;
import io.pulumi.context.ExportContext;
import io.pulumi.context.StackContext;
import io.pulumi.core.Output;
import io.pulumi.random.RandomId;
import io.pulumi.random.RandomIdArgs;
import io.pulumi.random.RandomInteger;
import io.pulumi.random.RandomIntegerArgs;
import io.pulumi.random.RandomPassword;
import io.pulumi.random.RandomPasswordArgs;
import io.pulumi.random.RandomPet;
import io.pulumi.random.RandomShuffle;
import io.pulumi.random.RandomShuffleArgs;
import io.pulumi.random.RandomString;
import io.pulumi.random.RandomStringArgs;
import io.pulumi.random.RandomUuid;

import java.util.List;

public class App {

    public static void main(String[] args) {
        int exitCode = Pulumi.run(App::stack);
        System.exit(exitCode);
    }

    private static ExportContext stack(StackContext ctx) {
        var randomPassword = new RandomPassword("my-password",
                RandomPasswordArgs.builder().length(16)
                        .special(true)
                        .overrideSpecial("_@")
                        .build());
        ctx.export("randomPassword", randomPassword.result());

        var randomPet = new RandomPet("my-pet");
        ctx.export("randomPetKeepers", randomPet.keepers());

        var randomInteger = new RandomInteger("my-int",
                RandomIntegerArgs.builder()
                        .max(100)
                        .min(0)
                        .build()
        );
        ctx.export("randomInteger", randomInteger.result());

        var randomString = new RandomString("my-string",
                RandomStringArgs.builder()
                        .length(10)
                        .build()
        ).result();
        ctx.export("randomString", randomString);

        var randomId = new RandomId("my-id",
                RandomIdArgs.builder()
                        .byteLength(10)
                        .build()
        );
        ctx.export("randomIdHex", randomId.hex());

        var randomUuid = new RandomUuid("my-uuid").result();
        ctx.export("randomUuid", randomUuid);

        var randomShuffle = new RandomShuffle("my-shuffle",
                RandomShuffleArgs.builder()
                        .inputs(List.of("A", "B", "C"))
                        .build()
        ).results();
        ctx.export("shuffled", randomShuffle);

        ctx.export("randomTuple", Output.tuple(randomString, randomUuid)
                .applyValue(t -> t.t1 + t.t2));
        ctx.export("randomAll", Output.all(randomString, randomUuid));

        return ctx.exports();
    }
}
