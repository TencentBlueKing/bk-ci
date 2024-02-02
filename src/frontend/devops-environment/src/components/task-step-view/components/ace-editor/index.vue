<template>
    <div
        ref="aceEditor"
        class="bkci-ace-editor"
        :style="{ height: `${height}px` }">
        <div
            ref="contentWrapper"
            v-bkloading="{ isLoading: isLoading, opacity: 0.2 }"
            class="bkci-ace-content"
            :class="{ readonly }"
            :style="boxStyle">
            <div
                class="bkci-ace-title"
                :style="{ height: `${tabHeight}px` }">
                <div
                    v-for="(val, key) in tabList"
                    :key="val"
                    class="bkci-ace-mode-item"
                    :class="{ 'active': currentLang === key }">
                    {{ key }}
                </div>
            </div>
            <div class="bkci-ace-main">
                <div class="ace-edit-content">
                    <div
                        :id="selfId"
                        :style="editorStyle" />
                </div>
                <div class="right-side-panel">
                    <slot name="side" />
                </div>
            </div>
        </div>
    </div>
</template>
<script>
    import ace from 'ace-builds/src-noconflict/ace'
    import { Base64 } from 'js-base64'

    import 'ace-builds/src-noconflict/mode-sh'
    import 'ace-builds/src-noconflict/snippets/sh'
    import 'ace-builds/src-noconflict/mode-batchfile'
    import 'ace-builds/src-noconflict/snippets/batchfile'
    import 'ace-builds/src-noconflict/mode-perl'
    import 'ace-builds/src-noconflict/snippets/perl'
    import 'ace-builds/src-noconflict/mode-python'
    import 'ace-builds/src-noconflict/snippets/python'
    import 'ace-builds/src-noconflict/mode-powershell'
    import 'ace-builds/src-noconflict/snippets/powershell'
    import 'ace-builds/src-noconflict/mode-sql'
    import 'ace-builds/src-noconflict/snippets/sql'
    import 'ace-builds/src-noconflict/theme-monokai'
    import 'ace-builds/src-noconflict/ext-error_marker'
    import 'ace-builds/src-noconflict/ext-language_tools'
    import 'ace-builds/src-noconflict/ext-keybinding_menu'
    import 'ace-builds/src-noconflict/ext-elastic_tabstops_lite'

    const TAB_HEIGHT = 40
    const LANG_MAP = {
        Shell: 'sh',
        Bat: 'batchfile',
        Perl: 'perl',
        Python: 'python',
        Powershell: 'powershell',
        SQL: 'sql'
    }
    const HTMLEncode = (value) => {
        const temp = document.createElement('textarea')
        temp.value = value
        return temp.value
    }

    export default {
        name: 'AceEditor',
        inheritAttrs: false,
        props: {
            // 脚本内容
            value: {
                type: String
            },
            height: {
                type: Number,
                default: 480
            },
            // 只读模式
            readonly: {
                type: Boolean,
                default: false
            },
            readonlyTips: {
                type: String,
                default: ''
            },
            // 当前的脚本语言
            lang: {
                type: String,
                required: true
            },
            // 可支持切换的脚本类型（array：显示tab; string: 不显示tab）
            options: {
                type: [
                    String,
                    Array
                ],
                default: () => Object.keys(LANG_MAP)
            }
        },
        data () {
            return {
                isLoading: false,
                content: '',
                currentLang: this.lang,
                isFullScreen: false,
                isShowHistoryPanel: false,
                tabHeight: TAB_HEIGHT,
                currentUser: {}
            }
        },
        computed: {
            /**
             * @desc 脚本编辑器块的样式
             * @returns {Object}
             */
            boxStyle () {
                const style = {
                    position: 'absolute',
                    top: 0,
                    left: 0,
                    width: '100%',
                    height: '100%'
                }
                if (this.isFullScreen) {
                    style.position = 'fixed'
                    style.zIndex = 1000 // eslint-disable-line no-underscore-dangle
                    style.height = '100vh'
                }
                return style
            },
            /**
             * @desc 脚本输入区的样式
             * @returns {Object}
             */
            editorStyle () {
                return {
                    height: this.isFullScreen ? `calc(100vh - ${TAB_HEIGHT}px)` : `${this.height - TAB_HEIGHT}px`
                }
            },
            /**
             * @desc 显示类型显示列表
             * @returns {Object}
             */
            tabList () {
                return this.options.reduce((res, item) => {
                    if (Object.prototype.hasOwnProperty.call(LANG_MAP, item)) {
                        res[item] = LANG_MAP[item]
                    }
                    return res
                }, {})
            },
            /**
             * @desc 脚本编辑器语言模式
             * @returns {String}
             */
            mode () {
                return `ace/mode/${LANG_MAP[this.currentLang]}`
            }
        },
        watch: {
            lang (newLang) {
                if (this.currentLang !== newLang) {
                    this.currentLang = newLang
                    setTimeout(() => {
                        this.editor.getSession().setMode(this.mode)
                    })
                }
            },
            readonly (readonly) {
                this.editor.setReadOnly(readonly)
            }
        },
        created () {
            this.selfId = `ace_editor_${Math.floor(Math.random() * 1000)}_${Date.now()}`
            this.valueMemo = {}
        },
        beforeDestroy () {
            this.handleExitFullScreen()
            this.$store.commit('setScriptCheckError', null)
        },
        mounted () {
            this.initEditor()
            document.body.addEventListener('keyup', this.handleExitByESC)
            this.$once('hook:beforeDestroy', () => {
                document.body.removeEventListener('keyup', this.handleExitByESC)
            })
        },
        methods: {
            /**
             * @desc 初始化脚本编辑器
             */
            initEditor () {
                const editor = ace.edit(this.selfId)
                editor.getSession().setMode(this.mode)
                editor.setOptions({
                    fontSize: 13,
                    enableBasicAutocompletion: true,
                    enableLiveAutocompletion: true,
                    enableSnippets: true,
                    wrapBehavioursEnabled: true,
                    autoScrollEditorIntoView: true,
                    copyWithEmptySelection: true,
                    useElasticTabstops: true,
                    printMarginColumn: true,
                    printMargin: 80,
                    scrollPastEnd: 0.2
                })
                editor.setTheme('ace/theme/monokai')
                editor.setShowPrintMargin(false)
                editor.$blockScrolling = Infinity
                editor.setReadOnly(this.readonly)

                editor.on('change', () => {
                    this.content = editor.getValue()
                    const content = Base64.encode(this.content)
                    this.editor.getSession().setAnnotations([])
                    this.$emit('input', content)
                    this.$emit('change', content)
                })
                editor.on('paste', (event) => {
                    event.text = HTMLEncode(event.text)
                })
                // 先保存 editor 在设置 value
                this.editor = editor

                if (this.value) {
                    this.editor.getSession().setAnnotations([])
                    // 只读模式没有默认值，直接使用输入值
                    if (this.readonly) {
                        this.editor.setValue(this.value)
                        this.editor.clearSelection()
                    }
                }
                this.$once('hook:beforeDestroy', () => {
                    editor.destroy()
                    editor.container.remove()
                })

                const $handler = document.querySelector(`#${this.selfId}`)
                $handler.addEventListener('keydown', this.handleReadonlyWarning)
                this.$once('hook:beforeDestroy', () => {
                    $handler.removeEventListener('keydown', this.handleReadonlyWarning)
                })
            },
            /**
             * @desc 外部调用
             */
            resize () {
                this.$nextTick(() => {
                    this.editor.resize()
                })
            },
            /**
             * @desc 外部调用-设置脚本编辑器内容
             * @param {String} 经过 base64 编码的脚本内容
             */
            setValue (value) {
                this.editor.setValue(Base64.decode(value))
                this.editor.clearSelection()
                this.editor.scrollToLine(Infinity)
            },
            /**
             * @desc 外部调用-重置脚本编辑内容使用默认脚本
             */
            resetValue () {
                this.editor.setValue(this.defaultScriptMap[this.lang])
                this.editor.clearSelection()
                this.editor.scrollToLine(Infinity)
            },
            /**
             * @desc readonly模式下键盘操作提示
             * @param {Object} event keydown事件
             */
            handleReadonlyWarning (event) {
                if (!this.readonly) {
                    return
                }
                const { target } = event
                // 脚本编辑器获得焦点的状态
                if (target.type !== 'textarea') {
                    return
                }

                if ([
                    'Escape',
                    'Meta',
                    'ShiftLeft',
                    'ShiftRight',
                    'ControlLeft',
                    'ControlRight',
                    'AltLeft',
                    'AltRight'
                ].includes(event.code)) {
                    return
                }
                if ((event.metaKey || event.ctrlKey)
                    && !['KeyV', 'KeyX'].includes(event.code)) {
                    return
                }
                this.$bkMessage({
                    theme: 'warning',
                    message: this.$t('environment.只读模式不支持编辑')
                })
            },
            
            /**
             * @desc 切换编辑的全屏状态
             *
             * 全屏时需要把dom移动到body下面
             */
            handleFullScreen () {
                this.isFullScreen = true
                this.messageInfo(this.$t('按 Esc 即可退出全屏模式'))
                document.body.appendChild(this.$refs.contentWrapper)
                this.$nextTick(() => {
                    this.editor.resize()
                })
            },
            /**
             * @desc 退出编辑的全屏状态
             *
             * 退出全屏时需要要把dom还原到原有位置
             */
            handleExitFullScreen () {
                this.isFullScreen = false
                this.$refs.aceEditor.appendChild(this.$refs.contentWrapper)
                this.$nextTick(() => {
                    this.editor.resize()
                })
            },
            /**
             * @desc esc快捷键退出编辑的全屏状态
             */
            handleExitByESC (event) {
                if (event.code !== 'Escape' || !this.isFullScreen) {
                    return
                }
                this.handleExitFullScreen()
            }
        }
    }
