"""Docker/Podman worker build orchestration."""

from __future__ import annotations

import asyncio
from dataclasses import dataclass, field
from pathlib import Path
import random
import string
import sys
from typing import Callable, Mapping

from .docker_cli import DockerRunner
from .types import DockerOptions, ImagePullPolicy, ThirdPartyBuildInfo

CONTAINER_WORKER_JAR = "/data/worker-agent.jar"
ENTRY_POINT_CMD = "/data/init.sh"
TARGET_JRE_DIR = "/usr/local/jre"
TARGET_JRE8_DIR = "/usr/local/jre8"
DOCKER_LOG_DIR = "/data/devops/logs"
DOCKER_DATA_DIR = "/data/devops/workspace"
DOCKER_NO_MOUNT = "__NO_MOUNT__"

_LOCAL_DOCKER_WORKSPACE = "docker_workspace"
_LOCAL_DOCKER_BUILD_TEMP = "docker_build_tmp"
_LONG_LOG_TAG = "toolong"


@dataclass(slots=True)
class DockerBuildOptions:
    build_info: ThirdPartyBuildInfo
    work_dir: str | Path
    worker_jar_path: str | Path
    docker_init_script_path: str | Path
    gateway: str
    project_id: str
    jdk17_dir_path: str | Path | None = None
    jdk8_dir_path: str | Path | None = None
    extra_env: Mapping[str, str] = field(default_factory=dict)
    platform: str | None = None
    post_log: Callable[[str], None] | None = None
    rand_suffix: Callable[[], str] | None = None


@dataclass(slots=True)
class DockerBuildResult:
    success: bool
    message: str


def _ensure_gateway(gateway: str) -> str:
    return gateway if gateway.startswith(("http://", "https://")) else "http://" + gateway


def _parse_api_docker_options(options: DockerOptions) -> list[str]:
    args: list[str] = []
    for volume in options.get("volumes", []) or []:
        if volume.strip():
            args.extend(["--volume", volume.strip()])
    for mount in options.get("mounts", []) or []:
        if mount.strip():
            args.extend(["--mount", mount.strip()])
    gpus = str(options.get("gpus", "") or "").strip()
    if gpus:
        args.extend(["--gpus", gpus])
    if options.get("privileged"):
        args.append("--privileged")
    for network in options.get("network", []) or []:
        if network.strip():
            args.extend(["--network", network.strip()])
    user = str(options.get("user", "") or "").strip()
    if user:
        args.extend(["--user", user])
    return args


def _build_user_docker_args(options: DockerOptions) -> list[str]:
    args = _parse_api_docker_options(options)
    index = 0
    options_with_values = {"--volume", "--mount", "--network", "--user", "--gpus"}
    while index < len(args):
        if args[index] in options_with_values:
            if index + 1 >= len(args) or not args[index + 1].strip():
                raise ValueError(f"docker option {args[index]} requires a non-empty value")
            index += 2
        else:
            index += 1
    return args


def _has_custom_network(options: DockerOptions) -> bool:
    return len(options.get("network", []) or []) > 0


def _need_local_image_inspect(is_latest: bool, policy: str) -> bool:
    if policy == ImagePullPolicy.ALWAYS.value:
        return False
    if policy == ImagePullPolicy.IF_NOT_PRESENT.value:
        return True
    return not is_latest


def _if_pull_image(local_exists: bool, is_latest: bool, policy: str) -> bool:
    if policy == ImagePullPolicy.ALWAYS.value:
        return True
    if policy == ImagePullPolicy.IF_NOT_PRESENT.value:
        return not local_exists
    return True if is_latest else not local_exists


def _has_jdk17_dir(path: str | Path | None) -> bool:
    return bool(path) and Path(path).is_dir()


def _container_env(options: DockerBuildOptions) -> list[str]:
    docker = options.build_info.get("dockerBuildInfo") or {}
    values = [
        f"devops_project_id={options.project_id}",
        f"devops_agent_id={docker.get('agentId', '')}",
        f"devops_agent_secret_key={docker.get('secretKey', '')}",
        f"devops_gateway={_ensure_gateway(options.gateway)}",
        "agent_build_env=DOCKER",
    ]
    if _has_jdk17_dir(options.jdk17_dir_path) and (options.platform or sys.platform) == "linux":
        values.extend(
            [
                f"DEVOPS_AGENT_JDK_8_PATH={TARGET_JRE8_DIR}/bin/java",
                f"DEVOPS_AGENT_JDK_17_PATH={TARGET_JRE_DIR}/bin/java",
            ]
        )
    values.extend(f"{key}={value}" for key, value in options.extra_env.items())
    return values


async def _container_mount_args(options: DockerBuildOptions) -> list[str]:
    build = options.build_info
    args: list[str] = []
    if _has_jdk17_dir(options.jdk17_dir_path):
        args.extend(
            [
                "--mount",
                f"type=bind,source={options.jdk17_dir_path},target={TARGET_JRE_DIR},readonly",
                "--mount",
                f"type=bind,source={options.jdk8_dir_path or ''},target={TARGET_JRE8_DIR},readonly",
            ]
        )
    elif options.jdk8_dir_path:
        args.extend(
            [
                "--mount",
                f"type=bind,source={options.jdk8_dir_path},target={TARGET_JRE_DIR},readonly",
            ]
        )
    args.extend(
        [
            "--mount",
            f"type=bind,source={options.worker_jar_path},target={CONTAINER_WORKER_JAR},readonly",
            "--mount",
            f"type=bind,source={options.docker_init_script_path},target={ENTRY_POINT_CMD},readonly",
        ]
    )

    work_dir = Path(options.work_dir)
    data_dir = work_dir / _LOCAL_DOCKER_WORKSPACE / "data" / str(
        build.get("pipelineId", "")
    ) / str(build.get("vmSeqId", ""))
    if build.get("workspace"):
        data_dir = Path(str(build["workspace"]))
    if str(data_dir) != DOCKER_NO_MOUNT:
        data_dir.mkdir(parents=True, exist_ok=True)
        args.extend(["--mount", f"type=bind,source={data_dir},target={DOCKER_DATA_DIR}"])

    logs_dir = (
        work_dir
        / _LOCAL_DOCKER_WORKSPACE
        / "logs"
        / str(build.get("buildId", ""))
        / str(build.get("vmSeqId", ""))
    )
    logs_dir.mkdir(parents=True, exist_ok=True)
    args.extend(["--mount", f"type=bind,source={logs_dir},target={DOCKER_LOG_DIR}"])
    return args


