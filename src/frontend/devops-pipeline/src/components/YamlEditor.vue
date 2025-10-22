/*
* Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
*
* Copyright (C) 2019 Tencent. All rights reserved.
*
* BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
*
* A copy of the MIT License is included in this file.
*
*
* Terms of the MIT License:
* ---------------------------------------------------
* Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit
persons to whom the Software is furnished to do so, subject to the following conditions:
*
* The above copyright notice and this permission notice shall be included in all copies or substantial portions of the
Software.
*
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/
<template>
    <section
        v-bkloading="{ isLoading }"
        :style="style"
    >
        <i
            :class="['yaml-full-screen-switcher', 'devops-icon', fullScreen ? 'icon-un-full-screen' : 'icon-full-screen']"
            :title="$t('editPage.isFullScreen')"
            @click="toggleFullScreen"
        />
        <div
            ref="box"
            :style="style"
        >
        </div>
        <ul
            v-if="!readOnly"
            class="yaml-error-summary"
        >
            <li
                v-for="(item, index) in errorList"
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
                // editor: null,
                isLoading: false,
                fullScreen: false,
                errorList: [],
                style: {
                    height: '100%',
                    width: '100%'
                }
            }
        },
        watch: {
            value (newValue) {
                if (this.monaco && !this.codeLens) {
                    this.registerCodeLensProvider()
                }
                if (this.editor) {
                    if (newValue !== this.editor.getValue()) {
                        this.editor.setValue(newValue)
                    }
                }
            },
            fullScreen () {
                this.$el.classList.toggle('ace-full-screen')
            },
            highlightRanges () {
                this.highlightBlocks(this.highlightRanges)
            },
            readOnly (val) {
                this.editor.updateOptions({ readOnly: val })
            }
        },
        async mounted () {
            this.isLoading = true
            this.monaco = await MonacoEditor.instance()
            this.editor = this.monaco.editor.create(this.$refs.box, {
                model: this.monaco.editor.createModel(this.value, 'yaml', this.monaco.Uri.parse(this.yamlUri)),
                automaticLayout: true,
                formatOnPaste: true,
                unicodeHighlight: {
                    ambiguousCharacters: false
                },
                minimap: {
                    enabled: false
                },
                readOnly: this.readOnly
            })
            this.monaco.editor.onDidChangeMarkers(() => {
                this.errorList = this.monaco.editor.getModelMarkers({
                    resource: this.monaco.Uri.parse(this.yamlUri)
                })
            })
            if (!this.codeLens && !!this.value) {
                this.registerCodeLensProvider()
            }
            this.isLoading = false
            this.highlightBlocks(this.highlightRanges)

            this.editor.onDidChangeModelContent(event => {
                const value = this.editor.getValue()

                if (this.value !== value) {
                    this.emitChange(value)
                }
                if (this.monaco && !this.codeLens) {
                    this.registerCodeLensProvider()
                } else {
                    this.$nextTick(() => {
                    this.scheuleUpdateCodeLens?.()
                    })
                }
            })
        },
        beforeDestroy () {
        this.editor?.getModel()?.dispose?.()
        this.codeLens?.dispose?.()
        this.editor?.dispose?.()
        },
        methods: {
            toggleFullScreen () {
                this.fullScreen = !this.fullScreen
            },
            insertFragmentAtPos (text, { stageIndex, containerIndex, elementIndex }) {
                try {
                    const doc = YAML.parse(this.value)
                    const jobs = this.getJobsByPos(doc, { stageIndex, containerIndex })
                    
                    // eslint-disable-next-line no-unused-vars
                    jobs[elementIndex] = YAML.parse(text)[0]
                    const result = YAML.stringify(doc)
                    this.emitChange(result)
                } catch (error) {
                    console.error(error)
                    this.$bkMessage({
                        theme: 'error',
                        message: error.message ?? error
                    })
                }
            },
            emitChange (value) {
                this.$emit('change', value)
                this.$emit('update:hasError', this.errorList.length > 0)
                this.$emit('input', value, this.editor)
            },
            format () {
            this.editor?.getAction('editor.action.formatDocument').run()
            },
            highlightBlocks (blocks) {
                if (this.monaco && this.editor && Array.isArray(blocks) && blocks.length > 0) {
                    const ranges = blocks.map(({ startMark, endMark }) => ({
                        range: new this.monaco.Range(
                            startMark.line + 1,
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
            handleYamlPluginClick ({ editingElementPos, pos, range }) {
                const atom = this.getAtomByPos(editingElementPos)
                if (atom.run) {
                    this.$emit('step-click', editingElementPos, {
                        ...atom,
                        with: {
                            script: atom.run
                        }
                    })
                } else {
                    this.$emit('step-click', editingElementPos, atom)
                }
            },
            visitYaml (yaml) {
                const lineCounter = new YAML.LineCounter()
                const doc = YAML.parseDocument(yaml, {
                    lineCounter
                })

                let steps = []
                let stageIndex = -1
                let containerIndex = -1
                const monaco = this.monaco

                YAML.visit(doc, {
                    Pair (_, pair) {
                        if (pair.key && pair.key.value === 'jobs') {
                            stageIndex++
                            containerIndex = -1
                        }
                        if (pair.key && pair.key.value === 'steps') {
                            if (stageIndex < 0) {
                                stageIndex = 0
                            }
                            containerIndex++
                            if (Array.isArray(pair.value.items)) {
                                steps = steps.concat(pair.value.items.map((item, index) => {
                                    return {
                                        pos: lineCounter.linePos(item.range[0]),
                                        range: new monaco.Range(
                                            lineCounter.linePos(item.range[0]).line,
                                            lineCounter.linePos(item.range[0]).col,
                                            lineCounter.linePos(item.range[1]).line,
                                            lineCounter.linePos(item.range[1]).col
                                        ),
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
                    }
                })
                return steps
            },
            getAtomByPos ({ stageIndex, containerIndex, elementIndex }) {
                try {
                    const doc = YAML.parse(this.value)
                    const steps = this.getJobsByPos(doc, { stageIndex, containerIndex })
                    return steps[elementIndex]
                } catch (error) {
                    return null
                }
            },
            getJobsByPos (doc, { stageIndex, containerIndex }) {
                try {
                    if (doc.stages?.[stageIndex] || doc.jobs) {
                        const jobs = doc.stages?.[stageIndex] ? Object.values(doc.stages[stageIndex].jobs) : Object.values(doc.jobs)
                        return jobs[containerIndex]?.steps ?? jobs[containerIndex]
                    }
                    return doc.steps
                } catch (error) {
                    return null
                }
            },
            registerCodeLensProvider (provider) {
                if (this.showYamlPlugin && !this.readOnly) {
                    console.log('codelens')

                    this.codeLens = this.monaco.languages.registerCodeLensProvider('yaml', {
                        onDidChange: (cb) => {
                            this.scheuleUpdateCodeLens = cb
                            return {
                                dispose: () => {
                                }
                            }
                        },
                        provideCodeLenses: (model, token) => {
                            const title = this.$t('atomModel')
                            const steps = this.visitYaml(this.value)
                            return {
                                lenses: steps.map((item, index) => ({
                                    range: {
                                        startLineNumber: item.pos.line,
                                        endLineNumber: item.pos.line,
                                        startColumn: item.pos.col
                                    },
                                    id: index,
                                    command: {
                                        id: this.editor.addCommand(0, () => {
                                            this.handleYamlPluginClick(item)
                                        }, ''),
                                        title
                                    }
                                })),
                                dispose: () => {
                                }
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

.yaml-full-screen-switcher {
    cursor: pointer;
    position: absolute;
    z-index: 1;
    right: 24px;
    top: 24px;
    color: white;
}

.code-highlight-block {
    background: #232D46;
}

.monaco-editor .codelens-decoration {
    // left: 0 !important;
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

    >li {
        display: flex;
        align-items: center;
        font-size: 12px;
        grid-gap: 8px;

        &:not(:last-child) {
            margin-bottom: 12px;
        }

        >p>span {
            color: white;
        }

        .devops-icon.icon-close-circle-fill {
            color: $dangerColor;
        }
    }
}
</style>
