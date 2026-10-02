#[path = "../../bk-ci-agent-worker/tests/support/mod.rs"]
mod support;
use axum::{extract::State, routing::post, Json, Router};
use bk_ci_agent_sdk::client::{ASK_PATH, FINISH_PATH, STARTUP_PATH};
use serde_json::{json, Value};
use std::{
    process::Stdio,
    sync::{
        atomic::{AtomicBool, Ordering},
        Arc, Mutex,
    },
    time::Duration,
};
use tokio::{
    io::{AsyncBufReadExt, AsyncReadExt, AsyncWriteExt, BufReader, Lines},
    process::{Child, ChildStdout, Command},
};

#[derive(Default)]
struct ServerState {
    deleted: AtomicBool,
    offer_build: AtomicBool,
    offer_docker: AtomicBool,
    fail_finish: AtomicBool,
    finishes: Mutex<Vec<Value>>,
}
async fn ask(State(state): State<Arc<ServerState>>, Json(request): Json<Value>) -> Json<Value> {
    assert_eq!(request["askEnable"]["upgrade"], false);
    let mut build = if request["askEnable"]["build"] == "BINARY"
        && state.offer_build.swap(false, Ordering::SeqCst)
    {
        json!({"projectId":"p","buildId":"b","vmSeqId":"1","pipelineId":"pipe","executeCount":1,"workspace":"D:/workspace with spaces","extraFromServer":42})
    } else {
        Value::Null
    };
    if !build.is_null() && state.offer_docker.load(Ordering::SeqCst) {
        build["dockerBuildInfo"] = json!({"image":"unsupported"});
    }
    Json(
        json!({"status":0,"message":"ok","agentStatus":if state.deleted.load(Ordering::SeqCst){"DELETE"}else{"IMPORT_OK"},"data":{"build":build}}),
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
        .route(
            STARTUP_PATH,
            post(|| async { Json(json!({"status":0,"message":"ok","data":null})) }),
        )
        .route(ASK_PATH, post(ask))
        .route(FINISH_PATH, post(finish))
        .with_state(state);
    let listener = tokio::net::TcpListener::bind("127.0.0.1:0").await.unwrap();
    let url = format!("http://{}", listener.local_addr().unwrap());
    let server = tokio::spawn(async move { axum::serve(listener, router).await.unwrap() });
    (url, server)
}
struct Process {
    child: Child,
    lines: Lines<BufReader<ChildStdout>>,
}
impl Process {
    fn start() -> Self {
        Self::start_with_args(&[])
    }
    fn start_with_args(args: &[&str]) -> Self {
        let mut cmd = Command::new(env!("CARGO_BIN_EXE_bk-ci-agent-managed"));
        cmd.args(args)
            .env_remove("RUST_LOG")
            .stdin(Stdio::piped())
            .stdout(Stdio::piped())
            .stderr(Stdio::piped())
            .kill_on_drop(true);
        #[cfg(windows)]
        cmd.creation_flags(0x08000000);
        let mut child = cmd.spawn().unwrap();
        let lines = BufReader::new(child.stdout.take().unwrap()).lines();
        Self { child, lines }
    }
    async fn write(&mut self, request: Value) {
        let mut bytes = serde_json::to_vec(&request).unwrap();
        bytes.push(b'\n');
        self.child
            .stdin
            .as_mut()
            .unwrap()
            .write_all(&bytes)
            .await
            .unwrap();
        self.child.stdin.as_mut().unwrap().flush().await.unwrap();
    }
    async fn until(&mut self, mut predicate: impl FnMut(&Value) -> bool) -> Value {
        tokio::time::timeout(Duration::from_secs(15), async {
            loop {
                let line = self
                    .lines
                    .next_line()
                    .await
                    .unwrap()
                    .expect("protocol stream ended");
                let value: Value =
                    serde_json::from_str(&line).expect("stdout polluted by non-protocol output");
                if predicate(&value) {
                    return value;
                }
            }
        })
        .await
        .expect("protocol timeout")
    }
    async fn exit(&mut self) -> std::process::ExitStatus {
        tokio::time::timeout(Duration::from_secs(5), self.child.wait())
            .await
            .expect("child did not exit with stdin open")
            .unwrap()
    }
}
fn init(url: &str, dir: &std::path::Path) -> Value {
    // No builds in non-JDK tests, so existing fixture paths suffice for path validation.
    let jar = dir.join("unused.jar");
    std::fs::write(&jar, b"unused").unwrap();
    json!({"jsonrpc":"2.0","id":1,"method":"agent.initialize","params":{
        "protocolVersion":1,
        "config":{"gateway":url,"projectId":"p","agentId":"a","secretKey":"test-secret"},
        "metadata":{"hostName":"host","hostIp":"127.0.0.1","detectOS":"WINDOWS","agentVersion":"rust","workerVersion":"probe","agentInstallPath":dir,"startedUser":"user"},
        "worker":{"javaExecutable":std::env::current_exe().unwrap(),"workerJar":jar,"dataDir":dir,"maxHeap":"128m"},
        "runtime":{"pollIntervalMs":10,"successReportDelayMs":0}
    }})
}

#[tokio::test]
async fn initializes_once_and_drains_without_waiting_for_stdin_eof() {
    let (url, server) = server(Arc::new(ServerState::default())).await;
    let dir = tempfile::tempdir().unwrap();
    let mut process = Process::start();
    let request = init(&url, dir.path());
    process.write(request.clone()).await;
    assert_eq!(
        process.until(|v| v["id"] == 1).await["result"]["protocolVersion"],
        1
    );
    process.until(|v| v["method"] == "agent.ready").await;
    process.write(request).await;
    assert_eq!(
        process.until(|v| v["id"] == 1).await["error"]["code"],
        -32001
    );
    process
        .write(json!({"jsonrpc":"2.0","id":2,"method":"agent.shutdown","params":{"mode":"drain"}}))
        .await;
    assert_eq!(
        process.until(|v| v["id"] == 2).await["result"]["accepted"],
        true
    );
    assert_eq!(
        process.until(|v| v["method"] == "agent.stopped").await["params"]["success"],
        true
    );
    assert!(process.exit().await.success());
    server.abort();
}

#[tokio::test]
async fn deletion_exits_even_when_parent_keeps_stdin_open() {
    let state = Arc::new(ServerState::default());
    state.deleted.store(true, Ordering::SeqCst);
    let (url, server) = server(state).await;
    let dir = tempfile::tempdir().unwrap();
    let mut process = Process::start();
    process.write(init(&url, dir.path())).await;
    assert_eq!(
        process.until(|v| v["method"] == "agent.stopped").await["params"]["success"],
        false
    );
    assert!(!process.exit().await.success());
    server.abort();
}

#[tokio::test]
async fn rejects_unsupported_protocol_and_exits_cleanly_on_eof() {
    let dir = tempfile::tempdir().unwrap();
    let mut request = init("http://127.0.0.1:1", dir.path());
    request["params"]["protocolVersion"] = json!(999);
    let mut process = Process::start();
    process.write(request).await;
    assert_eq!(
        process.until(|v| v["id"] == 1).await["error"]["code"],
        -32602
    );
    process.child.stdin.take();
    assert!(process.exit().await.success());
}

#[tokio::test]
#[ignore = "requires BK_CI_TEST_JDK (JDK 17+)"]
async fn aihub_subprocess_runs_real_jvm_and_reports_to_mock_backend() {
    let fixture = support::fixture();
    let state = Arc::new(ServerState::default());
    state.offer_build.store(true, Ordering::SeqCst);
    let (url, server) = server(state.clone()).await;
    let dir = tempfile::Builder::new()
        .prefix("bkci e2e spaces ")
        .tempdir()
        .unwrap();
    let mut request = init(&url, dir.path());
    request["params"]["worker"]["javaExecutable"] = json!(fixture.java);
    request["params"]["worker"]["workerJar"] = json!(fixture.jar);
    let mut process = Process::start();
    process.write(request).await;
    let finished = process
        .until(|v| v["method"] == "agent.event" && v["params"]["type"] == "taskFinished")
        .await;
    assert_eq!(finished["params"]["outcome"]["success"], true, "{finished}");
    assert_eq!(state.finishes.lock().unwrap().len(), 1);
    assert_eq!(state.finishes.lock().unwrap()[0]["extraFromServer"], 42);
    process
        .write(json!({"jsonrpc":"2.0","id":2,"method":"agent.shutdown"}))
        .await;
    process.until(|v| v["method"] == "agent.stopped").await;
    assert!(process.exit().await.success());
    server.abort();
}

#[tokio::test]
async fn failed_completion_is_retained_in_the_final_protocol_message() {
    let state = Arc::new(ServerState::default());
    state.offer_build.store(true, Ordering::SeqCst);
    state.offer_docker.store(true, Ordering::SeqCst);
    state.fail_finish.store(true, Ordering::SeqCst);
    let (url, server) = server(state.clone()).await;
    let dir = tempfile::tempdir().unwrap();
    let mut process = Process::start();
    process.write(init(&url, dir.path())).await;
    let stopped = process.until(|v| v["method"] == "agent.stopped").await;
    assert_eq!(stopped["params"]["success"], false);
    assert_eq!(
        stopped["params"]["unreportedCompletions"][0]["buildId"],
        "b"
    );
    assert_eq!(
        stopped["params"]["unreportedCompletions"][0]["extraFromServer"],
        42
    );
    assert_eq!(state.finishes.lock().unwrap().len(), 1);
    assert!(!process.exit().await.success());
    server.abort();
}

#[tokio::test]
async fn file_defaults_are_overridden_by_rpc_and_logs_stay_on_stderr() {
    let (url, server) = server(Arc::new(ServerState::default())).await;
    let dir = tempfile::tempdir().unwrap();
    let mut defaults = init("http://127.0.0.1:1", dir.path())["params"].clone();
    defaults["worker"]["maxHeap"] = json!("invalid heap");
    defaults["runtime"]["pollIntervalMs"] = json!(0);
    let path = dir.path().join("defaults.json");
    std::fs::write(&path, serde_json::to_vec(&defaults).unwrap()).unwrap();
    let mut process =
        Process::start_with_args(&["--config", path.to_str().unwrap(), "--log-filter", "info"]);
    process.write(json!({"jsonrpc":"2.0","id":1,"method":"agent.initialize","params":{
        "config":{"gateway":url}, "worker":{"maxHeap":"128m"}, "runtime":{"pollIntervalMs":10}
    }})).await;
    assert_eq!(
        process.until(|v| v["id"] == 1).await["result"]["initialized"],
        true
    );
    process.until(|v| v["method"] == "agent.ready").await;
    process
        .write(json!({"jsonrpc":"2.0","id":2,"method":"agent.shutdown"}))
        .await;
    process.until(|v| v["method"] == "agent.stopped").await;
    assert!(process.exit().await.success());
    let mut logs = String::new();
    process
        .child
        .stderr
        .take()
        .unwrap()
        .read_to_string(&mut logs)
        .await
        .unwrap();
    let entries: Vec<Value> = logs
        .lines()
        .map(|line| serde_json::from_str(line).unwrap())
        .collect();
    assert!(
        entries
            .iter()
            .any(|log| log["fields"]["message"] == "agent ready"),
        "{logs}"
    );
    assert!(
        !logs.contains("test-secret"),
        "credentials leaked to stderr"
    );
    server.abort();
}

#[tokio::test]
async fn transport_handles_fragmented_utf8_crlf_multiple_frames_and_eof() {
    let mut process = Process::start();
    // A multi-byte identifier split at arbitrary read boundaries still round-trips.
    let data = "{\"jsonrpc\":\"2.0\",\"id\":\"分身\",\"method\":\"unknown\"}\r\n{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"unknown\"}";
    for chunk in data.as_bytes().chunks(1) {
        process
            .child
            .stdin
            .as_mut()
            .unwrap()
            .write_all(chunk)
            .await
            .unwrap();
    }
    process.child.stdin.take();
    assert_eq!(
        process.until(|v| v["id"] == "分身").await["error"]["code"],
        -32601
    );
    assert_eq!(
        process.until(|v| v["id"] == 2).await["error"]["code"],
        -32601
    );
    assert!(process.exit().await.success());
}

#[tokio::test]
async fn transport_rejects_invalid_utf8_and_oversized_frames() {
    for bytes in [vec![0xff, b'\n'], vec![b'x'; 1024 * 1024 + 1]] {
        let mut process = Process::start();
        process
            .child
            .stdin
            .as_mut()
            .unwrap()
            .write_all(&bytes)
            .await
            .unwrap();
        assert_eq!(
            process.until(|v| v["error"].is_object()).await["error"]["code"],
            -32700
        );
        assert_eq!(process.exit().await.code(), Some(2));
    }
}

#[tokio::test]
async fn cli_help_errors_and_invalid_config_do_not_start_the_protocol() {
    let dir = tempfile::tempdir().unwrap();
    let invalid = dir.path().join("invalid.json");
    std::fs::write(
        &invalid,
        b"{\"secretKey\": \"sensitive-file-value\" invalid}",
    )
    .unwrap();
    for (args, success, expected) in [
        (vec!["--help"], true, "--log-format"),
        (vec!["--version"], true, env!("CARGO_PKG_VERSION")),
        (vec!["--unsupported"], false, "--unsupported"),
        (
            vec!["--log-filter", "invalid=not-a-level"],
            false,
            "invalid log filter",
        ),
        (
            vec!["--config", invalid.to_str().unwrap()],
            false,
            "cannot load configuration",
        ),
    ] {
        let mut command = Command::new(env!("CARGO_BIN_EXE_bk-ci-agent-managed"));
        command
            .args(&args)
            .env_remove("RUST_LOG")
            .stdin(Stdio::null())
            .kill_on_drop(true);
        #[cfg(windows)]
        command.creation_flags(0x08000000);
        let output = tokio::time::timeout(Duration::from_secs(5), command.output())
            .await
            .unwrap()
            .unwrap();
        assert_eq!(output.status.success(), success, "{args:?}");
        let text = String::from_utf8(if success {
            output.stdout
        } else {
            assert!(output.stdout.is_empty());
            output.stderr
        })
        .unwrap();
        assert!(text.contains(expected), "{text}");
        assert!(!text.contains("sensitive-file-value"));
    }
}
