import {
  fetchAuthoringNodeList,
  type AuthoringNodeItem,
  type AuthoringNodeResponse,
} from '@/api/authoringEnvironmentApi'
import { get, post } from '@/utils/http'

// Re-export types for backward compatibility
export type { AuthoringNodeItem, AuthoringNodeResponse as AuthoringNodesResponse }

/**
 * Startup info response from API
 */
export interface StartupInfo {
  canManualStartup: boolean
  canElementSkip?: boolean
  useLatestParameters?: boolean
  buildNo?: {
    buildNo: number
    buildNoType: string
    required: boolean
    currentBuildNo?: number
  }
  properties: StartupProperty[]
}

/**
 * Startup property item
 */
export interface StartupProperty {
  maxLength?: number
  id: string
  name?: string
  required: boolean
  constant: boolean
  type?: string
  defaultValue?: any
  value?: any
  desc?: string
  readOnly?: boolean
  valueNotEmpty?: boolean
  propertyType?: string
  label?: string
  isChanged?: boolean
  options?: Array<{ id: string; name: string }>
  category?: string
  // Payload for dynamic options from API
  payload?: {
    url?: string
    dataPath?: string
    paramId?: string
    paramName?: string
    [key: string]: unknown
  }
}

/**
 * Pipeline model response
 */
export interface PipelineModelResponse {
  modelAndSetting?: {
    model: {
      name: string
      stages: any[]
      [key: string]: any
    }
    [key: string]: any
  }
  [key: string]: any
}

/**
 * Execute pipeline response
 */
export interface ExecutePipelineResponse {
  id: string
  [key: string]: any
}

/**
 * Get startup info for manual execution
 * Falls back to mock data on API failure when ENABLE_MOCK_FALLBACK is true
 */
export async function requestStartupInfo({
  projectId,
  flowId,
  version,
}: {
  projectId: string
  flowId: string
  version?: number
}): Promise<StartupInfo> {
  const params: Record<string, any> = {}
  if (version) {
    params.version = version
  }
  return await get<StartupInfo>(
    `/process/api/user/builds/${projectId}/${flowId}/manualStartupInfo`,
    { params },
  )
}

/**
 * Get pipeline model by version
 * Falls back to mock data on API failure when ENABLE_MOCK_FALLBACK is true
 */
export async function fetchPipelineByVersion({
  projectId,
  flowId,
  version,
}: {
  projectId: string
  flowId: string
  version?: number
}): Promise<PipelineModelResponse> {
  const params: Record<string, any> = {}
  if (version) {
    params.version = version
  }

  return await get<PipelineModelResponse>(`/process/api/user/pipelines/${projectId}/${flowId}`, {
    params,
  })
}

/**
 * Get authoring nodes list for preview execution
 * Uses the centralized authoringEnvironmentApi
 */
export async function requestAuthoringNodes({
  projectId,
  envHashId,
}: {
  projectId: string
  envHashId: string
}): Promise<AuthoringNodeResponse> {
  return await fetchAuthoringNodeList({ projectId, envHashId })
}

/**
 * Execute pipeline
 * Falls back to mock data on API failure when ENABLE_MOCK_FALLBACK is true
 */
export async function requestExecPipeline({
  projectId,
  pipelineId,
  version,
  params,
  remark,
  resourceHashId,
}: {
  projectId: string
  pipelineId: string
  version?: number
  params: Record<string, any>
  remark?: string
  resourceHashId?: string
}): Promise<ExecutePipelineResponse> {
  const query: Record<string, any> = {}
  if (version) {
    query.version = version
  }
  // Handle buildNo parameter
  if (params.buildNo && typeof params.buildNo.currentBuildNo !== 'undefined') {
    query.buildNo = params.buildNo.currentBuildNo
    delete params.buildNo
  }

  // Add remark (run message) to params body with correct field name
  if (remark) {
    params.BK_CI_BUILD_MSG = remark
  }
  // Add resourceHashId (selected node) to params body with correct field name
  if (resourceHashId) {
    params.BK_CI_NODE_AGENT_ID = resourceHashId
  }

  return await post<ExecutePipelineResponse>(
    `/process/api/user/builds/${projectId}/${pipelineId}`,
    params,
    { params: query },
  )
}
