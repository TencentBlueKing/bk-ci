<template>
    <section class="file-param">
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
    </section>
</template>

<script>
    import VuexInput from '@/components/atomFormField/VuexInput'
    export default {
        components: {
            VuexInput
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
            uploadFileName: {
                type: String,
                default: ''
            },
            fileParamsNameFlag: {
                type: String,
                default: ''
            },
            isDiffParam: {
                type: String,
                default: false
            }
        },
        data () {
            return {
                fileDefaultVal: {
                    directory: '',
                    fileName: ''
                }
            }
        },
        watch: {
            uploadFileName (val) {
                if (this.fileParamsNameFlag === this.id || this.fileParamsNameFlag === this.name || !this.fileParamsNameFlag) {
                    this.updatePathFromFileName(val)
                }
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
            }
        }
    }
</script>

<style lang="scss" scoped>
    .file-param {
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
</style>
