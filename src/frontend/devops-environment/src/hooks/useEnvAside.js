import { ref, computed, watch } from 'vue'
import useInstance from './useInstance'
import useEnvDetail from './useEnvDetail'

import {
    ENV_TYPE_MAP
} from '@/store/constants'

const envList = ref([])
const envCountData = ref({})
export default function useEnvAside () {
    const { proxy } = useInstance()
    const isLoading = ref(false)
    const envName = ref('')
    const {
        setEnvDetailLoaded,
    } = useEnvDetail()
    
    // 初始化方法：检查并设置默认的 envType，获取列表
    const initData = async () => {
        const currentEnvType = proxy.$route.params?.envType
        const currentEnvId = proxy.$route.params?.envId
        
        // 如果 URL 中没有 envType 参数，设置默认值为 ALL
        if (!currentEnvType) {
            await proxy.$router.replace({
                name: 'envDetail',
                params: {
                    ...proxy.$route.params,
                    envType: ENV_TYPE_MAP.ALL,
                    envId: currentEnvId, // 保持原有的 envId
                    tabName: undefined // 清除 tabName，让 env_detail 重新设置
                }
            }).catch(err => {
                console.error('路由导航错误:', err)
            })
        }
        await fetchEnvList()
        await fetchEnvCountAsType()
    }

    const envType = computed(() => proxy.$route.params?.envType)
    const envId = computed(() => proxy.$route.params.envId)
    const projectId = computed(() => proxy.$route.params.projectId)
    
    // 计算环境总数
    const totalEnvCount = computed(() => {
        if (!envCountData.value) return 0
        return Object.values(envCountData.value).reduce((sum, count) => sum + (count || 0), 0)
    })
    const fetchEnvList = async () => {
        isLoading.value = true
        try {
            const params = {
                ...(envType.value !== ENV_TYPE_MAP.ALL ? { envType: envType.value } : {}),
                ...(envName.value ? { envName: envName.value } : {})
            }
            const res = await proxy.$store.dispatch('environment/requestEnvList', {
                projectId: projectId.value,
                params
            })
            envList.value = res
            proxy.$store.commit('environment/setEnvList', res)
            
            // 如果没有 envId 或者当前 envId 不在列表中，设置为第一个环境的 ID
            const currentEnvId = envId.value
            const validEnvId = currentEnvId && res.find(env => env.envHashId === currentEnvId)
                ? currentEnvId
                : res[0]?.envHashId
            if (envList.value.length === 0) {
                setEnvDetailLoaded(true)
            }
                
            if (currentEnvId !== validEnvId) {
                proxy.$router.replace({
                    name: 'envDetail',
                    params: {
                        ...proxy.$route.params,
                        envId: validEnvId,
                        tabName: undefined
                    }
                }).catch(err => {
                    console.error('路由导航错误:', err)
                })
            }
        } catch (error) {
            proxy?.$showTips({
                message: error.message,
                theme: 'error'
            })
        } finally {
            isLoading.value = false
        }
    }

    const fetchEnvCountAsType = async (createEnv = false) => {
        try {
            const res = await proxy.$store.dispatch('environment/requestEnvCount', {
                projectId: projectId.value,
                createEnv
            })
            envCountData.value = res
            return res
        } catch (error) {
            proxy?.$showTips({
                message: error.message || error,
                theme: 'error'
            })
            return null
        }
    }

    // 删除环境
    const deleteEnv = async (envHashId) => {
        try {
            await proxy.$store.dispatch('environment/toDeleteEnv', {
                projectId: projectId.value,
                envHashId
            })
            
            // 刷新环境列表和计数
            await fetchEnvList()
            await fetchEnvCountAsType()
            
            return true
        } catch (error) {
            throw error
        }
    }
    
    return {
        // data
        isLoading,
        envName,
        envType,
        envList,
        envCountData,
        totalEnvCount,

        // function
        initData,
        fetchEnvList,
        fetchEnvCountAsType,
        deleteEnv
    }
}