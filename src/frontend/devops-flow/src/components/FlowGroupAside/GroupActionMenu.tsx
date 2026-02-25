import { defineComponent, ref, type PropType } from 'vue'
import { Dropdown } from 'bkui-vue'
import { SvgIcon } from '../SvgIcon'
import styles from './GroupActionMenu.module.css'

const { DropdownMenu, DropdownItem } = Dropdown

interface Operation {
  id: string
  label: string
  icon?: string
  disabled?: boolean
}

interface Props {
  operations: Operation[]
  onOperationClick: (operationId: string) => void
}

export const GroupActionMenu = defineComponent({
  name: 'GroupActionMenu',
  props: {
    operations: {
      type: Array as () => Operation[],
      required: true,
    },
    onOperationClick: {
      type: Function as PropType<(operationId: string) => void>,
      required: true,
    },
  },
  setup(props: Props) {
    const handleClick = (e: MouseEvent, operationId: string) => {
      props.onOperationClick(operationId)
    }

    const popoverOptions = ref({
      clickContentAutoHide: true,
      hideIgnoreReference: true,
      boundary: 'body',
    })

    const stopPropagation = (e: MouseEvent) => {
      e.stopPropagation()
    }

    return () => (
      <Dropdown
        trigger="click"
        placement="bottom-end"
        popoverOptions={popoverOptions.value}
        onClick={stopPropagation}
      >
        {{
          default: () => (
            <div class={styles.trigger}>
              <SvgIcon name="more-fill" class={styles.icon} />
            </div>
          ),
          content: () => (
            <DropdownMenu>
              {props.operations.map((operation) => (
                <DropdownItem
                  key={operation.id}
                  onClick={(e: MouseEvent) => handleClick(e, operation.id)}
                  disabled={operation.disabled}
                >
                  <div class={styles.menuItem}>
                    {operation.icon && <SvgIcon name={operation.icon} class={styles.menuIcon} />}
                    <span>{operation.label}</span>
                  </div>
                </DropdownItem>
              ))}
            </DropdownMenu>
          ),
        }}
      </Dropdown>
    )
  },
})
