<template>
    <div class="bk-file-upload">
        <div class="file-list" v-if="uploadQueue.length">
            <div class="file-item" v-for="(file, index) of uploadQueue" :key="index">
                <div :class="['file-item-wrapper', { 'error': file.status === 'error', 'success': file.status === 'success' }]">
                    <div class="file-icon">
                        <img src="../../../images/placeholder.svg" alt="">
                    </div>
                    <div class="file-info">
                        <div class="file-metedata">
                            <p class="file-name">{{file.name}}</p>
                            <span class="file-status" v-if="file.status === 'uploading'">{{progress}}%</span>
                            <span class="file-status success" v-if="file.status === 'success'">上传成功</span>
                            <span class="file-status error" v-if="file.status === 'error'">上传失败</span>
                        </div>
                        <div :class="['file-progress']">
                            <div :class="['file-progress-bar']" :style="`width: ${progress}%`"></div>
                        </div>
                    </div>
                    <i class="bk-icon icon-close" @click="removeFile(index)" v-if="file.status !== 'success'"></i>
                </div>
                <p class="tip" v-if="file.statusText && file.status === 'error'">{{file.statusText}}</p>
            </div>
        </div>
        <template v-else>
            <div class="file-input">
                <button class="trigger-btn">
                    <img src="../../../images/upload.svg" alt="" class="upload-icon">
                    拖拽到此处上传或 <span>点击上传</span>
                </button>
                <input type="file" @change="selectFile" :multiple="multiple" :accept="accept">
            </div>
            <p class="tip prompt" v-if="tip">{{tip}}</p>
        </template>
    </div>
</template>
<script>
    import cookie from 'cookie'

    const CSRFToken = cookie.parse(document.cookie).backend_csrftoken
    export default {
        props: {
            // 必选参数，上传的地址
            postUrl: {
                type: String,
                default: ''
            },
            name: {
                type: String,
                default: 'upload-file'
            },
            // 设置上传的请求头部
            headers: {
                type: Object,
                default () {
                    return {}
                }
            },
            tip: {
                type: String,
                default: ''
            },
            multiple: {
                type: Boolean,
                default: false
            },
            accept: {
                type: String
            },
            maxSize: {
                type: Number
            },
            dragable: {
                type: Boolean,
                default: false
            },
            disabled: {
                type: Boolean,
                default: false
            },
            os: {
                type: Array,
                default () {
                    return []
                }
            }
        },
        data () {
            return {
                uploadQueue: [],
                isUploadLoading: false,
                progress: 0,
                queryStatusTimer: null
            }
        },
        
        beforeDestroy () {
            this.resetUploadStatus()
            this.clearUploadQueue()
        },
        methods: {
            selectFile (event) {
                const target = event.target
                const files = target.files

                if (!files.length) return
                for (const file of files) {
                    const fileObj = {
                        name: file.name,
                        size: file.size / 1000 / 1000,
                        type: file.type,
                        origin: file,
                        isUploadLoading: false,
                        status: '',
                        statusText: ''
                    }
                    const pos = fileObj.name.lastIndexOf('.')
                    const lastname = fileObj.name.substring(pos, fileObj.name.length)
                    
                    this.uploadQueue.push(fileObj)
                    if (this.maxSize && (fileObj.size > this.maxSize)) {
                        fileObj.status = 'error'
                        fileObj.statusText = `文件不能超过${this.maxSize}M`
                    } else if (!this.os.length) {
                        fileObj.status = 'error'
                        fileObj.statusText = `请先选择操作系统`
                    } else if (lastname.toLowerCase() !== '.zip') {
                        fileObj.status = 'error'
                        fileObj.statusText = `只允许上传 zip 格式的文件`
                    } else {
                        this.uploadFile(fileObj)
                    }
                }
            },
            uploadFile (fileObj) {
                this.isUploadLoading = true
                const formData = new FormData()
                formData.append('file', fileObj.origin)
                formData.append('os', `["${this.os.join('","')}"]`)
                fileObj.status = 'uploading'
                fileObj.statusText = '上传中'

                const xhr = new XMLHttpRequest()
                fileObj.xhr = xhr // 保存，用于中断请求

                xhr.withCredentials = true
                xhr.open('POST', this.postUrl, true)
                xhr.onreadystatechange = () => {
                    if (xhr.readyState === 4) {
                        if (xhr.status === 200) {
                            const response = JSON.parse(xhr.responseText)
                            
                            if (response.status === 0) {
                                this.isUploadLoading = false
                                this.progress = 100
                                fileObj.status = 'success'
                                fileObj.statusText = '上传成功'
                                this.$emit('uploadSuccess', response.data)
                            } else {
                                fileObj.status = 'error'
                                fileObj.statusText = response.message
                                this.resetUploadStatus(response.message)
                            }
                        } else if (xhr.response) {
                            const errResponse = JSON.parse(xhr.responseText)
                            fileObj.status = 'error'
                            fileObj.statusText = ''
                            this.resetUploadStatus(errResponse.message)
                        }
                    }
                }
                if (xhr.upload) {
                    xhr.upload.onprogress = event => {
                        if (event.lengthComputable) {
                            const progress = Math.floor(event.loaded / event.total * 100)

                            this.progress = progress >= 1 ? progress - 1 : 0
                        }
                    }
                }
                xhr.setRequestHeader('X-CSRFToken', CSRFToken)
                xhr.send(formData)
                this.$emit('uploadStart')
            },

            syncUploadTaskStatus (cb) {
                const self = this
                return async function queryTaskStatus (projectCode, taskId) {
                    const response = await self.$store.dispatch('artifactory/syncUploadStatus', {
                        projectCode,
                        taskId
                    })
                    const { taskStatus } = response
                    switch (true) {
                        case taskStatus === 'RUNNING':
                            self.queryStatusTimer = setTimeout(() => {
                                queryTaskStatus(projectCode, taskId)
                            }, 3000)
                            break
                        case taskStatus === 'SUCCESS':
                            return cb(response)
                        case taskStatus === 'FAILED':
                        case taskStatus === 'TIMEOUT':
                            break
                    }
                }
            },

            removeFile (index) {
                if (this.uploadQueue[index].xhr) {
                    this.uploadQueue[index].xhr.abort()
                }
                
                this.uploadQueue.splice(index, 1)
                this.resetUploadStatus()
            },
            clearUploadQueue () {
                this.uploadQueue.forEach(item => {
                    item.xhr.abort()
                })
                this.uploadQueue = []
            },
            resetUploadStatus (message) {
                this.isUploadLoading = false
                clearTimeout(this.queryStatusTimer)
                this.$emit('uploadFail', message)
            }
        }
    }
