import type { Element } from '@/api/flowModel'
import TriggerPropertyPanel from '@/components/TriggerPropertyPanel'
import { Button, Table } from 'bkui-vue'
import type { Column } from 'bkui-vue/lib/table/props'
import { computed, defineComponent, ref, type PropType } from 'vue'
import { useI18n } from 'vue-i18n'
import styles from './TriggerEventContent.module.css'

interface TriggerEvent {
  name: string
  icon: string
  version: string
  enabled: boolean
  type: string
  element: Element // 原始 Element 数据
}

export default defineComponent({
  name: 'TriggerEventContent',
  props: {
    triggerEvents: {
      type: Array as PropType<TriggerEvent[]>,
      default: () => [],
    },
  },
  setup(props) {
    const { t } = useI18n()

    // 侧边栏状态
    const sidebarVisible = ref(false)
    const selectedElement = ref<Element | null>(null)

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

    // 处理行点击
    const handleRowClick = (event: Event, row: TriggerEvent) => {
      selectedElement.value = row.element
      sidebarVisible.value = true
    }

    // 关闭侧边栏
    const handleCloseSidebar = () => {
      sidebarVisible.value = false
      selectedElement.value = null
    }

    return () => (
      <div class={styles.triggerEventContent}>
        <Table
          align="left"
          columns={columns.value as Column[]}
          data={props.triggerEvents}
          class={styles.table}
          onRowClick={handleRowClick}
        />
        <TriggerPropertyPanel
          visible={sidebarVisible.value}
          element={selectedElement.value}
          readonly={true}
          onUpdate:visible={handleCloseSidebar}
        />
      </div>
    )
  },
})
