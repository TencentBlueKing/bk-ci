/*
 * BK-CI Agent SDK - 物理机 worker 构建
 *
 * 对应 Go agent 的 src/pkg/job/do_build.go（Unix）、do_build_win.go（Windows）、
 * build.go（env 组装）。启动 worker-agent.jar 执行构建：
 *   java ... -jar worker-agent.jar <base64(JSON(buildInfo))>
 *
 * 平台差异（对齐 Go）：
 *   - Unix(linux/darwin)：写两层脚本。start.sh 内 `cd workDir` + java 命令；
 *     prepare_start.sh 用 `exec $SHELL -l start.sh` 读取用户 profile（tcsh 语序不同）。
 *     spawn prepare 脚本，detached:true(setpgid) 以便结束时按进程组清理。
 *   - Windows：直接 spawn java.exe + 参数数组，无 login shell、无进程组。
 *
 * JDK：SDK 不下载/安装 JDK，只认路径。jdk17Path/jdk8Path 可为 java 可执行文件
 * 或 JDK 目录（按平台拼 bin/java）。17 优先、缺失回退 8（对应 GetJavaLatest）。
 *
 * 返回 {success, message}，由调用方（如 DefaultBuildRunner）用 api.workerBuildFinish 上报。
 */

import * as fs from 'fs';
import * as path from 'path';
import { spawn } from 'child_process';
import { ThirdPartyBuildInfo } from './types';

export interface WorkerBuildOptions {
  /** 构建任务信息（来自 ask 响应的 build 字段）。 */
  buildInfo: ThirdPartyBuildInfo;
  /** agent 工作目录（脚本、error_msg 文件、cwd 均基于它）。 */
  workDir: string;
  /** JDK17 的 java 可执行文件路径或 JDK 目录（优先使用）。 */
  jdk17Path?: string;
  /** JDK8 的 java 可执行文件路径或 JDK 目录（jdk17 缺失时回退）。 */
  jdk8Path?: string;
  /** worker-agent.jar 的绝对路径。 */
  workerJarPath: string;
  /** 后台网关地址（写入 DEVOPS_GATEWAY，需已规整含 http 前缀）。 */
  gateway: string;
  /** 文件网关地址（写入 DEVOPS_FILE_GATEWAY）。 */
  fileGateway?: string;
  /**
   * 项目 id / agent id / 密钥。用于在 workDir 下缺少 .agent.properties 时自动补齐该文件，
   * 因为默认 worker 启动会读取 <cwd>/.agent.properties 获取这些信息。
   * projectId 不传时回退用 buildInfo.projectId。
   */
  projectId?: string;
  agentId?: string;
  secretKey?: string;
  /** agent 版本（写入 DEVOPS_AGENT_VERSION）。 */
  agentVersion: string;
  /** worker 版本（写入 DEVOPS_WORKER_VERSION）。 */
  workerVersion: string;
  /** 语言（写入 BK_CI_LOCALE_LANGUAGE），默认 zh_CN。 */
  language?: string;
  /** 额外注入的环境变量（对应 Go envs.GApiEnvVars）。 */
  extraEnv?: Record<string, string>;
  /**
   * 是否探测用户 SHELL（对应 Go DetectShell）。true 时 Unix 用 $SHELL，否则 /bin/bash。
   * 默认 false。
   */
  detectShell?: boolean;
  /**
   * 是否启用进程组退出（Unix setpgid + 结束后 kill 进程组，对应 DEVOPS_AGENT_ENABLE_EXIT_GROUP）。
   * 默认 true。
   */
  enableExitGroup?: boolean;
  /** 可选日志回调，默认走 console。 */
  logFn?: (msg: string) => void;
}

export interface WorkerBuildResult {
  success: boolean;
  message: string;
}

/** worker-agent.jar 版本检测参数。 */
export interface DetectWorkerVersionOptions {
  /** JDK17 的 java 可执行文件路径或 JDK 目录。 */
  jdk17Path: string;
  /** worker-agent.jar 路径。 */
  workerJarPath: string;
  /** 命令工作目录；默认使用 worker-agent.jar 所在目录。 */
  workDir?: string;
  /** 可选日志回调，默认走 console。 */
  logFn?: (msg: string) => void;
}

const macosJdkBinPath = path.join('Contents', 'Home', 'bin', 'java');

/**
 * 将 jdkPath（可为 java 可执行文件或 JDK 目录）解析为 java 可执行文件路径。
 * 对应 Go third_components/jdk.go GetJava 的平台拼接。
 * 若入参已指向文件（basename 以 java 开头），则原样返回。
 */
