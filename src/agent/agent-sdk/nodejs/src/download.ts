/*
 * BK-CI Agent SDK - 升级文件下载（worker.jar / docker init 脚本等）
 *
 * 对应 Go agent 的 src/pkg/util/httputil/devops.go 的 DownloadUpgradeFile 与
 * src/pkg/api/api.go 的 DownloadUpgradeFile / src/pkg/upgrade/download/*。
 *
 * 实现 eTag/md5 增量下载协议：
 *   - 本地已存在目标文件时，算其 md5 作为 &eTag=<md5> 传给后台；
 *   - 200 → 原子写入（写临时文件再 rename），并校验响应头 X-Checksum-Md5；
 *   - 304 → 文件未变更，返回旧 md5，notModified=true；
 *   - 404 → 抛错 file not found。
 *
 * 说明：与 Go 一致，下载走普通网关（getGateway()）而非 fileGateway，
 * 鉴权头用 config.getAuthHeaderMap()。零第三方依赖，基于 httpClient.requestStream。
 */

import * as fs from 'fs';
import * as path from 'path';
import * as crypto from 'crypto';
import { pipeline } from 'stream';
import { promisify } from 'util';
import { requestStream } from './httpClient';

const pipelineAsync = promisify(pipeline);

/** 下载接口路径前缀（对应 Go buildUrl 拼的 upgrade/files/download）。 */
export const DownloadApiPath =
  '/ms/environment/api/buildAgent/agent/thirdPartyAgent/upgrade/files/download';

/** worker.jar 在本地/服务端的固定文件名（对应 Go config.WorkAgentFile）。 */
export const WorkAgentFile = 'worker-agent.jar';
/** worker.jar 在服务端的下载相对路径（对应 Go "jar/worker-agent.jar"）。 */
export const WorkerJarServerFile = 'jar/worker-agent.jar';

/** docker init 脚本在本地的固定文件名（对应 Go config.DockerInitFile）。 */
export const DockerInitFile = 'agent_docker_init.sh';

export interface DownloadResult {
  /** 下载后（或未变更时的旧）文件 md5。 */
  md5: string;
  /** 是否为 304 未变更。 */
  notModified: boolean;
}

export interface DownloadFileOptions {
  /** 后台网关地址（可不带 http:// 前缀，会自动补全）。 */
  gateway: string;
  /** 鉴权头（config.getAuthHeaderMap()）。 */
  authHeaders: Record<string, string>;
  /** 服务端文件相对路径，作为 ?file= 查询参数，如 "jar/worker-agent.jar"。 */
  serverFile: string;
  /** 本地保存的绝对路径。 */
  savePath: string;
  /** 请求超时（毫秒），默认 300000（下载可能较大）。 */
  timeoutMs?: number;
}

function ensureGateway(gateway: string): string {
  if (gateway.startsWith('http://') || gateway.startsWith('https://')) {
    return gateway;
  }
  return 'http://' + gateway;
}

/** 计算文件 md5；文件不存在返回空串（对应 Go GetFileMd5 对不存在的处理）。 */
export async function fileMd5(filePath: string): Promise<string> {
  return new Promise((resolve, reject) => {
    let stream: fs.ReadStream;
    try {
      stream = fs.createReadStream(filePath);
    } catch (e) {
      resolve('');
      return;
    }
    const hash = crypto.createHash('md5');
    stream.on('error', (err: NodeJS.ErrnoException) => {
      if (err.code === 'ENOENT') {
        resolve('');
      } else {
        reject(err);
      }
    });
    stream.on('data', (chunk) => hash.update(chunk));
    stream.on('end', () => resolve(hash.digest('hex')));
  });
}

/**
 * 下载一个升级文件，实现 eTag/md5 增量协议。
 *
 * @returns 下载后文件 md5 与是否未变更。
 * @throws  404 时抛 "file not found"，md5 不匹配时抛 "file md5 not match"，其余非 2xx 抛错。
 */
export async function downloadFile(opts: DownloadFileOptions): Promise<DownloadResult> {
  const oldMd5 = await fileMd5(opts.savePath);

  let url =
    ensureGateway(opts.gateway) +
    DownloadApiPath +
    '?file=' +
    encodeURIComponent(opts.serverFile);
  if (oldMd5 !== '') {
    url += '&eTag=' + oldMd5;
  }

  const resp = await requestStream({
    method: 'GET',
    url,
    headers: opts.authHeaders,
    timeoutMs: opts.timeoutMs ?? 300_000,
  });

  // 非 2xx 的分支处理。注意：304 属于非 2xx，需先消费/丢弃响应流。
  if (!(resp.status >= 200 && resp.status < 300)) {
    resp.stream.resume(); // 丢弃响应体，释放 socket
    if (resp.status === 404) {
      throw new Error('file not found');
    }
    if (resp.status === 304) {
      return { md5: oldMd5, notModified: true };
    }
    throw new Error(`download file failed, status=${resp.status}, url=${opts.serverFile}`);
  }

  // 原子写入：先写临时文件再 rename（对应 Go AtomicWriteFile）。
  await fs.promises.mkdir(path.dirname(opts.savePath), { recursive: true });
  const tmpPath = opts.savePath + '.tmp.' + process.pid + '.' + Date.now();
  try {
    const out = fs.createWriteStream(tmpPath, { mode: 0o755 });
    await pipelineAsync(resp.stream, out);
    await fs.promises.rename(tmpPath, opts.savePath);
  } catch (e) {
    await fs.promises.rm(tmpPath, { force: true }).catch(() => undefined);
    throw e;
  }

  const newMd5 = await fileMd5(opts.savePath);

  const checksumMd5 = firstHeader(resp.headers['x-checksum-md5']);
  if (checksumMd5 && checksumMd5 !== newMd5) {
    throw new Error('file md5 not match');
  }

  return { md5: newMd5, notModified: false };
}

function firstHeader(v: string | string[] | undefined): string {
  if (v === undefined) return '';
  return Array.isArray(v) ? (v[0] ?? '') : v;
}

/**
 * 下载 worker.jar 到 <dir>/worker-agent.jar。
 * 对应 Go：serverFile="jar/worker-agent.jar"。
 */
export function downloadWorkerJar(
  gateway: string,
  authHeaders: Record<string, string>,
  dir: string,
  timeoutMs?: number
): Promise<DownloadResult> {
  return downloadFile({
    gateway,
    authHeaders,
    serverFile: WorkerJarServerFile,
    savePath: path.join(dir, WorkAgentFile),
    timeoutMs,
  });
}

/**
 * 下载 docker 初始化脚本到 <dir>/agent_docker_init.sh。
 * 服务端相对路径随平台不同（对应 Go download_unix/darwin/win.go）：
 *   - linux:  script/linux/agent_docker_init.sh
 *   - darwin: script/macos/agent_docker_init.sh
 *   - win32:  script/windows/agent_docker_init.sh
 */
export function downloadDockerInitFile(
  gateway: string,
  authHeaders: Record<string, string>,
  dir: string,
  platform: NodeJS.Platform = process.platform,
  timeoutMs?: number
): Promise<DownloadResult> {
  let serverFile: string;
  switch (platform) {
    case 'darwin':
      serverFile = 'script/macos/agent_docker_init.sh';
      break;
    case 'win32':
      serverFile = 'script/windows/agent_docker_init.sh';
      break;
    default:
      serverFile = 'script/linux/agent_docker_init.sh';
  }
  return downloadFile({
    gateway,
    authHeaders,
    serverFile,
    savePath: path.join(dir, DockerInitFile),
    timeoutMs,
  });
}
