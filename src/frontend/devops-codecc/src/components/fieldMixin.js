const fieldMixin = {
    props: {
        name: {
            type: String,
            required: true
        },
        value: {
            type: String,
            required: true,
            default: ''
        },
        disabled: {
            type: Boolean,
            default: false
        },
        handleChange: {
            type: Function,
            default: () => () => {}
        },
        element: {
            type: Object,
            default: () => ({})
        },
        container: {
            type: Object,
            default: () => ({})
        },
        rule: {
            type: Object,
            default: () => ({})
        },
        component: String,
        required: Boolean,
        hasError: {
            type: Boolean
        },
        hidden: {
            type: Boolean,
            default: false
        }
    },
    watch: {
        value (value, oldValue) {
            value !== oldValue && this.$emit('input', value)
        }
    }
}

export default fieldMixin
