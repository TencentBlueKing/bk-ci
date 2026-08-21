import { getResponseData, useUrlParser, type SelectDataConf } from '@/hooks/useDataSource'
import { get } from '@/utils/http'
import { Loading, Select } from 'bkui-vue'
import {
  computed,
  defineComponent,
  onMounted,
  ref,
  watch,
  type PropType,
} from 'vue'
import { useI18n } from 'vue-i18n'
import { SvgIcon } from '../SvgIcon'
import styles from './MetadataTigger.module.css'
import SelectInput from './SelectInput'

const { Option } = Select

const DEFAULT_DISPLAY_CONDITION_OPERATOR = '=='

const DISPLAY_CONDITION_OPERATORS = [
  { id: DEFAULT_DISPLAY_CONDITION_OPERATOR, name: DEFAULT_DISPLAY_CONDITION_OPERATOR },
  { id: '>=', name: '>=' },
  { id: '<=', name: '<=' },
  { id: '>', name: '>' },
  { id: '<', name: '<' },
  { id: 'IN', name: 'in' },
  { id: 'CONTAINS', name: 'Contains' },
  { id: 'STARTS_WITH', name: 'StartWith' },
  { id: 'ENDS_WITH', name: 'EndWith' },
]

interface MetadataParam {
  key: string
  operator: string
  value: string
}

interface MetadataOption {
  key: string
  values: string[]
  [key: string]: unknown
}

const isObject = (val: unknown): val is Record<string, unknown> =>
  Object.prototype.toString.call(val) === '[object Object]'

const normalizeParams = (raw: unknown, fallbackDefault?: MetadataParam[]): MetadataParam[] => {
  let list: unknown[] = []
  if (typeof raw === 'string') {
    try {
      list = raw ? JSON.parse(raw) : []
    } catch {
      list = []
    }
  } else if (Array.isArray(raw)) {
    list = raw
  } else if (Array.isArray(fallbackDefault) && fallbackDefault.length) {
    list = fallbackDefault
  }

  if (!Array.isArray(list) || !list.length) {
    return Array.isArray(fallbackDefault)
      ? fallbackDefault.map((item) => ({
          key: item.key || '',
          operator: item.operator || DEFAULT_DISPLAY_CONDITION_OPERATOR,
          value: item.value || '',
        }))
      : []
  }

  return list.map((item) => {
    const row = (isObject(item) ? item : {}) as Partial<MetadataParam>
    return {
      key: row.key || '',
      operator: row.operator || DEFAULT_DISPLAY_CONDITION_OPERATOR,
      value: row.value || '',
    }
  })
}

