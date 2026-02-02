import { searchFlowByName, type SimplePipelineInfo } from '@/api/flowContentList'
import { ROUTE_NAMES } from '@/constants/routes'
import { debounce } from '@/utils/util'
import { Input, Loading, Popover } from 'bkui-vue'
import { defineComponent, onMounted, type PropType, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { SvgIcon } from '../SvgIcon'
import styles from './FlowSelector.module.css'

export default defineComponent({
  name: 'FlowSelector',
  props: {
    projectId: {
      type: String,
      required: true,
    },
    currentFlowId: {
      type: String,
      required: true,
    },
    currentFlowName: {
      type: String,
      default: '',
    },
    onNameClick: {
      type: Function as PropType<() => void>,
    },
  },
  setup(props) {
    const { t } = useI18n()
    const router = useRouter()

    const isPopoverShow = ref(false)
    const searchKey = ref('')
    const loading = ref(false)
    const flowList = ref<SimplePipelineInfo[]>([])

    // 获取工作流列表（支持远程搜索）
    const fetchFlowList = async (keyword?: string) => {
      if (!props.projectId) return

      loading.value = true
      try {
        const list = await searchFlowByName(props.projectId, keyword || '')
        // 确保当前流水线在列表最前面
        flowList.value = generateFlowList(list)
      } catch (error) {
        console.error('Failed to fetch flow list:', error)
        flowList.value = []
      } finally {
        loading.value = false
      }
    }

    // 生成流水线列表，确保当前流水线在最前面
    const generateFlowList = (list: SimplePipelineInfo[]) => {
      if (!props.currentFlowId || !props.currentFlowName) {
        return list
      }
      const currentFlow = {
        pipelineId: props.currentFlowId,
        pipelineName: props.currentFlowName,
      }
      return [
        currentFlow,
        ...list.filter((item) => item.pipelineId !== props.currentFlowId),
      ]
    }

    // 防抖搜索
    const debouncedSearch = debounce((keyword: string) => {
      fetchFlowList(keyword)
    }, 300)

    // 监听搜索关键词变化，触发远程搜索
    watch(searchKey, (newVal) => {
      debouncedSearch(newVal)
    })

    // 选择工作流
    const handleSelectFlow = (flow: SimplePipelineInfo) => {
      if (flow.pipelineId === props.currentFlowId) {
        isPopoverShow.value = false
        return
      }

      // 跳转到 flow 基础路径，触发 beforeEnter 守卫获取最新版本并重定向
      router.push({
        name: ROUTE_NAMES.FLOW_DETAIL,
        params: {
          projectId: props.projectId,
          flowId: flow.pipelineId,
        },
      })
      isPopoverShow.value = false
    }

    // 点击工作流名称
    const handleNameClick = () => {
      if (props.onNameClick) {
        props.onNameClick()
      }
    }

    // 监听 popover 显示状态
    watch(isPopoverShow, (show) => {
      if (show) {
        // 每次打开时重新获取列表
        searchKey.value = ''
        fetchFlowList()
      }
    })

    // 组件挂载时预加载数据
    onMounted(() => {
      // 延迟加载，不阻塞初始渲染
      setTimeout(() => {
        if (props.projectId) {
          fetchFlowList()
        }
      }, 500)
    })

    // 渲染工作流列表项
    const renderFlowItem = (flow: SimplePipelineInfo) => {
      const isActive = flow.pipelineId === props.currentFlowId

      return (
        <div
          key={flow.pipelineId}
          class={[styles.flowItem, isActive && styles.flowItemActive]}
          onClick={() => handleSelectFlow(flow)}
        >
          <span class={styles.flowItemName} title={flow.pipelineName}>
            {flow.pipelineName}
          </span>
          {isActive && <SvgIcon name="check-line" class={styles.checkIcon} size={16} />}
        </div>
      )
    }

    // 渲染 popover 内容
    const renderPopoverContent = () => (
      <div class={styles.flowSelectorPopover}>
        <Input
        v-model={searchKey.value}
        placeholder={t('flow.common.search')}
        clearable
        type="search"
        />

        <div class={styles.flowListWrapper}>
          <Loading loading={loading.value} mode="spin" size="small">
            {flowList.value.length > 0 ? (
              <div class={styles.flowList}>
                {flowList.value.map((flow) => renderFlowItem(flow))}
              </div>
            ) : (
              <div class={styles.emptyState}>
                {searchKey.value
                  ? t('flow.searchResultsEmpty')
                  : t('flow.common.noData')}
              </div>
            )}
          </Loading>
        </div>
      </div>
    )

    return () => (
      <Popover
        trigger="click"
        theme="light"
        placement="bottom-start"
        arrow={false}
        is-show={isPopoverShow.value}
        onUpdate:isShow={(val: boolean) => (isPopoverShow.value = val)}
      >
        {{
          default: () => (
            <div class={styles.flowSelector}>
              <span
                class={[styles.flowName, props.onNameClick && styles.clickable]}
                title={props.currentFlowName}
                onClick={handleNameClick}
              >
                {props.currentFlowName || '--'}
              </span>
              <SvgIcon name="exchange-line" class={styles.exchangeIcon} size={16} />
            </div>
          ),
          content: renderPopoverContent,
        }}
      </Popover>
    )
  },
})
