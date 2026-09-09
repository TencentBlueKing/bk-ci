"""Validated, atomic worker-agent.jar initialization and upgrade."""

from __future__ import annotations

import asyncio
from dataclasses import dataclass
import os
from pathlib import Path
import shutil
from typing import Callable, Mapping

from .download import WORK_AGENT_FILE, download_worker_jar, file_md5
from .worker import detect_worker_version


@dataclass(slots=True)
class WorkerJarState:
    version: str
    md5: str


@dataclass(slots=True)
class WorkerJarUpgradeResult(WorkerJarState):
    changed: bool
    not_modified: bool


class DefaultWorkerJarManager:
    def __init__(
        self,
        *,
        gateway: str,
        auth_headers: Mapping[str, str],
        work_dir: str | Path,
        jdk17_path: str | Path,
        worker_jar_path: str | Path | None = None,
        upgrade_dir: str | Path | None = None,
        timeout_seconds: float = 300,
        log_fn: Callable[[str], None] | None = None,
    ) -> None:
        self.gateway = gateway
        self.auth_headers = dict(auth_headers)
        self.work_dir = Path(work_dir).resolve()
        self.jdk17_path = jdk17_path
        self.worker_jar_path = Path(
            worker_jar_path or self.work_dir / WORK_AGENT_FILE
        ).resolve()
        self.upgrade_dir = Path(upgrade_dir or self.work_dir / "upgrade").resolve()
        self.timeout_seconds = timeout_seconds
        self.log = log_fn or (lambda message: None)
        self._version = ""
        self._active_upgrade: asyncio.Task[WorkerJarUpgradeResult] | None = None
        if self.upgrade_dir / WORK_AGENT_FILE == self.worker_jar_path:
            raise ValueError("upgrade_dir must not contain the active worker-agent.jar")

    def get_version(self) -> str:
        return self._version

    def get_worker_jar_path(self) -> str:
        return str(self.worker_jar_path)

    async def initialize(self) -> WorkerJarState:
        version = await self._detect_version(self.worker_jar_path, self.worker_jar_path.parent)
        if version:
            self._version = version
            md5 = await file_md5(self.worker_jar_path)
            self.log(f"worker initialized: version={version}, md5={md5}")
            return WorkerJarState(version, md5)
        self.log("active worker is missing or invalid, downloading the default worker-agent.jar")
        result = await self.upgrade()
        return WorkerJarState(result.version, result.md5)

    async def upgrade(self) -> WorkerJarUpgradeResult:
        if self._active_upgrade is None or self._active_upgrade.done():
            self._active_upgrade = asyncio.create_task(
                self._do_upgrade(), name="bkci-worker-upgrade"
            )
        return await asyncio.shield(self._active_upgrade)

    async def _do_upgrade(self) -> WorkerJarUpgradeResult:
        self.upgrade_dir.mkdir(parents=True, exist_ok=True)
        download = await download_worker_jar(
            self.gateway,
            self.auth_headers,
            self.upgrade_dir,
            self.timeout_seconds,
        )
        candidate = self.upgrade_dir / WORK_AGENT_FILE
        version = await self._detect_version(candidate, self.upgrade_dir)
        if not version:
            raise RuntimeError(f"downloaded worker version is invalid: {candidate}")
        current_md5 = await file_md5(self.worker_jar_path)
        changed = current_md5 != download.md5
        if changed:
            await _atomic_copy_file(candidate, self.worker_jar_path, download.md5)
            self.log(
                f"worker upgraded: version={version}, oldMd5={current_md5 or '<missing>'}, "
                f"newMd5={download.md5}"
            )
        else:
            self.log(f"worker already up-to-date: version={version}, md5={download.md5}")
        self._version = version
        return WorkerJarUpgradeResult(
            version=version,
            md5=download.md5,
            changed=changed,
            not_modified=download.not_modified,
        )

    async def _detect_version(self, worker_jar_path: Path, work_dir: Path) -> str:
        return await detect_worker_version(
            jdk17_path=self.jdk17_path,
            worker_jar_path=worker_jar_path,
            work_dir=work_dir,
            log_fn=self.log,
        )


async def _atomic_copy_file(source: Path, target: Path, expected_md5: str) -> None:
    target.parent.mkdir(parents=True, exist_ok=True)
    temporary = target.with_name(f"{target.name}.upgrade.{os.getpid()}.{id(asyncio.current_task())}")
    try:
        await asyncio.to_thread(shutil.copyfile, source, temporary)
        copied_md5 = await file_md5(temporary)
        if copied_md5 != expected_md5:
            raise RuntimeError(
                f"copied worker md5 not match: expected={expected_md5}, actual={copied_md5}"
            )
        await asyncio.to_thread(os.replace, temporary, target)
    finally:
        temporary.unlink(missing_ok=True)

