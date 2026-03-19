import { defineComponent, type PropType } from 'vue'
import { Radio } from 'bkui-vue'
const { Group: RadioGroup } = Radio
import styles from './EnumInput.module.css'

export default defineComponent({
  name: 'EnumInput',
  props: {
    value: {
      type: [String, Number, Boolean],
      default: '',
    },
    name: {
      type: String,
      required: true,
    },
    list: {
      type: Array as PropType<
        Array<{ value: any; label: string; disabled?: boolean; hidden?: boolean }>
      >,
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
  },
  emits: ['change', 'update:value'],
  setup(props, { emit }) {
    const handleChange = (value: any) => {
      emit('update:value', value)
      emit('change', value)
      props.handleChange(props.name, value)
    }

    return () => (
      <RadioGroup
        class={styles.radioGroup}
        modelValue={props.value}
        disabled={props.disabled}
        onChange={handleChange}
      >
        {props.list.map((item) => {
          if (item.hidden) return null
          return (
            <Radio key={item.value} label={item.value} disabled={item.disabled}>
              {item.label}
            </Radio>
          )
        })}
      </RadioGroup>
    )
  },
})
