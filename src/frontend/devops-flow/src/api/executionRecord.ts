/**
 * Execution record related APIs
 * Reference: devops-pipeline's /process/api/user/builds/{projectId}/{pipelineId}/history/new
 */

import type { StageStatusInfo } from '@/types/flow'
import { statusIconMap } from '@/utils/flowStatus'
import { get, post } from '@/utils/http'
import { convertMillSec, convertTime } from '@/utils/util'

/**
 * Condition type enum for history filter conditions
 * 筛选条件类型枚举
 */
export enum HistoryConditionType {
  /** 触发方式 */
  TRIGGER_METHOD = 'TRIGGER_METHOD',
  /** 触发事件 */
  TRIGGER_EVENT = 'TRIGGER_EVENT',
  /** 触发人 */
  TRIGGER_USER = 'TRIGGER_USER',
  /** 触发节点（工作流节点） */
  TRIGGER_NODE = 'TRIGGER_NODE',
}

/**
 * Condition option item from API
 * 筛选条件选项项
 */
export interface ConditionOptionItem {
  id: string
  value: string
}

/**
 * Build record from API (matching devops-pipeline format)
 */
export interface BuildRecord {
  id: string
  buildNum: number
  buildNumAlias?: string
  userId: string
  trigger: string
  status: string
  stageStatus: StageStatusInfo[]
  queueTime?: number
  startTime?: number
  endTime?: number
  totalTime?: number
  executeTime?: number
  errorInfoList?: Array<{
    errorType?: number
    errorCode?: number
    errorMsg?: string
  }>
  remark?: string
  material?: Array<{
    aliasName?: string
    branchName?: string
    newCommitId?: string
    newCommitComment?: string
    url?: string
  }>
  [key: string]: any
}

/**
 * API response from /process/api/user/builds/{projectId}/{pipelineId}/history/new
 */
export interface BuildHistoryResponse {
  records: BuildRecord[]
  count: number
  totalPages: number
  page?: number
  pageSize?: number
  hasDownloadPermission?: boolean
  pipelineVersion?: number
}

/**
 * Stage status item for StageSteps component
 */
export interface StageStatusStep {
  stageId: string
  name?: string
  status: string // 原始状态，如 'SUCCEED', 'FAILED', 'RUNNING' 等
  statusCls: string // 状态类名，用于样式
  icon: string // 图标名称
  tooltip?: string
  progress?: number
}

export interface ErrorInfoItem {
  errorType?: number
  errorCode?: number
  errorMsg?: string
  taskId?: string
}

/**
 * ExecutionRecord for display (converted from BuildRecord)
 */
export interface ExecutionRecord {
  id: string
  buildNo: number
  checked: boolean
  status: string
  stageStatus: StageStatusStep[]
  workflowNode: string
  triggerAndUser: string
  triggerTime: string
  startTime: string
  endTime: string
  totalDuration: string
  executionDuration: string
  remark: string
  errorCode: string
  errorInfoList: ErrorInfoItem[]
}

/**
 * Query parameters for execution records
 */
export interface ExecutionRecordQueryParams {
  projectId: string
  pipelineId: string
  page?: number
  pageSize?: number
  /** 开始时间 */
  startTimeStartTime?: string
  /** 结束时间 */
  endTimeEndTime?: string
  /** 状态 */
  status?: string[]
  /** 触发方式 */
  triggerMethod?: string[]
  /** 触发事件 */
  triggerEvent?: string[]
  /** 触发人 */
  triggerUser?: string
  /** 触发节点（工作流节点） */
  triggerNode?: string
  /** 备注 */
  remark?: string
  debug?: boolean
}

/**
 * Response format for execution record list
 */
export interface ExecutionRecordListResponse {
  list: ExecutionRecord[]
  count: number
  page: number
  limit: number
  totalPages: number
}

/**
 * Convert BuildRecord from API to ExecutionRecord for display
 */
function convertBuildRecordToExecutionRecord(record: BuildRecord): ExecutionRecord {
  // Convert stage status to StageSteps format
  const stageStatus =
    record.stageStatus?.map((stage, index) => {
      const originalStatus = stage.status || 'UNKNOWN'
      const statusCls = originalStatus
      const icon = statusIconMap[originalStatus as keyof typeof statusIconMap] || 'circle'

      return {
        stageId: stage.stageId || `stage-${index}`,
        name: stage.name,
        status: originalStatus,
        statusCls,
        showMsg: stage.showMsg,
        icon,
      }
    }) || []

  const errorInfoList: ErrorInfoItem[] = record.errorInfoList ?? []
  const errorCode = errorInfoList[0]?.errorCode?.toString() || ''
  const triggerAndUser = `${record.trigger}/${record.userId}`

  return {
    id: record.id,
    buildNo: record.buildNum,
    checked: false,
    status: record.status || 'UNKNOWN',
    stageStatus,
    workflowNode: record.material?.[0]?.branchName || '--',
    triggerAndUser,
    triggerTime: convertTime(record.queueTime || 0),
    startTime: convertTime(record.startTime || 0),
    endTime: convertTime(record.endTime || 0),
    totalDuration: convertMillSec(record.totalTime || 0),
    executionDuration: convertMillSec(record.executeTime || 0),
    remark: record.remark || '',
    errorCode,
    errorInfoList,
  }
}

/**
 * Get execution records from API
 * API: GET /process/api/user/builds/{projectId}/{pipelineId}/history/new
 */
