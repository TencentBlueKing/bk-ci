<template>
    <div class="related-nodes-example">
        <h2>关联节点弹框示例</h2>
        <p>这是一个基于 useRelatedNodes hook 和 PipelineGroupEditDialog 布局样式的弹框示例</p>
        
        <bk-button
            theme="primary"
            @click="showRelatedNodes"
        >
            打开关联节点弹框
        </bk-button>
        
        <div v-if="selectedNodes.length > 0" class="selected-result">
            <h3>已选择的节点：</h3>
            <ul>
                <li v-for="node in selectedNodes" :key="node.id">
                    {{ node.name }} ({{ node.type }}) - {{ node.ip }}
                </li>
            </ul>
        </div>
        
        <!-- 关联节点弹框 -->
        <RelatedNodes
            @confirm="handleNodesConfirm"
            @close="handleNodesClose"
        />
    </div>
</template>

<script>
import RelatedNodes from './RelatedNodes.vue'
import useRelatedNodes from '../hooks/useRelatedNodes'

export default {
    name: 'RelatedNodesExample',
    components: {
        RelatedNodes
    },
    setup() {
        const { handleShowRelatedNodes } = useRelatedNodes()
        
        return {
            handleShowRelatedNodes
        }
    },
    data() {
        return {
            selectedNodes: []
        }
    },
    methods: {
        showRelatedNodes() {
            this.handleShowRelatedNodes()
        },
        
        handleNodesConfirm(nodes) {
            this.selectedNodes = nodes
            console.log('用户确认选择的节点:', nodes)
            this.$bkMessage({
                message: `成功选择了 ${nodes.length} 个节点`,
                theme: 'success'
            })
        },
        
        handleNodesClose() {
            console.log('用户关闭了弹框')
        }
    }
}
</script>

<style lang="scss" scoped>
.related-nodes-example {
    padding: 20px;
    
    h2 {
        margin-bottom: 16px;
        color: #313238;
    }
    
    p {
        margin-bottom: 20px;
        color: #63656E;
        line-height: 1.5;
    }
    
    .selected-result {
        margin-top: 20px;
        padding: 16px;
        background: #F5F7FA;
        border-radius: 4px;
        
        h3 {
            margin: 0 0 12px 0;
            color: #313238;
            font-size: 14px;
        }
        
        ul {
            margin: 0;
            padding-left: 20px;
            
            li {
                margin-bottom: 8px;
                color: #63656E;
                font-size: 14px;
            }
        }
    }
}
</style>