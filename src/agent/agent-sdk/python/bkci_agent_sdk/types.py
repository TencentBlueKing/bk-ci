"""BK-CI wire-protocol types.

Dictionary keys intentionally keep their camelCase spelling because these objects are sent to
and received from BK-CI without a translation layer.
"""

from __future__ import annotations

from enum import Enum
from typing import Any, TypedDict

__all__ = [
    "AgentHeartbeatInfo",
    "AgentHeartbeatResponse",
    "AgentPropsInfo",
    "AgentPropsResp",
    "AskEnable",
    "AskInfo",
    "AskResp",
    "BuildJobType",
    "Credential",
    "DevopsError",
    "DockerInitFileInfo",
    "DockerOptions",
    "ImageDebug",
    "ImageDebugFinish",
    "ImagePullPolicy",
    "LogMessage",
    "LogType",
    "PipelineResponse",
    "RegistryParams",
    "RegistryResponse",
    "ThirdPartyAgentStartInfo",
    "ThirdPartyBuildInfo",
    "ThirdPartyBuildWithStatus",
    "ThirdPartyDockerBuildInfo",
    "ThirdPartyDockerTaskInfo",
    "ThirdPartyTaskInfo",
    "UpgradeInfo",
    "UpgradeItem",
]


class BuildJobType(str, Enum):
    ALL = "ALL"
    DOCKER = "DOCKER"
    BINARY = "BINARY"
    NONE = "NONE"


class ImagePullPolicy(str, Enum):
    ALWAYS = "always"
    IF_NOT_PRESENT = "if-not-present"


class LogType(str, Enum):
    LOG = "LOG"
    DEBUG = "DEBUG"
    ERROR = "ERROR"
    WARN = "WARN"


class ThirdPartyAgentStartInfo(TypedDict, total=False):
    hostname: str
    hostIp: str
    detectOS: str
    masterVersion: str
    version: str


class Credential(TypedDict, total=False):
    user: str
    password: str
    errMsg: str


class DockerOptions(TypedDict, total=False):
    volumes: list[str]
    gpus: str
    mounts: list[str]
    privileged: bool
    network: list[str]
    user: str


class ThirdPartyDockerBuildInfo(TypedDict, total=False):
    agentId: str
    secretKey: str
    image: str
    credential: Credential
    options: DockerOptions
    imagePullPolicy: str


class ThirdPartyBuildInfo(TypedDict, total=False):
    projectId: str
    buildId: str
    vmSeqId: str
    workspace: str
    pipelineId: str
    dockerBuildInfo: ThirdPartyDockerBuildInfo | None
    executeCount: int | None
    containerHashId: str


class DevopsError(TypedDict, total=False):
    errorType: int
    errorMessage: str
    errorCode: int


class ThirdPartyBuildWithStatus(ThirdPartyBuildInfo, total=False):
    success: bool
    message: str
    error: DevopsError | None


class PipelineResponse(TypedDict, total=False):
    seqId: str
    status: str
    response: str


class ThirdPartyTaskInfo(TypedDict, total=False):
    projectId: str
    buildId: str
    vmSeqId: str
    workspace: str


class ThirdPartyDockerTaskInfo(TypedDict, total=False):
    projectId: str
    buildId: str
    vmSeqId: str


class DockerInitFileInfo(TypedDict, total=False):
    fileMd5: str
    needUpgrade: bool


class AgentPropsInfo(TypedDict, total=False):
    arch: str
    jdkVersion: list[str]
    dockerInitFileMd5: DockerInitFileInfo
    osVersion: str


class AgentHeartbeatInfo(TypedDict, total=False):
    masterVersion: str
    slaveVersion: str
    hostName: str
    agentIp: str
    parallelTaskCount: int
    agentInstallPath: str
    startedUser: str
    taskList: list[ThirdPartyTaskInfo]
    props: AgentPropsInfo
    dockerParallelTaskCount: int
    dockerTaskList: list[ThirdPartyDockerTaskInfo]
    errorExitData: dict[str, Any] | None


class AgentPropsResp(TypedDict, total=False):
    ignoreLocalIps: str
    keepLogsHours: int
    enablePipeline: bool


class AgentHeartbeatResponse(TypedDict, total=False):
    masterVersion: str
    slaveVersion: str
    agentStatus: str
    parallelTaskCount: int
    envs: dict[str, str] | None
    gateway: str
    fileGateway: str
    props: AgentPropsResp
    dockerParallelTaskCount: int
    language: str
    createMod: bool | None


class UpgradeInfo(TypedDict, total=False):
    workerVersion: str
    goAgentVersion: str
    jdkVersion: list[str]
    dockerInitFileInfo: DockerInitFileInfo


class UpgradeItem(TypedDict, total=False):
    agent: bool
    worker: bool
    jdk: bool
    dockerInitFile: bool


class ImageDebug(TypedDict, total=False):
    projectId: str
    buildId: str
    vmSeqId: str
    workspace: str
    pipelineId: str
    debugUserId: str
    debugId: int
    image: str
    credential: Credential
    options: DockerOptions


class ImageDebugFinish(TypedDict, total=False):
    projectId: str
    debugId: int
    pipelineId: str
    debugUrl: str
    success: bool
    error: DevopsError | None


class LogMessage(TypedDict, total=False):
    message: str
    timestamp: int
    tag: str
    jobId: str
    logType: LogType | str
    executeCount: int | None
    subTag: str | None


class AskEnable(TypedDict, total=False):
    build: BuildJobType | str
    upgrade: bool
    dockerDebug: bool
    pipeline: bool


class AskInfo(TypedDict, total=False):
    askEnable: AskEnable
    heartbeat: AgentHeartbeatInfo
    upgrade: UpgradeInfo | None


class AskResp(TypedDict, total=False):
    heartbeat: AgentHeartbeatResponse | None
    build: ThirdPartyBuildInfo | None
    upgrade: UpgradeItem | None
    pipeline: dict[str, Any] | None
    debug: ImageDebug | None


class RegistryParams(TypedDict, total=False):
    token: str
    deviceId: str
    userId: str


class RegistryResponse(TypedDict, total=False):
    projectId: str
    agentId: str
    secretKey: str
    gateway: str
    fileGateway: str
    parallelTaskCount: int
    dockerParallelTaskCount: int
    language: str
