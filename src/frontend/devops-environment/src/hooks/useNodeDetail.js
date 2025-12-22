import { computed, ref } from 'vue'
import { convertTime } from '@/utils/util'
import useInstance from './useInstance'

const currentNode = ref({})

export default function useNodeDetail () {
    const { proxy } = useInstance()
    const nodeHashId = computed(() => proxy.$route.query?.nodeHashId)
    const projectId = computed(() => proxy.$route.params?.projectId)
   
    /**
     * 获取节点详情
     */
    const fetchNodeDetail = async () => {
        if (!nodeHashId.value) return
        try {
            const res = await proxy.$store.dispatch('environment/requestNodeDetail', {
                projectId: projectId.value,
                nodeHashId: nodeHashId.value
            })
            currentNode.value = res ?? {}
            return res
        } catch (e) {
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

    return {
        // data
        currentNode,
        projectId,
        nodeHashId,

        // function
        fetchNodeDetail,
        updateDisplayName,
        deleteNode,
        setNodeTag
    }
}