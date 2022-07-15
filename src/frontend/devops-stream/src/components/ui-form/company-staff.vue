<template>
    <bk-user-selector
        v-model="value"
        :disabled="disabled"
        :placeholder="placeholder"
        class="filter-item"
        api="https://api.open.woa.com/api/c/compapi/v2/usermanage/fs_list_users/"
    ></bk-user-selector>
</template>

<script>
    import mixins from './mixins.js'

    export default {
        mixins: [mixins],
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
        }
    }
</script>
