package io.pulumi.core.internal;

import com.google.gson.Gson;
import io.pulumi.core.internal.Reflection.TypeShape;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReflectionTest {

    @Test
    void testTypeShapeToGSONConversion() {
        var token = TypeShape.list(Integer.class).toGSON().getType();
        var string = "[1,2,3,4]";

        var gson = new Gson();
        List<Integer> result = gson.fromJson(string, token);

        assertThat(result).isNotNull();
        assertThat(result).containsExactly(1,2,3,4);
    }
}