<template>
    <bk-popover class="dot-menu" placement="right" theme="dot-menu light" trigger="mouseenter" :arrow="false" offset="15" :distance="0">
        <div class="dot-menu-trigger">
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
    <!-- <div class="footer-ext-item"
        v-if="config.extMenu.length"
        :class="{
            active: isShowExtMenu
        }"
        @mouseenter="showExtMenu()"
        @mouseleave="hideExtMenu()">
        <div class="footer-ext-dots">
            <div class="ext-dot"></div>
            <div class="ext-dot"></div>
            <div class="ext-dot"></div>
        </div>
        <div class="footer-ext-menu"
            v-show="isShowExtMenu">
            <bk-popover class="dot-menu" placement="bottom-start" theme="dot-menu light" trigger="click" :arrow="false" offset="15" :distance="0">
                <ul>
                    <li :class="[{ 'is-disable': item.disable }, 'ext-menu-item']" v-for="(item, index) of config.extMenu" :key="index" @click.stop="clickMenuItem(item)">{{ item.text }}</li>
                </ul>
            </bk-popover>
            <ul>
                <li :class="[{ 'is-disable': item.disable }, 'ext-menu-item']" v-for="(item, index) of config.extMenu" :key="index" @click.stop="clickMenuItem(item)">{{ item.text }}</li>
            </ul>
        </div>
    </div> -->
</template>

<script>
    export default {
        props: {
            config: {
                type: Object,
                default () {
                    return {
                        buildId: 0,
                        buttonAllow: {},
                        content: [],
                        customBtns: [],
                        extMenu: [],
                        footer: [],
                        isRunning: false,
                        name: '',
                        runningInfo: {
                            time: '0',
                            percentage: '0%',
                            log: '',
                            buildCount: 0
                        },
                        status: 'success'
                    }
                }
            }
        },
        data () {
            return {
                isShowExtMenu: false,
                extMenuTimer: '',
                enterMenuTimer: ''
            }
        },
        methods: {
            clickMenuItem (item) {
                if (item.disable) return

                this.isShowExtMenu = false
                item.handler(this.config.pipelineId)
            }
            /**
             *  鼠标指向ext菜单触发器
             */
            // showExtMenu () {
            //     clearTimeout(this.extMenuTimer)

            //     if (this.isShowExtMenu) return

            //     this.enterMenuTimer = setTimeout(() => {
            //         const menu = this.$el.querySelector('.footer-ext-menu')
            //         const extDot = this.$el.querySelector('.ext-dot')
            //         const { clientWidth, clientHeight } = document.body
            //         this.isShowExtMenu = true
                    
            //         this.$nextTick(() => {
            //             const { bottom, right } = extDot.getBoundingClientRect()
            //             const { width, height } = menu.getBoundingClientRect()
            //             console.log(clientWidth, right, width)
            //             if (clientWidth - right - 22 < width) {
            //                 menu.style.right = '22px'
            //             } else {
            //                 menu.style.right = -width + 'px'
            //             }
            //             if (clientHeight - bottom < height) {
            //                 menu.style.top = -height + 'px'
            //             } else {
            //                 menu.style.top = '0'
            //             }
            //         })
            //     }, 200)
            // },
            // /**
            //  *  鼠标离开ext菜单触发器
            //  */
            // hideExtMenu () {
            //     clearTimeout(this.enterMenuTimer)
            //     this.extMenuTimer = setTimeout(() => {
            //         this.isShowExtMenu = false
            //         const menu = this.$el.querySelector('.footer-ext-menu')
            //         menu.style.top = '0'
            //     }, 200)
            // }
        }
    }
</script>

<style lang="scss">
     @import './../../../scss/conf';

    // .footer-ext-item {
    //     position: relative;
    //     width: 23px;
    //     height: 30px;
    //     cursor: pointer;
    //     &:hover,
    //     &.active {
    //         // background-color: $bgHoverColor;
    //         .ext-dot {
    //             background-color: $primaryColor;
    //         }
    //     }
    // }
    // .footer-ext-menu {
    //     position: absolute;
    //     top: -1px;
    //     right: -100px;
    //     width: 100px;
    //     border: 1px solid $borderWeightColor;
    //     border-radius: 2px;
    //     box-shadow: 0 3px 6px rgba(0, 0, 0, .1);
    //     z-index: 3;
    //     .ext-menu-item {
    //         display: block;
    //         width: 100%;
    //         height: 42px;
    //         line-height: 42px;
    //         text-align: center;
    //         font-size: 14px;
    //         color: $fontWeightColor;
    //         background-color: #fff;
    //         cursor: pointer;
    //         &:hover {
    //             background-color: $primaryLightColor;
    //             color: $primaryColor;
    //         }
    //     }
    //     .is-disable {
    //         cursor: not-allowed;
    //     }
    // }
    .footer-ext-dots {
        position: absolute;
        top: 50%;
        left: 50%;
        transform: translate(-50%, -50%);
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
        display: inline-block;
        vertical-align: middle;
    }

    .tippy-tooltip.dot-menu-theme {
        padding: 0;
    }
    .dot-menu-trigger {
        display: block;
        width: 20px;
        height: 30px;
        line-height: 30px;
        text-align: center;
        font-size: 0;
        cursor: pointer;
    }
    .dot-menu-trigger:hover {
        color: #3A84FF;
        background-color: #DCDEE5;
    }
    .dot-menu-trigger:before {
        content: "";
        display: inline-block;
        width: 3px;
        height: 3px;
        border-radius: 50%;
        background-color: currentColor;
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
