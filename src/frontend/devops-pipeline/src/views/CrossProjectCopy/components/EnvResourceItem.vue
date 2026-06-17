<template>
    <!-- 构建环境、部署环境 -->
    <BaseResourceItem
        :item="item"
        :header-title="item.resourceName"
        :strategies="strategyOptions"
        :is-read-only="isReadOnly"
        @strategy-change="handleChange"
    />
</template>

<script>
    import BaseResourceItem from './BaseResourceItem.vue'
    import { PipelineCopyResourceType, PipelineCopyStrategy } from '@/store/modules/crossProjectCopy/constants'

    // 构建环境 / 部署环境 策略配置
    const STRATEGY_CONFIG = {
        [PipelineCopyResourceType.BUILD_ENV]: [
            { value: PipelineCopyStrategy.BUILD_ENV_REUSE_SAME_NAME, labelKey: 'reuseTargetBuildEnv', descKey: 'reuseTargetBuildEnvDesc', disabledTipKey: 'noSameNameEnv' },
            { value: PipelineCopyStrategy.BUILD_ENV_CREATE_WITHOUT_NODE, labelKey: 'createNewEnv', descKey: 'createNewEnvDesc' },
            { value: PipelineCopyStrategy.BUILD_ENV_CREATE_AND_REUSE_SAME_NAME_NODE, labelKey: 'createNewEnvAndLink', descKey: 'createNewEnvAndLinkDesc' },
            { value: PipelineCopyStrategy.BUILD_ENV_CREATE_AND_MOVE_NODE, labelKey: 'createAndTransferNodes', descKey: 'createAndTransferNodesDesc', highRisk: true }
        ],
        [PipelineCopyResourceType.DEPLOY_ENV]: [
            { value: PipelineCopyStrategy.DEPLOY_ENV_REUSE_SAME_NAME, labelKey: 'reuseTargetBuildEnv', descKey: 'reuseTargetBuildEnvDesc', disabledTipKey: 'noSameNameEnv' },
            { value: PipelineCopyStrategy.DEPLOY_ENV_CREATE_WITHOUT_NODE, labelKey: 'createNewEnv', descKey: 'createNewEnvDesc' },
            { value: PipelineCopyStrategy.DEPLOY_ENV_CREATE_AND_REUSE_SAME_NAME_NODE, labelKey: 'createNewEnvAndLink', descKey: 'createNewEnvAndLinkDesc' },
            { value: PipelineCopyStrategy.DEPLOY_ENV_CREATE_AND_MOVE_NODE, labelKey: 'createAndTransferNodes', descKey: 'createAndTransferNodesDesc', highRisk: true }
        ]
    }

    export default {
        name: 'EnvResourceItem',
        components: {
            BaseResourceItem
        },
        props: {
            // 环境数据项（构建环境或部署环境）
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
        computed: {
            strategyOptions () {
                const config = STRATEGY_CONFIG[this.item.resourceType] || []
                return config.map(opt => ({
                    value: opt.value,
                    label: this.$t(opt.labelKey),
                    description: this.$t(opt.descKey),
                    disabled: opt.disabledTipKey ? !this.item.targetNameExists : false,
                    disabledTip: opt.disabledTipKey && !this.item.targetNameExists ? this.$t(opt.disabledTipKey) : '',
                    showJumpIcon: opt.disabledTipKey ? this.item.targetNameExists : false,
                    highRisk: opt.highRisk || false
                }))
            }
        },
        methods: {
            handleChange (value) {
                this.$emit('strategy-change', value)
            }
        }
    }
</script>
