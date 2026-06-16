import { computed } from 'vue'
import type { YamlPreviewSectionKey, YamlTransferMark } from '@/api/flowModel'
import { useFlowModel } from './useFlowModel'

/**
 * Flow configuration section types
 */
export type FlowConfigSection =
  | 'authoring-env'
  | 'basic-setting'
  | 'notice'
  | 'trigger-event'
  | 'permission-settings'
  | 'permission-delegation'
  | 'flow-model'

/**
 * Hook options
 */
export interface UseFlowConfigCodeOptions {
  projectId: string
  flowId?: string
  version: string
  section: FlowConfigSection
  autoLoad?: boolean
}

/**
 * 前端 section 到后端 yamlPreview key 的映射。
 * 后端 PreviewResponse 返回 pipeline/trigger/notice/setting 四个区域，
 * Flow 侧 authoring-env / basic-setting / permission-settings / permission-delegation
 * 都从属于同一个 setting 段，因此统一映射到 `setting`。
 */
const SECTION_TO_BACKEND_KEY: Record<FlowConfigSection, YamlPreviewSectionKey> = {
  'authoring-env': 'setting',
  'basic-setting': 'setting',
  notice: 'notice',
  'trigger-event': 'trigger',
  'permission-settings': 'setting',
  'permission-delegation': 'setting',
  'flow-model': 'pipeline',
}

/**
 * Hook to manage flow configuration code display with highlighting.
 *
 * 高亮区域完全使用后端 yamlPreview 返回的精确标记（0-based 行列），
 * 与 CodeEditor 的 `startMark.line + 1` 转换配合。
 */
export function useFlowConfigCode(options: UseFlowConfigCodeOptions) {
  const { section } = options
  const {
    yamlContent,
    yamlHighlightBlockMap,
    loading,
    flowSetting,
    flowModel,
    flowModelWithoutTriggerStage,
    isFlowEmpty,
  } = useFlowModel()

  const sectionHighlight = computed<YamlTransferMark[]>(() => {
    const backendKey = SECTION_TO_BACKEND_KEY[section]
    return yamlHighlightBlockMap.value?.[backendKey] ?? []
  })

  const isEmpty = computed(() => !flowSetting.value)

  return {
    loading,
    flowSetting,
    flowModel,
    flowModelWithoutTriggerStage,
    isFlowEmpty,
    yamlContent,
    sectionHighlight,
    isEmpty,
  }
}
