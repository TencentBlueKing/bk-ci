/*
 * BK-CI Agent SDK - 配置解析与鉴权
 *
 * 对应 Go agent 的 src/pkg/config/config.go 与 constant.go。
 * 负责：解析 .agent.properties(INI key=value)、构造鉴权头、规整网关地址。
 */

import * as fs from 'fs';
import * as path from 'path';

/** 鉴权头常量（对应 Go config/constant.go） */
export const AuthHeader = {
  BuildType: 'X-DEVOPS-BUILD-TYPE',
  ProjectId: 'X-DEVOPS-PROJECT-ID',
  AgentId: 'X-DEVOPS-AGENT-ID',
  SecretKey: 'X-DEVOPS-AGENT-SECRET-KEY',
  /** 日志接口额外携带 */
  BuildId: 'X-DEVOPS-BUILD-ID',
  VmSeqId: 'X-DEVOPS-VM-SID',
} as const;

export const BuildTypeAgent = 'AGENT';

/** Agent 状态（对应 Go AgentStatus*） */
export const AgentStatus = {
  ImportOk: 'IMPORT_OK',
  Delete: 'DELETE',
} as const;

/** .agent.properties 中的配置键（对应 Go config/config.go 的 Key*） */
export const ConfigKey = {
  ProjectId: 'devops.project.id',
  AgentId: 'devops.agent.id',
  SecretKey: 'devops.agent.secret.key',
  Gateway: 'landun.gateway',
  FileGateway: 'landun.fileGateway',
  ParallelTaskCount: 'devops.parallel.task.count',
  DockerParallelTaskCount: 'devops.docker.parallel.task.count',
  EnvType: 'landun.env',
  SlaveUser: 'devops.slave.user',
  CollectorOn: 'devops.agent.collectorOn',
  RequestTimeoutSec: 'devops.agent.request.timeout.sec',
  IgnoreLocalIps: 'devops.agent.ignoreLocalIps',
  LogsKeepHours: 'devops.agent.logs.keep.hours',
  EnableDockerBuild: 'devops.docker.enable',
  Language: 'devops.language',
  EnablePipeline: 'devops.pipeline.enable',
} as const;

/** SDK 运行所需的 Agent 配置（对应 Go AgentConfig 的核心子集） */
export interface AgentConfigOptions {
  gateway: string;
  fileGateway?: string;
  projectId: string;
  agentId: string;
  secretKey: string;
  buildType?: string;
  parallelTaskCount?: number;
  dockerParallelTaskCount?: number;
  envType?: string;
  slaveUser?: string;
  /** 请求超时（秒），默认 5 */
  timeoutSec?: number;
  ignoreLocalIps?: string;
  logsKeepHours?: number;
  enableDockerBuild?: boolean;
  language?: string;
  enablePipeline?: boolean;
}

/**
 * Agent 配置。可通过构造函数直接注入，或用 AgentConfig.fromPropertiesFile 从
 * .agent.properties 加载。字段在运行时会被心跳响应动态更新（gateway/并发数等）。
 */
export class AgentConfig {
  gateway: string;
  fileGateway: string;
  projectId: string;
  agentId: string;
  secretKey: string;
  buildType: string;
  parallelTaskCount: number;
  dockerParallelTaskCount: number;
  envType: string;
  slaveUser: string;
  timeoutSec: number;
  ignoreLocalIps: string;
  logsKeepHours: number;
  enableDockerBuild: boolean;
  language: string;
  enablePipeline: boolean;

  constructor(opts: AgentConfigOptions) {
    if (!opts.projectId) throw new Error('invalid config: projectId is required');
    if (!opts.agentId) throw new Error('invalid config: agentId is required');
    if (!opts.secretKey) throw new Error('invalid config: secretKey is required');
    if (!opts.gateway) throw new Error('invalid config: gateway is required');

    this.gateway = opts.gateway;
    this.fileGateway = opts.fileGateway ?? '';
    this.projectId = opts.projectId;
    this.agentId = opts.agentId;
    this.secretKey = opts.secretKey;
    this.buildType = opts.buildType ?? BuildTypeAgent;
    this.parallelTaskCount = opts.parallelTaskCount ?? 4;
    this.dockerParallelTaskCount = opts.dockerParallelTaskCount ?? 4;
    this.envType = opts.envType ?? '';
    this.slaveUser = opts.slaveUser ?? '';
    this.timeoutSec = opts.timeoutSec ?? 5;
    this.ignoreLocalIps = opts.ignoreLocalIps ?? '127.0.0.1';
    this.logsKeepHours = opts.logsKeepHours ?? 96;
    this.enableDockerBuild = opts.enableDockerBuild ?? false;
    this.language = opts.language ?? 'zh_CN';
    this.enablePipeline = opts.enablePipeline ?? false;
  }

