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
    import {
        REPOSITORY_API_URL_PREFIX
    } from '@/store/constants'
    import ciYamlTheme from '@/utils/ciYamlTheme'
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
            aceLangMap: {
                type: Object,
                default: () => ({})
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
            }
        },
        data () {
            return {
                editor: null,
                isLoading: false,
                monaco: null
            }
        },
        computed: {
            langMap () {
                return {
                    sh: 'shell',
                    bash: 'shell',
                    batchfile: 'bat',
                    cmd: 'bat',
                    pwsh: 'powershell',
                    ...(this.aceLangMap ?? {})
                }
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
                    this.monaco.editor.setModelLanguage(this.editor.getModel(), this.getLang(newVal))
                }
            },

            fullScreen (val) {
                this.$el.classList.toggle('ace-full-screen')
                const parent = document.querySelector('.bk-sideslider.bkci-property-panel')
                if (parent) {
                    parent.classList.toggle('with-ace-full-screen')
                }
            }
        },
        async mounted () {
            this.isLoading = true
            const [monaco, { GongfengMonacoEditor, ReleaseChannel }, accessToken] = await Promise.all([
                import(
                    /* webpackMode: "lazy" */
                    /* webpackPrefetch: true */
                    /* webpackPreload: true */
                    /* webpackChunkName: "monaco-editor" */
                    'monaco-editor'
                ),
                import(
                    /* webpackMode: "lazy" */
                    /* webpackPrefetch: true */
                    /* webpackPreload: true */
                    /* webpackChunkName: "monaco-editor" */
                    '@tencent/gongfeng-copilot-monaco'
                ),
                this.getAccessToken(false)
            ])
            this.monaco = monaco
            const gongfengEditor = new GongfengMonacoEditor(this.monaco, {
                app: {
                    name: 'bkci',
                    // 接入方版本号
                    version: '1.0.0'
                },
                // env: ReleaseChannel.INSIDER,
                env: ReleaseChannel.PRODUCTION,
                brandPaddingRight: 32,
                authenticatedSession: {
                    accessToken,
                    user: this.$userInfo.username,
                    refreshToken: this.getAccessToken
                }
            })

            this.monaco.editor.defineTheme('ciYamlTheme', ciYamlTheme)
            this.editor = await gongfengEditor.createEditor(this.$el, {
                value: this.value,
                language: this.getLang(this.lang),
                theme: 'ciYamlTheme',
                automaticLayout: true,
                minimap: {
                    enabled: false
                },
                scrollbar: {
                    alwaysConsumeMouseWheel: false
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
                return this.langMap[lang] || lang
            },
            calcSize (size) {
                const _size = size.toString()

                if (_size.match(/^\d*$/)) return `${size}px`
                if (_size.match(/^[0-9]{1,2}%$/)) return _size

                return '100%'
            },
            async getAccessToken (refresh = true) {
                try {
                    const tokenKey = '__GONGFENG_COPILOT_TOKEN__'
                    const token = localStorage.getItem(tokenKey)
                    if (token) return token
                    const res = await this.$ajax.get(`${REPOSITORY_API_URL_PREFIX}/user/copilot/tgit/getCopilotOpenToken?refresh=${refresh}`)
                    localStorage.setItem(tokenKey, res.data)
                    return res.data
                } catch (e) {
                    this.$showTips({
                        message: e.message,
                        theme: 'error'
                    })
                }
            }
        }
    }
</script>

<style lang="scss" scoped>
    .code-highlight-block {
        background: #232D46;
    }
</style>
