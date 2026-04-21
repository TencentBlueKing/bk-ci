import { CommonHeader } from '@/components/CommonHeader'
import FlowSelector from '@/components/FlowHeader/FlowSelector'
import { SvgIcon } from '@/components/SvgIcon'
import { ROUTE_NAMES } from '@/constants/routes'
import { useExecuteDetail, type ExecuteInfo } from '@/hooks/useExecuteDetail'
import { Button, InfoBox, Message, Popover } from 'bkui-vue'
import { computed, defineComponent, h, ref, type PropType } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import styles from './DetailHeader.module.css'

export default defineComponent({
  name: 'DetailHeader',
  props: {
    executeInfo: {
      type: Object as PropType<ExecuteInfo>,
      required: true,
    },
  },
  setup(props) {
    const { t } = useI18n()
    const router = useRouter()
    const route = useRoute()
    const projectId = computed(() => route.params.projectId as string)
    const flowId = computed(() => route.params.flowId as string)
    const btnLoading = ref(false)
    const { flowInfo, isRunning, isDebugExec, stopExecute, silentRefreshExecuteDetail, requestRePlayFlow } =
      useExecuteDetail()
    const isCurPipelineLocked = computed(() => flowInfo.value?.locked)

    const handleFlowDetail = () => {
      router.push({
        name: ROUTE_NAMES.FLOW_DETAIL_EXECUTION_RECORD,
        params: {
          flowId: flowId.value,
          version: flowInfo.value?.releaseVersion,
        },
      })
    }

    // 取消执行
    const handleCancel = async () => {
      try {
        btnLoading.value = true
        const res = await stopExecute(route.params.buildNo as string)
        if (res) {
          Message({
            message: t('flow.execute.stopSuc'),
            theme: 'success',
          })
          silentRefreshExecuteDetail()
        } else {
          throw Error(t('flow.execute.stopFail'))
        }
      } catch (error) {
        console.log('error:', error)
      } finally {
        btnLoading.value = false
      }
    }

    const retry = async (buildId: string, forceTrigger = false) => {
      const res = await requestRePlayFlow({
        projectId: projectId.value,
        pipelineId: flowId.value,
        buildId,
        forceTrigger,
      })
      if (res && res.id) {
        const params: Record<string, any> = {
          ...route.params,
          projectId: projectId.value,
          pipelineId: flowId.value,
          buildNo: res.id,
        }

        const query: Record<string, any> = {
          ...route.query,
          executeCount: undefined,
        }

        router.push({
          name: ROUTE_NAMES.FLOW_DETAIL_EXECUTION_DETAIL_TAB,
          params,
          query,
        })

        Message({
          message: t('flow.execute.rebuildSuc'),
          theme: 'success',
        })
      } else if (res?.code === 2101272) {
        btnLoading.value = false
        InfoBox({
          title: t('flow.execute.rePlay'),
          content: res?.message,
          width: 500,
          onConfirm: async () => {
            try {
              btnLoading.value = true
              await retry(buildId, true)
              return true
            } catch (err) {
              btnLoading.value = false
            }
          },
        })
      } else {
        throw Error(t('flow.execute.rebuildFail'))
      }
    }

    const handleReplayClick = () => {
      InfoBox({
        title: t('flow.execute.rePlayConfirmTips'),
        width: 500,
        contentAlign: 'left',
        cancelText: t('flow.common.cancel'),
        content: h(
          'div',
          {
            style: {
              background: '#f5f6fa',
              padding: '10px',
              fontSize: '12px',
              lineHeight: '20px',
            },
          },
          [h('p', t('flow.execute.rePlayInfo1')), h('p', t('flow.execute.rePlayInfo2'))],
        ),
        onConfirm: async () => {
          try {
            btnLoading.value = true
            await retry(props.executeInfo?.id)
            return true
          } catch (err) {
            console.log('error:', err)
            Message({
              message: (err as Error)?.message || err,
              theme: 'error',
            })
          } finally {
            btnLoading.value = false
          }
        },
      })
    }

    // 编辑
    const handleEdit = () => {
      router.push({
        name: ROUTE_NAMES.FLOW_EDIT_WORKFLOW_ORCHESTRATION,
        params: {
          flowId: flowId.value,
          version: flowInfo.value?.version,
          projectId: projectId.value,
        },
      })
    }

    // 执行
    const handleExecute = () => {
      router.push({
        name: ROUTE_NAMES.FLOW_PREVIEW,
        params: {
          flowId: flowId.value,
          version: flowInfo.value?.version,
          projectId: projectId.value,
        },
      })
    }

    return () => (
      <div>
        <CommonHeader workflowName={props.executeInfo.name} onWorkflowNameClick={handleFlowDetail}>
          {{
            'workflow-selector': () => (
              <FlowSelector
                projectId={projectId.value}
                currentFlowId={flowId.value}
                currentFlowName={props.executeInfo.name}
                onNameClick={handleFlowDetail}
              />
            ),
            'execution-detail': () => (
              <span>{`${t('flow.execute.executeDetail')}: #${props.executeInfo.currentBuildNum}`}</span>
            ),
            actions: () => (
              <>
                {!isDebugExec.value ? (
                  <div
                    class={[
                      styles.replayTrigger,
                      btnLoading.value || isCurPipelineLocked.value ? styles.replayTriggerDisabled : '',
                    ]}
                    onClick={() => {
                      if (btnLoading.value || isCurPipelineLocked.value) return
                      handleReplayClick()
                    }}
                  >
                    {btnLoading.value && (
                      <SvgIcon name="circle-2-1" class={['spinIcon', styles.spin]} />
                    )}
                    <span>{t('flow.execute.rePlay')}</span>
                    <Popover zIndex={999999} width={300} placement="right">
                      {{
                        default: () => (
                          <span
                            class={styles.replayInfoWrap}
                            onClick={(e: MouseEvent) => e.stopPropagation()}
                          >
                            <SvgIcon name="info-line" class={styles.infoIcon} />
                          </span>
                        ),
                        content: () => (
                          <>
                            <p>{t('flow.execute.rePlayTips1')}</p>
                            <p>{t('flow.execute.rePlayTips2')}</p>
                            <p>{t('flow.execute.rePlayTips3')}</p>
                          </>
                        ),
                      }}
                    </Popover>
                  </div>
                ) : null}
                {isRunning.value ? (
                  <Button onClick={handleCancel} outline theme="warning" loading={btnLoading.value}>
                    {t('flow.common.cancel')}
                  </Button>
                ) : null}

                <Button onClick={handleEdit}>{t('flow.content.edit')}</Button>
                <Button theme="primary" onClick={handleExecute}>
                  {t('flow.execute.exec')}
                </Button>
              </>
            ),
          }}
        </CommonHeader>
      </div>
    )
  },
})
