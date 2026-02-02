import type { Element } from '@/api/flowModel'
import AtomForm, { DISPLAY_MODE, type AtomPropsModel } from '@/components/AtomForm/AtomForm'
import { SvgIcon } from '@/components/SvgIcon'
import { createDefaultElement } from '@/utils/flowDefaults'
import {
  Button,
  Checkbox,
  Form,
  Input,
  Loading,
  Popover,
  Select,
  Sideslider,
  Switcher
} from 'bkui-vue'
import { computed, defineComponent, ref, watch, type PropType } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import type { TriggerModal } from '../../api/trigger'
import { useTriggerManager } from '../../hooks/useTriggerManager'
import styles from './TriggerPropertyPanel.module.css'

const cloneElement = (element: Element): Element => JSON.parse(JSON.stringify(element))

export default defineComponent({
  name: 'TriggerPropertyPanel',
  props: {
    visible: {
      type: Boolean,
      default: false,
    },
    element: {
      type: Object as PropType<Element | null>,
      default: null,
    },
  },
  emits: ['update:visible', 'save'],
  setup(props, { emit }) {
    const route = useRoute()
    const { t } = useI18n()
    const { FormItem } = Form
    const localElement = ref<Element | null>(null)
    const atomModal = ref<TriggerModal | null>(null)
    const isLoadingModalState = ref(false)
    const nameEditing = ref(false)
    const editingName = ref('')
    const defaultAdditionalOptions = createDefaultElement(0).additionalOptions!
    const triggerManager = useTriggerManager()

    const resetLocalElement = () => {
      localElement.value = props.element ? cloneElement(props.element) : null
    }

    watch(
      () => props.element,
      () => resetLocalElement(),
      { immediate: true },
    )

    watch(
      () => props.visible,
      (visible) => {
        if (visible) {
          resetLocalElement()
        }
      },
    )

    const ensureElementStructure = () => {
      if (!localElement.value) return
      if (!localElement.value.data) {
        localElement.value.data = { input: {}, output: [] }
      }
      if (!localElement.value.data.input) {
        localElement.value.data.input = {}
      }
      if (!localElement.value.additionalOptions) {
        localElement.value.additionalOptions = { ...defaultAdditionalOptions }
      }
    }

    const triggerType = computed(
      () => localElement.value?.atomCode || localElement.value?.['@type'] || '',
    )

    const getTriggerName = () => {
      if (localElement.value?.name) {
        return localElement.value.name
      }
      if (triggerType.value === 'manualTrigger') return t('flow.content.manualTrigger')
      if (triggerType.value === 'timerTrigger') return t('flow.content.timerTrigger')
      return triggerType.value || t('flow.content.triggerEvents')
    }

    const isManualTrigger = computed(() => triggerType.value === 'manualTrigger')

    const atomCode = computed(() => triggerType.value)
    const atomVersion = computed(() => localElement.value?.version || '1.latest')
    const versionOptions = computed(() => {
      const version = localElement.value?.version || '1.latest'
      return [
        {
          label: version.replace('.*', '.latest'),
          value: version,
        },
      ]
    })

    const handleClose = () => {
      emit('update:visible', false)
    }

    // Name editing handlers
    const handleEditIconClick = () => {
      editingName.value = getTriggerName()
      nameEditing.value = true
    }

    const handleNameChange = (value: string) => {
      editingName.value = value
    }

    const handleNameBlur = () => {
      if (localElement.value && editingName.value !== getTriggerName()) {
        localElement.value.name = editingName.value
      }
      nameEditing.value = false
    }

    const handleNameEnter = () => {
      handleNameBlur()
    }

    // StepId handler
    const handleStepIdChange = (value: string) => {
      if (!localElement.value) return
      localElement.value.stepId = value
    }

    const handleEnableChange = (value: boolean) => {
      ensureElementStructure()
      if (!localElement.value?.additionalOptions) return
      localElement.value.additionalOptions.enable = value
    }

    const handleVersionChange = (version: string) => {
      if (!localElement.value) return
      localElement.value.version = version
    }

    const handleManualFieldChange = (
      field: 'canElementSkip' | 'useLatestParameters',
      value: boolean,
    ) => {
      if (!localElement.value) return
      ;(localElement.value as any)[field] = value
    }

    const updateInput = (key: string, value: any) => {
      ensureElementStructure()
      if (!localElement.value) return
      if (!localElement.value.data) return
      localElement.value.data.input[key] = value
    }

    const handleSave = () => {
      if (!localElement.value) {
        handleClose()
        return
      }
      emit('save', cloneElement(localElement.value))
      handleClose()
    }

    const renderManualSection = () => {
      if (!isManualTrigger.value) return null
      return (
          <Form form-type="vertical" class={styles.manualForm}>
            <FormItem>
              <Checkbox
                modelValue={localElement.value?.canElementSkip ?? false}
                onChange={(val: boolean) => handleManualFieldChange('canElementSkip', val)}
              >
                {t('flow.triggerPanel.manualAllowSkip')}
              </Checkbox>
            </FormItem>
            <FormItem>
              <Checkbox
                modelValue={localElement.value?.useLatestParameters ?? false}
                onChange={(val: boolean) => handleManualFieldChange('useLatestParameters', val)}
              >
                {t('flow.triggerPanel.manualReuseParams')}
              </Checkbox>
            </FormItem>
          </Form>
      )
    }
    const loadAtomModal = async () => {
      if (isManualTrigger.value) return
      const code = atomCode.value
      const version = atomVersion.value
      if (!code || !version || !props.visible) return
      
      isLoadingModalState.value = true
      try {
        const modal = await triggerManager.fetchModal(localElement.value?.ownerStoreCode || '', code, version)
        atomModal.value = modal
      } catch (error) {
        console.error('Failed to load trigger atom modal:', error)
      } finally {
        isLoadingModalState.value = false
      }
    }

    watch([atomCode, atomVersion, () => props.visible], loadAtomModal, { immediate: true })


    const atomPropsModel = computed(() => {
      const modal = atomModal.value
      if (!modal) return null
      return modal.props as AtomPropsModel
    })

    const atomValue = computed(() => {
      return localElement.value?.data?.input || {}
    })

    const handleAtomFormChange = (name: string, value: any) => {
      updateInput(name, value)
    }

    const hasAtomFormConfig = computed(() => Object.keys(atomPropsModel.value || {}).length > 0)
    const isLoadingModal = computed(() => isLoadingModalState.value)

    const renderDynamicFormSection = () => {
      if (!localElement.value) return null
      if (isLoadingModal.value) {
        return (
          <div class={styles.loadingContainer}>
            <Loading loading={true} />
          </div>
        )
      }
      if (isManualTrigger.value) {
        return renderManualSection()
      }
      if (hasAtomFormConfig.value && atomPropsModel.value) {
        return (
          <div class={styles.section}>
            <AtomForm
              atomPropsModel={atomPropsModel.value}
              atomValue={atomValue.value}
              element={localElement.value}
              displayMode={DISPLAY_MODE.TRIGGER}
              onChange={handleAtomFormChange}
            />
          </div>
        )
      }

      return null
    }

    const renderContent = () => {
      if (!localElement.value) {
        return <div class={styles.emptyState}>{t('flow.triggerPanel.emptyState')}</div>
      }

      return (
        <div class={styles.panelBody}>
          <div class={styles.fieldGroup}>
            {/* Step ID 和版本选择 - 一行两列布局 */}
            <div class={[styles.stepIdAndVersionRow, isLoadingModal.value && styles.disabled]}>
              {/* Step ID 列 */}
              <div class={styles.stepIdColumn}>
                <div class={styles.labelWithIcon}>
                  <span>{t('flow.orchestration.stepId')}</span>
                  <Popover content={t('flow.orchestration.stepIdDesc')} placement="top">
                    <span class={styles.infoIcon}>
                      <SvgIcon name="info-circle" size={14} />
                    </span>
                  </Popover>
                </div>
                <Input
                  modelValue={localElement.value?.stepId || ''}
                  placeholder={t('flow.orchestration.stepIdPlaceholder')}
                  disabled={isLoadingModal.value}
                  onChange={handleStepIdChange}
                  class={styles.stepIdInput}
                />
              </div>
              
              {/* 版本选择列 */}
              <div class={styles.versionColumn}>
                <span class={styles.versionLabel}>{t('flow.content.version')}</span>
                <Select
                  modelValue={localElement.value.version || '1.latest'}
                  list={versionOptions.value}
                  disabled={isLoadingModal.value}
                  onChange={handleVersionChange}
                />
              </div>
            </div>
          </div>

          {renderDynamicFormSection()}
        </div>
      )
    }

    return () => (
      <Sideslider isShow={props.visible} width={560} onClosed={handleClose}>
        {{
          header: () => (
            <div class={styles.header}>
              <div class={styles.nameEdit}>
                {nameEditing.value ? (
                  <Input
                    modelValue={editingName.value}
                    maxlength={30}
                    placeholder={t('flow.orchestration.atomNamePlaceholder')}
                    onBlur={handleNameBlur}
                    onEnter={handleNameEnter}
                    onChange={handleNameChange}
                    class={styles.nameInput}
                  />
                ) : (
                  <>
                    <p class={styles.nameText} title={getTriggerName()}>
                      {getTriggerName()}
                    </p>
                    <span class={styles.editIcon} onClick={handleEditIconClick}>
                      <SvgIcon name="edit" size={16} />
                    </span>
                  </>
                )}
              </div>
              <div class={[styles.enableToggle, isLoadingModal.value && styles.disabled]}>
                <Switcher
                  size="small"
                  theme="primary"
                  modelValue={localElement.value?.additionalOptions?.enable ?? true}
                  disabled={isLoadingModal.value}
                  onChange={handleEnableChange}
                />
                <span>{t('flow.triggerPanel.enabledLabel')}</span>
              </div>
            </div>
          ),
          default: () => <div class={styles.content}>{renderContent()}</div>,
          footer: () => (
            <div class={styles.footer}>
              <Button 
                theme="primary" 
                onClick={handleSave} 
                disabled={!localElement.value || isLoadingModal.value}
              >
                {t('flow.content.save')}
              </Button>
              <Button onClick={handleClose} disabled={isLoadingModal.value}>
                {t('flow.common.cancel')}
              </Button>
            </div>
          ),
        }}
      </Sideslider>
    )
  },
})
