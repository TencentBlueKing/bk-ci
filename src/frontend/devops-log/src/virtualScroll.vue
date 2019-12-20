<template>
    <section class="scroll-home" @mousewheel.prevent="handleWheel">
        <ul class="scroll-index scroll" :style="`top: ${-totalScrollHeight}px; width: ${indexWidth}px`">
            <li class="scroll-item" :style="`height: ${itemHeight}px; top: ${item.top}px`" v-for="(item) in indexList" :key="item">
                {{item.value}}
                <span :class="[{ 'show-all': (item.tagData.list || []).length }, 'log-folder']" v-if="item.tagData" @click="foldListData(item.tagData || {})"></span>
            </li>
        </ul>
        <ul class="scroll scroll-main" :style="`top: ${-totalScrollHeight}px;width: ${mainWidth}px; left: ${indexWidth}px`">
            <li :class="[{ 'pointer': item.tagData }, 'scroll-item']"
                :style="`height: ${itemHeight}px; top: ${item.top}px; left: ${-bottomScrollDis * mainWidth / bottomScrollWidth}px; width: ${(mainWidth - bottomScrollWidth) * mainWidth / bottomScrollWidth + mainWidth}px`"
                v-for="item in listData"
                :key="item.top + item.value"
                @click="foldListData(item.tagData || {})"
            ><slot :data="item"></slot>
            </li>
        </ul>
        <canvas class="min-nav no-scroll" :style="`height: ${visHeight}px; width: ${visWidth / 10}px;right: ${visWidth / 100}px`" ref="minMap" @click="changeMinMap"></canvas>
        <span class="min-nav-slide no-scroll"
            v-if="itemHeight * totalNumber > visHeight"
            :style="`height: ${visHeight / 8}px; width: ${visWidth / 10}px; top: ${minMapTop}px;right: ${visWidth / 100}px`"
            @mousedown="startNavMove(mapHeight - visHeight / 8)"
        >
        </span>
        <canvas class="min-nav" :style="`height: ${visHeight}px; width: ${visWidth / 100}px`" ref="minNav"></canvas>
        <span class="min-nav-slide nav-show"
            :style="`height: ${navHeight}px; width: ${visWidth / 100}px; top: ${minNavTop}px`"
            v-if="navHeight < visHeight"
            @mousedown="startNavMove(visHeight - navHeight)"
        >
        </span>
        <span class="min-nav-slide bottom-scroll"
            :style="`left: ${indexWidth + bottomScrollDis + 20}px; width: ${bottomScrollWidth}px`"
            v-if="bottomScrollWidth < mainWidth"
            @mousedown="startBottomMove"
        >
        </span>
        <p class="list-empty" v-if="!$parent.isInit && totalNumber <= 0">{{ language('日志内容为空') }}</p>
    </section>
</template>

