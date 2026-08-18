/*
 * 最小可运行示例：实现 AgentHandler 并启动主循环。
 *
 * 运行前请准备好后台网关与鉴权信息。两种方式提供配置：
 *   1) AgentConfig.fromPropertiesFile('/path/to/.agent.properties')
 *   2) 直接 new AgentConfig({...})（本示例采用）
 *
 * 用 ts-node 运行：  npx ts-node examples/basic.ts
 */

import * as os from 'os';
import {
  AgentConfig,
  AgentHandler,
  AgentLoop,
  BuildJobType,
  HeartExtra,
  HeartbeatContext,
  ImageDebug,
  StartupInfo,
  ThirdPartyBuildInfo,
  UpgradeItem,
  AgentHeartbeatResponse,
  registry,
  RegistryParams
} from '../src';

const AGENT_VERSION = '1.0.0-nodejs-sdk';
const WORKER_VERSION = '';

/** 一个演示用的 Handler：仅打印日志，不做真实构建。 */
class DemoHandler implements AgentHandler {
  private runningBuilds = 0;
  private readonly maxParallel = 4;
  private upgrading = false;

  onStartup(): StartupInfo {
    return {
      hostName: os.hostname(),
      hostIp: firstNonLoopbackIp(),
      detectOS: `${os.platform()}_${os.release()}`,
      masterVersion: AGENT_VERSION,
      version: WORKER_VERSION,
    };
  }

  collectHeartExtra(_ctx: HeartbeatContext, upgradeEnable: boolean): HeartExtra {
    return {
      masterVersion: AGENT_VERSION,
      slaveVersion: WORKER_VERSION,
      hostName: os.hostname(),
      agentIp: firstNonLoopbackIp(),
      agentInstallPath: process.cwd(),
      startedUser: os.userInfo().username,
      props: {
        arch: os.arch(),
        jdkVersion: [],
        dockerInitFileMd5: { fileMd5: '', needUpgrade: false },
        osVersion: os.release(),
      },
      upgrade: upgradeEnable
        ? {
          workerVersion: WORKER_VERSION,
          goAgentVersion: AGENT_VERSION,
          jdkVersion: [],
          dockerInitFileInfo: { fileMd5: '', needUpgrade: false },
        }
        : null,
    };
  }

  isUpgrading(): boolean {
    return this.upgrading;
  }

  checkParallelTaskCount(): [boolean, boolean] {
    const normalCanRun = this.runningBuilds < this.maxParallel;
    // 本示例不支持 docker 构建
    return [false, normalCanRun];
  }

  hasRunningJob(): boolean {
    return this.runningBuilds > 0;
  }

  pipelineEnabled(): boolean {
    return false;
  }

  dockerDebugEnabled(): boolean {
    return false;
  }

  async onBuild(build: ThirdPartyBuildInfo): Promise<void> {
    this.runningBuilds++;
    console.log(`[demo] 领取到构建任务 buildId=${build.buildId} vmSeqId=${build.vmSeqId}`);
    try {
      // 这里应真正拉起 worker/执行器。示例仅模拟耗时。
      await new Promise((r) => setTimeout(r, 2000));
      console.log(`[demo] 构建完成 buildId=${build.buildId}`);
      // 真实场景应调用 loop.getApi().workerBuildFinish({...})
    } finally {
      this.runningBuilds--;
    }
  }

  onUpgrade(upgrade: UpgradeItem, hasBuild: boolean): void {
    console.log('[demo] 收到升级指令', upgrade, 'hasBuild=', hasBuild);
  }

  onPipeline(pipeline: Record<string, unknown>): void {
    console.log('[demo] 收到流水线指令', pipeline);
  }

  onImageDebug(debug: ImageDebug): void {
    console.log('[demo] 收到镜像调试指令 debugId=', debug.debugId);
  }

  onHeartbeatResp(resp: AgentHeartbeatResponse): void {
    console.log('[demo] 心跳响应，parallelTaskCount=', resp.parallelTaskCount);
  }

  onAgentDeleted(): void {
    console.warn('[demo] Agent 已被后台删除');
  }
}

function firstNonLoopbackIp(): string {
  const ifaces = os.networkInterfaces();
  for (const name of Object.keys(ifaces)) {
    for (const ni of ifaces[name] ?? []) {
      if (ni.family === 'IPv4' && !ni.internal) return ni.address;
    }
  }
  return '127.0.0.1';
}

async function main(): Promise<void> {
  const gateway = ""
  const regisResp = await registry(gateway, { token: "", deviceId: "", userId: "" });
  const config = new AgentConfig({
    gateway: gateway,
    projectId: regisResp.projectId,
    agentId: regisResp.agentId,
    secretKey: regisResp.secretKey,
    parallelTaskCount: regisResp.parallelTaskCount,
  });

  const loop = new AgentLoop({
    config,
    handler: new DemoHandler(),
    intervalMs: 5000,
    monitorFn: () => console.log('[demo] 采集监控指标...'),
    monitorIntervalMs: 60_000,
  });

  process.on('SIGINT', () => {
    console.log('收到 SIGINT，停止 agent 主循环');
    loop.stop();
  });

  await loop.run();
}

void main();

// 让 BuildJobType 被引用，避免示例中未使用告警（实际使用见 checkBuildType 逻辑）
void BuildJobType;
