"""BK-CI third-party agent API facade."""

from __future__ import annotations

from typing import Any, Mapping
from urllib.parse import urlencode

from .config import AgentConfig, AuthHeader
from .http_client import AgentResult, DevopsResult, HttpClient, is_not_ok
from .types import (
    AskInfo,
    AskResp,
    DevopsError,
    ImageDebug,
    LogMessage,
    PipelineResponse,
    RegistryParams,
    RegistryResponse,
    ThirdPartyAgentStartInfo,
    ThirdPartyBuildWithStatus,
)


class ApiPath:
    STARTUP = "/ms/environment/api/buildAgent/agent/thirdPartyAgent/startup"
    ASK = "/ms/dispatch/api/buildAgent/agent/thirdPartyAgent/ask"
    WORKER_BUILD_FINISH = (
        "/ms/dispatch/api/buildAgent/agent/thirdPartyAgent/workerBuildFinish"
    )
    PIPELINES = "/ms/environment/api/buildAgent/agent/thirdPartyAgent/agents/pipelines"
    UPGRADE = "/ms/dispatch/api/buildAgent/agent/thirdPartyAgent/upgrade"
    DOCKER_STARTUP_DEBUG = (
        "/ms/dispatch/api/buildAgent/agent/thirdPartyAgent/docker/startupDebug"
    )
    DOCKER_DEBUG_STATUS = (
        "/ms/dispatch/api/buildAgent/agent/thirdPartyAgent/docker/debug/status"
    )
    REGISTRY = "/ms/environment/api/external/thirdPartyAgent/registry"
    LOG = "/ms/log/api/build/logs"
    LOG_RED = "/ms/log/api/build/logs/red"
    LOG_YELLOW = "/ms/log/api/build/logs/yellow"


def _ensure_gateway(gateway: str) -> str:
    if gateway.startswith(("http://", "https://")):
        return gateway
    return "http://" + gateway


async def registry(
    gateway: str,
    params: RegistryParams | Mapping[str, str],
    client: HttpClient | None = None,
) -> RegistryResponse:
    http_client = client or HttpClient()
    query = urlencode(
        {
            "token": params.get("token", ""),
            "deviceId": params.get("deviceId", ""),
            "userId": params.get("userId", ""),
        }
    )
    result = await http_client.into_devops_result(
        method="POST", url=f"{_ensure_gateway(gateway)}{ApiPath.REGISTRY}?{query}"
    )
    if is_not_ok(result):
        raise RuntimeError(
            f"registry failed: status={result.status} message={result.message}"
        )
    if not isinstance(result.data, dict):
        raise RuntimeError("registry failed: empty data in response")
    return result.data  # type: ignore[return-value]


class AgentApi:
    def __init__(self, config: AgentConfig, client: HttpClient | None = None) -> None:
        self.config = config
        self.client = client or HttpClient(timeout_seconds=config.timeout_sec)

    def _url(self, path_and_query: str) -> str:
        return self.config.get_gateway() + path_and_query

    def _headers(self) -> dict[str, str]:
        return self.config.get_auth_header_map()

    async def ask(self, info: AskInfo) -> AgentResult[AskResp]:
        result = await self.client.into_agent_result(
            method="POST", url=self._url(ApiPath.ASK), headers=self._headers(), body=info
        )
        return result  # type: ignore[return-value]

    async def agent_startup(
        self, start_info: ThirdPartyAgentStartInfo
    ) -> DevopsResult[Any]:
        return await self.client.into_devops_result(
            method="POST",
            url=self._url(ApiPath.STARTUP),
            headers=self._headers(),
            body=start_info,
        )

    async def worker_build_finish(
        self, build_info: ThirdPartyBuildWithStatus
    ) -> DevopsResult[Any]:
        return await self.client.into_devops_result(
            method="POST",
            url=self._url(ApiPath.WORKER_BUILD_FINISH),
            headers=self._headers(),
            body=build_info,
        )

    async def update_pipeline_status(
        self, response: PipelineResponse
    ) -> DevopsResult[Any]:
        return await self.client.into_devops_result(
            method="PUT",
            url=self._url(ApiPath.PIPELINES),
            headers=self._headers(),
            body=response,
        )

    async def finish_upgrade(self, success: bool) -> AgentResult[Any]:
        return await self.client.into_agent_result(
            method="DELETE",
            url=self._url(f"{ApiPath.UPGRADE}?success={str(success).lower()}"),
            headers=self._headers(),
        )

    async def finish_docker_debug(
        self,
        image_debug: ImageDebug,
        success: bool,
        debug_url: str,
        error: DevopsError | None = None,
    ) -> DevopsResult[Any]:
        body = {
            "projectId": image_debug.get("projectId", ""),
            "debugId": image_debug.get("debugId", 0),
            "pipelineId": image_debug.get("pipelineId", ""),
            "debugUrl": debug_url,
            "success": success,
            "error": error,
        }
        return await self.client.into_devops_result(
            method="POST",
            url=self._url(ApiPath.DOCKER_STARTUP_DEBUG),
            headers=self._headers(),
            body=body,
        )

    async def fetch_docker_debug_status(self, debug_id: int) -> DevopsResult[Any]:
        return await self.client.into_devops_result(
            method="GET",
            url=self._url(f"{ApiPath.DOCKER_DEBUG_STATUS}?debugId={debug_id}"),
            headers=self._headers(),
        )

    async def add_log_line(
        self, build_id: str, message: LogMessage, vm_seq_id: str
    ) -> DevopsResult[Any]:
        return await self._post_log(ApiPath.LOG, build_id, message, vm_seq_id)

    async def add_log_red_line(
        self, build_id: str, message: LogMessage, vm_seq_id: str
    ) -> DevopsResult[Any]:
        return await self._post_log(ApiPath.LOG_RED, build_id, message, vm_seq_id)

    async def add_log_yellow_line(
        self, build_id: str, message: LogMessage, vm_seq_id: str
    ) -> DevopsResult[Any]:
        return await self._post_log(ApiPath.LOG_YELLOW, build_id, message, vm_seq_id)

    async def _post_log(
        self, path: str, build_id: str, message: LogMessage, vm_seq_id: str
    ) -> DevopsResult[Any]:
        headers = self._headers()
        headers[AuthHeader.BUILD_ID] = build_id
        headers[AuthHeader.VM_SEQ_ID] = vm_seq_id
        return await self.client.into_devops_result(
            method="POST", url=self._url(path), headers=headers, body=message
        )

