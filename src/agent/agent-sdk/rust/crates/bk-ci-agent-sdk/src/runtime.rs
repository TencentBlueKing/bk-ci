use crate::{
    protocol::*, AgentConfig, AgentMetadata, Backend, BuildInfo, BuildOutcome, Error, Result,
    TaskKey,
};
use async_trait::async_trait;
use futures_util::FutureExt;
use serde::{Deserialize, Serialize};
use std::{
    collections::{HashMap, HashSet, VecDeque},
    panic::AssertUnwindSafe,
    sync::Arc,
    time::Duration,
};
use tokio::{
    sync::{broadcast, RwLock},
    task::JoinSet,
    time::{sleep, sleep_until, Instant},
};
use tokio_util::sync::CancellationToken;

#[derive(Clone, Debug)]
pub struct ExecutionContext {
    /// Snapshot for this execution. Later heartbeat updates apply to subsequent executions.
    pub config: AgentConfig,
    pub metadata: AgentMetadata,
    pub cancellation: CancellationToken,
}

/// An executor returns a result; the SDK alone reports workerBuildFinish.
/// Implementations must observe cancellation and reap their child before returning.
#[async_trait]
pub trait Executor: Send + Sync {
    fn supports_binary(&self) -> bool {
        true
    }
    fn supports_docker(&self) -> bool {
        false
    }
    async fn execute(&self, build: BuildInfo, context: ExecutionContext) -> Result<BuildOutcome>;
}

#[derive(Clone, Debug, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", default)]
pub struct RuntimeOptions {
    pub poll_interval_ms: u64,
    pub startup_retry_ms: u64,
    /// Compatibility delay: the Java worker reports pipeline completion itself before the agent.
    pub success_report_delay_ms: u64,
    /// Default 1: repeated completion requests require backend idempotency.
    pub finish_attempts: u32,
    pub finish_retry_ms: u64,
    pub recent_task_capacity: usize,
}
impl Default for RuntimeOptions {
    fn default() -> Self {
        Self {
            poll_interval_ms: 5000,
            startup_retry_ms: 5000,
            success_report_delay_ms: 8000,
            finish_attempts: 1,
            finish_retry_ms: 1000,
            recent_task_capacity: 4096,
        }
    }
}

/// stop(): drain; terminate(): also cooperatively cancel executors. No process exit is performed.
#[derive(Clone, Default, Debug)]
pub struct Shutdown {
    stop: CancellationToken,
    terminate: CancellationToken,
}
impl Shutdown {
    pub fn new() -> Self {
        Self::default()
    }
    pub fn stop(&self) {
        self.stop.cancel();
    }
    pub fn terminate(&self) {
        self.stop.cancel();
        self.terminate.cancel();
    }
    pub fn is_stopping(&self) -> bool {
        self.stop.is_cancelled()
    }
}

#[derive(Clone, Debug, Serialize)]
#[serde(tag = "type", rename_all = "camelCase")]
pub enum AgentEvent {
    Ready,
    Draining,
    TaskStarted {
        task: TaskKey,
    },
    TaskFinished {
        task: TaskKey,
        outcome: BuildOutcome,
    },
    DuplicateTask {
        task: TaskKey,
    },
    ConfigurationUpdated {
        parallel_task_count: u32,
        docker_parallel_task_count: u32,
    },
    Warning {
        message: String,
    },
    /// Contains the completion payload so a host can retain/reconcile it explicitly.
    CompletionUnreported {
        completion: BuildFinish,
        message: String,
    },
}

pub struct Agent {
    config: Arc<RwLock<AgentConfig>>,
    metadata: AgentMetadata,
    backend: Arc<dyn Backend>,
    executor: Arc<dyn Executor>,
    options: RuntimeOptions,
    events: broadcast::Sender<AgentEvent>,
}
impl Agent {
    pub fn new(
        mut config: AgentConfig,
        metadata: AgentMetadata,
        backend: Arc<dyn Backend>,
        executor: Arc<dyn Executor>,
        options: RuntimeOptions,
    ) -> Result<Self> {
        config.validate()?;
        config.gateway = crate::config::normalize_gateway(&config.gateway)?;
        if !config.file_gateway.is_empty() {
            config.file_gateway = crate::config::normalize_gateway(&config.file_gateway)?;
        }
        if options.poll_interval_ms == 0
            || options.startup_retry_ms == 0
            || options.finish_attempts == 0
            || options.recent_task_capacity == 0
        {
            return Err(Error::Config(
                "intervals, finishAttempts and recentTaskCapacity must be positive".into(),
            ));
        }
        let (events, _) = broadcast::channel(256);
        Ok(Self {
            config: Arc::new(RwLock::new(config)),
            metadata,
            backend,
            executor,
            options,
            events,
        })
    }

    /// Subscribe before calling run. Slow observers may lag; execution never waits for observers.
    pub fn subscribe(&self) -> broadcast::Receiver<AgentEvent> {
        self.events.subscribe()
    }

