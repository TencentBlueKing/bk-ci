<template>
    <transition name="fade">
        <section v-if="imgSrc"
            class="full-screen"
            @mousemove="mouseMove"
            @mouseup="mouseUp"
        >
            <img ref="screenImg"
                :src="imgSrc"
                @mousewheel.prevent="scrollImage"
                @DOMMouseScroll.prevent="scrollImage"
                @mousedown="mouseDown"
                :class="{ init: isInit }"
                :style="{
                    width: `${width}px`,
                    height: `${height}px`,
                    top: `${top}px`,
                    left: `${left}px`
                }"
            >
        </section>
    </transition>
</template>

<script>
    export default {
        props: {
            imgSrc: String
        },

        data () {
            return {
                isInit: true,
                startMove: false,
                startTime: 0,
                startX: 0,
                startY: 0,
                width: 0,
                height: 0,
                top: 0,
                left: 0
            }
        },

        watch: {
            imgSrc (val) {
                if (val) {
                    this.isInit = true
                    this.width = 0
                    this.height = 0
                    this.top = 0
                    this.left = 0
                }
            }
        },

        methods: {
            startChange (event) {
                if (!this.isInit) return
                this.top = event.clientY - event.offsetY
                this.left = event.clientX - event.offsetX
                this.width = this.$refs.screenImg.clientWidth
                this.height = this.$refs.screenImg.clientHeight
                this.isInit = false
            },

            scrollImage (event) {
                const deltaY = Math.max(-1, Math.min(1, (event.wheelDeltaY || -event.detail)))
                const zoomDis = deltaY * 0.2
                this.startChange(event)
                this.width += this.width * zoomDis
                this.height += this.height * zoomDis
                this.top -= event.offsetY * zoomDis
                this.left -= event.offsetX * zoomDis
            },

            mouseDown (event) {
                event.preventDefault()
                this.startTime = new Date()
                this.startX = event.clientX
                this.startY = event.clientY
                this.startMove = true
                this.startChange(event)
            },

            mouseMove (event) {
                if (!this.startMove) return
                this.top += (event.clientY - (this.mouseMove.tempY || this.startY))
                this.left += (event.clientX - (this.mouseMove.tempX || this.startX))
                this.mouseMove.tempY = event.clientY
                this.mouseMove.tempX = event.clientX
            },

            mouseUp (event) {
                const diffTime = new Date() - this.startTime
                const diffDis = Math.sqrt((event.clientX - this.startX) ** 2 + (event.clientY - this.startY) ** 2)
                if (!this.startMove || (diffTime < 300 && diffDis < 20)) this.$emit('update:imgSrc', '')
                this.mouseMove.tempY = 0
                this.mouseMove.tempX = 0
                this.startMove = false
            }
        }
    }
</script>

<style lang="scss" scoped>
    .full-screen {
        position: fixed;
        top: 0;
        left: 0;
        bottom: 0;
        right: 0;
        z-index: 2;
        background: rgba(0, 0, 0, 0.6);
        cursor: pointer;
        img {
            cursor: grab;
            position: relative;
        }
        .init {
            max-width: 50vw;
            max-height: 50vh;
            height: auto!important;
            width: auto!important;
            top: 50%!important;
            left: 50%!important;
            transform: translate(-50%, -50%);
        }
    }
</style>
