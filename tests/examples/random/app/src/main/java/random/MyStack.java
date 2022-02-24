package random;

import io.pulumi.Stack;
import io.pulumi.core.Output;
import io.pulumi.core.annotations.OutputExport;
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
        var randomPassword = new RandomPassword("my-password", ($) -> {
                $.setLength(16)
                .setSpecial(true)
                .setOverrideSpecial("_@");
        });

        this.randomPassword = randomPassword.getResult();

        var randomPet = new RandomPet("my-pet");

        this.randomPetKeepers = randomPet.getKeepers();

        var randomInteger = new RandomInteger("my-int", $ -> {
                $.setMax(100)
                .setMin(0);
        });

        this.randomInteger = randomInteger.getResult();

        var randomString = new RandomString("my-string", $ -> {
                $.setLength(10);
        });

        this.randomString = randomString.getResult();

        var randomId = new RandomId("my-id", $ -> {
                $.setByteLength(10);
        });

        this.randomIdHex = randomId.getHex();

        var randomUuid = new RandomUuid("my-uuid");

        this.randomUuid = randomUuid.getResult();

        var randomShuffle = new RandomShuffle("my-shuffle", $ -> {
                $.setInputs(List.of("A", "B", "C"));
        });

        this.shuffled = randomShuffle.getResults();
    }
}
