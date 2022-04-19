package com.pulumi.example;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import com.pulumi.Log;
import com.pulumi.example.inputs.*;
import com.pulumi.core.Output;
import com.pulumi.core.internal.Internal;
import com.pulumi.core.internal.OutputData;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class InputTests {

    static <T> OutputData<T> waitFor(Output<T> io) {
        return Internal.of(io).getDataAsync().join();
    }

    @Test
    void testInputsFooArgs_nullValues() {
        assertThatThrownBy(() -> {
            var args = FooArgs.Empty;
            var map = Internal.from(args).toMapAsync(mock(Log.class)).join();
        }).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testInputsFooArgs_simpleValues() {
        var args = FooArgs.builder()
                .a(true)
                .b(true)
                .c(1)
                .d(2)
                .e("test1")
                .f("test2")
                .build();

        var map = Internal.from(args).toMapAsync(mock(Log.class)).join();

        assertThat(map).containsKey("a");
        assertThat(map).containsKey("b");
        assertThat(map).containsKey("c");
        assertThat(map).containsKey("d");
        assertThat(map).containsKey("e");
        assertThat(map).containsKey("f");

        assertThat(waitFor(map.get("a")).getValueNullable()).isNotNull().isEqualTo(true);
        assertThat(waitFor(map.get("b")).getValueNullable()).isNotNull().isEqualTo(true);
        assertThat(waitFor(map.get("c")).getValueNullable()).isNotNull().isEqualTo(1);
        assertThat(waitFor(map.get("d")).getValueNullable()).isNotNull().isEqualTo(2);
        assertThat(waitFor(map.get("e")).getValueNullable()).isNotNull().isEqualTo("test1");
        assertThat(waitFor(map.get("f")).getValueNullable()).isNotNull().isEqualTo("test2");
    }
}
