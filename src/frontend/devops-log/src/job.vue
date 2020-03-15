<template>
    <log-container v-bind="$props"
        @closeLog="$emit('closeLog')"
        @showSearchLog="showSearchLog"
        :show-time.sync="showTime"
        :search-str.sync="searchStr"
        :worker="worker">
        <ul class="plugin-list">
            <li v-for="plugin in curPluginList" :key="plugin.id" class="plugin-item">
                <p class="item-head" @click="expendLog(plugin)">
                    <span :class="[{ 'show-all': !!plugin.isFold }, 'log-folder']"></span>
                    <status-icon :status="plugin.status"></status-icon>
                    {{ plugin.name }}
                </p>
                <virtual-scroll class="log-scroll" :ref="plugin.id" v-show="plugin.isFold" :max-height="500" :id="plugin.id" :worker="worker">
                    <template slot-scope="item">
                        <span class="item-txt selection-color">
                            <span class="item-time selection-color" v-if="showTime">{{(item.data.isNewLine ? '' : item.data.timestamp)|timeFilter}}</span>
                            <span :class="['selection-color', { 'cur-search': curSearchIndex === item.data.index }]" :style="`color: ${item.data.color};font-weight: ${item.data.fontWeight}`" v-html="valuefilter(item.data.value)"></span>
                        </span>
                    </template>
                </virtual-scroll>
            </li>
        </ul>
    </log-container>
</template>

<script>
    // eslint-disable-next-line
    const Worker = require('worker-loader!./worker.js')
    import virtualScroll from './virtualScroll'
    import logContainer from './logContainer'
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
            logContainer,
            statusIcon
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
            },
            pluginList: {
                type: Array
            }
        },

        data () {
            return {
                worker: new Worker(),
                curPluginList: JSON.parse(JSON.stringify(this.pluginList)),
                showTime: false,
                searchStr: '',
                curSearchIndex: 0
            }
        },

        filters: {
            timeFilter (val) {
                if (!val) return ''
                const time = new Date(val)
                return `${time.getFullYear()}-${prezero(time.getMonth() + 1)}-${prezero(time.getDate())} ${prezero(time.getHours())}:${prezero(time.getMinutes())}:${prezero(time.getSeconds())}:${millisecond(time.getMilliseconds())}`
            }
        },

        mounted () {
            this.worker.postMessage({ type: 'initStatus', pluginList: this.curPluginList.map(x => x.id) })
        },

        beforeDestroy () {
            this.worker.terminate()
        },

        methods: {
            showSearchLog ({ index, refId }) {
                this.curSearchIndex = index
                index -= 5
                if (index < 0) index = 0
                const ref = this.$refs[refId][0]
                const ele = ref.$el
                const curPlugin = this.curPluginList.find(x => x.id === refId) || {}
                if (!curPlugin.isFold) curPlugin.isFold = true
                ele.scrollIntoViewIfNeeded()
                ref.scrollPageByIndex(index)
            },

            expendLog (plugin) {
                this.$set(plugin, 'isFold', !plugin.isFold)
                let ref = this.$refs[plugin.id]

                if (plugin.isFold) {
                    this.$nextTick(() => {
                        ref = this.$refs[plugin.id][0]
                        ref.setVisWidth()
                        this.$emit('openPlugin', plugin.id, ref)
                    })
                } else {
                    this.$emit('closePlugin', plugin.id)
                }
            },

            valuefilter (val) {
                const valArr = val.split(/<a[^>]+?href=["']?([^"']+)["']?[^>]*>([^<]+)<\/a>/gi)
                const transVal = (val = '') => {
                    let regStr = '\\s|<|>'
                    if (this.searchStr !== '') regStr += `|${this.searchStr}`
                    const tranReg = new RegExp(regStr, 'g')
                    return val.replace(tranReg, (str) => {
                        if (str === '<') return '&lt;'
                        else if (str === '>') return '&gt;'
                        else if (str === this.searchStr) return `<span class="search-str">${str}</span>`
                        else if (/\t/.test(str)) return '&nbsp;&nbsp;&nbsp;&nbsp;'
                        else return'&nbsp;'
                    })
                }
                let valRes = ''
                for (let index = 0; index < valArr.length; index += 3) {
                    if (typeof valArr[index] === 'undefined') continue
                    const firstVal = valArr[index]
                    const secVal = valArr[index + 1]
                    const thirdVal = valArr[index + 2]
                    valRes += transVal(firstVal)
                    if (secVal) valRes += `<a href='${secVal}' target='_blank'>${transVal(thirdVal)}</a>`
                }
                return valRes
            }
        }
    }
</script>

<style lang="scss" scoped>
    .plugin-list {
        height: calc(100% - 52px);
        overflow: auto;
    }
    .plugin-item {
        color: #ffffff;
        font-family: Consolas, "Courier New", monospace;
        font-weight: normal;
        font-size: 12px;
        line-height: 16px;
        .item-head {
            display: flex;
            justify-items: center;
            cursor: pointer;
            padding: 8px;
        }
        .log-folder {
            position: inherit;
            transform: rotate(-90deg);
            &.show-all {
                transform: rotate(0deg);
            }
        }
        /deep/ .log-status {
            width: 14px;
            height: 14px;
            margin: 0 9px;
            padding: 1px 0;
            svg {
                width: 14px;
                height: 14px;
            }
            i:before {
                top: -13px;
                left: 1px;
                position: absolute;
            }
        }
    }
    .log-scroll {
        color: #ffffff;
        font-family: Consolas, "Courier New", monospace;
        font-weight: normal;
        cursor: text;
        white-space: nowrap;
        letter-spacing: 0px;
        font-size: 12px;
        line-height: 16px;
        margin: 0 20px;
        .item-txt {
            position: relative;
            padding: 0 5px;
            .cur-search {
                /deep/ .search-str {
                    color: rgb(255, 255, 255);
                    background: rgb(33, 136, 255);
                    outline: rgb(121, 184, 255) solid 1px;
                }
            }
            /deep/ .search-str {
                color: rgb(36, 41, 46);
                background: rgb(255, 223, 93);
                outline: rgb(255, 223, 93) solid 1px;
            }
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
