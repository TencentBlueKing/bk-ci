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
  ReviewParam,
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

/**
 * 后端返回的 YAML 区域高亮标记（行列均为 0 起始）
 */
export interface YamlMarkPosition {
  line: number
  column: number
}

export interface YamlTransferMark {
  startMark: YamlMarkPosition
  endMark: YamlMarkPosition
}

/**
 * 后端 yamlPreview 支持的区域 key
 * 对应 com.tencent.devops.common.pipeline.pojo.transfer.PreviewResponse
 */
export type YamlPreviewSectionKey = 'pipeline' | 'trigger' | 'notice' | 'setting'

export type YamlHighlightBlockMap = Partial<Record<YamlPreviewSectionKey, YamlTransferMark[]>>

export interface YamlPreview extends YamlHighlightBlockMap {
  yaml: string
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
  baseVersion?: number | string
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
  pipelineId: string
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

// 注：model <-> yaml 的转换不在前端进行，统一通过后端
// `apiTransfer`（FULL_MODEL2YAML / FULL_YAML2MODEL）完成。
