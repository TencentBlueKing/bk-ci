"""Incremental and checksum-verified downloads for agent upgrade files."""

from __future__ import annotations

import asyncio
import hashlib
import os
from pathlib import Path
import shutil
import sys
import tempfile
from typing import Mapping, NamedTuple
from urllib.parse import urlencode

from .http_client import request_stream

DOWNLOAD_API_PATH = (
    "/ms/environment/api/buildAgent/agent/thirdPartyAgent/upgrade/files/download"
)
WORK_AGENT_FILE = "worker-agent.jar"
WORKER_JAR_SERVER_FILE = "jar/worker-agent.jar"
DOCKER_INIT_FILE = "agent_docker_init.sh"


class DownloadResult(NamedTuple):
    md5: str
    not_modified: bool


def _ensure_gateway(gateway: str) -> str:
    return gateway if gateway.startswith(("http://", "https://")) else "http://" + gateway


def _file_md5_sync(file_path: str | Path) -> str:
    digest = hashlib.md5()
    try:
        with Path(file_path).open("rb") as stream:
            for chunk in iter(lambda: stream.read(1024 * 1024), b""):
                digest.update(chunk)
    except FileNotFoundError:
        return ""
    return digest.hexdigest()


async def file_md5(file_path: str | Path) -> str:
    return await asyncio.to_thread(_file_md5_sync, file_path)


async def download_file(
    *,
    gateway: str,
    auth_headers: Mapping[str, str],
    server_file: str,
    save_path: str | Path,
    timeout_seconds: float = 300,
) -> DownloadResult:
    save = Path(save_path)
    old_md5 = await file_md5(save)
    query: dict[str, str] = {"file": server_file}
    if old_md5:
        query["eTag"] = old_md5
    url = f"{_ensure_gateway(gateway)}{DOWNLOAD_API_PATH}?{urlencode(query)}"
    response = await request_stream(
        method="GET", url=url, headers=auth_headers, timeout_seconds=timeout_seconds
    )

    if not 200 <= response.status < 300:
        await asyncio.to_thread(response.stream.close)
        if response.status == 404:
            raise FileNotFoundError("file not found")
        if response.status == 304:
            return DownloadResult(old_md5, True)
        raise RuntimeError(
            f"download file failed, status={response.status}, url={server_file}"
        )

    await asyncio.to_thread(save.parent.mkdir, parents=True, exist_ok=True)
    fd, temp_name = tempfile.mkstemp(prefix=save.name + ".tmp.", dir=save.parent)
    os.close(fd)
    temp_path = Path(temp_name)
    try:
        def copy_response() -> None:
            try:
                with temp_path.open("wb") as output:
                    shutil.copyfileobj(response.stream, output, 1024 * 1024)
            finally:
                response.stream.close()

        await asyncio.to_thread(copy_response)
        new_md5 = await file_md5(temp_path)
        checksum = (response.headers.get("X-Checksum-Md5") or "").strip()
        if checksum and checksum != new_md5:
            raise RuntimeError("file md5 not match")
        await asyncio.to_thread(os.chmod, temp_path, 0o755)
        await asyncio.to_thread(os.replace, temp_path, save)
        return DownloadResult(new_md5, False)
    finally:
        try:
            temp_path.unlink(missing_ok=True)
        except OSError:
            pass


async def download_worker_jar(
    gateway: str,
    auth_headers: Mapping[str, str],
    directory: str | Path,
    timeout_seconds: float = 300,
) -> DownloadResult:
    return await download_file(
        gateway=gateway,
        auth_headers=auth_headers,
        server_file=WORKER_JAR_SERVER_FILE,
        save_path=Path(directory) / WORK_AGENT_FILE,
        timeout_seconds=timeout_seconds,
    )


async def download_docker_init_file(
    gateway: str,
    auth_headers: Mapping[str, str],
    directory: str | Path,
    *,
    platform: str | None = None,
    timeout_seconds: float = 300,
) -> DownloadResult:
    target_platform = platform or sys.platform
    if target_platform == "darwin":
        server_file = "script/macos/agent_docker_init.sh"
    elif target_platform in {"win32", "cygwin"}:
        server_file = "script/windows/agent_docker_init.sh"
    else:
        server_file = "script/linux/agent_docker_init.sh"
    return await download_file(
        gateway=gateway,
        auth_headers=auth_headers,
        server_file=server_file,
        save_path=Path(directory) / DOCKER_INIT_FILE,
        timeout_seconds=timeout_seconds,
    )

