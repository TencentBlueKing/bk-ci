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
                :handle-change="(name, value) => updatePathFromDirectory(value)"
                name="path"
                v-validate="{ required: required }"
                :data-vv-scope="'pipelineParam'"
                :click-unfold="true"
                :placeholder="$t('editPage.filePathTips')"
                :value="fileDefaultVal.directory"
                :class="{
                    'is-diff-param': isDiffParam
                }"
            />
            <span
                v-if="enableVersionControl"
                :class="['random-generate', fileDefaultVal.randomFilePath ? '' : 'placeholder']"
            >
                /{{ fileDefaultVal.randomFilePath || $t('editPage.randomlyGenerate') }}/
            </span>
            <vuex-input
                class="file-name"
                :disabled="disabled"
                :handle-change="(name, value) => updatePathFromFileName(value)"
                name="fileName"
                v-validate="{ required: required }"
                :data-vv-scope="'pipelineParam'"
                :click-unfold="true"
                :placeholder="$t('editPage.fileNameTips')"
                :value="fileDefaultVal.fileName"
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
                @handle-change="(value) => uploadPathFromFileName(value)"
            />
        </div>
        <div v-if="!flex">
            <bk-checkbox
                v-model="enableVersionControl"
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
    import VuexInput from '@/components/atomFormField/VuexInput'
    import FileUpload from '@/components/atomFormField/FileUpload'
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
            versionControl: {
                type: Boolean,
                default: false
            },
            randomString: {
                type: String,
                default: ''
            }
        },
        data () {
            return {
                fileDefaultVal: {
                    directory: '',
                    fileName: '',
                    randomFilePath: ''
                },
                enableVersionControl: false
            }
        },
        watch: {
            value: {
                handler: 'splitFilePath',
                immediate: true
            },
            enableVersionControl () {
                this.updatePath()
            }
        },
        methods: {
            splitFilePath () {
                this.enableVersionControl = this.versionControl

                const lastSlashIndex = this.value.lastIndexOf('/')
                this.fileDefaultVal.fileName = this.value.slice(lastSlashIndex + 1)

                if (this.enableVersionControl && this.randomString) {
                    this.fileDefaultVal.randomFilePath = this.randomString

                    const randomStringIndex = this.value.indexOf(`/${this.randomString}/`)
                    this.fileDefaultVal.directory = randomStringIndex !== -1 ? this.value.slice(0, randomStringIndex) : this.value.slice(0, lastSlashIndex)
                } else {
                    this.fileDefaultVal.directory = this.value.slice(0, lastSlashIndex)
                }
            },
            updatePath () {
                const { directory, fileName, randomFilePath } = this.fileDefaultVal
                const path = this.enableVersionControl && randomFilePath
                    ? `${directory}/${randomFilePath}/${fileName}`
                    : `${directory}/${fileName}`
                this.handleChange(this.name, path)
                this.handleChange('randomStringInPath', this.fileDefaultVal.randomFilePath)
            },
            updatePathFromDirectory (value) {
                this.fileDefaultVal.directory = value
                this.updatePath()
            },
            updatePathFromFileName (value) {
                this.fileDefaultVal.fileName = value
                this.updatePath()
            },
            uploadPathFromFileName (value) {
                if (!this.fileDefaultVal.fileName) {
                    this.fileDefaultVal.fileName = value
                }

                if (this.enableVersionControl) {
                    this.fileDefaultVal.randomFilePath = randomString(8)
                }
                this.updatePath()
            },
            handleEnableVersionControl (value) {
                this.enableVersionControl = value
                if (!value) {
                    this.fileDefaultVal.randomFilePath = ''
                }
                this.handleChange('enableVersionControl', value)
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
