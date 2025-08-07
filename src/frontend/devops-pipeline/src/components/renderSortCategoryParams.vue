<template>
    <section class="render-sort-category-params">
        <details open>
            <summary class="category-collapse-trigger">
                <div class="header">
                    {{ name }}
                    <span
                        v-if="showFollowTemplateBtn"
                        :class="['icon-item', {
                            'is-follow': isFollowTemplate,
                            'disabled': checkStepId ? !stepId : false
                        }]"
                        v-bk-tooltips="{
                            content: getTooltipMessage,
                            width: 320
                        }"
                        @click.stop="handleChangeFollow"
                    >
                        <logo
                            name="template-mode"
                            size="12"
                        />
                    </span>
                    <span
                        v-if="showSetRequiredBtn"
                        :class="['icon-item', {
                            'active': isRequiredParam
                        }]"
                        v-bk-tooltips=" isRequiredParam ? $t('template.cancelParticipant') : $t('template.setParticipant')"
                        @click="handelChangeBuildNoRequired"
                    >
                        <Logo
                            :name="isRequiredParam ? 'set-param-active' : 'set-param-default'"
                            size="12"
                        />
                    </span>
                    <span
                        v-if="statusTagConfig.message"
                        :class="['status-tag ml10', statusTagConfig.theme]"
                    >
                        {{ statusTagConfig.message }}
                    </span>
                </div>
                <i class="devops-icon icon-angle-down"></i>
            </summary>
            <div
                :class="['collapse-content', {
                    'default': defaultLayout
                }]"
            >
                <slot name="content" />
            </div>
        </details>
    </section>
</template>

<script>
    import Logo from '@/components/Logo'
    export default {
        name: 'RenderSortCategoryParams',
        components: {
            Logo
        },
        props: {
            followTemplateKey: {
                type: String,
                default: ''
            },
            name: {
                type: String,
                default: ''
            },
            defaultLayout: {
                type: Boolean,
                default: false
            },
            isFollowTemplate: {
                type: Boolean,
                default: false
            },
            isRequiredParam: {
                type: Boolean,
                default: false
            },
            showFollowTemplateBtn: {
                type: Boolean,
                default: false
            },
            showSetRequiredBtn: {
                type: Boolean,
                default: false
            },
            handleFollowTemplate: {
                type: Function,
                default: () => () => {}
            },
            handleSetBuildNoRequired: {
                type: Function,
                default: () => () => {}
            },
            checkStepId: {
                type: Boolean,
                default: false
            },
            stepId: {
                type: String,
                default: ''
            },
            isNew: {
                type: Boolean,
                default: false
            },
            isDelete: {
                type: Boolean,
                default: false
            },
            isChange: {
                type: Boolean,
                default: false
            }
        },
        computed: {
            getTooltipMessage () {
                if (this.checkStepId) {
                    if (this.stepId) {
                        return this.isFollowTemplate
                            ? this.$t('template.notFollowTemplateTips')
                            : this.$t('template.followTemplateTips')
                    } else {
                        return this.$t('template.notStepIdTips', [this.name])
                    }
                } else {
                    return this.isFollowTemplate
                        ? this.$t('template.notFollowTemplateTips')
                        : this.$t('template.followTemplateTips')
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
        methods: {
            handleChangeFollow (event) {
                event.preventDefault()
                if (this.checkStepId && !this.stepId) return
                this.handleFollowTemplate(this.followTemplateKey)
            },
            handelChangeBuildNoRequired (event) {
                event.preventDefault()
                this.handleSetBuildNoRequired()
            }
        }
    }
</script>

<style lang="scss" scoped>
    .render-sort-category-params {
        margin-bottom: 20px;
        &:last-child {
            margin-bottom: 0px !important;
        }
    }
    .category-collapse-trigger {
        .header {
            display: flex;
            align-items: center;
        }
        display: flex;
        justify-content: space-between;
        align-items: center;
        cursor: pointer;
        height: 28px;
        background: #EAEBF0;
        border-radius: 2px;
        padding: 0 16px;
        font-size: 12px;
        color: #4D4F56;
        font-weight: 700;
        .icon-angle-down {
            font-size: 10px;
        }
    }

    .collapse-content {
        padding: 10px 16px 0;
        display: grid;
        grid-template-columns: repeat(2, minmax(200px, 1fr));
        grid-gap: 0 24px;
        &.default {
            display: block;
        }
    }

    details:not([open]) .collapse-content {
        display: none;
    }

    details:not([open]) .category-collapse-trigger .icon-angle-down {
        transform: rotate(-90deg);
    }
    .icon-item {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        width: 22px;
        height: 22px;
        border-radius: 2px;
        background: #EAEBF0;
        color: #4D4F56;
        cursor: pointer;
        margin-left: 10px;
        z-index: 100;
        &.is-follow {
            background: #CDDFFE;
            color: #3A84FF;
        }
        &.active {
            background: #CDDFFE;
        }
        &.disabled {
            cursor: not-allowed;
        }
    }
</style>
