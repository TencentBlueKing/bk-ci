<template>
    <div class="quality-overview-wrapper">
        <div class="inner-header">
            <div class="title">{{$t('quality.总览')}}</div>
            <a class="job-guide" @click="linkToDocs">{{$t('quality.了解更多质量红线')}}<i class="devops-icon icon-tiaozhuan"></i></a>
        </div>

        <section
            class="sub-view-port"
            v-bkloading="{
                isLoading: loading.isLoading,
                title: loading.title
            }">
            <image-empty v-if="showContent && isEmptyRule"
                :title="emptyInfo.title"
                :desc="emptyInfo.desc"
                :btns="emptyInfo.btns">
            </image-empty>
            <div class="quality-overview-content" :class="{ 'overflow-content': isOverflow }" v-if="showContent && !isEmptyRule">
                <div class="overview-index-list">
                    <div class="indicator-card" :class="{ 'jumpable-item': index === 0 || index === 2 }"
                        v-for="(entry, index) in indicatorList" :key="index"
                        @click="toLink(entry.label)">
                        <div class="card-info-title">
                            <i :class="{ 'devops-icon': true, [`icon-${entry.icon}`]: true }"></i>
                            <span class="title">{{entry.name}}</span>
                        </div>
                        <div class="card-info-stastics">
                            <span class="total-count">{{entry.value}}</span>
                            <span v-if="entry.label === 'ruleCount' || entry.label === 'pipelineCount'">{{$t('quality.条')}}</span>
                            <span v-if="entry.label === 'indicatoCount'">{{$t('quality.个')}}</span>
                            <span v-if="entry.label === 'interceptCount'">{{$t('quality.次')}}</span>
                        </div>
                    </div>
                </div>
                <div class="intercept-chart-wrapper">
                    <div class="intercept-item intercept-rank">
                        <p class="chart-name">{{$t('quality.流水线拦截Top5')}}</p>
                        <chart class="chart-wrapper rankchart-wrapper"
                            :loading="loading.isLoading"
                            :option="processOptions('rank')"
                            autoresize
                        >
                        </chart>
                    </div>
                    <div class="intercept-item intercept-trend">
                        <p class="chart-name">{{$t('quality.生效流水线执行数/拦截数趋势')}}</p>
                        <chart class="chart-wrapper trend-chart-wrapper"
                            :option="processOptions('trend')"
                            :loading="loading.isLoading"
                            autoresize
                        >
                        </chart>
                    </div>
                </div>
                <div class="intercept-record-wrapper">
                    <div class="record-list-nav">
                        <p class="info-title">{{$t('quality.拦截历史')}}</p>
                    </div>
                    <div class="intercept-tips" v-if="interceptRecordList.length">
                        <i class="devops-icon icon-exclamation-circle"></i>
                        <span class="intercept-count">{{$t('quality.仅展示最近10条。')}}
                            <span class="more-history" v-if="interceptRecordList.length" @click="toRouteLink('interceptHistory')">{{$t('quality.查看更多')}}</span>
                        </span>
                    </div>
                    <div class="record-list">
                        <bk-table
                            size="small"
                            class="record-table"
                            :data="interceptRecordList"
                            :row-class-name="handleRowStyle"
                            @row-click="handleRowClick">
                            <bk-table-column :label="$t('quality.流水线')" prop="pipelineName">
                                <template slot-scope="props">
                                    <a class="item-times item-pipelinename" :title="props.row.pipelineName"
                                        target="_blank"
                                        :href="`/console/pipeline/${projectId}/${props.row.pipelineId}/detail/${props.row.buildId}`"
                                    >{{props.row.pipelineName}}</a>
                                </template>
                            </bk-table-column>
                            <bk-table-column :label="$t('quality.红线规则')" prop="ruleName">
                                <template slot-scope="props">
                                    <span class="item-times" :title="props.row.ruleName" @click="toRouteLink('ruleList')">{{props.row.ruleName}}</span>
                                </template>
                            </bk-table-column>
                            <bk-table-column :label="$t('quality.拦截详情')" prop="resultMsg" min-width="200" class-name="indicator-item">
                                <template slot-scope="props">
                                    <div class="indicator-detail" :title="handleRemark(props.row.resultMsg)">
                                        <span v-for="(col, key) in props.row.resultMsg" :key="key">
                                            <span>{{col.indicatorName}}</span>
                                            <span>=</span>
                                            <span>{{col.actualValue === undefined ? 'null' : col.actualValue}}</span>，
                                            <span>{{$t('quality.期望')}}
                                                <span>{{indexHandlerConf[col.operation]}}</span>
                                                <span>{{col.value}}</span>
                                            </span>
                                            <br>
                                        </span>
                                    </div>
                                </template>
                            </bk-table-column>
                            <bk-table-column :label="$t('quality.拦截时间')" prop="interceptTime">
                                <template slot-scope="props">
                                    {{localConvertTime(props.row.interceptTime)}}
                                </template>
                            </bk-table-column>
                        </bk-table>
                    </div>
                </div>
            </div>
        </section>
    </div>
