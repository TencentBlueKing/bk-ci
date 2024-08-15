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
                    'is-diff-param': isDiffParam
                }"
            />
        </div>
        <div class="file-upload">
            <file-upload
                name="fileName"
                :file-path="value"
                @handle-change="(value) => uploadPathFromFileName(value)"
            />
        </div>
    </section>
</template>

<script>
    import VuexInput from '@/components/atomFormField/VuexInput'
    import FileUpload from '@/components/FileUpload'
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
            fileParamsNameFlag: {
                type: String,
                default: ''
            },
            isDiffParam: {
                type: String,
                default: false
            },
            flex: {
                type: Boolean,
                default: false
            }
        },
        data () {
            return {
                fileDefaultVal: {
                    directory: '',
                    fileName: ''
                },
                uploadFileName: ''
            }
        },
        watch: {
            uploadFileName (val) {
                this.updatePathFromFileName(val)
            },
            value () {
                this.splitFilePath()
            }
        },
        created () {
            this.splitFilePath()
        },
        methods: {
            splitFilePath () {
                const lastSlashIndex = this.value.lastIndexOf('/')
                this.fileDefaultVal.directory = this.value.substr(0, lastSlashIndex)
                this.fileDefaultVal.fileName = this.value.substr(lastSlashIndex + 1)
            },
            updatePathFromDirectory (value) {
                this.fileDefaultVal.directory = value
                const val = `${this.fileDefaultVal.directory}/${this.fileDefaultVal.fileName}`
                this.handleChange(this.name, val)
            },
            updatePathFromFileName (value) {
                this.fileDefaultVal.fileName = value
                const val = `${this.fileDefaultVal.directory}/${this.fileDefaultVal.fileName}`
                this.handleChange(this.name, val)
            },
            uploadPathFromFileName (value, name) {
                this.uploadFileName = value
            }
        }
    }
</script>

<style lang="scss" scoped>
    .file-param {
        width: 100%;
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
    .file-input {
        width: 100%;
        display: flex;
        .path-input {
            border-radius: 2px 0 0 2px;
        }
        .file-name {
            border-radius: 0 2px 2px 0;
            border-left: 0;
        }
    }
    .is-diff-param {
        border-color: #FF9C01 !important;
    }

    .file-upload {
        display: flex;
        color: #737987;
        margin-top: 10px;
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
