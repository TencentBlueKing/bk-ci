/**
 * 触发器市场相关 API
 */

import { get } from '@/utils/http'

const STORE_API_URL_PREFIX = '/store/api'

/**
 * 触发器分类信息
 */
export interface TriggerType {
  ownerStoreCode: string
  name: string
  count: number
  weight?: number
}

/**
 * 触发器基础信息（列表项）
 */
export interface TriggerBaseItem {
  atomCode: string
  name: string
  summary?: string
  logoUrl?: string
  version?: string
  publisher?: string
  score?: number
  recentExecuteNum?: number
  hotFlag?: boolean
  recommendFlag?: boolean
  installed?: boolean
  defaultFlag?: boolean
  defaultVersion?: string
  ownerStoreCode: string
}

/**
 * 触发器详情配置
 */
export interface TriggerModal {
  atomCode: string
  name: string
  version: string
  classType?: string
  sourceCode?: string
  os?: string[]
  input?: Record<string, any>
  output?: Record<string, any>
  props?: Record<string, any>
  htmlTemplateVersion?: string
  [key: string]: any
}

/**
 * 获取触发器分类列表
 * @returns 分类列表
 */
export function fetchTriggerTypes(): Promise<TriggerType[]> {
  return get<TriggerType[]>(`${STORE_API_URL_PREFIX}/user/market/trigger/listOwnerStoreCodes`)
}

/**
 * 获取触发器列表参数
 */
export interface FetchTriggerListParams {
  ownerStoreCode?: string
  keyword?: string
  page?: number
  pageSize?: number
}

/**
 * 触发器列表响应
 */
export interface FetchTriggerListResponse {
  records: TriggerBaseItem[]
  count: number
  page: number
  pageSize: number
}

/**
 * 获取触发器列表
 * @param params 查询参数
 * @returns 触发器列表
 */
export function fetchTriggerList(params: FetchTriggerListParams = {}): Promise<FetchTriggerListResponse> {
  return get<FetchTriggerListResponse>(`${STORE_API_URL_PREFIX}/user/market/trigger/list`, {
    params,
  })
}

/**
 * 获取触发器详情配置
 * @param ownerStoreCode 来源代码
 * @param atomCode 插件代码
 * @param version 版本号
 * @returns 触发器配置详情
 */
export function fetchTriggerModal(
  ownerStoreCode: string,
  atomCode: string,
  version: string,
): Promise<TriggerModal> {
  return get<TriggerModal>(
    `${STORE_API_URL_PREFIX}/user/market/trigger/${atomCode}/${version}`,
  )
}
