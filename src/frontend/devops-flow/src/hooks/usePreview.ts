import type { AuthoringNodeItem, StartupProperty } from '@/api/preview'
import { ROUTE_NAMES } from '@/constants/routes'
import { usePreviewStore } from '@/stores'
import { Message } from 'bkui-vue'
import type { ComputedRef, Ref } from 'vue'
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'

// ============================================
// 1. Type Definitions
// ============================================

/** Param type for form handling */
export type ParamType = 'params' | 'versionParam' | 'build' | 'constant' | 'other'

/** Section IDs for collapsible sections */
export type SectionId = 1 | 2 | 3 | 4 | 5

/** Atoms count result */
interface AtomsCount {
  selected: number
  total: number
}

/** UsePreview options */
interface UsePreviewOptions {
  /** Auto load data on mount */
  autoLoad?: boolean
  /** Default expanded sections */
  defaultExpandedSections?: SectionId[]
}

/** UsePreview return type */
interface UsePreviewReturn {
  // Route params
  projectId: ComputedRef<string>
  flowId: ComputedRef<string>
  version: ComputedRef<number | undefined>
  isDebugMode: ComputedRef<boolean>
  canExecute: ComputedRef<boolean>
  flowName: ComputedRef<string>

  // UI State
  activeSections: Ref<Set<SectionId>>
  checkAll: Ref<boolean>
  selectedNode: Ref<string>
  runMessage: Ref<string>
  
  // Validation state
  invalidParams: Ref<Set<string>>
  isNodeInvalid: Ref<boolean>

  // Authoring nodes
  authoringNodes: ComputedRef<AuthoringNodeItem[]>
  authoringNodesLoading: ComputedRef<boolean>

  // Grouped params (computed)
  groupedParams: ComputedRef<Record<string, StartupProperty[]>>
  groupedConstants: ComputedRef<Record<string, StartupProperty[]>>
  groupedOtherParams: ComputedRef<Record<string, StartupProperty[]>>
  hasGroupedParams: ComputedRef<boolean>
  hasGroupedConstants: ComputedRef<boolean>
  hasGroupedOtherParams: ComputedRef<boolean>

  // Atoms count (computed)
  selectedAtomsCount: ComputedRef<AtomsCount>

  // Store getters
  store: ReturnType<typeof usePreviewStore>

  // Actions
  toggleSection: (id: SectionId) => void
  isSectionExpanded: (id: SectionId) => boolean
  handleParamChange: (type: ParamType, key: string, value: unknown) => void
  handleBuildNoChange: (key: string, value: unknown) => void
  handleCheckAllChange: (checked: boolean) => void
  handlePipelineChange: (newPipeline: unknown) => void
  handleResetDefault: (e?: Event) => void
  handleSaveCurrentParams: (e?: Event) => void
  handleVersionChange: (newVersion: number) => void
  handleExecute: () => Promise<void>
  loadData: (ver?: number) => Promise<void>
  loadAuthoringNodes: (envHashId: string) => Promise<void>
}

// ============================================
// 2. Pure Utility Functions
// ============================================

/**
 * Group params by category field (pure function)
 */
export const groupParamsByCategory = (
  list: StartupProperty[],
  notGroupedKey = '未分组'
): Record<string, StartupProperty[]> => {
  if (!list.length) return {}
  
  return list.reduce<Record<string, StartupProperty[]>>((acc, item) => {
    const categoryKey = item.category || notGroupedKey
    return {
      ...acc,
      [categoryKey]: [...(acc[categoryKey] || []), item]
    }
  }, {})
}

/**
 * Calculate selected atoms count from pipeline model (pure function)
 */
export const calculateAtomsCount = (stages: unknown[] | undefined): AtomsCount => {
  if (!stages) return { selected: 0, total: 0 }

  return stages.reduce<AtomsCount>((acc, stage: any) => {
    const containers = stage.containers || []
    const stageCount = containers.reduce((containerAcc: AtomsCount, container: any) => {
      const elements = container.elements || []
      const elementCount = elements.reduce((elemAcc: AtomsCount, element: any) => ({
        total: elemAcc.total + 1,
        selected: elemAcc.selected + (element.canElementSkip !== false ? 1 : 0)
      }), { selected: 0, total: 0 })
      
      return {
        total: containerAcc.total + elementCount.total,
        selected: containerAcc.selected + elementCount.selected
      }
    }, { selected: 0, total: 0 })

    return {
      total: acc.total + stageCount.total,
      selected: acc.selected + stageCount.selected
    }
  }, { selected: 0, total: 0 })
}

