//! One-time local identity discovery using OS-backed crates; no monitoring loop.
use crate::{protocol::AgentProps, AgentMetadata, Error, Result};
use sysinfo::System;

impl AgentMetadata {
    /// Detect machine identity locally. Versions identify the caller binary and supplied worker.
    pub fn detect(
        agent_version: impl Into<String>,
        worker_version: impl Into<String>,
    ) -> Result<Self> {
        let executable = std::env::current_exe()?;
        let directory = executable
            .parent()
            .ok_or_else(|| Error::Config("cannot locate executable directory".into()))?;
        let host_name =
            System::host_name().ok_or_else(|| Error::Config("cannot detect host name".into()))?;
        let started_user =
            whoami::username().map_err(|_| Error::Config("cannot detect current user".into()))?;
        let host_ip = local_ip_address::local_ip()
            .or_else(|_| local_ip_address::local_ipv6())
            .unwrap_or_else(|_| {
                tracing::warn!("cannot detect local IP; using loopback");
                std::net::Ipv4Addr::LOCALHOST.into()
            })
            .to_string();
        Ok(Self {
            host_name,
            host_ip,
            detect_os: match std::env::consts::OS {
                "macos" => "macos",
                "windows" => "windows",
                "linux" => "linux",
                _ => "other",
            }
            .into(),
            agent_version: agent_version.into(),
            worker_version: worker_version.into(),
            agent_install_path: directory.to_string_lossy().into_owned(),
            started_user,
            props: AgentProps {
                arch: match std::env::consts::ARCH {
                    "x86_64" => "amd64",
                    "aarch64" => "arm64",
                    "x86" => "386",
                    arch => arch,
                }
                .into(),
                os_version: System::long_os_version()
                    .or_else(System::os_version)
                    .unwrap_or_default(),
                // aihub supplies the JDK path; no installed-JDK or Docker discovery is performed.
                ..AgentProps::default()
            },
        })
    }
}
