<template>
    <section class="pp-min-map" ref="minMapMain">
        <main v-show="showMap" class="min-map-main">
            <canvas ref="minMapCanvas"></canvas>
            <span class="min-map-nav"
                :style="`width: ${navWidth}px; height: ${navHeight}px; top: ${navTop}px; left: ${navLeft}px`"
                @mousedown="startMove"
            ></span>
        </main>
        <Logo name="mapSee" size="20" class="min-map-logo" v-if="showMap" @click.native="showMap = false"></Logo>
        <Logo name="mapUnSee" size="20" class="min-map-logo" v-else @click.native="showMap = true"></Logo>
    </section>
</template>

<script>
    import Logo from '@/components/Logo'
    const pipelineStyle = {
        itemWidth: 240,
        itemHeight: 42,
        stageMarginRight: 82,
        stageWidth: 283.35,
        stageHeight: 32,
        stageBottomDis: 10,
        atomBottomDis: 11,
        containerBottomDis: 16,
        containerGap: 26,
        stageColor: '#eff5ff',
        containerLeftColor: '#3c96ff',
        containerColor: '#33333f',
        atomBorderColor: '#c3cdd7',
        atomColor: '#fff',
        fontSize: 25,
        containerLeftWidth: 42,
        lineWidth: 0.5
    }

    const containerStatusColor = {
        SUCCEED: '#34d97b',
        FAILED: '#ff5656',
        HEARTBEAT_TIMEOUT: '#ff5656',
        QUEUE_TIMEOUT: '#63656e',
        EXEC_TIMEOUT: '#63656e',
        REVIEWING: '#ffb400',
        REVIEW_ABORT: '#ffb400',
        SKIP: '#63656e',
        CANCELED: '#ffb400',
        TERMINATE: '#ffb400',
        RUNNING: '#459fff',
        PREPARE_ENV: '#459fff',
        undefined: '#63656e'
    }

    const atomBorderStatusColor = {
        SUCCEED: '#34d97b',
        FAILED: '#ff5656',
        HEARTBEAT_TIMEOUT: '#ff5656',
        QUEUE_TIMEOUT: '#ff5656',
        EXEC_TIMEOUT: '#ff5656',
        REVIEWING: '#ffb400',
        REVIEW_ABORT: '#ffb400',
        SKIP: '#c3cdd7',
        CANCELED: '#ffb400',
        TERMINATE: '#ffb400',
        RUNNING: '#459fff',
        PREPARE_ENV: '#459fff',
        undefined: '#c3cdd7'
    }

    const stageStatusColor = {
        SUCCEED: '#f3fff6',
        FAILED: '#fff9f9',
        PAUSE: '#f3f3f3',
        RUNNING: '#eff5ff',
        SKIP: '#c3cdd7',
        CANCELED: '#f3f3f3',
        undefined: '#f3f3f3'
    }

    const stageBorderColor = {
        SUCCEED: '#bbefc9',
        FAILED: '#ffd4d4',
        PAUSE: '#d0d8ea',
        RUNNING: '#d4e8ff',
        SKIP: '#c3cdd7',
        CANCELED: '#d0d8ea',
        undefined: '#d0d8ea'
    }

    export default {
        components: {
            Logo
        },

        props: {
            scrollClass: {
                type: String,
                required: true
            },
            stages: {
                type: Array,
                required: true
            }
        },

        data () {
            return {
                navHeight: 0,
                navWidth: 0,
                navTop: 0,
                navLeft: 0,
                rate: 0,
                sw: 0,
                sh: 0,
                realHeght: 0,
                realWidth: 0,
                scrollEle: {},
                canvasCtx: {},
                showMap: true
            }
        },

        watch: {
            stages: {
                handler () {
                    this.$nextTick(this.drawMiniMap)
                },
                deep: true
            }
        },

        mounted () {
            this.initStatus()
        },

        beforeDestroy () {
            document.removeEventListener('mousemove', this.mapMove)
            document.removeEventListener('mouseup', this.moveEnd)
            this.scrollEle.removeEventListener('scroll', this.eleScroll, { passive: true })
        },

        methods: {
            initStatus () {
                this.$nextTick(() => {
                    this.scrollEle = document.querySelector(this.scrollClass)
                    document.addEventListener('mousemove', this.mapMove)
                    document.addEventListener('mouseup', this.moveEnd)
                    this.scrollEle && this.scrollEle.addEventListener('scroll', this.eleScroll, { passive: true })
                    const dpr = window.devicePixelRatio || 1
                    this.$refs.minMapCanvas.width = 200 * dpr
                    this.$refs.minMapCanvas.height = 134 * dpr
                    this.$refs.minMapCanvas.getContext('2d').setTransform(dpr, 0, 0, dpr, 0, 0)
                    this.canvasCtx = this.$refs.minMapCanvas.getContext('2d')
                    this.canvasCtx.lineWidth = pipelineStyle.lineWidth
                    this.canvasCtx.textBaseline = 'top'
                    this.drawMiniMap()
                })
            },

            drawMiniMap () {
                this.realHeght = this.scrollEle.scrollHeight
                this.realWidth = this.scrollEle.scrollWidth
                const heightRate = 134 / this.realHeght
                const widthRate = 200 / this.realWidth
                this.rate = Math.min(heightRate, widthRate)
                const sw = this.rate * this.realWidth
                const sh = this.rate * this.realHeght
                this.navTop = this.scrollEle.scrollTop / this.realHeght * sh
                this.navLeft = this.scrollEle.scrollLeft / this.realWidth * sw
                this.navWidth = this.scrollEle.offsetWidth / this.realWidth * sw
                this.navHeight = this.scrollEle.offsetHeight / this.realHeght * sh
                this.navWidth = this.navWidth > sw ? sw : this.navWidth
                this.navHeight = this.navHeight > sh ? sh : this.navHeight
                this.sw = sw
                this.sh = sh
                this.canvasCtx.clearRect(0, 0, 200, 134)
                this.canvasCtx.font = `normal normal normal ${pipelineStyle.fontSize * this.rate}px pingFangSC-Regular`
                this.drawStages()
            },

            drawStages () {
                (this.stages || []).forEach((stage, index) => {
                    const startX = (index * (pipelineStyle.itemWidth + pipelineStyle.stageMarginRight) - (pipelineStyle.stageWidth - pipelineStyle.itemWidth) / 2) * this.rate
                    const startY = 0
                    if (startX >= 0) {
                        this.canvasCtx.fillStyle = this.$route.name === 'pipelinesDetail' ? stageStatusColor[stage.status] : pipelineStyle.stageColor
                        this.canvasCtx.fillRect(startX, startY, pipelineStyle.stageWidth * this.rate, pipelineStyle.stageHeight * this.rate)
                        this.canvasCtx.strokeStyle = this.$route.name === 'pipelinesDetail' ? stageBorderColor[stage.status] : pipelineStyle.stageColor
                        this.canvasCtx.strokeRect(startX, startY, pipelineStyle.stageWidth * this.rate, pipelineStyle.stageHeight * this.rate)
                    }
                    const containers = stage.containers || []
                    this.drawContainer.x = index * (pipelineStyle.itemWidth + pipelineStyle.stageMarginRight) * this.rate
                    this.drawContainer.y = (pipelineStyle.stageBottomDis + pipelineStyle.stageHeight) * this.rate
                    containers.forEach((container) => this.drawContainer(container))
                })
            },

            drawContainer (container) {
                const elements = container.elements || []
                this.canvasCtx.fillStyle = this.$route.name === 'pipelinesDetail' ? containerStatusColor[container.status] : pipelineStyle.containerColor
                this.canvasCtx.fillRect(this.drawContainer.x, this.drawContainer.y, pipelineStyle.itemWidth * this.rate, pipelineStyle.itemHeight * this.rate)
                this.canvasCtx.fillStyle = this.$route.name === 'pipelinesDetail' ? containerStatusColor[container.status] : pipelineStyle.containerLeftColor
                this.canvasCtx.fillRect(this.drawContainer.x, this.drawContainer.y, pipelineStyle.containerLeftWidth * this.rate, pipelineStyle.containerLeftWidth * this.rate)
                this.drawContainer.y += (pipelineStyle.containerLeftWidth + pipelineStyle.containerBottomDis) * this.rate
                if (elements.length <= 0) this.drawElement({})
                elements.forEach((element) => this.drawElement(element))
                this.drawContainer.y += (pipelineStyle.containerGap - pipelineStyle.atomBottomDis) * this.rate
            },

            drawElement (element) {
                this.canvasCtx.strokeStyle = this.$route.name === 'pipelinesDetail' ? atomBorderStatusColor[element.status] : pipelineStyle.atomBorderColor
                this.canvasCtx.strokeRect(this.drawContainer.x, this.drawContainer.y, pipelineStyle.itemWidth * this.rate, pipelineStyle.itemHeight * this.rate)
                this.canvasCtx.fillStyle = pipelineStyle.atomColor
                this.canvasCtx.fillRect(this.drawContainer.x, this.drawContainer.y, pipelineStyle.itemWidth * this.rate, pipelineStyle.itemHeight * this.rate)
                this.drawContainer.y += (pipelineStyle.itemHeight + pipelineStyle.atomBottomDis) * this.rate
            },

            startMove (event) {
                this.startMove.isStart = true
                this.startMove.x = event.clientX
                this.startMove.y = event.clientY
            },

            mapMove (event) {
                if (!this.startMove.isStart) return
                const currentX = event.clientX
                const currentY = event.clientY
                let top = this.navTop + currentY - this.startMove.y
                let left = this.navLeft + currentX - this.startMove.x
                top = top <= 0 ? 0 : (top >= (this.sh - this.navHeight) ? this.sh - this.navHeight : top)
                left = left <= 0 ? 0 : (left >= (this.sw - this.navWidth) ? this.sw - this.navWidth : left)

                this.scrollEle.scrollTop = top * this.realHeght / this.sh
                this.scrollEle.scrollLeft = left * this.realWidth / this.sw
                this.navTop = top
                this.navLeft = left
                this.startMove.x = currentX
                this.startMove.y = currentY
            },

            moveEnd () {
                this.startMove.isStart = false
            },

            eleScroll (event) {
                if (this.startMove.isStart) return
                const top = this.scrollEle.scrollTop
                const left = this.scrollEle.scrollLeft
                this.navTop = top / this.realHeght * this.sh
                this.navLeft = left / this.realWidth * this.sw
            }
        }
    }
</script>

<style lang="scss" scope>
    .pp-min-map {
        position: fixed;
        bottom: 36px;
        right: 36px;
        z-index: 4;
        background: #e6e6e6;
        box-shadow: 0px 0px 6px 0px rgba(49,50,56,0.1);
        border-radius: 2px;
        user-select: none;
        .min-map-main {
            width: 200px;
            height: 134px;
        }
        .min-map-nav {
            display: inline-block;
            position: absolute;
            z-index: 48;
            cursor: grab;
            user-select: none;
            background: #FFF;
            opacity: 0.5;
            border-radius: 2px;
            border: 1px solid #999;
            &:hover {
                border: 1px solid #333;
            }
        }
        canvas {
            height: 134px;
            width: 200px;
        }
        .min-map-logo {
            position: absolute;
            right: -10px;
            bottom: -10px;
            z-index: 5;
            cursor: pointer;
        }
    }
</style>
