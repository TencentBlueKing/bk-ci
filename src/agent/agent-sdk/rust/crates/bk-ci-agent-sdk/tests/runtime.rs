use bk_ci_agent_sdk::{protocol::*, *};
use std::{
    collections::VecDeque,
    sync::{
        atomic::{AtomicBool, AtomicUsize, Ordering},
        Arc, Mutex,
    },
    time::Duration,
};
use tokio::sync::Semaphore;

fn config() -> AgentConfig {
    serde_json::from_value(serde_json::json!({"gateway":"http://localhost","projectId":"p","agentId":"a","secretKey":"secret","parallelTaskCount":2})).unwrap()
}
fn metadata() -> AgentMetadata {
    serde_json::from_value(serde_json::json!({"hostName":"host","hostIp":"127.0.0.1","detectOS":"WINDOWS","agentVersion":"rust","workerVersion":"worker","agentInstallPath":"C:/agent","startedUser":"user"})).unwrap()
}
fn build(vm: &str, attempt: u32) -> BuildInfo {
    serde_json::from_value(serde_json::json!({"projectId":"p","buildId":"b","vmSeqId":vm,"pipelineId":"pipe","executeCount":attempt,"workspace":"work","futureField":{"keep":true}})).unwrap()
}
fn response(build: Option<BuildInfo>) -> AgentResponse {
    AgentResponse {
        agent_status: "IMPORT_OK".into(),
        data: Some(AskResponse {
            build,
            heartbeat: None,
        }),
    }
}
fn options() -> RuntimeOptions {
    RuntimeOptions {
        poll_interval_ms: 10,
        startup_retry_ms: 60_000,
        success_report_delay_ms: 0,
        ..Default::default()
    }
}
#[derive(Default)]
struct MockBackend {
    responses: Mutex<VecDeque<AgentResponse>>,
    asks: Mutex<Vec<AskRequest>>,
    finishes: Mutex<Vec<BuildFinish>>,
    startup_fail: AtomicBool,
    startups: AtomicUsize,
    finish_fail: AtomicBool,
    first_ask_gate: Option<Arc<Semaphore>>,
}
#[async_trait]
impl Backend for MockBackend {
    async fn startup(&self, _: &AgentConfig, _: &StartupInfo) -> Result<()> {
        self.startups.fetch_add(1, Ordering::SeqCst);
        if self.startup_fail.load(Ordering::SeqCst) {
            Err(Error::Transport("offline".into()))
        } else {
            Ok(())
        }
    }
    async fn ask(&self, _: &AgentConfig, info: &AskRequest) -> Result<AgentResponse> {
        let first = {
            let mut asks = self.asks.lock().unwrap();
            asks.push(info.clone());
            asks.len() == 1
        };
        if first {
            if let Some(gate) = &self.first_ask_gate {
                gate.acquire().await.unwrap().forget();
            }
        }
        Ok(self
            .responses
            .lock()
            .unwrap()
            .pop_front()
            .unwrap_or_else(|| response(None)))
    }
    async fn finish(&self, _: &AgentConfig, info: &BuildFinish) -> Result<()> {
        self.finishes.lock().unwrap().push(info.clone());
        if self.finish_fail.load(Ordering::SeqCst) {
            Err(Error::Transport("report offline".into()))
        } else {
            Ok(())
        }
    }
}
struct RecordingExecutor {
    started: Mutex<Vec<(TaskKey, u32)>>,
    gate: Semaphore,
    panic: bool,
}
impl RecordingExecutor {
    fn new() -> Self {
        Self {
            started: Mutex::new(vec![]),
            gate: Semaphore::new(0),
            panic: false,
        }
    }
}
#[async_trait]
impl Executor for RecordingExecutor {
    async fn execute(&self, build: BuildInfo, context: ExecutionContext) -> Result<BuildOutcome> {
        self.started
            .lock()
            .unwrap()
            .push((build.key(), context.config.parallel_task_count));
        assert!(!self.panic, "test executor panic");
        tokio::select! {
            _ = context.cancellation.cancelled() => Ok(BuildOutcome::failure("cancelled")),
            permit = self.gate.acquire() => { permit.unwrap().forget(); Ok(BuildOutcome::success("done")) }
        }
    }
}
async fn until(mut check: impl FnMut() -> bool) {
    tokio::time::timeout(Duration::from_secs(5), async {
        while !check() {
            tokio::time::sleep(Duration::from_millis(5)).await;
        }
    })
    .await
    .expect("condition timed out");
}
fn start(
    backend: Arc<MockBackend>,
    executor: Arc<RecordingExecutor>,
    shutdown: Shutdown,
) -> tokio::task::JoinHandle<Result<()>> {
    let agent = Agent::new(config(), metadata(), backend, executor, options()).unwrap();
    tokio::spawn(agent.run(shutdown))
}
async fn joined(handle: tokio::task::JoinHandle<Result<()>>) -> Result<()> {
    tokio::time::timeout(Duration::from_secs(5), handle)
        .await
        .expect("agent did not stop")
        .unwrap()
}

