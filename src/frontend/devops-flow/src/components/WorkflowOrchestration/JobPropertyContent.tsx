import type { Container, CustomVariable, Stage } from '@/api/flowModel'
import KeyValueMap from '@/components/AtomForm/KeyValueMap'
import { getJobRunConditionList } from '@/constants/flowOptionConfig'
import { JobRunCondition } from '@/utils/flowDefaults'
import { computeContainerIsError, validateContainer, validateJobControlOption } from '@/utils/validation'
import { Checkbox, Collapse, Form, Input, Radio, Select, Switcher } from 'bkui-vue'
import { computed, defineComponent, ref, watch, type PropType } from 'vue'
import { useI18n } from 'vue-i18n'
import styles from './JobPropertyPanel.module.css'
import sharedStyles from './shared.module.css'

const { FormItem } = Form

// Dependency type enum
enum DependOnType {
  ID = 'ID',
  NAME = 'NAME',
}

// Validation rules
const RULES = {
  MUTEX_QUEUE: { min: 1, max: 50 },
  MATRIX_CONCURRENCY: { min: 1, max: 20 },
}

export interface JobPropertyContentProps {
  /** Container data */
  container: Container | null
  /** Current Job's parent Stage (for getting dependent Job list) */
  stage?: Stage | null
  /** Current Job index in the Stage */
  containerIndex?: number
  /** Whether the form is editable */
  editable?: boolean
  /** Whether it's a new job */
  isNew?: boolean
  /** Whether it's a Finally Stage */
  isFinally?: boolean
  /** Whether to show the name field (only for new mode) */
  showNameField?: boolean
  /** Whether to show the job id field */
  showJobIdField?: boolean
}

