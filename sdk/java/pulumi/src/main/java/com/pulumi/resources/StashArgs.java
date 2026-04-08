// Copyright 2025, Pulumi Corporation.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.pulumi.resources;

import com.pulumi.core.Output;
import com.pulumi.core.annotations.Import;

import javax.annotation.Nullable;

/**
 * The set of arguments for constructing a {@link Stash} resource.
 */
public final class StashArgs extends ResourceArgs {

    @Import(name = "input")
    @Nullable
    private final Output<Object> input;

    private StashArgs(@Nullable Output<Object> input) {
        this.input = input;
    }

    /**
     * The value to store in the stash resource.
     *
     * @return the input value
     */
    public Output<Object> input() {
        return input;
    }

    /**
     * @return a {@link Builder} instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * The builder for {@link StashArgs}.
     */
    public static final class Builder {
        @Nullable
        private Output<Object> input;

        /**
         * @param input the value to store in the stash resource.
         * @return the {@link Builder}
         */
        public Builder input(@Nullable Output<Object> input) {
            this.input = input;
            return this;
        }

        /**
         * @param input the value to store in the stash resource.
         * @return the {@link Builder}
         */
        public Builder input(Object input) {
            this.input = Output.of(input);
            return this;
        }

        /**
         * @return a {@link StashArgs} instance created from this {@link Builder}
         */
        public StashArgs build() {
            return new StashArgs(this.input);
        }
    }
}
