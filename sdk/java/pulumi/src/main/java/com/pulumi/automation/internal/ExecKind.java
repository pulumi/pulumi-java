package com.pulumi.automation.internal;

import static java.util.Objects.requireNonNull;

public enum ExecKind {
    None(""),
    Local("auto.local"),
    Inline("auto.inline");

    private final String flag;

    ExecKind(String flag) {
        this.flag = requireNonNull(flag);
    }

    public String flag() {
        return flag;
    }
}
