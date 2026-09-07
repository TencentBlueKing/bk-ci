import type { AuthoringEnvItem } from '@/api/authoringEnvironmentApi'
import type { Container } from '@/api/flowModel'
import {
  getPluginProperties,
  type PluginPropertyItem,
} from '@/api/flowContentList'
import { useFlowModelStore } from '@/stores/flowModel'
import { storeToRefs } from 'pinia'
import { useRoute } from 'vue-router'

export interface IncompatiblePlugin {
  atomCode: string
  name: string
  supportedOs: string[]
}

export interface EnvironmentOsCompatibilityResult {
  type: 'unchanged' | 'compatible' | 'incompatible'
  previousOs: string
  nextOs: string
  incompatiblePlugins: IncompatiblePlugin[]
}

interface FlowPlugin {
  atomCode: string
  name: string
  version?: string
}

function normalizeOs(os?: string): string {
  return os?.toUpperCase() || ''
}

function matchVersion(pattern: string, version: string): boolean {
  if (pattern === version) return true
  const expression = pattern
    .replace(/[.+?^${}()|[\]\\]/g, '\\$&')
    .replace(/\*/g, '.*')
  return new RegExp(`^${expression}$`).test(version)
}

function getSupportedOs(atomProp: PluginPropertyItem, version?: string): string[] {
  if (version && atomProp.versionOsMap) {
    const versionEntry = Object.entries(atomProp.versionOsMap)
      .find(([pattern]) => matchVersion(pattern, version))
    if (versionEntry) {
      return versionEntry[1].map(normalizeOs).filter((os) => os && os !== 'NONE')
    }
  }

  return (atomProp.os || []).map(normalizeOs).filter((os) => os && os !== 'NONE')
}

/**
 * 收集 detail 模型中的插件，并在创作环境 OS 变更前校验插件兼容性。
 */
export function useEnvironmentOsCompatibility() {
  const route = useRoute()
  const flowModelStore = useFlowModelStore()
  const { flowModel } = storeToRefs(flowModelStore)

  function collectFlowPlugins(): FlowPlugin[] {
    const plugins: FlowPlugin[] = []
    const seen = new Set<string>()

    flowModel.value?.stages?.forEach((stage) => {
      stage.containers?.forEach((container) => {
        container.elements?.forEach((element) => {
          if (!element.atomCode) return
          const key = `${element.atomCode}:${element.version || ''}:${element.name || ''}`
          if (seen.has(key)) return
          seen.add(key)
          plugins.push({
            atomCode: element.atomCode,
            name: element.name || element.atomCode,
            version: element.version,
          })
        })
      })
    })

    return plugins
  }

  async function checkEnvironmentChange(
    currentEnv?: AuthoringEnvItem,
    nextEnv?: AuthoringEnvItem,
  ): Promise<EnvironmentOsCompatibilityResult> {
    const previousOs = normalizeOs(currentEnv?.os)
    const nextOs = normalizeOs(nextEnv?.os)
    const unchangedResult: EnvironmentOsCompatibilityResult = {
      type: 'unchanged',
      previousOs,
      nextOs,
      incompatiblePlugins: [],
    }

    if (!previousOs || !nextOs || previousOs === nextOs) {
      return unchangedResult
    }

    const projectId = route.params.projectId as string
    const pipelineId = route.params.flowId as string
    if (!projectId || !pipelineId) {
      return unchangedResult
    }

    const routeVersion = Number(route.params.version)
    const atomProperties = await getPluginProperties({
      projectId,
      pipelineId,
      version: Number.isFinite(routeVersion) && routeVersion > 0 ? routeVersion : undefined,
    })

    const incompatiblePlugins = collectFlowPlugins().reduce<IncompatiblePlugin[]>(
      (result, plugin) => {
        const atomProp = atomProperties[plugin.atomCode]
        if (!atomProp) return result

        const supportedOs = getSupportedOs(atomProp, plugin.version)
        // 未声明 OS 或仅声明 NONE 的插件不依赖创作环境，不参与拦截。
        if (!supportedOs.length || supportedOs.includes(nextOs)) return result

        result.push({
          atomCode: plugin.atomCode,
          name: plugin.name,
          supportedOs,
        })
        return result
      },
      [],
    )

    return {
      type: incompatiblePlugins.length ? 'incompatible' : 'compatible',
      previousOs,
      nextOs,
      incompatiblePlugins,
    }
  }

  /**
   * 环境切换确认后，将编排中所有 Job（含矩阵组子 Job）的 baseOS 同步为新环境 OS。
   */
  function updateJobBaseOs(os?: string) {
    const nextOs = normalizeOs(os)
    const model = flowModel.value
    if (!nextOs || !model) return

    const updateContainer = (container: Container): Container => ({
      ...container,
      baseOS: nextOs,
      groupContainers: container.groupContainers?.map(updateContainer),
    })

    flowModelStore.updateFlowModel({
      ...model,
      // 第一个 Stage 为触发器，后续 Stage 中的 Container 才是编排 Job。
      stages: model.stages.map((stage, index) => index === 0
        ? stage
        : {
            ...stage,
            containers: stage.containers?.map(updateContainer),
          }),
    })
  }

  return {
    checkEnvironmentChange,
    updateJobBaseOs,
  }
}

export default useEnvironmentOsCompatibility
