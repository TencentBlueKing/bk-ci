<template>
    <div class="agent-offline-records-container">
        <h3 class="section-title">{{ $t('environment.offlineRecords') }}</h3>
        <!-- Agent离线记录内容 -->
        <div class="offline-records-content">
            <p>Agent离线记录内容待实现</p>
        </div>
    </div>
</template>

<script>
    export default {
        name: 'AgentOfflineRecords'
    }
</script>

<style lang="scss" scoped>
.agent-offline-records-container {
    padding: 20px;
    height: 100%;
    overflow-y: auto;
    
    .section-title {
        font-size: 14px;
        font-weight: 700;
        color: #63656E;
        margin-bottom: 16px;
    }
    
    .offline-records-content {
        background: #F5F7FA;
        padding: 20px;
        border-radius: 2px;
    }
}
</style>
