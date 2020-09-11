<template>
    <bk-upload
        theme="button"
        :tip="tip"
        name="file"
        :delay-time="delayTime"
        :handle-res-code="handleUploadRes"
        :with-credentials="true"
        :multiple="false"
        :form-data-attributes="[{ name: 'projectId', value: $route.params.projectId }, { name: 'path', value: filePath }]"
        :url="uploadAcrtifactUrl"
        @on-success="handleUploadDone"
        @on-error="handleUploadDone"
        :size="10"
    >
    </bk-upload>
</template>

<script>
    export default {
        props: {
            filePath: String,
            size: {
                type: Number,
                default: 10
            }
        },
        data () {
            return {
                tip: this.$t('sizeLimit', [this.size]),
                delayTime: 500
            }
        },
        computed: {
            uploadAcrtifactUrl () {
                return `${AJAX_URL_PIRFIX}/artifactory/api/user/artifactories/file/uploadToPath`
            }
        },
        methods: {
            handleUploadRes (response) {
                return response.data
            },
            handleUploadDone ({ name, errorMsg }) {
                this.$showTips({
                    theme: errorMsg ? 'error' : 'success',
                    message: errorMsg || `${name}文件上传成功`
                })
                this.tip = errorMsg || this.$t('fileUploadSuccess')
            }
        }
    }
</script>
