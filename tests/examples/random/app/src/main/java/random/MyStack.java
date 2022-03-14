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

    // FIXME: this does show up in stack outputs on the first run.
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

    @OutputExport(type = String.class)
    private final Output<String> randomTuple;

    @OutputExport(type = List.class, parameters = {String.class})
    private final Output<List<String>> randomAll;

    public MyStack() {
        var randomPassword = new RandomPassword("my-password",
                $ -> $.length(16)
                        .special(true)
                        .overrideSpecial("_@"));

        this.randomPassword = randomPassword.getResult();

        var randomPet = new RandomPet("my-pet");

        this.randomPetKeepers = randomPet.getKeepers();

        var randomInteger = new RandomInteger("my-int",
                $ -> $.max(100)
                        .min(0)
        );

        this.randomInteger = randomInteger.getResult();

        var randomString = new RandomString("my-string", $ -> $.length(10));

        this.randomString = randomString.getResult();

        var randomId = new RandomId("my-id", $ -> $.byteLength(10));

        this.randomIdHex = randomId.getHex();

        var randomUuid = new RandomUuid("my-uuid");

        this.randomUuid = randomUuid.getResult();

        var randomShuffle = new RandomShuffle("my-shuffle",
                $ -> $.inputs(List.of("A", "B", "C"))
        );

        this.shuffled = randomShuffle.getResults();

        this.randomTuple = Output.tuple(this.randomString, this.randomUuid)
                .applyValue(t -> t.t1 + t.t2);

        this.randomAll = Output.all(this.randomString, this.randomUuid);
    }
}
