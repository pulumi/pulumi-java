package com;

import com.pulumi.resources.ComponentResource;
import com.pulumi.resources.ResourceArgs;

class MyComponentArgs extends ResourceArgs {
}

public class MyComponent extends ComponentResource {
    public MyComponent(MyComponentArgs args) {
        super("my-component:index:MyComponent", "test");
    }
}
