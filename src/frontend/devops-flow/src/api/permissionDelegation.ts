/**
 * 权限代持相关 API
 */
import { get, post } from '@/utils/http'
import { AUTH_API_URL_PREFIX } from '@/utils/apiUrlPrefix'
import { RESOURCE_TYPES } from '@/components/Permission/constants'
/**
 * 资源授权数据结构
 */
export interface ResourceAuthData {
  id: number
  projectCode: string
  resourceType: string
  resourceName: string
  resourceCode: string
  handoverTime: number
  handoverFrom: string
  handoverFromCnName: string
  executePermission: boolean
}

/**
 * 资源授权交接项
 */
export interface ResourceAuthorizationHandoverItem {
  projectCode: string
  resourceType: string
  resourceName: string
  resourceCode: string
  handoverFrom: string
  handoverTo: string
}

/**
 * 重置资源授权参数
 */
export interface ResetResourceAuthParams {
  projectCode: string
  resourceType: string
  handoverChannel: string
  resourceAuthorizationHandoverList: ResourceAuthorizationHandoverItem[]
}

/**
 * 重置失败项
 */
export interface ResetFailedItem {
  handoverFailedMessage?: string
  [key: string]: any
}

/**
 * 重置资源授权响应
 */
export interface ResetResourceAuthResponse {
  FAILED?: ResetFailedItem[]
  [key: string]: any
}

/**
 * 获取资源授权信息
 * @param projectId 项目ID
 * @param flowId 流水线ID
 */
export async function getResourceAuthorization({
  projectId,
  flowId,
}: {
  projectId: string
  flowId: string
}): Promise<ResourceAuthData> {
  try {
    const response = await get<ResourceAuthData>(
      `${AUTH_API_URL_PREFIX}/user/auth/authorization/${projectId}/${RESOURCE_TYPES.CREATIVE_STREAM}/getResourceAuthorization`,
      {
        params: { resourceCode: flowId },
      },
    )
    return response
  } catch (error) {
    throw error
  }
}

/**
 * 重置资源授权
 * @param projectId 项目ID
 * @param params 重置参数
 */
export async function resetResourceAuthorization(
  projectId: string,
  params: ResetResourceAuthParams,
): Promise<ResetResourceAuthResponse> {
  try {
    const response = await post<ResetResourceAuthResponse>(
      `${AUTH_API_URL_PREFIX}/user/auth/authorization/${projectId}/resetResourceAuthorization`,
      params,
    )
    return response
  } catch (error) {
    throw error
  }
}
