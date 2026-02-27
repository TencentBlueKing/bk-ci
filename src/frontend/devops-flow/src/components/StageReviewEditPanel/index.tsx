import StaffInput from '@/components/AtomForm/StaffInput'
import { SvgIcon } from '@/components/SvgIcon'
import { useUIStore } from '@/stores/ui'
import type { CheckConfig, ReviewGroup, Stage } from '@/types/flow'
import { Button, Divider, Form, Input, Radio, Sideslider } from 'bkui-vue'
import { storeToRefs } from 'pinia'
import { computed, defineComponent, type PropType, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import styles from './StageReviewEditPanel.module.css'

const { FormItem } = Form

const DEFAULT_CHECK_CONFIG: CheckConfig = {
  manualTrigger: false,
  reviewGroups: [],
  notifyType: ['RTX'],
  markdownContent: true,
  timeout: 24,
}

export default defineComponent({
  name: 'StageReviewEditPanel',
  props: {
    isShow: {
      type: Boolean,
      default: false,
    },
    stage: {
      type: Object as PropType<Stage | null>,
      default: null,
    },
    checkType: {
      type: String as PropType<'checkIn' | 'checkOut'>,
      default: 'checkIn',
    },
  },
  emits: ['close', 'change'],
  setup(props, { emit }) {
    const { t } = useI18n()
    const { isVariablePanelOpen } = storeToRefs(useUIStore())

    const formData = reactive<CheckConfig>({ ...DEFAULT_CHECK_CONFIG })
    const reviewGroupsCopy = ref<ReviewGroup[]>([])

    const stageTitle = computed(() => {
      const stageName = props.stage?.name || 'Stage'
      const typeLabel = props.checkType === 'checkIn'
        ? t('flow.stageReviewEdit.checkIn')
        : t('flow.stageReviewEdit.checkOut')
      return `${stageName} - ${typeLabel}`
    })

    const hasReviewerError = computed(() => {
      if (!formData.manualTrigger) return false
      if (reviewGroupsCopy.value.length === 0) return true
      return reviewGroupsCopy.value.some(
        (g) => !g.name || (g.reviewers.length === 0 && (!g.groups || g.groups.length === 0)),
      )
    })

    const timeoutValid = computed(() => {
      const t = Number(formData.timeout)
      return Number.isFinite(t) && t >= 1 && t <= 720
    })

    watch(
      () => [props.isShow, props.stage, props.checkType],
      () => {
        if (props.isShow && props.stage) {
          const existing = (props.stage[props.checkType] as CheckConfig) || {}
          const merged = { ...DEFAULT_CHECK_CONFIG, ...existing }
          Object.assign(formData, merged)
          reviewGroupsCopy.value = JSON.parse(
            JSON.stringify(merged.reviewGroups || []),
          )
        }
      },
      { immediate: true },
    )

    const emitChange = () => {
      if (!props.stage) return
      const updatedCheckConfig: CheckConfig = {
        ...formData,
        reviewGroups: reviewGroupsCopy.value,
        isReviewError: formData.manualTrigger && (hasReviewerError.value || !timeoutValid.value),
      }
      emit('change', props.stage, props.checkType, updatedCheckConfig)
    }

    const handleManualTriggerChange = (val: boolean) => {
      formData.manualTrigger = val
      emitChange()
    }

    const handleTimeoutChange = (val: string) => {
      formData.timeout = Number(val)
      emitChange()
    }

    const handleDescChange = (val: string) => {
      formData.reviewDesc = val
      emitChange()
    }

    const addReviewGroup = () => {
      if (reviewGroupsCopy.value.length >= 5) return
      reviewGroupsCopy.value.push({
        name: `Flow ${reviewGroupsCopy.value.length + 1}`,
        reviewers: [],
        groups: [],
      })
      emitChange()
    }

    const deleteReviewGroup = (index: number) => {
      reviewGroupsCopy.value.splice(index, 1)
      emitChange()
    }

    const updateReviewGroupName = (index: number, val: string) => {
      const group = reviewGroupsCopy.value[index]
      if (group) group.name = val
      emitChange()
    }

    const updateReviewGroupUsers = (index: number, val: string[]) => {
      const group = reviewGroupsCopy.value[index]
      if (group) group.reviewers = val
      emitChange()
    }

    const handleClose = () => {
      emit('close')
    }

    const renderReviewGroups = () => {
      return (
        <div>
          {reviewGroupsCopy.value.map((group, index) => (
            <div key={index} class={styles.reviewGroupItem}>
              <Input
                class={styles.reviewGroupName}
                modelValue={group.name}
                placeholder={`Flow ${index + 1}`}
                onInput={(val: string) => updateReviewGroupName(index, val)}
              />
              <div class={styles.reviewGroupUsers}>
                <StaffInput
                  value={group.reviewers}
                  name={`reviewers-${index}`}
                  placeholder={t('flow.stageReviewEdit.reviewerPlaceholder')}
                  handleChange={(_: string, val: string[]) => updateReviewGroupUsers(index, val)}
                />
              </div>
              <Button
                text
                theme="primary"
                class={styles.reviewGroupDelete}
                onClick={() => deleteReviewGroup(index)}
              >
                {t('flow.actions.delete')}
              </Button>
            </div>
          ))}
          {hasReviewerError.value && formData.manualTrigger && (
            <span class={styles.errorTip}>
              {t('flow.stageReviewEdit.reviewerRequired')}
            </span>
          )}
          <Button
            text
            theme="primary"
            class={styles.addReviewGroupBtn}
            disabled={reviewGroupsCopy.value.length >= 5}
            onClick={addReviewGroup}
          >
            <SvgIcon name="add-small" size={16} />
            {t('flow.stageReviewEdit.addReviewFlow')}
          </Button>
        </div>
      )
    }

    return () => (
      <Sideslider
        isShow={props.isShow}
        width={640}
        quickClose={true}
        transfer
        onClosed={handleClose}
        onUpdate:isShow={(val: boolean) => !val && handleClose()}
        class={['bkci-property-panel', isVariablePanelOpen.value && 'with-variable-open']}
      >
        {{
          header: () => <span>{stageTitle.value}</span>,
          default: () => (
            <div class={styles.stageReviewEditContent}>
              <Form formType="vertical">
                <FormItem label={t('flow.stageReviewEdit.stageCondition')}>
                  <Radio.Group
                    modelValue={formData.manualTrigger}
                    onChange={handleManualTriggerChange}
                    class={styles.reviewRadioGroup}
                  >
                    <Radio label={false}>{t('flow.stageReviewEdit.autoExecute')}</Radio>
                    <Radio label={true}>{t('flow.stageReviewEdit.manualReview')}</Radio>
                  </Radio.Group>
                </FormItem>

                {formData.manualTrigger && (
                  <>
                    <Divider class={styles.sectionDivider} />

                    <FormItem
                      label={t('flow.stageReviewEdit.approvalFlow')}
                      required
                      class={styles.formSection}
                    >
                      {renderReviewGroups()}
                    </FormItem>

                    <FormItem
                      label={t('flow.stageReviewEdit.reviewDesc')}
                      class={styles.formSection}
                    >
                      <Input
                        type="textarea"
                        modelValue={formData.reviewDesc || ''}
                        placeholder={t('flow.stageReviewEdit.reviewDescPlaceholder')}
                        rows={3}
                        onInput={handleDescChange}
                      />
                    </FormItem>

                    <FormItem
                      label={t('flow.stageReviewEdit.timeout')}
                      required
                      class={styles.formSection}
                    >
                      <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                        <Input
                          type="number"
                          class={styles.timeoutInput}
                          modelValue={String(formData.timeout)}
                          min={1}
                          max={720}
                          onInput={handleTimeoutChange}
                        />
                        <span class={styles.timeoutSuffix}>
                          {t('flow.stageReviewEdit.hours')}
                        </span>
                      </div>
                      {!timeoutValid.value && (
                        <span class={styles.errorTip}>
                          {t('flow.stageReviewEdit.timeoutError')}
                        </span>
                      )}
                    </FormItem>
                  </>
                )}
              </Form>
            </div>
          ),
        }}
      </Sideslider>
    )
  },
})
