//! Default executor for the existing Java worker. Java/JAR installation is owned by the host.
#![forbid(unsafe_code)]

use async_trait::async_trait;
use base64::{engine::general_purpose::STANDARD, Engine};
use bk_ci_agent_sdk::{BuildInfo, BuildOutcome, Error, ExecutionContext, Executor, Result};
use serde::{Deserialize, Serialize};
use std::{collections::BTreeMap, ffi::OsString, path::PathBuf, process::Stdio};
use tokio::{fs, process::Command};

fn heap() -> String {
    "2g".into()
}

#[derive(Clone, Debug, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct JavaWorkerOptions {
    /// Exact executable path supplied by the host. No PATH search or JDK fallback.
    pub java_executable: PathBuf,
    pub worker_jar: PathBuf,
    /// Private writable directory for this agent instance; retained run directories contain logs.
    pub data_dir: PathBuf,
    #[serde(default = "heap")]
    pub max_heap: String,
    #[serde(default)]
    pub environment: BTreeMap<String, String>,
}

pub struct JavaWorker {
    options: JavaWorkerOptions,
}
impl JavaWorker {
    pub fn new(options: JavaWorkerOptions) -> Result<Self> {
        for (name, path) in [
            ("javaExecutable", &options.java_executable),
            ("workerJar", &options.worker_jar),
            ("dataDir", &options.data_dir),
        ] {
            if !path.is_absolute() {
                return Err(Error::Config(format!("{name} must be an absolute path")));
            }
        }
        for (name, path) in [
            ("javaExecutable", &options.java_executable),
            ("workerJar", &options.worker_jar),
        ] {
            if !path.is_file() {
                return Err(Error::Config(format!(
                    "{name} must reference an existing file"
                )));
            }
        }
        if options.max_heap.is_empty()
            || !options.max_heap.chars().all(|c| c.is_ascii_alphanumeric())
        {
            return Err(Error::Config(
                "maxHeap must be a JVM heap size, such as 2g".into(),
            ));
        }
        Ok(Self { options })
    }

    async fn prepare(&self, build: &BuildInfo, context: &ExecutionContext) -> Result<Prepared> {
        let runs = self.options.data_dir.join("runs");
        fs::create_dir_all(&runs).await?;
        // No untrusted build ID is used as a filesystem path. Each execution gets its own cwd
        // and immutable worker credentials so concurrent config updates cannot race.
        let run_dir = tempfile::Builder::new()
            .prefix("worker-")
            .tempdir_in(&runs)?
            .keep();
        let tmp = run_dir.join("build_tmp");
        fs::create_dir_all(&tmp).await?;
        let config = &context.config;
        let properties = [
            ("devops.project.id", config.project_id.as_str()),
            ("devops.agent.id", config.agent_id.as_str()),
            ("devops.agent.secret.key", config.secret_key.as_str()),
            ("landun.gateway", config.gateway.as_str()),
            ("landun.fileGateway", config.file_gateway.as_str()),
        ]
        .into_iter()
        .map(|(k, v)| format!("{k}={}\n", escape_property(v)))
        .collect::<String>();
        let property_path = run_dir.join(".agent.properties");
        let mut opts = fs::OpenOptions::new();
        opts.write(true).create_new(true);
        #[cfg(unix)]
        opts.mode(0o600);
        let mut file = opts.open(&property_path).await?;
        use tokio::io::AsyncWriteExt;
        file.write_all(properties.as_bytes()).await?;
        file.flush().await?;
        drop(file);

        let error_file = tmp.join("worker-error.log");
        fs::write(&error_file, "build process was killed").await?;
        let mut effective_build = build.clone();
        if effective_build.workspace.is_empty() {
            // Business-node workspace resolution belongs to environment/dispatch.
            // Keep the existing worker fallback rooted at the supplied instance directory.
            let pipeline = effective_build.pipeline_id.as_deref().unwrap_or("default");
            let pipeline = safe_component(pipeline);
            effective_build.workspace = self
                .options
                .data_dir
                .join("workspace")
                .join(pipeline)
                .join("src")
                .to_string_lossy()
                .into_owned();
        }
        let encoded = STANDARD.encode(
            serde_json::to_vec(&effective_build).map_err(|e| Error::Protocol(e.to_string()))?,
        );
        let args: Vec<OsString> = vec![
            format!("-Djava.io.tmpdir={}", tmp.display()).into(),
            format!("-Ddevops.agent.error.file={}", error_file.display()).into(),
            "-Dbuild.type=AGENT".into(),
            format!(
                "-DAGENT_LOG_PREFIX={}_{}_agent",
                safe_component(&build.build_id),
                safe_component(&build.vm_seq_id)
            )
            .into(),
            format!("-Xmx{}", self.options.max_heap).into(),
            "-jar".into(),
            self.options.worker_jar.as_os_str().to_owned(),
            encoded.into(),
        ];
        let mut env = BTreeMap::new();
        for (key, value) in self.options.environment.iter().chain(config.envs.iter()) {
            let key = if cfg!(windows) {
                key.to_ascii_uppercase()
            } else {
                key.clone()
            };
            env.insert(key, value.clone());
        }
        // Protocol-owned fields take precedence over arbitrary host/backend environment entries.
        for (key, value) in [
            (
                "DEVOPS_AGENT_VERSION",
                context.metadata.agent_version.as_str(),
            ),
            (
                "DEVOPS_WORKER_VERSION",
                context.metadata.worker_version.as_str(),
            ),
            (
                "DEVOPS_SLAVE_VERSION",
                context.metadata.worker_version.as_str(),
            ),
            ("DEVOPS_PROJECT_ID", build.project_id.as_str()),
            ("PROJECT_ID", build.project_id.as_str()),
            ("DEVOPS_BUILD_ID", build.build_id.as_str()),
            ("BUILD_ID", build.build_id.as_str()),
            ("DEVOPS_VM_SEQ_ID", build.vm_seq_id.as_str()),
            ("VM_SEQ_ID", build.vm_seq_id.as_str()),
            ("DEVOPS_GATEWAY", config.gateway.as_str()),
            ("DEVOPS_FILE_GATEWAY", config.file_gateway.as_str()),
            ("BK_CI_LOCALE_LANGUAGE", config.language.as_str()),
        ] {
            env.insert(key.into(), value.into());
        }
        env.insert(
            "DEVOPS_AGENT_JDK_17_PATH".into(),
            self.options.java_executable.to_string_lossy().into_owned(),
        );
        // Optional additional JDK variables can be supplied by the host in environment.
        Ok(Prepared {
            run_dir,
            error_file,
            args,
            env,
        })
    }
}
struct Prepared {
    run_dir: PathBuf,
    error_file: PathBuf,
    args: Vec<OsString>,
    env: BTreeMap<String, String>,
}

