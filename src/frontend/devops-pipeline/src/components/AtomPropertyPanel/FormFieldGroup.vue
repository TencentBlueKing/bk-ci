<script>
    export default {
        name: 'form-field-group',
        props: {
            label: {
                type: String,
                default: ''
            },
            topDivider: {
                type: Boolean,
                default: false
            },
            desc: {
                type: String,
                default: ''
            },
            name: {
                type: String
            },
            value: {
                type: Boolean,
                default: false
            },
            showSwitch: {
                type: Boolean,
                default: false
            },
            handleChange: {
                type: Function
            }
        },
        methods: {
            handleChangeSwitch (val) {
                this.handleChange(this.name, val)
            }
        },
        render (h) {
            const { $slots, label, desc, value, topDivider, showSwitch, handleChangeSwitch } = this
            return (
                <div class="form-field bk-form-item">
                    {
                        topDivider && <div class="top-border-divider"></div>
                    }
                    {
                        label && <div class="bk-form-group-label">{label}：</div>
                    }
                    {
                        label && desc.trim() && <bk-popover placement="top">
                            <i class="bk-icon icon-info-circle"></i>
                            <div slot="content" style="white-space: pre-wrap; font-size: 12px; max-width: 500px;">
                                <div> {desc} </div>
                            </div>
                        </bk-popover>
                    }
                    {
                        showSwitch && <span class="bk-form-group-label-divider"></span>
                    }
                    {
                        showSwitch && <bk-switcher value={value} theme="primary" onChange={handleChangeSwitch}></bk-switcher>
                    }
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
        display: inline-block;
        min-height: 32px;
        line-height: 32px;
    }
    .bk-form-group-content {
        padding: 15px 20px 15px;
        background-color: #f5f7fa;
    }
    .bk-form-group-label-divider {
        display: inline-block;
        border-left: 1px solid #DCDEE5;
        width: 1px;
        height: 20px;
        line-height: 20px;
        position: relative;
        top: 6px;
        margin: 0 10px
    }
    .top-border-divider {
        height: 1px;
        width: 100%;
        margin: 24px 0;
        border-top: 1px solid #DCDEE5;
    }
</style>
