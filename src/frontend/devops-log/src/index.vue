<template>
    <article class="log-home">
        <section class="log-main">
            <header class="log-head">
                <span class="log-title"><status-icon :status="status"></status-icon>{{ title }}</span>
                <p class="log-buttons">
                    <bk-select v-if="![0, 1].includes(+executeCount)" placeholder="重试次数" class="log-execute">
                        <bk-option v-for="execute in executeCount"
                            :key="execute"
                            :id="execute"
                            :name="execute"
                            @click.native="changeExecute(execute)"
                        >
                        </bk-option>
                    </bk-select>
                    <button class="log-button" @click="showTime = !showTime">显示时间</button>
                    <button class="log-button" @click="downLoad">下载日志</button>
                </p>
            </header>

            <virtual-scroll class="log-scroll" ref="scroll" :id="id">
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

            <section class="log-loading" v-if="isInit">
                <div class="lds-ring"><div></div><div></div><div></div><div></div></div>
            </section>
        </section>
    </article>
</template>

<script>
    import virtualScroll from './virtualScroll'
    import statusIcon from './status'

    function prezero (num) {
        num = Number(num)
        if (num < 10) return '0' + num
        return num
    }

    function millisecond (num) {
        num = Number(num)
        if (num < 10) return '00' + num
        else if (num < 100) return '0' + num
        return num
    }

    export default {
        components: {
            virtualScroll,
            statusIcon
        },

        filters: {
            timeFilter (val) {
                if (!val) return ''
                const time = new Date(val)
                return `${time.getFullYear()}-${prezero(time.getMonth() + 1)}-${prezero(time.getDate())} ${prezero(time.getHours())}:${prezero(time.getMinutes())}:${prezero(time.getSeconds())}:${millisecond(time.getMilliseconds())}`
            }
        },

        props: {
            isInit: {
                type: Boolean,
                default: false
            },
            downLoadLink: {
                type: String
            },
            downLoadName: {
                type: String
            },
            executeCount: {
                type: Number,
                default: 0
            },
            logType: {
                type: String
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
                offsetTop: 0,
                currentExe: 1,
                completeInit: false
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

        beforeDestroy () {
            document.removeEventListener('mousedown', this.startShare)
            document.removeEventListener('mousemove', this.shareMove)
            document.removeEventListener('mouseup', this.showShare)
        },

        methods: {
            downLoad () {
                fetch(this.downLoadLink, {
                    method: 'GET',
                    headers: {
                        'content-type': 'application/json'
                    },
                    credentials: 'include'
                }).then((res) => {
                    if (res.status >= 200 && res.status < 300) {
                        return res
                    } else {
                        throw new Error(res.statusText)
                    }
                }).then(res => res.blob()).then((blob) => {
                    const a = document.createElement('a')
                    const url = window.URL || window.webkitURL || window.moxURL
                    a.href = url.createObjectURL(blob)
                    a.download = this.downLoadName + '.log'
                    document.body.appendChild(a)
                    a.click()
                    document.body.removeChild(a)
                }).catch((err) => {
                    console.error(err.message || err)
                }).finally(() => {
                    this.fileLoadPending = false
                })
            },

            changeExecute (execute) {
                if (this.currentExe === execute) return
                this.currentExe = execute
                this.$refs.scroll.resetData()
                this.$emit('changeExecute', execute)
            },

            shareMove (event) {
                if (!this.isShareMove) return
                let curTarget = event.target
                if (curTarget.classList.contains('selection-color') && !curTarget.classList.contains('item-txt')) curTarget = curTarget.parentNode.parentNode
                if (curTarget.classList.contains('item-txt')) curTarget = curTarget.parentNode
                if (curTarget.className && curTarget.classList.contains('scroll-item')) {
                    const top = curTarget.style.top.slice(0, -2)
                    if (this.startShareIndex === -1) this.startShareIndex = top
                    this.endShareIndex = top
                }
            },

            startShare (event) {
                const time = new Date()
                if (time - this.startShare.time < 350) {
                    event.preventDefault()
                }
                this.startShare.time = time

                let curTarget = event.target
                if (curTarget === this.$refs.shareIcon) return
                if (curTarget.classList.contains('log-home')) this.$emit('closeLog')
                const selection = document.getSelection()
                selection.removeAllRanges()
                const eleShare = document.querySelector('.share-icon')
                eleShare.style.display = 'none'
                this.isShareMove = true
                if (!curTarget.classList.contains('item-txt')) curTarget = curTarget.parentNode
                if (curTarget.classList.contains('item-txt')) this.startShareIndex = curTarget.parentNode.style.top.slice(0, -2)
            },

            showShare (event) {
                this.isShareMove = false
                const eleShare = document.querySelector('.share-icon')
                const selection = window.getSelection()
                const txt = selection.toString()
                if (txt && this.completeInit) {
                    const left = event.clientX - this.offsetLeft + 15
                    const top = event.clientY - this.offsetTop + 15
                    eleShare.style.display = 'inline'
                    eleShare.style.left = left + 'px'
                    eleShare.style.top = top + 'px'
                }
            },

            copyLink (event) {
                const { minMapTop, bottomScrollDis, foldList } = this.$refs.scroll
                const eleShare = document.querySelector('.share-icon')
                const selection = window.getSelection()
                const startParentNode = selection.anchorNode.parentNode
                const endParentNode = selection.focusNode.parentNode

                eleShare.style.display = 'none'
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

                const flodIndexs = []
                foldList.forEach((x) => {
                    const currentData = x.data.tagData
                    if (currentData.list.length) flodIndexs.push(x.index)
                })

                const url = this.getLinkUrl({
                    isStartFirst,
                    isEndFirst,
                    id: this.id || '',
                    startShareIndex,
                    endShareIndex,
                    startOffset,
                    endOffset,
                    minMapTop,
                    bottomScrollDis,
                    showTime: this.showTime,
                    flodIndexs,
                    logType: this.logType
                })
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

            addLogData (data, isInit) {
                this.completeInit = isInit
                const type = isInit ? 'initLog' : 'addListData'
                const foldParam = this.$route.query.flodIndexs
                const id = this.$route.query.id
                let foldArr = []
                if (typeof foldParam !== 'undefined' && foldParam && id === this.id) foldArr = foldParam.split(',')
                const scroll = this.$refs.scroll
                const lastIndex = scroll.indexList[scroll.indexList.length - 1] || {}
                const isBottom = +lastIndex.value === +scroll.totalNumber
                const addListPostData = {
                    oldNumber: scroll.totalNumber,
                    oldItemNumber: scroll.itemNumber,
                    oldMapHeight: scroll.mapHeight,
                    oldVisHeight: scroll.visHeight
                }
                scroll.addListData(data, type, foldArr.map(x => +x))
                if (!isBottom) {
                    scroll.getNumberChangeList(addListPostData)
                } else {
                    scroll.scrollPageByIndex(scroll.totalNumber - scroll.itemNumber)
                }
            },

            scrollPageToBottom () {
                const scroll = this.$refs.scroll
                scroll.scrollPageByIndex(scroll.totalNumber - scroll.itemNumber)
            }
        }
    }
</script>

<style lang="scss" scoped>
    .log-loading {
        position: absolute;
        bottom: 0;
        height: calc(100% - 84px);
        width: 100%;
        background: black;
        z-index: 100;
        .lds-ring {
            display: inline-block;
            position: relative;
            width: 80px;
            height: 80px;
            top: 50%;
            left: 50%;
            transform: translate3d(-50%, -50%, 0);
        }
        .lds-ring div {
            box-sizing: border-box;
            display: block;
            position: absolute;
            width: 37px;
            height: 37px;
            margin: 8px;
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
                border-bottom: 1px solid;
                border-bottom-color: #2b2b2b;
                display: flex;
                align-items: center;
                justify-content: space-between;
                color: #d4d4d4;
                .log-buttons {
                    display: flex;
                    align-items: center;
                    line-height: 30px;
                    .log-button {
                        color: #c2cade;
                        background: #2f363d;
                        border: 1px solid #444d56;
                        margin-right: 10px;
                        height: 32px;
                        line-height: 30px;
                        display: inline-block;
                        outline: none;
                        cursor: pointer;
                        white-space: nowrap;
                        -webkit-appearance: none;
                        padding: 0 15px;
                        text-align: center;
                        vertical-align: middle;
                        font-size: 14px;
                        border-radius: 2px;
                        box-sizing: border-box;
                        text-decoration: none;
                        transition: background-color .3s ease;
                        min-width: 68px;
                        position: relative;
                        &:hover {
                            color: #fff;
                            background: #3f454a;
                        }
                    }
                    .log-execute {
                        width: 100px;
                        margin-right: 10px;
                        color: #c2cade;
                        background: #2f363d;
                        border-color: #444d56;
                        font-size: 14px;
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
