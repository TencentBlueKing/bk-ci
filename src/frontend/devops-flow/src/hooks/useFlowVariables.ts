import type { Element, Param } from '@/api/flowModel'
import { getPluginOutputVariables, getSystemVariables, updateFlowModelParams } from '@/api/variable'
import { useFlowModelStore } from '@/stores/flowModel'
import type { ReadOnlyVariableGroup } from '@/types/variable'
import { storeToRefs } from 'pinia'
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'

/**
 * Hook for managing all flow variables (flow variables, plugin output variables, system variables)
 */
export function useFlowVariables(flowId: string) {
  const flowModelStore = useFlowModelStore()
  const route = useRoute()
  const { flowModel } = storeToRefs(flowModelStore)

  // Flow variables - from flowModel using computed, directly use Param type
  const projectId = computed(() => {
    return route.params.projectId as string
  })

  const currentVersion = computed(() => {
    return (route.params.version as string) || ''
  })
  const variables = computed<Param[]>(() => {
    if (!flowModel.value || !flowModel.value.stages || flowModel.value.stages.length === 0) {
      return []
    }

    // Get trigger stage (first stage)
    const triggerStage = flowModel.value.stages[0]
    if (!triggerStage || !triggerStage.containers || triggerStage.containers.length === 0) {
      return []
    }

    // Get params from trigger container
    const triggerContainer = triggerStage.containers[0]
    if (!triggerContainer || !triggerContainer.params) {
      return []
    }

    // Ensure all params have order field, use index as default
    const params = triggerContainer.params.map((param, index) => ({
      ...param,
      order: param.order ?? index,
    }))

    // Sort by order field
    return params.sort((a, b) => {
      const orderA = a.order ?? Infinity
      const orderB = b.order ?? Infinity
      return orderA - orderB
    })
  })

  // Plugin output variables
  const pluginOutputVariables = ref<ReadOnlyVariableGroup[]>([])
  const atomsOutputMap = ref<Record<string, any>>({})
  const pluginOutputLoading = ref(false)

  // System variables
  const systemVariables = ref<ReadOnlyVariableGroup[]>([])
  const systemVariablesLoading = ref(false)

  onMounted(() => {
    if (flowId) {
      flowModelStore.loadFlowModel(projectId.value, flowId, currentVersion.value)
    }
  })

  // Get all elements from flow model (excluding trigger stage)
  const allElements = computed<Element[]>(() => {
    if (!flowModel.value || !flowModel.value.stages) {
      return []
    }

    const elements: Element[] = []
    // Skip trigger stage at index 0
    flowModel.value.stages.slice(1).forEach((stage) => {
      stage.containers?.forEach((container) => {
        container.elements?.forEach((element) => {
          elements.push(element)
        })
      })
    })
    return elements
  })

  /**
   * Update flowModel with current variables
   */
  const syncVariablesToFlowModel = () => {
    if (flowModel.value) {
      updateFlowModelParams(flowModel.value, variables.value)
      flowModelStore.updateFlowModel(flowModel.value)
    }
  }

  /**
   * Add a new variable
   */
  const addVariable = (variable: Param) => {
    // Set order for new variable (append to end)
    const maxOrder = variables.value.reduce((max, v) => {
      const order = v.order ?? 0
      return Math.max(max, order)
    }, -1)
    const newVariable = {
      ...variable,
      order: maxOrder + 1,
    }
    const updatedVariables = [...variables.value, newVariable]
    updateParams(updatedVariables)
  }

  /**
   * Update an existing variable
   */
  const updateVariable = (variable: Param) => {
    const updatedVariables = variables.value.map((v) => (v.id === variable.id ? variable : v))
    updateParams(updatedVariables)
  }

  /**
   * Delete a variable
   */
  const removeVariable = async (variableId: string) => {
    const updatedVariables = variables.value.filter((v) => v.id !== variableId)
    updateParams(updatedVariables)
  }

  /**
   * Update variable order
   */
  const updateParams = (newParams: Param[]) => {
    if (flowModel.value) {
      // Ensure all params have order field
      const paramsWithOrder = newParams.map((param, index) => ({
        ...param,
        order: param.order ?? index,
      }))
      updateFlowModelParams(flowModel.value, paramsWithOrder)
      flowModelStore.updateFlowModel(flowModel.value)
    }
  }
  /**
   * Get existing variable IDs
   */
  const existingIds = computed(() => variables.value.map((v) => v.id))

  /**
   * Fetch plugin output variables from API
   */
  const fetchPluginOutputVariables = async () => {
    if (allElements.value.length === 0) {
      pluginOutputVariables.value = []
      return
    }

    pluginOutputLoading.value = true
    try {
      // Get output map from API
      atomsOutputMap.value = await getPluginOutputVariables(allElements.value)

      // Build plugin output variables list
      // Reference: devops-pipeline/src/components/PipelineEditTabs/components/atom-output-var.vue
      const variableGroups: ReadOnlyVariableGroup[] = []
      // Skip trigger stage at index 0
      flowModel.value?.stages.slice(1).forEach((stage, sIdx) => {
        stage.containers?.forEach((container, cIdx) => {
          container.elements?.forEach((element, eIdx) => {
            const key = `${element.atomCode}@${element.version}`
            // Get output from API map first, fallback to model data
            const apiOutput = atomsOutputMap.value[key] || {}
            const modelOutput = element.data?.output || {}
            const realOutput = Object.keys(apiOutput).length > 0 ? apiOutput : modelOutput

            if (Object.keys(realOutput).length > 0) {
              variableGroups.push({
                hasStepId: !!(element.stepId && typeof element.stepId !== 'undefined'),
                name: `${sIdx + 1}-${cIdx + 1}-${eIdx + 1}-${element.name}`,
                params: Object.keys(realOutput).map((item) => ({
                  id: item,
                  name: item,
                  desc: realOutput[item]?.description || realOutput[item]?.desc,
                })),
              })
            }
          })
        })
      })

      pluginOutputVariables.value = variableGroups
    } catch (error) {
      console.error('Failed to fetch plugin output variables:', error)
      pluginOutputVariables.value = []
    } finally {
      pluginOutputLoading.value = false
    }
  }

  /**
   * Fetch system variables from API
   */
  const fetchSystemVariables = async () => {
    if (systemVariables.value.length > 0) {
      return // Already loaded
    }

    systemVariablesLoading.value = true
    try {
      systemVariables.value = await getSystemVariables()
    } catch (error) {
      console.error('Failed to fetch system variables:', error)
      systemVariables.value = []
    } finally {
      systemVariablesLoading.value = false
    }
  }

  return {
    // Flow variables
    variables,
    existingIds,
    addVariable,
    updateVariable,
    removeVariable,
    updateParams,
    syncVariablesToFlowModel,

    // Plugin output variables
    pluginOutputVariables,
    atomsOutputMap,
    pluginOutputLoading,
    fetchPluginOutputVariables,

    // System variables
    systemVariables,
    systemVariablesLoading,
    fetchSystemVariables,
  }
}
