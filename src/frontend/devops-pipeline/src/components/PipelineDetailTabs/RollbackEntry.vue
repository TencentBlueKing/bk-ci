<template>
    <bk-button
        text
        size="small"
        theme="primary"
        :disabled="loading"
        :loading="loading"
        @click.stop="rollback"
    >
        <slot>
            {{ $t('rollback') }}
        </slot>
    </bk-button>
</template>

<script>
    import { mapActions, mapMutations } from 'vuex'
    import { navConfirm } from '@/utils/util'
    export default {
        props: {
            version: {
                type: Number,
                required: true
            },
            versionName: {
                type: String,
                required: true
            },
            draftVersionName: {
                type: String
            }
        },
        data () {
            return {
                loading: false
            }
        },
        methods: {
            ...mapActions('pipelines', [
                'rollbackPipelineVersion'
            ]),
            ...mapMutations('pipelines', [
                'updatePipelineInfo'
            ]),
            async rollback () {
                try {
                    this.loading = true
                    const hasDraft = this.draftVersionName
                        ? {
                            title: this.$t('hasDraftTips', [this.draftVersionName]),
                            content: this.$t('dropDraftTips', [this.versionName])
                        }
                        : {
                            content: this.$t('createDraftTips', [this.versionName])
                        }
                    const result = await navConfirm({
                        ...hasDraft,
                        theme: 'warning'
                    })
                    if (!result) {
                        return
                    }
                    const { version, versionName } = await this.rollbackPipelineVersion({
                        ...this.$route.params,
                        version: this.version
                    })
                    this.updatePipelineInfo({
                        version,
                        versionName
                    })

                    if (version) {
                        this.$showTips({
                            message: this.$t('rollback') + this.$t('success'),
                            theme: 'success'
                        })
                        this.$router.push({
                            name: 'pipelinesEdit',
                            params: {
                                ...this.$route.params,
                                version
                            }
                        })
                    }
                } catch (error) {
                    this.$showTips({
                        message: error.message || error,
                        theme: 'error'
                    })
                } finally {
                    this.loading = false
                }
            }
        }
    }
</script>
