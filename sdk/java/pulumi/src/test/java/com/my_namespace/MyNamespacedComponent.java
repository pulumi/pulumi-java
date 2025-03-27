package com.my_namespace;

import com.pulumi.resources.ComponentResource;
import com.pulumi.resources.ResourceArgs;

class MyNamespacedComponentArgs extends ResourceArgs {
}

public class MyNamespacedComponent extends ComponentResource {
    public MyNamespacedComponent(MyNamespacedComponentArgs args) {
        super("my-component:index:MyNamespacedComponent", "test");
    }
}
