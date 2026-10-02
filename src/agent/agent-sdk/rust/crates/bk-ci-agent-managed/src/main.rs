//! JSON-RPC 2.0 over newline-delimited JSON. stdout is reserved for protocol messages.
#![forbid(unsafe_code)]
mod settings;
mod transport;

use bk_ci_agent_sdk::{Agent, AgentEvent, HttpBackend, Shutdown};
use bk_ci_agent_worker::JavaWorker;
use clap::{CommandFactory, Parser};
use futures_util::StreamExt;
use serde_json::{json, Value};
use settings::{Cli, InitializationDefaults};
use std::{io, process::ExitCode, sync::Arc};
use tokio::{io::AsyncWriteExt, sync::broadcast, task::JoinHandle};
use transport::input_channel;

#[tokio::main]
async fn main() -> ExitCode {
    let cli = Cli::parse();
    if settings::init_logging(&cli).is_err() {
        Cli::command()
            .error(
                clap::error::ErrorKind::InvalidValue,
                "invalid log filter or logging initialization failed",
            )
            .exit();
    }
    let defaults = match InitializationDefaults::load(cli.config.as_deref()) {
        Ok(defaults) => defaults,
        Err(_) => {
            // Parser diagnostics may contain secrets from a malformed configuration file.
            Cli::command()
                .error(
                    clap::error::ErrorKind::ValueValidation,
                    "cannot load configuration file; provide a readable JSON or TOML file",
                )
                .exit();
        }
    };
    match serve(defaults).await {
        Ok(code) => ExitCode::from(code),
        Err(error) => {
            tracing::error!(%error, "managed agent I/O failure");
            ExitCode::FAILURE
        }
    }
}

async fn send(value: &Value) -> io::Result<()> {
    let mut bytes = serde_json::to_vec(value)?;
    bytes.push(b'\n');
    let mut out = tokio::io::stdout();
    out.write_all(&bytes).await?;
    out.flush().await
}
async fn reply(id: Option<Value>, result: Value) -> io::Result<()> {
    if let Some(id) = id {
        send(&json!({"jsonrpc":"2.0","id":id,"result":result})).await?;
    }
    Ok(())
}
async fn error(id: Option<Value>, code: i64, message: &str) -> io::Result<()> {
    if let Some(id) = id {
        send(&json!({"jsonrpc":"2.0","id":id,"error":{"code":code,"message":message}})).await?;
    }
    Ok(())
}

