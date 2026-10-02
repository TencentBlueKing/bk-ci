use serde::{Deserialize, Serialize};
use serde_json::{Map, Value};
use std::collections::BTreeMap;

#[derive(Clone, Copy, Debug, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "SCREAMING_SNAKE_CASE")]
pub enum BuildJobType {
    All,
    Docker,
    Binary,
    None,
}

/// Preserve unknown build fields when forwarding to worker and reporting completion.
#[derive(Clone, Debug, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct BuildInfo {
    pub project_id: String,
    pub build_id: String,
    pub vm_seq_id: String,
    #[serde(default)]
    pub workspace: String,
    #[serde(default)]
    pub pipeline_id: Option<String>,
    #[serde(default)]
    pub execute_count: Option<u32>,
    #[serde(default)]
    pub container_hash_id: Option<String>,
    #[serde(default)]
    pub docker_build_info: Option<Value>,
    #[serde(flatten)]
    pub extra: Map<String, Value>,
}

#[derive(Clone, Debug, PartialEq, Eq, Hash, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct TaskKey {
    pub project_id: String,
    pub build_id: String,
    pub vm_seq_id: String,
    pub execute_count: Option<u32>,
}
impl BuildInfo {
    pub fn key(&self) -> TaskKey {
        TaskKey {
            project_id: self.project_id.clone(),
            build_id: self.build_id.clone(),
            vm_seq_id: self.vm_seq_id.clone(),
            execute_count: self.execute_count,
        }
    }
    pub fn task(&self) -> TaskInfo {
        TaskInfo {
            project_id: self.project_id.clone(),
            build_id: self.build_id.clone(),
            vm_seq_id: self.vm_seq_id.clone(),
            workspace: self.workspace.clone(),
        }
    }
}
#[derive(Clone, Debug, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct TaskInfo {
    pub project_id: String,
    pub build_id: String,
    pub vm_seq_id: String,
    pub workspace: String,
}

#[derive(Clone, Debug, Serialize, Deserialize)]
pub struct BuildOutcome {
    pub success: bool,
    pub message: String,
    pub error: Option<Value>,
}
impl BuildOutcome {
    pub fn success(message: impl Into<String>) -> Self {
        Self {
            success: true,
            message: message.into(),
            error: None,
        }
    }
    pub fn failure(message: impl Into<String>) -> Self {
        Self {
            success: false,
            message: message.into(),
            error: None,
        }
    }
}
#[derive(Clone, Debug, Serialize, Deserialize)]
pub struct BuildFinish {
    #[serde(flatten)]
    pub build: BuildInfo,
    #[serde(flatten)]
    pub outcome: BuildOutcome,
}
#[derive(Clone, Debug, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct StartupInfo {
    pub hostname: String,
    pub host_ip: String,
    #[serde(rename = "detectOS")]
    pub detect_os: String,
    pub master_version: String,
    pub version: String,
}
#[derive(Clone, Debug, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AgentProps {
    pub arch: String,
    pub jdk_version: Vec<String>,
    pub docker_init_file_md5: DockerInitFileInfo,
    pub os_version: String,
}
#[derive(Clone, Debug, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct DockerInitFileInfo {
    pub file_md5: String,
    pub need_upgrade: bool,
}

#[derive(Clone, Debug, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Heartbeat {
    pub master_version: String,
    pub slave_version: String,
    pub host_name: String,
    pub agent_ip: String,
    pub parallel_task_count: u32,
    pub agent_install_path: String,
    pub started_user: String,
    pub task_list: Vec<TaskInfo>,
    pub props: AgentProps,
    pub docker_parallel_task_count: u32,
    pub docker_task_list: Vec<TaskInfo>,
    pub error_exit_data: Option<Value>,
}
#[derive(Clone, Debug, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AskEnable {
    pub build: BuildJobType,
    pub upgrade: bool,
    pub docker_debug: bool,
    pub pipeline: bool,
}
#[derive(Clone, Debug, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AskRequest {
    pub ask_enable: AskEnable,
    pub heartbeat: Heartbeat,
    pub upgrade: Option<Value>,
}
#[derive(Clone, Debug, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct HeartbeatResponse {
    #[serde(default)]
    pub parallel_task_count: Option<u32>,
    #[serde(default)]
    pub docker_parallel_task_count: Option<u32>,
    #[serde(default)]
    pub gateway: Option<String>,
    #[serde(default)]
    pub file_gateway: Option<String>,
    #[serde(default)]
    pub language: Option<String>,
    #[serde(default)]
    pub envs: Option<BTreeMap<String, String>>,
}
#[derive(Clone, Debug, Default, Serialize, Deserialize)]
pub struct AskResponse {
    #[serde(default)]
    pub heartbeat: Option<HeartbeatResponse>,
    #[serde(default)]
    pub build: Option<BuildInfo>,
}
#[derive(Clone, Debug, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AgentResponse {
    pub agent_status: String,
    #[serde(default)]
    pub data: Option<AskResponse>,
}

#[derive(Clone, Debug, Deserialize)]
pub struct Envelope<T> {
    pub status: i64,
    #[serde(default)]
    pub message: String,
    pub data: Option<T>,
    #[serde(default, rename = "agentStatus")]
    pub agent_status: Option<String>,
}
