use axum::{
    body::Bytes,
    extract::State,
    http::{HeaderMap, StatusCode, Uri},
    response::IntoResponse,
    routing::post,
    Router,
};
use bk_ci_agent_sdk::{client::*, protocol::*, *};
use serde_json::{json, Value};
use std::sync::{Arc, Mutex};

type Requests = Arc<Mutex<Vec<(String, HeaderMap, Value)>>>;
async fn record(
    State(requests): State<Requests>,
    uri: Uri,
    headers: HeaderMap,
    body: Bytes,
) -> impl IntoResponse {
    let body: Value = serde_json::from_slice(&body).unwrap();
    requests
        .lock()
        .unwrap()
        .push((uri.path().into(), headers, body));
    axum::Json(if uri.path() == ASK_PATH {
        json!({"status":0,"message":"ok","agentStatus":"IMPORT_OK","data":{"heartbeat":{"parallelTaskCount":0},"build":{"projectId":"p","buildId":"b","vmSeqId":"2","executeCount":3,"newField":17}}})
    } else {
        json!({"status":0,"message":"ok","data":null})
    })
}
fn config(gateway: &str) -> AgentConfig {
    serde_json::from_value(
        json!({"gateway":gateway,"projectId":"p","agentId":"a","secretKey":"test-secret"}),
    )
    .unwrap()
}
fn startup() -> StartupInfo {
    StartupInfo {
        hostname: "host".into(),
        host_ip: "127.0.0.1".into(),
        detect_os: "WINDOWS".into(),
        master_version: "rust".into(),
        version: "worker".into(),
    }
}
async fn server(router: Router) -> (String, tokio::task::JoinHandle<()>) {
    let listener = tokio::net::TcpListener::bind("127.0.0.1:0").await.unwrap();
    let gateway = format!("http://{}", listener.local_addr().unwrap());
    let task = tokio::spawn(async move { axum::serve(listener, router).await.unwrap() });
    (gateway, task)
}
#[tokio::test]
async fn real_http_matches_headers_paths_wire_names_and_preserves_build_extensions() {
    let requests: Requests = Default::default();
    let router = Router::new()
        .route(STARTUP_PATH, post(record))
        .route(ASK_PATH, post(record))
        .route(FINISH_PATH, post(record))
        .with_state(requests.clone());
    let (gateway, task) = server(router).await;
    let backend = HttpBackend::new().unwrap();
    let cfg = config(&gateway);
    backend.startup(&cfg, &startup()).await.unwrap();
    let ask:AskRequest=serde_json::from_value(json!({
        "askEnable":{"build":"BINARY","upgrade":false,"dockerDebug":false,"pipeline":false},
        "heartbeat":{"masterVersion":"rust","slaveVersion":"worker","hostName":"host","agentIp":"127.0.0.1","parallelTaskCount":4,
        "agentInstallPath":"C:/agent","startedUser":"user","taskList":[],"props":{"arch":"amd64","jdkVersion":[],"dockerInitFileMd5":{"fileMd5":"","needUpgrade":false},"osVersion":"Windows"},
        "dockerParallelTaskCount":4,"dockerTaskList":[],"errorExitData":null},"upgrade":null
    })).unwrap();
    let response = backend.ask(&cfg, &ask).await.unwrap();
    let build = response.data.unwrap().build.unwrap();
    backend
        .finish(
            &cfg,
            &BuildFinish {
                build,
                outcome: BuildOutcome::success("done"),
            },
        )
        .await
        .unwrap();
    let all = requests.lock().unwrap();
    assert_eq!(all.len(), 3);
    for (_, headers, _) in all.iter() {
        assert_eq!(headers["X-DEVOPS-BUILD-TYPE"], "AGENT");
        assert_eq!(headers["X-DEVOPS-PROJECT-ID"], "p");
        assert_eq!(headers["X-DEVOPS-AGENT-ID"], "a");
        assert_eq!(headers["X-DEVOPS-AGENT-SECRET-KEY"], "test-secret");
    }
    assert_eq!(all[0].2["detectOS"], "WINDOWS");
    assert_eq!(all[1].2["askEnable"]["build"], "BINARY");
    assert_eq!(all[2].2["newField"], 17);
    assert_eq!(all[2].2["executeCount"], 3);
    assert_eq!(all[2].2["success"], true);
    assert!(all[2].2["error"].is_null());
    task.abort();
}
#[tokio::test]
async fn http_failures_invalid_json_and_backend_errors_are_distinguished() {
    let (gateway, task) =
        server(Router::new().route(STARTUP_PATH, post(|| async { StatusCode::UNAUTHORIZED })))
            .await;
    let backend = HttpBackend::new().unwrap();
    assert!(matches!(
        backend.startup(&config(&gateway), &startup()).await,
        Err(Error::Http(401))
    ));
    task.abort();
    let (gateway, task) =
        server(Router::new().route(STARTUP_PATH, post(|| async { "not json" }))).await;
    assert!(matches!(
        backend.startup(&config(&gateway), &startup()).await,
        Err(Error::Protocol(_))
    ));
    task.abort();
    let (gateway, task) = server(Router::new().route(
        STARTUP_PATH,
        post(|| async { axum::Json(json!({"status":9,"message":"bad test-secret","data":null})) }),
    ))
    .await;
    let error = backend
        .startup(&config(&gateway), &startup())
        .await
        .unwrap_err();
    assert!(!error.to_string().contains("test-secret"));
    task.abort();
}
#[test]
fn config_updates_are_atomic_and_debug_hides_credentials() {
    let mut config = config("http://localhost");
    let update = HeartbeatResponse {
        parallel_task_count: Some(0),
        gateway: Some("file:///invalid".into()),
        ..Default::default()
    };
    assert!(config.apply_heartbeat(&update).is_err());
    assert_eq!(config.parallel_task_count, 4);
    assert!(!format!("{config:?}").contains("test-secret"));
}
