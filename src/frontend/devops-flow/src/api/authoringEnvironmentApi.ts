import { ENVIRONMENT_API_URL_PREFIX } from '@/utils/apiUrlPrefix'
import { get } from '@/utils/http'

/**
 * ====================================
 * Authoring Environment API Module
 * ====================================
 * 
 * This module centralizes all APIs related to authoring environments.
 * Including: environment list, node list, etc.
 */

// ============ Type Definitions ============

/**
 * Authoring environment item
 */
export interface AuthoringEnvItem {
  envHashId: string
  name: string
  desc: string
  envType: string
  envNodeType: string
  nodeCount: number
  tags: Array<{
    tagKeyId: number
    tagKeyName: string
    tagAllowMulValue: boolean
    canUpdate: 'INTERNAL' | 'TRUE' | 'FALSE'
    tagValues: Array<{
      tagValueId: number
      tagValueName: string
      nodeCount: number
      canUpdate: 'INTERNAL' | 'TRUE' | 'FALSE'
    }>
  }>
  envVars: Array<{
    name: string
    value: string
    secure: boolean
    lastUpdateUser: string
    lastUpdateTime: number
  }>
  createdUser: string
  createdTime: number
  updatedUser: string
  updatedTime: number
  canEdit: boolean
  canDelete: boolean
  canUse: boolean
  projectName: string
}

/**
 * Authoring node item
 */
export interface AuthoringNodeItem {
  nodeHashId: string
  nodeId: string
  name: string
  ip: string
  nodeStatus: string
  agentStatus: boolean
  nodeType: string
  osName: string
  createdUser: string
  operator: string
  bakOperator: string
  gateway: string
  displayName: string
  bizId: number
  envEnableNode: boolean
  lastModifyTime: number
  nodeName: string
  size: string
  agentHashId: string
  agentId: number
}

/**
 * Paginated response for node list
 */
export interface AuthoringNodeResponse {
  count: number
  page: number
  pageSize: number
  totalPages: number
  records: AuthoringNodeItem[]
}

/**
 * Creation node for UI display
 */
export interface CreationNode {
  id: string
  name: string
  ip: string
  status: 'online' | 'offline'
  agentStatus: boolean
  envEnableNode: boolean
}

/**
 * Authoring environment for UI display
 */
export interface AuthoringEnvironment {
  id: string
  name: string
  creationNodes: CreationNode[]
  workspace: string
  description?: string
}

/**
 * Environment list item for select component
 */
export interface EnvSelectItem extends AuthoringEnvItem {
  value: string
  label: string
}

// ============ API Request Parameters ============

/**
 * Parameters for fetching environment list
 */
export interface FetchEnvListParams {
  projectId: string
  envType?: string
}

/**
 * Parameters for fetching node list
 */
export interface FetchNodeListParams {
  projectId: string
  envHashId: string
}

// ============ API Functions ============

/**
 * Fetch authoring environment list
 * @param params - Request parameters
 * @returns Promise<AuthoringEnvItem[]>
 */
export async function fetchAuthoringEnvList(
  params: FetchEnvListParams
): Promise<AuthoringEnvItem[]> {
  const { projectId, envType = 'CREATE' } = params
  try {
    const res = await get<AuthoringEnvItem[]>(
      `${ENVIRONMENT_API_URL_PREFIX}/user/environment/${projectId}?envType=${envType}`
    )
    
    return res
  } catch (error) {
    console.error('Failed to fetch authoring environment list:', error)
    throw error
  }
}

/**
 * Fetch authoring node list by environment name
 * @param params - Request parameters
 * @returns Promise<AuthoringNodeResponse>
 */
export async function fetchAuthoringNodeList(
  params: FetchNodeListParams
): Promise<AuthoringNodeResponse> {
  const { projectId, envHashId } = params
  
  try {
    
    const res = await get<AuthoringNodeResponse>(
      `${ENVIRONMENT_API_URL_PREFIX}/user/environment/${projectId}/${envHashId}/listNodesNew`
    )
    return res
  } catch (error) {
    console.error('Failed to fetch authoring node list:', error)
    throw error
  }
}

// ============ Helper Functions ============

/**
 * Convert API AuthoringNodeItem to UI CreationNode
 * @param node - API node item
 * @returns CreationNode
 */
export function convertToCreationNode(node: AuthoringNodeItem): CreationNode {
  return {
    id: node.nodeHashId,
    name: node.displayName,
    ip: node.ip || '',
    status: node.agentStatus ? 'online' : 'offline',
    agentStatus: node.agentStatus,
    envEnableNode: node.envEnableNode,
  }
}

