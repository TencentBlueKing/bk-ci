import { TagInput } from 'bkui-vue'
import { defineComponent, type PropType } from 'vue'

export default defineComponent({
  name: 'StaffInput',
  props: {
    value: {
      type: Array as PropType<string[]>,
      default: () => []
    },
    name: {
      type: String,
      required: true
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
    const handleChange = (val: string[]) => {
      emit('update:value', val)
      emit('change', val)
      props.handleChange(props.name, val)
    }

    return () => (
      <TagInput
        modelValue={props.value}
        placeholder={props.placeholder}
        disabled={props.disabled}
        allowCreate={true}
        allowAutoMatch={true}
        hasDeleteIcon={true}
        onChange={handleChange}
      />
    )
  }
})
