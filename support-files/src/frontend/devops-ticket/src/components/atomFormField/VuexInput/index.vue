<script>
    import atomFieldMixin from '../atomFieldMixin'
    export default {
        name: 'vuex-input',
        mixins: [atomFieldMixin],
        props: {
            value: [String, Number],
            type: {
                type: String,
                default: 'text'
            },
            isError: {
                type: Boolean,
                default: false
            }
        },
        methods: {
            handleInput (e, isBlur = false) {
                const { value, name } = e.target
                const trimVal = isBlur ? value.trim() : value

                this.$emit('input', trimVal)
                this.handleChange(name, trimVal)
            },
            handleBlur (e) {
                this.handleInput(e, true)
            }
        },
        render (h) {
            const { type, value, name, handleInput, disabled, handleBlur } = this
            
            return (
                <input disabled={disabled} type={type} class='bk-form-input' name={name} value={value} onBlur={handleBlur} onInput={handleInput} />
            )
        }
    }
</script>
