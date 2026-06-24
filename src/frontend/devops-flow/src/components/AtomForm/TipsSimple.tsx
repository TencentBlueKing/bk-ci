import { get } from '@/utils/http'
import { getResponseData } from '@/hooks/useDataSource'
import { defineComponent, ref, computed, watch, onMounted, type PropType } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import styles from './TipsSimple.module.css'

export default defineComponent({
  name: 'TipsSimple',
  props: {
    name: {
      type: String,
      default: '',
    },
    value: {
      type: [String, Number, Boolean, Object, Array],
      default: '',
    },
    tipStr: {
      type: String,
      default: '',
    },
    url: {
      type: String,
      default: '',
    },
    dataPath: {
      type: String,
      default: '',
    },
    atomValue: {
      type: Object as PropType<Record<string, any>>,
      default: () => ({}),
    },
    disabled: {
      type: Boolean,
      default: false,
    },
    handleChange: {
      type: Function,
      default: () => () => {},
    },
    placeholder: {
      type: String,
      default: '',
    },
  },
  setup(props) {
    const { t } = useI18n()
    const route = useRoute()
    const tip = ref('')
    const paramKeys = ref<string[]>([])

    const paramValues = computed(() => {
      const params = (route?.params || {}) as Record<string, any>
      return { ...params, ...props.atomValue }
    })

    const escapeHtml = (val: string): string => {
      return val.replace(/[&<>"'/]/g, (str) => {
        const map: Record<string, string> = {
          '&': '&amp;',
          '<': '&lt;',
          '>': '&gt;',
          '"': '&quot;',
          "'": '&#x27;',
          '/': '&#x2F;',
        }
        return map[str] || str
      })
    }

    const formatter = (data: any): string => {
      const type = typeof data
      switch (type) {
        case 'object':
          return JSON.stringify(data)
        case 'boolean':
          return data ? t('true') : t('false')
        case 'number':
          return Number.isNaN(data) ? '' : String(data)
        case 'string':
          return data
        default:
          return String(data ?? '')
      }
    }

    const handleDeepValue = (reg: RegExp, str: string): string => {
      return str.replace(reg, (raw, key) => {
        const hasParam = Object.prototype.hasOwnProperty.call(paramValues.value, key)
        const value = hasParam ? paramValues.value[key] : raw
        if (hasParam) paramKeys.value.push(key)
        return value
      })
    }

    const handleData = (tipStr: string) => {
      // Parse markdown-style links [text](url)
      const str = tipStr.replace(/\[([^\]]+)\]\(([^)]+)\)/gim, (_, key, value) => {
        const isSafe = /^https?:\/\//i.test(value)
        if (isSafe) {
          return `<a class="text-link" href="${value}" target="_blank">${key}</a>`
        }
        return `<a class="text-bad-link text-link" title="${t('editPage.badLink')}">${key}</a>`
      })

      paramKeys.value = []

      // Replace {param} placeholders
      tip.value = str.replace(/{([^{}/]+)}/gim, (raw, key) => {
        const hasParam = typeof paramValues.value[key] !== 'undefined'
        const value = hasParam ? paramValues.value[key] : raw
        if (hasParam) paramKeys.value.push(key)
        const val = formatter(value)
        return escapeHtml(val)
      })
    }

    const initData = async () => {
      if (props.url) {
        try {
          const url = handleDeepValue(/\$\{([^}]+)\}/gim, props.url)
          const res = await get(url)
          const tipStr = getResponseData(res, props.dataPath || undefined)
          handleData(typeof tipStr === 'string' ? tipStr : String(tipStr ?? ''))
        } catch (err: any) {
          console.error(err.message || err)
        }
      } else {
        handleData(props.tipStr)
      }
    }

    watch(
      paramValues,
      (value, oldValue) => {
        const index = paramKeys.value.findIndex((key) => value[key] !== oldValue[key])
        if (index > -1) {
          initData()
        }
      },
      { deep: true },
    )

    onMounted(() => {
      initData()
    })

    return () => (
      <h3 class={[styles.componentTip, 'pointer-events-auto']}>
        <span class={styles.tipMessage} v-html={tip.value} />
      </h3>
    )
  },
})
