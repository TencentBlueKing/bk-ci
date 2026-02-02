import { defineComponent, onBeforeMount } from 'vue'
import { useI18n } from 'vue-i18n'
import { DatePicker, Timeline } from 'bkui-vue'
import SearchSelect from '@blueking/search-select-v3'
import InfiniteScroll from '@/components/InfiniteScroll'
import EmptyTableStatus from '@/components/EmptyTable'
import { useTriggerRecordData, type Styles } from '@/hooks/useTriggerRecordData'
import type { TriggerRecordItem, QueryListFunction } from '@/api/triggerRecord'
import styles from './TriggerRecord.module.css'

export default defineComponent({
  name: 'TriggerRecord',

  props: {
    projectId: {
      type: String,
      required: true,
    },
  },

  setup(props) {
    const { t } = useI18n()
    const {
      dateTimeRange,
      searchValue,
      shortcuts,
      filterData,
      searchPlaceHolder,
      emptyStatusType,
      init,
      generateTimelineList,
      fetchTriggerEventList,
      handleFilterChange,
      handleClearSearch,
      handleClearCalendar,
      handleSearchChange,
    } = useTriggerRecordData(styles as Styles)

    onBeforeMount(() => {
      init()
    })

    return () => (
      <div class={styles.triggerRecord}>
        <InfiniteScroll
          dataFetcher={fetchTriggerEventList}
          scrollBoxClassName={styles.triggerRecordScrollContainer as string}
          pageSize={36}
          initialPage={1}
        >
          {{
            default: ({
              list,
              queryList,
            }: {
              list: TriggerRecordItem[]
              queryList: QueryListFunction
            }) => {
              const timelineList = generateTimelineList(list)
              return (
                <>
                  <header class={styles.triggerEventFilterBar}>
                    <DatePicker
                      v-model={dateTimeRange.value}
                      placeholder={t('flow.triggerRecord.pickTimeRange')}
                      shortcuts={shortcuts.value}
                      type="datetimerange"
                      use-shortcut-text
                      onClear={() => handleClearCalendar(queryList)}
                      onPick-success={() => handleFilterChange(queryList)}
                    />
                    <SearchSelect
                      modelValue={searchValue.value}
                      data={filterData.value}
                      placeholder={searchPlaceHolder.value}
                      class={styles.searchInput}
                      uniqueSelect
                      onUpdate:modelValue={(value) => handleSearchChange(value, queryList)}
                      onSearch={() => handleFilterChange(queryList)}
                    />
                  </header>

                  <div class={styles.triggerRecordScrollContainer} style={list.length ? {}: {'align-items': 'center'}}>
                    {list.length === 0 ? (
                      <EmptyTableStatus
                        class={styles.emptyStatus}
                        type={emptyStatusType.value}
                        onClear={() => handleClearSearch(queryList)}
                      />
                    ) : (
                      <Timeline list={timelineList} />
                    )}
                  </div>
                </>
              )
            },
          }}
        </InfiniteScroll>
      </div>
    )
  },
})
