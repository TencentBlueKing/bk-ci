use crate::{config::normalize_gateway, protocol::*, AgentConfig, Error, Result};
use async_trait::async_trait;
use reqwest::{
    header::{HeaderMap, HeaderValue},
    Client,
};
use serde::{de::DeserializeOwned, Serialize};
use serde_json::Value;
use std::time::Duration;

pub const STARTUP_PATH: &str = "/ms/environment/api/buildAgent/agent/thirdPartyAgent/startup";
pub const ASK_PATH: &str = "/ms/dispatch/api/buildAgent/agent/thirdPartyAgent/ask";
pub const FINISH_PATH: &str = "/ms/dispatch/api/buildAgent/agent/thirdPartyAgent/workerBuildFinish";

#[async_trait]
pub trait Backend: Send + Sync {
    async fn startup(&self, config: &AgentConfig, info: &StartupInfo) -> Result<()>;
    async fn ask(&self, config: &AgentConfig, info: &AskRequest) -> Result<AgentResponse>;
    async fn finish(&self, config: &AgentConfig, info: &BuildFinish) -> Result<()>;
}
#[derive(Clone)]
pub struct HttpBackend {
    client: Client,
}
impl HttpBackend {
    pub fn new() -> Result<Self> {
        let client = Client::builder()
            .redirect(reqwest::redirect::Policy::none())
            .build()
            .map_err(|e| Error::Transport(e.without_url().to_string()))?;
        Ok(Self { client })
    }
    async fn post<T: DeserializeOwned, B: Serialize + Sync>(
        &self,
        config: &AgentConfig,
        path: &str,
        body: &B,
    ) -> Result<Envelope<T>> {
        config.validate()?;
        let mut headers = HeaderMap::new();
        for (key, value) in [
            ("X-DEVOPS-BUILD-TYPE", "AGENT"),
            ("X-DEVOPS-PROJECT-ID", config.project_id.as_str()),
            ("X-DEVOPS-AGENT-ID", config.agent_id.as_str()),
            ("X-DEVOPS-AGENT-SECRET-KEY", config.secret_key.as_str()),
        ] {
            let mut val = HeaderValue::from_str(value)
                .map_err(|_| Error::Config(format!("invalid {key} header")))?;
            if key.ends_with("SECRET-KEY") {
                val.set_sensitive(true);
            }
            headers.insert(
                reqwest::header::HeaderName::from_bytes(key.as_bytes()).unwrap(),
                val,
            );
        }
        let response = self
            .client
            .post(format!("{}{path}", normalize_gateway(&config.gateway)?))
            .headers(headers)
            .timeout(Duration::from_millis(config.request_timeout_ms))
            .json(body)
            .send()
            .await
            .map_err(|e| Error::Transport(e.without_url().to_string()))?;
        if !response.status().is_success() {
            return Err(Error::Http(response.status().as_u16()));
        }
        let result: Envelope<T> = response
            .json()
            .await
            .map_err(|_| Error::Protocol("invalid JSON response envelope".into()))?;
        if result.status != 0 {
            return Err(Error::Backend {
                status: result.status,
                message: result.message.replace(&config.secret_key, "[REDACTED]"),
            });
        }
        Ok(result)
    }
}
#[async_trait]
impl Backend for HttpBackend {
    async fn startup(&self, config: &AgentConfig, info: &StartupInfo) -> Result<()> {
        let response = self.post::<Value, _>(config, STARTUP_PATH, info).await?;
        if response.data.as_ref().and_then(Value::as_str) == Some("DELETE") {
            return Err(Error::Deleted);
        }
        Ok(())
    }
    async fn ask(&self, config: &AgentConfig, info: &AskRequest) -> Result<AgentResponse> {
        let response = self.post::<AskResponse, _>(config, ASK_PATH, info).await?;
        let agent_status = response
            .agent_status
            .ok_or_else(|| Error::Protocol("missing agentStatus".into()))?;
        Ok(AgentResponse {
            agent_status,
            data: response.data,
        })
    }
    async fn finish(&self, config: &AgentConfig, info: &BuildFinish) -> Result<()> {
        self.post::<Value, _>(config, FINISH_PATH, info).await?;
        Ok(())
    }
}
