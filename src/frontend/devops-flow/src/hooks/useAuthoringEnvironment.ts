import type {
  AuthoringEnvironment,
  AuthoringEnvItem,
  AuthoringNodeItem,
  CreationNode,
  EnvSelectItem,
} from '@/api/authoringEnvironmentApi'
import { useAuthoringEnvironmentStore } from '@/stores/authoringEnvironmentStore'
import { storeToRefs } from 'pinia'
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRoute } from 'vue-router'

/**
 * ====================================
 * useAuthoringEnvironment Hook
 * ====================================
 *
 * A unified hook for components to interact with authoring environment functionality.
 * Provides a simple and consistent API for accessing and managing authoring environments.
 */

// ============ Type Exports ============

export type {
  AuthoringEnvironment,
  AuthoringEnvItem,
  AuthoringNodeItem,
  CreationNode,
  EnvSelectItem,
}

// ============ Hook Options ============

export interface UseAuthoringEnvironmentOptions {
  /**
   * Project ID (optional, will use route.params.projectId if not provided)
   */
  projectId?: string

  /**
   * Flow ID (optional, will use route.params.flowId if not provided)
   */
  flowId?: string

  /**
   * Auto load environment list on mount
   * @default false
   */
  autoLoadEnvList?: boolean

  /**
   * Environment type for filtering
   * @default 'CREATE'
   */
  envType?: string

  /**
   * Auto load node list when environment changes
   * @default true
   */
  autoLoadNodes?: boolean

  /**
   * Reset store state on unmount
   * @default false
   */
  resetOnUnmount?: boolean

  /**
   * Initial environment name
   * @default ''
   */
  envHashId?: string
}

// ============ Main Hook ============

/**
 * Hook for managing authoring environment functionality
 * @param options - Hook configuration options
 * @returns Authoring environment state and methods
 */
export function useAuthoringEnvironment(options: UseAuthoringEnvironmentOptions = {}) {
  const route = useRoute()
  const store = useAuthoringEnvironmentStore()

  // ============ Options with defaults ============

  const {
    projectId: optionProjectId,
    flowId: optionFlowId,
    autoLoadEnvList = false,
    envType = 'CREATE',
    resetOnUnmount = false,
  } = options

  // ============ Computed Values ============

  /**
   * Get project ID from options or route
   */
  const projectId = computed(() => {
    return optionProjectId || (route.params.projectId as string) || ''
  })

  /**
   * Get flow ID from options or route
   */
  const flowId = computed(() => {
    return optionFlowId || (route.params.flowId as string) || ''
  })

  // ============ Store Refs ============

  const {
    envList,
    nodeList,
    envListLoading,
    nodeListLoading,
    errorMessage,
    creationNodes,
    isLoading,
    isEmpty,
    isNodeListEmpty,
  } = storeToRefs(store)

  const envSelectList = computed<EnvSelectItem[]>(() => {
    return envList.value.map((env) => ({
      ...env,
      value: env.envHashId,
      label: env.name,
    }))
  })

  // ============ Methods ============

  /**
   * Load environment list
   * @param customEnvType - Optional custom environment type
   */
  const loadEnvList = async (customEnvType?: string): Promise<void> => {
    await store.loadEnvList(projectId.value, customEnvType || envType)
  }

  /**
   * Load node list for specified environment
   * @param envHashId - Environment envHashId
   */
  const loadNodeList = async (envHashId: string): Promise<void> => {
    await store.loadNodeList(projectId.value, envHashId)
  }

  /**
   * Refresh environment list
   */
  const refreshEnvList = async (): Promise<void> => {
    await store.refreshEnvList(projectId.value, envType)
  }

  /**
   * Refresh node list for current selected environment
   */
  const refreshNodeList = async (): Promise<void> => {
    await store.refreshNodeList(projectId.value)
  }

  /**
   * Get environment by hash ID
   * @param envHashId - Environment hash ID
   * @returns Environment item or undefined
   */
  const getEnvByHashId = (envHashId: string): AuthoringEnvItem | undefined => {
    return envList.value.find((env) => env.envHashId === envHashId)
  }

  /**
   * Reset all state
   */
  const resetState = (): void => {
    store.resetState()
  }

  // ============ Lifecycle ============

  onMounted(async () => {
    // Auto load environment list if enabled
    if (autoLoadEnvList && projectId.value) {
      loadEnvList()
    }
  })

  onUnmounted(() => {
    // Reset state on unmount if enabled
    if (resetOnUnmount) {
      resetState()
    }
  })

  function goEnvironment(envHashId?: string) {
    let url = `${location.origin}/console/environment/${route.params.projectId}`
    if (envHashId) {
      url += `/creative/env/ALL/${envHashId}/node`
    }
    window.open(url, '_blank')
  }

  // ============ Return ============

  return {
    // Computed identifiers
    projectId,
    flowId,

    // State refs
    envList,
    nodeList,
    envListLoading,
    nodeListLoading,
    errorMessage,

    // Computed state
    envSelectList,
    creationNodes,
    isLoading,
    isEmpty,
    isNodeListEmpty,

    // Methods
    loadEnvList,
    loadNodeList,
    refreshEnvList,
    refreshNodeList,
    getEnvByHashId,
    resetState,
    goEnvironment,
  }
}

// ============ Default Export ============

export default useAuthoringEnvironment
