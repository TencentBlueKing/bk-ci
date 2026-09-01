import { Popover, Switcher } from 'bkui-vue'
import { defineComponent, type PropType } from 'vue'
import styles from './FormFieldGroup.module.css'

export default defineComponent({
  name: 'FormFieldGroup',
  props: {
    label: {
      type: String,
      default: '',
    },
    desc: {
      type: String,
      default: '',
    },
    name: {
      type: String,
      default: '',
    },
    value: {
      type: Boolean,
      default: false,
    },
    showSwitch: {
      type: Boolean,
      default: false,
    },
    topDivider: {
      type: Boolean,
      default: false,
    },
    docs: {
      type: String,
      default: '',
    },
    docsLink: {
      type: String,
      default: '',
    },
    handleChange: {
      type: Function as PropType<(name: string, value: any) => void>,
      default: undefined,
    },
  },
  setup(props, { slots }) {
    const handleSwitchChange = (val: boolean) => {
      props.handleChange?.(props.name, val)
    }

    const handleToDocs = () => {
      if (props.docsLink) {
        window.open(props.docsLink, '_blank')
      }
    }

    return () => (
      <div class={styles.formFieldGroup}>
        {props.topDivider && <div class={styles.topDivider} />}
        <div class={styles.groupHeader}>
          {props.label && <span class={styles.groupLabel}>{props.label}：</span>}
          {props.label && props.desc?.trim() && (
            <Popover placement="top">
              {{
                default: () => <i class={`bk-icon icon-info-circle ${styles.infoIcon}`} />,
                content: () => (
                  <div style={{ whiteSpace: 'pre-wrap', fontSize: '12px', maxWidth: '500px' }}>
                    {props.desc}
                  </div>
                ),
              }}
            </Popover>
          )}
          {props.showSwitch && <span class={styles.labelDivider} />}
          {props.showSwitch && (
            <Switcher
              modelValue={props.value}
              theme="primary"
              size="small"
              onChange={handleSwitchChange}
            />
          )}
          {props.docsLink && (
            <span class={styles.docsLink} onClick={handleToDocs}>
              {props.docs}
            </span>
          )}
        </div>
        {((props.showSwitch && props.value) || !props.showSwitch) && (
          <div class={styles.groupContent}>{slots.default?.()}</div>
        )}
      </div>
    )
  },
})
