import type { TriggerRecordItem, QueryListFunction } from '@/api/triggerRecord'
import { type StatusType } from '@/types/flow'
import { useI18n } from 'vue-i18n'
import { statusColorMap } from '@/utils/flowStatus'
import { formatDate } from '@/utils/util'
import { Button } from 'bkui-vue'
import { overflowTitle } from 'bkui-vue/lib/directives'
import { storeToRefs } from 'pinia'
import { computed, h, withDirectives } from 'vue'
import { useTriggerRecordStore } from '../stores/triggerRecord'
import EventDesc from '@/views/Flow/Detail/TriggerRecord/EventDesc'
import {
  BUILD_NUM_LINK_REG,
  safeUrl,
  toText,
} from '@/views/Flow/Detail/TriggerRecord/eventDescConfig'

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
   * 渲染 buildNum：后端历史会下发 `<a href="..." target="_blank">xxx</a>` 这样的 HTML 串，
   * 这里通过 BUILD_NUM_LINK_REG 解析出 href/text，再用 safeUrl 校验后用 vnode 渲染，
   * 避免 v-html / innerHTML 导致的 XSS 风险。校验失败则降级为纯文本。
   */
  function renderBuildNum(buildNum: string) {
    const match = toText(buildNum).match(BUILD_NUM_LINK_REG)
    if (match) {
      const href = safeUrl(match[1])
      if (href) {
        return h(
          'a',
          {
            class: 'text-link',
            href,
            target: '_blank',
            rel: 'noopener noreferrer',
          },
          match[2],
        )
      }
      return match[2]
    }
    return buildNum
  }

  /**
   * 生成timelineList的函数
   * @param list 列表数据
   * @param queryList 刷新列表数据的方法
   * @returns 时间轴数据列表
   */
  function generateTimelineList(list: TriggerRecordItem[], queryList?: QueryListFunction) {
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
                    h(EventDesc, { eventDesc: event.eventDesc }),
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
                  event.buildNum && h('em', renderBuildNum(event.buildNum)),
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
                  onClick: async () => {
                    const success = await store.triggerEvent(event)
                    if (success && queryList) {
                      queryList(1, undefined, true)
                    }
                  },
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
