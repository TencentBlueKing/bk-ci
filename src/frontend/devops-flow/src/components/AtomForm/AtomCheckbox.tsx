import { defineComponent } from 'vue'
import { Checkbox, Popover } from 'bkui-vue'
import { SvgIcon } from '@/components/SvgIcon'
import styles from './AtomCheckbox.module.css'

export default defineComponent({
  name: 'AtomCheckbox',
  props: {
    value: {
      type: Boolean,
      default: false,
    },
    name: {
      type: String,
      required: true,
    },
    text: {
      type: String,
      default: '',
    },
    desc: {
      type: String,
      default: '',
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
    const handleChange = (value: boolean) => {
      emit('update:value', value)
      emit('change', value)
      props.handleChange(props.name, value)
    }

    return () => (
      <div class={styles.atomCheckbox}>
        <Checkbox modelValue={props.value} disabled={props.disabled} onChange={handleChange}>
          {props.text}
        </Checkbox>
        {props.desc && (
          <Popover content={props.desc} placement="top">
            <span class={styles.infoIcon}>
              <SvgIcon name="info-circle" />
            </span>
          </Popover>
        )}
      </div>
    )
  },
})