/**
 * Create immutable set toggle (pure function)
 */
export const toggleSetItem = <T>(set: Set<T>, item: T): Set<T> => {
  const newSet = new Set(set)
  if (newSet.has(item)) {
    newSet.delete(item)
  } else {
    newSet.add(item)
  }
  return newSet
}

// ============================================
// 3. Composable Definition
// ============================================

/**
 * Preview composable hook
 * Encapsulates all preview business logic with immutable data flow
 */
export const usePreview = (options: UsePreviewOptions = {}): UsePreviewReturn => {
  const {
    autoLoad = true,
    defaultExpandedSections = [1, 2, 3, 4, 5]
  } = options

  // ----------------------------------------
  // 3.1 Dependencies
  // ----------------------------------------
  const { t } = useI18n()
  const route = useRoute()
  const router = useRouter()
  const store = usePreviewStore()

  // ----------------------------------------
  // 3.2 Route Params (Computed)
  // ----------------------------------------
  const projectId = computed(() => route.params.projectId as string)
  const flowId = computed(() => route.params.flowId as string)
  const version = computed(() => {
    const v = route.params.version
    return v ? Number(v) : undefined
  })
  const isDebugMode = computed(() => 
    Object.prototype.hasOwnProperty.call(route.query, 'debug')
  )

  const canExecute = computed(() =>
    store.atomicFlowInfo.value?.permissions?.canExecute ?? true
  )
  const flowName = computed(() =>
    store.atomicPipelineModel.value?.modelAndSetting.model.name as string ?? '--'
  )

  // ----------------------------------------
  // 3.3 Local UI State
  // ----------------------------------------
  const activeSections = ref<Set<SectionId>>(new Set(defaultExpandedSections as SectionId[]))
  const checkAll = ref(true)
  const selectedNode = ref()
  const runMessage = ref('')
  
  // Validation state - track invalid params for highlighting
  const invalidParams = ref<Set<string>>(new Set())
  const isNodeInvalid = ref(false)

  // ----------------------------------------
  // 3.4 Computed Data Layer (Derived from Store)
  // ----------------------------------------

  /** Authoring nodes list */
  const authoringNodes = computed(() => store.authoringNodes)

  /** Authoring nodes loading state */
  const authoringNodesLoading = computed(() => store.authoringNodesLoading)

  const groupedParams = computed(() => 
    groupParamsByCategory(store.paramList, '未分组入参')
  )

  const groupedConstants = computed(() => 
    groupParamsByCategory(store.constantParams, '未分组常量')
  )

  const groupedOtherParams = computed(() => 
    groupParamsByCategory(store.otherParams, '未分组变量')
  )

  const hasGroupedParams = computed(() => 
    Object.keys(groupedParams.value).length > 0
  )

  const hasGroupedConstants = computed(() => 
    Object.keys(groupedConstants.value).length > 0
  )

  const hasGroupedOtherParams = computed(() => 
    Object.keys(groupedOtherParams.value).length > 0
  )

  const selectedAtomsCount = computed(() => 
    calculateAtomsCount(store.pipelineModel?.stages)
  )

  // ----------------------------------------
  // 3.5 UI Actions (Pure, No Side Effects)
  // ----------------------------------------
  
  /**
   * Toggle section collapse state (immutable)
   */
  const toggleSection = (id: SectionId): void => {
    activeSections.value = toggleSetItem(activeSections.value, id)
  }

  /**
   * Check if section is expanded
   */
  const isSectionExpanded = (id: SectionId): boolean => {
    return activeSections.value.has(id)
  }

  // ----------------------------------------
  // 3.6 Store Actions (Delegated to Store)
  // ----------------------------------------
  
  /**
   * Handle param value change
   */
  const handleParamChange = (type: ParamType, key: string, value: unknown): void => {
    store.updateParamValue(type, key, value)
    // Clear validation error when param is changed
    if (invalidParams.value.has(key)) {
      const newInvalidParams = new Set(invalidParams.value)
      newInvalidParams.delete(key)
      invalidParams.value = newInvalidParams
    }
  }

  /**
   * Handle buildNo value change
   */
  const handleBuildNoChange = (key: string, value: unknown): void => {
    store.updateBuildNo(key, value)
  }

  /**
   * Handle check all change for element skip
   */
  const handleCheckAllChange = (checked: boolean): void => {
    checkAll.value = checked
    store.setPipelineSkipProp(checked)
  }

  /**
   * Handle pipeline change from BkPipeline component
   */
  const handlePipelineChange = (newPipeline: unknown): void => {
    store.updatePipelineFromChange(newPipeline as any)
    // Update checkAll state based on new pipeline state
    const count = calculateAtomsCount((newPipeline as any)?.stages)
    checkAll.value = count.total > 0 && count.selected === count.total
  }

  /**
   * Handle reset to default params
   */
  const handleResetDefault = (e?: Event): void => {
    e?.stopPropagation()
    
    // Reset each param to its default value (immutable updates via store)
    store.paramList.forEach(param => {
      store.updateParamValue('params', param.id, param.defaultValue)
    })
    
    Message({
      theme: 'success',
      message: t('flow.preview.resetSuccess'),
    })
  }

  /**
   * Handle save current params
   */
  const handleSaveCurrentParams = (e?: Event): void => {
    e?.stopPropagation()
    // TODO: Implement save current params API
    Message({
      theme: 'success',
      message: t('flow.common.success'),
    })
  }

  /**
   * Handle version change
   */
  const handleVersionChange = (newVersion: number): void => {
    loadData(newVersion)
  }

  // ----------------------------------------
  // 3.7 Data Fetching (Async Actions)
  // ----------------------------------------
  
  /**
   * Load preview data from API
   */
  const loadData = async (ver?: number): Promise<void> => {
    try {
      await store.loadPreviewData({
        projectId: projectId.value,
        flowId: flowId.value,
        version: ver ?? version.value,
      })

      // Set pipeline skip props after loading
      store.setPipelineSkipProp(checkAll.value)

      // Check if can manual startup
      if (!store.startupInfo?.canManualStartup) {
        Message({
          theme: 'error',
          message: t('flow.preview.cannotManualStartup'),
        })
      }
      
      await loadAuthoringNodes(store.atomicPipelineModel.value?.modelAndSetting?.setting?.envHashId ?? '')
    } catch (error: unknown) {
      console.error('Failed to load preview data:', error)
      Message({
        theme: 'error',
        message: (error as Error)?.message || t('flow.preview.loadFailed'),
      })
      router.back()
    }
  }

  /**
   * Load authoring nodes by envHashId
   */
  const loadAuthoringNodes = async (envHashId: string): Promise<void> => {
    try {
     
      await store.loadAuthoringNodes({
        projectId: projectId.value,
        envHashId,
      })

      // Set first available node as default selected
      const availableNodes = store.authoringNodes.filter(
        (node: AuthoringNodeItem) => node.agentStatus && node.envEnableNode
      )
      if (availableNodes.length > 0 && !selectedNode.value) {
        selectedNode.value = availableNodes[0]!.agentHashId
      }
    } catch (error: unknown) {
      console.error('Failed to load authoring nodes:', error)
      // Don't show error message as this is non-critical
    }
  }

  /**
   * Validate required params before execution
   * Returns empty array if valid, otherwise returns list of empty required param ids
   */
  const validateRequiredParams = (): string[] => {
    const emptyRequiredParams: string[] = []
    
    // Check params (only valueNotEmpty params are required during execution)
    store.paramList.forEach(param => {
      if (param.valueNotEmpty) {
        const value = store.paramsValues[param.id]
        // Check if value is empty (undefined, null, empty string)
        if (value === undefined || value === null || value === '') {
          emptyRequiredParams.push(param.id)
        }
      }
    })
    
    // Check version params if visible
    if (store.isVisibleVersion) {
      store.versionParamList.forEach(param => {
        if (param.valueNotEmpty) {
          const value = store.versionParamValues[param.id]
          if (value === undefined || value === null || value === '') {
            emptyRequiredParams.push(param.id)
          }
        }
      })
    }
    
    return emptyRequiredParams
  }

  /**
   * Execute the pipeline
   */
  const handleExecute = async (): Promise<void> => {
    // Reset validation state
    isNodeInvalid.value = false
    invalidParams.value = new Set()

    // Validate creation node is selected
    if (!selectedNode.value) {
      isNodeInvalid.value = true
      // Expand runtime info section to show the error
      if (!activeSections.value.has(1)) {
        activeSections.value = new Set([...activeSections.value, 1])
      }
      Message({
        theme: 'error',
        message: t('flow.preview.creationNodeRequired'),
      })
      return
    }

    // Validate required params are not empty
    const emptyRequiredParams = validateRequiredParams()
    if (emptyRequiredParams.length > 0) {
      // Update invalid params for highlighting
      invalidParams.value = new Set(emptyRequiredParams)
      // Expand input params section to show errors
      if (!activeSections.value.has(2)) {
        activeSections.value = new Set([...activeSections.value, 2])
      }
      Message({
        theme: 'error',
        message: t('flow.preview.requiredParamsEmpty', { params: emptyRequiredParams.join(', ') }),
      })
      return
    }

    try {
      const skipAtoms = store.canElementSkip ? store.getSkippedAtoms() : {}
      
      // Get pipelineId from flowInfo, fallback to flowId if not available
      const pipelineId = store.flowInfo?.pipelineId || flowId.value
      
      const result = await store.executePipeline({
        projectId: projectId.value,
        pipelineId,
        version: isDebugMode.value ? store.flowInfo?.version : version.value,
        skipAtoms,
        remark: runMessage.value,
        resourceHashId: selectedNode.value,
      })

      if (result?.id) {
        Message({
          theme: 'success',
          message: t('flow.preview.executeSuccess'),
        })

        // Navigate to execution detail
        router.push({
          name: ROUTE_NAMES.FLOW_DETAIL_EXECUTION_DETAIL_TAB,
          params: {
            projectId: projectId.value,
            flowId: flowId.value,
            buildNo: result.id,
          },
        })
      } else {
        Message({
          theme: 'error',
          message: t('flow.preview.executeFailed'),
        })
      }
    } catch (error: unknown) {
      console.error('Failed to execute pipeline:', error)
      Message({
        theme: 'error',
        message: (error as Error)?.message || t('flow.preview.executeFailed'),
      })
    }
  }

  // ----------------------------------------
  // 3.8 Lifecycle & Watchers
  // ----------------------------------------
  
  // Watch version change to reload data
  watch(version, () => {
    if (autoLoad) {
      loadData()
    }
  })

  // Watch selectedNode to clear validation error when node is selected
  watch(selectedNode, (newValue) => {
    if (newValue && isNodeInvalid.value) {
      isNodeInvalid.value = false
    }
  })

  // Load data on mount
  onMounted(() => {
    if (autoLoad) {
      loadData()
    }
  })

  // Clean up on unmount
  onBeforeUnmount(() => {
    store.$reset()
  })

  // ----------------------------------------
  // 3.9 Return Public API
  // ----------------------------------------
  return {
    // Route params
    projectId, 
    flowId,
    version,
    isDebugMode,
    flowName,
    canExecute,

    // UI State
    activeSections,
    checkAll,
    selectedNode,
    runMessage,
    
    // Validation state
    invalidParams,
    isNodeInvalid,

    // Authoring nodes
    authoringNodes,
    authoringNodesLoading,

    // Grouped params
    groupedParams,
    groupedConstants,
    groupedOtherParams,
    hasGroupedParams,
    hasGroupedConstants,
    hasGroupedOtherParams,

    // Atoms count
    selectedAtomsCount,

    // Store access
    store,

    // Actions
    toggleSection,
    isSectionExpanded,
    handleParamChange,
    handleBuildNoChange,
    handleCheckAllChange,
    handlePipelineChange,
    handleResetDefault,
    handleSaveCurrentParams,
    handleVersionChange,
    handleExecute,
    loadData,
    loadAuthoringNodes,
  }
}

// Export types
export type { AtomsCount, UsePreviewOptions, UsePreviewReturn }

