<template>
    <section class="render-sort-category-params">
        <details open>
            <summary class="category-collapse-trigger">
                <div class="header">
                    {{ name }}
                    <ToggleRequiredParamPopover
                        v-if="showSetRequiredBtn && !isDelete"
                        is-collapsed
                        :is-required-param="isRequiredParam"
                        :handle-change="handelChangeRequired"
                    />
                    <ToggleFollowTemplatePopover
                        v-if="showFollowTemplateBtn && !isDelete"
                        is-collapsed
                        :is-follow-template="isFollowTemplate"
                        :handle-change="handleChangeFollow"
                        :type="configType"
                    />
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
    import ToggleRequiredParamPopover from '@/components/ToggleRequiredParamPopover.vue'
    import ToggleFollowTemplatePopover from '@/components/ToggleFollowTemplatePopover.vue'
    export default {
        name: 'RenderSortCategoryParams',
        components: {
            ToggleRequiredParamPopover,
            ToggleFollowTemplatePopover
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
            handleSetRequired: {
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
            },
            configType: {
                type: String,
                default: ''
            }
        },
        computed: {
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
                if (this.checkStepId && !this.stepId) return
                this.handleFollowTemplate(this.followTemplateKey)
            },
            handelChangeRequired (event) {
                this.handleSetRequired()
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
</style>