export function resolveJavaBin(jdkPath: string, platform: NodeJS.Platform = process.platform): string {
  const base = path.basename(jdkPath).toLowerCase();
  if (base === 'java' || base === 'java.exe') {
    return jdkPath;
  }
  // 视为 JDK 目录，按平台拼接 bin/java
  if (platform === 'darwin') {
    return path.join(jdkPath, macosJdkBinPath);
  }
  if (platform === 'win32') {
    return path.join(jdkPath, 'bin', 'java.exe');
  }
  return path.join(jdkPath, 'bin', 'java');
}

/**
 * 选择最终使用的 java 可执行文件：jdk17 优先，其可执行文件不存在时回退 jdk8。
 * 对应 Go GetJavaLatest。返回空串表示两者都不可用。
 */
export function resolveLatestJava(
  jdk17Path: string | undefined,
  jdk8Path: string | undefined,
  platform: NodeJS.Platform = process.platform
): string {
  if (jdk17Path) {
    const j17 = resolveJavaBin(jdk17Path, platform);
    if (fs.existsSync(j17)) {
      return j17;
    }
  }
  if (jdk8Path) {
    return resolveJavaBin(jdk8Path, platform);
  }
  return '';
}

/**
 * 使用 JDK17 检测 worker-agent.jar 的版本。
 *
 * 执行方式与 Go agent 的 DetectWorkerVersion 一致：
 *   java -Djava.io.tmpdir=<workDir>/build_tmp -Xmx256m -cp <jar>
 *     com.tencent.devops.agent.AgentVersionKt
 *
 * JVM 可能在版本号前输出临时目录、内存等告警，因此不会直接 trim 整段输出，
 * 而是逐行查找合法版本。检测失败或输出中没有合法版本时返回空字符串。
 */
export async function detectWorkerVersion(opts: DetectWorkerVersionOptions): Promise<string> {
  const log = opts.logFn ?? ((m: string) => console.info('[agent-sdk][worker]', m));
  const javaBin = path.resolve(resolveJavaBin(opts.jdk17Path));
  const workerJarPath = path.resolve(opts.workerJarPath);
  const workDir = path.resolve(opts.workDir ?? path.dirname(workerJarPath));

  if (!fs.existsSync(javaBin)) {
    log(`detect worker version failed: jdk17 java missing: ${javaBin}`);
    return '';
  }
  if (!fs.existsSync(workerJarPath)) {
    log(`detect worker version failed: worker jar missing: ${workerJarPath}`);
    return '';
  }

  const tmpDir = path.join(workDir, 'build_tmp');
  try {
    await fs.promises.mkdir(tmpDir, { recursive: true });
  } catch (e) {
    log(`detect worker version failed: cannot create temp dir ${tmpDir}: ${errorMessage(e)}`);
    return '';
  }

  const args = [
    '-Djava.io.tmpdir=' + tmpDir,
    '-Xmx256m',
    '-cp',
    workerJarPath,
    'com.tencent.devops.agent.AgentVersionKt',
  ];
  const result = await spawnAndCollect(javaBin, args, workDir);
  if (result.error) {
    log(
      `detect worker version failed: ${result.error.message}` +
        (result.output === '' ? '' : `, output: ${result.output.trim()}`)
    );
    return '';
  }

  const version = parseWorkerVersion(result.output);
  if (version === '') {
    log('detect worker version failed: no valid version found in command output');
  } else {
    log(`detected worker version: ${version}`);
  }
  return version;
}

/**
 * 从 AgentVersionKt 的混合输出中解析 worker 版本。
 * 兼容 v1.2.3、v1.2.3-RELEASE、v1.2.3-SNAPSHOT 和 v1.2.3-beta.4。
 */
export function parseWorkerVersion(output: string): string {
  const legacyVersion = /^v\d+\.\d+\.\d+(?:-RELEASE|-SNAPSHOT)?$/;
  const basicVersion = /^v\d+\.\d+\.\d+$/;
  const suffixedVersion = /^v\d+\.\d+\.\d+-([^-\.\s]+)\.(\d+)$/;

  for (const outputLine of output.split(/\r?\n/)) {
    let line = outputLine.trim();
    if (line === '' || line.includes(' ') || line.includes('OPTIONS')) {
      continue;
    }
    if (line.length > 64) {
      line = line.slice(0, 64);
    }

    const suffixMatch = suffixedVersion.exec(line);
    const matchesCurrentFormat =
      basicVersion.test(line) ||
      (suffixMatch !== null && !/^\d+$/.test(suffixMatch[1]));
    if (matchesCurrentFormat || legacyVersion.test(line)) {
      return line;
    }
  }
  return '';
}

