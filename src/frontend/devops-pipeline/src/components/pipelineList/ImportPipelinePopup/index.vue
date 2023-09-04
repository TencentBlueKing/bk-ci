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
    import { mapActions } from 'vuex'
    import { hashID } from '@/utils/util'
    export default {
        name: 'import-pipeline-popup',
        props: {
            isShow: {
                type: Boolean
            },
            title: {
                type: String
            },
            handleImportSuccess: {
                type: Function
            }
        },
        watch: {
            isShow (show) {
                this.isShow = show
            }
        },
        methods: {
            ...mapActions('atom', [
                'setImportedPipelineJson',
                'setPipeline'
            ]),
            ...mapActions('pipelines', [
                'setPipelineSetting'
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

            handleSuccess (result) {
                if (typeof this.handleImportSuccess === 'function') {
                    this.handleImportSuccess(result)
                    return
                }
                const newPipelineName = `${result.model.name}_${hashID().slice(0, 8)}`
                this.setImportedPipelineJson(result)
                this.setPipelineSetting({
                    ...result.setting,
                    pipelineName: newPipelineName
                })
                this.setPipeline({
                    ...result.model,
                    name: newPipelineName
                })
                this.$nextTick(() => {
                    this.$router.push({
                        name: 'pipelineImportEdit'
                    })
                })
            },
            handleUploadError (file) {
                this.$showTips({
                    message: this.$t('invalidPipelineJson'),
                    theme: 'error'
                })
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
