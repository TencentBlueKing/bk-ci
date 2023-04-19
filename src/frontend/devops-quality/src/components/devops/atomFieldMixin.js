const atomFieldMixin = {
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
        hasError: {
            type: Boolean
        }
    },
    watch: {
        value (value, oldValue) {
            value !== oldValue && this.$emit('input', value)
        }
    }
}

export default atomFieldMixin
