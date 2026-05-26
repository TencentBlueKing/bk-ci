import { ref, computed } from 'vue'
import useEnvDetail from './useEnvDetail'
import useInstance from './useInstance'

const RELATED_TYPE = {
    NODE: 'static',
    TAG: 'dynamic'
}
const isShow = ref(false)
const tagList = ref([])
const availableLabelKeys = ref([])
const tagValuesMap = ref({})

export default function useRelatedNodes () {
    const { proxy } = useInstance()
    const {
        currentEnv
    } = useEnvDetail()
    const isLoading = ref(false)
    const relatedType = ref(RELATED_TYPE[currentEnv.value?.envNodeType] || RELATED_TYPE.NODE)
    const projectId = computed(() => proxy.$route.params.projectId)
    const envHashId = computed(() => proxy.$route.params.envId)
    const handleShowRelatedNodes = () => {
        isShow.value = true
    }
    const nodeList = ref([])
    
    // 根据标签键ID获取对应的标签值选项
    const getLabelValues = (tagKeyId) => {
        return tagValuesMap.value[tagKeyId] || []
    }
    
    // 获取标签列表
    const fetchTagList = async () => {
        try {
            const res = await proxy.$store.dispatch('environment/requestNodeTagList', projectId.value)
            // 保存原始数据
            tagList.value = res ?? []
            
            // 转换为标签键选项列表
            availableLabelKeys.value = tagList.value.map(tag => ({
                id: tag.tagKeyId,
                name: tag.tagKeyName
            }))
            
            // 构建标签值映射表
            const valuesMap = {}
            tagList.value.forEach(tag => {
                if (tag.tagValues && tag.tagValues.length > 0) {
                    valuesMap[tag.tagKeyId] = tag.tagValues.map(val => ({
                        id: val.tagValueId,
                        name: val.tagValueName
                    }))
                }
            })
            tagValuesMap.value = valuesMap
        } catch (error) {
            console.error('Failed to fetch tag list:', error)
        }
    }
    const requestNodeList = async (params, tags = []) => {
        try {
            isLoading.value = true
            const res = await proxy.$store.dispatch('environment/requestNodeList', {
                projectId: projectId.value,
                envHashId: envHashId.value,
                params,
                tags
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
            return res
        } catch (error) {
            throw error
        }
    }
    return {
        // data
        isShow,
        nodeList,
        isLoading,
        relatedType,
        RELATED_TYPE,
        tagList,
        availableLabelKeys,
        
        // function
        relateNodes,
        requestNodeList,
        handleCloseDialog,
        handleShowRelatedNodes,
        fetchTagList,
        getLabelValues
    }
}
