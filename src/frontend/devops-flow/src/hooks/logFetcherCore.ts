/**
 * Log Fetcher Core
 * Shared core logic for log fetching, used by useLogFetcher and useMultiLogFetcher
 */
import { getAfterLog, getInitLog, type GetLogParams, type LogItem, type LogResponse } from '@/api/log'
import { hashID } from '@/utils/util'

// Shared interface for log post data
export interface LogPostData {
  projectId: string
  pipelineId: string
  buildId: string
  tag?: string
  jobId?: string
  currentExe: number
  lineNo: number
  debug: boolean
  subTag?: string
  hashId?: string
}

// Result of a log fetch operation
export interface LogFetchResult {
  success: boolean
  logs: LogItem[]
  lastLineNo: number
  finished: boolean
  hasMore: boolean
  subTags?: string[]
  errorMessage?: string
}

// Options for creating log post data
export interface CreateLogPostDataOptions {
  projectId: string
  pipelineId: string
  buildId: string
  tag?: string
  jobId?: string
  executeCount: number
  debug?: boolean
  subTag?: string
}

/**
 * Create initial log post data
 */
export function createLogPostData(options: CreateLogPostDataOptions): LogPostData {
  return {
    projectId: options.projectId,
    pipelineId: options.pipelineId,
    buildId: options.buildId,
    tag: options.tag,
    jobId: options.jobId,
    currentExe: options.executeCount,
    lineNo: 0,
    debug: options.debug || false,
    subTag: options.subTag || '',
    hashId: '',
  }
}

/**
 * Generate a unique hash ID for request tracking
 */
export function generateHashId(): string {
  return hashID()
}

/**
 * Core log fetching function
 * Returns the fetch result without managing state or polling
 */
export async function fetchLogCore(postData: LogPostData): Promise<LogFetchResult> {
  try {
    const logMethod = postData.lineNo <= 0 ? getInitLog : getAfterLog
    const params: GetLogParams = {
      projectId: postData.projectId,
      pipelineId: postData.pipelineId,
      buildId: postData.buildId,
      executeCount: postData.currentExe,
      debug: postData.debug,
    }

    // Set tag or jobId based on availability
    if (postData.tag) {
      params.tag = postData.tag
    }
    if (postData.jobId) {
      params.jobId = postData.jobId
    }

    // Only pass subTag when it has a value
    if (postData.subTag) {
      params.subTag = postData.subTag
    }

    // getAfterLog needs lineNo
    if (postData.lineNo > 0) {
      params.lineNo = postData.lineNo
    }

    const res = await logMethod(params)
    const data: LogResponse = res || {}

    if (data.status !== 0) {
      return {
        success: false,
        logs: [],
        lastLineNo: postData.lineNo,
        finished: false,
        hasMore: false,
        errorMessage: data.message ?? 'Failed to fetch log',
      }
    }

    const logs = data.logs || []
    const lastLog = logs[logs.length - 1] as LogItem | undefined
    const lastLogNo = lastLog?.lineNo ?? (postData.lineNo > 0 ? postData.lineNo - 1 : -1)

    return {
      success: true,
      logs,
      lastLineNo: +lastLogNo + 1,
      finished: data.finished ?? false,
      hasMore: data.hasMore ?? false,
      subTags: data.subTags,
    }
  } catch (err: unknown) {
    return {
      success: false,
      logs: [],
      lastLineNo: postData.lineNo,
      finished: false,
      hasMore: false,
      errorMessage: err instanceof Error ? err.message : String(err),
    }
  }
}

/**
 * Calculate next poll delay based on fetch result
 */
export function getNextPollDelay(result: LogFetchResult): number {
  if (result.finished && result.hasMore) {
    return 100 // Fast poll for remaining logs
  }
  return 1000 // Normal poll interval
}

/**
 * Check if polling should continue
 */
export function shouldContinuePolling(result: LogFetchResult): boolean {
  return !result.finished || result.hasMore
}
