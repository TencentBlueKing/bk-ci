import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import { Message } from 'bkui-vue'
import { extForFile, repoTypeMap, ARTIFACTORY_TYPE, ARTIFACT_TYPES } from '@/utils/flowConst'
import { convertFileSize, convertTime } from '@/utils/util'
import { useExecuteDetail } from '@/hooks/useExecuteDetail'
import {
  requestOutputs,
  requestFileInfo,
  requestMetadataLabels,
  requestDownloadUrl,
  requestCustomDirTree,
  requestCopyFile,
  type Output,
  type GetFileInfoParams,
  type GetDownloadUrlParams,
  type ArtifactoryType,
  type GetCustomDirTreeParams,
  type CopyFileParams,
} from '@/api/outputs'

/**
 * Outputs 页面状态管理
 */
export const useOutputsStore = defineStore('outputs', () => {
  // ========== 状态定义 ==========
  const { t } = useI18n()
  const route = useRoute()
  const { executeDetail } = useExecuteDetail()
  // 制品列表
  const outputs = ref<Output[]>([])
  // 当前选中的制品
  const activeOutput = ref<Output | null>(null)
  // 当前制品的详情数据(元数据数组)
  const activeOutputDetail = ref<any>(null)
  // 元数据筛选值
  const artifactValue = ref<any[]>([])
  // 元数据筛选数据
  const artifactFilterData = ref<any[]>([])
  // 加载状态
  const isLoading = ref(false)

  const pagination = ref({
    page: 1,
    pageSize: 20,
    count: 0,
  })

  // ========== 计算属性 ==========
  // 是否为调试执行
  const isDebugExec = computed(() => executeDetail.value?.debug ?? false)
  // 筛选条件查询参数
  const filterQuery = computed(() => {
    const uniqueKeys = new Set()
    const result: Array<{ key: string; value: string }> = []

    artifactValue.value.forEach((item) => {
      item.values?.forEach((value: any) => {
        const keyValue = `${item.id}:${value.id}`
        if (!uniqueKeys.has(keyValue)) {
          uniqueKeys.add(keyValue)
          result.push({ key: item.id, value: value.id })
        }
      })
    })

    return result
  })

  // ========== Actions ==========

  /**
   * 获取元数据标签列表
   */
  async function getArtifactDate() {
    const repoList = await requestMetadataLabels({
      projectId: route.params.projectId as string,
      pipelineId: route.params.flowId as string,
      ...(isDebugExec.value ? { debug: isDebugExec.value } : {}),
    })

    artifactFilterData.value = repoList.map((item) => {
      const labelColorMapKeys = Object.keys(item.labelColorMap)
      return {
        id: item.labelKey,
        name: item.labelKey,
        multiable: true,
        ...(item.enumType
          ? {
              children: labelColorMapKeys.map((key) => ({
                id: key,
                name: key,
              })),
            }
          : {}),
      }
    })
  }

  async function init() {
    const { projectId, flowId, buildNo: buildId } = route.params

    try {
      isLoading.value = true
      const outputsResponse = await requestOutputs({
        projectId: projectId as string,
        pipelineId: flowId as string,
        buildId: buildId as string,
        qualityMetadata: filterQuery.value,
      })
      const { records } = outputsResponse
      outputs.value = records.map((item) => {
        const isReportOutput = item.artifactoryType === ARTIFACTORY_TYPE.REPORT
        const isImageOutput = item.artifactoryType === ARTIFACTORY_TYPE.IMAGE
        const icon = isReportOutput ? 'order' : item.folder ? 'folder' : extForFile(item.name)
        const id = isReportOutput
          ? item.createTime + (item.indexFileUrl ? item.indexFileUrl : '')
          : item.fullPath
        const type = isArtifact(item.artifactoryType) ? 'ARTIFACT' : ''
        return {
          type,
          ...item,
          id,
          icon,
          isReportOutput,
          isApp: ['ipafile', 'apkfile'].includes(icon),
          downloadable: isArtifact(item.artifactoryType) && !isImageOutput,
          isImageOutput,
        }
      })
    } catch (err: any) {
      Message({
        message: err.message ? err.message : err,
        theme: 'error',
      })
      outputs.value = []
    } finally {
      isLoading.value = false
    }
  }

  /**
   * 是否为制品
   */
  function isArtifact(artifactoryType: string) {
    return (ARTIFACT_TYPES as readonly string[]).includes(artifactoryType)
  }

  function getFolderSize(payload: Output) {
    if (!payload.folder) return '0'
    return getValuesByKey(payload.properties, 'size')
  }

  /**
   * 获取包含的文件和文件夹数量
   */
  function getInclude(payload: Output) {
    if (!payload.folder) return '--'
    const fileCount = getValuesByKey(payload.properties, 'fileCount')
    const folderCount = getValuesByKey(payload.properties, 'folderCount')
    return t('flow.execute.fileAndFolder', [fileCount, folderCount])
  }

  /**
   * 从属性列表中获取值
   */
  function getValuesByKey(data: any, key: string) {
    for (const item of data) {
      if (key.includes(item.key)) {
        return item.value
      }
    }
  }

  /**
   * 获取文件详情(返回元数据数组)
   */
  async function showDetail(output: Output) {
    const { projectId } = route.params
    try {
      isLoading.value = true
      const params: GetFileInfoParams = {
        projectId: projectId as string,
        artifactoryType: output.artifactoryType,
        path: output.fullPath,
      }
      const res = await requestFileInfo(params)
      activeOutputDetail.value = {
        ...output,
        ...res,
        artifactoryTypeTxt: repoTypeMap[output.artifactoryType] ?? '--',
        size: output.folder
          ? convertFileSize(getFolderSize(output), 'B')
          : res.size > 0
            ? convertFileSize(res.size, 'B')
            : '--',
        createdTime: convertTime(res.createdTime * 1000),
        modifiedTime: convertTime(res.modifiedTime * 1000),
        icon: !output.folder ? extForFile(res.name) : 'folder',
        include: getInclude(output),
      }
    } catch (err: any) {
      Message({
        message: err.message ? err.message : err,
        theme: 'error',
      })
    } finally {
      isLoading.value = false
    }
  }
  /**
   * 设置当前选中的制品
   */
  function setActiveOutput(output: Output): void {
    activeOutput.value = output
    switch (output.type) {
      case 'THIRDPARTY':
      case 'INTERNAL':
        break
      case 'ARTIFACT':
        showDetail(output)
        break
    }
  }

  /**
   *  获取制品下载URL
   */
  async function fetchDownloadUrl(artifactoryType: ArtifactoryType, path: string) {
    try {
      const params: GetDownloadUrlParams = {
        projectId: route.params.projectId as string,
        artifactoryType,
        path,
      }
      const res = await requestDownloadUrl(params)
      return res.url2
    } catch (error) {
      console.log('error:', error)
    }
  }

  async function requestCustomFolder(path: string) {
    try {
      const params: GetCustomDirTreeParams = {
        projectId: route.params.projectId as string,
        path,
      }
      return await requestCustomDirTree(params)
    } catch (error) {
      console.log('error:', error)
    }
  }

  async function requestCopyArtifactories(params: CopyFileParams): Promise<boolean> {
    return await requestCopyFile(params)
  }

  return {
    // State
    executeDetail,
    outputs,
    activeOutput,
    activeOutputDetail,
    artifactValue,
    artifactFilterData,
    isLoading,
    pagination,
    isDebugExec,
    filterQuery,

    // Actions
    init,
    getArtifactDate,
    setActiveOutput,
    isArtifact,
    getValuesByKey,
    fetchDownloadUrl,
    requestCustomFolder,
    requestCopyArtifactories,
  }
})
