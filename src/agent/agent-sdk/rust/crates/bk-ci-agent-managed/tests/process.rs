#[path = "../../bk-ci-agent-worker/tests/support/mod.rs"]
mod support;

use axum::{extract::State, routing::post, Json, Router};
use base64::{engine::general_purpose::STANDARD, Engine};
use bk_ci_agent_sdk::client::{ASK_PATH, FINISH_PATH, STARTUP_PATH};
use serde_json::{json, Value};
use std::{
    path::Path,
    process::{Output, Stdio},
    sync::{
        atomic::{AtomicBool, Ordering},
        Arc, Mutex,
    },
    time::Duration,
};
use tokio::process::{Child, Command};

#[derive(Default)]
struct ServerState {
    deleted: AtomicBool,
    offer_build: AtomicBool,
    offer_docker: AtomicBool,
    fail_finish: AtomicBool,
    startups: Mutex<Vec<Value>>,
    heartbeats: Mutex<Vec<Value>>,
    finishes: Mutex<Vec<Value>>,
}

async fn startup(State(state): State<Arc<ServerState>>, Json(body): Json<Value>) -> Json<Value> {
    state.startups.lock().unwrap().push(body);
    Json(json!({"status":0,"message":"ok","data":null}))
}
async fn ask(State(state): State<Arc<ServerState>>, Json(body): Json<Value>) -> Json<Value> {
    assert_eq!(body["askEnable"]["upgrade"], false);
    let mut build = if body["askEnable"]["build"] == "BINARY"
        && state.offer_build.swap(false, Ordering::SeqCst)
    {
        json!({"projectId":"p","buildId":"b","vmSeqId":"1","pipelineId":"pipe","executeCount":1,"workspace":"D:/workspace with spaces","extraFromServer":42})
    } else {
        Value::Null
    };
    if !build.is_null() && state.offer_docker.load(Ordering::SeqCst) {
        build["dockerBuildInfo"] = json!({"image":"unsupported"});
    }
    state.heartbeats.lock().unwrap().push(body);
    Json(
        json!({"status":0,"message":"ok","agentStatus":if state.deleted.load(Ordering::SeqCst) {"DELETE"} else {"IMPORT_OK"}, "data":{"build":build}}),
    )
}
async fn finish(State(state): State<Arc<ServerState>>, Json(body): Json<Value>) -> Json<Value> {
    state.finishes.lock().unwrap().push(body);
    Json(
        json!({"status":if state.fail_finish.load(Ordering::SeqCst) {9} else {0},"message":"finish","data":null}),
    )
}
async fn server(state: Arc<ServerState>) -> (String, tokio::task::JoinHandle<()>) {
    let router = Router::new()
        .route(STARTUP_PATH, post(startup))
        .route(ASK_PATH, post(ask))
        .route(FINISH_PATH, post(finish))
        .with_state(state);
    let listener = tokio::net::TcpListener::bind("127.0.0.1:0").await.unwrap();
    let url = format!("http://{}", listener.local_addr().unwrap());
    let server = tokio::spawn(async move { axum::serve(listener, router).await.unwrap() });
    (url, server)
}
fn parameters(url: &str, dir: &Path) -> Value {
    let jar = dir.join("unused.jar");
    std::fs::write(&jar, b"unused").unwrap();
    json!({
        "config":{"gateway":url,"projectId":"p","agentId":"a","secretKey":"test-secret"},
        // An existing executable exits immediately on the unsupported version-probe arguments.
        // Builds are only enabled in the real-JVM test, which replaces these fixture paths.
        "worker":{"javaExecutable":std::env::current_exe().unwrap(),"workerJar":jar,"dataDir":dir,"maxHeap":"128m"},
        "runtime":{"pollIntervalMs":10,"successReportDelayMs":0}
    })
}
fn command() -> Command {
    let mut cmd = Command::new(env!("CARGO_BIN_EXE_bk-ci-agent-managed"));
    cmd.env_remove("RUST_LOG")
        .stdin(Stdio::null())
        .stdout(Stdio::piped())
        .stderr(Stdio::piped())
        .kill_on_drop(true);
    #[cfg(windows)]
    cmd.creation_flags(0x08000000);
    cmd
}
fn start(params: &Value) -> Child {
    command()
        .arg(STANDARD.encode(params.to_string()))
        .spawn()
        .unwrap()
}
async fn wait_until(mut predicate: impl FnMut() -> bool) {
    tokio::time::timeout(Duration::from_secs(15), async {
        while !predicate() {
            tokio::time::sleep(Duration::from_millis(10)).await;
        }
    })
    .await
    .expect("backend observation timeout");
}
async fn output(child: Child) -> Output {
    tokio::time::timeout(Duration::from_secs(15), child.wait_with_output())
        .await
        .expect("child exit timeout")
        .unwrap()
}
fn assert_logs(output: &Output) -> Vec<Value> {
    assert!(
        output.stdout.is_empty(),
        "stdout should not carry a host protocol"
    );
    let stderr = String::from_utf8_lossy(&output.stderr);
    assert!(!stderr.contains("test-secret"));
    stderr
        .lines()
        .map(|line| serde_json::from_str(line).expect("expected JSON log"))
        .collect()
}

