use crate::{
    protocol::{AgentProps, HeartbeatResponse, StartupInfo},
    Error, Result,
};
use serde::{Deserialize, Serialize};
use std::{collections::BTreeMap, fmt};

fn four() -> u32 {
    4
}
fn timeout() -> u64 {
    30_000
}
fn locale() -> String {
    "zh_CN".into()
}

#[derive(Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AgentConfig {
    pub gateway: String,
    #[serde(default)]
    pub file_gateway: String,
    pub project_id: String,
    pub agent_id: String,
    pub secret_key: String,
    #[serde(default = "four")]
    pub parallel_task_count: u32,
    #[serde(default = "four")]
    pub docker_parallel_task_count: u32,
    #[serde(default = "locale")]
    pub language: String,
    #[serde(default = "timeout")]
    pub request_timeout_ms: u64,
    #[serde(default)]
    pub envs: BTreeMap<String, String>,
}
impl fmt::Debug for AgentConfig {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.debug_struct("AgentConfig")
            .field("project_id", &self.project_id)
            .field("agent_id", &self.agent_id)
            .field("secret_key", &"[REDACTED]")
            .finish_non_exhaustive()
    }
}
impl AgentConfig {
    pub fn validate(&self) -> Result<()> {
        for (name, value) in [
            ("projectId", &self.project_id),
            ("agentId", &self.agent_id),
            ("secretKey", &self.secret_key),
        ] {
            if value.trim().is_empty() || value.contains(['\r', '\n']) {
                return Err(Error::Config(format!(
                    "{name} is empty or contains a newline"
                )));
            }
        }
        normalize_gateway(&self.gateway)?;
        if !self.file_gateway.is_empty() {
            normalize_gateway(&self.file_gateway)?;
        }
        if self.request_timeout_ms == 0 {
            return Err(Error::Config("requestTimeoutMs must be positive".into()));
        }
        Ok(())
    }
    pub fn apply_heartbeat(&mut self, h: &HeartbeatResponse) -> Result<()> {
        // Validate the entire update before publishing it to the loop and new executors.
        let mut next = self.clone();
        if let Some(n) = h.parallel_task_count {
            next.parallel_task_count = n;
        }
        if let Some(n) = h.docker_parallel_task_count {
            next.docker_parallel_task_count = n;
        }
        if let Some(v) = &h.gateway {
            if !v.is_empty() {
                next.gateway = normalize_gateway(v)?;
            }
        }
        if let Some(v) = &h.file_gateway {
            if !v.is_empty() {
                next.file_gateway = normalize_gateway(v)?;
            }
        }
        if let Some(v) = &h.language {
            if !v.is_empty() {
                next.language.clone_from(v);
            }
        }
        if let Some(v) = &h.envs {
            next.envs.clone_from(v);
        }
        next.validate()?;
        *self = next;
        Ok(())
    }
}
pub fn normalize_gateway(value: &str) -> Result<String> {
    let value = if value.contains("://") {
        value.to_owned()
    } else {
        format!("http://{value}")
    };
    let url =
        reqwest::Url::parse(&value).map_err(|_| Error::Config("invalid gateway URL".into()))?;
    if !matches!(url.scheme(), "http" | "https")
        || url.host_str().is_none()
        || !url.username().is_empty()
        || url.password().is_some()
        || url.query().is_some()
        || url.fragment().is_some()
    {
        return Err(Error::Config(
            "gateway must be an HTTP(S) URL without credentials, query or fragment".into(),
        ));
    }
    Ok(value.trim_end_matches('/').to_owned())
}

/// Host-provided metadata; no JDK discovery or machine monitoring is performed by the SDK.
#[derive(Clone, Debug, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AgentMetadata {
    pub host_name: String,
    pub host_ip: String,
    #[serde(rename = "detectOS")]
    pub detect_os: String,
    pub agent_version: String,
    pub worker_version: String,
    pub agent_install_path: String,
    pub started_user: String,
    #[serde(default)]
    pub props: AgentProps,
}
impl AgentMetadata {
    pub fn startup(&self) -> StartupInfo {
        StartupInfo {
            hostname: self.host_name.clone(),
            host_ip: self.host_ip.clone(),
            detect_os: self.detect_os.clone(),
            master_version: self.agent_version.clone(),
            version: self.worker_version.clone(),
        }
    }
}
