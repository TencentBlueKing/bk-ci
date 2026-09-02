/*
 * BK-CI Agent SDK - docker 构建编排
 *
 * 移植 Go agent 的 src/pkg/job/build_docker.go 与 docker_runtime.go。
 * 用 docker/podman 拉起容器执行 worker：拉镜像 → create → start → wait → 取日志 → 结果。
 *
 * 容器内约定挂载（对齐 Go docker_runtime.go）：
 *   - worker-agent.jar → /data/worker-agent.jar（只读）
 *   - jdk17 目录       → /usr/local/jre（只读），jdk8 → /usr/local/jre8（只读）
 *     （仅 jdk8 时挂到 /usr/local/jre）
 *   - init.sh          → /data/init.sh（只读），entrypoint /bin/sh -c /data/init.sh
 *   - workspace/data   → DockerDataDir，logs → /data/devops/logs
 *
 * JDK env（DEVOPS_AGENT_JDK_*_PATH）仅在 linux 注入容器；mac/win 需用户自行设置（同 Go）。
 * 返回 {success, message}，由调用方用 api.workerBuildFinish 上报。
 */

import * as fs from 'fs';
import * as path from 'path';
import { DockerRunner } from './dockercli';
import { DockerOptions, ImagePullPolicy, ThirdPartyBuildInfo } from './types';

/** 容器内 worker.jar 路径。 */
export const ContainerWorkerJar = '/data/worker-agent.jar';
/** 容器内 init.sh 路径（entrypoint）。 */
export const EntryPointCmd = '/data/init.sh';
/** 容器内 jdk17 挂载点。 */
export const TargetJreDir = '/usr/local/jre';
/** 容器内 jdk8 挂载点。 */
export const TargetJre8Dir = '/usr/local/jre8';
/** 容器内日志目录。 */
export const DockerLogDir = '/data/devops/logs';
/** 容器内 workspace 数据目录（对应 Go constant_out.go DockerDataDir）。 */
export const DockerDataDir = '/data/devops/workspace';
/** 不挂载标记（对应 Go DockerNoMount）。 */
export const DockerNoMount = '__NO_MOUNT__';

const LocalDockerWorkSpaceDirName = 'docker_workspace';
const LocalDockerBuildTmpDirName = 'docker_build_tmp';
const longLogTag = 'toolong';

export interface DockerBuildOptions {
  /** 构建任务信息，须含 dockerBuildInfo。 */
  buildInfo: ThirdPartyBuildInfo;
  /** agent 工作目录。 */
  workDir: string;
  /** JDK17 目录（挂载到容器 /usr/local/jre）。为空则仅挂 jdk8 到 /usr/local/jre。 */
  jdk17DirPath?: string;
  /** JDK8 目录。 */
  jdk8DirPath?: string;
  /** worker-agent.jar 的宿主机绝对路径。 */
  workerJarPath: string;
  /** docker init 脚本的宿主机绝对路径（挂载到 /data/init.sh）。 */
  dockerInitScriptPath: string;
  /** 后台网关地址（写入容器 devops_gateway）。 */
  gateway: string;
  /** 项目 id（写入容器 devops_project_id）。 */
  projectId: string;
  /** 额外注入容器的环境变量。 */
  extraEnv?: Record<string, string>;
  /** 目标平台（决定是否注入 JDK env），默认当前平台。 */
  platform?: NodeJS.Platform;
  /** 日志上报回调（用于把 pull/容器日志转发到后台）。 */
  postLog?: (message: string) => void;
  /** 随机名后缀生成器（便于测试），默认随机 8 位。 */
  randSuffix?: () => string;
}

export interface DockerBuildResult {
  success: boolean;
  message: string;
}

function ensureGateway(gateway: string): string {
  if (gateway.startsWith('http://') || gateway.startsWith('https://')) return gateway;
  return 'http://' + gateway;
}

function randString(n: number): string {
  const chars = 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ';
  let s = '';
  for (let i = 0; i < n; i++) s += chars[Math.floor(Math.random() * chars.length)];
  return s;
}

// ── 用户 docker options 解析（对应 job_docker.parseApiDockerOptions）──
function parseApiDockerOptions(o: DockerOptions): string[] {
  const args: string[] = [];
  for (const v of o.volumes ?? []) {
    if (v.trim() === '') continue;
    args.push('--volume', v.trim());
  }
  for (const m of o.mounts ?? []) {
    if (m.trim() === '') continue;
    args.push('--mount', m.trim());
  }
  if ((o.gpus ?? '').trim() !== '') {
    args.push('--gpus', (o.gpus as string).trim());
  }
  if (o.privileged) {
    args.push('--privileged');
  }
  for (const n of o.network ?? []) {
    if (n.trim() === '') continue;
    args.push('--network', n.trim());
  }
  if ((o.user ?? '').trim() !== '') {
    args.push('--user', (o.user as string).trim());
  }
  return args;
}

