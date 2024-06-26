// *** WARNING: this file was generated by test. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package com.pulumi.example.local.enums;

import com.pulumi.core.annotations.EnumType;
import java.lang.Double;
import java.util.StringJoiner;

    @EnumType
    public enum MyEnum {
        Pi(3.141500),
        Small(0.000000);

        private final Double value;

        MyEnum(Double value) {
            this.value = value;
        }

        @EnumType.Converter
        public Double getValue() {
            return this.value;
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", "MyEnum[", "]")
                .add("value='" + this.value + "'")
                .toString();
        }
    }