</template>

<script>
    
    import { use } from 'echarts/core'
    import { CanvasRenderer } from 'echarts/renderers'
    import { LineChart, BarChart, PieChart } from 'echarts/charts'
    import {
        GridComponent,
        TitleComponent,
        TooltipComponent,
        LegendComponent
    } from 'echarts/components'
    import VChart from 'vue-echarts'
    
    import {
        rankOptions,
        trendOptions
    } from '@/utils/chart-option'
    import imageEmpty from '@/components/common/imageEmpty'
    import { convertTime } from '@/utils/util'

    use([
        CanvasRenderer,
        LineChart,
        BarChart,
        GridComponent,
        PieChart,
        TitleComponent,
        TooltipComponent,
        LegendComponent
    ])

    export default {
        components: {
            chart: VChart,
            'image-empty': imageEmpty
        },
        data () {
            return {
                showContent: false,
                isEmptyRule: false,
                totalInterceptRecor: 0,
                docsUrl: this.BKCI_DOCS?.GATE_DOC ?? '',
                pipelineListLabel: [],
                pipelineListValue: [],
                trendList: [],
                trendDate: [],
                pipelineExcuList: [],
                interceptRecordList: [],
                indicatorList: [
                    { icon: 'rule', name: this.$t('quality.规则数'), label: 'ruleCount', value: '0' },
                    { icon: 'experience', name: this.$t('quality.指标数'), label: 'indicatoCount', value: '0' },
                    { icon: 'pipeline', name: this.$t('quality.生效流水线'), label: 'pipelineCount', value: '0' },
                    { icon: 'minus-circle', name: this.$t('quality.红线拦截次数'), label: 'interceptCount', value: '0' }
                ],
                indexHandlerConf: {
                    LT: '<',
                    LE: '<=',
                    GT: '>',
                    GE: '>=',
                    EQ: '='
                },
                loading: {
                    isLoading: false,
                    title: ''
                },
                emptyInfo: {
                    title: this.$t('quality.创建第一条质量红线规则'),
                    desc: this.$t('quality.通过设置不同的指标和阈值，质量红线规则可以控制流水线发布的质量'),
                    btns: [
                        {
                            type: 'primary',
                            size: 'normal',
                            handler: () => this.toRouteLink('createRule'),
                            text: this.$t('quality.创建规则')
                        }
                    ]
                }
            }
        },
        computed: {
            projectId () {
                return this.$route.params.projectId
            },
            isOverflow () {
                return this.interceptRecordList.length > 3
            }
        },
        watch: {
            projectId () {
                this.pipelineListLabel.splice(0, this.pipelineListLabel.length)
                this.pipelineListValue.splice(0, this.pipelineListValue.length)
                this.trendDate.splice(0, this.trendDate.length)
                this.trendList.splice(0, this.trendList.length)
                this.pipelineExcuList.splice(0, this.pipelineExcuList.length)
                this.interceptRecordList.splice(0, this.interceptRecordList.length)
                this.init()
            }
        },
        async mounted () {
            await this.init()
        },
        methods: {
            async init () {
                const {
                    loading
                } = this

                loading.isLoading = true
                loading.title = this.$t('quality.数据加载中，请稍候')

                try {
                    await this.requestOverview()
                } catch (err) {
                    this.$bkMessage({
                        message: err.message ? err.message : err,
                        theme: 'error'
                    })
                } finally {
                    setTimeout(() => {
                        this.loading.isLoading = false
                        this.showContent = true
                    }, 500)
                }
            },
            processOptions (type, data) {
                const {
                    parse,
                    stringify
                } = JSON
                let options = {}

                // 拦截流水线Top5
                if (type === 'rank') {
                    options = parse(stringify(rankOptions))

                    options.yAxis.axisLabel.formatter = (value) => {
                        if (value.length > 9) {
                            return value.substring(0, 9) + '...'
                        } else {
                            return value
                        }
                    }

                    options.yAxis.data = this.pipelineListLabel

                    options.tooltip.formatter = (params) => {
                        const colorList = ['#FFE356', '#FFCD56', '#FFB456', '#FF8E56', '#FF5656']
                        const commonStyle = 'display:inline-block;margin-right:5px;border-radius:10px;'
                        return `
                            <span style="${commonStyle} max-width:240px;white-space:normal;">${params[0].name}</span>
                            <br/>
                            <span style="${commonStyle} width:9px;height:9px;background-color:${colorList[params[0].dataIndex]}"></span>
                            <span>${this.$t('quality.拦截数：')}</span>${params[0].value}`
                    }

                    options.series[0].itemStyle.normal.color = (params) => {
                        const colorList = ['#FFE356', '#FFCD56', '#FFB456', '#FF8E56', '#FF5656']
                        return colorList[params.dataIndex]
                    }
                    console.log(this.pipelineListValue)
                    options.series[0].data = this.pipelineListValue
                }

                // 拦截趋势
                if (type === 'trend') {
                    const t = this.$t.bind(this)
                    options = trendOptions(t)

                    options.tooltip.formatter = (params) => {
                        const commonStyle = 'display:inline-block;margin-right:5px;border-radius:10px;'
                        return `<span style="${commonStyle} max-width:240px;white-space:normal;">${params[0].name}</span>
                                <br/>
                                <span style="${commonStyle} width:9px;height:9px;background-color: #3c96ff"></span>
                                <span>${this.$t('quality.生效流水线执行数：')}</span>${params[0].value}
                                <br/>
                                <span style="${commonStyle} width:9px;height:9px;background-color: #FF5656"></span>
                                <span>${this.$t('quality.拦截数：')}</span>${params[1].value}`
                    }

                    options.yAxis.axisLabel.formatter = (params) => {
                        if (parseInt(params) !== params) {
                            return ''
                        }
                        return parseInt(params)
                    }
                    console.log(this.trendDate, this.trendList, this.pipelineExcuList)
                    options.xAxis.data = this.trendDate
                    options.series[1].data = this.trendList
                    options.series[0].data = this.pipelineExcuList
                }
                
                return options
            },
            async requestOverview () {
                try {
                    const res = await this.$store.dispatch('quality/requestOverview', {
                        projectId: this.projectId
                    })

                    this.indicatorList.forEach(indicator => {
                        indicator.value = res[indicator.label]
                    })

                    if (res.ruleCount) {
                        this.isEmptyRule = false
                        this.requestPipelineIntercept()
                        this.requestDailyIntercept()
                        this.requestruleIntercept()
                    } else {
                        this.isEmptyRule = true
                    }
                } catch (err) {
                    const message = err.message ? err.message : err
                    const theme = 'error'

                    this.$bkMessage({
                        message,
                        theme
                    })
                }
            },
            async requestPipelineIntercept () {
                try {
                    const res = await this.$store.dispatch('quality/requestPipelineIntercept', {
                        projectId: this.projectId
                    })

                    if (res.length) {
                        res.forEach(val => {
                            this.pipelineListLabel.unshift(val.pipelineName)
                            this.pipelineListValue.unshift(val.count)
                        })
                    }
                } catch (err) {
                    const message = err.message ? err.message : err
                    const theme = 'error'

                    this.$bkMessage({
                        message,
                        theme
                    })
                }
            },
            async requestDailyIntercept () {
                try {
                    const res = await this.$store.dispatch('quality/requestDailyIntercept', {
                        projectId: this.projectId
                    })

                    if (res.length) {
                        res.forEach(val => {
                            this.trendDate.push(val.date)
                            this.trendList.push(val.count)
                            this.pipelineExcuList.push(val.pipelineExecuteCount)
                        })
                    }
                } catch (err) {
                    const message = err.message ? err.message : err
                    const theme = 'error'

                    this.$bkMessage({
                        message,
                        theme
                    })
                }
            },
            async requestruleIntercept () {
                try {
                    const res = await this.$store.dispatch('quality/requestruleIntercept', {
                        projectId: this.projectId
                    })

                    this.interceptRecordList.splice(0, this.interceptRecordList.length)
                    if (res.records) {
                        res.records.forEach(item => {
                            this.interceptRecordList.push(item)
                        })
                        this.totalInterceptRecor = res.count
                    }
                } catch (err) {
                    const message = err.message ? err.message : err
                    const theme = 'error'

                    this.$bkMessage({
                        message,
                        theme
                    })
                }
            },
            handleRowClick (row, e) {
                this.activeIndex = row.index
                this.hideArtifactoriesPopup()
            },
            linkToDocs () {
                window.open(this.docsUrl, '_blank')
            },
            toLink (label) {
                if (label === 'ruleCount' || label === 'pipelineCount') {
                    this.toRouteLink('ruleList')
                } else if (label === 'interceptCount') {
                    this.toRouteLink('interceptHistory')
                } else if (label === 'indicatoCount') {
                    this.toRouteLink('metadataList')
                }
            },
            toRouteLink (path) {
                this.$router.push({
                    name: path,
                    params: {
                        projectId: this.projectId
                    },
                    hash: path === 'interceptHistory' ? '#INTERCEPT' : ''
                })
            },
            handleRemark (indicators) {
                let tips = ''
                indicators.forEach((item, index) => {
                    const isWrap = index === (indicators.length - 1) ? '' : '\n'
                    const actualVal = item.actualValue === undefined ? 'null' : item.actualValue
                    tips += `${item.indicatorName}=${actualVal}${this.$t('quality.，期望')}${this.indexHandlerConf[item.operation]}${item.value}${isWrap}`
                })

                return tips
            },
            /**
             * 处理时间格式
             */
            localConvertTime (timestamp) {
                return convertTime(timestamp)
            }
        }
    }
