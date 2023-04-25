<template>
    <div class="pipeline-report-wetest" v-bkloading="{ isLoading: loading.isLoading }">
        <table class="bk-table pipeline-history-table">
            <thead>
                <tr>
                    <th width="220" class="history-id">名称</th>
                    <th width="100">版本</th>
                    <th width="100">报告</th>
                    <th width="100">耗时</th>
                    <th width="100">通过率</th>
                    <th width="200">提交时间</th>
                    <th width="179" class="history-operations">操作</th>
                </tr>
            </thead>
            <tbody v-if="pipelineReportList.length">
                <tr v-for="(report, index) in pipelineReportList" :key="index">
                    <td class="history-id"><div class="report-wetest-td name">{{ report.name }}</div></td>
                    <td><div class="report-wetest-td version">{{ report.version || '--' }}</div></td>
                    <td>
                        <div class="report-wetest-td report">
                            <div class="bk-spin-loading bk-spin-loading-mini bk-spin-loading-primary" v-if="report.status === 'RUNNING'">
                                <div class="rotate rotate1"></div>
                                <div class="rotate rotate2"></div>
                                <div class="rotate rotate3"></div>
                                <div class="rotate rotate4"></div>
                                <div class="rotate rotate5"></div>
                                <div class="rotate rotate6"></div>
                                <div class="rotate rotate7"></div>
                                <div class="rotate rotate8"></div>
                            </div>
                            <i class="devops-icon" :class="buildClass(report.status)" v-else></i>
                            {{ buildStatus(report.status) }}
                        </div>
                    </td>
                    <td><div class="report-wetest-td taking">{{ duratuon(report.endTime, report.beginTime) }}</div></td>
                    <td><div class="report-wetest-td pass-rate">{{ report.passingRate || 0 }}%</div></td>
                    <td><div class="report-wetest-td commit-time">{{ getDateStr(report.beginTime) }}</div></td>
                    <td>
                        <div class="report-wetest-td">
                            <a href="javascript:;" class="text-link" @click.stop="linkWetestDetail(report)">查看详情</a>
                            <a class="text-link" :href="wetestUrl(report)" target="_blank">WeTest报告</a>
                        </div>
                    </td>
                </tr>
            </tbody>
            <tbody v-else>
                <tr>
                    <td colspan="7" style="text-align: center;">暂无相关数据</td>
                </tr>
            </tbody>
        </table>
    </div>
</template>

<script>
    export default {
        name: 'wetest-report',
        props: {
            pipelineReportList: {
                type: Array,
                default: []
            },
            loading: {
                type: Object,
                default: {
                    isLoading: false
                }
            }
        },
        data () {
            return {}
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
            }
        },
        methods: {
            buildClass (type) {
                if (type === 'RUNNING') {
                    return 'icon-circle-2-1 spin-icon'
                } else if (type === 'SUCCESS') {
                    return 'icon-check-circle-shape'
                } else if (type === 'FAIL' || type === 'TIMEOUT') {
                    return 'icon-close-circle-shape'
                }
            },
            buildStatus (type) {
                if (type === 'RUNNING') {
                    return '进行中'
                } else if (type === 'SUCCESS') {
                    return '已完成'
                } else if (type === 'FAIL') {
                    return '失败'
                } else if (type === 'TIMEOUT') {
                    return '超时'
                }
            },
            duratuon (endtime, starttime) {
                if (endtime && starttime) {
                    return `${Math.ceil((endtime - starttime) / 60000)}分钟`
                } else {
                    return 0
                }
            },
            getDateStr (dateStr) {
                const date = new Date(dateStr)
                const y = date.getFullYear()
                const m = date.getMonth() + 1
                const d = date.getDate()
                const h = date.getHours()
                const min = date.getMinutes()
                const s = date.getSeconds()

                return `${y}-${m < 10 ? '0' + m : m}-${d < 10 ? '0' + d : d} ${h < 10 ? '0' + h : h}:${min < 10 ? '0' + min : min}:${s < 10 ? '0' + s : s}`
            },
            linkWetestDetail (report) {
                if (report.testId) {
                    window.open(`${WEB_URL_PREFIX}/wetest/${this.projectId}/detail/${report.testId}/${report.startUserId}`, '_blank')
                }
            },
            wetestUrl (report) {
                return report && report.testId ? `https://wetest.qq.com/cloud/report/result?testid=${report.testId}` : false
            }
        }
    }
</script>

<style lang="scss">
    @import './../../scss/conf';

    .pipeline-report-wetest {
        tbody {
            td:hover {
                cursor: default;
            }
            .text-link {
                cursor: pointer;
            }
        }
        .history-id {
            padding-left: 33px;
        }
        .report-wetest-td {
            white-space: nowrap;
            text-overflow: ellipsis;
            overflow: hidden;
        }
        .report {
            .devops-icon {
                vertical-align: middle;
            }
            .icon-circle-2-1{
                color: #3c96ff;
            }
            .icon-check-circle-shape {
                color: #30d878;
            }
            .icon-close-circle-shape {
                color: #f72239;
            }
        }
    }
</style>