/**
 * 组装 worker 环境变量（对应 Go build.go runBuild 的 goEnv）。
 * 含新旧两套 key 以保持兼容。
 */
export function buildWorkerEnv(opts: WorkerBuildOptions): Record<string, string> {
  const b = opts.buildInfo;
  const env: Record<string, string> = {
    DEVOPS_AGENT_VERSION: opts.agentVersion,
    DEVOPS_WORKER_VERSION: opts.workerVersion,
    DEVOPS_PROJECT_ID: b.projectId,
    DEVOPS_BUILD_ID: b.buildId,
    DEVOPS_VM_SEQ_ID: b.vmSeqId,
    // deprecated 兼容旧 key
    DEVOPS_SLAVE_VERSION: opts.workerVersion,
    PROJECT_ID: b.projectId,
    BUILD_ID: b.buildId,
    VM_SEQ_ID: b.vmSeqId,
    DEVOPS_FILE_GATEWAY: opts.fileGateway ?? '',
    DEVOPS_GATEWAY: opts.gateway,
    BK_CI_LOCALE_LANGUAGE: opts.language ?? 'zh_CN',
  };

  if (opts.jdk8Path) {
    const j8 = resolveJavaBin(opts.jdk8Path);
    if (fs.existsSync(j8)) env.DEVOPS_AGENT_JDK_8_PATH = j8;
  }
  if (opts.jdk17Path) {
    const j17 = resolveJavaBin(opts.jdk17Path);
    if (fs.existsSync(j17)) env.DEVOPS_AGENT_JDK_17_PATH = j17;
  }

  if (opts.extraEnv) {
    for (const [k, v] of Object.entries(opts.extraEnv)) {
      env[k] = v;
    }
  }
  return env;
}

/** base64(JSON(buildInfo))，作为 worker 的唯一参数（对应 Go getEncodedBuildInfo）。 */
function getEncodedBuildInfo(buildInfo: ThirdPartyBuildInfo): string {
  return Buffer.from(JSON.stringify(buildInfo), 'utf-8').toString('base64');
}

/**
 * 确保 <workDir>/.agent.properties 存在。默认 worker 启动时会读取该文件获取项目与鉴权信息，
 * SDK 直接拉起 worker 时该文件可能缺失。若不存在则用 options 中的配置生成，仅写入
 * worker 必需的 5 个 key（对应用户约定）：
 *   devops.project.id / devops.agent.id / devops.agent.secret.key /
 *   landun.gateway / landun.fileGateway
 * 已存在则不覆盖（尊重用户/agent 既有配置）。
 */
async function ensureAgentProperties(
  opts: WorkerBuildOptions,
  log: (msg: string) => void
): Promise<void> {
  const propFile = path.join(opts.workDir, '.agent.properties');
  if (fs.existsSync(propFile)) {
    return;
  }

  const projectId = opts.projectId ?? opts.buildInfo.projectId;
  const lines = [
    `devops.project.id=${projectId}`,
    `devops.agent.id=${opts.agentId ?? ''}`,
    `devops.agent.secret.key=${opts.secretKey ?? ''}`,
    `landun.gateway=${opts.gateway}`,
    `landun.fileGateway=${opts.fileGateway ?? ''}`,
  ];
  await fs.promises.mkdir(opts.workDir, { recursive: true });
  await fs.promises.writeFile(propFile, lines.join('\n') + '\n', { mode: 0o644 });
  log(`generated .agent.properties at ${propFile}`);
}

/** worker 异常信息文件路径（对应 Go getWorkerErrorMsgFile）。 */
function getWorkerErrorMsgFile(workDir: string, buildId: string, vmSeqId: string): string {
  return path.join(workDir, 'build_tmp', `${buildId}_${vmSeqId}_build_msg.log`);
}

const BUILDER_PROCESS_WAS_KILLED = 'build process was killed';

function getCurrentShell(detectShell: boolean): string {
  if (detectShell) {
    const shell = (process.env.SHELL ?? '').trim();
    return shell === '' ? '/bin/bash' : shell;
  }
  return '/bin/bash';
}

/**
 * 生成 prepare 脚本内容（外层套娃，exec login shell 以读取 profile）。
 * 对应 Go getShellLines：tcsh 语序不同。
 */
