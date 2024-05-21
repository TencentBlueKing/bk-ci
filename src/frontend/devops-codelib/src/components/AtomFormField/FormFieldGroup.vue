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
            },
            docs: {
                type: String,
                default: ''
            },
            docsLink: {
                type: String,
                default: ''
            },
            disabled: {
                type: Boolean,
                default: false
            }
        },
        methods: {
            handleChangeSwitch (val) {
                this.handleChange(this.name, val)
            },
            handleToDocs () {
                window.open(this.docsLink, '_blank')
            }
        },
        render (h) {
            const { $slots, label, desc, value, topDivider, showSwitch, handleChangeSwitch, docsLink, docs, handleToDocs, disabled } = this
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
                        showSwitch && <bk-switcher value={value} theme="primary" disabled={disabled} onChange={handleChangeSwitch}></bk-switcher>
                    }
                    {
                        docsLink && <span class="bk-form-group-docs-link" onClick={handleToDocs}>
                            <logo name="tiaozhuan" size="14" style="fill:#3c96ff;position:relative;top:2px;right:2px;" />
                            {docs}
                        </span>
                    }
                    {
                        ((showSwitch && value) || !showSwitch) && <div class="bk-form-group-content">
                            {$slots.default}
                        </div>
                    }
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
    .bk-form-group-docs-link {
        float: right;
        line-height: 32px;
        cursor: pointer;
        color: #3c96ff;
    }
    .top-border-divider {
        height: 1px;
        width: 100%;
        margin: 24px 0;
        border-top: 1px solid #DCDEE5;
    }
</style>
