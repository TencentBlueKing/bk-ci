import { get } from '@/utils/http'
import { PROCESS_API_URL_PREFIX } from '@/utils/apiUrlPrefix'
import { type StatusType } from '@/types/flow'
/**
 * 触发记录相关 API
 */
export interface TriggerRecordItem {
  detailId: number
  projectId: string
  eventId: number
  triggerType: 'CODE_GIT' | 'MANUAL' | 'TIMER' | 'REMOTE'
  eventSource: string
  eventType: string
  triggerUser: string
  eventDesc: string
  eventTime: number
  status: StatusType
  pipelineId: string
  pipelineName: string
  buildId?: string
  buildNum?: string
  reason?: string
  reasonDetailList?: string[]
}

export interface TriggerRecordParams {
  projectId: string
  pipelineId?: string
  page?: number
  pageSize?: number
  startTime?: number | undefined
  endTime?: number | undefined
  eventType?: string
  triggerUser?: string
  triggerType?: string
}

export interface TriggerRecordListResponse {
  records: TriggerRecordItem[]
  count: number
}

// 查询列表函数类型
export type QueryListFunction = (page: number, pageSize?: number, isRefresh?: boolean) => void

/**
 * 触发器和事件类型项
 */
export interface TypeItem {
  id: string
  value: string
}

/**
 * 获取触发记录列表
 */
export async function getTriggerRecords(
  triggerParams: TriggerRecordParams,
): Promise<TriggerRecordListResponse> {
  try {
    const { projectId, pipelineId, ...params } = triggerParams
    const response = await get<TriggerRecordListResponse>(
      `${PROCESS_API_URL_PREFIX}/user/trigger/event/${projectId}/${pipelineId}/listPipelineTriggerEvent`,
      { params },
    )
    return response
  } catch (error) {
    throw error
  }
}

/**
 * 获取触发器类型列表
 */
export async function getTriggerTypes(): Promise<TypeItem[]> {
  try {
    const response = await get<TypeItem[]>(
      `${PROCESS_API_URL_PREFIX}/user/trigger/event/listTriggerType`,
    )
    return response
  } catch (error) {
    throw error
  }
}

/**
 * 获取事件类型列表
 */
export async function getEventTypes(): Promise<TypeItem[]> {
  try {
    const response = await get<TypeItem[]>(
      `${PROCESS_API_URL_PREFIX}/user/trigger/event/listEventType`,
    )
    return response
  } catch (error) {
    throw error
  }
}
