import { computed } from 'vue'
import useInstance from './useInstance'
import useNodeDetail from './useNodeDetail'

export default function useTaskDetail () {
    const { proxy } = useInstance()
    const { currentNode } = useNodeDetail()
    const routeName = computed(() => proxy.$route.name)
    const envHashId = computed(() => proxy.$route.params?.envId)
    const projectId = computed(() => proxy.$route.params?.projectId)
    
    // 根据路由动态获取 ID 参数
    const getIdParams = () => {
        return routeName.value === 'envDetail'
            ? { envId: envHashId.value }
            : { agentId: currentNode.value?.agentId }
    }

    // 获取任务列表（view: PIPELINE / JOB / BUILD）
    const fetchTaskList = async (view, params) => {
        try {
            const res = await proxy.$store.dispatch('environment/requestAgentPipelineList', {
                projectId: projectId.value,
                body: {
                    ...getIdParams(),
                    ...params,
                    view
                }
            })
            return res
        } catch (e) {
            throw e
        }
    }

    // 视图 -> 展开明细接口映射
    const BUILD_DETAIL_ACTION_MAP = {
        PIPELINE: 'environment/fetchAgentBuildsByPipeline',
        JOB: 'environment/fetchAgentBuildsByJob',
        BUILD: 'environment/fetchAgentBuildsByBuild'
    }

    // 展开加载构建明细，按视图维度请求
    const fetchBuildDetail = async (view, {
        pipelineId,
        jobId,
        buildId,
        params
    }) => {
        try {
            const action = BUILD_DETAIL_ACTION_MAP[view] || BUILD_DETAIL_ACTION_MAP.JOB
            const res = await proxy.$store.dispatch(action, {
                params: {
                    projectId: projectId.value,
                    ...getIdParams(),
                    ...(pipelineId ? { pipelineId } : {}),
                    ...(jobId ? { jobId } : {}),
                    ...(buildId ? { buildId } : {}),
                    ...params
                }
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
        fetchTaskList,
        fetchBuildDetail,
        searchJobByName,
        searchPipelineByName,
        searchByCreator
    }
}
