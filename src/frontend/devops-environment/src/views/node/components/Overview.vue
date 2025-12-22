<template>
    <div class="node-overview-container">
        <h3 class="section-title">{{ $t('environment.overview') }}</h3>
        <!-- 节点概览内容 -->
        <div class="overview-content">
            <p>节点概览内容待实现</p>
        </div>
    </div>
</template>

<script>
    export default {
        name: 'NodeOverview'
    }
</script>

<style lang="scss" scoped>
.node-overview-container {
    padding: 20px;
    height: 100%;
    overflow-y: auto;
    
    .section-title {
        font-size: 14px;
        font-weight: 700;
        color: #63656E;
        margin-bottom: 16px;
    }
    
    .overview-content {
        background: #F5F7FA;
        padding: 20px;
        border-radius: 2px;
    }
}
</style>
