import { Pagination, Table } from 'bkui-vue'
import { computed, defineComponent, PropType, ref, watch } from 'vue'
import styles from './ActivityMessage.module.css'
import ActivityRichValue from './ActivityRichValue'
import { DataTableContent } from './types'

export default defineComponent({
  name: 'DataTableActivity',
  props: {
    content: { type: Object as PropType<DataTableContent>, required: true },
  },
  setup(props) {
    const columns = computed(() =>
      props.content.columns.map(col => ({
        field: col.key,
        label: col.label,
        ...(col.width && { width: col.width }),
        render: ({ cell }: { cell: unknown }) => <ActivityRichValue cell={cell} />,
      })),
    )
    const isServerPaginated = computed(() => {
      const { pagination, rows } = props.content
      if (!pagination) return true
      return pagination.total !== rows.length
    })

    const currentPage = ref(props.content.pagination?.page ?? 1)
    const currentLimit = ref(props.content.pagination?.pageSize ?? 10)

    watch(
      () => props.content.pagination,
      (val) => {
        if (val) {
          currentPage.value = val.page ?? 1
          currentLimit.value = val.pageSize ?? 10
        }
      },
    )

    const paginatedRows = computed(() => {
      const { rows } = props.content
      if (isServerPaginated.value) return rows
      const start = (currentPage.value - 1) * currentLimit.value
      return rows.slice(start, start + currentLimit.value)
    })


    function handlePageChange(page: number) {
      currentPage.value = page
    }

    function handleLimitChange(limit: number) {
      currentLimit.value = limit
      currentPage.value = 1
    }

    return () => {
      const { title, summary, pagination } = props.content

      return (
        <div class={styles.activityCard}>
          <div class={styles.cardTitle}>{title}</div>
          {summary && <div class={styles.cardSummary}>{summary}</div>}
          <Table
            data={paginatedRows.value}
            columns={columns.value}
            border={['row']}
            stripe
            showOverflowTooltip
          />
          {pagination && !isServerPaginated.value && (
            <Pagination
              class={styles.pagination}
              modelValue={currentPage.value}
              count={pagination.total}
              limit={currentLimit.value}
              onChange={handlePageChange}
              onLimitChange={handleLimitChange}
              small
              showTotalCount
            />
          )}
        </div>
      )
    }
  },
})
