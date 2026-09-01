<template>
    <div class="risk-warning-content">
        <!-- 顶部提示文案 -->
        <p>
            <i18n
                v-if="contentConfig.isTicket"
                :path="contentConfig.transferRiskPath"
                tag="span"
            >
                <span class="bold">{{ $t('copy') }}</span>
            </i18n>
            <i18n
                v-else
                :path="contentConfig.transferRiskPath"
                tag="span"
            >
                <span class="bold">{{ $t('transfer') }}</span>
                <span class="bold">{{ $t('rollbackUnsupported') }}</span>
            </i18n>
        </p>
        
        <!-- 资源名称列表 -->
        <div
            v-if="resourceNames && resourceNames.length"
            class="env-name"
        >
            <p
                v-for="(name, index) in resourceNames"
                :key="index"
                v-bk-tooltips="name"
            >
                {{ name }}
            </p>
        </div>
        
        <!-- 黄色提示框 -->
        <div class="err-tips">
            <!-- 操作影响 -->
            <div class="risk-section">
                <span class="risk-label">{{ $t('operationImpact') }}</span>
                <span>{{ $t(contentConfig.operationImpactDescKey) }}</span>
            </div>
            
            <!-- 源项目风险 -->
            <div
                v-if="contentConfig.showSourceProjectRisk"
                class="risk-section"
            >
                <span class="risk-label">{{ $t('sourceProjectRisk') }}</span>
                <i18n
                    v-if="!contentConfig.isNodeSourceRisk"
                    path="sourceProjectRiskDesc"
                    tag="span"
                >
                    <span class="risk-highlight">{{ $t('loseNodeUsage') }}</span>
                    <span class="risk-highlight">{{ $t('unableBuild') }}</span>
                </i18n>
                <i18n
                    v-else
                    path="nodeSourceProjectRiskDesc"
                    tag="span"
                >
                    <span class="risk-highlight">{{ $t('nodeLoseNodeUsage') }}</span>
                    <span class="risk-highlight">{{ $t('unableBuild') }}</span>
                </i18n>
            </div>
            
            <!-- 确认前置条件 -->
            <div
                v-if="contentConfig.showConfirmPrerequisite"
                class="risk-section"
            >
                <span class="risk-label">{{ $t('confirmPrerequisite') }}</span>
                <i18n
                    path="confirmPrerequisiteDesc"
                    tag="span"
                >
                    <span class="risk-highlight">{{ $t('alreadySwitched') }}</span>
                    <span class="risk-highlight">{{ $t('canStop') }}</span>
                </i18n>
            </div>
            
            <!-- 后续处理 -->
            <div
                v-if="contentConfig.showSubsequentProcessing"
                class="risk-section"
            >
                <span class="risk-label">{{ $t('subsequentProcessing') }}</span>
                <template v-if="resourceType === PipelineCopyResourceType.BUILD_ENV || resourceType === PipelineCopyResourceType.BUILD_NODE">
                    <ol class="risk-ol">
                        <li v-if="resourceType === PipelineCopyResourceType.BUILD_ENV">
                            <i18n
                                path="subsequentProcessing1"
                                tag="span"
                            >
                                <span class="risk-highlight">devops.project.id</span>
                            </i18n>
                        </li>
                        <li v-if="resourceType === PipelineCopyResourceType.BUILD_ENV">
                            {{ $t('subsequentProcessing2') }}
                        </li>
                        <li v-if="resourceType === PipelineCopyResourceType.BUILD_NODE">
                            <i18n
                                path="nodeSubsequentProcessing1"
                                tag="span"
                            >
                                <span class="risk-highlight">devops.project.id</span>
                            </i18n>
                        </li>
                        <li v-if="resourceType === PipelineCopyResourceType.BUILD_NODE">
                            {{ $t('nodeSubsequentProcessing2') }}
                        </li>
                    </ol>
                </template>
                <span v-if="resourceType === PipelineCopyResourceType.DEPLOY_ENV">{{ $t('deployEnvSubsequentProcessing') }}</span>
                <span v-if="resourceType === PipelineCopyResourceType.DEPLOY_NODE">{{ $t('deployNodeSubsequentProcessing') }}</span>
            </div>
            
            <!-- 审计提醒 -->
            <div
                v-if="contentConfig.showAuditReminder"
                class="risk-section"
            >
                <span class="risk-label">{{ $t('auditReminder') }}</span>
                <i18n
                    path="credentialAuditReminderDesc"
                    tag="span"
                >
                    <span class="risk-highlight">{{ $t('highSensitiveDataOperation') }}</span>
                    <span class="risk-highlight">{{ $t('auditTrail') }}</span>
                </i18n>
            </div>
        </div>
    </div>
