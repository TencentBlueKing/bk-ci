import type { MenuItem } from '@/api/flowContentList'
import { deleteContent, toggleFlowFavorite } from '@/api/flowContentList'
import { CommonHeader } from '@/components/CommonHeader'
import { FLOW_GROUP_TYPES } from '@/constants/flowGroup'
import { useDeleteConfirm } from '@/hooks/useDeleteConfirm'
import { useFlowInfoStore } from '@/stores/flowInfoStore'
import type { FlowInfo, FlowVersion } from '@/types/flow'
import { Button, Message, Select, Tag } from 'bkui-vue'
import type { PropType } from 'vue'
import { computed, defineComponent, h, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import ExtMenu from '../ExtMenu'
import { SvgIcon } from '../SvgIcon'
import { ExportFlowDialog } from './ExportFlowDialog'
import FlowSelector from './FlowSelector'
import styles from './index.module.css'

const { Option } = Select

export interface VersionOption {
  value: string
  label: string
  isLatest?: boolean
}

export const FlowHeader = defineComponent({
  name: 'FlowHeader',
  props: {
    loading: {
      type: Boolean,
      default: false,
    },
    flowInfo: {
      type: Object as PropType<FlowInfo>,
      required: true,
    },
    versionList: {
      type: Array as PropType<FlowVersion[]>,
      required: true,
    },
    onEdit: {
      type: Function as PropType<() => void>,
    },
    onExecute: {
      type: Function as PropType<() => void>,
    },
    onVersionChange: {
      type: Function as PropType<(version: number) => void>,
    },
  },
  setup(props) {
    const { t } = useI18n()
    const router = useRouter()
    const route = useRoute()
    const selectedVersion = ref(Number(route.params.version))
    const { showDeleteConfirm } = useDeleteConfirm()
    const flowInfoStore = useFlowInfoStore()

    // 监听路由参数变化，更新选中的版本
    watch(
      () => route.params.version,
      (newVersion) => {
        if (newVersion) {
          selectedVersion.value = Number(newVersion)
        }
      }
    )

    // 对话框显示状态
    const showExportDialog = ref(false)

    const flowList = {
      name: 'flowList',
      params: {
        groupId: FLOW_GROUP_TYPES.ALL_FLOWS,
      },
    }

    const projectId = computed(() => route.params.projectId as string)
    const flowId = computed(() => props.flowInfo?.pipelineId || '')
    const flowName = computed(() => props.flowInfo?.pipelineName || '')

    // 收藏/取消收藏处理
    const handleCollect = async (data: FlowInfo) => {
      const isCollected = data.hasCollect
      const newCollectState = !isCollected
      try {
        await toggleFlowFavorite(projectId.value, data.pipelineId, newCollectState)
        const action = newCollectState ? t('flow.content.favorite') : t('flow.content.uncollect')
        Message({ theme: 'success', message: `${action}${t('flow.common.success')}` })
        // 刷新 flowInfo 以更新收藏状态
        flowInfoStore.getFlowInfo()
      } catch (error: any) {
        Message({ theme: 'error', message: error?.message || t('flow.common.failed') })
      }
    }

    // 计算收藏按钮文本
    const collectText = computed(() => {
      return props.flowInfo?.hasCollect ? t('flow.content.uncollect') : t('flow.content.favorite')
    })

    const moreActions = computed<MenuItem[]>(() => [
      {
        text: collectText.value,
        handler: (data: any) => handleCollect(data),
      },
      {
        text: t('flow.actions.rename'),
        handler: () => {
          props.onEdit?.()
        },
      },
      {
        text: t('flow.content.export'),
        handler: () => {
          showExportDialog.value = true
        },
      },
      {
        text: t('flow.actions.delete'),
        handler: (data: any) => {
          const objectName = data?.pipelineName || data?.pipelineId
          showDeleteConfirm({
            message: () => [
              `${t('flow.content.confirmDeleteFlow')}\n${t('flow.content.operationObject')}: `,
              h(
                'strong',
                { style: 'font-weight: 700; color: var(--color-text-primary);' },
                objectName,
              ),
            ],
            onConfirm: async () => {
              await deleteContent({
                projectId: projectId.value,
                pipelineIds: [data?.pipelineId],
              })
              router.push(flowList)
            },
          })
        },
      },
    ])

    const currentVersion = computed(() => {
      console.log('props.versionList:', props.versionList.find((v) => v.version === selectedVersion.value))
      return props.versionList.find((v) => v.version === selectedVersion.value)
    })

    const renderTag = () => {
      return (
        <Tag theme="success" size="small" class={styles.tag}>
          {t('flow.content.latest')}
        </Tag>
      )
    }

    const renderCheckIcon = (isLatest: boolean = false) => {
      return (
        <SvgIcon
          name="check-circle"
          class={[styles.checkIcon, isLatest && styles.latestCheckIcon]}
        />
      )
    }

    return () => (
        <>
          <CommonHeader loading={props.loading} workflowName={props.flowInfo?.pipelineName }>
            {{
              'workflow-selector': () => (
                <FlowSelector
                  projectId={route.params.projectId as string}
                  currentFlowId={props.flowInfo?.pipelineId || (route.params.flowId as string)}
                  currentFlowName={props.flowInfo?.pipelineName}
                />
              ),
              'version-selector': () => (
                <Select
                  v-model={selectedVersion.value}
                  onChange={props.onVersionChange}
                  class={styles.versionSelector}
                >
                  {{
                    trigger: () => (
                      <span class={styles.versionTrigger}>
                        {renderCheckIcon(currentVersion.value?.isLatest)}
                        {currentVersion.value?.versionName || '--'}
                        {currentVersion.value?.isLatest && renderTag()}
                        <SvgIcon name="angle-down" class={styles.versionSelectToggleIcon} />
                      </span>
                    ),
                    default: () =>
                      props.versionList.map((version) => (
                        <Option
                          key={version.version}
                          value={version.version}
                          label={version.versionName}
                        >
                          <div class={styles.versionOption}>
                            {renderCheckIcon(version.isLatest)}
                            <span>{version.versionName}</span>
                            {version.isLatest && renderTag()}
                          </div>
                        </Option>
                      )),
                  }}
                </Select>
              ),
              actions: () => (
                <>
                  <Button onClick={props.onEdit}>{t('flow.content.edit')}</Button>
                  <Button theme="primary" onClick={props.onExecute}>
                    {t('flow.content.execute')}
                  </Button>
                  <ExtMenu data={props.flowInfo} config={moreActions.value} />
                </>
              ),
            }}
          </CommonHeader>

          {/* 导出对话框 */}
          <ExportFlowDialog
            v-model:isShow={showExportDialog.value}
            flowId={flowId.value}
            flowName={flowName.value}
          />
        </>
      )
  },
})
