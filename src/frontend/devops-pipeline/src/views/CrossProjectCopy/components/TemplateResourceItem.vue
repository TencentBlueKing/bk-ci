<template>
    <!-- 流水线模板 -->
    <BaseResourceItem
        :item="item"
        :header-title="item.resourceName"
        :strategies="strategyOptions"
        @strategy-change="handleChange"
    />
</template>

<script>
    import BaseResourceItem from './BaseResourceItem.vue'
    import { PipelineCopyStrategy } from '@/store/modules/crossProjectCopy/constants'

    export default {
        name: 'TemplateResourceItem',
        components: {
            BaseResourceItem
        },
        props: {
            // 模板数据项
            item: {
                type: Object,
                required: true
            }
        },
        computed: {
            strategyOptions () {
                return [
                    {
                        value: PipelineCopyStrategy.PIPELINE_TEMPLATE_REUSE_SAME_NAME,
                        label: this.$t('reuseTargetTemplate'),
                        description: this.$t('reuseTargetTemplateDesc'),
                        disabled: !this.item.targetNameExists,
                        disabledTip: !this.item.targetNameExists ? this.$t('noSameNameTemplate') : '',
                        showJumpIcon: this.item.targetNameExists
                    },
                    {
                        value: PipelineCopyStrategy.PIPELINE_REUSE_SOURCE_ID,
                        label: this.$t('copyAsNewTemplate'),
                        description: this.$t('copyAsNewTemplateDesc'),
                        disabled: false
                    }
                ]
            }
        },
        methods: {
            handleChange (value) {
                this.$emit('strategy-change', value)
            }
        }
    }
</script>
