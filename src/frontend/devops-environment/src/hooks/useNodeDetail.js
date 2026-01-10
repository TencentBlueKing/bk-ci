import { computed, ref, reactive } from 'vue'
import { convertTime } from '@/utils/util'
import useInstance from './useInstance'

const currentNode = ref({})
const agentOfflineData = reactive({
    list: [],
    loading: false
})
const nodeEnvsList = ref([]) // 节点环境变量列表

export default function useNodeDetail () {
    const { proxy } = useInstance()
    const nodeHashId = computed(() => proxy.$route.query?.nodeHashId)
    const projectId = computed(() => proxy.$route.params?.projectId)
    const nodeDetailLoaded = ref(false) // 节点详情是否加载完成
   
    /**
     * 获取节点详情
     */
    const fetchNodeDetail = async () => {
        if (!nodeHashId.value) return
        try {
            nodeDetailLoaded.value = false
            const res = await proxy.$store.dispatch('environment/requestNodeDetail', {
                projectId: projectId.value,
                nodeHashId: nodeHashId.value
            })
            currentNode.value = res ?? {}
            nodeDetailLoaded.value = true
            return res
        } catch (e) {
            nodeDetailLoaded.value = false
            proxy.$bkMessage({
                message: e.message || proxy.$t('environment.fetchNodeDetailFailed'),
                theme: 'error'
            })
            throw e
        }
    }

    /**
     * 更新节点显示名称
     */
    const updateDisplayName = async (displayName) => {
        if (!nodeHashId.value) return
        try {
            const res = await proxy.$store.dispatch('environment/updateDisplayName', {
                projectId: projectId.value,
                nodeHashId: nodeHashId.value,
                params: { displayName }
            })
            // 更新本地缓存
            if (currentNode.value) {
                currentNode.value.displayName = displayName
            }
            return res
        } catch (e) {
            throw e
        }
    }

    /**
     * 删除节点
     */
    const deleteNode = async () => {
        if (!nodeHashId.value) return
        try {
            const res = await proxy.$store.dispatch('environment/toDeleteNode', {
                projectId: projectId.value,
                params: [nodeHashId.value]
            })
            return res
        } catch (e) {
            throw e
        }
    }

    /**
     * 设置节点标签
     */
    const setNodeTag = async (tags) => {
        if (!currentNode.value?.nodeId) return
        try {
            const res = await proxy.$store.dispatch('environment/setNodeTag', {
                projectId: projectId.value,
                params: {
                    nodeId: currentNode.value.nodeId,
                    tags
                }
            })
            return res
        } catch (e) {
            throw e
        }
    }

    /**
     * 获取Agent离线记录
     * @param {Number} page - 页码
     * @param {Number} pageSize - 每页条数
     */
    const fetchAgentOfflinePeriod = async (page, pageSize) => {
        if (!nodeHashId.value) return
        agentOfflineData.loading = true
        try {
            const res = await proxy.$store.dispatch('environment/requestAgentOfflinePeriod', {
                projectId: projectId.value,
                agentHashId: currentNode.value.agentId,
                page,
                pageSize
            })
            agentOfflineData.list = res?.records || []
            return res
        } catch (e) {
            proxy.$bkMessage({
                message: e.message || proxy.$t('environment.fetchOfflineRecordsFailed'),
                theme: 'error'
            })
        } finally {
            agentOfflineData.loading = false
        }
    }

    /**
     * 保存最大构建并发数
     * @param {Number} count - 并发数
     */
    const saveParallelTaskCount = async (count) => {
        if (!nodeHashId.value) return
        try {
            const res = await proxy.$store.dispatch('environment/saveParallelTaskCount', {
                projectId: projectId.value,
                nodeHashId: nodeHashId.value,
                count
            })
            return res
        } catch (e) {
            throw e
        }
    }

    /**
     * 保存docker构建最大并发数
     * @param {Number} count - 并发数
     */
    const saveDockerParallelTaskCount = async (count) => {
        if (!nodeHashId.value) return
        try {
            const res = await proxy.$store.dispatch('environment/saveDockerParallelTaskCount', {
                projectId: projectId.value,
                nodeHashId: nodeHashId.value,
                count
            })
            return res
        } catch (e) {
            throw e
        }
    }

    /**
     * 获取节点环境变量列表
     * @param {Object} params - 查询参数 { envName, envValue, source }
     */
    const fetchNodeEnvs = async (params) => {
        if (!nodeHashId.value) return
        try {
            const res = await proxy.$store.dispatch('environment/requestEnvs', {
                projectId: projectId.value,
                nodeHashId: nodeHashId.value,
                params
            })
            nodeEnvsList.value = res || []
            return res
        } catch (e) {
            throw e
        }
    }

    /**
     * 保存节点环境变量
     * @param {Array} envVars - 环境变量数组
     */
    const saveNodeEnvs = async (envVars) => {
        if (!nodeHashId.value) return
        try {
            const res = await proxy.$store.dispatch('environment/saveEnvs', {
                projectId: projectId.value,
                nodeHashId: nodeHashId.value,
                params: envVars
            })
            return res
        } catch (e) {
            throw e
        }
    }

    /**
     * 删除节点环境变量
     * @param {Object} row - 要删除的环境变量
     */
    const deleteNodeEnv = async (row) => {
        if (!nodeHashId.value) return
        // 根据 row 的内容找到真实的索引
        const realIndex = nodeEnvsList.value.findIndex(item =>
            item.name === row.name && item.value === row.value && item.secure === row.secure
        )
        const newEnvVars = nodeEnvsList.value.filter((item, i) => i !== realIndex)
        
        try {
            const res = await saveNodeEnvs(newEnvVars)
            return res
        } catch (e) {
            throw e
        }
    }

    return {
        // data
        currentNode,
        projectId,
        nodeHashId,
        agentOfflineData,
        nodeEnvsList,
        nodeDetailLoaded,

        // function
        fetchNodeDetail,
        updateDisplayName,
        deleteNode,
        setNodeTag,
        fetchAgentOfflinePeriod,
        saveParallelTaskCount,
        saveDockerParallelTaskCount,
        fetchNodeEnvs,
        saveNodeEnvs,
        deleteNodeEnv
    }
}