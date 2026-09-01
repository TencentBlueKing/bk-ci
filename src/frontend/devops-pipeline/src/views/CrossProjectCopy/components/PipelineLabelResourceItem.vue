<template>
    <!-- 流水线标签 -->
    <BaseResourceItem
        :key="labelResourceItem.copyStrategy"
        :item="labelResourceItem"
        :header-title="$t('processingStrategy')"
        :show-affected-count="false"
        :show-jump-icon="false"
        :strategies="strategyOptions"
        :is-read-only="isReadOnly"
        @strategy-change="handleStrategyChange"
    >
        <template #extra-config>
            <div
                v-if="isAutoReuse"
                class="label-config"
            >
                <bk-collapse v-model="activeCollapseNames">
                    <bk-collapse-item
                        v-for="labelGroup in filteredLabelsData"
                        :key="labelGroup.name"
                        :name="labelGroup.name"
                        hide-arrow
                    >
                        <div class="collapse-header">
                            <i
                                :class="['devops-icon icon-down-shape', {
                                    'is-collaped': !activeCollapseNames.includes(labelGroup.name)
                                }]"
                            />
                            <span class="header-title">{{ $t(labelGroup.titleKey, [labelGroup.labels.length]) }}</span>
                            <span class="header-subtitle">{{ $t(labelGroup.subtitleKey) }}</span>
                        </div>
                        <template #content>
                            <div class="label-tags">
                                <div
                                    v-for="label in labelGroup.labels"
                                    :key="label.resourceId"
                                    class="label-tag"
                                    :class="{ active: labelGroup.expandedLabelId === label.resourceId }"
                                    @click="toggleLabelExpand(labelGroup, label)"
                                >
                                    <span class="label-name">{{ label.resourceName }}</span>
                                    <span class="label-count">{{ label.pipelineReferCount }}</span>
                                </div>
                            </div>

                            <!-- 流水线列表区域 -->
                            <div
                                class="pipeline-list-container"
                                v-if="labelGroup.expandedLabelId"
                            >
                                <PipelineTableContent
                                    :key="labelGroup.expandedLabelId"
                                    :item="getCurrentLabel(labelGroup)"
                                    :project-id="projectId"
                                    :task-id="taskId"
                                    :table-max-height="320"
                                    :show-title="true"
                                    :title="getCurrentLabel(labelGroup) ? $t('affectPipelines', [getCurrentLabel(labelGroup).resourceName, getCurrentLabel(labelGroup).pipelineReferCount || 0]) : ''"
                                />
                            </div>
                        </template>
                    </bk-collapse-item>
                </bk-collapse>
            </div>
        </template>
    </BaseResourceItem>
</template>