#[tokio::test]
async fn multiple_jobs_attempts_and_duplicate_claims_are_accounted_separately() {
    let backend = Arc::new(MockBackend::default());
    backend.responses.lock().unwrap().extend([
        response(Some(build("1", 1))),
        response(Some(build("2", 1))),
        response(Some(build("1", 1))),
    ]);
    let executor = Arc::new(RecordingExecutor::new());
    let shutdown = Shutdown::new();
    let handle = start(backend.clone(), executor.clone(), shutdown.clone());
    until(|| backend.asks.lock().unwrap().len() >= 3).await;
    assert_eq!(executor.started.lock().unwrap().len(), 2);
    let asks = backend.asks.lock().unwrap().clone();
    assert_eq!(asks[2].ask_enable.build, BuildJobType::None);
    assert_eq!(asks[2].heartbeat.task_list.len(), 2);
    assert!(!asks[0].ask_enable.upgrade);
    executor.gate.add_permits(2);
    until(|| backend.finishes.lock().unwrap().len() == 2).await;
    backend
        .responses
        .lock()
        .unwrap()
        .extend([response(Some(build("1", 1))), response(Some(build("1", 2)))]);
    until(|| executor.started.lock().unwrap().len() == 3).await;
    executor.gate.add_permits(1);
    until(|| backend.finishes.lock().unwrap().len() == 3).await;
    shutdown.stop();
    joined(handle).await.unwrap();
    assert_eq!(
        backend.finishes.lock().unwrap()[0].build.extra["futureField"]["keep"],
        true
    );
}

#[tokio::test]
async fn heartbeat_changes_capacity_before_the_next_claim() {
    let backend = Arc::new(MockBackend::default());
    let mut first = response(Some(build("1", 1)));
    first.data.as_mut().unwrap().heartbeat = Some(HeartbeatResponse {
        parallel_task_count: Some(1),
        ..Default::default()
    });
    backend.responses.lock().unwrap().push_back(first);
    let executor = Arc::new(RecordingExecutor::new());
    let shutdown = Shutdown::new();
    let handle = start(backend.clone(), executor.clone(), shutdown.clone());
    until(|| backend.asks.lock().unwrap().len() >= 2).await;
    assert_eq!(
        backend.asks.lock().unwrap()[1].ask_enable.build,
        BuildJobType::None
    );
    assert_eq!(executor.started.lock().unwrap()[0].1, 1);
    let mut update = response(None);
    update.data.as_mut().unwrap().heartbeat = Some(HeartbeatResponse {
        parallel_task_count: Some(0),
        ..Default::default()
    });
    backend
        .responses
        .lock()
        .unwrap()
        .extend([update, response(Some(build("2", 1)))]);
    until(|| executor.started.lock().unwrap().len() == 2).await;
    assert_eq!(executor.started.lock().unwrap()[1].1, 0); // zero retains Go's unlimited semantics
    shutdown.stop();
    until(|| {
        backend.asks.lock().unwrap().last().is_some_and(|a| {
            a.ask_enable.build == BuildJobType::None && a.heartbeat.task_list.len() == 2
        })
    })
    .await;
    assert!(!handle.is_finished());
    executor.gate.add_permits(2);
    joined(handle).await.unwrap();
}

#[tokio::test]
async fn stop_interrupts_startup_backoff() {
    let backend = Arc::new(MockBackend::default());
    backend.startup_fail.store(true, Ordering::SeqCst);
    let shutdown = Shutdown::new();
    let handle = start(
        backend.clone(),
        Arc::new(RecordingExecutor::new()),
        shutdown.clone(),
    );
    until(|| backend.startups.load(Ordering::SeqCst) > 0).await;
    shutdown.stop();
    joined(handle).await.unwrap();
}

