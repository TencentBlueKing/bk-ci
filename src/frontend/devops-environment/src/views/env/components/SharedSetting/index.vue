<template>
    <div class="shared-setting-container">
        <bk-alert
            theme="info"
            :title="$t('environment.sharedSettingTips')"
            :closable="false"
            class="env-alert"
        />

        <div class="operation-area">
            <bk-button
                theme="primary"
                @click="handleAddProject"
            >
                {{ $t('environment.relatedProject') }}
            </bk-button>

            <search-select
                ref="searchSelect"
                class="search-input"
                v-model="searchValue"
                :data="searchList"
                clearable
                :show-condition="false"
                :placeholder="filterTips"
            />
        </div>

        <bk-table
            v-bkloading="{ isLoading }"
            :data="relatedProjectList"
            :max-height="tableMaxHeight"
            :pagination="pagination"
            @page-change="handlePageChange"
            @page-limit-change="handlePageLimitChange"
        >
            <bk-table-column
                :label="$t('environment.project')"
                prop="name"
                min-width="200"
            />
            <bk-table-column
                :label="$t('environment.operateUser')"
                prop="creator"
                min-width="120"
            />
            <bk-table-column
                :label="$t('environment.operateTime')"
                prop="updateTime"
                min-width="160"
            />
            <bk-table-column
                :label="$t('environment.operation')"
                fixed="right"
                min-width="120"
            >
                <template slot-scope="{ row }">
                    <bk-button
                        text
                        theme="primary"
                        @click="handleDelete(row)"
                    >
                        {{ $t('environment.delete') }}
                    </bk-button>
                </template>
            </bk-table-column>
        </bk-table>
        <select-env-share-dialog
            :show-project-dialog="showProjectDialog"
            :project-id="projectId"
            :env-hash-id="envHashId"
            @confirm="handleShareEnv"
            @cancel="handleCancelShare"
        />
    </div>
</template>