#[tokio::test]
async fn starts_from_one_argument_with_closed_stdin_and_detects_machine_identity() {
    let state = Arc::new(ServerState::default());
    let (url, server) = server(state.clone()).await;
    let dir = tempfile::tempdir().unwrap();
    let params = parameters(&url, dir.path());
    let mut child = start(&params);
    wait_until(|| state.heartbeats.lock().unwrap().len() >= 2).await;
    assert!(
        child.try_wait().unwrap().is_none(),
        "stdin EOF must not stop the agent"
    );
    let startup = state.startups.lock().unwrap()[0].clone();
    let heartbeat = state.heartbeats.lock().unwrap()[0]["heartbeat"].clone();
    assert_eq!(startup["hostname"], sysinfo::System::host_name().unwrap());
    assert!(startup["hostIp"]
        .as_str()
        .unwrap()
        .parse::<std::net::IpAddr>()
        .is_ok());
    assert_eq!(startup["detectOS"], std::env::consts::OS);
    assert_eq!(startup["masterVersion"], env!("CARGO_PKG_VERSION"));
    assert_eq!(heartbeat["hostName"], startup["hostname"]);
    assert_eq!(heartbeat["agentIp"], startup["hostIp"]);
    assert_eq!(heartbeat["startedUser"], whoami::username().unwrap());
    assert_eq!(
        heartbeat["agentInstallPath"],
        Path::new(env!("CARGO_BIN_EXE_bk-ci-agent-managed"))
            .parent()
            .unwrap()
            .to_string_lossy()
            .as_ref()
    );
    assert!(!heartbeat["props"]["arch"].as_str().unwrap().is_empty());
    assert!(!heartbeat["props"]["osVersion"].as_str().unwrap().is_empty());
    // This is the sole host-side stop action: no shutdown request or stdin interaction.
    child.kill().await.unwrap();
    let out = output(child).await;
    assert!(!out.status.success());
    let logs = assert_logs(&out);
    assert!(logs
        .iter()
        .any(|entry| entry["fields"]["message"] == "agent ready"));
    assert!(!String::from_utf8_lossy(&out.stderr).contains(&STANDARD.encode(params.to_string())));
    server.abort();
}

#[tokio::test]
async fn backend_deletion_exits_without_a_parent_protocol() {
    let state = Arc::new(ServerState::default());
    state.deleted.store(true, Ordering::SeqCst);
    let (url, server) = server(state).await;
    let dir = tempfile::tempdir().unwrap();
    let out = output(start(&parameters(&url, dir.path()))).await;
    assert_eq!(out.status.code(), Some(1));
    assert_logs(&out);
    server.abort();
}

