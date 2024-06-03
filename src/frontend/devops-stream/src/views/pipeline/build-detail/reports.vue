<template>
    <article class="detail-report-home" v-bkloading="{ isLoading }">
        <bk-exception class="exception-wrap-item" type="403" scene="part" v-if="noPermission">
            <span>{{$t('exception.noPermission')}}</span>
        </bk-exception>
        <template v-else>
            <template v-if="reportList.length">
                <bk-tab :active.sync="reportIndex">
                    <bk-tab-panel
                        v-for="(report, index) in reportList"
                        v-bind="report"
                        :key="index">
                    </bk-tab-panel>
                </bk-tab>
                <bk-table :data="chooseReport.thirdReports"
                    :outer-border="false"
                    :header-border="false"
                    :header-cell-style="{ background: '#FAFBFD' }"
                    v-if="chooseReport.type === 'THIRDPARTY'"
                    class="report-file report-table"
                >
                    <bk-table-column label="Name" show-overflow-tooltip>
                        <template slot-scope="props">
                            <icon name="cc-jump-link" size="14" class="jump-icon" />
                            <a :href="props.row.indexFileUrl" target="_blank" class="text-link">{{ props.row.name }}</a>
                        </template>
                    </bk-table-column>
                </bk-table>
                <iframe :src="chooseReport.indexFileUrl" frameborder="0" class="report-file" v-else></iframe>
            </template>
            <span class="bk-table-empty-text" v-if="!isLoading && reportList.length <= 0">
                <i class="bk-table-empty-icon bk-icon icon-empty"></i>
                <div>{{$t('pipeline.noReports')}}</div>
            </span>
        </template>
    </article>
</template>

<script>
    import { mapState } from 'vuex'
    import { pipelines } from '@/http'

    export default {
        data () {
            return {
                isLoading: false,
                reportList: [],
                reportIndex: 0,
                noPermission: false
            }
        },

        computed: {
            ...mapState(['projectId']),

            chooseReport () {
                return this.reportList.find((report, index) => (index === this.reportIndex)) || {}
            }
        },

        watch: {
            '$route.params.buildId' () {
                this.initData()
            }
        },

        created () {
            this.initData()
        },

        methods: {
            initData () {
                const postData = {
                    projectId: this.projectId,
                    pipelineId: this.$route.params.pipelineId,
                    buildId: this.$route.params.buildId
                }
                this.isLoading = true
                pipelines.requestReportList(postData).then((res) => {
                    const thirdReports = []
                    const innerReports = [];
                    (res || []).forEach((item) => {
                        if (item.type === 'THIRDPARTY') {
                            thirdReports.push(item)
                        } else {
                            innerReports.push(item)
                        }
                    })
                    this.reportList = innerReports
                    if (thirdReports.length) this.reportList.push({ name: this.$t('pipeline.thirdReport'), thirdReports, type: 'THIRDPARTY' })
                    this.reportList = this.reportList.map((report, index) => ({
                        name: index,
                        label: report.name,
                        indexFileUrl: report.indexFileUrl,
                        type: report.type,
                        thirdReports: report.thirdReports
                    }))
                }).catch((err) => {
                    this.noPermission = err.code === 2129002
                    this.$bkMessage({ theme: 'error', message: err.message || err })
                }).finally(() => {
                    this.isLoading = false
                    this.$emit('complete')
                })
            }
        }
    }
</script>

<style lang="postcss" scoped>
    .detail-report-home {
        /deep/ .bk-tab-section {
            padding: 0;
            border: none;
        }
    }
    .report-table {
        padding: 20px;
    }
    .report-file {
        height: calc(100% - 50px);
        width: 100%;
        .jump-icon {
            fill: #3c96ff;
            vertical-align: bottom;
            margin-right: 3px;
        }
    }
    .bk-table-empty-text {
        width: 100px;
        margin: 0 auto;
        text-align: center;
        display: block;
    }
    .exception-wrap-item {
        padding-top: 45px;
    }
    /deep/ .bk-table {
        border: none;
        height: 100%;
        &::before {
            background-color: #fff;
        }
        td, th.is-leaf {
            border-bottom-color: #f0f1f5;
        }
        .bk-table-body-wrapper {
            max-height: calc(100% - 43px);
            overflow-y: auto;
            overflow-x: hidden;
        }
        .cell {
            overflow: hidden;
        }
        .bk-table-header, .bk-table-body {
            width: auto !important;
        }
    }
</style>
