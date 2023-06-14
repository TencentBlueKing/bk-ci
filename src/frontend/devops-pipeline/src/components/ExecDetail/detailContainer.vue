<template>
    <article class="log-home">
        <!-- <span @click="closeLog" class="log-home-close-bar">
            <i class="devops-icon icon-angle-right"></i>
        </span> -->
        <section v-bk-clickoutside="closeLog" :class="[currentTab === 'log' ? 'black-theme over-hidden' : 'white-theme', 'log-main']">
            <header class="log-head">
                <span class="log-title"><status-icon :status="status" :is-hook="isHook"></status-icon>{{ title }}</span>
                <slot name="tab"></slot>
                <slot name="tool"></slot>
            </header>

            <slot name="content"></slot>
        </section>
    </article>
</template>

<script>
    import statusIcon from './status'

    export default {
        components: {
            statusIcon
        },

        props: {
            status: {
                type: String
            },
            title: {
                type: String
            },
            currentTab: {
                type: String
            },
            isHook: {
                type: Boolean
            }
        },

        methods: {
            closeLog (event) {
                this.$emit('close')
            }
        }
    }
</script>

<style lang="scss" scoped>
    ::v-deep .head-tab {
        font-size: 0;
        span {
            font-size: 14px;
            cursor: pointer;
            font-weight: normal;
            padding: 4px 12px;
            color: #999999;
            background: #2e3342;
            &.active {
                color: #fff;
                background: #1a6df3;
            }
            &:first-child {
                border-radius: 3px 0 0 3px;
            }
            &:last-child {
                border-radius: 0 3px 3px 0;
            }
        }
    }

    ::v-deep .head-tool {
        cursor: pointer;
        font-size: 14px;
        margin-right: 15px;
        color: #3c96ff;
    }

    .log-home {
        position: fixed;
        width: 100vw;
        height: 100vh;
        right: 0;
        top: 0;
        background-color: rgba(0, 0, 0, .2);
        z-index: 1000;
        .scroll-loading {
            position: absolute;
            bottom: 0;
            width: 100%;
            height: 16px;
        }
        .log-main {
            position: absolute;
            right: 26px;
            width: 80vw;
            height: calc(100vh - 32px);
            display: flex;
            flex-direction: column;
            margin: 16px;
            border-radius: 6px;
            transition-property: transform, opacity;
            transition: transform 200ms cubic-bezier(.165,.84,.44,1), opacity 100ms cubic-bezier(.215,.61,.355,1);
            &.over-hidden {
                overflow: hidden;
            }
            .log-head {
                background-color: rgb(37, 41, 53);
                line-height: 48px;
                padding: 5px 20px;
                border-bottom: 1px solid;
                border-bottom-color: #2b2b2b;
                display: flex;
                align-items: center;
                justify-content: space-between;
                color: #d4d4d4;
                position: relative;
                .head-tab {
                    position: absolute;
                    left: 50%;
                    transform: translateX(-50%);
                }
                .log-title {
                    display: flex;
                    align-items: center;
                }
            }
            &.black-theme {
                background: #1e1e1e;
            }
            &.white-theme {
                background: #fff;
                overflow: auto;
                box-shadow: 0 0 10px 0 rgba(0, 0, 0, .2);
                &.log-main .log-head {
                    border-top-right-radius: 6px;
                }
            }
        }
    }
    .log-home-close-bar {
        cursor: pointer;
        height: 59px;
        width: 26px;
        position: absolute;
        right: calc(80vw + 42px);
        top: 16px;
        background: #464953;
        color: white;
        display: flex;
        align-items: center;
        justify-content: center;
        border-top-left-radius: 6px;
        border-bottom-left-radius: 6px;
    }
</style>
