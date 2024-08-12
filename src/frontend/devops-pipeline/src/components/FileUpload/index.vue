<template>
    <bk-upload
        ref="uploadRef"
        theme="button"
        :tip="tip"
        :files="files"
        name="file"
        :delay-time="delayTime"
        :handle-res-code="handleUploadRes"
        with-credentials
        :multiple="false"
        :size="100"
        :custom-request="handleUploadFile"
        @on-error="handleUploadError"
    >
    </bk-upload>
</template>

<script>
    export default {
        props: {
            filePath: String,
            size: {
                type: Number,
                default: 100
            }
        },
        data () {
            return {
                tip: this.$t('sizeLimit', [this.size]),
                delayTime: 500,
                files: []
            }
        },
        computed: {
            uploadAcrtifactUrl () {
                return `${API_URL_PREFIX}/artifactory/api/user/artifactories/file/uploadToPath`
            }
        },
        methods: {
            handleUploadRes (response) {
                return response.data
            },
            handleUploadError () {
                setTimeout(() => {
                    this.files = []
                }, 1700)
            },
            handleUploadFile (res) {
                this.files = []
                if (!this.filePath) {
                    this.$showTips({
                        theme: 'error',
                        message: '请先填写存放文件的目录'
                    })
                    this.files = []
                    return
                }
                try {
                    const file = res.fileList[0] || ''
                    this.files[0] = file
                    this.files[0].progress = '20%'
                    this.$emit('handle-change', file.name)
                    const formData = new FormData()
                    setTimeout(async () => {
                        formData.append('file', file.origin)
                        formData.append('projectId', this.$route.params.projectId)
                        formData.append('path', this.filePath)
                        
                        const response = await this.$ajax.post(`${this.uploadAcrtifactUrl}`, formData, {
                            headers: {
                                'Content-Type': 'multipart/form-data'
                            }
                        })
                        this.files = res.fileList
                        this.files[0].progress = '100%'
                        this.$refs.uploadRef?.handleSuccess(response, this.files[0])
                        this.$refs.uploadRef?.hideFileList()
                        
                        this.tip = response.data !== 'true' ? response.data : this.$t('fileUploadSuccess')
                        this.$showTips({
                            theme: response.data !== 'true' ? 'error' : 'success',
                            message: `${file.name}文件上传成功` || response.data
                        })
                    }, 300)
                } catch (e) {
                    console.error(e)
                }
            }
        }
    }
</script>
