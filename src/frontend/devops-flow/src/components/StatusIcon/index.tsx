import { SvgIcon } from '@/components/SvgIcon'
import { STATUS, type StatusType } from '@/types/flow'
import { statusColorMap, statusIconMap } from '@/utils/flowStatus'
import { computed, defineComponent } from 'vue'
import styles from './StatusIcon.module.css'

type StatusKey = keyof typeof statusIconMap
function isValidStatus(status: string): status is StatusKey {
  return status in statusIconMap
}

export default defineComponent({
  name: 'StatusIcon',
  components: {
    SvgIcon,
  },
  props: {
    status: {
      type: String as () => StatusType,
      default: '',
    },
    size: {
      type: [Number, String],
      default: 16,
    },
  },
  setup(props) {
    const logoName = computed(() => {
      const status = props.status || STATUS.UNEXEC
      return isValidStatus(status) ? statusIconMap[status] : statusIconMap.UNKNOWN
    })
    const isRunning = computed(() => logoName.value === 'circle-2-1')
    const isEnqueue = computed(() => logoName.value === 'hourglass')

    return () => (
      <span
        class={[
          styles.statusIcon,
          isRunning.value ? 'spinIcon' : '',
          isEnqueue.value ? styles.hourglassQueue : '',
        ]}
        style={{ color: statusColorMap[props.status || STATUS.UNEXEC] }}
      >
        <SvgIcon name={logoName.value} size={props.size} />
      </span>
    )
  },
})
