import { useExecuteDetail } from '@/hooks/useExecuteDetail'
import { SvgIcon } from '@/components/SvgIcon'
import { useAuthStore } from '@/stores/auth'
import type { CheckConfig, ReviewGroup, ReviewParam, Stage } from '@/types/flow'
import { ParamType } from '@/types/variable'
import { convertTime } from '@/utils/util'
import { Button, Input, Message, Radio, Select, Sideslider, Steps, Table, Timeline } from 'bkui-vue'
import { computed, defineComponent, ref, watch, type PropType } from 'vue'
import { useI18n } from 'vue-i18n'
import styles from './StageReviewPanel.module.css'

const { Option } = Select
const { Column } = Table

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
    const { requestTriggerStage } = useExecuteDetail()
    const authStore = useAuthStore()

    const isCancel = ref(false)
    const suggest = ref('')
    const errMessage = ref('')
    const isApproving = ref(false)
    const curStep = ref(1)
    const localReviewParams = ref<ReviewParam[]>([])
    const errorParamKeys = ref<Set<string>>(new Set())

    const stageControl = computed<Partial<CheckConfig>>(() => {
      return (props.stage?.[props.reviewType] as Partial<CheckConfig>) || {}
    })

    const isManualTrigger = computed(() => {
      return stageControl.value.manualTrigger === true
    })

    const reviewGroups = computed<ReviewGroup[]>(() => {
      return stageControl.value.reviewGroups || []
    })

    const hasReviewParams = computed(() => localReviewParams.value.length > 0)

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

    const initReviewParams = () => {
      const curGroup = curReviewGroup.value
      const groupParams = curGroup?.params
      const baseParams = stageControl.value.reviewParams || []
      const source = Array.isArray(groupParams) && groupParams.length > 0
        ? groupParams
        : baseParams
      localReviewParams.value = JSON.parse(JSON.stringify(source))
    }

    watch(
      () => props.isShow,
      (val) => {
        if (val) {
          errMessage.value = ''
          errorParamKeys.value.clear()
          const execIndex = reviewGroups.value.findIndex((x) => x.status === undefined)
          curStep.value = execIndex >= 0 ? execIndex + 1 : 1
          initReviewParams()
        }
      },
      { immediate: true },
    )

    const handleStepChange = (index: number) => {
      curStep.value = index
    }

    const handleClose = () => {
      emit('close')
    }

    const getParamDisplayName = (param: ReviewParam) => {
      return param.chineseName || param.key.replace(/^variables\./, '')
    }

    const validateReviewParams = (): boolean => {
      if (isCancel.value) {
        errorParamKeys.value.clear()
        return true
      }

      const errors = new Set<string>()
      const errorNames: string[] = []
      for (const param of localReviewParams.value) {
        if (!param.required) continue
        const { value, valueType, key } = param
        let isEmpty = value === undefined || value === ''
        if (!isEmpty && valueType === ParamType.MULTIPLE && Array.isArray(value) && value.length === 0) {
          isEmpty = true
        }
        if (isEmpty) {
          errors.add(key)
          errorNames.push(getParamDisplayName(param))
        }
      }

      errorParamKeys.value = errors
      if (errors.size > 0) {
        errMessage.value = t('flow.execute.paramsRequiredError', [errorNames.join(', ')])
        return false
      }
      return true
    }

    const confirmApprove = async () => {
      if (isCancel.value && suggest.value.trim() === '') {
        errMessage.value = t('flow.execute.opinionRequired')
        return
      }

      if (!validateReviewParams()) return

      errMessage.value = ''

      if (!showReviewGroup.value) return

      isApproving.value = true
      try {
        await requestTriggerStage({
          stageId: props.stage.id,
          cancel: isCancel.value,
          suggest: suggest.value,
          id: showReviewGroup.value.id,
          reviewParams: localReviewParams.value as unknown as Record<string, unknown>[],
        })
        Message({ theme: 'success', message: t('flow.execute.operateSuc'), limit: 1 })
        handleClose()
        emit('approve')
      } catch (err: any) {
        Message({ theme: 'error', message: err?.message || t('flow.execute.operateFail'), limit: 1 })
      } finally {
        isApproving.value = false
      }
    }

    const renderParamValueInput = (param: ReviewParam, index: number) => {
      const isDisabled = disabled.value
      const { valueType } = param

      if (valueType === ParamType.BOOLEAN) {
        return (
          <Radio.Group
            modelValue={param.value}
            onChange={(val: boolean) => { localReviewParams.value[index]!.value = val }}
            disabled={isDisabled}
          >
            <Radio label={true}>true</Radio>
            <Radio label={false}>false</Radio>
          </Radio.Group>
        )
      }

      if (valueType === ParamType.ENUM) {
        return (
          <Select
            modelValue={param.value}
            onChange={(val: string) => { localReviewParams.value[index]!.value = val }}
            disabled={isDisabled}
            clearable
            class={styles.paramValueInput}
          >
            {(param.options || []).map((opt) => (
              <Option key={opt.value} value={opt.value} label={opt.key} />
            ))}
          </Select>
        )
      }

      if (valueType === ParamType.MULTIPLE) {
        return (
          <Select
            modelValue={param.value}
            onChange={(val: string[]) => { localReviewParams.value[index]!.value = val }}
            disabled={isDisabled}
            multiple
            clearable
            class={styles.paramValueInput}
          >
            {(param.options || []).map((opt) => (
              <Option key={opt.value} value={opt.value} label={opt.key} />
            ))}
          </Select>
        )
      }

      if (valueType === ParamType.TEXTAREA) {
        return (
          <Input
            type="textarea"
            modelValue={param.value as string}
            rows={2}
            disabled={isDisabled}
            onChange={(val: string) => { localReviewParams.value[index]!.value = val }}
            class={styles.paramValueInput}
          />
        )
      }

      return (
        <Input
          modelValue={param.value as string}
          disabled={isDisabled}
          onChange={(val: string) => { localReviewParams.value[index]!.value = val }}
          class={styles.paramValueInput}
        />
      )
    }

    const renderReviewParamsSection = () => {
      if (!hasReviewParams.value) return null

      return (
        <>
          <span class={styles.reviewSubtitle}>{t('flow.execute.customVariables')}</span>
          <div class={styles.reviewParamsList}>
            {localReviewParams.value.map((param, index) => {
              const hasError = errorParamKeys.value.has(param.key)
              return (
                <div key={param.key} class={styles.reviewParamRow}>
                  <div class={styles.reviewParamName}>
                    <Input modelValue={getParamDisplayName(param)} disabled class={styles.paramNameInput} />
                    <span class={styles.paramRequired}>{param.required ? '*' : ''}</span>
                  </div>
                  <div class={[styles.reviewParamValueWrap, hasError && styles.paramValueError]}>
                    {renderParamValueInput(param, index)}
                  </div>
                  {param.desc && (
                    <span
                      class={styles.paramDescIcon}
                      v-bk-tooltips={param.desc}
                    >
                      <SvgIcon name="info-circle" size={14} />
                    </span>
                  )}
                </div>
              )
            })}
          </div>
        </>
      )
    }

    const getReviewTimelineList = () => {
      return reviewGroups.value.map((group) => {
        const paramStr = (group.params || []).map(({ key, value }) => {
          const name = (String(key || '')).replace(/^variables\./, '')
          return `${name}=${JSON.stringify(value)}`
        }).join(' | ')

        let content: any
        let type: string

        switch (group.status) {
          case 'PROCESS':
            type = 'success'
            content = (
              <div class={styles.timelineItem}>
                <p class={styles.timelineTitle}>
                  <span class={styles.timelineName}>{group.name}</span>
                  <span class={styles.timelineNormal}>{t('flow.execute.approvedBy', [group.operator])}</span>
                  <span class={styles.timelineApprove}>{t('flow.execute.approveResult')}</span>
                </p>
                {paramStr && <p class={styles.timelineRow}><span class={styles.timelineLabel}>{t('flow.execute.changedParams')}</span>{paramStr}</p>}
                <p class={styles.timelineRow}><span class={styles.timelineLabel}>{t('flow.execute.approveOpinion')}</span>{group.suggest || ''}</p>
                <p class={styles.timelineRow}><span class={styles.timelineLabel}>{t('flow.execute.approveTime')}</span>{group.reviewTime ? convertTime(group.reviewTime) : '--'}</p>
              </div>
            )
            break
          case 'ABORT':
            type = 'danger'
            content = (
              <div class={styles.timelineItem}>
                <p class={styles.timelineTitle}>
                  <span class={styles.timelineName}>{group.name}</span>
                  <span class={styles.timelineNormal}>{t('flow.execute.abortedBy', [group.operator])}</span>
                  <span class={styles.timelineAbort}>{t('flow.execute.abortResult')}</span>
                </p>
                <p class={styles.timelineRow}><span class={styles.timelineLabel}>{t('flow.execute.approveOpinion')}</span>{group.suggest || ''}</p>
                <p class={styles.timelineRow}><span class={styles.timelineLabel}>{t('flow.execute.approveTime')}</span>{group.reviewTime ? convertTime(group.reviewTime) : '--'}</p>
              </div>
            )
            break
          default:
            type = 'default'
            content = (
              <div class={styles.timelineItem}>
                <p class={styles.timelineTitle}>
                  <span class={styles.timelineName}>{group.name}</span>
                  <span class={styles.timelineNormal}>{t('flow.execute.approver')}{group.reviewers.join(', ')}</span>
                </p>
              </div>
            )
        }

        return { tag: group.name, content, type, nodeType: 'vnode', filled: true }
      })
    }

    const nameFormatter = (_row: ReviewParam, _col: unknown, val: string) => {
      return (val || '').replace(/^variables\./, '')
    }

    const valueFormatter = (_row: ReviewParam, _col: unknown, val: unknown) => {
      if (Array.isArray(val)) return val.length ? `[${val.join(', ')}]` : '--'
      return val !== undefined && val !== '' ? String(val) : '--'
    }

    const renderShowContent = () => {
      const reviewParams = stageControl.value.reviewParams || []

      return (
        <div class={styles.stageReviewContent}>
          <span class={styles.reviewSubtitle}>{t('flow.stageReviewEdit.stageCondition')}</span>
          <Radio.Group modelValue={isManualTrigger.value} class={styles.reviewResult}>
            <Radio label={false} disabled>{t('flow.stageReviewEdit.autoExecute')}</Radio>
            <Radio label={true} disabled>{t('flow.stageReviewEdit.manualReview')}</Radio>
          </Radio.Group>

          {isManualTrigger.value && (
            <>
              {stageControl.value.reviewDesc && (
                <>
                  <span class={styles.reviewSubtitle}>{t('flow.execute.reviewDescription')}</span>
                  <div class={styles.reviewDescBox}>{stageControl.value.reviewDesc}</div>
                </>
              )}

              {reviewGroups.value.length > 0 && (
                <>
                  <span class={styles.reviewSubtitle}>{t('flow.execute.approvalFlow')}</span>
                  <Timeline list={getReviewTimelineList()} class={styles.reviewTimeline} />
                </>
              )}

              {reviewParams.length > 0 && (
                <>
                  <span class={styles.reviewSubtitle}>{t('flow.execute.customVariables')}</span>
                  <Table data={reviewParams} border="outer" class={styles.paramsTable}>
                    <Column label={t('flow.execute.alias')} showOverflowTooltip>
                      {{
                        default: ({ row }: { row: ReviewParam }) => (
                          <span>
                            {row.chineseName || '--'}
                            {row.desc && (
                              <span class={styles.paramInfoIcon} v-bk-tooltips={row.desc}>
                                <SvgIcon name="info-circle" size={14} />
                              </span>
                            )}
                          </span>
                        ),
                      }}
                    </Column>
                    <Column label={t('flow.execute.paramName')} prop="key" formatter={nameFormatter} showOverflowTooltip />
                    <Column label={t('flow.execute.paramValue')} prop="value" formatter={valueFormatter} showOverflowTooltip />
                  </Table>
                </>
              )}
            </>
          )}
        </div>
      )
    }

    const renderApproveContent = () => {
      if (!isStagePause.value || !isManualTrigger.value) {
        return renderShowContent()
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

          {renderReviewParamsSection()}

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
