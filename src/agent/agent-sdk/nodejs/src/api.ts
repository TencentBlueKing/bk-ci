/*
 * BK-CI Agent SDK - API 接口层
 *
 * 对应 Go agent 的 src/pkg/api/api.go。封装与后台交互的 HTTP 接口，
 * 路径、方法、请求体与 Go 版本严格对齐，并自动注入鉴权头。
 */

import { AgentConfig, AuthHeader } from './config';
import { AgentResult, DevopsResult, HttpClient, isNotOk } from './httpClient';
import {
  AskInfo,
  AskResp,
  ImageDebug,
  ImageDebugFinish,
  LogMessage,
  PipelineResponse,
  RegistryParams,
  RegistryResponse,
  ThirdPartyAgentStartInfo,
  ThirdPartyBuildWithStatus,
  DevopsError,
} from './types';

/** 各接口的路径（对应 Go api.go 中的 buildUrl 参数） */
export const ApiPath = {
  Startup: '/ms/environment/api/buildAgent/agent/thirdPartyAgent/startup',
  Ask: '/ms/dispatch/api/buildAgent/agent/thirdPartyAgent/ask',
  WorkerBuildFinish: '/ms/dispatch/api/buildAgent/agent/thirdPartyAgent/workerBuildFinish',
  Pipelines: '/ms/environment/api/buildAgent/agent/thirdPartyAgent/agents/pipelines',
  Upgrade: '/ms/dispatch/api/buildAgent/agent/thirdPartyAgent/upgrade',
  DockerStartupDebug: '/ms/dispatch/api/buildAgent/agent/thirdPartyAgent/docker/startupDebug',
  DockerDebugStatus: '/ms/dispatch/api/buildAgent/agent/thirdPartyAgent/docker/debug/status',
  Registry: '/ms/environment/api/external/thirdPartyAgent/registry',
  Log: '/ms/log/api/build/logs',
  LogRed: '/ms/log/api/build/logs/red',
  LogYellow: '/ms/log/api/build/logs/yellow',
} as const;

function ensureGateway(gateway: string): string {
  if (gateway.startsWith('http://') || gateway.startsWith('https://')) {
    return gateway;
  }
  return 'http://' + gateway;
}

/**
 * 通过 token/deviceId/userId 向后台注册/获取 Agent 配置信息。
 *
 * 该接口发生在尚未拥有 AgentConfig（secretKey 等）之前，因此不依赖 AgentApi 实例，
 * 也不携带 X-DEVOPS-* 鉴权头，凭 token 通过 query 参数鉴权。
 *
 * @param gateway 后台网关地址（可不带 http:// 前缀，函数会自动补全）
 * @param params  token / deviceId / userId
 * @param client  可选的 HttpClient，默认新建一个
 * @returns 后台返回的 RegistryResponse（可用于构造 AgentConfig）
 */
export async function registry(
  gateway: string,
  params: RegistryParams,
  client: HttpClient = new HttpClient()
): Promise<RegistryResponse> {
  const query =
    `?token=${encodeURIComponent(params.token)}` +
    `&deviceId=${encodeURIComponent(params.deviceId)}` +
    `&userId=${encodeURIComponent(params.userId)}`;
  const url = ensureGateway(gateway) + ApiPath.Registry + query;

  const result = await client.intoDevopsResult<RegistryResponse>({
    method: 'GET',
    url,
  });
  if (isNotOk(result)) {
    throw new Error(`registry failed: status=${result.status} message=${result.message}`);
  }
  if (!result.data) {
    throw new Error('registry failed: empty data in response');
  }
  return result.data;
}


/**
 * AgentApi 封装所有后台接口。持有 config（提供鉴权头与网关）与 HttpClient。
 */
export class AgentApi {
  constructor(
    private readonly config: AgentConfig,
    private readonly client: HttpClient
  ) {}

  private buildUrl(pathAndQuery: string): string {
    return this.config.getGateway() + pathAndQuery;
  }

  private authHeaders(): Record<string, string> {
    return this.config.getAuthHeaderMap();
  }

