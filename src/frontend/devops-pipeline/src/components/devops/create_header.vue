<template>
    <div class="create-header clearfix">
        <div class="create-header-left">
            <bk-button size="normal" theme="primary"
                :disabled="buttonDisabled"
                v-if="buttonType === 'button'"
                @click="toggleTemplatePopup">
                <i class="devops-icon icon-plus"></i>
                <span>{{ createText }}</span>
            </bk-button>

            <bk-dropdown-menu
                v-if="buttonType === 'menu'"
                :trigger="triggerType"
                :align="localDropdownMenuConfig.align || 'center'"
                @show="localDropdownMenuConfig.showHandler"
                @hide="!localDropdownMenuConfig.hideHandler">
                <bk-button theme="primary" icon="devops-icon icon-plus" slot="dropdown-trigger">
                    {{ createText }}
                </bk-button>
                <ul class="bk-dropdown-list" slot="dropdown-content">
                    <li v-for="(item, index) of localDropdownMenuConfig.list" :key="index">
                        <a href="javascript:;"
                            @click="item.handler(index)">
                            {{ item.text }}
                        </a>
                    </li>
                </ul>
            </bk-dropdown-menu>
        </div>

        <div class="create-header-right"
            v-if="$slots.addon">
            <!-- <slot name="pre-addon"></slot>
            <i class="devops-icon toggle-layout"
                :class="`icon-${localLayout.current === 'card' ? 'panel' : 'apps'}`"
                @click="layoutToggle">
            </i>
            <slot name="post-addon"></slot> -->
            <slot name="addon"></slot>
        </div>

    </div>
</template>

<script>
    export default {
        props: {
            createText: {
                type: String,
                default: ''
            },
            buttonDisabled: {
                type: Boolean,
                default: false
            },
            buttonType: {
                type: String,
                default: 'button'
            },
            triggerType: {
                type: String,
                default: 'mouseover'
            },
            dropdownMenuConfig: {
                type: Object,
                default () {
                    return {
                        showHandler: () => {},
                        hideHandler: () => {},
                        align: 'center',
                        list: []
                    }
                }
            }
        },
        data () {
            return {
                localDropdownMenuConfig: {
                    showHandler: () => {},
                    hideHandler: () => {},
                    align: 'center',
                    list: []
                }
            }
        },
        mounted () {
            this.localDropdownMenuConfig = Object.assign(this.localDropdownMenuConfig, this.dropdownMenuConfig)
        },
        methods: {
            /**
             *  点击新增按钮回调函数
             */
            toggleTemplatePopup (templatePopupShow) {
                this.$emit('createPipeline')
            }
        }
    }
</script>

<style lang="scss">
    .create-header {
        height: 36px;
        &-left {
            float: left;
            width: 50%;
            height: 100%;
        }
        &-right {
            float: right;
            width: 50%;
            height: 100%;
            text-align: right;
        }
    }
</style>
