<script>
    import atomFieldMixin from '../atomFieldMixin'
    export default {
        name: 'vuex-textarea',
        mixins: [atomFieldMixin],
        data () {
            return {
                title: '',
                inputDisabled: false
            }
        },
        mounted () {
            const ele = document.querySelector('.atom-form-box') || {}
            if (ele.classList.contains('readonly') || this.disabled) {
                this.title = this.value
                this.inputDisabled = true
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
            const { value, inputDisabled, handleInput, name, handleBlur, title } = this
            return (
                <textarea title={title} onBlur={handleBlur} onInput={handleInput} class='bk-form-textarea pointer-events-auto' name={name} disabled={inputDisabled} value={value} />
            )
        }
    }
</script>

<style lang="scss" scoped>
    .bk-form-textarea {
        resize: vertical;
    }
</style>
