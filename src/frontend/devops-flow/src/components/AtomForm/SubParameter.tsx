import { get } from '@/utils/http'
import { Input, Loading, Message, Select } from 'bkui-vue'
import { computed, defineComponent, onMounted, ref, watch, type PropType } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import { SvgIcon } from '../SvgIcon'
import styles from './SubParameter.module.css'

const { Option } = Select

interface Parameter {
  key: string
  value: string
  type?: string
  hasKey?: boolean
  disabled?: boolean
}

interface ParamConfig {
  paramType?: string
  list?: Array<{ key: string; value: string; type?: string }>
  url?: string
  urlQuery?: Record<string, string>
}

const isObject = (val: unknown): val is Record<string, unknown> =>
  Object.prototype.toString.call(val) === '[object Object]'

export default defineComponent({
  name: 'SubParameter',
  props: {
    value: {
      type: [String, Array] as PropType<string | Parameter[]>,
      default: () => [],
    },
    name: {
      type: String,
      required: true,
    },
    title: {
      type: String,
      default: '',
    },
    label: {
      type: String,
      default: '',
    },
    desc: {
      type: String,
      default: '',
    },
    param: {
      type: Object as PropType<ParamConfig>,
      default: () => ({}),
    },
    handleChange: {
      type: Function,
      default: () => () => {},
    },
    disabled: {
      type: Boolean,
      default: false,
    },
    atomValue: {
      type: Object,
      default: () => ({}),
    },
  },
  emits: ['change', 'update:value'],
  setup(props, { emit }) {
    const { t } = useI18n()
    const isLoading = ref(false)
    const parameters = ref<Parameter[]>([])
    const route = useRoute()
    const subParamsKeyList = ref<Array<{ key: string; value: string; type?: string }>>([])

    // Type map for getting type info by key
    const typeMap = computed(() => {
      const map = new Map<string, { type: string; defaultValue: string }>()
      subParamsKeyList.value.forEach(item => {
        map.set(item.key, {
          type: item.type || 'text',
          defaultValue: item.value,
        })
      })
      return map
    })

    // Initialize data from value prop
    const initData = () => {
      let values: Parameter[] = []
      
      if (typeof props.value === 'string') {
        try {
          values = props.value ? JSON.parse(props.value) : []
        } catch {
          values = []
        }
      } else if (Array.isArray(props.value)) {
        values = props.value
      }

      parameters.value = values.map(item => {
        const typeInfo = typeMap.value.get(item.key)
        return {
          ...item,
          type: typeInfo?.type || item.type || 'text',
          value: isObject(item.value) ? JSON.stringify(item.value) : String(item.value || ''),
          hasKey: !!typeInfo,
          disabled: !typeInfo && !!item.key,
        }
      })
    }

    // Generate request URL with query parameters
    const generateReqUrl = (url: string, query: Record<string, unknown>): [string, string[]] => {
      const queryKey: string[] = []
      let lackParam = false
      const newUrl = url.replace(/{([^{}]+)}/g, (_, key) => {
        const value = query[key]
        queryKey.push(key)
        if (typeof value === 'undefined') lackParam = true
        return String(value ?? '')
      })
      return [lackParam ? '' : newUrl, queryKey]
    }

    // Fetch parameters list from API
    const getParametersList = async () => {
      const param = props.param
      // If paramType is list and list is provided, use it directly
      if (param?.paramType === 'list' && Array.isArray(param.list)) {
        subParamsKeyList.value = param.list
        return
      }

      if (!param?.url) return

      const [url] = generateReqUrl(param.url, {
        ...props.atomValue,
        projectId: route.params.projectId,
        pipelineId: route.params.flowId,
      })
      if (!url) return

      // Build URL with query parameters
      let finalUrl = url
      const urlQuery = param.urlQuery || {}
      Object.keys(urlQuery).forEach((key, index) => {
        const value = props.atomValue[key] ?? urlQuery[key]
        finalUrl += `${index <= 0 ? '?' : '&'}${key}=${value}`
      })
      isLoading.value = true
      try {
        const response = await get<any>(finalUrl) 
        subParamsKeyList.value = response
      } catch (e: any) {
        Message({
          message: e.message || t('common.loadFailed', 'Failed to load data'),
          theme: 'error',
        })
      } finally {
        isLoading.value = false
      }
    }

    // Update parameters and emit change
    const updateParameters = () => {
      const res = parameters.value.map(parameter => ({
        key: parameter.key,
        value: isObject(parameter.value) ? JSON.stringify(parameter.value) : parameter.value,
      }))
      const jsonStr = JSON.stringify(res)
      emit('update:value', jsonStr)
      emit('change', jsonStr)
      props.handleChange(props.name, jsonStr)
    }

    // Add a new parameter
    const handleAddParam = () => {
      parameters.value.push({
        key: '',
        value: '',
        hasKey: true,
      })
    }

    // Remove a parameter
    const handleRemoveParam = (index: number) => {
      parameters.value.splice(index, 1)
      updateParameters()
    }

    // Handle key change
    const handleChangeKey = (key: string, index: number) => {
      const param = parameters.value[index]
      if (!param) return
      param.key = isObject(key) ? JSON.stringify(key) : key
      
      const info = typeMap.value.get(key)
      if (info?.defaultValue) {
        param.value = isObject(info.defaultValue) ? JSON.stringify(info.defaultValue) : info.defaultValue
      } else {
        param.value = ''
      }
      param.type = info?.type || 'text'
      updateParameters()
    }

    // Handle value change
    const handleChangeValue = (val: string, index: number) => {
      if (!parameters.value[index]) return
      parameters.value[index].value = val
      updateParameters()
    }

    // Get input type based on parameter type
    const getInputType = (type?: string) => {
      const typeMap: Record<string, string> = {
        textarea: 'textarea',
        long: 'number',
      }
      return typeMap[type || ''] || 'text'
    }

    // Watch for subParamsKeyList changes to reinitialize data
    watch(
      () => subParamsKeyList.value,
      () => {
        initData()
      },
    )

    // Watch for atomValue changes that affect the parameters list
    watch(
      () => props.atomValue,
      (newVal, oldVal) => {
        if (oldVal !== undefined) {
          const subPipChanged = newVal?.subPip !== oldVal?.subPip
          const subBranchChanged = newVal?.subBranch !== oldVal?.subBranch
          if (subPipChanged || subBranchChanged) {
            parameters.value = []
            getParametersList()
            initData()
          }
        }
      },
      { deep: true },
    )

    onMounted(() => {
      getParametersList()
      initData()
    })

    return () => (
      <Loading loading={isLoading.value} mode="spin" size="small" class={styles.subParameter}>
        <label class={styles.label}>
          {props.title || props.label}
          {!props.disabled && (
            <span class={styles.addBtn} onClick={handleAddParam}>
              <SvgIcon name="close-circle" class={styles.addIcon} />
              {t('common.addParam', 'Add Parameter')}
            </span>
          )}
        </label>
        {props.desc && <div class={styles.desc}>{props.desc}</div>}
        {parameters.value.length > 0 && (
            <ul class={styles.paramsList}>
              {parameters.value.map((parameter, index) => (
                <li
                  key={`${parameter.key}-${index}`}
                  class={styles.paramItem}
                  title={parameter.disabled ? t('common.notParamsTip', 'This parameter is not in the list') : undefined}
                >
                  {parameter.hasKey ? (
                    <Select
                      class={styles.inputCom}
                      modelValue={parameter.key}
                      disabled={props.disabled}
                      onChange={(val: string) => handleChangeKey(val, index)}
                    >
                      {subParamsKeyList.value.map(option => (
                        <Option
                          key={option.key}
                          value={option.key}
                          label={option.key}
                          disabled={parameters.value.some(p => p.key === option.key && p !== parameter)}
                        />
                      ))}
                    </Select>
                  ) : (
                    <Input
                      class={[styles.inputCom, styles.disabledInput]}
                      modelValue={parameter.key}
                      disabled
                      title={parameter.key}
                    />
                  )}
                  <span class={styles.inputSeg}>=</span>
                  <Input
                    class={[styles.inputCom, parameter.disabled && styles.disabledInput]}
                    modelValue={parameter.value}
                    type={getInputType(parameter.type)}
                    disabled={props.disabled || parameter.disabled}
                    title={parameter.value}
                    onChange={(val: string) => handleChangeValue(val, index)}
                  />
                  {!props.disabled && (
                    <i
                      class={`bk-icon icon-minus-circle ${styles.minusBtn}`}
                      onClick={() => handleRemoveParam(index)}
                    />
                  )}
                </li>
              ))}
            </ul>
        )}
      
      </Loading>
    )
  },
})