<script>
    import { ref, computed, watch, onMounted, onBeforeUnmount, nextTick } from 'vue'
    import useInstance from '@/hooks/useInstance'
    import usePagination from '@/hooks/usePagination'
    import useEnvDetail from '@/hooks/useEnvDetail'
    import SearchSelect from '@blueking/search-select'
    import '@blueking/search-select/dist/styles/index.css'
    import SelectEnvShareDialog from './SelectEnvShareDialog'
    
    export default {
        name: 'SharedSetting',
        components: {
            SearchSelect,
            SelectEnvShareDialog
        },
        setup () {
            const { proxy } = useInstance()
            const {
                pagination,
                resetPage,
                resetPagination,
                pageChange,
                pageLimitChange,
                updatePagination
            } = usePagination()
            const {
                envHashId,
                projectId,
                relatedProjectList,
                fetchEnvRelatedProject
            } = useEnvDetail()
            const isLoading = ref(false)
            const tableMaxHeight = ref(565)
            const searchValue = ref([])
            const showProjectDialog = ref(false)
            const searchList = computed(() => [
                {
                    name: proxy.$t('environment.project'),
                    id: 'name'
                },
                {
                    name: proxy.$t('environment.operateUser'),
                    id: 'operator'
                }
            ])
            
            const filterTips = computed(() => {
                return searchList.value.map(item => item.name).join(' / ')
            })
            
            // 获取项目列表参数（分页，过滤选项）
            const searchQuery = computed(() => {
                return searchValue.value.reduce((acc, item) => {
                    acc[item.id] = item.values.map(value => value.id).join(',')
                    return acc
                }, {
                    page: pagination.value.current,
                    pageSize: pagination.value.limit
                })
            })
            
            // 用于标记是否是 envHashId 变化导致的搜索值清空
            const isEnvIdChanging = ref(false)
            
            // 监听搜索值变化
            watch(() => searchValue.value, () => {
                // 如果是 envHashId 变化导致的清空，不触发搜索
                if (isEnvIdChanging.value) return
                resetPage()
                fetchData()
            })
            
            // 监听 envHashId 变化
            watch(() => envHashId.value, () => {
                resetPagination() // 重置分页配置
                isEnvIdChanging.value = true
                searchValue.value = []
                fetchData()
                nextTick(() => {
                    isEnvIdChanging.value = false
                })
            })
            
            // 动态计算表格最大高度
            const calculateTableHeight = () => {
                // 获取容器高度
                const container = document.querySelector('.shared-setting-container')
                if (container) {
                    const containerHeight = container.clientHeight
                    // 减去头部高度（按钮和搜索框区域）和表格上边距
                    // 头部高度约 32px（按钮高度）+ 20px（margin-top）+ 52px (bk-alter-32px, margin-bottom-20px) = 104px
                    const headerHeight = 104
                    const calculatedHeight = containerHeight - headerHeight
                    // 确保计算出的高度大于最小值
                    if (calculatedHeight > 200) {
                        tableMaxHeight.value = calculatedHeight
                    }
                }
            }
            
            // 窗口大小变化时重新计算
            const handleResize = () => {
                calculateTableHeight()
            }
            
            // 获取项目列表
            const fetchData = async () => {
                try {
                    isLoading.value = true
                    const res = await fetchEnvRelatedProject(searchQuery.value)
                    
                    updatePagination({
                        count: res.count,
                        page: res.page,
                        pageSize: res.pageSize
                    })
                } catch (err) {
                    console.error('获取共享项目列表失败:', err)
                    proxy.$bkMessage({
                        theme: 'error',
                        message: err.message
                    })
                } finally {
                    isLoading.value = false
                }
            }
            
            // 添加项目
            const handleAddProject = () => {
                showProjectDialog.value = true
            }
            
            
            // 删除项目
            const handleDelete = (row) => {
                proxy.$bkInfo({
                    title: proxy.$t('environment.removeShareProjectConfirm', [row.name]),
                    confirmFn: async () => {
                        try {
                            await proxy.$store.dispatch('environment/removeProjectShare', {
                                envHashId: envHashId.value,
                                projectId: projectId.value,
                                sharedProjectId: row.gitProjectId
                            })
                            proxy.$bkMessage({
                                theme: 'success',
                                message: proxy.$t('environment.successfullyDeleted')
                            })
                            resetPage()
                            fetchData()
                        } catch (err) {
                            console.error('删除项目失败:', err)
                            proxy.$bkMessage({
                                theme: 'error',
                                message: err.message || proxy.$t('environment.deleteFailed')
                            })
                        }
                    }
                })
            }

            const handlePageChange = (page) => {
                pageChange(page)
                fetchData()
            }

            const handlePageLimitChange = (limit) => {
                pageChange(1)
                pageLimitChange(limit)
                fetchData()
            }

            const handleShareEnv = async (selection) => {
                try {
                    const sharedProjects = selection.map(item => ({
                        projectId: item.projectId,
                        name: item.name,
                        type: 'PROJECT',
                        creator: item.creator,
                        createTime: item.createTime,
                        updateTime: item.updateTime
                    }))
                    await proxy.$store.dispatch('environment/shareEnv', {
                        projectId: projectId.value,
                        envHashId: envHashId.value,
                        body: {
                            sharedProjects
                        }
                    })

                    proxy.$bkMessage({
                        theme: 'success',
                        message: proxy.$t('environment.shareEnvSuc')
                    })
                    showProjectDialog.value = false
                    resetPage()
                    fetchData()
                } catch (e) {
                    proxy.$bkMessage({
                        theme: 'error',
                        message: e.message || e
                    })
                    throw e
                }
            }

            const handleCancelShare = () => {
                showProjectDialog.value = false
            }
            
            onMounted(() => {
                fetchData()
                calculateTableHeight()
                window.addEventListener('resize', handleResize)
            })
            
            onBeforeUnmount(() => {
                window.removeEventListener('resize', handleResize)
            })
            
            return {
                // data
                isLoading,
                searchValue,
                searchList,
                filterTips,
                relatedProjectList,
                pagination,
                tableMaxHeight,
                projectId,
                envHashId,
                showProjectDialog,
                
                // function
                resetPage,
                resetPagination,
                handlePageChange,
                handlePageLimitChange,
                handleAddProject,
                handleDelete,
                handleShareEnv,
                handleCancelShare
            }
        }
    }
</script>

<style lang="scss" scoped>
.shared-setting-container {
    height: calc(100% - 90px);
    .search-input {
        width: 480px;
        background: white;
        ::placeholder {
            color: #c4c6cc;
        }
    }
    .env-alert {
        margin-bottom: 20px;
    }

    .operation-area {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 20px;

        .search-select {
            width: 400px;
        }
    }
}
</style>
