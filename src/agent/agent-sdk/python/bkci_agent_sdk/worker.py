"""Physical-machine worker-agent.jar execution and version detection."""

from __future__ import annotations

import asyncio
import base64
from dataclasses import dataclass, field
import json
import os
from pathlib import Path
import shlex
import signal
import sys
from typing import Callable, Mapping

from .types import ThirdPartyBuildInfo

MACOS_JAVA_RELATIVE_PATH = Path("Contents") / "Home" / "bin" / "java"
BUILDER_PROCESS_WAS_KILLED = "build process was killed"


@dataclass(slots=True)
class WorkerBuildOptions:
    build_info: ThirdPartyBuildInfo
    work_dir: str | Path
    worker_jar_path: str | Path
    gateway: str
    agent_version: str
    worker_version: str
    jdk17_path: str | Path | None = None
    jdk8_path: str | Path | None = None
    file_gateway: str = ""
    project_id: str | None = None
    agent_id: str = ""
    secret_key: str = ""
    language: str = "zh_CN"
    extra_env: Mapping[str, str] = field(default_factory=dict)
    detect_shell: bool = False
    enable_exit_group: bool = True
    log_fn: Callable[[str], None] | None = None


@dataclass(slots=True)
class WorkerBuildResult:
    success: bool
    message: str


def resolve_java_bin(jdk_path: str | Path, platform: str | None = None) -> str:
    path = Path(jdk_path)
    if path.name.lower() in {"java", "java.exe"}:
        return str(path)
    target_platform = platform or sys.platform
    if target_platform == "darwin":
        return str(path / MACOS_JAVA_RELATIVE_PATH)
    if target_platform in {"win32", "cygwin"}:
        return str(path / "bin" / "java.exe")
    return str(path / "bin" / "java")


def resolve_latest_java(
    jdk17_path: str | Path | None,
    jdk8_path: str | Path | None,
    platform: str | None = None,
) -> str:
    if jdk17_path is not None:
        java17 = resolve_java_bin(jdk17_path, platform)
        if Path(java17).exists():
            return java17
    if jdk8_path is not None:
        return resolve_java_bin(jdk8_path, platform)
    return ""


def parse_worker_version(output: str) -> str:
    import re

    legacy = re.compile(r"^v\d+\.\d+\.\d+(?:-RELEASE|-SNAPSHOT)?$")
    basic = re.compile(r"^v\d+\.\d+\.\d+$")
    suffixed = re.compile(r"^v\d+\.\d+\.\d+-([^-\.\s]+)\.(\d+)$")
    for output_line in output.splitlines():
        line = output_line.strip()
        if not line or " " in line or "OPTIONS" in line:
            continue
        line = line[:64]
        suffix_match = suffixed.fullmatch(line)
        current = bool(basic.fullmatch(line)) or bool(
            suffix_match and not suffix_match.group(1).isdigit()
        )
        if current or legacy.fullmatch(line):
            return line
    return ""


async def detect_worker_version(
    *,
    jdk17_path: str | Path,
    worker_jar_path: str | Path,
    work_dir: str | Path | None = None,
    log_fn: Callable[[str], None] | None = None,
) -> str:
    log = log_fn or (lambda message: None)
    java_bin = Path(resolve_java_bin(jdk17_path)).resolve()
    worker_jar = Path(worker_jar_path).resolve()
    work = Path(work_dir).resolve() if work_dir else worker_jar.parent
    if not java_bin.exists():
        log(f"detect worker version failed: jdk17 java missing: {java_bin}")
        return ""
    if not worker_jar.exists():
        log(f"detect worker version failed: worker jar missing: {worker_jar}")
        return ""
    temp_dir = work / "build_tmp"
    try:
        temp_dir.mkdir(parents=True, exist_ok=True)
    except OSError as error:
        log(f"detect worker version failed: cannot create temp dir {temp_dir}: {error}")
        return ""
    args = [
        f"-Djava.io.tmpdir={temp_dir}",
        "-Xmx256m",
        "-cp",
        str(worker_jar),
        "com.tencent.devops.agent.AgentVersionKt",
    ]
    output, error = await _spawn_and_collect(str(java_bin), args, work)
    if error:
        suffix = "" if not output else f", output: {output.strip()}"
        log(f"detect worker version failed: {error}{suffix}")
        return ""
    version = parse_worker_version(output)
    if version:
        log(f"detected worker version: {version}")
    else:
        log("detect worker version failed: no valid version found in command output")
    return version


