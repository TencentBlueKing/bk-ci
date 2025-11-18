<template>
    <bk-crontab
        v-model="cronValue"
        :language-package="languagePackage"
        @change="handleChangeCron"
        @error="handleError"
    />
</template>
<script>
    import BkCrontab from '@blueking/crontab/vue2'
    import '@blueking/crontab/vue2/vue2.css'
    export default {
        components: {
            BkCrontab
        },
        props: {
            value: {
                type: String,
                default: ['* * * * *']
            },
            name: {
                type: String,
                default: ''
            },
            handleChange: {
                type: Function,
                default: () => {}
            }
        },
        data () {
            return {
                hasInternalError: false,
                internalValue: '' // 保存组件内部的值（包括错误的值）
            }
        },
        computed: {
            cronValue: {
                get () {
                    if (this.hasInternalError) {
                        return this.internalValue
                    }
                    return Array.isArray(this.value) ? this.value.join('') : ''
                },
                set (val) {
                    this.internalValue = val
                }
            },
            // curLocal () {
            //     const localeAliasMap = {
            //         'zh-CN': 'zh-CN',
            //         'ja-JP': 'en',
            //         'en-US': 'en'
            //     }
            //     return localeAliasMap[this.$i18n.locale] || 'zh-CN'
            // },
            languagePackage () {
                return {
                    'en-US': {
                        '下次': 'Next',
                        '分': 'Minute',
                        '时': 'Hour',
                        '日': 'Day',
                        '月': 'Month',
                        '周': 'Week',
                        '快捷选项': 'Quick Select',
                        '每分钟': 'Every Minute',
                        '每小时': 'Every Hour',
                        '每天': 'Every Day',
                        '每周': 'Every Week',
                        '每月': 'Every Month'
                    },
                    'ja-jp': {
                        '下次': '次回',
                        '分': '分',
                        '时': '時',
                        '日': '日',
                        '月': '月',
                        '周': '週',
                        '快捷选项': 'クイックセレクト',
                        '每分钟': '毎分',
                        '每小时': '毎時',
                        '每天': '毎日',
                        '每周': '毎週',
                        '每月': '毎月'
                    }
                }
            }
        },
        methods: {
            handleChangeCron (value) {
                this.hasInternalError = false
                this.handleChange(this.name, !!value ? [value] : [])
                this.$nextTick(() => {
                    if (this.$parent && this.$parent.$validator) {
                        this.$parent.$validator.validate(this.name)
                    }
                })
            },
            handleError () {
                // 标记组件有格式错误，但不修改 value
                // 保持用户输入的错误值显示在界面上
                this.hasInternalError = true
                // 设置一个空数组触发校验失败
                this.handleChange(this.name, [])
                // 触发校验
                this.$nextTick(() => {
                    if (this.$parent && this.$parent.$validator) {
                        this.$parent.$validator.validate(this.name)
                    }
                })
            }
        }
    }
</script>
