import {
  addToFlowGroup,
  copyContent,
  deleteContent,
  disableContent,
  getContentDetail,
  getContentTableData,
  getRecycleTableData,
  restoreContent,
  getMatchDynamicView,
  saveAsTemplate,
  toggleFlowFavorite,
  type DeleteContentParams,
  type ContentTableItem,
  type ContentTableParams,
  type CopyFlowParams,
  type MatchDynamicViewParams,
  type SaveAsTemplateParams,
  type AddToFlowGroupParams,
} from '@/api/flowContentList'
import { getProjectGroups } from '@/api/flowLabelGroup'
import { RESOURCE_ACTION, RESOURCE_TYPES } from '@/components/Permission/constants'
import { useRoute } from 'vue-router'
import { ROUTE_NAMES } from '@/constants/routes'
import { STATUS, type StageStatusInfo } from '@/types/flow'
import { VERSION_STATUS_ENUM } from '@/utils/flowConst'
import { convertTime } from '@/utils/util'
import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'

// 操作列弹窗类型枚举
export enum DialogType {
  ADD_TO = 'addTo',
  COPY = 'copy',
  SAVE_AS_TEMPLATE = 'saveAsTemplate',
}

type IconMap = {
  SUCCEED: string
  FAILED: string
  RUNNING: string
  PAUSE: string
  SKIP: string
}

