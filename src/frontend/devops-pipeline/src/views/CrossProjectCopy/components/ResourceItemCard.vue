<template>
    <div class="resource-item-card">
        <!-- 左侧内容 -->
        <div class="resource-left">
            <!-- 资源标题 -->
            <div class="resource-title">
                <span class="title-text">
                    {{ resourceTypeText }}：{{ data.resourceName }}
                </span>
                <a @click="handleJump">
                    <Logo
                        class="title-link"
                        name="tiaozhuan"
                        size="14"
                    />
                </a>
                <span
                    v-if="data.copyAction === 'NEED_COMPLETION'"
                    class="status-tag"
                >{{ $t('alreadyNewEmptyEnv') }}</span>
                
                <!-- <template v-else>
                    <i18n
                        v-if="data.resourceType === 'BUILD_ENV'"
                        path="alreadyNewEnvAndTransferBuildNode"
                        tag="span"
                        class="status-tag"
                    >
                        <span style="color:#3A84FF">{{ data.count || 0 }}</span>
                    </i18n>

                    <i18n
                        v-if="data.resourceType === 'BUILD_NODE'"
                        path="alreadyTransferBuildNode"
                        tag="span"
                        class="status-tag"
                    >
                        <span style="color:#3A84FF">{{ data.count || 0 }}</span>
                    </i18n>

                    <i18n
                        v-if="data.resourceType === 'DEPLOY_ENV'"
                        path="alreadyNewEnvAndTransferDeployNode"
                        tag="span"
                        class="status-tag"
                    >
                        <span style="color:#3A84FF">{{ data.count || 0 }}</span>
                    </i18n>

                    <i18n
                        v-if="data.resourceType === 'DEPLOY_NODE'"
                        path="alreadyTransferDeployNode"
                        tag="span"
                        class="status-tag"
                    >
                        <span style="color:#3A84FF">{{ data.count || 0 }}</span>
                    </i18n>
                </template> -->
            </div>
            
            <!-- 处理策略 -->
            <div class="resource-info">
                <p class="info-label">{{ $t('currentStrategy') }}</p>
                <p class="info-value">{{ strategyText }}</p>
            </div>
        </div>
        
        <!-- 右侧内容 -->
        <div
            class="resource-right"
            :class="{ 'is-pending': data.status === 'UNPROCESSED' }"
        >
            <div
                v-if="showDescription"
                class="resour-item"
            >
                <!-- 消息提示 -->
                <p class="message-text">
                    <span class="message-label">{{ $t('targetProject') }}：</span>
                    {{ descriptionText }}
                </p>
                <bk-checkbox
                    v-if="data.status === 'UNPROCESSED'"
                    :value="data.confirmed"
                    @change="handleComplete"
                >
                    {{ actionButtonText }}
                </bk-checkbox>
            </div>
            
            <!-- 操作区域 -->
            <!-- <div
                class="resour-item"
                v-if="data.sourceProject"
            >
                <p class="message-text">
                    <span class="message-label">源项目:</span>
                    {{ data.sourceProject }}
                </p>
                <bk-button
                    v-if="data.showRefreshButton"
                    text
                    theme="primary"
                    @click="handleRefresh"
                >
                    <i class="bk-icon icon-refresh"></i>
                    刷新检查
                </bk-button>
            </div> -->
        </div>
    </div>
</template>

