import { useFlowInfoStore } from '@/stores/flowInfoStore';
import { storeToRefs } from 'pinia';
import { computed, onBeforeUnmount, onMounted, watch } from 'vue';
import { useRoute } from 'vue-router';
import { VERSION_STATUS_ENUM } from '../utils/flowConst';

export function useFlowInfo() {
  const store = useFlowInfoStore()
  const route = useRoute()
  const { flowInfo, flowVersionList, loading } = storeToRefs(store)

  const releasedVersionList = computed(() => {
    return flowVersionList.value?.filter((v => v.status === VERSION_STATUS_ENUM.RELEASED)) ?? []
  })

  onMounted(() => {
    store.initFlowInfo()
  })

  
  function reset() {
    store.reset()
  }
  
  watch(
    () => route.params.flowId,
    (newFlowId) => {
      if (newFlowId) {
        store.initFlowInfo()
      }
    }
  )

  return { flowInfo, flowVersionList, releasedVersionList, loading, reset }
}
