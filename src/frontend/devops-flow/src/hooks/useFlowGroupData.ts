import { computed } from 'vue'
import { storeToRefs } from 'pinia'
import { useFlowGroupStore } from '../stores/flowGroup'
import { FLOW_GROUP_TYPES } from '../constants/flowGroup'
import { useI18n } from 'vue-i18n'

/**
 * 组件数据 Hook
 * 从 store 获取数据，进行二次加工，提供给组件使用
 */
export function useFlowGroupData() {
  const store = useFlowGroupStore()
  const { t } = useI18n()

  // 使用 storeToRefs 确保响应式
  const { counts, flowGroups, loading, personalFlowGroups, projectFlowGroups } = storeToRefs(store)

  // 从 store 获取原始数据

  /**
   * 我的创作流组总数（组的数量）
   * 收藏(1) + 我创建的(1) + 个人组的数量
   */
  const myFlowGroupsTotal = computed(() => {
    return 2 + personalFlowGroups.value.length
  })

  /**
   * 项目创作流组总数（组的数量）
   */
  const projectFlowGroupsTotal = computed(() => {
    return projectFlowGroups.value.length
  })

  /**
   * 我的创作流菜单项列表
   * 包含：收藏、我创建的、所有个人组
   */
  const myFlowGroupMenuItems = computed(() => {
    return [
      {
        id: FLOW_GROUP_TYPES.MY_CREATED,
        icon: 'user',
        name: t('flow.sidebar.myCreated'),
        pipelineCount: counts.value.myPipelineCount,
        showAction: false,
      },
      {
        id: FLOW_GROUP_TYPES.MY_FAVORITES,
        icon: 'star',
        name: t('flow.sidebar.myFavorites'),
        pipelineCount: counts.value.myFavoriteCount,
        showAction: false,
      },
      ...personalFlowGroups.value,
    ]
  })

  return {
    // 原始数据（使用 storeToRefs 确保响应式）
    counts,
    flowGroups,
    personalFlowGroups,
    projectFlowGroups,
    loading,

    // 二次加工的数据
    myFlowGroupsTotal,
    projectFlowGroupsTotal,
    myFlowGroupMenuItems,

    // 操作方法（直接暴露 store 的方法）
    createFlowGroup: store.createFlowGroup,
    removeFlowGroup: store.removeFlowGroup,
    renameFlowGroup: store.renameFlowGroup,
    pinFlowGroup: store.pinFlowGroup,
    loadAllData: store.loadAllData,
  }
}
