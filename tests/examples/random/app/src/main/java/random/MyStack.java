package random;

import java.util.List;
import java.util.Map;

import io.pulumi.Config;
import io.pulumi.Stack;
import io.pulumi.core.Output;
import io.pulumi.core.internal.annotations.OutputExport;
import io.pulumi.resources.CustomResourceOptions;

import io.pulumi.random.RandomId;
import io.pulumi.random.RandomInteger;
import io.pulumi.random.RandomPassword;
import io.pulumi.random.RandomPet;
import io.pulumi.random.RandomShuffle;
import io.pulumi.random.RandomString;
import io.pulumi.random.RandomUuid;
import io.pulumi.random.inputs.RandomIdArgs;
import io.pulumi.random.inputs.RandomIntegerArgs;
import io.pulumi.random.inputs.RandomPasswordArgs;
import io.pulumi.random.inputs.RandomPetArgs;
import io.pulumi.random.inputs.RandomShuffleArgs;
import io.pulumi.random.inputs.RandomStringArgs;
import io.pulumi.random.inputs.RandomUuidArgs;

public final class MyStack extends Stack {

    @OutputExport(name="randomPassword", type=String.class, parameters={})
    private Output<String> randomPassword;

    // TODO this does not seem to be showing up in stack outputs.
    @OutputExport(name="randomPetKeepers", type=Map.class, parameters={String.class, Object.class})
    private Output<Map<String, Object>> randomPetKeepers;

    @OutputExport(name="randomInteger", type=Integer.class, parameters={})
    private Output<Integer> randomInteger;

    @OutputExport(name="randomString", type=String.class, parameters={})
    private Output<String> randomString;

    @OutputExport(name="randomIdHex", type=String.class, parameters={})
    private Output<String> randomIdHex;

    @OutputExport(name="randomUuid", type=String.class, parameters={})
    private Output<String> randomUuid;

    @OutputExport(name="shuffled", type=List.class, parameters={String.class})
    private Output<List<String>> shuffled;

    public MyStack() {
        var randomPassword = new RandomPassword("my-password",
                                                RandomPasswordArgs.builder()
                                                .setLength(16)
                                                .setSpecial(true)
                                                .setOverrideSpecial("_%@")
                                                .build(),
                                                CustomResourceOptions.Empty);

        this.randomPassword = randomPassword.getResult();

        var randomPet = new RandomPet("my-pet",
                                      RandomPetArgs.builder().build(),
                                      CustomResourceOptions.Empty);

        this.randomPetKeepers = randomPet.getKeepers();

        var randomInteger = new RandomInteger("my-int",
                                              RandomIntegerArgs.builder()
                                              .setMax(100)
                                              .setMin(0)
                                              .build(),
                                              CustomResourceOptions.Empty);

        this.randomInteger = randomInteger.getResult();

        var randomString = new RandomString("my-string",
                                            RandomStringArgs.builder()
                                            .setLength(10)
                                            .build(),
                                            CustomResourceOptions.Empty);

        this.randomString = randomString.getResult();

        var randomId = new RandomId("my-id",
                                    RandomIdArgs.builder()
                                    .setByteLength(10)
                                    .build(),
                                    CustomResourceOptions.Empty);

        this.randomIdHex = randomId.getHex();

        var randomUuid = new RandomUuid("my-uuid",
                                        RandomUuidArgs.builder().build(),
                                        CustomResourceOptions.Empty);

        this.randomUuid = randomUuid.getResult();

        var randomShuffle = new RandomShuffle("my-shuffle",
                                              RandomShuffleArgs.builder()
                                              .setInputs(List.of("A", "B", "C"))
                                              .build(),
                                              CustomResourceOptions.Empty);

        this.shuffled = randomShuffle.getResults();
    }
}
