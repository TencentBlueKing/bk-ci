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
                enable-scroll-load
                :scroll-loading="bottomLoadingOptions"
                :remote-method="handleSearch"
                @change="handleBranchChange"
                @toggle="handleToggle"
                @scroll-end="loadMore"
            >
                <div
                    slot="trigger"
                    class="pac-version-dropmenu-trigger"
                >
                    <p class="pipeline-version-name">
                        <span v-bk-overflow-tips>
                            {{ selectedBranch }}
                        </span>
                        <i
                            v-if="currentBranch.versionStatus === 'RELEASED'"
                            class="pipeline-release-version-tag"
                        >
                            {{ $t('latest') }}
                        </i>
                        <i
                            v-else-if="currentBranch.defaultBranch"
                            class="pipeline-default-version-tag"
                        >
                            {{ $t('preview.defaultBranch') }}
                        </i>
                    </p>
                    <i
                        v-if="isLoading"
                        class="devops-icon icon-circle-2-1 spin-icon"
                    />
                    <i
                        v-else
                        class="bk-icon icon-angle-down"
                    />
                </div>
                <bk-option-group
                    v-for="group in branchGroupList"
                    :key="group.name"
                    :name="group.name"
                >
                    <bk-option
                        v-for="branch in group.children"
                        :key="branch.name"
                        :id="branch.name"
                        :name="branch.name"
                    >
                        <div
                            class="branch-option-item"
                            :title="branch.name"
                        >
                            <span>{{ branch.name }}</span>
                            <i
                                v-if="branch.versionStatus === 'RELEASED'"
                                class="pipeline-release-version-tag"
                            >
                                {{ $t('latest') }}
                            </i>
                            <i
                                v-else-if="branch.defaultBranch"
                                class="pipeline-default-version-tag"
                            >
                                {{ $t('preview.defaultBranch') }}
                            </i>
                        </div>
                    </bk-option>
                </bk-option-group>
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
            const hasNext = ref(true)
            const currentPage = ref(1)
            const pageSize = 20
            const bottomLoadingOptions = ref({
                size: 'small',
                isLoading: false
            })

            // 计算属性
            const projectId = computed(() => proxy.$route.params.projectId)
            const pipelineId = computed(() => proxy.$route.params.pipelineId)
            const pacEnabled = computed(() => proxy.$store.getters['atom/pacEnabled'])

            // 当前选中的分支对象
            const currentBranch = computed(() => {
                return branchList.value.find(b => b.name === selectedBranch.value) || {}
            })

            // 分支分组列表
            const branchGroupList = computed(() => {
                const defaultBranches = branchList.value.filter(b => b.versionStatus === 'RELEASED')
                const otherBranches = branchList.value.filter(b => b.versionStatus !== 'RELEASED')
                return [
                    { name: proxy.$t('preview.latestVersion'), children: defaultBranches },
                    { name: proxy.$t('preview.repoBranch'), children: otherBranches }
                ].filter(group => group.children.length > 0)
            })

            // 获取分支列表
            const fetchBranchList = async (page = 1) => {
                if (!pipelineId.value) return

                if (page > 1 && !hasNext.value) return

                if (page === 1) {
                    isLoading.value = true
                } else {
                    bottomLoadingOptions.value.isLoading = true
                }
                const isInitialLoad = isFirstLoad.value
                isFirstLoad.value = false

                try {
                    const data = await proxy.$store.dispatch('common/getPACBranchList', {
                        projectId: projectId.value,
                        pipelineId: pipelineId.value,
                        search: searchKey.value,
                        page,
                        pageSize
                    })
                    
                    currentPage.value = page
                    // 判断是否还有更多数据
                    hasNext.value = Array.isArray(data) ? data.length >= pageSize : false

                    if (page === 1) {
                        branchList.value = data || []
                    } else {
                        branchList.value.push(...(data || []))
                    }

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
                    bottomLoadingOptions.value.isLoading = false
                }
            }

            // 滚动加载更多
            const loadMore = () => {
                fetchBranchList(currentPage.value + 1)
            }

            let searchTimer = null

            // 搜索分支（带防抖）
            const handleSearch = (keyword) => {
                searchKey.value = keyword
                branchList.value = []
                hasNext.value = true
                currentPage.value = 1
                if (searchTimer) {
                    clearTimeout(searchTimer)
                }
                searchTimer = setTimeout(() => {
                    fetchBranchList(1)
                }, 300)
            }

            // 分支变更
            const handleBranchChange = (value) => {
                if (isInitializing.value) return
                const branchInfo = branchList.value.find(b => b.name === value)
                // 将分支名保存到 sessionStorage，用于 PAC 分支选择器匹配
                const cacheKey = `pac_branch_${projectId.value}_${pipelineId.value}`
                sessionStorage.setItem(cacheKey, value)
                emit('branch-change', value, branchInfo)
            }

            // 下拉框展开/收起
            const handleToggle = (isOpen) => {
                if (isOpen && branchList.value.length === 0) {
                    hasNext.value = true
                    currentPage.value = 1
                    fetchBranchList(1)
                }
            }

            // 监听 pipelineId 变化，重新获取分支列表
            watch(pipelineId, (newVal) => {
                if (newVal) {
                    selectedBranch.value = ''
                    hasNext.value = true
                    currentPage.value = 1
                    fetchBranchList(1)
                }
            })

            onMounted(() => {
                // 优先从 sessionStorage 读取缓存的分支名，其次从 URL 参数读取
                const cacheKey = `pac_branch_${projectId.value}_${pipelineId.value}`
                const cachedBranch = sessionStorage.getItem(cacheKey)
                initialVersionName.value = cachedBranch || proxy.$route.query.versionName || ''
                
                if (pipelineId.value) {
                    fetchBranchList(1)
                }
            })

            return {
                selectedBranch,
                branchList,
                branchGroupList,
                currentBranch,
                isLoading,
                pacEnabled,
                pipelineId,
                bottomLoadingOptions,
                handleSearch,
                handleBranchChange,
                handleToggle,
                loadMore
            }
        }
    })
</script>

<style lang="scss">
@import "@/scss/mixins/ellipsis";
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
        width: 400px;
        
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
}
.pipeline-default-version-tag,
.pipeline-release-version-tag {
    display: inline-flex;
    align-items: center;
    background: #E4FAF0 ;
    border: 1px solid #A5E0C6;
    color: #14A568;
    padding: 0 4px;
    border-radius: 2px;
    font-size: 10px;
    height: 16px;
    line-height: 16px;
    align-self: center;
    font-style: normal;
    flex-shrink: 0;
}
.pipeline-default-version-tag {
    color: #3a84ff;
    background: #e8f0fc;
    border: 1px solid #9bc1fd;
}
.pac-version-dropmenu-trigger {
    display: flex;
    align-items: center;
    gap: 6px;
    height: 30px;
    line-height: 30px;
    padding: 0 8px;
    min-width: 200px;
    width: 100%;
    cursor: pointer;
    overflow: hidden;
   
    .pipeline-version-name {
        display: flex;
        flex: 1;
        min-width: 0;
        overflow: hidden;
        > span {
            line-height: 24px;
            margin-right: 5px;
            @include ellipsis();
        }
    }
    .icon-circle-2-1,
    .icon-angle-down {
        flex-shrink: 0;
    }
    .icon-circle-2-1 {
        width: 20px;
        height: 20px;
        display: flex;
        align-items: center;
        justify-content: center;
    }
    .icon-angle-down {
        transition: transform 0.3s;
        font-size: 20px;
    }
}
</style>
