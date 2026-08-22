/*
 * BK-CI Agent SDK - docker/podman CLI 封装
 *
 * 移植 Go agent 的 src/pkg/dockercli/dockercli.go。基于 child_process.spawn
 * 直接调用 docker/podman 命令行（非 daemon API），零第三方依赖。
 *
 * runtime 选择：环境变量 DEVOPS_AGENT_CONTAINER_RUNTIME，默认 "docker"。
 * 支持：serverOS / imageExists / pullImage(凭据 stdin login) / createContainer /
 *       startContainer / waitContainer / removeContainer / containerLogs。
 * 日志脱敏：docker -e KEY=VALUE 中 KEY 含 secret/password/token/credential 时 mask 值。
 */

import { spawn } from 'child_process';
import * as os from 'os';
import * as fs from 'fs';
import * as path from 'path';

export const DevopsAgentContainerRuntime = 'DEVOPS_AGENT_CONTAINER_RUNTIME';

export type DockerLogLevel = 'DEBUG' | 'INFO' | 'WARN' | 'ERROR';

export interface DockerLogEntry {
  level: DockerLogLevel;
  message: string;
}

export type DockerEventLogFn = (entry: DockerLogEntry) => void;

/** 读取 runtime 二进制名（对应 Go RuntimeBinary），默认 docker。 */
export function runtimeBinary(): string {
  const v = (process.env[DevopsAgentContainerRuntime] ?? '').trim();
  return v !== '' ? v : 'docker';
}

interface RunResult {
  stdout: string;
  stderr: string;
  err: Error | null;
}

const sensitiveEnvKeys = ['secret', 'password', 'token', 'credential'];

export class DockerRunner {
  private readonly workDir: string;
  private readonly binary: string;
  private readonly eventf?: DockerEventLogFn;

  constructor(workDir: string, eventf?: DockerEventLogFn, binary?: string) {
    this.workDir = workDir;
    this.binary = binary ?? runtimeBinary();
    this.eventf = eventf;
  }

  binaryName(): string {
    return this.binary;
  }

  async serverOS(): Promise<string> {
    const r = await this.run(null, ['version', '--format', '{{.Server.Os}}']);
    return r.stdout.trim();
  }

  /**
   * 镜像是否本地存在（依赖 image inspect 退出码，不解析 stderr 文本）。
   * daemon 不可达时返回 false（后续 pull/create 会暴露真实错误）。
   */
  async imageExists(image: string): Promise<boolean> {
    const r = await this.run(null, ['image', 'inspect', image]);
    return r.err === null;
  }

  /** 拉取镜像；有凭据时用临时 config 目录 + --password-stdin login。返回合并日志输出。 */
  async pullImage(image: string, user: string, password: string): Promise<string> {
    if (!user || !password) {
      const r = await this.run(null, ['pull', image]);
      if (r.err) throw r.err;
      return r.stdout + r.stderr;
    }

    const os = await import('os');
    const fs = await import('fs');
    const path = await import('path');
    const cfgDir = await fs.promises.mkdtemp(path.join(os.tmpdir(), 'bkci-docker-config-'));
    try {
      const registry = registryFromImage(image);
      const loginArgs = ['--config', cfgDir, 'login', '-u', user, '--password-stdin'];
      if (registry !== '') loginArgs.push(registry);
      const loginRes = await this.run(Buffer.from(password), loginArgs);
      if (loginRes.err) throw loginRes.err;

      const pullRes = await this.run(null, ['--config', cfgDir, 'pull', image]);
      if (pullRes.err) throw pullRes.err;
      return pullRes.stdout + pullRes.stderr;
    } finally {
      await fs.promises.rm(cfgDir, { recursive: true, force: true }).catch(() => undefined);
    }
  }

  /** 创建容器，返回 container id（取输出最后一行）。 */
  async createContainer(args: string[]): Promise<string> {
    const r = await this.run(null, ['create', ...args]);
    if (r.err) throw r.err;
    const lines = r.stdout.trim().split('\n');
    const last = (lines[lines.length - 1] ?? '').trim();
    if (last === '') {
      throw new Error(`empty container id returned from ${this.binary} create`);
    }
    return last;
  }

  async startContainer(containerId: string): Promise<void> {
    const r = await this.run(null, ['start', containerId]);
    if (r.err) throw r.err;
  }

  async stopContainer(containerId: string): Promise<void> {
    const r = await this.run(null, ['stop', '-t', '0', containerId]);
    if (r.err) throw r.err;
  }

  async removeContainer(containerId: string): Promise<void> {
    const r = await this.run(null, ['rm', '-f', containerId]);
    if (r.err) throw r.err;
  }

