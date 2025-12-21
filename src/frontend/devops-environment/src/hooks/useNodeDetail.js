import { computed, ref } from 'vue'
import { convertTime } from '@/utils/util'
import useInstance from './useInstance'

const currentNode = ref({})

export default function useEnvDetail () {
    const { proxy } = useInstance()
    const nodeHashId = computed(() => proxy.$route.params?.nodeHashId)
    const projectId = computed(() => proxy.$route.params?.projectId)
   
    return {
        // data
        currentNode,
        projectId,
        nodeHashId,

        // function
    }
}