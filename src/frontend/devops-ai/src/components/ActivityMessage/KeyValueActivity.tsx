import { Table } from 'bkui-vue'
import { computed, defineComponent, PropType } from 'vue'
import styles from './ActivityMessage.module.css'
import ActivityRichValue from './ActivityRichValue'
import { KeyValueContent } from './types'

const KV_COLUMNS = [
  { field: 'label', label: '属性', width: 180 },
  {
    field: 'displayValue',
    label: '值',
    render: ({ cell }: { cell: unknown }) => <ActivityRichValue cell={cell} />,
  },
]

export default defineComponent({
  name: 'KeyValueActivity',
  props: {
    content: { type: Object as PropType<KeyValueContent>, required: true },
  },
  setup(props) {
    const rows = computed(() => {
      if (Array.isArray(props.content.items)) {
        return props.content.items.map(item => ({
          ...item,
          displayValue: String(item.value),
        }));
      } else if (Object.keys(props.content.data).length === 0) return [];
      
      return Object.keys(props.content.data).map(item => ({
        label: item,
        displayValue: String(props.content.data[item]),
      }));
    })

    return () => (
      <div class={styles.activityCard}>
        <div class={styles.cardTitle}>{props.content.title}</div>
        <Table
          data={rows.value}
          columns={KV_COLUMNS}
          border={['row']}
          showOverflowTooltip
        />
      </div>
    )
  },
})
