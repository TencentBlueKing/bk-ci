import { computed, defineComponent, type PropType } from 'vue'
import { useI18n } from 'vue-i18n'
import { Message } from 'bkui-vue'
import { SvgIcon } from '@/components/SvgIcon'
import { VariablePanelTab, type PluginOutputVariable, type SystemVariable } from '@/types/variable'
import styles from './VariableItem.module.css'

export interface ReadOnlyVariableItemProps {
  variable: PluginOutputVariable | SystemVariable
  type: VariablePanelTab
}

export default defineComponent({
  name: 'ReadOnlyVariableItem',
  props: {
    variable: {
      type: Object as PropType<PluginOutputVariable | SystemVariable>,
      required: true,
    },
    type: {
      type: String as PropType<VariablePanelTab>,
      required: true,
    },
  },
  components: {
    SvgIcon,
  },
  emits: ['copy'],
  setup(props, { emit }) {
    const { t } = useI18n()
    const isPluginVar = computed(() => props.type === VariablePanelTab.PLUGIN_OUTPUT)

    // Get reference format based on type
    const getReference = () => {
      if (isPluginVar.value) {
        const pluginVar = props.variable as PluginOutputVariable
        return `\${{ steps.${pluginVar.stepId}.outputs.${pluginVar.id} }}`
      }
      return `\${{ ci.${props.variable.id} }}`
    }

    // Handle copy reference
    const handleCopy = (event: Event) => {
      event.stopPropagation()
      const reference = getReference()
      navigator.clipboard.writeText(reference).then(() => {
        Message({ theme: 'success', message: t('flow.variable.copySuccess') })
        emit('copy', reference)
      })
    }

    return () => (
      <div class={styles.variableItem}>
        <div class={styles.variableInfo}>
          <div class={styles.variableHeader}>
            <span class={styles.variableId}>
              {props.variable.name ?? props.variable.id ?? '--'}
            </span>
          </div>
          {props.variable.desc && <div class={styles.variableDesc}>{props.variable.desc}</div>}
        </div>

        <div class={styles.variableActions}>
          <span
            class={styles.actionItem}
            onClick={handleCopy}
            title={t('flow.variable.copyReference')}
          >
            <SvgIcon name="copy" />
          </span>
        </div>
      </div>
    )
  },
})
