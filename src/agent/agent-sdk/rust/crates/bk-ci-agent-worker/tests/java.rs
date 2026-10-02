#[path = "support/mod.rs"]
mod support;
use bk_ci_agent_sdk::*;
use bk_ci_agent_worker::*;
use serde_json::json;
use std::{collections::BTreeMap, time::Duration};

fn build() -> BuildInfo {
    serde_json::from_value(json!({"projectId":"p","buildId":"b","vmSeqId":"1","executeCount":1,"workspace":"D:/workspace with spaces/数字分身","pipelineId":"pipe","futureField":42})).unwrap()
}
fn context() -> ExecutionContext {
    ExecutionContext {
        config:serde_json::from_value(json!({"gateway":"http://localhost","projectId":"p","agentId":"a","secretKey":" test=中\\文 😀€Ā\t\u{0001} secret"})).unwrap(),
        metadata:serde_json::from_value(json!({"hostName":"host","hostIp":"127.0.0.1","detectOS":"WINDOWS","agentVersion":"rust","workerVersion":"probe","agentInstallPath":"agent","startedUser":"user"})).unwrap(),
        cancellation:CancellationToken::new()
    }
}
fn executor(data: &std::path::Path, mode: &str, extra: BTreeMap<String, String>) -> JavaWorker {
    let fixture = support::fixture();
    let mut environment = extra;
    environment.insert("BK_CI_PROBE_MODE".into(), mode.into());
    environment.insert(
        "BK_CI_PROBE_SECRET".into(),
        context().config.secret_key.clone(),
    );
    // Reserved execution identity cannot be overwritten by arbitrary environment configuration.
    environment.insert(
        if cfg!(windows) {
            "devops_build_id"
        } else {
            "DEVOPS_BUILD_ID"
        }
        .into(),
        "wrong".into(),
    );
    JavaWorker::new(JavaWorkerOptions {
        java_executable: fixture.java.clone(),
        worker_jar: fixture.jar.clone(),
        data_dir: data.to_owned(),
        max_heap: "128m".into(),
        environment,
    })
    .unwrap()
}
#[tokio::test]
#[ignore = "requires BK_CI_TEST_JDK (JDK 17+)"]
async fn java_worker_receives_paths_properties_environment_and_unknown_build_fields() {
    let dir = tempfile::Builder::new()
        .prefix("bkci worker spaces ")
        .tempdir()
        .unwrap();
    let worker = executor(dir.path(), "success", Default::default());
    let result = worker.execute(build(), context()).await.unwrap();
    assert!(result.success, "{}", result.message);
    let run = std::fs::read_dir(dir.path().join("runs"))
        .unwrap()
        .next()
        .unwrap()
        .unwrap()
        .path();
    let received: serde_json::Value =
        serde_json::from_slice(&std::fs::read(run.join("received.json")).unwrap()).unwrap();
    assert_eq!(received["futureField"], 42);
    assert_eq!(received["workspace"], "D:/workspace with spaces/数字分身");
    assert!(std::fs::read_to_string(run.join("stdout.log"))
        .unwrap()
        .contains("worker stdout"));
    assert!(std::fs::read_to_string(run.join("stderr.log"))
        .unwrap()
        .contains("worker stderr"));
}
#[tokio::test]
#[ignore = "requires BK_CI_TEST_JDK (JDK 17+)"]
async fn java_error_marker_preserves_go_completion_semantics() {
    for (mode, success) in [("fail", false), ("nonzero", true)] {
        let dir = tempfile::tempdir().unwrap();
        let result = executor(dir.path(), mode, Default::default())
            .execute(build(), context())
            .await
            .unwrap();
        assert_eq!(result.success, success, "{}", result.message);
    }
}
#[tokio::test]
#[ignore = "requires BK_CI_TEST_JDK (JDK 17+)"]
async fn cancellation_kills_and_reaps_the_worker() {
    let dir = tempfile::tempdir().unwrap();
    let started = dir.path().join("started");
    let env = BTreeMap::from([(
        "BK_CI_PROBE_STARTED".into(),
        started.to_string_lossy().into_owned(),
    )]);
    let worker = executor(dir.path(), "hang", env);
    let context = context();
    let cancellation = context.cancellation.clone();
    let running = tokio::spawn(async move { worker.execute(build(), context).await });
    tokio::time::timeout(Duration::from_secs(10), async {
        while !started.exists() {
            tokio::time::sleep(Duration::from_millis(10)).await;
        }
    })
    .await
    .unwrap();
    cancellation.cancel();
    let outcome = tokio::time::timeout(Duration::from_secs(5), running)
        .await
        .unwrap()
        .unwrap()
        .unwrap();
    assert!(!outcome.success);
    assert!(outcome.message.contains("cancelled"));
}

#[tokio::test]
#[ignore = "requires BK_CI_TEST_JDK (JDK 17+); exercises the 10-second version timeout"]
async fn version_detection_failure_falls_back_and_a_stuck_probe_is_reaped() {
    for mode in ["invalid", "hang"] {
        let dir = tempfile::tempdir().unwrap();
        let started = dir.path().join("version-probe-pid");
        let environment = BTreeMap::from([
            ("BK_CI_PROBE_VERSION".into(), mode.into()),
            (
                "BK_CI_PROBE_VERSION_STARTED".into(),
                started.to_string_lossy().into_owned(),
            ),
        ]);
        let worker = executor(dir.path(), "success", environment);
        let metadata = tokio::time::timeout(
            Duration::from_secs(15),
            worker.detect_metadata("test-agent"),
        )
        .await
        .unwrap()
        .unwrap();
        assert_eq!(metadata.agent_version, "test-agent");
        assert!(metadata.worker_version.is_empty());
        if mode == "hang" {
            let pid =
                sysinfo::Pid::from_u32(std::fs::read_to_string(&started).unwrap().parse().unwrap());
            let mut system = sysinfo::System::new();
            system.refresh_processes(sysinfo::ProcessesToUpdate::Some(&[pid]), true);
            assert!(
                system.process(pid).is_none(),
                "timed-out version probe was not reaped"
            );
        }
    }
}
