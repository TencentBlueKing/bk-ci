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

            <virtual-scroll class="log-scroll" ref="scroll">
                <template slot-scope="item">
                    <span class="item-txt selection-color">
                        <span class="item-time selection-color" v-if="showTime">{{(item.data.isNewLine ? '' : item.data.timestamp)|timeFilter}}</span>
                        <span class="selection-color" :style="`color: ${item.data.color};font-weight: ${item.data.fontWeight}`" v-html="valuefilter(item.data.value)"></span>
                    </span>
                </template>
            </virtual-scroll>
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
            status: {
                type: String
            },
            title: {
                type: String
            }
        },

        data () {
            return {
                searchResult: [],
                showSearchIndex: 0,
                showTime: false,
                currentExe: this.executeCount
            }
        },

        mounted () {
            document.addEventListener('mousedown', this.closeLog)
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
                }).replace(/&lt;a((?!&gt;).)+?href=["']?([^"']+)["']?((?!&gt;).)*&gt;(((?!&lt;).)+)&lt;\/a&gt;/gi, "<a href='$2' target='_blank'>$4</a>")
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

            addLogData (data) {
                const scroll = this.$refs.scroll
                scroll.addListData(data)
            },

            handleApiErr (err) {
                const scroll = this.$refs.scroll
                scroll.handleApiErr(err)
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
            display: inline-block;
            min-width: 166px;
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
