import type { AtomModal } from '@/api/atom'
import type { Container, Element, FlowModel, FlowSettings, Stage } from '@/api/flowModel'
import { useAtomStore } from '@/stores/atom'
import { useFlowModelStore } from '@/stores/flowModel'
import {
  diffAtomVersions,
  getAtomDefaultValue,
  getAtomOutputObj,
  isNewAtomTemplate,
} from '@/utils/atom'
import {
  createDefaultContainer,
  createDefaultElement,
  createDefaultStage,
} from '@/utils/flowDefaults'
import { randomLenString } from '@/utils/util'
import type { AddAtomEventPayload, AddStageEventPayload, ClickEventPayload } from 'bkui-pipeline'
import { storeToRefs } from 'pinia'
import { computed, ref } from 'vue'
import { DEFAULT_VERSION } from './useAtomVersion'
import { useEditingPos } from './useEditingPos'

export function useFlowModel() {
  const store = useFlowModelStore()
  const atomStore = useAtomStore()

  // 使用 storeToRefs 确保响应式
  const {
    flowModel,
    yamlContent,
    loading,
    hasError,
    currentFlowId,
    hasUnsavedChanges,
    isFlowEmpty,
    flowSetting,
    hasValidationError,
    hasOrchestrationError,
    hasSettingsError,
  } = storeToRefs(store)

  // 统一的位置/索引管理
  const {
    realEditingPos,
    setEditingPos,
    clearEditingPos,
    isEditingStage,
    isEditingJob,
    isEditingPlugin,
  } = useEditingPos()

  // 新建状态标记
  const isNewStage = ref(false)
  const isNewJob = ref(false)

  // 临时存储正在编辑的对象（用于新建时，或者作为编辑时的临时副本）
  const tempEditingObject = ref<Stage | Container | Element | null>(null)

  // 计算属性：Flow 数据（去除触发器 Stage 用于展示）
  const flowModelWithoutTriggerStage = computed(() => {
    if (!flowModel.value?.stages || flowModel.value.stages.length === 0) {
      return null
    }
    return {
      ...flowModel.value,
      stages: flowModel.value.stages.slice(1),
    }
  })

  // 计算属性：是否有 Stage（排除 trigger stage）
  const hasFlowStages = computed(() => {
    return flowModel.value?.stages && flowModel.value?.stages.length > 1
  })

  // 计算属性：触发事件列表（从 flowModel 的 trigger stage 中提取）
  const triggerEvents = computed(() => {
    const triggerStage = flowModel.value?.stages?.[0]
    if (!triggerStage) return []

    const container = triggerStage.containers?.[0]
    if (!container) return []

    return container.elements || []
  })

  // 获取当前正在编辑的对象
  const editingStage = computed(() => {
    if (!isEditingStage.value) return null
    if (isNewStage.value) return tempEditingObject.value as Stage
    return flowModel.value?.stages[realEditingPos.value.stageIndex] || null
  })

  const editingContainer = computed(() => {
    if (!isEditingJob.value) return null
    if (isNewJob.value) return tempEditingObject.value as Container
    const { stageIndex, containerIndex } = realEditingPos.value
    if (containerIndex === undefined) return null
    return flowModel.value?.stages[stageIndex]?.containers?.[containerIndex] || null
  })

  // 当前编辑 Job 所属的 Stage
  const editingContainerStage = computed(() => {
    if (!isEditingJob.value) return null
    const { stageIndex } = realEditingPos.value
    return flowModel.value?.stages[stageIndex] || null
  })

  // 当前编辑 Element 所属的 Job
  const editingElementContainer = computed(() => {
    if (!isEditingPlugin.value) return null
    const { stageIndex, containerIndex } = realEditingPos.value
    return flowModel.value?.stages[stageIndex]?.containers?.[containerIndex!]
  })

  // 当前编辑 Job 的索引
  const editingContainerIndex = computed(() => {
    if (!isEditingJob.value) return -1
    return realEditingPos.value.containerIndex ?? -1
  })

  // 当前编辑的 Stage 是否是 Finally Stage
  const isEditingFinallyStage = computed(() => {
    return editingContainerStage.value?.finally === true
  })

  const editingElement = computed(() => {
    if (!isEditingPlugin.value) return null
    const { stageIndex, containerIndex, elementIndex } = realEditingPos.value
    return flowModel.value?.stages[stageIndex]?.containers?.[containerIndex!]?.elements?.[
      elementIndex!
    ]
  })

  /**
   * 触发变更事件
   */
  const emitChange = () => {
    if (flowModel.value) {
      store.updateFlowModel(flowModel.value)
    }
  }

  // ========== 基础操作方法 (CRUD) ==========

  const addStage = (stage?: Stage) => {
    const { stageIndex } = realEditingPos.value
    const newStage = {
      ...(tempEditingObject.value as Stage),
      ...stage,
    }
    if (flowModel.value) {
      let insertIndex = stageIndex

      // 如果是 Finally Stage，确保添加到最后
      if (newStage.finally) {
        insertIndex = flowModel.value.stages.length
      } else if (hasFinallyStage.value) {
        // 如果已经有 Finally Stage，确保普通 Stage 不会插入到 Finally Stage 之后
        // Finally Stage 的位置是 stages.length - 1
        const finallyStageIndex = flowModel.value.stages.length - 1
        if (insertIndex >= finallyStageIndex) {
          insertIndex = finallyStageIndex // 插入到 Finally Stage 之前
        }
      }

      flowModel.value.stages = [
        ...flowModel.value.stages.slice(0, insertIndex),
        newStage,
        ...flowModel.value.stages.slice(insertIndex),
      ]
      emitChange()
    }
    return newStage
  }

  const updateStage = (stage: Stage) => {
    const { stageIndex } = realEditingPos.value
    if (flowModel.value?.stages[stageIndex]) {
      flowModel.value.stages[stageIndex] = stage
      emitChange()
    }
  }

  const deleteStage = () => {
    const { stageIndex } = realEditingPos.value
    if (flowModel.value?.stages[stageIndex]) {
      flowModel.value.stages.splice(stageIndex, 1)
      emitChange()
    }
  }

  const addJob = (container: Partial<Container>) => {
    const { stageIndex } = realEditingPos.value
    const stage = flowModel.value?.stages[stageIndex]
    if (!stage) return

    if (!stage.containers) stage.containers = []
    const newContainer = {
      ...(tempEditingObject.value as Container),
      ...container,
    }
    stage.containers = [...stage.containers, newContainer]
    emitChange()
    return newContainer
  }

  const updateJob = (container: Partial<Container>) => {
    const { stageIndex, containerIndex } = realEditingPos.value
    const stage = flowModel.value?.stages[stageIndex]
    const currentContainer = stage?.containers?.[containerIndex!]
    if (currentContainer) {
      stage.containers[containerIndex!] = {
        ...currentContainer,
        ...container,
      }
      emitChange()
    }
  }

  const deleteJob = () => {
    const { stageIndex, containerIndex } = realEditingPos.value
    if (containerIndex === undefined) return
    const stage = flowModel.value?.stages[stageIndex]
    if (stage && stage.containers) {
      stage.containers.splice(containerIndex, 1)
      emitChange()
    }
  }

  const addPlugin = () => {
    const { stageIndex, containerIndex, elementIndex } = realEditingPos.value
    const stage = flowModel.value?.stages[stageIndex!]
    const container = stage?.containers?.[containerIndex!]
    if (!container) return

    if (!container.elements) container.elements = []
    const newElement = createDefaultElement(elementIndex!)

    container.elements = [
      ...container.elements.slice(0, elementIndex!),
      newElement,
      ...container.elements.slice(elementIndex!),
    ]

    emitChange()
    return newElement
  }

  const updateAtom = (element: Element) => {
    const { stageIndex, containerIndex, elementIndex } = realEditingPos.value
    const container = flowModel.value?.stages[stageIndex]?.containers?.[containerIndex!]
    if (container && container.elements && elementIndex !== undefined) {
      container.elements[elementIndex] = element
      emitChange()
    }
  }

  const deletePlugin = () => {
    const { stageIndex, containerIndex, elementIndex } = realEditingPos.value
    if (containerIndex === undefined || elementIndex === undefined) return
    const container = flowModel.value?.stages[stageIndex!]?.containers?.[containerIndex!]
    if (container && container.elements) {
      container.elements.splice(elementIndex, 1)
      emitChange()
    }
  }

  // ========== 业务逻辑方法 (Event Handlers) ==========

  /**
   * 处理 Flow 点击事件 (Stage, Job, Plugin)
   */
  const handleFlowClick = (payload: ClickEventPayload) => {
    const { stageIndex = -1, containerIndex, elementIndex, atomIndex } = payload
    // 兼容 atomIndex
    const realElementIndex = elementIndex !== undefined ? elementIndex : atomIndex

    if (stageIndex === -1) return
    // 重置新建状态
    isNewStage.value = false
    isNewJob.value = false
    tempEditingObject.value = null
    if (realElementIndex !== undefined && realElementIndex !== -1 && containerIndex !== undefined) {
      // Click Plugin
      setEditingPos({ stageIndex, containerIndex, elementIndex: realElementIndex })
    } else if (containerIndex !== undefined && containerIndex !== -1) {
      // Click Job
      setEditingPos({ stageIndex, containerIndex })
    } else {
      // Click Stage
      setEditingPos({ stageIndex })
    }
  }

  // 计算属性：是否已有 Finally Stage
  const hasFinallyStage = computed(() => {
    if (!flowModel.value?.stages || flowModel.value.stages.length <= 1) {
      return false
    }
    // 最后一个 Stage（排除 trigger stage）是否是 finally stage
    const lastStage = flowModel.value.stages[flowModel.value.stages.length - 1]
    return lastStage?.finally === true
  })

  /**
   * 处理添加 Stage (打开面板)
   * @param payload.isFinally - 是否是 Finally Stage
   */
  const handleAddStage = ({
    stageIndex,
    isParallel,
    isFinally,
  }: AddStageEventPayload & { isFinally?: boolean }) => {
    if (isParallel) {
      handleAddJob({ stageIndex })
      return
    }

    // 如果已经有 Finally Stage，不允许再添加 Finally Stage
    if (hasFinallyStage.value && isFinally) {
      console.warn('Already has a Finally Stage, cannot add another one')
      return
    }

    const newStage = createDefaultStage(stageIndex, {
      name: isFinally ? 'Final' : `Stage-${stageIndex + 1}`,
      finally: isFinally || false,
      containers: [
        createDefaultContainer(1, {
          name: 'Job1',
          dispatchType: {
            buildType: 'CREATE_AGENT_ENV',
            value: '${{variables.BK_CI_NODE_AGENT_ID}}',
          },
        }),
      ],
    })
    tempEditingObject.value = newStage
    isNewStage.value = true

    setEditingPos({ stageIndex })
  }

  const handleAddFirstStage = () => {
    handleAddStage({ stageIndex: 0 })
  }

  /**
   * 处理 Stage 变更 (属性面板)
   */
  const handleStageChange = (stage: Stage) => {
    // 只在非新建状态下更新模型
    if (!isNewStage.value) {
      updateStage(stage)
    }
    // 更新编辑状态 (如果是新建，更新 temp 对象)
    if (isNewStage.value) {
      tempEditingObject.value = stage
    }
  }

  /**
   * 确认添加/修改 Stage
   */
  const handleStageConfirm = (stage: Stage) => {
    // 使用传入的 stage 进行保存
    if (isNewStage.value) {
      addStage(stage)
    } else {
      updateStage(stage)
    }
    isNewStage.value = false
    tempEditingObject.value = null
    clearEditingPos()
  }

  /**
   * 处理添加 Job (打开面板)
   * @param payload.jobType - Job 类型: 'create' (创作任务) 或 'cloud' (云任务)
   */
  const handleAddJob = (payload: AddStageEventPayload & { jobType?: string }) => {
    const { stageIndex, jobType = 'create' } = payload
    const stage = flowModel.value?.stages[stageIndex + 1]
    if (!stage) return
    const containerIndex = stage.containers?.length || 0

    // 根据 jobType 设置 @type 和 classType
    const containerType = jobType === 'cloud' ? 'normal' : 'vmBuild'
    const newContainer = createDefaultContainer(containerIndex, {
      '@type': containerType,
      classType: containerType,
      ...(containerType === 'vmBuild'
        ? {
            dispatchType: {
              buildType: 'CREATE_AGENT_ENV',
              value: '${{variables.BK_CI_NODE_AGENT_ID}}',
            },
          }
        : {}),
    })
    console.log('handleAddJob', stageIndex, containerIndex, newContainer, stage.containers.length)
    setEditingPos({ stageIndex, containerIndex: stage.containers.length })
    tempEditingObject.value = newContainer
    isNewJob.value = true
  }

  /**
   * 确认添加/修改 Job
   */
  const handleJobConfirm = (container: Partial<Container>) => {
    if (isNewJob.value) {
      addJob(container)
    } else {
      updateJob(container)
    }
    isNewJob.value = false
    tempEditingObject.value = null
    clearEditingPos()
  }

  /**
   * 处理添加 Plugin (直接添加占位符并打开面板)
   */
  const handleAddAtom = (payload: AddAtomEventPayload) => {
    const { stageIndex, containerIndex, atomIndex } = payload
    const insertIndex = (atomIndex ?? 0) + 1
    setEditingPos({ stageIndex, containerIndex, elementIndex: insertIndex })
    addPlugin()
  }

  /**
   * 插件选择完成
   */
  const handleAtomSelect = ({
    atomCode,
    version,
    atomModal,
  }: {
    atomCode: string
    version?: string
    atomModal?: AtomModal
  }) => {
    const { stageIndex, containerIndex, elementIndex } = realEditingPos.value
    if (
      !atomModal ||
      stageIndex === -1 ||
      containerIndex === undefined ||
      elementIndex === undefined
    )
      return

    const container = flowModel.value?.stages[stageIndex]?.containers?.[containerIndex]
    const preVerEle = container?.elements?.[elementIndex!]

    const isChangeAtom = !preVerEle || preVerEle.atomCode !== atomCode
    const finalVersion = version || DEFAULT_VERSION
    const htmlTemplateVersion = atomModal.htmlTemplateVersion
    const isNewTemplate = isNewAtomTemplate(htmlTemplateVersion)
    const atomProps = atomModal.props || {}

    let element: Element

    if (isNewTemplate) {
      const preVerData = (preVerEle?.data as any) || {}
      const preVerModelProps: Record<string, any> = {}
      const atomInputProps = (atomProps.input as Record<string, any>) || {}

      const diffRes = diffAtomVersions(
        (preVerData.input as Record<string, any>) || {},
        (preVerModelProps.input as Record<string, any>) || {},
        atomInputProps,
        isChangeAtom,
      )

      const mergedInput = {
        ...getAtomDefaultValue(atomInputProps),
        ...diffRes.atomValue,
      }

      const outputObj = getAtomOutputObj(atomProps.output || {})

      element = createDefaultElement(elementIndex, {
        id: preVerEle?.id || `element-${randomLenString(4)}`,
        '@type':
          atomModal.classType && atomModal.classType !== atomCode ? atomModal.classType : atomCode,
        atomCode,
        name: isChangeAtom ? atomModal.name : preVerEle.name,
        version: finalVersion,
        classType: atomModal.classType || atomCode,
        data: {
          input: mergedInput,
          output: outputObj,
          namespace: isChangeAtom ? '' : preVerData.namespace || '',
          config: atomProps.config || {},
        } as any,
      })
    } else {
      const preVerModelProps = {}
      const diffRes = diffAtomVersions(
        (preVerEle as Record<string, any>) || {},
        preVerModelProps,
        atomProps,
        isChangeAtom,
      )
      const mergedProps = {
        ...getAtomDefaultValue(atomProps),
        ...diffRes.atomValue,
      }
      element = createDefaultElement(elementIndex, {
        id: preVerEle?.id || `element-${randomLenString(4)}`,
        '@type':
          atomModal.classType && atomModal.classType !== atomCode ? atomModal.classType : atomCode,
        atomCode,
        version: finalVersion,
        name: isChangeAtom ? atomModal.name : preVerEle.name,
        ...mergedProps,
      })
    }

    if (atomModal.logoUrl) (element as any).logoUrl = atomModal.logoUrl

    atomStore.setAtomModal(atomCode, finalVersion, atomModal)

    updateAtom(element)
    // clearEditingPos()
  }

  const saveFlow = async (params: Parameters<typeof store.saveFlow>[0]) => {
    return await store.saveFlow(params)
  }

  /**
   * Load flow model (with optional force reload)
   */
  const loadFlow = async (
    loadProjectId: string,
    loadFlowId: string,
    loadVersion?: string,
    forceReload = false,
  ) => {
    return await store.loadFlowModel(loadProjectId, loadFlowId, loadVersion, forceReload)
  }

  const updateFlowModel = (model: FlowModel) => {
    store.updateFlowModel(model)
  }

  const updateFlowSetting = (setting: FlowSettings) => {
    store.updateFlowSetting(setting)
  }

  const updateYaml = (yaml: string) => store.updateYamlContent(yaml)
  const reset = () => store.reset()

  return {
    // State
    flowModel,
    flowModelWithoutTriggerStage,
    hasFlowStages,
    hasFinallyStage,
    triggerEvents,
    yamlContent,
    loading,
    hasError,
    currentFlowId,
    hasUnsavedChanges,
    hasValidationError,
    hasOrchestrationError,
    hasSettingsError,
    isFlowEmpty,
    flowSetting,
    // Editing State
    realEditingPos,
    isEditingStage,
    isEditingJob,
    isEditingPlugin,
    isNewStage,
    isNewJob,

    editingStage,
    editingContainer,
    editingContainerStage,
    editingContainerIndex,
    isEditingFinallyStage,
    editingElementContainer,
    editingElement, // Plugin context

    // Actions
    saveFlow,
    loadFlow,
    updateFlowModel,
    updateFlowSetting,
    updateYaml,
    reset,

    // CRUD
    addStage,
    updateStage,
    deleteStage,
    addJob,
    updateJob,
    deleteJob,
    addPlugin,
    updateAtom,
    deletePlugin,

    // Handlers
    handleFlowClick,
    handleAddStage,
    handleAddFirstStage,
    handleStageChange,
    handleStageConfirm,
    handleAddJob,
    handleJobConfirm,
    handleAddAtom,
    handleAtomSelect,
    // Close handler (simply clear pos)
    handleClosePanel: clearEditingPos,

    /**
     * 处理 BkPipeline 组件的变更事件（用于复制、拖拽等操作）
     * bk-pipeline 会发出不同类型的对象：
     * - Stage 复制/删除：发出完整的 pipeline 对象 { stages: [...] }
     * - Job 复制/删除：发出 stage 对象 { containers: [...], id: ... }
     * - Atom 复制/删除：发出 container 对象 { elements: [...], containerId: ... }
     * @param changedObject - bk-pipeline 发出的变更对象
     */

    handlePipelineChange: (changedObject: any) => {
      if (!flowModel.value?.stages) return

      const triggerStage = flowModel.value.stages[0]
      const currentStages = flowModel.value.stages

      // 判断变更对象的类型并相应处理
      if (changedObject?.stages && Array.isArray(changedObject.stages)) {
        // 收到的是 pipeline 对象（Stage 复制/删除/拖拽）
        // 直接替换整个 stages 数组以触发响应式更新
        flowModel.value.stages = [triggerStage, ...changedObject.stages] as Stage[]
      } else if (changedObject?.containers && Array.isArray(changedObject.containers)) {
        // 收到的是 stage 对象（Job 复制/删除）
        // 找到对应的 stage 并替换（+1 是因为 index 0 是 trigger stage）
        const stageId = changedObject.id
        const stageIndex = currentStages.findIndex((s) => s.id === stageId)
        if (stageIndex !== -1) {
          // 使用 splice 替换以触发响应式更新
          currentStages.splice(stageIndex, 1, changedObject as Stage)
        }
      } else if (changedObject?.elements && Array.isArray(changedObject.elements)) {
        // 收到的是 container 对象（Atom 复制/删除）
        // 找到对应的 container 并替换
        const containerId = changedObject.containerId
        for (const stage of currentStages) {
          if (!stage?.containers) continue
          const cIdx = stage.containers.findIndex((c) => c.containerId === containerId)
          if (cIdx !== -1) {
            // 使用 splice 替换以触发响应式更新
            stage.containers.splice(cIdx, 1, changedObject as Container)
            break
          }
        }
      }

      emitChange()
    },
  }
}