def build_worker_env(options: WorkerBuildOptions) -> dict[str, str]:
    build = options.build_info
    project_id = str(build.get("projectId", ""))
    build_id = str(build.get("buildId", ""))
    vm_seq_id = str(build.get("vmSeqId", ""))
    env = {
        "DEVOPS_AGENT_VERSION": options.agent_version,
        "DEVOPS_WORKER_VERSION": options.worker_version,
        "DEVOPS_PROJECT_ID": project_id,
        "DEVOPS_BUILD_ID": build_id,
        "DEVOPS_VM_SEQ_ID": vm_seq_id,
        "DEVOPS_SLAVE_VERSION": options.worker_version,
        "PROJECT_ID": project_id,
        "BUILD_ID": build_id,
        "VM_SEQ_ID": vm_seq_id,
        "DEVOPS_FILE_GATEWAY": options.file_gateway,
        "DEVOPS_GATEWAY": options.gateway,
        "BK_CI_LOCALE_LANGUAGE": options.language,
    }
    if options.jdk8_path is not None:
        java8 = resolve_java_bin(options.jdk8_path)
        if Path(java8).exists():
            env["DEVOPS_AGENT_JDK_8_PATH"] = java8
    if options.jdk17_path is not None:
        java17 = resolve_java_bin(options.jdk17_path)
        if Path(java17).exists():
            env["DEVOPS_AGENT_JDK_17_PATH"] = java17
    env.update({str(key): str(value) for key, value in options.extra_env.items()})
    return env


def _encoded_build_info(build_info: ThirdPartyBuildInfo) -> str:
    raw = json.dumps(build_info, ensure_ascii=False, separators=(",", ":")).encode("utf-8")
    return base64.b64encode(raw).decode("ascii")


async def _ensure_agent_properties(options: WorkerBuildOptions, log: Callable[[str], None]) -> None:
    work_dir = Path(options.work_dir)
    properties = work_dir / ".agent.properties"
    if properties.exists():
        return
    work_dir.mkdir(parents=True, exist_ok=True)
    project_id = options.project_id or str(options.build_info.get("projectId", ""))
    content = "\n".join(
        [
            f"devops.project.id={project_id}",
            f"devops.agent.id={options.agent_id}",
            f"devops.agent.secret.key={options.secret_key}",
            f"landun.gateway={options.gateway}",
            f"landun.fileGateway={options.file_gateway}",
            "",
        ]
    )
    await asyncio.to_thread(properties.write_text, content, encoding="utf-8")
    await asyncio.to_thread(os.chmod, properties, 0o644)
    log(f"generated .agent.properties at {properties}")


def _worker_error_file(work_dir: Path, build_id: str, vm_seq_id: str) -> Path:
    return work_dir / "build_tmp" / f"{build_id}_{vm_seq_id}_build_msg.log"


def _current_shell(detect_shell: bool) -> str:
    if detect_shell:
        return os.environ.get("SHELL", "").strip() or "/bin/bash"
    return "/bin/bash"


def _prepare_script_lines(shell: str, script_file: Path) -> list[str]:
    quoted_script = shlex.quote(str(script_file))
    if shell == "/bin/tcsh":
        return [f"#!{shell}", f"exec {shlex.quote(shell)} {quoted_script} -l"]
    return [f"#!{shell}", f"exec {shlex.quote(shell)} -l {quoted_script}"]


