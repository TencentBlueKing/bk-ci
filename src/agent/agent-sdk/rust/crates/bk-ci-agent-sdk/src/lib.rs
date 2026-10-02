//! Embeddable agent: the host owns configuration, the async runtime and process lifecycle.
//! The SDK owns task accounting and third-party agent protocol orchestration.
#![forbid(unsafe_code)]

pub mod client;
pub mod config;
mod metadata;
pub mod protocol;
pub mod runtime;

pub use async_trait::async_trait;
pub use client::{Backend, HttpBackend};
pub use config::{AgentConfig, AgentMetadata};
pub use protocol::{BuildInfo, BuildOutcome, TaskKey};
pub use runtime::{Agent, AgentEvent, ExecutionContext, Executor, RuntimeOptions, Shutdown};
pub use tokio_util::sync::CancellationToken;

#[derive(Debug, thiserror::Error)]
pub enum Error {
    #[error("invalid configuration: {0}")]
    Config(String),
    #[error("transport error: {0}")]
    Transport(String),
    #[error("HTTP status {0}")]
    Http(u16),
    #[error("backend status {status}: {message}")]
    Backend { status: i64, message: String },
    #[error("invalid backend response: {0}")]
    Protocol(String),
    #[error("I/O error: {0}")]
    Io(#[from] std::io::Error),
    #[error("agent registration has been deleted")]
    Deleted,
    #[error("completion reporting failed for {} task(s)", .0.len())]
    Unreported(Vec<protocol::BuildFinish>),
}
pub type Result<T> = std::result::Result<T, Error>;
