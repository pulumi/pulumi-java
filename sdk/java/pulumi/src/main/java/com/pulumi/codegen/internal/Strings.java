package com.pulumi.codegen.internal;

public class Strings {
    /**
     * Returns the number of Unicode grapheme clusters in {@code s}, matching the
     * behavior of PCL's {@code length()} on strings.
     *
     * <p>Implements a simple grapheme cluster counter: combining marks merge with
     * the preceding base, and a Zero-Width Joiner (U+200D) fuses the next code
     * point into the current cluster.
     */
    public static int length(String s) {
        int count = 0;
        boolean prevZwj = false;
        int i = 0;
        while (i < s.length()) {
            int cp = s.codePointAt(i);
            i += Character.charCount(cp);
            if (prevZwj) {
                prevZwj = false;
                continue;
            }
            if (cp == 0x200D) {
                prevZwj = true;
                continue;
            }
            int category = Character.getType(cp);
            if (category == Character.NON_SPACING_MARK
                    || category == Character.COMBINING_SPACING_MARK
                    || category == Character.ENCLOSING_MARK) {
                continue;
            }
            count++;
        }
        return count;
    }
}
