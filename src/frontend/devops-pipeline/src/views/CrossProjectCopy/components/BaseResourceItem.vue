<template>
    <div
        class="detail-item"
        :style="{ borderTop: isReadOnly ? 'none' : '1px solid #C4C6CC' }"
    >
        <!-- Header 区域 -->
        <div
            class="detail-item-header"
            v-if="!isLabelOrGroupReadOnly"
        >
            <div>
                <!-- 待处理状态：
                    1. 其他资源类型：item.status === 'UNPROCESSED'
                    2. 流水线标签/分组：item.unprocessedCount > 0（只要还有待处理的，就显示待处理图标）
                -->
                <span
                    v-if="item.status === 'UNPROCESSED' || item.unprocessedCount > 0"
                    class="pending-icon"
                ></span>
                <!-- 插槽：自定义 header 内容 -->
                <slot name="header">
                    <span class="header-name">{{ headerTitle }}</span>
                    <a
                        v-if="showJumpIcon"
                        class="jump-icon"
                        @click="handleJump"
                    >
                        <Logo
                            name="tiaozhuan"
                            size="16"
                        />
                    </a>
                </slot>
                <!-- 插槽：header 额外信息 -->
                <slot name="header-extra"></slot>
            </div>
            <!-- 受影响流水线数量 -->
            <p
                v-if="showAffectedCount && item.pipelineReferCount > 0"
                class="affected-pipeline-count"
                :class="{ clickable: item.resourceType }"
                @click="handleShowPipelines"
            >
                <span>{{ $t('affectedPipelineCount', [item.pipelineReferCount]) }}</span>
                <Logo
                    name="list-line"
                    size="12"
                    class="list-line"
                />
            </p>
        </div>

        <!-- 策略选择区域 -->

        <!-- 只读模式：显示纯文本策略 -->
        <div
            v-if="isReadOnly"
            class="strategy-readonly"
            :style="{ borderBottom: !isLabelOrGroupReadOnly ? '1px solid #C4C6CC' : 'none' }"
        >
            <p>
                <span class="readonly-label">{{ $t('currentStrategy') }}：</span>
                <span class="readonly-value">{{ currentStrategyLabel }}</span>
            </p>
            <p v-if="item.copyStrategy === PipelineCopyStrategy.CREDENTIAL_REPLACE_TARGET && item.resourceType === 'CREDENTIAL'">
                <span class="readonly-label">{{ $t('targetProjectCredential') }}：</span>
                <span class="readonly-value">{{ item.targetResourceName }}</span>
            </p>
        </div>
        <!-- 编辑模式：显示策略选择 -->
        <bk-radio-group
            v-else
            class="strategy-group"
            :value="item.copyStrategy"
            @change="handleStrategyChange"
        >
            <div
                v-for="strategy in strategies"
                :key="strategy.value"
                class="strategy-item"
                :class="{
                    active: item.copyStrategy === strategy.value,
                    disabled: strategy.disabled,
                    'high-risk': strategy.highRisk
                }"
            >
                <bk-radio
                    :value="strategy.value"
                    :disabled="strategy.disabled"
                >
                    {{ strategy.label }}
                    <!-- 不可用时的提示 -->
                    <span
                        v-if="strategy.disabled && strategy.disabledTip"
                        class="no-same-name"
                    >
                        <i class="bk-icon icon-info-circle" />
                        {{ strategy.disabledTip }}
                    </span>
                    <!-- 跳转图标 -->
                    <!-- <Logo
                        v-if="strategy.showJumpIcon"
                        name="tiaozhuan"
                        size="16"
                        class="jump-icon"
                        v-bk-tooltips="$t('details.viewDetail')"
                    /> -->
                    <!-- 高风险标识 -->
                    <span
                        v-if="strategy.highRisk"
                        class="high-risk-badge"
                    >
                        {{ $t('highRisk') }}
                    </span>
                    <!-- 推荐标识 -->
                    <span
                        v-if="strategy.recommended"
                        class="recommend-badge"
                    >
                        {{ $t('recommendBadge') }}
                    </span>
                </bk-radio>
                <p class="strategy-desc">{{ strategy.description }}</p>
            </div>
        </bk-radio-group>

        <!-- 插槽：策略选择后的额外配置区域（如授权配置、凭据选择等） -->
        <slot name="extra-config"></slot>

        <!-- 影响流水线详情弹窗 -->
        <bk-dialog
            v-model="pipelineDialogVisible"
            header-position="left"
            :title="$t('affectedPipelineDetail')"
            :width="640"
            :mask-close="false"
            :esc-close="false"
        >
            <div class="pipeline-dialog-content">
                <PipelineTableContent
                    v-if="pipelineDialogVisible"
                    :item="item"
                    :project-id="projectId"
                    :task-id="taskId"
                    :table-max-height="400"
                    :show-title="true"
                    :title="$t('affectPipelines', [item.resourceName, item.pipelineReferCount])"
                />
            </div>
        </bk-dialog>
    </div>
