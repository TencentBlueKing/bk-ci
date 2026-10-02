use base64::{engine::general_purpose::STANDARD, Engine};
use bk_ci_agent_sdk::{AgentConfig, RuntimeOptions};
use bk_ci_agent_worker::JavaWorkerOptions;
use clap::Parser;
use serde::Deserialize;
use tracing_subscriber::EnvFilter;

#[derive(Parser)]
#[command(version, about = "BK-CI agent subprocess started and stopped by aihub")]
pub(crate) struct Cli {
    /// Standard Base64 of a UTF-8 JSON object containing config, worker and optional runtime.
    #[arg(value_name = "BASE64_JSON", hide_possible_values = true)]
    pub parameters: String,
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub(crate) struct Parameters {
    pub config: AgentConfig,
    pub worker: JavaWorkerOptions,
    #[serde(default)]
    pub runtime: RuntimeOptions,
}
impl Parameters {
    pub fn decode(encoded: &str) -> Result<Self, &'static str> {
        let decoded = STANDARD
            .decode(encoded)
            .map_err(|_| "argument must be standard Base64")?;
        // Never attach the input or serde's diagnostics: they may include credentials.
        serde_json::from_slice(&decoded)
            .map_err(|_| "decoded argument must be a valid agent configuration JSON object")
    }
}

pub(crate) fn init_logging() -> Result<(), Box<dyn std::error::Error + Send + Sync>> {
    let filter = match std::env::var("RUST_LOG") {
        Ok(value) => EnvFilter::try_new(value)?,
        Err(std::env::VarError::NotPresent) => EnvFilter::new("info"),
        Err(error) => return Err(error.into()),
    };
    tracing_subscriber::fmt()
        .with_env_filter(filter)
        .with_writer(std::io::stderr)
        .with_ansi(false)
        .json()
        .try_init()
}

#[cfg(test)]
mod tests {
    use super::*;
    use serde_json::{json, Value};

    #[test]
    fn accepts_one_encoded_configuration_without_metadata_or_protocol() {
        let params = Parameters::decode(
            &STANDARD.encode(include_bytes!("../../../examples/initialize.json")),
        )
        .unwrap();
        assert_eq!(params.config.parallel_task_count, 4);
        assert_eq!(params.runtime.poll_interval_ms, 5000);
    }

    #[test]
    fn rejects_malformed_payload_and_obsolete_fields_without_echoing_secrets() {
        let original: Value =
            serde_json::from_str(include_str!("../../../examples/initialize.json")).unwrap();
        for value in [
            Value::Null,
            json!([]),
            json!({}),
            {
                let mut value = original.clone();
                value["metadata"] = json!({"hostName":"forged"});
                value
            },
            {
                let mut value = original.clone();
                value["protocolVersion"] = json!(1);
                value
            },
            {
                let mut value = original;
                value["config"]["parallelTaskCount"] = json!("secret-as-number");
                value
            },
        ] {
            let error = Parameters::decode(&STANDARD.encode(value.to_string()))
                .err()
                .unwrap();
            assert!(!error.contains("secret-as-number"));
        }
        assert!(Parameters::decode("%").is_err());
        assert!(Parameters::decode(&STANDARD.encode([0xff])).is_err());
    }
}
