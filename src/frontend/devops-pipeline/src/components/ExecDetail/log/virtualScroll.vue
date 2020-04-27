<template>
    <section :class="['scroll-home', `id-${id}`, { 'min-height': totalNumber <= 0, 'show-empty': hasCompleteInit }]" :style="`height: ${visHeight}px`" @mousewheel.prevent="handleWheel" @DOMMouseScroll.prevent="handleWheel">
        <template v-if="!isLogErr">
            <ul class="scroll-index scroll" :style="`top: ${-totalScrollHeight}px; width: ${indexWidth}px; height: ${ulHeight}px`">
                <li class="scroll-item" :style="`height: ${itemHeight}px; top: ${item.top}px`" v-for="(item) in indexList" :key="item">
                    {{item.isNewLine ? '' : item.value}}
                    <span :class="[{ 'show-all': item.hasFolded }, 'log-folder']" v-if="item.isFold" @click="foldListData(item.index, item.isFold)"></span>
                </li>
            </ul>
            <ul class="scroll scroll-main" :style="`height: ${ulHeight}px; top: ${-totalScrollHeight}px ;width: ${mainWidth}px; left: ${indexWidth}px`">
                <li :class="[{ 'pointer': item.isFold, hover: item.showIndex === curHoverIndex }, 'scroll-item']"
                    @mouseenter="curHoverIndex = item.showIndex"
                    @mouseleave="curHoverIndex = -1"
                    :style="`height: ${itemHeight}px; top: ${item.top}px; left: ${-bottomScrollDis * (itemWidth - mainWidth) / (mainWidth - bottomScrollWidth) }px;`"
                    v-for="item in listData"
                    :key="item.top + item.value"
                    @click="foldListData(item.index, item.isFold)"
                ><slot :data="item"></slot>
                </li>
            </ul>
            <span v-if="itemHeight * totalNumber > visHeight" class="min-nav min-map" :style="`height: ${visHeight}px; right: ${visWidth * 11 / 100}px`"></span>
            <canvas v-show="itemHeight * totalNumber > visHeight" class="min-nav no-scroll" :style="`height: ${visHeight}px; width: ${visWidth / 10}px;right: ${visWidth / 100}px`" ref="minMap" @click="changeMinMap"></canvas>
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
        </template>

        <p class="list-empty" v-if="isLogErr || (hasCompleteInit && totalNumber <= 0)">{{ errMessage }}</p>
        <section class="log-loading" v-if="!hasCompleteInit">
            <div class="lds-ring"><div></div><div></div><div></div><div></div></div>
        </section>
    </section>
</template>

