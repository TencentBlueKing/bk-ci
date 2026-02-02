import {
  replayFlow,
  requestBuildParamCombination,
  requestBuildParams,
  requestFlowVersion,
  requestPipelineExecDetail,
  requestTerminatePipeline,
  retryFlow,
} from '@/api/executeDetail'
import { fetchFlowInfo, updateRemark } from '@/api/flowInfo'
import type { ExecuteDetailData, FlowInfo } from '@/types/flow'
import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { useRoute } from 'vue-router'

export const useExecuteDetailStore = defineStore('executeDetail', () => {
  const route = useRoute()
  
  // 使用 computed 属性，自动响应路由参数变化
  const flowId = computed(() => route.params.flowId as string)
  const projectId = computed(() => route.params.projectId as string)
  const buildNo = computed(() => route.params.buildNo as string)

  const loading = ref(false)
  const executeDetail = ref<ExecuteDetailData | null>(null)
  const flowInfo = ref<FlowInfo | null>(null)

  async function getExecuteDetail() {
    const params = {
      projectId: projectId.value,
      buildNo: buildNo.value,
      pipelineId: flowId.value,
      executeCount: route.query.executeCount
        ? Number(route.query.executeCount)
        : undefined,
    }
    return await requestPipelineExecDetail(params)
  }

  async function getFlowInfoDetail() {
    return await fetchFlowInfo({
      projectId: projectId.value,
      flowId: flowId.value,
    })
  }

  async function initExecuteDetail() {
    try {
      loading.value = true
      // 先清空旧数据，避免显示旧数据
      executeDetail.value = null
      flowInfo.value = null
      
      const [executeRes, flowInfoRes] = await Promise.all([getExecuteDetail(), getFlowInfoDetail()])

      executeDetail.value = executeRes
      flowInfo.value = flowInfoRes
    } catch (error) {
      console.error('Failed to fetch execute detail:', error)
      executeDetail.value = null
      flowInfo.value = null
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
  async function  fetchVersionDetail (version: number) {
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
      console.log(error)
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
   * 获取启动参数及当前参数组合
   * @param urlParams 
   * @returns 
   */
  async function getStartupParams(urlParams: { projectId: string; pipelineId: string; buildId: string }) {
    const [buildParams, paramProperties] = await Promise.all([
      requestBuildParams(urlParams),
      requestBuildParamCombination(urlParams),
    ])
    return {
      buildParams,
      paramProperties,
    }
  }

  return {
    loading,
    executeDetail,
    flowInfo,

    initExecuteDetail,
    silentRefreshExecuteDetail,
    stopExecute,
    requestRePlayFlow,
    requestRetryFlow,
    requestUpdateRemark,
    getStartupParams,
    fetchVersionDetail,
  }
})
