import { computed, ref } from 'vue'
import { convertTime } from '@/utils/util'
import useInstance from './useInstance'

const currentEnv = ref({})

const envDetailLoaded = ref(false) // 环境详情是否加载完成
export default function useEnvDetail () {
    const { proxy } = useInstance()
    const envNodeList = ref([]) // 节点列表
    const envHashId = computed(() => proxy.$route.params?.envId)
    const projectId = computed(() => proxy.$route.params?.projectId)
    const envList = computed(() => proxy.$store.getters['environment/getEnvList'] || [])
    const isBuiltInEnv = computed(() => envHashId.value?.startsWith('-'))
    const isPersonalProject = computed(() => {
        // 个人项目
        const projectList = proxy.$store.state.projectList || []
        const curProject = projectList.find(p => p.projectCode === projectId.value)
        return curProject?.projectScope === 1 ?? false
    })
    
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
            envDetailLoaded.value = false
            const res = await proxy.$store.dispatch('environment/requestEnvDetail', {
                projectId: projectId.value,
                envHashId: envHashId.value
            })
            currentEnv.value = res ?? {}
            envDetailLoaded.value = true
            return res
        } catch (e) {
            envDetailLoaded.value = false
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
    const toggleEnableNode = async (nodeHashId, enableNode, reason) => {
        if (!envHashId.value) return
        try {
            const res = await proxy.$store.dispatch('environment/enableNode', {
                projectId: projectId.value,
                envHashId: envHashId.value,
                nodeHashId,
                enableNode,
                reason
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

    const setEnvDetailLoaded = (value) => {
        envDetailLoaded.value = value
    }

    // 操作日志列表
    const operateLogList = ref([])
    const operateLogPagination = ref({
        count: 0,
        page: 1,
        pageSize: 10,
        totalPages: 0
    })
    const fetchOperateLogList = async (params = {}) => {
        if (!envHashId.value) return
        try {
            const res = await proxy.$store.dispatch('environment/requestOperateLogList', {
                params: {
                    projectId: projectId.value,
                    envHashId: envHashId.value,
                    page: params.page || 1,
                    pageSize: params.pageSize || 10,
                    ...(params.operator ? { operator: params.operator } : {})
                }
            })
            operateLogList.value = res?.records || []
            operateLogPagination.value = {
                count: res?.count || 0,
                page: res?.page || 1,
                pageSize: res?.pageSize || 10,
                totalPages: res?.totalPages || 0
            }
            return res
        } catch (e) {
            throw e
        }
    }

    // 项目成员列表
    const projectMemberList = ref([])
    const memberPage = ref(1)
    const memberPageSize = ref(15)
    const memberHasMore = ref(true)
    const memberLoading = ref(false)

    const fetchProjectMembers = async (isLoadMore = false) => {
        if (!projectId.value) return
        if (memberLoading.value) return
        if (isLoadMore && !memberHasMore.value) return

        memberLoading.value = true
        try {
            if (!isLoadMore) {
                memberPage.value = 1
                memberHasMore.value = true
            }
            const res = await proxy.$store.dispatch('environment/requestProjectMembers', {
                projectId: projectId.value,
                page: memberPage.value,
                pageSize: memberPageSize.value
            })
            const rawRecords = res.records || []
            const records = rawRecords.filter(item => !item.departed)
            if (isLoadMore) {
                projectMemberList.value = [...projectMemberList.value, ...records]
            } else {
                projectMemberList.value = records
            }
            // 使用未过滤的记录数与总记录数比较，避免因过滤departed导致判断错误
            const totalLoaded = isLoadMore
                ? (memberPage.value - 1) * memberPageSize.value + rawRecords.length
                : rawRecords.length
            memberHasMore.value = totalLoaded < (res.count || 0)
            if (memberHasMore.value) {
                memberPage.value += 1
            }
            return res
        } catch (e) {
            throw e
        } finally {
            memberLoading.value = false
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
        envDetailLoaded,
        envList,
        isBuiltInEnv,
        isPersonalProject,
        operateLogList,
        operateLogPagination,
        projectMemberList,
        memberHasMore,
        memberLoading,

        // function
        fetchEnvNodeList,
        requestRemoveNode,
        fetchEnvDetail,
        fetchEnvParamsList,
        updateEnvDetail,
        fetchEnvRelatedProject,
        toggleEnableNode,
        setEnvDetailLoaded,
        fetchOperateLogList,
        fetchProjectMembers
    }
}