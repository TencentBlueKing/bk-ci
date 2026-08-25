import { searchFlowByName, type SimplePipelineInfo } from '@/api/flowContentList'
import { ROUTE_NAMES } from '@/constants/routes'
import { debounce } from '@/utils/util'
import { Select } from 'bkui-vue'
import { defineComponent, onMounted, type PropType, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { SvgIcon } from '../SvgIcon'
import styles from './FlowSelector.module.css'

const { Option } = Select

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

    const selectedFlowId = ref(props.currentFlowId)
    const loading = ref(false)
    const flowList = ref<SimplePipelineInfo[]>([])

    watch(
      () => props.currentFlowId,
      (flowId) => {
        selectedFlowId.value = flowId
      },
    )

    const fetchFlowList = async (keyword?: string) => {
      if (!props.projectId) return

      loading.value = true
      try {
        const list = await searchFlowByName(props.projectId, keyword || '')
        flowList.value = generateFlowList(list)
      } catch (error) {
        console.error('Failed to fetch flow list:', error)
        flowList.value = []
      } finally {
        loading.value = false
      }
    }

    const generateFlowList = (list: SimplePipelineInfo[]) => {
      if (!props.currentFlowId || !props.currentFlowName) {
        return list
      }
      const currentFlow = {
        pipelineId: props.currentFlowId,
        pipelineName: props.currentFlowName,
      }
      return [currentFlow, ...list.filter((item) => item.pipelineId !== props.currentFlowId)]
    }

    const debouncedSearch = debounce((keyword: string) => {
      fetchFlowList(keyword)
    }, 300)

    const handleSearchFlow = (keyword: string) => {
      debouncedSearch(keyword)
    }

    const handleSelectFlow = (flowId: string) => {
      if (flowId === props.currentFlowId) return

      router.push({
        name: ROUTE_NAMES.FLOW_DETAIL,
        params: {
          projectId: props.projectId,
          flowId,
        },
      })
    }

    const handleSelectorToggle = (isOpen: boolean) => {
      if (isOpen) {
        fetchFlowList()
      }
    }

    const handleNameClick = () => {
      props.onNameClick?.()
    }

    onMounted(() => {
      setTimeout(() => {
        if (props.projectId) {
          fetchFlowList()
        }
      }, 500)
    })

    const renderFlowOption = (flow: SimplePipelineInfo) => {
      const isActive = flow.pipelineId === props.currentFlowId

      return (
        <Option key={flow.pipelineId} value={flow.pipelineId} label={flow.pipelineName}>
          <div class={styles.flowOption}>
            <span class={styles.flowOptionName} title={flow.pipelineName}>
              {flow.pipelineName}
            </span>
            {isActive ? <SvgIcon name="check-line" class={styles.checkIcon} size={12} /> : null}
          </div>
        </Option>
      )
    }

    return () => (
      <Select
        v-model={selectedFlowId.value}
        class={styles.flowSelector}
        filterable
        remoteMethod={handleSearchFlow}
        loading={loading.value}
        clearable={false}
        popoverMinWidth={240}
        popoverOptions={{ extCls: styles.flowSelectorPopover }}
        searchPlaceholder={t('flow.common.search')}
        onChange={handleSelectFlow}
        onToggle={handleSelectorToggle}
      >
        {{
          trigger: () => (
            <div class={styles.flowSelectorTrigger}>
              <span
                class={[styles.flowName, props.onNameClick && styles.clickable]}
                title={props.currentFlowName}
                onClick={props.onNameClick ? handleNameClick : undefined}
              >
                {props.currentFlowName || '--'}
              </span>
              <SvgIcon name="exchange-line" class={styles.exchangeIcon} size={16} />
            </div>
          ),
          default: () => flowList.value.map((flow) => renderFlowOption(flow)),
        }}
      </Select>
    )
  },
})
