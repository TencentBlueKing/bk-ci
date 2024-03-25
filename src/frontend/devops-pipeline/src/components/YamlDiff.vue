<template>
    <div
        :style="{ height }"
        ref="diffBox"
        class="pipeline-yaml-diff"
        v-bkloading="{ isLoading, color: '#1d1d1d' }"
    >
    </div>
</template>

<script>
    import MonacoEditor from '@/utils/monacoEditor'
    export default {
        props: {
            height: {
                type: String,
                default: '666px'
            },
            oldYaml: {
                type: String,
                default: ''
            },
            newYaml: {
                type: String,
                default: ''
            }
        },
        data () {
            return {
                isLoading: false
            }
        },
        watch: {
            oldYaml (newValue) {
                if (this.editor) {
                    this.diff(newValue, this.newYaml)
                }
            },
            newYaml (newValue) {
                if (this.editor) {
                    this.diff(this.oldYaml, newValue)
                }
            }
        },
        async mounted () {
            this.isLoading = true
            this.monaco = await MonacoEditor.instance()
            this.isLoading = false

            this.editor = this.monaco.editor.createDiffEditor(
                this.$refs.diffBox,
                {
                    language: 'yaml',
                    readOnly: this.readOnly,
                    automaticLayout: true,
                    unicodeHighlight: {
                        ambiguousCharacters: false
                    }
                }
            )
            this.diff(this.oldYaml, this.newYaml)
        },
        beforeDestroy () {
            if (this.editor) {
                this.editor.dispose()
            }
        },
        methods: {
            diff (oldYaml, newYaml) {
                const originalModel = this.monaco.editor.createModel(
                    oldYaml,
                    'yaml'
                )
                const modifiedModel = this.monaco.editor.createModel(
                    newYaml,
                    'yaml'
                )
                this.editor.setModel({
                    original: originalModel,
                    modified: modifiedModel
                })
            }
        }
    }
</script>

<style lang="scss">
    .pipeline-yaml-diff {
        background: #1d1d1d;
    }
</style>