    fn emit(&self, event: AgentEvent) {
        // Libraries emit through the host's subscriber; they never install a global logger.
        // Do not log configuration, payloads or executor/backend error text (may contain secrets).
        match &event {
            AgentEvent::Ready => tracing::info!("agent ready"),
            AgentEvent::Draining => tracing::info!("agent draining"),
            AgentEvent::TaskStarted { task } => tracing::info!(?task, "task started"),
            AgentEvent::TaskFinished { task, outcome } => {
                tracing::info!(?task, success = outcome.success, "task finished")
            }
            AgentEvent::DuplicateTask { task } => tracing::warn!(?task, "duplicate task ignored"),
            AgentEvent::ConfigurationUpdated {
                parallel_task_count,
                docker_parallel_task_count,
            } => tracing::debug!(
                parallel_task_count,
                docker_parallel_task_count,
                "configuration updated"
            ),
            AgentEvent::Warning { .. } => {
                tracing::warn!("agent warning; details available through AgentEvent")
            }
            AgentEvent::CompletionUnreported { completion, .. } => {
                tracing::error!(task = ?completion.build.key(), "completion reporting failed")
            }
        }
        let _ = self.events.send(event);
    }

    pub async fn run(self, shutdown: Shutdown) -> Result<()> {
        loop {
            if shutdown.is_stopping() {
                return Ok(());
            }
            let config = self.config.read().await.clone();
            let startup = self.metadata.startup();
            let result = tokio::select! {
                biased;
                _ = shutdown.stop.cancelled() => return Ok(()),
                result = self.backend.startup(&config, &startup) => result,
            };
            match result {
                Ok(()) => break,
                Err(Error::Deleted) => return Err(Error::Deleted),
                Err(e) => {
                    self.emit(AgentEvent::Warning {
                        message: format!("startup: {e}"),
                    });
                    tokio::select! {
                        _ = shutdown.stop.cancelled() => return Ok(()),
                        _ = sleep(Duration::from_millis(self.options.startup_retry_ms)) => {}
                    }
                }
            }
        }
        if shutdown.is_stopping() {
            return Ok(());
        }
        self.emit(AgentEvent::Ready);
        let mut tasks = HashMap::<TaskKey, BuildInfo>::new();
        let mut jobs: JoinSet<(BuildFinish, std::result::Result<(), String>)> = JoinSet::new();
        let mut recent = HashSet::new();
        let mut recent_order = VecDeque::new();
        let mut unreported = Vec::new();
        let mut draining = false;
        let mut deleted = false;
        let mut next_poll = Instant::now();

        loop {
            if !draining && shutdown.is_stopping() {
                draining = true;
                self.emit(AgentEvent::Draining);
            }
            if draining && jobs.is_empty() {
                if !unreported.is_empty() {
                    return Err(Error::Unreported(unreported));
                }
                return if deleted { Err(Error::Deleted) } else { Ok(()) };
            }
            tokio::select! {
                biased;
                _ = shutdown.stop.cancelled(), if !draining => {
                    draining = true;
                    self.emit(AgentEvent::Draining);
                }
                completed = jobs.join_next(), if !jobs.is_empty() => {
                    match completed.expect("nonempty JoinSet") {
                        Ok((finish, report)) => {
                            let key = finish.build.key();
                            tasks.remove(&key);
                            recent.insert(key.clone());
                            recent_order.push_back(key.clone());
                            while recent_order.len() > self.options.recent_task_capacity {
                                if let Some(old) = recent_order.pop_front() { recent.remove(&old); }
                            }
                            self.emit(AgentEvent::TaskFinished { task: key.clone(), outcome: finish.outcome.clone() });
                            if let Err(error) = report {
                                unreported.push(finish.clone());
                                self.emit(AgentEvent::CompletionUnreported { completion: finish, message: error });
                                shutdown.stop();
                            }
                        }
                        Err(error) => {
                            // SDK task panic is fatal: preserve outstanding identities for reconciliation.
                            unreported.extend(tasks.values().cloned().map(|build| BuildFinish { build, outcome: BuildOutcome::failure("SDK execution task failed") }));
                            self.emit(AgentEvent::Warning { message: format!("execution task failed: {error}") });
                            shutdown.terminate();
                        }
                    }
                }
                _ = sleep_until(next_poll), if !deleted => {
                    let config = self.config.read().await.clone();
                    let mut request = self.heartbeat(&config, &tasks, draining || shutdown.is_stopping());
                    // A claimed task in an in-flight response must be accounted for even after stop().
                    if shutdown.is_stopping() { request.ask_enable.build = BuildJobType::None; }
                    let response = self.backend.ask(&config, &request).await;
                    next_poll = Instant::now() + Duration::from_millis(self.options.poll_interval_ms);
                    match response {
                        Err(e) => self.emit(AgentEvent::Warning { message: format!("ask: {e}") }),
                        Ok(response) => {
                            if response.agent_status == "DELETE" {
                                deleted = true;
                                shutdown.stop();
                                continue;
                            }
                            if response.agent_status != "IMPORT_OK" {
                                self.emit(AgentEvent::Warning { message: format!("agent status: {}", response.agent_status) });
                                continue;
                            }
                            if let Some(data) = response.data {
                                if let Some(heart) = data.heartbeat {
                                    let mut config = self.config.write().await;
                                    match config.apply_heartbeat(&heart) {
                                        Ok(()) => self.emit(AgentEvent::ConfigurationUpdated {
                                            parallel_task_count: config.parallel_task_count,
                                            docker_parallel_task_count: config.docker_parallel_task_count }),
                                        Err(e) => self.emit(AgentEvent::Warning { message: format!("heartbeat configuration: {e}") }),
                                    }
                                }
                                if let Some(build) = data.build {
                                    let key = build.key();
                                    if tasks.contains_key(&key) || recent.contains(&key) {
                                        self.emit(AgentEvent::DuplicateTask { task: key });
                                        continue;
                                    }
                                    let docker = build.docker_build_info.is_some();
                                    let advertised = match request.ask_enable.build {
                                        BuildJobType::All => true,
                                        BuildJobType::Binary => !docker,
                                        BuildJobType::Docker => docker,
                                        BuildJobType::None => false,
                                    };
                                    let rejection = if !advertised {
                                        Some("backend returned a task outside advertised capacity/capabilities".to_owned())
                                    } else { None };
                                    tasks.insert(key.clone(), build.clone());
                                    self.emit(AgentEvent::TaskStarted { task: key });
                                    let executor = self.executor.clone();
                                    let backend = self.backend.clone();
                                    let current = self.config.clone();
                                    let options = self.options.clone();
                                    let context = ExecutionContext { config: current.read().await.clone(),
                                        metadata: self.metadata.clone(), cancellation: shutdown.terminate.child_token() };
                                    jobs.spawn(async move {
                                        let outcome = if let Some(reason) = rejection { BuildOutcome::failure(reason) }
                                        else {
                                            match AssertUnwindSafe(executor.execute(build.clone(), context.clone())).catch_unwind().await {
                                                Ok(Ok(outcome)) => outcome,
                                                Ok(Err(error)) => BuildOutcome::failure(error.to_string()),
                                                Err(_) => BuildOutcome::failure("executor panicked"),
                                            }
                                        };
                                        if outcome.success {
                                            tokio::select! {
                                                _ = sleep(Duration::from_millis(options.success_report_delay_ms)) => {}
                                                _ = context.cancellation.cancelled() => {}
                                            }
                                        }
                                        let completion = BuildFinish { build, outcome };
                                        let mut report = Ok(());
                                        for attempt in 0..options.finish_attempts {
                                            let config = current.read().await.clone();
                                            report = backend.finish(&config, &completion).await.map_err(|e| e.to_string());
                                            if report.is_ok() || context.cancellation.is_cancelled() { break; }
                                            if attempt + 1 < options.finish_attempts {
                                                tokio::select! {
                                                    _ = sleep(Duration::from_millis(options.finish_retry_ms)) => {}
                                                    _ = context.cancellation.cancelled() => { break; }
                                                }
                                            }
                                        }
                                        (completion, report)
                                    });
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    fn heartbeat(
        &self,
        config: &AgentConfig,
        tasks: &HashMap<TaskKey, BuildInfo>,
        draining: bool,
    ) -> AskRequest {
        let normal: Vec<_> = tasks
            .values()
            .filter(|b| b.docker_build_info.is_none())
            .map(BuildInfo::task)
            .collect();
        let docker: Vec<_> = tasks
            .values()
            .filter(|b| b.docker_build_info.is_some())
            .map(BuildInfo::task)
            .collect();
        let normal_available = !draining
            && self.executor.supports_binary()
            && (config.parallel_task_count == 0
                || normal.len() < config.parallel_task_count as usize);
        let docker_available = !draining
            && self.executor.supports_docker()
            && (config.docker_parallel_task_count == 0
                || docker.len() < config.docker_parallel_task_count as usize);
        let build = match (normal_available, docker_available) {
            (true, true) => BuildJobType::All,
            (true, false) => BuildJobType::Binary,
            (false, true) => BuildJobType::Docker,
            _ => BuildJobType::None,
        };
        AskRequest {
            ask_enable: AskEnable {
                build,
                upgrade: false,
                docker_debug: false,
                pipeline: false,
            },
            upgrade: None,
            heartbeat: Heartbeat {
                master_version: self.metadata.agent_version.clone(),
                slave_version: self.metadata.worker_version.clone(),
                host_name: self.metadata.host_name.clone(),
                agent_ip: self.metadata.host_ip.clone(),
                parallel_task_count: config.parallel_task_count,
                docker_parallel_task_count: config.docker_parallel_task_count,
                agent_install_path: self.metadata.agent_install_path.clone(),
                started_user: self.metadata.started_user.clone(),
                task_list: normal,
                docker_task_list: docker,
                props: self.metadata.props.clone(),
                error_exit_data: None,
            },
        }
    }
}
