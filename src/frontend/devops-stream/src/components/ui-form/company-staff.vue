<template>
    <bk-member-selector
        v-model="copyValue"
        :style="{ width: '100%' }"
        :disabled="disabled"
        :placeholder="placeholder"
        api="https://api.open.woa.com/api/c/compapi/v2/usermanage/fs_list_users/"
        @change="handleChoose"
    ></bk-member-selector>
</template>

<script>
    export default {
        name: 'CompanyStaff',
        props: {
            value: {
                type: Array,
                required: true,
                default: () => []
            },
            disabled: {
                type: Boolean,
                default: false
            },
            placeholder: {
                type: String,
                default: ''
            }
        },
        data () {
            return {
                copyValue: []
            }
        },
        created () {
            this.copyValue = [...this.value]
        },
        methods: {
            handleChoose (val) {
                this.$emit('input', val)
                this.$emit('change', val)
            }
        }
    }
</script>
