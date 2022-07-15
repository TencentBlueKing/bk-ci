<template>
    <bk-user-selector
        v-model="copyValue"
        :style="{ width: '100%' }"
        :disabled="disabled"
        :placeholder="placeholder"
        key="company-staff"
        api="https://api.open.woa.com/api/c/compapi/v2/usermanage/fs_list_users/"
        @change="handleChoose"
    ></bk-user-selector>
</template>

<script>
    import BkUserSelector from '@blueking/user-selector'

    export default {
        name: 'CompanyStaff',
        components: {
            BkUserSelector
        },
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
