<script>

    export default {
        name: 'form-field',
        props: {
            label: {
                type: String,
                default: ''
            },
            inline: {
                type: Boolean,
                default: false
            },
            required: {
                type: Boolean,
                default: false
            },
            isError: {
                type: Boolean,
                default: false
            },
            errorMsg: {
                type: String,
                default: ''
            },
            hideColon: {
                type: Boolean,
                default: false
            },
            desc: {
                type: String,
                default: ''
            },
            docsLink: {
                type: String,
                default: ''
            },
            descLink: {
                type: String,
                default: ''
            },
            descLinkText: {
                type: String,
                default: ''
            },
            type: {
                type: String
            },
            labelWidth: {
                type: Number
            },
            bottomDivider: {
                type: Boolean,
                default: false
            }
        },
        computed: {
            widthStyle () {
                if (!this.labelWidth) return {}
                return {
                    width: `${this.labelWidth}px`
                }
            }
        },
        render (h) {
            const { label, inline, required, $slots, isError, errorMsg, hideColon, desc, docsLink, descLink, descLinkText, type, widthStyle, bottomDivider } = this
            const descMap = desc.split('\n')
            return (
                <div class={{
                    'form-field': true,
                    'bk-form-item': !inline,
                    'form-field-group-item': type === 'groupItem',
                    'bk-form-inline-item': inline,
                    'is-required': required,
                    'is-danger': isError
                }} >
                    { label && <label title={label} class='bk-label atom-form-label' style={widthStyle}>{label}{hideColon ? '' : '：'}
                        { docsLink
                            && <a target="_blank" href={docsLink}><i class="bk-icon icon-question-circle"></i></a>
                        }
                        { label.trim() && desc.trim() && <bk-popover placement="top">
                            <i class={{ 'bk-icon': true, 'icon-info-circle': true }} style={{ 'margin-left': hideColon ? '4px' : '0', color: hideColon ? '#979BA5' : '' }}></i>
                            <div slot="content" style="white-space: pre-wrap; font-size: 12px; max-width: 500px;">
                                <div>
                                    {
                                        descMap.length > 1
                                        ? descMap.map(item => (
                                            <div>{item}</div>
                                        ))
                                        : desc
                                    }
                                    { descLink && <a class="desc-link" target="_blank" href={descLink}>{descLinkText}</a>}
                                </div>
                            </div>
                        </bk-popover>
                    }
                    </label> }
                    
                    <div class='bk-form-content'>
                        {$slots.default}
                        {isError ? $slots.errorTip || <p class='bk-form-help is-danger'>{errorMsg}</p> : null}
                    </div>
                    {
                        bottomDivider
                        ? (
                            <div class="bottom-border-divider"></div>
                        )
                        : undefined
                    }
                </div>
            )
        }
    }
</script>

<style lang="scss">
    .form-field {
        .icon-info-circle, .icon-question-circle {
            color: #C3CDD7;
            font-size: 14px;
            pointer-events: auto;
        }
    }
    .form-field-group-item {
        display: flex;
        align-items: center;
        line-height: 32px;
        margin-top: 16px !important;
        &:first-child {
            margin-top: 0px !important;
        }
        .atom-form-label {
            text-align: right !important;
            word-break: break-all;
            align-self: self-start;
        }
        .bk-form-content {
            flex: 1;
        }
    }
    .form-field.bk-form-item {
        position: relative;
    }
    .bk-form-item,
    .bk-form-inline-item {
        .bk-label {
            position: relative;
        }
    }
    .bk-form-vertical {
        .bk-form-item.is-required .bk-label,
        .bk-form-inline-item.is-required .bk-label {
            margin-right: 10px;
        }
    }
    .desc-link {
        color: #3c96ff;
    }

    .bottom-border-divider {
        height: 1px;
        width: 100%;
        margin: 24px 0 8px;
        border-bottom: 1px solid #DCDEE5;
    }
</style>