<script>
    export default {
        props: {
            itemHeight: {
                type: Number,
                default: 16
            },
            id: {
                type: String
            },
            worker: {
                type: Object
            }
        },

        data () {
            return {
                ulHeight: 0,
                indexList: [],
                listData: [],
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
                itemWidth: 0,
                isScrolling: false,
                isBottomMove: false,
                curHoverIndex: -1,
                hasCompleteInit: false,
                errMessage: this.$t('execDetail.emptyLog'),
                isLogErr: false
            }
        },

        computed: {
            mainWidth () {
                return this.visWidth * 89 / 100 - this.indexWidth - 20
            }
        },

        mounted () {
            this.setVisWidth()
            this.initEvent()
            this.initWorker()
        },

        beforeDestroy () {
            document.removeEventListener('mousedown', this.clearSelection)
            document.removeEventListener('mousemove', this.minNavMove)
            document.removeEventListener('mouseup', this.moveEnd)
            window.removeEventListener('resize', this.resize)
            window.removeEventListener('keydown', this.quickHorizontalMove)
        },

        methods: {
            clearSelection () {
                window.getSelection().removeAllRanges()
            },

            setVisWidth () {
                const mainEle = document.querySelector(`.id-${this.id}`)
                this.visWidth = mainEle.offsetWidth
                let visHeight = mainEle.offsetHeight
                const pluListEle = document.querySelector('.job-plugin-list-log')
                if (pluListEle) visHeight = (pluListEle.offsetHeight || 500) - 80
                this.maxVisHeight = visHeight
            },

            handleApiErr (errMessage) {
                this.hasCompleteInit = true
                this.isLogErr = true
                this.errMessage = errMessage
            },

            resetData () {
                this.totalNumber = 0
                this.hasCompleteInit = false
                this.indexList = []
                this.listData = []
                this.changeStatus()
                this.worker.postMessage({ type: 'resetData', id: this.id })
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

            foldListData (startIndex, isFold) {
                if (isFold) {
                    const postData = {
                        id: this.id,
                        type: 'foldListData',
                        startIndex
                    }
                    this.worker.postMessage(postData)
                }
            },

            changeStatus () {
                this.totalHeight = this.totalNumber * this.itemHeight
                const visHeight = this.totalHeight > this.maxVisHeight ? this.maxVisHeight : this.totalHeight
                if (this.visHeight !== visHeight) {
                    this.visHeight = visHeight
                    const dpr = window.devicePixelRatio || 1
                    this.$refs.minMap.width = this.visWidth / 10 * dpr
                    this.$refs.minMap.height = this.visHeight * dpr
                    this.$refs.minMap.getContext('2d').setTransform(dpr, 0, 0, dpr, 0, 0)
                    this.$refs.minNav.width = this.visWidth / 100 * dpr
                    this.$refs.minNav.height = this.visHeight * dpr
                    this.$refs.minNav.getContext('2d').setTransform(dpr, 0, 0, dpr, 0, 0)
                }
                this.itemNumber = this.totalHeight > this.visHeight ? Math.ceil(this.visHeight / this.itemHeight) : this.totalNumber
                this.ulHeight = this.totalHeight > 400000 ? 1000000 : this.totalHeight
                const heightRate = this.visHeight / this.totalHeight
                const minNavHeight = heightRate * this.visHeight
                this.navHeight = heightRate > 1 ? this.visHeight : (minNavHeight < 20 ? 20 : minNavHeight)
                const moveMaxHeight = this.totalNumber * this.itemHeight / 8
                this.mapHeight = moveMaxHeight < this.visHeight ? moveMaxHeight : this.visHeight
            },

            initEvent () {
                document.addEventListener('mousedown', this.clearSelection)
                document.addEventListener('mousemove', this.minNavMove)
                document.addEventListener('mouseup', this.moveEnd)
                window.addEventListener('resize', this.resize)
                window.addEventListener('keydown', this.quickHorizontalMove)
            },

            resize (event) {
                this.slowExec(() => {
                    const lastHeight = this.visHeight
                    this.setVisWidth()
                    this.changeStatus()
                    this.minMapTop = this.visHeight / lastHeight * this.minMapTop
                    this.minNavTop = this.minMapTop * (this.visHeight - this.navHeight) / (this.mapHeight - this.visHeight / 8)

                    this.totalScrollHeight = this.minMapTop / (this.mapHeight - this.visHeight / 8) * (this.totalHeight - this.visHeight)
                    this.getListData()
                })
            },

            quickHorizontalMove (event) {
                if (['ArrowLeft', 'ArrowRight'].includes(event.code)) {
                    let wheelDeltaX = -1
                    if (event.code === 'ArrowLeft') wheelDeltaX = 1
                    this.handleHorizontalScroll({ wheelDeltaX })
                }
            },

            handleWheel (event) {
                const target = event.target
                const classList = target.classList
                if (this.isScrolling || (classList && classList.contains('no-scroll'))) return

                const isVerticalScroll = event.wheelDeltaX !== undefined ? Math.abs(event.wheelDeltaY) > Math.abs(event.wheelDeltaX) : event.axis === 2
                if (isVerticalScroll) this.handleVerticalScroll(event)
                else this.handleHorizontalScroll(event)
            },

            handleHorizontalScroll (event) {
                event.preventDefault()
                if (this.bottomScrollWidth >= this.mainWidth) return

                const deltaX = -Math.max(-1, Math.min(1, (event.wheelDeltaX || -event.detail)))
                let bottomScrollLeft = this.bottomScrollDis + deltaX * 4
                if (bottomScrollLeft <= 0) bottomScrollLeft = 0
                if (bottomScrollLeft + this.bottomScrollWidth >= this.mainWidth) bottomScrollLeft = this.mainWidth - this.bottomScrollWidth
                this.bottomScrollDis = bottomScrollLeft
            },

            handleVerticalScroll (event) {
                const deltaY = Math.max(-1, Math.min(1, (event.wheelDeltaY || -event.detail)))
                const firstIndex = this.indexList[0] || {}
                const lastIndex = this.indexList[this.indexList.length - 1] || {}
                const scrollEle = this.$el.parentElement.parentElement || {}
                const downPreDefault = lastIndex.listIndex + 1 < this.totalNumber || scrollEle.scrollTop + scrollEle.offsetHeight >= scrollEle.scrollHeight
                const upPreDefault = firstIndex.listIndex > 0 || scrollEle.scrollTop <= 0
                const shouldPreDefault = deltaY < 0 ? downPreDefault : upPreDefault
                if (!shouldPreDefault) scrollEle.scrollTop += deltaY * -80

                if (this.itemHeight * this.totalNumber <= this.visHeight) return
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
                let height = this.itemHeight * index
                if (height <= 0) height = 0
                else if (height >= this.totalHeight - this.visHeight) height = this.totalHeight - this.visHeight
                if (this.totalHeight <= this.visHeight) height = 0
                const heightDiff = (this.totalHeight - this.visHeight) || 1
                this.minMapTop = height / heightDiff * (this.mapHeight - this.visHeight / 8)
                this.minNavTop = height / heightDiff * (this.visHeight - this.navHeight)
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
                    mapHeight: this.mapHeight,
                    id: this.id
                }
                this.worker.postMessage(postData)
            },

            initWorker () {
                this.worker.addEventListener('message', (event) => {
                    const data = event.data
                    if (data.id !== this.id) return
                    switch (data.type) {
                        case 'completeInit':
                            this.freshDataScrollBottom(data)
                            this.hasCompleteInit = true
                            break
                        case 'completeAdd':
                            const lastIndexData = this.indexList[this.indexList.length - 1] || { listIndex: 0 }
                            if (this.totalNumber - lastIndexData.listIndex <= 3) {
                                this.freshDataScrollBottom(data)
                            } else {
                                this.freshDataNoScroll(data)
                            }
                            break
                        case 'wheelGetData':
                            this.drawList(data)
                            break
                        case 'completeFold':
                            this.freshDataNoScroll(data)
                            break
                    }
                })
            },

            freshDataScrollBottom (data) {
                this.totalNumber = data.number
                this.indexWidth = (Math.log10(this.totalNumber) + 1) * 7
                this.changeStatus()
                this.scrollPageByIndex(this.totalNumber - this.itemNumber + 1)
            },

            freshDataNoScroll (data) {
                const oldNumber = this.totalNumber
                const oldItemNumber = this.itemNumber
                const oldMapHeight = this.mapHeight
                const oldVisHeight = this.visHeight
                this.totalNumber = data.number
                this.indexWidth = (Math.log10(this.totalNumber) + 1) * 7
                this.changeStatus()
                this.getNumberChangeList({ oldNumber, oldItemNumber, oldMapHeight, oldVisHeight })
            },

            getNumberChangeList ({ oldNumber, oldItemNumber, oldMapHeight, oldVisHeight }) {
                let minMapTop = this.minMapTop * (oldNumber - oldItemNumber) / ((oldMapHeight - oldVisHeight / 8) || 1) / ((this.totalNumber - this.itemNumber) || 1) * (this.mapHeight - this.visHeight / 8)
                let totalScrollHeight = minMapTop / ((this.mapHeight - this.visHeight / 8) || 1) * (this.totalHeight - this.visHeight)
                if (minMapTop <= 0) {
                    minMapTop = 0
                    totalScrollHeight = 0
                } else if (minMapTop > this.mapHeight - this.visHeight / 8) {
                    minMapTop = this.mapHeight - this.visHeight / 8
                    totalScrollHeight = this.totalHeight - this.visHeight
                }
                this.minMapTop = minMapTop
                this.minNavTop = this.minMapTop * (this.visHeight - this.navHeight) / ((this.mapHeight - this.visHeight / 8) || 1)
                this.getListData(totalScrollHeight)
            },

            drawList (data) {
                Object.assign(this, data)
                const minMapList = data.minMapList || []
                const canvasContext = this.$refs.minMap.getContext('2d')
                canvasContext.clearRect(0, 0, this.visWidth / 10, this.visHeight)
                for (let index = 0; index < minMapList.length; index++) {
                    const currentItem = minMapList[index]
                    const currentColor = currentItem.color || 'rgba(255,255,255,1)'
                    if (currentItem.color) canvasContext.font = `normal normal bold 2px Consolas`
                    else canvasContext.font = `normal normal normal 2px Consolas`
                    canvasContext.fillStyle = currentColor
                    canvasContext.fillText(currentItem.message, 5, ((index + 1) * 2))
                }
                this.isScrolling = false
            },

            addLogData (list) {
                const type = this.hasCompleteInit ? 'addListData' : 'initLog'
                const postData = { type, list, mainWidth: this.mainWidth, id: this.id }
                this.worker.postMessage(postData)
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
            }
        }
    }
