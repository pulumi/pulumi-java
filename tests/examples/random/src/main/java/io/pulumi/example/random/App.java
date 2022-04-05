package io.pulumi.example.random;

import io.pulumi.Pulumi;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class App {

    public static void main(String[] args) {
        int exitCode = Pulumi.run(App::stack);
        System.exit(exitCode);
    }

    private static Map<String, Output<?>> stack() {
        var exports = new HashMap<String, Output<?>>();

        var randomPassword = new RandomPassword("my-password",
            RandomPasswordArgs.builder().length(16)
                .special(true)
                .overrideSpecial("_@")
                .build());

        exports.put("randomPassword", randomPassword.getResult());

        var randomPet = new RandomPet("my-pet");

        exports.put("randomPetKeepers", randomPet.getKeepers());

        var randomInteger = new RandomInteger("my-int",
            RandomIntegerArgs.builder()
                .max(100)
                .min(0)
                .build()
        );

        exports.put("randomInteger", randomInteger.getResult());

        var randomString = new RandomString("my-string",
            RandomStringArgs.builder()
                .length(10)
                .build()
        ).getResult();

        exports.put("randomString", randomString);

        var randomId = new RandomId("my-id",
            RandomIdArgs.builder()
                .byteLength(10)
                .build()
        );

        exports.put("randomIdHex", randomId.getHex());

        var randomUuid = new RandomUuid("my-uuid").getResult();
        exports.put("randomUuid", randomUuid);

        var randomShuffle = new RandomShuffle("my-shuffle",
            RandomShuffleArgs.builder()
                .inputs(List.of("A", "B", "C"))
                .build()
        ).getResults();

        exports.put("shuffled", randomShuffle);

        exports.put("randomTuple", Output.tuple(randomString, randomUuid)
            .applyValue(t -> t.t1 + t.t2));

        exports.put("randomAll", Output.all(randomString, randomUuid));

        return exports;
    }
}
