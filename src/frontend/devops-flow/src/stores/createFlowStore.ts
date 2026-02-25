import {
  apiGetProjectTemplates,
  apiGetStoreTemplates,
  createContent,
  type CreateContentFormData,
  type CreateContentParams,
  type GetStoreTemplatesParams,
  type StoreTemplateItem,
  type TemplateObject,
} from '@/api/flowContentList'
import { useFlowModel } from '@/hooks/useFlowModel'
import { templateTypeEnum } from '@/utils/flowConst'
import { defineStore } from 'pinia'
import { ref } from 'vue'
import { useRoute } from 'vue-router'

import { Message } from 'bkui-vue'

/**
 * 创作流创建流程状态管理
 *
 * Use useAuthoringEnvironment hook in components that need environment management.
 */
export const useNewFlowStore = defineStore('newFlow', () => {
  const route = useRoute()

  const { updateFlowModel, updateFlowSetting } = useFlowModel()
  // ============ Core State ============

  /**
   * Current step in the creation flow
   */
  const currentStep = ref(1)

  /**
   * Form data for creating a new flow
   */
  const formData = ref<CreateContentFormData>({
    baseInfo: {
      pipelineName: '',
      pipelineDesc: '',
      envHashId: '',
    },
    templateInfo: {
      activeTemplate: { name: '', logoUrl: '', desc: '' },
      currentModel: templateTypeEnum.FREEDOM,
      cloneTemplateSet: [],
      activeMenuItem: 'flowModel',
    },
  })

  /**
   * Loading state for creating flow
   */
  const isLoading = ref(false)

  // ============ Template State ============

  /**
   * Project template list
   */
  const projectModelList = ref<TemplateObject[]>([])

  /**
   * Store template list
   */
  const storeModelList = ref<StoreTemplateItem[]>([])

  /**
   * Loading state for project templates
   */
  const projectModelLoading = ref(false)

  /**
   * Loading state for store templates
   */
  const storeModelLoading = ref(false)

  // ============ Template Actions ============

  /**
   * Fetch project template list
   */
  async function fetchProjectTemplates(projectId: string) {
    try {
      projectModelLoading.value = true
      const response = await apiGetProjectTemplates(projectId)

      if (response?.templates) {
        projectModelList.value = Object.values(response.templates).map((template: any) => ({
          ...template,
          id: template.templateId,
          name: template.name,
          logoUrl: template.logoUrl,
          desc: template.desc,
          templateType: template.templateType,
        }))

        // Set first template as active if available
        const firstTemplate = projectModelList.value[0]
        if (firstTemplate) {
          formData.value.templateInfo.activeTemplate = firstTemplate
          const flowModel: any = {
            name: firstTemplate.name || '',
            desc: firstTemplate.desc || '',
            stages: firstTemplate.stages || [],
            labels: [],
          }
          updateFlowModel(flowModel)
          updateFlowSetting({
            envHashId: formData.value.baseInfo.envHashId,
            pipelineName: formData.value.baseInfo.pipelineName,
            desc: formData.value.baseInfo.pipelineDesc || '',
          })
        }
      } else {
        projectModelList.value = []
      }
    } catch (error) {
      console.error('Failed to fetch project templates:', error)
      projectModelList.value = []
    } finally {
      projectModelLoading.value = false
    }
  }

  /**
   * Fetch store template list
   */
  async function fetchStoreTemplates() {
    try {
      storeModelLoading.value = true
      const param: GetStoreTemplatesParams = {
        page: 1,
        pageSize: 50,
        projectCode: route.params.projectId as string,
        keyword: '',
      }
      const res = await apiGetStoreTemplates(param)

      if (res) {
        storeModelList.value = res.records
      }
    } catch (error: any) {
      Message({ theme: 'error', message: error.message || error })
      storeModelList.value = []
    } finally {
      storeModelLoading.value = false
    }
  }

  // ============ Form Actions ============

  /**
   * Initialize form data with default values
   */
  function initFormData(): CreateContentFormData {
    return {
      baseInfo: {
        pipelineName: '',
        pipelineDesc: '',
        envHashId: '',
      },
      templateInfo: {
        activeTemplate: { name: '', logoUrl: '', desc: '' },
        currentModel: templateTypeEnum.FREEDOM,
        cloneTemplateSet: [],
        activeMenuItem: 'flowModel',
      },
    }
  }

  /**
   * Reset form state to initial values
   */
  function resetForm() {
    formData.value = initFormData()
    currentStep.value = 1
    isLoading.value = false
    projectModelList.value = []
    storeModelList.value = []
  }

  /**
   * Update base info section of form
   */
  function updateBaseInfo(data: { pipelineName: string; pipelineDesc: string; envHashId: string }) {
    formData.value.baseInfo = { ...formData.value.baseInfo, ...data }
  }

  /**
   * Update template info section of form
   */
  function updateTemplateInfo(data: any) {
    formData.value.templateInfo = { ...formData.value.templateInfo, ...data }
  }

  // ============ Flow Creation ============

  /**
   * Create a new flow
   */
  async function createNewFlow(params: CreateContentParams) {
    isLoading.value = true
    try {
      const result = await createContent(params)
      return result
    } catch (error) {
      console.error('Failed to create new flow:', error)
      throw error
    } finally {
      isLoading.value = false
    }
  }

  return {
    // Core State
    currentStep,
    formData,
    isLoading,

    // Template State
    projectModelList,
    storeModelList,
    projectModelLoading,
    storeModelLoading,

    // Template Actions
    fetchProjectTemplates,
    fetchStoreTemplates,

    // Form Actions
    resetForm,
    updateBaseInfo,
    updateTemplateInfo,

    // Flow Creation
    createNewFlow,
  }
})
