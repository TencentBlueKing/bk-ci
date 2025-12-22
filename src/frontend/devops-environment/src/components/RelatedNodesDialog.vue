<template>
    <bk-dialog
        :value="isShow"
        width="1080"
        ext-cls="related-nodes-dialog"
        :position="dialogConfigs"
        scrollable
        :quick-close="false"
        :close-icon="false"
        :draggable="false"
    >
        <div class="dialog-content">
            <div class="left-section">
                <div class="title-section">
                    {{ currentEnv.name }} - {{ $t('environment.relatedNodes') }}
                </div>
                <!-- 关联策略 -->
                <div class="form-section">
                    <div class="form-label">{{ $t('environment.relatedStrategies') }}</div>
                    <bk-radio-group v-model="relatedType">
                        <bk-radio
                            class="mr10"
                            value="static"
                        >
                            {{ $t('environment.staticRelated') }}
                        </bk-radio>
                        <bk-radio value="dynamic">{{ $t('environment.dynamicRelated') }}</bk-radio>
                    </bk-radio-group>
                </div>

                <bk-input
                    v-model="searchKeyword"
                    placeholder="搜索节点"
                    right-icon="bk-icon icon-search"
                    @enter="handleSearch"
                    clearable
                />

                <div class="node-list-section">
                    <!-- 顶部操作栏 -->
                    <div class="list-header">
                        <bk-button
                            text
                            class="btn-text"
                            size="small"
                            @click="handleToggleSelectAll"
                        >
                            全选 ({{ nodeList.length }})
                        </bk-button>
                        <bk-button
                            text
                            class="btn-text"
                            size="small"
                            @click="handleClearSelection"
                        >
                            清空
                        </bk-button>
                    </div>

                    <!-- 节点列表 -->
                    <div
                        ref="nodeListContainer"
                        class="node-list-container"
                        @scroll="handleScroll"
                    >
                        <div
                            v-for="node in displayNodeList"
                            :key="node.id"
                            class="node-item"
                            @click="handleNodeClick(node)"
                        >
                            <bk-checkbox
                                :value="isNodeSelected(node.id)"
                            />
                            <div class="node-content">
                                <div class="node-main-info">
                                    <span class="node-name">{{ node.name }}</span>
                                    <span
                                        class="node-status"
                                        :class="`status-${node.status?.toLowerCase()}`"
                                    >
                                        <i class="bk-icon icon-circle"></i>
                                        {{ node.statusText || '正常' }}
                                    </span>
                                </div>
                                <div class="node-sub-info">
                                    <span class="node-ip">{{ node.ip }}</span>
                                    <span
                                        v-if="node.agentId"
                                        class="node-agent"
                                    >{{ node.agentId }}</span>
                                </div>
                            </div>
                        </div>

                        <!-- 加载更多 -->
                        <div
                            v-if="hasMore"
                            class="loading-more"
                        >
                            <bk-loading
                                :loading="isLoadingMore"
                                size="small"
                            >
                                {{ isLoadingMore ? '加载中...' : '滚动加载更多' }}
                            </bk-loading>
                        </div>

                        <!-- 无更多数据 -->
                        <div
                            v-if="!hasMore && displayNodeList.length > 0"
                            class="no-more"
                        >
                            没有更多数据了
                        </div>

                        <!-- 空状态 -->
                        <div
                            v-if="displayNodeList.length === 0 && !isLoading"
                            class="empty-state"
                        >
                            <i class="bk-icon icon-empty"></i>
                            <p>暂无节点数据</p>
                        </div>
                    </div>
                </div>
            </div>

            <!-- 右侧预览区域 -->
            <div class="right-section">
                <div class="preview-header">
                    <span class="title">结果预览</span>
                </div>
                
                <div class="preview-stats">
                    <span>共 <span class="count total-count">{{ totalCount }}</span> 个，</span>
                    <span>新增 <span class="count new-count">{{ newCount }}</span> 个，</span>
                    <span>移除 <span class="count remove-count">{{ removeCount }}</span> 个</span>
                </div>

                <div class="preview-content">
                    <div
                        v-if="selectedNodesList.length > 0"
                        class="selected-nodes"
                    >
                        <div
                            v-for="node in selectedNodesList"
                            :key="node.id"
                            class="selected-node-item"
                        >
                            <span class="node-name">{{ node.name }}</span>
                            <span class="node-group">{{ node.group }}</span>
                        </div>
                    </div>
                    
                    <div
                        v-else
                        class="empty-state"
                    >
                        <i class="bk-icon icon-empty"></i>
                        <p>暂无选择的节点</p>
                    </div>
                </div>
            </div>
        </div>
        
        <div
            slot="footer"
            class="dialog-footer"
        >
            <bk-button
                theme="primary"
                @click="handleSave"
            >
                保存
            </bk-button>
            <bk-button @click="handelCancel">取消</bk-button>
        </div>
    </bk-dialog>
