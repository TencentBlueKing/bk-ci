<template>
    <article class="log-home" v-if="show">
        <section class="log-main">
            <header class="log-head">
                <span class="log-title"><status-icon :status="status"></status-icon>{{ title }}</span>
                <p class="log-buttons">
                    <bk-button class="log-button" @click="showTime=!showTime">显示时间</bk-button>
                    <bk-button class="log-button">下载日志</bk-button>
                </p>
            </header>

            <virtual-scroll class="log-scroll" ref="scroll" v-bkloading="{ isLoading: isInit }" :id="id" v-for="plugin in pluginList" :key="plugin.id">
                <template slot-scope="item">
                    <span class="item-txt selection-color"
                        v-if="!isInit"
                    >
                        <span class="item-time selection-color">{{(showTime ? item.data.timestamp : '')|timeFilter}}</span>
                        <span class="selection-color" :style="`color: ${item.data.color};font-weight: ${item.data.fontWeight}`">{{item.data.value || ''}}</span>
                    </span>
                </template>
            </virtual-scroll>

            <span class="share-icon" @mouseup="copyLink" ref="shareIcon"></span>
        </section>
    </article>
</template>

<script>
    import virtualScroll from './virtualScroll'
    import statusIcon from './status'

    export default {
        components: {
            virtualScroll,
            statusIcon
        },

        props: {
            pluginList: {
                type: Array
            },
            show: {
                type: Boolean,
                default: false
            },
            isInit: {
                type: Boolean,
                default: false
            },
            status: {
                type: String
            },
            title: {
                type: String
            },
            id: {
                type: String,
                default: null
            },
            linkUrl: {
                type: String
            }
        },

        filters: {
            timeFilter (val) {
                if (!val) return ''
                const time = new Date(val)
                return `${time.getFullYear()}-${time.getMonth() + 1}-${time.getDate()} ${time.getHours()}:${time.getMinutes()}:${time.getSeconds()}`
            }
        },

        data () {
            return {
                searchResult: [],
                showSearchIndex: 0,
                startShareIndex: -1,
                endShareIndex: -1,
                startOffset: 0,
                endOffset: 0,
                showShareIcon: false,
                isShareMove: false,
                showTime: this.$route.query.showTime === 'true',
                offsetLeft: 0,
                offsetTop: 0
            }
        },

        mounted () {
            document.addEventListener('mousedown', this.startShare)
            document.addEventListener('mousemove', this.shareMove)
            document.addEventListener('mouseup', this.showShare)

            const mainEle = document.querySelector('.log-main')
            this.offsetLeft = mainEle.offsetLeft
            this.offsetTop = mainEle.offsetTop
        },

        beforeDestroy() {
            document.removeEventListener('mousedown', this.startShare)
            document.removeEventListener('mousemove', this.shareMove)
            document.removeEventListener('mouseup', this.showShare)
        },

        methods: {
            shareMove (event) {
                if (!this.isShareMove) return
                let curTarget = event.target
                if (!curTarget.classList.contains('item-txt')) curTarget = curTarget.parentNode
                if (curTarget.classList.contains('item-txt')) {
                    const top = curTarget.parentNode.style.top.slice(0, -2)
                    if (this.startShareIndex === -1) this.startShareIndex = top
                    this.endShareIndex = top
                }
            },

            startShare (event) {
                let curTarget = event.target
                if (curTarget === this.$refs.shareIcon) return
                if (curTarget.classList.contains('log-home')) this.$emit('closeLog')
                const selection = document.getSelection()
                selection.removeAllRanges()
                const eleShare = document.querySelector('.share-icon')
                eleShare.style.display = "none"
                this.isShareMove = true
                if (!curTarget.classList.contains('item-txt')) curTarget = curTarget.parentNode
                if (curTarget.classList.contains('item-txt')) this.startShareIndex = curTarget.parentNode.style.top.slice(0, -2)
            },

            showShare (event) {
                this.isShareMove = false
                const eleShare = document.querySelector('.share-icon')
                const scroll = this.$refs.scroll
                const selection = window.getSelection()
                const txt = selection.toString()
                if (txt) {
                    const left = event.clientX - this.offsetLeft
                    const top = event.clientY - this.offsetTop
                    eleShare.style.display = "inline"
                    eleShare.style.left = left + "px"
                    eleShare.style.top = top + "px"
                }
            },

            copyLink (event) {
                const { minMapTop, bottomScrollDis, flodIndexs } = this.$refs.scroll
                const eleShare = document.querySelector('.share-icon')
                const selection = window.getSelection()
                const startParentNode = selection.anchorNode.parentNode
                const endParentNode = selection.focusNode.parentNode

                eleShare.style.display = "none"
                let startShareIndex = +this.startShareIndex
                let endShareIndex = +this.endShareIndex
                let startOffset = selection.anchorOffset
                let endOffset = selection.focusOffset
                let isStartFirst = startParentNode.classList.contains('item-time') ? 0 : 1
                let isEndFirst = endParentNode.classList.contains('item-time') ? 0 : 1

                function changeTemp () {
                    let temp = startOffset
                    startOffset = endOffset
                    endOffset = temp
                    temp = isStartFirst
                    isStartFirst = isEndFirst
                    isEndFirst = temp
                    temp = startShareIndex
                    startShareIndex = endShareIndex
                    endShareIndex = temp
                }
                if (startShareIndex > endShareIndex) changeTemp()
                if (startShareIndex === endShareIndex && startOffset > endOffset) changeTemp()


                const url = this.getLinkUrl({ showLog: true, isStartFirst, isEndFirst, id: this.id || '', startShareIndex, endShareIndex, startOffset, endOffset, minMapTop, bottomScrollDis, showTime: this.showTime, flodIndexs })
                const input = document.createElement('input')
                document.body.appendChild(input)
                input.setAttribute('value', url)
                input.select()
                if (document.execCommand('copy')) {
                    document.execCommand('copy')
                    this.$bkMessage({ theme: 'success', message: '复制链接成功' })
                }
                document.body.removeChild(input)
            },

            getLinkUrl (params = {}) {
                const keys = Object.keys(params)
                const urlArr = this.linkUrl.split('?')
                const firstEle = keys.shift()
                let url = `${urlArr[0]}?${urlArr[1] ? (urlArr[1] + '&') : ''}${firstEle}=${params[firstEle]}`
                keys.forEach((key) => {
                    url = `${url}&${key}=${params[key]}`
                })
                return url
            },

            addLogData(data, isInit) {
                const type = isInit ? 'initLog' : 'addListData'
                const foldParam = this.$route.query.flodIndexs
                const id = this.$route.query.id
                let foldArr = []
                if (typeof foldParam !== 'undefined' && foldParam && id === this.id) foldArr = foldParam.split(',')
                const scroll = this.$refs.scroll
                const lastIndex = scroll.indexList[scroll.indexList.length] || {}
                const isBottom = +lastIndex.value === +scroll.totalNumber
                scroll.addListData(data, type, foldArr.map(x => +x))
                if (isInit || !isBottom) {
                    this.$refs.scroll.getListData()
                } else {
                    this.scrollPage(scroll.totalNumber - scroll.itemNumber)
                }
            },

            scrollPage (index) {
                if (index < 0) this.showSearchIndex = this.searchResult.length - 1
                else if (index < this.searchResult.length) this.showSearchIndex = index
                else this.showSearchIndex = 0
                const listIndex = this.searchResult[this.showSearchIndex]
                this.$refs.scroll.scrollPageByIndex(listIndex)
            }
        }
    }
