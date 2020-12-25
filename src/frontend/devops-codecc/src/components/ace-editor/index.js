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
                import(`brace/mode/${newVal}`).then(() => {
                    this.$ace.getSession().setMode(`ace/mode/${newVal}`)
                })
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

            const {
                $ace,
                readOnly
            } = this
            let {
                lang,
                theme
            } = this
            const session = $ace.getSession()
            lang = lang || 'javascript'
            theme = theme || 'monokai'

            this.$emit('init', $ace)
            session.setNewLineMode('unix')

            Promise.all([import(`brace/mode/${lang}`), import(`brace/theme/${theme}`)]).then(() => {
                session.setMode(`ace/mode/${lang}`) // 配置语言
                $ace.setTheme(`ace/theme/${theme}`) // 配置主题
            })

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
