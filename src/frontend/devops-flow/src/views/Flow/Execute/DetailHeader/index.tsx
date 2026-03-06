import { CommonHeader } from '@/components/CommonHeader'
import FlowSelector from '@/components/FlowHeader/FlowSelector'
import { SvgIcon } from '@/components/SvgIcon'
import { ROUTE_NAMES } from '@/constants/routes'
import { useExecuteDetail, type ExecuteInfo } from '@/hooks/useExecuteDetail'
import { Button, Dropdown, InfoBox, Message, Popover } from 'bkui-vue'
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
    const isDropdownShow = ref(false)
    const { flowInfo, isRunning, isDebugExec, stopExecute, silentRefreshExecuteDetail, requestRePlayFlow, requestRetryFlow } =
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

    const retry = async (type = 'reBuild', buildId: string, forceTrigger = false) => {
      const retryFn = type === 'reBuild' ? requestRetryFlow : requestRePlayFlow
      // 请求执行构建
      const res = await retryFn({
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

        if (type === 'reBuild' && 'executeCount' in res) {
          query.executeCount = String(res.executeCount)
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
              await retry('rePlay', buildId, true)
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

    // 重新构建
    const handleClick = (type = 'reBuild') => {
      isDropdownShow.value = false
      const title =
        type === 'reBuild'
          ? t('flow.execute.reBuildConfirmTips')
          : t('flow.execute.rePlayConfirmTips')
      InfoBox({
        title,
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
          type === 'reBuild'
            ? [h('p', t('flow.execute.reBuildInfo1')), h('p', t('flow.execute.reBuildInfo2'))]
            : [h('p', t('flow.execute.rePlayInfo1')), h('p', t('flow.execute.rePlayInfo2'))],
        ),
        onConfirm: async () => {
          try {
            btnLoading.value = true
            await retry(type, props.executeInfo?.id)
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
                  <Dropdown
                    isShow={isDropdownShow.value}
                    onShowChange={(val: boolean) => { isDropdownShow.value = val }}
                    trigger="click"
                    disabled={btnLoading.value || isCurPipelineLocked.value}
                  >
                    {{
                      default: () => (
                        <div class={styles.rebuildDropdownTrigger}>
                          {btnLoading.value && (
                            <SvgIcon name="circle-2-1" class={['spinIcon', styles.spin]} />
                          )}
                          <span>{t('flow.execute.reBuild')}</span>
                          <SvgIcon name="angle-down" />
                        </div>
                      ),
                      content: () => (
                        <ul class={styles.rebuildDropdownContent}>
                          <li
                            class={[
                              styles.dropdownItem,
                              btnLoading.value || isCurPipelineLocked.value ? styles.disabled : '',
                            ]}
                            onClick={() => handleClick('reBuild')}
                          >
                            {t('flow.execute.reBuild')}
                            <Popover zIndex={3000} width={300} placement="bottom">
                              {{
                                default: () => <SvgIcon name="info-line" class={styles.infoIcon} />,
                                content: () => (
                                  <>
                                    <p>{t('flow.execute.reBuildTips1')}</p>
                                    <p>{t('flow.execute.reBuildTips2')}</p>
                                    <p>{t('flow.execute.reBuildTips3')}</p>
                                  </>
                                ),
                              }}
                            </Popover>
                          </li>
                          <li
                            class={[
                              styles.dropdownItem,
                              btnLoading.value || isCurPipelineLocked.value ? styles.disabled : '',
                            ]}
                            onClick={() => handleClick('rePlay')}
                          >
                            {t('flow.execute.rePlay')}
                            <Popover zIndex={3000} width={300} placement="bottom">
                              {{
                                default: () => <SvgIcon name="info-line" class={styles.infoIcon} />,
                                content: () => (
                                  <>
                                    <p>{t('flow.execute.rePlayTips1')}</p>
                                    <p>{t('flow.execute.rePlayTips2')}</p>
                                    <p>{t('flow.execute.rePlayTips3')}</p>
                                  </>
                                ),
                              }}
                            </Popover>
                          </li>
                        </ul>
                      ),
                    }}
                  </Dropdown>
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
