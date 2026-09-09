"""The extension protocol implemented by SDK consumers."""

from __future__ import annotations

from dataclasses import dataclass, field
from typing import Any, Awaitable, Mapping, Protocol

from .types import (
    AgentHeartbeatResponse,
    AgentPropsInfo,
    ImageDebug,
    ThirdPartyBuildInfo,
    ThirdPartyDockerTaskInfo,
    ThirdPartyTaskInfo,
    UpgradeInfo,
    UpgradeItem,
)


MaybeAwaitable = Awaitable[Any] | Any


@dataclass(slots=True)
class HeartbeatContext:
    task_list: list[ThirdPartyTaskInfo] = field(default_factory=list)
    docker_task_list: list[ThirdPartyDockerTaskInfo] = field(default_factory=list)

    @property
    def taskList(self) -> list[ThirdPartyTaskInfo]:  # noqa: N802
        """Node.js-compatible spelling for handlers shared during a migration."""
        return self.task_list

    @property
    def dockerTaskList(self) -> list[ThirdPartyDockerTaskInfo]:  # noqa: N802
        return self.docker_task_list


@dataclass(slots=True)
class StartupInfo:
    host_name: str
    host_ip: str
    detect_os: str
    master_version: str
    version: str


@dataclass(slots=True)
class HeartExtra:
    master_version: str
    slave_version: str
    host_name: str
    agent_ip: str
    agent_install_path: str
    started_user: str
    props: AgentPropsInfo
    upgrade: UpgradeInfo | None = None
    override: dict[str, Any] = field(default_factory=dict)


class AgentHandler(Protocol):
    def on_startup(self) -> StartupInfo | Mapping[str, Any] | Awaitable[Any]: ...

    def collect_heart_extra(
        self, context: HeartbeatContext, upgrade_enable: bool
    ) -> HeartExtra | Mapping[str, Any] | Awaitable[Any]: ...

    def is_upgrading(self) -> bool: ...

    def check_parallel_task_count(self) -> tuple[bool, bool]: ...

    def has_running_job(self) -> bool: ...

    def pipeline_enabled(self) -> bool: ...

    def docker_debug_enabled(self) -> bool: ...

    def on_build(self, build: ThirdPartyBuildInfo) -> MaybeAwaitable: ...

    def on_upgrade(self, upgrade: UpgradeItem, has_build: bool) -> MaybeAwaitable: ...

    def on_pipeline(self, pipeline: dict[str, Any]) -> MaybeAwaitable: ...

    def on_image_debug(self, debug: ImageDebug) -> MaybeAwaitable: ...

    def on_heartbeat_resp(self, response: AgentHeartbeatResponse) -> MaybeAwaitable: ...

    def on_agent_deleted(self) -> MaybeAwaitable: ...
