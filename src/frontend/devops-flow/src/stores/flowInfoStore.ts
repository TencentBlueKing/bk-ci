import * as apiFlowInfo from '@/api/flowInfo'
import type { FlowInfo, FlowVersion } from '@/types/flow'
import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { useRoute } from 'vue-router'

export const useFlowInfoStore = defineStore('flowInfo', () => {
  const route = useRoute()
  const projectId = computed(() => route.params.projectId as string)
  const flowId = computed(() => route.params.flowId as string)
  const flowVersionList = ref<FlowVersion[] | null>([])

  const flowInfo = ref<FlowInfo | null>(null)
  const loading = ref<boolean>(false)

  async function getFlowInfo() {
    loading.value = true
    try {
      const res = await apiFlowInfo.fetchFlowInfo({
        projectId: projectId.value,
        flowId: flowId.value,
      })
      flowInfo.value = res
    } catch (error) {
      console.error('Failed to get flow info:', error)
      throw error
    } finally {
      loading.value = false
    }
  }
  async function getFlowVersionList() {
    loading.value = true
    try {
      const { records } = await apiFlowInfo.getFlowVersionList({
        projectId: projectId.value,
        flowId: flowId.value,
      })
      flowVersionList.value = records
    } finally {
      loading.value = false
    }
  }

  /**
   * Fetch paginated version list (for version history side slider and dropdown scroll-load)
   */
  async function fetchPaginatedVersionList(params: {
    page: number
    pageSize: number
    versionName?: string
  }) {
    const res = await apiFlowInfo.getFlowVersionList({
      projectId: projectId.value,
      flowId: flowId.value,
      ...params,
    })
    return res
  }

  function reset() {
    flowInfo.value = null
    flowVersionList.value = []
    loading.value = false
  }

  function initFlowInfo() {
    getFlowInfo()
    getFlowVersionList()
  }

  return {
    flowInfo,
    flowVersionList,
    loading,

    getFlowInfo,
    getFlowVersionList,
    fetchPaginatedVersionList,
    initFlowInfo,
    reset   
  }
})
