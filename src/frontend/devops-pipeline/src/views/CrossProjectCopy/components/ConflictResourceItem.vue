<template>
    <!-- 流水线冲突 -->
    <div
        class="detail-item"
        :style="{ marginBottom: isReadOnly ? '' : '12px', borderTop: isReadOnly ? '1px solid #C4C6CC' : '', borderBottom: (isReadOnly && isLast) ? '1px solid #C4C6CC' : '' }"
    >
        <div class="detail-item-header conflict-header">
            <!-- 源流水线信息 -->
            <div>
                <span
                    v-if="item.status === 'UNPROCESSED'"
                    class="pending-icon"
                ></span>
                <span class="source-label">{{ $t('source') }}</span>
                <span class="header-name">{{ item.resourceName }}</span>
                <a
                    class="jump-icon"
                    @click="handleJump"
                >
                    <Logo
                        name="tiaozhuan"
                        size="16"
                    />
                </a>
                <p class="repo-url">
                    <span>{{ item.resourceId }}</span>
                </p>
            </div>

            <!-- 目标流水线信息 -->
            <div class="target-content">
                <Logo
                    v-if="!isReadOnly || (isReadOnly && item.copyStrategy !== PipelineCopyStrategy.PIPELINE_SKIP)"
                    name="arrow-right"
                    size="14"
                    class="arrow-right"
                    :style="{ color: !isReadOnly ? '#F59500' : '#C4C6CC' }"
                />
                <div class="target-pipelines">
                    <!-- nameConflict 目标流水线 -->
                    <div
                        v-if="nameConflictData"
                        class="target-item"
                    >
                        <div class="pipeline-info">
                            <span class="target-label">{{ $t('target') }}</span>
                            <div class="pipeline-info-content">
                                <p>
                                    <span class="target-name name-conflict">{{ item.copyStrategy === PipelineCopyStrategy.PIPELINE_AUTO_RESOLVE_CONFLICT ? item.targetResourceName : nameConflictData.pipelineName }}</span>
                                    <Logo
                                        v-if="!item.copyStrategy"
                                        name="tiaozhuan"
                                        size="16"
                                        class="jump-icon"
                                        @click="handleJumpToTarget(nameConflictData.pipelineId)"
                                    />
                                </p>
                                <p class="repo-url">
                                    <span>{{ nameConflictData.pipelineId }}</span>
                                    <span class="creator-info">{{ $t('creator') }}：{{ nameConflictData.creator }}</span>
                                </p>
                            </div>
                        </div>
                    </div>

                    <!-- idConflict 目标流水线 -->
                    <div
                        v-if="idConflictData"
                        class="target-item"
                    >
                        <div class="pipeline-info">
                            <span class="target-label">{{ $t('target') }}</span>
                            <div class="pipeline-info-content">
                                <p>
                                    <span class="target-name">{{ idConflictData.pipelineName }}</span>
                                    <Logo
                                        v-if="!item.copyStrategy"
                                        name="tiaozhuan"
                                        size="16"
                                        class="jump-icon"
                                        @click="handleJumpToTarget(idConflictData.pipelineId)"
                                    />
                                </p>
                                <!-- ID 冲突：自动解决时显示提示，否则显示 pipelineId 和 creator -->
                                <p
                                    v-if="isAutoResolve && idConflictData"
                                    class="auto-id-tip"
                                >
                                    （{{ $t('autoGenerateNewId') }}）
                                </p>
                                <p
                                    v-else
                                    class="repo-url"
                                >
                                    <span class="id-conflict">{{ idConflictData.pipelineId }}</span>
                                    <span class="creator-info">{{ $t('creator') }}：{{ idConflictData.creator }}</span>
                                </p>
                            </div>
                        </div>
                    </div>
                    <div
                        v-if="isReadOnly && item.copyStrategy === PipelineCopyStrategy.PIPELINE_SKIP"
                        class="read-only-skip"
                    >
                        {{ $t('skipThisPipeline') }}
                    </div>
                </div>
            </div>

            <!-- 冲突标签 -->
            <div
                v-if="!isReadOnly"
                class="conflict-badges"
            >
                <span
                    v-if="nameConflictData"
                    class="conflict-badge name-conflict-badge"
                >
                    {{ $t('nameConflict') }}
                </span>
                <span
                    v-if="idConflictData"
                    class="conflict-badge id-conflict-badge"
                >
                    {{ $t('idConflict') }}
                </span>
            </div>
        </div>

        <!-- 冲突处理选择区域 -->
        <bk-radio-group
            v-if="!isReadOnly"
            class="strategy-group"
            :value="item.copyStrategy"
            @change="handleChange"
        >
            <!-- 自动解决冲突选项 -->
            <div
                class="strategy-item"
                :class="{ active: isAutoResolve }"
            >
                <bk-radio :value="strategyValues.autoResolve">
                    {{ $t('autoResolveConflict') }}
                </bk-radio>
                <p class="strategy-desc">{{ autoResolveDesc }}</p>
            </div>

            <!-- 跳过此流水线选项 -->
            <div
                class="strategy-item"
                :class="{ active: isSkip }"
            >
                <bk-radio :value="strategyValues.skip">{{ $t('skipThisPipeline') }}</bk-radio>
                <p class="strategy-desc">{{ $t('skipThisPipelineDesc') }}</p>
            </div>
        </bk-radio-group>
    </div>
