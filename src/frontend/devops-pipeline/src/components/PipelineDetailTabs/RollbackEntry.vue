<template>
    <bk-button
        text
        size="small"
        theme="primary"
        @click.stop="rollback"
    >
        <slot>
            {{ $t('rollback') }}
        </slot>
    </bk-button>
</template>

<script>
    import { mapActions } from 'vuex'
    import { navConfirm } from '@/utils/util'
    export default {
        props: {
            version: {
                type: Number,
                required: true
            }
        },
        methods: {
            ...mapActions('pipelines', [
                'rollbackPipelineVersion'
            ]),
            async rollback () {
                try {
                    const bool = true
                    const hasDraft = bool
                        ? {
                            title: this.$t('hasDraftTips', ['P1.T1.1']),
                            content: this.$t('dropDraftTips', ['P1.T1.1'])
                        }
                        : {
                            content: this.$t('createDraftTips', ['P1.T1.1'])
                        }
                    const result = await navConfirm({
                        ...hasDraft,
                        theme: 'warning'
                    })
                    if (!result) {
                        return
                    }
                    const res = this.rollbackPipelineVersion({
                        ...this.$route.params,
                        version: this.version
                    })
                    if (res) {
                        this.$showTips({
                            message: this.$t('rollback') + this.$t('success'),
                            theme: 'success'
                        })
                    }
                } catch (error) {
                    this.$showTips({
                        message: error.message || error,
                        theme: 'error'
                    })
                }
            }
        }
    }
</script>
