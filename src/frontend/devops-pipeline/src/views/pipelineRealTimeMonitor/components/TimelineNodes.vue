<template>
    <div class="timeline-nodes">
        <div class="nodes-list">
            <div
                v-for="(node, index) in nodes"
                :key="index"
                class="node-item"
            >
                <span
                    class="node-icon"
                    :class="node.type"
                ></span>
                <span class="node-value">{{ node.value }}</span>
                <span class="node-label">{{ node.label }}</span>
            </div>
        </div>
    </div>
</template>

<script>
    import { defineComponent } from 'vue'

    export default defineComponent({
        name: 'TimelineNodes',
        props: {
            nodes: {
                type: Array,
                default: () => []
            }
        }
    })
</script>

<style lang="scss" scoped>
.timeline-nodes {
    padding: 2px 0px 18px 0px;
}

.nodes-list {
    display: flex;
    align-items: center;
    gap: 24px;
    flex-wrap: wrap;
}

.node-item {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 12px;
    color: #63656e;
    padding: 6px 8px;
    border-radius: 2px;
    cursor: pointer;
    transition: all 0.3s ease;
    
    &:hover {
        box-shadow: 0 2px 6px 0 rgba(58, 132, 255, 0.15);
        transform: translateY(-2px);
    }
    
    .node-icon {
        width: 16px;
        height: 16px;
        border-radius: 2px;
        flex-shrink: 0;
    }
    
    .node-value {
        font-size: 20px;
        color: #313238;
    }
    
    .node-label {
        font-size: 14px;
        color: #979BA5;
    }
    
    // 不同类型的图标颜色
    .node-icon.success {
        background: #2dcb56;
    }
    
    .node-icon.warning {
        background: #ff9c01;
    }
    
    .node-icon.info {
        background: #3a84ff;
    }
    
    .node-icon.danger {
        background: #ea3636;
    }
    
    .node-icon.gray {
        background: #c4c6cc;
    }
}
</style>