</script>
<style lang='scss'>
    .bkci-ace-editor {
        position: relative;
        display: flex;
        flex-direction: column;
        width: 100%;
        .ace_editor {
            padding-right: 14px;
            overflow: unset;
            font-family: Menlo, Monaco, Consolas, Courier, monospace;

        .ace_scrollbar.ace_scrollbar-v,
        .ace_scrollbar.ace_scrollbar-h {
            &::-webkit-scrollbar {
                background: #272822 !important;
            }
            &::-webkit-scrollbar-thumb {
                background-color: #3b3c42;
                border: 1px solid #63656e;
                border-radius: 0;
            }

            &::-webkit-scrollbar-corner {
                background-color: transparent;
            }
        }

        .ace_scrollbar-v {
            &::-webkit-scrollbar {
                width: 14px;
            }
        }

        .ace_scrollbar-h {
            &::-webkit-scrollbar {
                height: 14px;
            }
        }
    }

    .readonly {
        .bkci-ace-title,
        .ace_gutter,
        .ace_content {
            filter: grayscale(0%) brightness(80%) saturate(70%) opacity(95%);
        }

        .bkci-ace-mode-item {
            cursor: default;
        }
    }
  }

    .bkci-ace-title {
        display: flex;
        font-size: 14px;
        color: #fff;
        background: #202024;

        .bkci-ace-mode-item {
            display: flex;
            padding: 0 22px;
            color: #979ba5;
            cursor: pointer;
            border-top: 2px solid transparent;
            user-select: none;
            align-items: center;

            &.active {
                color: #fff;
                background: #313238;
                border-top: 2px solid #3a84ff;
            }
        }
    }

  .bkci-ace-main {
    display: flex;
    background: #272822;

    .ace-edit-content {
      flex: 1;
      overflow: hidden;
    }

    .bk-loading {
      background: "rgba(0, 0, 0, 80%)" !important;
    }
  }

  .bkci-ace-action {
    position: absolute;
    top: 0;
    right: 0;
    z-index: 1;
    display: flex;
    align-items: center;
    padding-right: 9px;
    font-size: 16px;
    line-height: 1;
    color: #c4c6cc;

    .job-icon {
      padding: 10px 9px;
      cursor: pointer;
    }
  }

  .bkci-ace-history-panel {
    position: absolute;
    top: 40px;
    right: 10px;
    width: 350px;
    background: #fff;
    border-radius: 2px;
    user-select: none;

    &::before {
      position: absolute;
      top: -4px;
      right: 45px;
      width: 10px;
      height: 10px;
      background: inherit;
      content: "";
      transform: rotateZ(-45deg);
    }

    .panel-header {
      display: flex;
      align-items: center;
      height: 60px;
      padding: 0 20px;
      font-size: 14px;
      color: #313238;
      border-bottom: 1px solid #e7e7e7;

      .save-btn {
        display: flex;
        width: 86px;
        height: 32px;
        margin-left: auto;
        color: #63656e;
        cursor: pointer;
        background: #fff;
        border: 1px solid #c4c6cc;
        border-radius: 2px;
        align-items: center;
        justify-content: center;
      }
    }

    .panel-body {
      padding: 10px 20px;
      font-family: 'MicrosoftYaHei'; /* stylelint-disable-line */
      font-size: 12px;
      color: #4f5050;
      background: #fafbfd;

      .item {
        display: flex;
        height: 32px;
        align-items: center;
      }

      .history-name {
        width: 255px;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      .history-action {
        margin-left: auto;
        color: #3a84ff;
        cursor: pointer;
      }
    }

    .history-empty {
      padding-top: 44px;
      padding-bottom: 85px;
    }
  }
</style>