function getPrepareScriptLines(shell: string, scriptFile: string): string[] {
  if (shell === '/bin/tcsh') {
    return ['#!' + shell, `exec ${shell} ${scriptFile} -l`];
  }
  return ['#!' + shell, `exec ${shell} -l ${scriptFile}`];
}

/**
 * 启动物理机 worker 构建并等待其结束。
 * 返回结果由调用方用 api.workerBuildFinish 上报（成功后 sleep 8s 由调用方处理）。
 */
export async function runWorkerBuild(opts: WorkerBuildOptions): Promise<WorkerBuildResult> {
  const log = opts.logFn ?? ((m: string) => console.info('[agent-sdk][worker]', m));
  const b = opts.buildInfo;

  // 校验 workerJar 存在（对应 Go runBuild 的自愈判断，SDK 只做存在性校验）。
  if (!fs.existsSync(opts.workerJarPath)) {
    const msg = `worker jar missing: ${opts.workerJarPath}`;
    log(msg);
    return { success: false, message: msg };
  }

  const javaBin = resolveLatestJava(opts.jdk17Path, opts.jdk8Path);
  if (javaBin === '') {
    const msg = 'no available jdk (both jdk17 and jdk8 path missing or invalid)';
    log(msg);
    return { success: false, message: msg };
  }

  // 默认 worker 启动会读取 <workDir>/.agent.properties 获取项目/鉴权信息，
  // SDK 场景该文件可能不存在，这里用 options 中的配置自动补齐。
  await ensureAgentProperties(opts, log);

  // 临时目录（对应 Go MkBuildTmpDir）。
  const tmpDir = path.join(opts.workDir, 'build_tmp');
  await fs.promises.mkdir(tmpDir, { recursive: true });

  const errorMsgFile = getWorkerErrorMsgFile(opts.workDir, b.buildId, b.vmSeqId);
  const agentLogPrefix = `${b.buildId}_${b.vmSeqId}_agent`;
  const encoded = getEncodedBuildInfo(b);
  const env = { ...process.env, ...buildWorkerEnv(opts) };
  const enableExitGroup = opts.enableExitGroup ?? true;

  // 需在构建结束后清理的临时文件（对应 Go ToDelTmpFiles）。
  const toDelFiles: string[] = [errorMsgFile];

  const isWindows = process.platform === 'win32';

  // #5806 预先录入异常信息，worker 正常结束会清空/覆盖它；未清空则说明被杀。
  await fs.promises.writeFile(errorMsgFile, BUILDER_PROCESS_WAS_KILLED, { mode: 0o777 });

  let exitCode = 0;
  let exitErr: Error | null = null;

  // TODO: windows这里应该还有些东西没搬过来参考GO
  if (isWindows) {
    // Windows：直接 spawn java.exe（对应 do_build_win.go 的 args 数组）。
    const args = [
      '-Djava.io.tmpdir=' + tmpDir,
      '-Ddevops.agent.error.file=' + errorMsgFile,
      '-Dbuild.type=AGENT',
      '-DAGENT_LOG_PREFIX=' + agentLogPrefix,
      '-Xmx2g',
      '-jar',
      opts.workerJarPath,
      encoded,
    ];
    log(`start worker: ${javaBin} ${args.join(' ')}`);
    ({ exitCode, exitErr } = await spawnAndWait(javaBin, args, opts.workDir, env, false));
  } else {
    // Unix：两层 login-shell 脚本（对应 do_build.go writeStartBuildAgentScript）。
    const shell = getCurrentShell(opts.detectShell ?? false);
    const prepareScriptFile = path.join(
      opts.workDir,
      `devops_agent_prepare_start_${b.projectId}_${b.buildId}_${b.vmSeqId}.sh`
    );
    const scriptFile = path.join(
      opts.workDir,
      `devops_agent_start_${b.projectId}_${b.buildId}_${b.vmSeqId}.sh`
    );
    toDelFiles.push(scriptFile, prepareScriptFile);

    const startLines = [
      '#!' + shell,
      `cd ${opts.workDir}`,
      `${javaBin} -Ddevops.slave.agent.start.file=${scriptFile} ` +
        `-Ddevops.slave.agent.prepare.start.file=${prepareScriptFile} ` +
        `-Ddevops.agent.error.file=${errorMsgFile} ` +
        `-Dbuild.type=AGENT -DAGENT_LOG_PREFIX=${agentLogPrefix} -Xmx2g ` +
        `-Djava.io.tmpdir=${tmpDir} -jar ${opts.workerJarPath} ${encoded}`,
    ];
    await fs.promises.writeFile(scriptFile, startLines.join('\n'), { mode: 0o777 });

    const prepareLines = getPrepareScriptLines(shell, scriptFile);
    await fs.promises.writeFile(prepareScriptFile, prepareLines.join('\n'), { mode: 0o777 });

    log(`start worker via prepare script: ${prepareScriptFile}`);
    ({ exitCode, exitErr } = await spawnAndWait(
      prepareScriptFile,
      [],
      opts.workDir,
      env,
      enableExitGroup
    ));
  }

  // #5806 从 error_msg 文件读取信息：非空说明 worker 未正常结束。
  let msg = '';
  try {
    msg = (await fs.promises.readFile(errorMsgFile, 'utf-8')).trim();
  } catch {
    msg = '';
  }

  const cmdErrMsg = exitErr ? '|' + exitErr.message : '';

  let success = true;
  if (msg.length === 0) {
    // 正常结束：error_msg 已被 worker 清空。
    msg = `worker process exit${cmdErrMsg}`;
  } else {
    msg += cmdErrMsg;
    success = false;
  }

  log(`build[${b.buildId}] finish, exitCode=${exitCode}, success=${success}, msg=${msg}`);

  // 清理临时文件（对应 Go workerBuildFinish 的 ToDelTmpFiles 清理）。
  for (const f of toDelFiles) {
    await fs.promises.rm(f, { force: true }).catch(() => undefined);
  }

  return { success, message: msg };
}

