import { DatePicker } from 'bkui-vue'
import { defineComponent, type PropType } from 'vue'

export default defineComponent({
  name: 'AtomDatePicker',
  props: {
    value: {
      type: [String, Number, Date, Array],
      default: ''
    },
    name: {
      type: String,
      required: true
    },
    type: {
      type: String as PropType<'date' | 'daterange' | 'datetime' | 'datetimerange' | 'month' | 'year'>,
      default: 'date'
    },
    placeholder: {
      type: String,
      default: ''
    },
    disabled: {
      type: Boolean,
      default: false
    },
    handleChange: {
      type: Function,
      default: () => () => {}
    }
  },
  emits: ['change', 'update:value'],
  setup(props, { emit }) {
    const handleChange = (val: any) => {
      emit('update:value', val)
      emit('change', val)
      props.handleChange(props.name, val)
    }

    return () => (
      <DatePicker
        modelValue={props.value as unknown as Date | [Date, Date]}
        placeholder={props.placeholder}
        disabled={props.disabled}
        type={props.type}
        onChange={handleChange}
      />
    )
  }
})
