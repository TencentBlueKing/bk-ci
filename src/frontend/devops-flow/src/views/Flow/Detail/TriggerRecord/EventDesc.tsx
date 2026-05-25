import { computed, defineComponent, h, type PropType } from 'vue'
import { I18nT } from 'vue-i18n'
import {
  EVENT_DESC_PARAM_MAPPERS,
  EVENT_DESC_SLOT_NAMES,
  normalizeEventDesc,
  safeUrl,
  type RawEventDesc,
  type SlotData,
} from './eventDescConfig'
import styles from './TriggerRecord.module.css'

/** flow 服务的 i18n 命名空间前缀（与 src/frontend/locale/flow/*.json 的根节点一致） */
const I18N_PREFIX = 'flow.triggerRecord.eventDesc.'

/**
 * 单个 slot 的渲染：
 *   - link 且 href 安全 → <a target="_blank" rel="noopener noreferrer">
 *   - user → 带 trigger-user 样式的 <span>
 *   - 其他 → 普通 <span>
 */
function renderSlot(slotData: SlotData) {
  if (slotData.type === 'link' && slotData.href) {
    return h(
      'a',
      {
        class: 'text-link',
        href: slotData.href,
        target: '_blank',
        rel: 'noopener noreferrer',
      },
      slotData.text,
    )
  }
  return h(
    'span',
    {
      class: slotData.type === 'user' ? styles['trigger-user'] : undefined,
    },
    slotData.text || '',
  )
}

/**
 * 触发事件描述渲染组件。
 *
 * 接收后端下发的 `eventDesc`（对象 / JSON 字符串 / 普通字符串），
 * 通过 i18n + 命名插槽的方式安全渲染，避免 v-html 带来的 XSS 风险，
 * 同时支持多语言。
 *
 * 参考 PR #12987 的 `EventDesc.vue`，按 Vue3 + vue-i18n v9 的 `<I18nT>` 重写。
 */
export default defineComponent({
  name: 'EventDesc',
  props: {
    eventDesc: {
      type: [Object, String, null] as unknown as PropType<RawEventDesc>,
      default: () => ({}),
    },
  },
  setup(props) {
    const normalized = computed(() => normalizeEventDesc(props.eventDesc))

    const mapper = computed(() => EVENT_DESC_PARAM_MAPPERS[normalized.value.code])

    /** 完整的 i18n keypath（含命名空间），无对应映射时为空 */
    const descKey = computed(() =>
      mapper.value ? `${I18N_PREFIX}${normalized.value.code}` : '',
    )

    /** 本次事件实际用到的所有 slot 数据；链接走 safeUrl 校验，校验失败降级为纯文本 */
    const slotMap = computed<Record<string, SlotData>>(() => {
      if (!mapper.value) return {}
      const raw = mapper.value(normalized.value.params || [])
      return Object.entries(raw).reduce<Record<string, SlotData>>((acc, [name, param]) => {
        if (param.type === 'link') {
          const href = safeUrl(param.href)
          acc[name] = href ? { ...param, href } : { type: 'text', text: param.text }
        } else {
          acc[name] = param
        }
        return acc
      }, {})
    })

    return () => {
      // 没有可识别的 code，直接显示 defaultMessage 兜底
      if (!descKey.value) {
        return h('span', normalized.value.defaultMessage || '')
      }

      // 仅为本事件用到的占位符注册命名插槽，避免无关 slot 影响渲染
      const slots: Record<string, () => ReturnType<typeof h>> = {}
      EVENT_DESC_SLOT_NAMES.forEach((slotName) => {
        const slotData = slotMap.value[slotName]
        if (slotData) {
          slots[slotName] = () => renderSlot(slotData)
        }
      })

      return h(
        I18nT,
        {
          keypath: descKey.value,
          tag: 'span',
          class: 'trigger-event-desc-content',
        },
        slots,
      )
    }
  },
})
