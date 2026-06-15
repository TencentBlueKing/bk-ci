import { defineComponent } from 'vue'
import { Input } from 'bkui-vue'

export default defineComponent({
  name: 'VuexInput',
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
    type: {
      type: String,
      default: 'text',
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
        modelValue={props.value}
        name={props.name}
        type={props.type as any}
        placeholder={props.placeholder}
        disabled={props.disabled}
        onChange={handleChange}
      />
    )
  },
})
