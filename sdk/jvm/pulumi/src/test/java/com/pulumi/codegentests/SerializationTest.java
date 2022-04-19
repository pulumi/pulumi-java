package com.pulumi.codegentests;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

import static com.pulumi.codegen.Serialization.*;

public class SerializationTest {
    @Test
    public void testSerializeJsonWorks() {
        var expr = JsonObject(
                JsonProperty("firstName", "John"),
                JsonProperty("lastName", "Doe"),
                JsonProperty("hobbies", JsonArray(
                        JsonObject(
                                JsonProperty("name", "programming")
                        )
                ))
        );

        var serialized = SerializeJson(expr);

        var gson = new Gson();
        var type = new TypeToken<Map<String, Object>>(){}.getType();
        Map<String, Object> deserialized = gson.fromJson(serialized, type);
        assertThat(deserialized.containsKey("firstName")).isTrue();
        assertThat(deserialized.get("firstName")).isEqualTo("John");
        assertThat(deserialized.containsKey("lastName")).isTrue();
        assertThat(deserialized.get("lastName")).isEqualTo("Doe");
        assertThat(deserialized.containsKey("hobbies")).isTrue();
        var hobbies = (ArrayList<Map<String, Object>>)deserialized.get("hobbies");
        assertThat(hobbies).hasSize(1);
        assertThat(hobbies.get(0).containsKey("name")).isTrue();
        assertThat(hobbies.get(0).get("name")).isEqualTo("programming");
    }
}