</template>

<script>
    import Logo from '@/components/Logo'
    import { getTimestamp } from '@/utils/util'
    import { PipelineCopyStrategy } from '@/store/modules/crossProjectCopy/constants'

    export default {
        name: 'ConflictResourceItem',
        components: {
            Logo
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
            },
            // 是否为最后一个
            isLast: {
                type: Boolean,
                default: false
            }
        },
        data () {
            return {
                // 存储初始流水线名称，用于切换策略时恢复
                initialPipelineName: '',
                // 存储 targetResourceName 的初始值
                initialTargetResourceName: ''
            }
        },
        computed: {
            projectId () {
                return this.$route.params.projectId
            },
            nameConflictData () {
                return this.item.resourceProperties.nameConflict || null
            },
            idConflictData () {
                return this.item.resourceProperties.idConflict || null
            },
            isAutoResolve () {
                return this.item.copyStrategy === PipelineCopyStrategy.PIPELINE_AUTO_RESOLVE_CONFLICT
            },
            isSkip () {
                return this.item.copyStrategy === PipelineCopyStrategy.PIPELINE_SKIP
            },
            strategyValues () {
                return {
                    autoResolve: PipelineCopyStrategy.PIPELINE_AUTO_RESOLVE_CONFLICT,
                    skip: PipelineCopyStrategy.PIPELINE_SKIP
                }
            },
            autoResolveDesc () {
                const desc1 = this.$t('autoResolveConflictDesc1')
                const desc2 = this.$t('autoResolveConflictDesc2')
                if (this.nameConflictData && !this.idConflictData) {
                    return `${desc1}。`
                } else if (!this.nameConflictData && this.idConflictData) {
                    return `${desc2}。`
                } else if (this.nameConflictData && this.idConflictData) {
                    return `${desc1}；${desc2}`
                }
                return ''
            }
        },
        watch: {
            nameConflictData: {
                immediate: true,
                handler (val) {
                    if (val && !this.initialPipelineName) {
                        this.initialPipelineName = val.pipelineName
                    }
                }
            }
        },
        created () {
            this.PipelineCopyStrategy = PipelineCopyStrategy
            // 保存 targetResourceName 的初始值
            this.initialTargetResourceName = this.item.targetResourceName || ''
        },
        methods: {
            // 处理策略变更
            handleChange (value) {
                if (value === PipelineCopyStrategy.PIPELINE_AUTO_RESOLVE_CONFLICT) {
                    // 自动解决冲突：
                    if (this.nameConflictData) {
                        if (this.initialTargetResourceName) {
                            this.item.targetResourceName = this.initialTargetResourceName
                            this.item.resourceProperties.nameConflict.pipelineName = this.initialTargetResourceName
                        } else {
                            const timestamp = getTimestamp()
                            const newName = `${this.initialPipelineName}_${timestamp}`
                            this.item.targetResourceName = newName

                            this.item.resourceProperties.nameConflict.pipelineName = newName
                        }
                    }
                } else if (value === PipelineCopyStrategy.PIPELINE_SKIP) {
                    // 切换到跳过时，恢复初始名称
                    if (this.nameConflictData) {
                        this.item.resourceProperties.nameConflict.pipelineName = this.initialPipelineName
                        this.item.targetResourceName = this.initialPipelineName
                    }
                }
                this.$emit('strategy-change', value)
            },
            // 这里待考量
            handleJump () {
                window.open(`/console/pipeline/${this.projectId}/${this.item.resourceId}/history/pipeline`, '__blank')
            },
            // 跳转到目标流水线
            handleJumpToTarget (pipelineId) {
                if (!pipelineId) return
                window.open(`/console/pipeline/${this.projectId}/${pipelineId}/history/pipeline`, '__blank')
            }
        }
    }
</script>

<style lang="scss" scoped>
    @import '@/scss/resource-dependency';
    .read-only-skip {
        font-size: 12px;
        color: #979BA5;
    }
</style>
