<template>
    <article class="detail-report-home" v-bkloading="{ isLoading }">
        <template v-if="reportList.length">
            <ul class="report-list">
                <li v-for="(report, index) in reportList"
                    :key="index"
                    :class="{ 'text-overflow': true, active: reportIndex === index }"
                    @click="reportIndex = index"
                    v-bk-overflow-tips
                >
                    {{ report.name }}
                </li>
            </ul>

            <bk-table :data="chooseReport.thirdReports"
                :outer-border="false"
                :header-border="false"
                :header-cell-style="{ background: '#FAFBFD' }"
                v-if="chooseReport.type === 'THIRDPARTY'"
                class="report-file"
            >
                <bk-table-column :label="$t('name')" show-overflow-tooltip>
                    <template slot-scope="props">
                        <logo name="tiaozhuan" size="18" class="jump-icon" />
                        <a :href="props.row.indexFileUrl" target="_blank" class="text-link">{{ props.row.name }}</a>
                    </template>
                </bk-table-column>
            </bk-table>
            <iframe :src="chooseReport.indexFileUrl" frameborder="0" class="report-file" v-else></iframe>
        </template>
        <span class="bk-table-empty-text" v-if="!isLoading && reportList.length <= 0">
            <i class="bk-table-empty-icon bk-icon icon-empty"></i>
            <div>{{ $t('empty') }}</div>
        </span>
    </article>
</template>

<script>
    import Logo from '@/components/Logo'

    export default {
        components: {
            Logo
        },

        props: {
            taskId: String
        },

        data () {
            return {
                isLoading: true,
                reportList: [],
                reportIndex: 0
            }
        },

        computed: {
            chooseReport () {
                return this.reportList.find((report, index) => (index === this.reportIndex)) || {}
            }
        },

        watch: {
            taskId () {
                this.$nextTick(() => {
                    this.initData()
                })
            }
        },

        created () {
            this.initData()
        },

        methods: {
            initData () {
                const routeParam = this.$route.params || {}
                const postData = {
                    projectId: routeParam.projectId,
                    pipelineId: routeParam.pipelineId,
                    buildId: routeParam.buildNo,
                    taskId: this.taskId
                }
                this.isLoading = true
                this.$store.dispatch('common/requestReportList', postData).then((res) => {
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
                    if (thirdReports.length) this.reportList.push({ name: this.$t('details.thirdReport'), thirdReports, type: 'THIRDPARTY' })
                    if (this.reportList.length <= 0) {
                        this.$emit('hidden')
                    }
                }).catch((err) => {
                    this.$bkMessage({ theme: 'error', message: err.message || err })
                }).finally(() => {
                    this.isLoading = false
                    this.$emit('complete')
                })
            }
        }
    }
</script>

<style lang="scss" scoped>
    .detail-report-home {
        height: calc(100% - 59px);
        display: flex;
        align-items: stretch;
    }
    .report-list {
        width: 150px;
        border-right: 1px solid #ebedf0;
        padding: 30px 0;
        height: 100%;
        overflow-y: auto;
        li {
            position: relative;
            line-height: 48px;
            font-size: 14px;
            padding-left: 16px;
            color: #222222;
            cursor: pointer;
            &.active {
                background: #f3f9ff;
                color: #1592ff;
                &:before {
                    content: '';
                    position: absolute;
                    left: 0;
                    top: 0;
                    bottom: 0;
                    width: 4px;
                    background: #1a6df3;
                }
            }
        }
    }
    .report-file {
        flex: 1;
        padding: 30px;
        .jump-icon {
            fill: #3c96ff;
            vertical-align: bottom;
            margin-right: 3px;
        }
    }
    .bk-table-empty-text {
        width: 100%;
        text-align: center;
    }
    ::v-deep .bk-table {
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