</template>

<script>
    import Logo from '@/components/Logo'
    import PipelineTableContent from './PipelineTableContent.vue'
    import { PipelineCopyStrategy } from '@/store/modules/crossProjectCopy/constants'

    export default {
        name: 'BaseResourceItem',
        components: {
            Logo,
            PipelineTableContent
        },
        props: {
            // 资源数据项
            item: {
                type: Object,
                required: true
            },
            // Header 标题（当不使用 header 插槽时）
            headerTitle: {
                type: String,
                default: ''
            },
            // 是否显示跳转图标
            showJumpIcon: {
                type: Boolean,
                default: true
            },
            // 是否显示受影响流水线数量
            showAffectedCount: {
                type: Boolean,
                default: true
            },
            // 策略配置列表
            strategies: {
                type: Array,
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
                pipelineDialogVisible: false
            }
        },
        computed: {
            projectId () {
                return this.$route.params.projectId
            },
            taskId () {
                return this.$route.params.taskId
            },
            // 是否是流水线标签或分组
            isLabelOrGroupReadOnly () {
                const isLabelOrGroup = ['PIPELINE_LABEL', 'PIPELINE_GROUP'].includes(this.item.resourceType)
                return isLabelOrGroup
            },
            // 当前选中的策略中文名称
            currentStrategyLabel () {
                const copyStrategy = this.isLabelOrGroupReadOnly
                    ? this.item.resources?.[0]?.copyStrategy
                    : this.item.copyStrategy

                const currentStrategy = this.strategies.find(s => s.value === copyStrategy)
                return currentStrategy?.label || '--'
            }
        },
        created () {
            this.PipelineCopyStrategy = PipelineCopyStrategy
        },
        methods: {
            handleStrategyChange (value) {
                this.$emit('strategy-change', value)
            },
            handleShowPipelines () {
                this.pipelineDialogVisible = true
            },
            handleJump () {
                const { resourceType, resourceId, resourceProperties } = this.item
                const projectId = this.projectId
                let url = ''
                switch (resourceType) {
                    case 'PIPELINE_TEMPLATE':
                        url = `/console/pipeline/${projectId}/template/${resourceId}`
                        break
                    case 'REPOSITORY':
                        url = `/console/codelib/${projectId}/?id=${resourceId}&scmType=${resourceProperties.scmCode}`
                        break
                    case 'BUILD_ENV':
                    case 'DEPLOY_ENV':
                        url = `/console/environment/${projectId}/envDetail/${resourceId}`
                        break
                    case 'BUILD_NODE':
                    case 'DEPLOY_NODE':
                        url = `/console/environment/${projectId}/node/nodeDetail/${resourceId}`
                        break
                    case 'CREDENTIAL':
                        url = `/console/ticket/${projectId}`
                        break
                }
                if (url) {
                    window.open(url, '_blank')
                }
            }
        }
    }
</script>

<style lang="scss" scoped>
    @import '@/scss/resource-dependency';

    .affected-pipeline-count {
        &.clickable {
            cursor: pointer;
        }
    }
    .pipeline-dialog-content {
        margin-top: 8px;
    }

    .strategy-readonly {
        display: flex;
        align-items: center;
        justify-content: space-between;
        margin-top: 10px;
        font-size: 12px;
        background-color: #FAFBFD;
        padding: 18px 24px;
        p {
            flex: 1;
        }

        .readonly-label {
            color: #979BA5;
        }

        .readonly-value {
            color: #313238;
        }
    }
</style>
