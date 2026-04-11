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
                        <span>{{ branch.name }}</span>
                        <bk-tag
                            v-if="branch.versionStatus === 'RELEASED'"
                            theme="success"
                            class="branch-tag"
                        >
                            {{ $t('preview.releasedVersion') }}
                        </bk-tag>
                        <bk-tag
                            v-else-if="branch.defaultBranch"
                            theme="info"
                            class="branch-tag"
                        >
                            {{ $t('preview.defaultBranch') }}
                        </bk-tag>
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
    import { PAC_BRANCH_INIT_DONE, bus } from '@/utils/bus'

    export default defineComponent({
        name: 'PacBranchSelector',
        emits: ['branch-change'],
        setup (_, { emit }) {
            const { proxy } = useInstance()

            const selectedBranch = ref('')
            const branchList = ref([])
            const isLoading = ref(false)
            const searchKey = ref('')
            const isInitializing = ref(false) // 标记是否正在初始化
            const initialVersionName = ref('')
            const isFirstLoad = ref(true) // 标记是否首次加载

            // 计算属性
            const projectId = computed(() => proxy.$route.params.projectId)
            const pipelineId = computed(() => proxy.$route.params.pipelineId)
            const pacEnabled = computed(() => proxy.$store.getters['atom/pacEnabled'])

            // 获取分支列表
            const fetchBranchList = async () => {
                if (!pipelineId.value) return

                isLoading.value = true
                const isInitialLoad = isFirstLoad.value
                isFirstLoad.value = false

                try {
                    const data = await proxy.$store.dispatch('common/getPACBranchList', {
                        projectId: projectId.value,
                        pipelineId: pipelineId.value,
                        search: searchKey.value
                    })
                    
                    branchList.value = data || []

                    // 如果没有选中分支且有分支数据，则选中对应分支
                    if (!selectedBranch.value && branchList.value.length > 0) {
                        let targetBranch = null
                        
                        // 优先匹配初始化时保存的 versionName
                        if (initialVersionName.value) {
                            targetBranch = branchList.value.find(b => b.name === initialVersionName.value)
                        }
                        // 如果没有找到，则匹配 RELEASED 版本
                        if (!targetBranch) {
                            targetBranch = branchList.value.find(b => b.versionStatus === 'RELEASED')
                        }
                        // 如果还是没有找到，则选择第一个分支
                        if (!targetBranch) {
                            targetBranch = branchList.value[0]
                        }
                        
                        if (targetBranch) {
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
                    } else if (isInitialLoad && branchList.value.length === 0) {
                        // 首次加载且分支列表为空，通知 preview 组件初始化完成（无分支可选）
                        bus.$emit(PAC_BRANCH_INIT_DONE, { hasBranch: false })
                    }
                } catch (error) {
                    console.error('Failed to fetch branch list:', error)
                    // 首次加载失败时，通知 preview 组件初始化完成（失败）
                    if (isInitialLoad) {
                        bus.$emit(PAC_BRANCH_INIT_DONE, { hasBranch: false, error })
                    }
                } finally {
                    isLoading.value = false
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
                    fetchBranchList()
                }, 300)
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
                    fetchBranchList()
                }
            }

            // 监听 pipelineId 变化，重新获取分支列表
            watch(pipelineId, (newVal) => {
                if (newVal) {
                    selectedBranch.value = ''
                    fetchBranchList()
                }
            })

            onMounted(() => {
                // 优先从 sessionStorage 读取缓存的分支名，其次从 URL 参数读取
                const cacheKey = `pac_branch_${projectId.value}_${pipelineId.value}`
                const cachedBranch = sessionStorage.getItem(cacheKey)
                initialVersionName.value = cachedBranch || proxy.$route.query.versionName || ''
                
                if (pipelineId.value) {
                    fetchBranchList()
                }
            })

            return {
                selectedBranch,
                branchList,
                isLoading,
                pacEnabled,
                pipelineId,
                handleSearch,
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
    display: flex;
    align-items: center;
    justify-content: space-between;
    width: 100%;
    white-space: nowrap;
    overflow: hidden;
    
    > span {
        flex: 1;
        overflow: hidden;
        text-overflow: ellipsis;
    }

    .branch-tag {
        flex-shrink: 0;
        margin-left: 8px;
        height: 20px;
        line-height: 18px;
        font-size: 12px;
    }
}
</style>