  /** 轮询领任务（对应 Go Ask）。返回 AgentResult，其 data 为 AskResp。 */
  async ask(info: AskInfo): Promise<AgentResult<AskResp>> {
    return this.client.intoAgentResult<AskResp>({
      method: 'POST',
      url: this.buildUrl(ApiPath.Ask),
      headers: this.authHeaders(),
      body: info,
    });
  }

  /** Agent 启动上报（对应 Go AgentStartup） */
  async agentStartup(startInfo: ThirdPartyAgentStartInfo): Promise<DevopsResult> {
    return this.client.intoDevopsResult({
      method: 'POST',
      url: this.buildUrl(ApiPath.Startup),
      headers: this.authHeaders(),
      body: startInfo,
    });
  }

  /** 构建任务完成上报（对应 Go WorkerBuildFinish） */
  async workerBuildFinish(buildInfo: ThirdPartyBuildWithStatus): Promise<DevopsResult> {
    return this.client.intoDevopsResult({
      method: 'POST',
      url: this.buildUrl(ApiPath.WorkerBuildFinish),
      headers: this.authHeaders(),
      body: buildInfo,
    });
  }

  /** 流水线执行状态上报（对应 Go UpdatePipelineStatus） */
  async updatePipelineStatus(response: PipelineResponse): Promise<DevopsResult> {
    return this.client.intoDevopsResult({
      method: 'PUT',
      url: this.buildUrl(ApiPath.Pipelines),
      headers: this.authHeaders(),
      body: response,
    });
  }

  /** 升级完成上报（对应 Go FinishUpgrade） */
  async finishUpgrade(success: boolean): Promise<AgentResult> {
    return this.client.intoAgentResult({
      method: 'DELETE',
      url: this.buildUrl(`${ApiPath.Upgrade}?success=${success}`),
      headers: this.authHeaders(),
    });
  }

  /** 镜像调试完成上报（对应 Go FinishDockerDebug） */
  async finishDockerDebug(
    imageDebug: ImageDebug,
    success: boolean,
    debugUrl: string,
    error?: DevopsError | null
  ): Promise<DevopsResult> {
    const body: ImageDebugFinish = {
      projectId: imageDebug.projectId,
      debugId: imageDebug.debugId,
      pipelineId: imageDebug.pipelineId,
      debugUrl,
      success,
      error: error ?? null,
    };
    return this.client.intoDevopsResult({
      method: 'POST',
      url: this.buildUrl(ApiPath.DockerStartupDebug),
      headers: this.authHeaders(),
      body,
    });
  }

  /** 查询镜像调试状态（对应 Go FetchDockerDebugStatus） */
  async fetchDockerDebugStatus(debugId: number): Promise<DevopsResult> {
    return this.client.intoDevopsResult({
      method: 'GET',
      url: this.buildUrl(`${ApiPath.DockerDebugStatus}?debugId=${debugId}`),
      headers: this.authHeaders(),
    });
  }

  /** 上报普通日志（对应 Go AddLogLine） */
  async addLogLine(buildId: string, message: LogMessage, vmSeqId: string): Promise<DevopsResult> {
    return this.postLog(ApiPath.Log, buildId, message, vmSeqId);
  }

  /** 上报红色（错误）日志（对应 Go AddLogRedLine） */
  async addLogRedLine(buildId: string, message: LogMessage, vmSeqId: string): Promise<DevopsResult> {
    return this.postLog(ApiPath.LogRed, buildId, message, vmSeqId);
  }

  /** 上报黄色（警告）日志（对应 Go AddLogYellowLine） */
  async addLogYellowLine(
    buildId: string,
    message: LogMessage,
    vmSeqId: string
  ): Promise<DevopsResult> {
    return this.postLog(ApiPath.LogYellow, buildId, message, vmSeqId);
  }

  private postLog(
    path: string,
    buildId: string,
    message: LogMessage,
    vmSeqId: string
  ): Promise<DevopsResult> {
    const headers = this.authHeaders();
    headers[AuthHeader.BuildId] = buildId;
    headers[AuthHeader.VmSeqId] = vmSeqId;
    return this.client.intoDevopsResult({
      method: 'POST',
      url: this.buildUrl(path),
      headers,
      body: message,
    });
  }
}
