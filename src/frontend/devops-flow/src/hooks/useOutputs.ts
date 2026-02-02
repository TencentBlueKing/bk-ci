import { ref, computed, nextTick } from 'vue'
import { storeToRefs } from 'pinia'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useOutputsStore } from '@/stores/outputsStore'
import { convertFileSize } from '@/utils/util'
import { repoTypeNameMap } from '@/utils/flowConst'
import type { Output } from '@/api/outputs'

/**
 * 筛选项数据结构
 */
interface FilterItem {
  id: string
  name: string
  multiable?: boolean
  values?: Array<{ id: string; name: string }>
  children?: Array<{ id: string; name: string }>
}

/**
 * Outputs 页面业务逻辑 Hook
 */
export function useOutputs(currentTab: string) {
  const { t } = useI18n()
  const route = useRoute()
  const router = useRouter()
  const store = useOutputsStore()
  // ========== Store 状态引用 ==========
  const {
    outputs,
    activeOutput,
    activeOutputDetail,
    artifactValue,
    artifactFilterData,
    isDebugExec,
    filterQuery,
    isLoading,
    pagination,
  } = storeToRefs(store)

  // ========== 本地状态 ==========
  const keyWord = ref('')
  const copyToDialogRef = ref()
  const iframeReportRef = ref()
  const qualityMetadata = ref<{ labelKey?: string; values?: string[] }>({})

  // ========== 计算属性 - 数据二次加工 ==========

  // 第三方报告列表
  const thirdPartyReportList = computed(() =>
    outputs.value.filter((report) => isThirdReport(report.reportType)),
  )

  // 报告列表（不含第三方报告）
  const reports = computed(() =>
    outputs.value.filter(
      (item) => item.artifactoryType === 'REPORT' && !isThirdReport(item.reportType),
    ),
  )

  // 制品列表
  const artifacts = computed(() =>
    outputs.value.filter((item) => store.isArtifact(item.artifactoryType)),
  )

  // 可见的输出列表（根据 currentTab 和搜索关键词过滤）
  const visibleOutputs = computed(() => {
    // 添加第三方报告聚合项
    const thirdReportList =
      thirdPartyReportList.value.length > 0
        ? [
            {
              id: 'THIRDPARTY',
              type: 'REPORT',
              reportType: 'THIRDPARTY',
              name: t('flow.execute.thirdReport'),
              icon: 'bar-chart',
            } as Output,
          ]
        : []

    let visibleList: Output[] = []
    switch (currentTab) {
      case 'artifacts':
        visibleList = artifacts.value
        break
      case 'reports':
        visibleList = [...reports.value, ...thirdReportList]
        break
      default:
        visibleList = [
          ...outputs.value.filter((output) => !isThirdReport(output.reportType)),
          ...thirdReportList,
        ]
    }

    // 关键词搜索
    return visibleList
      .filter((output) => output.name.toLowerCase().includes(keyWord.value.toLowerCase()))
      .map(({ size, ...rest }) => ({
        ...rest,
        size: size ? convertFileSize(size, 'B') : '--',
      }))
  })

  // 当前是否为第三方报告
  const isActiveThirdReport = computed(() => isThirdReport(activeOutput.value?.reportType))

  // 当前是否为自定义报告
  const isCustomizeReport = computed(() => activeOutput.value?.reportType === 'INTERNAL')

  // 操作按钮列表
  const btns = computed(() => [
    {
      text: t('flow.execute.goRepo'),
      handler: () => {
        if (!activeOutput.value) return

        const urlPrefix = `/console/repo/${route.params.projectId}`
        const pos = activeOutput.value.fullPath.lastIndexOf('/')
        const fileName = activeOutput.value.fullPath.substring(0, pos)
        const repoName = repoTypeNameMap[activeOutput.value.artifactoryType]
        let url = `${urlPrefix}/generic?repoName=${repoName}&path=${encodeURIComponent(fileName)}/default`

        if (activeOutput.value.isImageOutput) {
          const imageVerion = activeOutput.value.fullName?.slice(
            activeOutput.value.fullName.lastIndexOf(':') + 1,
          )
          url = `${urlPrefix}/docker/package?repoName=${repoName}&packageKey=${encodeURIComponent(`docker://${activeOutput.value.name}`)}&version=${imageVerion}`
        }
        window.open(url, '_blank')
      },
    },
  ])

  // 制品更多操作菜单
  const artifactMoreActions = computed(() => [
    {
      text: t('flow.execute.copyTo'),
      handler: () => copyToDialogRef.value?.show?.(),
    },
  ])

  // 基础信息行配置
  const baseInfoRows = computed(() =>
    activeOutputDetail.value?.folder
      ? [
          { key: 'name', name: t('flow.execute.directoryName') },
          { key: 'fullName', name: t('flow.execute.directoryPath') },
          { key: 'size', name: t('flow.execute.size') },
          { key: 'include', name: t('flow.execute.include') },
          { key: 'createdTime', name: t('flow.execute.created') },
          { key: 'modifiedTime', name: t('flow.execute.lastModified') },
        ]
      : [
          { key: 'name', name: t('flow.execute.name') },
          { key: 'fullName', name: t('flow.execute.filePath') },
          { key: 'size', name: t('flow.execute.size') },
          { key: 'createdTime', name: t('flow.execute.created') },
          { key: 'modifiedTime', name: t('flow.execute.lastModified') },
        ],
  )

  // 校验和行配置
  const checkSumRows = computed(() => [
    { key: 'sha256', name: 'SHA256' },
    { key: 'sha1', name: 'SHA1' },
    { key: 'md5', name: 'MD5' },
  ])

  // 信息块配置
  const infoBlocks = computed(() => {
    const baseInfoBlock = {
      key: 'baseInfo',
      title: t('flow.execute.baseInfo'),
      block: baseInfoRows.value,
      value: activeOutputDetail.value,
    }

    if (activeOutputDetail.value.folder) {
      return [baseInfoBlock]
    }

    return [
      baseInfoBlock,
      {
        key: 'meta',
        title: t('flow.execute.metaData'),
        value: activeOutputDetail.value.nodeMetadata,
      },
      {
        key: 'checkSum',
        title: t('flow.execute.checkSum'),
        block: checkSumRows.value,
        value: activeOutputDetail.value.checksums,
      },
    ]
  })

  // ========== 方法 ==========

  /**
   * 判断是否为第三方报告
   */
  const isThirdReport = (reportType?: string) => {
    return ['THIRDPARTY'].includes(reportType || '')
  }

  /**
   * 全屏查看报告
   */
  function fullScreenViewReport(output: Output) {
    store.setActiveOutput(output)
    nextTick(() => {
      iframeReportRef.value?.toggleFullScreen?.()
    })
  }

  /**
   * 更新搜索条件
   */
  function updateSearchKey(value: FilterItem[]) {
    artifactValue.value = value
    const metadataKey = route.query.metadataKey
    const hasMetadataKey = value.some((item) => item.id === metadataKey)
    const query = { ...route.query }

    if (!hasMetadataKey) {
      delete query.metadataKey
      delete query.metadataValues
      router.replace({ query })
      store.init()
    }
  }

  /**
   * 初始化筛选条件（从路由参数）
   */
  function initializeArtifactValue() {
    if (!Object.keys(qualityMetadata.value).length) return

    const { labelKey, values } = qualityMetadata.value
    if (labelKey && values) {
      artifactValue.value = [
        {
          id: labelKey,
          name: labelKey,
          multiable: true,
          values: values.map((item) => ({
            id: item,
            name: item,
          })),
        },
      ]
    } else {
      artifactValue.value = []
    }
    store.init()
  }

  function getFolderSize(payload: any) {
    if (!payload.folder) return '0'
    return getValuesByKey(payload.properties, 'size')
  }

  function getValuesByKey(data: Array<{ key: string; value: any }>, key: string) {
    for (const item of data) {
      if (key.includes(item.key)) {
        return item.value
      }
    }
  }

  return {
    // Store 状态
    outputs,
    activeOutput,
    activeOutputDetail,
    artifactValue,
    artifactFilterData,
    isDebugExec,
    filterQuery,
    isLoading,
    pagination,

    // 本地状态
    keyWord,
    copyToDialogRef,
    iframeReportRef,
    qualityMetadata,

    // 计算属性
    thirdPartyReportList,
    reports,
    artifacts,
    visibleOutputs,
    isActiveThirdReport,
    isCustomizeReport,
    btns,
    artifactMoreActions,
    baseInfoRows,
    checkSumRows,
    infoBlocks,

    // 方法
    init: store.init,
    getArtifactDate: store.getArtifactDate,
    setActiveOutput: store.setActiveOutput,
    requestDownloadUrl: store.fetchDownloadUrl,
    requestCustomFolder: store.requestCustomFolder,
    requestCopyArtifactories: store.requestCopyArtifactories,

    fullScreenViewReport,
    updateSearchKey,
    initializeArtifactValue,
    getFolderSize,
  }
}