function buildUserDockerArgs(o: DockerOptions): string[] {
  const argv = parseApiDockerOptions(o);
  for (let i = 0; i < argv.length; i++) {
    switch (argv[i]) {
      case '--volume':
      case '--mount':
      case '--network':
      case '--user':
      case '--gpus':
        if (i + 1 >= argv.length || argv[i + 1].trim() === '') {
          throw new Error(`docker option ${argv[i]} requires a non-empty value`);
        }
        i++;
        break;
    }
  }
  return argv;
}

function hasCustomNetwork(o: DockerOptions): boolean {
  return (o.network ?? []).length > 0;
}

function needLocalImageInspect(isLatest: boolean, policy: string): boolean {
  switch (policy) {
    case ImagePullPolicy.Always:
      return false;
    case ImagePullPolicy.IfNotPresent:
      return true;
    default:
      return !isLatest;
  }
}

function ifPullImage(localExist: boolean, isLatest: boolean, policy: string): boolean {
  switch (policy) {
    case ImagePullPolicy.Always:
      return true;
    case ImagePullPolicy.IfNotPresent:
      return !localExist;
    default:
      if (isLatest) return true;
      return !localExist;
  }
}

function hasJdk17Dir(jdk17DirPath?: string): boolean {
  if (!jdk17DirPath) return false;
  try {
    return fs.statSync(jdk17DirPath).isDirectory();
  } catch {
    return false;
  }
}

// ── 容器环境变量（对应 docker_runtime.go parseContainerEnv）──
function parseContainerEnv(opts: DockerBuildOptions): string[] {
  const d = opts.buildInfo.dockerBuildInfo;
  const platform = opts.platform ?? process.platform;
  const vars: string[] = [];
  vars.push('devops_project_id=' + opts.projectId);
  vars.push('devops_agent_id=' + (d?.agentId ?? ''));
  vars.push('devops_agent_secret_key=' + (d?.secretKey ?? ''));
  vars.push('devops_gateway=' + ensureGateway(opts.gateway));
  vars.push('agent_build_env=DOCKER');
  // mac/win 无法使用 agent 自带 JDK，仅 linux 注入
  if (hasJdk17Dir(opts.jdk17DirPath) && platform === 'linux') {
    vars.push('DEVOPS_AGENT_JDK_8_PATH=' + TargetJre8Dir + '/bin/java');
    vars.push('DEVOPS_AGENT_JDK_17_PATH=' + TargetJreDir + '/bin/java');
  }
  if (opts.extraEnv) {
    for (const [k, v] of Object.entries(opts.extraEnv)) {
      vars.push(`${k}=${v}`);
    }
  }
  return vars;
}

// ── mount 参数（对应 docker_runtime.go parseContainerMountArgs）──
async function parseContainerMountArgs(opts: DockerBuildOptions): Promise<string[]> {
  const b = opts.buildInfo;
  const args: string[] = [];

  if (hasJdk17Dir(opts.jdk17DirPath)) {
    args.push(
      '--mount',
      `type=bind,source=${opts.jdk17DirPath},target=${TargetJreDir},readonly`,
      '--mount',
      `type=bind,source=${opts.jdk8DirPath ?? ''},target=${TargetJre8Dir},readonly`
    );
  } else if (opts.jdk8DirPath) {
    args.push('--mount', `type=bind,source=${opts.jdk8DirPath},target=${TargetJreDir},readonly`);
  }

  args.push(
    '--mount',
    `type=bind,source=${opts.workerJarPath},target=${ContainerWorkerJar},readonly`,
    '--mount',
    `type=bind,source=${opts.dockerInitScriptPath},target=${EntryPointCmd},readonly`
  );

  let dataDir = path.join(
    opts.workDir,
    LocalDockerWorkSpaceDirName,
    'data',
    b.pipelineId,
    b.vmSeqId
  );
  if (b.workspace) {
    dataDir = b.workspace;
  }
  if (dataDir !== DockerNoMount) {
    await fs.promises.mkdir(dataDir, { recursive: true });
    args.push('--mount', `type=bind,source=${dataDir},target=${DockerDataDir}`);
  }

  const logsDir = path.join(
    opts.workDir,
    LocalDockerWorkSpaceDirName,
    'logs',
    b.buildId,
    b.vmSeqId
  );
  await fs.promises.mkdir(logsDir, { recursive: true });
  args.push('--mount', `type=bind,source=${logsDir},target=${DockerLogDir}`);

  return args;
}

/** 组装 docker create 参数（对应 docker_runtime.go buildDockerCreateArgs）。 */
export async function buildDockerCreateArgs(
  containerName: string,
  image: string,
  opts: DockerBuildOptions
): Promise<string[]> {
  const d = opts.buildInfo.dockerBuildInfo;
  const options: DockerOptions = d?.options ?? {};
  const mountArgs = await parseContainerMountArgs(opts);
  const userArgs = buildUserDockerArgs(options);

  const args: string[] = ['--name', containerName];
  args.push(...userArgs);
  if (!hasCustomNetwork(options)) {
    args.push('--network', 'bridge');
  }
  for (const e of parseContainerEnv(opts)) {
    args.push('-e', e);
  }
  args.push(...mountArgs);
  args.push('--entrypoint', '/bin/sh', image, '-c', EntryPointCmd);
  return args;
}

