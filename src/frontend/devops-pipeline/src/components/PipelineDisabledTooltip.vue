<template>
    <div
        :id="id"
        class="pipeline-disabled-tooltip"
    >
        <p class="disabled-tooltip-title">{{ $t('pacPipelineLockTips') }}</p>
        <p
            v-if="isPageAndYamlDisabled"
            class="disabled-tooltip-sub-title"
        >
            {{ $t('resolveBothDisabledTips') }}
        </p>
        <div
            v-for="group in disabledTooltipGroups"
            :key="group.key"
            class="disabled-tooltip-group"
        >
            <p
                v-if="group.title"
                class="disabled-tooltip-group-title"
            >
                {{ group.title }}
            </p>
            <p
                v-for="item in group.items"
                :key="`${item.value}-${Date.now()}`"
                class="disabled-tooltip-item"
            >
                <span class="item-label">{{ item.label }}</span>
                <span class="item-value">{{ item.value }}</span>
            </p>
        </div>
    </div>
</template>

<script>
    import { mapState, mapGetters } from 'vuex'
    import { convertTime } from '@/utils/util'

    export default {
        name: 'PipelineDisabledTooltip',
        props: {
            id: {
                type: String,
                required: true
            }
        },
        computed: {
            ...mapState('atom', ['pipelineInfo']),
            ...mapGetters({
                isCurPipelineLocked: 'atom/isCurPipelineLocked',
                isCurPipelineYamlLocked: 'atom/isCurPipelineYamlLocked'
            }),
            // 页面与 YAML 均禁用
            isPageAndYamlDisabled () {
                return this.isCurPipelineLocked && this.isCurPipelineYamlLocked
            },
            // 存量禁用数据没有禁用人/时间/原因
            hasPageDisabledInfo () {
                return !!(this.pipelineInfo?.lockUser || this.pipelineInfo?.lockTime || this.pipelineInfo?.lockReason)
            },
            // 页面（UI）禁用明细
            pageDisabledItems () {
                return [
                    { label: this.$t('disabledByLabel'), value: this.pipelineInfo?.lockUser || '--' },
                    { label: this.$t('disabledAtLabel'), value: convertTime(this.pipelineInfo?.lockTime) },
                    { label: this.$t('disableReasonLabel'), value: this.pipelineInfo?.lockReason || '--' }
                ]
            },
            // YAML 声明禁用明细
            yamlDisabledItems () {
                return [
                    { label: this.$t('submitterLabel'), value: this.pipelineInfo?.yamlLockUser || '--' },
                    { label: this.$t('submitTimeLabel'), value: convertTime(this.pipelineInfo?.yamlLockTime) }
                ]
            },
            // 禁用 tooltip 分组信息：三种禁用场景
            disabledTooltipGroups () {
                // 场景三：页面与 YAML 均禁用，分组展示两者明细
                if (this.isPageAndYamlDisabled) {
                    return [
                        // 存量页面禁用数据没有禁用明细，跳过页面禁用分组
                        ...(this.hasPageDisabledInfo ? [{ key: 'page', title: this.$t('pageDisabledLabel'), items: this.pageDisabledItems }] : []),
                        { key: 'yaml', title: this.$t('yamlDisabledLabel'), items: this.yamlDisabledItems }
                    ]
                }
                // 场景二：仅代码库 YAML 声明禁用，额外展示禁用来源
                if (this.isCurPipelineYamlLocked) {
                    return [{
                        key: 'yaml',
                        items: [
                            { label: this.$t('disabledSourceLabel'), value: this.$t('yamlDisabledSourceDesc') },
                            ...this.yamlDisabledItems
                        ]
                    }]
                }
                // 场景一：仅页面禁用；存量数据无禁用明细，只展示标题
                return this.hasPageDisabledInfo ? [{ key: 'page', items: this.pageDisabledItems }] : []
            }
        }
    }
</script>

<style lang="scss">
.pipeline-disabled-tooltip {
    font-size: 12px;
    line-height: 20px;
    text-align: left;
    .disabled-tooltip-title {
        font-weight: bold;
        color: #313238;
        margin-bottom: 4px;
    }
    .disabled-tooltip-sub-title {
        color: #63656e;
        margin-bottom: 8px;
    }
    .disabled-tooltip-group {
        margin-top: 8px;
        padding: 8px 12px;
        background: #f5f7fa;
        border-radius: 4px;
    }
    .disabled-tooltip-group-title {
        font-weight: bold;
        color: #313238;
        margin-bottom: 4px;
    }
    .disabled-tooltip-item {
        display: flex;
        .item-label {
            width: 64px;
            flex-shrink: 0;
            color: #979ba5;
        }
        .item-value {
            word-break: break-all;
            color: #313238;
        }
    }
}
</style>
