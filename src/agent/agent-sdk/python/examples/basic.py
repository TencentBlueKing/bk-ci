"""Minimal composition of AgentLoop, DefaultBuildRunner, and worker management."""

from __future__ import annotations

import asyncio
import getpass
import os
from pathlib import Path
import platform
import socket

from bkci_agent_sdk import (
    AgentApi,
    AgentConfig,
    AgentLoop,
    DefaultBuildRunner,
    DefaultWorkerJarManager,
    HeartExtra,
    HttpClient,
    StartupInfo,
)

AGENT_VERSION = "1.0.0-python-sdk"
WORK_DIR = Path.cwd()
JDK17_PATH = os.environ.get("BKCI_JDK17_PATH", "")


class DemoHandler:
    def __init__(self, runner, worker_manager, api):
        self.runner = runner
        self.worker_manager = worker_manager
        self.api = api
        self.upgrading = False

    def on_startup(self):
        return StartupInfo(
            host_name=socket.gethostname(),
            host_ip="127.0.0.1",
            detect_os=f"{platform.system()}_{platform.release()}",
            master_version=AGENT_VERSION,
            version=self.worker_manager.get_version(),
        )

    def collect_heart_extra(self, _context, upgrade_enable):
        worker_version = self.worker_manager.get_version()
        return HeartExtra(
            master_version=AGENT_VERSION,
            slave_version=worker_version,
            host_name=socket.gethostname(),
            agent_ip="127.0.0.1",
            agent_install_path=str(WORK_DIR),
            started_user=getpass.getuser(),
            props={
                "arch": platform.machine(),
                "jdkVersion": [],
                "dockerInitFileMd5": {"fileMd5": "", "needUpgrade": False},
                "osVersion": platform.release(),
            },
            override={
                "taskList": self.runner.get_task_list(),
                "dockerTaskList": self.runner.get_docker_task_list(),
            },
            upgrade=(
                {
                    "workerVersion": worker_version,
                    "goAgentVersion": AGENT_VERSION,
                    "jdkVersion": [],
                    "dockerInitFileInfo": {"fileMd5": "", "needUpgrade": False},
                }
                if upgrade_enable
                else None
            ),
        )

    def is_upgrading(self): return self.upgrading
    def check_parallel_task_count(self): return self.runner.check_parallel_task_count()
    def has_running_job(self): return self.runner.has_running_job()
    def pipeline_enabled(self): return False
    def docker_debug_enabled(self): return False

    async def on_build(self, build):
        await self.runner.on_build(build)

    async def on_upgrade(self, upgrade, has_build):
        if not upgrade.get("worker") or has_build or self.upgrading:
            return
        self.upgrading = True
        success = False
        try:
            await self.worker_manager.upgrade()
            success = True
        finally:
            self.upgrading = False
            await self.api.finish_upgrade(success)

    async def on_pipeline(self, pipeline): pass
    async def on_image_debug(self, debug): pass
    async def on_heartbeat_resp(self, response): pass
    async def on_agent_deleted(self): pass


async def main() -> None:
    config = AgentConfig.from_properties_file()
    api = AgentApi(config, HttpClient(timeout_seconds=config.timeout_sec))
    worker_manager = DefaultWorkerJarManager(
        gateway=config.get_gateway(),
        auth_headers=config.get_auth_header_map(),
        work_dir=WORK_DIR,
        jdk17_path=JDK17_PATH,
    )
    await worker_manager.initialize()
    runner = DefaultBuildRunner(
        api=api,
        work_dir=WORK_DIR,
        worker_jar_path=worker_manager.get_worker_jar_path(),
        jdk17_path=JDK17_PATH,
        gateway=config.get_gateway(),
        file_gateway=config.file_gateway,
        project_id=config.project_id,
        agent_id=config.agent_id,
        secret_key=config.secret_key,
        agent_version=AGENT_VERSION,
        worker_version=worker_manager.get_version,
        parallel_task_count=config.parallel_task_count,
        docker_parallel_task_count=config.docker_parallel_task_count,
    )
    loop = AgentLoop(
        config=config,
        handler=DemoHandler(runner, worker_manager, api),
    )
    await loop.run()


if __name__ == "__main__":
    asyncio.run(main())

