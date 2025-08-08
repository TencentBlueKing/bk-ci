<template>
    <div>
        <bk-checkbox-group v-model="noticeTypeList">
            <bk-checkbox
                v-for="(col, index) in noticeTypeMap"
                :key="index"
                :value="col.value"
                :disabled="disabled"
                style="margin-right: 20px;"
            >
                {{ col.name }}
            </bk-checkbox>
        </bk-checkbox-group>
    </div>
</template>

<script>
    export default {
        props: {
            value: {
                type: Array,
                default: () => []
            },
            handleChange: {
                type: Function,
                default: () => () => {}
            },
            disabled: {
                type: Boolean,
                default: false
            }
        },
        data () {
            return {
                noticeTypeMap: [
                    { name: '企业微信', value: 'RTX', isChecked: true },
                    { name: '邮件', value: 'EMAIL', isChecked: true },
                    { name: '微信', value: 'WECHAT', isChecked: true }
                ],
                noticeTypeList: ['RTX', 'EMAIL', 'WECHAT']
            }
        },
        watch: {
            noticeTypeList: {
                handler (newV) {
                    this.handleChange('notifyType', newV)
                },
                immediate: true
            }
        },
        created () {
            if (this.value.length) this.noticeTypeList = this.value
        }
    }
</script>

<style lang="scss">
    
</style>