</template>

<script>
    import { ref, computed, onMounted, nextTick } from 'vue'
    import useRelatedNodes from '@/hooks/useRelatedNodes'
    import useEnvDetail from '@/hooks/useEnvDetail'
    export default {
        name: 'RelatedNodes',
        setup () {
            const {
                isShow,
                isLoading,
                relatedType,
                searchKeyword,
                selectedNodesList,
                handleSearch,
                handleSave,
                handelCancel,
                RELATED_TYPE
            } = useRelatedNodes()
            const {
                currentEnv
            } = useEnvDetail()

            // 节点列表容器引用
            const nodeListContainer = ref(null)
            
            // 分页相关
            const currentPage = ref(1)
            const pageSize = ref(20)
            const isLoadingMore = ref(false)
            const hasMore = ref(true)

            const dialogConfigs = {
                top: '120'
            }

            // 模拟完整节点数据（实际应该从接口获取）
            const nodeList = ref([
                { id: 'node-1', name: 'devops1', ip: '9.135.88.82', agentId: 'ins-0svirhrz47yp76qu', status: 'NORMAL', statusText: '正常' },
                { id: 'node-2', name: 'devops2', ip: '9.135.88.82', agentId: 'ins-0svirhrz47yp76qu', status: 'NORMAL', statusText: '正常' },
                { id: 'node-3', name: 'bkdevops-dev-console-1', ip: '9.135.88.82', agentId: 'ins-0svirhrz47yp76qu', status: 'NORMAL', statusText: '正常' },
                { id: 'node-4', name: 'bkdevops-dev-console-2', ip: '9.135.88.82', agentId: 'ins-0svirhrz47yp76qu', status: 'NORMAL', statusText: '正常' },
                { id: 'node-5', name: 'bkdevops-dev-console-3', ip: '9.135.88.82', agentId: 'ins-0svirhrz47yp76qu', status: 'NORMAL', statusText: '正常' },
                { id: 'node-6', name: 'bkdevops-dev-console-4', ip: '9.135.88.82', agentId: 'ins-0svirhrz47yp76qu', status: 'NORMAL', statusText: '正常' },
                { id: 'node-7', name: 'TENCENT64site', ip: '9.135.88.82', agentId: 'ins-0svirhrz47yp76qu', status: 'NORMAL', statusText: '正常' },
                { id: 'node-8', name: 'bkdevops-dev-console-5', ip: '9.135.88.82', agentId: 'ins-0svirhrz47yp76qu', status: 'NORMAL', statusText: '正常' },
                { id: 'node-9', name: 'bkdevops-dev-console-6', ip: '9.135.88.82', agentId: 'ins-0svirhrz47yp76qu', status: 'NORMAL', statusText: '正常' }
            ])

            // 当前显示的节点列表（分页后）
            const displayNodeList = ref([])

            // 选中的节点 ID 集合
            const selectedNodeIds = computed(() => new Set(selectedNodesList.value.map(node => node.id)))

            // 是否全选
            const isAllSelected = computed(() => {
                return displayNodeList.value.length > 0 && displayNodeList.value.every(node => selectedNodeIds.value.has(node.id))
            })

            // 是否半选状态
            const isIndeterminate = computed(() => {
                const selectedCount = displayNodeList.value.filter(node => selectedNodeIds.value.has(node.id)).length
                return selectedCount > 0 && selectedCount < displayNodeList.value.length
            })

            // 统计数据
            const totalCount = computed(() => selectedNodesList.value.length)
            const newCount = ref(0)
            const removeCount = ref(0)

            // 判断节点是否被选中
            const isNodeSelected = (nodeId) => {
                return selectedNodeIds.value.has(nodeId)
            }

            // 处理节点点击
            const handleNodeClick = (node) => {
                const isSelected = isNodeSelected(node.id)
                if (isSelected) {
                    // 移除节点
                    const index = selectedNodesList.value.findIndex(item => item.id === node.id)
                    if (index > -1) {
                        selectedNodesList.value.splice(index, 1)
                    }
                } else {
                    // 添加节点
                    selectedNodesList.value.push({
                        id: node.id,
                        name: node.name,
                        ip: node.ip,
                        agentId: node.agentId,
                        status: node.status
                    })
                }
            }

            // 切换全选/取消全选
            const handleToggleSelectAll = () => {
                if (isAllSelected.value) {
                    // 如果已经全选，则取消全选当前显示的节点
                    const displayNodeIds = new Set(displayNodeList.value.map(n => n.id))
                    selectedNodesList.value = selectedNodesList.value.filter(
                        item => !displayNodeIds.has(item.id)
                    )
                } else {
                    // 全选当前显示的节点
                    displayNodeList.value.forEach(node => {
                        if (!selectedNodesList.value.find(item => item.id === node.id)) {
                            selectedNodesList.value.push({
                                id: node.id,
                                name: node.name,
                                ip: node.ip,
                                agentId: node.agentId,
                                status: node.status
                            })
                        }
                    })
                }
            }

            // 清空选择
            const handleClearSelection = () => {
                selectedNodesList.value = []
            }

            // 加载更多数据
            const loadMore = async () => {
                if (isLoadingMore.value || !hasMore.value) return

                isLoadingMore.value = true
                
                // 模拟加载延迟
                await new Promise(resolve => setTimeout(resolve, 500))

                const startIndex = (currentPage.value - 1) * pageSize.value
                const endIndex = startIndex + pageSize.value
                const newNodes = nodeList.value.slice(startIndex, endIndex)

                if (newNodes.length > 0) {
                    displayNodeList.value.push(...newNodes)
                    currentPage.value++
                } else {
                    hasMore.value = false
                }

                isLoadingMore.value = false
            }

            // 滚动事件处理
            const handleScroll = (e) => {
                const { scrollTop, scrollHeight, clientHeight } = e.target
                
                // 距离底部 50px 时触发加载
                if (scrollHeight - scrollTop - clientHeight < 50) {
                    loadMore()
                }
            }

            // 初始加载
            const initNodeList = async () => {
                displayNodeList.value = []
                currentPage.value = 1
                hasMore.value = true
                await loadMore()
            }

            onMounted(() => {
                initNodeList()
            })

            return {
                isShow,
                isLoading,
                relatedType,
                searchKeyword,
                selectedNodesList,
                nodeList,
                displayNodeList,
                nodeListContainer,
                totalCount,
                newCount,
                removeCount,
                currentEnv,
                dialogConfigs,
                isAllSelected,
                isIndeterminate,
                isLoadingMore,
                hasMore,
                isNodeSelected,
                handleNodeClick,
                handleToggleSelectAll,
                handleClearSelection,
                handleScroll,
                handleSearch,
                handleSave,
                handelCancel
            }
        }
    }