  /** 生成 4 个鉴权头（对应 Go GetAuthHeaderMap） */
  getAuthHeaderMap(): Record<string, string> {
    return {
      [AuthHeader.BuildType]: this.buildType,
      [AuthHeader.ProjectId]: this.projectId,
      [AuthHeader.AgentId]: this.agentId,
      [AuthHeader.SecretKey]: this.secretKey,
    };
  }

  /** 规整网关地址：无 http(s) 前缀则补 http://（对应 Go GetGateWay） */
  getGateway(): string {
    if (this.gateway.startsWith('http://') || this.gateway.startsWith('https://')) {
      return this.gateway;
    }
    return 'http://' + this.gateway;
  }

  /**
   * 从 .agent.properties 文件加载配置。
   * @param filePath 配置文件路径。默认取 cwd 下的 .agent.properties。
   */
  static fromPropertiesFile(filePath?: string): AgentConfig {
    const p = filePath ?? path.join(process.cwd(), '.agent.properties');
    const raw = fs.readFileSync(p, 'utf-8');
    const kv = parseProperties(raw);

    const get = (k: string): string => (kv[k] ?? '').trim();
    const getInt = (k: string, def: number): number => {
      const v = get(k);
      if (v === '') return def;
      const n = parseInt(v, 10);
      return Number.isNaN(n) ? def : n;
    };
    const getBool = (k: string, def: boolean): boolean => {
      const v = get(k).toLowerCase();
      if (v === '') return def;
      return v === 'true' || v === '1' || v === 'yes' || v === 'on';
    };

    return new AgentConfig({
      projectId: get(ConfigKey.ProjectId),
      agentId: get(ConfigKey.AgentId),
      secretKey: get(ConfigKey.SecretKey),
      gateway: get(ConfigKey.Gateway),
      fileGateway: get(ConfigKey.FileGateway),
      parallelTaskCount: getInt(ConfigKey.ParallelTaskCount, 4),
      dockerParallelTaskCount: getInt(ConfigKey.DockerParallelTaskCount, 4),
      envType: get(ConfigKey.EnvType),
      slaveUser: get(ConfigKey.SlaveUser),
      timeoutSec: getInt(ConfigKey.RequestTimeoutSec, 5),
      ignoreLocalIps: get(ConfigKey.IgnoreLocalIps) || '127.0.0.1',
      logsKeepHours: getInt(ConfigKey.LogsKeepHours, 96),
      enableDockerBuild: getBool(ConfigKey.EnableDockerBuild, false),
      language: get(ConfigKey.Language) || 'zh_CN',
      enablePipeline: getBool(ConfigKey.EnablePipeline, false),
    });
  }

  /**
   * 由 registry 接口返回的数据构造 AgentConfig。
   * 通常配合 api.registry(gateway, {token, deviceId, userId}) 使用：
   *   const info = await registry(gateway, { token, deviceId, userId });
   *   const config = AgentConfig.fromRegistry(info);
   */
  static fromRegistry(info: {
    projectId: string;
    agentId: string;
    secretKey: string;
    gateway: string;
    fileGateway?: string;
    parallelTaskCount?: number;
    dockerParallelTaskCount?: number;
  }): AgentConfig {
    return new AgentConfig({
      projectId: info.projectId,
      agentId: info.agentId,
      secretKey: info.secretKey,
      gateway: info.gateway,
      fileGateway: info.fileGateway,
      parallelTaskCount: info.parallelTaskCount,
      dockerParallelTaskCount: info.dockerParallelTaskCount,
    });
  }
}

/**
 * 解析 properties(INI key=value) 文本为键值对。
 * 支持 # 与 ; 注释行，忽略空行；只取第一个 = 之前为 key。
 */
export function parseProperties(content: string): Record<string, string> {
  const result: Record<string, string> = {};
  const lines = content.split(/\r?\n/);
  for (const line of lines) {
    const trimmed = line.trim();
    if (trimmed === '' || trimmed.startsWith('#') || trimmed.startsWith(';')) {
      continue;
    }
    const idx = trimmed.indexOf('=');
    if (idx < 0) continue;
    const key = trimmed.slice(0, idx).trim();
    const value = trimmed.slice(idx + 1).trim();
    if (key !== '') {
      result[key] = value;
    }
  }
  return result;
}
