import type { AtomModal } from '@/api/atom'
import type { Element } from '@/api/flowModel'
import AtomForm, { DISPLAY_MODE, type AtomPropsModel } from '@/components/AtomForm/AtomForm'
import { SvgIcon } from '@/components/SvgIcon'
import { getAtomDefaultValue, getAtomOutputObj } from '@/utils/atom'
import { createDefaultElement } from '@/utils/flowDefaults'
import { validateAtomElement } from '@/utils/validation'
import {
  Button,
  Checkbox,
  Form,
  Input,
  Loading,
  Message,
  Popover,
  Select,
  Sideslider,
  Switcher,
} from 'bkui-vue'
import { computed, defineComponent, ref, watch, type PropType } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import type { AtomVersion } from '../../api/atom'
import type { TriggerModal } from '../../api/trigger'
import { useTriggerManager } from '../../hooks/useTriggerManager'
import styles from './TriggerPropertyPanel.module.css'

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
    readonly: {
      type: Boolean,
      default: false,
    },
  },
  emits: ['update:visible', 'save'],
  setup(props, { emit }) {
    const { t } = useI18n()
    const { FormItem } = Form
    const route = useRoute()
    const triggerManager = useTriggerManager()
    const projectCode = computed(() => route.params.projectId as string)

    const localElement = ref<Element | null>(null)
    const atomModal = ref<TriggerModal | null>(null)
    const isLoadingModal = ref(false)
    const isLoadingVersions = ref(false)
    const versionList = ref<AtomVersion[]>([])
    const nameEditing = ref(false)
    const editingName = ref('')
    const showErrors = ref(false)

    // ========== Element Initialization ==========

    const resetLocalElement = () => {
      if (!props.element) {
        localElement.value = null
        return
      }
      const el: Element = JSON.parse(JSON.stringify(props.element))
      if (!el.data) el.data = { input: {}, output: [] }
      if (!el.data.input) el.data.input = {}
      if (!el.additionalOptions) {
        el.additionalOptions = { ...createDefaultElement(0).additionalOptions! }
      }
      localElement.value = el
    }

    watch(() => props.element, resetLocalElement, { immediate: true })
    watch(
      () => props.visible,
      (visible) => {
        if (visible) {
          showErrors.value = false
          resetLocalElement()
        }
      },
    )

    // ========== Computed ==========

    const triggerType = computed(
      () => localElement.value?.atomCode || localElement.value?.['@type'] || '',
    )
    const isManualTrigger = computed(() => triggerType.value === 'manualTrigger')

    const triggerName = computed(() => {
      if (localElement.value?.name) return localElement.value.name
      if (triggerType.value === 'manualTrigger') return t('flow.content.manualTrigger')
      if (triggerType.value === 'timerTrigger') return t('flow.content.timerTrigger')
      return triggerType.value || t('flow.content.triggerEvents')
    })

    const atomVersion = computed(() => localElement.value?.version || '1.latest')
    const versionOptions = computed(() => {
      if (versionList.value.length > 0) {
        return versionList.value.map((v) => ({
          label: v.versionValue.replace('.*', '.latest'),
          value: v.versionValue,
        }))
      }
      return [
        {
          label: atomVersion.value.replace('.*', '.latest'),
          value: atomVersion.value,
        },
      ]
    })

    const atomPropsModel = computed(() => {
      if (!atomModal.value) return null
      return atomModal.value.props as AtomPropsModel
    })

    const atomValue = computed(() => {
      const input = localElement.value?.data?.input
      if (input && Object.keys(input).length > 0) return input
      const inputModel = atomPropsModel.value?.input || atomPropsModel.value || {}
      return getAtomDefaultValue(inputModel)
    })

    const hasAtomFormConfig = computed(() => Object.keys(atomPropsModel.value || {}).length > 0)
    const isDisabled = computed(() => isLoadingModal.value || props.readonly)

    const triggerErrorFields = computed(() => {
      if (!localElement.value || isManualTrigger.value) return []
      return validateAtomElement(
        localElement.value,
        atomModal.value as AtomModal | null,
        atomValue.value,
        { skipAdditionalOptions: true },
      )
    })

    // ========== Modal Loading ==========

    const applyModalDefaults = (modal: TriggerModal) => {
      if (!localElement.value?.data) return

      const modalProps = modal.props || {}
      const inputModel = (modalProps as Record<string, any>).input || modalProps
      const outputModel = (modalProps as Record<string, any>).output || {}

      if (Object.keys(localElement.value.data.input).length === 0) {
        localElement.value.data.input = getAtomDefaultValue(inputModel)
      }

      const currentOutput = localElement.value.data.output
      const isOutputEmpty =
        !currentOutput ||
        (Array.isArray(currentOutput) && currentOutput.length === 0) ||
        (typeof currentOutput === 'object' && Object.keys(currentOutput).length === 0)
      if (isOutputEmpty) {
        localElement.value.data.output = getAtomOutputObj(outputModel) as any
      }
    }

    const loadAtomModal = async () => {
      if (isManualTrigger.value) return
      const code = triggerType.value
      const version = atomVersion.value
      if (!code || !version || !props.visible) return

      isLoadingModal.value = true
      try {
        const modal = await triggerManager.fetchModal(
          localElement.value?.ownerStoreCode || '',
          code,
          version,
        )
        atomModal.value = modal
        if (modal) applyModalDefaults(modal)
      } catch (error) {
        console.error('Failed to load trigger atom modal:', error)
      } finally {
        isLoadingModal.value = false
      }
    }

    watch([triggerType, atomVersion, () => props.visible], loadAtomModal, { immediate: true })

    const loadVersionList = async () => {
      if (isManualTrigger.value) return
      const code = triggerType.value
      if (!code || !projectCode.value || !props.visible) return

      isLoadingVersions.value = true
      try {
        versionList.value = await triggerManager.fetchVersionList(projectCode.value, code)
      } catch (error) {
        console.error('Failed to load trigger version list:', error)
      } finally {
        isLoadingVersions.value = false
      }
    }

    watch([triggerType, () => props.visible], loadVersionList, { immediate: true })

    // ========== Handlers ==========

    const handleClose = () => {
      showErrors.value = false
      emit('update:visible', false)
    }

    const handleSave = () => {
      if (!localElement.value) {
        handleClose()
        return
      }

      if (triggerErrorFields.value.length > 0) {
        showErrors.value = true
        Message({ theme: 'warning', message: t('flow.triggerPanel.validationError') })
        return
      }

      const elementToSave = JSON.parse(JSON.stringify(localElement.value))
      delete elementToSave.isError
      emit('save', elementToSave)
      handleClose()
    }

    const handleEditIconClick = () => {
      editingName.value = triggerName.value
      nameEditing.value = true
    }

    const handleNameBlur = () => {
      if (localElement.value && editingName.value !== triggerName.value) {
        localElement.value.name = editingName.value
      }
      nameEditing.value = false
    }

    const handleEnableChange = (value: boolean) => {
      if (!localElement.value?.additionalOptions) return
      localElement.value.additionalOptions.enable = value
    }

    const updateInput = (key: string, value: any) => {
      if (!localElement.value?.data) return
      localElement.value.data.input[key] = value
    }

    // ========== Render ==========

    const renderManualSection = () => (
      <Form form-type="vertical" class={styles.manualForm}>
        <FormItem>
          <Checkbox
            modelValue={localElement.value?.canElementSkip ?? false}
            disabled={props.readonly}
            onChange={(val: boolean) => {
              if (localElement.value) (localElement.value as any).canElementSkip = val
            }}
          >
            {t('flow.triggerPanel.manualAllowSkip')}
          </Checkbox>
        </FormItem>
        <FormItem>
          <Checkbox
            modelValue={localElement.value?.useLatestParameters ?? false}
            disabled={props.readonly}
            onChange={(val: boolean) => {
              if (localElement.value) (localElement.value as any).useLatestParameters = val
            }}
          >
            {t('flow.triggerPanel.manualReuseParams')}
          </Checkbox>
        </FormItem>
      </Form>
    )

    const renderDynamicFormSection = () => {
      if (!localElement.value) return null
      if (isLoadingModal.value) {
        return (
          <div class={styles.loadingContainer}>
            <Loading loading={true} />
          </div>
        )
      }
      if (isManualTrigger.value) return renderManualSection()
      if (hasAtomFormConfig.value && atomPropsModel.value) {
        return (
          <div class={styles.section}>
            <AtomForm
              atomPropsModel={atomPropsModel.value}
              atomValue={atomValue.value}
              element={localElement.value}
              displayMode={DISPLAY_MODE.TRIGGER}
              disabled={props.readonly}
              errorFields={showErrors.value ? triggerErrorFields.value : []}
              onChange={updateInput}
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
            <div class={[styles.stepIdAndVersionRow, isDisabled.value && styles.disabled]}>
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
                  modelValue={localElement.value.stepId || ''}
                  placeholder={t('flow.orchestration.stepIdPlaceholder')}
                  disabled={isDisabled.value}
                  onChange={(val: string) => {
                    localElement.value!.stepId = val
                  }}
                  class={styles.stepIdInput}
                />
              </div>
              <div class={styles.versionColumn}>
                <span class={styles.versionLabel}>{t('flow.content.version')}</span>
                <Select
                  modelValue={localElement.value.version || '1.latest'}
                  list={versionOptions.value}
                  loading={isLoadingVersions.value}
                  disabled={isDisabled.value}
                  onChange={(ver: string) => {
                    if (!localElement.value || ver === localElement.value.version) return
                    localElement.value.version = ver
                    localElement.value.data = { input: {}, output: [] }
                  }}
                />
              </div>
            </div>
          </div>
          {renderDynamicFormSection()}
        </div>
      )
    }

    return () => (
      <Sideslider isShow={props.visible} width={560} transfer onClosed={handleClose}>
        {{
          header: () => (
            <div class={styles.header}>
              <div class={styles.nameEdit}>
                {!props.readonly && nameEditing.value ? (
                  <Input
                    modelValue={editingName.value}
                    maxlength={30}
                    placeholder={t('flow.orchestration.atomNamePlaceholder')}
                    onBlur={handleNameBlur}
                    onEnter={handleNameBlur}
                    onChange={(val: string) => {
                      editingName.value = val
                    }}
                    class={styles.nameInput}
                  />
                ) : (
                  <>
                    <p class={styles.nameText} title={triggerName.value}>
                      {triggerName.value}
                    </p>
                    {!props.readonly && (
                      <span class={styles.editIcon} onClick={handleEditIconClick}>
                        <SvgIcon name="edit" size={16} />
                      </span>
                    )}
                  </>
                )}
              </div>
              <div class={[styles.enableToggle, isDisabled.value && styles.disabled]}>
                <Switcher
                  size="small"
                  theme="primary"
                  modelValue={localElement.value?.additionalOptions?.enable ?? true}
                  disabled={isDisabled.value}
                  onChange={handleEnableChange}
                />
                <span>{t('flow.triggerPanel.enabledLabel')}</span>
              </div>
            </div>
          ),
          default: () => <div class={styles.content}>{renderContent()}</div>,
          footer: () =>
            props.readonly ? null : (
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
