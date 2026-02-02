import {
  type Collation,
  type ContentTableItem,
  type SortType,
} from '@/api/flowContentList'
import { type GroupResponse } from '@/api/flowLabelGroup'
import { FLOW_GROUP_TYPES } from '@/constants/flowGroup'
import { ROUTE_NAMES } from '@/constants/routes'
import { useDeleteConfirm } from '@/hooks/useDeleteConfirm'
import { useFlowGroupData } from '@/hooks/useFlowGroupData'
import { Message } from 'bkui-vue'
import { storeToRefs } from 'pinia'
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { useFlowHomeContentStore } from '../stores/flowContentList'
import { FLOW_SORT_FILED, ORDER_ENUM } from '../utils/flowConst'

export interface Styles {
  iconStarBtn: string
  [key: string]: string
}

// 搜索参数的 key 列表
const SEARCH_KEYS = ['filterByPipelineName', 'filterByCreator', 'filterByViewIds', 'filterByLabels']

/**
 * 创作流列表数据 Hook
 * 用于管理创作流列表表格的数据获取、排序、分页等操作
 */
export function useFlowListData(styles?: Styles) {
  const { showDeleteConfirm } = useDeleteConfirm()
  const { flowGroups, loadAllData } = useFlowGroupData()
  const store = useFlowHomeContentStore()
  const { t } = useI18n()
  const route = useRoute()
  const router = useRouter()

  const {
    flowTableList,
    pagination,
    tableLoading,
    isShowAddToDialog,
    isShowCopyDialog,
    isShowSaveAsTemplateDialog,
    currentActionData,
  } = storeToRefs(store)

  // 本地状态管理
  const sortShow = ref(false)
  const currentSortType = ref(
    (route.query.sortType as string) ||
      localStorage.getItem('flowSortType') ||
      FLOW_SORT_FILED.latestBuildStartDate,
  )
  const currentCollation = ref(
    (route.query.collation as string) ||
      localStorage.getItem('flowSortCollation') ||
      ORDER_ENUM.ascending,
  )


  const currentGroup = computed(() => {
    const groupId = route.params.groupId as string
    
    // 先从 flowGroups 中查找
    const foundGroup = flowGroups.value.find((item) => item.id === groupId)
    if (foundGroup) {
      return foundGroup
    }
    
    // 如果没找到,说明是系统分组,构建一个虚拟的 group 对象
    const systemGroupNames: Record<string, string> = {
      [FLOW_GROUP_TYPES.ALL_FLOWS]: t('flow.common.allFlows'),
      [FLOW_GROUP_TYPES.MY_FAVORITES]: t('flow.sidebar.myFavorites'),
      [FLOW_GROUP_TYPES.MY_CREATED]: t('flow.sidebar.myCreated'),
      [FLOW_GROUP_TYPES.RECYCLE_BIN]: t('flow.sidebar.recycleBin'),
    }
    
    if (systemGroupNames[groupId]) {
      return {
        id: groupId,
        name: systemGroupNames[groupId],
      }
    }
    
    return undefined
  })

  // 搜索选择器的值（原始数据，用于 UI 展示）
  const searchValue = ref<
    Array<{ id: string; name: string; values?: Array<{ id: string; name: string }> }>
  >([])

  const labelsGroup = ref<GroupResponse[]>([])

  // 搜索选择器的数据配置
  const searchData = computed(() => {
    const baseSearchConfig = [
      {
        id: 'filterByPipelineName',
        name: t('flow.content.name'),
      },
      {
        id: 'filterByCreator',
        name: t('flow.content.creator'),
      },
      {
        id: 'filterByViewIds',
        name: t('flow.content.flowGroup'),
        multiple: true,
        children: flowGroups.value.filter((item) => item.viewType !== -1),
      },
    ]

    // 将 labelsGroup 中的每个分组作为独立的搜索配置项
    const labelSearchConfigs = labelsGroup.value
      .filter((item) => Array.isArray(item.labels) && item.labels.length > 0)
      .map((item) => ({
        id: item.id,
        name: item.name,
        multiple: true,
        children: item.labels,
      }))

    return [...baseSearchConfig, ...labelSearchConfigs]
  })

  const flatSearchParams = computed(() => {
    const params: any = {}
    const labelIds: string[] = []
    // 获取所有标签分组的 ID
    const labelGroupIds = labelsGroup.value.map((item) => item.id)

    searchValue.value.forEach((item) => {
      // 检查是否是标签分组
      if (labelGroupIds.includes(item.id)) {
        // 收集所有标签 ID
        if (item.values?.length) {
          labelIds.push(...item.values.map((v) => v.id))
        }
      } else if (item.values?.length) {
        // 其他多选类型
        params[item.id] = item.values.map((v) => v.id).join(',')
      } else if (item.name) {
        // 普通文本输入类型
        params[item.id] = item.name
      }
    })

    // 合并所有标签到 filterByLabels
    if (labelIds.length > 0) {
      params.filterByLabels = labelIds.join(',')
    }

    return params
  })

  const searchPlaceHolder = computed(() => {
    return searchData.value.map((item) => item.name).join('/')
  })

  const newFromTemplatePopupShow = ref(false)
  const importFlowPopupShow = ref(false)

  const isRecycleBin = computed(() => {
    return route.params.groupId === FLOW_GROUP_TYPES.RECYCLE_BIN
  })
  const latestExecIsStageProgress = ref(
    localStorage.getItem('latestExecIsStageProgress') === 'true' || false,
  )

  const currentSortIconName = computed(() => getSortIconName(currentSortType.value))
  const newFlowList = computed(() => [
    {
      text: t('flow.content.newFromTemplate'),
      handler: handleNewFromTemplate,
    },
    {
      text: t('flow.content.importFlow'),
      handler: handleImportFlow,
    },
  ])
  const sortList = computed(() => {
    return [
      {
        id: FLOW_SORT_FILED.flowName,
        name: t('flow.content.orderByAlpha'),
      },
      {
        id: FLOW_SORT_FILED.createDate,
        name: t('flow.content.orderByCreateTime'),
      },
      {
        id: FLOW_SORT_FILED.updateTime,
        name: t('flow.content.orderByUpdateTime'),
      },
      {
        id: FLOW_SORT_FILED.latestBuildStartDate,
        name: t('flow.content.orderByExecuteTime'),
      },
    ].map((sort) => ({
      ...sort,
      active: isActiveSort(sort.id),
      sortIcon: getSortIconName(sort.id),
    }))
  })

  /**
   * 初始化搜索选择器的值（从 URL 查询参数中获取）
   */
  let isSearchInitialized = false
  watch(
    flowGroups,
    (groups) => {
      // 只在首次加载且有数据时初始化一次
      if (!isSearchInitialized && groups.length > 0) {
        isSearchInitialized = true
        initSearchFromQuery()
      }
    },
    { immediate: true },
  )

  /**
   * 使用指定的 groupId 加载数据
   */
  function loadContentDataWithGroupId(groupId: string) {
    loadContentData(groupId)
    // 同时更新组数据
    loadAllData()
  }

  /**
   * 加载表格数据
   */
  async function loadContentData(groupId?: string) {
    const params = {
      projectId: route.params.projectId as string,
      page: pagination.value.current,
      pageSize: pagination.value.limit,
      sortType: currentSortType.value as SortType,
      collation:
        currentCollation.value === 'null'
          ? 'DEFAULT'
          : (currentCollation.value.toLocaleUpperCase() as Collation),
      viewId: groupId || (route.params.groupId as string),
      ...flatSearchParams.value,
    }
    await store.fetchFlowList(params)
  }

  /**
   * 从模板新建创作流
   */
  function handleNewFromTemplate() {
    newFromTemplatePopupShow.value = !newFromTemplatePopupShow.value
  }

  /**
   * 导入创作流
   */
  function handleImportFlow() {
    importFlowPopupShow.value = !importFlowPopupShow.value
  }

  /**
   * 更新路由查询参数
   */
  function updateQuery(clearSearchParams = false) {
    const queryParams: any = {
      ...route.query,
      sortType: currentSortType.value,
      ...(currentCollation.value ? { collation: currentCollation.value } : {}),
    }

    // 根据参数决定是否清除搜索参数
    if (clearSearchParams) {
      SEARCH_KEYS.forEach((key) => {
        delete queryParams[key]
      })
    }

    // 合并扁平化的搜索参数到路由中
    Object.assign(queryParams, flatSearchParams.value)

    router.push({
      query: queryParams,
    })
  }

  /**
   * 从路由查询参数初始化搜索条件
   */
  function initSearchFromQuery() {
    searchValue.value = SEARCH_KEYS.reduce(
      (result, key) => {
        const queryValue = route.query[key] as string
        if (!queryValue) return result

        // 特殊处理 filterByLabels - 将标签分配到各个分组
        if (key === 'filterByLabels') {
          const labelIds = queryValue.split(',')

          labelsGroup.value.forEach((labelGroup) => {
            if (!labelGroup.labels?.length) return

            const matchedLabels = labelIds
              .map((id) => labelGroup.labels.find((label) => label.id === id))
              .filter(Boolean)
              .map((label) => ({ id: label!.id, name: label!.name }))

            if (matchedLabels.length > 0) {
              result.push({
                id: labelGroup.id,
                name: labelGroup.name,
                values: matchedLabels,
              })
            }
          })
          return result
        }

        // 处理其他搜索参数
        const searchConfig = searchData.value.find((item) => item.id === key)
        if (!searchConfig) return result

        // 处理多选类型（有 children）
        if (searchConfig.children?.length) {
          const values = queryValue
            .split(',')
            .map((id) => searchConfig.children?.find((c) => c.id === id))
            .filter(Boolean)
            .map((child) => ({ id: child!.id, name: child!.name }))

          if (values.length > 0) {
            result.push({ id: key, name: searchConfig.name, values })
          }
        } else {
          // 处理普通文本输入类型
          result.push({
            id: key,
            name: searchConfig.name,
            values: [{ id: queryValue, name: queryValue }],
          })
        }

        return result
      },
      [] as Array<{ id: string; name: string; values?: Array<{ id: string; name: string }> }>,
    )
  }

  /**
   * 检查是否为当前激活的排序类型
   */
  function isActiveSort(sortType: string) {
    return currentSortType.value === sortType
  }

  /**
   * 获取排序图标名称
   */
  function getSortIconName(sortType: string) {
    if (isActiveSort(sortType) && currentCollation.value && currentCollation.value !== 'null') {
      return `sort-${currentCollation.value.toLowerCase()}`
    }
    return 'sort'
  }

  /**
   * 改变排序类型
   */
  function changeSortType(sortType: string) {
    if (sortType === currentSortType.value) {
      currentCollation.value =
        currentCollation.value === ORDER_ENUM.descending
          ? ORDER_ENUM.ascending
          : ORDER_ENUM.descending
    } else {
      switch (sortType) {
        case FLOW_SORT_FILED.flowName:
          currentCollation.value = ORDER_ENUM.ascending
          break
        case FLOW_SORT_FILED.createDate:
        case FLOW_SORT_FILED.updateTime:
        case FLOW_SORT_FILED.latestBuildStartDate:
          currentCollation.value = ORDER_ENUM.descending
          break
      }
      currentSortType.value = sortType
      sortShow.value = false
    }

    localStorage.setItem('flowSortType', currentSortType.value)
    localStorage.setItem('flowSortCollation', currentCollation.value)

    updateQuery()
  }

  /**
   * 表格排序变化处理
   */
  function handleTableSortChange({ sortType, collation }: { sortType: string; collation: string }) {
    currentSortType.value = sortType
    currentCollation.value = collation

    localStorage.setItem('flowSortType', sortType)
    localStorage.setItem('flowSortCollation', collation)
    updateQuery()
  }

  /**
   * 获取当前行的收藏按钮元素
   */
  function getStarButtonFromEvent(e: MouseEvent): HTMLElement | null {
    const target = e.target as HTMLElement
    const trElement = target.closest('tr.hover-highlight')
    return trElement ? trElement.querySelector(`.${styles?.iconStarBtn}`) : null
  }

  /**
   * 设置收藏按钮的显示状态
   */
  function setStarButtonVisibility(starBtn: HTMLElement | null, isVisible: boolean): void {
    if (starBtn) {
      starBtn.style.display = isVisible ? 'block' : 'none'
    }
  }

  function rowMouseEnter(e: MouseEvent, row: ContentTableItem): void {
    const starBtn = getStarButtonFromEvent(e)
    setStarButtonVisibility(starBtn, true)
  }

  function rowMouseLeave(e: MouseEvent, row: ContentTableItem): void {
    const starBtn = getStarButtonFromEvent(e)
    if (starBtn && !row.hasCollect) {
      setStarButtonVisibility(starBtn, false)
    }
  }

  /**
   * 收藏/取消收藏处理
   * @param type - 当前收藏状态
   * @param pipelineId - 流程 ID
   */
  async function collectHandler(type: boolean, pipelineId: string) {
    try {
      const res = await store.updateCollect(!type, pipelineId)
      if (res) {
        const action = !type ? t('flow.content.favorite') : t('flow.content.uncollect')
        const message = `${action}${t('flow.common.success')}`
        Message({ theme: 'success', message })
        loadContentDataWithGroupId(route.params.groupId as string)
      }
    } catch (error: any) {
      Message({ theme: 'error', message: error.message || error })
    }
  }

  /**
   * 分页变化处理
   */
  function handlePageChange(current: number) {
    pagination.value.current = current
  }

  /**
   * 每页大小变化处理
   */
  function handleLimitChange(limit: number) {
    pagination.value.limit = limit
    pagination.value.current = 1
  }

  function searchRefresh(clearSearchParams = false) {
    pagination.value.current = 1
    updateQuery(clearSearchParams)
    loadContentData()
  }

  /**
   * 处理搜索选择器变化
   */
  const handleSearchChange = (
    value: Array<{ id: string; name: string; values?: Array<{ id: string; name: string }> }>,
  ) => {
    searchValue.value = value
    searchRefresh(true)
  }

  /**
   * 清空搜索条件
   */
  function handleClearSearch() {
    searchValue.value = []
    searchRefresh(true)
  }

  function switchExecView() {
    latestExecIsStageProgress.value = !latestExecIsStageProgress.value
    localStorage.setItem('latestExecIsStageProgress', latestExecIsStageProgress.value.toString())
  }

  async function handleRestore(row: ContentTableItem) {
    showDeleteConfirm({
      message: t('flow.restore.restoreFlowConfirm', [row.pipelineName]),
      cancelText: t('flow.common.cancel'),
      theme: 'primary',
      confirmText: t('flow.common.confirm'),
      onConfirm: async () => {
        // TODO 恢复创作流
      },
    })
  }

  /**
   * 执行创作流
   */
  function handleExecute(row: ContentTableItem) {
    router.push({
      name: ROUTE_NAMES.FLOW_PREVIEW,
      params: { flowId: row.pipelineId, version: row.pipelineVersion, projectId: row.projectId },
    })
  }

  /**
   * 跳转到编辑页面
   * @param row 创作流数据
   */
  function goEdit(row: ContentTableItem) {
    router.push({
      name: ROUTE_NAMES.FLOW_EDIT_WORKFLOW_ORCHESTRATION,
      params: { flowId: row.pipelineId },
    })
  }

  return {
    // 原始数据（使用 storeToRefs 确保响应式）
    flowTableList,
    pagination,
    tableLoading,
    isShowAddToDialog,
    isShowCopyDialog,
    isShowSaveAsTemplateDialog,
    currentActionData,

    // 本地状态
    sortList,
    newFlowList,
    sortShow,
    currentSortType,
    currentCollation,
    currentSortIconName,
    newFromTemplatePopupShow,
    importFlowPopupShow,
    isRecycleBin,
    latestExecIsStageProgress,
    searchValue,
    searchData,
    searchPlaceHolder,
    currentGroup,
    labelsGroup,

    // 表格操作方法
    loadContentData,
    loadContentDataWithGroupId,
    changeSortType,
    handleTableSortChange,
    handlePageChange,
    handleLimitChange,
    handleSearchChange,
    handleClearSearch,
    updateQuery,
    switchExecView,
    handleRestore,
    collectHandler,
    rowMouseEnter,
    rowMouseLeave,
    goEdit,
    handleExecute,
    initSearchFromQuery,

    // 操作方法（直接暴露 store 的方法）
    closeAllDialogs: store.closeAllDialogs,
    removeContent: store.removeContent,
    confirmEnableAction: store.confirmEnableAction,
    copyContentItem: store.copyContentItem,
    saveContentAsTemplate: store.saveContentAsTemplate,
    addContentToFlowGroup: store.addContentToFlowGroup,
    setDeleteActionCallback: store.setDeleteActionCallback,
    setEnableActionCallback: store.setEnableActionCallback,
    getMatchDynamicData: store.getMatchDynamicData,
    getProjectTagList: store.getProjectTagList,
  }
}
