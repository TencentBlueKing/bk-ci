package com.tencent.devops.agent;

public class AgentVersionKt {
    public static void main(String[] args) throws Exception {
        String mode = System.getenv("BK_CI_PROBE_VERSION");
        if ("hang".equals(mode)) {
            String started = System.getenv("BK_CI_PROBE_VERSION_STARTED");
            if (started != null) java.nio.file.Files.writeString(java.nio.file.Path.of(started), Long.toString(ProcessHandle.current().pid()));
            Thread.sleep(600_000);
        }
        if ("invalid".equals(mode)) {
            System.out.println("not-a-version");
            System.exit(1);
        }
        System.out.println("Picked up JAVA_TOOL_OPTIONS: ignored");
        System.out.println("v4.0.0-beta.1");
    }
}