</script>

<style lang="scss">
@import '@/scss/conf';

.related-nodes-dialog {
    .bk-dialog-body {
        padding: 0;
    }
    .bk-dialog-tool {
        display: none;
    }

    .dialog-content {
        display: flex;
        height: 650px;
       
        .left-section {
            flex: 1;
            padding: 24px;
            border-right: 1px solid #DCDEE5;
            display: flex;
            flex-direction: column;
            .title-section {
                font-size: 14px;
                color: #313238;
                margin-bottom: 16px;
            }
            .form-section {
                margin-bottom: 20px;
                
                .form-label {
                    font-size: 12px;
                    color: #63656E;
                    margin-bottom: 8px;
                    font-weight: 500;
                }
                
                .bk-radio-group {
                    .bk-radio {
                        margin-right: 24px;
                    }
                }
                .bk-radio-text {
                    font-size: 12px;
                }
            }
            
            .node-list-section {
                flex: 1;
                overflow: hidden;
                display: flex;
                flex-direction: column;
                background: #fff;
                
                .list-header {
                    display: flex;
                    align-items: center;
                    padding: 4px 8px 0;
                    background: #fff;
                    .btn-text {
                        padding: 0 !important;
                        &:first-child {
                            margin-right: 12px;

                        }
                    }
                }
                
                .node-list-container {
                    flex: 1;
                    padding: 0 8px;
                    overflow-y: auto;
                    
                    .node-item {
                        display: flex;
                        align-items: flex-start;
                        cursor: pointer;
                        padding: 8px 0;
                        transition: background-color 0.2s;
                        .node-content {
                            flex: 1;
                            padding-left: 8px;
                            
                            .node-main-info {
                                display: flex;
                                align-items: center;
                                justify-content: space-between;
                                margin-bottom: 6px;
                                
                                .node-name {
                                    font-size: 14px;
                                    color: #313238;
                                    font-weight: normal;
                                    flex: 1;
                                    overflow: hidden;
                                    text-overflow: ellipsis;
                                    white-space: nowrap;
                                }
                                
                                .node-status {
                                    display: flex;
                                    align-items: center;
                                    font-size: 12px;
                                    margin-left: 16px;
                                    flex-shrink: 0;
                                    
                                    .bk-icon {
                                        font-size: 6px;
                                        margin-right: 6px;
                                    }
                                    
                                    &.status-normal {
                                        color: #2DCB56;
                                    }
                                    
                                    &.status-abnormal {
                                        color: #EA3636;
                                    }
                                }
                            }
                            
                            .node-sub-info {
                                display: flex;
                                align-items: center;
                                font-size: 12px;
                                color: #C4C6CC;
                                line-height: 1.5;
                                
                                .node-ip {
                                    margin-right: 12px;
                                }
                                
                                .node-agent {
                                    overflow: hidden;
                                    text-overflow: ellipsis;
                                    white-space: nowrap;
                                    
                                    &::before {
                                        content: '|';
                                        margin-right: 12px;
                                        color: #DCDEE5;
                                    }
                                }
                            }
                        }
                    }
                    
                    .loading-more,
                    .no-more {
                        padding: 16px;
                        text-align: center;
                        font-size: 12px;
                        color: #979BA5;
                    }
                    
                    .empty-state {
                        display: flex;
                        flex-direction: column;
                        align-items: center;
                        justify-content: center;
                        padding: 60px 0;
                        color: #979BA5;
                        
                        .bk-icon {
                            font-size: 48px;
                            margin-bottom: 16px;
                        }
                        
                        p {
                            margin: 0;
                            font-size: 14px;
                        }
                    }
                }
            }
        }
        
        .right-section {
            width: 420px;
            background: #F5F7FA;
            padding: 24px;
            display: flex;
            flex-direction: column;
            
            .preview-header {
                margin-bottom: 16px;
                
                .title {
                    font-size: 16px;
                    font-weight: 600;
                    color: #313238;
                }
            }
            
            .preview-stats {
                font-size: 12px;
                color: #63656E;
                margin-bottom: 16px;
                
                .count {
                    font-weight: 600;
                    
                    &.total-count {
                        color: #313238;
                    }
                    
                    &.new-count {
                        color: #2DCB56;
                    }
                    
                    &.remove-count {
                        color: #EA3636;
                    }
                }
            }
            
            .preview-content {
                flex: 1;
                overflow: hidden;
                
                .selected-nodes {
                    height: 100%;
                    overflow: auto;
                    
                    .selected-node-item {
                        display: flex;
                        align-items: center;
                        padding: 8px 12px;
                        margin-bottom: 8px;
                        background: white;
                        border-radius: 4px;
                        border: 1px solid #DCDEE5;
                        
                        .node-name {
                            flex: 1;
                            font-size: 14px;
                            color: #313238;
                            white-space: nowrap;
                            overflow: hidden;
                            text-overflow: ellipsis;
                        }
                        
                        .node-group {
                            font-size: 12px;
                            color: #979BA5;
                            margin-left: 8px;
                        }
                    }
                }
                
                .empty-state {
                    display: flex;
                    flex-direction: column;
                    align-items: center;
                    justify-content: center;
                    height: 100%;
                    color: #979BA5;
                    
                    .bk-icon {
                        font-size: 48px;
                        margin-bottom: 16px;
                    }
                    
                    p {
                        margin: 0;
                        font-size: 14px;
                    }
                }
            }
        }
    }
    
    .dialog-footer {
        text-align: right;
        .bk-button {
            margin-left: 8px;
        }
    }
}
</style>