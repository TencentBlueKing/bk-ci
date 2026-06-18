import CronTab from '@blueking/crontab'
import '@blueking/crontab/vue3/vue3.css'
import { computed, defineComponent, inject, onBeforeUnmount, type PropType, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { REPORT_FIELD_ERROR_KEY } from './AtomForm'
import styles from './CronTab.module.css'

export default defineComponent({
  name: 'CronTab',
  props: {
    value: {
      type: [Array, String] as PropType<string[] | string>,
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
    const { t, locale } = useI18n()
    const hasInternalError = ref(false)
    const internalValue = ref('')
    const reportFieldError = inject(REPORT_FIELD_ERROR_KEY, null)
    let isMounted = true

    onBeforeUnmount(() => {
      isMounted = false
      reportFieldError?.(props.name, false)
    })

    const cronLocale = computed(() =>
      locale.value?.toLowerCase().startsWith('zh') ? 'zh-CN' : 'en',
    )

    const cron = computed(() => {
      if (hasInternalError.value && internalValue.value) {
        return internalValue.value
      }
      if (Array.isArray(props.value)) {
        return props.value.join('')
      }
      return (props.value as string) || ''
    })

    const handleModelUpdate = (val: string) => {
      internalValue.value = val
    }

    const handleCronChange = (val: string) => {
      if (!isMounted) return
      if (!val) {
        hasInternalError.value = true
        reportFieldError?.(props.name, true)
        return
      }
      hasInternalError.value = false
      reportFieldError?.(props.name, false)
      emit('update:value', val)
      emit('change', val)
      props.handleChange(props.name, val)
    }

    const handleError = () => {
      if (!isMounted) return
      if (!internalValue.value && Array.isArray(props.value) && props.value.length > 0) {
        internalValue.value = props.value.join('')
      }
      hasInternalError.value = true
      reportFieldError?.(props.name, true)
    }

    return () => (
      <div class={props.disabled ? styles.cronDisabled : ''}>
        <CronTab
          modelValue={cron.value}
          onUpdate:modelValue={handleModelUpdate}
          onChange={handleCronChange}
          onError={handleError}
          local={cronLocale.value}
        />
        {hasInternalError.value && (
          <p class={styles.cronErrorMessage}>{t('flow.orchestration.cronInvalid')}</p>
        )}
      </div>
    )
  },
})
