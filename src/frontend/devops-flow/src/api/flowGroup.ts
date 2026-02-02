/**
 * 创作流组相关 API
 */
import { post, get, del, put } from '@/utils/http'
import { PROCESS_API_URL_PREFIX } from '@/utils/apiUrlPrefix'

export type ViewType = 1 | 2 | -1 // 1: 动态分组，2: 静态分组，-1: 未分组

export interface FlowGroupItem {
  id: string
  name: string
  pac?: boolean
  pipelineCount?: number
  projectId?: string
  icon?: string
  showAction?: boolean
  projected?: boolean // true 表示项目组，false 或 undefined 表示个人组
  pin?: boolean
  viewType?: ViewType
  createTime?: number
  updateTime?: number
  creator?: string
  top?: boolean
}

export interface FlowGroupCounts {
  totalCount: number
  myFavoriteCount: number
  myPipelineCount: number
  recycleCount: number
  recentUseCount: number
}

export interface EditGroupParams {
  id?: string
  name?: string
  projected?: boolean
  viewType?: number
  logic?: 'AND' | 'OR'
  filters?: []
  pipelineIds?: string[]
}

/**
 * 获取创作流组列表
 * 通过 projected 字段区分项目组和个人组
 */
export async function getFlowGroups(projectId: string): Promise<FlowGroupItem[]> {
  try {
    const res = await get<FlowGroupItem[]>(
      `${PROCESS_API_URL_PREFIX}/user/pipelineViews/projects/${projectId}/list`,
    )
    return res
  } catch (error) {
    throw error
  }
}

/**
 * 获取创作流组数量
 */
export async function getFlowGroupsCount(projectId: string): Promise<FlowGroupCounts> {
  try {
    const res = await get<FlowGroupCounts>(
      `${PROCESS_API_URL_PREFIX}/user/pipelines/projects/${projectId}/getCount`,
    )
    return res
  } catch (error) {
    throw error
  }
}

/**
 * 创建创作流组
 */
export async function createFlowGroup(
  projectId: string,
  params: EditGroupParams,
): Promise<FlowGroupItem> {
  try {
    const res = await post<FlowGroupItem>(
      `${PROCESS_API_URL_PREFIX}/user/pipelineViews/projects/${projectId}`,
      params,
    )
    return res
  } catch (error) {
    throw error
  }
}

/**
 * 删除创作流组
 */
export async function deleteFlowGroup(projectId: string, viewId: string): Promise<boolean> {
  try {
    const res = await del<boolean>(`${PROCESS_API_URL_PREFIX}/user/pipelineViews/projects/${projectId}/views/${viewId}`)
    return res
  } catch (error) {
    throw error
  }
}

/**
 * 重命名创作流组
 */
export async function renameFlowGroup(projectId: string, viewId: string, params: EditGroupParams,): Promise<boolean> {
  try {
    const res = await put<boolean>(`${PROCESS_API_URL_PREFIX}/user/pipelineViews/projects/${projectId}/views/${viewId}`, params)
    return res
  } catch (error) {
    throw error
  }
}

/**
 * 置顶创作流组
 */
export async function pinFlowGroup(projectId: string, viewId: string, enabled: boolean): Promise<boolean> {
  try {
    const res = await post<boolean>(`${PROCESS_API_URL_PREFIX}/user/pipelineViews/projects/${projectId}/views/${viewId}/top`, {enabled})
    return res
  } catch (error) {
    throw error
  }
}
