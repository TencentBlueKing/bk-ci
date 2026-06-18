/**
 * 日志相关 API
 * 参考 devops-pipeline 的日志接口实现
 */
import httpInstance from '@/utils/http/httpClient'
import { get } from '@/utils/http'

const LOG_API_URL_PREFIX = 'log/api'

/**
 * 日志条目类型
 */
export interface LogItem {
  lineNo: number
  timestamp: number
  message: string
  priority?: string
  tag?: string
}

/**
 * 日志响应数据
 */
export interface LogResponse {
  status: number
  message?: string
  logs: LogItem[]
  finished: boolean
  hasMore: boolean
  subTags?: string[]
  timeUsed?: number
}

/**
 * 获取日志请求参数
 */
export interface GetLogParams {
  projectId: string
  pipelineId: string
  buildId: string
  tag?: string
  jobId?: string
  executeCount: number
  subTag?: string
  debug?: boolean
  lineNo?: number
}

/**
 * 第一次拉取日志
 */
export function getInitLog(params: GetLogParams): Promise<LogResponse> {
  const { projectId, pipelineId, buildId, tag, jobId, executeCount, subTag, debug } = params
  return get<LogResponse>(`${LOG_API_URL_PREFIX}/user/logs/${projectId}/${pipelineId}/${buildId}`, {
    params: {
      tag,
      jobId,
      executeCount,
      subTag,
      debug,
    },
  })
}

/**
 * 后续拉取日志（带行号）
 */
export function getAfterLog(params: GetLogParams): Promise<LogResponse> {
  const { projectId, pipelineId, buildId, tag, jobId, executeCount, lineNo, subTag, debug } = params
  return get<LogResponse>(
    `${LOG_API_URL_PREFIX}/user/logs/${projectId}/${pipelineId}/${buildId}/after`,
    {
      params: {
        tag,
        jobId,
        executeCount,
        start: lineNo,
        subTag,
        debug,
      },
    },
  )
}

export interface DownloadLogParams {
  projectId: string
  pipelineId: string
  buildId: string
  tag?: string
  jobId?: string
  executeCount: number
  fileName?: string
}

/**
 * 下载日志文件
 * 通过 httpInstance 发起请求以携带 X-DEVOPS-CHANNEL 等自定义头部
 */
export async function downloadLogFile(params: DownloadLogParams): Promise<void> {
  const { projectId, pipelineId, buildId, tag, jobId, executeCount, fileName } = params
  const url = `${LOG_API_URL_PREFIX}/user/logs/${projectId}/${pipelineId}/${buildId}/download`

  const data = await httpInstance.get(url, {
    params: {
      tag,
      jobId,
      executeCount,
      fileName: fileName ? encodeURIComponent(fileName) : undefined,
    },
    responseType: 'blob',
  })

  // httpClient 响应拦截器已解包 response.data，返回值直接是 Blob
  const blob = data as unknown as Blob
  const blobUrl = URL.createObjectURL(blob)
  const anchor = document.createElement('a')
  anchor.href = blobUrl
  anchor.download = fileName ? `${fileName}.log` : 'log.txt'
  document.body.appendChild(anchor)
  anchor.click()
  document.body.removeChild(anchor)
  URL.revokeObjectURL(blobUrl)
}
