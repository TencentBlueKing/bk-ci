<template>
    <BkUserSelector
        :model-value="value"
        :api-base-url="apiBaseUrl"
        :tenant-id="tenantId"
        :placeholder="placeholder"
        :disabled="disabled"
        draggable
        :multiple="isMultiple"
        @change="handleUserChange"
    />
</template>

<script>
    import BkUserSelector from '@blueking/bk-user-selector/vue2'
    import '@blueking/bk-user-selector/vue2/vue2.css'
    import TenantSingleton from '../../../utils/tenant'
    import atomFieldMixin from '../atomFieldMixin'

    export default {
        name: 'user-input',
        components: {
            BkUserSelector
        },
        mixins: [atomFieldMixin],
        props: {
            name: {
                type: String,
                default: ''
            },
            value: {
                type: String,
                default: ''
            },
            placeholder: {
                type: String,
                default: ''
            },
            disabled: {
                type: Boolean,
                default: false
            }
        },
        computed: {
            apiBaseUrl () {
                return TenantSingleton.getInstance().apiBaseUrl
            },
            tenantId () {
                return TenantSingleton.getInstance().tenantId
            },
            isMultiple () {
                return Array.isArray(this.value)
            }
        },
        methods: {
            handleUserChange (user) {
                this.handleChange(this.name, user)
            }
        }
    }
</script>