/** 执行命令并收集 stdout/stderr；非 0 退出码按失败处理。 */
function spawnAndCollect(
  command: string,
  args: string[],
  cwd: string
): Promise<{ output: string; error: Error | null }> {
  return new Promise((resolve) => {
    const child = spawn(command, args, {
      cwd,
      env: process.env,
      stdio: ['ignore', 'pipe', 'pipe'],
    });
    const stdout: Buffer[] = [];
    const stderr: Buffer[] = [];
    let spawnError: Error | null = null;

    child.stdout.on('data', (chunk: Buffer | string) => {
      stdout.push(Buffer.isBuffer(chunk) ? chunk : Buffer.from(chunk));
    });
    child.stderr.on('data', (chunk: Buffer | string) => {
      stderr.push(Buffer.isBuffer(chunk) ? chunk : Buffer.from(chunk));
    });
    child.once('error', (err) => {
      spawnError = err;
    });
    child.once('close', (code, signal) => {
      const output =
        Buffer.concat(stdout).toString('utf-8') + '\n' + Buffer.concat(stderr).toString('utf-8');
      if (spawnError) {
        resolve({ output, error: spawnError });
        return;
      }
      if (code !== 0) {
        const desc = signal ? `killed by signal ${signal}` : `exit code ${code ?? -1}`;
        resolve({ output, error: new Error(desc) });
        return;
      }
      resolve({ output, error: null });
    });
  });
}

function errorMessage(error: unknown): string {
  return error instanceof Error ? error.message : String(error);
}

/**
 * spawn 一个进程并等待退出。
 * @param useProcessGroup Unix 下 detached:true(setpgid)，退出后按进程组 SIGKILL 清理子进程。
 */
function spawnAndWait(
  command: string,
  args: string[],
  cwd: string,
  env: NodeJS.ProcessEnv,
  useProcessGroup: boolean
): Promise<{ exitCode: number; exitErr: Error | null }> {
  return new Promise((resolve) => {
    const child = spawn(command, args, {
      cwd,
      env,
      detached: useProcessGroup,
      stdio: 'ignore',
    });

    child.on('error', (err) => {
      resolve({ exitCode: -1, exitErr: err });
    });

    // TODO: 这里逻辑也不太一致，应该不是kill整个进程组，要看下
    child.on('exit', (code, signal) => {
      // Unix 进程组清理：结束后 kill 整个进程组，回收残留子进程。
      if (useProcessGroup && child.pid) {
        try {
          process.kill(-child.pid, 'SIGKILL');
        } catch {
          // 进程组可能已退出，忽略
        }
      }
      if (code === 0) {
        resolve({ exitCode: 0, exitErr: null });
      } else {
        const desc = signal ? `killed by signal ${signal}` : `exit code ${code}`;
        resolve({ exitCode: code ?? -1, exitErr: new Error(desc) });
      }
    });
  });
}
