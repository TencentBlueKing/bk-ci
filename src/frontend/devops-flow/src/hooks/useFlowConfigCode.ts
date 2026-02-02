import { computed } from 'vue'
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
 * Hook to manage flow configuration code display with highlighting
 */
export function useFlowConfigCode(options: UseFlowConfigCodeOptions) {
  const { section } = options
  const { yamlContent, loading, flowSetting, flowModel } = useFlowModel()

  /**
   * Calculate highlight ranges for specific configuration section
   */
  const sectionHighlight = computed(() => {
    const yaml = yamlContent.value
    if (!yaml) return []

    const lines = yaml.split('\n')
    let startLine = -1
    let endLine = -1

    // Define section patterns
    const sectionPatterns: Record<FlowConfigSection, string[]> = {
      'authoring-env': ['# Authoring Environment Configuration', 'authoring-env:'],
      'basic-setting': ['name:', 'desc:', 'version:'],
      notice: ['notices:'],
      'trigger-event': ['on:'],
      'permission-settings': ['permissions:', 'access-control:'],
      'permission-delegation': ['delegation:', 'delegates:'],
    }

    const patterns = sectionPatterns[section] || []

    // Find the section start
    for (let i = 0; i < lines.length; i++) {
      const line = lines[i]?.trim()

      for (const pattern of patterns) {
        if (line?.startsWith(pattern)) {
          startLine = i + 1 // Monaco Editor uses 1-based line numbers
          break
        }
      }

      if (startLine > 0) break
    }

    if (startLine > 0) {
      // Find the end of section
      if (section === 'authoring-env') {
        // For authoring-env, find next major section
        for (let i = startLine; i < lines.length; i++) {
          const line = lines[i]?.trim()
          if (line === '' && lines[i + 1]?.trim()?.match(/^[a-zA-Z]/)) {
            endLine = i
            break
          }
          if (line?.match(/^(on|variables|stages|notices|concurrency|syntax-dialect):/)) {
            endLine = i
            break
          }
        }
      } else if (section === 'basic-setting') {
        // For basic settings, highlight first few lines
        endLine = Math.min(startLine + 5, lines.length)
      } else if (section === 'notice') {
        // For notices, find end of notices section
        for (let i = startLine; i < lines.length; i++) {
          const line = lines[i]?.trim()
          if (line?.match(/^(concurrency|syntax-dialect):/)) {
            endLine = i
            break
          }
        }
        if (endLine === -1) endLine = lines.length
      } else if (section === 'trigger-event') {
        // For trigger events, find end of on section
        for (let i = startLine; i < lines.length; i++) {
          const line = lines[i]?.trim()
          if (line?.match(/^(variables|stages):/)) {
            endLine = i
            break
          }
        }
        if (endLine === -1) endLine = startLine + 10
      } else {
        // Default: highlight next 10 lines or until next major section
        endLine = Math.min(startLine + 10, lines.length)
      }
    }

    if (startLine > 0 && endLine > startLine) {
      return [
        {
          startMark: { line: startLine, column: 1 },
          endMark: { line: endLine, column: 1 },
        },
      ]
    }

    return []
  })

  /**
   * Check if configuration is empty
   */
  const isEmpty = computed(() => !flowSetting.value)

  return {
    loading,
    flowSetting,
    flowModel,
    yamlContent,
    sectionHighlight,
    isEmpty,
  }
}
