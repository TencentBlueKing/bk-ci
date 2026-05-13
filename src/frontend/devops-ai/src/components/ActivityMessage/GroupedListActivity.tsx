import { defineComponent, type PropType } from 'vue'
import BkCollapse from 'bkui-vue/lib/collapse'
import { Table } from 'bkui-vue'
import type { GroupedListContent } from './types'
import ActivityRichValue from './ActivityRichValue'
import styles from './ActivityMessage.module.css'

const BkCollapsePanel = BkCollapse.CollapsePanel

function deriveColumns(items: Array<Record<string, unknown>>) {
  if (items.length === 0) return []
  return Object.keys(items[0]).map(key => ({
    field: key,
    label: key,
    render: ({ cell }: { cell: unknown }) => <ActivityRichValue cell={cell} />,
  }))
}

export default defineComponent({
  name: 'GroupedListActivity',
  props: {
    content: { type: Object as PropType<GroupedListContent>, required: true },
  },
  setup(props) {
    return () => {
      const { title, groups } = props.content

      return (
        <div class={styles.activityCard}>
          <div class={styles.cardTitle}>{title}</div>
          <BkCollapse>
            {groups.map((group, i) => (
              <BkCollapsePanel
                key={i}
                name={i}
                title={group.label}
                v-slots={{
                  content: () =>
                    group.items.length > 0 ? (
                      <Table
                        data={group.items}
                        columns={deriveColumns(group.items)}
                        border={['row']}
                        stripe
                        showOverflowTooltip
                      />
                    ) : null,
                }}
              />
            ))}
          </BkCollapse>
        </div>
      )
    }
  },
})
