<template>
    <article class="log-home">
        <button
            type="button"
            @click="closeLog"
            class="log-home-close-bar"
            :aria-label="$t('close')"
            :title="$t('close')"
        >
            <span
                class="log-home-close-icon"
                :style="closeIconStyle"
                aria-hidden="true"
            />
        </button>
        <section
            v-bk-clickoutside="closeLog"
            ref="logMain"
            :class="[currentTab && currentTab !== 'log' ? 'white-theme' : 'black-theme over-hidden', 'log-main', { 'allow-tool-overflow': allowToolOverflow }]"
        >
            <header class="log-head">
                <span class="log-title"><status-icon
                    :status="status"
                    :is-hook="isHook"
                    outline
                ></status-icon><em
                    v-if="positionCode"
                    class="position-code"
                >{{ positionCode }}</em><span
                    class="log-title-text"
                    :title="title"
                >{{ title }}</span></span>
                <div class="log-head-tab"><slot name="tab"></slot></div>
                <div
                    v-if="hasToolSlot"
                    ref="headTools"
                    class="log-head-tools"
                    :tabindex="isScrollableTools ? 0 : undefined"
                    :role="isScrollableTools ? 'group' : undefined"
                    :aria-label="isScrollableTools ? $t('logPanel.scrollToolbar') : undefined"
                >
                    <slot name="tool"></slot>
                </div>
            </header>

            <main class="log-content">
                <slot name="content"></slot>
            </main>
        </section>
    </article>
</template>

