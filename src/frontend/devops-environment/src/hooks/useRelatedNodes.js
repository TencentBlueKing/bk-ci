import { ref, computed } from 'vue'
import useEnvDetail from './useEnvDetail'
import useInstance from './useInstance'

const RELATED_TYPE = {
    STATIC: 'static',
    DYNAMIC: 'dynamic'
}
const isShow = ref(false)
export default function useRelatedNodes () {
    const { proxy } = useInstance()
    const {
        currentEnv
    } = useEnvDetail()
    const isLoading = ref(false)
    const relatedType = ref(RELATED_TYPE.STATIC)
    const projectId = computed(() => proxy.$route.params.projectId)
    const envHashId = computed(() => proxy.$route.params.envId)
    const handleShowRelatedNodes = () => {
        isShow.value = true
    }
    const nodeList = ref([])
    const requestNodeList = async (params) => {
        try {
            isLoading.value = true
            const res = await proxy.$store.dispatch('environment/requestNodeList', {
                projectId: projectId.value,
                envHashId: envHashId.value,
                params
            })
            
            if (params.page === 1) {
                nodeList.value = res.records || []
            } else {
                nodeList.value = [...nodeList.value, ...(res.records || [])]
            }
            
            return res
        } catch (e) {
            console.error(e)
            return { records: [], count: 0 }
        } finally {
            isLoading.value = false
        }
    }
    
    const handleCloseDialog = () => {
        isShow.value = false
    }
    
    const relateNodes = async (params) => {
        try {
            const res = await proxy.$store.dispatch('environment/importEnvNode', {
                projectId: projectId.value,
                envHashId: envHashId.value,
                params
            })
        } catch (error) {
            
        }
    }
    return {
        // data
        isShow,
        nodeList,
        isLoading,
        relatedType,
        RELATED_TYPE,
        
        // function
        relateNodes,
        requestNodeList,
        handleCloseDialog,
        handleShowRelatedNodes,
    }
}