async fn serve(defaults: InitializationDefaults) -> io::Result<u8> {
    let mut input = input_channel();
    let shutdown = Shutdown::new();
    let mut runner: Option<JoinHandle<bk_ci_agent_sdk::Result<()>>> = None;
    let mut events: Option<broadcast::Receiver<AgentEvent>> = None;
    let mut input_open = true;
    let mut signal_seen = false;

    loop {
        tokio::select! {
            biased;
            result = async { runner.as_mut().unwrap().await }, if runner.is_some() => {
                // Flush queued terminal events before announcing stopped.
                if let Some(rx) = &mut events {
                    while let Ok(event) = rx.try_recv() { notify(event).await?; }
                }
                let (success, message, unreported) = match result {
                    Ok(Ok(())) => (true, String::new(), Vec::new()),
                    Ok(Err(bk_ci_agent_sdk::Error::Unreported(completions))) =>
                        (false, "completion reporting failed".into(), completions),
                    Ok(Err(error)) => (false, error.to_string(), Vec::new()),
                    Err(_) => (false, "agent task failed".into(), Vec::new()),
                };
                send(&json!({"jsonrpc":"2.0","method":"agent.stopped","params":{
                    "success":success,"message":message,"unreportedCompletions":unreported
                }})).await?;
                return Ok(if success {0} else {1});
            }
            event = async { events.as_mut().unwrap().recv().await }, if events.is_some() => {
                match event {
                    Ok(event) => notify(event).await?,
                    Err(broadcast::error::RecvError::Lagged(count)) => {
                        send(&json!({"jsonrpc":"2.0","method":"agent.event","params":{"type":"eventsLost","count":count}})).await?;
                    }
                    Err(broadcast::error::RecvError::Closed) => events = None,
                }
            }
            frame = input.next(), if input_open => {
                let frame = match frame {
                    Some(Ok(frame)) => frame,
                    Some(Err(_)) => {
                        error(Some(Value::Null), -32700, "invalid UTF-8 or oversized frame").await?;
                        input_open = false;
                        shutdown.stop();
                        if runner.is_none() { return Ok(2); }
                        continue;
                    }
                    None => {
                        input_open = false;
                        shutdown.stop();
                        if runner.is_none() { return Ok(0); }
                        continue;
                    }
                };
                let request: Value = match serde_json::from_str(&frame) {
                    Ok(value) => value,
                    Err(_) => { error(Some(Value::Null), -32700, "invalid JSON").await?; continue; }
                };
                let id = request.get("id").cloned();
                if !request.is_object() || request.get("jsonrpc").and_then(Value::as_str) != Some("2.0")
                    || !request.get("method").is_some_and(Value::is_string)
                    || id.as_ref().is_some_and(|id| !(id.is_null() || id.is_string() || id.is_number())) {
                    error(Some(Value::Null), -32600, "invalid request").await?;
                    continue;
                }
                match request["method"].as_str().unwrap() {
                    "agent.initialize" => {
                        if runner.is_some() {
                            error(id, -32001, "agent is already initialized").await?;
                            continue;
                        }
                        let params = match defaults.resolve(request.get("params").cloned().unwrap_or(Value::Null)) {
                            Ok(params) => params,
                            Err(_) => { error(id, -32602, "invalid initialization parameters").await?; continue; }
                        };
                        if params.protocol_version != 1 {
                            error(id, -32602, "unsupported protocolVersion; expected 1").await?;
                            continue;
                        }
                        let agent = (|| {
                            let executor = JavaWorker::new(params.worker)?;
                            Agent::new(params.config, params.metadata, Arc::new(HttpBackend::new()?),
                                Arc::new(executor), params.runtime)
                        })();
                        match agent {
                            Ok(agent) => {
                                events = Some(agent.subscribe());
                                reply(id, json!({"protocolVersion":1,"initialized":true})).await?;
                                runner = Some(tokio::spawn(agent.run(shutdown.clone())));
                            }
                            Err(err) => error(id, -32602, &err.to_string()).await?,
                        }
                    }
                    "agent.shutdown" => {
                        if runner.is_none() { error(id, -32002, "agent is not initialized").await?; continue; }
                        let params = request.get("params");
                        if params.is_some_and(|p| !p.is_object())
                            || params.and_then(|p| p.get("mode")).is_some_and(|v| !v.is_string()) {
                            error(id, -32602, "shutdown params must be an object with a string mode").await?;
                            continue;
                        }
                        let mode = params.and_then(|p| p.get("mode")).and_then(Value::as_str).unwrap_or("drain");
                        match mode {
                            "drain" => shutdown.stop(),
                            "terminate" => shutdown.terminate(),
                            _ => { error(id, -32602, "mode must be drain or terminate").await?; continue; }
                        }
                        reply(id, json!({"accepted":true,"mode":mode})).await?;
                    }
                    _ => error(id, -32601, "method not found").await?,
                }
            }
            _ = tokio::signal::ctrl_c(), if !signal_seen => {
                signal_seen = true;
                shutdown.terminate();
                if runner.is_none() { return Ok(0); }
            }
        }
    }
}
async fn notify(event: AgentEvent) -> io::Result<()> {
    let method = if matches!(event, AgentEvent::Ready) {
        "agent.ready"
    } else {
        "agent.event"
    };
    send(&json!({"jsonrpc":"2.0","method":method,"params":event})).await
}
