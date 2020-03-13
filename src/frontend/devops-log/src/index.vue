<template>
    <log-container v-bind="$props" @closeLog="$emit('closeLog')" @changeExecute="changeExecute" :show-time.sync="showTime">
        <virtual-scroll class="log-scroll" ref="scroll" :id="id" :worker="worker">
            <template slot-scope="item">
                <span class="item-txt selection-color">
                    <span class="item-time selection-color" v-if="showTime">{{(item.data.isNewLine ? '' : item.data.timestamp)|timeFilter}}</span>
                    <span class="selection-color" :style="`color: ${item.data.color};font-weight: ${item.data.fontWeight}`" v-html="valuefilter(item.data.value)"></span>
                </span>
            </template>
        </virtual-scroll>
    </log-container>
</template>

<script>
    // eslint-disable-next-line
    const Worker = require('worker-loader!./worker.js')
    import virtualScroll from './virtualScroll'
    import logContainer from './logContainer'

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
            logContainer
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
            id: {
                type: String
            }
        },

        data () {
            return {
                worker: new Worker(),
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

        beforeDestroy() {
            this.worker.terminate()
        },

        methods: {
            changeExecute (execute) {
                this.$refs.scroll.resetData()
                this.$emit('changeExecute', this.id, execute)
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

            addLogData (data) {
                const scroll = this.$refs.scroll
                scroll.addLogData(data)
            },

            handleApiErr (err) {
                const scroll = this.$refs.scroll
                scroll.handleApiErr(err)
            }
        }
    }
</script>

<style lang="scss" scoped>
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
                .log-tools {
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
