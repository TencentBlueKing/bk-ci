import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class WorkerProbe {
    public static void main(String[] args) throws Exception {
        if (!"AGENT".equals(System.getProperty("build.type"))) throw new Exception("missing build type");
        if (args.length != 1) throw new Exception("wrong argument count");
        Properties props = new Properties();
        try (var stream = Files.newInputStream(Path.of(".agent.properties"))) { props.load(stream); }
        if (!"p".equals(props.getProperty("devops.project.id"))) throw new Exception("project config missing");
        if (!"a".equals(props.getProperty("devops.agent.id"))) throw new Exception("agent config missing");
        String expectedSecret = System.getenv("BK_CI_PROBE_SECRET");
        if (expectedSecret != null && !expectedSecret.equals(props.getProperty("devops.agent.secret.key"))) throw new Exception("properties escaping mismatch");
        if (!"b".equals(System.getenv("DEVOPS_BUILD_ID"))) throw new Exception("build environment missing");
        if (!"b".equals(System.getenv("BUILD_ID"))) throw new Exception("legacy environment missing");
        String payload = new String(Base64.getDecoder().decode(args[0]), StandardCharsets.UTF_8);
        Files.writeString(Path.of("received.json"), payload);
        Files.writeString(Path.of("received-gateway.txt"), props.getProperty("landun.gateway"));
        System.out.println("worker stdout");
        System.err.println("worker stderr");
        String mode = System.getenv().getOrDefault("BK_CI_PROBE_MODE", "success");
        String started = System.getenv("BK_CI_PROBE_STARTED");
        if (started != null) Files.writeString(Path.of(started), Long.toString(ProcessHandle.current().pid()));
        if ("hang".equals(mode)) Thread.sleep(600_000);
        Path error = Path.of(System.getProperty("devops.agent.error.file"));
        Files.writeString(error, "fail".equals(mode) ? "probe worker failure" : "");
        if ("nonzero".equals(mode)) System.exit(17);
    }
}
