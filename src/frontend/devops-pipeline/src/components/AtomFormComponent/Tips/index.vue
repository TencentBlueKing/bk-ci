<template>
    <h3 class="component-tip pointer-events-auto">
        <span class="tip-icon">
            <i class="devops-icon icon-info-circle-shape"></i>
        </span>
        <span class="tip-message" v-html="tip"></span>
    </h3>
</template>

<script>
    import mixins from '../mixins'

    export default {
        name: 'date-picker',
        mixins: [mixins],
        props: {
            tipStr: {
                type: String
            }
        },

        data () {
            return {
                list: [],
                tip: ''
            }
        },

        computed: {
            paramValues () {
                const { atomValue = {}, $route: { params = {} } } = this
                return { ...params, ...atomValue }
            }
        },

        watch: {
            paramValues: {
                handler (value, oldValue) {
                    const index = this.list.findIndex((key) => value[key] !== oldValue[key])
                    if (index > -1) {
                        this.initData()
                    }
                },
                deep: true
            }
        },

        created () {
            this.initData()
        },

        methods: {
            initData () {
                const str = this.tipStr.replace(/\[([^\]]+)\]\(([^\)]+)\)/gim, (str, key, value) => {
                    const isSafe = /^https?\:\/\//i.test(value)
                    let res = ''
                    if (isSafe) res = `<a class="text-link" href="${value}" target="_Blank">${key}</a>`
                    else res = `<a class="text-bad-link text-link" title="$t('editPage.badLink')">${key}</a>`
                    return res
                })

                this.list = []
                this.tip = str.replace(/{([^\{\}]+)}/gim, (str, key) => {
                    this.list.push(key)
                    const val = this.formatter(this.paramValues[key])
                    return this.escapeHtml(val)
                })
            },

            formatter (data) {
                let res = ''
                const type = typeof data
                switch (type) {
                    case 'object':
                        res = JSON.stringify(data)
                        break
                    case 'boolean':
                        res = data ? this.$t('true') : this.$t('false')
                        break
                    case 'number':
                        res = Number.isNaN(data) ? '' : String(data)
                        break
                    case 'string':
                        res = data
                        break
                    default:
                        res = ''
                        break
                }
                return res
            },

            escapeHtml (val) {
                return val.replace(/[&<>"\'\/]/g, (str) => {
                    let res = ''
                    switch (str) {
                        case '&':
                            res = '&amp;'
                            break
                        case '<':
                            res = '&lt;'
                            break
                        case '>':
                            res = '&gt;'
                            break
                        case '"':
                            res = '&quot;'
                            break
                        case '\'':
                            res = '&#x27;'
                            break
                        case '/':
                            res = '&#x2F;'
                            break
                    }
                    return res
                })
            }
        }
    }
</script>

<style lang="scss" scope>
    .component-tip {
        font-weight: normal;
        font-size: 12px;
        display: flex;
        border: 1px solid #e6e6e6;
        .tip-icon {
            min-width: 44px;
            background-color: #ffb400;
            position: relative;
            .devops-icon {
                font-size: 18px;
                color: #fff;
                display: inline-block;
                position: absolute;
                top: 50%;
                left: 50%;
                transform: translate3d(-50%, -50%, 0);
            }
        }
        .tip-message {
            flex: 1;
            padding: 10px 18px;
            line-height: 22px;
            word-break: break-all;
            .text-link {
                font-size: 12px;
                margin: 0;
            }
            .text-bad-link {
                cursor: not-allowed;
            }
        }
    }
</style>
