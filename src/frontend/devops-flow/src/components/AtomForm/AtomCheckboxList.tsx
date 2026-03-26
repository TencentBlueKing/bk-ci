import { Checkbox } from 'bkui-vue'
import { defineComponent, type PropType } from 'vue'
import styles from './AtomCheckboxList.module.css'
const { Group: CheckboxGroup } = Checkbox

export default defineComponent({
  name: 'AtomCheckboxList',
  props: {
    value: {
      type: Array as PropType<string[]>,
      default: () => [],
    },
    name: {
      type: String,
      required: true,
    },
    list: {
      type: Array as PropType<Array<{ id: string; name: string; disabled?: boolean }>>,
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
    const handleChange = (value: string[]) => {
      emit('update:value', value)
      emit('change', value)
      props.handleChange(props.name, value)
    }

    return () => (
      <CheckboxGroup modelValue={props.value} disabled={props.disabled} onChange={handleChange}>
        {props.list.map((item) => (
          <Checkbox
            key={item.id}
            label={item.id}
            disabled={item.disabled}
            class={styles.checkboxItem}
          >
            {item.name}
          </Checkbox>
        ))}
      </CheckboxGroup>
    )
  },
})
