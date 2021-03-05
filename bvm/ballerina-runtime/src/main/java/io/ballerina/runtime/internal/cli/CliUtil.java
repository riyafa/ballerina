package io.ballerina.runtime.internal.cli;

public class CliUtil {

    static boolean isOption(String arg) {
        return arg.startsWith("--");
    }

    private CliUtil() {
    }
}
