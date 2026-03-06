import { ref } from 'vue'
import { defineStore } from 'pinia'

/**
 * UI 状态管理 Store
 * 管理全局 UI 状态，如侧边栏、变量面板等
 */
export const useUIStore = defineStore('ui', () => {
  // 变量面板展开状态
  const isVariablePanelOpen = ref(true)

  // 当前项目 ID（用于检测项目切换）
  const currentProjectId = ref<string | null>(null)

  /**
   * 设置变量面板状态
   */
  function setVariablePanelOpen(isOpen: boolean) {
    isVariablePanelOpen.value = isOpen
  }

  /**
   * 切换变量面板状态
   */
  function toggleVariablePanel() {
    isVariablePanelOpen.value = !isVariablePanelOpen.value
  }

  /**
   * 设置当前项目 ID
   */
  function setCurrentProjectId(projectId: string | null) {
    currentProjectId.value = projectId
  }

  return {
    isVariablePanelOpen,
    setVariablePanelOpen,
    toggleVariablePanel,
    currentProjectId,
    setCurrentProjectId,
  }
})
