<script>
    import atomFieldMixin from '../atomFieldMixin'
    export default {
        name: 'vuex-input',
        mixins: [atomFieldMixin],
        props: {
            value: [String, Number],
            inputType: {
                type: String,
                default: 'text'
            },
            isError: {
                type: Boolean,
                default: false
            },
            placeholder: {
                type: String,
                default: ''
            }
        },
        methods: {
            handleInput (e, isBlur = false) {
                const { value, name } = e.target
                const trimVal = isBlur ? value.trim() : value

                if (trimVal !== String(this.value).trim() || trimVal !== '') {
                    this.$emit('input', trimVal)
                    this.handleChange(name, trimVal)
                }
            },
            handleBlur (e) {
                this.handleInput(e, true)
            }
        },
        render (h) {
            const { inputType, value, name, handleInput, readOnly, handleBlur, title, placeholder } = this

            return (
                <input placeholder={placeholder} title={title} disabled={readOnly} type={inputType} class='bk-form-input pointer-events-auto' name={name} value={value} onBlur={handleBlur} onInput={handleInput} />
            )
        }
    }
</script>