#[async_trait]
impl Executor for JavaWorker {
    async fn execute(&self, build: BuildInfo, context: ExecutionContext) -> Result<BuildOutcome> {
        if build.docker_build_info.is_some() {
            return Ok(BuildOutcome::failure(
                "JavaWorker does not support Docker tasks",
            ));
        }
        if context.cancellation.is_cancelled() {
            return Ok(BuildOutcome::failure("execution cancelled before launch"));
        }
        let prepared = self.prepare(&build, &context).await?;
        let stdout = fs::File::create(prepared.run_dir.join("stdout.log"))
            .await?
            .into_std()
            .await;
        let stderr = fs::File::create(prepared.run_dir.join("stderr.log"))
            .await?
            .into_std()
            .await;
        let mut command = Command::new(&self.options.java_executable);
        command
            .args(&prepared.args)
            .current_dir(&prepared.run_dir)
            .envs(&prepared.env)
            .stdin(Stdio::null())
            .stdout(stdout)
            .stderr(stderr)
            .kill_on_drop(true);
        #[cfg(windows)]
        command.creation_flags(0x08000000); // CREATE_NO_WINDOW; never pop up a console.
        if context.cancellation.is_cancelled() {
            return Ok(BuildOutcome::failure("execution cancelled before launch"));
        }
        let mut child = match command.spawn() {
            Ok(child) => child,
            Err(error) => {
                return Ok(BuildOutcome::failure(format!(
                    "worker launch failed: {error}"
                )))
            }
        };
        tracing::debug!(task = ?build.key(), pid = child.id(), run_dir = %prepared.run_dir.display(), "worker launched");
        let status = tokio::select! {
            biased;
            _ = context.cancellation.cancelled() => {
                // The supervisor owns whole-process-tree termination; this executor kills and
                // reaps its direct worker. Never leave a live child behind on a normal return.
                child.kill().await?;
                tracing::debug!(task = ?build.key(), "worker cancelled and reaped");
                return Ok(BuildOutcome::failure("worker execution cancelled"));
            }
            status = child.wait() => status?,
        };
        tracing::debug!(task = ?build.key(), %status, "worker exited");
        let message = match fs::read_to_string(&prepared.error_file).await {
            Ok(v) => v.trim().to_owned(),
            Err(e) if e.kind() == std::io::ErrorKind::NotFound => String::new(),
            Err(e) => return Err(e.into()),
        };
        // Match Go's #10362 behavior: Java's error marker is authoritative. The JVM can exit
        // nonzero after reporting successful pipeline completion.
        if message.is_empty() {
            Ok(BuildOutcome::success(format!(
                "worker process exit ({status})"
            )))
        } else {
            Ok(BuildOutcome::failure(format!("{message} ({status})")))
        }
    }
}

fn safe_component(value: &str) -> String {
    let value: String = value
        .chars()
        .take(96)
        .map(|c| {
            if c.is_ascii_alphanumeric() || c == '-' || c == '_' {
                c
            } else {
                '_'
            }
        })
        .collect();
    if value.is_empty() {
        "default".into()
    } else {
        value
    }
}

/// Compatibility exception to using general-purpose crates: java-properties 2.0.0 writes
/// non-BMP characters as a single \\uXXXXX escape and uses Windows-1252 rather than ISO-8859-1.
/// Keep this small UTF-16 encoder until a compatible writer is available. The real JVM test
/// covers supplementary characters, Latin-1 boundaries, controls and property separators.
fn escape_property(value: &str) -> String {
    let mut result = String::new();
    for c in value.encode_utf16() {
        match c {
            0x5c => result.push_str("\\\\"),
            0xa => result.push_str("\\n"),
            0xd => result.push_str("\\r"),
            0x9 => result.push_str("\\t"),
            0xc => result.push_str("\\f"),
            0x20 | 0x3d | 0x3a | 0x23 | 0x21 => {
                result.push('\\');
                result.push(char::from_u32(c as u32).unwrap());
            }
            0x21..=0x7e => result.push(char::from_u32(c as u32).unwrap()),
            _ => result.push_str(&format!("\\u{c:04x}")),
        }
    }
    result
}

#[cfg(test)]
mod tests {
    use super::*;
    #[test]
    fn java_properties_preserves_unicode_and_prevents_property_injection() {
        assert_eq!(
            escape_property(" a=b\\c\n中😀€Ā\u{0001}"),
            "\\ a\\=b\\\\c\\n\\u4e2d\\ud83d\\ude00\\u20ac\\u0100\\u0001"
        );
    }

    #[test]
    fn identifiers_cannot_escape_work_directory() {
        assert_eq!(safe_component("../../evil"), "______evil");
        assert_eq!(safe_component(""), "default");
    }
}
