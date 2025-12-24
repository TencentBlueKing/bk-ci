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
        render-directive="if"
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
                <template v-if="relatedType === RELATED_TYPE.NODE">
                    <bk-input
                        v-model="searchKeyword"
                        :placeholder="$t('environment.searchNodePlaceholder')"
                        right-icon="bk-icon icon-search"
                        @enter="handleSearch"
                        @clear="handleSearch"
                        clearable
                    />
    
                    <div
                        class="node-list-section"
                        v-bkloading="{ isLoading }"
                    >
                        <!-- 顶部操作栏 -->
                        <div
                            v-if="nodeList.length"
                            class="list-header"
                        >
                            <bk-button
                                text
                                class="btn-text"
                                size="small"
                                @click="handleToggleSelectAll"
                            >
                                {{ $t('environment.selectAll') }} ({{ nodeList.length }})
                            </bk-button>
                            <bk-button
                                text
                                class="btn-text"
                                size="small"
                                @click="handleClearSelection"
                            >
                                {{ $t('environment.clear') }}
                            </bk-button>
                        </div>
    
                        <!-- 节点列表 -->
                        <div
                            class="node-list-container"
                            @scroll="handleScroll"
                        >
                            <div
                                v-for="node in nodeList"
                                :key="node.nodeHashId"
                                class="node-item"
                                @click="handleNodeClick(node)"
                            >
                                <bk-checkbox
                                    @change="handleNodeClick(node)"
                                    :value="isNodeSelected(node.nodeHashId)"
                                />
                                <div class="node-content">
                                    <div class="node-main-info">
                                        <span class="node-name">{{ node.displayName }}</span>
                                        <!-- <span
                                            class="node-status"
                                            :class="`status-${node.status?.toLowerCase()}`"
                                        >
                                            <i class="bk-icon icon-circle"></i>
                                            {{ node.statusText || '正常' }}
                                        </span> -->
                                    </div>
                                    <div class="node-sub-info">
                                        <span class="node-ip">{{ node.ip }}</span>
                                        <!-- <span
                                            v-if="node.agentId"
                                            class="node-agent"
                                        >
                                            {{ node.agentId }}
                                        </span> -->
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
                                    {{ $t('environment.loadingTitle') }}
                                </bk-loading>
                            </div>
    
                            <!-- 无更多数据 -->
                            <div
                                v-if="!hasMore && nodeList.length"
                                class="no-more"
                            >
                                {{ $t('environment.noMore') }}
                            </div>
    
                            <!-- 空状态 -->
                            <div
                                v-if="nodeList.length === 0 && !isLoading"
                                class="empty-state"
                            >
                                <i class="bk-icon icon-empty"></i>
                                <p>{{ $t('environment.noData') }}</p>
                            </div>
                        </div>
                    </div>
                </template>
                <template v-else>
                    <!-- 动态关联 -->
                    <div class="dynamic-related-section">
                        <!-- 标签选择表格 -->
                        <div class="table-section">
                            <bk-table
                                :data="labelRules"
                                class="label-table"
                            >
                                <bk-table-column
                                    :label="$t('environment.labelKey')"
                                    prop="tagKeyId"
                                    min-width="200"
                                >
                                    <template #default="{ row, $index }">
                                        <bk-select
                                            v-model="row.tagKeyId"
                                            :placeholder="$t('environment.pleaseSelectLabelKey')"
                                            :clearable="false"
                                            @change="handleLabelKeyChange(row, $index)"
                                        >
                                            <bk-option
                                                v-for="option in availableLabelKeys"
                                                :key="option.id"
                                                :id="option.id"
                                                :name="option.name"
                                            />
                                        </bk-select>
                                    </template>
                                </bk-table-column>
                                <bk-table-column
                                    :label="$t('environment.labelValue')"
                                    prop="tagValues"
                                    min-width="200"
                                >
                                    <template #default="{ row }">
                                        <bk-select
                                            v-model="row.tagValues"
                                            :placeholder="$t('environment.pleaseSelectLabelValue')"
                                            :clearable="false"
                                            :disabled="!row.tagKeyId"
                                            multiple
                                        >
                                            <bk-option
                                                v-for="val in getLabelValues(row.tagKeyId)"
                                                :key="val.id"
                                                :id="val.id"
                                                :name="val.name"
                                            />
                                        </bk-select>
                                    </template>
                                </bk-table-column>
                                <bk-table-column
                                    width="80"
                                >
                                    <template #default="{ $index }">
                                        <i
                                            class="operator-icon bk-icon icon-plus-circle mr10"
                                            @click="handleAddRule($index)"
                                        />
                                        <i
                                            class="operator-icon bk-icon icon-minus-circle"
                                            @click="handleDeleteRule($index)"
                                        />
                                    </template>
                                </bk-table-column>
                            </bk-table>

                            <bk-button
                                class="preview-button"
                                theme="primary"
                                outline
                                :disabled="!hasValidLabelRules"
                                @click="handlePreviewResult"
                            >
                                {{ $t('environment.previewResult') }}
                            </bk-button>
                        </div>
                    </div>
                </template>
            </div>

            <!-- 右侧预览区域 -->
            <div class="right-section">
                <div class="preview-header">
                    <span class="title">{{ $t('environment.previewResult') }}</span>
                </div>
                <div class="preview-stats">
                    <i18n path="environment.totalItem">
                        <span class="count total-count">{{ totalCount }}</span>
                    </i18n>
                    <i18n path="environment.newItem">
                        <span class="count new-count">{{ newCount }}</span>
                    </i18n>
                    <i18n path="environment.removeItem">
                        <span class="count remove-count">{{ removeCount }}</span>
                    </i18n>
                </div>

                <div
                    class="preview-content"
                    v-bkloading="{ isLoading: isPreviewLoading }"
                >
                    <div
                        v-if="shouldShowSelectedNodes"
                        class="selected-nodes"
                    >
                        <div
                            v-for="node in selectedNodesList"
                            :key="node.nodeHashId"
                            class="selected-node-item"
                            :class="{ 'is-deleted': node.isDelete }"
                        >
                            <div class="node-info">
                                <div class="node-name">
                                    {{ node.displayName }}
                                    <bk-tag
                                        v-if="node.isNew && !node.isDelete"
                                        theme="success"
                                        size="small"
                                    >
                                        {{ $t('environment.new') }}
                                    </bk-tag>
                                    <bk-tag
                                        v-if="node.isDelete"
                                        theme="danger"
                                        size="small"
                                    >
                                        {{ $t('environment.remove') }}
                                    </bk-tag>
                                </div>
                                <div class="node-details">
                                    <span class="node-ip">{{ node.ip }}</span>
                                    <!-- <span
                                        v-if="node.agentId"
                                        class="node-agent"
                                    >
                                        {{ node.agentId }}
                                    </span> -->
                                </div>
                            </div>
                        </div>
                    </div>
                    
                    <div
                        v-else
                        class="empty-state"
                    >
                        <Logo
                            name="empty"
                            size="60"
                        />
                        <p class="mt20">
                            {{ relatedType === RELATED_TYPE.NODE ? $t('environment.noSelectNode') : $t('environment.noSetLabel') }}
                        </p>
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
                :loading="isSaveLoading"
                @click="handleSave"
            >
                {{ $t('environment.save') }}
            </bk-button>
            <bk-button
                :loading="isSaveLoading"
                @click="handleCloseDialog"
            >
                {{ $t('environment.cancel') }}
            </bk-button>
        </div>
    </bk-dialog>