export default defineComponent({
  name: 'metadata-tigger',
  props: {
    value: {
      type: [String, Array] as PropType<string | MetadataParam[]>,
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
    options: {
      type: Array as PropType<MetadataOption[]>,
      default: () => [],
    },
    optionsConf: {
      type: Object as PropType<SelectDataConf>,
      default: () => ({}),
    },
    atomValue: {
      type: Object as PropType<Record<string, unknown>>,
      default: () => ({}),
    },
    default: {
      type: Array as PropType<MetadataParam[]>,
      default: () => [],
    },
  },
  emits: ['change', 'update:value'],
  setup(props, { emit }) {
    const { t } = useI18n()
    const { parseUrl } = useUrlParser()

    const isLoading = ref(false)
    const paramValues = ref<MetadataParam[]>([])
    const optionList = ref<MetadataOption[]>([])

    const mergedOptionsConf = computed(() => ({
      url: '',
      paramId: 'id',
      paramName: 'name',
      searchable: false,
      clearable: false,
      multiple: false,
      ...props.optionsConf,
    }))

    const hasUrl = computed(
      () =>
        !!mergedOptionsConf.value.url && typeof mergedOptionsConf.value.url === 'string',
    )

    const reqUrl = computed(() => {
      if (!hasUrl.value) return ''
      return parseUrl(mergedOptionsConf.value.url as string, {
        atomValue: props.atomValue,
      })
    })

    const listSource = computed(() => (hasUrl.value ? optionList.value : props.options || []))

    const keyList = computed(() =>
      listSource.value.map((item) => ({
        id: item.key,
        name: item.key,
      })),
    )

    const getValueListByIndex = (index: number) => {
      const key = paramValues.value[index]?.key
      if (!key) return []
      const keyItem = listSource.value.find((item) => item.key === key)
      return (keyItem?.values || []).map((item) => ({
        id: item,
        name: item,
      }))
    }

    const updateParameters = () => {
      const res = paramValues.value.map((parameter) => {
        const value = isObject(parameter.value)
          ? JSON.stringify(parameter.value)
          : parameter.value
        return {
          key: parameter.key,
          operator: parameter.operator || DEFAULT_DISPLAY_CONDITION_OPERATOR,
          value,
        }
      })
      emit('update:value', res)
      emit('change', res)
      props.handleChange(props.name, res)
    }

    const addParam = () => {
      paramValues.value.push({
        key: '',
        operator: DEFAULT_DISPLAY_CONDITION_OPERATOR,
        value: '',
      })
      updateParameters()
    }

    const cutParam = (index: number) => {
      paramValues.value.splice(index, 1)
      updateParameters()
    }

    const handleChangeKey = (_name: string, value: string, index: number) => {
      if (!paramValues.value[index]) return
      paramValues.value[index].key = value
      if (paramValues.value[index].value) {
        paramValues.value[index].value = ''
      }
      updateParameters()
    }

    const handleChangeOperator = (operator: string, index: number) => {
      if (!paramValues.value[index]) return
      paramValues.value[index].operator = operator || DEFAULT_DISPLAY_CONDITION_OPERATOR
      updateParameters()
    }

    const handleChangeValue = (_name: string, value: string, index: number) => {
      if (!paramValues.value[index]) return
      paramValues.value[index].value = value
      updateParameters()
    }

    const getOptionList = async () => {
      if (!hasUrl.value) {
        optionList.value = []
        return
      }

      const url = reqUrl.value
      if (!url) {
        optionList.value = []
        return
      }

      try {
        isLoading.value = true
        const { paramId, paramName, dataPath } = mergedOptionsConf.value
        const res = await get(url)
        const options = getResponseData(res, dataPath as string | undefined)
        optionList.value = options.map((item) => {
          if (isObject(item)) {
            return {
              ...item,
              key: String(item[paramId as string] ?? ''),
              values: (item[paramName as string] as string[]) || [],
            }
          }
          return {
            key: String(item),
            values: [],
          }
        })
      } catch (e) {
        console.error(e)
        optionList.value = []
      } finally {
        isLoading.value = false
      }
    }

    watch(
      () => props.value,
      (newVal) => {
        paramValues.value = normalizeParams(newVal, props.default)
      },
      { immediate: true, deep: true },
    )

    watch(reqUrl, (newUrl, oldUrl) => {
      if (!hasUrl.value) return
      if (oldUrl !== undefined && newUrl !== oldUrl) {
        getOptionList()
      }
    })

    onMounted(() => {
      if (hasUrl.value) {
        getOptionList()
      }
    })

    return () => (
      <Loading loading={isLoading.value} mode="spin" size="small" class={styles.metadataTigger}>
        <ul>
          {paramValues.value.map((parameter, index) => (
            <li key={index} class={styles.paramInput}>
              <SelectInput
                class={styles.inputCom}
                name={`metadata-tigger-key-${index}`}
                value={parameter.key}
                placeholder={t('flow.content.keyPlaceholder')}
                disabled={props.disabled}
                options={keyList.value}
                handleChange={(name: string, value: string) =>
                  handleChangeKey(name, value, index)
                }
              />
              <Select
                class={styles.inputOperator}
                modelValue={parameter.operator}
                disabled={props.disabled}
                clearable={false}
                onChange={(val: string) => handleChangeOperator(val, index)}
              >
                {DISPLAY_CONDITION_OPERATORS.map((option) => (
                  <Option key={option.id} value={option.id} label={option.name} />
                ))}
              </Select>
              <SelectInput
                class={styles.inputCom}
                name={`metadata-tigger-value-${index}`}
                value={parameter.value}
                placeholder={t('flow.content.valuePlaceholder')}
                disabled={props.disabled}
                options={getValueListByIndex(index)}
                handleChange={(name: string, value: string) =>
                  handleChangeValue(name, value, index)
                }
              />
              {!props.disabled && (
                <span class={styles.minusBtn} onClick={() => cutParam(index)}>
                  <SvgIcon name="minus-circle" size={16} />
                </span>
              )}
            </li>
          ))}
        </ul>
        {!props.disabled && (
          <span class={styles.addParamsBtn} onClick={addParam}>
            <SvgIcon name="add-small" size={16} />
            {t('flow.orchestration.add')}
          </span>
        )}
      </Loading>
    )
  },
})
