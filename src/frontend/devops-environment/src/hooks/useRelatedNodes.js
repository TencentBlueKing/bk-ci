import { ref, computed } from 'vue'
import useEnvDetail from './useEnvDetail'
const RELATED_TYPE = {
    STATIC: 'static',
    DYNAMIC: 'dynamic'
}
const globalState = {
    isShow: ref(false),
    isLoading: ref(false),
    relatedType: ref(RELATED_TYPE.STATIC),
    searchKeyword: ref(''),
    selectedNodesList: ref([])
}
export default function useRelatedNodes () {
    const {
        currentEnv
    } = useEnvDetail()
    const currentNodeList = ref([])
    const handleShowRelatedNodes = (list) => {
        currentNodeList.value = list
        console.log(currentNodeList.value, 1)
        globalState.isShow.value = true
    }
    
    const handelCancel = () => {
        globalState.isShow.value = false
    }
    
    const handleSearch = () => {
        console.log('搜索节点:', globalState.searchKeyword.value)
        // TODO: 实现搜索逻辑
    }
    
    const handleSave = () => {
        console.log('保存选择的节点:', globalState.selectedNodesList.value)
        // TODO: 实现保存逻辑
        globalState.isShow.value = false
    }
    
    return {
        isShow: globalState.isShow,
        isLoading: globalState.isLoading,
        relatedType: globalState.relatedType,
        searchKeyword: globalState.searchKeyword,
        selectedNodesList: globalState.selectedNodesList,
        handleShowRelatedNodes,
        handleSearch,
        handleSave,
        handelCancel,
        RELATED_TYPE
    }
}
