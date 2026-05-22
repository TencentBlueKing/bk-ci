import { Input, Popover } from 'bkui-vue'
import { defineComponent } from 'vue'
import styles from './CompositeInput.module.css'

export default defineComponent({
  name: 'CompositeInput',
  props: {
    value: {
      type: String,
      default: '',
    },
    name: {
      type: String,
      required: true,
    },
    appendText: {
      type: String,
      default: '',
    },
    prependText: {
      type: String,
      default: '',
    },
    disabled: {
      type: Boolean,
      default: false,
    },
    iconDesc: {
      type: String,
      default: '',
    },
    placeholder: {
      type: String,
      default: '',
    },
    labelWidth: {
      type: String,
      default: '',
    },
    width: {
      type: String,
      default: '',
    },
    handleChange: {
      type: Function,
      default: () => () => {},
    },
  },
  setup(props) {
    const handleChangeValue = (val: string) => {
      props.handleChange(props.name, val)
    }

    return () => (
      <div class={styles.compositeInputWrapper}>
        <Input
          class={styles.compositeInput}
          style={{ width: props.width ? `${props.width}px` : '100%' }}
          disabled={props.disabled}
          modelValue={props.value}
          placeholder={props.placeholder}
          onChange={handleChangeValue}
          prefix={props.prependText}
          suffix={props.appendText}
        />
        {props.iconDesc && (
          <Popover placement="top">
            {{
              default: () => <i class={`bk-icon icon-question-circle-shape ${styles.iconDesc}`} />,
              content: () => props.iconDesc,
            }}
          </Popover>
        )}
      </div>
    )
  },
})
