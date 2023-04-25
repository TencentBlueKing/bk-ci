<template>
    <bk-member-selector
        v-model="value"
        @change="onChange"
        :placeholder="placeholder"
        :disabled="disabled"
        :type="inputType"
    ></bk-member-selector>
</template>

<script>

    export default {
        name: 'company-staff-input',
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
            },
            inputType: {
                type: String,
                default: 'rtx'
            },
            handleChange: {
                type: Function,
                default: () => () => {}
            }
        },
        methods: {
            onChange (value) {
                this.handleChange(this.name, value)
            }
        }
    }
</script>
