import type { TriggerRecordItem } from '@/api/triggerRecord'
import { type StatusType } from '@/types/flow'
import { useI18n } from 'vue-i18n'
import { statusColorMap } from '@/utils/flowStatus'
import { formatDate } from '@/utils/util'
import { Button } from 'bkui-vue'
import { overflowTitle } from 'bkui-vue/lib/directives'
import { storeToRefs } from 'pinia'
import { computed, h, withDirectives } from 'vue'
import { useTriggerRecordStore } from '../stores/triggerRecord'

export interface Styles {
  iconStarBtn: string
  [key: string]: string
}

export function useTriggerRecordData(styles: Styles) {
  const store = useTriggerRecordStore()
  const { t } = useI18n()

  // 使用 storeToRefs 确保响应式
  const { triggerEventList, dateTimeRange, searchValue, shortcuts, filterData, searchPlaceHolder, reTriggerLoadingId } =
    storeToRefs(store)

  // 计算空状态类型
  const emptyStatusType = computed(() => {
    return dateTimeRange.value.length > 0 || searchValue.value.length > 0 ? 'search-empty' : 'empty'
  })

  /**
   * 时间格式化函数
   */
  function convertTime(time: number) {
    return formatDate(time, 'YYYY-MM-DD HH:mm:ss')
  }

  /**
   * 生成timelineList的函数
   * @param list 列表数据
   * @returns 时间轴数据列表
   */
  function generateTimelineList(list: TriggerRecordItem[]) {
    const dateMap = list.reduce((acc, item) => {
      const date = formatDate(item.eventTime, 'YYYY-MM-DD')
      if (!acc.has(date)) {
        acc.set(date, [])
      }
      acc.get(date).push(item)
      return acc
    }, new Map())

    return Array.from(dateMap).map(([date, events]) => ({
      tag: date,
      nodeType: 'vnode',
      content: h(
        'ul',
        { class: styles['trigger-event-list'] },
        events.map((event: any, index: number) => {
          const statusColor = statusColorMap[event.status as StatusType] || '#C4C6CC'

          return h(
            'li',
            {
              key: index,
              class: styles['trigger-event-item'],
            },
            [
              // 状态指示器
              h(
                'span',
                {
                  class: styles['trigger-event-item-indicator'],
                  style: `background: ${statusColor}29`,
                },
                [h('i', { style: `background: ${statusColor}` })],
              ),

              // 事件描述
              h(
                'p',
                { class: styles['trigger-event-desc'] },
                withDirectives(
                  h('p', { class: 'text-ellipsis' }, [
                    h('span', { innerHTML: event.eventDesc }),
                    h('span', convertTime(event.eventTime)),
                  ]),
                  [[overflowTitle, { type: 'tips' }]],
                ),
              ),

              // 触发原因和构建信息
              h(
                'p',
                { class: styles['trigger-event-reason'] },
                [
                  event.reason && h('span', event.reason),
                  event.reason && ' | ',
                  event.buildNum && h('em', { innerHTML: event.buildNum }),
                  !event.buildNum &&
                    Array.isArray(event.reasonDetailList) &&
                    h(
                      'em',
                      withDirectives(
                        h('span', { class: 'text-ellipsis' }, event.reasonDetailList.join(' | ')),
                        [[overflowTitle, { type: 'tips' }]],
                      ),
                    ),
                ].filter(Boolean),
              ),

              // 重新触发按钮
              (event.eventType !== 'TIME_TRIGGER' &&
                h(
                Button,
                {
                  text: true,
                  size: 'small',
                  theme: 'primary',
                  loading: reTriggerLoadingId.value === event.detailId,
                  onClick: () => store.triggerEvent(event),
                },
                t('flow.triggerRecord.retrigger'),
              )),
            ],
          )
        }),
      ),
    }))
  }

  return {
    // 数据
    triggerEventList,
    dateTimeRange,
    searchValue,
    shortcuts,
    filterData,
    searchPlaceHolder,
    emptyStatusType,

    generateTimelineList,

    init: store.init,
    fetchTriggerEventList: store.fetchTriggerEventList,
    handleFilterChange: store.handleFilterChange,
    handleClearSearch: store.handleClearSearch,
    handleClearCalendar: store.handleClearCalendar,
    handleSearchChange: store.handleSearchChange,
  }
}
