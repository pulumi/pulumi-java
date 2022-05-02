package com.pulumi.example.random;

import com.pulumi.Pulumi;
import com.pulumi.core.Output;
import com.pulumi.random.RandomId;
import com.pulumi.random.RandomIdArgs;
import com.pulumi.random.RandomInteger;
import com.pulumi.random.RandomIntegerArgs;
import com.pulumi.random.RandomPassword;
import com.pulumi.random.RandomPasswordArgs;
import com.pulumi.random.RandomPet;
import com.pulumi.random.RandomShuffle;
import com.pulumi.random.RandomShuffleArgs;
import com.pulumi.random.RandomString;
import com.pulumi.random.RandomStringArgs;
import com.pulumi.random.RandomUuid;

import java.util.List;

public class App {

    public static void main(String[] args) {
        Pulumi.run(ctx -> {
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
        });
    }
}
