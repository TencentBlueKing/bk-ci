import { type FlowInfo, type FlowVersion } from '@/types/flow'
import { PROCESS_API_URL_PREFIX } from '@/utils/apiUrlPrefix'
import { get, post } from '@/utils/http'

export interface ResponseWithRecords<T> {
  records: T[]
  total: number
  page: number
  pageSize: number
}
/**
 * Get flow basic info
 * Falls back to mock data on API failure when ENABLE_MOCK_FALLBACK is true
 */
export async function fetchFlowInfo({
  projectId,
  flowId,
}: {
  projectId: string
  flowId: string
}): Promise<FlowInfo> {
  return await get<FlowInfo>(
    `${PROCESS_API_URL_PREFIX}/user/version/projects/${projectId}/pipelines/${flowId}/detail`,
  )
}

/**
 * Get flow version list with pagination and search support
 */
export async function getFlowVersionList({
  projectId,
  flowId,
  page,
  pageSize,
  versionName,
}: {
  projectId: string
  flowId: string
  page?: number
  pageSize?: number
  versionName?: string
}): Promise<ResponseWithRecords<FlowVersion>> {
  const params: Record<string, string> = {}
  if (page !== undefined) params.page = String(page)
  if (pageSize !== undefined) params.pageSize = String(pageSize)
  if (versionName) params.versionName = versionName

  const query = Object.keys(params).length ? `?${new URLSearchParams(params).toString()}` : ''
  const res = await get<ResponseWithRecords<FlowVersion>>(
    `/process/api/user/version/projects/${projectId}/pipelines/${flowId}/versions${query}`,
  )
  return res
}

/**
 * Update build remark
 */
export async function updateRemark({
  projectId,
  pipelineId,
  buildId,
  remark,
}: {
  projectId: string
  pipelineId: string
  buildId: string
  remark: string
}): Promise<boolean> {
  await post(
    `${PROCESS_API_URL_PREFIX}/user/builds/${projectId}/${pipelineId}/${buildId}/updateRemark`,
    { remark },
  )
  return true
}