/**
 * 执行 docker 构建。runner 由调用方创建（可注入日志回调），opts 提供任务与路径信息。
 * 返回结果由调用方用 api.workerBuildFinish 上报（成功 sleep 8s 由调用方处理）。
 */
export async function runDockerBuild(
  runner: DockerRunner,
  opts: DockerBuildOptions
): Promise<DockerBuildResult> {
  const b = opts.buildInfo;
  const d = b.dockerBuildInfo;
  const postLog = opts.postLog ?? (() => undefined);

  if (!d) {
    return { success: false, message: 'dockerBuildInfo is missing' };
  }
  if (d.credential && d.credential.errMsg) {
    return { success: false, message: `get docker cred error: ${d.credential.errMsg}` };
  }

  const imageName = (d.image ?? '').trim();
  const imageStr = imageName.replace(/^https?:\/\//, '');

  const imageStrSub = imageStr.split(':');
  let isLatest = false;
  if (imageStrSub.length === 2 && imageStrSub[1] === 'latest') {
    isLatest = true;
  } else if (imageStrSub.length === 1) {
    isLatest = true;
  }
  const policy = d.imagePullPolicy ?? '';

  let localExist = false;
  if (needLocalImageInspect(isLatest, policy)) {
    try {
      localExist = await runner.imageExists(imageName);
    } catch (e) {
      return { success: false, message: `inspect docker image error: ${errMsg(e)}` };
    }
  }

  if (ifPullImage(localExist, isLatest, policy)) {
    if (isLatest) postLog('pull latest image');
    postLog(`start pull image: ${imageName}`);
    try {
      const out = await runner.pullImage(
        imageName,
        d.credential?.user ?? '',
        d.credential?.password ?? ''
      );
      if (out.trim() !== '') postLog(out);
    } catch (e) {
      return { success: false, message: `pull image ${imageName} error: ${errMsg(e)}` };
    }
  } else {
    postLog('use local exist image: ' + imageName);
  }

  // 创建 docker 构建临时空间
  const tmpDir = path.join(opts.workDir, LocalDockerBuildTmpDirName);
  try {
    await fs.promises.mkdir(tmpDir, { recursive: true });
  } catch (e) {
    return { success: false, message: `create docker tmp dir error: ${errMsg(e)}` };
  }

  const rand = opts.randSuffix ? opts.randSuffix() : randString(8);
  const containerName = `dispatch-${b.buildId}-${b.vmSeqId}-${rand}`;

  let createArgs: string[];
  try {
    createArgs = await buildDockerCreateArgs(containerName, imageStr, opts);
  } catch (e) {
    return { success: false, message: `parse docker options error: ${errMsg(e)}` };
  }

  let containerId: string;
  try {
    containerId = await runner.createContainer(createArgs);
  } catch (e) {
    return { success: false, message: `create container ${containerName} error: ${errMsg(e)}` };
  }

  try {
    try {
      await runner.startContainer(containerId);
    } catch (e) {
      return { success: false, message: `start container ${containerName} error: ${errMsg(e)}` };
    }

    let statusCode: number;
    try {
      statusCode = await runner.waitContainer(containerId);
    } catch (e) {
      return { success: false, message: `wait container ${containerName} error: ${errMsg(e)}` };
    }

    if (statusCode !== 0) {
      let msg = await readDockerLogFile(opts, longLogTag);
      const containerLog = await runner.containerLogs(containerId).catch(() => '');
      if (msg === '') {
        msg = containerLog;
      } else if (containerLog.trim() !== '') {
        postLog('docker container log: ' + containerLog);
      }
      if (msg === longLogTag) msg = '';
      return {
        success: false,
        message: `container ${containerName} exit code ${statusCode}: ${msg}`,
      };
    }

    return { success: true, message: '' };
  } finally {
    // 清理容器（对应 Go defer RemoveContainer）
    await runner.removeContainer(containerId).catch(() => undefined);
  }
}

/** 读取挂载出的 docker.log；超过 1000 字节返回 longLogTag（对应 Go logFile）。 */
async function readDockerLogFile(opts: DockerBuildOptions, tag: string): Promise<string> {
  const b = opts.buildInfo;
  const logFile = path.join(
    opts.workDir,
    LocalDockerWorkSpaceDirName,
    'logs',
    b.buildId,
    b.vmSeqId,
    'docker.log'
  );
  try {
    const content = await fs.promises.readFile(logFile);
    if (content.length > 1000) return tag;
    return content.toString('utf-8');
  } catch (e) {
    if ((e as NodeJS.ErrnoException).code === 'ENOENT') return '';
    return `read log file error ${errMsg(e)}`;
  }
}

function errMsg(e: unknown): string {
  return e instanceof Error ? e.message : String(e);
}
