<template>
    <bk-dialog
        v-model="isShow"
        theme="primary"
        :width="600"
        :height="400"
        :title="title || $t('newPipelineFromJSONLabel')"
        :mask-close="false"
        :show-footer="false"
        @cancel="handleCancel"
    >
        {{ $t('importPipelineLabel') }}
        <bk-upload
            v-if="isShow"
            accept=".json, .yaml, .yml, application/json, application/x-yaml"
            :with-credentials="true"
            :custom-request="handleSelect"
        >
        </bk-upload>
    </bk-dialog>
</template>

<script>
    import { mapActions, mapState } from 'vuex'
    import { CODE_MODE, UI_MODE } from '@/utils/pipelineConst'

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
            ...mapActions({
                updatePipelineMode: 'updatePipelineMode'
            }),

            handleSelect ({ fileObj, onProgress, onSuccess, onDone }) {
                const reader = new FileReader()
                reader.readAsText(fileObj.origin)
                reader.addEventListener('loadend', async e => {
                    try {
                        if (fileObj.type === 'application/json' || fileObj.name.endsWith('.json')) {
                            const jsonResult = JSON.parse(reader.result)
                            const isValid = this.checkJsonValid(jsonResult)
                            const code = isValid ? 0 : 1
                            const message = isValid ? null : this.$t('invalidPipelineJsonOrYaml')

                            onSuccess({
                                code,
                                message,
                                result: jsonResult
                            }, fileObj)

                            if (isValid) {
                                this.handleSuccess(jsonResult, UI_MODE)
                            }
                        } else if (fileObj.type === 'application/x-yaml' || fileObj.name.endsWith('.yaml') || fileObj.name.endsWith('.yml')) {
                            const yaml = e.target.result
                            const isValid = !!yaml
                            const code = isValid ? 0 : 1
                            const message = isValid ? null : this.$t('invalidPipelineJsonOrYaml')

                            onSuccess({
                                code,
                                message,
                                result: yaml
                            }, fileObj)

                            if (isValid) {
                                this.handleSuccess(yaml, CODE_MODE)
                            }
                        }
                    } catch (e) {
                        console.log(e)
                        onSuccess({
                            code: 1,
                            message: this.$t('invalidPipelineJsonOrYaml'),
                            result: ''
                        }, fileObj)
                    } finally {
                        onDone(fileObj)
                    }
                })
                reader.addEventListener('progress', onProgress)
            },

            async handleSuccess (result, type = UI_MODE) {
                let res
                if (type === UI_MODE) {
                    const newPipelineName = result.model.name
                    res = await this.updatePipeline(result, newPipelineName)
                } else if (type === CODE_MODE) {
                    this.updatePipelineMode(CODE_MODE)
                    res = await this.updateCodeModePipeline(result)
                }

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
            async updateCodeModePipeline (result) {
                this.$store.dispatch('atom/setPipelineYaml', result)
                this.setEditFrom(true)
                try {
                    const { modelAndSetting } = await this.transferPipeline({
                        projectId: this.$route.params.projectId,
                        actionType: 'FULL_YAML2MODEL',
                        oldYaml: result
                    })
                    const newPipelineName = modelAndSetting.model.name
                    
                    const pipeline = {
                        ...modelAndSetting.model,
                        name: newPipelineName
                    }
                    this.setPipelineSetting({
                        ...modelAndSetting.setting,
                        pipelineId: this.pipelineId ?? modelAndSetting.setting.pipelineId,
                        pipelineName: newPipelineName
                    })
                    this.setPipeline(pipeline)
                    this.setPipelineWithoutTrigger({
                        ...pipeline,
                        stages: modelAndSetting.model.stages.slice(1)
                    })
                    this.setPipelineEditing(true)

                    return true
                } catch (error) {
                    this.$showTips({
                        message: error.message,
                        theme: 'error'
                    })
                    return false
                }
            },
            async updatePipeline (result, newPipelineName) {
                const { templateId, instanceFromTemplate, ...restModel } = result.model
                const pipeline = {
                    ...restModel,
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
                this.setEditFrom(true)
                return true
            },
            checkJsonValid (json) {
                try {
                    return (json.model.stages && json.setting.pipelineName) || json.stages
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
