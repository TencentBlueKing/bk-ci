import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import { useRoute } from 'vue-router'
import {
  getFlowGroups,
  getFlowGroupsCount,
  createFlowGroup as apiCreateFlowGroup,
  deleteFlowGroup as apiDeleteFlowGroup,
  renameFlowGroup as apiRenameFlowGroup,
  pinFlowGroup as apiPinFlowGroup,
  type FlowGroupItem,
  type EditGroupParams,
  type FlowGroupCounts,
} from '../api/flowGroup'
import { FLOW_GROUP_TYPES } from '../constants/flowGroup'

export const useFlowGroupStore = defineStore('flowGroup', () => {
  const route = useRoute()
  const projectId = computed(() => route.params.projectId as string)
  // 数量统计
  const counts = ref<FlowGroupCounts>({
    totalCount: 0,
    myFavoriteCount: 0,
    myPipelineCount: 0,
    recycleCount: 0,
    recentUseCount: 0,
  })

  // 所有创作流组列表（包含个人组和项目组，已做前置处理）
  const flowGroups = ref<FlowGroupItem[]>([])

  // 加载状态
  const loading = ref(false)

  // 计算属性：个人创作流组列表（projected 为 false 或 undefined）
  const personalFlowGroups = computed(() => {
    return flowGroups.value.filter((group) => !group.projected)
  })

  // 计算属性：项目创作流组列表（projected 为 true）
  const projectFlowGroups = computed(() => {
    return flowGroups.value.filter((group) => group.projected === true)
  })

  /**
   * 前置数据处理：为组添加图标等处理
   */
  function processFlowGroup(group: FlowGroupItem): FlowGroupItem {
    return {
      ...group,
      // 如果 id 是 unclassified，使用 un-group 图标
      icon: group.id === FLOW_GROUP_TYPES.UNCLASSIFIED_FLOWS ? 'un-group' : group.icon || 'group',
      // 默认显示操作按钮
      showAction:
        group.id === FLOW_GROUP_TYPES.UNCLASSIFIED_FLOWS
          ? false
          : group.showAction !== undefined
            ? group.showAction
            : true,
    }
  }

  /**
   * 加载所有数据（调用 API 并做前置处理）
   */
  async function loadAllData() {
    loading.value = true
    try {
      // 并发加载数据
      const [groups, countsData] = await Promise.all([
        getFlowGroups(projectId.value),
        getFlowGroupsCount(projectId.value),
      ])

      // 前置数据处理：为每个组添加图标等
      // flowGroups.value = groups.map(processFlowGroup);

      // TODO: 一期暂时屏蔽分组，只保留未分组数据
      flowGroups.value = groups
        .slice(0, 1)
        .filter((group): group is FlowGroupItem => group !== undefined)
        .map(processFlowGroup)
      counts.value = countsData
    } catch (error) {
      console.error('Failed to load flow group data:', error)
      throw error
    } finally {
      loading.value = false
    }
  }

  /**
   * 创建创作流组
   */
  async function createFlowGroup(params: EditGroupParams) {
    try {
      const newGroup = await apiCreateFlowGroup(projectId.value, params)
      return newGroup
    } catch (error) {
      console.error('Failed to create flow group:', error)
      throw error
    }
  }

  /**
   * 删除创作流组
   */
  async function removeFlowGroup(id: string) {
    try {
      const res = await apiDeleteFlowGroup(projectId.value, id)
      if (res) {
        loadAllData()
      }
    } catch (error) {
      console.error('Failed to remove flow group:', error)
      throw error
    }
  }

  /**
   * 重命名创作流组
   */
  async function renameFlowGroup(params: EditGroupParams) {
    try {
      const { id, ...otherParams } = params
      const updatedGroup = await apiRenameFlowGroup(projectId.value, id as string, otherParams)
      return updatedGroup
    } catch (error) {
      console.error('Failed to rename flow group:', error)
      throw error
    }
  }

  /**
   * 置顶/取消置顶创作流组
   */
  async function pinFlowGroup(id: string, enabled: boolean) {
    try {
      const index = flowGroups.value.findIndex((g) => g.id === id)
      const group = flowGroups.value[index]
      if (!group) {
        throw new Error('Group not found')
      }
      const updatedGroup = await apiPinFlowGroup(projectId.value, id, enabled)
      return updatedGroup
    } catch (error) {
      console.error('Failed to pin flow group:', error)
      throw error
    }
  }

  /**
   * 刷新数据
   */
  async function refresh() {
    await loadAllData()
  }

  /**
   * 重置数据
   */
  function reset() {
    counts.value = {
      totalCount: 0,
      myFavoriteCount: 0,
      myPipelineCount: 0,
      recycleCount: 0,
      recentUseCount: 0,
    }
    flowGroups.value = []
    loading.value = false
  }

  return {
    // State
    counts,
    flowGroups,
    loading,
    // Computed
    personalFlowGroups,
    projectFlowGroups,
    // Actions
    loadAllData,
    createFlowGroup,
    removeFlowGroup,
    renameFlowGroup,
    pinFlowGroup,
    refresh,
    reset,
  }
})
