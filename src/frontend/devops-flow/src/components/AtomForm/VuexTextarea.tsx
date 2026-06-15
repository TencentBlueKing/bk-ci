import { defineComponent } from 'vue'
import { Input } from 'bkui-vue'

export default defineComponent({
  name: 'VuexTextarea',
  props: {
    value: {
      type: [String, Number],
      default: '',
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
    placeholder: {
      type: String,
      default: '',
    },
    rows: {
      type: Number,
      default: 3,
    },
  },
  emits: ['change', 'update:value'],
  setup(props, { emit }) {
    const handleChange = (value: string | number) => {
      emit('update:value', value)
      emit('change', value)
      props.handleChange(props.name, value)
    }

    return () => (
      <Input
        type="textarea"
        modelValue={props.value}
        name={props.name}
        placeholder={props.placeholder}
        disabled={props.disabled}
        rows={props.rows}
        onChange={handleChange}
      />
    )
  },
})
