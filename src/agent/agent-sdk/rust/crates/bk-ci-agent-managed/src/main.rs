//! One Base64 JSON argument; aihub owns process startup and termination.
#![forbid(unsafe_code)]
mod settings;

use bk_ci_agent_sdk::{protocol::BuildFinish, Agent, Error, HttpBackend, Shutdown};
use bk_ci_agent_worker::JavaWorker;
use clap::{error::ErrorKind, Parser};
use settings::{Cli, Parameters};
use std::{path::Path, process::ExitCode, sync::Arc};

#[tokio::main]
async fn main() -> ExitCode {
    let cli = match Cli::try_parse() {
        Ok(cli) => cli,
        Err(error) => {
            if matches!(
                error.kind(),
                ErrorKind::DisplayHelp | ErrorKind::DisplayVersion
            ) {
                let _ = error.print();
                return ExitCode::SUCCESS;
            }
            // clap can quote unexpected arguments; do not echo a Base64 credential payload.
            eprintln!("expected one Base64 JSON argument; use --help");
            return ExitCode::from(2);
        }
    };
    if settings::init_logging().is_err() {
        eprintln!("cannot initialize logging; check RUST_LOG");
        return ExitCode::from(2);
    }
    let params = match Parameters::decode(&cli.parameters) {
        Ok(params) => params,
        Err(message) => {
            tracing::error!(message, "invalid startup argument");
            return ExitCode::from(2);
        }
    };
    let data_dir = params.worker.data_dir.clone();
    let agent = async {
        params.config.validate()?;
        let worker = JavaWorker::new(params.worker)?;
        let metadata = worker.detect_metadata(env!("CARGO_PKG_VERSION")).await?;
        Agent::new(
            params.config,
            metadata,
            Arc::new(HttpBackend::new()?),
            Arc::new(worker),
            params.runtime,
        )
    }
    .await;
    let agent = match agent {
        Ok(agent) => agent,
        Err(error) => {
            tracing::error!(%error, "agent initialization failed");
            return ExitCode::from(2);
        }
    };
    // No stdin reader, IPC server or shutdown signal handler. The supervisor terminates us.
    match agent.run(Shutdown::new()).await {
        Ok(()) => ExitCode::SUCCESS,
        Err(Error::Unreported(completions)) => {
            // Keep recoverable completions without requiring a host IPC/event protocol.
            match retain_completions(&data_dir, &completions) {
                Ok(path) => {
                    tracing::error!(count = completions.len(), file = %path.display(), "completion reporting failed; retained locally")
                }
                Err(_) => tracing::error!(
                    count = completions.len(),
                    "completion reporting failed; local retention also failed"
                ),
            }
            ExitCode::FAILURE
        }
        Err(error) => {
            tracing::error!(%error, "agent stopped");
            ExitCode::FAILURE
        }
    }
}

/// A unique file in the private instance directory avoids overwriting earlier completions.
/// tempfile sets owner-only permissions on Unix; Windows inherits the directory ACL.
fn retain_completions(
    data_dir: &Path,
    completions: &[BuildFinish],
) -> std::io::Result<std::path::PathBuf> {
    use std::io::Write;
    let directory = data_dir.join("unreported");
    std::fs::create_dir_all(&directory)?;
    let mut file = tempfile::Builder::new()
        .prefix("finish-")
        .suffix(".json")
        .tempfile_in(directory)?;
    serde_json::to_writer(&mut file, completions)?;
    file.flush()?;
    file.as_file().sync_all()?;
    let (_, path) = file.keep()?;
    Ok(path)
}
