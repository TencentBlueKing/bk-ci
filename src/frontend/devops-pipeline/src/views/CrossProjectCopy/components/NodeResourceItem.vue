<template>
    <!-- 构建节点、部署节点 -->
    <BaseResourceItem
        :item="item"
        :header-title="item.resourceName"
        :strategies="strategyOptions"
        @strategy-change="handleChange"
    />
</template>

<script>
    import BaseResourceItem from './BaseResourceItem.vue'
    import { PipelineCopyResourceType, PipelineCopyStrategy } from '@/store/modules/crossProjectCopy/constants'

    // 构建节点 / 部署节点 策略配置
    const STRATEGY_CONFIG = {
        [PipelineCopyResourceType.BUILD_NODE]: [
            { value: PipelineCopyStrategy.BUILD_NODE_REUSE_SAME_NAME, labelKey: 'reuseTargetBuildNode', descKey: 'reuseTargetBuildNodeDesc', disabledTipKey: 'noSameNameNode' },
            { value: PipelineCopyStrategy.BUILD_NODE_MOVE_TO_TARGET_PROJECT, labelKey: 'transferToTargetProject', descKey: 'transferToTargetProjectDesc', highRisk: true }
        ],
        [PipelineCopyResourceType.DEPLOY_NODE]: [
            { value: PipelineCopyStrategy.DEPLOY_NODE_REUSE_SAME_NAME, labelKey: 'reuseTargetBuildNode', descKey: 'reuseTargetBuildNodeDesc', disabledTipKey: 'noSameNameNode' },
            { value: PipelineCopyStrategy.DEPLOY_NODE_MOVE_TO_TARGET_PROJECT, labelKey: 'transferToTargetProject', descKey: 'transferToTargetProjectDesc', highRisk: true }
        ]
    }

    export default {
        name: 'NodeResourceItem',
        components: {
            BaseResourceItem
        },
        props: {
            // 节点数据项（构建节点或部署节点）
            item: {
                type: Object,
                required: true
            },
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
