<template>
    <article class="log-home">
        <section class="log-main">
            <header class="log-head">
                <span class="log-title"><status-icon :status="status"></status-icon>{{ title }}</span>
                <p class="log-buttons">
                    <bk-select v-if="![0, 1].includes(+executeCount)" :placeholder="language('重试次数')" class="log-execute" :value="currentExe" :clearable="false">
                        <bk-option v-for="execute in executeCount"
                            :key="execute"
                            :id="execute"
                            :name="execute"
                            @click.native="changeExecute(execute)"
                        >
                        </bk-option>
                    </bk-select>
                    <button class="log-button" @click="showTime = !showTime">{{ language('显示时间') }}</button>
                    <button class="log-button" @click="downLoad">{{ language('下载日志') }}</button>
                </p>
            </header>

            <virtual-scroll class="log-scroll" ref="scroll" :id="id" :currentExe="currentExe">
                <template slot-scope="item">
                    <span class="item-txt selection-color"
                        v-if="!isInit"
                    >
                        <span class="item-time selection-color">{{(showTime ? item.data.timestamp : '')|timeFilter}}</span>
                        <span class="selection-color" :style="`color: ${item.data.color};font-weight: ${item.data.fontWeight}`" v-html="valuefilter(item.data.value)"></span>
                    </span>
                </template>
            </virtual-scroll>

            <section class="log-loading" v-if="isInit">
                <div class="lds-ring"><div></div><div></div><div></div><div></div></div>
            </section>
        </section>
    </article>
</template>

<script>
    import virtualScroll from './virtualScroll'
    import statusIcon from './status'
    import language from './locale'

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
                showTime: false,
                currentExe: this.executeCount,
                completeInit: false
            }
        },

        mounted () {
            document.addEventListener('mousedown', this.closeLog)

            const query = this.$route.query || {}
            const id = query.id
            if (id === this.id) this.currentExe = +query.currentExe
        },

        beforeDestroy () {
            document.removeEventListener('mousedown', this.closeLog)
        },

        methods: {
            language,

            closeLog (event) {
                let curTarget = event.target
                if (curTarget.classList.contains('log-home')) this.$emit('closeLog')
            },

            valuefilter (val) {
                return val.replace(/\s|<|>/g, (str) => {
                    let res = '&nbsp;'
                    switch (str) {
                        case '<':
                            res = '&lt;'
                            break;
                        case '>':
                            res = '&gt;'
                            break;
                        default:
                            res = '&nbsp;'
                            break;
                    }
                    return res
                }).replace(/&lt;a.+?href=["']?([^"']+)["']?.*&gt;(.+)&lt;\/a&gt;/g, "<a href='$1' target='_blank'>$2</a>")
            },

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

            copyLink (event) {
                const url = this.getLinkUrl({})
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
                const id = this.$route.query.id
                const scroll = this.$refs.scroll
                const lastIndex = scroll.indexList[scroll.indexList.length - 1] || {}
                const isBottom = +lastIndex.value === +scroll.totalNumber

                scroll.addListData(data, type)
                if (!isBottom) {
                    const addListPostData = {
                        oldNumber: scroll.totalNumber,
                        oldItemNumber: scroll.itemNumber,
                        oldMapHeight: scroll.mapHeight,
                        oldVisHeight: scroll.visHeight
                    }
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
        background: #1e1e1e;
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
            width: 80%;
            height: calc(100% - 32px);
            float: right;
            display: flex;
            flex-direction: column;
            margin: 16px;
            border-radius: 6px;
            overflow: hidden;
            transition-property: transform, opacity;
            transition: transform 200ms cubic-bezier(.165,.84,.44,1), opacity 100ms cubic-bezier(.215,.61,.355,1);
            background: #1e1e1e;
            .log-head {
                line-height: 52px;
                padding: 10px 20px 8px;
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
                        background: #222529;
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
                            background: #292c2d;
                        }
                    }
                    .log-execute {
                        width: 100px;
                        margin-right: 10px;
                        color: #c2cade;
                        background: #222529;
                        border-color: #444d56;
                        font-size: 14px;
                        &:hover {
                            color: #fff;
                            background: #292c2d;
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
        margin-top: 16px;
        .item-txt {
            position: relative;
            padding: 0 5px;
        }
        .item-time {
            color: #959da5;
            font-weight: 400;
            padding-right: 5px;
        }
        /deep/ a {
            color: #3c96ff;
            text-decoration: underline;
            &:active, &:visited, &:hover {
                color: #3c96ff;
            }
        }
        /deep/ a, /deep/ .selection-color {
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
