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
    <div
        v-bkloading="{ isLoading }"
        :style="{ 'height': calcSize(height), 'width': calcSize(width) }"
    >
    </div>
</template>
<script>

    export default {
        props: {
            value: {
                type: String,
                default: ''
            },
            width: {
                type: [Number, String],
                default: 500
            },
            height: {
                type: [Number, String],
                default: 300
            },
            lang: {
                type: String,
                default: 'text'
            },
            theme: {
                type: String,
                default: 'monokai'
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
            }
        },
        data () {
            return {
                editor: null,
                isLoading: false,
                monaco: null
            }
        },
        watch: {
            value (newValue) {
                if (this.editor) {
                    if (newValue !== this.editor.getValue()) {
                        this.editor.setValue(newValue)
                    }
                }
            },

            lang (newVal) {
                if (this.editor) {
                    this.monaco.editor.setModelLanguage(this.editor.getModel(), newVal)
                }
            },

            theme (newVal) {
                if (this.editor) {
                    this.monaco.editor.setTheme(newVal)
                }
            },

            fullScreen () {
                this.$el.classList.toggle('ace-full-screen')
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
            this.editor = this.monaco.editor.create(this.$el, {
                value: this.value,
                language: this.getLang(this.lang),
                theme: 'vs-dark',
                automaticLayout: true,
                minimap: {
                    enabled: false
                },
                readOnly: this.readOnly
            })
            this.isLoading = false

            this.editor.onDidChangeModelContent(event => {
                const value = this.editor.getValue()
                if (this.value !== value) {
                    this.$emit('change', value, event)
                    this.$emit('update:hasError', !value)
                    this.$emit('input', value, this.editor, event)
                }
            })
        },
        beforeDestroy () {
            this.editor?.dispose?.()
        },
        methods: {
            getLang (lang) {
                const langMap = {
                    sh: 'shell',
                    batchfile: 'bat'
                }

                return langMap[lang] || lang
            },
            calcSize (size) {
                const _size = size.toString()

                if (_size.match(/^\d*$/)) return `${size}px`
                if (_size.match(/^[0-9]?%$/)) return _size

                return '100%'
            }
        }
    }
</script>
