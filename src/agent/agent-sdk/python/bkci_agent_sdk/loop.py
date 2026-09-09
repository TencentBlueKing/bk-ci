"""Reusable agent startup, heartbeat, polling, and task-dispatch loop."""

from __future__ import annotations

import asyncio
import inspect
import logging
from dataclasses import asdict, is_dataclass
from typing import Any, Awaitable, Callable, Mapping

from .api import AgentApi
from .config import AgentConfig, AgentStatus
from .handler import AgentHandler, HeartExtra, HeartbeatContext, StartupInfo
from .http_client import HttpClient, is_agent_delete, is_not_ok
from .types import AgentHeartbeatInfo, AskEnable, AskResp, BuildJobType, UpgradeInfo


class AgentLoop:
    """Long-running orchestration shared by all third-party Python agents."""

    def __init__(
        self,
        *,
        config: AgentConfig,
        handler: AgentHandler,
        interval_seconds: float = 5,
        startup_retry_seconds: float = 5,
        logger: Any | None = None,
        http_client: HttpClient | None = None,
        monitor_fn: Callable[[], Any] | None = None,
        monitor_interval_seconds: float = 60,
        on_init: Callable[[], Any] | None = None,
    ) -> None:
        self.config = config
        self.handler = handler
        self.interval_seconds = interval_seconds
        self.startup_retry_seconds = startup_retry_seconds
        self.logger = logger or logging.getLogger("bkci_agent_sdk")
        self.api = AgentApi(config, http_client)
        self.monitor_fn = monitor_fn
        self.monitor_interval_seconds = monitor_interval_seconds
        self.on_init = on_init

        self._running = False
        self._stop_event = asyncio.Event()
        self._background_tasks: set[asyncio.Task[Any]] = set()
        self._monitor_task: asyncio.Task[Any] | None = None

    def get_api(self) -> AgentApi:
        return self.api

    @property
    def running(self) -> bool:
        return self._running

    async def run(self) -> None:
        if self._running:
            raise RuntimeError("AgentLoop already running")
        self._running = True
        self._stop_event.clear()
        try:
            await self._startup()
            if self._stop_event.is_set():
                return
            if self.monitor_fn is not None:
                self._monitor_task = asyncio.create_task(
                    self._monitor_loop(), name="bkci-agent-monitor"
                )
            if self.on_init is not None:
                await self._safe_call("on_init", self.on_init)

            while not self._stop_event.is_set():
                await self._do_ask()
                if not await self._wait_or_stop(self.interval_seconds):
                    break
        finally:
            if self._monitor_task is not None:
                self._monitor_task.cancel()
                await asyncio.gather(self._monitor_task, return_exceptions=True)
                self._monitor_task = None
            self._running = False

    def stop(self) -> None:
        self._stop_event.set()

    async def _startup(self) -> None:
        while not self._stop_event.is_set():
            try:
                info = await self._invoke_handler("on_startup", "onStartup")
                result = await self.api.agent_startup(
                    {
                        "hostname": _field(info, "host_name", "hostName"),
                        "hostIp": _field(info, "host_ip", "hostIp"),
                        "detectOS": _field(info, "detect_os", "detectOS"),
                        "masterVersion": _field(info, "master_version", "masterVersion"),
                        "version": _field(info, "version"),
                    }
                )
                if is_not_ok(result):
                    raise RuntimeError(f"agent startup result failed: {result.message}")
                self.logger.info("agent startup success")
                return
            except Exception as error:
                self.logger.error("agent startup failed: %s", _error_message(error))
                if not await self._wait_or_stop(self.startup_retry_seconds):
                    return

    async def _do_ask(self) -> None:
        try:
            enable = self._gen_ask_enable()
        except Exception as error:
            self.logger.error("build ask enable failed: %s", _error_message(error))
            return

        try:
            heartbeat, upgrade = await self._build_heartbeat(bool(enable["upgrade"]))
        except Exception as error:
            self.logger.error("build heartbeat failed: %s", _error_message(error))
            return

        try:
            result = await self.api.ask(
                {"askEnable": enable, "heartbeat": heartbeat, "upgrade": upgrade}
            )
        except Exception as error:
            self.logger.error("ask request failed: %s", _error_message(error))
            return

        if is_not_ok(result):
            self.logger.error("ask request result failed: %s", result.message)
            return
        if result.agent_status != AgentStatus.IMPORT_OK:
            self.logger.error("agent status [%s] not ok", result.agent_status)
            if is_agent_delete(result):
                self.logger.warning("agent has been deleted")
                await self._safe_handler_call(
                    "on_agent_deleted", "on_agent_deleted", "onAgentDeleted"
                )
            return

        response: AskResp = result.data if isinstance(result.data, dict) else {}
        ask_hook = _find_method(self.handler, "on_ask_resp", "onAskResp")
        if ask_hook is not None:
            await self._safe_call("on_ask_resp", lambda: ask_hook(response))
        self._do_agent_job(enable, response)

    def _do_agent_job(self, enable: AskEnable, response: AskResp) -> None:
        heartbeat = response.get("heartbeat")
        if heartbeat:
            self._safe_go(
                "on_heartbeat_resp",
                lambda: self._invoke_handler(
                    "on_heartbeat_resp", "onHeartbeatResp", args=(heartbeat,)
                ),
            )

        has_build = enable["build"] != BuildJobType.NONE and bool(response.get("build"))
        if has_build:
            build = response.get("build")
            if build is not None:
                self._safe_go(
                    "on_build",
                    lambda: self._invoke_handler("on_build", "onBuild", args=(build,)),
                )

        upgrade = response.get("upgrade")
        if enable["upgrade"] and upgrade:
            self._safe_go(
                "on_upgrade",
                lambda: self._invoke_handler(
                    "on_upgrade", "onUpgrade", args=(upgrade, has_build)
                ),
            )

        pipeline = response.get("pipeline")
        if enable["pipeline"] and pipeline:
            self._safe_go(
                "on_pipeline",
                lambda: self._invoke_handler(
                    "on_pipeline", "onPipeline", args=(pipeline,)
                ),
            )

        debug = response.get("debug")
        if enable["dockerDebug"] and debug:
            self._safe_go(
                "on_image_debug",
                lambda: self._invoke_handler(
                    "on_image_debug", "onImageDebug", args=(debug,)
                ),
            )

    def _gen_ask_enable(self) -> AskEnable:
        return {
            "build": self._check_build_type(),
            "upgrade": self._check_upgrade(),
            "dockerDebug": bool(
                _call_sync(self.handler, "docker_debug_enabled", "dockerDebugEnabled")
            ),
            "pipeline": bool(
                _call_sync(self.handler, "pipeline_enabled", "pipelineEnabled")
            ),
        }

    def _check_build_type(self) -> BuildJobType:
        if _call_sync(self.handler, "is_upgrading", "isUpgrading"):
            return BuildJobType.NONE
        docker_can_run, normal_can_run = _call_sync(
            self.handler, "check_parallel_task_count", "checkParallelTaskCount"
        )
        if not docker_can_run and not normal_can_run:
            return BuildJobType.NONE
        if docker_can_run and normal_can_run:
            return BuildJobType.ALL
        if normal_can_run:
            return BuildJobType.BINARY
        return BuildJobType.DOCKER

    def _check_upgrade(self) -> bool:
        if _call_sync(self.handler, "has_running_job", "hasRunningJob"):
            return False
        return not bool(_call_sync(self.handler, "is_upgrading", "isUpgrading"))

    async def _build_heartbeat(
        self, upgrade_enable: bool
    ) -> tuple[AgentHeartbeatInfo, UpgradeInfo | None]:
        extra = await self._invoke_handler(
            "collect_heart_extra",
            "collectHeartExtra",
            args=(HeartbeatContext(), upgrade_enable),
        )
        override = _field(extra, "override", default={}) or {}
        if not isinstance(override, Mapping):
            raise TypeError("HeartExtra.override must be a mapping")
        heartbeat: AgentHeartbeatInfo = {
            "masterVersion": _field(extra, "master_version", "masterVersion"),
            "slaveVersion": _field(extra, "slave_version", "slaveVersion"),
            "hostName": _field(extra, "host_name", "hostName"),
            "agentIp": _field(extra, "agent_ip", "agentIp"),
            "parallelTaskCount": self.config.parallel_task_count,
            "agentInstallPath": _field(extra, "agent_install_path", "agentInstallPath"),
            "startedUser": _field(extra, "started_user", "startedUser"),
            "taskList": list(override.get("taskList", [])),
            "props": _field(extra, "props"),
            "dockerParallelTaskCount": self.config.docker_parallel_task_count,
            "dockerTaskList": list(override.get("dockerTaskList", [])),
            "errorExitData": override.get("errorExitData"),
        }
        heartbeat.update(override)  # type: ignore[typeddict-item]
        upgrade = _field(extra, "upgrade", default=None) if upgrade_enable else None
        return heartbeat, upgrade

    async def _monitor_loop(self) -> None:
        while not self._stop_event.is_set():
            if self.monitor_fn is not None:
                await self._safe_call("monitor", self.monitor_fn)
            if not await self._wait_or_stop(self.monitor_interval_seconds):
                return

    def _safe_go(self, name: str, callback: Callable[[], Awaitable[Any] | Any]) -> None:
        task = asyncio.create_task(self._safe_call(name, callback), name=f"bkci-{name}")
        self._background_tasks.add(task)
        task.add_done_callback(self._background_tasks.discard)

    async def _safe_handler_call(
        self, name: str, snake_name: str, camel_name: str, *args: Any
    ) -> None:
        await self._safe_call(
            name, lambda: self._invoke_handler(snake_name, camel_name, args=args)
        )

    async def _safe_call(
        self, name: str, callback: Callable[[], Awaitable[Any] | Any]
    ) -> None:
        try:
            value = callback()
            if inspect.isawaitable(value):
                await value
        except Exception as error:
            self.logger.error("hook [%s] error: %s", name, _error_message(error))

    async def _invoke_handler(
        self, snake_name: str, camel_name: str, args: tuple[Any, ...] = ()
    ) -> Any:
        method = _find_method(self.handler, snake_name, camel_name)
        if method is None:
            raise AttributeError(f"handler does not implement {snake_name}")
        value = method(*args)
        return await value if inspect.isawaitable(value) else value

    async def _wait_or_stop(self, timeout: float) -> bool:
        try:
            await asyncio.wait_for(self._stop_event.wait(), timeout=max(0, timeout))
        except asyncio.TimeoutError:
            return True
        return False


def _find_method(target: Any, snake_name: str, camel_name: str) -> Callable[..., Any] | None:
    method = getattr(target, snake_name, None) or getattr(target, camel_name, None)
    return method if callable(method) else None


def _call_sync(target: Any, snake_name: str, camel_name: str) -> Any:
    method = _find_method(target, snake_name, camel_name)
    if method is None:
        raise AttributeError(f"handler does not implement {snake_name}")
    value = method()
    if inspect.isawaitable(value):
        raise TypeError(f"handler state method {snake_name} must be synchronous")
    return value


_MISSING = object()


def _field(value: Any, *names: str, default: Any = _MISSING) -> Any:
    if is_dataclass(value) and not isinstance(value, type):
        value = asdict(value)
    if isinstance(value, Mapping):
        for name in names:
            if name in value:
                return value[name]
    else:
        for name in names:
            if hasattr(value, name):
                return getattr(value, name)
    if default is not _MISSING:
        return default
    raise KeyError(f"missing required field: {'/'.join(names)}")


def _error_message(error: BaseException) -> str:
    return str(error)

