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

module.exports = {
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
            $ace: null,
            isLoading: false
        }
    },
    watch: {
        value (newVal) {
            const content = this.$ace.getValue()
            // 如果值没有变化无须重新设置，重新设置会导致焦点跳到最后
            newVal !== content && this.$ace.setValue(newVal, 1)
        },
        lang (newVal) {
            if (newVal) {
                require(`brace/mode/${newVal}`)
                this.$ace.getSession().setMode(`ace/mode/${newVal}`)
            }
        },
        fullScreen () {
            this.$el.classList.toggle('ace-full-screen')
            this.$nextTick(() => {
                this.$ace.resize()
            })
        }
    },
    methods: {
        calcSize (size) {
            const _size = size.toString()

            if (_size.match(/^\d*$/)) return `${size}px`
            if (_size.match(/^[0-9]?%$/)) return _size

            return '100%'
        }
    },
    mounted () {
        this.isLoading = true
        import(
            /* webpackChunkName: "brace" */
            'brace'
        ).then(ace => {
            this.isLoading = false
            this.$ace = ace.edit(this.$el)
            this.$ace.$blockScrolling = Infinity

            const { $ace, lang = 'javascript', theme = 'monokai', readOnly } = this
            const session = $ace.getSession()

            this.$emit('init', $ace)
            session.setNewLineMode('unix')

            require(`brace/mode/${lang}`)
            require(`brace/theme/${theme}`)

            session.setMode(`ace/mode/${lang}`) // 配置语言
            $ace.setTheme(`ace/theme/${theme}`) // 配置主题
            session.setUseWrapMode(true) // 自动换行
            $ace.setValue(this.value, 1) // 设置默认内容
            $ace.setReadOnly(readOnly) // 设置是否为只读模式
            $ace.setShowPrintMargin(false) // 不显示打印边距

            // 绑定输入事件回调
            $ace.on('change', ($editor, $fn) => {
                const content = $ace.getValue()

                this.$emit('update:hasError', !content)
                this.$emit('input', content, $editor, $fn)
            })
        })
    },
    render () {
        const { isLoading, width, height, calcSize } = this
        return (
            <div v-bkloading={{ isLoading }} style={{ 'height': calcSize(height), 'width': calcSize(width) }}></div>
        )
    }
}
