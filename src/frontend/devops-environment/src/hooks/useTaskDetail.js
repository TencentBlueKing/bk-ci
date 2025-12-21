import { computed } from 'vue'
import useInstance from './useInstance'

export default function useTaskDetail () {
    const { proxy } = useInstance()
    const routeName = computed(() => proxy.$route.name)
    const envHashId = computed(() => proxy.$route.params?.envId)
    const nodeHashId = computed(() => proxy.$route.params?.nodeHashId)
    const projectId = computed(() => proxy.$route.params?.projectId)
    
    // 根据路由动态获取 ID 参数
    const getIdParams = () => {
        return routeName.value === 'envDetail'
            ? { envId: envHashId.value }
            : { agentId: nodeHashId.value }
    }

    const fetchJobTaskList = async (params) => {
        try {
            const res = await proxy.$store.dispatch('environment/requestAgentJobTaskList', {
                projectId: projectId.value,
                params
            })
            return res
        } catch (e) {
            throw e
        }
    }

    const fetchPipelineBuildHistory = async ({
        pipelineId,
        containerId,
        params
    }) => {
        try {
            const res = await proxy.$store.dispatch('environment/requestPipelineBuildHistory', {
                projectId: projectId.value,
                pipelineId,
                containerId,
                params
            })
            return res
        } catch (e) {
            throw e
        }
    }

    // 根据Job名称搜索
    const searchJobByName = async (keyword) => {
        try {
            const res = await proxy.$store.dispatch('environment/searchJobByName', {
                params: {
                    projectId: projectId.value,
                    ...getIdParams(),
                    jobName: keyword
                }
            })
            return res
        } catch (e) {
            throw e
        }
    }

    // 根据流水线名称搜索
    const searchPipelineByName = async (keyword) => {
        try {
            const res = await proxy.$store.dispatch('environment/searchPipelineByName', {
                params: {
                    projectId: projectId.value,
                    ...getIdParams(),
                    pipelineName: keyword
                }
            })
            return res
        } catch (e) {
            throw e
        }
    }

    // 根据触发人搜索
    const searchByCreator = async (keyword) => {
        try {
            const res = await proxy.$store.dispatch('environment/searchByCreator', {
                params: {
                    projectId: projectId.value,
                    ...getIdParams(),
                    creator: keyword
                }
            })
            return res
        } catch (e) {
            throw e
        }
    }

    return {
        fetchJobTaskList,
        fetchPipelineBuildHistory,
        searchJobByName,
        searchPipelineByName,
        searchByCreator
    }
}
