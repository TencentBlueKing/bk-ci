<script>
    import Logo from '@/components/Logo'
    import NamingConventionTip from '@/components/namingConventionTip.vue'
    export default {
        name: 'form-field',
        components: {
            NamingConventionTip
        },
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
            },
            customDesc: {
                type: Boolean,
                default: false
            },
            showOperateBtn: {
                type: Boolean,
                default: false
            },
            isChange: {
                type: Boolean,
                default: false
            },
            isDelete: {
                type: Boolean,
                default: false
            },
            isNew: {
                type: Boolean,
                default: false
            },
            isRequiredParam: {
                // 是否为入参
                type: Boolean,
                default: false
            },
            handleUseDefaultValue: {
                type: Function,
                default: () => () => {}
            },
            handleSetParmaRequired: {
                type: Function,
                default: () => () => {}
            }
        },
        computed: {
            widthStyle () {
                if (!this.labelWidth) return {}
                return {
                    width: `${this.labelWidth}px`
                }
            },
            statusTagConfig () {
                let message, theme
                if (this.isDelete) {
                    message = this.$t('deleted')
                    theme = 'danger'
                }
                if (this.isNew) {
                    message = this.$t('new')
                    theme = 'success'
                }
                return {
                    message,
                    theme,
                    isShow: this.isDelete || this.isNew
                }
            }
        },
        render (h) {
            const {
                label, inline, required, $slots, isError, errorMsg, hideColon, desc, docsLink,
                descLink, descLinkText, type, widthStyle, bottomDivider, customDesc, showOperateBtn,
                statusTagConfig, isDelete, isChange, isRequiredParam, $t, handleSetParmaRequired, handleUseDefaultValue
            } = this
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
                    {
                        label && <label title={label} class='bk-label atom-form-label' style={widthStyle}>
                            {
                                <span class={{ deleted: isDelete }}>{label}</span>
                            }
                            { hideColon ? '' : '：' }
                            { docsLink
                                && <a target="_blank" href={docsLink}><i class="bk-icon icon-question-circle"></i></a>
                            }
                            { label.trim() && (desc.trim() || customDesc) && <bk-popover placement={customDesc ? 'top-start' : 'top'} theme={customDesc ? 'light' : 'dark'} width={customDesc ? 892 : 'auto'}>
                                    <i class={{ 'bk-icon': true, 'icon-info-circle': true }} style={{ 'margin-left': hideColon ? '4px' : '0', color: hideColon ? '#979BA5' : '' }}></i>
                                    <div slot="content">
                                        {
                                            customDesc
                                            ? <NamingConventionTip/>
                                            : <div style="white-space: pre-wrap; overflow-wrap: break-word; font-size: 12px; max-width: 500px;">
                                                {
                                                    descMap.length > 1
                                                    ? descMap.map(item => (
                                                        <div>{item}</div>
                                                    ))
                                                    : desc
                                                }
                                                { descLink && <a class="desc-link" target="_blank" href={descLink}>{descLinkText}</a>}
                                            </div>
                                        }

                                    </div>
                                </bk-popover>
                            }
                            {
                                statusTagConfig.isShow && <span class={['status-tag', statusTagConfig.theme]}>
                                    {statusTagConfig.message}
                                </span>
                            }
                        </label>
                    }

                    {
                        showOperateBtn && !isDelete && <span
                            class='operate-btn'
                        >
                            {
                                isChange && (
                                    <span class={['icon-item', {
                                            'show-dot': isChange
                                        }]}
                                        onClick={handleUseDefaultValue}
                                        v-bk-tooltips={$t('template.useDefaultValue')}
                                    >
                                        <Logo
                                            name="use-default"
                                            size="20"
                                        />
                                    </span>
                                )
                            }
                            <span class={['icon-item', {
                                    active: isRequiredParam
                                }]}
                                v-bk-tooltips={
                                    isRequiredParam ? $t('template.cancelParticipant') : $t('template.setParticipant')
                                }
                                onClick={handleSetParmaRequired}
                            >
                                <Logo
                                    name="set-param"
                                    size="14"
                                />
                            </span>
                        </span>
                    }

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
        &:hover {
            .operate-btn {
                visibility: visible;
            }
        }
        .icon-info-circle, .icon-question-circle {
            color: #C3CDD7;
            font-size: 14px;
            pointer-events: auto;
        }
        .atom-form-label {
            .deleted {
                color: #a7a9ac !important;
                text-decoration: line-through;
            }
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
    .status-tag {
        padding: 0 8px;
        border-radius: 2px;
        font-size: 12px;
        height: 16px;
        line-height: 16px;
        font-weight: 400;
        &.success {
            color: #299E56;
            background: #DAF6E5;
        }
        &.danger {
            color: #E71818;
            background: #FFEBEB;
        }
    }
    .operate-btn {
        display: flex;
        justify-content: end;
        align-items: center;
        visibility: hidden;
        height: 32px;
        .icon-item {
            position: relative;
            position: relative;
            display: flex;
            align-items: center;
            justify-content: space-around;
            width: 24px;
            height: 24px;
            background: #EAEBF0;
            border-radius: 2px;
            margin-left: 6px;
            cursor: pointer;
            &.active {
                background: #CDDFFE;
            }
        }
        .show-dot {
            &::after {
                content: '';
                position: absolute;
                top: -2px;
                right: -2px;
                width: 5px;
                height: 5px;
                background: red;
                border-radius: 50%;
            }
        }
    }
</style>
