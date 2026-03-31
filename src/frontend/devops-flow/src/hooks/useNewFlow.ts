import { type CreateContentParams } from '@/api/flowContentList'
import { templateTypeEnum } from '@/utils/flowConst'
import { storeToRefs } from 'pinia'
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'

import { ROUTE_NAMES } from '@/constants/routes'
import { useNewFlowStore } from '@/stores/createFlowStore'
import { Message } from 'bkui-vue'
import { useRoute, useRouter } from 'vue-router'

/**
 * NewFlowPopup component business logic hook
 *
 * Note: Authoring environment state is managed separately through useAuthoringEnvironment hook.
 * Components that need environment management should use that hook directly.
 */
export function useNewFlow() {
  const { t } = useI18n()
  const router = useRouter()
  const route = useRoute()
  const store = useNewFlowStore()

  // Store state refs
  const {
    currentStep,
    formData,
    isLoading,
    projectModelList,
    storeModelList,
    projectModelLoading,
    storeModelLoading,
  } = storeToRefs(store)

  // Local state
  const baseInfoRef = ref<any>()

  /**
   * Clear form validation errors
   */
  function clearFormValidation() {
    baseInfoRef.value?.formRef?.clearValidate?.()
  }

  /**
   * Validate current step
   */
  async function validateCurrentStep(): Promise<boolean> {
    if (currentStep.value === 1) {
      try {
        await baseInfoRef.value?.formRef?.validate?.()
        return true
      } catch (error) {
        console.error('Form validation failed:', error)
        return false
      }
    }
    return true
  }

  /**
   * Handle step change
   */
  async function handleStepChange(targetStep: number): Promise<void> {
    if (targetStep === currentStep.value) return

    const isValid = await validateCurrentStep()
    if (!isValid) return

    currentStep.value = targetStep
  }

  /**
   * Go to next step
   */
  async function handleNextStep(): Promise<void> {
    const isValid = await validateCurrentStep()
    if (!isValid) return

    currentStep.value++
  }

  /**
   * Go to previous step
   */
  function handlePrevStep(): void {
    currentStep.value--
  }

  /**
   * Confirm and create the flow
   */
  async function handleConfirm(): Promise<void> {
    const isValid = await validateCurrentStep()
    if (!isValid) return

    try {
      const params: CreateContentParams = {
        projectId: route.params.projectId as string,
        ...formData.value.baseInfo,
        templateId: formData.value.templateInfo.activeTemplate.templateId!,
        templateVersion: formData.value.templateInfo.activeTemplate.version!,
        ...formData.value.templateInfo.cloneTemplateSet.reduce(
          (result, item) => {
            result[item] = true
            return result
          },
          {} as Record<string, boolean>,
        ),
        instanceType: formData.value.templateInfo.currentModel,
        emptyTemplate:
          formData.value.templateInfo.activeTemplate.templateType === templateTypeEnum.PUBLIC,
      }

      const res = await store.createNewFlow(params)
      if (res) {
        router.push({
          name: ROUTE_NAMES.FLOW_EDIT_WORKFLOW_ORCHESTRATION,
          params: { ...route.params, flowId: res.pipelineId, version: res.version },
        })
        store.resetForm()
      }
    } catch (error: any) {
      Message({
        message: error.message || error || t('flow.content.createFailed'),
        theme: 'error',
      })
    }
  }

  return {
    // Store state
    currentStep,
    formData,
    isLoading,
    projectModelList,
    storeModelList,
    projectModelLoading,
    storeModelLoading,

    // Local state
    baseInfoRef,

    // Step navigation methods
    clearFormValidation,
    validateCurrentStep,
    handleStepChange,
    handleNextStep,
    handlePrevStep,
    handleConfirm,

    // Store actions
    resetForm: store.resetForm,
    updateBaseInfo: store.updateBaseInfo,
    updateTemplateInfo: store.updateTemplateInfo,
    fetchProjectTemplates: store.fetchProjectTemplates,
    fetchStoreTemplates: store.fetchStoreTemplates,
  }
}
