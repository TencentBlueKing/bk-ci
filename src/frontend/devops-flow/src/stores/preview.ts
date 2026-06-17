import { fetchAuthoringNodeList } from '@/api/authoringEnvironmentApi'
import { fetchFlowInfo } from '@/api/flowInfo'
import { getFlowModel, type FlowModelAndSetting } from '@/api/flowModel'
import {
  requestExecPipeline,
  requestStartupInfo,
  type AuthoringNodeItem,
  type StartupInfo,
  type StartupProperty,
} from '@/api/preview'
import type { Container, Element, FlowInfo, FlowModel, Stage } from '@/types/flow'
import { allVersionKeyList } from '@/utils/flowConst'
import { defineStore } from 'pinia'
import { computed, ref, shallowRef } from 'vue'

// ============================================
// Type Definitions
// ============================================

/** Atomic state wrapper with loading and error */
interface AtomicState<T> {
  value: T
  loading: boolean
  error: Error | null
}

/** Processed startup property with additional UI fields */
interface ProcessedProperty extends StartupProperty {
  isChanged?: boolean
  readOnly?: boolean
  label?: string
}

/** Build number configuration */
interface BuildNoConfig {
  required?: boolean
  [key: string]: unknown
}

/** Params value type - key-value pairs */
type ParamsRecord = Record<string, unknown>

/** Skip atoms record type */
type SkipAtomsRecord = Record<string, boolean>

/** Param category type */
type ParamCategory = 'params' | 'versionParam' | 'build' | 'constant' | 'other'

// ============================================
// Utility Functions
// ============================================

const isShallowEqual = (a: unknown, b: unknown): boolean => {
  if (a === b) return true
  if (typeof a !== typeof b) return false
  if (typeof a !== 'object' || a === null || b === null) return false

  const objA = a as Record<string, unknown>
  const objB = b as Record<string, unknown>
  const keysA = Object.keys(objA)
  const keysB = Object.keys(objB)

  if (keysA.length !== keysB.length) return false
  return keysA.every((key) => objA[key] === objB[key])
}

const isPlainObject = (value: unknown): value is Record<string, unknown> => {
  return typeof value === 'object' && value !== null && !Array.isArray(value)
}

const createParamsValuesMap = (
  paramList: StartupProperty[],
  key: 'value' | 'defaultValue' = 'value',
  existingValues?: ParamsRecord,
): ParamsRecord => {
  return paramList.reduce<ParamsRecord>(
    (acc, param) => ({
      ...acc,
      [param.id]: existingValues?.[param.id] ?? param[key],
    }),
    {},
  )
}

// ============================================
// Store Definition
// ============================================

