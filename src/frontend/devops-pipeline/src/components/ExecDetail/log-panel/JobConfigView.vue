<template>
    <div class="job-cfg">
        <div class="job-cfg-toolbar">
            <strong class="job-cfg-title">{{ $t('logPanel.jobConfigTitle') }}</strong>
            <bk-button
                text
                theme="primary"
                @click="$emit('view-config')"
            >
                {{ $t('logPanel.viewJobConfig') }}
            </bk-button>
        </div>
        <bk-table
            class="job-cfg-table"
            :data="rows"
            :outer-border="true"
            :header-border="false"
        >
            <bk-table-column
                :label="$t('logPanel.paramName')"
                prop="key"
                width="280"
                show-overflow-tooltip
            />
            <bk-table-column
                :label="$t('logPanel.paramValue')"
                prop="value"
                show-overflow-tooltip
            />
            <template slot="empty">
                <span class="job-cfg-empty">{{ $t('logPanel.emptyJobConfig') }}</span>
            </template>
        </bk-table>
    </div>
</template>

<script>
    export default {
        name: 'JobConfigView',
        props: {
            rows: {
                type: Array,
                default: () => []
            }
        }
    }
</script>

<style lang="scss" scoped>
.job-cfg {
    flex: 1;
    min-height: 0;
    display: flex;
    flex-direction: column;
    gap: 12px;
    padding: 16px 24px 24px;
    background: #f5f7fa;
    width: 100%;
    overflow: auto;
}
.job-cfg-toolbar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    flex-shrink: 0;
}
.job-cfg-title {
    font-size: 14px;
    font-weight: 700;
    line-height: 22px;
    color: #313238;
}
.job-cfg-empty {
    color: #979ba5;
    font-size: 12px;
}
.job-cfg-table {
    background: #fff;
}
</style>
