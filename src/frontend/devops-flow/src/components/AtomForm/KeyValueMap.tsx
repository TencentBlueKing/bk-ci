import { defineComponent, ref, watch, type PropType } from 'vue'
import { Button, Input } from 'bkui-vue'
import { SvgIcon } from '@/components/SvgIcon'
import styles from './KeyValueMap.module.css'

interface KeyValueItem {
  key: string
  value: string
}

export default defineComponent({
  name: 'KeyValueMap',
  props: {
    value: {
      type: Array as PropType<KeyValueItem[]>,
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
    editValueOnly: {
      type: Boolean,
      default: false,
    },
    allowNull: {
      type: Boolean,
      default: true,
    },
    addBtnText: {
      type: String,
      default: '添加参数',
    },
    keyPlaceholder: {
      type: String,
      default: 'Key',
    },
    valuePlaceholder: {
      type: String,
      default: 'Value',
    },
  },
  emits: ['change', 'update:value'],
  setup(props, { emit }) {
    const list = ref<KeyValueItem[]>([])

    watch(
      () => props.value,
      (val) => {
        if (Array.isArray(val)) {
          list.value = [...val]
        } else {
          list.value = []
        }
      },
      { immediate: true, deep: true },
    )

    const triggerChange = () => {
      const newValue = [...list.value]
      emit('update:value', newValue)
      emit('change', newValue)
      props.handleChange(props.name, newValue)
    }

    const handleKeyChange = (index: number, val: string) => {
      if (list.value[index]) {
        list.value[index].key = val
        triggerChange()
      }
    }

    const handleValueChange = (index: number, val: string) => {
      if (list.value[index]) {
        list.value[index].value = val
        triggerChange()
      }
    }

    const handleRemove = (index: number) => {
      list.value.splice(index, 1)
      triggerChange()
    }

    const handleAdd = () => {
      list.value.push({ key: '', value: '' })
      triggerChange()
    }

    return () => (
      <div class={styles.keyValueMap}>
        <ul class={styles.keyValueList}>
          {list.value.map((item: KeyValueItem, index: number) => (
            <li key={index} class={styles.keyValueItem}>
              <div class={`${styles.itemInput} ${styles.key}`}>
                <Input
                  modelValue={item.key}
                  disabled={props.disabled || props.editValueOnly}
                  placeholder={props.keyPlaceholder}
                  onChange={(val: string) => handleKeyChange(index, val)}
                />
              </div>
              <span class={styles.separator}>=</span>
              <div class={`${styles.itemInput} ${styles.value}`}>
                <Input
                  modelValue={item.value}
                  disabled={props.disabled}
                  placeholder={props.valuePlaceholder}
                  onChange={(val: string) => handleValueChange(index, val)}
                />
              </div>
              {!props.disabled && !props.editValueOnly && (
                <div class={styles.itemAction} onClick={() => handleRemove(index)}>
                  <SvgIcon name="minus-circle" class={styles.iconBtn} />
                </div>
              )}
            </li>
          ))}
        </ul>
        {!props.disabled && !props.editValueOnly && (
          <Button text theme="primary" onClick={handleAdd}>
            <div class={styles.addBtn}>
              <SvgIcon name="add-small" size={18} />
              <span>{props.addBtnText}</span>
            </div>
          </Button>
        )}
      </div>
    )
  },
})