export const usePreviewStore = defineStore('preview', () => {
  // ----------------------------------------
  // Atomic Data Layer
  // ----------------------------------------

  const atomicStartupInfo = shallowRef<AtomicState<StartupInfo | null>>({
    value: null,
    loading: false,
    error: null,
  })

  const atomicPipelineModel = shallowRef<AtomicState<FlowModelAndSetting | null>>({
    value: null,
    loading: false,
    error: null,
  })

  const atomicFlowInfo = shallowRef<AtomicState<FlowInfo | null>>({
    value: null,
    loading: false,
    error: null,
  })

  const atomicAuthoringNodes = shallowRef<AtomicState<AuthoringNodeItem[]>>({
    value: [],
    loading: false,
    error: null,
  })

  // User-modified param values
  const userParamsValues = ref<ParamsRecord>({})
  const userVersionParamValues = ref<ParamsRecord>({})
  const userBuildValues = ref<ParamsRecord>({})
  const userConstantValues = ref<ParamsRecord>({})
  const userOtherValues = ref<ParamsRecord>({})
  const userBuildNo = ref<BuildNoConfig>({})

  // Pipeline stages with skip state
  const stagesWithSkipState = shallowRef<Stage[]>([])

  // Execution state
  const executing = ref(false)

  // ----------------------------------------
  // Computed Data Layer
  // ----------------------------------------

  const loading = computed(
    () =>
      atomicStartupInfo.value.loading ||
      atomicPipelineModel.value.loading ||
      atomicFlowInfo.value.loading,
  )

  const error = computed(
    () =>
      atomicStartupInfo.value.error ||
      atomicPipelineModel.value.error ||
      atomicFlowInfo.value.error,
  )

  const startupInfo = computed(() => atomicStartupInfo.value.value)
  const rawPipelineModel = computed(() => atomicPipelineModel.value.value)

  const pipelineModel = computed(() => {
    const rawModel = rawPipelineModel.value?.modelAndSetting.model
    if (!rawModel) return null
    return {
      ...rawModel,
      stages: stagesWithSkipState.value.length > 0 ? stagesWithSkipState.value : rawModel.stages,
    }
  })

  const flowInfo = computed(() => atomicFlowInfo.value.value)
  const authoringNodes = computed(() => atomicAuthoringNodes.value.value)
  const authoringNodesLoading = computed(() => atomicAuthoringNodes.value.loading)

  const canElementSkip = computed(() => startupInfo.value?.canElementSkip ?? false)
  const canManualStartup = computed(() => startupInfo.value?.canManualStartup ?? false)
  const useLastParams = computed(() => startupInfo.value?.useLatestParameters ?? false)
  const buildNo = computed(() => userBuildNo.value)
  const isVisibleVersion = computed(() => userBuildNo.value.required ?? false)
  const paramValueKey = computed<'value' | 'defaultValue'>(() =>
    useLastParams.value ? 'value' : 'defaultValue',
  )

  // Helper to create label
  const createLabel = (id: string, name?: string) => (name ? `${id}(${name})` : id)

  const paramList = computed<ProcessedProperty[]>(() => {
    const properties = startupInfo.value?.properties ?? []
    return properties
      .filter(
        (p) =>
          !p.constant &&
          p.required &&
          !allVersionKeyList.includes(p.id) &&
          p.propertyType !== 'BUILD',
      )
      .map((p) => ({
        ...p,
        isChanged: isPlainObject(p.defaultValue)
          ? !isShallowEqual(p.defaultValue, p.value)
          : p.defaultValue !== p.value,
        readOnly: false,
        label: createLabel(p.id, p.name),
      }))
  })

  const versionParamList = computed<ProcessedProperty[]>(() => {
    const properties = startupInfo.value?.properties ?? []
    return properties
      .filter((p) => allVersionKeyList.includes(p.id))
      .map((p) => ({ ...p, isChanged: p.defaultValue !== p.value }))
  })

  const buildList = computed<ProcessedProperty[]>(() => {
    const properties = startupInfo.value?.properties ?? []
    return properties.filter((p) => p.propertyType === 'BUILD')
  })

  const constantParams = computed<ProcessedProperty[]>(() => {
    const properties = startupInfo.value?.properties ?? []
    return properties
      .filter((p) => p.constant)
      .map((p) => ({ ...p, label: createLabel(p.id, p.name) }))
  })

  const otherParams = computed<ProcessedProperty[]>(() => {
    const properties = startupInfo.value?.properties ?? []
    return properties
      .filter(
        (p) =>
          !p.constant &&
          !p.required &&
          !allVersionKeyList.includes(p.id) &&
          p.propertyType !== 'BUILD',
      )
      .map((p) => ({ ...p, label: createLabel(p.id, p.name) }))
  })

  const hasPipelineParams = computed(() => {
    return isVisibleVersion.value
      ? paramList.value.length + versionParamList.value.length > 0
      : paramList.value.length > 0
  })

  const hasOtherParams = computed(() => {
    return isVisibleVersion.value
      ? otherParams.value.length > 0
      : otherParams.value.length + versionParamList.value.length > 0
  })

  const pipelineParams = computed<ProcessedProperty[]>(() => {
    return isVisibleVersion.value
      ? [...paramList.value, ...versionParamList.value]
      : paramList.value
  })

  const paramsValues = computed(() => ({
    ...createParamsValuesMap(paramList.value, paramValueKey.value),
    ...userParamsValues.value,
  }))

  const versionParamValues = computed(() => ({
    ...createParamsValuesMap(versionParamList.value, paramValueKey.value),
    ...userVersionParamValues.value,
  }))

  const buildValues = computed(() => ({
    ...createParamsValuesMap(buildList.value, paramValueKey.value),
    ...userBuildValues.value,
  }))

  const constantValues = computed(() => ({
    ...createParamsValuesMap(constantParams.value, paramValueKey.value),
    ...userConstantValues.value,
  }))

  const otherValues = computed(() => ({
    ...createParamsValuesMap(otherParams.value, paramValueKey.value),
    ...userOtherValues.value,
  }))

  // Extract all elements from stages
  const allElements = computed(() => {
    return stagesWithSkipState.value.flatMap((stage) =>
      (stage.containers ?? []).flatMap((container) => container.elements ?? []),
    )
  })

  // Build skipped atoms record
  const skippedAtoms = computed<SkipAtomsRecord>(() => {
    return allElements.value
      .filter((el) => !(el as Element & { canElementSkip?: boolean }).canElementSkip)
      .reduce<SkipAtomsRecord>(
        (acc, el) => ({
          ...acc,
          [`devops_container_condition_skip_atoms_${el.id}`]: true,
        }),
        {},
      )
  })

  // ----------------------------------------
  // Actions
  // ----------------------------------------

  const transformStagesWithSkipProp = (stages: Stage[], checkedTotal: boolean): Stage[] => {
    return stages.map((stage) => {
      const stageDisabled =
        (stage.stageControlOption as { enable?: boolean } | undefined)?.enable === false
      const runStage = !stageDisabled && checkedTotal

      const containers = stage.containers?.map((container: Container) => {
        const containerDisabled = container.jobControlOption?.enable === false
        const runContainer = !containerDisabled && checkedTotal

        const elements = container.elements?.map((element: Element) => {
          const isSkipEle = element.additionalOptions?.enable === false || containerDisabled
          return { ...element, canElementSkip: !isSkipEle && checkedTotal }
        })

        return { ...container, runContainer, elements }
      })

      return { ...stage, runStage, containers }
    })
  }

  const initParams = (info: StartupInfo, existingValues?: ParamsRecord): boolean => {
    if (!info.canManualStartup) return false

    if (info.buildNo) {
      userBuildNo.value = { ...info.buildNo }
    }

    const key = info.useLatestParameters ? 'value' : 'defaultValue'

    const filterParams = (predicate: (p: StartupProperty) => boolean) =>
      info.properties.filter(predicate)

    userParamsValues.value = createParamsValuesMap(
      filterParams(
        (p) =>
          !p.constant &&
          p.required &&
          !allVersionKeyList.includes(p.id) &&
          p.propertyType !== 'BUILD',
      ),
      key,
      existingValues,
    )

    userVersionParamValues.value = createParamsValuesMap(
      filterParams((p) => allVersionKeyList.includes(p.id)),
      key,
      existingValues,
    )

    userBuildValues.value = createParamsValuesMap(
      filterParams((p) => p.propertyType === 'BUILD'),
      key,
      existingValues,
    )

    userConstantValues.value = createParamsValuesMap(
      filterParams((p) => p.constant),
      key,
      existingValues,
    )

    userOtherValues.value = createParamsValuesMap(
      filterParams(
        (p) =>
          !p.constant &&
          !p.required &&
          !allVersionKeyList.includes(p.id) &&
          p.propertyType !== 'BUILD',
      ),
      key,
      existingValues,
    )

    return true
  }

  const loadAuthoringNodes = async ({
    projectId,
    envHashId,
  }: {
    projectId: string
    envHashId: string
  }) => {
    atomicAuthoringNodes.value = { ...atomicAuthoringNodes.value, loading: true, error: null }

    try {
      const response = await fetchAuthoringNodeList({ projectId, envHashId })
      atomicAuthoringNodes.value = { value: response.records || [], loading: false, error: null }
      return response
    } catch (err) {
      const error = err instanceof Error ? err : new Error(String(err))
      atomicAuthoringNodes.value = { ...atomicAuthoringNodes.value, loading: false, error }
      console.error('Failed to load authoring nodes:', error)
      throw error
    }
  }

  const loadPreviewData = async ({
    projectId,
    flowId,
    version,
  }: {
    projectId: string
    flowId: string
    version?: number
  }) => {
    // Set loading states
    atomicStartupInfo.value = { ...atomicStartupInfo.value, loading: true, error: null }
    atomicPipelineModel.value = { ...atomicPipelineModel.value, loading: true, error: null }
    atomicFlowInfo.value = { ...atomicFlowInfo.value, loading: true, error: null }

    try {
      const [infoRes, pipelineRes, flowInfoRes] = await Promise.all([
        requestStartupInfo({ projectId, flowId, version }),
        getFlowModel(projectId, flowId, version?.toString()),
        fetchFlowInfo({ projectId, flowId }),
      ])

      atomicStartupInfo.value = { value: infoRes, loading: false, error: null }
      atomicFlowInfo.value = { value: flowInfoRes, loading: false, error: null }

      if (pipelineRes?.modelAndSetting?.model) {
        const processedModel: FlowModel = {
          ...pipelineRes.modelAndSetting.model,
          stages: pipelineRes.modelAndSetting.model.stages.slice(1),
        }
        atomicPipelineModel.value = {
          value: {
            ...pipelineRes,
            modelAndSetting: { ...pipelineRes.modelAndSetting, model: processedModel },
          },
          loading: false,
          error: null,
        }
        stagesWithSkipState.value = transformStagesWithSkipProp(processedModel.stages, true)
      } else {
        atomicPipelineModel.value = { value: null, loading: false, error: null }
      }

      initParams(infoRes)
      return { success: true }
    } catch (err) {
      const error = err instanceof Error ? err : new Error(String(err))
      atomicStartupInfo.value = { ...atomicStartupInfo.value, loading: false, error }
      atomicPipelineModel.value = { ...atomicPipelineModel.value, loading: false, error }
      atomicFlowInfo.value = { ...atomicFlowInfo.value, loading: false, error }
      console.error('Failed to load preview data:', error)
      throw error.message || error
    }
  }

  const executePipeline = async ({
    projectId,
    pipelineId,
    version,
    skipAtoms = {},
    remark = '',
    resourceHashId = '',
  }: {
    projectId: string
    pipelineId: string
    version?: number
    skipAtoms?: SkipAtomsRecord
    remark?: string
    resourceHashId?: string
  }) => {
    try {
      executing.value = true

      const collectedParams: ParamsRecord = {
        ...paramsValues.value,
        ...versionParamValues.value,
        ...buildValues.value,
        ...constantValues.value,
        ...otherValues.value,
        ...skipAtoms,
      }

      // Stringify object values except buildNo
      const processedParams = Object.entries(collectedParams).reduce<ParamsRecord>(
        (acc, [key, value]) => ({
          ...acc,
          [key]: key !== 'buildNo' && isPlainObject(value) ? JSON.stringify(value) : value,
        }),
        {},
      )

      const finalParams: ParamsRecord =
        Object.keys(userBuildNo.value).length > 0
          ? { ...processedParams, buildNo: userBuildNo.value }
          : processedParams

      return await requestExecPipeline({
        projectId,
        pipelineId,
        version,
        params: finalParams,
        remark,
        resourceHashId,
      })
    } finally {
      executing.value = false
    }
  }

  const updateParamValue = (category: ParamCategory, key: string, value: unknown): void => {
    const categoryMap: Record<ParamCategory, typeof userParamsValues> = {
      params: userParamsValues,
      versionParam: userVersionParamValues,
      build: userBuildValues,
      constant: userConstantValues,
      other: userOtherValues,
    }
    const target = categoryMap[category]
    if (target) {
      target.value = { ...target.value, [key]: value }
    }
  }

  const updateBuildNo = (key: string, value: unknown): void => {
    userBuildNo.value = { ...userBuildNo.value, [key]: value }
  }

  const setPipelineSkipProp = (checkedTotal: boolean): void => {
    const rawModel = rawPipelineModel.value
    if (!rawModel?.modelAndSetting.model.stages) return
    stagesWithSkipState.value = transformStagesWithSkipProp(
      rawModel.modelAndSetting.model.stages,
      checkedTotal,
    )
  }

  const updatePipelineFromChange = (newPipeline: { stages: Stage[] }): void => {
    if (!newPipeline?.stages) return
    // Deep clone to ensure shallowRef reactivity update detects all nested changes
    // This is necessary because Object.assign in bk-pipeline modifies objects in place
    // and shallowRef only detects top-level reference changes
    stagesWithSkipState.value = JSON.parse(JSON.stringify(newPipeline.stages))
  }

  const getAllElements = (stages: Stage[]): Element[] => {
    return stages.flatMap((stage) =>
      (stage.containers ?? []).flatMap((container) => container.elements ?? []),
    )
  }

  const getSkippedAtoms = (): SkipAtomsRecord => skippedAtoms.value

  const $reset = (): void => {
    atomicStartupInfo.value = { value: null, loading: false, error: null }
    atomicPipelineModel.value = { value: null, loading: false, error: null }
    atomicFlowInfo.value = { value: null, loading: false, error: null }
    atomicAuthoringNodes.value = { value: [], loading: false, error: null }

    userParamsValues.value = {}
    userVersionParamValues.value = {}
    userBuildValues.value = {}
    userConstantValues.value = {}
    userOtherValues.value = {}
    userBuildNo.value = {}
    stagesWithSkipState.value = []
    executing.value = false
  }

  // ----------------------------------------
  // Return Public API
  // ----------------------------------------

  return {
    // Atomic States
    atomicStartupInfo,
    atomicPipelineModel,
    atomicFlowInfo,
    atomicAuthoringNodes,

    // Computed State
    loading,
    error,
    executing,
    startupInfo,
    pipelineModel,
    flowInfo,
    authoringNodes,
    authoringNodesLoading,
    paramList,
    versionParamList,
    buildList,
    constantParams,
    otherParams,
    paramsValues,
    versionParamValues,
    buildValues,
    constantValues,
    otherValues,
    buildNo,
    isVisibleVersion,

    // Computed Flags
    canElementSkip,
    canManualStartup,
    useLastParams,
    hasPipelineParams,
    hasOtherParams,
    pipelineParams,
    allElements,
    skippedAtoms,

    // Pipeline Stages State
    stagesWithSkipState,
    rawPipelineModel,

    // Actions
    loadPreviewData,
    loadAuthoringNodes,
    executePipeline,
    updateParamValue,
    updateBuildNo,
    initParams,
    setPipelineSkipProp,
    updatePipelineFromChange,
    getAllElements,
    getSkippedAtoms,
    $reset,
  }
})

// Export types for external use
export type {
  AtomicState,
  BuildNoConfig,
  ParamCategory,
  ParamsRecord,
  ProcessedProperty,
  SkipAtomsRecord,
}
