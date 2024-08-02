<template>
    <section class="code-section-home">
        <section :class="[{ 'max-height': limitHeight && !isFullScreen, 'un-full-screen': !isFullScreen }, 'ci-yml']" :style="styleVar"></section>
        <i :class="['bk-icon', isFullScreen ? 'icon-un-full-screen' : 'icon-full-screen']" @click="toggleFullScreen(!isFullScreen)"></i>
    </section>
</template>

<script>
    import CodeMirror from 'codemirror'
    import 'codemirror/addon/display/autorefresh'
    import 'codemirror/mode/yaml/yaml'
    import 'codemirror/lib/codemirror.css'
    import 'codemirror/theme/3024-night.css'
    import 'codemirror/addon/display/fullscreen'
    import 'codemirror/addon/display/fullscreen.css'

    export default {
        props: {
            readOnly: {
                type: Boolean,
                default: true
            },
            limitHeight: {
                type: Boolean,
                default: true
            },
            cursorBlinkRate: {
                type: Number,
                default: 0
            },
            code: {
                type: String,
                require: true
            },
            height: {
                type: String,
                default: '400px'
            }
        },

        data () {
            const vm = this
            return {
                codeMirrorCon: {
                    lineNumbers: true,
                    lineWrapping: true,
                    tabMode: 'indent',
                    mode: 'yaml',
                    theme: '3024-night',
                    cursorBlinkRate: this.cursorBlinkRate,
                    readOnly: this.readOnly,
                    autoRefresh: true,
                    autofocus: false,
                    extraKeys: {
                        Esc () {
                            vm.toggleFullScreen(false)
                        }
                    }
                },
                codeEditor: undefined,
                isFullScreen: false
            }
        },

        computed: {
            styleVar () {
                return {
                    '--code-editor-height': this.height
                }
            }
        },

        watch: {
            code () {
                if (this.code === this.codeEditor.getValue()) return
                this.codeEditor.setValue(this.code || '')
            }
        },

        mounted () {
            this.initCodeMirror()
        },

        beforeDestroy () {
            this.codeEditor = undefined
        },

        methods: {
            initCodeMirror () {
                const ele = this.$el.querySelector('.ci-yml')
                if (this.codeEditor) ele.innerHTML = ''
                this.codeEditor = CodeMirror(ele, this.codeMirrorCon)
                this.codeEditor.setValue(this.code || '')
                this.codeEditor.on('change', this.changeValue)
                this.codeEditor.on('blur', this.blur)
                this.codeEditor.on('focus', this.focus)
            },

            blur () {
                this.$emit('blur')
            },

            focus () {
                this.$emit('focus')
            },

            changeValue (instance) {
                const value = instance.getValue()
                this.$emit('change')
                this.$emit('update:code', value)
            },

            getValue () {
                return this.codeEditor.getValue()
            },

            toggleFullScreen (val) {
                this.isFullScreen = val
                this.codeEditor.setOption('fullScreen', val)
                this.$nextTick(() => {
                    this.codeEditor.refresh()
                })
            }
        }
    }
</script>

<style lang="postcss" scoped>
    .code-section-home {
        position: relative;
    }

    .ci-yml {
        background: black;
    }

    .bk-icon {
        position: absolute;
        right: 10px;
        top: 10px;
        color: #fff;
        cursor: pointer;
        z-index: 999;
        &.icon-un-full-screen {
            position: fixed;
        }
    }

    .max-height {
        height: var(--code-editor-height);
        /deep/ .CodeMirror-scroll {
            height: var(--code-editor-height);
        }
        /deep/ .CodeMirror {
            max-height: var(--code-editor-height);
        }
    }

    /deep/ .CodeMirror {
        font-family: Consolas, "Courier New", monospace;
        line-height: 1.5;
    }
    .un-full-screen /deep/ .CodeMirror {
        min-height: 300px;
        margin-bottom: 20px;
        height: auto;
        .CodeMirror-scroll {
            min-height: 300px;
        }
    }
</style>
