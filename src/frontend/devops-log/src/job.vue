<template>
    <log-container v-bind="$props" @closeLog="$emit('closeLog')" :show-time.sync="showTime">
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
                            <span class="selection-color" :style="`color: ${item.data.color};font-weight: ${item.data.fontWeight}`" v-html="valuefilter(item.data.value)"></span>
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
                showTime: false
            }
        },

        filters: {
            timeFilter (val) {
                if (!val) return ''
                const time = new Date(val)
                return `${time.getFullYear()}-${prezero(time.getMonth() + 1)}-${prezero(time.getDate())} ${prezero(time.getHours())}:${prezero(time.getMinutes())}:${prezero(time.getSeconds())}:${millisecond(time.getMilliseconds())}`
            }
        },

        beforeDestroy () {
            this.worker.terminate()
        },

        methods: {
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