<script>
    // eslint-disable-next-line
    const Worker = require('worker-loader!./worker.js')
    import language from './locale'

    export default {
        props: {
            itemHeight: {
                type: Number,
                default: 16
            },
            id: {
                type: String
            }
        },

        data () {
            return {
                indexList: [],
                listData: [],
                foldList: [],
                worker: {},
                totalHeight: 0,
                itemNumber: 0,
                totalNumber: 0,
                visHeight: 0,
                visWidth: 0,
                totalScrollHeight: 0,
                startMinMapMove: false,
                tempVal: 0,
                minMapTop: 0,
                minNavTop: 0,
                navHeight: 0,
                mapHeight: 0,
                moveRate: 0,
                bottomScrollWidth: Infinity,
                bottomScrollDis: 0,
                indexWidth: 0,
                isScrolling: false,
                isBottomMove: false
            }
        },

        computed: {
            mainWidth () {
                return this.visWidth * 89 / 100 - this.indexWidth - 20
            }
        },

        mounted () {
            this.initStatus()
            this.initEvent()
            this.initWorker()
            this.drawMinNav()
        },

        beforeDestroy () {
            this.worker.terminate()
            document.removeEventListener('mousemove', this.minNavMove)
            document.removeEventListener('mouseup', this.moveEnd)
            window.removeEventListener('resize', this.resize)
        },

        methods: {
            language,

            resetData () {
                this.foldList = []
                this.totalNumber = 0
                this.setStatus()
                this.worker.postMessage({ type: 'resetData' })
            },

            changeMinMap () {
                const offsetY = event.offsetY
                const diffDis = offsetY - this.minMapTop - this.visHeight / 16
                let minMapTop = this.minMapTop + diffDis * 8 / ((this.totalNumber - this.itemNumber) * 16) * (this.mapHeight - this.visHeight / 8)
                if (minMapTop <= 0 || this.mapHeight <= this.visHeight / 8) minMapTop = 0
                else if (minMapTop >= this.mapHeight - this.visHeight / 8) minMapTop = this.mapHeight - this.visHeight / 8
                this.minMapTop = minMapTop
                this.totalScrollHeight = this.minMapTop / (this.mapHeight - this.visHeight / 8) * (this.totalHeight - this.visHeight)
                this.minNavTop = this.minMapTop * (this.visHeight - this.navHeight) / (this.mapHeight - this.visHeight / 8)
                this.getListData(this.totalScrollHeight)
            },

            initLink () {
                const query = this.$route.query || {}
                const minMapTop = query.minMapTop
                const id = query.id
                if (typeof minMapTop !== 'undefined' && id === this.id) {
                    this.minMapTop = +minMapTop
                    this.totalScrollHeight = this.minMapTop / (this.mapHeight - this.visHeight / 8) * (this.totalHeight - this.visHeight)
                    this.minNavTop = this.minMapTop * (this.visHeight - this.navHeight) / (this.mapHeight - this.visHeight / 8)
                    this.getListData(this.totalScrollHeight, false, 'initLink')
                }
            },

            foldListData (tagData) {
                const { startIndex } = tagData
                if (typeof startIndex !== 'undefined') {
                    const postData = {
                        type: 'foldListData',
                        startIndex
                    }
                    this.worker.postMessage(postData)
                }
            },

            initStatus () {
                const mainEle = document.querySelector('.scroll-home')
                this.visHeight = mainEle.offsetHeight
                this.visWidth = mainEle.offsetWidth
                const dpr = window.devicePixelRatio || 1
                this.$refs.minMap.width = this.visWidth / 10 * dpr
                this.$refs.minMap.height = this.visHeight * dpr
                this.$refs.minMap.getContext('2d').setTransform(dpr, 0, 0, dpr, 0, 0)
                this.$refs.minNav.width = this.visWidth / 100 * dpr
                this.$refs.minNav.height = this.visHeight * dpr
                this.$refs.minNav.getContext('2d').setTransform(dpr, 0, 0, dpr, 0, 0)
            },

            initEvent () {
                document.addEventListener('mousemove', this.minNavMove)
                document.addEventListener('mouseup', this.moveEnd)
                window.addEventListener('resize', this.resize)
            },

            resize (event) {
                this.slowExec(() => {
                    const lastHeight = this.visHeight
                    this.initStatus()
                    this.setStatus()
                    this.minMapTop = this.visHeight / lastHeight * this.minMapTop
                    this.minNavTop = this.minMapTop * (this.visHeight - this.navHeight) / (this.mapHeight - this.visHeight / 8)
                    
                    this.totalScrollHeight = this.minMapTop / (this.mapHeight - this.visHeight / 8) * (this.totalHeight - this.visHeight)
                    this.getListData()
                })
            },

            handleWheel (data) {
                const target = event.target
                const classList = target.classList
                if (this.isScrolling || this.itemHeight * this.totalNumber <= this.visHeight || (classList && classList.contains('no-scroll'))) return

                // const deltaX = Math.max(-1, Math.min(1, (event.wheelDeltaX || -event.detail)))
                // let bottomScrollLeft = this.bottomScrollDis + deltaX * 10
                // if (bottomScrollLeft <= 0) bottomScrollLeft = 0
                // if (bottomScrollLeft + this.bottomScrollWidth >= this.mainWidth) bottomScrollLeft = this.mainWidth - this.bottomScrollWidth
                // this.bottomScrollDis = bottomScrollLeft

                const deltaY = Math.max(-1, Math.min(1, (event.wheelDeltaY || -event.detail)))
                let dis = deltaY * -(this.itemHeight * 3)
                let tickGap = deltaY * -2
                if (deltaY === 0) {
                    dis = 0
                    tickGap = 0
                }
                const scrollHeight = this.minMapTop + (dis + tickGap) * (this.mapHeight - this.visHeight / 8) / (this.totalHeight - this.itemHeight * this.itemNumber)

                let totalScrollHeight = 0
                let minMapTop = 0
                let minNavTop = 0

                if (scrollHeight < 0) {
                    totalScrollHeight = 0
                    minMapTop = 0
                    minNavTop = 0
                } else if (scrollHeight >= 0 && scrollHeight <= (this.mapHeight - this.visHeight / 8)) {
                    totalScrollHeight = scrollHeight * (this.totalHeight - this.itemHeight * this.itemNumber) / (this.mapHeight - this.visHeight / 8)
                    minMapTop = scrollHeight
                    minNavTop = this.minNavTop + (dis + tickGap) * (this.visHeight - this.navHeight) / (this.totalHeight - this.itemHeight * this.itemNumber)
                } else {
                    totalScrollHeight = this.totalHeight - this.visHeight
                    minMapTop = this.mapHeight - this.visHeight / 8
                    minNavTop = this.visHeight - this.navHeight
                }

                this.minMapTop = minMapTop
                this.minNavTop = minNavTop

                this.getListData(totalScrollHeight)
                this.isScrolling = true
            },
            
            scrollPageByIndex (index) {
                let height = this.itemHeight * (index + 1)
                if (height <= 0) height = 0
                else if (height >= this.totalHeight - this.visHeight) height = this.totalHeight - this.visHeight
                this.minMapTop = height / (this.totalHeight - this.visHeight) * (this.mapHeight - this.visHeight / 8)
                this.minNavTop = height / (this.totalHeight - this.visHeight) * (this.visHeight - this.navHeight)
                this.getListData(height)
            },

            getListData (totalScrollHeight = this.totalScrollHeight, isResize = false, type = 'wheelGetData') {
                const postData = {
                    type,
                    totalScrollHeight,
                    isResize,
                    totalHeight: this.totalHeight,
                    itemHeight: this.itemHeight,
                    itemNumber: this.itemNumber,
                    canvasHeight: this.visHeight,
                    canvasWidth: this.visWidth / 10,
                    minMapTop: this.minMapTop,
                    mapHeight: this.mapHeight
                }
                this.worker.postMessage(postData)
            },

            initWorker () {
                this.worker = new Worker()
                this.worker.addEventListener('message', (event) => {
                    const data = event.data
                    switch (data.type) {
                        case 'completeInit':
                            this.totalNumber = data.number
                            this.foldList = data.foldList
                            this.setStatus()
                            this.initLink()
                            break
                        case 'wheelGetData':
                            this.drawList(data)
                            break
                        case 'completeFold':
                            const oldNumber = this.totalNumber
                            const oldItemNumber = this.itemNumber
                            const oldMapHeight = this.mapHeight
                            const oldVisHeight = this.visHeight
                            this.totalNumber = data.number
                            this.foldList = data.foldList
                            this.setStatus()
                            this.getNumberChangeList({ oldNumber, oldItemNumber, oldMapHeight, oldVisHeight })
                            break
                        case 'initLink':
                            this.drawList(data)
                            setTimeout(() => {
                                this.handleInitLink()
                            }, 0)
                            break
                    }
                })
            },

            getNumberChangeList ({ oldNumber, oldItemNumber, oldMapHeight, oldVisHeight }) {
                let minMapTop = this.minMapTop * (oldNumber - oldItemNumber) / ((oldMapHeight - oldVisHeight / 8) || 1) / ((this.totalNumber - this.itemNumber) || 1) * (this.mapHeight - this.visHeight / 8)
                let totalScrollHeight = minMapTop / (this.mapHeight - this.visHeight / 8) * (this.totalHeight - this.visHeight)
                if (minMapTop <= 0) {
                    minMapTop = 0
                    totalScrollHeight = 0
                } else if (minMapTop > this.mapHeight - this.visHeight / 8) {
                    minMapTop = this.mapHeight - this.visHeight / 8
                    totalScrollHeight = this.totalHeight - this.visHeight
                }
                this.minMapTop = minMapTop
                this.minNavTop = this.minMapTop * (this.visHeight - this.navHeight) / (this.mapHeight - this.visHeight / 8)
                this.getListData(totalScrollHeight)
            },

            handleInitLink () {
                const { bottomScrollDis, startShareIndex, endShareIndex, startOffset, endOffset, isStartFirst, isEndFirst } = this.$route.query
                this.bottomScrollDis = +bottomScrollDis || 0
                const list = document.querySelectorAll('.item-txt')
                const selection = window.getSelection()
                const range = document.createRange()
                const start = Array.from(list).find((x) => (x.parentNode.offsetTop === +startShareIndex))
                const end = Array.from(list).find((x) => (x.parentNode.offsetTop === +endShareIndex))
                if (!start || !end) return
                const startElement = start.children[+isStartFirst]
                const endElement = end.children[+isEndFirst]
                let startRange = +startOffset
                let endRange = +endOffset
                if (startRange > startElement.childNodes[0].length) startRange = startElement.childNodes[0].length
                if (endRange > endElement.childNodes[0].length) endRange = endElement.childNodes[0].length
                range.setStart(startElement.childNodes[0], startRange)
                range.setEnd(endElement.childNodes[0], endRange)
                selection.removeAllRanges()
                selection.addRange(range)
            },

            drawList (data) {
                Object.assign(this, data)
                const context = this.$refs.minMap.getContext('2d')
                context.clearRect(0, 0, this.visWidth / 10, this.visHeight)
                context.drawImage(data.offscreenBitMap, 0, 0)
                this.isScrolling = false
            },

            addListData (list, type, foldIndexs = []) {
                const postData = { type, list, foldIndexs }
                this.totalNumber += list.length
                this.indexWidth = (Math.log10(this.totalNumber) + 1) * 7
                list.forEach((item) => {
                    const width = this.mainWidth / (item.message.length * 6.8) * this.mainWidth
                    if (width < this.bottomScrollWidth && width < this.mainWidth) this.bottomScrollWidth = width
                })
                this.setStatus()
                this.worker.postMessage(postData)
            },

            setStatus () {
                this.totalHeight = this.totalNumber * this.itemHeight
                this.itemNumber = this.totalHeight > this.visHeight ? Math.ceil(this.visHeight / this.itemHeight) : this.totalNumber
                const heightRate = this.visHeight / this.totalHeight
                const minNavHeight = heightRate * this.visHeight
                this.navHeight = heightRate > 1 ? this.visHeight : (minNavHeight < 20 ? 20 : minNavHeight)
                const moveMaxHeight = this.totalNumber * this.itemHeight / 8
                this.mapHeight = moveMaxHeight < this.visHeight ? moveMaxHeight : this.visHeight
            },

            startBottomMove (event) {
                this.tempVal = event.screenX
                this.startMinMapMove = true
                this.isBottomMove = true
            },

            startNavMove (rate) {
                this.moveRate = rate
                this.tempVal = event.screenY
                this.startMinMapMove = true
            },

            minNavMove () {
                if (!this.startMinMapMove) return

                if (this.isBottomMove) {
                    const moveDis = event.screenX - this.tempVal
                    let bottomScrollLeft = this.bottomScrollDis + moveDis
                    if (bottomScrollLeft <= 0) bottomScrollLeft = 0
                    if (bottomScrollLeft + this.bottomScrollWidth >= this.mainWidth) bottomScrollLeft = this.mainWidth - this.bottomScrollWidth
                    this.bottomScrollDis = bottomScrollLeft
                    this.tempVal = event.screenX
                } else {
                    const moveDis = event.screenY - this.tempVal
                    let minMapTop = this.minMapTop + (moveDis / this.moveRate) * (this.mapHeight - this.visHeight / 8)
                    if (minMapTop <= 0) minMapTop = 0
                    if (minMapTop >= (this.mapHeight - this.visHeight / 8)) minMapTop = this.mapHeight - this.visHeight / 8

                    const totalScrollHeight = minMapTop / (this.mapHeight - this.visHeight / 8) * (this.totalHeight - this.visHeight)
                    this.tempVal = event.screenY
                    this.minMapTop = minMapTop
                    this.minNavTop = minMapTop * (this.visHeight - this.navHeight) / (this.mapHeight - this.visHeight / 8)
                    this.slowExec(() => {
                        this.getListData(totalScrollHeight)
                    })
                }
            },

            slowExec (callBack) {
                // 节流，限制触发频率
                const now = +new Date()
                if (now - (this.slowExec.lastTime || 0) >= 100) {
                    this.slowExec.lastTime = now
                    callBack()
                }

                // 保证最后一次能触发
                window.clearTimeout(this.slowExec.timeId)
                this.slowExec.timeId = window.setTimeout(() => {
                    callBack()
                }, 50)
            },
            
            moveEnd () {
                event.preventDefault()
                this.startMinMapMove = false
                this.isBottomMove = false
            },

            drawMinNav (searchList = []) {
                const context = this.$refs.minNav.getContext('2d')
                const width = this.visWidth / 100
                context.clearRect(0, 0, width, this.visHeight)
                context.lineWidth = 1
                context.fillStyle = 'rgba(255, 255, 255, 0.45)'
                context.strokeStyle = 'rgba(255, 255, 255, 0.3)'
                context.beginPath()
                context.moveTo(0, 0)
                searchList.forEach((item) => {
                    const y = item / this.totalNumber * this.visHeight
                    context.lineTo(0, y)
                    context.fillRect(0, y, width, 2)
                })
                context.lineTo(0, this.visHeight)
                context.stroke()
            }
        }
    }