</script>

<style lang="scss">
    @import '@/scss/conf.scss';
    
    .quality-overview-wrapper {
        overflow: auto;
        .inner-header {
            display: flex;
            justify-content: space-between;
            padding: 18px 20px;
            width: 100%;
            height: 60px;
            border-bottom: 1px solid $borderWeightColor;
            background-color: #fff;
            box-shadow:0px 2px 5px 0px rgba(51,60,72,0.03);
            .title {
                font-size: 16px;
            }
            .job-guide {
                margin-right: 10px;
                color: $primaryColor;
                cursor: pointer;
            }
            .icon-tiaozhuan {
                position: relative;
                top: 2px;
                margin-left: 8px;
                font-size: 16px;
            }
        }
        .quality-overview-content {
            height: 100%;
            min-height: 680px;
            padding: 20px;
            overflow: hidden;
        }
        .overflow-content {
            min-height: 800px;
        }
        .overview-index-list {
            width: 100%;
            display: flex;
            .indicator-card {
                margin-right: 20px;
                padding: 18px 24px;
                flex: 1;
                height: 100px;
                background: #fff;
                border-radius:2px;
                border:1px solid $borderWeightColor;
                &:nth-child(4n) {
                    margin-right: 0;
                }
                &:hover {
                    box-shadow: 0 3px 8px 0 rgba(0,0,0,0.2), 0 0 0 1px rgba(0,0,0,0.08);
                    cursor: pointer;
                }
            }
            .jumpable-item {
                cursor: pointer;
            }
            .card-info-title {
                .devops-icon {
                    position: relative;
                    top: 4px;
                    margin-right: 4px;
                    font-size: 20px;
                    color: $primaryColor
                }
            }
            .card-info-stastics {
                padding-right: 10px;
                text-align: right;
                span {
                    font-size: 16px;
                }
                .total-count {
                    font-size: 36px;
                    font-weight: regular;
                }
            }
        }
        .intercept-chart-wrapper {
            display: flex;
            margin-top: 26px;
            height: 240px;
            width: 100%;
            .intercept-item {
                flex: 1;
                p {
                    line-height: 36px;
                    border-bottom: 1px solid $borderWeightColor;
                }
            }
            .chart-name {
                font-weight: bold;
            }
            .intercept-trend {
                padding-left: 20px;
            }
            .chart-wrapper {
                width: 96%;
                height: calc(100% - 36px);
            }
            .rankchart-wrapper {
                width: 98%;
            }
        }
        .intercept-record-wrapper {
            margin-top: 26px;
            height: calc(100% - 360px);
            overflow: unset;
            .record-list-nav {
                display: flex;
                justify-content: space-between;
                line-height: 36px;
                border-bottom: 1px solid $borderWeightColor;
            }
            .record-list-nav {
                font-weight: bold;
            }
            .intercept-tips {
                margin: 10px 0 0;
                padding: 0 20px;
                height: 42px;
                line-height: 42px;
                border: 1px solid #FFC947;
                background-color: #FFF3DA;
                .devops-icon {
                    position: relative;
                    top: 2px;
                    margin-right: 6px;
                    color: #FFC947;
                    font-size: 16px;
                }
                .more-history {
                    color: $primaryColor;
                    cursor: pointer;
                }
            }
            .record-list {
                margin-top: 10px;
                height: calc(100% - 130px);
                min-height: 204px;
                overflow: auto;
            }
            .record-table {
                .item-times {
                    color: $primaryColor;
                    cursor: pointer;
                }
                .indicator-detail {
                    display: -webkit-box;
                    -webkit-line-clamp: 3;
                    -webkit-box-orient: vertical;
                    word-break: break-all;
                    overflow: hidden;
                    max-height: 60px;
                    line-height: 1.5;
                }
                td.indicator-item .cell {
                    padding: 10px 15px;
                }
            }
        }
    }
</style>
