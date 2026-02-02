import { computed } from 'vue';
import { useFlowModel } from './useFlowModel';

/**
 * Flow YAML 配置区域类型
 */
export type FlowConfigSection =
  | 'authoring-env'
  | 'basic-setting'
  | 'notice'
  | 'trigger-event'
  | 'permission-settings'
  | 'permission-delegation'

interface HighlightRange {
  startMark: { line: number; column: number }
  endMark: { line: number; column: number }
}

interface UseYamlHighlightOptions {
  flowId?: string
  version: string
  projectId: string
  autoLoad?: boolean
}

// 配置区域模式定义
const SECTION_PATTERNS: Record<FlowConfigSection, string[]> = {
  'authoring-env': ['# Authoring Environment Configuration', 'authoring-env:'],
  'basic-setting': ['name:', 'desc:', 'version:'],
  'notice': ['notices:'],
  'trigger-event': ['on:'],
  'permission-settings': ['permissions:', 'access-control:'],
  'permission-delegation': ['delegation:', 'delegates:'],
}

// 区域结束标记
const END_SECTION_PATTERN = /^(on|variables|stages|notices|concurrency|syntax-dialect):/

/**
 * 计算 YAML 区域高亮范围
 */
function calculateSectionHighlight(yaml: string, section: FlowConfigSection): HighlightRange[] {
  if (!yaml) return []

  const lines = yaml.split('\n')
  const patterns = SECTION_PATTERNS[section] || []
  let startLine = -1
  let endLine = -1

  // 查找区域起始行
  for (let i = 0; i < lines.length; i++) {
    const line = lines[i]?.trim()
    if (patterns.some((pattern) => line?.startsWith(pattern))) {
      startLine = i + 1 // Monaco 使用 1-based 行号
      break
    }
  }

  if (startLine <= 0) return []

  // 查找区域结束行
  switch (section) {
    case 'authoring-env':
      for (let i = startLine; i < lines.length; i++) {
        const line = lines[i]?.trim()
        const nextLine = lines[i + 1]?.trim()
        if ((line === '' && nextLine?.match(/^[a-zA-Z]/)) || line?.match(END_SECTION_PATTERN)) {
          endLine = i
          break
        }
      }
      break

    case 'basic-setting':
      endLine = Math.min(startLine + 5, lines.length)
      break

    case 'notice':
      for (let i = startLine; i < lines.length; i++) {
        if (lines[i]?.trim()?.match(/^(concurrency|syntax-dialect):/)) {
          endLine = i
          break
        }
      }
      if (endLine === -1) endLine = lines.length
      break

    case 'trigger-event':
      for (let i = startLine; i < lines.length; i++) {
        if (lines[i]?.trim()?.match(/^(variables|stages):/)) {
          endLine = i
          break
        }
      }
      if (endLine === -1) endLine = startLine + 10
      break

    default:
      endLine = Math.min(startLine + 10, lines.length)
  }

  if (startLine > 0 && endLine > startLine) {
    return [{ startMark: { line: startLine, column: 1 }, endMark: { line: endLine, column: 1 } }]
  }

  return []
}

/**
 * YAML 高亮 Hook - 统一管理 Flow 配置的 YAML 代码高亮
 * 
 * @example
 * ```ts
 * const { yamlContent, getSectionHighlight } = useYamlHighlight({ flowId })
 * const authoringEnvHighlight = getSectionHighlight('authoring-env')
 * ```
 */
export function useYamlHighlight(options: UseYamlHighlightOptions) {
  if (!options) {
    throw new Error('options is required')
  }

  const { yamlContent, loading, flowModel, flowSetting } = useFlowModel()

  /**
   * 获取指定配置区域的高亮范围
   */
  const getSectionHighlight = (section: FlowConfigSection) => {
    return computed(() => calculateSectionHighlight(yamlContent.value, section))
  }

  /**
   * 检查配置是否为空
   */
  const isEmpty = computed(() => !flowSetting.value)

  return {
    loading,
    yamlContent,
    flowModel,
    flowSetting,
    isEmpty,
    getSectionHighlight,
    // 预设常用的高亮计算
    authoringEnvHighlight: computed(() => calculateSectionHighlight(yamlContent.value, 'authoring-env')),
    triggerEventHighlight: computed(() => calculateSectionHighlight(yamlContent.value, 'trigger-event')),
    noticeHighlight: computed(() => calculateSectionHighlight(yamlContent.value, 'notice')),
  }
}

export type { HighlightRange, UseYamlHighlightOptions };