</script>

<style lang="scss" scoped>
    ul, li {
        margin: 0;
        padding: 0;
        list-style: none;
    }
    .scroll-home {
        position: relative;
        height: 100%;
        overflow-y: hidden;
        .list-empty {
            position: absolute;
            background: url('./assets/png/empty.png') center no-repeat;
            background-size: contain;
            height: 80px;
            width: 80px;
            box-sizing: border-box;
            transform: translate(-50%, -50%);
            text-align: center;
            top: 45%;
            left: 50%;
            padding-top: 100px;
        }
        .scroll-index {
            text-align: right;
            user-select: none;
            li {
                width: 100%;
                color: rgba(166, 166, 166, 1)
            }
            .log-folder {
                background-image: url("./assets/png/down.png");
                display: inline-block;
                height: 16px;
                width: 16px;
                position: absolute;
                cursor: pointer;
                transform: rotate(0deg);
                transition: transform 200ms;
                top: 0;
                right: -20px;
                &.show-all {
                    transform: rotate(-90deg);
                }
            }
        }
        .scroll-main {
            overflow: hidden;
            margin-left: 20px;
            .pointer {
                cursor: pointer;
            }
            .scroll-item {
                min-width: 100%;
                &:hover {
                    background: #282828;
                }
            }
        }
        .scroll {
            position: absolute;
            will-change: transform;
            height: 1000000px;
            cursor: default;
            .scroll-item {
                box-sizing: border-box;
                position: absolute;
            }
        }
        .bottom-scroll {
            bottom: 0;
            height: 15px;
            background: rgba(121, 121, 121, 0.4);
        }
        .min-nav {
            position: absolute;
            right: 0;
            cursor: default;
            user-select: none;
            &:hover + span {
                background: rgba(121, 121, 121, 0.4);
            }
        }
        .min-nav-slide {
            position: absolute;
            transition: opacity .1s linear;
            will-change: transform;
            cursor: default;
            user-select: none;
            right: 0;
            &.nav-show {
                background: rgba(121, 121, 121, 0.4);
            }
            &:hover {
                background: rgba(121, 121, 121, 0.5);
            }
            &:active {
                background: rgba(121, 121, 121, 0.55);
            }
        }
    }
