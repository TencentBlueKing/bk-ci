<template>
    <div class="pipeline-code-check"
        v-bkloading="{
            isLoading: loading.isLoading,
            title: loading.title
        }">
        <template v-if="!faildCodeCCScript && dataList.length && showContent">
            <!-- overview start -->
            <!-- <p class="update-time">更新时间 :
                <span>{{checkUpdateTime || '-'}}</span>
                <i class="devops-icon icon-refresh" @click="init"></i>
            </p> -->

            <!-- overview start -->
            <!-- <section class="code-check-header">
                <div class="code-check-header-wrapper">
                    <ul class="code-check-box">
                        <li class="code-check-card"
                            v-for="item of dataList"
                            :key="item.tool_name_en"
                            @click="toLinkCodecc()">
                            <div class="card-title">
                                {{ codeccOptions[item.tool_name_en].title }}
                            </div>
                            <div class="code-check-detail">
                                <div class="check-info">{{ item[codeccOptions[item.tool_name_en].activeVal] }}</div>
                                <div class="check-label-row">
                                    <div class="leftover-label">
                                        <span class="item-active-text">{{ codeccOptions[item.tool_name_en].activeKey }}</span>
                                        <span class="item-label-change"
                                            v-if="codeccOptions[item.tool_name_en].hasChange">
                                            <i class="devops-icon change-direction"
                                                :class="[ item[codeccOptions[item.tool_name_en].activeVal] > 0 ? 'icon-angle-up' : 'icon-angle-down']"></i>
                                        </span>
                                    </div>
                                    <div class="other-label">
                                        <span class="card-item-label" v-for="(legend, index) of codeccOptions[item.tool_name_en].normalLegend" :key="index">
                                            <span class="item-label-text">{{ legend.key }}</span>
                                            <span>:</span>
                                            <span class="item-label-value">{{ item[legend.value] }}</span>
                                        </span>
                                    </div>
                                </div>
                            </div>
                        </li>
                    </ul>
                </div>
            </section> -->
            <section class="code-check-header">
                <div class="code-check-header-wrapper">
                    <div class="code-check-head"
                        v-for="tool of dataList"
                        :key="tool.tool_name_en"
                        @click="toLinkCodecc(tool.defect_detail_url)">
                        <div class="code-check-title">{{ tool.tool_name_cn }}</div>
                        <!-- <chart class="chart-wrapper"
                            v-if="tool.result_status === 'success'"
                            :options="setOptions(tool)"
                            auto-resize>
                        </chart> -->
                        <div class="tool-display-circle" v-if="tool.result_status === 'success'">
                            <div class="circle-content"></div>
                        </div>
                        <div class="no-data-right" v-else>
                            <img src="../../images/500.png">
                            <p>分析异常</p>
                        </div>
                        <div class="center-content value-item" v-if="tool.result_status === 'success'">
                            <div class="check-value">{{ tool[codeccOptions[tool.tool_name_en].mainVal] }}</div>
                        </div>
                        <div class="center-content label-item" v-if="tool.result_status === 'success'">
                            <div class="check-item">{{ codeccOptions[tool.tool_name_en].mainKey }}</div>
                        </div>
                        <div class="charts-legend" v-if="tool.result_status === 'success'">
                            <div class="legend-item">
                                <i class="first-index-icon"></i>
                                <label>{{ codeccOptions[tool.tool_name_en].activeKey }}</label>
                                <span class="legend-data">{{ tool[codeccOptions[tool.tool_name_en].activeVal] }}</span>
                                <i v-if="![&quot;KLOCWORK&quot;, &quot;COVERITY&quot;].includes(tool.tool_name_cn)"
                                    :class="{ &quot;devops-icon&quot;: true,
                                              &quot;icon-arrows-up&quot;: parseFloat(tool[codeccOptions[tool.tool_name_en].activeChange]) > 0,
                                              &quot;icon-arrows-down&quot;: parseFloat(tool[codeccOptions[tool.tool_name_en].activeChange]) < 0 }"></i>
                            </div>
                            <div class="legend-item">
                                <i class="second-index-icon"></i>
                                <label>{{ codeccOptions[tool.tool_name_en].normalKey }}</label>
                                <span class="legend-data">{{ tool[codeccOptions[tool.tool_name_en].normalVal] }}</span>
                                <i v-if="![&quot;KLOCWORK&quot;, &quot;COVERITY&quot;].includes(tool.tool_name_cn)"
                                    :class="{ &quot;devops-icon&quot;: true,
                                              &quot;icon-arrows-up&quot;: parseFloat(tool[codeccOptions[tool.tool_name_en].normalChange]) > 0,
                                              &quot;icon-arrows-down&quot;: parseFloat(tool[codeccOptions[tool.tool_name_en].normalChange]) < 0 }"></i>
                            </div>
                        </div>
                    </div>
                </div>
            </section>
            <!-- overview end -->
            <template v-for="item of dataList">
                <section class="code-check-row" :key="item.tool_name_en" v-if="item.result_status === 'success'"
                    :class="{ 'lint-check-row': lintReportArr.includes(item.tool_name_en) }">
                    <div class="row-head">
                        <div class="row-head-text">
                            {{ codeccOptions[item.tool_name_en].title }}
                        </div>
                        <div class="row-head-link">
                            <a class="text-link" @click="toLinkCodecc(item.defect_report_url)">查看详情</a>
                        </div>
                    </div>

                    <div class="code-check-row-wrapper">
                        <div class="code-check-item"
                            v-for="(chart, index) in codeccOptions[item.tool_name_en].charts" :key="index">
                            <chart class="chart-wrapper"
                                v-if="chart.enable !== false"
                                :option="processOptions(chart, item)"
                                autoresize>
                            </chart>
                            <empty v-if="chart.enable === false" :is-code-check="true"
                                :empty-title="chart.title">
                            </empty>
                        </div>
                    </div>

                    <p class="lint-tool-tips" v-if="lintReportArr.includes(item.tool_name_en)">
                        注：以上均为接入后新增告警数据，CodeCC鼓励该工具新告警清零。
                    </p>
                </section>
            </template>
        </template>

        <!-- 流水线执行失败 -->
        <template v-if="faildCodeCCScript && showContent">
            <empty-tips
                :title="faildExcuCongig.title"
                :desc="faildExcuCongig.desc"
                :btns="faildExcuCongig.btns">
                <section slot="btns">
                    <bk-button
                        :theme="faildExcuCongig.btns[0].theme"
                        :size="faildExcuCongig.btns[0].size"
                        @click="faildExcuCongig.btns[0].handler">
                        {{ faildExcuCongig.btns[0].text }}
                    </bk-button>
                </section>
            </empty-tips>
        </template>

        <!-- 代码检查报告暂无数据 -->
        <template v-if="!faildCodeCCScript && !dataList.length && showContent">
            <empty-tips
                :title="emptyTipsConfig.title"
                :desc="emptyTipsConfig.desc"
                :btns="emptyTipsConfig.btns">
                <section slot="btns">
                    <bk-button
                        :theme="emptyTipsConfig.btns[0].theme"
                        :size="emptyTipsConfig.btns[0].size"
                        @click="emptyTipsConfig.btns[0].handler">
                        {{ emptyTipsConfig.btns[0].text }}
                    </bk-button>
                </section>
            </empty-tips>
        </template>

        <!-- 暂无代码检查报告 -->
        <!-- <template v-if="noCodeCCScript && showContent">
            <empty-tips
                :title="noScriptConfig.title"
                :desc="noScriptConfig.desc"
                :btns="noScriptConfig.btns">
                <section slot="btns">
                    <bk-button class=""
                        :theme="noScriptConfig.btns[0].theme"
                        :size="noScriptConfig.btns[0].size"
                        @click="noScriptConfig.btns[0].handler">
                        {{ noScriptConfig.btns[0].text }}
                    </bk-button>
                </section>
            </empty-tips>
        </template> -->

        <empty-tips
            v-if="hasNoPermission"
            :title="noPermissionTipsConfig.title"
            :desc="noPermissionTipsConfig.desc"
            :btns="noPermissionTipsConfig.btns">
        </empty-tips>
    </div>
