<template>
    <bk-upload
        theme="button"
        :tip="$t('sizeLimit', [size])"
        name="file"
        :delay-time="500"
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
        computed: {
            uploadAcrtifactUrl () {
                return `${AJAX_URL_PIRFIX}/artifactory/api/user/artifactories/file/uploadToPath`
            }
        },
        methods: {
            handleUploadRes (response) {
                console.log(response.data)
                return response.data
            },
            handleUploadDone ({ name, errorMsg }) {
                this.$showTips({
                    theme: errorMsg ? 'error' : 'success',
                    message: errorMsg || `${name}文件上传成功`
                })
            }
        }
    }
</script>
