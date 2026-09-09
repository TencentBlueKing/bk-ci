"""Agent configuration, properties parsing, and authentication headers."""

from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path
from typing import Any, Mapping


class AuthHeader:
    BUILD_TYPE = "X-DEVOPS-BUILD-TYPE"
    PROJECT_ID = "X-DEVOPS-PROJECT-ID"
    AGENT_ID = "X-DEVOPS-AGENT-ID"
    SECRET_KEY = "X-DEVOPS-AGENT-SECRET-KEY"
    BUILD_ID = "X-DEVOPS-BUILD-ID"
    VM_SEQ_ID = "X-DEVOPS-VM-SID"


BUILD_TYPE_AGENT = "AGENT"


class AgentStatus:
    IMPORT_OK = "IMPORT_OK"
    DELETE = "DELETE"


class ConfigKey:
    PROJECT_ID = "devops.project.id"
    AGENT_ID = "devops.agent.id"
    SECRET_KEY = "devops.agent.secret.key"
    GATEWAY = "landun.gateway"
    FILE_GATEWAY = "landun.fileGateway"
    PARALLEL_TASK_COUNT = "devops.parallel.task.count"
    DOCKER_PARALLEL_TASK_COUNT = "devops.docker.parallel.task.count"
    ENV_TYPE = "landun.env"
    SLAVE_USER = "devops.slave.user"
    COLLECTOR_ON = "devops.agent.collectorOn"
    REQUEST_TIMEOUT_SEC = "devops.agent.request.timeout.sec"
    IGNORE_LOCAL_IPS = "devops.agent.ignoreLocalIps"
    LOGS_KEEP_HOURS = "devops.agent.logs.keep.hours"
    ENABLE_DOCKER_BUILD = "devops.docker.enable"
    LANGUAGE = "devops.language"
    ENABLE_PIPELINE = "devops.pipeline.enable"


@dataclass(slots=True)
class AgentConfig:
    gateway: str
    project_id: str
    agent_id: str
    secret_key: str
    file_gateway: str = ""
    build_type: str = BUILD_TYPE_AGENT
    parallel_task_count: int = 4
    docker_parallel_task_count: int = 4
    env_type: str = ""
    slave_user: str = ""
    timeout_sec: int = 5
    ignore_local_ips: str = "127.0.0.1"
    logs_keep_hours: int = 96
    enable_docker_build: bool = False
    language: str = "zh_CN"
    enable_pipeline: bool = False

    def __post_init__(self) -> None:
        for field_name in ("project_id", "agent_id", "secret_key", "gateway"):
            if not getattr(self, field_name):
                raise ValueError(f"invalid config: {field_name} is required")

    def get_auth_header_map(self) -> dict[str, str]:
        return {
            AuthHeader.BUILD_TYPE: self.build_type,
            AuthHeader.PROJECT_ID: self.project_id,
            AuthHeader.AGENT_ID: self.agent_id,
            AuthHeader.SECRET_KEY: self.secret_key,
        }

    def get_gateway(self) -> str:
        if self.gateway.startswith(("http://", "https://")):
            return self.gateway
        return "http://" + self.gateway

    @classmethod
    def from_properties_file(cls, file_path: str | Path | None = None) -> AgentConfig:
        path = Path(file_path) if file_path is not None else Path.cwd() / ".agent.properties"
        values = parse_properties(path.read_text(encoding="utf-8"))

        def get(key: str) -> str:
            return values.get(key, "").strip()

        def get_int(key: str, default: int) -> int:
            try:
                return int(get(key)) if get(key) else default
            except ValueError:
                return default

        def get_bool(key: str, default: bool) -> bool:
            value = get(key).lower()
            return value in {"true", "1", "yes", "on"} if value else default

        return cls(
            project_id=get(ConfigKey.PROJECT_ID),
            agent_id=get(ConfigKey.AGENT_ID),
            secret_key=get(ConfigKey.SECRET_KEY),
            gateway=get(ConfigKey.GATEWAY),
            file_gateway=get(ConfigKey.FILE_GATEWAY),
            parallel_task_count=get_int(ConfigKey.PARALLEL_TASK_COUNT, 4),
            docker_parallel_task_count=get_int(ConfigKey.DOCKER_PARALLEL_TASK_COUNT, 4),
            env_type=get(ConfigKey.ENV_TYPE),
            slave_user=get(ConfigKey.SLAVE_USER),
            timeout_sec=get_int(ConfigKey.REQUEST_TIMEOUT_SEC, 5),
            ignore_local_ips=get(ConfigKey.IGNORE_LOCAL_IPS) or "127.0.0.1",
            logs_keep_hours=get_int(ConfigKey.LOGS_KEEP_HOURS, 96),
            enable_docker_build=get_bool(ConfigKey.ENABLE_DOCKER_BUILD, False),
            language=get(ConfigKey.LANGUAGE) or "zh_CN",
            enable_pipeline=get_bool(ConfigKey.ENABLE_PIPELINE, False),
        )

    @classmethod
    def from_registry(cls, info: Mapping[str, Any]) -> AgentConfig:
        def int_value(key: str, default: int) -> int:
            value = info.get(key)
            return default if value is None else int(value)

        return cls(
            project_id=str(info.get("projectId", "")),
            agent_id=str(info.get("agentId", "")),
            secret_key=str(info.get("secretKey", "")),
            gateway=str(info.get("gateway", "")),
            file_gateway=str(info.get("fileGateway", "")),
            parallel_task_count=int_value("parallelTaskCount", 4),
            docker_parallel_task_count=int_value("dockerParallelTaskCount", 4),
            language=str(info.get("language") or "zh_CN"),
        )


def parse_properties(content: str) -> dict[str, str]:
    result: dict[str, str] = {}
    for line in content.splitlines():
        stripped = line.strip()
        if not stripped or stripped.startswith(("#", ";")) or "=" not in stripped:
            continue
        key, value = stripped.split("=", 1)
        key = key.strip()
        if key:
            result[key] = value.strip()
    return result
