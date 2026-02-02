import { defineComponent, computed, type PropType } from 'vue'
import { useI18n } from 'vue-i18n'
import { Table, Button } from 'bkui-vue'
import styles from './TriggerEventContent.module.css'
import type { Column } from 'bkui-vue/lib/table/props'

interface TriggerEvent {
  name: string
  icon: string
  version: string
  enabled: boolean
  type: string
}

export default defineComponent({
  name: 'TriggerEventContent',
  props: {
    triggerEvents: {
      type: Array as PropType<TriggerEvent[]>,
      default: '',
    },
  },
  setup(props) {
    const { t } = useI18n()

    // 表格列定义
    const columns = computed(() => [
      {
        label: t('flow.content.event'),
        field: 'name',
      },
      {
        label: t('flow.content.version'),
        field: 'version',
      },
      {
        label: t('flow.content.enableStatus'),
        field: 'enabled',
        render: ({ row }: { row: TriggerEvent }) => (
          <Button
            theme={row.enabled ? 'primary' : 'default'}
            size="small"
            disabled
            class={styles.statusButton}
          >
            {row.enabled ? t('flow.content.enable') : t('flow.content.disable')}
          </Button>
        ),
      },
    ])

    return () => (
      <div class={styles.triggerEventContent}>
        <Table
          align="left"
          columns={columns.value as Column[]}
          data={props.triggerEvents}
          class={styles.table}
        />
      </div>
    )
  },
})
