import { computed, ref } from 'vue'
import { convertTime } from '@/utils/util'
import useInstance from './useInstance'

const currentEnv = ref({})

export default function useEnvDetail () {
    const { proxy } = useInstance()
    const envNodeList = ref([]) // 节点列表
    const envHashId = computed(() => proxy.$route.params?.envId)
    const projectId = computed(() => proxy.$route.params?.projectId)
    const envList = computed(() => proxy.$store.getters['environment/getEnvList'] || [])
    
    // 获取环境节点列表
    const fetchEnvNodeList = async (params) => {
        if (!envHashId.value) return
        try {
            const res = await proxy.$store.dispatch('environment/requestEnvNodeList', {
                projectId: projectId.value,
                envHashId: envHashId.value,
                params: {
                    ...params
                }
            })
            envNodeList.value = res.records
            // 返回完整的响应数据，包含分页信息
            return res
        } catch (e) {
            throw e
        }
    }

    // 删除环境节点
    const requestRemoveNode = async (params) => {
        if (!envHashId.value) return
        try {
            const res = await proxy.$store.dispatch('environment/toDeleteEnvNode', {
                projectId: projectId.value,
                envHashId: envHashId.value,
                params
            })
            return res
        } catch (e) {
            throw e
        }
    }
    // 获取环境详情

    const fetchEnvDetail = async () => {
        if (!envHashId.value) return
        try {
            const res = await proxy.$store.dispatch('environment/requestEnvDetail', {
                projectId: projectId.value,
                envHashId: envHashId.value
            })
            currentEnv.value = res ?? {}
            return res
        } catch (e) {
            throw e
        }
    }
    // 获取环境的环境变量
    const envParamsList = ref([]) // 环境变量列表
    const fetchEnvParamsList = async (params) => {
        if (!envHashId.value) return
        try {
            const res = await proxy.$store.dispatch('environment/requestEnvParamsList', {
                projectId: projectId.value,
                envHashId: envHashId.value,
                params
            })
            envParamsList.value = res ?? []
            return res
        } catch (e) {
            throw e
        }
    }
    // 修改环境
    const updateEnvDetail = async (params) => {
        try {
            const res = await proxy.$store.dispatch('environment/toModifyEnv', {
                projectId: projectId.value,
                envHashId: envHashId.value,
                params
            })
            return res
        } catch (e) {
            throw e
        }
    }

    // 启用/停用节点
    const toggleEnableNode = async (nodeHashId, enableNode) => {
        if (!envHashId.value) return
        try {
            const res = await proxy.$store.dispatch('environment/enableNode', {
                projectId: projectId.value,
                envHashId: envHashId.value,
                nodeHashId,
                enableNode
            })
            return res
        } catch (e) {
            throw e
        }
    }

    const relatedProjectList = ref([])
    const fetchEnvRelatedProject = async (params) => {
        try {
            const res = await proxy.$store.dispatch('environment/requestShareEnvProjectList', {
                projectId: projectId.value,
                envHashId: envHashId.value,
                params
            })

            relatedProjectList.value = res.records.map(record => ({
                ...record,
                updateTime: convertTime(record.updateTime * 1000)
            })) ?? []
            return res
        } catch (e) {
            throw e
        }
    }

    return {
        // data
        currentEnv,
        envNodeList,
        envParamsList,
        projectId,
        envHashId,
        relatedProjectList,

        // function
        fetchEnvNodeList,
        requestRemoveNode,
        fetchEnvDetail,
        fetchEnvParamsList,
        updateEnvDetail,
        fetchEnvRelatedProject,
        toggleEnableNode
    }
}