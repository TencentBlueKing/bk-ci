import type { Stage } from '@/api/flowModel'
import { SvgIcon } from '@/components/SvgIcon'
import { useUIStore } from '@/stores/ui'
import { validateStageControlOption } from '@/utils/validation'
import { Button, InfoBox, Input, Sideslider } from 'bkui-vue'
import { storeToRefs } from 'pinia'
import { defineComponent, type PropType, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import sharedStyles from './shared.module.css'
import StagePropertyContent from './StagePropertyContent'
import styles from './StagePropertyPanel.module.css'

export interface StagePropertyPanelProps {
  stage: Stage | null
  modelValue: boolean
  editable: boolean
  isNew?: boolean
}

export default defineComponent({
  name: 'StagePropertyPanel',
  props: {
    stage: {
      type: Object as PropType<Stage | null>,
      default: null,
    },
    modelValue: {
      type: Boolean,
      default: false,
    },
    editable: {
      type: Boolean,
      default: true,
    },
    isNew: {
      type: Boolean,
      default: false,
    },
  },
  emits: ['update:modelValue', 'change', 'confirm'],
  setup(props, { emit }) {
    const { t } = useI18n()
    const { isVariablePanelOpen } = storeToRefs(useUIStore())

    // ========== State ==========
    const nameEditing = ref(false)
    const stageName = ref('')
    const updatedStage = ref<Stage | null>(null)

    // ========== Helpers ==========
    function closePanel() {
      emit('update:modelValue', false)
    }

    // ========== Watchers ==========
    // Sync props.stage to local state only when a different stage is selected
    watch(
      () => props.stage?.id,
      () => {
        if (props.stage) {
          stageName.value = props.stage.name || ''
          updatedStage.value = { ...props.stage }
        }
      },
      { immediate: true },
    )

    // ========== Handlers ==========
    function handleStageChange(stage: Stage) {
      updatedStage.value = stage
      stageName.value = stage.name || ''
      // Emit change for edit mode (not new mode)
      if (!props.isNew) {
        emit('change', stage)
      }
    }

    function handleConfirm() {
      if (!stageName.value?.trim()) {
        InfoBox({
          title: t('flow.common.failed'),
          subTitle: t('flow.orchestration.stageNameRequired'),
          theme: 'danger',
        })
        return
      }
      if (updatedStage.value) {
        const stageErrors = validateStageControlOption(updatedStage.value)
        if (stageErrors.length > 0) {
          InfoBox({
            title: t('flow.common.failed'),
            subTitle: t('flow.orchestration.stageControlOptionInvalid'),
            theme: 'danger',
          })
          return
        }
        emit('confirm', updatedStage.value)
        closePanel()
      }
    }

    function exitNameEdit() {
      if (props.editable) nameEditing.value = false
    }

    function handleNameChange(val: string) {
      stageName.value = val
      if (updatedStage.value) {
        updatedStage.value = { ...updatedStage.value, name: val }
        if (!props.isNew) {
          emit('change', updatedStage.value)
        }
      }
    }

    // ========== Render ==========
    return () => (
      <Sideslider
        isShow={props.modelValue}
        width={640}
        quick-close
        transfer
        onUpdate:isShow={(val: boolean) => emit('update:modelValue', val)}
        class={['bkci-property-panel', isVariablePanelOpen.value && 'with-variable-open']}
      >
        {{
          header: () => (
            <div class={sharedStyles.propertyPanelHeader}>
              {props.isNew ? (
                <span>{t('flow.orchestration.addStage')}</span>
              ) : (
                <div class={sharedStyles.nameEdit}>
                  {nameEditing.value ? (
                    <Input
                      modelValue={stageName.value}
                      maxlength={30}
                      placeholder={t('flow.orchestration.stageNamePlaceholder')}
                      onBlur={exitNameEdit}
                      onEnter={exitNameEdit}
                      onChange={handleNameChange}
                      class={sharedStyles.nameInput}
                      autoFocus
                    />
                  ) : (
                    <>
                      <p class={sharedStyles.nameText} title={stageName.value}>
                        {stageName.value || t('flow.orchestration.stageNamePlaceholder')}
                      </p>
                      {props.editable && (
                        <span
                          class={sharedStyles.editIcon}
                          onClick={() => (nameEditing.value = true)}
                        >
                          <SvgIcon name="edit" size={16} />
                        </span>
                      )}
                    </>
                  )}
                </div>
              )}
            </div>
          ),

          default: () => (
            <StagePropertyContent
              class={styles.stagePropertyContent}
              stage={props.stage}
              editable={props.editable}
              isNew={props.isNew}
              showNameField={props.isNew}
              onChange={handleStageChange}
            />
          ),

          footer: () =>
            props.isNew && (
              <div class={styles.stagePanelFooter}>
                <Button theme="primary" onClick={handleConfirm} disabled={!props.editable}>
                  {t('flow.orchestration.add')}
                </Button>
                <Button onClick={closePanel}>{t('flow.common.cancel')}</Button>
              </div>
            ),
        }}
      </Sideslider>
    )
  },
})