</template>

<script>
    import { ref, computed, onMounted, watch } from 'vue'
    import Logo from '@/components/Logo'
    import useRelatedNodes from '@/hooks/useRelatedNodes'
    import useEnvDetail from '@/hooks/useEnvDetail'
    import usePagination from '@/hooks/usePagination'
    import useInstance from '@/hooks/useInstance'
    export default {
        name: 'RelatedNodes',
        components: {
            Logo
        },
        props: {
            currentNodeList: {
                type: Array,
                default: () => []
            }
        },
        emits: ['save-success'],
        setup (props, { emit }) {
            const { proxy } = useInstance()
            const {
                isShow,
                nodeList,
                isLoading,
                relatedType,
                RELATED_TYPE,
                relateNodes,
                requestNodeList,
                handleCloseDialog,
                availableLabelKeys,
                fetchTagList,
                getLabelValues
            } = useRelatedNodes()

            const {
                currentEnv
            } = useEnvDetail()

            const {
                pagination,
                pageChange,
            } = usePagination({
                limit: 50,
                current: 1
            })

            const searchKeyword = ref('')
            const dialogConfigs = {
                top: '120'
            }
            const isSaveLoading = ref(false)
            const selectedNodesList = ref([])
            // 保存两种模式下各自选择的节点
            const staticModeSelectedNodes = ref([])
            const dynamicModeSelectedNodes = ref([])
            
            // 动态关联相关数据
            const labelRules = ref([
                { tagKeyId: '', tagValues: [] }
            ])
            
            // 动态模式是否已经预览过
            const isDynamicPreviewed = ref(false)

            const isLoadingMore = ref(false)
            const hasMore = computed(() => {
                return nodeList.value.length < pagination.value.count
            })
           
            // 选中的节点 ID 集合（不包括已删除的）
            const selectedNodeIds = computed(() => new Set(
                selectedNodesList.value
                    .filter(node => !node.isDelete)
                    .map(node => node.nodeHashId)
            ))

            // 是否全选
            const isAllSelected = computed(() => {
                return nodeList.value.length && nodeList.value.every(node => selectedNodeIds.value.has(node.nodeHashId))
            })
            
            // 判断所有标签规则是否都有效（所有规则的 key 和 value 都不为空）
            const hasValidLabelRules = computed(() => {
                return labelRules.value.length && labelRules.value.every(rule => rule.tagKeyId && rule.tagValues?.length)
            })
            
            // 判断是否应该显示选中的节点列表
            const shouldShowSelectedNodes = computed(() => {
                // 静态模式：有选中节点就显示
                if (relatedType.value === RELATED_TYPE.NODE) {
                    return !!selectedNodesList.value.length
                }
                // 动态模式：需要点击预览后才显示
                return isDynamicPreviewed.value && selectedNodesList.value.length > 0
            })

            // 统计数据 - 不包括已删除的节点
            const totalCount = computed(() => {
                // 动态模式未预览时返回 0
                if (relatedType.value === RELATED_TYPE.TAG && !isDynamicPreviewed.value) {
                    return 0
                }
                return selectedNodesList.value.filter(node => !node.isDelete).length
            })
            
            // 计算新增和移除的节点数量
            const currentNodeIds = computed(() => new Set(props.currentNodeList.map(node => node.nodeHashId)))
            
            const newCount = computed(() => {
                // 动态模式未预览时返回 0
                if (relatedType.value === RELATED_TYPE.TAG && !isDynamicPreviewed.value) {
                    return 0
                }
                // 新增的节点 = 当前选中的节点中（未删除且不在原有节点列表中的）
                return selectedNodesList.value.filter(node => !node.isDelete && !currentNodeIds.value.has(node.nodeHashId)).length
            })
            
            const removeCount = computed(() => {
                // 动态模式未预览时返回 0
                if (relatedType.value === RELATED_TYPE.TAG && !isDynamicPreviewed.value) {
                    return 0
                }
                // 移除的节点 = 标记为删除的节点数量
                return selectedNodesList.value.filter(node => node.isDelete).length
            })

            // 判断节点是否被选中
            const isNodeSelected = (nodeHashId) => {
                return selectedNodeIds.value.has(nodeHashId)
            }
            const handleSearch = (keyword) => {
                searchKeyword.value = keyword
                pageChange(1)
                fetchNodeList()
            }
            const handleNodeClick = (node) => {
                const index = selectedNodesList.value.findIndex(item => item.nodeHashId === node.nodeHashId)
                
                if (index > -1) {
                    const existingNode = selectedNodesList.value[index]
                    // 节点已存在
                    if (existingNode.isDelete) {
                        // 如果是被删除状态，恢复节点
                        selectedNodesList.value.splice(index, 1, { ...existingNode, isDelete: false })
                    } else {
                        // 如果是选中状态，标记为删除
                        // 检查是否是原有节点
                        const isOriginalNode = currentNodeIds.value.has(node.nodeHashId)
                        if (isOriginalNode) {
                            // 原有节点标记为删除 - 使用对象替换确保响应式
                            selectedNodesList.value.splice(index, 1, { ...existingNode, isDelete: true })
                        } else {
                            // 新增的节点直接移除
                            selectedNodesList.value.splice(index, 1)
                        }
                    }
                } else {
                    // 添加新节点，判断是否是新增节点
                    const isOriginalNode = currentNodeIds.value.has(node.nodeHashId)
                    selectedNodesList.value.push({
                        ...node,
                        isDelete: false,
                        isNew: !isOriginalNode  // 不在原有列表中的标记为新增
                    })
                }
            }

            // 切换全选/取消全选
            const handleToggleSelectAll = () => {
                if (isAllSelected.value) {
                    // 如果已经全选，则取消全选当前页的节点
                    nodeList.value.forEach(node => {
                        const index = selectedNodesList.value.findIndex(item => item.nodeHashId === node.nodeHashId)
                        if (index > -1) {
                            const existingNode = selectedNodesList.value[index]
                            if (!existingNode.isDelete) {
                                // 检查是否是原有节点
                                const isOriginalNode = currentNodeIds.value.has(node.nodeHashId)
                                if (isOriginalNode) {
                                    // 原有节点标记为删除 - 使用对象替换确保响应式
                                    selectedNodesList.value.splice(index, 1, { ...existingNode, isDelete: true })
                                } else {
                                    // 新增的节点直接移除
                                    selectedNodesList.value.splice(index, 1)
                                }
                            }
                        }
                    })
                } else {
                    // 全选当前页的节点
                    nodeList.value.forEach(node => {
                        const index = selectedNodesList.value.findIndex(item => item.nodeHashId === node.nodeHashId)
                        if (index > -1) {
                            const existingNode = selectedNodesList.value[index]
                            // 如果节点已存在且被删除，恢复它 - 使用对象替换确保响应式
                            if (existingNode.isDelete) {
                                selectedNodesList.value.splice(index, 1, { ...existingNode, isDelete: false })
                            }
                        } else {
                            // 添加新节点，判断是否是新增节点
                            const isOriginalNode = currentNodeIds.value.has(node.nodeHashId)
                            selectedNodesList.value.push({
                                ...node,
                                isDelete: false,
                                isNew: !isOriginalNode
                            })
                        }
                    })
                }
            }


            // 获取节点列表
            const fetchNodeList = async () => {
                try {
                    const res = await requestNodeList({
                        page: pagination.value.current,
                        pageSize: pagination.value.limit,
                        keywords: searchKeyword.value
                    })
                    pagination.value.count = res.count
                    pageChange(pagination.value.current + 1)
                } catch (error) {
                    console.error(error)
                }
            }

            // 清空选择
            const handleClearSelection = () => {
                // 原有的节点标记为删除，新增的节点直接移除
                selectedNodesList.value = selectedNodesList.value
                    .filter(node => currentNodeIds.value.has(node.nodeHashId))
                    .map(node => ({ ...node, isDelete: true }))
            }

            // 加载更多数据
            const loadMore = async () => {
                if (isLoadingMore.value || !hasMore.value) return
                
                try {
                    isLoadingMore.value = true
                    await fetchNodeList()
                } finally {
                    isLoadingMore.value = false
                }
            }

            // 滚动事件处理
            const handleScroll = (e) => {
                const { scrollTop, scrollHeight, clientHeight } = e.target
                if (scrollHeight - scrollTop - clientHeight < 50) {
                    loadMore()
                }
            }

            const handleSave = async () => {
                try {
                    isSaveLoading.value = true
                    const params = {
                        ...(relatedType.value === RELATED_TYPE.NODE
                            ? {
                                nodeHashIds: selectedNodesList.value
                                    .filter(node => !node.isDelete)
                                    .map(node => node.nodeHashId)
                            }
                            : {
                                // 每个标签键+值组合成一条记录
                                tags: labelRules.value
                                    .filter(rule => rule.tagKeyId && rule.tagValues?.length)
                                    .flatMap(rule =>
                                        rule.tagValues.map(tagValueId => ({
                                            tagKeyId: rule.tagKeyId,
                                            tagValueId
                                        }))
                                    )
                            }
                        )
                    }

                    await relateNodes(params)
                    
                    emit('save-success')
                    handleCloseDialog()
                } catch (e) {
                    console.error(e)
                    proxy.$bkMessage({
                        theme: 'error',
                        message: e.message || e
                    })
                } finally {
                    isSaveLoading.value = false
                }
            }
            
            // 添加规则
            const handleAddRule = () => {
                labelRules.value.push({ tagKeyId: '', tagValues: [] })
            }
            
            // 删除规则
            const handleDeleteRule = (index) => {
                if (labelRules.value.length > 1) {
                    labelRules.value.splice(index, 1)
                } else {
                    labelRules.value[index].tagKeyId = ''
                    labelRules.value[index].tagValues = []
                }
            }
            
            const isPreviewLoading = ref(false)
            // 预览动态关联结果
            const handlePreviewResult = async () => {
                try {
                    isPreviewLoading.value = true
                    const res = await requestNodeList({
                        page: pagination.value.current,
                        pageSize: 1000
                    }, labelRules.value)
                    
                    // 获取预览结果的节点列表
                    const previewNodes = res.records || []
                    
                    // 构建预览节点 ID 集合
                    const previewNodeIds = new Set(previewNodes.map(node => node.nodeHashId))
                    
                    // 1. 标记已关联但不在预览结果中的节点为删除（isDelete: true）
                    const deletedNodes = props.currentNodeList
                        .filter(node => !previewNodeIds.has(node.nodeHashId))
                        .map(node => ({
                            ...node,
                            isDelete: true,
                            isNew: false
                        }))
                    
                    // 2. 标记预览结果中新增的节点（isNew: true）
                    const newNodes = previewNodes
                        .filter(node => !currentNodeIds.value.has(node.nodeHashId))
                        .map(node => ({
                            ...node,
                            isDelete: false,
                            isNew: true
                        }))
                    
                    // 3. 既在预览结果中又在已关联列表中的节点（保持不变）
                    const unchangedNodes = previewNodes
                        .filter(node => currentNodeIds.value.has(node.nodeHashId))
                        .map(node => ({
                            ...node,
                            isDelete: false,
                            isNew: false
                        }))
                    
                    selectedNodesList.value = [...unchangedNodes, ...newNodes, ...deletedNodes]
                    // 标记已经预览过
                    isDynamicPreviewed.value = true
                } catch (error) {
                    proxy.$bkMessage({
                        theme: 'error',
                        message: error.message || error
                    })
                } finally {
                    isPreviewLoading.value = false
                }
            }
            
            // 标签键改变时清空值
            const handleLabelKeyChange = (row, index) => {
                row.tagValues = []
            }

            const initData = async () => {
                if (relatedType.value === RELATED_TYPE.NODE) {
                    // 静态模式：加载节点列表
                    pageChange(1)
                    searchKeyword.value = ''
                    await fetchNodeList()
                    // 恢复静态模式下的选择
                    selectedNodesList.value = [...staticModeSelectedNodes.value]
                } else {
                    // 动态模式：清空节点列表，重置预览状态
                    selectedNodesList.value = []
                    isDynamicPreviewed.value = false
                }
            }

            // 监听弹窗显示状态,显示时初始化数据
            watch(() => isShow.value, (newVal) => {
                if (newVal) {
                    fetchTagList()
                    // 从 currentNodeList 初始化静态模式的数据
                    if (props.currentNodeList?.length) {
                        staticModeSelectedNodes.value = props.currentNodeList.map(node => ({ ...node }))
                    }
                    // 初始化当前模式的数据
                    initData()
                } else {
                    // 弹窗关闭时还原所有数据
                    selectedNodesList.value = []
                    staticModeSelectedNodes.value = []
                    dynamicModeSelectedNodes.value = []
                    isDynamicPreviewed.value = false
                    labelRules.value = [{ tagKeyId: '', tagValues: [] }]
                    searchKeyword.value = ''
                    pageChange(1)
                    nodeList.value = []
                    relatedType.value = RELATED_TYPE.NODE
                }
            })

            watch(() => relatedType.value, async (val, oldVal) => {
                pageChange(1)
                labelRules.value = [{ tagKeyId: '', tagValues: [] }]
                // 切换模式前，保存当前模式的选择
                if (val === RELATED_TYPE.NODE) {
                    // 切换到静态模式前，保存动态模式的选择
                    dynamicModeSelectedNodes.value = [...selectedNodesList.value]
                } else {
                    // 切换到动态模式前，保存静态模式的选择
                    staticModeSelectedNodes.value = [...selectedNodesList.value]
                }
                initData()
            })

            return {
                // data
                isShow,
                isLoading,
                relatedType,
                searchKeyword,
                currentEnv,
                selectedNodesList,
                nodeList,
                pagination,
                totalCount,
                newCount,
                removeCount,
                dialogConfigs,
                isAllSelected,
                isLoadingMore,
                hasMore,
                isNodeSelected,
                RELATED_TYPE,
                isSaveLoading,
                labelRules,
                availableLabelKeys,
                hasValidLabelRules,
                shouldShowSelectedNodes,
                isPreviewLoading,

                // function
                handleNodeClick,
                handleToggleSelectAll,
                handleClearSelection,
                handleScroll,
                handleSearch,
                handleCloseDialog,
                handleSave,
                getLabelValues,
                handleAddRule,
                handleDeleteRule,
                handleLabelKeyChange,
                handlePreviewResult
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
                                    line-height: 22px;
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
                                    margin-right: 8px;
                                }
                                
                                .node-agent {
                                    overflow: hidden;
                                    text-overflow: ellipsis;
                                    white-space: nowrap;
                                    
                                    &::before {
                                        content: '|';
                                        margin-right: 8px;
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
            
            // 动态关联样式
            .dynamic-related-section {
                flex: 1;
                overflow-y: auto;
                
                .table-section {
                    .section-title {
                        font-size: 12px;
                        color: #63656E;
                        margin-bottom: 8px;
                        font-weight: 500;
                    }
                    
                    .label-table {
                        margin-bottom: 12px;
                        
                        .bk-table-body {
                            tr:hover {
                                background-color: #F5F7FA;
                            }
                        }
                    }
                    .preview-button {
                        width: 100%;
                    }
                    
                    .operator-icon {
                        font-size: 14px;
                        cursor: pointer;
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
                        color: #3a84ff;
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
                        padding: 0 16px;
                        margin-bottom: 8px;
                        background: white;
                        border-radius: 2px;
                        transition: opacity 0.2s;
                        
                        &.is-deleted {
                            .node-info {
                                .node-name {
                                    text-decoration: line-through;
                                    color: #C4C6CC;
                                }
                                
                                .node-details {
                                    color: #C4C6CC;
                                }
                            }
                        }
                        
                        .node-info {
                            .node-name {
                                height: 32px;
                                font-size: 14px;
                                color: #313238;
                                margin-bottom: 2px;
                                word-break: break-all;
                                line-height: 1.5;
                                display: flex;
                                align-items: center;
                                gap: 8px;
                            }
                            
                            .node-details {
                                padding-bottom: 4px;
                                align-items: center;
                                font-size: 12px;
                                color: #979BA5;
                                line-height: 1.5;
                                
                                .node-agent {
                                    overflow: hidden;
                                    text-overflow: ellipsis;
                                    white-space: nowrap;
                                    margin-left: 8px;
                                    &::before {
                                        content: '|';
                                        margin-right: 8px;
                                        color: #DCDEE5;
                                    }
                                }
                            }
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