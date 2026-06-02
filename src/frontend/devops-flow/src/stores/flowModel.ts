import {
  getFlowModel,
  saveFlowModel,
  type FlowModel,
  type FlowSettings,
  type SaveFlowModelParams,
  type YamlHighlightBlockMap,
} from '@/api/flowModel'
import { getPluginProperties, type PluginProperty } from '@/api/flowContentList'
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

  // 后端返回的 YAML 区域高亮标记（pipeline/trigger/notice/setting）
  // 用于在 Code 模式下定位不同配置区域
  const yamlHighlightBlockMap = ref<YamlHighlightBlockMap>({})

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

  // 编辑脏标记：用于判断切换模式时是否需要调用后端 transfer API
  // - modelDirty: 用户在 UI 模式下修改了 flowModel/flowSetting，YAML 已过期
  // - yamlDirty: 用户在 Code 模式下修改了 YAML，flowModel 已过期
  // 两边同时为 false 表示数据已同步（无需请求后端转换）
  const modelDirty = ref(false)
  const yamlDirty = ref(false)

  // 导入模式：数据来自文件导入，尚未持久化
  const isImportMode = ref(false)

  const IMPORT_STORAGE_KEY = 'flow_import_data'

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
   * 将 atomProp 中的运行时属性（os、logoUrl、buildLessRunFlag）
   * 合并到 model 各 element 上，供前端展示使用。
   * 参考 devops-pipeline dealPipelineRes 实现。
   */
  function mergeAtomProps(model: FlowModel, atomProp: PluginProperty) {
    model.stages?.forEach((stage: any) => {
      stage.containers?.forEach((container: any) => {
        container.elements?.forEach((element: any) => {
          if (element.atomCode && atomProp[element.atomCode]) {
            Object.assign(element, atomProp[element.atomCode])
          }
        })
      })
    })
  }

  /** atomProp 运行时字段，不属于 model 持久化数据 */
  const ATOM_PROP_RUNTIME_KEYS = ['logoUrl', 'os', 'buildLessRunFlag']

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
    if (isImportMode.value) return

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
      const [modelRes, atomProp] = await Promise.all([
        getFlowModel(projectId, flowId, version),
        getPluginProperties({ projectId, pipelineId: flowId, version: version ? Number(version) : undefined }).catch(() => null),
      ])      
      const model = modelRes.modelAndSetting?.model
      if (model && atomProp) {
        mergeAtomProps(model, atomProp)
      }

      flowModel.value = model
      flowSetting.value = modelRes.modelAndSetting?.setting
      const { yaml: previewYaml, ...highlightMap } = modelRes.yamlPreview ?? { yaml: '' }
      yamlContent.value = previewYaml || ''
      yamlHighlightBlockMap.value = highlightMap as YamlHighlightBlockMap
      if (!modelRes.yamlSupported) {
        modeStore.setMode(UI_MODE)
      }
      hasUnsavedChanges.value = false
      modelDirty.value = false
      yamlDirty.value = false
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
   * 注意：仅更新 model，不会同步生成 YAML（前端无能力将 model 转 YAML）。
   * YAML 与 model 的同步通过切换模式时调用后端 transfer API 完成。
   * @param model 新的 Flow 模型
   */
  function updateFlowModel(model: FlowModel) {
    flowModel.value = model
    hasUnsavedChanges.value = true
    modelDirty.value = true
    yamlHighlightBlockMap.value = {}
    if (isImportMode.value) persistImportData()
  }

  function updateFlowSetting(setting: FlowSettings) {
    flowSetting.value = setting
    hasUnsavedChanges.value = true
    modelDirty.value = true
    yamlHighlightBlockMap.value = {}
    if (isImportMode.value) persistImportData()
  }

  /**
   * 更新 YAML 内容
   * 注意：仅更新 YAML，不会同步生成 model（前端无能力将 YAML 转 model）。
   * YAML 与 model 的同步通过切换模式时调用后端 transfer API 完成。
   * @param yaml YAML 字符串
   */
  function updateYamlContent(yaml: string) {
    yamlContent.value = yaml
    hasUnsavedChanges.value = true
    yamlDirty.value = true
    yamlHighlightBlockMap.value = {}
  }

  /**
   * 切换模式时由后端 transfer API 返回结果同步两侧数据。
   * 调用后两侧均为干净状态。
   */
  function applyTransferResult(payload: {
    yaml?: string
    model?: FlowModel
    setting?: FlowSettings
  }) {
    if (payload.yaml !== undefined) {
      yamlContent.value = payload.yaml
    }
    if (payload.model) {
      flowModel.value = payload.model
    }
    if (payload.setting) {
      flowSetting.value = payload.setting
    }
    modelDirty.value = false
    yamlDirty.value = false
    yamlHighlightBlockMap.value = {}
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
            ATOM_PROP_RUNTIME_KEYS.forEach((key) => delete element[key])
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
        yaml: params.yaml ?? yamlContent.value,
      }

      const response = await saveFlowModel(saveParams)
      hasUnsavedChanges.value = false
      modelDirty.value = false
      yamlDirty.value = false
      return response
    } catch (error) {
      console.error('Failed to save flow model:', error)
      throw error
    } finally {
      loading.value = false
    }
  }

  function persistImportData() {
    try {
      sessionStorage.setItem(IMPORT_STORAGE_KEY, JSON.stringify({
        model: flowModel.value,
        setting: flowSetting.value,
        yaml: yamlContent.value,
      }))
    } catch (e) {
      console.error('Failed to persist import data:', e)
    }
  }

  function clearImportData() {
    sessionStorage.removeItem(IMPORT_STORAGE_KEY)
  }

  /**
   * 从 sessionStorage 恢复导入数据（用于页面刷新后恢复）
   * @returns 是否成功恢复
   */
  function restoreImportedFlowModel(): boolean {
    try {
      const raw = sessionStorage.getItem(IMPORT_STORAGE_KEY)
      if (!raw) return false
      const { model, setting, yaml } = JSON.parse(raw)
      if (!model) return false
      flowModel.value = model
      flowSetting.value = setting
      yamlContent.value = yaml || ''
      yamlHighlightBlockMap.value = {}
      isImportMode.value = true
      hasUnsavedChanges.value = true
      modelDirty.value = false
      yamlDirty.value = false
      currentFlowId.value = ''
      currentVersion.value = ''
      hasError.value = false
      return true
    } catch (e) {
      console.error('Failed to restore import data:', e)
      clearImportData()
      return false
    }
  }

  /**
   * 设置导入的 Flow 模型数据（不通过 API 加载，直接设置到 store）
   * 用于导入场景：文件解析后填充到编辑器，用户编辑后保存才创建创作流
   */
  function setImportedFlowModel(
    model: FlowModel,
    setting: FlowSettings,
    yaml?: string,
  ) {
    flowModel.value = model
    flowSetting.value = setting
    yamlContent.value = yaml || ''
    yamlHighlightBlockMap.value = {}
    isImportMode.value = true
    hasUnsavedChanges.value = true
    modelDirty.value = false
    yamlDirty.value = false
    currentFlowId.value = ''
    currentVersion.value = ''
    hasError.value = false
    persistImportData()
  }

  /**
   * 重置状态
   */
  function reset() {
    flowModel.value = null
    yamlContent.value = ''
    yamlHighlightBlockMap.value = {}
    flowSetting.value = null
    loading.value = false
    hasError.value = false
    currentFlowId.value = ''
    currentVersion.value = ''
    hasUnsavedChanges.value = false
    modelDirty.value = false
    yamlDirty.value = false
    isImportMode.value = false
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
    yamlHighlightBlockMap,
    loading,
    hasError,
    currentFlowId,
    currentVersion,
    hasUnsavedChanges,
    modelDirty,
    yamlDirty,
    isImportMode,

    // 计算属性
    isFlowEmpty,
    hasValidationError,
    hasOrchestrationError,
    hasSettingsError,
    settingsErrorFields,

    // 方法
    loadFlowModel,
    setImportedFlowModel,
    restoreImportedFlowModel,
    clearImportData,
    updateFlowModel,
    updateFlowSetting,
    updateYamlContent,
    applyTransferResult,
    saveFlow,
    reset,
    setHasError,
  }
})