<script>
    import angleRightIcon from '../../../../svg-sprites/devops/angle-right.svg'
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
            positionCode: {
                type: String,
                default: ''
            },
            isHook: {
                type: Boolean
            },
            allowToolOverflow: {
                type: Boolean,
                default: false
            }
        },

        data () {
            return {
                closeIconStyle: {
                    backgroundColor: 'currentColor',
                    mask: `url(${angleRightIcon}) no-repeat center / contain`,
                    WebkitMask: `url(${angleRightIcon}) no-repeat center / contain`
                },
                isScrollableTools: false,
                resizeObserver: null
            }
        },

        computed: {
            hasToolSlot () {
                return Boolean(this.$slots.tool && this.$slots.tool.length)
            }
        },

        mounted () {
            this.$nextTick(() => {
                if (typeof window.ResizeObserver !== 'undefined') {
                    this.resizeObserver = new window.ResizeObserver(() => this.syncToolScrollability())
                    if (this.$refs.logMain) this.resizeObserver.observe(this.$refs.logMain)
                    if (this.$refs.headTools) this.resizeObserver.observe(this.$refs.headTools)
                } else {
                    window.addEventListener('resize', this.syncToolScrollability)
                }
                this.syncToolScrollability()
            })
        },

        updated () {
            this.syncToolScrollability()
        },

        beforeDestroy () {
            if (this.resizeObserver) this.resizeObserver.disconnect()
            else window.removeEventListener('resize', this.syncToolScrollability)
        },

        methods: {
            syncToolScrollability () {
                const tools = this.$refs.headTools
                const scrollable = Boolean(tools && tools.scrollWidth > tools.clientWidth)
                if (this.isScrollableTools !== scrollable) this.isScrollableTools = scrollable
            },
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
            font-size: 12px;
            cursor: pointer;
            display: inline-flex;
            align-items: center;
            justify-content: center;
            font-weight: normal;
            height: 26px;
            line-height: 20px;
            padding: 0 12px;
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
        font: 12px/20px "Microsoft YaHei", sans-serif;
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
        --log-panel-width: min(1200px, calc(100vw - 40px));
        .scroll-loading {
            position: absolute;
            bottom: 0;
            width: 100%;
            height: 16px;
        }
        .log-main {
            position: absolute;
            right: 0;
            width: var(--log-panel-width);
            container-name: exec-log-panel;
            height: calc(100vh - 16px);
            display: flex;
            flex-direction: column;
            margin: 8px;
            border-radius: 0 4px 4px 4px;
            transition-property: transform, opacity;
            transition: transform 200ms cubic-bezier(.165,.84,.44,1), opacity 100ms cubic-bezier(.215,.61,.355,1);
            &.over-hidden {
                overflow: hidden;
            }
            .log-content {
                flex: 1 1 auto;
                min-height: 0;
            }
            ::v-deep .bk-log .scroll-home > .log-loading {
                position: absolute;
                inset: 0;
                width: 100%;
                height: 100%;
                display: flex;
                align-items: center;
                justify-content: center;
                transform: none;
                background: #2c2d34;
            }
            ::v-deep .bk-log .scroll-home > .log-loading .lds-ring {
                position: static;
                left: auto;
                transform: none;
            }
            ::v-deep .log-not-started {
                display: block;
                flex: 1 1 auto;
                padding: 12px 20px;
                color: #979ba5;
                font: 12px/20px "Microsoft YaHei", sans-serif;
                box-sizing: border-box;
                .log-line-number {
                    display: inline-block;
                    width: 32px;
                    margin-right: 10px;
                    color: #777c88;
                    text-align: right;
                }
            }
            .log-head {
                height: 60px;
                box-sizing: border-box;
                background-color: #242a36;
                font: 12px/20px "Microsoft YaHei", sans-serif;
                padding: 0 16px;
                border-bottom: 1px solid;
                border-bottom-color: #2b2b2b;
                align-items: center;
                justify-content: space-between;
                color: #d4d4d4;
                position: relative;
                flex: 0 0 auto; /* 确保 header 固定在顶部 */
                display: grid;
                grid-template-columns: minmax(0, 1fr) max-content minmax(0, 1fr);
                column-gap: 16px;
                overflow: hidden;
                overflow-y: hidden;
                .head-tab {
                    grid-column: 2;
                    flex-shrink: 0;
                }
                .log-head-tab {
                    grid-column: 2;
                    grid-row: 1;
                    justify-self: center;
                    min-width: max-content;
                }
                .log-head-tools {
                    grid-column: 3;
                    grid-row: 1;
                    justify-self: stretch;
                    min-width: 0;
                    width: 100%;
                    max-width: 100%;
                    box-sizing: border-box;
                    display: flex;
                    justify-content: flex-start;
                    overflow-x: auto;
                    overflow-y: hidden;
                    scrollbar-width: thin;
                    &:focus-visible { outline: 2px solid #3c96ff; outline-offset: -2px; }
                    ::v-deep .log-panel-toolbar {
                        flex: 0 0 auto;
                        margin-left: auto;
                    }
                }
                .log-title {
                    display: flex;
                    align-items: center;
                    flex: 1 1 auto;
                    min-width: 0;
                    overflow: hidden;
                    white-space: nowrap;
                    text-overflow: ellipsis;
                    ::v-deep .log-status {
                        flex: 0 0 16px;
                        width: 16px;
                        height: 16px;
                        padding: 0;
                        line-height: 16px;
                    }
                    ::v-deep .log-status svg { width: 16px; height: 16px; }
                    ::v-deep .log-status + .position-code,
                    ::v-deep .log-status + .log-title-text { margin-left: 8px; }
                    .position-code {
                        margin-right: 8px;
                        color: #979ba5;
                        font-size: 12px;
                        font-style: normal;
                        font-weight: normal;
                        line-height: 20px;
                    }
                    .log-title-text {
                        min-width: 0;
                        color: #fff;
                        font-size: 12px;
                        line-height: 20px;
                        overflow: hidden;
                        text-overflow: ellipsis;
                        white-space: nowrap;
                    }
                }
                ::v-deep .head-tool {
                    width: max-content;
                }
            }
            &.allow-tool-overflow {
                .log-head,
                .log-head .log-head-tools {
                    overflow: visible;
                }
            }
            &.black-theme {
                background: #2c2d34;
                box-shadow: 0 0 10px 0 rgba(0, 0, 0, .15);
                .log-content {
                    display: flex;
                    flex-direction: column;
                    overflow: hidden;
                }
            }
            &.white-theme {
                background: #fff;
                overflow: hidden;
                box-shadow: 0 0 10px 0 rgba(0, 0, 0, .2);
                .log-content {
                    display: flex;
                    flex-direction: column;
                    overflow: auto;
                }
                &.log-main .log-head {
                    border-top-right-radius: 4px;
                }
            }
        }
    }
    .log-home-close-bar {
        box-sizing: border-box;
        border: 0;
        padding: 0;
        font: inherit;
        cursor: pointer;
        height: 60px;
        width: 24px;
        position: absolute;
        right: calc(var(--log-panel-width) + 8px);
        top: 8px;
        background: #464953;
        color: #c4c6cc;
        display: flex;
        align-items: center;
        justify-content: center;
        border-radius: 2px 0 0 2px;
        &:focus-visible {
            outline: 2px solid #3c96ff;
            outline-offset: 2px;
        }
    }
    .log-home-close-icon {
        width: 16px;
        height: 16px;
        flex-shrink: 0;
    }
</style>
