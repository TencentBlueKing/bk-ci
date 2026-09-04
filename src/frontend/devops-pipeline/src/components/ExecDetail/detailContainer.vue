<template>
    <article class="log-home">
        <span @click="closeLog" class="log-home-close-bar" title="收起日志">
            <i class="devops-icon icon-angle-right"></i>
        </span>
        <section
            v-bk-clickoutside="handleClickOutside"
            :class="[currentTab && currentTab !== 'log' ? 'white-theme' : 'black-theme over-hidden', 'log-main']"
        >
            <header class="log-head">
                <span class="log-title">
                    <status-icon
                        :status="status"
                        :is-hook="isHook"
                    ></status-icon>
                    <span
                        v-if="position"
                        class="log-pos"
                        :title="position"
                    >{{ position }}</span>
                    <span class="log-title-text">{{ title }}</span>
                </span>
                <slot name="tab"></slot>
                <slot name="tool"></slot>
            </header>

            <main class="log-content">
                <slot name="content"></slot>
            </main>
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
            position: {
                type: String,
                default: ''
            },
            currentTab: {
                type: String
            },
            isHook: {
                type: Boolean
            },
            ignoreClose: {
                type: Boolean,
                default: false
            }
        },

        methods: {
            handleClickOutside () {
                if (this.ignoreClose) return
                this.closeLog()
            },
            closeLog () {
                this.$emit('close')
            }
        }
    }
</script>

<style lang="scss" scoped>
    ::v-deep .head-tab {
        font-size: 0;
        display: flex;
        align-items: center;
        span {
            font-size: 12px;
            line-height: 20px;
            height: 26px;
            box-sizing: border-box;
            cursor: pointer;
            font-weight: normal;
            padding: 3px 12px;
            color: #fff;
            background: #2e3342;
            &.active {
                color: #fff;
                background: #3a84ff;
            }
            &:first-child {
                border-radius: 2px 0 0 2px;
            }
            &:last-child {
                border-radius: 0 2px 2px 0;
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
            .log-content {
                flex: 1 1 auto;
                min-height: 0;
            }
            .log-head {
                background-color: #242a36;
                height: 60px;
                line-height: 60px;
                padding: 0 16px;
                border-bottom: none;
                display: flex;
                align-items: center;
                justify-content: space-between;
                color: #f0f1f5;
                position: relative;
                flex: 0 0 auto; /* 确保 header 固定在顶部 */
                .head-tab {
                    position: absolute;
                    left: 50%;
                    transform: translateX(-50%);
                }
                .log-title {
                    display: flex;
                    align-items: center;
                    gap: 8px;
                    min-width: 0;
                    max-width: 22%;
                }
                .log-pos {
                    flex-shrink: 0;
                    padding: 0 6px;
                    height: 18px;
                    line-height: 18px;
                    border-radius: 2px;
                    background: #3a3f4b;
                    color: #c4c6cc;
                    font-size: 11px;
                    font-variant-numeric: tabular-nums;
                }
                .log-title-text {
                    overflow: hidden;
                    text-overflow: ellipsis;
                    white-space: nowrap;
                    font-size: 12px;
                    line-height: 20px;
                    color: #fff;
                }
            }
            &.black-theme {
                background: #2c2d34;
                .log-content {
                    display: flex;
                    overflow: hidden;
                }
            }
            &.white-theme {
                background: #fff;
                overflow: hidden;
                box-shadow: 0 0 10px 0 rgba(0, 0, 0, .2);
                .log-content {
                    overflow: auto;
                    display: flex;
                    flex-direction: column;
                    background: #f5f7fa;
                }
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
