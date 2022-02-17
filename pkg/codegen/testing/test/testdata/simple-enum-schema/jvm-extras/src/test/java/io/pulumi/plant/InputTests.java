package io.pulumi.plant;

import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import io.pulumi.Log;
import io.pulumi.core.Input;
import io.pulumi.core.Either;
import io.pulumi.plant.inputs.*;
import io.pulumi.plant.enums.*;
import io.pulumi.core.InputOutput;
import io.pulumi.core.internal.InputOutputData;
import io.pulumi.core.internal.Internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class InputTests {

    static <T, IO extends InputOutput<T, IO>> InputOutputData<T> waitFor(IO io) {
        return Internal.of(io).getDataAsync().join();
    }

    @Test
    void testContainerArgs_nullValues() {
        var args = ContainerArgs.Empty;
        var map = Internal.from(args).toOptionalMapAsync(mock(Log.class)).join();

        assertThat(map).containsKey("brightness");
        assertThat(map).containsKey("color");
        assertThat(map).containsKey("material");
        assertThat(map).containsKey("size");

        assertThat(waitFor((Input) map.get("brightness").get())).isEqualTo(waitFor(Input.empty()));
        assertThat(waitFor((Input) map.get("color").get())).isEqualTo(waitFor(Input.empty()));
        assertThat(waitFor((Input) map.get("material").get())).isEqualTo(waitFor(Input.empty()));
        assertThat(waitFor((Input) map.get("size").get())).isEqualTo(waitFor(Input.empty()));
    }

    @Test
    void testContainerArgs_simpleValues() {
        var args = ContainerArgs.builder()
                .setBrightness(ContainerBrightness.ZeroPointOne)
                .setColor(Input.of(Either.ofLeft(ContainerColor.Red)))
                .setMaterial("glass")
                .setSize(ContainerSize.FourInch)
                .build();

        var map = Internal.from(args).toOptionalMapAsync(mock(Log.class)).join();

        assertThat(map).containsKey("brightness");
        assertThat(map).containsKey("color");
        assertThat(map).containsKey("material");
        assertThat(map).containsKey("size");

        assertThat(waitFor((Input) map.get("brightness").get())).isEqualTo(waitFor(Input.of(ContainerBrightness.ZeroPointOne)));
        assertThat(waitFor((Input) map.get("color").get())).isEqualTo(waitFor(Input.ofLeft(ContainerColor.Red)));
        assertThat(waitFor((Input) map.get("material").get())).isEqualTo(waitFor(Input.of("glass")));
        assertThat(waitFor((Input) map.get("size").get())).isEqualTo(waitFor(Input.of(ContainerSize.FourInch)));
    }
}