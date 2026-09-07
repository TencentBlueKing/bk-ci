import { useAuthoringEnvironmentStore } from '@/stores/authoringEnvironmentStore'
import { useFlowModelStore } from '@/stores/flowModel'
import { useUIStore } from '@/stores/ui'
import { resolveAuthoringBaseOS, type AuthoringBaseOS } from '@/utils/project'
import { storeToRefs } from 'pinia'
import { computed, type ComputedRef } from 'vue'

/**
 * Resolve Job baseOS from the Flow's selected authoring environment.
 * Falls back to project-type OS when env is unset or os is unavailable.
 */
export function useAuthoringBaseOS(): {
  authoringBaseOS: ComputedRef<AuthoringBaseOS>
} {
  const uiStore = useUIStore()
  const { flowSetting } = storeToRefs(useFlowModelStore())
  const { envList } = storeToRefs(useAuthoringEnvironmentStore())
  const { currentProject } = storeToRefs(uiStore)

  const authoringBaseOS = computed(() => {
    const envHashId = flowSetting.value?.envHashId
    const selectedEnv = envHashId
      ? envList.value.find((env) => env.envHashId === envHashId)
      : undefined
    return resolveAuthoringBaseOS(selectedEnv?.os, currentProject.value)
  })

  return {
    authoringBaseOS,
  }
}

export default useAuthoringBaseOS
