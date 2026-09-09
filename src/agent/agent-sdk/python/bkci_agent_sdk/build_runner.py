"""Optional default build dispatcher for physical and container builds."""

from __future__ import annotations

import asyncio
import inspect
from pathlib import Path
from typing import Any, Callable, Mapping

from .api import AgentApi
from .docker_build import DockerBuildOptions, run_docker_build
from .docker_cli import DockerLogEntry, DockerRunner
from .types import ThirdPartyBuildInfo, ThirdPartyDockerTaskInfo, ThirdPartyTaskInfo
from .worker import WorkerBuildOptions, run_worker_build


class DefaultBuildRunner:
    def __init__(
        self,
        *,
        api: AgentApi,
        work_dir: str | Path,
        worker_jar_path: str | Path,
        gateway: str,
        project_id: str,
        agent_version: str,
        worker_version: str | Callable[[], str],
        jdk17_path: str | Path | None = None,
        jdk8_path: str | Path | None = None,
        jdk17_dir_path: str | Path | None = None,
        jdk8_dir_path: str | Path | None = None,
        docker_init_script_path: str | Path | None = None,
        file_gateway: str = "",
        agent_id: str = "",
        secret_key: str = "",
        language: str = "zh_CN",
        parallel_task_count: int = 4,
        docker_parallel_task_count: int = 4,
        detect_shell: bool = False,
        extra_env: Mapping[str, str] | None = None,
        post_log: Callable[[ThirdPartyBuildInfo, str], Any] | None = None,
        log_fn: Callable[[str], None] | None = None,
        finish_delay_seconds: float = 8,
    ) -> None:
        self.api = api
        self.work_dir = Path(work_dir)
        self.worker_jar_path = Path(worker_jar_path)
        self.gateway = gateway
        self.project_id = project_id
        self.agent_version = agent_version
        self.worker_version = worker_version
        self.jdk17_path = jdk17_path
        self.jdk8_path = jdk8_path
        self.jdk17_dir_path = jdk17_dir_path
        self.jdk8_dir_path = jdk8_dir_path
        self.docker_init_script_path = docker_init_script_path
        self.file_gateway = file_gateway
        self.agent_id = agent_id
        self.secret_key = secret_key
        self.language = language
        self.parallel_task_count = parallel_task_count
        self.docker_parallel_task_count = docker_parallel_task_count
        self.detect_shell = detect_shell
        self.extra_env = dict(extra_env or {})
        self.post_log = post_log
        self.log = log_fn or (lambda message: None)
        self.finish_delay_seconds = finish_delay_seconds
        self._tasks: dict[str, ThirdPartyTaskInfo] = {}
        self._docker_tasks: dict[str, ThirdPartyDockerTaskInfo] = {}

    def get_task_list(self) -> list[ThirdPartyTaskInfo]:
        return list(self._tasks.values())

    def get_docker_task_list(self) -> list[ThirdPartyDockerTaskInfo]:
        return list(self._docker_tasks.values())

    def has_running_job(self) -> bool:
        return bool(self._tasks or self._docker_tasks)

    def check_parallel_task_count(self) -> tuple[bool, bool]:
        docker_can_run = self.docker_parallel_task_count == 0 or (
            len(self._docker_tasks) < self.docker_parallel_task_count
        )
        normal_can_run = self.parallel_task_count == 0 or (
            len(self._tasks) < self.parallel_task_count
        )
        return docker_can_run, normal_can_run

    async def on_build(self, build: ThirdPartyBuildInfo) -> None:
        if build.get("dockerBuildInfo"):
            await self._run_docker(build)
        else:
            await self._run_normal(build)

    async def _run_normal(self, build: ThirdPartyBuildInfo) -> None:
        build_id = str(build.get("buildId", ""))
        self._tasks[build_id] = {
            "projectId": str(build.get("projectId", "")),
            "buildId": build_id,
            "vmSeqId": str(build.get("vmSeqId", "")),
            "workspace": str(build.get("workspace", "")),
        }
        try:
            result = await run_worker_build(
                WorkerBuildOptions(
                    build_info=build,
                    work_dir=self.work_dir,
                    worker_jar_path=self.worker_jar_path,
                    gateway=self.gateway,
                    agent_version=self.agent_version,
                    worker_version=self._worker_version(),
                    jdk17_path=self.jdk17_path,
                    jdk8_path=self.jdk8_path,
                    file_gateway=self.file_gateway,
                    project_id=self.project_id,
                    agent_id=self.agent_id,
                    secret_key=self.secret_key,
                    language=self.language,
                    extra_env=self.extra_env,
                    detect_shell=self.detect_shell,
                    log_fn=self.log,
                )
            )
            await self._finish(build, result.success, result.message)
        except Exception as error:
            await self._finish(build, False, str(error))
        finally:
            self._tasks.pop(build_id, None)

    async def _run_docker(self, build: ThirdPartyBuildInfo) -> None:
        build_id = str(build.get("buildId", ""))
        self._docker_tasks[build_id] = {
            "projectId": str(build.get("projectId", "")),
            "buildId": build_id,
            "vmSeqId": str(build.get("vmSeqId", "")),
        }
        try:
            if self.docker_init_script_path is None:
                await self._finish(build, False, "dockerInitScriptPath not configured")
                return

            def docker_event(entry: DockerLogEntry) -> None:
                if self.post_log:
                    self._dispatch_callback(self.post_log(build, f"[docker] {entry.message}"))
                self.log(f"[docker][{entry.level}] {entry.message}")

            runner = DockerRunner(self.work_dir, docker_event)
            result = await run_docker_build(
                runner,
                DockerBuildOptions(
                    build_info=build,
                    work_dir=self.work_dir,
                    jdk17_dir_path=self.jdk17_dir_path,
                    jdk8_dir_path=self.jdk8_dir_path,
                    worker_jar_path=self.worker_jar_path,
                    docker_init_script_path=self.docker_init_script_path,
                    gateway=self.gateway,
                    project_id=self.project_id,
                    extra_env=self.extra_env,
                    post_log=(
                        (lambda message: self._dispatch_callback(self.post_log(build, message)))
                        if self.post_log
                        else None
                    ),
                ),
            )
            await self._finish(build, result.success, result.message)
        except Exception as error:
            await self._finish(build, False, str(error))
        finally:
            self._docker_tasks.pop(build_id, None)

    def _worker_version(self) -> str:
        return self.worker_version() if callable(self.worker_version) else self.worker_version

    async def _finish(self, build: ThirdPartyBuildInfo, success: bool, message: str) -> None:
        if success and self.finish_delay_seconds > 0:
            await asyncio.sleep(self.finish_delay_seconds)
        body = dict(build)
        body.update({"success": success, "message": message, "error": None})
        try:
            result = await self.api.worker_build_finish(body)  # type: ignore[arg-type]
            if result.status != 0:
                self.log(f"workerBuildFinish result not ok: {result.message}")
        except Exception as error:
            self.log(f"workerBuildFinish failed: {error}")

    @staticmethod
    def _dispatch_callback(value: Any) -> None:
        if inspect.isawaitable(value):
            asyncio.create_task(value)
