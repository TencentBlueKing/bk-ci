/**
 * 可用范围相关 API
 */
import { get, post, del } from '@/utils/http'
import { PROCESS_API_URL_PREFIX } from '@/utils/apiUrlPrefix'

export interface VisibleRangeRecord {
  type: string // USER, ORG, DEPT等
  scopeId: string
  scopeName: string
}

export interface VisibleRangeQueryParams {
  projectId: string
  pipelineId: string
  page?: number
  pageSize?: number
}

export interface VisibleRangeListResponse {
  records: VisibleRangeRecord[]
  count: number
}

export interface AddVisibleRangeItem {
  type: string // DEPT, USER, ORG, GROUP 等
  scopeId: string
  scopeName: string
}

export interface AddVisibleRangeParams {
  projectId: string
  pipelineId: string
  items: AddVisibleRangeItem[]
}

export interface RemoveVisibleRangeParams {
  projectId: string
  pipelineId: string
  ids: string[] // 可见范围ID列表
}

/**
 * 获取可用范围列表
 * @param params 查询参数
 */
export async function getVisibleRangeList(
  params: VisibleRangeQueryParams,
): Promise<VisibleRangeListResponse> {
  try {
    const { projectId, pipelineId, page, pageSize } = params
    const response = await get<VisibleRangeListResponse>(
      `${PROCESS_API_URL_PREFIX}/user/pipeline/visibility/${projectId}/${pipelineId}`,
      { 
        params: { 
          page,
          pageSize,
        } 
      },
    )
    return response
  } catch (error) {
    throw error
  }
}

/**
 * 添加可用范围
 * @param params 添加参数
 */
export async function addVisibleRange(params: AddVisibleRangeParams): Promise<void> {
  try {
    const { projectId, pipelineId, items } = params
    await post(
      `${PROCESS_API_URL_PREFIX}/user/pipeline/visibility/${projectId}/${pipelineId}`,
      items,
    )
  } catch (error) {
    throw error
  }
}

/**
 * 移除可用范围
 * @param params 移除参数
 */
export async function removeVisibleRange(params: RemoveVisibleRangeParams): Promise<void> {
  try {
    const { projectId, pipelineId, ids } = params
    await del(
      `${PROCESS_API_URL_PREFIX}/user/pipeline/visibility/${projectId}/${pipelineId}`,
      {
        data: ids,
      },
    )
  } catch (error) {
    throw error
  }
}
