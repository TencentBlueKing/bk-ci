import { computed, ref, reactive } from 'vue'
import { convertTime } from '@/utils/util'
import useInstance from './useInstance'
import { fetchNodeRelations, invalidateNodeRelationsCache } from './useNodeRelations'
import { SERVICE_RESOURCE_TYPE } from '@/store/constants'

const currentNode = ref({})
const agentOfflineData = reactive({
    list: [],
    loading: false
})
const nodeEnvsList = ref([]) // 节点环境变量列表
/** 概览「所属环境」，与详情一并加载，供页面 loading 收敛 */
const nodeEnvRelations = ref([])

export default function useNodeDetail () {
    const { proxy } = useInstance()
    const nodeHashId = computed(() => proxy.$route.query?.nodeHashId)
    const projectId = computed(() => proxy.$route.params?.projectId)
    const nodeDetailLoaded = ref(false) // 节点详情 + 所属环境是否都加载完成

    const isCreateResType = () => proxy.$route.params?.resType === SERVICE_RESOURCE_TYPE.CREATE

    /**
     * 创作流：按详情 envIds 拉所属环境；流水线跳过
     */
    const fetchEnvRelations = async () => {
        if (!isCreateResType()) {
            nodeEnvRelations.value = []
            return []
        }
        const hashId = currentNode.value?.nodeHashId || nodeHashId.value
        if (!hashId) {
            nodeEnvRelations.value = []
            return []
        }
        try {
            invalidateNodeRelationsCache(hashId)
            const relations = await fetchNodeRelations({
                projectId: projectId.value,
                nodeHashId: hashId,
                isCreateResType: true,
                envIds: Array.isArray(currentNode.value?.envIds)
                    ? currentNode.value.envIds
                    : [],
                store: proxy.$store,
                resType: SERVICE_RESOURCE_TYPE.CREATE,
                force: true
            })
            nodeEnvRelations.value = relations.environments || []
            return nodeEnvRelations.value
        } catch (e) {
            console.error('fetchEnvRelations failed', e)
            nodeEnvRelations.value = []
            return []
        }
    }

    /**
     * thirdPartyAgentDetail 不含 createWorkspaceSource / createWorkspaceId，
     * 需用列表行数据补齐，供详情头部分类标签与概览使用。
     */
    const mergeListNodeMeta = (detail, listNode) => {
        const base = detail && typeof detail === 'object' ? { ...detail } : {}
        if (!listNode || typeof listNode !== 'object') {
            if (!base.nodeHashId && (base.nodeId || nodeHashId.value)) {
                base.nodeHashId = base.nodeId || nodeHashId.value
            }
            if (!base.nodeStatus && base.status) {
                base.nodeStatus = base.status
            }
            return base
        }
        // 列表侧 createWorkspaceSource / nodeStatus 对「已释放/已销毁」标签更可靠，优先保留
        const listStatus = listNode.nodeStatus || listNode.status
        const detailStatus = base.nodeStatus || base.status
        return {
            ...base,
            nodeHashId: base.nodeHashId || listNode.nodeHashId || base.nodeId || nodeHashId.value,
            nodeStatus: listStatus || detailStatus,
            status: listStatus || detailStatus || base.status,
            createWorkspaceSource: listNode.createWorkspaceSource
                || listNode.nodeSource
                || base.createWorkspaceSource,
            // 入驻云桌面四字段以节点列表为准
            createWorkspaceId: listNode.createWorkspaceId || base.createWorkspaceId,
            displayName: listNode.displayName || base.displayName,
            osName: listNode.osName || listNode.os || base.osName || base.os,
            ip: listNode.ip || base.ip,
            // 权限与列表一致（详情对已释放/已销毁常无 canEdit，清理会被误判无权限）
            canEdit: listNode.canEdit ?? base.canEdit,
            canDelete: listNode.canDelete ?? base.canDelete,
            canView: listNode.canView ?? base.canView,
            // 所属环境：详情 thirdPartyAgentDetail.envIds（envHashId 数组）
            envIds: Array.isArray(base.envIds) ? base.envIds : (listNode.envIds || [])
        }
    }

    /**
     * 获取节点详情，并等待所属环境等关联接口完成后再结束 loading
     * @param {Object} [listNode] 列表行，用于补齐创作流节点类型字段
     */
    const fetchNodeDetail = async (listNode) => {
        if (!nodeHashId.value) return
        try {
            nodeDetailLoaded.value = false
            nodeEnvRelations.value = []
            const res = await proxy.$store.dispatch('environment/requestNodeDetail', {
                projectId: projectId.value,
                nodeHashId: nodeHashId.value
            })
            currentNode.value = mergeListNodeMeta(res ?? {}, listNode)
            // 所属环境（及内部 requestEnvDetail）完成后再放开页面 loading
            await fetchEnvRelations()
            nodeDetailLoaded.value = true
            return currentNode.value
        } catch (e) {
            // 已释放/已销毁节点详情接口可能失败，回退列表行以保证分类/中性标签可展示
            if (listNode) {
                currentNode.value = mergeListNodeMeta({}, listNode)
                await fetchEnvRelations()
                nodeDetailLoaded.value = true
                return currentNode.value
            }
            nodeDetailLoaded.value = false
            nodeEnvRelations.value = []
            proxy.$bkMessage({
                message: e.message || proxy.$t('environment.fetchNodeDetailFailed'),
                theme: 'error'
            })
            throw e
        }
    }

    /**
     * 列表数据到达后，补齐创作流字段；若 envIds 变化则补拉所属环境（不打断页面 loading）
     */
    const applyListNodeMeta = async (listNode) => {
        if (!listNode || !currentNode.value) return
        const prevEnvIds = (currentNode.value.envIds || []).join(',')
        currentNode.value = mergeListNodeMeta(currentNode.value, listNode)
        const nextEnvIds = (currentNode.value.envIds || []).join(',')
        if (isCreateResType() && prevEnvIds !== nextEnvIds) {
            await fetchEnvRelations()
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
        nodeEnvRelations,

        // function
        fetchNodeDetail,
        fetchEnvRelations,
        applyListNodeMeta,
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