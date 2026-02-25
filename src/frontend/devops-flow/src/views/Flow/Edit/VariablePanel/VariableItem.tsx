import { defineComponent, type PropType } from 'vue'
import { useI18n } from 'vue-i18n'
import { PopConfirm, Message } from 'bkui-vue'
import { SvgIcon } from '@/components/SvgIcon'
import type { Param } from '@/api/flowModel'
import styles from './VariableItem.module.css'

export default defineComponent({
  name: 'VariableItem',
  props: {
    dragHandleCls: {
      type: String,
    },
    variable: {
      type: Object as PropType<Param>,
      required: true,
    },
    editable: {
      type: Boolean,
      default: true,
    },
    draggable: {
      type: Boolean,
      default: true,
    },
  },
  components: {
    SvgIcon,
  },
  emits: ['edit', 'delete', 'copy'],
  setup(props, { emit }) {
    const { t } = useI18n()

    // Handle edit
    const handleEdit = () => {
      if (props.editable) {
        emit('edit', props.variable)
      }
    }

    // Handle delete
    const handleDelete = () => {
      emit('delete', props.variable.id)
    }

    // Handle copy variable reference
    const handleCopy = (event: Event) => {
      event.stopPropagation() // 阻止事件冒泡
      const reference = `$\{{variables.${props.variable.id}}}`
      navigator.clipboard.writeText(reference).then(() => {
        Message({ theme: 'success', message: 'Variable reference copied to clipboard' })
        emit('copy', reference)
      })
    }

    // Handle edit button click
    const handleEditClick = (event: Event) => {
      event.stopPropagation() // 阻止事件冒泡
      handleEdit()
    }

    // Get value display
    const getValueDisplay = () => {
      const { defaultValue, type } = props.variable
      if (type === 'BOOLEAN') {
        return String(defaultValue)
      }
      if (Array.isArray(defaultValue)) {
        return defaultValue.join(', ')
      }
      return defaultValue || '-'
    }

    return () => (
      <div class={styles.variableItem}>
        <div class={styles.variableInfo} onClick={handleEdit}>
          <div class={styles.variableHeader}>
            <span class={styles.variableId}>{props.variable.id}</span>
            <span class={styles.variableType}>{props.variable.type}</span>
          </div>
          <div class={styles.variableName}>{props.variable.name}</div>
          {props.variable.desc && <div class={styles.variableDesc}>{props.variable.desc}</div>}
          <div class={styles.variableValue}>
            <span class={styles.valueLabel}>{t('flow.variable.defaultValue')}:</span>
            <span class={styles.valueText}>{getValueDisplay()}</span>
          </div>
        </div>

        {props.draggable && (
          <div
            class={[styles.dragHandle, props.dragHandleCls]}
            onMousedown={(e) => e.stopPropagation()}
            title="Drag to reorder"
          >
            <SvgIcon name="drag-small" size={24} />
          </div>
        )}

        {/* Variable actions - visible on hover */}
        {props.editable && (
          <div class={styles.variableActions}>
            <span class={styles.actionItem} onClick={handleCopy}>
              <SvgIcon name="copy" />
            </span>
            <span class={styles.actionItem} onClick={handleEditClick}>
              <SvgIcon name="arrows-up" size={18} />
            </span>
            <PopConfirm
              title={t('flow.variable.deleteConfirm')}
              trigger="click"
              content={t('flow.variable.deleteConfirmContent', { name: props.variable.name })}
              onConfirm={handleDelete}
            >
              <span class={styles.actionItem}>
                <SvgIcon name="minus-circle" class={styles.dangerAction} />
              </span>
            </PopConfirm>
          </div>
        )}
      </div>
    )
  },
})
