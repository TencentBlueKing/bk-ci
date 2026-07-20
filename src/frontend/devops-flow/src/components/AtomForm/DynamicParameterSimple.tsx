import { Input, Select } from 'bkui-vue'
import { defineComponent, ref, watch, type PropType } from 'vue'
import { useI18n } from 'vue-i18n'
import { SvgIcon } from '@/components/SvgIcon'
import EnumInput from './EnumInput'
import styles from './DynamicParameterSimple.module.css'

const { Option } = Select

interface RowAttribute {
  id: string
  label?: string
  type?: string
  value?: string
  desc?: string
  placeholder?: string
  disabled?: boolean
  options?: Array<{ id: string | number; name: string }>
  list?: Array<{ value: any; label: string; disabled?: boolean; hidden?: boolean }>
}

interface RowTemplate {
  rowAttributes: RowAttribute[]
}

const clone = <T,>(data: T): T => JSON.parse(JSON.stringify(data))

export default defineComponent({
  name: 'DynamicParameterSimple',
  props: {
    name: {
      type: String,
      required: true,
    },
    value: {
      type: [String, Array] as PropType<string | Array<Record<string, string>>>,
      default: '',
    },
    parameters: {
      type: Array as PropType<RowTemplate[]>,
      default: () => [],
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
      type: Object as PropType<Record<string, unknown>>,
      default: () => ({}),
    },
  },
  emits: ['change', 'update:value'],
  setup(props, { emit }) {
    const { t } = useI18n()
    const curParameters = ref<RowTemplate[]>([])

    // Parse the stored value into an array of key/value objects
    const parseValue = (): Array<Record<string, string>> => {
      let values: unknown = props.value
      if (values === '' || values === undefined || values === null) {
        values = props.atomValue?.[props.name] ?? []
      }
      if (typeof values === 'string') {
        try {
          values = JSON.parse(values || '[]')
        } catch {
          values = []
        }
      }
      return Array.isArray(values) ? (values as Array<Record<string, string>>) : []
    }

    const initData = () => {
      const template = clone(props.parameters || [])
      const values = parseValue()

      if (values.length && template[0]) {
        curParameters.value = values.map((value) => {
          const currentRow = clone(template[0]!)
          const keys = Object.keys(value)
          const vals = Object.values(value)
          currentRow.rowAttributes.forEach((attr, index) => {
            if (index < keys.length) {
              attr.id = keys[index]!
              attr.value = vals[index] ?? ''
            }
          })
          return currentRow
        })
      } else {
        curParameters.value = template
      }
    }

    const updateParameters = () => {
      const res = curParameters.value.map((row) => {
        const obj: Record<string, string> = {}
        row.rowAttributes.forEach((attr) => {
          obj[attr.id] = attr.value || ''
        })
        return obj
      })
      const jsonStr = JSON.stringify(res)
      emit('update:value', jsonStr)
      emit('change', jsonStr)
      props.handleChange(props.name, jsonStr)
    }

    // Add a new row, cloned from the current row template
    const plusParam = (row: RowTemplate, index: number) => {
      curParameters.value.splice(index, 0, clone(row))
      updateParameters()
    }

    const minusParam = (index: number) => {
      curParameters.value.splice(index, 1)
      updateParameters()
    }

    const updateValue = (attr: RowAttribute, newValue: string) => {
      attr.value = newValue
      updateParameters()
    }

    // 仅在初始化以及模板（parameters）变化时重建内部数据；
    // 不监听 props.value，避免自身 emit 触发重建导致输入框失焦/光标跳动。
    watch(
      () => props.parameters,
      () => initData(),
      { immediate: true, deep: true },
    )

    const renderControl = (attr: RowAttribute) => {
      if (attr.type === 'select') {
        return (
          <Select
            class={styles.inputCom}
            modelValue={attr.value}
            disabled={props.disabled}
            placeholder={attr.placeholder || t('flow.common.selectPlaceholder', '请选择')}
            filterable
            onChange={(val: string) => updateValue(attr, val)}
          >
            {(attr.options || []).map((option) => (
              <Option key={option.id} id={option.id} name={option.name} />
            ))}
          </Select>
        )
      }

      if (attr.type === 'enum-input') {
        return (
          <EnumInput
            class={styles.inputCom}
            name={attr.id}
            list={attr.list || []}
            disabled={props.disabled}
            value={attr.value}
            handleChange={(_name: string, val: string) => updateValue(attr, val)}
          />
        )
      }

      return (
        <Input
          class={styles.inputCom}
          modelValue={attr.value}
          disabled={props.disabled}
          clearable={!props.disabled}
          placeholder={attr.placeholder || t('flow.common.inputPlaceholder', '请输入')}
          title={attr.value}
          onChange={(val: string) => updateValue(attr, val)}
        />
      )
    }

    return () => (
      <ul class={styles.paramMain}>
        {curParameters.value.map((row, rowIndex) => {
          const hasLabel = !!row.rowAttributes?.[0]?.label
          return (
            <li key={rowIndex} class={styles.paramRow}>
              {row.rowAttributes.map((attr) => (
                <div key={attr.id} class={styles.paramField}>
                  {attr.label && (
                    <p class={styles.fieldLabel} title={attr.label}>
                      <label>{attr.label}</label>
                      {attr.desc && (
                        <i
                          class={['bk-icon', 'icon-info-circle', styles.fieldDesc]}
                          v-bk-tooltips={{ content: attr.desc, allowHTML: false }}
                        />
                      )}
                    </p>
                  )}
                  {renderControl(attr)}
                </div>
              ))}
              {!props.disabled && (
                <div class={[styles.actionIcons, hasLabel ? styles.actionWithLabel : '']}>
                  <SvgIcon
                    name="add-small"
                    size={18}
                    class={styles.iconBtn}
                    onClick={() => plusParam(row, rowIndex)}
                  />
                  {curParameters.value.length > 1 && (
                    <SvgIcon
                      name="minus-circle"
                      size={16}
                      class={styles.iconBtn}
                      onClick={() => minusParam(rowIndex)}
                    />
                  )}
                </div>
              )}
            </li>
          )
        })}
      </ul>
    )
  },
})
