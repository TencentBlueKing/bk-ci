<template>
    <section class="render-sort-category-params">
        <details open>
            <summary class="category-collapse-trigger">
                <div>
                    {{ name }}
                    <span
                        v-if="showFollowTemplateBtn"
                        :class="['icon-item', {
                            'active': isFollowTemplate
                        }]"
                        v-bk-tooltips="{
                            content: isFollowTemplate ? $t('template.notFollowTemplateTips') : $t('template.followTemplateTips'),
                            width: 320
                        }"
                        @click.stop="handleChangeFollow"
                    >
                        <Logo
                            :name="isFollowTemplate ? 'template-mode-color' : 'template-mode'"
                            size="14"
                        />
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
            displayKey: {
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
                default: true
            },
            showFollowTemplateBtn: {
                type: Boolean,
                default: false
            },
            handleFollowTemplate: {
                type: Function,
                default: () => () => {}
            }
        },
        methods: {
            handleChangeFollow (event) {
                event.preventDefault()
                this.handleFollowTemplate(this.displayKey)
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
        position: relative;
        align-items: center;
        display: inline-block;
        width: 20px;
        height: 20px;
        background: #EAEBF0;
        border-radius: 2px;
        margin-left: 6px;
        cursor: pointer;
        z-index: 100;
        &.active {
            background: #CDDFFE;
        }
    }
</style>
