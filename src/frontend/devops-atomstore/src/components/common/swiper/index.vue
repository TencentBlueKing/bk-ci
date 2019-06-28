<template>
    <section class="swiper-home" ref="swiper">
        <hgroup :style="{ width: `${swiperMainWith}px`, transform: `translateX(-${imageTransfer}px)` }"
            :class="[isTransition ? 'transition' : 'no-transition', 'swiper-main']"
            @mousedown="moveStart"
            @mousemove="moveing"
            @mouseup="mmoveEnd"
            @mouseout="mmoveEnd"
            @transitionend="transitionend"
        >
            <h3 v-for="(pic, index) in picList"
                :key="index"
                class="swiper-img"
                :class="pic.class"
                :style="{ 'background-color': pic.color }"
            >
            </h3>
        </hgroup>
        <ul class="swiper-index">
            <li v-for="(pic, index) in pics"
                :key="index"
                :class="{ 'current-index': currentIndex === index + 1 }"
                @mouseover="handleMouseIndex(index + 1)"
            >
            </li>
        </ul>
        <i class="swiper-nav nav-left" @click="changeIndex(-1)"></i>
        <i class="swiper-nav nav-right" @click="changeIndex(1)"></i>
    </section>
</template>

<script>
    export default {
        props: {
            pics: Array
        },

        data () {
            return {
                width: 0, // 图片宽度
                swiperMainWith: 0, // 轮播图宽度
                currentIndex: 1, // 轮播图索引
                isStartMove: false,
                isTransition: false,
                startMovePoint: 0,
                mouseDistance: 0,
                loopId: ''
            }
        },

        computed: {
            picList () {
                const first = this.pics[0]
                const last = this.pics.slice(-1)
                return [...last, ...this.pics, first]
            },

            imageTransfer () {
                const indexMove = this.width * this.currentIndex
                const imageMove = indexMove - this.mouseDistance
                return imageMove
            }
        },

        mounted () {
            this.initStatus()
        },

        beforeDestroy () {
            this.destoryStatus()
        },

        methods: {
            initStatus () {
                this.width = this.$refs.swiper.offsetWidth
                this.swiperMainWith = this.width * (this.pics.length + 2)
                this.startLoop()

                document.addEventListener('visibilitychange', this.visChange)
            },

            destoryStatus () {
                this.endLoop()
                document.removeEventListener('visibilitychange', this.visChange)
            },

            visChange (event) {
                const hidden = event.target.hidden || false
                if (hidden) this.endLoop()
                else this.startLoop()
            },

            changeIndex (index) {
                this.isTransition = true
                this.startLoop()
                this.currentIndex += index
            },

            handleMouseIndex (index) {
                this.isTransition = true
                this.startLoop()
                this.currentIndex = index
            },

            moveStart (event) {
                this.endLoop()
                event.preventDefault()

                this.isTransition = false
                this.isStartMove = true
                this.startMovePoint = event.clientX
            },

            moveing (event) {
                if (!this.isStartMove) return
                const mouseMove = event.clientX - this.startMovePoint
                this.mouseDistance = mouseMove
            },

            mmoveEnd (event) {
                this.startLoop()
                this.isTransition = true
                this.isStartMove = false

                const threshold = this.width / 3
                const absMouseDis = Math.abs(this.mouseDistance)
                if (absMouseDis > threshold) this.currentIndex -= absMouseDis / this.mouseDistance
                this.mouseDistance = 0
            },

            transitionend () {
                const picLength = this.picList.length - 1
                if (this.currentIndex <= 0) {
                    this.isTransition = false
                    this.currentIndex = picLength - 1
                }
                if (this.currentIndex >= picLength) {
                    this.isTransition = false
                    this.currentIndex = 1
                }
            },

            startLoop () {
                this.endLoop()

                this.loopId = window.setTimeout(() => {
                    this.isTransition = true
                    this.currentIndex++
                    this.startLoop()
                }, 8000)
            },

            endLoop () {
                window.clearTimeout(this.loopId)
            }
        }
    }
</script>

<style lang="scss" scoped>
    @import '@/assets/scss/conf.scss';

    .swiper-home {
        position: relative;
        overflow: hidden;
    }
    .swiper-main {
        height: 100%;
        display: flex;
        overflow: hidden;
        &.transition {
            transition: 0.525s cubic-bezier(0.42, 0, 0.58, 1);
        }
        &.no-transition {
            transition: none;
        }
        .swiper-img {
            width: 100%;
            height: 100%;
            background-size: contain;
            background-repeat: no-repeat;
            background-position: center;
        }
        .first-pic {
            background-image: url('../../../images/firstBanner.webp');
        }
        .second-pic {
            background-image: url('../../../images/firstBanner.webp');
        }
    }
    .swiper-index {
        position: absolute;
        bottom: 10px;
        left: 0;
        right: 0;
        display: flex;
        justify-content: center;
        li {
            width: 11px;
            height: 4px;
            margin: 0 3px;
            background: $fontWeightColor;
            border-radius: 2px;
            transition: width 0.525s;
            &.current-index {
                width: 17px;
                background: $darkWhite;
            }
        }
    }
    .swiper-nav {
        cursor: pointer;
        position: absolute;
        display: block;
        width: 14px;
        height: 14px;
        border-left: 4px solid $white;
        border-bottom: 4px solid $white;
        opacity: .3;
        top: 109px;
        &.nav-left {
            left: 14px;
            transform: rotate(45deg);
            &:hover {
                transform: rotate(45deg) scale(1.1);
            }
        }
        &.nav-right {
            right: 14px;
            transform: rotate(225deg);
            &:hover {
                transform: rotate(225deg) scale(1.1);
            }
        }
        &:hover {
            opacity: .8;
        }
        &:active {
            opacity: .6;
        }
    }
</style>
