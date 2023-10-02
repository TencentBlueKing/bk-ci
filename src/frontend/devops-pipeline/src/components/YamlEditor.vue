/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
<template>
    <section v-bkloading="{ isLoading }" :style="{
        height: '100%',
        width: '100%'
    }">
        <div
            ref="box"
            :style="{
                height: '100%',
                width: '100%'
            }"
        >
        </div>
        <ul v-if="!readOnly" class="yaml-error-summary">
            <li
                v-for="(item, index) in errors"
                :key="index"
            >
                <i class="devops-icon icon-close-circle-fill"></i>
                <p>
                    <span>
                        {{ item.message }}
                    </span>
                    ({{ item.startLineNumber }}, {{ item.startColumn }})
                </p>
            </li>
        </ul>
    </section>
</template>
<script>
    import MonacoEditor from '@/utils/monacoEditor'
    import YAML from 'yaml'
    // import { listen } from 'vscode-ws-jsonrpc'

    // import {
    //     MonacoLanguageClient,
    //     CloseAction,
    //     ErrorAction,
    //     createConnection,
    //     MonacoServices
    // } from 'monaco-languageclient'

    // window.setImmediate = window.setTimeout
    export default {
        props: {
            value: {
                type: String,
                default: ''
            },
            readOnly: {
                type: Boolean,
                default: false
            },
            fullScreen: {
                type: Boolean,
                default: false
            },
            hasError: {
                type: Boolean,
                default: false
            },
            highlightRanges: {
                type: Array,
                default: () => []
            },
            yamlUri: {
                type: String,
                default: '.ci.yml'
            },
            showYamlPlugin: {
                type: Boolean,
                default: false
            }
        },
        data () {
            return {
                editor: null,
                isLoading: false,
                monaco: null,
                errors: []
            }
        },
        watch: {
            value (newValue) {
                console.log('change', newValue !== this.editor?.getValue?.())
                if (this.editor) {
                    if (newValue !== this.editor.getValue()) {
                        this.editor.setValue(newValue)
                        setTimeout(() => {
                            this.registerCodeLensProvider()
                        }, 0)
                    }
                }
            },
            fullScreen () {
                this.$el.classList.toggle('ace-full-screen')
            },
            highlightRanges () {
                this.highlightBlocks(this.highlightRanges)
            }
        },
        async mounted () {
            this.isLoading = true
            this.monaco = await MonacoEditor.instance()
            this.editor = this.monaco.editor.create(this.$refs.box, {
                model: this.monaco.editor.createModel(this.value, 'yaml', this.monaco.Uri.parse(this.yamlUri)),
                // automaticLayout: true,
                formatOnPaste: true,
                minimap: {
                    enabled: false
                },
                readOnly: this.readOnly
            })
            this.monaco.editor.onDidChangeMarkers(() => {
                this.errors = this.monaco.editor.getModelMarkers({
                    resource: this.monaco.Uri.parse(this.yamlUri)
                })
            })

            this.isLoading = false
            this.highlightBlocks(this.highlightRanges)
            this.registerCodeLensProvider()

            this.editor.onDidChangeModelContent(event => {
                const value = this.editor.getValue()
                console.log('didChanges')
                this.errors = this.monaco.editor.getModelMarkers({
                    resource: this.monaco.Uri.parse(this.yamlUri)
                })

                if (this.value !== value) {
                    this.$emit('change', value, event)
                    this.$emit('update:hasError', !value)
                    this.$emit('input', value, this.editor, event)
                }
            })
        },
        beforeDestroy () {
            this.editor?.getModel()?.dispose?.()
            this.editor?.dispose?.()
        },
        methods: {
            // connectToLangServer () {
            //     const webSocket = new WebSocket('ws://127.0.0.1:8989')

            //     listen({
            //         webSocket,
            //         onConnection: (connection) => {
            //             const languageClient = this.createLanguageClient(connection)
            //             const disposable = languageClient.start()

            //             connection.onClose(function () {
            //                 return disposable.dispose()
            //             })

            //             connection.onError(function (error) {
            //                 console.log(error)
            //             })
            //         }
            //     })
            // },
            // createLanguageClient (connection) {
            //     return new MonacoLanguageClient({
            //         name: 'Monaco language client',
            //         clientOptions: {
            //             documentSelector: ['yaml'],
            //             errorHandler: {
            //                 error: () => ErrorAction.Continue,
            //                 closed: () => CloseAction.DoNotRestart
            //             }
            //         },

            //         connectionProvider: {
            //             get: (errorHandler, closeHandler) => {
            //                 return Promise.resolve(
            //                     createConnection(connection, errorHandler, closeHandler)
            //                 )
            //             }
            //         }
            //     })
            // },
            format () {
                this.editor?.getAction('editor.action.formatDocument').run()
            },
            highlightBlocks (blocks) {
                if (this.monaco && this.editor && Array.isArray(blocks) && blocks.length > 0) {
                    const ranges = blocks.map(({ startMark, endMark }) => ({
                        range: new this.monaco.Range(
                            startMark.line,
                            startMark.column,
                            endMark.line,
                            endMark.column
                        ),
                        options: {
                            isWholeLine: true,
                            className: 'code-highlight-block',
                            marginClassName: 'code-highlight-block'
                        }
                    }))
                    this.collections?.clear?.()
                    this.collections = this.editor.createDecorationsCollection(ranges)
                    this.editor.revealRangeInCenterIfOutsideViewport(ranges[0].range, this.monaco.editor.ScrollType.Smooth)
                } else {
                    this.collections?.clear?.()
                }
            },
            handleYamlPluginClick (editingElementPos) {
                console.log(YAML.parse(this.value))
                const stages = YAML.parse(this.value).stages
                const jobs = Object.values(stages[editingElementPos.stageIndex].jobs)

                const element = jobs[editingElementPos.containerIndex].steps[editingElementPos.elementIndex].with
                this.$emit('step-click', editingElementPos, element)
            },
            visitYaml (yaml) {
                const lineCounter = new YAML.LineCounter()
                const doc = YAML.parseDocument(yaml, {
                    lineCounter
                })

                let steps = []
                let stageIndex = -1
                let containerIndex = -1

                YAML.visit(doc, {
                    Pair (_, pair) {
                        if (pair.key && pair.key.value === 'jobs') {
                            stageIndex++
                            containerIndex = -1
                        }
                        if (pair.key && pair.key.value === 'steps') {
                            containerIndex++
                            steps = steps.concat(pair.value.items.map((item, index) => {
                                return {
                                    pos: lineCounter.linePos(item.range[0]),
                                    editingElementPos: {
                                        stageIndex,
                                        containerIndex,
                                        elementIndex: index
                                    }
                                }
                            }))
                            return YAML.visit.SKIP
                        }
                    }
                })
                return steps
            },
            registerCodeLensProvider (provider) {
                if (this.showYamlPlugin && !this.readOnly) {
                    console.log('codelens')
                    this.codeLens?.dispose?.()
                    const title = this.$t('atomModel')
                    const steps = this.visitYaml(this.value)
                    this.codeLens = this.monaco.languages.registerCodeLensProvider('yaml', {
                        provideCodeLenses: (model, token) => {
                            return {
                                lenses: steps.map((item, index) => ({
                                    range: {
                                        startLineNumber: item.pos.line,
                                        startColumn: item.pos.col
                                    },
                                    id: index,
                                    command: {
                                        id: this.editor.addCommand(0, () => {
                                            this.handleYamlPluginClick(item.editingElementPos)
                                        }),
                                        title
                                    }
                                })),
                                dispose: () => {}
                            }
                        }
                    })
                } else {
                    this.editor?.updateOptions?.({
                        codeLens: false
                    })
                }
            }
        }
    }
</script>

<style lang="scss">
    @import '@/scss/conf';
    .code-highlight-block {
        background: #3A84FF;
        opacity: .1;
    }
    .monaco-editor .codelens-decoration  {
        left: 0 !important;
        color: #63656E !important;
    }
    .yaml-error-summary {
        position: absolute;
        bottom: 0;
        max-height: 200px;
        width: 100%;
        background: #212121;
        padding: 20px 16px;
        overflow: auto;
        ::webkit-scrollbar {
            width: 14px;
        }
        ::webkit-scrollbar-thumb {
            background: rgba(121, 121, 121, 0.4);
        }
        > li {
            display: flex;
            align-items: center;
            font-size: 12px;
            grid-gap: 8px;
            &:not(:last-child) {
                margin-bottom: 12px;
            }
            > p > span {
                color: white;
            }
            .devops-icon.icon-close-circle-fill {
                color: $dangerColor;
            }
        }
    }
</style>
