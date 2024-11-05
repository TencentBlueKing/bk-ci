<template>
    <bk-popover
        :disabled="config.length === 0"
        placement="bottom"
        ref="dotMenuRef"
        theme="dot-menu light"
        trigger="click"
        class="template-ext-menu"
        :transfer="true"
        :tippy-options="{
            arrow: false,
            offset: '5, 1',
            placement: 'bottom'
        }"
        :on-show="handleShowMenu"
        :on-hide="handleHideMenu"
    >
        <div :class="`dot-menu-trigger ${extCls}`">
            <span
                :class="[{ 'has-show': hasShow }, 'show-more']"
            >
                {{ $t('more') }}
            </span>
        </div>
        <ul
            v-if="config.length > 0"
            class="dot-menu-list"
            slot="content"
        >
            <li
                v-perm="item.permissionData ? {
                    hasPermission: item.hasPermission,
                    disablePermissionApi: item.disablePermissionApi,
                    permissionData: item.permissionData
                } : {}"
                :class="[{ 'is-disable': item.disable, 'bk-permission-disable': !item.hasPermission }, 'dot-menu-item']"
                v-for="(item, index) of config"
                v-bk-tooltips="getTooltips(item)"
                :key="index"
                @click.stop="clickMenuItem(item)"
            >
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
        data () {
            return {
                hasShow: false
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

                this.$refs.dotMenuRef.hideHandler()
                item.handler(this.data, item)
            },
            handleShowMenu () {
                this.hasShow = true
            },
            handleHideMenu () {
                this.hasShow = false
            }
        }
    }
</script>

<style lang="scss">
     @import '../../scss/conf';

    .template-ext-menu {
        height: 40px;
        display: flex;
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
        padding: 0 !important;
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
            color: $primaryColor;
        }
    }
    .show-more {
        position: relative;
        padding-right: 20px;
        font-size: 12px;
        color: #7b7d8a;
        margin-left: 10px;
        &:after {
            content: '';
            position: absolute;
            top: 23px;
            right: 8px;
            height: 8px;
            width: 8px;
            border-right: 1px solid $fontColor;
            border-bottom: 1px solid $fontColor;
            transition: transform 200ms;
            transform: rotate(45deg);
            transform-origin: 6px 6px;
        }
    }
    
    .has-show:after {
        transform: rotate(225deg);
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
            text-align: center;
            cursor: pointer;
            &:hover {
                background-color: $primaryLightColor;
                color: $primaryColor;
            }
        }
        .is-disable {
            cursor: not-allowed;
            color: #c4c6cc;
            &:hover {
                color: #c4c6cc !important;
            }
        }
    }
</style>
