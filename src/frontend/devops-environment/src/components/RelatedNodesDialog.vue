<template>
    <bk-dialog
        :value="isShow"
        width="1080"
        ext-cls="related-nodes-dialog"
        scrollable
        :quick-close="false"
        :close-icon="false"
        :draggable="false"
    >
        <div class="dialog-content">
            <div class="left-section">
                <div class="title-section">
                    环境1 - 关联节点
                </div>
                <!-- 关联策略 -->
                <div class="form-section">
                    <div class="form-label">关联策略</div>
                    <bk-radio-group v-model="relatedType">
                        <bk-radio
                            class="mr10"
                            value="static"
                        >
                            静态关联
                        </bk-radio>
                        <bk-radio value="dynamic">动态关联</bk-radio>
                    </bk-radio-group>
                </div>

                <div class="form-section">
                    <bk-input
                        v-model="searchKeyword"
                        placeholder="搜索标签/节点"
                        right-icon="bk-icon icon-search"
                        @enter="handleSearch"
                        clearable
                    />
                </div>

                <div class="tree-section">
                    <bk-big-tree
                        ref="nodeTree"
                        :data="treeData"
                        :show-checkbox="true"
                        :expand-all="true"
                        :check-strictly="false"
                        :multiple="true"
                        node-key="id"
                        :show-icon="false"
                        :options="treeOptions"
                        @check-change="handleNodeCheck"
                    >
                        <template slot-scope="{ node, data }">
                            <div class="tree-node">
                                <span class="node-name">
                                    {{ data.name }}
                                    <span
                                        v-if="data.count"
                                        class="node-count"
                                    >
                                        ({{ data.count }})
                                    </span>
                                </span>
                            </div>
                        </template>
                    </bk-big-tree>
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
    import { ref, computed } from 'vue'
    import useRelatedNodes from '../hooks/useRelatedNodes'

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
                handelCancel
            } = useRelatedNodes()

            // 模拟树形数据
            const treeData = ref([
                {
                    id: 'deploy-nodes',
                    name: '部署节点',
                    count: 7,
                    type: 'folder',
                    children: [
                        {
                            id: 'node-1',
                            name: '蓝盾前端发布',
                            type: 'node',
                            group: '流水线组 8'
                        },
                        {
                            id: 'node-2',
                            name: '蓝盾后台发布',
                            type: 'node',
                            group: '流水线组 8'
                        },
                        {
                            id: 'node-3',
                            name: '部署节点 7',
                            type: 'node',
                            group: '流水线组 8'
                        },
                        {
                            id: 'node-4',
                            name: '部署节点 6',
                            type: 'node',
                            group: '流水线组 8'
                        },
                        {
                            id: 'node-5',
                            name: '部署节点 5',
                            type: 'node',
                            group: '流水线组 8'
                        },
                        {
                            id: 'node-6',
                            name: '部署节点 1',
                            type: 'node',
                            group: '流水线组 8'
                        },
                        {
                            id: 'node-7',
                            name: '各字比较长比较长比较长比较长比较长比较长的部署节点...',
                            type: 'node',
                            group: '流水线组 8'
                        }
                    ]
                }
            ])

            // 统计数据
            const totalCount = computed(() => selectedNodesList.value.length)
            const newCount = ref(0)
            const removeCount = ref(0)

            // 处理节点选择
            const handleNodeCheck = (data, checked, indeterminate) => {
                if (data.type === 'node') {
                    if (checked) {
                        // 添加节点
                        if (!selectedNodesList.value.find(item => item.id === data.id)) {
                            selectedNodesList.value.push({
                                id: data.id,
                                name: data.name,
                                type: data.type,
                                group: data.group
                            })
                        }
                    } else {
                        // 移除节点
                        const index = selectedNodesList.value.findIndex(item => item.id === data.id)
                        if (index > -1) {
                            selectedNodesList.value.splice(index, 1)
                        }
                    }
                }
            }

            return {
                isShow,
                isLoading,
                relatedType,
                searchKeyword,
                selectedNodesList,
                treeData,
                totalCount,
                newCount,
                removeCount,
                handleNodeCheck,
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
        height: 500px;
       
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
            
            .tree-section {
                flex: 1;
                overflow: hidden;
                
                .bk-big-tree {
                    height: 100%;
                    overflow: auto;
                    
                    // 修复复选框和文本对齐问题
                    :deep(.bk-tree-node) {
                        .bk-tree-node-content {
                            display: flex;
                            align-items: center;
                            
                            .bk-tree-node-checkbox {
                                margin-right: 8px;
                            }
                            
                            .bk-tree-node-label {
                                flex: 1;
                                display: flex;
                                align-items: center;
                            }
                        }
                    }
                    
                    .tree-node {
                        display: flex;
                        align-items: center;
                        width: 100%;
                        
                        .node-name {
                            display: flex;
                            align-items: center;
                            flex: 1;
                            font-size: 14px;
                            color: #313238;
                            white-space: nowrap;
                            overflow: hidden;
                            text-overflow: ellipsis;
                        }
                        
                        .node-count {
                            color: #979BA5;
                            font-size: 12px;
                            margin-left: 4px;
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

// 全局修复 bk-big-tree 的复选框对齐问题
.related-nodes-dialog {
    .bk-big-tree {
        .bk-tree-node-content {
            display: flex !important;
            align-items: center !important;
            
            .bk-tree-node-checkbox {
                margin-right: 8px !important;
                flex-shrink: 0 !important;
            }
            
            .bk-tree-node-label {
                flex: 1 !important;
                display: flex !important;
                align-items: center !important;
            }
        }
        
        // 确保树节点内容正确对齐
        .bk-tree-node {
            .bk-tree-node-content {
                height: auto !important;
                line-height: normal !important;
            }
        }
    }
}
</style>