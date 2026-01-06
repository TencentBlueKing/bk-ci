import { ref } from 'vue'
import useInstance from './useInstance'

const defaultCreateEnvParam = () => {
    return {
        desc: '',
        envType: 'BUILD',
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
    
    const showCreateEnvDialog = () => {
        isShow.value = true
        envParams.value = defaultCreateEnvParam()
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