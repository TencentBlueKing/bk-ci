<template>
    <div class="node-settings-container">
        <h3 class="section-title">{{ $t('environment.settings') }}</h3>
        <!-- 节点设置内容 -->
        <div class="settings-content">
            <p>节点设置内容待实现</p>
        </div>
    </div>
</template>

<script>
    export default {
        name: 'NodeSettings'
    }
</script>

<style lang="scss" scoped>
.node-settings-container {
    padding: 20px;
    height: 100%;
    overflow-y: auto;
    
    .section-title {
        font-size: 14px;
        font-weight: 700;
        color: #63656E;
        margin-bottom: 16px;
    }
    
    .settings-content {
        background: #F5F7FA;
        padding: 20px;
        border-radius: 2px;
    }
}
</style>
