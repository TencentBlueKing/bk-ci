<template>
    <bk-crontab
        v-model="cronValue"
        :local="curLocal"
        @change="handleChangeCron"
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
                return this.value.join('')
            },
            curLocal () {
                const localeAliasMap = {
                    'zh-CN': 'zh-CN',
                    'ja-JP': 'en',
                    'en-US': 'en'
                }
                return localeAliasMap[this.$i18n.locale] || 'zh-CN'
            }
        },
        methods: {
            handleChangeCron (value) {
                this.handleChange(this.name, [value])
            }
        }
    }
</script>