</template>

<script>
    import { use } from 'echarts/core'
    import VChart from 'vue-echarts'
    import { CanvasRenderer } from 'echarts/renderers'
    import { LineChart, BarChart, PieChart } from 'echarts/charts'
    import {
        GridComponent,
        TooltipComponent,
        TitleComponent,
        LegendComponent
    } from 'echarts/components'
    import empty from '@/components/common/empty'
    import emptyTips from '@/components/devops/emptyTips'
    import {
        pieOption,
        barOption,
        lineOption,
        overViewOption
    } from '@/utils/chart-options'
    import codeccOptions from '@/utils/codecc-options'
    import { mapGetters, mapState } from 'vuex'

    use([
        CanvasRenderer,
        LineChart,
        BarChart,
        PieChart,
        GridComponent,
        TitleComponent,
        TooltipComponent,
        LegendComponent
    ])

    export default {
        components: {
            chart: VChart,
            empty,
            emptyTips
        },
        data () {
            return {
                // 用lint类报告展示的工具
                lintReportArr: ['CPPLINT', 'PYLINT', 'ESLINT', 'CHECKSTYLE', 'STYLECOP', 'GOML', 'DETEKT', 'PHPCS', 'SENSITIVE', 'OCCHECK', 'GOCILINT', 'HORUSPY', 'WOODPECKER_SENSITIVE', 'RIPS'],
                textStyle: {
                    fontSize: '48px',
                    color: '#63656E'
                },
                codeccOptions,
                dataList: [],
                loading: {
                    isLoading: false,
                    title: '数据加载中，请稍候'
                },
                showContent: false,
                faildExcuCongig: {
                    title: '代码检查报告暂无数据',
                    desc: 'codecc插件执行失败，暂时获取不到相关数据',
                    btns: [
                        {
                            theme: 'primary',
                            size: 'normal',
                            handler: this.execDetailHandler,
                            text: '查看构建详情'
                        }
                    ]
                },
                emptyTipsConfig: {
                    title: '代码检查报告暂无数据',
                    desc: '流水线中关联的代码检查插件尚未执行成功，暂时获取不到相关数据',
                    btns: [
                        {
                            theme: 'primary',
                            size: 'normal',
                            handler: this.execHandler,
                            text: '查看构建记录'
                        }
                    ]
                },
                noScriptConfig: {
                    title: '暂无代码检查报告',
                    desc: '在交付之前做一次全面的代码检查，是保证交付质量的好习惯，去为你的流水线添加一个代码检查插件吧',
                    btns: [
                        {
                            theme: 'primary',
                            size: 'normal',
                            handler: this.addScriptHandler,
                            text: '去添加插件'
                        }
                    ]
                },
                curTaskId: '',
                componentType: '',
                checkUpdateTime: '',
                faildCodeCCScript: false,
                hasNoPermission: false,
                noPermissionTipsConfig: {
                    title: '没有权限',
                    desc: '你没有查看该流水线的权限，请切换项目或申请相应权限',
                    btns: [
                        {
                            theme: 'primary',
                            size: 'normal',
                            handler: this.changeProject,
                            text: '切换项目'
                        },
                        {
                            theme: 'success',
                            size: 'normal',
                            handler: this.goToApplyPerm,
                            text: '申请权限'
                        }
                    ]
                }
            }
        },
        computed: {
            projectId () {
                return this.$route.params.projectId
            },
            pipelineId () {
                return this.$route.params.pipelineId
            },
            buildNo () {
                return this.$route.params.buildNo
            },
            ...mapGetters({
                curPipeline: 'pipelines/getCurPipeline'
            }),
            ...mapState([
                'fetchError'
            ])
        },
        watch: {
            pipelineId (val) {
                this.init()
            },
            fetchError (error) {
                if (error.code === 403) {
                    this.loading.isLoading = false
                    this.hasNoPermission = true
                }
            }
        },
        async mounted () {
            document.querySelector('#app').classList.add('overflow-hidden')
            await this.init()
        },
        methods: {
            execHandler () {
                this.$router.push({
                    name: 'pipelinesHistory',
                    params: {
                        buildNo: this.$route.params.buildNo,
                        pipelineId: this.$route.params.pipelineId
                    }
                })
            },
            execDetailHandler () {
                const {
                    projectId,
                    pipelineId,
                    buildNo
                } = this

                this.$router.push({
                    name: 'pipelinesDetail',
                    params: {
                        projectId,
                        pipelineId,
                        buildNo: buildNo,
                        type: 'executeDetail'
                    }
                })
            },
            /**
             * 将字符串数字转成数字类型
             */
            convertStrToNum (str) {
                const isFloat = typeof str === 'string' ? str.match(/\./) : false

                if (isFloat) {
                    return parseFloat(str).toFixed(2)
                } else {
                    return parseInt(str)
                }
            },
            async init () {
                try {
                    this.loading.isLoading = true

                    // 获取代码检查报告
                    const res = await this.$store.dispatch('pipelines/requestNewCodeCCReport', {
                        buildId: this.$route.params.buildNo
                    })

                    if (res && !res.tool_snapshot_list.length) {
                        this.dataList.splice(0, this.dataList.length)
                    } else if (res && res.tool_snapshot_list.length) {
                        // this.curTaskId = res.task_id
                        res.tool_snapshot_list.forEach(data => {
                            if (data.tool_name_cn === 'COVERITY' && !data.tool_name_en) {
                                data.tool_name_en = data.tool_name_cn
                                data.tool_name_cn = 'Coverity'
                            }
                        })
                        this.dataList.splice(0, this.dataList.length, ...res.tool_snapshot_list)
                    }
                    this.showContent = true
                } catch (err) {
                    if (err.code === 403) {
                        this.loading.isLoading = false
                        this.hasNoPermission = true
                    } else {
                        this.$showTips({
                            message: err.message || err,
                            theme: 'error'
                        })
                    }
                } finally {
                    setTimeout(() => {
                        this.loading.isLoading = false
                    }, 200)
                }
            },
            /**
             * 拼接图表所需配置
             * @param {String} type 当前图表的类型
             * @param {Object} settings 当前图表所需的字段配置
             * @param {Object} data 当前所需的数据
             */
            processOptions (settings, data) {
                const {
                    parse,
                    stringify
                } = JSON
                const {
                    type,
                    opts,
                    title
                } = settings
                let options = {}
                const seriesList = []

                // 当前图表是饼图
                if (type === 'pie') {
                    const legendList = []

                    options = parse(stringify(pieOption))

                    opts.forEach(item => {
                        legendList.push({
                            name: item.text,
                            icon: 'circle'
                        })

                        seriesList.push({
                            value: data[item.key],
                            name: item.text
                        })
                    })

                    options.legend.data.splice(0, options.legend.data.length, ...legendList)
                    options.legend.formatter = (name) => {
                        let target
                        for (let i = 0; i < seriesList.length; i++) {
                            if (seriesList[i].name === name) {
                                target = seriesList[i].value
                            }
                        }
                        const arr = [
                            '{a|' + name + '}'
                                + '{b|' + target + '}'
                        ]

                        return arr.join('')
                    }
                    options.legend.textStyle = {
                        rich: {
                            a: {
                                color: '#8a8f9a'
                            },
                            b: {
                                color: '#63656E',
                                padding: [0, 0, 0, 16]
                            }
                        }
                    }
                    if (settings.color && settings.color.length) {
                        options.color = settings.color
                    }
                }

                // 当前图表是柱状图
                if (type === 'bar') {
                    const xAxisList = []

                    options = parse(stringify(barOption))

                    if (opts instanceof Array) {
                        if (opts.length) {
                            opts.forEach(item => {
                                xAxisList.push(item.text)
                                seriesList.push(data[item.key])
                            })
                        }
                    } else {
                        const list = data[opts]

                        if (list) {
                            if (list.length) {
                                list.forEach(item => {
                                    const tempArr = []
                                    xAxisList.push(item[settings.xKey])
                                    if (settings.level) {
                                        settings.level.forEach(val => {
                                            tempArr.push({
                                                key: val.key,
                                                text: val.text,
                                                value: item[val.key]
                                            })
                                        })
                                    }
                                    seriesList.push({
                                        name: item.name,
                                        value: item.total_count,
                                        levelList: [...tempArr]
                                    })
                                })
                            } else {
                                settings.enable = false
                                return {}
                            }
                        } else {
                            settings.enable = false
                            return {}
                        }
                    }

                    options.xAxis[0].data.splice(0, options.xAxis[0].data.length, ...xAxisList)
                }

                // 当前图表是折线图
                if (type === 'line') {
                    const xAxisList = []

                    options = parse(stringify(lineOption))

                    if (opts instanceof Array) {
                        console.log('array')
                    } else {
                        const list = data[opts]

                        if (list) {
                            if (list.length) {
                                list.forEach(item => {
                                    xAxisList.push(item[settings.xKey])
                                    seriesList.push(item[settings.yKey])
                                })
                            } else {
                                settings.enable = false
                                return {}
                            }
                        } else {
                            settings.enable = false
                            return {}
                        }
                    }
                    const tempArr = []
                    seriesList.forEach(item => {
                        tempArr.push(item.toFixed(2))
                    })
                    seriesList.splice(0, seriesList.length, ...tempArr)
                    options.xAxis.data.splice(0, options.xAxis.data.length, ...xAxisList)
                }

                options.title.text = title
                options.series[0].name = data.tool_name_cn

                if (type === 'line' && settings.title === '代码重复率趋势') {
                    options.series[0].itemStyle.normal.label = {
                        show: false,
                        positiong: 'top',
                        formatter: '{c}%'
                    }
                    options.yAxis.axisLabel.formatter = '{value} %'
                    options.tooltip.formatter = (params) => {
                        let res = params.seriesName + '<br/>'
                        res += '<span style="display:inline-block;margin-right:5px;border-radius:10px;width:9px;height:9px;background-color:'
                            + options.color + '"></span>' + params.name + '：' + params.data + '%'
                        return res
                    }
                }

                if (type === 'bar' && opts === 'author_list') {
                    // options.tooltip.formatter = (params) => {
                    //     let res = params[0].name + '<br/>'
                    //     let target = params[0].data.levelList
                    //     for (let i = 0; i < target.length; i++) {
                    //         res += '<span style="display:inline-block;margin-right:5px;border-radius:10px;width:9px;height:9px;background-color:' +
                    //                 options.color[i] + '"></span>' +
                    //                 target[i].text + '：' + target[i].value + '<br/>'
                    //     }

                    //     return res
                    // }

                    const newSeriesList = []
                    settings.level.forEach((item, index) => {
                        const temp = {
                            name: item.text,
                            type: 'bar',
                            stack: 'author_list',
                            data: [],
                            itemStyle: {
                                normal: {
                                    color: options.color[index],
                                    barBorderRadius: [0, 0, 0, 0]
                                }
                            },
                            barMaxWidth: 50
                        }
                        data[opts].forEach(opt => {
                            temp.data.push(opt[item.key])
                        })
                        newSeriesList.push(temp)
                    })

                    options.series.splice(0, options.series.length)
                    newSeriesList.forEach(series => {
                        options.series.push(series)
                    })
                } else {
                    options.series[0].data.splice(0, options.series[0].data.length, ...seriesList)
                }

                if (type === 'bar' && opts !== 'author_list' && options.series[0].itemStyle) {
                    options.series[0].itemStyle.normal.color = (params) => {
                        const colorList = ['#3c96ff', '#7fcaff', '#a1eaee']
                        return colorList[params.dataIndex]
                    }
                }

                return options
            },
            setOptions (data) {
                const {
                    parse,
                    stringify
                } = JSON

                const options = parse(stringify(overViewOption))
                const legendList = []
                const seriesList = []

                codeccOptions[data.tool_name_en].normalLegend.forEach(item => {
                    legendList.push({
                        name: item.key,
                        icon: 'circle'
                    })

                    seriesList.push({
                        name: item.key,
                        value: data[item.value]
                    })
                })

                options.legend.data.splice(0, options.legend.data.length, ...legendList)
                options.series[0].data.splice(0, options.series[0].data.length, ...seriesList)

                return options
            },
            addScriptHandler () {
                this.$router.push({
                    name: 'pipelinesEdit',
                    params: {
                        projectId: this.projectId,
                        pipelineId: this.pipelineId
                    }
                })
            },
            changeProject () {
                this.$toggleProjectMenu(true)
            },
            goToApplyPerm () {
                const url = `/backend/api/perm/apply/subsystem/?client_id=pipeline&project_code=${this.projectId}&service_code=pipeline&role_manager=pipeline:${this.pipelineId}`
                window.open(url, '_blank')
            },
            toLinkCodecc (url) {
                // const url = `${WEB_URL_PREFIX}/codecc/${this.projectId}/procontrol/buglist?proj_id=${this.curTaskId}&toolName=COVERITY&projectId=${this.projectId}&buildId=${this.buildNo}`
                window.open(url, '_blank')
            }
        },
        beforeDestory () {
            document.querySelector('#app').classList.remove('overflow-hidden')
        }
    }
