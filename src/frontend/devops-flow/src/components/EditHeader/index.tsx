import { ReleaseSlider } from '@/components/ReleaseSlider'
import { FLOW_GROUP_TYPES } from '@/constants/flowGroup'
import { ROUTE_NAMES } from '@/constants/routes'
import { useFlowModel } from '@/hooks/useFlowModel'
import { useUIStore } from '@/stores/ui'
import { VERSION_STATUS_ENUM } from '@/utils/flowConst'
import { Button, Message, Tag } from 'bkui-vue'
import { computed, defineComponent, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { useFlowInfo } from '../../hooks/useFlowInfo'
import { CommonHeader } from '../CommonHeader'
import FlowSelector from '../FlowHeader/FlowSelector'
import styles from './EditHeader.module.css'

export const EditHeader = defineComponent({
  name: 'EditHeader',
  props: {
    loading: {
      type: Boolean,
      default: false,
    },
  },
  setup(props) {
    const { t } = useI18n()
    const route = useRoute()
    const router = useRouter()
    const flowId = computed(() => route.params.flowId as string)
    const projectId = computed(() => route.params.projectId as string)

    const flowModel = useFlowModel()
    const { flowInfo } = useFlowInfo()
    const uiStore = useUIStore()
    const isSaving = ref(false)
    const isReleaseSliderShow = ref(false)

    const workflowName = computed(() => {
      return flowModel.flowModel.value?.name || '--'
    })

    const currentVersion = computed(() => {
      return Number(route.params.version) || 1
    })

    const baseVersionName = computed(() => {
      return `V${currentVersion.value}`
    })

    // Version name display logic (similar to devops-pipeline EditHeader)
    const currentVersionName = computed(() => {
      if (flowInfo.value?.canDebug) {
        // Draft version: show "Draft (based on VX)"
        return t('flow.edit.draftVersion', [
          flowInfo.value?.baseVersionName || baseVersionName.value,
        ])
      }
      // Released version: show version name directly
      return flowInfo.value?.versionName || baseVersionName.value
    })

    const handleCancel = () => {
      // 如果只有草稿版本（没有正式发布版本），跳转到列表页
      if (flowInfo.value?.latestVersionStatus === VERSION_STATUS_ENUM.COMMITTING) {
        router.push({
          name: ROUTE_NAMES.FLOW_LIST,
          params: { groupId: FLOW_GROUP_TYPES.ALL_FLOWS },
        })
        return
      }
      // 否则跳转到创作流详情页
      router.push({
        name: ROUTE_NAMES.FLOW_DETAIL_EXECUTION_RECORD,
        params: { ...route.params, version: flowInfo.value?.releaseVersion },
      })
    }

    const handleSave = async () => {
      if (!projectId.value) {
        Message({
          theme: 'error',
          message: t('flow.content.projectIdRequired'),
        })
        return
      }

      if (!flowModel.flowModel.value) {
        Message({
          theme: 'error',
          message: t('flow.content.noFlowModel'),
        })
        return
      }

      isSaving.value = true

      try {
        const response = await flowModel.saveFlow({
          projectId: projectId.value,
          pipelineId: flowId.value,
          storageType: 'MODEL',
        })

        Message({
          theme: 'success',
          message: t('flow.content.saveSuccess'),
        })

        // Update route version parameter after successful save
        if (response?.version) {
          router.replace({
            name: route.name as string,
            params: {
              ...route.params,
              version: response.version,
            },
            query: route.query,
          })
        }
      } catch (error: any) {
        console.error('Failed to save flow:', error)
        Message({
          theme: 'error',
          message: error?.message || t('flow.content.saveFailed'),
        })
      } finally {
        isSaving.value = false
      }
    }

    const handleDebug = () => {
      // TODO: Implement debug logic
      console.log('Debug flow')
    }

    const handlePublish = () => {
      // Check if there are unsaved changes
      if (flowModel.hasUnsavedChanges.value) {
        Message({
          theme: 'warning',
          message: t('flow.release.saveBeforeRelease'),
        })
        return
      }
      isReleaseSliderShow.value = true
      uiStore.setVariablePanelOpen(false)
    }

    const handleReleased = () => {
      // Reload flow model after release
      flowModel.loadFlow(projectId.value, flowId.value, route.params.version as string, true)
    }

    // Render version tag in breadcrumb area
    const renderVersionTag = () => (
      <span class={styles.versionTag}>
        <Tag>
          <span class={styles.versionTagText} title={currentVersionName.value}>
            {currentVersionName.value}
          </span>
        </Tag>
      </span>
    )

    return () => (
      <>
        <CommonHeader
          loading={props.loading}
          workflowName={workflowName.value}
          onWorkflowNameClick={handleCancel}
        >
          {{
            'workflow-selector': () => (
              <FlowSelector
                projectId={projectId.value}
                currentFlowId={flowId.value}
                currentFlowName={workflowName.value}
                onNameClick={handleCancel}
              />
            ),
            'version-selector': renderVersionTag,
            default: () => (
              <div class={styles.editHeader}>
                <div class={styles.headerLeft}>
                  <span class={styles.flowName}>
                    {t('flow.content.edit')} - {flowId}
                  </span>
                </div>
              </div>
            ),
            actions: () => (
              <div class={styles.headerRight}>
                <Button onClick={handleCancel} disabled={isSaving.value}>
                  {t('flow.common.cancel')}
                </Button>
                <Button
                  outline
                  theme="primary"
                  onClick={handleSave}
                  loading={isSaving.value}
                  disabled={isSaving.value || !flowModel.hasUnsavedChanges.value}
                >
                  {t('flow.content.save')}
                </Button>
                <Button theme="primary" onClick={handlePublish} disabled={isSaving.value}>
                  {t('flow.content.publish')}
                </Button>
              </div>
            ),
          }}
        </CommonHeader>

        <ReleaseSlider
          v-model:isShow={isReleaseSliderShow.value}
          projectId={projectId.value}
          flowId={flowId.value}
          version={currentVersion.value}
          baseVersionName={baseVersionName.value}
          onReleased={handleReleased}
        />
      </>
    )
  },
})
