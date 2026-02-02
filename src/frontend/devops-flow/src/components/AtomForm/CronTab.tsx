import CronTab from '@blueking/crontab'
import '@blueking/crontab/vue3/vue3.css'
import { computed, defineComponent, type PropType } from 'vue'
import { useI18n } from 'vue-i18n'

export default defineComponent({
  name: 'CronTab',
  props: {
    value: {
      type: Array as PropType<string[]>,
      default: () => [],
    },
    name: {
      type: String,
      required: true,
    },
    handleChange: {
      type: Function,
      default: () => () => {},
    },
    disabled: {
      type: Boolean,
      default: false,
    },
  },
  emits: ['change', 'update:value'],
  setup(props, { emit }) {
    const { locale } = useI18n()

    const cronLocale = computed(() =>
      locale.value?.toLowerCase().startsWith('zh') ? 'zh-CN' : 'en',
    )

    const cron = computed(() => {
      if (Array.isArray(props.value)) {
        return props.value.join('')
      }
      return props.value || ''
    })

    const handleCronChange = (val: string) => {
      emit('update:value', val)
      emit('change', val)
      props.handleChange(props.name, val)
    }

    return () => (
      <CronTab
        modelValue={cron.value}
        onUpdate:modelValue={handleCronChange}
        local={cronLocale.value}
      />
    )
  },
})
