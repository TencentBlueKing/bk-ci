import { ref } from 'vue'
import useInstance from './useInstance'

const defaultCreateEnvParam = (envType = 'BUILD') => {
    return {
        desc: '',
        envType,
        name: '',
        // createdUser: '',
        source: 'EXISTING'
    }
}

const envParams = ref(defaultCreateEnvParam())
const isShow = ref(false)
const isLoading = ref(false)

export default function useCreateEnv (onSuccess, onError) {
    const { proxy } = useInstance()
    
    const showCreateEnvDialog = (envType = 'BUILD') => {
        isShow.value = true
        const validEnvType = (typeof envType === 'string') ? envType : 'BUILD'
        envParams.value = defaultCreateEnvParam(validEnvType)
    }
    
    const closeCreateEnvDialog = () => {
        isShow.value = false
    }

    const createNewEnv = async () => {
        isLoading.value = true
        try {
            const projectId = proxy.$route.params.projectId
            const res = await proxy.$store.dispatch('environment/createNewEnv', {
                projectId,
                params: envParams.value
            })
            
            closeCreateEnvDialog()
            onSuccess?.(res)
        } catch (err) {
            console.error(err)
            onError?.(err)
        } finally {
            isLoading.value = false
        }
    }
    
    return {
        // data
        isShow,
        isLoading,
        envParams,

        // function
        createNewEnv,
        closeCreateEnvDialog,
        showCreateEnvDialog
    }
}