import type { StartupProperty } from '@/api/preview'
import { getResponseData, useUrlParser } from '@/hooks/useDataSource'
import { get } from '@/utils/http'
import { Select } from 'bkui-vue'
import { defineComponent, onMounted, ref, watch, type PropType } from 'vue'
import styles from './Preview.module.css'

interface OptionItem {
  value: string | number
  label: string
  [key: string]: unknown
}

export default defineComponent({
  name: 'DynamicSelect',
  props: {
    param: {
      type: Object as PropType<StartupProperty>,
      required: true,
    },
    modelValue: {
      type: [String, Number, Boolean],
      default: '',
    },
    disabled: {
      type: Boolean,
      default: false,
    },
    isInvalid: {
      type: Boolean,
      default: false,
    },
  },
  emits: ['update:modelValue', 'change'],
  setup(props, { emit }) {
    const { parseUrl } = useUrlParser()
    const isLoading = ref(false)
    const options = ref<OptionItem[]>([])

    /**
     * Check if param has API URL configuration
     */
    const hasApiUrl = (): boolean => {
      return Boolean(props.param.payload?.url)
    }

    /**
     * Normalize options to unified format { value, label }
     */
    const normalizeOptions = (
      rawData: unknown[],
      paramId: string,
      paramName: string,
    ): OptionItem[] => {
      return rawData
        .filter(
          (item): item is Record<string, unknown> => item !== null && typeof item === 'object',
        )
        .map((item) => ({
          ...item,
          value: (item[paramId] ?? item.id ?? item.key ?? '') as string | number,
          label: (item[paramName] ?? item.name ?? item.value ?? '') as string,
        }))
    }

    /**
     * Load options from API
     */
    const loadOptionsFromApi = async () => {
      const payload = props.param.payload
      if (!payload?.url) return

      try {
        isLoading.value = true
        const parsedUrl = parseUrl(payload.url)
        const response = await get(parsedUrl)
        const resData = getResponseData(response, payload.dataPath)

        // Get field mapping from payload, with fallbacks
        const paramId = payload.paramId || 'id'
        const paramName = payload.paramName || 'name'

        options.value = normalizeOptions(resData, paramId, paramName)
      } catch (e) {
        console.error('Failed to load options from API:', e)
        // Fallback to static options if API fails
        loadStaticOptions()
      } finally {
        isLoading.value = false
      }
    }

    /**
     * Load static options from param.options
     */
    const loadStaticOptions = () => {
      const staticOptions = props.param.options || []
      options.value = staticOptions.map((opt) => ({
        value: opt.id,
        label: opt.name,
      }))
    }

    /**
     * Initialize options based on configuration
     * Priority: API URL > static options
     */
    const initOptions = () => {
      if (hasApiUrl()) {
        loadOptionsFromApi()
      } else {
        loadStaticOptions()
      }
    }

    // Watch param changes to reload options
    watch(
      () => props.param,
      () => {
        initOptions()
      },
      { deep: true },
    )

    onMounted(() => {
      initOptions()
    })

    const handleChange = (val: string | number) => {
      emit('update:modelValue', val)
      emit('change', val)
    }

    return () => (
      <Select
        modelValue={props.modelValue}
        clearable={false}
        disabled={props.disabled || props.param.readOnly}
        loading={isLoading.value}
        class={[styles.fullWidthSelect, props.isInvalid && styles.inputInvalid]}
        onChange={handleChange}
      >
        {options.value.map((opt) => (
          <Select.Option key={opt.value} value={opt.value} label={opt.label}>
            {/* Show label as main text, show value in parentheses if different */}
            <span>{opt.label}</span>
          </Select.Option>
        ))}
      </Select>
    )
  },
})
