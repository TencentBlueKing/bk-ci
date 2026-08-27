<template>
    <bk-user-selector
        :model-value="value"
        :api-base-url="apiBaseUrl"
        :tenant-id="tenantId"
        :multiple="multiple"
        @change="handleUserChange"
    />
</template>

<script>
    import BkUserSelector from '@blueking/bk-user-selector/vue2'
    import '@blueking/bk-user-selector/vue2/vue2.css'
    
    export default {
        components: {
            BkUserSelector
        },
        props: {
            multiple: {
                type: Boolean,
                default: false
            },
            value: {
                type: [String, Array],
            }
        },
        computed: {
            apiBaseUrl () {
                return this.$tenantApiBaseUrl || undefined
            },
            tenantId () {
                return this.$tenantId || undefined
            }
        },
        created () {
            console.log(this.value)
        },
        methods: {
            handleUserChange (user) {
                this.$emit('input', user)
                this.$emit('update:value', user)
            }
        }
    }
</script>
