import { get, post, put, del } from '@/utils/http'
import { PROCESS_API_URL_PREFIX } from '@/utils/apiUrlPrefix'

/**
 * 标签分组相关 API
 */

/**
 * 标签项接口
 */
export interface GroupLabelItem {
  id: string
  groupId: string
  name: string
  createTime: number
  uptimeTime: number
  createUser: string
  updateUser: string
}

/**
 * 分组响应接口
 */
export interface GroupResponse {
  id: string
  projectId: string
  name: string
  createTime: number
  updateTime: number
  createUser: string
  updateUser: string
  labels: GroupLabelItem[]
}

/**
 * 更新分组参数
 */
export interface UpdateGroupParams {
  id: string
  projectId: string
  name: string
}

/**
 * 更改标签参数
 */
export interface ChangeTagParams {
  id?: string
  groupId: string
  name: string
}

/**
 * 获取项目标签分组列表
 */
export async function getProjectGroups(projectId: string): Promise<GroupResponse[]> {
  try {
    const res = await get<GroupResponse[]>(
      `${PROCESS_API_URL_PREFIX}/user/pipelineGroups/groups?projectId=${projectId}`,
    )
    return res
  } catch (error) {
    throw error
  }
}

/**
 * 更新标签分组
 */
export async function updateProjectGroups(
  projectId: string,
  params: UpdateGroupParams,
): Promise<boolean> {
  try {
    const res = await put<boolean>(
      `${PROCESS_API_URL_PREFIX}/user/pipelineGroups/groups?projectId=${projectId}`,
      params,
    )
    return res
  } catch (error) {
    throw error
  }
}

/**
 * 添加标签分组
 */
export async function addProjectGroups(
  projectId: string,
  params: UpdateGroupParams,
): Promise<true> {
  try {
    const res = await post<true>(
      `${PROCESS_API_URL_PREFIX}/user/pipelineGroups/groups?projectId=${projectId}`,
      params,
    )
    return res
  } catch (error) {
    throw error
  }
}

/**
 * 删除标签分组
 */
export async function deleteProjectGroups(projectId: string, groupId: string): Promise<true> {
  try {
    const res = await del<true>(
      `${PROCESS_API_URL_PREFIX}/user/pipelineGroups/groups?projectId=${projectId}&groupId=${groupId}`,
    )
    return res
  } catch (error) {
    throw error
  }
}

/**
 * 更新标签
 */
export async function updateProjectTags(
  projectId: string,
  params: ChangeTagParams,
): Promise<boolean> {
  try {
    const res = await put<boolean>(
      `${PROCESS_API_URL_PREFIX}/user/pipelineGroups/labels?projectId=${projectId}`,
      params,
    )
    return res
  } catch (error) {
    throw error
  }
}

/**
 * 添加标签
 */
export async function addProjectTags(projectId: string, params: ChangeTagParams): Promise<true> {
  try {
    const res = await post<true>(
      `${PROCESS_API_URL_PREFIX}/user/pipelineGroups/labels?projectId=${projectId}`,
      params,
    )
    return res
  } catch (error) {
    throw error
  }
}

/**
 * 删除标签
 */
export async function deleteProjectTags(projectId: string, labelId: string): Promise<true> {
  try {
    const res = await del<true>(
      `${PROCESS_API_URL_PREFIX}/user/pipelineGroups/labels?projectId=${projectId}&labelId=${labelId}`,
    )
    return res
  } catch (error) {
    throw error
  }
}
