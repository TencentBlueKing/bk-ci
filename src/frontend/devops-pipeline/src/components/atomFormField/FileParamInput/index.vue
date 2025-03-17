<template>
    <section
        :class="{
            'file-param': true,
            'flex-column': flex
        }"
    >
        <div class="file-input">
            <vuex-input
                class="path-input"
                :disabled="disabled"
                :handle-change="updatePath"
                name="directory"
                v-validate="{ required: required }"
                :data-vv-scope="'pipelineParam'"
                :click-unfold="true"
                :placeholder="$t('editPage.filePathTips')"
                :value="directory"
                :class="{
                    'is-diff-param': isDiffParam
                }"
            />
            <span
                v-if="enableVersionControl"
                :class="['random-generate', randomFilePath ? '' : 'placeholder']"
            >
                /{{ randomFilePath || $t('editPage.randomlyGenerate') }}/
            </span>
            <vuex-input
                class="file-name"
                :disabled="disabled"
                :handle-change="updatePath"
                name="fileName"
                v-validate="{ required: required }"
                :data-vv-scope="'pipelineParam'"
                :click-unfold="true"
                :placeholder="$t('editPage.fileNameTips')"
                :value="fileName"
                :class="{
                    'is-diff-param': isDiffParam,
                    'border-left-none': !enableVersionControl
                }"
            />
        </div>
        <div
            v-if="!flex"
            class="upload-tips"
        >
            {{ $t('sizeLimit', [100]) }}
        </div>
        <div
            :class="{
                'mt10': !flex,
                'file-upload': flex
            }"
        >
            <file-upload
                name="fileName"
                :file-path="value"
                @handle-change="uploadPathFromFileName"
            />
        </div>
        <div v-if="!flex">
            <bk-checkbox
                :value="enableVersionControl"
                @change="handleEnableVersionControl"
            >
                {{ $t('editPage.enableVersionControl') }}
            </bk-checkbox>
            <i
                class="bk-icon icon-info-circle"
                style="color: #63656e;"
                v-bk-tooltips="{ content: $t('editPage.versionControlTip'), placement: 'bottom-start' }"
            ></i>
        </div>
    </section>
</template>

<script>
    import FileUpload from '@/components/atomFormField/FileUpload'
    import VuexInput from '@/components/atomFormField/VuexInput'
    import { randomString } from '@/utils/util'

    export default {
        components: {
            VuexInput,
            FileUpload
        },
        props: {
            id: {
                type: String,
                default: ''
            },
            name: {
                type: String,
                default: ''
            },
            handleChange: {
                type: Function,
                default: () => {}
            },
            disabled: Boolean,
            value: {
                type: String,
                default: ''
            },
            required: {
                type: Boolean,
                default: false
            },
            isDiffParam: {
                type: String,
                default: false
            },
            flex: {
                type: Boolean,
                default: false
            },
            enableVersionControl: {
                type: Boolean,
                default: false
            },
            randomSubPath: {
                type: String,
                default: ''
            }
        },
        data () {
            const that = this
            return {
                directory: '',
                fileName: '',
                randomFilePath: that.randomSubPath
            }
        },
        watch: {
            value: {
                handler: function (newValue) {
                    const lastSlashIndex = newValue?.lastIndexOf('/')
                    
                    this.fileName = newValue.slice(lastSlashIndex + 1)

                    if (this.enableVersionControl && this.randomFilePath) {
                        this.randomFilePath = this.randomSubPath
                        const randomStringLastIndex = newValue.lastIndexOf(`/${this.randomFilePath}`)
                        this.directory = newValue.slice(0, randomStringLastIndex)
                    } else {
                        this.directory = newValue.slice(0, lastSlashIndex)
                    }
                },
                immediate: true
            }
        },
        methods: {
            updatePath (name, value, newFile = false) {
                if (newFile && this.enableVersionControl) {
                    this.randomFilePath = randomString(8)
                }
                this[name] = value

                const path = [
                    this.directory,
                    ...(this.randomFilePath ? [this.randomFilePath] : []),
                    this.fileName
                ].join('/')

                // 执行页面需要传json给后端去改变latestRandomStringInPath的值
                const finalValue = this.flex && this.enableVersionControl
                    ? {
                        directory: path,
                        latestRandomStringInPath: this.randomFilePath
                    }
                    : path
                this.handleChange(this.name, finalValue)

                if (!this.flex) {
                    this.handleChange('randomStringInPath', this.randomFilePath)
                }
            },
            uploadPathFromFileName (value) {
                if (!this.fileName) {
                    this.fileName = value
                }
                this.updatePath('fileName', this.fileName, true)
            },
            handleEnableVersionControl (value) {
                if (!this.flex && !value) {
                    this.randomFilePath = ''
                }
                this.handleChange('enableVersionControl', value)
                this.updatePath('enableVersionControl', value)
            }
        }
    }
</script>

<style lang="scss" scoped>
    .file-param {
        width: 100%;
    }
    .display-flex {
        display: flex;
    }
    .flex-column {
        display: flex;
        .file-upload {
            margin-left: 10px;
            margin-top: 0;
            ::v-deep .bk-upload.button {
                .all-file {
                    width: 100%;
                    position: absolute;
                    right: 0;
                    top: 0;
                }
            }
        }
    }
    .border-left-none {
        border-left: none;
    }
    .file-input {
        width: 100%;
        display: flex;
        .path-input {
            border-radius: 2px 0 0 2px;
        }
        .random-generate {
            font-size: 12px;
            color: #737987;
            flex-shrink: 0;
            padding: 0 10px;
        }
        .placeholder {
            color: #c4c6cc;
        }
        .file-name {
            border-radius: 0 2px 2px 0;
        }
    }
    .is-diff-param {
        border-color: #FF9C01 !important;
    }
    .upload-tips {
        font-size: 12px;
    }
    .file-upload {
        display: flex;
        color: #737987;
        ::v-deep .bk-upload.button {
            position: static;
            display: flex;
            .file-wrapper {
                margin-bottom: 0;
                height: 32px;
                background: white;
            }
            p.tip {
                white-space: nowrap;
                position: static;
                margin-left: 8px;
            }
            .all-file {
                width: 100%;
                position: absolute;
                right: 0;
                top: 80;
                .file-item {
                    margin-bottom: 0;
                    &.file-item-fail {
                        background: rgb(254,221,220);
                    }
                }
                .error-msg {
                    margin: 0
                }
            }
        }
    }
</style>