export async function getExecutionRecords(
  params: ExecutionRecordQueryParams,
): Promise<ExecutionRecordListResponse> {
  const { projectId, pipelineId, page = 1, pageSize = 20, debug = false, ...filterParams } = params

  // Build query parameters
  const queryParams = new URLSearchParams()
  queryParams.append('page', String(page))
  queryParams.append('pageSize', String(pageSize))

  if (debug) {
    queryParams.append('debug', 'true')
  }

  // Add filter parameters
  // 状态
  if (filterParams.status?.length) {
    filterParams.status.forEach((s) => queryParams.append('status', s))
  }
  // 触发方式
  if (filterParams.triggerMethod?.length) {
    filterParams.triggerMethod.forEach((m) => queryParams.append('triggerMethod', m))
  }
  // 触发事件
  if (filterParams.triggerEvent?.length) {
    filterParams.triggerEvent.forEach((e) => queryParams.append('triggerEvent', e))
  }
  // 触发人
  if (filterParams.triggerUser) {
    queryParams.append('triggerUser', filterParams.triggerUser)
  }
  // 触发节点（工作流节点）
  if (filterParams.triggerNode) {
    queryParams.append('triggerNode', filterParams.triggerNode)
  }
  // 备注
  if (filterParams.remark) {
    queryParams.append('remark', filterParams.remark)
  }
  // 时间范围
  if (filterParams.startTimeStartTime) {
    queryParams.append('startTimeStartTime', filterParams.startTimeStartTime)
  }
  if (filterParams.endTimeEndTime) {
    queryParams.append('endTimeEndTime', filterParams.endTimeEndTime)
  }

  try {
    const response = await get<BuildHistoryResponse>(
      `/process/api/user/builds/${projectId}/${pipelineId}/history/new`,
      { params: Object.fromEntries(queryParams) },
    )

    // Convert API records to display format
    const list = response.records.map(convertBuildRecordToExecutionRecord)
    return {
      list,
      count: response.count || list.length,
      page: response.page || page,
      limit: response.pageSize || pageSize,
      totalPages: response.totalPages || Math.ceil((response.count || list.length) / pageSize),
    }
  } catch (error) {
    console.error('Failed to get execution records:', error)
    throw error
  }
}

/**
 * Query params for getHistoryConditions API
 * 获取筛选条件选项的查询参数
 */
export interface HistoryConditionsQueryParams {
  /** 项目ID */
  projectId: string
  /** 流水线ID */
  pipelineId: string
  /** 构建条件类型 */
  conditionType: HistoryConditionType
  /** 页码 */
  page?: number
  /** 每页大小 */
  pageSize?: number
  /** 查询关键字 */
  keyword?: string
  /** 是否指定查询调试数据 */
  debug?: boolean
}

/**
 * Response for history conditions API
 * 筛选条件选项响应
 */
export interface HistoryConditionsResponse {
  records: ConditionOptionItem[]
  count: number
  totalPages: number
  page: number
  pageSize: number
}

/**
 * Get history filter conditions options
 * 获取流水线构建历史中的查询条件选项
 * API: GET /api/user/builds/projects/{projectId}/pipelines/{pipelineId}/history/conditions
 */
export async function getHistoryConditions(
  params: HistoryConditionsQueryParams,
): Promise<ConditionOptionItem[]> {
  const { projectId, pipelineId, conditionType, page = 1, pageSize = 20, keyword, debug } = params

  const queryParams = new URLSearchParams()
  queryParams.append('conditionType', conditionType)
  queryParams.append('page', String(page))
  queryParams.append('pageSize', String(pageSize))

  if (keyword) {
    queryParams.append('keyword', keyword)
  }
  if (debug !== undefined) {
    queryParams.append('debug', String(debug))
  }

  try {
    const response = await get<HistoryConditionsResponse>(
      `/process/api/user/builds/projects/${projectId}/pipelines/${pipelineId}/history/conditions?${queryParams.toString()}`,
    )
    return response.records || []
  } catch (error) {
    console.error(`Failed to get history conditions for ${conditionType}:`, error)
    return []
  }
}

/**
 * Get all filter conditions for execution history
 * 获取执行历史的所有筛选条件选项
 */
export async function getAllHistoryConditions(
  projectId: string,
  pipelineId: string,
  debug?: boolean,
): Promise<{
  triggerMethods: ConditionOptionItem[]
  triggerEvents: ConditionOptionItem[]
  triggerUsers: ConditionOptionItem[]
  triggerNodes: ConditionOptionItem[]
}> {
  const baseParams = { projectId, pipelineId, debug }

  const [triggerMethods, triggerEvents, triggerUsers, triggerNodes] = await Promise.all([
    getHistoryConditions({ ...baseParams, conditionType: HistoryConditionType.TRIGGER_METHOD }),
    getHistoryConditions({ ...baseParams, conditionType: HistoryConditionType.TRIGGER_EVENT }),
    getHistoryConditions({ ...baseParams, conditionType: HistoryConditionType.TRIGGER_USER }),
    getHistoryConditions({ ...baseParams, conditionType: HistoryConditionType.TRIGGER_NODE }),
  ])

  return {
    triggerMethods,
    triggerEvents,
    triggerUsers,
    triggerNodes,
  }
}

/**
 * Update build remark
 * 更新构建备注
 * API: POST /process/api/user/builds/{projectId}/{pipelineId}/{buildId}/updateRemark
 */
export async function updateBuildRemark(
  projectId: string,
  pipelineId: string,
  buildId: string,
  remark: string,
): Promise<boolean> {
  try {
    await post(`/process/api/user/builds/${projectId}/${pipelineId}/${buildId}/updateRemark`, {
      remark,
    })
    return true
  } catch (error) {
    console.error('Failed to update build remark:', error)
    throw error
  }
}
