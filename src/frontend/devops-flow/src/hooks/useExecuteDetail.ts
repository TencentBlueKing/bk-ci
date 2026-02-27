import { useExecuteDetailStore } from '@/stores/executeDetail'
import { storeToRefs } from 'pinia'
import { computed } from 'vue'
import { isRunning } from '../utils/flowStatus'

export type ExecuteInfo = {
  id: string
  name: string
  latestBuildNum: number
  currentBuildNum: number
}
/**
 * NewFlowPopup组件业务逻辑 Hook
 */
export function useExecuteDetail() {
  const store = useExecuteDetailStore()
  const { loading, executeDetail, flowInfo } = storeToRefs(store)

  // 计算执行详情的相关状态
  const isRunningOrNot = computed(() => isRunning(executeDetail.value?.status))

  const isLatestBuild = computed(
    () =>
      executeDetail.value?.buildNum === executeDetail.value?.latestBuildNum &&
      executeDetail.value?.curVersion === executeDetail.value?.latestVersion,
  )

  // 使用 computed 使 executeInfo 响应式更新
  const executeInfo = computed<ExecuteInfo>(() => ({
    id: executeDetail.value?.id || '',
    name: executeDetail.value?.pipelineName || 'stream-ci-demo',
    currentBuildNum: executeDetail.value?.buildNum ?? 1,
    latestBuildNum: executeDetail.value?.latestBuildNum ?? 1,
  }))

  const isDebugExec = computed(() => executeDetail.value?.debug ?? false)

  return {
    // Store状态
    loading,
    executeDetail,
    flowInfo,

    // 计算状态
    executeInfo,
    isRunning: isRunningOrNot,
    isLatestBuild,
    isDebugExec,

    // 方法
    initExecuteDetail: store.initExecuteDetail,
    silentRefreshExecuteDetail: store.silentRefreshExecuteDetail,
    stopExecute: store.stopExecute,
    requestRePlayFlow: store.requestRePlayFlow,
    requestRetryFlow: store.requestRetryFlow,
    requestTriggerStage: store.requestTriggerStage,
    requestUpdateRemark: store.requestUpdateRemark,
    getStartupParams: store.getStartupParams,
    fetchVersionDetail: store.fetchVersionDetail,
  }
}