</script>
<style lang="scss">
    .bk-file-upload {
        .upload-icon {
            width: 24px;
            vertical-align: middle;
            margin-top: -5px;
        }
        .file-item-wrapper {
            height: 70px;
            border: 1px dashed #C3CDD7;
            border-radius: 2px;
            background:rgba(250,251,253,1);
            display: flex;
            padding: 13px;
            position: relative;
            .file-icon {
                width: 45px;
                height: 45px;
                border-radius: 2px;
                margin-right: 5px;
            }
            .icon-close {
                font-size: 12px;
                cursor: pointer;
                margin-top: -4px;
            }
            .file-info {
                flex: 1;
                text-align: left;
            }
            .file-metedata {
                overflow: hidden;
            }
            .file-name {
                font-size: 14px;
                color: #737987;
                margin: 5px auto 7px auto;
                line-height: 1;
                text-align: left;
                float: left;
            }
            .file-status {
                    float: right;
                font-size: 12px;
                color: #737987;
                margin: 5px 10px 7px auto;
                &.success {
                    color: #30D878;
                }
                &.error {
                    color: #FF5656;
                }
            }
            &.error {
                .file-progress {
                    .file-progress-bar {
                        background: #FF5656;
                    }
                }
            }
            &.success {
                .file-progress {
                    .file-progress-bar {
                        background: #30D878;
                    }
                }
            }
            .file-progress {
                width: 100%;
                height: 6px;
                background: #DDE4EB;
                position: relative;
                border-radius: 12px;
                overflow: hidden;
                .file-progress-bar {
                    position: absolute;
                    border-radius: 12px;
                    left: 0;
                    top: 0;
                    width: 100%;
                    height: 6px;
                    background: #3C96FF;
                    transition: width 0.3s ease;
                    &.running {
                        animation: progress 2s infinite ease-in-out;
                    }
                }
            }
            + .file-item {
                margin-top: 10px;
            }
        }

        .tip {
            color: #ff5656;
            font-size: 14px;
            margin: 4px 0;
        }

        .prompt {
            color: #7b7d8a;
        }

        .file-input {
            width: 100%;
            min-width: 500px;
            height: 70px;
            line-height: 70px;
            border: 1px dashed #C3CDD7;
            border-radius: 2px;
            text-align: center;
            position: relative;
            .trigger-btn {
                text-align: center;
                display: block;
                width: 100%;
                font-size: 14px;
                line-height: 70px;
                border: none;
                background: none;
                position: relative;
                cursor: pointer;
                outline: none;
                span {
                    color: #3C96FF;
                }
            }
                input[type=file] {
                width: 100%;
                height: 70px;
                position: absolute;
                left: 0;
                z-index: 10;
                cursor: pointer;
                top: 0;
                opacity: 0;
            }
        }
    }

    @keyframes progress {
        from {
            width: 0;
        }
        to {
            width: 100%;
        }
    }
</style>