  /** 等待容器结束，返回退出码（对应 docker wait）。 */
  async waitContainer(containerId: string): Promise<number> {
    const r = await this.run(null, ['wait', containerId]);
    if (r.err) throw r.err;
    const code = parseInt(r.stdout.trim(), 10);
    if (Number.isNaN(code)) {
      throw new Error(`parse wait exit code failed: ${r.stdout.trim()}`);
    }
    return code;
  }

  async containerLogs(containerId: string): Promise<string> {
    const r = await this.run(null, ['logs', containerId]);
    // 与 Go 一致：即便有错也返回已收集的输出
    return r.stdout + r.stderr;
  }

  /** 底层执行：spawn runtime，收集 stdout/stderr，脱敏后按级别记日志。 */
  private run(stdin: Buffer | null, args: string[]): Promise<RunResult> {
    this.log(classifyCommandLevel(args), formatCommand(this.binary, args));
    return new Promise((resolve) => {
      const child = spawn(this.binary, args, {
        cwd: this.workDir,
        env: process.env,
      });

      let stdout = '';
      let stderr = '';
      child.stdout.on('data', (c) => (stdout += c.toString()));
      child.stderr.on('data', (c) => (stderr += c.toString()));

      const finish = (err: Error | null): void => {
        if (stdout.trim() !== '') {
          this.log(classifyStreamLevel(false, err, stdout, args), `[stdout]\n${stdout.trim()}`);
        }
        if (stderr.trim() !== '') {
          this.log(classifyStreamLevel(true, err, stderr, args), `[stderr]\n${stderr.trim()}`);
        }
        if (err) {
          resolve({
            stdout,
            stderr,
            err: new Error(`${formatCommand(this.binary, args)} failed: ${err.message}`),
          });
        } else {
          resolve({ stdout, stderr, err: null });
        }
      };

      child.on('error', (err) => finish(err));
      child.on('close', (code) => {
        finish(code === 0 ? null : new Error(`exit code ${code}`));
      });

      if (stdin !== null) {
        child.stdin.write(stdin);
      }
      child.stdin.end();
    });
  }

  private log(level: DockerLogLevel, message: string): void {
    if (this.eventf) {
      this.eventf({ level, message });
      return;
    }
    console.info(`[agent-sdk][docker][${level}]`, message);
  }
}

/** 从镜像名推断 registry host（对应 Go registryFromImage）。 */
export function registryFromImage(image: string): string {
  const s = image.replace(/^https?:\/\//, '').trim();
  const idx = s.indexOf('/');
  if (idx < 0) return '';
  const first = s.slice(0, idx);
  if (first.includes('.') || first.includes(':') || first === 'localhost') {
    return first;
  }
  return '';
}

/** 拼接命令用于日志（对含敏感 env 值脱敏）。 */
function formatCommand(binary: string, args: string[]): string {
  const parts = [binary];
  let maskNext = false;
  for (const arg of args) {
    let display = arg;
    if (maskNext) {
      display = maskEnvValue(arg);
      maskNext = false;
    } else if (arg === '-e' || arg === '--env') {
      maskNext = true;
    }
    if (/[ \t\n"']/.test(display)) {
      parts.push(JSON.stringify(display));
    } else {
      parts.push(display);
    }
  }
  return parts.join(' ');
}

function maskEnvValue(env: string): string {
  const eqIdx = env.indexOf('=');
  if (eqIdx < 0) return env;
  const key = env.slice(0, eqIdx).toLowerCase();
  for (const s of sensitiveEnvKeys) {
    if (key.includes(s)) {
      return env.slice(0, eqIdx + 1) + '******';
    }
  }
  return env;
}

function classifyCommandLevel(args: string[]): DockerLogLevel {
  if (args.length >= 2 && args[0] === 'image' && args[1] === 'inspect') {
    return 'DEBUG';
  }
  return 'INFO';
}

function classifyStreamLevel(
  isStderr: boolean,
  runErr: Error | null,
  output: string,
  args: string[]
): DockerLogLevel {
  if (runErr) {
    if (isExpectedFailure(args)) return 'INFO';
    return isStderr ? 'ERROR' : 'WARN';
  }
  if (isStderr && looksLikeWarning(output)) return 'WARN';
  return 'INFO';
}

function isExpectedFailure(args: string[]): boolean {
  return args.length >= 2 && args[0] === 'image' && args[1] === 'inspect';
}

function looksLikeWarning(output: string): boolean {
  const s = output.toLowerCase();
  return s.includes('warning') || s.includes('warn:') || s.includes('deprecated');
}
