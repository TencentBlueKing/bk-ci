/*
 * BK-CI Agent SDK - 协议类型定义
 *
 * 本文件对应 Go agent 的 src/pkg/api/type.go，字段名与后台交互的 JSON 完全对齐。
 * 所有与后台通信的请求/响应结构都在这里定义，是跨语言协议规范的 TypeScript 落地。
 */

/** 构建任务类型（对应 Go BuildJobType） */
export enum BuildJobType {
  All = 'ALL',
  Docker = 'DOCKER',
  Binary = 'BINARY',
  None = 'NONE',
}

/** 镜像拉取策略 */
export enum ImagePullPolicy {
  Always = 'always',
  IfNotPresent = 'if-not-present',
}

/** 日志类型 */
export enum LogType {
  Log = 'LOG',
  Debug = 'DEBUG',
  Error = 'ERROR',
  Warn = 'WARN',
}

/** Agent 启动上报信息 */
export interface ThirdPartyAgentStartInfo {
  hostname: string;
  hostIp: string;
  detectOS: string;
  masterVersion: string;
  /** 对应 Go json tag "version"（SlaveVersion） */
  version: string;
}

export interface Credential {
  user: string;
  password: string;
  errMsg?: string;
}

export interface DockerOptions {
  volumes?: string[];
  gpus?: string;
  mounts?: string[];
  privileged?: boolean;
  network?: string[];
  user?: string;
}

export interface ThirdPartyDockerBuildInfo {
  agentId: string;
  secretKey: string;
  image: string;
  credential: Credential;
  options: DockerOptions;
  imagePullPolicy: string;
}

/** 构建任务信息（ask 响应中的 build 字段） */
export interface ThirdPartyBuildInfo {
  projectId: string;
  buildId: string;
  vmSeqId: string;
  workspace: string;
  pipelineId: string;
  dockerBuildInfo?: ThirdPartyDockerBuildInfo | null;
  executeCount?: number | null;
  containerHashId?: string;
}

/** 错误信息 */
export interface DevopsError {
  errorType: number;
  errorMessage: string;
  errorCode: number;
}

/** 构建完成上报（workerBuildFinish 请求体），继承 ThirdPartyBuildInfo */
export interface ThirdPartyBuildWithStatus extends ThirdPartyBuildInfo {
  success: boolean;
  message: string;
  error?: DevopsError | null;
}

/** 流水线执行结果上报 */
export interface PipelineResponse {
  seqId: string;
  status: string;
  response: string;
}

/** 正在运行的普通构建任务（心跳上报用） */
export interface ThirdPartyTaskInfo {
  projectId: string;
  buildId: string;
  vmSeqId: string;
  workspace: string;
}

/** 正在运行的 Docker 构建任务（心跳上报用） */
export interface ThirdPartyDockerTaskInfo {
  projectId: string;
  buildId: string;
  vmSeqId: string;
}

export interface DockerInitFileInfo {
  fileMd5: string;
  needUpgrade: boolean;
}

/** 心跳上报中的属性信息 */
export interface AgentPropsInfo {
  arch: string;
  jdkVersion: string[];
  dockerInitFileMd5: DockerInitFileInfo;
  osVersion: string;
}

/** 退出错误数据（对应 Go exitcode.ExitErrorType，此处按需保留为宽松结构） */
export interface ExitErrorType {
  errorEnum?: string;
  message?: string;
  [key: string]: unknown;
}

/** 心跳上报信息（AskInfo.heartbeat） */
export interface AgentHeartbeatInfo {
  masterVersion: string;
  slaveVersion: string;
  hostName: string;
  agentIp: string;
  parallelTaskCount: number;
  agentInstallPath: string;
  startedUser: string;
  taskList: ThirdPartyTaskInfo[];
  props: AgentPropsInfo;
  dockerParallelTaskCount: number;
  dockerTaskList: ThirdPartyDockerTaskInfo[];
  errorExitData?: ExitErrorType | null;
}

/** 心跳响应中的属性信息 */
export interface AgentPropsResp {
  ignoreLocalIps: string;
  keepLogsHours: number;
  enablePipeline: boolean;
}

/** 心跳响应（ask 响应中的 heartbeat 字段） */
export interface AgentHeartbeatResponse {
  masterVersion: string;
  slaveVersion: string;
  agentStatus: string;
  parallelTaskCount: number;
  envs: Record<string, string> | null;
  gateway: string;
  fileGateway: string;
  props: AgentPropsResp;
  dockerParallelTaskCount: number;
  language: string;
  createMod?: boolean | null;
}

/** 升级信息上报（AskInfo.upgrade） */
export interface UpgradeInfo {
  workerVersion: string;
  goAgentVersion: string;
  jdkVersion: string[];
  dockerInitFileInfo: DockerInitFileInfo;
}

/** 升级项（ask 响应中的 upgrade 字段） */
export interface UpgradeItem {
  agent: boolean;
  worker: boolean;
  jdk: boolean;
  dockerInitFile: boolean;
}

/** 镜像调试信息（ask 响应中的 debug 字段） */
export interface ImageDebug {
  projectId: string;
  buildId: string;
  vmSeqId: string;
  workspace: string;
  pipelineId: string;
  debugUserId: string;
  debugId: number;
  image: string;
  credential: Credential;
  options: DockerOptions;
}

/** 镜像调试完成上报 */
export interface ImageDebugFinish {
  projectId: string;
  debugId: number;
  pipelineId: string;
  debugUrl: string;
  success: boolean;
  error?: DevopsError | null;
}

/** 日志消息 */
export interface LogMessage {
  message: string;
  /** 毫秒时间戳 */
  timestamp: number;
  tag: string;
  jobId: string;
  logType: LogType;
  executeCount?: number | null;
  subTag?: string | null;
}

/** Ask 请求的能力开关 */
export interface AskEnable {
  build: BuildJobType;
  upgrade: boolean;
  dockerDebug: boolean;
  pipeline: boolean;
}

/** Ask 请求体 */
export interface AskInfo {
  askEnable: AskEnable;
  heartbeat: AgentHeartbeatInfo;
  upgrade?: UpgradeInfo | null;
}

/** Ask 响应体（AgentResult.data 反序列化后的结构） */
export interface AskResp {
  heartbeat?: AgentHeartbeatResponse | null;
  build?: ThirdPartyBuildInfo | null;
  upgrade?: UpgradeItem | null;
  /** 注意：Go 中为 map[string]any，因此保持为任意键值对象 */
  pipeline?: Record<string, unknown> | null;
  debug?: ImageDebug | null;
}

/** registry 接口的请求参数（通过 query 传递） */
export interface RegistryParams {
  token: string;
  deviceId: string;
  userId: string;
}

/**
 * registry 接口返回的 Agent 配置信息（DevopsResult.data 反序列化后的结构）。
 * 字段用于构造 AgentConfig，对应后台 ThirdPartyAgentStaticInfo 的核心子集。
 */
export interface RegistryResponse {
  projectId: string;
  agentId: string;
  secretKey: string;
  gateway: string;
  fileGateway: string;
  parallelTaskCount: number;
  dockerParallelTaskCount: number;
  language: string;
}
