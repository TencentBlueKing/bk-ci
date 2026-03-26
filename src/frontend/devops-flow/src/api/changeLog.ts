/**
 * 变更日志相关 API
 */
import { get } from '@/utils/http'
import { PROCESS_API_URL_PREFIX } from '@/utils/apiUrlPrefix'

export interface ChangeLogRecord {
  id: number
  projectId: string
  pipelineId: string
  version: number
  operator: string
  operationLogType: string
  operationLogStr: string
  params: string
  operateTime: number
  versionName: string
  versionCreateTime: number
  status: string
}

export interface ChangeLogQueryParams {
  projectId: string
  flowId: string
  creator?: string
  page?: number
  pageSize?: number
}

export interface ChangeLogListResponse {
  records: ChangeLogRecord[]
  count: number
  page: number
  pageSize: number
  totalPages: number
}

/**
 * 获取操作人列表
 * @param projectId 项目ID
 * @param flowId 创作流ID
 */
export async function getChangeLogOperators(projectId: string, flowId: string): Promise<string[]> {
  try {
    const response = await get<string[]>(
      `${PROCESS_API_URL_PREFIX}/user/version/projects/${projectId}/pipelines/${flowId}/operatorList`,
    )
    return response
  } catch (error) {
    throw error
  }
}

/**
 * 获取变更日志列表
 * @param projectId 项目ID
 * @param flowId 创作流ID
 * @param params 查询参数
 */
export async function getChangeLogList(
  params: ChangeLogQueryParams,
): Promise<ChangeLogListResponse> {
  try {
    const { projectId, flowId, ...otherParams } = params
    const response = await get<ChangeLogListResponse>(
      `${PROCESS_API_URL_PREFIX}/user/version/projects/${projectId}/pipelines/${flowId}/operationLog`,
      { params: otherParams },
    )
    return response
  } catch (error) {
    throw error
  }
}