</script>

<style lang="scss">
    @import './../../scss/conf';

    .pipeline-code-check {
        padding-top: 20px;
        position: relative;
        min-height: 97%;
        overflow: auto !important;
        .update-time {
            margin-bottom: 16px;
            line-height: 16px;
            color: #63656E;
            .icon-refresh {
                position: relative;
                top: 2px;
                cursor: pointer;
            }
        }
        .code-check-header {
            display: inline-block;
            width: 100%;
            min-width: 1280px;
            margin-bottom: 20px;
            .code-check-head {
                position: relative;
                margin-right: -1px;
                margin-bottom: -1px;
                padding: 40px;
                width: calc(100% / 3);
                height: 361px;
                float: left;
                border: 1px solid #dde4eb;
                background-color: #fff;
                font-weight: 600;
                cursor: pointer;
                // box-shadow: 0 3px 6px rgba(0, 0, 0, .05);
            }
            .no-data-right {
                padding-top: 40px;
                text-align: center;
                img {
                    margin-bottom: 20px;
                    width: 268px;
                    height: 140px;
                }
            }
            .tool-display-circle {
                display: flex;
                justify-content: center;
                align-items: center;
                width: 100%;
                height: 100%;
                text-align: center;
                .circle-content {
                    width: 200px;
                    height: 200px;
                    border: 12px solid $primaryColor;
                    border-radius: 50%;
                }
            }
            .center-content {
                position: absolute;
                top: 40px;
                display: flex;
                width: calc(100% - 80px);
                height: calc(100% - 80px);
                justify-content: center;
                align-items: center;
            }
            .value-item {
                font-size: 40px;
                font-weight: 500;
                z-index: 1;
            }
            .check-value {
                position: relative;
                z-index: 2;
                &::after {
                    content: "";
                    display: none;
                    position: absolute;
                    bottom: 10%;
                    left: 0;
                    margin-top: -1px;
                    background-color: $primaryColor;
                    width: 100%;
                    height: 2px;
                }
                &:hover {
                    color: $primaryColor;
                    &::after {
                        display: block;
                    }
                }
            }
            .label-item {
                top: 80px;
                font-size: 14px;
                font-weight: normal;
            }
            .charts-legend {
                position: absolute;
                top: 40px;
                right: 40px;
                font-size: 14px;
                font-weight: normal;
            }
            .legend-item {
                margin-bottom: 10px;
            }
            .legend-data {
                margin-left: 20px;
                font-weight: bold;
            }
            .first-index-icon,
            .second-index-icon {
                display: inline-block;
                position: relative;
                top: 2px;
                width: 14px;
                height: 14px;
                border-radius: 50%;
                border: 3px solid $primaryColor;
            }
            .second-index-icon {
                border-color: #dde4eb;
            }
            .icon-arrows-up,
            .icon-arrows-down {
                display: inline-block;
                font-size: 10px;
                font-weight: bold;
                color: #30d878;
            }
            .icon-arrows-down {
                color: #ff5656;
            }
            .center-icon {
                position: relative;
                top: 10px;
                left: 4px;
            }
            // .code-check-box {
            //     display: inline-block;
            //     min-width: 1660px;
            // }
            // .code-check-card {
            //     float: left;
            //     margin-bottom: 20px;
            //     margin-right: 20px;
            //     padding: 32px 30px 34px 30px;
            //     width: 385px;
            //     height: 150px;
            //     border: 1px solid $borderWeightColor;
            //     border-radius: 2px;
            //     box-shadow: 0 3px 6px rgba(0, 0, 0, .05);
            //     cursor: pointer;
            //     &:nth-child(4n) {
            //         margin-right: 0;
            //     }
            // }
            // .card-title {
            //     margin-bottom: 4px;
            //     font-size: 18px;
            //     color: $fontLighterColor;
            // }
            // .code-check-detail {
            //     display: flex;
            //     justify-content: flex-start;
            // }
            // .check-info {
            //     line-height: 68px;
            //     font-size: 60px;
            //     color: $fontWeightColor;
            // }
            // .check-label-row {
            //     margin-left: 18px;
            // }
            // .change-direction {
            //     margin-left: 4px;
            //     font-size: 12px;
            // }
            // .icon-angle-up {
            //     color: #ff6d6d;
            // }
            // .icon-angle-up {
            //     color: #3cda80;
            // }
            // .item-active-text {
            //     font-size: 16px;
            // }
            // .leftover-label,
            // .other-label {
            //     position: relative;
            //     top: 12px;
            //     font-size: 14px;
            // }
            // .other-label {
            //     top: 20px;
            // }
            // .card-item-label {
            //     &:first-child {
            //         margin-right: 14px;
            //     }
            // }
        }
        .code-check-row {
            height: 361px;
            min-width: 1280px;
            margin-bottom: 20px;
            border: 1px solid $borderWeightColor;
            background-color: #fff;
            box-shadow: 0 3px 6px rgba(0, 0, 0, .05);
            &:last-child {
                margin-bottom: 0;
            }
        }
        .lint-tool-tips {
            margin-top: 20px;
            font-size: 12px;
            color: #999;
            text-align: center;
        }
        .row-head {
            display: flex;
            height: 40px;
            padding: 10px 20px;
            border-bottom: 1px solid $borderWeightColor;
            & + .code-check-row-wrapper {
                height: calc(100% - 50px);
            }
            &-text {
                flex: 1;
                font-weight: bold;
                .tips {
                    padding-left: 30px;
                    font-size: 12px;
                    color: $fontWeightColor;
                }
            }
            &-link {
                flex: 1;
                text-align: right;
                line-height: 17px;
            }
        }
        .lint-check-row {
            height: 461px;
            .code-check-row-wrapper {
                height: calc(100% - 100px);
            }
        }
        .code-check-row-wrapper {
            display: flex;
            height: 100%;
        }
        .code-check-item {
            flex: 1;
            border-right: 1px solid $borderWeightColor;
            &:last-child {
                border: none;
            }
        }
        .item-head {
            display: flex;
            padding: 40px 40px 10px;
        }
        .item-title {
            flex: 1;
            font-weight: bold;
        }
        .item-labels {
            position: relative;
            flex: 2;
            text-align: right;
            font-size: 0;
            &-row {
                display: flex;
                justify-content: flex-end;
                align-items: center;
                & + .item-labels-row {
                    margin-top: 10px;
                }
            }
            &-text {
                display: inline-block;
                margin-left: 10px;
                font-size: 14px;
            }
            &-num {
                display: inline-block;
                width: 65px;
                font: {
                    weight: bold;
                    size: 14px;
                }
            }
            &-change {
                font-size: 12px;
                .change-indicator {
                    display: inline-block;
                    margin-left: 3px;
                    transform: scale(.7);
                    &.up {
                        color: #30d878;
                    }
                    &.down {
                        color: #ff5656;
                    }
                }
            }
        }
        .item-label {
            display: inline-block;
            width: 16px;
            height: 16px;
            border: 3px solid $borderWeightColor;
            border-radius: 50%;
            &.active {
                border-color: #30d878;
            }
        }
        .item-body {
            text-align: center;
        }
        .ring-content {
            position: absolute;
            top: calc(50% + 20px);
            left: 50%;
            transform: translateX(-50%);
            font-size: 14px;
            color: $fontWeightColor;
        }
        .item-labels-icon {
            position: absolute;
            display: block;
            right: -15px;
            font-size: 12px;
            font-weight: bold;
            transform: scale(.7);
        }
        .chart-wrapper {
            width: 100%;
            height: 100%;
        }
        .devops-empty-tips {
            margin-top: 191px;
        }
        .text-link {
            cursor: pointer;
        }
    }
</style>
