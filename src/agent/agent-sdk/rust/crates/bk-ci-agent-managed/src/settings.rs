use bk_ci_agent_sdk::{AgentConfig, AgentMetadata, RuntimeOptions};
use bk_ci_agent_worker::JavaWorkerOptions;
use clap::{Parser, ValueEnum};
use config::{Config, ConfigError, File, FileFormat};
use serde::Deserialize;
use serde_json::Value;
use std::path::{Path, PathBuf};
use tracing_subscriber::EnvFilter;

#[derive(Parser)]
#[command(
    version,
    about = "BK-CI agent subprocess: JSON-RPC 2.0 on stdin/stdout"
)]
pub(crate) struct Cli {
    /// Optional JSON/TOML initialization defaults; agent.initialize params take precedence.
    #[arg(long, value_name = "FILE")]
    pub config: Option<PathBuf>,
    /// tracing filter; CLI overrides RUST_LOG.
    #[arg(long, env = "RUST_LOG", default_value = "info")]
    pub log_filter: String,
    /// Agent logs go to stderr; stdout is reserved for protocol messages.
    #[arg(long, value_enum, default_value_t = LogFormat::Json)]
    pub log_format: LogFormat,
}

#[derive(Clone, Copy, ValueEnum)]
pub(crate) enum LogFormat {
    Json,
    Text,
}

pub(crate) fn init_logging(cli: &Cli) -> Result<(), Box<dyn std::error::Error + Send + Sync>> {
    let filter = EnvFilter::try_new(&cli.log_filter)?;
    let subscriber = tracing_subscriber::fmt()
        .with_env_filter(filter)
        .with_writer(std::io::stderr)
        .with_ansi(false);
    match cli.log_format {
        LogFormat::Json => subscriber.json().try_init()?,
        LogFormat::Text => subscriber.try_init()?,
    }
    Ok(())
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct Initialize {
    pub protocol_version: u32,
    pub config: AgentConfig,
    pub metadata: AgentMetadata,
    pub worker: JavaWorkerOptions,
    #[serde(default)]
    pub runtime: RuntimeOptions,
}

/// Snapshot optional defaults once at startup. No implicit file discovery or environment loading.
pub(crate) struct InitializationDefaults(Config);
impl InitializationDefaults {
    pub fn load(path: Option<&Path>) -> Result<Self, ConfigError> {
        let mut builder = Config::builder();
        if let Some(path) = path {
            builder = builder.add_source(File::from(path));
        }
        builder.build().map(Self)
    }

    pub fn resolve(&self, params: Value) -> Result<Initialize, ConfigError> {
        if !params.is_object() {
            return Err(ConfigError::Message(
                "initialization params must be an object".into(),
            ));
        }
        let merged: Value = Config::builder()
            .add_source(self.0.clone())
            .add_source(File::from_str(&params.to_string(), FileFormat::Json))
            .build()?
            .try_deserialize()?;
        // Keep the RPC's strict types: config's direct deserializer otherwise coerces strings/numbers.
        serde_json::from_value(merged).map_err(|error| ConfigError::Message(error.to_string()))
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use serde_json::json;

    fn params() -> Value {
        serde_json::from_str(include_str!("../../../examples/initialize.json")).unwrap()
    }

    #[test]
    fn initialization_preserves_strict_rpc_types() {
        let defaults = InitializationDefaults::load(None).unwrap();
        let mut data = params();
        data["config"]["parallelTaskCount"] = json!("4");
        assert!(defaults.resolve(data).is_err());
        assert!(defaults.resolve(Value::Null).is_err());
        assert!(defaults.resolve(json!([])).is_err());
        assert!(defaults.resolve(json!({})).is_err());
        assert_eq!(
            defaults
                .resolve(params())
                .unwrap()
                .config
                .parallel_task_count,
            4
        );
    }

    #[test]
    fn toml_defaults_are_a_snapshot_and_rpc_merges_nested_fields() {
        let dir = tempfile::tempdir().unwrap();
        let path = dir.path().join("defaults.toml");
        std::fs::write(
            &path,
            "[runtime]\npollIntervalMs = 123\nfinishRetryMs = 456\n",
        )
        .unwrap();
        let defaults = InitializationDefaults::load(Some(&path)).unwrap();
        // File replacement after startup must not affect an agent's initialization.
        std::fs::write(&path, "[runtime]\npollIntervalMs = 999\n").unwrap();
        let mut data = params();
        data["runtime"] = json!({"finishRetryMs":789});
        let result = defaults.resolve(data).unwrap();
        assert_eq!(result.runtime.poll_interval_ms, 123);
        assert_eq!(result.runtime.finish_retry_ms, 789);
        assert_eq!(
            result.runtime.startup_retry_ms,
            RuntimeOptions::default().startup_retry_ms
        );
    }
}