#[tokio::test]
async fn failed_completions_are_retained_without_overwriting_a_previous_run() {
    let state = Arc::new(ServerState::default());
    state.offer_docker.store(true, Ordering::SeqCst);
    state.fail_finish.store(true, Ordering::SeqCst);
    let (url, server) = server(state.clone()).await;
    let dir = tempfile::tempdir().unwrap();
    for _ in 0..2 {
        state.offer_build.store(true, Ordering::SeqCst);
        let out = output(start(&parameters(&url, dir.path()))).await;
        assert_eq!(out.status.code(), Some(1));
        assert_logs(&out);
    }
    let files: Vec<_> = std::fs::read_dir(dir.path().join("unreported"))
        .unwrap()
        .collect();
    assert_eq!(files.len(), 2);
    for file in files {
        let records: Value =
            serde_json::from_slice(&std::fs::read(file.unwrap().path()).unwrap()).unwrap();
        assert_eq!(records[0]["buildId"], "b");
        assert_eq!(records[0]["extraFromServer"], 42);
    }
    server.abort();
}

#[tokio::test]
async fn rejects_invalid_arguments_without_echoing_credentials_or_starting_http() {
    let state = Arc::new(ServerState::default());
    let (url, server) = server(state.clone()).await;
    let dir = tempfile::tempdir().unwrap();
    let params = parameters(&url, dir.path());
    let encoded = STANDARD.encode(params.to_string());
    let mut obsolete = params;
    obsolete["metadata"] = json!({"hostName":"forged"});
    let encoded_obsolete = STANDARD.encode(obsolete.to_string());
    for args in [
        vec![],
        vec!["invalid-base64-secret"],
        vec![&encoded, &encoded],
        vec![&encoded_obsolete],
        vec!["--config", "secret-file"],
    ] {
        let mut cmd = command();
        cmd.args(args);
        let out = output(cmd.spawn().unwrap()).await;
        assert_eq!(out.status.code(), Some(2));
        assert!(out.stdout.is_empty());
        let stderr = String::from_utf8_lossy(&out.stderr);
        for secret in [
            "test-secret",
            "invalid-base64-secret",
            "secret-file",
            &encoded,
            &encoded_obsolete,
        ] {
            assert!(!stderr.contains(secret));
        }
    }
    assert!(state.startups.lock().unwrap().is_empty());
    for arg in ["--help", "--version"] {
        let out = output(command().arg(arg).spawn().unwrap()).await;
        assert!(out.status.success());
        assert!(!out.stdout.is_empty());
    }
    server.abort();
}

#[tokio::test]
#[ignore = "requires BK_CI_TEST_JDK (JDK 17+)"]
async fn runs_real_worker_and_detects_its_version_without_supplied_metadata() {
    let fixture = support::fixture();
    let state = Arc::new(ServerState::default());
    state.offer_build.store(true, Ordering::SeqCst);
    let (url, server) = server(state.clone()).await;
    let dir = tempfile::tempdir().unwrap();
    let mut params = parameters(&url, dir.path());
    params["worker"]["javaExecutable"] = json!(fixture.java);
    params["worker"]["workerJar"] = json!(fixture.jar);
    let child = start(&params);
    wait_until(|| !state.finishes.lock().unwrap().is_empty()).await;
    assert_eq!(
        state.startups.lock().unwrap()[0]["version"],
        "v4.0.0-beta.1"
    );
    assert_eq!(
        state.heartbeats.lock().unwrap()[0]["heartbeat"]["slaveVersion"],
        "v4.0.0-beta.1"
    );
    assert_eq!(state.finishes.lock().unwrap()[0]["success"], true);
    assert_eq!(state.finishes.lock().unwrap()[0]["extraFromServer"], 42);
    state.deleted.store(true, Ordering::SeqCst);
    let out = output(child).await;
    assert_eq!(out.status.code(), Some(1));
    assert_logs(&out);
    server.abort();
}