<script>
    import Logo from '@/components/Logo'
    import {
        PipelineCopyResourceStatus,
        PipelineCopyAction,
        PipelineCopyResourceType,
        PipelineCopyResourceTypeI18nKey
    } from '@/store/modules/crossProjectCopy/constants.js'
    export default {
        name: 'ResourceItemCard',
        components: {
            Logo
        },
        props: {
            data: {
                type: Object,
                required: true,
                validator: (data) => {
                    return data.id && data.resourceType && data.resourceName
                }
            },
            actionButtonText: {
                type: String,
                default: '我已处理'
            }
        },
        data () {
            return {
            }
        },
        computed: {
            projectId () {
                return this.$route.params.projectId
            },
            // 资源类型文本
            resourceTypeText () {
                const i18nKey = PipelineCopyResourceTypeI18nKey[this.data.resourceType]
                return i18nKey ? this.$t(i18nKey) : this.data.resourceType
            },
        
            // 策略文本
            strategyText () {
                const i18nKey = this.data.copyStrategy
                return this.$t(i18nKey)
            },

            descriptionText () {
                const { resourceType, confirmed, copyAction } = this.data

                // NEED_TRANSFER：资源转移
                if (copyAction === PipelineCopyAction.NEED_TRANSFER) {
                    const isNode = resourceType === PipelineCopyResourceType.DEPLOY_NODE
                        || resourceType === PipelineCopyResourceType.BUILD_NODE
                    const isEnv = resourceType === PipelineCopyResourceType.BUILD_ENV
                        || resourceType === PipelineCopyResourceType.DEPLOY_ENV

                    if (isNode) {
                        return this.$t('updateBuildNodeAgent')
                    }
                    if (isEnv) {
                        return this.$t('updateBuildMachineAgent')
                    }
                    return ''
                }

                // NEED_COMPLETION：资源补齐
                if (copyAction === PipelineCopyAction.NEED_COMPLETION) {
                    if (confirmed === false) {
                        return this.$t('addNodeAndVerifyPipeline')
                    }
                    return this.$t('addedNodeSuggestVerifyPipeline')
                }

                return ''
            },

            showDescription () {
                if (this.data.copyAction === PipelineCopyAction.NEED_COMPLETION) return true
                if (this.data.copyAction === PipelineCopyAction.NEED_TRANSFER) {
                    const rt = this.data.resourceType
                    return rt === PipelineCopyResourceType.BUILD_NODE
                        || rt === PipelineCopyResourceType.BUILD_ENV
                }
                return false
            }
        },
        methods: {
            handleComplete (value) {
                this.$emit('handle-complete', {
                    ...this.data,
                    confirmed: value,
                    status: value ? PipelineCopyResourceStatus.PROCESSED : PipelineCopyResourceStatus.UNPROCESSED
                })
            },
            handleRefresh () {
                this.$emit('refresh-check', this.data)
            },
            handleJump () {
                const { resourceType, resourceId } = this.data
                const projectId = this.projectId
                let url = ''
                switch (resourceType) {
                    case 'BUILD_ENV':
                    case 'DEPLOY_ENV':
                        url = `/console/environment/${projectId}/pipeline/env/ALL/${resourceId}/node`
                        break
                    case 'BUILD_NODE':
                    case 'DEPLOY_NODE':
                        url = `/console/environment/${projectId}/pipeline/node/allNode?nodeHashId=${resourceId}`
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
.resource-item-card {
    display: flex;
    gap: 12px;
    padding-top: 12px;
    margin-bottom: 12px;
    border-top: 1px solid #C4C6CC;
    
    // 左侧内容
    .resource-left {
        flex: 1;
        min-width: 0;
        padding: 3px 0;
        
        .resource-title {
            display: flex;
            align-items: center;
            gap: 8px;
            margin-bottom: 14px;
            
            .title-text {
                font-size: 14px;
                font-weight: 600;
                color: #4D4F56;
                font-weight: 700;
            }
            
            .title-link {
                color: #979BA5;

                &:hover {
                    color: #699DF4;
                }
            }
            
            .status-tag {
                color: #979ba5;
                font-size: 12px;
            }
        }
        
        .resource-info {
            margin-left: 16px;

            .info-label {
                font-size: 12px;
                color: #979BA5;
                margin-bottom: 6px;
            }
            
            .info-value {
                font-size: 12px;
                color: #4D4F56;
            }
        }
    }
    
    // 右侧内容
    .resource-right {
        flex: 1;
        padding: 8px 16px 8px 12px;
        flex-direction: column;
        gap: 6px;
        border-radius: 4px;
        border-left: 2px solid #C4C6CC;
        background-color: #FAFBFD;
        color: #979BA5;
        font-size: 12px;
        
        // 待补齐状态 - 橙色边框
        &.is-pending {
            border-left-color: #F59500;
            color: #4D4F56;
        }
        
        .resour-item {
            display: flex;
            align-items: center;
            justify-content: space-between;
            border-bottom: 1px solid #DCDEE5;
            padding: 6px 0;
        }
    }
}
</style>