</style>
<style lang="scss">
    :root {
        --palette-primary-darken-6: rgba(0, 103, 181, 1);
        --palette-primary-darken-10: rgba(0, 91, 161, 1);
        --palette-primary-darkened-6: 0, 103, 181;
        --palette-primary-darkened-10: 0, 91, 161;
        --palette-primary-shade-30: 0, 69, 120;
        --palette-primary-shade-20: 0, 90, 158;
        --palette-primary-shade-10: 16, 110, 190;
        --palette-primary: 0, 120, 212;
        --palette-primary-tint-10: 43, 136, 216;
        --palette-primary-tint-20: 199, 224, 244;
        --palette-primary-tint-30: 222, 236, 249;
        --palette-primary-tint-40: 239, 246, 252;
        --palette-neutral-100: 0, 0, 0;
        --palette-neutral-80: 51, 51, 51;
        --palette-neutral-70: 76, 76, 76;
        --palette-neutral-60: 102, 102, 102;
        --palette-neutral-30: 166, 166, 166;
        --palette-neutral-20: 200, 200, 200;
        --palette-neutral-10: 218, 218, 218;
        --palette-neutral-8: 234, 234, 234;
        --palette-neutral-6: 239, 239, 239;
        --palette-neutral-4: 244, 244, 244;
        --palette-neutral-2: 248, 248, 248;
        --palette-neutral-0: 255, 255, 255;
        --palette-error: rgba(232, 17, 35, 1);
        --palette-error-6: rgba(203, 15, 31, 1);
        --palette-error-10: rgba(184, 14, 28, 1);
        --palette-black-alpha-0: rgba(var(--palette-neutral-100), 0);
        --palette-black-alpha-2: rgba(var(--palette-neutral-100), 0.02);
        --palette-black-alpha-4: rgba(var(--palette-neutral-100), 0.04);
        --palette-black-alpha-6: rgba(var(--palette-neutral-100), 0.06);
        --palette-black-alpha-8: rgba(var(--palette-neutral-100), 0.08);
        --palette-black-alpha-10: rgba(var(--palette-neutral-100), 0.10);
        --palette-black-alpha-20: rgba(var(--palette-neutral-100), 0.20);
        --palette-black-alpha-30: rgba(var(--palette-neutral-100), 0.30);
        --palette-black-alpha-60: rgba(var(--palette-neutral-100), 0.60);
        --palette-black-alpha-70: rgba(var(--palette-neutral-100), 0.70);
        --palette-black-alpha-80: rgba(var(--palette-neutral-100), 0.80);
        --palette-black-alpha-100: rgba(var(--palette-neutral-100), 1);
        --palette-accent1-light: 249, 235, 235; --palette-accent1: 218, 10, 0;
        --palette-accent1-dark: 168, 0, 0;
        --palette-accent2-light: 223, 246, 221;
        --palette-accent2: 186, 216, 10;
        --palette-accent2-dark: 16, 124, 16;
        --palette-accent3-light: 255, 244, 206;
        --palette-accent3: 248, 168, 0;
        --palette-accent3-dark: 220, 182, 122;
        --background-color: rgba(var(--palette-neutral-0), 1);
        --communication-foreground: rgba(var(--palette-primary-shade-20), 1);
        --communication-background: rgba(var(--palette-primary), 1);
        --status-info-foreground: rgba(0, 120, 212, 1);
        --status-info-background: rgba(0, 120, 212, 1);
        --status-error-foreground: rgba(205, 74, 69, 1);
        --status-error-background: rgba(var(--palette-accent1-light), 1);
        --status-error-text: rgba(var(--palette-accent1), 1);
        --status-error-strong: rgba(var(--palette-accent1-dark), 1);
        --status-success-foreground: rgba(var(--palette-accent2-dark), 1);
        --status-success-background: rgba(var(--palette-accent2-light), 1);
        --status-warning-foreground: rgba(250, 157, 45, 1);
        --status-warning-background: rgba(var(--palette-accent3-light), 1);
        --text-primary-color: rgba(var(--palette-neutral-100), .9);
        --text-secondary-color: rgba(var(--palette-neutral-100), .55);
        --text-disabled-color: rgba(var(--palette-neutral-100), .38);
        --text-on-communication-background: var(--background-color);
        --border-subtle-color: rgba(var(--palette-neutral-100), .08);
        --callout-background-color: var(--background-color);
        --callout-filtered-background-color: rgba(var(--palette-neutral-0), 0.86);
        --callout-shadow-color: rgba(0, 0, 0, .132);
        --callout-shadow-secondary-color: rgba(0, 0, 0, .108);
        --panel-shadow-color: rgba(0, 0, 0, .22);
        --panel-shadow-secondary-color: rgba(0, 0, 0, .18);
        --focus-pulse-max-color: rgba(var(--palette-primary), 0.35);
        --focus-pulse-min-color: rgba(var(--palette-primary), 0.15);
        --third-party-icon-filter: none;
        --diff-color-original: rgba(172, 0, 0, 0.1);
        --diff-color-modified: rgba(51, 153, 51, 0.1);
        --component-label-default-color: rgba(var(--palette-neutral-6), 1);
        --component-label-default-color-hover: rgba(var(--palette-neutral-10), 1);
        --component-grid-row-hover-color: rgba(var(--palette-neutral-2), 1);
        --component-grid-selected-row-color: rgba(var(--palette-primary-tint-30), 1);
        --component-grid-focus-border-color: rgba(var(--palette-primary), 1);
        --component-grid-link-selected-row-color: rgba(var(--palette-primary-shade-10), 1);
        --component-grid-link-hover-color: rgba(var(--palette-primary-shade-20), 1);
        --component-grid-action-hover-color: rgba(var(--palette-neutral-8), 1);
        --component-grid-action-selected-cell-hover-color: rgba(var(--palette-primary-tint-30), 1);
        --component-grid-cell-bottom-border-color: rgba(var(--palette-neutral-8), 1);
        --component-grid-drag-source-color: rgba(var(--palette-neutral-0), 0.40);
        --search-match-background: rgba(255, 255, 0, 0.6);
        --search-selected-match-background: rgba(245, 139, 31, 0.8);
        --icon-folder-color: #dcb67a;
        --component-errorBoundary-border-color: rgba(var(--palette-accent1), 1);
        --component-errorBoundary-background-color: rgba(var(--palette-accent1-light), 1);
        --nav-header-background: var(--background-color);
        --nav-header-item-hover-background: rgba(var(--palette-neutral-100), 0.02);
        --nav-header-active-item-background: rgba(var(--palette-neutral-100), 0.08);
        --nav-header-text-primary-color: var(--text-primary-color);
        --nav-header-text-secondary-color: var(--text-secondary-color);
        --nav-header-text-disabled-color: var(--text-disabled-color);
        --nav-header-product-color: rgba(var(--palette-primary), 1);
        --nav-vertical-background-color: rgba(var(--palette-neutral-8), 1);
        --nav-vertical-item-hover-background: rgba(var(--palette-neutral-100), 0.04);
        --nav-vertical-active-group-background: rgba(var(--palette-neutral-100), 0.06);
        --nav-vertical-active-item-background: rgba(var(--palette-neutral-100), 0.12);
        --nav-vertical-text-primary-color: var(--text-primary-color);
        --nav-vertical-text-secondary-color: var(--text-secondary-color);
        --component-menu-selected-item-background: rgba(var(--palette-neutral-4), 1);
        --component-htmlEditor-background-color: var(--background-color);
        --component-htmlEditor-foreground-color: var(--text-primary-color)
    }
</style>
