<template>
    <div class="draft-debug-record-box">
        <bk-alert
            class="draft-debug-record-hint"
            :title="$t('debugHint')"
        ></bk-alert>
        <BuildHistoryTab
            class="draft-debug-history-tab"
            is-debug
        />
    </div>
</template>

<script>
    import BuildHistoryTab from '@/components/PipelineDetailTabs/BuildHistoryTab'
    import '@blueking/search-select/dist/styles/index.css'
    export default {
        components: {
            BuildHistoryTab
        }
    }
</script>

<style lang="scss">
    .draft-debug-record-box {
        display: flex;
        flex-direction: column;
        .draft-debug-record-hint {
            flex-shrink: 0;
        }
        .draft-debug-history-tab {
            flex: 1;
            overflow: hidden;
        }
    }
</style>
