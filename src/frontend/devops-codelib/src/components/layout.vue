<template>
    <div
        ref="layout"
        class="codelib-page-layout"
        :class="{
            'is-flod': flod
        }"
    >
        <div class="layout-left">
            <div
                class="left-wraper"
                :style="styles"
            >
                <slot />
            </div>
        </div>
        <div
            v-if="flod"
            class="layout-right"
            :style="rightStyles"
        >
            <div
                class="right-wraper"
                :class="{ active: isShowRight }"
            >
                <slot
                    v-if="isShowRight"
                    name="flod"
                />
            </div>
        </div>
    </div>
</template>
<script>
    import { getOffset } from '@/utils/'
    export default {
        name: '',
        props: {
            flod: {
                type: Boolean,
                default: false
            }
        },
        data () {
            return {
                isShowRight: false,
                layoutWidth: 'auto',
                layoutOffsetTop: 0
            }
        },
        computed: {
            styles () {
                if (this.flod) {
                    return {
                        width: '400px'
                    }
                }
                return {
                    width: this.layoutWidth
                }
            },
            rightStyles () {
                const paddingBottom = 18
                return {
                    height: `calc(100vh -  ${this.layoutOffsetTop + paddingBottom}px)`
                }
            }
        },
        watch: {
            flod: {
                handler (flod) {
                    if (flod) {
                        setTimeout(() => {
                            this.isShowRight = flod
                        }, 110)
                    } else {
                        this.isShowRight = false
                    }
                },
                immediate: true
            }
        },
        mounted () {
            this.init()
            window.addEventListener('resize', this.init)
            this.$once('hook:beforeDestroy', () => {
                window.removeEventListener('resize', this.init)
            })
        },
        methods: {
            /**
             * @desc 根据屏幕尺寸动态计算 layout 的位置信息
             */
            init () {
                const layoutWidth = this.$refs.layout.getBoundingClientRect().width
                this.layoutWidth = `${layoutWidth}px`
                const offsetTop = getOffset(this.$refs.layout).top
                this.layoutOffsetTop = offsetTop
            }
        }
    }
</script>
<style lang='scss'>
    .codelib-page-layout {
        display: flex;

        &.is-flod {
            .devops-codelib-table {
                .bk-table-row {
                    cursor: pointer;
                    &.active {
                        background: #eff5ff;
                    }
                }
            }
        }

        .layout-left {
            position: relative;
            z-index: 9;
            transition: all 0.15s;
            flex: 0 0 auto;
            .left-wraper {
                transition: all 0.1s;
            }
            .bk-table {
                background-color: #fff;
            }
            .bk-table::before {
                content: unset;
            }
            
        }

        .layout-right {
            position: relative;
            margin-left: 16px;
            overflow: hidden;
            border-radius: 2px;
            transition: all 0.15s;
            flex: 1;
            box-shadow: 0 2px 2px 0 rgba(0,0,0,.15);
            .right-wraper {
                opacity: 0%;
                height: 100%;
                transition: all 0.5s;

                &.active {
                    opacity: 100%;
                }
            }
        }
    }
</style>
