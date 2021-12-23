package io.pulumi.example;

import org.junit.jupiter.api.Test;

import java.util.Optional;
import io.pulumi.core.Input;
import io.pulumi.example.inputs.*;

import static org.assertj.core.api.Assertions.assertThat;

class InputTests {

    @Test
    void testPetArgs_nullValues() {
        var args = PetArgs.Empty;

        var map = args.internalToOptionalMapAsync().join();

        assertThat(map).containsKey("age");
        assertThat(map).containsKey("name");
        assertThat(map).containsKey("nameArray");
        assertThat(map).containsKey("nameMap");
        assertThat(map).containsKey("requiredName");
        assertThat(map).containsKey("requiredNameArray");
        assertThat(map).containsKey("requiredNameMap");

        assertThat(map).containsEntry("age", Optional.empty());
        assertThat(map).containsEntry("name", Optional.empty());
        assertThat(map).containsEntry("nameArray", Optional.empty());
        assertThat(map).containsEntry("nameMap", Optional.empty());
        assertThat(map).containsEntry("requiredName", Optional.empty());
        assertThat(map).containsEntry("requiredNameArray", Optional.empty());
        assertThat(map).containsEntry("requiredNameMap", Optional.empty());
    }
/* TODO: make this work
    @Test
    void testContainerArgs_simpleValues() {
        var args = PetArgs.builder()
                .setAge(1)
                .setName()
                .setNameArray()
                .setNameMap()
                .setRequiredName()
                .setRequiredNameArray()
                .setRequiredNameMap()
                .build();

        var map = args.internalToOptionalMapAsync().join();

        assertThat(map).containsKey("age");
        assertThat(map).containsKey("name");
        assertThat(map).containsKey("nameArray");
        assertThat(map).containsKey("nameMap");
        assertThat(map).containsKey("requiredName");
        assertThat(map).containsKey("requiredNameArray");
        assertThat(map).containsKey("requiredNameMap");

        assertThat(map).containsEntry("age", Optional.empty());
        assertThat(map).containsEntry("name", Optional.empty());
        assertThat(map).containsEntry("nameArray", Optional.empty());
        assertThat(map).containsEntry("nameMap", Optional.empty());
        assertThat(map).containsEntry("requiredName", Optional.empty());
        assertThat(map).containsEntry("requiredNameArray", Optional.empty());
        assertThat(map).containsEntry("requiredNameMap", Optional.empty());
    }*/
}