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
            }
        },
        render (h) {
            const { label, inline, required, $slots, isError, errorMsg, desc, docsLink, descLink, descLinkText } = this
            return (
                <div class={{ 'form-field': true, 'bk-form-item': !inline, 'bk-form-inline-item': inline, 'is-required': required, 'is-danger': isError }} >
                    { label && <label title={label} class='bk-label atom-form-label'>{label}：
                        { docsLink
                            && <a target="_blank" href={docsLink}><i class="bk-icon icon-question-circle"></i></a>
                        }
                        { label.trim() && desc.trim() && <bk-popover placement="top">
                            <i class="bk-icon icon-info-circle"></i>
                            <div slot="content" style="white-space: pre-wrap; font-size: 12px; max-width: 500px;">
                                <div> {desc} { descLink && <a class="desc-link" target="_blank" href={descLink}>{descLinkText}</a>} </div>
                            </div>
                        </bk-popover>
                    }
                    </label> }

                    <div class='bk-form-content'>
                        {$slots.default}
                        {isError ? $slots.errorTip || <p class='bk-form-help is-danger'>{errorMsg}</p> : null}
                    </div>
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
            vertical-align: middle;
            pointer-events: auto;
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
</style>