async def run_worker_build(options: WorkerBuildOptions) -> WorkerBuildResult:
    log = options.log_fn or (lambda message: None)
    worker_jar = Path(options.worker_jar_path)
    if not worker_jar.exists():
        message = f"worker jar missing: {worker_jar}"
        log(message)
        return WorkerBuildResult(False, message)
    java_bin = resolve_latest_java(options.jdk17_path, options.jdk8_path)
    if not java_bin:
        message = "no available jdk (both jdk17 and jdk8 path missing or invalid)"
        log(message)
        return WorkerBuildResult(False, message)

    await _ensure_agent_properties(options, log)
    work_dir = Path(options.work_dir)
    temp_dir = work_dir / "build_tmp"
    temp_dir.mkdir(parents=True, exist_ok=True)
    build = options.build_info
    project_id = str(build.get("projectId", ""))
    build_id = str(build.get("buildId", ""))
    vm_seq_id = str(build.get("vmSeqId", ""))
    error_file = _worker_error_file(work_dir, build_id, vm_seq_id)
    log_prefix = f"{build_id}_{vm_seq_id}_agent"
    encoded = _encoded_build_info(build)
    env = os.environ.copy()
    env.update(build_worker_env(options))
    cleanup = [error_file]
    await asyncio.to_thread(error_file.write_text, BUILDER_PROCESS_WAS_KILLED, encoding="utf-8")
    await asyncio.to_thread(os.chmod, error_file, 0o777)

    if sys.platform in {"win32", "cygwin"}:
        args = [
            f"-Djava.io.tmpdir={temp_dir}",
            f"-Ddevops.agent.error.file={error_file}",
            "-Dbuild.type=AGENT",
            f"-DAGENT_LOG_PREFIX={log_prefix}",
            "-Xmx2g",
            "-jar",
            str(worker_jar),
            encoded,
        ]
        log(f"start worker: {java_bin} {' '.join(args)}")
        exit_code, exit_error = await _spawn_and_wait(
            java_bin, args, work_dir, env, use_process_group=False
        )
    else:
        shell = _current_shell(options.detect_shell)
        prepare_script = work_dir / (
            f"devops_agent_prepare_start_{project_id}_{build_id}_{vm_seq_id}.sh"
        )
        start_script = work_dir / f"devops_agent_start_{project_id}_{build_id}_{vm_seq_id}.sh"
        cleanup.extend([start_script, prepare_script])
        java_args = [
            f"-Ddevops.slave.agent.start.file={start_script}",
            f"-Ddevops.slave.agent.prepare.start.file={prepare_script}",
            f"-Ddevops.agent.error.file={error_file}",
            "-Dbuild.type=AGENT",
            f"-DAGENT_LOG_PREFIX={log_prefix}",
            "-Xmx2g",
            f"-Djava.io.tmpdir={temp_dir}",
            "-jar",
            str(worker_jar),
            encoded,
        ]
        command = " ".join(shlex.quote(item) for item in [java_bin, *java_args])
        start_content = f"#!{shell}\ncd {shlex.quote(str(work_dir))}\n{command}"
        prepare_content = "\n".join(_prepare_script_lines(shell, start_script))
        await asyncio.to_thread(start_script.write_text, start_content, encoding="utf-8")
        await asyncio.to_thread(prepare_script.write_text, prepare_content, encoding="utf-8")
        await asyncio.to_thread(os.chmod, start_script, 0o777)
        await asyncio.to_thread(os.chmod, prepare_script, 0o777)
        log(f"start worker via prepare script: {prepare_script}")
        exit_code, exit_error = await _spawn_and_wait(
            str(prepare_script),
            [],
            work_dir,
            env,
            use_process_group=options.enable_exit_group,
        )

    try:
        message = (await asyncio.to_thread(error_file.read_text, encoding="utf-8")).strip()
    except OSError:
        message = ""
    command_error = f"|{exit_error}" if exit_error else ""
    if not message:
        message = f"worker process exit{command_error}"
        success = True
    else:
        message += command_error
        success = False
    log(
        f"build[{build_id}] finish, exitCode={exit_code}, success={str(success).lower()}, "
        f"msg={message}"
    )
    for path in cleanup:
        try:
            path.unlink(missing_ok=True)
        except OSError:
            pass
    return WorkerBuildResult(success, message)


async def _spawn_and_collect(
    command: str, args: list[str], cwd: str | Path
) -> tuple[str, str | None]:
    try:
        process = await asyncio.create_subprocess_exec(
            command,
            *args,
            cwd=str(cwd),
            env=os.environ.copy(),
            stdin=asyncio.subprocess.DEVNULL,
            stdout=asyncio.subprocess.PIPE,
            stderr=asyncio.subprocess.PIPE,
        )
        stdout, stderr = await process.communicate()
    except OSError as error:
        return "", str(error)
    output = stdout.decode(errors="replace") + "\n" + stderr.decode(errors="replace")
    return (output, None) if process.returncode == 0 else (output, f"exit code {process.returncode}")


async def _spawn_and_wait(
    command: str,
    args: list[str],
    cwd: str | Path,
    env: Mapping[str, str],
    *,
    use_process_group: bool,
) -> tuple[int, str | None]:
    try:
        process = await asyncio.create_subprocess_exec(
            command,
            *args,
            cwd=str(cwd),
            env=dict(env),
            stdin=asyncio.subprocess.DEVNULL,
            stdout=asyncio.subprocess.DEVNULL,
            stderr=asyncio.subprocess.DEVNULL,
            start_new_session=use_process_group and os.name != "nt",
        )
    except OSError as error:
        return -1, str(error)
    return_code = await process.wait()
    if use_process_group and os.name != "nt" and process.pid:
        try:
            os.killpg(process.pid, signal.SIGKILL)
        except ProcessLookupError:
            pass
        except PermissionError:
            pass
    if return_code == 0:
        return 0, None
    if return_code < 0:
        return return_code, f"killed by signal {-return_code}"
    return return_code, f"exit code {return_code}"

