import type { ConditionOptionItem, ErrorInfoItem, ExecutionRecord } from '@/api/executionRecord'
import {
  getHistoryConditions,
  HistoryConditionType,
  updateBuildRemark,
} from '@/api/executionRecord'
import { retryFlow } from '@/api/executeDetail'
import StageSteps from '@/components/StageSteps'
import StatusIcon from '@/components/StatusIcon'
import { SvgIcon } from '@/components/SvgIcon'
import { ROUTE_NAMES } from '@/constants/routes'
import { useExecutionRecordData } from '@/hooks/useExecutionRecordData'
import { useExecutionRecordStore } from '@/stores/executionRecord'
import { errorTypeMap } from '@/utils/flowConst'
import { statusColorMap } from '@/utils/flowStatus'
import SearchSelect from '@blueking/search-select-v3'
import { Button, DatePicker, Dialog, Exception, Input, Loading, Message, Popover, Table } from 'bkui-vue'
import type { Column } from 'bkui-vue/lib/table/props'
import { computed, defineComponent, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import styles from './ExecutionRecord.module.css'

// localStorage key for column settings
const LS_COLUMN_KEY = 'flow_execution_record_columns'

// 执行记录表格列配置（公共配置）
export const EXECUTION_RECORD_COLUMNS = [
  { field: 'buildNo', labelKey: 'flow.content.buildNo', disabled: true },
  { field: 'stageStatus', labelKey: 'flow.content.stageStatus' },
  { field: 'nodeName', labelKey: 'flow.content.workflowNode' },
  { field: 'triggerAndUser', labelKey: 'flow.content.triggerMethodAndUser' },
  { field: 'triggerTime', labelKey: 'flow.content.triggerTime' },
  { field: 'startTime', labelKey: 'flow.content.executionStartTime' },
  { field: 'endTime', labelKey: 'flow.content.executionEndTime' },
  { field: 'totalDuration', labelKey: 'flow.content.totalDuration' },
  { field: 'executionDuration', labelKey: 'flow.content.executionDuration' },
  { field: 'remark', labelKey: 'flow.content.remark' },
  { field: 'errorCode', labelKey: 'flow.content.errorCode' },
] as const

// Default visible columns (使用 field 值)
const DEFAULT_COLUMN_KEYS = EXECUTION_RECORD_COLUMNS.map((col) => col.field)

// Constants for Search Keys
const SEARCH_KEY = {
  /** 触发方式 */
  TRIGGER_METHOD: 'triggerMethod',
  /** 触发事件 */
  TRIGGER_EVENT: 'triggerEventTypes',
  /** 触发人 */
  TRIGGER_USER: 'triggerUser',
  /** 触发节点（工作流节点） */
  TRIGGER_NODE: 'triggerNodeHashIds',
  /** 状态 (保留旧的) */
  STATUS: 'status',
  /** 备注 */
  REMARK: 'remark',
} as const

type SearchValues = {
  id: string
  name: string
  values?: Array<{ id: string; name: string }>
}[]

export default defineComponent({
  name: 'ExecutionRecord',
  setup() {
    // ==================== Composables & Hooks ====================
    const { t } = useI18n()
    const route = useRoute()
    const router = useRouter()
    const store = useExecutionRecordStore()

    const {
      records: tableData,
      pagination,
      loading,
      handlePageChange,
      handleLimitChange,
      updateQueryParams,
      refresh,
    } = useExecutionRecordData()

    // ==================== Computed Properties ====================
    const projectId = computed(() => route.params.projectId as string)
    const flowId = computed(() => route.params.flowId as string)

    // ==================== Reactive References ====================
    // 筛选条件选项列表
    const statusList = ref<any[]>([])
    const triggerMethodList = ref<ConditionOptionItem[]>([])
    const triggerEventList = ref<ConditionOptionItem[]>([])
    const triggerUserList = ref<ConditionOptionItem[]>([])
    const triggerNodeList = ref<ConditionOptionItem[]>([])

    // 列设置相关
    const getInitialColumnKeys = (): string[] => {
      try {
        const saved = localStorage.getItem(LS_COLUMN_KEY)
        if (saved) {
          const parsed = JSON.parse(saved)
          if (Array.isArray(parsed) && parsed.length > 0) {
            return parsed
          }
        }
      } catch (e) {
        console.error('Failed to parse saved column keys:', e)
      }
      return [...DEFAULT_COLUMN_KEYS]
    }
    const selectedColumnKeys = ref<string[]>(getInitialColumnKeys())
    const tableSettings = ref({
      fields: EXECUTION_RECORD_COLUMNS.map((col) => ({
        field: col.field,
        label: t(col.labelKey),
        disabled: 'disabled' in col ? col.disabled : false,
      })),
      checked: [...selectedColumnKeys.value],
      showLineHeight: false,
    })

    // 错误码弹窗相关
    const showErrorDialog = ref(false)
    const activeErrorRecord = ref<ExecutionRecord | null>(null)

    const getErrorTypeInfo = (errorType?: number) => {
      const idx = errorType ?? 0
      return errorTypeMap[idx] ?? errorTypeMap[0]
    }

    const openErrorDialog = (record: ExecutionRecord) => {
      activeErrorRecord.value = record
      showErrorDialog.value = true
    }

    const closeErrorDialog = () => {
      showErrorDialog.value = false
      activeErrorRecord.value = null
    }

    // 重新执行构建相关
    const retryingMap = ref<Record<string, boolean>>({})

    // 搜索和筛选相关
    const dateRange = ref<[Date, Date] | null>(null)
    const searchValue = ref<SearchValues>([])

    // 备注编辑相关
    const remarkEditState = ref<Record<string, { tempRemark: string; saving: boolean }>>({})
    const popoverShowMap = ref<Record<string, boolean>>({})

    // ==================== Computed Properties ====================
    const searchData = computed(() => [
      {
        id: SEARCH_KEY.STATUS,
        name: t('flow.content.status'),
        multiable: true,
        children: statusList.value.map((item) => ({ id: item.id, name: item.value })),
      },
      {
        id: SEARCH_KEY.TRIGGER_METHOD,
        name: t('flow.content.triggerMethod'),
        multiable: true,
        async: true,
        children: triggerMethodList.value.map((item) => ({ id: item.id, name: item.value })),
      },
      {
        id: SEARCH_KEY.TRIGGER_EVENT,
        name: t('flow.content.triggerEvent'),
        multiable: true,
        async: true,
        children: triggerEventList.value.map((item) => ({ id: item.id, name: item.value })),
      },
      {
        id: SEARCH_KEY.TRIGGER_USER,
        name: t('flow.content.triggerUser'),
        async: true,
        children: triggerUserList.value.map((item) => ({ id: item.id, name: item.value })),
      },
      {
        id: SEARCH_KEY.TRIGGER_NODE,
        name: t('flow.content.workflowNode'),
        async: true,
        children: triggerNodeList.value.map((item) => ({ id: item.id, name: item.value })),
      },
      {
        id: SEARCH_KEY.REMARK,
        name: t('flow.content.remark'),
      },
    ])

    const searchPlaceHolder = computed(() => {
      return searchData.value.map((item) => item.name).join('/')
    })

    const hasActiveFilters = computed(() => {
      return searchValue.value.length > 0 || dateRange.value !== null
    })

    const showEmptyState = computed(() => {
      return (
        !loading.value &&
        tableData.value.length === 0 &&
        pagination.value.count === 0 &&
        !hasActiveFilters.value
      )
    })

    const allTableColumns = computed(() => {
      return EXECUTION_RECORD_COLUMNS.map((col) => {
        const baseColumn: any = {
          label: t(col.labelKey),
          field: col.field,
        }

        // 为特定列添加自定义 render 函数
        if (col.field === 'buildNo') {
          baseColumn.render = ({ row }: any) => {
            const record = row as ExecutionRecord
            const status = record.status || 'UNKNOWN'
            const statusColor =
              statusColorMap[status as keyof typeof statusColorMap] || statusColorMap.UNKNOWN

            return (
              <span class={styles.buildNoStatus}>
                <StatusIcon status={status as any} size={14} />
                <span
                  class={styles.buildNo}
                  style={{ color: statusColor }}
                  onClick={() => {
                    router.push({
                      name: ROUTE_NAMES.FLOW_DETAIL_EXECUTION_DETAIL_TAB,
                      params: {
                        flowId: flowId.value,
                        buildNo: record.id,
                      },
                    })
                  }}
                >
                  #{record.buildNo}
                </span>
              </span>
            )
          }
        } else if (col.field === 'stageStatus') {
          baseColumn.render = ({ row }: any) => {
            const record = row as ExecutionRecord
            return renderStageStatus(record.stageStatus, record.id)
          }
        } else if (col.field === 'remark') {
          baseColumn.render = ({ row }: any) => renderRemark(row as ExecutionRecord)
        } else if (col.field === 'errorCode') {
          baseColumn.minWidth = 280
          baseColumn.render = ({ row }: any) => {
            const record = row as ExecutionRecord
            const errorList = record.errorInfoList
            if (!errorList || errorList.length === 0) return <span>--</span>
            return (
              <div class={styles.errorCodeCell}>
                <SvgIcon
                  name="menu-sub"
                  size={14}
                  class={styles.errorCodeIcon}
                  onClick={(e: MouseEvent) => {
                    e.stopPropagation()
                    openErrorDialog(record)
                  }}
                />
                <ul class={styles.errorCodeList}>
                  {errorList.map((item: ErrorInfoItem, index: number) => {
                    const typeInfo = getErrorTypeInfo(item.errorType)
                    return (
                      <li class={styles.errorCodeItem} key={item.taskId ?? index}>
                        <span class={styles.errorCodeLabel} v-overflow-title>{t(typeInfo.title)}</span>
                        <span>({item.errorCode})</span>
                      </li>
                    )
                  })}
                </ul>
              </div>
            )
          }
        }

        return baseColumn
      })
    })

    const operationColumn = computed(() => ({
      label: t('flow.content.operation'),
      field: 'operation',
      width: 100,
      fixed: 'right' as const,
      render: ({ row }: any) => {
        const record = row as ExecutionRecord
        const isRetrying = retryingMap.value[record.id]

        if (!canRetry(record.status)) {
          return <span class={styles.operationDisabled}>--</span>
        }

        return (
          <Button
            text
            theme="primary"
            size="small"
            loading={isRetrying}
            onClick={() => handleRetry(record)}
          >
            {t('flow.content.rebuild')}
          </Button>
        )
      },
    }))

    const tableColumns = computed(() => {
      return [...allTableColumns.value, operationColumn.value]
    })

    // ==================== Methods ====================
    // 工具函数
    const canRetry = (status: string) => {
      return !['RUNNING', 'QUEUE', 'PREPARE_ENV', 'REVIEWING'].includes(status)
    }

    const getRemarkEditState = (rowId: string, initialRemark: string) => {
      if (!remarkEditState.value[rowId]) {
        remarkEditState.value[rowId] = { tempRemark: initialRemark, saving: false }
      }
      return remarkEditState.value[rowId]
    }

    const hidePopover = (rowId: string) => {
      popoverShowMap.value[rowId] = false
    }

    // 远程搜索筛选条件选项
    const searchConditionOptions = async (
      conditionType: HistoryConditionType,
      keyword: string,
    ): Promise<Array<{ id: string; name: string }>> => {
      if (!projectId.value || !flowId.value) return []
      try {
        const options = await getHistoryConditions({
          projectId: projectId.value,
          pipelineId: flowId.value,
          conditionType,
          keyword,
        })
        return options.map((item) => ({ id: item.id, name: item.value }))
      } catch (e) {
        console.error(e)
        return []
      }
    }

    const getMenuList = async (item: any, keyword: string) => {
      switch (item.id) {
        case SEARCH_KEY.TRIGGER_METHOD:
          return await searchConditionOptions(HistoryConditionType.TRIGGER_METHOD, keyword)
        case SEARCH_KEY.TRIGGER_EVENT:
          return await searchConditionOptions(HistoryConditionType.TRIGGER_EVENT, keyword)
        case SEARCH_KEY.TRIGGER_USER:
          return await searchConditionOptions(HistoryConditionType.TRIGGER_USER, keyword)
        case SEARCH_KEY.TRIGGER_NODE:
          return await searchConditionOptions(HistoryConditionType.TRIGGER_NODE, keyword)
        default:
          return []
      }
    }

    // 事件处理函数
    const handleExecuteNow = () => {
      router.push({
        name: ROUTE_NAMES.FLOW_PREVIEW,
        params: {
          projectId: projectId.value,
          flowId: flowId.value,
        },
      })
    }

    const handleSettingChange = (settings: { checked: string[] }) => {
      selectedColumnKeys.value = settings.checked
      tableSettings.value.checked = settings.checked
      localStorage.setItem(LS_COLUMN_KEY, JSON.stringify(settings.checked))
    }

    const handleRetry = async (row: ExecutionRecord) => {
      if (retryingMap.value[row.id]) return

      try {
        retryingMap.value[row.id] = true
        await retryFlow({
          projectId: projectId.value,
          pipelineId: flowId.value,
          buildId: row.id,
        })
        Message({
          theme: 'success',
          message: t('flow.content.retrySuccess'),
        })
        refresh()
      } catch (e: any) {
        Message({
          theme: 'error',
          message: e.message || t('flow.content.retryFailed'),
        })
      } finally {
        retryingMap.value[row.id] = false
      }
    }

    const handleSearchChange = (value: SearchValues) => {
      searchValue.value = value

      const params: Record<string, string | string[] | undefined> = {
        [SEARCH_KEY.STATUS]: undefined,
        [SEARCH_KEY.TRIGGER_METHOD]: undefined,
        [SEARCH_KEY.TRIGGER_EVENT]: undefined,
        [SEARCH_KEY.TRIGGER_USER]: undefined,
        [SEARCH_KEY.TRIGGER_NODE]: undefined,
        [SEARCH_KEY.REMARK]: undefined,
      }

      value.forEach((item) => {
        if (item.id === SEARCH_KEY.STATUS && item.values) {
          params[SEARCH_KEY.STATUS] = item.values.map((v) => v.id)
        } else if (item.id === SEARCH_KEY.TRIGGER_METHOD && item.values) {
          params[SEARCH_KEY.TRIGGER_METHOD] = item.values.map((v) => v.id)
        } else if (item.id === SEARCH_KEY.TRIGGER_EVENT && item.values) {
          params[SEARCH_KEY.TRIGGER_EVENT] = item.values.map((v) => v.id)
        } else if (item.id === SEARCH_KEY.TRIGGER_USER && item.values) {
          if (item.values.length > 0) {
            params[SEARCH_KEY.TRIGGER_USER] = item.values[0]!.id
          }
        } else if (item.id === SEARCH_KEY.TRIGGER_NODE && item.values) {
          if (item.values.length > 0) {
            params[SEARCH_KEY.TRIGGER_NODE] = item.values[0]!.id
          }
        } else if (item.id === SEARCH_KEY.REMARK && item.values) {
          if (item.values.length > 0) {
            params[SEARCH_KEY.REMARK] = item.values[0]!.id
          }
        }
      })

      updateQueryParams(params)
      refresh()

      const query = { ...route.query, ...params }
      Object.keys(params).forEach((key) => {
        if (params[key] === undefined) {
          delete query[key]
        }
      })
      router.replace({ query })
    }

    const handleSearch = () => {
      // 搜索逻辑已在 handleSearchChange 中处理
    }

    const handleDateClear = () => {
      dateRange.value = null
      updateQueryParams({
        startTimeStartTime: '',
        endTimeEndTime: '',
      })
      refresh()

      const query = { ...route.query }
      delete query.startTimeStartTime
      delete query.endTimeEndTime
      router.replace({ query })
    }

    const handleRemarkSave = async (row: ExecutionRecord, hidePopover: () => void) => {
      const state = remarkEditState.value[row.id]
      if (!state || state.saving) return

      const preRemark = row.remark
      try {
        if (state.tempRemark !== row.remark) {
          state.saving = true
          row.remark = state.tempRemark
          await updateBuildRemark(projectId.value, flowId.value, row.id, state.tempRemark)
          Message({
            theme: 'success',
            message: t('flow.common.success'),
          })
        }
        hidePopover()
      } catch (e) {
        console.error(e)
        Message({
          theme: 'error',
          message: t('flow.common.failed'),
        })
        row.remark = preRemark
      } finally {
        state.saving = false
      }
    }

    // 渲染函数
    const renderStageStatus = (stages: ExecutionRecord['stageStatus'], buildId: string) => {
      const stageWithoutTrigger = stages.slice(1)
      if (!stageWithoutTrigger || stageWithoutTrigger.length === 0) {
        return <span>--</span>
      }
      return <StageSteps steps={stageWithoutTrigger} buildId={buildId} />
    }

    const renderRemark = (row: ExecutionRecord) => {
      return (
        <Popover
          trigger="click"
          placement="bottom-start"
          theme="light"
          width={300}
          arrow={false}
          extCls="remark-edit-popover"
          isShow={popoverShowMap.value[row.id] || false}
          onAfterShow={() => {
            popoverShowMap.value[row.id] = true
            remarkEditState.value[row.id] = { tempRemark: row.remark || '', saving: false }
          }}
          onAfterHidden={() => {
            popoverShowMap.value[row.id] = false
          }}
        >
          {{
            default: () => (
              <div class={styles.remarkCell}>
                <span class={styles.remarkText} title={row.remark || ''}>
                  {row.remark || '--'}
                </span>
                <SvgIcon name="edit" size={14} class={styles.remarkEdit} />
              </div>
            ),
            content: () => {
              const state = getRemarkEditState(row.id, row.remark || '')
              return (
                <div class={styles.remarkPopover}>
                  <div class={styles.remarkPopoverTitle}>{t('flow.content.editRemark')}</div>
                  <Input
                    type="textarea"
                    rows={4}
                    maxlength={4096}
                    disabled={state.saving}
                    v-model={state.tempRemark}
                    placeholder={t('flow.content.remarkPlaceholder')}
                  />
                  <div class={styles.remarkPopoverActions}>
                    <span class={styles.remarkPopoverCancel} onClick={() => hidePopover(row.id)}>
                      {t('flow.common.cancel')}
                    </span>
                    <span
                      class={[styles.remarkPopoverConfirm, state.saving && styles.disabled]}
                      onClick={() => handleRemarkSave(row, () => hidePopover(row.id))}
                    >
                      {state.saving ? t('flow.common.saving') : t('flow.common.save')}
                    </span>
                  </div>
                </div>
              )
            },
          }}
        </Popover>
      )
    }

    // ==================== Lifecycle Hooks ====================
    // 初始化：从 URL query 恢复搜索条件
    const {
      startTimeStartTime,
      endTimeEndTime,
      status,
      triggerMethod,
      triggerEvent,
      triggerUser,
      triggerNode,
      remark,
    } = route.query

    if (startTimeStartTime && endTimeEndTime) {
      dateRange.value = [new Date(Number(startTimeStartTime)), new Date(Number(endTimeEndTime))]
    }

    const initialSearchValue: any[] = []
    const restoreSearchItem = (key: string, id: string, isMulti = false) => {
      const value = route.query[key]
      if (value) {
        const item = searchData.value.find((d) => d.id === id)
        if (item) {
          if (isMulti) {
            const list = Array.isArray(value) ? value : [value]
            const values = (list as string[]).map((v) => {
              const option = item.children?.find((c) => c.id === v)
              return { id: v, name: option?.name || v }
            })
            initialSearchValue.push({ id, name: item.name, values })
          } else {
            initialSearchValue.push({
              id,
              name: item.name,
              values: [{ id: value as string, name: value as string }],
            })
          }
        }
      }
    }

    restoreSearchItem(SEARCH_KEY.STATUS, SEARCH_KEY.STATUS, true)
    restoreSearchItem(SEARCH_KEY.TRIGGER_METHOD, SEARCH_KEY.TRIGGER_METHOD, true)
    restoreSearchItem(SEARCH_KEY.TRIGGER_EVENT, SEARCH_KEY.TRIGGER_EVENT, true)
    restoreSearchItem(SEARCH_KEY.TRIGGER_USER, SEARCH_KEY.TRIGGER_USER)
    restoreSearchItem(SEARCH_KEY.TRIGGER_NODE, SEARCH_KEY.TRIGGER_NODE)
    restoreSearchItem(SEARCH_KEY.REMARK, SEARCH_KEY.REMARK)

    if (initialSearchValue.length > 0) {
      searchValue.value = initialSearchValue
    }

    if (Object.keys(route.query).length > 0) {
      store.setQueryParams({
        startTimeStartTime: startTimeStartTime as string,
        endTimeEndTime: endTimeEndTime as string,
        status: Array.isArray(status) ? status : status ? [status] : undefined,
        triggerMethod: Array.isArray(triggerMethod)
          ? triggerMethod
          : triggerMethod
            ? [triggerMethod]
            : undefined,
        triggerEvent: Array.isArray(triggerEvent)
          ? triggerEvent
          : triggerEvent
            ? [triggerEvent]
            : undefined,
        triggerUser: triggerUser as string,
        triggerNode: triggerNode as string,
        remark: remark as string,
      } as any)
    }

    onMounted(async () => {
      if (projectId.value && flowId.value) {
        try {
          const [triggerMethods, triggerEvents, triggerUsers, triggerNodes] = await Promise.all([
            getHistoryConditions({
              projectId: projectId.value,
              pipelineId: flowId.value,
              conditionType: HistoryConditionType.TRIGGER_METHOD,
            }),
            getHistoryConditions({
              projectId: projectId.value,
              pipelineId: flowId.value,
              conditionType: HistoryConditionType.TRIGGER_EVENT,
            }),
            getHistoryConditions({
              projectId: projectId.value,
              pipelineId: flowId.value,
              conditionType: HistoryConditionType.TRIGGER_USER,
            }),
            getHistoryConditions({
              projectId: projectId.value,
              pipelineId: flowId.value,
              conditionType: HistoryConditionType.TRIGGER_NODE,
            }),
          ])
          triggerMethodList.value = triggerMethods
          triggerEventList.value = triggerEvents
          triggerUserList.value = triggerUsers
          triggerNodeList.value = triggerNodes
        } catch (e) {
          console.error(e)
        }
      }
    })

    // ==================== Watchers ====================
    watch(dateRange, (newRange) => {
      let storeParams: any = {}
      let urlQueryParams: any = {}
      if (newRange && newRange.length === 2) {
        const startTimeValue = String(newRange[0].getTime())
        const endTimeValue = String(newRange[1].getTime())
        storeParams = {
          startTimeStartTime: startTimeValue,
          endTimeEndTime: endTimeValue,
        }
        urlQueryParams = {
          startTimeStartTime: startTimeValue,
          endTimeEndTime: endTimeValue,
        }
      } else {
        storeParams = {
          startTimeStartTime: '',
          endTimeEndTime: '',
        }
        urlQueryParams = {
          startTimeStartTime: undefined,
          endTimeEndTime: undefined,
        }
      }
      updateQueryParams(storeParams)
      refresh()

      const query = { ...route.query, ...urlQueryParams }
      if (!urlQueryParams.startTimeStartTime) {
        delete query.startTimeStartTime
        delete query.endTimeEndTime
      }
      router.replace({ query })
    })

    return () => (
      <div class={styles.executionRecord}>
        {showEmptyState.value ? (
          <div class={styles.emptyState}>
            <Exception type="empty">
              {{
                default: () => (
                  <>
                    <div class={styles.emptyTitle}>{t('flow.content.noExecutionRecord')}</div>
                    <div class={styles.emptyDescBox}>
                      <p class={styles.emptyDescText}>{t('flow.content.noExecutionRecordDesc1')}</p>
                      <p class={styles.emptyDescText}>{t('flow.content.noExecutionRecordDesc2')}</p>
                    </div>
                    <Button theme="primary" onClick={handleExecuteNow}>
                      {t('flow.content.executeNow')}
                    </Button>
                  </>
                ),
              }}
            </Exception>
          </div>
        ) : (
          <>
            {/* 筛选区域 */}
            <div class={styles.filterBar}>
              <div class={styles.filterLeft}>
                <DatePicker
                  type="daterange"
                  placeholder={t('flow.content.selectStartTimeRange')}
                  v-model={dateRange.value}
                  class={styles.datePicker}
                  clearable
                  onClear={handleDateClear}
                />
              </div>
              <div class={styles.filterRight}>
                <SearchSelect
                  modelValue={searchValue.value}
                  data={searchData.value}
                  unique-select
                  placeholder={searchPlaceHolder.value}
                  class={styles.searchInput}
                  getMenuList={getMenuList}
                  onUpdate:modelValue={handleSearchChange}
                  onSearch={handleSearch}
                />
              </div>
            </div>

            {/* Table area */}
            <Loading
              class={styles.tableWrapper}
              loading={loading.value}
              mode="spin"
              theme="primary"
              size="small"
            >
              <Table
                data={tableData.value}
                columns={tableColumns.value as Column[]}
                class={styles.table}
                settings={tableSettings.value}
                pagination={pagination.value}
                border="outer"
                remotePagination
                onPageValueChange={handlePageChange}
                onPageLimitChange={handleLimitChange}
                onSettingChange={handleSettingChange}
              />
            </Loading>
          </>
        )}

        <Dialog
          isShow={showErrorDialog.value}
          title={`#${activeErrorRecord.value?.buildNo ?? ''} - ${t('flow.content.errorCode')}`}
          width={640}
          onClosed={closeErrorDialog}
        >
          {{
            default: () => (
              <ul class={styles.errorInfoList}>
                {activeErrorRecord.value?.errorInfoList.map((item: ErrorInfoItem, index: number) => {
                  const typeInfo = getErrorTypeInfo(item.errorType)
                  return (
                    <li key={item.taskId ?? index} class={styles.errorInfoItem}>
                      <SvgIcon name={typeInfo.icon} size={18} class={styles.errorInfoIcon} />
                      <p class={styles.errorInfoText} v-overflow-title>
                        {t(typeInfo.title)} (<b>{item.errorCode}</b>)
                        {item.errorMsg ? `: ${item.errorMsg}` : ''}
                      </p>
                    </li>
                  )
                })}
              </ul>
            ),
            footer: () => (
              <Button onClick={closeErrorDialog}>{t('flow.common.close')}</Button>
            ),
          }}
        </Dialog>
      </div>
    )
  },
})
