/**
 * 创作流模型相关 API
 *
 * NOTE: Core types (Stage, Container, Element, etc.) are defined in @/types/flow.ts
 * and re-exported here for backward compatibility.
 */

import { get } from '@/utils/http'
import type { FlowModel, FlowSettings } from '../types/flow'

// Re-export types from types/flow.ts for backward compatibility
export type {
  AdditionalOptions,
  CheckConfig,
  ReviewGroup,
  Container,
  CustomVariable,
  DispatchType,
  Element,
  FlowModel,
  FlowSettings,
  JobControlOption,
  MatrixControlOption,
  MutexGroup,
  Param,
  Stage,
  StageControlOption,
  Subscription,
} from '../types/flow'

export interface FlowModelAndSetting {
  version: number
  versionName: string
  baseVersion: number
  baseVersionName: string
  modelAndSetting: {
    model: FlowModel
    setting: FlowSettings
  }
  yamlPreview?: YamlPreview
  canDebug: boolean
  yamlSupported: boolean
  yamlInvalidMsg?: string
  updater: string
  updateTime: number
}

export interface YamlPreview {
  yaml: string
  [key: string]: unknown
}

/**
 * 获取 Flow 模型数据
 * @param flowId 创作流 ID
 * @param version 版本号（可选）
 */
export async function getFlowModel(
  projectId: string,
  flowId: string,
  version?: string,
): Promise<FlowModelAndSetting> {
  const response = await get<FlowModelAndSetting>(
    `/process/api/user/version/projects/${projectId}/pipelines/${flowId}/versions/${version}`,
  )
  return response
}

/**
 * 保存 Flow 模型数据的请求参数
 */
export interface SaveFlowModelParams {
  projectId: string
  pipelineId?: string
  baseVersion?: string
  storageType?: 'MODEL' | 'YAML'
  modelAndSetting?: {
    model: FlowModel
    setting?: FlowSettings
  }
  yaml?: string
}

/**
 * 保存 Flow 模型数据的响应
 */
export interface SaveFlowModelResponse {
  version: string
  versionName: string
  flowId: string
}

/**
 * 保存 Flow 模型数据
 * @param params 保存参数
 */
export async function saveFlowModel(params: SaveFlowModelParams): Promise<SaveFlowModelResponse> {
  const { post } = await import('@/utils/http')
  const { projectId, ...restParams } = params

  const response = await post<SaveFlowModelResponse>(
    `/process/api/user/version/projects/${projectId}/saveDraft`,
    restParams,
  )

  return response
}

/**
 * 将 Flow 模型转换为 YAML 格式
 * @param model Flow 模型数据
 */
export function flowModelToYaml(model: FlowModel): string {
  // TODO: 实现实际的转换逻辑
  // 这里简单返回 JSON 字符串作为示例
  return ''
}

/**
 * 将 YAML 格式转换为 Flow 模型
 * @param yaml YAML 字符串
 */
export function yamlToFlowModel(yaml: string): FlowModel {
  // TODO: 实现实际的转换逻辑
  // 这里简单解析 JSON 字符串作为示例
  try {
    return JSON.parse(yaml)
  } catch (error) {
    console.error('Failed to parse YAML:', error)
    throw new Error('Invalid YAML format')
  }
}
