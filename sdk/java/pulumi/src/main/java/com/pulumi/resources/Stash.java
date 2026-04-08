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
import com.pulumi.core.annotations.Export;

import javax.annotation.Nullable;

/**
 * Stash stores an arbitrary value in the stack's state for later retrieval.
 * The output property preserves the initial input value even when the input is updated.
 */
public class Stash extends CustomResource {

    @Export(name = "input", refs = {Object.class})
    private Output<Object> input;

    @Export(name = "output", refs = {Object.class})
    private Output<Object> output;

    /**
     * Create a {@link Stash} resource with the given unique name and arguments.
     *
     * @param name The unique name of the stash resource.
     * @param args The arguments to use to populate this resource's properties.
     */
    public Stash(String name, StashArgs args) {
        this(name, args, null);
    }

    /**
     * Create a {@link Stash} resource with the given unique name, arguments, and options.
     *
     * @param name    The unique name of the stash resource.
     * @param args    The arguments to use to populate this resource's properties.
     * @param options A bag of options that control this resource's behavior.
     */
    public Stash(String name, StashArgs args, @Nullable CustomResourceOptions options) {
        super("pulumi:index:Stash", name, args, CustomResourceOptions.merge(options, null));
    }

    /**
     * The most recent value passed to the stash resource.
     *
     * @return the input value
     */
    public Output<Object> input() {
        return input;
    }

    /**
     * The value saved in the state for the stash. This preserves the initial
     * input value even when the input is updated in future deployments.
     *
     * @return the output value
     */
    public Output<Object> output() {
        return output;
    }
}
