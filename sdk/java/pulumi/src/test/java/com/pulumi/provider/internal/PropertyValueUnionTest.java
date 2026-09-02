package com.pulumi.provider.internal;

import com.pulumi.core.Output;
import com.pulumi.core.annotations.ConstValue;
import com.pulumi.core.annotations.Import;
import com.pulumi.core.annotations.UnionType;
import com.pulumi.core.internal.Internal;
import com.pulumi.core.internal.UnionCase;
import com.pulumi.provider.internal.properties.PropertyDeserializationException;
import com.pulumi.provider.internal.properties.PropertyValue;
import com.pulumi.provider.internal.properties.PropertyValueSerializer;
import com.pulumi.resources.ResourceArgs;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class PropertyValueUnionTest {

    @UnionType
    @UnionType.Case(type = VariantOneArgs.class)
    @UnionType.Case(type = VariantTwoArgs.class)
    @UnionType.Case(type = ShapeArgs.OfString.class, refs = {String.class}, tree = "[0]")
    public interface ShapeArgs {
        static ShapeArgs of(String value) {
            return new OfString(value);
        }

        final class OfString implements ShapeArgs, UnionCase {
            private final String value;

            private OfString(String value) {
                this.value = Objects.requireNonNull(value);
            }

            @Override
            public String value() {
                return value;
            }
        }
    }

    static class VariantOneArgs extends ResourceArgs implements ShapeArgs {
        @Import(name = "kind", required = true)
        @ConstValue("one")
        private Output<String> kind;

        @Import(name = "field1")
        private Output<String> field1;

        private VariantOneArgs() {
        }
    }

    static class VariantTwoArgs extends ResourceArgs implements ShapeArgs {
        @Import(name = "kind", required = true)
        @ConstValue("two")
        private Output<String> kind;

        @Import(name = "field1")
        private Output<String> field1;

        private VariantTwoArgs() {
        }
    }

    static class HolderArgs extends ResourceArgs {
        @Import(name = "shape", required = true)
        private ShapeArgs shape;

        @Import(name = "shapes")
        private List<ShapeArgs> shapes;

        @Import(name = "optionalShape")
        private ShapeArgs optionalShape;

        private HolderArgs() {
        }
    }

    @Test
    void deserializesTheMemberAConstantSelects() {
        var value = PropertyValue.of(Map.of(
                "shape", PropertyValue.of(Map.of(
                        "kind", PropertyValue.of("two"),
                        "field1", PropertyValue.of("x")
                ))
        ));
        var args = PropertyValueSerializer.deserialize(value, HolderArgs.class);
        assertThat(args.shape).isInstanceOf(VariantTwoArgs.class);
        assertThat(Internal.of(((VariantTwoArgs) args.shape).field1).getValueNullable().join()).isEqualTo("x");
    }

    @Test
    void deserializesACaseAndItsNestedForm() {
        var value = PropertyValue.of(Map.of(
                "shape", PropertyValue.of("hello"),
                "shapes", PropertyValue.of(List.of(
                        PropertyValue.of("a"),
                        PropertyValue.of(Map.of("kind", PropertyValue.of("one")))
                ))
        ));
        var args = PropertyValueSerializer.deserialize(value, HolderArgs.class);
        assertThat(args.shape).isInstanceOf(ShapeArgs.OfString.class);
        assertThat(((ShapeArgs.OfString) args.shape).value()).isEqualTo("hello");
        assertThat(args.shapes.get(0)).isInstanceOf(ShapeArgs.OfString.class);
        assertThat(args.shapes.get(1)).isInstanceOf(VariantOneArgs.class);
    }

    @Test
    void rejectsAValueThatFitsNoMember() {
        var value = PropertyValue.of(Map.of("shape", PropertyValue.of(true)));
        assertThatThrownBy(() -> PropertyValueSerializer.deserialize(value, HolderArgs.class))
                .isInstanceOf(PropertyDeserializationException.class)
                .hasMessageContaining("Expected a value that fits one member of")
                .hasMessageContaining("got a Boolean that fits none of");
    }

    @Test
    void deserializesANullUnionAsNull() {
        var value = PropertyValue.of(Map.of(
                "shape", PropertyValue.of("hello"),
                "optionalShape", PropertyValue.NULL
        ));
        var args = PropertyValueSerializer.deserialize(value, HolderArgs.class);
        assertThat(args.optionalShape).isNull();
    }

    @Test
    void rejectsAnUnknownOutputThatFitsSeveralMembers() {
        var value = PropertyValue.of(Map.of("shape", PropertyValue.of(Map.of(
                "kind", PropertyValue.of(new PropertyValue.OutputReference(PropertyValue.COMPUTED, Set.of()))
        ))));
        assertThatThrownBy(() -> PropertyValueSerializer.deserialize(value, HolderArgs.class))
                .isInstanceOf(PropertyDeserializationException.class)
                .hasMessageContaining("fits several of");
    }

    @Test
    void serializesACaseAsItsValue() {
        var serialized = PropertyValueSerializer.stateFromComponentResource(new Holder(ShapeArgs.of("hello")));
        assertThat(serialized).isEqualTo(Map.of("shape", PropertyValue.of("hello")));
    }

    static class Holder {
        @com.pulumi.core.annotations.Export(name = "shape")
        private final ShapeArgs shape;

        Holder(ShapeArgs shape) {
            this.shape = shape;
        }
    }
}
