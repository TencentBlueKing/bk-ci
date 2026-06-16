import { computed, defineComponent, ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { Select, Table, Loading } from 'bkui-vue'
import EmptyTableStatus from '@/components/EmptyTable'
import type { Column } from 'bkui-vue/lib/table/props'
import { useTableHeight } from '@/hooks/useTableHeight'
import { convertTime } from '@/utils/util'
import { useChangeLog } from '@/hooks/useChangeLog'
import styles from './ChangeLog.module.css'

export default defineComponent({
  name: 'ChangeLog',
  setup() {
    const { t } = useI18n()
    const tableContainerRef = ref<HTMLDivElement>()
    const { maxHeight } = useTableHeight(tableContainerRef)
    const {
      operatorOptions,
      changeLogList,
      loading,
      pagination,
      searchValue,
      handleSelectChange,
      handlePageChange,
      handleLimitChange,
      init,
    } = useChangeLog()

    const tableColumn = computed(
      () =>
        [
          {
            field: 'operator',
            label: t('flow.operator.operator'),
          },
          {
            field: 'operateTime',
            label: t('flow.operator.operateTime'),
            render: ({ row }: { row: any }) => convertTime(row.operateTime),
          },
          {
            field: 'operationLogStr',
            label: t('flow.operator.operateLogDesc'),
          },
        ] as Column[],
    )

    onMounted(() => {
      init()
    })

    return () => (
      <div class={styles.changeLog}>
        <Select
          v-model={searchValue.value}
          class={styles.searchInput}
          placeholder={t('flow.operator.operator')}
          onChange={handleSelectChange}
        >
          {operatorOptions.value.map((i) => (
            <Select.Option id={i.value} name={i.name} key={i.value}></Select.Option>
          ))}
        </Select>
        <div ref={tableContainerRef} class={styles.changeLogContainer}>
          <Loading loading={loading.value} mode="spin" theme="primary" size="small">
            <Table
              data={changeLogList.value}
              columns={tableColumn.value}
              max-height={maxHeight.value}
              border={['row', 'outer']}
              remote-pagination
              pagination={pagination.value}
              onPageValueChange={handlePageChange}
              onPageLimitChange={handleLimitChange}
            >
              {{
                empty: () => <EmptyTableStatus type="empty" />,
              }}
            </Table>
          </Loading>
        </div>
      </div>
    )
  },
})
