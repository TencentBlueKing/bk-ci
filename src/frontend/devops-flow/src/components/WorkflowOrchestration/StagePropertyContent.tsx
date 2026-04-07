import type { CustomVariable, Stage } from '@/api/flowModel'
import KeyValueMap from '@/components/AtomForm/KeyValueMap'
import { SvgIcon } from '@/components/SvgIcon'
import { getStageRunConditionList } from '@/constants/flowOptionConfig'
import { StageRunCondition } from '@/utils/flowDefaults'
import { validateStageControlOption } from '@/utils/validation'
import { Checkbox, Collapse, Form, Input, Popover, Select } from 'bkui-vue'
import { computed, defineComponent, type PropType, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import sharedStyles from './shared.module.css'
import styles from './StagePropertyPanel.module.css'

const { FormItem } = Form

export interface StageFormData {
  name: string
  enable: boolean
  fastKill: boolean
  runCondition: string
  customVariables: CustomVariable[]
  customCondition: string
}

export interface StagePropertyContentProps {
  /** Stage data */
  stage: Stage | null
  /** Whether the form is editable */
  editable?: boolean
  /** Whether it's a new stage */
  isNew?: boolean
  /** Whether to show the name field (only for new mode) */
  showNameField?: boolean
}

export default defineComponent({
  name: 'StagePropertyContent',
  props: {
    stage: {
      type: Object as PropType<Stage | null>,
      default: null,
    },
    editable: {
      type: Boolean,
      default: true,
    },
    isNew: {
      type: Boolean,
      default: false,
    },
    showNameField: {
      type: Boolean,
      default: true,
    },
  },
  emits: ['change'],
  setup(props, { emit }) {
    const { t } = useI18n()

    // ========== State ==========
    const formData = ref<StageFormData>({
      name: '',
      enable: true,
      fastKill: false,
      runCondition: 'AFTER_LAST_FINISHED',
      customVariables: [],
      customCondition: '',
    })

    // 默认展开流程控制选项面板
    const activeIndex = ref(['flowControl'])

    // ========== Computed ==========
    const isTriggerStage = computed(() => props.stage?.containers?.[0]?.['@type'] === 'trigger')
    const isFinallyStage = computed(() => props.stage?.finally === true)
    const showCustomVariables = computed(() =>
      [
        StageRunCondition.CUSTOM_VARIABLE_MATCH,
        StageRunCondition.CUSTOM_VARIABLE_MATCH_NOT_RUN,
      ].includes(formData.value.runCondition as StageRunCondition),
    )
    const showCustomCondition = computed(
      () => formData.value.runCondition === StageRunCondition.CUSTOM_CONDITION_MATCH,
    )

    // Show flow control section only for non-trigger and non-finally stages
    const showFlowControl = computed(() => !isTriggerStage.value && !isFinallyStage.value)

    const stageCtrlErrorFields = computed(() => {
      if (!props.stage) return []
      return validateStageControlOption({
        stageControlOption: {
          enable: formData.value.enable,
          runCondition: formData.value.runCondition,
          customVariables: formData.value.customVariables,
          customCondition: formData.value.customCondition,
        },
      })
    })

    const runConditionOptions = getStageRunConditionList(t).map((opt) => ({
      label: opt.name,
      value: opt.id,
    }))

    // ========== Helpers ==========
    function extractFormData(stage: Stage): StageFormData {
      const ctrl = stage.stageControlOption
      return {
        name: stage.name || '',
        enable: ctrl?.enable ?? true,
        fastKill: stage.fastKill || false,
        runCondition: ctrl?.runCondition || 'AFTER_LAST_FINISHED',
        customVariables: ctrl?.customVariables || [],
        customCondition: ctrl?.customCondition || '',
      }
    }

    function isFormDataEqual(a: StageFormData, b: StageFormData): boolean {
      return (
        a.name === b.name &&
        a.enable === b.enable &&
        a.fastKill === b.fastKill &&
        a.runCondition === b.runCondition &&
        a.customCondition === b.customCondition &&
        a.customVariables.length === b.customVariables.length &&
        a.customVariables.every(
          (v, i) => v.key === b.customVariables[i]?.key && v.value === b.customVariables[i]?.value,
        )
      )
    }

    function buildUpdatedStage(): Stage | null {
      if (!props.stage) return null
      return {
        ...props.stage,
        name: formData.value.name,
        fastKill: formData.value.fastKill,
        stageControlOption: {
          ...props.stage.stageControlOption,
          enable: formData.value.enable,
          runCondition: formData.value.runCondition,
          customVariables: formData.value.customVariables,
          customCondition: formData.value.customCondition,
        },
      }
    }

    // ========== Watchers ==========
    // Sync props.stage → formData (guard against no-op updates to break the cycle:
    // props.stage → formData → emit('change') → parent update → props.stage …)
    watch(
      () => props.stage,
      (stage) => {
        if (!stage) return
        const incoming = extractFormData(stage)
        if (isFormDataEqual(incoming, formData.value)) return
        formData.value = incoming
      },
      { immediate: true, deep: true },
    )

    // Clean up runCondition related fields
    watch(
      () => formData.value.runCondition,
      (condition) => {
        const isVarMatch = [
          StageRunCondition.CUSTOM_VARIABLE_MATCH,
          StageRunCondition.CUSTOM_VARIABLE_MATCH_NOT_RUN,
        ].includes(condition as StageRunCondition)

        if (!isVarMatch) {
          formData.value.customVariables = []
        } else if (!formData.value.customVariables?.length) {
          formData.value.customVariables = [{ key: 'param1', value: '' }]
        }

        if (condition !== StageRunCondition.CUSTOM_CONDITION_MATCH) {
          formData.value.customCondition = ''
        }
      },
    )

    // Emit change when formData changes
    watch(
      formData,
      () => {
        const updated = buildUpdatedStage()
        if (updated) {
          ; (updated as Record<string, unknown>).isError = stageCtrlErrorFields.value.length > 0
          emit('change', updated)
        }
      },
      { deep: true },
    )

    // ========== Render ==========
    return () => (
      <div class={styles.stagePanelContent}>
        <Form form-type="vertical" model={formData.value}>
          {/* Stage Name - only show in new mode and when showNameField is true */}
          {props.isNew && props.showNameField && (
            <FormItem label={t('flow.orchestration.stageName')} required>
              <Input
                v-model={formData.value.name}
                maxlength={30}
                placeholder={t('flow.orchestration.stageNamePlaceholder')}
                disabled={!props.editable}
              />
            </FormItem>
          )}

          {/* Flow Control Section */}
          {showFlowControl.value && (
            <div class={sharedStyles.flowControlSection}>
              <Collapse modelValue={activeIndex.value} useBlockTheme>
                <Collapse.CollapsePanel name="flowControl">
                  {{
                    default: () => (
                      <div class={sharedStyles.collapseHeader}>
                        <span>{t('flow.orchestration.flowControlOptions')}</span>
                      </div>
                    ),
                    content: () => (
                      <div class={sharedStyles.collapseContent}>
                        <FormItem>
                          <Checkbox v-model={formData.value.enable} disabled={!props.editable}>
                            {t('flow.orchestration.enableStage')}
                          </Checkbox>
                        </FormItem>

                        <FormItem>
                          <Checkbox v-model={formData.value.fastKill} disabled={!props.editable}>
                            {t('flow.orchestration.stageFastKill')}
                          </Checkbox>
                          <Popover
                            content={t('flow.orchestration.stageFastKillDesc')}
                            placement="top"
                            trigger="click"
                            boundary="window"
                          >
                            <span class={sharedStyles.infoIcon}>
                              <SvgIcon name="info-circle" size={14} />
                            </span>
                          </Popover>
                        </FormItem>

                        <FormItem label={t('flow.orchestration.whenToRunStage')} required>
                          <Select
                            v-model={formData.value.runCondition}
                            disabled={!props.editable}
                            list={runConditionOptions}
                          />
                        </FormItem>

                        {showCustomVariables.value && (
                          <FormItem
                            required
                            v-slots={{
                              label: () => (
                                <div class={sharedStyles.labelWithIcon}>
                                  <span>{t('flow.orchestration.customVar')}</span>
                                </div>
                              ),
                            }}
                          >
                            <KeyValueMap
                              value={formData.value.customVariables}
                              name="customVariables"
                              handleChange={(_: string, val: CustomVariable[]) =>
                                (formData.value.customVariables = val)
                              }
                              addBtnText={t('flow.orchestration.addVariable')}
                              keyPlaceholder={t('flow.orchestration.envKeyPlaceholder')}
                              valuePlaceholder={t('flow.orchestration.envValuePlaceholder')}
                              allowNull={false}
                              disabled={!props.editable}
                            />
                          </FormItem>
                        )}

                        {showCustomCondition.value && (
                          <FormItem
                            required
                            class={stageCtrlErrorFields.value.includes('stageCustomCondition') ? sharedStyles.fieldError : ''}
                            label={t('flow.orchestration.customConditionExp')}
                          >
                            <Input
                              v-model={formData.value.customCondition}
                              placeholder={t('flow.orchestration.customConditionExpPlaceholder')}
                              disabled={!props.editable}
                            />
                            {stageCtrlErrorFields.value.includes('stageCustomCondition') && (
                              <p class={sharedStyles.fieldErrorMessage}>
                                {t('flow.orchestration.fieldRequired')}
                              </p>
                            )}
                          </FormItem>
                        )}
                      </div>
                    ),
                  }}
                </Collapse.CollapsePanel>
              </Collapse>
            </div>
          )}
        </Form>
      </div>
    )
  },
})
