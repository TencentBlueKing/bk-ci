<script>
    import atomFieldMixin from '../atomFieldMixin'
    export default {
        name: 'vuex-textarea',
        mixins: [atomFieldMixin],
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
            const { value, readOnly, handleInput, name, handleBlur, title, clickUnfold, placeholder } = this
            return (
                <textarea placeholder={placeholder} title={title} onBlur={handleBlur} onInput={handleInput} class={['bk-form-textarea pointer-events-auto', clickUnfold ? 'textarea-styles' : '', readOnly ? 'hover-textarea-styles' : '']} name={name} disabled={readOnly} value={value} />
            )
        }
    }
</script>

<style lang="scss" scoped>
    .bk-form-textarea {
        resize: vertical;
    }
    .textarea-styles {
        position: absolute;
        resize: none;
        min-height: 32px;
        line-height: 20px !important;
        margin-top: 1px;
        &:focus {
            height: 100px!important;
            z-index: 10;
        }
    }
    .hover-textarea-styles {
        &:hover {
            top: 0;
            height: 100px!important;
            z-index: 10;
        }
    }
</style>
