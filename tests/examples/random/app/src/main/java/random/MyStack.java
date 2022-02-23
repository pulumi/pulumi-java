package random;

import io.pulumi.Stack;
import io.pulumi.core.Output;
import io.pulumi.core.annotations.OutputExport;
import io.pulumi.resources.CustomResourceOptions;
import io.pulumi.random.*;

import java.util.List;
import java.util.Map;

public final class MyStack extends Stack {

    @OutputExport(type = String.class)
    private final Output<String> randomPassword;

    // TODO this does not seem to be showing up in stack outputs.
    @OutputExport(type = Map.class, parameters = {String.class, Object.class})
    private final Output<Map<String, Object>> randomPetKeepers;

    @OutputExport(type = Integer.class)
    private final Output<Integer> randomInteger;

    @OutputExport(type = String.class)
    private final Output<String> randomString;

    @OutputExport(type = String.class)
    private final Output<String> randomIdHex;

    @OutputExport(type = String.class)
    private final Output<String> randomUuid;

    @OutputExport(type = List.class, parameters = {String.class})
    private final Output<List<String>> shuffled;

    public MyStack() {
        var randomPassword = new RandomPassword("my-password",
                RandomPasswordArgs.builder()
                        .setLength(16)
                        .setSpecial(true)
                        .setOverrideSpecial("_%@")
                        .build());

        this.randomPassword = randomPassword.getResult();

        var randomPet = new RandomPet("my-pet");

        this.randomPetKeepers = randomPet.getKeepers();

        var randomInteger = new RandomInteger("my-int",
                RandomIntegerArgs.builder()
                        .setMax(100)
                        .setMin(0)
                        .build());

        this.randomInteger = randomInteger.getResult();

        var randomString = new RandomString("my-string",
                RandomStringArgs.builder()
                        .setLength(10)
                        .build());

        this.randomString = randomString.getResult();

        var randomId = new RandomId("my-id",
                RandomIdArgs.builder()
                        .setByteLength(10)
                        .build());

        this.randomIdHex = randomId.getHex();

        var randomUuid = new RandomUuid("my-uuid");

        this.randomUuid = randomUuid.getResult();

        var randomShuffle = new RandomShuffle("my-shuffle",
                RandomShuffleArgs.builder()
                        .setInputs(List.of("A", "B", "C"))
                        .build());

        this.shuffled = randomShuffle.getResults();
    }
}
