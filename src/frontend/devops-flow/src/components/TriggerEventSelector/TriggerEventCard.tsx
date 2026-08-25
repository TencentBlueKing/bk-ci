import type { TriggerBaseItem } from '@/api/trigger'
import { SvgIcon } from '@/components/SvgIcon'
import { Loading } from 'bkui-vue'
import { computed, defineComponent, type PropType } from 'vue'
import styles from './TriggerEventCard.module.css'

export default defineComponent({
  name: 'TriggerEventCard',
  props: {
    eventAtom: {
      type: Object as PropType<TriggerBaseItem>,
      required: true,
    },
    compact: {
      type: Boolean,
      default: false,
    },
    loading: {
      type: Boolean,
      default: false,
    },
    disabled: {
      type: Boolean,
      default: false,
    },
    keyword: {
      type: String,
      default: '',
    },
  },
  emits: ['click'],
  setup(props, { emit }) {
    // 获取事件图标名称
    const eventAtomLogo = computed(() => {
      // 根据atomCode或classifyCode返回对应的图标

      return 'placeholder'
    })

    // 格式化使用次数
    const formatUsageCount = (count?: number) => {
      if (!count) return '0'
      if (count >= 1000) {
        return `${(count / 1000).toFixed(1)}k`
      }
      return count.toLocaleString()
    }

    const handleClick = () => {
      if (!props.loading && !props.disabled) {
        emit('click')
      }
    }

    // 高亮搜索关键字
    const renderHighlightText = (text: string) => {
      const keyword = props.keyword.trim()
      if (!keyword || !text) return text

      const escaped = keyword.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
      const parts = text.split(new RegExp(`(${escaped})`, 'gi'))
      return parts.map((part, index) =>
        part.toLowerCase() === keyword.toLowerCase() ? (
          <span key={index} class={styles.highlight}>
            {part}
          </span>
        ) : (
          part
        ),
      )
    }

    return () => (
      <div
        class={[
          styles.eventCard,
          props.loading && styles.eventCardLoading,
          props.disabled && styles.eventCardDisabled,
        ]}
        onClick={handleClick}
      >
        <div class={styles.eventIcon}>
          {props.eventAtom.logoUrl ? (
            <img src={props.eventAtom.logoUrl} alt={props.eventAtom.name} />
          ) : (
            <SvgIcon name={eventAtomLogo.value} size={48} />
          )}
        </div>

        <div class={styles.eventInfo}>
          <div class={styles.eventName}>
            <span class={styles.eventNameText}>
              {renderHighlightText(props.eventAtom.name)}
            </span>
            {props.loading && <Loading mode="spin" size="mini" class={styles.loadingSpin} />}
          </div>
          <div class={styles.eventDesc}>{renderHighlightText(props.eventAtom.summary || '')}</div>
          <div class={styles.eventMeta}>
            <span class={styles.eventRating}>
              <SvgIcon name="star" size={12} class={styles.starIcon} />
              {props.eventAtom.score?.toFixed(1) || '4.5'}
            </span>
            <span class={styles.eventUsage}>
              {formatUsageCount(props.eventAtom.recentExecuteNum)}
            </span>
          </div>
        </div>
      </div>
    )
  },
})
