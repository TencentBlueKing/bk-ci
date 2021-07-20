<template>
    
    <bk-popover class="dot-menu" placement="right" ref="dotMenuRef" theme="dot-menu light" trigger="mouseenter" :arrow="false" offset="15" :distance="0">
        <div v-if="config.extMenu.length" class="dot-menu-trigger">
            <div class="footer-ext-dots">
                <div class="ext-dot"></div>
                <div class="ext-dot"></div>
                <div class="ext-dot"></div>
            </div>
        </div>
        <ul class="dot-menu-list" slot="content">
            <li :class="[{ 'is-disable': item.disable }, 'dot-menu-item']" v-for="(item, index) of config.extMenu" :key="index" @click.stop="clickMenuItem(item)">{{ item.text }}</li>
        </ul>
    </bk-popover>
    
</template>

<script>
    export default {
        props: {
            config: {
                type: Object,
                default () {
                    return {
                        extMenu: []
                    }
                }
            }
        },
        methods: {
            clickMenuItem (item) {
                if (item.disable) return

                if (item.isJumpToTem) {
                    this.$refs.dotMenuRef.hideHandler()
                    item.handler(this.config.templateId)
                    return
                }

                this.$refs.dotMenuRef.hideHandler()
                item.handler(this.config.pipelineId)
            }
        }
    }
</script>

<style lang="scss">
     @import './../../../scss/conf';
    
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
        width: 23px;
        height: 30px;
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
        width: 23px;
        height: 100%;
        text-align: center;
        font-size: 0;
        cursor: pointer;
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
            height: 42px;
            line-height: 42px;
            text-align: center;
            font-size: 14px;
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
