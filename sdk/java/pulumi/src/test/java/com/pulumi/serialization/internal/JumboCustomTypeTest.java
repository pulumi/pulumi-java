package com.pulumi.serialization.internal;

import com.google.protobuf.NullValue;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import com.pulumi.Log;
import com.pulumi.test.internal.PulumiTestInternal;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class JumboCustomTypeTest {

    private final static Log log = PulumiTestInternal.mockLog();

    @Test
    void testJumboCustomTypeDeserialization() {
        var deserializer = new Deserializer(log);
        var converter = new Converter(log, deserializer);
        var value = Value.newBuilder()
                .setStructValue(Struct.newBuilder()
                        .putFields("bar1", Value.newBuilder().setStringValue("bar1").build())
                        .putFields("bar2", Value.newBuilder().setStringValue("bar2").build())
                        .putFields("bar3", Value.newBuilder().setStringValue("bar3").build())
                        .putFields("bar100", Value.newBuilder().setNullValue(NullValue.NULL_VALUE).build())
                        .putFields("bar255", Value.newBuilder().setStringValue("bar255").build())
                        .putFields("bar256", Value.newBuilder().setStringValue("bar256").build())
                        .build())
                .build();
        var data = converter.convertValue(
            "testJumboCustomType", value, JumboCustomType.class
        );
        var o = data.getValueNullable();
        assertThat(o).isNotNull();
        assertThat(o.bar1()).hasValue("bar1");
        assertThat(o.bar2()).hasValue("bar2");
        assertThat(o.bar3()).hasValue("bar3");
        assertThat(o.bar100()).isEmpty(); // set explicitly as null in struct
        assertThat(o.bar101()).isEmpty(); // not present in the struct
        assertThat(o.bar255()).hasValue("bar255");
        assertThat(o.bar256()).hasValue("bar256");
    }
}
