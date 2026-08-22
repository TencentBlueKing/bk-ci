/*
 * BK-CI Agent SDK - 默认 worker-agent.jar 管理与升级
 *
 * 对齐 Go agent 的 pkg/upgrade/downloadUpgradeWorker + DoUpgradeOperation：
 *   1. 下载到 <workDir>/upgrade/worker-agent.jar，不直接覆盖正在使用的 jar；
 *   2. 使用 JDK17 运行 AgentVersionKt，候选版本合法后才允许替换；
 *   3. 比较 MD5，内容未变化时跳过替换；
 *   4. 通过同目录临时文件 + rename 更新工作目录中的 worker-agent.jar。
 *
 * 是否有构建运行、升级期间是否停止接单以及 finishUpgrade 上报由 AgentHandler
 * 编排；这样该管理器也可以脱离 AgentLoop 单独使用。
 */

import * as fs from 'fs';
import * as path from 'path';
import { WorkAgentFile, downloadWorkerJar, fileMd5 } from './download';
import { detectWorkerVersion } from './worker';

export interface DefaultWorkerJarManagerOptions {
  /** 后台网关地址。 */
  gateway: string;
  /** 下载 worker-agent.jar 使用的 Agent 鉴权头。 */
  authHeaders: Record<string, string>;
  /** Agent 工作目录。 */
  workDir: string;
  /** JDK17 的 java 可执行文件路径或 JDK 目录。 */
  jdk17Path: string;
  /** 正式 worker-agent.jar 路径；默认 <workDir>/worker-agent.jar。 */
  workerJarPath?: string;
  /** 候选升级文件目录；默认 <workDir>/upgrade。 */
  upgradeDir?: string;
  /** 下载超时（毫秒），默认使用 downloadWorkerJar 的 300000。 */
  timeoutMs?: number;
  /** 可选日志回调，默认走 console。 */
  logFn?: (msg: string) => void;
}

export interface WorkerJarState {
  version: string;
  md5: string;
}

export interface WorkerJarUpgradeResult extends WorkerJarState {
  /** 正式 worker-agent.jar 是否被替换。 */
  changed: boolean;
  /** 下载接口是否返回 304。 */
  notModified: boolean;
}

/**
 * 官方 worker-agent.jar 的默认管理器。
 *
 * initialize() 在启动前获取当前版本；当前 jar 缺失或无合法版本时，会尝试从后台
 * 下载并安装默认 jar。upgrade() 用于处理后台下发的 worker 升级指令。
 */
export class DefaultWorkerJarManager {
  private readonly opts: DefaultWorkerJarManagerOptions;
  private readonly workerJarPath: string;
  private readonly upgradeDir: string;
  private readonly log: (msg: string) => void;
  private version = '';
  private activeUpgrade?: Promise<WorkerJarUpgradeResult>;

  constructor(opts: DefaultWorkerJarManagerOptions) {
    this.opts = opts;
    this.workerJarPath = path.resolve(
      opts.workerJarPath ?? path.join(opts.workDir, WorkAgentFile)
    );
    this.upgradeDir = path.resolve(opts.upgradeDir ?? path.join(opts.workDir, 'upgrade'));
    this.log = opts.logFn ?? ((m) => console.info('[agent-sdk][worker-upgrade]', m));

    const upgradeJarPath = path.join(this.upgradeDir, WorkAgentFile);
    if (upgradeJarPath === this.workerJarPath) {
      throw new Error('upgradeDir must not contain the active worker-agent.jar');
    }
  }

  /** 当前已确认合法的 worker 版本；initialize 前为空字符串。 */
  getVersion(): string {
    return this.version;
  }

  /** 正式 worker-agent.jar 的绝对路径。 */
  getWorkerJarPath(): string {
    return this.workerJarPath;
  }

  /**
   * 启动初始化：优先使用当前 jar；缺失或版本非法时从后台下载默认 jar 自愈。
   * 初始化失败会抛异常，避免 Agent 用空版本启动后继续领取构建。
   */
  async initialize(): Promise<WorkerJarState> {
    const version = await this.detectVersion(this.workerJarPath, path.dirname(this.workerJarPath));
    if (version !== '') {
      this.version = version;
      const md5 = await fileMd5(this.workerJarPath);
      this.log(`worker initialized: version=${version}, md5=${md5}`);
      return { version, md5 };
    }

    this.log('active worker is missing or invalid, downloading the default worker-agent.jar');
    const result = await this.upgrade();
    return { version: result.version, md5: result.md5 };
  }

  /**
   * 下载、校验并按需替换默认 worker-agent.jar。
   * 同一个管理器上的并发调用会复用同一次升级，避免重复下载和相互覆盖。
   */
  upgrade(): Promise<WorkerJarUpgradeResult> {
    if (this.activeUpgrade) {
      return this.activeUpgrade;
    }

    const task = this.doUpgrade();
    const clearActiveUpgrade = (): void => {
      if (this.activeUpgrade === task) {
        this.activeUpgrade = undefined;
      }
    };
    this.activeUpgrade = task;
    void task.then(clearActiveUpgrade, clearActiveUpgrade);
    return task;
  }

  private async doUpgrade(): Promise<WorkerJarUpgradeResult> {
    await fs.promises.mkdir(this.upgradeDir, { recursive: true });
    const download = await downloadWorkerJar(
      this.opts.gateway,
      this.opts.authHeaders,
      this.upgradeDir,
      this.opts.timeoutMs
    );
    const upgradeJarPath = path.join(this.upgradeDir, WorkAgentFile);
    const version = await this.detectVersion(upgradeJarPath, this.upgradeDir);
    if (version === '') {
      throw new Error(`downloaded worker version is invalid: ${upgradeJarPath}`);
    }

    const currentMd5 = await fileMd5(this.workerJarPath);
    const changed = currentMd5 !== download.md5;
    if (changed) {
      await atomicCopyFile(upgradeJarPath, this.workerJarPath, download.md5);
      this.log(
        `worker upgraded: version=${version}, oldMd5=${currentMd5 || '<missing>'}, newMd5=${download.md5}`
      );
    } else {
      this.log(`worker already up-to-date: version=${version}, md5=${download.md5}`);
    }

    this.version = version;
    return {
      version,
      md5: download.md5,
      changed,
      notModified: download.notModified,
    };
  }

  private detectVersion(workerJarPath: string, workDir: string): Promise<string> {
    return detectWorkerVersion({
      jdk17Path: this.opts.jdk17Path,
      workerJarPath,
      workDir,
      logFn: this.log,
    });
  }
}

/** 将已校验的候选文件复制到目标目录后原子替换正式文件。 */
async function atomicCopyFile(source: string, target: string, expectedMd5: string): Promise<void> {
  await fs.promises.mkdir(path.dirname(target), { recursive: true });
  const tempPath = `${target}.upgrade.${process.pid}.${Date.now()}`;
  try {
    await fs.promises.copyFile(source, tempPath);
    const copiedMd5 = await fileMd5(tempPath);
    if (copiedMd5 !== expectedMd5) {
      throw new Error(`copied worker md5 not match: expected=${expectedMd5}, actual=${copiedMd5}`);
    }
    await fs.promises.rename(tempPath, target);
  } catch (e) {
    await fs.promises.rm(tempPath, { force: true }).catch(() => undefined);
    throw e;
  }
}
