package com.pulumi.codegen.internal;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Files {
    /**
     * Accepts a path to a directory and returns a list of the names of
     * the files within that directory. This function maps to the readDir(...) function
     * from PCL.
     */
    public static List<KeyedValue<String>> readDir(String directoryPath) {
        var results = new ArrayList<KeyedValue<String>>();
        var files = new File(directoryPath).listFiles();
        if (files == null) {
            return results;
        }
        int counter = 0;
        for (var file : files) {
            if (file.isFile()) {
                var key = String.valueOf(counter);
                var fileName = file.getName();
                results.add(KeyedValue.create(key, fileName));
            }
        }

        return results;
    }
}
