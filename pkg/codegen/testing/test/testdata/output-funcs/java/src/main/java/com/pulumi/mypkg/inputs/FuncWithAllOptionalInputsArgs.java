// *** WARNING: this file was generated by test. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package com.pulumi.mypkg.inputs;

import com.pulumi.core.Output;
import com.pulumi.core.annotations.Import;
import java.lang.String;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;


public final class FuncWithAllOptionalInputsArgs extends com.pulumi.resources.InvokeArgs {

    public static final FuncWithAllOptionalInputsArgs Empty = new FuncWithAllOptionalInputsArgs();

    /**
     * Property A
     * 
     */
    @Import(name="a")
    private @Nullable Output<String> a;

    /**
     * @return Property A
     * 
     */
    public Optional<Output<String>> a() {
        return Optional.ofNullable(this.a);
    }

    /**
     * Property B
     * 
     */
    @Import(name="b")
    private @Nullable Output<String> b;

    /**
     * @return Property B
     * 
     */
    public Optional<Output<String>> b() {
        return Optional.ofNullable(this.b);
    }

    private FuncWithAllOptionalInputsArgs() {}

    private FuncWithAllOptionalInputsArgs(FuncWithAllOptionalInputsArgs $) {
        this.a = $.a;
        this.b = $.b;
    }

    public static Builder builder() {
        return new Builder();
    }
    public static Builder builder(FuncWithAllOptionalInputsArgs defaults) {
        return new Builder(defaults);
    }

    public static final class Builder {
        private FuncWithAllOptionalInputsArgs $;

        public Builder() {
            $ = new FuncWithAllOptionalInputsArgs();
        }

        public Builder(FuncWithAllOptionalInputsArgs defaults) {
            $ = new FuncWithAllOptionalInputsArgs(Objects.requireNonNull(defaults));
        }

        /**
         * @param a Property A
         * 
         * @return builder
         * 
         */
        public Builder a(@Nullable Output<String> a) {
            $.a = a;
            return this;
        }

        /**
         * @param a Property A
         * 
         * @return builder
         * 
         */
        public Builder a(String a) {
            return a(Output.of(a));
        }

        /**
         * @param b Property B
         * 
         * @return builder
         * 
         */
        public Builder b(@Nullable Output<String> b) {
            $.b = b;
            return this;
        }

        /**
         * @param b Property B
         * 
         * @return builder
         * 
         */
        public Builder b(String b) {
            return b(Output.of(b));
        }

        public FuncWithAllOptionalInputsArgs build() {
            return $;
        }
    }

}
