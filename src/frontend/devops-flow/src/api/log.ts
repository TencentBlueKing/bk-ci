/**
 * 日志相关 API
 * 参考 devops-pipeline 的日志接口实现
 */
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

/**
 * 获取日志状态
 */
export function getLogStatus(params: {
  projectId: string
  pipelineId: string
  buildId: string
  tag: string
  executeCount: number
}): Promise<{ status: number }> {
  const { projectId, pipelineId, buildId, tag, executeCount } = params
  return get<{ status: number }>(
    `${LOG_API_URL_PREFIX}/user/logs/${projectId}/${pipelineId}/${buildId}/mode`,
    {
      params: {
        tag,
        executeCount,
      },
    },
  )
}

/**
 * 构建日志下载链接
 */
export function buildLogDownloadUrl(params: {
  projectId: string
  pipelineId: string
  buildId: string
  tag?: string
  jobId?: string
  executeCount: number
  fileName?: string
}): string {
  const { projectId, pipelineId, buildId, tag, jobId, executeCount, fileName } = params
  const baseUrl = `${LOG_API_URL_PREFIX}/user/logs/${projectId}/${pipelineId}/${buildId}/download`
  const queryParams = new URLSearchParams()

  if (tag) queryParams.append('tag', tag)
  if (jobId) queryParams.append('jobId', jobId)
  if (executeCount) queryParams.append('executeCount', String(executeCount))
  if (fileName) queryParams.append('fileName', fileName)

  const queryString = queryParams.toString()
  return queryString ? `${baseUrl}?${queryString}` : baseUrl
}
