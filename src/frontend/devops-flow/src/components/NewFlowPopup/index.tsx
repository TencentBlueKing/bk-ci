import { useNewFlow } from '@/hooks/useNewFlow'
import { Button, Dialog, Steps } from 'bkui-vue'
import { defineComponent, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import BaseInfo from './BaseInfo'
import styles from './Index.module.css'
import SelectTemplate from './SelectTemplate'

export default defineComponent({
  name: 'NewFlowPopup',
  props: {
    isShow: {
      type: Boolean,
      default: false,
    },
  },
  emits: ['update:isShow', 'confirm'],
  setup(props, { emit }) {
    const { t } = useI18n()

    const {
      currentStep,
      formData,
      isLoading,
      baseInfoRef,
      resetForm,
      clearFormValidation,
      handleStepChange,
      handleNextStep,
      handlePrevStep,
      updateBaseInfo,
      updateTemplateInfo,
      handleConfirm,
    } = useNewFlow()

    // 步骤配置
    const steps = [
      {
        title: t('flow.content.basicSettings'),
        icon: 1,
        description: t('flow.content.chooseEnvironment'),
      },
      {
        title: t('flow.content.selectTemplate'),
        icon: 2,
        description: t('flow.content.startFromBlankOrTemplate'),
      },
    ]

    /**
     * 监听弹窗显示状态变化
     */
    watch(
      () => props.isShow,
      (newVal) => {
        if (newVal) {
          resetForm()
          clearFormValidation()
        }
      },
    )

    /**
     * 切换步骤按钮
     */
    async function handleChangeStep() {
      if (currentStep.value === 1) {
        await handleNextStep()
      } else {
        handlePrevStep()
      }
    }

    /**
     * 关闭弹窗
     */
    function onClose() {
      resetForm()
      emit('update:isShow', false)
    }

    return () => (
      <Dialog
        is-show={props.isShow}
        theme="primary"
        zIndex={1000}
        width={1200}
        quick-close={false}
        onClosed={onClose}
        class={styles.newFlowPopup}
      >
        {{
          header: () => (
            <div class={styles.header}>
              <span>{t('flow.content.newFlow')}</span>
              <div class={styles.stepContent}>
                <Steps
                  theme="primary"
                  controllable={true}
                  cur-step={currentStep.value}
                  steps={steps}
                  onClick={handleStepChange}
                ></Steps>
              </div>
            </div>
          ),
          default: () => (
            <div class={styles.content}>
              {currentStep.value === 1 ? (
                <BaseInfo
                  ref={baseInfoRef}
                  v-model={formData.value.baseInfo}
                  onUpdate:modelValue={updateBaseInfo}
                />
              ) : (
                <SelectTemplate
                  modelValue={formData.value.templateInfo}
                  onUpdate:modelValue={updateTemplateInfo}
                />
              )}
            </div>
          ),
          footer: () => (
            <>
              <Button
                class={styles.btn}
                loading={isLoading.value}
                theme="primary"
                onClick={handleChangeStep}
              >
                {currentStep.value === 1
                  ? t('flow.content.nextStep')
                  : t('flow.content.previousStep')}
              </Button>
              {currentStep.value === 2 ? (
                <Button
                  class={styles.btn}
                  loading={isLoading.value}
                  theme="primary"
                  onClick={handleConfirm}
                >
                  {t('flow.content.createAndStartOrchestrating')}
                </Button>
              ) : null}
              <Button class={styles.btn} loading={isLoading.value} onClick={onClose}>
                {t('flow.common.cancel')}
              </Button>
            </>
          ),
        }}
      </Dialog>
    )
  },
})
