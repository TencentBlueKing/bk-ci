/*
 * BK-CI Agent SDK - HTTP 客户端与结果封装
 *
 * 对应 Go agent 的 src/pkg/util/httputil。使用 Node 内置 http/https，零第三方依赖。
 * 统一处理鉴权头注入、请求超时、以及 DevopsResult / AgentResult 的解析。
 */

import * as http from 'http';
import * as https from 'https';
import { URL } from 'url';
import { AgentStatus } from './config';

/** 后台统一返回结构（对应 Go DevopsResult）。status===0 表示成功。 */
export interface DevopsResult<T = unknown> {
  data: T;
  status: number;
  message: string;
}

/** ask 接口返回结构（对应 Go AgentResult），额外带 agentStatus。 */
export interface AgentResult<T = unknown> extends DevopsResult<T> {
  agentStatus: string;
}

export function isOk(r: DevopsResult): boolean {
  return r.status === 0;
}

export function isNotOk(r: DevopsResult): boolean {
  return r.status !== 0;
}

export function isAgentDelete(r: AgentResult): boolean {
  return !!r.agentStatus && r.agentStatus === AgentStatus.Delete;
}

export interface HttpClientOptions {
  /** 请求超时（毫秒） */
  timeoutMs?: number;
}

export interface RequestOptions {
  method: 'GET' | 'POST' | 'PUT' | 'DELETE';
  url: string;
  headers?: Record<string, string>;
  /** 请求体对象，将被 JSON.stringify */
  body?: unknown;
  timeoutMs?: number;
}

export interface RawResponse {
  status: number;
  body: string;
}

/**
 * 发起一次 HTTP 请求，返回原始响应文本。基于 Node 内置模块，支持 http/https。
 */
export function request(opts: RequestOptions): Promise<RawResponse> {
  return new Promise((resolve, reject) => {
    let parsed: URL;
    try {
      parsed = new URL(opts.url);
    } catch (e) {
      reject(new Error(`invalid url: ${opts.url}`));
      return;
    }

    const isHttps = parsed.protocol === 'https:';
    const lib = isHttps ? https : http;

    const headers: Record<string, string> = { ...(opts.headers ?? {}) };
    let payload: string | undefined;
    if (opts.body !== undefined && opts.body !== null) {
      payload = JSON.stringify(opts.body);
      headers['Content-Type'] = 'application/json';
      headers['Content-Length'] = Buffer.byteLength(payload).toString();
    }

    const req = lib.request(
      {
        protocol: parsed.protocol,
        hostname: parsed.hostname,
        port: parsed.port || (isHttps ? 443 : 80),
        path: parsed.pathname + parsed.search,
        method: opts.method,
        headers,
      },
      (res) => {
        const chunks: Buffer[] = [];
        res.on('data', (c) => chunks.push(c as Buffer));
        res.on('end', () => {
          resolve({
            status: res.statusCode ?? 0,
            body: Buffer.concat(chunks).toString('utf-8'),
          });
        });
      }
    );

    const timeout = opts.timeoutMs ?? 30_000;
    req.setTimeout(timeout, () => {
      req.destroy(new Error(`request timeout after ${timeout}ms: ${opts.url}`));
    });

    req.on('error', (err) => reject(err));

    if (payload !== undefined) {
      req.write(payload);
    }
    req.end();
  });
}

/** HTTP 客户端：持有默认超时，提供解析为 DevopsResult / AgentResult 的便捷方法。 */
export class HttpClient {
  private readonly timeoutMs: number;

  constructor(opts: HttpClientOptions = {}) {
    this.timeoutMs = opts.timeoutMs ?? 30_000;
  }

  async requestRaw(opts: Omit<RequestOptions, 'timeoutMs'>): Promise<RawResponse> {
    return request({ ...opts, timeoutMs: this.timeoutMs });
  }

  async intoDevopsResult<T = unknown>(
    opts: Omit<RequestOptions, 'timeoutMs'>
  ): Promise<DevopsResult<T>> {
    const raw = await this.requestRaw(opts);
    return parseResult<DevopsResult<T>>(raw, opts.url);
  }

  async intoAgentResult<T = unknown>(
    opts: Omit<RequestOptions, 'timeoutMs'>
  ): Promise<AgentResult<T>> {
    const raw = await this.requestRaw(opts);
    return parseResult<AgentResult<T>>(raw, opts.url);
  }
}

function parseResult<R>(raw: RawResponse, url: string): R {
  try {
    return JSON.parse(raw.body) as R;
  } catch (e) {
    throw new Error(
      `parse result error, url=${url} status=${raw.status} body=${raw.body.slice(0, 500)}`
    );
  }
}
