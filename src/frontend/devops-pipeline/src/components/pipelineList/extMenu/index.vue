<template>
    <bk-popover
        :disabled="config.length === 0"
        placement="bottom"
        ref="dotMenuRef"
        theme="dot-menu light"
        trigger="click"
        class="pipeline-ext-menu"
        :tippy-options="{
            arrow: false,
            placement: 'bottom-end'
        }"
    >
        <div :class="`dot-menu-trigger ${extCls}`">
            <i class="devops-icon icon-more"></i>
        </div>
        <ul v-if="config.length > 0" class="dot-menu-list" slot="content">
            <li
                :class="[{ 'is-disable': item.disable }, 'dot-menu-item']"
                v-for="(item, index) of config"
                v-bk-tooltips="getTooltips(item)"
                :key="index"
                @click.stop="clickMenuItem(item)">
                {{ item.text }}
            </li>
        </ul>
    </bk-popover>

</template>

<script>
    export default {
        props: {
            data: {
                type: Object,
                default: () => ({})
            },
            config: {
                type: Array,
                default: () => []
            },
            extCls: {
                type: String,
                default: ''
            }
        },
        methods: {
            getTooltips (item) {
                return {
                    content: item?.tooltips,
                    disabled: !item?.tooltips,
                    allowHTML: false
                }
            },
            clickMenuItem (item) {
                if (item.disable) return

                if (item.isJumpToTem) {
                    this.$refs.dotMenuRef.hideHandler()
                    item.handler(this.config.templateId)
                    return
                }

                this.$refs.dotMenuRef.hideHandler()
                console.log(this.data)
                item.handler(this.data, item)
            }
        }
    }
</script>

<style lang="scss">
     @import './../../../scss/conf';

    .pipeline-ext-menu {
        display: flex;
        .tippy-active .dot-menu-trigger {
            background: #EAEBF0;
        }
    }

    .ext-dot {
        width: 3px;
        height: 3px;
        border-radius: 50%;
        background-color: $fontWeightColor;
        & + .ext-dot {
            margin-top: 4px;
        }
    }

    .dot-menu {
        position: relative;
        padding: 0 4px;
        display: inline-flex;
        cursor: pointer;
        &:hover,
        &.active {
            .ext-dot {
                background-color: $primaryColor;
            }
        }
        > div {
            height: 100%;
        }
    }

    .task-card .dot-menu {
        height: 100%;
        border-left: 1px solid #dde4eb;
        &:hover{
            background-color: $bgHoverColor;
        }
    }

    .tippy-tooltip.dot-menu-theme {
        padding: 0;
    }
    .dot-menu-trigger {
        display: flex;
        align-items: center;
        justify-content: center;
        text-align: center;
        font-size: 22px;
        cursor: pointer;
        border-radius: 50%;
        &:hover {
            background: #EAEBF0;
            color: $primaryColor;
        }
    }

    .dot-menu-list {
        margin: 0;
        padding: 0;
        min-width: 100px;
        list-style: none;
        border: 1px solid $borderWeightColor;
        border-radius: 2px;
        box-shadow: 0 3px 6px rgba(0, 0, 0, .1);
        z-index: 3;
        .dot-menu-item {
            display: block;
            width: 100%;
            height: 32px;
            line-height: 32px;
            font-size: 12px;
            padding: 0 20px;
            color: $fontWeightColor;
            background-color: #fff;
            cursor: pointer;
            &:hover {
                background-color: $primaryLightColor;
                color: $primaryColor;
            }
        }
        .is-disable {
            cursor: not-allowed
        }
    }
</style>