<script>
    import BaseResourceItem from './BaseResourceItem.vue'
    import PipelineTableContent from './PipelineTableContent.vue'
    import { PipelineCopyStrategy } from '@/store/modules/crossProjectCopy/constants'

    export default {
        name: 'PipelineLabelResourceItem',
        components: {
            BaseResourceItem,
            PipelineTableContent
        },
        props: {
            item: {
                type: Object,
                required: true
            },
            // 是否只读模式
            isReadOnly: {
                type: Boolean,
                default: false
            }
        },
        data () {
            return {
                activeCollapseNames: ['reusable', 'new'],
                // 标签数据 UI 状态（labels 从 item.resources 获取）
                labelsData: [
                    {
                        name: 'reusable',
                        titleKey: 'reusableLabels',
                        subtitleKey: 'targetProjectExists',
                        labels: [],
                        // 每个分组独立的状态
                        expandedLabelId: null
                    },
                    {
                        name: 'new',
                        titleKey: 'newLabels',
                        subtitleKey: 'targetProjectNotExists',
                        labels: [],
                        expandedLabelId: null
                    }
                ]
            }
        },
        computed: {
            labelResourceItem () {
                return {
                    ...this.item,
                    copyStrategy: this.item.resources?.[0]?.copyStrategy
                }
            },
            projectId () {
                return this.$route.params.projectId
            },
            taskId () {
                return this.$route.params.taskId
            },
            isAutoReuse () {
                const copyStrategy = this.labelResourceItem.copyStrategy
                return copyStrategy === PipelineCopyStrategy.LABEL_AUTO_REUSE_OR_CREATE
            },
            strategyOptions () {
                return [
                    {
                        value: PipelineCopyStrategy.LABEL_AUTO_REUSE_OR_CREATE,
                        label: this.$t('autoReusePipelineLabel'),
                        description: this.$t('autoReusePipelineLabelDesc'),
                        disabled: false,
                        recommended: true
                    },
                    {
                        value: PipelineCopyStrategy.LABEL_IGNORE,
                        label: this.$t('skipAllPipelineLabel'),
                        description: this.$t('skipAllPipelineLabelDesc'),
                        disabled: false
                    }
                ]
            },
            filteredLabelsData () {
                return this.labelsData.filter(group => group.labels.length > 0)
            }
        },
        watch: {
            'labelResourceItem.copyStrategy': {
                handler (newVal) {
                    if (newVal === PipelineCopyStrategy.LABEL_AUTO_REUSE_OR_CREATE) {
                        this.initLabelsData(this.item.resources)
                    }
                },
                immediate: true
            },
            // 监听 isAutoReuse 变化（只读模式下需要自动加载标签数据）
            isAutoReuse: {
                handler (newVal) {
                    if (newVal && this.isReadOnly) {
                        this.initLabelsData(this.item.resources)
                    }
                },
                immediate: true
            }
        },
        methods: {
            getCurrentLabel (labelGroup) {
                if (!labelGroup.expandedLabelId) return null
                return labelGroup.labels.find(l => l.resourceId === labelGroup.expandedLabelId)
            },
            handleStrategyChange (value) {
                console.log("🚀 ~ value:", value)
                this.$emit('strategy-change', value)
            },
            initLabelsData (resources) {
                if (!Array.isArray(resources)) return
                this.labelsData[0].labels = []
                this.labelsData[1].labels = []

                resources.forEach(label => {
                    if (label.targetNameExists) {
                        this.labelsData[0].labels.push(label)
                    } else {
                        this.labelsData[1].labels.push(label)
                    }
                })
            },
            toggleLabelExpand (labelGroup, label) {
                if (labelGroup.expandedLabelId === label.resourceId) {
                    labelGroup.expandedLabelId = null
                } else {
                    labelGroup.expandedLabelId = label.resourceId
                }
            }
        }
    }
</script>

<style lang="scss" scoped>
.label-config {
    margin-top: 16px;

    ::v-deep .bk-collapse-item-header,
    ::v-deep  .bk-collapse-item-content {
        padding: 0;
    }

    ::v-deep .bk-collapse-item-header {
        height: auto;
    }

    ::v-deep  .bk-collapse-item-content {
        padding: 16px 24px 8px;
    }

    .collapse-header {
        display: flex;
        align-items: center;
        padding: 5px 8px;
        height: auto;
        margin-top: 16px;
        line-height: 20px;
        background: #F0F1F5;

        .devops-icon.icon-down-shape {
            font-size: 12px;
            color: #4D4F56;
            display: inline-flex;
            transition: all 0.3s ease;
            margin-right: 8px;
            &.is-collaped {
                transform: rotate(-90deg);
            }
        }

        .header-title {
            font-size: 14px;
            font-weight: 400;
            color: #4D4F56;
        }

        .header-subtitle {
            font-size: 12px;
            color: #979BA5;
            margin-left: 16px;
        }
    }

    .label-tags {
        display: flex;
        flex-wrap: wrap;
        gap: 8px;

        .label-tag {
            display: inline-flex;
            align-items: center;
            padding: 4px 12px;
            background: #F5F7FA;
            border: 1px solid #DCDEE5;
            border-radius: 2px;
            cursor: pointer;
            transition: all 0.3s;

            &:hover {
                border-color: #979BA5;
            }

            &.active {
                background: #F0F5FF;
                border-color: #699DF4;
                color: #3A84FF;

                .label-count {
                    background: #e1ecff;
                    color: #3A84FF;
                }
            }

            .label-name {
                font-size: 12px;
            }

            .label-count {
                margin-left: 8px;
                padding: 0 6px;
                color: #9a9ea8;
                background: #f0f1f5;
                border-radius: 8px;
                font-size: 12px;
                line-height: 16px;
            }
        }
    }
    .pipeline-list-container {
        margin-top: 16px;
        padding: 12px 16px 8px 16px;
        border-radius: 2px;
        border: 1px solid #DCDEE5;
    }
}
</style>
