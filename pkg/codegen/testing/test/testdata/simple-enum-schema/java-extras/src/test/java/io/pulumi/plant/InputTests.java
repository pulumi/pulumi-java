package com.pulumi.plant;

import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import com.pulumi.Log;
import com.pulumi.core.Output;
import com.pulumi.core.Either;
import com.pulumi.plant.inputs.*;
import com.pulumi.plant.enums.*;
import com.pulumi.core.Output;
import com.pulumi.core.internal.OutputData;
import com.pulumi.core.internal.Internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class InputTests {

    static <T> OutputData<T> waitFor(Output<T> io) {
        return Internal.of(io).getDataAsync().join();
    }

    static <T> T waitForValue(Output<T> io) {
        return waitFor(io).getValueNullable();
    }

    @Test
    void testContainerArgs_exceptionWhenMissingRequiredArgs() {
        assertThatThrownBy(() -> {
                ContainerArgs.builder().build();
        });
    }

    @Test
    void testContainerArgs_incompleteValues() {
        var args = ContainerArgs.builder()
             .size(ContainerSize.FourInch)
             .build();

        var map = Internal.from(args).toMapAsync(mock(Log.class)).join();

        // brightness is set via schema default
        assertThat(waitForValue(map.get("brightness"))).isEqualTo(ContainerBrightness.One);

        // size is set explicitly in the buidler
        assertThat(waitForValue(map.get("size"))).isEqualTo(ContainerSize.FourInch);

        assertThat(waitForValue(map.get("color"))).isNull();
        assertThat(waitForValue(map.get("material"))).isNull();
    }

    @Test
    void testContainerArgs_simpleValues() {
        var args = ContainerArgs.builder()
                .brightness(ContainerBrightness.ZeroPointOne)
                .color(Output.of(Either.ofLeft(ContainerColor.Red)))
                .material("glass")
                .size(ContainerSize.FourInch)
                .build();

        var map = Internal.from(args).toMapAsync(mock(Log.class)).join();

        assertThat(map).containsKey("brightness");
        assertThat(map).containsKey("color");
        assertThat(map).containsKey("material");
        assertThat(map).containsKey("size");

        assertThat(waitFor(map.get("brightness")).getValueNullable()).isEqualTo(ContainerBrightness.ZeroPointOne);
        assertThat(waitFor(map.get("color")).getValueNullable()).isEqualTo(Either.ofLeft(ContainerColor.Red));
        assertThat(waitFor(map.get("material")).getValueNullable()).isEqualTo("glass");
        assertThat(waitFor(map.get("size")).getValueNullable()).isEqualTo(ContainerSize.FourInch);
    }
}
