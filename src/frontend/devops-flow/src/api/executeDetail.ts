import {
  type FlowInfo,
  type ExecuteDetailData
} from '@/types/flow'
import { PROCESS_API_URL_PREFIX } from '@/utils/apiUrlPrefix'
import { del, get, post } from '@/utils/http'

/**
 * 重试流水线响应数据
 */
export interface RetryPipelineResponse {
  id: string // 构建ID
  executeCount: number // 执行次数
  projectId: string // 项目ID
  pipelineId: string // 流水线ID
  num: number // 构建编号
  code?: number // 错误码（可选）
  message?: string // 错误信息（可选）
}

/**
 * 重放创作流状态
 */
export type ReplayStatus =
  | 'CANNOT_REPLAY'
  | 'CAN_REPLAY'
  | 'REPLAY_SUCCESS'
  | 'REPLAYING'
  | 'REPLAY_FAILED'

/**
 * 重放创作流响应数据
 */
export interface ReplayPipelineResponse {
  status: ReplayStatus // 重放状态
  id: string // 构建ID
  code?: number // 错误码（可选）
  message?: string // 错误信息（可选）
}

/**
 * 构建启动参数项
 */
export interface BuildParamItem {
  key: string
  value?: any
  valueType?: string
  readOnly?: boolean
  desc?: string
  defaultValue?: any
}

/**
 * 获取当前参数组合
 */
export interface BuildParamProperty {
  id: string
  name?: string
  required?: boolean
  constant?: boolean
  type?: string
  defaultValue?: any
  value?: any
  desc?: string
  readOnly?: boolean
  valueNotEmpty?: boolean
  removeFlag?: boolean
}

/**
 * 获取执行历史构建详情数据
 * @param projectId 项目ID
 * @param buildNo 构建编号
 * @param pipelineId 流水线ID
 * @param executeCount 执行次数（可选）
 * @returns 构建详情数据
 */
export function requestPipelineExecDetail({
  projectId,
  buildNo,
  pipelineId,
  executeCount,
}: {
  projectId: string
  buildNo: string
  pipelineId: string
  executeCount?: number
}): Promise<ExecuteDetailData> {
  try {
    const url = executeCount
    ? `${PROCESS_API_URL_PREFIX}/user/builds/projects/${projectId}/pipelines/${pipelineId}/builds/${buildNo}/record?executeCount=${executeCount}`
    : `${PROCESS_API_URL_PREFIX}/user/builds/projects/${projectId}/pipelines/${pipelineId}/builds/${buildNo}/record`

    const res = get<ExecuteDetailData>(url)
    return res
  } catch (error) {
    throw error
  }
}

/**
 * 获取指定版本号的流水线编排版本信息
 */
export function requestFlowVersion({
  projectId,
  pipelineId,
  version,
}: {
  projectId: string
  pipelineId: string
  version: number
}): Promise<FlowInfo> {
  try {
    const res = get<FlowInfo>(`${PROCESS_API_URL_PREFIX}/user/version/projects/${projectId}/pipelines/${pipelineId}/versions/${version}/info`)
    return res
  } catch (error) {
    throw error
  }
}

/**
 * 终止创作流执行
 * @param projectId 项目ID
 * @param pipelineId 创作流ID
 * @param buildId 构建ID
 * @returns 是否成功终止
 */
export function requestTerminatePipeline({
  projectId,
  pipelineId,
  buildId,
}: {
  projectId: string
  pipelineId: string
  buildId: string
}): Promise<boolean> {
  return del<boolean>(
    `${PROCESS_API_URL_PREFIX}/user/builds/${projectId}/${pipelineId}/${buildId}`
  )
}

/**
 * 重试创作流
 * @param projectId 项目ID
 * @param flowId 流水线ID
 * @param buildId 构建ID
 * @param taskId 任务ID（可选）
 * @param failedContainer 失败容器（可选）
 * @param skip 是否跳过（可选）
 */
export function retryFlow({
  projectId,
  pipelineId,
  buildId,
  taskId,
  failedContainer,
  skip,
}: {
  projectId: string
  pipelineId: string
  buildId: string
  taskId?: string
  failedContainer?: string
  skip?: boolean
}): Promise<RetryPipelineResponse> {
  const failedContainerStr = failedContainer !== undefined ? `&failedContainer=${failedContainer}` : ''
  const queryStr = taskId ? `?taskId=${taskId}${failedContainerStr}&skip=${skip}` : ''
  
  return post<RetryPipelineResponse>(
    `${PROCESS_API_URL_PREFIX}/user/builds/${projectId}/${pipelineId}/${buildId}/retry${queryStr}`
  )
}

/**
 * 重放创作流
 * @param projectId 项目ID
 * @param flowId 流水线ID
 * @param buildId 构建ID
 * @param forceTrigger 是否强制触发（可选）
 */
export function replayFlow({
  projectId,
  pipelineId,
  buildId,
  forceTrigger = false,
}: {
  projectId: string
  pipelineId: string
  buildId: string
  forceTrigger?: boolean
}): Promise<ReplayPipelineResponse> {
  return post<ReplayPipelineResponse>(
    `${PROCESS_API_URL_PREFIX}/user/builds/${projectId}/${pipelineId}/${buildId}/replayByBuild?forceTrigger=${forceTrigger}`
  )
}

/**
 * 获取启动参数值
 * @param projectId 项目ID
 * @param pipelineId 流水线ID
 * @param buildId 构建ID
 * @param archiveFlag 归档标志（可选）
 */
export function requestBuildParams({
  projectId,
  pipelineId,
  buildId,
  archiveFlag,
}: {
  projectId: string
  pipelineId: string
  buildId: string
  archiveFlag?: boolean
}): Promise<BuildParamItem[]> {
  const params: Record<string, any> = {}
  if (archiveFlag !== undefined && archiveFlag !== null) {
    params.archiveFlag = archiveFlag
  }
  
  return get<BuildParamItem[]>(
    `${PROCESS_API_URL_PREFIX}/user/builds/${projectId}/${pipelineId}/${buildId}/parameters`,
    { params }
  )
}

/**
 * 获取启动参数组合
 * @param projectId 项目ID
 * @param pipelineId 流水线ID
 * @param buildId 构建ID
 */
export function requestBuildParamCombination({
  projectId,
  pipelineId,
  buildId,
}: {
  projectId: string
  pipelineId: string
  buildId: string
}): Promise<BuildParamProperty[]> {
  return get<BuildParamProperty[]>(
    `${PROCESS_API_URL_PREFIX}/user/buildParam/${projectId}/${pipelineId}/${buildId}/getCombinationFromBuild`
  )
}
