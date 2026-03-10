import CompleteLog from '@/components/CompleteLog'
import { JobDetail, PluginDetail, StageDetail } from '@/components/ExecDetail'
import StageReviewPanel from '@/components/StageReviewPanel'
import { SvgIcon } from '@/components/SvgIcon'
import { useExecuteDetail } from '@/hooks/useExecuteDetail'
import {
  STATUS,
  type Container,
  type Element,
  type ExecutionRecord,
  type FlowModel,
  type Stage,
} from '@/types/flow'
import { isSkip } from '@/utils/flowStatus'
import { convertMillSec, convertTime } from '@/utils/util'
import 'bkui-pipeline/dist/bk-pipeline.css'
import BkPipeline from 'bkui-pipeline/vue3'
import { Button, Checkbox, Dialog, Message, Popover, Radio, Select } from 'bkui-vue'
import { computed, defineComponent, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import styles from './ExecPipeline.module.css'

interface TimeStep {
  title: string
  description: string
  hasPopup: boolean
  popupContent?: 'queue' | 'time'
}

interface TimeDetailRow {
  field: string
  label: string
  value: string
}

interface ExecuteCountOption {
  id: number
  name: string
  user: string
  timeCost?: string
}

export default defineComponent({
  name: 'ExecPipeline',
  props: {
    matchRules: {
      type: Array,
      default: () => [],
    },
  },
  setup(props) {
    const { t } = useI18n()
    const route = useRoute()
    const router = useRouter()

    // 从 store 获取执行详情数据（全局唯一）
    const { executeDetail, isRunning, requestRetryFlow, silentRefreshExecuteDetail } =
      useExecuteDetail()

    // ==================== State ====================
    const hideSkipExecTask = ref(false)

    // Stage 重试状态
    const showRetryStageDialog = ref(false)
    const retryTaskId = ref('')
    const skipTask = ref(false)
    const failedContainer = ref(false)

    // Stage 审核状态
    const showStageReviewPanel = ref(false)
    const reviewStage = ref<Stage | null>(null)
    const reviewType = ref<'checkIn' | 'checkOut'>('checkIn')
    const isExpandAllMatrix = ref(true)
    const showLog = ref(false)
    const showErrors = ref(false)
    const activeErrorAtom = ref<any>(null)
    const bkPipelineRef = ref<any>(null)
    const scrollBoxRef = ref<HTMLElement | null>(null)
    const errorPopupRef = ref<HTMLElement | null>(null)

    // 日志面板状态
    const showJobDetail = ref(false)
    const showPluginDetail = ref(false)
    const showStageDetail = ref(false)
    const selectedContainer = ref<Container | null>(null)
    const selectedElement = ref<Element | null>(null)
    const selectedStage = ref<Stage | null>(null)
    const selectedContainerId = ref<string>('')
    const selectedContainerStage = ref<Stage | null>(null)
    const selectedContainerIndex = ref<number>(-1)

    // ==================== Computed: Pipeline Data ====================
    const curPipeline = computed<FlowModel | null>(() => {
      return executeDetail.value?.model || null
    })

    // 根据 hideSkipExecTask 过滤跳过的步骤，同时过滤掉第一个stage
    const filteredPipeline = computed<FlowModel | null>(() => {
      if (!curPipeline.value) {
        return null
      }

      // 过滤掉第一个stage
      const stagesWithoutFirst = curPipeline.value.stages?.slice(1) || []

      if (!hideSkipExecTask.value) {
        return {
          ...curPipeline.value,
          stages: stagesWithoutFirst,
        }
      }

      const stages = stagesWithoutFirst
        .filter((stage) => !isSkip(stage.status))
        .map((stage) => {
          const containers = stage.containers
            ?.filter((container) => !isSkip(container.status))
            .map((container) => {
              const elements = container.elements?.filter((element) => !isSkip(element.status))

              // 处理矩阵组容器
              if (container.matrixGroupFlag && Array.isArray(container.groupContainers)) {
                return {
                  ...container,
                  elements,
                  groupContainers: container.groupContainers
                    .filter((groupContainer) => !isSkip(groupContainer.status))
                    .map((groupContainer) => {
                      const subElements = groupContainer.elements?.filter(
                        (element: any, index: number) =>
                          !isSkip(element.status ?? elements?.[index]?.status),
                      )
                      return {
                        ...groupContainer,
                        elements: subElements,
                      }
                    }),
                }
              }

              return {
                ...container,
                elements,
              }
            })

          return {
            ...stage,
            containers,
          }
        })

      return {
        ...curPipeline.value,
        stages,
      }
    })

    const executeCount = computed(() => {
      return route.query.executeCount
        ? Number(route.query.executeCount)
        : (executeDetail.value?.executeCount ?? 1)
    })

    const cancelUserId = computed(() => {
      return executeDetail.value?.cancelUserId ?? '--'
    })

    // ==================== Computed: Execute Count Options ====================
    const executeCounts = computed<ExecuteCountOption[]>(() => {
      const len = executeDetail.value?.recordList?.length ?? 0
      return (
        executeDetail.value?.recordList
          ?.map((record: ExecutionRecord, index: number) => ({
            id: len - index,
            name: `${len - index} / ${len}`,
            user: record.startUser,
            timeCost: convertMillSec(
              (record.timeCost?.totalCost || 0) + (record.timeCost?.queueCost || 0),
              true,
            ),
          }))
          .reverse() ?? []
      )
    })

    // ==================== Computed: Status & Labels ====================
    const statusLabel = computed(() => {
      return executeDetail.value?.status ? t(`flow.statusMap.${executeDetail.value.status}`) : ''
    })

    // ==================== Computed: Time Information ====================
    const timeSteps = computed<TimeStep[]>(() => {
      return [
        {
          title: t('flow.execute.triggerTime'),
          description: convertTime(executeDetail.value?.queueTime || 0),
          hasPopup: true,
          popupContent: 'queue',
        },
        {
          title: t('flow.execute.startTime'),
          description: convertTime(executeDetail.value?.startTime || 0),
          hasPopup: true,
          popupContent: 'time',
        },
        {
          title: t('flow.execute.endTime'),
          description: convertTime(executeDetail.value?.endTime || 0),
          hasPopup: false,
        },
      ]
    })

    const sumCost = computed(() => {
      const timeCost = executeDetail.value?.model?.timeCost
      return convertMillSec(timeCost?.totalCost, true)
    })

    const queueCost = computed(() => {
      return convertMillSec(executeDetail.value?.model?.timeCost?.queueCost)
    })

    const totalCost = computed(() => {
      return convertMillSec(executeDetail.value?.model?.timeCost?.totalCost)
    })

    const timeDetailRows = computed<TimeDetailRow[]>(() => {
      return ['executeCost', 'systemCost', 'waitCost'].map((key) => ({
        field: key,
        label: t(`flow.execute.${key}`),
        value: convertMillSec(
          executeDetail.value?.model?.timeCost?.[
          key as keyof typeof executeDetail.value.model.timeCost
          ],
        ),
      }))
    })

    // ==================== Computed: Error Information ====================
    const errorList = computed(() => {
      return executeDetail.value?.errorInfoList || []
    })

    const showErrorPopup = computed(() => {
      return Array.isArray(errorList.value) && errorList.value.length > 0
    })

    // ==================== Methods: Event Handlers ====================
    const handleExecuteCountChange = (executeCount: number) => {
      return router.push({
        name: route.name || undefined,
        params: route.params,
        query: {
          ...route.query,
          executeCount: String(executeCount),
        },
      })
    }

    const toggleErrorPopup = () => {
      showErrors.value = !showErrors.value
    }

    const showCompleteLog = () => {
      showLog.value = true
    }

    const hideCompleteLog = () => {
      showLog.value = false
    }

    // 关闭所有日志面板
    const closeAllLogPanels = () => {
      showJobDetail.value = false
      showPluginDetail.value = false
      showStageDetail.value = false
      selectedContainer.value = null
      selectedElement.value = null
      selectedStage.value = null
      selectedContainerId.value = ''
      selectedContainerStage.value = null
      selectedContainerIndex.value = -1
    }

    // ==================== Methods: Pipeline Operations ====================
    const expandAllMatrix = async (expand: boolean) => {
      try {
        // 使用 filteredPipeline 而不是 executeDetail.value.model，因为已经过滤了第一个stage
        const stages = filteredPipeline.value?.stages || []

        for (let i = 0; i < stages.length; i++) {
          const stage = stages[i]
          if (!stage) continue
          for (let j = 0; j < (stage.containers?.length || 0); j++) {
            const matrix = stage.containers[j]
            if (!matrix) continue
            if (matrix.matrixGroupFlag && matrix.groupContainers) {
              for (let k = 0; k < matrix.groupContainers.length; k++) {
                const container = matrix.groupContainers[k]
                if (container) {
                  bkPipelineRef.value?.expandMatrix?.(stage.id, matrix.id, container.id, expand)
                }
              }
            } else {
              bkPipelineRef.value?.expandJob?.(stage.id, matrix.id, expand)
            }
          }
        }
      } catch (error) {
        console.error('expandAllMatrix error', error)
      }
    }

    const handlePipelineClick = (args: any) => {
      console.log('Pipeline click:', args)

      // 从 bk-pipeline 组件传来的参数格式：
      // - 点击 Atom/Element: { stageIndex, containerIndex, containerGroupIndex, elementIndex }
      // - 点击 Container/Job: { stageIndex, containerIndex, containerGroupIndex, container }
      // - 点击 Stage: { stageIndex }
      const {
        stageIndex,
        containerIndex,
        containerGroupIndex,
        elementIndex,
        container: clickedContainer,
      } = args || {}

      // 关闭之前打开的面板
      closeAllLogPanels()

      if (!executeDetail.value?.model?.stages) {
        return
      }

      // 注意：filteredPipeline 已经过滤掉了第一个 stage，所以索引需要 +1
      const stages = executeDetail.value.model.stages

      // 如果有 elementIndex，说明点击的是插件/Atom
      if (elementIndex !== undefined && stageIndex !== undefined && containerIndex !== undefined) {
        const stage = stages[stageIndex + 1] // +1 因为过滤掉了第一个 stage
        if (stage && stage.containers) {
          let targetContainer = stage.containers[containerIndex]

          // 处理矩阵容器组
          if (containerGroupIndex !== undefined && targetContainer?.groupContainers) {
            targetContainer = targetContainer.groupContainers[containerGroupIndex]
          }

          if (targetContainer && targetContainer.elements) {
            const element = targetContainer.elements[elementIndex]
            if (element) {
              selectedElement.value = element as Element
              selectedContainerId.value = (targetContainer.id ||
                targetContainer.containerId ||
                '') as string
              showPluginDetail.value = true
            }
          }
        }
        return
      }

      // 如果有 clickedContainer 或 containerIndex（但没有 elementIndex），说明点击的是 Job/Container
      if ((clickedContainer || containerIndex !== undefined) && stageIndex !== undefined) {
        const stage = stages[stageIndex + 1]
        if (stage && stage.containers) {
          let targetContainer = clickedContainer || stage.containers[containerIndex]

          // 处理矩阵容器组
          if (
            !clickedContainer &&
            containerGroupIndex !== undefined &&
            stage.containers[containerIndex]?.groupContainers
          ) {
            targetContainer = stage.containers[containerIndex].groupContainers[containerGroupIndex]
          }

          if (targetContainer) {
            selectedContainer.value = targetContainer as Container
            selectedContainerStage.value = stage
            selectedContainerIndex.value =
              containerGroupIndex !== undefined ? containerGroupIndex : containerIndex
            showJobDetail.value = true
          }
        }
        return
      }

      // 只有 stageIndex，说明点击的是 Stage
      if (stageIndex !== undefined) {
        const stage = stages[stageIndex + 1]
        if (stage) {
          selectedStage.value = stage
          showStageDetail.value = true
        }
        return
      }
    }

    const handleStageCheck = (payload: any) => {
      const { type = 'checkIn', stageIndex } = payload || {}
      if (!executeDetail.value?.model?.stages) return
      const stage = executeDetail.value.model.stages[stageIndex + 1]
      if (stage) {
        reviewStage.value = stage
        reviewType.value = type
        showStageReviewPanel.value = true
      }
    }

    const handleRetry = (payload: any) => {
      const { taskId, skip = false } = payload || {}
      retryTaskId.value = taskId
      skipTask.value = skip
      showRetryStageDialog.value = true
    }

    const handleContinue = async (payload: any) => {
      const { taskId, skip = false } = payload || {}
      retryTaskId.value = taskId
      skipTask.value = skip
      await retryPipeline(false)
    }

    const retryPipeline = async (isStageRetry: boolean) => {
      showRetryStageDialog.value = false
      try {
        const res = await requestRetryFlow({
          projectId: route.params.projectId as string,
          pipelineId: route.params.flowId as string,
          buildId: route.params.buildNo as string,
          taskId: retryTaskId.value,
          skip: skipTask.value,
          ...(isStageRetry ? { failedContainer: String(failedContainer.value) } : {}),
        })
        if (res?.id) {
          const msg = skipTask.value ? t('flow.execute.skipSuc') : t('flow.execute.retrySuc')
          Message({ theme: 'success', message: msg, limit: 1 })
          if (res.executeCount) {
            await handleExecuteCountChange(res.executeCount)
          } else {
            await silentRefreshExecuteDetail()
          }
        } else {
          Message({ theme: 'error', message: (res as any)?.message || t('flow.execute.operateFail'), limit: 1 })
        }
      } catch (err: any) {
        Message({ theme: 'error', message: err?.message || t('flow.execute.operateFail'), limit: 1 })
      } finally {
        retryTaskId.value = ''
        skipTask.value = false
      }
    }

    const handleStageReviewApprove = async () => {
      await silentRefreshExecuteDetail()
    }

    const closeStageReviewPanel = () => {
      showStageReviewPanel.value = false
      reviewStage.value = null
    }
    const setShowErrorPopup = () => {
      showErrors.value = true
    }

    // 动态更新错误弹窗高度，用于调整底部 padding
    const updateErrorPopupHeight = () => {
      nextTick(() => {
        if (errorPopupRef.value && showErrorPopup.value) {
          const height = showErrors.value ? errorPopupRef.value.offsetHeight : 42
          const root = document.documentElement
          root.style.setProperty('--error-popup-height', `${height}px`)
        } else {
          const root = document.documentElement
          root.style.setProperty('--error-popup-height', '0px')
        }
      })
    }

    // ==================== Render Helpers ====================
    const renderTimeStepPopover = (step: TimeStep) => {
      if (!step.hasPopup) return null

      return (
        <Popover theme="light" placement="bottom" trigger="hover" boundary="window">
          {{
            default: () => (
              <span class={styles.timeStepDivider}>
                <p></p>
              </span>
            ),
            content: () =>
              step.popupContent === 'queue' ? (
                <div class={styles.queueTimeDetailPopup}>
                  <div class={styles.pipelineTimeDetailSum}>
                    <span>{t('flow.execute.queueCost')}</span>
                    <span class={styles.constantWidthNum}>{queueCost.value}</span>
                  </div>
                </div>
              ) : (
                <div class={styles.timeDetailPopup}>
                  <div class={styles.pipelineTimeDetailSum}>
                    <span>{t('flow.execute.totalCost')}</span>
                    <span class={styles.constantWidthNum}>
                      {isRunning.value ? `${t('flow.execute.running')}...` : totalCost.value}
                    </span>
                  </div>
                  <ul class={styles.pipelineTimeDetailSumList}>
                    {timeDetailRows.value.map((cost) => (
                      <li key={cost.field}>
                        <span>{cost.label}</span>
                        <span class={styles.constantWidthNum}>{cost.value}</span>
                      </li>
                    ))}
                  </ul>
                </div>
              ),
          }}
        </Popover>
      )
    }

    const renderExecuteCountSelect = () => {
      return (
        <Select
          modelValue={executeCount.value}
          popoverMinWidth={300}
          clearable={false}
          onChange={handleExecuteCountChange}
          class={styles.pipelineExecCountSelect}
        >
          {executeCounts.value.map((item) => (
            <Select.Option key={item.id} id={item.id} name={item.name}>
              <div class={styles.execCountSelectOption}>
                <span>{item.name}</span>
                {item.timeCost && <span class={styles.execCountTimeCost}>{item.timeCost}</span>}
                <span class={styles.execCountSelectOptionUser}>{item.user}</span>
              </div>
            </Select.Option>
          ))}
        </Select>
      )
    }

    const renderTimeLine = () => {
      return (
        <ul class={styles.pipelineExecTimeline}>
          {timeSteps.value.map((step) => (
            <li key={step.title} class={styles.pipelineExecTimelineItem}>
              <span class={styles.titleItem}>
                <p>{step.title}</p>
                {renderTimeStepPopover(step)}
              </span>
              <p class={styles.constantWidthNum}>{step.description}</p>
            </li>
          ))}
        </ul>
      )
    }

    const renderPipelineControls = () => {
      return (
        <header class={styles.pipelineStyleSettingHeader}>
          <Checkbox v-model={hideSkipExecTask.value} class={styles.hideSkipPipelineTask}>
            {t('flow.execute.hideSkipStep')}
          </Checkbox>
          <Checkbox
            v-model={isExpandAllMatrix.value}
            onChange={expandAllMatrix}
            class={styles.expandJobCheckbox}
          >
            {t('flow.execute.isExpandJob')}
          </Checkbox>
          <Button text theme="primary" onClick={showCompleteLog}>
            <SvgIcon name="txt" size={16} />
            {t('flow.execute.viewLog')}
          </Button>
        </header>
      )
    }

    const renderErrorPopup = () => {
      if (!showErrorPopup.value) return null

      return (
        <footer
          ref={errorPopupRef}
          class={[styles.execErrorsPopup, showErrors.value && styles.visible]}
        >
          <Button text theme="normal" class={styles.dragDot} onClick={toggleErrorPopup}>
            <SvgIcon
              name="arrows-up"
              size={30}
              class={[styles.toggleErrorPopupIcon, showErrors.value && styles.rotated]}
            />
          </Button>
          <div class={styles.errorContent}>
            {t('flow.execute.errorInfo')}: {errorList.value.length} {t('flow.execute.errors')}
          </div>
        </footer>
      )
    }

    // 渲染完整日志组件
    const renderCompleteLogComponent = () => {
      if (!showLog.value || !executeDetail.value) return null

      return (
        <CompleteLog
          execDetail={executeDetail.value}
          executeCount={executeCount.value}
          onClose={hideCompleteLog}
        />
      )
    }

    // 渲染 Job 详情面板
    const renderJobDetailPanel = () => {
      if (!showJobDetail.value || !selectedContainer.value || !executeDetail.value) return null

      return (
        <JobDetail
          isShow={showJobDetail.value}
          execDetail={executeDetail.value}
          container={selectedContainer.value}
          stage={selectedContainerStage.value}
          containerIndex={selectedContainerIndex.value}
          executeCount={executeCount.value}
          onClose={() => {
            showJobDetail.value = false
            selectedContainer.value = null
            selectedContainerStage.value = null
            selectedContainerIndex.value = -1
          }}
        />
      )
    }

    // 渲染插件详情面板
    const renderPluginDetailPanel = () => {
      if (!showPluginDetail.value || !selectedElement.value || !executeDetail.value) return null

      return (
        <PluginDetail
          isShow={showPluginDetail.value}
          execDetail={executeDetail.value}
          element={selectedElement.value}
          containerId={selectedContainerId.value}
          executeCount={executeCount.value}
          onClose={() => {
            showPluginDetail.value = false
            selectedElement.value = null
            selectedContainerId.value = ''
          }}
        />
      )
    }

    // 渲染 Stage 重试弹窗
    const renderRetryStageDialog = () => {
      return (
        <Dialog
          isShow={showRetryStageDialog.value}
          title={t('flow.execute.stageRetryTitle')}
          width={400}
          onClosed={() => {
            showRetryStageDialog.value = false
          }}
          onConfirm={() => retryPipeline(true)}
        >
          <Radio.Group v-model={failedContainer.value}>
            <Radio label={false}>{t('flow.execute.retryAllJobs')}</Radio>
            <Radio label={true}>{t('flow.execute.retryFailJobs')}</Radio>
          </Radio.Group>
        </Dialog>
      )
    }

    // 渲染 Stage 审核面板
    const renderStageReviewPanel = () => {
      if (!reviewStage.value) return null

      return (
        <StageReviewPanel
          isShow={showStageReviewPanel.value}
          stage={reviewStage.value}
          reviewType={reviewType.value}
          onClose={closeStageReviewPanel}
          onApprove={handleStageReviewApprove}
        />
      )
    }

    // 渲染 Stage 详情面板
    const renderStageDetailPanel = () => {
      if (!showStageDetail.value || !selectedStage.value || !executeDetail.value) return null

      return (
        <StageDetail
          isShow={showStageDetail.value}
          execDetail={executeDetail.value}
          stage={selectedStage.value}
          executeCount={executeCount.value}
          onClose={() => {
            showStageDetail.value = false
            selectedStage.value = null
          }}
        />
      )
    }

    // ==================== Watchers ====================
    watch(executeCount, () => {
      nextTick(() => {
        if (errorList.value.length > 0) {
          setShowErrorPopup()
        }
        updateErrorPopupHeight()
      })
    })

    watch(showErrors, () => {
      nextTick(() => {
        updateErrorPopupHeight()
      })
    })

    watch(showErrorPopup, () => {
      nextTick(() => {
        updateErrorPopupHeight()
      })
    })

    // ==================== Lifecycle ====================
    onMounted(() => {
      nextTick(() => {
        // 延迟展开，确保组件完全渲染
        setTimeout(() => {
          if (isExpandAllMatrix.value && bkPipelineRef.value && filteredPipeline.value) {
            expandAllMatrix(true)
          }
          updateErrorPopupHeight()
        }, 500)
      })
    })

    onUnmounted(() => {
      const root = document.documentElement
      root.style.removeProperty('--error-popup-height')
    })

    // ==================== Render ====================
    return () => {
      if (!executeDetail.value || !curPipeline.value) {
        return <div class={styles.emptyState}>{t('flow.execute.noData')}</div>
      }

      return (
        <div class={styles.execPipelineWrapper}>
          {/* 滚动视口占位 */}
          <div class={styles.pipelineModelScrollViewport}>
            <p></p>
          </div>

          {/* 执行摘要 */}
          <div class={styles.pipelineExecSummary}>
            <div class={styles.pipelineExecCount}>
              <span>{t('flow.execute.num')}</span>
              {renderExecuteCountSelect()}
              <span class={styles.execStatusLabel}>
                {t('flow.execute.times', [executeCount.value])}
                {executeDetail.value.status === STATUS.CANCELED && (
                  <SvgIcon
                    name="info-circle"
                    size={16}
                    v-bk-tooltips={`${t('flow.execute.canceller')}：${cancelUserId.value}`}
                  />
                )}
              </span>
              {!isRunning.value && (
                <span>
                  {' '}
                  {t('flow.execute.totalCost')}：{sumCost.value}{' '}
                </span>
              )}
            </div>
            {renderTimeLine()}
          </div>

          {/* 执行内容 */}
          <section class={styles.pipelineExecContent}>
            {renderPipelineControls()}
            <div ref={scrollBoxRef} class={styles.execPipelineScrollBox}>
              <div class={styles.execPipelineUiWrapper}>
                {filteredPipeline.value && (
                  <BkPipeline
                    ref={bkPipelineRef}
                    editable={false}
                    isCreativeStream={true}
                    isExecDetail={true}
                    currentExecCount={executeCount.value}
                    cancelUserId={cancelUserId.value as string}
                    pipeline={filteredPipeline.value}
                    matchRules={props.matchRules as any}
                    onClick={handlePipelineClick}
                    onStageCheck={handleStageCheck}
                    onStageRetry={handleRetry}
                    onAtomContinue={handleContinue}
                  />
                )}
              </div>
            </div>
            {/* {renderErrorPopup()} */}
          </section>

          {/* 日志面板 */}
          {renderCompleteLogComponent()}
          {renderJobDetailPanel()}
          {renderPluginDetailPanel()}
          {renderStageDetailPanel()}

          {/* Stage 重试弹窗 & 审核面板 */}
          {renderRetryStageDialog()}
          {renderStageReviewPanel()}
        </div>
      )
    }
  },
})
