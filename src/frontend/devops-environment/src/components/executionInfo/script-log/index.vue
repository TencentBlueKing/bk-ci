<template>
    <div
        class="step-execute-script-log"
        v-bkloading="{
            isLoading,
            opacity: .1
        }">
        <div class="log-wraper">
            <div v-once id="executeScriptLog" style="height: 100%;" />
        </div>
        <div class="log-action-box">
            <div class="action-item" v-bk-tooltips="backTopTips" @click="handleScrollTop">
                <icon name="up-to-top" size="20" />
            </div>
            <div class="action-item action-bottom" v-bk-tooltips="backBottomTips" @click="handleScrollBottom">
                <icon name="up-to-top" size="20" />
            </div>
        </div>
    </div>
</template>
<script>
    import ace from 'ace-builds/src-noconflict/ace'
    import 'ace-builds/src-noconflict/mode-text'
    import 'ace-builds/src-noconflict/theme-monokai'
    import 'ace-builds/src-noconflict/ext-searchbox'
    import { mapActions } from 'vuex'

    export default {
        // mixins: [
        //     mixins
        // ],
        props: {
            name: String,
            hostId: Number,
            finished: Boolean,
            ip: {
                type: String
            },
            logFilter: {
                type: String,
                default: ''
            },
            fontSize: {
                type: Number,
                default: 12
            },
            lineFeed: {
                type: Boolean,
                default: true
            }
        },
        data () {
            return {
                // 日志loading，切换主机的时候才显示
                isLoading: false,
                // 自动动滚动到底部
                isWillAutoScroll: true
            }
        },
        computed: {
            jobInstanceId () {
                return this.$route.query.jobInstanceId
            },
            stepInstanceId () {
                return this.$route.query.stepInstanceId
            },
            projectId () {
                return this.$route.params.projectId
            }
        },
        watch: {
            /**
             * @desc 字体大小改变时虚拟滚动重新计算
             */
            fontSize: {
                handler (fontSize) {
                    this.editor.setFontSize(fontSize)
                }
            },
            lineFeed: {
                handler (lineFeed) {
                    setTimeout(() => {
                        this.editor && this.editor.setOptions({
                            wrap: lineFeed ? 'free' : 'none'
                        })
                    })
                },
                immediate: true
            },
            /**
             * @desc 查看的日志目标改变，重新获取日志
             *
             * 日志目标改变，重置页面操作的数据
             */
            hostId: {
                handler (val) {
                    if (val) {
                        this.autoScrollTimeout()
                        this.fetchLogContent()
                    } else {
                        this.editor && this.editor.setValue('')
                        this.editor && this.editor.clearSelection()
                    }
                },
                immediate: true
            }
        },
        created () {
            this.backTopTips = {
                content: this.$t('environment.回到顶部'),
                placements: [
                    'top'
                ],
                theme: 'light'
            }
            this.backBottomTips = {
                content: this.$t('environment.前往底部'),
                placements: [
                    'top'
                ],
                theme: 'light'
            }
        },
        mounted () {
            this.initEditor()
        },
        methods: {
            ...mapActions('environment', [
                'getJobInstanceLogs'
            ]),
            /**
             * @desc 获取脚本日志
             */
            fetchLogContent () {
                if (!this.ip) {
                    this.isLoading = false
                    if (this.editor) {
                        this.editor.setValue('')
                        this.editor.clearSelection()
                    }
                }
                if (this.finished) {
                    this.isLoading = true
                }

                this.getJobInstanceLogs({
                    projectId: this.projectId,
                    jobInstanceId: this.jobInstanceId,
                    stepInstanceId: this.stepInstanceId,
                    hostIdList: [this.hostId]
                })
                    .then(res => {
                        const logContent = res.scriptTaskLogs[0].logContent || ''
                        
                        this.editor.getSession().setValue(logContent)
                        this.editor.scrollToLine(Infinity)
                        if (!this.finished) {
                            setTimeout(() => {
                                this.fetchLogContent()
                            }, 5000)
                        }
                    })
                    .finally(() => {
                        this.isLoading = false
                    })
            },
            initEditor () {
                const editor = ace.edit('executeScriptLog')
                // editor.getSession().setMode('ace/mode/text')
                editor.setTheme('ace/theme/monokai')
                editor.setOptions({
                    fontSize: this.fontSize,
                    wrapBehavioursEnabled: true,
                    copyWithEmptySelection: true,
                    useElasticTabstops: true,
                    printMarginColumn: false,
                    printMargin: 80,
                    showPrintMargin: false,
                    scrollPastEnd: 0.05,
                    fixedWidthGutter: true
                })
                editor.$blockScrolling = Infinity
                editor.setReadOnly(true)
                const editorSession = editor.getSession()
                // 自动换行时不添加缩进
                editorSession.$indentedSoftWrap = false
                editorSession.setUseWrapMode(true)
                editorSession.on('changeScrollTop', (scrollTop) => {
                    const {
                        height,
                        maxHeight
                    } = editor.renderer.layerConfig
                    this.isWillAutoScroll = height + scrollTop + 30 >= maxHeight
                })
                this.editor = editor
                this.$once('hook:beforeDestroy', () => {
                    editor.destroy()
                    editor.container.remove()
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
             * @desc 日志滚动定时器
             */
            autoScrollTimeout () {
                if (this.isWillAutoScroll && !this.isLoading) {
                    this.handleScrollBottom()
                }
                setTimeout(() => {
                    this.autoScrollTimer = this.autoScrollTimeout()
                }, 1000)
            },
            /**
             * @desc 回到日志顶部
             */
            handleScrollTop () {
                this.editor.scrollToLine(0)
            },
            /**
             * @desc 回到日志底部
             */
            handleScrollBottom () {
                this.isWillAutoScroll = true
                this.editor.scrollToLine(Infinity)
            }
        }
    }
</script>
<style lang='scss'>
    @keyframes script-execute-loading {
        0% {
            content: ".";
        }

        30% {
            content: "..";
        }

        60% {
            content: "...";
        }
    }

    .step-execute-script-log {
        position: relative;
        height: 100%;
        max-height: 100%;
        min-height: 100%;

        .log-wraper {
            position: absolute;
            top: 0;
            bottom: 20px;
            left: 0;
            width: 100%;
            padding-right: 20px;
            /* stylelint-disable selector-class-pattern */
            .ace_editor {
                overflow: unset;
                line-height: 1.6;
                color: #c4c6cc;
                background: #1d1d1d;

                .ace_gutter {
                    padding-top: 4px;
                    margin-bottom: -4px;
                    color: #63656e;
                    background: #292929;
                }

                .ace_scroller {
                    padding-top: 4px;
                    margin-bottom: -4px;
                    
                }

                .ace_hidden-cursors .ace_cursor {
                    opacity: 0% !important;
                }

                .ace_selected-word {
                    background: rgb(135 139 145 / 25%);
                }

                .ace_scrollbar-v,
                .ace_scrollbar-h {
                    &::-webkit-scrollbar {
                        background: #1d1d1d !important;
                    }
                    &::-webkit-scrollbar-thumb {
                        background-color: #3b3c42;
                        border: 1px solid #63656e;
                    }

                    &::-webkit-scrollbar-corner {
                        background-color: transparent;
                    }
                }

                .ace_scrollbar-v {
                    margin-right: -20px;

                    &::-webkit-scrollbar {
                        width: 14px;
                    }
                }

                .ace_scrollbar-h {
                    margin-bottom: -20px;

                    &::-webkit-scrollbar {
                        height: 14px;
                    }
                }
            }
        }

        .log-status {
            position: absolute;
            bottom: 0;
            left: 0;
            padding-left: 20px;
            color: #fff;
        }

        .log-loading {
            &::after {
                display: inline-block;
                content: ".";
                animation: script-execute-loading 2s linear infinite;
            }
        }

        .keyword {
            color: #212124;
            background: #f0dc73;
        }

        .log-action-box {
            position: absolute;
            right: 20px;
            bottom: 20px;
            z-index: 10;
            display: flex;

            .action-item {
                position: relative;
                display: flex;
                width: 32px;
                height: 32px;
                margin-left: 12px;
                font-size: 18px;
                color: #000;
                cursor: pointer;
                background: rgb(255 255 255 / 80%);
                border-radius: 50%;
                align-items: center;
                justify-content: center;

                &:hover {
                    background: rgb(255 255 255);
                }

                &.action-bottom {
                    transform: rotateZ(180deg);
                }
            }
        }
    }
</style>
