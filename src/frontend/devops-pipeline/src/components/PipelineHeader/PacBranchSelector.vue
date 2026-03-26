<template>
    <div
        v-if="pacEnabled"
        class="pac-branch-selector"
    >
        <div class="branch-selector-wrapper">
            <span class="branch-selector-label">{{ $t('preview.selectBranch') }}</span>
            <bk-select
                v-model="selectedBranch"
                class="branch-selector-dropdown"
                :placeholder="$t('preview.selectBranchPlaceholder')"
                :loading="isLoading"
                searchable
                :clearable="false"
                :remote-method="handleSearch"
                :scroll-loading="scrollLoading"
                @scroll-end="handleScrollEnd"
                @change="handleBranchChange"
                @toggle="handleToggle"
            >
                <bk-option
                    v-for="branch in branchList"
                    :key="branch.name"
                    :id="branch.name"
                    :name="branch.name"
                >
                    <div
                        class="branch-option-item"
                        :title="branch.name"
                    >
                        {{ branch.name }}
                    </div>
                </bk-option>
            </bk-select>
        </div>
        <i
            v-bk-tooltips="$t('preview.runBranchPipeline')"
            class="bk-icon icon-info-circle branch-info-icon"
        />
    </div>
</template>

<script>
    import { defineComponent, ref, computed, watch, onMounted } from 'vue'
    import useInstance from '@/hook/useInstance'

    export default defineComponent({
        name: 'PacBranchSelector',
        emits: ['branch-change'],
        setup (_, { emit }) {
            const { proxy } = useInstance()

            const selectedBranch = ref('')
            const branchList = ref([])
            const isLoading = ref(false)
            const scrollLoading = ref(false)
            const searchKey = ref('')
            const isInitializing = ref(false) // 标记是否正在初始化
            const defaultBranch = ref('') // 默认分支名称
            const pagination = ref({
                page: 1,
                pageSize: 100,
                hasMore: true
            })

            // 计算属性
            const projectId = computed(() => proxy.$route.params.projectId)
            const pipelineInfo = computed(() => proxy.$store.state.atom.pipelineInfo)
            const pacEnabled = computed(() => proxy.$store.getters['atom/pacEnabled'])
            const repoHashId = computed(() => pipelineInfo.value?.yamlInfo?.repoHashId)

            // 获取仓库信息（用于获取默认分支）
            const fetchRepoInfo = async () => {
                if (!repoHashId.value) return
                try {
                    const data = await proxy.$store.dispatch('common/getPACRepoInfo', {
                        projectId: projectId.value,
                        repoHashIdOrName: repoHashId.value
                    })
                    defaultBranch.value = data?.defaultBranch || ''
                } catch (error) {
                    console.error('Failed to fetch repo info:', error)
                }
            }

            // 获取分支列表
            const fetchBranchList = async (isLoadMore = false) => {
                if (!repoHashId.value) return

                if (isLoadMore) {
                    scrollLoading.value = true
                } else {
                    isLoading.value = true
                    pagination.value.page = 1
                    pagination.value.hasMore = true
                }

                try {
                    const data = await proxy.$store.dispatch('common/getPACBranchList', {
                        projectId: projectId.value,
                        repoHashIdOrName: repoHashId.value,
                        search: searchKey.value,
                        page: pagination.value.page,
                        pageSize: pagination.value.pageSize
                    })
                    
                    if (isLoadMore) {
                        branchList.value = [...branchList.value, ...data]
                    } else {
                        branchList.value = data || []
                    }

                    // 判断是否还有更多数据
                    pagination.value.hasMore = data?.length >= pagination.value.pageSize

                    // 如果没有选中分支且有分支数据，则选中默认分支或第一个分支
                    if (!selectedBranch.value && branchList.value.length > 0) {
                        // 优先使用从仓库信息获取的默认分支
                        let targetBranch = null
                        if (defaultBranch.value) {
                            targetBranch = branchList.value.find(b => b.name === defaultBranch.value)
                        }
                        // 如果没找到默认分支，则使用第一个分支
                        if (!targetBranch) {
                            targetBranch = branchList.value[0]
                        }
                        // 设置初始化标记，避免 @change 重复触发
                        isInitializing.value = true
                        selectedBranch.value = targetBranch.name
                        // 触发分支变更事件
                        emit('branch-change', targetBranch.name, targetBranch)
                        // 延迟重置标记，确保 @change 事件已被忽略
                        setTimeout(() => {
                            isInitializing.value = false
                        }, 0)
                    }
                } catch (error) {
                    console.error('Failed to fetch branch list:', error)
                } finally {
                    isLoading.value = false
                    scrollLoading.value = false
                }
            }

            let searchTimer = null

            // 搜索分支（带防抖）
            const handleSearch = (keyword) => {
                searchKey.value = keyword
                branchList.value = []
                if (searchTimer) {
                    clearTimeout(searchTimer)
                }
                searchTimer = setTimeout(() => {
                    fetchBranchList(false)
                }, 300)
            }

            // 滚动加载更多
            const handleScrollEnd = () => {
                if (!pagination.value.hasMore || scrollLoading.value) return
                pagination.value.page++
                fetchBranchList(true)
            }

            // 分支变更（用户手动选择时触发）
            const handleBranchChange = (value) => {
                // 初始化期间，忽略 @change 事件（避免重复触发）
                if (isInitializing.value) return
                const branchInfo = branchList.value.find(b => b.name === value)
                emit('branch-change', value, branchInfo)
            }

            // 下拉框展开/收起
            const handleToggle = (isOpen) => {
                if (isOpen && branchList.value.length === 0) {
                    fetchBranchList(false)
                }
            }

            // 监听 repoHashId 变化，重新获取分支列表
            watch(repoHashId, async (newVal) => {
                if (newVal) {
                    selectedBranch.value = ''
                    defaultBranch.value = ''
                    await fetchRepoInfo()
                    fetchBranchList(false)
                }
            })

            // 组件挂载时获取分支列表
            onMounted(async () => {
                if (repoHashId.value) {
                    await fetchRepoInfo()
                    fetchBranchList(false)
                }
            })

            return {
                selectedBranch,
                branchList,
                isLoading,
                scrollLoading,
                pacEnabled,
                repoHashId,
                handleSearch,
                handleScrollEnd,
                handleBranchChange,
                handleToggle
            }
        }
    })
</script>

<style lang="scss">
.pac-branch-selector {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-left: 32px;

    .branch-selector-wrapper {
        display: flex;
        align-items: center;
    }

    .branch-selector-label {
        display: flex;
        align-items: center;
        height: 32px;
        padding: 0 12px;
        color: #63656E;
        font-size: 12px;
        white-space: nowrap;
        background-color: #FAFBFD;
        border: 1px solid #C4C6CC;
        border-right: none;
        border-radius: 2px 0 0 2px;
    }

    .branch-selector-dropdown {
        width: 300px;
        
        &.bk-select {
            .bk-select-trigger {
                .bk-input--text {
                    border-radius: 0 2px 2px 0;
                }
            }
        }

    }
    
    .branch-info-icon {
        font-size: 14px;
        color: #979BA5;
        cursor: pointer;
        
        &:hover {
            color: #3A84FF;
        }
    }
}

.branch-option-item {
    width: 100%;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
}
</style>
