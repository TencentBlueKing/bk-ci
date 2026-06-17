import {
  replayFlow,
  requestBuildParamCombination,
  requestBuildParams,
  requestFlowVersion,
  requestPipelineExecDetail,
  requestTerminatePipeline,
  retryFlow,
  triggerStage as triggerStageApi,
} from '@/api/executeDetail'
import { updateRemark } from '@/api/flowInfo'
import type { ExecuteDetailData } from '@/types/flow'
import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { useRoute } from 'vue-router'
import { useFlowInfoStore } from './flowInfoStore'

export const useExecuteDetailStore = defineStore('executeDetail', () => {
  const route = useRoute()

  // 使用 computed 属性，自动响应路由参数变化
  const flowId = computed(() => route.params.flowId as string)
  const projectId = computed(() => route.params.projectId as string)
  const buildNo = computed(() => route.params.buildNo as string)

  const flowInfoStore = useFlowInfoStore()

  const loading = ref(false)
  const executeDetail = ref<ExecuteDetailData | null>(null)

  async function getExecuteDetail() {
    const params = {
      projectId: projectId.value,
      buildNo: buildNo.value,
      pipelineId: flowId.value,
      executeCount: route.query.executeCount ? Number(route.query.executeCount) : undefined,
    }
    return await requestPipelineExecDetail(params)
  }

  async function initExecuteDetail() {
    try {
      loading.value = true
      executeDetail.value = null
      flowInfoStore.reset()

      const [executeRes] = await Promise.all([getExecuteDetail(), flowInfoStore.getFlowInfo()])

      executeDetail.value = executeRes
    } catch (error) {
      console.error('Failed to fetch execute detail:', error)
      executeDetail.value = null
    } finally {
      loading.value = false
    }
  }

  /**
   * Silently refresh execute detail data (only record API, no loading state)
   * Used for polling to update execution progress without UI loading effect
   */
  async function silentRefreshExecuteDetail() {
    try {
      const executeRes = await getExecuteDetail()
      executeDetail.value = executeRes
      return executeRes
    } catch (error) {
      console.error('Failed to silently refresh execute detail:', error)
      throw error
    }
  }
  /**
   * 获取指定版本号的流水线编排版本信息
   */
  async function fetchVersionDetail(version: number) {
    try {
      const result = await requestFlowVersion({
        version,
        projectId: projectId.value,
        pipelineId: flowId.value,
      })
      return result
    } catch (error) {
      throw error
    }
  }
  /**
   *  终止流水线
   */
  async function stopExecute(buildId: string) {
    try {
      const res = await requestTerminatePipeline({
        projectId: projectId.value,
        pipelineId: flowId.value,
        buildId: buildId,
      })
      return res
    } catch (error) {
      console.log('Failed to stoperror:', error)
    }
  }

  /**
   * 重试创作流retryFlow
   */
  async function requestRetryFlow({
    projectId,
    pipelineId,
    buildId,
    taskId,
    failedContainer,
    skip,
  }: {
    projectId: string
    pipelineId: string
    buildId: string
    taskId?: string
    failedContainer?: string
    skip?: boolean
  }) {
    try {
      const params = {
        projectId,
        pipelineId,
        buildId,
        taskId,
        failedContainer,
        skip,
      }
      return await retryFlow(params)
    } catch (error) {
      throw error
    }
  }

  /**
   * 重放创作流
   */
  async function requestRePlayFlow({
    projectId,
    pipelineId,
    buildId,
    forceTrigger,
  }: {
    projectId: string
    pipelineId: string
    buildId: string
    forceTrigger?: boolean
  }) {
    try {
      const params = {
        projectId,
        pipelineId,
        buildId,
        forceTrigger,
      }
      return await replayFlow(params)
    } catch (error) {
      console.log(error)
    }
  }

  async function requestUpdateRemark({
    projectId,
    pipelineId,
    buildId,
    remark,
  }: {
    projectId: string
    pipelineId: string
    buildId: string
    remark: string
  }) {
    try {
      return await updateRemark({
        projectId,
        pipelineId,
        buildId,
        remark,
      })
    } catch (error) {}
  }

  /**
   * Stage 审核触发
   */
  async function requestTriggerStage({
    stageId,
    cancel,
    reviewParams,
    id,
    suggest,
  }: {
    stageId: string
    cancel: boolean
    reviewParams?: Record<string, unknown>[]
    id?: string
    suggest?: string
  }) {
    return await triggerStageApi({
      projectId: projectId.value,
      pipelineId: flowId.value,
      buildNo: buildNo.value,
      stageId,
      cancel,
      reviewParams,
      id,
      suggest,
    })
  }

  /**
   * 获取启动参数及当前参数组合
   * @param urlParams
   * @returns
   */
  async function getStartupParams(urlParams: {
    projectId: string
    pipelineId: string
    buildId: string
  }) {
    const [buildParams, paramProperties] = await Promise.all([
      requestBuildParams(urlParams),
      requestBuildParamCombination(urlParams),
    ])
    return {
      buildParams,
      paramProperties,
    }
  }

  /**
   * Update execution detail from a WebSocket push.
   * The payload shape matches the REST API response.
   */
  function updateFromWebSocket(data: ExecuteDetailData | null) {
    if (data) {
      executeDetail.value = data
    }
  }

  function $reset() {
    loading.value = false
    executeDetail.value = null
    flowInfoStore.reset()
  }

  return {
    loading,
    executeDetail,

    initExecuteDetail,
    silentRefreshExecuteDetail,
    updateFromWebSocket,
    $reset,
    stopExecute,
    requestRePlayFlow,
    requestRetryFlow,
    requestTriggerStage,
    requestUpdateRemark,
    getStartupParams,
    fetchVersionDetail,
  }
})
