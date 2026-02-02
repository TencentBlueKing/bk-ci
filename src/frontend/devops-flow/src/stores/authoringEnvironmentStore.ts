import {
  fetchAuthoringEnvList as apiFetchEnvList,
  fetchAuthoringNodeList as apiFetchNodeList,
  convertToCreationNode,
  type AuthoringEnvItem,
  type AuthoringNodeItem,
  type CreationNode
} from '@/api/authoringEnvironmentApi'
import { Message } from 'bkui-vue'
import { defineStore } from 'pinia'
import { computed, ref } from 'vue'

/**
 * ====================================
 * Authoring Environment Store
 * ====================================
 * 
 * This store centralizes all state management for authoring environments.
 * Including: environment list, node list, loading states, etc.
 */

export const useAuthoringEnvironmentStore = defineStore('authoringEnvironment', () => {
  // ============ State ============
  
  /**
   * List of authoring environments from API
   */
  const envList = ref<AuthoringEnvItem[]>([])
  
  /**
   * List of authoring nodes from API
   */
  const nodeList = ref<AuthoringNodeItem[]>([])
  
  /**
   * Current selected environment name
   */
  const selectedEnvHashId = ref<string>('')
  
  /**
   * Loading state for environment list
   */
  const envListLoading = ref(false)
  
  /**
   * Loading state for node list
   */
  const nodeListLoading = ref(false)
  
  /**
   * Error message
   */
  const errorMessage = ref<string>('')
  
  /**
   * Node list converted to CreationNode format
   */
  const creationNodes = computed<CreationNode[]>(() => {
    return nodeList.value.map(convertToCreationNode)
  })
  
  /**
   * Combined loading state
   */
  const isLoading = computed(() => envListLoading.value || nodeListLoading.value)
  
  /**
   * Check if environment list is empty
   */
  const isEmpty = computed(() => envList.value.length === 0)
  
  /**
   * Check if node list is empty
   */
  const isNodeListEmpty = computed(() => nodeList.value.length === 0)

  // ============ Actions ============
  
  /**
   * Load authoring environment list
   * @param projectId - Project ID
   * @param envType - Environment type (default: 'CREATE')
   */
  async function loadEnvList(projectId: string, envType: string = 'CREATE'): Promise<void> {
    if (!projectId) {
      console.warn('Project ID is required to load environment list')
      return
    }
    
    try {
      envListLoading.value = true
      errorMessage.value = ''
      const result = await apiFetchEnvList({ projectId, envType })
      envList.value = result
    } catch (error: any) {
      console.error('Failed to load environment list:', error)
      errorMessage.value = 'Failed to load environment list'
      envList.value = []
      Message({  theme: 'error', message: error.message || error })
    } finally {
      envListLoading.value = false
    }
  }
  
  /**
   * Load authoring node list by environment name
   * @param projectId - Project ID
   * @param envHashId - Environment hash ID
   */
  async function loadNodeList(projectId: string, envHashId: string): Promise<void> {
    if (!projectId || !envHashId) {
      console.warn('Project ID and environment name are required to load node list')
      nodeList.value = []
      return
    }
    
    try {
      nodeListLoading.value = true
      errorMessage.value = ''
      
      const result = await apiFetchNodeList({ projectId, envHashId })
      nodeList.value = result.records || []
    } catch (error: any) {
      console.error('Failed to load node list:', error)
      errorMessage.value = 'Failed to load node list'
      nodeList.value = []
      Message({  theme: 'error', message: error.message || error })
    } finally {
      nodeListLoading.value = false
    }
  }
  
  /**
   * Refresh environment list
   * @param projectId - Project ID
   * @param envType - Environment type (default: 'CREATE')
   */
  async function refreshEnvList(projectId: string, envType: string = 'CREATE'): Promise<void> {
    await loadEnvList(projectId, envType)
  }
  
  /**
   * Refresh node list for current selected environment
   * @param projectId - Project ID
   */
  async function refreshNodeList(projectId: string): Promise<void> {
    if (!selectedEnvHashId.value) return
    await loadNodeList(projectId, selectedEnvHashId.value)
  }
  
  /**
   * Reset store state
   */
  function resetState(): void {
    envList.value = []
    nodeList.value = []
    selectedEnvHashId.value = ''
    errorMessage.value = ''
    envListLoading.value = false
    nodeListLoading.value = false
  }
  
  /**
   * Clear node list
   */
  function clearNodeList(): void {
    nodeList.value = []
  }
  
  /**
   * Set error message
   * @param message - Error message
   */
  function setError(message: string): void {
    errorMessage.value = message
  }
  
  /**
   * Clear error message
   */
  function clearError(): void {
    errorMessage.value = ''
  }

  return {
    // State
    envList,
    nodeList,
    envListLoading,
    nodeListLoading,
    errorMessage,
    
    // Computed
    creationNodes,
    isLoading,
    isEmpty,
    isNodeListEmpty,
    
    // Actions
    loadEnvList,
    loadNodeList,
    refreshEnvList,
    refreshNodeList,
    resetState,
    clearNodeList,
    setError,
    clearError,
  }
})
