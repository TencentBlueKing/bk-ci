import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRoute } from 'vue-router'
import UseInstance from './useInstance'

const StepStatus = {
    SUCCESS: 'success',
    FAIL: 'fail',
    DOING: 'doing',
    UNDO: 'undo',
}

export default function useProgress () {
    const { proxy } = UseInstance()
    const { $store, $route } = proxy
    
    let timer = null
    const progress = ref(null)
    const runningStep = ref(null)

    const storeId = computed(() => $route.params.storeId)

    const isRunning = computed(() => {
        const processInfos = progress.value?.processInfos ?? []
        return !processInfos.some((p, index) => p.status === StepStatus.FAIL
            || (
                index === processInfos.length - 1 && p.status === StepStatus.SUCCESS
            ))
    })

    const isEnd = computed(() => {
        const processInfos = progress.value?.processInfos ?? []
        return processInfos[processInfos.length - 1]?.status === StepStatus.SUCCESS
    })

    async function fetchProgress () {
        try {
            progress.value = await $store.dispatch('store/fetchProgress', storeId.value)
            if (progress.value) {
                runningStep.value = progress.value.processInfos.find((p) =>
                    p.status === StepStatus.DOING || p.status === StepStatus.FAIL
                )
            }
        } catch (error) {
            clearTimeout(timer)
        }
    }

    async function loopProgress () {
        clearTimeout(timer)
        await fetchProgress()
        if (isRunning.value && (runningStep.value?.code === 'build' || runningStep.value?.code === 'approve')) {
            timer = setTimeout(() => {
                loopProgress()
            }, 6000)
        }
    }

    function refreshProgress () {
        clearTimeout(timer)
    }

    onMounted(() => {
        loopProgress()
    })

    onBeforeUnmount(() => {
        clearTimeout(timer)
    })

    return {
        progress,
        isEnd,
        runningStep,
        refreshProgress,
        loopProgress,
        fetchProgress,
        StepStatus
    }
}