</script>

<style lang="scss" scoped>
    .log-loading {
        position: absolute;
        top: 50%;
        transform: translateY(-50%);
        width: 100%;
        background: #1e1e1e;
        z-index: 100;
        .lds-ring {
            display: inline-block;
            position: relative;
            width: 80px;
            height: 80px;
            left: 50%;
            transform: translateX(-50%);
        }
        .lds-ring div {
            box-sizing: border-box;
            display: block;
            position: absolute;
            width: 37px;
            height: 37px;
            border: 3px solid #fff;
            border-radius: 50%;
            animation: lds-ring 1.2s cubic-bezier(0.5, 0, 0.5, 1) infinite;
            border-color: #fff transparent transparent transparent;
        }
        .lds-ring div:nth-child(1) {
            animation-delay: -0.45s;
        }
        .lds-ring div:nth-child(2) {
            animation-delay: -0.3s;
        }
        .lds-ring div:nth-child(3) {
            animation-delay: -0.15s;
        }
        @keyframes lds-ring {
            0% {
                transform: rotate(0deg);
            }
            100% {
                transform: rotate(360deg);
            }
        }
    }

    ul, li {
        margin: 0;
        padding: 0;
        list-style: none;
    }
    .scroll-home {
        position: relative;
        height: 100%;
        overflow-y: hidden;
        &.min-height {
            min-height: 20px;
            &.show-empty {
                min-height: 110px;
            }
        }
        .list-empty {
            position: absolute;
            background: url('../../../images/empty.png') center no-repeat;
            background-size: contain;
            height: 80px;
            width: 220px;
            box-sizing: border-box;
            transform: translate(-50%, -50%);
            text-align: center;
            top: 45%;
            left: 50%;
            padding-top: 80px;
            line-height: 21px;
        }
        .scroll-index {
            text-align: right;
            user-select: none;
            li {
                width: 100%;
                color: rgba(166, 166, 166, 1)
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
                &.hover {
                    background: #333030;
                }
            }
        }
        .scroll {
            position: absolute;
            will-change: transform;
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
        .min-map {
            width: 6px;
            box-shadow: #000000 -6px 0 6px -6px inset;
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
