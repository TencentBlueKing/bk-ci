<template>
    <bk-crontab
        v-model="cronValue"
        :local="curLocal"
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
        computed: {
            cronValue () {
                // 处理 value 可能是字符串（错误标记）或数组的情况
                if (typeof this.value === 'string') {
                    return ''
                }
                return Array.isArray(this.value) ? this.value.join('') : ''
            },
            curLocal () {
                const localeAliasMap = {
                    'zh-CN': 'zh-CN',
                    'ja-JP': 'en',
                    'en-US': 'en'
                }
                return localeAliasMap[this.$i18n.locale] || 'zh-CN'
            },
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
                // 更新值为正常的数组格式
                this.handleChange(this.name, !!value ? [value] : [])
                // 值变化时触发校验
                this.$nextTick(() => {
                    if (this.$parent && this.$parent.$validator) {
                        this.$parent.$validator.validate(this.name)
                    }
                })
            },
            handleError () {
                // 使用字符串 '[]' 来标记组件有错误，crontabArrayRule 识别为空数组
                this.handleChange(this.name, '[]')
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
