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
        @click.stop="handleClick"
    >
        <slot>
            {{ operateName }}
        </slot>
    </bk-button>
</template>

<script>
    import { mapState, mapActions, mapGetters } from 'vuex'
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
            draftBaseVersionName: {
                type: String
            },
            draftBaseVersion: {
                type: Number
            },
            projectId: {
                type: String,
                required: true
            },
            pipelineId: {
                type: String,
                required: true
            },
            isActiveDraft: Boolean,
            isReleaseVersion: Boolean
        },
        data () {
            return {
                loading: false,
                RESOURCE_ACTION
            }
        },
        computed: {
            ...mapState('atom', [
                'pipelineInfo'
            ]),
            ...mapGetters({
                hasDraftPipeline: 'atom/hasDraftPipeline'
            }),
            isRollback () {
                const { baseVersion, releaseVersion } = this.pipelineInfo
                const isReleaseVersion = this.version === releaseVersion
                return !(this.isActiveDraft || baseVersion === this.version || (isReleaseVersion && !this.hasDraftPipeline))
            },
            operateName () {
                return this.isRollback
                    ? this.$t('rollback')
                    : this.$t('edit')
            }
        },
        methods: {
            ...mapActions('pipelines', [
                'rollbackPipelineVersion'
            ]),
            handleClick () {
                if (this.isRollback) {
                    this.rollback()
                } else {
                    this.goEdit(this.version)
                }
            },
            async rollback () {
                try {
                    this.loading = true
                    const hasDraft = this.hasDraftPipeline
                        ? {
                            title: this.$t('hasDraftTips', [this.draftBaseVersionName]),
                            content: this.$t('dropDraftTips', [this.versionName])
                        }
                        : {
                            content: this.$t('createDraftTips', [this.versionName])
                        }
                    const result = await navConfirm({
                        ...hasDraft,
                        width: 620,
                        type: hasDraft ? 'warning' : '',
                        theme: hasDraft ? 'warning' : ''
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
                        this.goEdit(version)
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
            },
            goEdit (version) {
                this.$router.push({
                    name: 'pipelinesEdit',
                    params: {
                        ...this.$route.params,
                        version
                    }
                })
            }
        }
    }
</script>