#[tokio::test]
async fn stop_during_inflight_claim_does_not_discard_the_claimed_task() {
    let gate = Arc::new(Semaphore::new(0));
    let backend = Arc::new(MockBackend {
        first_ask_gate: Some(gate.clone()),
        ..Default::default()
    });
    backend
        .responses
        .lock()
        .unwrap()
        .push_back(response(Some(build("1", 1))));
    let executor = Arc::new(RecordingExecutor::new());
    let shutdown = Shutdown::new();
    let handle = start(backend.clone(), executor.clone(), shutdown.clone());
    until(|| !backend.asks.lock().unwrap().is_empty()).await;
    shutdown.stop();
    gate.add_permits(1);
    until(|| executor.started.lock().unwrap().len() == 1).await;
    executor.gate.add_permits(1);
    joined(handle).await.unwrap();
    assert_eq!(backend.finishes.lock().unwrap().len(), 1);
}

#[tokio::test]
async fn terminate_cancels_execution_and_reports_failure() {
    let backend = Arc::new(MockBackend::default());
    backend
        .responses
        .lock()
        .unwrap()
        .push_back(response(Some(build("1", 1))));
    let executor = Arc::new(RecordingExecutor::new());
    let shutdown = Shutdown::new();
    let handle = start(backend.clone(), executor.clone(), shutdown.clone());
    until(|| !executor.started.lock().unwrap().is_empty()).await;
    shutdown.terminate();
    joined(handle).await.unwrap();
    assert!(!backend.finishes.lock().unwrap()[0].outcome.success);
}

#[tokio::test]
async fn executor_panic_reports_failure_instead_of_losing_the_task() {
    let backend = Arc::new(MockBackend::default());
    backend
        .responses
        .lock()
        .unwrap()
        .push_back(response(Some(build("1", 1))));
    let executor = Arc::new(RecordingExecutor {
        panic: true,
        ..RecordingExecutor::new()
    });
    let shutdown = Shutdown::new();
    let handle = start(backend.clone(), executor, shutdown.clone());
    until(|| !backend.finishes.lock().unwrap().is_empty()).await;
    shutdown.stop();
    joined(handle).await.unwrap();
    assert!(!backend.finishes.lock().unwrap()[0].outcome.success);
}

#[tokio::test]
async fn completion_failure_is_returned_and_not_silently_dropped() {
    let backend = Arc::new(MockBackend::default());
    backend.finish_fail.store(true, Ordering::SeqCst);
    backend
        .responses
        .lock()
        .unwrap()
        .push_back(response(Some(build("1", 1))));
    let executor = Arc::new(RecordingExecutor::new());
    executor.gate.add_permits(1);
    let handle = start(backend.clone(), executor, Shutdown::new());
    assert!(matches!(joined(handle).await,Err(Error::Unreported(tasks)) if tasks.len()==1));
    assert_eq!(backend.finishes.lock().unwrap().len(), 1); // no implicit duplicate side effects
}

#[tokio::test]
async fn unsupported_docker_claim_is_reported_without_launching_an_executor() {
    let backend = Arc::new(MockBackend::default());
    let mut docker = build("1", 1);
    docker.docker_build_info = Some(serde_json::json!({"image":"test"}));
    backend
        .responses
        .lock()
        .unwrap()
        .push_back(response(Some(docker)));
    let executor = Arc::new(RecordingExecutor::new());
    let shutdown = Shutdown::new();
    let handle = start(backend.clone(), executor.clone(), shutdown.clone());
    until(|| !backend.finishes.lock().unwrap().is_empty()).await;
    shutdown.stop();
    joined(handle).await.unwrap();
    assert!(executor.started.lock().unwrap().is_empty());
    assert!(!backend.finishes.lock().unwrap()[0].outcome.success);
}

#[tokio::test]
async fn deletion_stops_polling_and_drains_existing_work() {
    let backend = Arc::new(MockBackend::default());
    backend.responses.lock().unwrap().extend([
        response(Some(build("1", 1))),
        AgentResponse {
            agent_status: "DELETE".into(),
            data: None,
        },
    ]);
    let executor = Arc::new(RecordingExecutor::new());
    let handle = start(backend.clone(), executor.clone(), Shutdown::new());
    until(|| backend.asks.lock().unwrap().len() == 2).await;
    executor.gate.add_permits(1);
    assert!(matches!(joined(handle).await, Err(Error::Deleted)));
    assert_eq!(backend.asks.lock().unwrap().len(), 2);
}
