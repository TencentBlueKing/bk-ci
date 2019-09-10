<script>
    import atomFieldMixin from '../atomFieldMixin'
    export default {
        name: 'vuex-textarea',
        mixins: [atomFieldMixin],
        methods: {
            handleInput (e, isBlur = false) {
                const { value, name } = e.target
                const trimVal = isBlur ? value.trim() : value

                if (trimVal !== this.value) {
                    this.$emit('input', trimVal)
                    this.handleChange(name, trimVal)
                }
            },
            handleBlur (e) {
                this.handleInput(e, true)
            }
        },
        render (h) {
            const { value, disabled, handleInput, name, handleBlur } = this
            return (
                <textarea onBlur={handleBlur} onInput={handleInput} class='bk-form-textarea' name={name} disabled={disabled} value={value} />
            )
        }
    }
</script>

<style lang="scss" scoped>
    .bk-form-textarea {
        resize: vertical;
    }
</style>
