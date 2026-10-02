//! cargo run -p bk-ci-agent-worker --example embedded -- examples/initialize.json
use bk_ci_agent_sdk::{Agent, AgentConfig, HttpBackend, RuntimeOptions, Shutdown};
use bk_ci_agent_worker::{JavaWorker, JavaWorkerOptions};
use serde::Deserialize;
use std::sync::Arc;

#[derive(Deserialize)]
struct Parameters {
    config: AgentConfig,
    worker: JavaWorkerOptions,
    #[serde(default)]
    runtime: RuntimeOptions,
}
#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    let file = std::env::args()
        .nth(1)
        .ok_or("pass a configuration JSON file")?;
    let params: Parameters = serde_json::from_slice(&std::fs::read(file)?)?;
    let worker = JavaWorker::new(params.worker)?;
    let metadata = worker.detect_metadata(env!("CARGO_PKG_VERSION")).await?;
    let agent = Agent::new(
        params.config,
        metadata,
        Arc::new(HttpBackend::new()?),
        Arc::new(worker),
        params.runtime,
    )?;
    // Embedding hosts can still use the library's cooperative Shutdown API.
    agent.run(Shutdown::new()).await?;
    Ok(())
}
