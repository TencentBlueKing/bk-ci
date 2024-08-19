<template>
    <bk-dialog v-model="isShow"
        theme="primary"
        :width="600"
        :height="400"
        :title="title || $t('newPipelineFromJSONLabel')"
        :mask-close="false"
        :show-footer="false"
        @cancel="handleCancel"
    >
        {{$t('importPipelineLabel')}}
        <bk-upload
            v-if="isShow"
            accept="application/json"
            :with-credentials="true"
            :custom-request="handleSelect"
        >
        </bk-upload>
    </bk-dialog>
</template>

<script>
    import { hashID } from '@/utils/util'
    import { mapActions, mapState } from 'vuex'
    export default {
        name: 'import-pipeline-popup',
        props: {
            isShow: {
                type: Boolean
            },
            title: {
                type: String
            },
            pipelineId: {
                type: String
            },
            pipelineName: {
                type: String
            },
            handleImportSuccess: {
                type: Function
            }
        },
        computed: {
            ...mapState('atom', [
                'pipelineInfo'
            ])
        },
        watch: {
            isShow (show) {
                this.isShow = show
            }
        },
        methods: {
            ...mapActions('atom', [
                'setEditFrom',
                'setPipelineEditing',
                'transferPipeline',
                'setPipeline',
                'setPipelineYaml',
                'setPipelineSetting',
                'setPipelineWithoutTrigger'
            ]),

            handleSelect ({ fileObj, onProgress, onSuccess, onDone }) {
                const reader = new FileReader()
                reader.readAsText(fileObj.origin)

                reader.addEventListener('loadend', e => {
                    try {
                        const jsonResult = JSON.parse(reader.result)
                        const isValid = this.checkJosnValid(jsonResult)
                        const code = isValid ? 0 : 1
                        const message = isValid ? null : this.$t('invalidPipelineJson')

                        onSuccess({
                            code,
                            message,
                            result: jsonResult
                        }, fileObj)

                        if (isValid) {
                            this.handleSuccess(jsonResult)
                        }
                    } catch (e) {
                        console.log(e)
                        onSuccess({
                            code: 1,
                            message: this.$t('invalidPipelineJson'),
                            result: ''
                        }, fileObj)
                    } finally {
                        onDone(fileObj)
                    }
                })

                reader.addEventListener('progress', onProgress)
            },

            async handleSuccess (result) {
                const newPipelineName = this.pipelineName || `${result.model.name}_${hashID().slice(0, 8)}`
                const res = await this.updatePipeline(result, newPipelineName)
                this.setEditFrom(true)
                if (res) {
                    if (typeof this.handleImportSuccess === 'function') {
                        this.handleImportSuccess()
                        return
                    }

                    this.$nextTick(() => {
                        this.$router.push({
                            name: 'pipelineImportEdit',
                            params: {
                                tab: 'pipeline'
                            }
                        })
                    })
                }
            },
            handleUploadError (file) {
                this.$showTips({
                    message: this.$t('invalidPipelineJson'),
                    theme: 'error'
                })
            },
            async updatePipeline (result, newPipelineName) {
                const pipeline = {
                    ...result.model,
                    instanceFromTemplate: false,
                    name: newPipelineName
                }
                try {
                    await this.transferPipeline({
                        projectId: this.$route.params.projectId,
                        actionType: 'FULL_MODEL2YAML',
                        modelAndSetting: {
                            model: {
                                ...result.model,
                                name: newPipelineName
                            },
                            setting: {
                                ...result.setting,
                                pipelineName: newPipelineName
                            }
                        },
                        oldYaml: ''
                    })
                } catch (error) {
                    this.$showTips({
                        message: error.message,
                        theme: 'error'
                    })
                }

                this.setPipelineSetting({
                    ...result.setting,
                    pipelineId: this.pipelineId ?? result.setting.pipelineId,
                    pipelineName: newPipelineName
                })
                this.setPipeline(pipeline)
                this.setPipelineWithoutTrigger({
                    ...pipeline,
                    stages: result.model.stages.slice(1)
                })
                this.setPipelineEditing(true)
                return true
            },
            checkJosnValid (json) {
                try {
                    return json.model.stages && json.setting.pipelineName
                } catch (e) {
                    return false
                }
            },
            handleCancel () {
                this.$emit('update:isShow', false)
            }
        }
    }
</script>

<style lang="scss">
    .drop-upload {
        padding-right: 5px !important;
    }
</style>
