<template>
    <section class="pipeline-steps__scroller clearfix">
        <div class="pipeline-scroller scroller-left fl"
            :class="{ disabled: !allowPullLeft }"
            @click="pullLeft">
            <i class="devops-icon icon-angle-double-left"></i>
        </div>
        <div class="pipeline-scroller scroller-right fr"
            :class="{ disabled: !allowPullRight }"
            @click="pullRight">
            <i class="devops-icon icon-angle-double-right"></i>
        </div>
    </section>
</template>

<script>
    import {
        converStrToNum
    } from '@/utils/util'

    export default {
        props: {
            refName: { // 与scroller组件相关的组件ref名
                type: String,
                default: 'pipeline'
            },
            stageLength: { // stage个数
                type: Number,
                default: 1
            },
            avaliableWidth: { // 可用的宽度
                type: Number,
                default: 0
            },
            scrollTo: { // 指定滚动位移
                type: Number,
                default: 0
            }
        },
        data () {
            return {
                listWrapper: null, // 最外层DOM
                listScroller: null, // stages的滚动区域
                listWrapperWidth: 0, // 最外层DOM的宽度
                listScrollerWidth: 0, // 所有stage的宽度
                stageWidth: 348, // 每个stage的宽度
                scrollerTranslateX: 0
            }
        },
        computed: {
            calcIsDisabled () {
                const {
                    listWrapperWidth,
                    listScrollerWidth
                } = this

                return listWrapperWidth > listScrollerWidth
            },
            // 是否允许向左滑动
            allowPullLeft () {
                return !this.calcIsDisabled && this.scrollerTranslateX < 0
            },
            // 是否允许向右滑动
            allowPullRight () {
                return !this.calcIsDisabled && this.listWrapperWidth + Math.abs(this.scrollerTranslateX) < this.listScrollerWidth
            }
        },
        watch: {
            avaliableWidth (val) {
                const {
                    listWrapper
                } = this

                if (val && listWrapper) {
                    const width = `${val}px`
                    listWrapper.style.width = width
                    this.listWrapperWidth = val

                    this.responseResize(val)
                }
            },
            stageLength (newVal, oldVal) {
                const {
                    listScroller,
                    listWrapperWidth
                } = this

                if (newVal && listScroller) {
                    const width = newVal * this.stageWidth

                    this.listScrollerWidth = width

                    if (newVal > oldVal) { // 增加stage
                        listScroller.style.width = `${width}px`
                    } else { // 删除stage
                        this.responseResize(listWrapperWidth)

                        setTimeout(() => {
                            listScroller.style.width = `${width}px`
                        }, 1000)
                    }
                }
            },
            scrollTo (val) {
                const {
                    listWrapperWidth,
                    listScrollerWidth
                } = this

                if (listWrapperWidth < listScrollerWidth) {
                    const estimateTranslateX = val * this.stageWidth
                    const maxTranslateX = listScrollerWidth - listWrapperWidth
                    let realTranslateX

                    if (estimateTranslateX > maxTranslateX) {
                        realTranslateX = maxTranslateX * (-1)
                    } else {
                        realTranslateX = estimateTranslateX * (-1)
                    }

                    this.listScroller.style.transform = `translateX(${realTranslateX}px)`
                    this.scrollerTranslateX = realTranslateX
                }
            }
        },
        mounted () {
            this.init()

            window.onresize = () => {
                this.listWrapperWidth = converStrToNum(getComputedStyle(this.$parent.$el).width, 'px')
                this.$emit('update:avaliableWidth', this.listWrapperWidth)
            }
        },
        methods: {
            /**
             *  初始化数据
             */
            init () {
                const { refName } = this

                this.stages = this.$parent.$refs[refName]
                this.listWrapper = this.stages.querySelector('[data-type="wrapper"]')
                this.listScroller = this.stages.querySelector('[data-type="scroll"]')

                this.listWrapperWidth = ~~getComputedStyle(this.listWrapper).width.replace('px', '')
                this.listScrollerWidth = ~~getComputedStyle(this.listScroller).width.replace('px', '')
            },
            /**
             *  流水线列表向左滚动
             */
            pullLeft () {
                if (!this.allowPullLeft) return

                this.scrollHandler('left')
            },
            /**
             *  流水线列表向右滚动
             */
            pullRight () {
                if (!this.allowPullRight) return

                this.scrollHandler('right')
            },
            /**
             *  执行滚动
             */
            scrollHandler (direction, displacement = this.stageWidth) {
                const {
                    listScroller
                } = this
                const curTranslateX = listScroller.style.transform.match(/^translateX\((.*)px\)$/)
                const curDisplacement = curTranslateX ? curTranslateX[1] : 0
                let newDisplacement
                let tmp

                if (direction === 'left') {
                    tmp = ~~curDisplacement + displacement

                    newDisplacement = tmp > 0 ? 0 : tmp
                } else {
                    newDisplacement = ~~curDisplacement - displacement
                }

                listScroller.style.transform = `translateX(${newDisplacement}px)`
                this.scrollerTranslateX = newDisplacement
            },
            /**
             *  stages自适应窗口宽度
             *  @param {Number} val - 窗口大小改变后或stage个数改变后listWrapper的宽度
             */
            responseResize (val) {
                const {
                    scrollerTranslateX,
                    listScrollerWidth,
                    listScroller
                } = this
                const calcTranslateX = listScrollerWidth - val
                let realTranslateX

                if (calcTranslateX > 0 && calcTranslateX < Math.abs(scrollerTranslateX)) { // 剩余的stage个数超过可显示的个数
                    realTranslateX = -1 * calcTranslateX
                } else { // 剩余的个数在一屏内可以放下
                    realTranslateX = 0
                }

                listScroller.style.transform = `translateX(${realTranslateX}px)`
            }
        }
    }
</script>

<style lang="scss">
    @import './../../scss/conf';

    .pipeline-steps__scroller {
        /*margin: 20px 60px 0 46px;*/
        .pipeline-scroller {
            display: block;
            width: 32px;
            height: 32px;
            line-height: 32px;
            border: 1px solid $borderColor;
            border-radius: 2px;
            background-color: #fff;
            text-align: center;
            cursor: pointer;
            color: #8a8f9b;
            font-size: 12px;
            &:hover {
                background-color: $primaryColor;
                color: #fff;
            }
            &.disabled {
                background-color: $borderWeightColor;
                cursor: not-allowed;
                color: #8a8f9b;
                opacity: .6;
            }
        }
    }

    [data-type="scroll"] {
        transition: transform linear .3s;
    }
</style>
