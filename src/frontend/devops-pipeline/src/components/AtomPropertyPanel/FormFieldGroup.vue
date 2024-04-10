
<script>
    export default {
        name: 'form-field-group',
        props: {
            label: {
                type: String,
                default: ''
            }
        },
        render (h) {
            const { $slots, label } = this
            return (
                <div class="form-field bk-form-item">
                    <div class="bk-form-group-label">{label}：</div>
                    <div class="bk-form-group-content">
                        {$slots.default}
                    </div>
                </div>
            )
        }
    }
</script>

<style lang="scss">
    .bk-form-group-label {
        min-height: 32px;
        line-height: 32px;
    }
    .bk-form-group-content {
        padding: 15px 20px 15px;
        background-color: #f5f7fa;
    }
</style>
