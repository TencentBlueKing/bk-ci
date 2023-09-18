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
    import ciYamlTheme from '@/utils/ciYamlTheme'
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
            this.monaco = await import(
                /* webpackMode: "lazy" */
                /* webpackPrefetch: true */
                /* webpackPreload: true */
                /* webpackChunkName: "monaco-editor" */
                'monaco-editor'
            )

            this.isLoading = false

            this.monaco.editor.defineTheme('ciYamlTheme', ciYamlTheme)

            this.editor = this.monaco.editor.createDiffEditor(
                this.$refs.diffBox,
                {
                    language: 'yaml',
                    theme: 'ciYamlTheme',
                    readOnly: this.readOnly,
                    automaticLayout: true,
                    hideCursorInOverviewRuler: false
                }
            )
            if (this.oldYaml && this.newYaml) {
                this.diff(this.oldYaml, this.newYaml)
            }
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
