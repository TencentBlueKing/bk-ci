import { TagInput } from 'bkui-vue'
import { computed, defineComponent, type PropType } from 'vue'

export default defineComponent({
  name: 'StaffInput',
  props: {
    value: {
      type: [Array, String] as PropType<string[] | string>,
      default: () => [],
    },
    name: {
      type: String,
      required: true,
    },
    placeholder: {
      type: String,
      default: '',
    },
    disabled: {
      type: Boolean,
      default: false,
    },
    handleChange: {
      type: Function,
      default: () => () => {},
    },
  },
  emits: ['change', 'update:value'],
  setup(props, { emit }) {
    const normalizedValue = computed(() => {
      if (Array.isArray(props.value)) return props.value
      if (typeof props.value === 'string' && props.value) return props.value.split(',')
      return []
    })

    const handleChange = (val: string[]) => {
      emit('update:value', val)
      emit('change', val)
      props.handleChange(props.name, val)
    }

    return () => (
      <TagInput
        modelValue={normalizedValue.value}
        placeholder={props.placeholder}
        disabled={props.disabled}
        allowCreate={true}
        allowAutoMatch={true}
        hasDeleteIcon={true}
        copyable={false}
        separator=","
        list={[]}
        onChange={handleChange}
      />
    )
  },
})
