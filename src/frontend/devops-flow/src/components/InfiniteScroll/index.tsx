import { defineComponent, ref, onMounted, onBeforeUnmount, provide, type PropType } from 'vue'
import { throttle } from '@/utils/util'
import { useI18n } from 'vue-i18n'
import { Loading } from 'bkui-vue'
import styles from './InfiniteScroll.module.css'

const SCROLL_THRESHOLD = 250

// 列表项接口
interface ListItem {
  [key: string]: any
}

// 数据获取响应接口
export interface FetchResponse {
  records: ListItem[]
  count: number
}

// 数据获取函数类型
export type DataFetcher = (page: number, pageSize: number) => Promise<FetchResponse>

// 插槽属性接口
export interface InfiniteScrollSlots {
  default: (props: {
    list: ListItem[]
    isLoading: boolean
    isLoadingMore: boolean
    queryList: (
      page?: number,
      pageSize?: number,
      isRefresh?: boolean,
    ) => Promise<FetchResponse | undefined>
    setScrollTop: (top: number) => void
    animateScroll: (top: number, speed?: number) => void
    totals: number
  }) => any
}

// 更新列表函数类型
export type UpdateListFunction = (isRefresh?: boolean) => Promise<FetchResponse | undefined>

export default defineComponent({
  name: 'InfiniteScrollList',

  props: {
    // 数据获取函数
    dataFetcher: {
      type: Function as PropType<DataFetcher>,
      required: true,
      default: () => () => Promise.resolve({ records: [], count: 0 }),
    },
    // 滚动容器类名
    scrollBoxClassName: {
      type: String,
      required: true,
    },
    // 每页大小
    pageSize: {
      type: Number,
      default: 24,
    },
    // 初始页码
    initialPage: {
      type: Number,
      default: 1,
    },
  },

  setup(props, { slots }) {
    const { t } = useI18n()

    // 响应式数据
    const isLoading = ref(true)
    const isLoadingMore = ref(false)
    const list = ref<ListItem[]>([])
    const currentPage = ref(props.initialPage)
    const scrollTop = ref(0)
    const hasNext = ref(false)
    const totals = ref(0)
    const throttleScroll = ref<((e: Event) => void) | null>(null)

    // 更新列表方法 - 提供给子组件使用
    const updateList: UpdateListFunction = async (isRefresh = false) => {
      const len = list.value.length
      const res = await queryList(1, len > props.pageSize ? len : props.pageSize, isRefresh)
      list.value = res?.records || []
      return res
    }

    // 提供 updateList 方法给子组件
    provide('updateList', updateList)

    // 设置滚动位置
    const setScrollTop = (top: number) => {
      scrollTop.value = top
    }

    // 动画滚动到指定位置
    const animateScroll = (top: number, speed = 0) => {
      const scrollTable = document.querySelector(`.${props.scrollBoxClassName}`)
      if (scrollTable && top !== scrollTable.scrollTop) {
        scrollTable.scrollTo(0, top)
      }
    }

    // 处理滚动事件
    const handleScroll = (e: Event) => {
      const target = e.target as HTMLElement
      if (!target || !(target instanceof HTMLElement)) return
      setScrollTop(target.scrollTop)
      const offset = target.scrollHeight - (target.offsetHeight + target.scrollTop)
      if (offset <= SCROLL_THRESHOLD && hasNext.value && !isLoadingMore.value) {
        scrollLoadMore()
      }
    }

    // 获取数据
    const fetchData = async (page = 1, pageSize = props.pageSize) => {
      const res = await props.dataFetcher(page, pageSize)
      if (res) {
        list.value = page === 1 ? res.records : [...list.value, ...res.records]

        currentPage.value = Math.ceil(list.value.length / pageSize)
        hasNext.value = list.value.length < res.count
        totals.value = res.count
        return res
      }
    }

    // 查询列表数据
    const queryList = async (page = 1, pageSize = props.pageSize, isRefresh = false) => {
      try {
        isLoading.value = !isRefresh
        const res = await fetchData(page, pageSize)
        return res
      } catch (e) {
        console.error('InfiniteScroll queryList error:', e)
      } finally {
        isLoading.value = false
      }
    }

    // 滚动加载更多
    const scrollLoadMore = async () => {
      try {
        isLoadingMore.value = true
        await fetchData(currentPage.value + 1, props.pageSize)
      } catch (e) {
        console.error('InfiniteScroll scrollLoadMore error:', e)
        // 这里可以添加消息提示
        // this.$showTips({
        //   message: t('history.loadingErr'),
        //   theme: 'error'
        // })
      } finally {
        isLoadingMore.value = false
      }
    }

    onMounted(() => {
      const { scrollBoxClassName } = props
      const scrollTable = document.querySelector(`.${scrollBoxClassName}`)

      // 初始化数据
      queryList(props.initialPage, props.pageSize)

      // 添加滚动监听
      throttleScroll.value = throttle(handleScroll, 500)
      if (scrollTable && throttleScroll.value) {
        scrollTable.addEventListener('scroll', throttleScroll.value)
      }
    })

    onBeforeUnmount(() => {
      const scrollTable = document.querySelector(`.${props.scrollBoxClassName}`)
      if (scrollTable && throttleScroll.value) {
        scrollTable.removeEventListener('scroll', throttleScroll.value)
      }
    })

    return () => (
      <div style={{ height: '100%' }}>
        <Loading loading={isLoading.value} style={{ height: '100%' }}>
          {slots.default?.({
            list: list.value,
            isLoading: isLoading.value,
            isLoadingMore: isLoadingMore.value,
            queryList,
            setScrollTop,
            animateScroll,
            totals: totals.value,
          })}
          <div
            class={styles.loadingMore}
            v-bkloading={{
              loading: isLoadingMore.value,
              title: t('flow.triggerRecord.loadingTips'),
              size: 'small',
            }}
          ></div>
        </Loading>
      </div>
    )
  },
})