</template>

<script>
    import { PipelineCopyResourceType } from '@/store/modules/crossProjectCopy/constants'
    export default {
        name: 'RiskWarningContent',
        props: {
            // 资源名称列表（单个或多个）
            resourceNames: {
                type: Array,
                default: () => []
            },
            resourceType: {
                type: String,
                default: ''
            }
        },
        computed: {
            // 内容配置
            contentConfig () {
                const type = this.resourceType
                const isTicket = type === PipelineCopyResourceType.CREDENTIAL
                const isNode = type === PipelineCopyResourceType.BUILD_NODE || type === PipelineCopyResourceType.DEPLOY_NODE

                const transferRiskPathMap = {
                    [PipelineCopyResourceType.BUILD_ENV]: 'buildEnvTransferRiskWarning',
                    [PipelineCopyResourceType.DEPLOY_ENV]: 'deployEnvTransferRiskWarning',
                    [PipelineCopyResourceType.BUILD_NODE]: 'buildNodeTransferRiskWarning',
                    [PipelineCopyResourceType.DEPLOY_NODE]: 'deployNodeTransferRiskWarning',
                    [PipelineCopyResourceType.CREDENTIAL]: 'credentialTransferRiskWarning'
                }

                const operationImpactDescKeyMap = {
                    [PipelineCopyResourceType.BUILD_ENV]: 'operationImpactDesc',
                    [PipelineCopyResourceType.DEPLOY_ENV]: 'deployEnvOperationImpactDesc',
                    [PipelineCopyResourceType.BUILD_NODE]: 'buildNodeOperationImpactDesc',
                    [PipelineCopyResourceType.DEPLOY_NODE]: 'deployNodeOperationImpactDesc',
                    [PipelineCopyResourceType.CREDENTIAL]: 'credentialOperationImpactDesc'
                }

                return {
                    isTicket,
                    isNode,
                    showSourceProjectRisk: !isTicket,
                    showConfirmPrerequisite: !isTicket,
                    showSubsequentProcessing: !isTicket,
                    showAuditReminder: isTicket,
                    transferRiskPath: transferRiskPathMap[type] || '',
                    operationImpactDescKey: operationImpactDescKeyMap[type] || 'operationImpactDesc',
                    isNodeSourceRisk: isNode
                }
            }
        },
        created () {
            this.PipelineCopyResourceType = PipelineCopyResourceType
        }
    }
</script>

<style lang="scss" scoped>
    .risk-warning-content {
        font-size: 12px;
        color: #000000;
        margin-top: 12px;

        .bold {
            font-weight: 700;
        }

        .env-name {
            display: flex;
            flex-direction: column;
            gap: 8px;
            padding: 8px 16px;
            border-radius: 2px;
            background: #F5F7FA;
            color: #4D4F56;
            margin: 8px 0 16px;
            p {
                width: 100%;
            }
        }
        
        .err-tips {
            background: #FEF2F2;
            border-radius: 2px;
            padding: 12px 16px;

            .risk-section {
                display: flex;
                margin-bottom: 8px;
                line-height: 20px;
                color: #4D4F56;

                &:last-child {
                    margin-bottom: 0;
                }
            }

            .risk-label {
                display: inline-block;
                font-weight: 700;
                flex-shrink: 0;
                width: 80px;
                color: #313238;
                margin: 0 8px 0 16px;
            }

            .risk-highlight {
                flex: 1;
                color: #E71818;
            }

            .risk-ol {
                flex: 1;
                padding-left: 12px;
                margin: 4px 0 0;

                li {
                    margin-bottom: 4px;
                    line-height: 20px;
                    list-style: decimal !important;

                    &:last-child {
                        margin-bottom: 0;
                    }
                }
            }
        }
    }
</style>
