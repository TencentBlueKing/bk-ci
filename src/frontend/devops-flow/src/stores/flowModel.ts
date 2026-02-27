import {
  flowModelToYaml,
  getFlowModel,
  saveFlowModel,
  yamlToFlowModel,
  type FlowModel,
  type FlowSettings,
  type SaveFlowModelParams,
} from '@/api/flowModel'
import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { useModeStore } from '@/stores/flowMode'
import { UI_MODE } from '@/utils/flowConst'
import { validateFlowSettings } from '@/utils/validation'

/**
 * 创作流模型状态管理
 */
export const useFlowModelStore = defineStore('flowModel', () => {
  const modeStore = useModeStore()
  // Flow 模型数据
  const flowModel = ref<FlowModel | null>(null)
  const flowSetting = ref<FlowSettings | null>(null)

  // YAML 格式的代码内容
  const yamlContent = ref<string>('')

  // 加载状态
  const loading = ref(false)

  // 错误状态
  const hasError = ref(false)

  // 当前创作流 ID
  const currentFlowId = ref<string>('')

  // 当前版本号
  const currentVersion = ref<string>('')

  // 是否有未保存的更改
  const hasUnsavedChanges = ref(false)

  /**
   * 计算属性：Flow 是否为空
   */
  const isFlowEmpty = computed(() => {
    return !flowModel.value || flowModel.value.stages.length === 0
  })

  /**
   * 计算属性：设置中存在校验错误的字段列表
   */
  const settingsErrorFields = computed(() => {
    return validateFlowSettings(flowSetting.value)
  })

  /**
   * 计算属性：编排模型中是否存在校验错误（isError 标记）
   */
  const hasOrchestrationError = computed(() => {
    if (!flowModel.value?.stages) return false
    return flowModel.value.stages.some((stage: any) => {
      if (stage.isError) return true
      return stage.containers?.some((container: any) => {
        if (container.isError) return true
        return container.elements?.some((element: any) => element.isError)
      })
    })
  })

  /**
   * 计算属性：设置中是否存在校验错误
   */
  const hasSettingsError = computed(() => {
    return settingsErrorFields.value.length > 0
  })

  /**
   * 计算属性：编排或设置中是否存在任一校验错误
   */
  const hasValidationError = computed(() => {
    return hasOrchestrationError.value || hasSettingsError.value
  })

  /**
   * 加载 Flow 模型数据
   * @param projectId 项目 ID
   * @param flowId 创作流 ID
   * @param version 版本号（可选）
   * @param forceReload 是否强制重新加载（默认 false）
   */
  async function loadFlowModel(
    projectId: string,
    flowId: string,
    version?: string,
    forceReload = false,
  ) {
    // Skip if already loading
    if (loading.value) return

    const versionStr = version || ''

    // Skip if data already exists for this flowId + version and not forcing reload
    // This prevents tab switching from overwriting edited data
    const isSameFlow = currentFlowId.value === flowId && currentVersion.value === versionStr
    if (!forceReload && isSameFlow && flowModel.value !== null) {
      return
    }

    loading.value = true
    hasError.value = false
    currentFlowId.value = flowId
    currentVersion.value = versionStr
    try {
      const model = await getFlowModel(projectId, flowId, version)
      flowModel.value = model.modelAndSetting?.model
      flowSetting.value = model.modelAndSetting?.setting
      yamlContent.value = model.yamlPreview?.yaml || ''
      if (!model.yamlSupported) {
        modeStore.setMode(UI_MODE)
      }
      hasUnsavedChanges.value = false
    } catch (error) {
      console.error('Failed to load flow model:', error)
      hasError.value = true
      throw error
    } finally {
      loading.value = false
    }
  }

  /**
   * 更新 Flow 模型数据
   * @param model 新的 Flow 模型
   */
  function updateFlowModel(model: FlowModel) {
    flowModel.value = model
    yamlContent.value = flowModelToYaml(model)
    hasUnsavedChanges.value = true
  }

  function updateFlowSetting(setting: FlowSettings) {
    flowSetting.value = setting
    hasUnsavedChanges.value = true
  }

  /**
   * 更新 YAML 内容
   * @param yaml YAML 字符串
   */
  function updateYamlContent(yaml: string) {
    yamlContent.value = yaml
    hasUnsavedChanges.value = true

    try {
      const model = yamlToFlowModel(yaml)
      flowModel.value = model
      hasError.value = false
    } catch (error) {
      console.error('Failed to parse YAML:', error)
      hasError.value = true
    }
  }

  /**
   * 保存 Flow 模型
   * @param params 保存参数（包含projectId等）
   */
  async function saveFlow(params: SaveFlowModelParams) {
    if (!flowModel.value) {
      throw new Error('No flow model')
    }

    loading.value = true

    try {
      const modelCopy = JSON.parse(JSON.stringify(flowModel.value))

      modelCopy.stages?.forEach((stage: any) => {
        delete stage.isError
        stage.containers?.forEach((container: any) => {
          delete container.isError
          if (container.baseOS) {
            delete container.baseOS
          }
          container.elements?.forEach((element: any) => {
            delete element.isError
          })
        })
      })

      const saveParams: SaveFlowModelParams = {
        ...params,
        modelAndSetting: {
          model: modelCopy,
          setting: flowSetting.value!,
        },
        storageType: params.storageType || 'MODEL',
      }

      const response = await saveFlowModel(saveParams)
      hasUnsavedChanges.value = false
      return response
    } catch (error) {
      console.error('Failed to save flow model:', error)
      throw error
    } finally {
      loading.value = false
    }
  }

  /**
   * 重置状态
   */
  function reset() {
    flowModel.value = null
    yamlContent.value = ''
    flowSetting.value = null
    loading.value = false
    hasError.value = false
    currentFlowId.value = ''
    currentVersion.value = ''
    hasUnsavedChanges.value = false
  }

  /**
   * 设置错误状态
   * @param error 是否有错误
   */
  function setHasError(error: boolean) {
    hasError.value = error
  }

  return {
    // 状态
    flowModel,
    flowSetting,
    yamlContent,
    loading,
    hasError,
    currentFlowId,
    currentVersion,
    hasUnsavedChanges,

    // 计算属性
    isFlowEmpty,
    hasValidationError,
    hasOrchestrationError,
    hasSettingsError,
    settingsErrorFields,

    // 方法
    loadFlowModel,
    updateFlowModel,
    updateFlowSetting,
    updateYamlContent,
    saveFlow,
    reset,
    setHasError,
  }
})
