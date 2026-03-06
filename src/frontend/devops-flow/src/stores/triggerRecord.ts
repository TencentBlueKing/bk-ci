import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import { useI18n } from 'vue-i18n'
import { Message } from 'bkui-vue'
import { weekAgo } from '@/utils/util'
import {
  getTriggerRecords,
  getTriggerTypes,
  getEventTypes,
  reTriggerEvent,
  type TriggerRecordItem,
  type TriggerRecordParams,
  type TriggerRecordListResponse,
  type QueryListFunction,
  type TypeItem,
} from '@/api/triggerRecord'
import { useRoute } from 'vue-router'

interface SearchItemValue {
  id: string
  name: string
}

interface SearchItem {
  id: string
  name: string
  values?: SearchItemValue[]
}

export const useTriggerRecordStore = defineStore('triggerRecord', () => {
  const { t } = useI18n()
  const router = useRoute()

  const loading = ref(false)
  const reTriggerLoadingId = ref<string | null>(null)
  const triggerEventList = ref<TriggerRecordItem[]>([])
  const triggerTypeList = ref<TypeItem[]>([])
  const eventTypeList = ref<TypeItem[]>([])

  const DEFAULT_DATE_RANGE = weekAgo() as [Date, Date]
  const dateTimeRange = ref<[Date, Date] | []>([...DEFAULT_DATE_RANGE])
  const searchValue = ref<SearchItem[]>([])

  const shortcuts = computed(() => [
    {
      text: t('flow.triggerRecord.today'),
      value() {
        const end = new Date()
        const start = new Date(end.getFullYear(), end.getMonth(), end.getDate())
        return [start, end]
      },
    },
    {
      text: t('flow.triggerRecord.yesterday'),
      value() {
        const time = new Date()
        const end = new Date(time.getFullYear(), time.getMonth(), time.getDate() - 1, 23, 59, 59)
        const start = new Date(time.getFullYear(), time.getMonth(), time.getDate() - 1)
        return [start, end]
      },
    },
    {
      text: t('flow.triggerRecord.last3days'),
      value() {
        const end = new Date()
        const start = new Date()
        start.setTime(start.getTime() - 3600 * 1000 * 24 * 3)
        return [start, end]
      },
    },
    {
      text: t('flow.triggerRecord.last7days'),
      value() {
        const end = new Date()
        const start = new Date()
        start.setTime(start.getTime() - 3600 * 1000 * 24 * 7)
        return [start, end]
      },
    },
  ])
  const filterData = computed(() => [
    {
      name: t('flow.triggerRecord.triggerType'),
      id: 'triggerType',
      children: triggerTypeList.value.map((item) => ({
        id: item.id,
        name: item.value,
      })),
    },
    {
      name: t('flow.triggerRecord.eventType'),
      id: 'eventType',
      multiple: true,
      children: eventTypeList.value.map((item) => ({
        id: item.id,
        name: item.value,
      })),
    },
    {
      name: t('flow.triggerRecord.trigger'),
      id: 'triggerUser',
    },
  ])

  const searchPlaceHolder = computed(() => {
    return filterData.value.map((item) => item.name).join('/')
  })

  /**
   * 过滤参数获取
   */
  function getSearchQuery(): Partial<TriggerRecordParams> {
    return searchValue.value.reduce(
      (acc: Partial<TriggerRecordParams>, item) => {
        if (item.values?.length) {
          const allowedFields: (keyof TriggerRecordParams)[] = [
            'eventType',
            'triggerUser',
            'triggerType',
          ]

          if (allowedFields.includes(item.id as keyof TriggerRecordParams)) {
            const key = item.id as keyof TriggerRecordParams
            acc[key] = item.values.map((value) => value.id).join(',') as any
          }
        }
        return acc
      },
      {
        startTime: dateTimeRange.value[0] ? +new Date(dateTimeRange.value[0]) : undefined,
        endTime: dateTimeRange.value[1] ? +new Date(dateTimeRange.value[1]) : undefined,
      },
    )
  }

  /**
   * 加载触发记录列表
   */
  async function fetchTriggerEventList(
    page: number,
    pageSize: number,
  ): Promise<TriggerRecordListResponse> {
    loading.value = true
    try {
      const params: TriggerRecordParams = {
        projectId: router.params.projectId as string,
        pipelineId: router.params.flowId as string,
        ...getSearchQuery(),
        page,
        pageSize,
      }

      const response = await getTriggerRecords(params)
      return {
        records: response?.records || [],
        count: response?.count || 0,
      }
    } catch (error) {
      console.error('加载触发记录失败:', error)
      return {
        records: [],
        count: 0,
      }
    } finally {
      loading.value = false
    }
  }

  /**
   * 过滤变更处理
   */
  const handleFilterChange = (queryList: QueryListFunction) => {
    queryList(1)
  }

  /**
   * 清空搜索条件
   */
  const handleClearSearch = (queryList: QueryListFunction) => {
    searchValue.value = []
    dateTimeRange.value = []
    queryList(1)
  }

  /**
   * 清空日历搜索条件
   */
  const handleClearCalendar = (queryList: QueryListFunction) => {
    dateTimeRange.value = []
    queryList(1)
  }

  const handleSearchChange = (value: SearchItem[], queryList: QueryListFunction) => {
    searchValue.value = value
    queryList(1)
  }

  /**
   * 获取触发器类型和事件类型数据
   */
  async function init() {
    try {
      const [triggerTypes, eventTypes] = await Promise.all([getTriggerTypes(), getEventTypes()])

      triggerTypeList.value = triggerTypes
      eventTypeList.value = eventTypes
    } catch (error) {
      console.error('获取触发器类型和事件类型失败:', error)
      return {
        triggerTypes: [],
        eventTypes: [],
      }
    }
  }

  async function triggerEvent(event: any) {
   try {
      reTriggerLoadingId.value = event.detailId
      const res = await reTriggerEvent(router.params.projectId as string, event.detailId)
      reTriggerLoadingId.value = null
      if (res) {
        Message({ theme: 'success', message: t('flow.triggerRecord.retrigger') + t('flow.common.success') })
        fetchTriggerEventList(1, 24)
      }
   } catch (error) {
      console.error('触发失败:', error)
      reTriggerLoadingId.value = null
   }
  }

  return {
    // 状态
    triggerEventList,
    dateTimeRange,
    searchValue,
    shortcuts,
    filterData,
    searchPlaceHolder,
    reTriggerLoadingId,

    // 方法
    fetchTriggerEventList,
    init,
    handleClearCalendar,
    handleFilterChange,
    handleClearSearch,
    handleSearchChange,
    triggerEvent,
  }
})