async def build_docker_create_args(
    container_name: str, image: str, options: DockerBuildOptions
) -> list[str]:
    docker = options.build_info.get("dockerBuildInfo") or {}
    docker_options: DockerOptions = docker.get("options") or {}
    mount_args = await _container_mount_args(options)
    user_args = _build_user_docker_args(docker_options)
    args = ["--name", container_name, *user_args]
    if not _has_custom_network(docker_options):
        args.extend(["--network", "bridge"])
    for value in _container_env(options):
        args.extend(["-e", value])
    args.extend(mount_args)
    args.extend(["--entrypoint", "/bin/sh", image, "-c", ENTRY_POINT_CMD])
    return args


async def run_docker_build(
    runner: DockerRunner, options: DockerBuildOptions
) -> DockerBuildResult:
    build = options.build_info
    docker = build.get("dockerBuildInfo")
    post_log = options.post_log or (lambda message: None)
    if not docker:
        return DockerBuildResult(False, "dockerBuildInfo is missing")
    credential = docker.get("credential") or {}
    if credential.get("errMsg"):
        return DockerBuildResult(False, f"get docker cred error: {credential['errMsg']}")

    image_name = str(docker.get("image", "") or "").strip()
    image = image_name.removeprefix("http://").removeprefix("https://")
    image_parts = image.split(":")
    is_latest = len(image_parts) == 1 or (
        len(image_parts) == 2 and image_parts[1] == "latest"
    )
    raw_policy = docker.get("imagePullPolicy", "")
    policy = raw_policy.value if isinstance(raw_policy, ImagePullPolicy) else str(raw_policy or "")
    local_exists = False
    if _need_local_image_inspect(is_latest, policy):
        try:
            local_exists = await runner.image_exists(image_name)
        except Exception as error:
            return DockerBuildResult(False, f"inspect docker image error: {error}")

    if _if_pull_image(local_exists, is_latest, policy):
        if is_latest:
            post_log("pull latest image")
        post_log(f"start pull image: {image_name}")
        try:
            output = await runner.pull_image(
                image_name,
                str(credential.get("user", "")),
                str(credential.get("password", "")),
            )
            if output.strip():
                post_log(output)
        except Exception as error:
            return DockerBuildResult(False, f"pull image {image_name} error: {error}")
    else:
        post_log(f"use local exist image: {image_name}")

    try:
        (Path(options.work_dir) / _LOCAL_DOCKER_BUILD_TEMP).mkdir(parents=True, exist_ok=True)
    except OSError as error:
        return DockerBuildResult(False, f"create docker tmp dir error: {error}")
    suffix = options.rand_suffix() if options.rand_suffix else "".join(
        random.choice(string.ascii_letters) for _ in range(8)
    )
    container_name = (
        f"dispatch-{build.get('buildId', '')}-{build.get('vmSeqId', '')}-{suffix}"
    )
    try:
        create_args = await build_docker_create_args(container_name, image, options)
    except Exception as error:
        return DockerBuildResult(False, f"parse docker options error: {error}")
    try:
        container_id = await runner.create_container(create_args)
    except Exception as error:
        return DockerBuildResult(False, f"create container {container_name} error: {error}")

    try:
        try:
            await runner.start_container(container_id)
        except Exception as error:
            return DockerBuildResult(False, f"start container {container_name} error: {error}")
        try:
            status_code = await runner.wait_container(container_id)
        except Exception as error:
            return DockerBuildResult(False, f"wait container {container_name} error: {error}")
        if status_code != 0:
            message = await _read_docker_log_file(options, _LONG_LOG_TAG)
            try:
                container_log = await runner.container_logs(container_id)
            except Exception:
                container_log = ""
            if not message:
                message = container_log
            elif container_log.strip():
                post_log("docker container log: " + container_log)
            if message == _LONG_LOG_TAG:
                message = ""
            return DockerBuildResult(
                False, f"container {container_name} exit code {status_code}: {message}"
            )
        return DockerBuildResult(True, "")
    finally:
        try:
            await runner.remove_container(container_id)
        except Exception:
            pass


async def _read_docker_log_file(options: DockerBuildOptions, tag: str) -> str:
    build = options.build_info
    log_file = (
        Path(options.work_dir)
        / _LOCAL_DOCKER_WORKSPACE
        / "logs"
        / str(build.get("buildId", ""))
        / str(build.get("vmSeqId", ""))
        / "docker.log"
    )
    try:
        content = await asyncio.to_thread(log_file.read_bytes)
    except FileNotFoundError:
        return ""
    except OSError as error:
        return f"read log file error {error}"
    return tag if len(content) > 1000 else content.decode("utf-8", errors="replace")
