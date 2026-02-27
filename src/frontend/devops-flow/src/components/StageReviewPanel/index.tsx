import { useExecuteDetail } from '@/hooks/useExecuteDetail'
import { SvgIcon } from '@/components/SvgIcon'
import { useAuthStore } from '@/stores/auth'
import type { Stage } from '@/types/flow'
import { convertTime } from '@/utils/util'
import { Button, Input, Message, Radio, Sideslider, Steps } from 'bkui-vue'
import { computed, defineComponent, ref, watch, type PropType } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import styles from './StageReviewPanel.module.css'

interface ReviewGroup {
  id: string
  name: string
  reviewers: string[]
  operator?: string
  suggest?: string
  status?: 'PROCESS' | 'ABORT' | undefined
  params?: Record<string, unknown>[]
}

interface StageControl {
  manualTrigger?: boolean
  timeout?: number
  reviewDesc?: string
  reviewGroups?: ReviewGroup[]
  reviewParams?: Record<string, unknown>[]
  status?: string
  markdownContent?: boolean
  notifyType?: string[]
}

export default defineComponent({
  name: 'StageReviewPanel',
  props: {
    isShow: {
      type: Boolean,
      default: false,
    },
    stage: {
      type: Object as PropType<Stage>,
      default: () => ({}),
    },
    reviewType: {
      type: String as PropType<'checkIn' | 'checkOut'>,
      default: 'checkIn',
    },
  },
  emits: ['close', 'approve'],
  setup(props, { emit }) {
    const { t } = useI18n()
    const route = useRoute()
    const { requestTriggerStage, silentRefreshExecuteDetail } = useExecuteDetail()
    const authStore = useAuthStore()

    const isCancel = ref(false)
    const suggest = ref('')
    const errMessage = ref('')
    const isApproving = ref(false)
    const curStep = ref(1)

    const stageControl = computed<StageControl>(() => {
      return (props.stage?.[props.reviewType] as StageControl) || {}
    })

    const reviewGroups = computed<ReviewGroup[]>(() => {
      return stageControl.value.reviewGroups || []
    })

    const isStagePause = computed(() => {
      try {
        return stageControl.value.status === 'REVIEWING'
      } catch {
        return false
      }
    })

    const curReviewGroup = computed(() => {
      return reviewGroups.value.find((review) => review.status === undefined)
    })

    const canTriggerStage = computed(() => {
      if (!curReviewGroup.value) return false
      return curReviewGroup.value.reviewers.includes(authStore.username)
    })

    const disabled = computed(() => {
      return !isStagePause.value || !canTriggerStage.value
    })

    const stageTitle = computed(() => {
      const stageName = props.stage?.name || 'Stage'
      return `${t('flow.execute.stageReview')} - ${stageName}`
    })

    const computedTime = computed(() => {
      try {
        const hour2Ms = 60 * 60 * 1000
        const timeout = stageControl.value.timeout || 24
        return convertTime((props.stage?.startEpoch || 0) + timeout * hour2Ms)
      } catch {
        return '--'
      }
    })

    const showReviewGroup = computed(() => {
      if (curStep.value > 0 && curStep.value <= reviewGroups.value.length) {
        return reviewGroups.value[curStep.value - 1]
      }
      return curReviewGroup.value || reviewGroups.value[0]
    })

    const computedStatusTxt = computed(() => {
      if (!showReviewGroup.value) return ''
      const curExecIndex = reviewGroups.value.findIndex((x) => x.status === undefined) + 1
      const { reviewers, operator } = showReviewGroup.value

      if (curExecIndex < curStep.value) {
        return t('flow.execute.approved', [operator])
      }
      if (curExecIndex === curStep.value) {
        return t('flow.execute.pendingApproval', [reviewers.join(', ')])
      }
      return t('flow.execute.waitApproval', [reviewers.join(', ')])
    })

    const computedReviewSteps = computed(() => {
      return reviewGroups.value.map((item, index) => {
        const statusMap: Record<string, string> = {
          ABORT: 'error',
          PROCESS: 'done',
        }
        let status = item.status ? statusMap[item.status] : undefined
        const curExecIndex = reviewGroups.value.findIndex((x) => x.status === undefined)
        if (curExecIndex === index) status = 'loading'
        return { status, title: item.name, icon: index + 1 }
      })
    })

    watch(
      () => showReviewGroup.value,
      (group) => {
        if (group) {
          suggest.value = group.suggest || ''
          isCancel.value = group.status === 'ABORT'
        }
      },
      { immediate: true },
    )

    watch(
      () => props.isShow,
      (val) => {
        if (val) {
          errMessage.value = ''
          const execIndex = reviewGroups.value.findIndex((x) => x.status === undefined)
          curStep.value = execIndex >= 0 ? execIndex + 1 : 1
        }
      },
    )

    const handleStepChange = (index: number) => {
      curStep.value = index
    }

    const handleClose = () => {
      emit('close')
    }

    const confirmApprove = async () => {
      if (isCancel.value && suggest.value.trim() === '') {
        errMessage.value = t('flow.execute.opinionRequired')
        return
      }
      errMessage.value = ''

      if (!showReviewGroup.value) return

      isApproving.value = true
      try {
        await requestTriggerStage({
          stageId: props.stage.id,
          cancel: isCancel.value,
          suggest: suggest.value,
          id: showReviewGroup.value.id,
          reviewParams: stageControl.value.reviewParams as Record<string, unknown>[],
        })
        Message({ theme: 'success', message: t('flow.execute.operateSuc'), limit: 1 })
        emit('approve')
        handleClose()
        await silentRefreshExecuteDetail()
      } catch (err: any) {
        Message({ theme: 'error', message: err?.message || t('flow.execute.operateFail'), limit: 1 })
      } finally {
        isApproving.value = false
      }
    }

    const renderApproveContent = () => {
      if (!isStagePause.value) {
        return (
          <div class={styles.stageReviewContent}>
            {stageControl.value.reviewDesc && (
              <div class={styles.reviewDesc}>{stageControl.value.reviewDesc}</div>
            )}
            {reviewGroups.value.length > 0 && (
              <>
                <span class={styles.reviewSubtitle}>{t('flow.execute.approvalFlow')}</span>
                <Steps
                  controllable
                  class={styles.reviewSteps}
                  steps={computedReviewSteps.value}
                  curStep={curStep.value}
                  onStepChanged={handleStepChange}
                />
              </>
            )}
            <div class={styles.disabledTip}>
              {t('flow.execute.currentStatus')}: {computedStatusTxt.value}
            </div>
          </div>
        )
      }

      return (
        <div class={styles.stageReviewContent}>
          {stageControl.value.reviewDesc && (
            <>
              <span class={styles.reviewSubtitle}>{t('flow.execute.stageReview')}</span>
              <div class={styles.reviewDesc}>{stageControl.value.reviewDesc}</div>
            </>
          )}

          {reviewGroups.value.length > 0 && (
            <>
              <span class={styles.reviewSubtitle}>
                {t('flow.execute.approvalFlow')}
                <span class={styles.reviewClock} v-bk-tooltips={t('flow.execute.timeOutTips')}>
                  <SvgIcon name="alarm-clock" size={14} />
                  {computedTime.value}
                </span>
              </span>
              <Steps
                controllable
                class={styles.reviewSteps}
                steps={computedReviewSteps.value}
                curStep={curStep.value}
                onStepChanged={handleStepChange}
              />
            </>
          )}

          <span class={styles.reviewSubtitle}>
            {t('flow.execute.currentStatus')}
            <span class={[styles.grayColor, styles.statusLine]}> {computedStatusTxt.value}</span>
          </span>

          <Radio.Group v-model={isCancel.value} class={styles.reviewResult}>
            <Radio label={false} disabled={disabled.value}>
              {t('flow.execute.approve')}{' '}
              <span class={styles.grayColor}>({t('flow.execute.approveRes')})</span>
            </Radio>
            <Radio label={true} disabled={disabled.value}>
              {t('flow.execute.abort')}{' '}
              <span class={styles.grayColor}>({t('flow.execute.abortRes')})</span>
            </Radio>
          </Radio.Group>

          <span class={styles.reviewSubtitle}>{t('flow.execute.approvalOpinion')}</span>
          <Input
            type="textarea"
            placeholder={t('flow.execute.opinionTips')}
            rows={3}
            maxlength={200}
            disabled={disabled.value}
            v-model={suggest.value}
          />
          {errMessage.value && <span class={styles.errorMessage}>{errMessage.value}</span>}

          {!disabled.value && (
            <div class={styles.approveFooter}>
              <Button
                theme="primary"
                onClick={confirmApprove}
                loading={isApproving.value}
                disabled={disabled.value}
              >
                {t('flow.execute.confirm')}
              </Button>
              <Button onClick={handleClose} disabled={isApproving.value}>
                {t('flow.execute.cancel')}
              </Button>
            </div>
          )}
        </div>
      )
    }

    return () => (
      <Sideslider
        class="bkci-property-panel"
        width={640}
        isShow={props.isShow}
        quickClose={true}
        onClosed={handleClose}
        onUpdate:isShow={(val: boolean) => !val && handleClose()}
      >
        {{
          header: () => <span>{stageTitle.value}</span>,
          default: () => renderApproveContent(),
        }}
      </Sideslider>
    )
  },
})