export const useFlowHomeContentStore = defineStore('flowContentList', () => {
  const { t, locale } = useI18n()
  const route = useRoute()
  const flowTableList = ref<ContentTableItem[]>([])
  const tableLoading = ref(false)
  const pagination = ref({
    current: 1,
    count: 0,
    limit: 20,
  })

  const currentActionData = ref<any>(null)
  const statusIconMap = computed<IconMap>(() => ({
    SUCCEED: 'check-circle-shape',
    FAILED: 'close-circle-shape',
    RUNNING: 'circle-2-1',
    PAUSE: 'play-circle-shape',
    SKIP: 'redo-arrow',
    CANCELED: 'abort',
  }))

  const isShowAddToDialog = ref(false)
  const isShowCopyDialog = ref(false)
  const isShowSaveAsTemplateDialog = ref(false)

  // 删除和启用/禁用操作的回调函数
  let deleteActionCallback: ((data: ContentTableItem) => void) | null = null
  let enableActionCallback: ((data: ContentTableItem) => void) | null = null

  /**
   * 设置删除操作回调
   */
  function setDeleteActionCallback(callback: (data: ContentTableItem) => void) {
    deleteActionCallback = callback
  }

  /**
   * 设置启用/禁用操作回调
   */
  function setEnableActionCallback(callback: (data: ContentTableItem) => void) {
    enableActionCallback = callback
  }

  /**
   * 添加操作按钮配置
   */
  function processContentItem(content: ContentTableItem): ContentTableItem {
    const isDraft = content.latestVersionStatus === VERSION_STATUS_ENUM.COMMITTING
    const isBranch = content.latestVersionStatus === VERSION_STATUS_ENUM.BRANCH
    return {
      ...content,
      latestBuildRoute: {
        name: ROUTE_NAMES.FLOW_DETAIL_EXECUTION_DETAIL_TAB,
        params: {
          projectId: content.projectId,
          flowId: content.pipelineId,
          buildNo: content.latestBuildId,
        },
      },
      updateDate: convertTime(content.updateTime),
      createDate: convertTime(content.createTime),
      duration: calcDuration(content),
      progress: calcProgress(content),
      onlyDraftVersion: isDraft,
      onlyBranchVersion: isBranch,
      latestBuildStartDate: content.latestBuildStartTime
        ? convertTime(content.latestBuildStartTime)
        : '--',
      latestBuildStageStatus: getLatestBuildStageStatus(content),
      released: content.latestVersionStatus === VERSION_STATUS_ENUM.RELEASED,
      disabled: isDisabledPipeline(content),
      tooltips: disabledTips(content),
      flowAction: getFlowActions(content, isDraft),
    }
  }

  /**
   * 获取流水线操作菜单配置
   */
  function getFlowActions(content: ContentTableItem, isDraft: boolean) {
    const permissions = content.permissions
    const editPermData = {
      projectId: content.projectId,
      resourceType: RESOURCE_TYPES.CREATIVE_STREAM,
      resourceCode: content.pipelineId,
      action: RESOURCE_ACTION.EDIT,
    }
    const deletePermData = {
      projectId: content.projectId,
      resourceType: RESOURCE_TYPES.CREATIVE_STREAM,
      resourceCode: content.pipelineId,
      action: RESOURCE_ACTION.DELETE,
    }

    const actions = [
      {
        key: 'enable',
        text: content.lock ? t('flow.content.enable') : t('flow.content.disable'),
        hasPermission: permissions?.canEdit ?? true,
        disablePermissionApi: true,
        permissionData: editPermData,
        handler: (data: ContentTableItem) => {
          if (enableActionCallback) {
            enableActionCallback(data)
          } else {
            openActionDialog(data, DialogType.ADD_TO)
          }
        },
      },
      {
        key: 'addTo',
        text: t('flow.content.addTo'),
        hasPermission: permissions?.canEdit ?? true,
        disablePermissionApi: true,
        permissionData: editPermData,
        handler: (data: ContentTableItem) => openActionDialog(data, DialogType.ADD_TO),
      },
      {
        key: 'copy',
        text: t('flow.content.copyCreationFlow'),
        hasPermission: permissions?.canEdit ?? true,
        disablePermissionApi: true,
        permissionData: editPermData,
        handler: (data: ContentTableItem) => openActionDialog(data, DialogType.COPY),
      },
      // {
      //   key: 'saveAsTemplate',
      //   text: t('flow.content.saveAsTemplate'),
      //   handler: (data: ContentTableItem) => openActionDialog(data, DialogType.SAVE_AS_TEMPLATE),
      // },
      {
        key: 'delete',
        text: t('flow.actions.delete'),
        hasPermission: permissions?.canDelete ?? true,
        disablePermissionApi: true,
        permissionData: deletePermData,
        handler: (data: ContentTableItem) => {
          if (deleteActionCallback) {
            deleteActionCallback(data)
          } else {
            openActionDialog(data, DialogType.ADD_TO)
          }
        },
      },
    ]

    // 草稿状态只显示：添加至、复制创作流、删除
    const draftActions = ['addTo', 'copy', 'delete']
    return isDraft ? actions.filter((action) => draftActions.includes(action.key)) : actions
  }

  function calcProgress({
    latestBuildStatus,
    lastBuildFinishCount = 0,
    lastBuildTotalCount = 1,
    currentTimestamp,
    latestBuildStartTime,
  }: ContentTableItem) {
    if (latestBuildStatus === STATUS.RUNNING) {
      return `${t('flow.content.execedTimes')}${convertMStoStringByRule(currentTimestamp - latestBuildStartTime)}(${Math.floor((lastBuildFinishCount / lastBuildTotalCount) * 100)}%)`
    }
    return ''
  }

  /**
   *  将毫秒值转换成x时x分x秒的形式并使用格式化规则
   *  @param {Number} time - 时间的毫秒形式
   *  @return {String} str - 转换后的字符串
   */
  function convertMStoStringByRule(time: number) {
    if (time < 0) {
      return '--'
    }
    let res = ''
    if (locale.value === 'en-US') {
      res = convertToEn(time)
    } else {
      res = convertToCn(time)
    }
    return res
  }

  function convertToCn(time: number) {
    const str = convertMStoString(time)
    let res = str
    const arr = str.match(/^\d{1,}([\u4e00-\u9fa5]){1,}/) || []
    if (arr.length) {
      switch (arr[1]) {
        case '秒':
          res = t('flow.content.lessThanOneMinute')
          break
        case '天':
          res = t('flow.content.greaterThanDays', [arr[0]])
          break
        case '时':
          res = str.replace(/\d{1,}秒/, '')
          break
      }
    }
    return res
  }

  function convertToEn(time: number) {
    const sec = time / 1000
    let res = ''
    if (sec <= 60) {
      res = 'less than 1 minute'
    } else if (sec <= 60 * 60) {
      res = `${Math.floor(sec / 60)}m and ${Math.floor(sec % 60)}s`
    } else if (time <= 60 * 60 * 24) {
      res = `${Math.floor(sec / 3600)}h and ${Math.floor((sec % 60) / 60)}m`
    } else {
      res = `more than ${Math.floor(sec / 86400)} days`
    }
    return res
  }

  /**
   * 获取执行耗时
   */
  function calcDuration({
    latestBuildEndTime,
    latestBuildStartTime,
    latestBuildNum,
  }: ContentTableItem) {
    if (latestBuildNum) {
      const duration = convertMStoStringByRule(latestBuildEndTime - latestBuildStartTime)
      return t('flow.content.totalTime', [duration])
    }
    return '--'
  }

  /**
   *  将毫秒值转换成x时x分x秒的形式
   *  @param {Number} time - 时间的毫秒形式
   *  @return {String} str - 转换后的字符串
   */
  function convertMStoString(time: number) {
    function getSeconds(sec: number) {
      return `${sec}${t('flow.content.timeMap.seconds')}`
    }

    function getMinutes(sec: number) {
      if (sec / 60 >= 1) {
        return `${Math.floor(sec / 60)}${t('flow.content.timeMap.minutes')}${getSeconds(sec % 60)}`
      } else {
        return getSeconds(sec)
      }
    }

    function getHours(sec: number) {
      if (sec / 3600 >= 1) {
        return `${Math.floor(sec / 3600)}${t('flow.content.timeMap.hours')}${getMinutes(sec % 3600)}`
      } else {
        return getMinutes(sec)
      }
    }

    function getDays(sec: number) {
      if (sec / 86400 >= 1) {
        return `${Math.floor(sec / 86400)}${t('flow.content.timeMap.days')}${getHours(sec % 86400)}`
      } else {
        return getHours(sec)
      }
    }

    return time ? getDays(Math.floor(time / 1000)) : `0${t('flow.content.timeMap.seconds')}`
  }

  function getStageTooltip(stage: StageStatusInfo) {
    switch (true) {
      case !!stage.elapsed:
        return `${stage.name}: ${convertMStoString(stage.elapsed)}`
      case stage.status === STATUS.PAUSE:
        return t('flow.content.toCheck')
      case stage.status === STATUS.SKIP:
        return t('flow.content.skipStageDesc')
    }
  }

  function isDisabledPipeline(item: ContentTableItem) {
    return item.lock || !item.canManualStartup
  }

  function disabledTips(item: ContentTableItem): string | { disabled: boolean } | undefined {
    if (!isDisabledPipeline(item)) return { disabled: true }
    return t(item.lock ? 'flow.content.pipelineLockTips' : 'flow.content.pipelineManualDisable')
  }

  /**
   * 获取最近执行stage进度数据
   */
  function getLatestBuildStageStatus(item: ContentTableItem) {
    return item.latestBuildStageStatus
      ? item.latestBuildStageStatus.slice(1).map((stage: StageStatusInfo) => {
          const supportedStatuses = [
            STATUS.SUCCEED,
            STATUS.FAILED,
            STATUS.RUNNING,
            STATUS.PAUSE,
            STATUS.SKIP,
            STATUS.CANCELED,
          ] as const
          const icon = supportedStatuses.includes(stage.status as any)
            ? statusIconMap.value[stage.status as keyof IconMap]
            : 'circle'

          return {
            ...stage,
            tooltip: getStageTooltip(stage),
            icon,
            statusCls: stage.status,
          }
        })
      : undefined
  }

  /**
   * 打开列操作弹窗
   */
  function openActionDialog(data: ContentTableItem, dialogType: DialogType) {
    currentActionData.value = data

    // 根据弹窗类型设置对应的显示状态
    switch (dialogType) {
      case DialogType.ADD_TO:
        isShowAddToDialog.value = true
        break
      case DialogType.COPY:
        isShowCopyDialog.value = true
        break
      case DialogType.SAVE_AS_TEMPLATE:
        isShowSaveAsTemplateDialog.value = true
        break
    }
  }

  /**
   * 关闭列操作弹窗
   */
  function closeAllDialogs() {
    isShowAddToDialog.value = false
    isShowCopyDialog.value = false
    isShowSaveAsTemplateDialog.value = false
    currentActionData.value = null
  }

  /**
   * 加载内容表格数据
   */
  async function fetchFlowList(params: ContentTableParams,  isRecycleBin = false) {
    tableLoading.value = true
    try {
      const response = isRecycleBin
        ? await getRecycleTableData(params)
        : await getContentTableData(params)

      flowTableList.value = response.records.map(processContentItem)
      pagination.value = {
        count: response.count,
        current: response.page,
        limit: response.pageSize,
      }

      return response
    } catch (error) {
      console.error('Failed to load content table data:', error)
      throw error
    } finally {
      tableLoading.value = false
    }
  }

  /**
   * 获取内容详情
   */
  async function restoreFlow(pipelineId: string) {
    try {
      const res = await restoreContent({
        pipelineId,
        projectId: route.params.projectId as string,
      })
      return res
    } catch (error) {
      throw error
    }
  }

  /**
   * 获取内容详情
   */
  async function loadContentDetail(id: string) {
    try {
      const detail = await getContentDetail(id)
      return detail
    } catch (error) {
      console.error('Failed to load content detail:', error)
      throw error
    }
  }

  /**
   * 删除创作流
   */
  async function removeContent(flowId: string) {
    try {
      const params: DeleteContentParams = {
        projectId: route.params.projectId as string,
        pipelineIds: [flowId],
      }
      const res = await deleteContent(params)
      return res
    } catch (error) {
      console.error('Failed to remove content:', error)
      throw error
    }
  }

  /**
   * 禁用创作流
   */
  async function confirmEnableAction(flowId: string, lock: boolean) {
    try {
      const params: { pipelineId: string; projectId: string; enable: boolean } = {
        pipelineId: flowId,
        projectId: route.params.projectId as string,
        enable: lock,
      }
      const res = await disableContent(params)
      return res
    } catch (error) {
      console.error('Failed to disable content:', error)
      throw error
    }
  }

  /**
   * 复制创作流
   */
  async function copyContentItem(param: CopyFlowParams) {
    try {
      const result = await copyContent({
        ...param,
        projectId: route.params.projectId as string,
      })
      return result
    } catch (error) {
      console.error('Failed to copy content:', error)
      throw error
    }
  }

  /**
   * 另存为模板
   */
  async function saveContentAsTemplate(params: SaveAsTemplateParams) {
    try {
      const result = await saveAsTemplate(route.params.projectId as string, params)
      return result
    } catch (error) {
      console.error('Failed to save content as template:', error)
      throw error
    }
  }

  /**
   * 添加至创作流组
   */
  async function addContentToFlowGroup(params: AddToFlowGroupParams) {
    try {
      const res = await addToFlowGroup(route.params.projectId as string, params)
      return res
    } catch (error) {
      console.error('Failed to add content to flow group:', error)
      throw error
    }
  }

  async function getMatchDynamicData(params: MatchDynamicViewParams) {
    try {
      const result = await getMatchDynamicView(route.params.projectId as string, params)
      return result
    } catch (error) {
      console.error('Failed to get match dynamic view:', error)
      throw error
    }
  }
  async function getProjectTagList() {
    try {
      const result = await getProjectGroups(route.params.projectId as string)
      return result
    } catch (error) {
      console.error('Failed to get project tag list:', error)
      throw error
    }
  }

  async function updateCollect(type: boolean, pipelineId: string) {
    try {
      const result = await toggleFlowFavorite(route.params.projectId as string, pipelineId, type)
      return result
    } catch (error) {
      console.error(error)
      throw error
    }
  }

  /**
   * Update pipeline statuses in-place from a WebSocket push.
   * The payload is a map of { [pipelineId]: statusFields }.
   */
  function updatePipelineStatusFromWs(data: Record<string, Partial<ContentTableItem>>) {
    if (!data || typeof data !== 'object') return
    Object.keys(data).forEach((pipelineId) => {
      const item = flowTableList.value.find((f) => f.pipelineId === pipelineId)
      if (item) {
        Object.assign(item, data[pipelineId])
      }
    })
  }

  return {
    // State
    flowTableList,
    pagination,
    tableLoading,
    isShowAddToDialog,
    isShowCopyDialog,
    isShowSaveAsTemplateDialog,
    currentActionData,
    // Actions
    convertMStoString,
    closeAllDialogs,
    fetchFlowList,
    loadContentDetail,
    removeContent,
    confirmEnableAction,
    restoreFlow,
    copyContentItem,
    saveContentAsTemplate,
    addContentToFlowGroup,
    setDeleteActionCallback,
    setEnableActionCallback,
    getMatchDynamicData,
    getProjectTagList,
    updateCollect,
    updatePipelineStatusFromWs,
  }
})
