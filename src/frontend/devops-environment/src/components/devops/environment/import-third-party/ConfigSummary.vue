<template>
    <div class="config-summary">
        <div class="config-summary-header">
            <p class="config-summary-title">{{ $t('environment.installSession.currentConfig') }}</p>
            <bk-button
                theme="default"
                class="summary-edit"
                @click="$emit('edit')"
            >
                {{ $t('environment.installSession.editConfig') }}
            </bk-button>
        </div>
        <div class="config-summary-body">
            <span
                v-for="item in items"
                :key="item.label"
                class="summary-item"
            >
                <span class="summary-label">{{ item.label }}</span>{{ item.value }}
            </span>
        </div>
    </div>
</template>

<script>
    /**
     * 导入第三方构建机弹窗 · 第二步顶部的只读配置摘要。
     * 既是"这条命令带着什么配置"的说明，也省掉第二段再造一份摘要；
     * 留空的并发已由父组件换算成实际生效的默认值，不让用户回头猜。
     */
    export default {
        name: 'ConfigSummary',
        props: {
            /** [{ label: string, value: string }] */
            items: {
                type: Array,
                default: () => []
            }
        }
    }
</script>

<style lang="scss" scoped>
    .config-summary {
        display: flex;
        flex-direction: column;
        gap: 12px;
        padding: 10px 16px;
        background: #f5f7fa;
        border-radius: 2px;
        font-size: 12px;
        color: #313238;
    }
    .config-summary-title {
        margin: 0;
        font-size: 14px;
        font-weight: 400;
        line-height: 22px;
        color: #313238;
    }
    .config-summary-header {
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 12px;
    }
    .config-summary-body {
        display: flex;
        flex-wrap: wrap;
        align-items: center;
        gap: 4px 20px;
    }
    .summary-label {
        margin-right: 6px;
        color: #979ba5;
    }
    .summary-edit {
        flex: none;
    }
</style>
