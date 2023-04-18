<template>
    <bk-dialog
        width="500"
        :loading="saving"
        render-directive="if"
        header-position="left"
        :mask-close="false"
        v-model="isShow"
        :title="$t('subpage.renamePipeline')"
        @confirm="submit"
        @cancel="handleClose"
    >
        <bk-form :model="renamePipelineModel" form-type="vertical">
            <bk-form-item :label="$t('pipelineName')" required property="name">
                <bk-input
                    v-model="renamePipelineModel.name"
                    :placeholder="$t('pipelineNameInputTips')"
                    :maxlength="40"
                />
            </bk-form-item>
        </bk-form>
    </bk-dialog>
</template>

<script>
    import { mapActions } from 'vuex'
    export default {
        props: {
            isShow: Boolean,
            projectId: {
                type: String,
                required: true
            },
            pipelineId: {
                type: String,
                required: true
            },
            pipelineName: {
                type: String,
                required: true
            }
        },
        data () {
            return {
                saving: false,
                renamePipelineModel: {
                    name: this.pipelineName
                }
            }
        },
        watch: {
            pipelineName (val) {
                this.renamePipelineModel.name = val
            }
        },
        methods: {
            ...mapActions('pipelines', ['renamePipeline']),
            async submit () {
                if (this.saving) return
                let message = ''
                let theme = ''
                this.saving = true
                try {
                    if (!this.renamePipelineModel.name) {
                        throw new Error(this.$t('subpage.nameNullTips'))
                    }
                    await this.renamePipeline({
                        projectId: this.projectId,
                        pipelineId: this.pipelineId,
                        ...this.renamePipelineModel
                    })
                    message = this.$t('updateSuc')
                    theme = 'success'
                    this.$emit('done', this.renamePipelineModel.name)
                    this.handleClose()
                } catch (err) {
                    this.handleError(err, [
                        {
                            actionId: this.$permissionActionMap.edit,
                            resourceId: this.$permissionResourceMap.pipeline,
                            instanceId: [
                                {
                                    id: this.pipelineId,
                                    name: this.pipelineName
                                }
                            ],
                            projectId: this.projectId
                        }
                    ])
                } finally {
                    message
                        && this.$showTips({
                            message,
                            theme
                        })
                }
            },
            handleClose () {
                this.renamePipelineModel.name = ''
                this.$emit('close')
            }
        }
    }
</script>