export default defineComponent({
  name: 'JobPropertyContent',
  props: {
    container: {
      type: Object as PropType<Container | null>,
      default: null,
    },
    stage: {
      type: Object as PropType<Stage | null>,
      default: null,
    },
    containerIndex: {
      type: Number,
      default: -1,
    },
    editable: {
      type: Boolean,
      default: true,
    },
    isNew: {
      type: Boolean,
      default: false,
    },
    isFinally: {
      type: Boolean,
      default: false,
    },
    showNameField: {
      type: Boolean,
      default: true,
    },
    showJobIdField: {
      type: Boolean,
      default: true,
    },
  },
  emits: ['change'],
  setup(props, { emit }) {
    const { t } = useI18n()

    // ========== State ==========
    const formData = ref<Container | null>(null)
    // 默认展开流程控制选项面板
    const activeIndex = ref(['flowControl'])

    // ========== Computed ==========
    // Run condition options (distinguish between normal and Finally stage)
    const runConditionOptions = computed(() =>
      getJobRunConditionList(t, props.isFinally).map((opt) => ({
        label: opt.name,
        value: opt.id,
      })),
    )

    // Dependency type options
    const dependOnTypeOptions = [
      { label: t('flow.orchestration.dependOnById'), value: DependOnType.ID },
      { label: t('flow.orchestration.dependOnByName'), value: DependOnType.NAME },
    ]

    // List of Jobs that can be depended on (excluding current Job)
    const dependOnJobList = computed(() => {
      if (!props.stage?.containers) return []
      return props.stage.containers
        .filter((container, index) => index !== props.containerIndex && container.jobId)
        .map((container) => ({
          value: container.jobId,
          label: `${container.name} (${container.jobId})`,
          disabled: !container.jobId,
        }))
    })

    // Condition display computed properties
    const jobCtrl = computed(() => formData.value?.jobControlOption)
    const showCustomVariables = computed(() =>
      [
        JobRunCondition.CUSTOM_VARIABLE_MATCH,
        JobRunCondition.CUSTOM_VARIABLE_MATCH_NOT_RUN,
      ].includes(jobCtrl.value?.runCondition as JobRunCondition),
    )
    const showCustomCondition = computed(
      () => jobCtrl.value?.runCondition === JobRunCondition.CUSTOM_CONDITION_MATCH,
    )
    const showDependOnId = computed(
      () => jobCtrl.value?.dependOnType === DependOnType.ID || !jobCtrl.value?.dependOnType,
    )
    const showDependOnName = computed(() => jobCtrl.value?.dependOnType === DependOnType.NAME)

    const containerErrorFields = computed(() => {
      if (!formData.value) return []
      return validateContainer(formData.value)
    })

    const jobCtrlErrorFields = computed(() => {
      if (!formData.value) return []
      return validateJobControlOption(formData.value)
    })

    // Clean up runCondition related fields - with guard to prevent recursive updates
    watch(
      () => jobCtrl.value?.runCondition,
      (condition, oldCondition) => {
        if (!formData.value?.jobControlOption) return

        // Skip if condition hasn't actually changed
        if (condition === oldCondition) return

        const isVarMatch = [
          JobRunCondition.CUSTOM_VARIABLE_MATCH,
          JobRunCondition.CUSTOM_VARIABLE_MATCH_NOT_RUN,
        ].includes(condition as JobRunCondition)

        if (!isVarMatch) {
          formData.value.jobControlOption.customVariables = []
        } else if (!formData.value.jobControlOption.customVariables?.length) {
          formData.value.jobControlOption.customVariables = [{ key: 'param1', value: '' }]
        }

        if (condition !== JobRunCondition.CUSTOM_CONDITION_MATCH) {
          formData.value.jobControlOption.customCondition = ''
        }
      },
    )

    // ========== Watchers ==========
    // Sync props.container to formData
    watch(
      () => props.container,
      (container) => {
        formData.value = container ? { ...container } : null
      },
      { immediate: true },
    )
    // Emit change when formData changes - with debounce to prevent recursive updates
    watch(
      formData,
      () => {
        if (formData.value) {
          ; (formData.value as Record<string, unknown>).isError = computeContainerIsError(
            formData.value,
          )
          emit('change', formData.value)
        }
      },
      { deep: true },
    )

    // ========== Helpers ==========
    function stopPropagation(e: Event) {
      e.stopPropagation()
    }

    // ========== Render Helpers ==========
    const renderMatrixSection = () => {
      if (!formData.value?.matrixControlOption) return null
      const matrix = formData.value.matrixControlOption

      return (
        <div class={sharedStyles.flowControlSection}>
          <Collapse useBlockTheme>
            <Collapse.CollapsePanel>
              {{
                default: () => (
                  <div class={sharedStyles.collapseHeader}>
                    <div class={sharedStyles.collapseHeaderLeft}>
                      <span>{t('flow.orchestration.matrixJob')}</span>
                      <Switcher
                        v-model={formData.value!.matrixGroupFlag}
                        size="small"
                        theme="primary"
                        disabled={!props.editable}
                        onClick={stopPropagation}
                      />
                    </div>
                    <a
                      href="https://docs.bkci.net/"
                      target="_blank"
                      class={sharedStyles.docLink}
                      onClick={stopPropagation}
                    >
                      {t('flow.orchestration.viewDocumentation')}
                    </a>
                  </div>
                ),
                content: () =>
                  formData.value!.matrixGroupFlag && (
                    <div class={sharedStyles.collapseContent}>
                      <FormItem label={t('flow.orchestration.strategy')} required>
                        <Input
                          v-model={matrix.strategyStr}
                          type="textarea"
                          rows={4}
                          placeholder={t('flow.orchestration.strategyPlaceholder')}
                          disabled={!props.editable}
                        />
                        <p class={sharedStyles.fieldDesc}>{t('flow.orchestration.strategyDesc')}</p>
                      </FormItem>

                      <FormItem label={t('flow.orchestration.includeCase')}>
                        <Input
                          v-model={matrix.includeCaseStr}
                          type="textarea"
                          rows={4}
                          placeholder={t('flow.orchestration.includeCasePlaceholder')}
                          disabled={!props.editable}
                        />
                        <p class={sharedStyles.fieldDesc}>
                          {t('flow.orchestration.includeCaseDesc')}
                        </p>
                      </FormItem>

                      <FormItem label={t('flow.orchestration.excludeCase')}>
                        <Input
                          v-model={matrix.excludeCaseStr}
                          type="textarea"
                          rows={4}
                          placeholder={t('flow.orchestration.excludeCasePlaceholder')}
                          disabled={!props.editable}
                        />
                        <p class={sharedStyles.fieldDesc}>
                          {t('flow.orchestration.excludeCaseDesc')}
                        </p>
                      </FormItem>

                      <FormItem>
                        <Checkbox v-model={matrix.fastKill} disabled={!props.editable}>
                          {t('flow.orchestration.fastKill')}
                        </Checkbox>
                      </FormItem>

                      <FormItem label={t('flow.orchestration.maxConcurrency')} required>
                        <Input
                          v-model={matrix.maxConcurrency}
                          type="number"
                          min={RULES.MATRIX_CONCURRENCY.min}
                          max={RULES.MATRIX_CONCURRENCY.max}
                          placeholder={t('flow.orchestration.maxConcurrencyPlaceholder')}
                          disabled={!props.editable}
                        />
                      </FormItem>
                    </div>
                  ),
              }}
            </Collapse.CollapsePanel>
          </Collapse>
        </div>
      )
    }

    const renderFlowControlSection = () => {
      if (!formData.value?.jobControlOption) return null
      const ctrl = formData.value.jobControlOption

      return (
        <div class={sharedStyles.flowControlSection}>
          <Collapse modelValue={activeIndex.value} useBlockTheme>
            <Collapse.CollapsePanel name="flowControl">
              {{
                default: () => (
                  <div class={sharedStyles.collapseHeader}>
                    <span class={styles.collapseTitle}>
                      {t('flow.orchestration.flowControlOptions')}
                    </span>
                  </div>
                ),
                content: () => (
                  <div class={sharedStyles.collapseContent}>
                    {/* Enable Job */}
                    <FormItem>
                      <Checkbox v-model={ctrl.enable} disabled={!props.editable}>
                        {t('flow.orchestration.enableJob')}
                      </Checkbox>
                    </FormItem>

                    {/* Depend on previous Job */}
                    <FormItem label={t('flow.orchestration.dependOn')}>
                      <p class={sharedStyles.fieldDesc}>{t('flow.orchestration.dependOnDesc')}</p>
                      <Radio.Group v-model={ctrl.dependOnType} class={styles.dependOnTypeGroup}>
                        {dependOnTypeOptions.map((opt) => (
                          <Radio key={opt.value} label={opt.value} disabled={!props.editable}>
                            {opt.label}
                          </Radio>
                        ))}
                      </Radio.Group>

                      {showDependOnId.value && (
                        <Select
                          v-model={ctrl.dependOnId}
                          multiple
                          placeholder={t('flow.orchestration.selectDependOnJob')}
                          disabled={!props.editable}
                          list={dependOnJobList.value}
                        />
                      )}

                      {showDependOnName.value && (
                        <Input
                          v-model={ctrl.dependOnName}
                          placeholder={t('flow.orchestration.dependOnNamePlaceholder')}
                          disabled={!props.editable}
                        />
                      )}
                    </FormItem>

                    {/* Job timeout */}
                    <FormItem
                      label={t('flow.orchestration.jobTimeout')}
                      required
                      class={jobCtrlErrorFields.value.includes('jobTimeout') ? sharedStyles.fieldError : ''}
                    >
                      <Input
                        v-model={ctrl.timeout}
                        type="number"
                        placeholder={t('flow.orchestration.jobTimeoutPlaceholder')}
                        disabled={!props.editable}
                      />
                      <p class={sharedStyles.fieldDesc}>{t('flow.orchestration.jobTimeoutDesc')}</p>
                    </FormItem>

                    {/* Run condition */}
                    <FormItem label={t('flow.orchestration.whenToRunJob')} required>
                      <Select
                        v-model={ctrl.runCondition}
                        disabled={!props.editable}
                        list={runConditionOptions.value}
                      />
                    </FormItem>

                    {/* Custom variables */}
                    {showCustomVariables.value && (
                      <FormItem
                        required
                        class={jobCtrlErrorFields.value.includes('jobCustomVariables') ? sharedStyles.fieldError : ''}
                        label={t('flow.orchestration.customVar')}
                      >
                        <KeyValueMap
                          value={ctrl.customVariables || []}
                          name="customVariables"
                          handleChange={(_: string, val: CustomVariable[]) =>
                            (ctrl.customVariables = val)
                          }
                          addBtnText={t('flow.orchestration.addVariable')}
                          keyPlaceholder={t('flow.orchestration.envKeyPlaceholder')}
                          valuePlaceholder={t('flow.orchestration.envValuePlaceholder')}
                          allowNull={false}
                          disabled={!props.editable}
                        />
                      </FormItem>
                    )}

                    {/* Custom condition expression */}
                    {showCustomCondition.value && (
                      <FormItem
                        label={t('flow.orchestration.customConditionExp')}
                        required
                        class={jobCtrlErrorFields.value.includes('jobCustomCondition') ? sharedStyles.fieldError : ''}
                        v-slots={{
                          label: () => (
                            <div class={sharedStyles.labelWithIcon}>
                              <span>{t('flow.orchestration.customConditionExp')}</span>
                              <a
                                href="https://docs.bkci.net/"
                                target="_blank"
                                class={sharedStyles.docLink}
                              >
                                {t('flow.orchestration.viewDocumentation')}
                              </a>
                            </div>
                          ),
                        }}
                      >
                        <Input
                          v-model={ctrl.customCondition}
                          placeholder={t('flow.orchestration.customConditionExpPlaceholder')}
                          disabled={!props.editable}
                        />
                      </FormItem>
                    )}
                  </div>
                ),
              }}
            </Collapse.CollapsePanel>
          </Collapse>
        </div>
      )
    }

    const renderMutexSection = () => {
      if (!formData.value?.mutexGroup) return null
      const mutex = formData.value.mutexGroup

      return (
        <div class={sharedStyles.flowControlSection}>
          <Collapse useBlockTheme>
            <Collapse.CollapsePanel>
              {{
                default: () => (
                  <div class={sharedStyles.collapseHeader}>
                    <div class={sharedStyles.collapseHeaderLeft}>
                      <span>{t('flow.orchestration.mutexGroup')}</span>
                      <Switcher
                        v-model={mutex.enable}
                        size="small"
                        theme="primary"
                        disabled={!props.editable}
                        onClick={stopPropagation}
                      />
                    </div>
                  </div>
                ),
                content: () =>
                  mutex.enable && (
                    <div class={sharedStyles.collapseContent}>
                      <FormItem
                        label={t('flow.orchestration.mutexGroupName')}
                        required
                        class={containerErrorFields.value.includes('mutexGroupName') ? sharedStyles.fieldError : ''}
                      >
                        <Input
                          v-model={mutex.mutexGroupName}
                          placeholder={t('flow.orchestration.mutexGroupNamePlaceholder')}
                          disabled={!props.editable}
                        />
                      </FormItem>

                      <FormItem>
                        <Checkbox v-model={mutex.queueEnable} disabled={!props.editable}>
                          {t('flow.orchestration.queueEnable')}
                        </Checkbox>
                      </FormItem>

                      {mutex.queueEnable && (
                        <>
                          <FormItem label={t('flow.orchestration.mutexTimeout')} required>
                            <Input
                              v-model={mutex.timeoutVar}
                              type="number"
                              placeholder={t('flow.orchestration.mutexTimeoutPlaceholder')}
                              disabled={!props.editable}
                            />
                            <p class={sharedStyles.fieldDesc}>
                              {t('flow.orchestration.mutexTimeoutDesc')}
                            </p>
                          </FormItem>

                          <FormItem label={t('flow.orchestration.queueSize')} required>
                            <Input
                              v-model={mutex.queue}
                              type="number"
                              min={RULES.MUTEX_QUEUE.min}
                              max={RULES.MUTEX_QUEUE.max}
                              placeholder={t('flow.orchestration.queueSizePlaceholder')}
                              disabled={!props.editable}
                            />
                          </FormItem>
                        </>
                      )}
                    </div>
                  ),
              }}
            </Collapse.CollapsePanel>
          </Collapse>
        </div>
      )
    }

    // ========== Render ==========
    return () =>
      formData.value && (
        <div class={styles.jobPanelContent}>
          <Form form-type="vertical" model={formData.value}>
            {/* Job Name - only show in new mode and when showNameField is true */}
            {props.isNew && props.showNameField && (
              <FormItem property="name" label={t('flow.orchestration.jobName')} required>
                <Input
                  v-model={formData.value.name}
                  maxlength={30}
                  placeholder={t('flow.orchestration.jobNamePlaceholder')}
                  disabled={!props.editable}
                />
              </FormItem>
            )}

            {/* Job ID */}
            {props.showJobIdField && (
              <FormItem
                label={t('flow.orchestration.jobId')}
                property="jobId"
                required
                class={containerErrorFields.value.includes('jobId') ? sharedStyles.fieldError : ''}
              >
                <Input
                  v-model={formData.value.jobId}
                  placeholder={t('flow.orchestration.jobIdPlaceholder')}
                  disabled={!props.editable}
                />
              </FormItem>
            )}

            {/* Matrix Job */}
            {renderMatrixSection()}

            {/* Flow Control */}
            {renderFlowControlSection()}

            {/* Mutex Group */}
            {renderMutexSection()}
          </Form>
        </div>
      )
  },
})
