import { Button } from 'bkui-vue'
import { defineComponent, type PropType } from 'vue'

export default defineComponent({
  name: 'EnumButton',
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
      type: Array as PropType<Array<{ value: any; label: string; disabled?: boolean }>>,
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
  setup(props) {
    return () => (
      <div class="bk-button-group">
        {props.list.map((item) => (
          <Button
            key={item.value}
            size="small"
            disabled={props.disabled || item.disabled}
            class={item.value === props.value ? 'is-selected' : ''}
            onClick={() => props.handleChange(props.name, item.value)}
          >
            {item.label}
          </Button>
        ))}
      </div>
    )
  },
})