</script>

<style lang="scss" scoped>
    .share-icon {
        position: absolute;
        display: none;
        cursor: pointer;
        user-select: none;
        width: 34px;
        height: 34px;
        border-radius: 4px;
        background-color: rgba(255, 255, 255, 1);
        background-image: url('./assets/png/link.png');
        background-size: 20px;
        background-position: center;
        background-repeat: no-repeat;
        opacity: 0.8;
        &:hover {
            opacity: 1;
        }
    }
    .log-home {
        position: fixed;
        top: 0;
        left: 0;
        bottom: 0;
        right: 0;
        background-color: rgba(0, 0, 0, .2);
        z-index: 1000;
        .scroll-loading {
            position: absolute;
            bottom: 0;
            width: 100%;
            height: 16px;
        }
        .log-main {
            position: relative;
            width: 75%;
            height: calc(100% - 32px);
            float: right;
            display: flex;
            flex-direction: column;
            margin: 16px;
            border-radius: 6px;
            overflow: hidden;
            transition-property: transform, opacity;
            transition: transform 200ms cubic-bezier(.165,.84,.44,1), opacity 100ms cubic-bezier(.215,.61,.355,1);
            background: black;
            /deep/ .bk-loading {
                background: black !important;
            }
            .log-head {
                line-height: 52px;
                margin: 16px 20px;
                border-bottom: 2px solid;
                border-bottom-color: rgba(102,102,102,1);
                display: flex;
                align-items: center;
                justify-content: space-between;
                color: #d4d4d4;
                .log-buttons {
                    display: flex;
                    align-items: center;
                    .log-button {
                        color: #c2cade;
                        background: #2f363d;
                        border-color: #444d56;
                        margin-right: 10px;
                        &:hover {
                            color: #fff;
                        }
                    }
                }
                .log-title {
                    display: flex;
                    align-items: center;
                }
                .log-search {
                    display: flex;
                    align-items: center;
                    background-color: #2d2d30;
                    line-height: 30px;
                    padding: 0 20px;
                    font-size: 14px;
                    .search-input {
                        width: 200px;
                    }
                    /deep/ .bk-form-input {
                        border: 1px solid transparent;
                        background-color: rgb(60, 60, 60)!important;
                        color: rgb(204, 204, 204);
                        margin: 5px 0;
                        height: 24px;
                        width: 200px;
                    }
                    .search-summary {
                        padding: 0 5px;
                        font-size: 12px;
                    }
                    .bk-icon {
                        margin-left: 5px;
                        cursor: pointer;
                        user-select: none;
                    }
                }
            }
        }
    }
    .log-scroll {
        flex: 1;
        color: #ffffff;
        font-family: Consolas, "Courier New", monospace;
        font-weight: normal;
        cursor: text;
        white-space: nowrap;
        letter-spacing: 0px;
        font-size: 12px;
        line-height: 16px;
        margin-left: 10px;
        .item-txt {
            position: relative;
            padding: 0 5px;
        }
        .item-time {
            color: #959da5;
            font-weight: 400;
            padding-right: 5px;
        }
        /deep/ .selection-color {
            &::selection {
                background-color: rgba(70, 146, 222, 0.54);
            }
            &::-moz-selection {
                background: rgba(70, 146, 222, 0.54);
            }
            &::-webkit-selection {
                background: rgba(70, 146, 222, 0.54);
            }
        }
    }
</style>
