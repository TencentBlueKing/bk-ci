<template>
    <section class="plugin-log">
        <search :worker="worker"
            :execute-count="executeCount"
            :search-str.sync="searchStr"
            :show-time.sync="showTime"
            :down-load-link="downLoadLink"
            @showSearchLog="showSearchLog"
            @changeExecute="changeExecute"

        ></search>
        <virtual-scroll class="log-scroll" ref="scroll" :id="id" :worker="worker">
            <template slot-scope="item">
                <span class="item-txt selection-color">
                    <span class="item-time selection-color" v-if="showTime">{{(item.data.isNewLine ? '' : item.data.timestamp)|timeFilter}}</span>
                    <span :class="['selection-color', { 'cur-search': curSearchIndex === item.data.index }]" :style="`color: ${item.data.color};font-weight: ${item.data.fontWeight}`" v-html="valuefilter(item.data.value)"></span>
                </span>
            </template>
        </virtual-scroll>
    </section>
</template>

<script>
    import { mapActions, mapState } from 'vuex'
    import virtualScroll from './virtualScroll'
    import search from '../tools/search'
    import { hashID } from '@/utils/util.js'
    // eslint-disable-next-line
    const Worker = require('worker-loader!./worker.js')

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
        filters: {
            timeFilter (val) {
                if (!val) return ''
                const time = new Date(val)
                return `${time.getFullYear()}-${prezero(time.getMonth() + 1)}-${prezero(time.getDate())} ${prezero(time.getHours())}:${prezero(time.getMinutes())}:${prezero(time.getSeconds())}:${millisecond(time.getMilliseconds())}`
            }
        },

        components: {
            virtualScroll,
            search
        },

        props: {
            id: {
                type: String
            },
            currentTab: {
                type: String
            },
            buildId: {
                type: String
            },
            executeCount: {
                type: Number
            }
        },

        data () {
            return {
                searchStr: '',
                showTime: false,
                worker: new Worker(),
                curSearchIndex: 0,
                postData: {
                    projectId: this.$route.params.projectId,
                    pipelineId: this.$route.params.pipelineId,
                    buildId: this.buildId,
                    tag: this.id,
                    currentExe: this.executeCount,
                    lineNo: 0
                },
                timeId: '',
                clearIds: []
            }
        },

        computed: {
            ...mapState('atom', [
                'execDetail',
                'editingElementPos'
            ]),

            downLoadLink () {
                const editingElementPos = this.editingElementPos
                const fileName = encodeURI(encodeURI(`${editingElementPos.stageIndex + 1}-${editingElementPos.containerIndex + 1}-${editingElementPos.elementIndex + 1}-${this.currentElement.name}`))
                const tag = this.currentElement.id
                return `${AJAX_URL_PIRFIX}/log/api/user/logs/${this.$route.params.projectId}/${this.$route.params.pipelineId}/${this.execDetail.id}/download?tag=${tag}&executeCount=${this.postData.currentExe}&fileName=${fileName}`
            },

            currentElement () {
                const {
                    editingElementPos: { stageIndex, containerIndex, elementIndex },
                    execDetail: { model: { stages } }
                } = this
                return stages[stageIndex].containers[containerIndex].elements[elementIndex]
            }
        },

        mounted () {
            this.worker.postMessage({ type: 'initStatus', pluginList: [this.id] })
            this.getLog()
        },

        beforeDestroy () {
            this.worker.terminate()
            this.closeLog()
        },

        methods: {
            ...mapActions('atom', [
                'getInitLog',
                'getAfterLog'
            ]),

            getLog () {
                const id = hashID()
                this.getLog.id = id
                let logMethod = this.getAfterLog
                if (this.postData.lineNo <= 0) logMethod = this.getInitLog

                logMethod(this.postData).then((res) => {
                    if (this.clearIds.includes(id)) return

                    const scroll = this.$refs.scroll
                    res = res.data || {}
                    if (res.status !== 0) {
                        let errMessage
                        switch (res.status) {
                            case 1:
                                errMessage = this.$t('history.logEmpty')
                                break
                            case 2:
                                errMessage = this.$t('history.logClear')
                                break
                            case 3:
                                errMessage = this.$t('history.logClose')
                                break
                            default:
                                errMessage = this.$t('history.logErr')
                                break
                        }
                        scroll.handleApiErr(errMessage)
                        return
                    }

                    const logs = res.logs || []
                    const lastLog = logs[logs.length - 1] || {}
                    const lastLogNo = lastLog.lineNo || this.postData.lineNo - 1 || -1
                    this.postData.lineNo = +lastLogNo + 1

                    if (res.finished) {
                        if (res.hasMore) {
                            scroll.addLogData(logs)
                            this.timeId = setTimeout(this.getLog, 100)
                        } else {
                            scroll.addLogData(logs)
                        }
                    } else {
                        scroll.addLogData(logs)
                        this.timeId = setTimeout(this.getLog, 1000)
                    }
                }).catch((err) => {
                    this.$bkMessage({ theme: 'error', message: err.message || err })
                    this.$refs.scroll.handleApiErr(err.message)
                })
            },

            changeExecute (execute) {
                this.$refs.scroll.resetData()
                this.postData.currentExe = execute
                this.postData.lineNo = 0
                this.closeLog()
                this.getLog()
            },

            closeLog () {
                clearTimeout(this.timeId)
                this.clearIds.push(this.getLog.id)
            },

            showSearchLog ({ index, realIndex }) {
                this.curSearchIndex = realIndex
                index -= 5
                if (index < 0) index = 0
                this.$refs.scroll.scrollPageByIndex(index)
            },

            handleApiErr (err) {
                const scroll = this.$refs.scroll
                if (scroll) scroll.handleApiErr(err)
            },

            valuefilter (val) {
                const valArr = val.split(/<a[^>]+?href=["']?([^"']+)["']?[^>]*>([^<]+)<\/a>/gi)
                const transSearch = this.searchStr.replace(/\*|\.|\?|\+|\$|\^|\[|\]|\(|\)|\{|\}|\||\\|\//g, (str) => `\\${str}`)
                const searchReg = new RegExp(`^${transSearch}$`, 'i')
                const transVal = (val = '') => {
                    let regStr = '\\s|<|>'
                    if (transSearch !== '') regStr += `|${transSearch}`
                    const tranReg = new RegExp(regStr, 'gi')
                    return val.replace(tranReg, (str) => {
                        if (str === '<') return '&lt;'
                        else if (str === '>') return '&gt;'
                        else if (searchReg.test(str)) return `<span class="search-str">${str}</span>`
                        else if (/\t/.test(str)) return '&nbsp;&nbsp;&nbsp;&nbsp;'
                        else return '&nbsp;'
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
    .plugin-log {
        display: flex;
        flex-direction: column;
        flex: 1;
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
        margin-top: 5px;
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
