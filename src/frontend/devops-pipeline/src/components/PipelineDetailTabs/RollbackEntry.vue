<template>
    <bk-button
        text
        size="small"
        theme="primary"
        :disabled="loading"
        :loading="loading"
        v-perm="{
            permissionData: {
                projectId: projectId,
                resourceType: 'pipeline',
                resourceCode: pipelineId,
                action: RESOURCE_ACTION.EDIT
            }
        }"
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
    import { UPDATE_PIPELINE_INFO } from '@/store/modules/atom/constants'
    import {
        RESOURCE_ACTION
    } from '@/utils/permission'

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
            },
            projectId: {
                type: String,
                required: true
            },
            pipelineId: {
                type: String,
                required: true
            }
        },
        data () {
            return {
                loading: false,
                RESOURCE_ACTION
            }
        },
        methods: {
            ...mapActions('pipelines', [
                'rollbackPipelineVersion'
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
                    this.$store.commit(`atom/${UPDATE_PIPELINE_INFO}`, {
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
                    this.handleError(error, {
                        projectId: this.projectId,
                        resourceCode: this.pipelineId,
                        action: this.$permissionResourceAction.EDIT
                    })
                } finally {
                    this.loading = false
                }
            }
        }
    }
</script>
