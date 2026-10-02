//! cargo run -p bk-ci-agent-worker --example embedded -- examples/initialize.json
use bk_ci_agent_sdk::{Agent, AgentConfig, AgentMetadata, HttpBackend, RuntimeOptions, Shutdown};
use bk_ci_agent_worker::{JavaWorker, JavaWorkerOptions};
use serde::Deserialize;
use std::sync::Arc;

#[derive(Deserialize)]
struct Parameters {
    config: AgentConfig,
    metadata: AgentMetadata,
    worker: JavaWorkerOptions,
    #[serde(default)]
    runtime: RuntimeOptions,
}
#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    let file = std::env::args()
        .nth(1)
        .ok_or("pass an initialization JSON file")?;
    let params: Parameters = serde_json::from_slice(&std::fs::read(file)?)?;
    let agent = Agent::new(
        params.config,
        params.metadata,
        Arc::new(HttpBackend::new()?),
        Arc::new(JavaWorker::new(params.worker)?),
        params.runtime,
    )?;
    let shutdown = Shutdown::new();
    let signal = shutdown.clone();
    let signal_task = tokio::spawn(async move {
        if tokio::signal::ctrl_c().await.is_ok() {
            signal.stop();
        }
    });
    let result = agent.run(shutdown).await;
    signal_task.abort();
    // Error::Unreported carries the exact completion payloads for host-side reconciliation.
    result?;
    Ok(())
}
