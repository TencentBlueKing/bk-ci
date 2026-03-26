/**
 * Variable management API
 */
import { post } from '@/utils/http'
import { type ReadOnlyVariableGroup } from '@/types/variable'
import type { FlowModel, Param } from './flowModel'

const PROCESS_API_URL_PREFIX = '/process/api'
const STORE_API_URL_PREFIX = '/store/api'

/**
 * Get system variables (grouped)
 * Reference: devops-pipeline/src/store/modules/atom/actions.js requestCommonParams
 */
export async function getSystemVariables(): Promise<ReadOnlyVariableGroup[]> {
  try {
    const data = await post<ReadOnlyVariableGroup[]>(
      `${PROCESS_API_URL_PREFIX}/user/buildParam/common`,
    )
    return data || []
  } catch (error) {
    console.error('Failed to get system variables:', error)
    return []
  }
}

/**
 * Update flow model's trigger stage container params with variables
 * @param model Flow model
 * @param variables Flow variables
 */
export function updateFlowModelParams(model: FlowModel | null, variables: Param[]): void {
  if (!model || !model.stages || model.stages.length === 0) {
    return
  }

  // Get trigger stage (first stage)
  const triggerStage = model.stages[0]
  if (!triggerStage) {
    return
  }

  // Ensure containers array exists
  if (!triggerStage.containers) {
    triggerStage.containers = []
  }

  // Update params in the first container
  const triggerContainer = triggerStage.containers[0]
  if (!triggerContainer) {
    return
  }
  triggerContainer.params = variables
}

/**
 * Get plugin output variables from API
 * Reference: devops-pipeline/src/store/modules/atom/actions.js fetchAtomsOutput
 * @param elements Array of elements from flow model to get atom codes and versions
 */
export async function getPluginOutputVariables(
  elements: Array<{ atomCode?: string; version?: string }>,
): Promise<Record<string, any>> {
  try {
    // Extract unique atom codes with versions
    const arr = elements
      .filter((ele) => ele.atomCode && ele.version)
      .map((ele) => `${ele.atomCode}@${ele.version}`)
    const data = Array.from(new Set(arr))

    if (data.length === 0) {
      return {}
    }

    const response = await post<Record<string, string>>(
      `${STORE_API_URL_PREFIX}/user/pipeline/atom/output/info/list`,
      data,
    )

    // Parse JSON strings in response
    const map: Record<string, any> = {}
    for (const item in response) {
      try {
        const value = response[item]
        if (value && typeof value === 'string') {
          map[item] = JSON.parse(value)
        } else {
          map[item] = value
        }
      } catch (e) {
        console.error('Failed to parse output for', item, e)
      }
    }

    return map
  } catch (error) {
    console.error('Failed to get plugin output variables:', error)
    return {}
  }
}
