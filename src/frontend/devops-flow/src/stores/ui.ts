import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import {
  getAuthoringBaseOS,
  getCurrentProject,
  getWindowProjectList,
  isPersonalProject as checkPersonalProject,
  type ProjectMeta,
} from '@/utils/project'

/**
 * UI 状态管理 Store
 * 管理全局 UI 状态，如侧边栏、变量面板等
 */
export const useUIStore = defineStore('ui', () => {
  // 变量面板展开状态
  const isVariablePanelOpen = ref(true)

  // 当前项目 ID（用于检测项目切换）
  const currentProjectId = ref<string | null>(null)

  // 父应用同步过来的项目列表，用于判断当前项目类型
  const projectList = ref<ProjectMeta[]>(getWindowProjectList())

  const currentProject = computed(() => getCurrentProject(projectList.value, currentProjectId.value))

  const isPersonalProject = computed(() => checkPersonalProject(currentProject.value))

  const authoringBaseOS = computed(() => getAuthoringBaseOS(currentProject.value))

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

  /**
   * 设置父应用同步的项目列表
   */
  function setProjectList(list: unknown) {
    projectList.value = Array.isArray(list) ? (list as ProjectMeta[]) : []
  }

  return {
    isVariablePanelOpen,
    setVariablePanelOpen,
    toggleVariablePanel,
    currentProjectId,
    setCurrentProjectId,
    projectList,
    currentProject,
    isPersonalProject,
    authoringBaseOS,
    setProjectList,
  }
})
