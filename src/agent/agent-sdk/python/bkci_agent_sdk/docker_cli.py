"""Dependency-free Docker/Podman CLI runner."""

from __future__ import annotations

import asyncio
from dataclasses import dataclass
import logging
import os
from pathlib import Path
import shutil
import tempfile
from typing import Callable, Literal

DEVOPS_AGENT_CONTAINER_RUNTIME = "DEVOPS_AGENT_CONTAINER_RUNTIME"
DockerLogLevel = Literal["DEBUG", "INFO", "WARN", "ERROR"]


@dataclass(slots=True)
class DockerLogEntry:
    level: DockerLogLevel
    message: str


DockerEventLogFn = Callable[[DockerLogEntry], None]


@dataclass(slots=True)
class _RunResult:
    stdout: str
    stderr: str
    error: str | None


def runtime_binary() -> str:
    return os.environ.get(DEVOPS_AGENT_CONTAINER_RUNTIME, "").strip() or "docker"


class DockerRunner:
    def __init__(
        self,
        work_dir: str | Path,
        event_fn: DockerEventLogFn | None = None,
        binary: str | None = None,
    ) -> None:
        self.work_dir = str(work_dir)
        self.binary = binary or runtime_binary()
        self.event_fn = event_fn

    def binary_name(self) -> str:
        return self.binary

    async def server_os(self) -> str:
        result = await self._run(None, ["version", "--format", "{{.Server.Os}}"])
        return result.stdout.strip()

    async def image_exists(self, image: str) -> bool:
        return (await self._run(None, ["image", "inspect", image])).error is None

    async def pull_image(self, image: str, user: str, password: str) -> str:
        if not user or not password:
            result = await self._run(None, ["pull", image])
            if result.error:
                raise RuntimeError(result.error)
            return result.stdout + result.stderr
        config_dir = Path(tempfile.mkdtemp(prefix="bkci-docker-config-"))
        try:
            registry = registry_from_image(image)
            login_args = [
                "--config",
                str(config_dir),
                "login",
                "-u",
                user,
                "--password-stdin",
            ]
            if registry:
                login_args.append(registry)
            login = await self._run(password.encode(), login_args)
            if login.error:
                raise RuntimeError(login.error)
            pull = await self._run(None, ["--config", str(config_dir), "pull", image])
            if pull.error:
                raise RuntimeError(pull.error)
            return pull.stdout + pull.stderr
        finally:
            await asyncio.to_thread(shutil.rmtree, config_dir, ignore_errors=True)

    async def create_container(self, args: list[str]) -> str:
        result = await self._run(None, ["create", *args])
        if result.error:
            raise RuntimeError(result.error)
        lines = result.stdout.strip().splitlines()
        container_id = lines[-1].strip() if lines else ""
        if not container_id:
            raise RuntimeError(f"empty container id returned from {self.binary} create")
        return container_id

    async def start_container(self, container_id: str) -> None:
        result = await self._run(None, ["start", container_id])
        if result.error:
            raise RuntimeError(result.error)

    async def stop_container(self, container_id: str) -> None:
        result = await self._run(None, ["stop", "-t", "0", container_id])
        if result.error:
            raise RuntimeError(result.error)

    async def remove_container(self, container_id: str) -> None:
        result = await self._run(None, ["rm", "-f", container_id])
        if result.error:
            raise RuntimeError(result.error)

    async def wait_container(self, container_id: str) -> int:
        result = await self._run(None, ["wait", container_id])
        if result.error:
            raise RuntimeError(result.error)
        try:
            return int(result.stdout.strip())
        except ValueError as error:
            raise RuntimeError(f"parse wait exit code failed: {result.stdout.strip()}") from error

    async def container_logs(self, container_id: str) -> str:
        result = await self._run(None, ["logs", container_id])
        return result.stdout + result.stderr

    async def _run(self, stdin: bytes | None, args: list[str]) -> _RunResult:
        self._log(_classify_command_level(args), _format_command(self.binary, args))
        try:
            process = await asyncio.create_subprocess_exec(
                self.binary,
                *args,
                cwd=self.work_dir,
                env=os.environ.copy(),
                stdin=asyncio.subprocess.PIPE,
                stdout=asyncio.subprocess.PIPE,
                stderr=asyncio.subprocess.PIPE,
            )
            stdout_raw, stderr_raw = await process.communicate(stdin)
            stdout = stdout_raw.decode(errors="replace")
            stderr = stderr_raw.decode(errors="replace")
            error = None if process.returncode == 0 else f"exit code {process.returncode}"
        except OSError as exception:
            stdout, stderr, error = "", "", str(exception)

        if stdout.strip():
            self._log(
                _classify_stream_level(False, error, stdout, args),
                f"[stdout]\n{stdout.strip()}",
            )
        if stderr.strip():
            self._log(
                _classify_stream_level(True, error, stderr, args),
                f"[stderr]\n{stderr.strip()}",
            )
        if error:
            error = f"{_format_command(self.binary, args)} failed: {error}"
        return _RunResult(stdout, stderr, error)

    def _log(self, level: DockerLogLevel, message: str) -> None:
        if self.event_fn:
            self.event_fn(DockerLogEntry(level, message))
        else:
            logging.getLogger("bkci_agent_sdk.docker").log(
                logging.ERROR if level == "ERROR" else logging.INFO,
                "[%s] %s",
                level,
                message,
            )


def registry_from_image(image: str) -> str:
    value = image.removeprefix("http://").removeprefix("https://").strip()
    if "/" not in value:
        return ""
    first = value.split("/", 1)[0]
    if "." in first or ":" in first or first == "localhost":
        return first
    return ""


_SENSITIVE_ENV_KEYS = ("secret", "password", "token", "credential")


def _format_command(binary: str, args: list[str]) -> str:
    parts = [binary]
    mask_next = False
    for argument in args:
        display = _mask_env_value(argument) if mask_next else argument
        mask_next = argument in {"-e", "--env"} if not mask_next else False
        if any(character in display for character in " \t\n\"'"):
            import json

            display = json.dumps(display)
        parts.append(display)
    return " ".join(parts)


def _mask_env_value(value: str) -> str:
    if "=" not in value:
        return value
    key, raw = value.split("=", 1)
    if any(sensitive in key.lower() for sensitive in _SENSITIVE_ENV_KEYS):
        return f"{key}=******"
    return f"{key}={raw}"


def _classify_command_level(args: list[str]) -> DockerLogLevel:
    return "DEBUG" if args[:2] == ["image", "inspect"] else "INFO"


def _classify_stream_level(
    is_stderr: bool, error: str | None, output: str, args: list[str]
) -> DockerLogLevel:
    if error:
        if args[:2] == ["image", "inspect"]:
            return "INFO"
        return "ERROR" if is_stderr else "WARN"
    lowered = output.lower()
    if is_stderr and any(value in lowered for value in ("warning", "warn:", "deprecated")):
        return "WARN"
    return "INFO"
