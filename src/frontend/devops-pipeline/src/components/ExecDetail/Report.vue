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

            <iframe :src="reportUrl" frameborder="0" class="report-file"></iframe>
        </template>

        <span class="bk-table-empty-text" v-if="!isLoading && reportList.length <= 0">
            <i class="bk-table-empty-icon bk-icon icon-empty"></i>
            <div>{{ $t('empty') }}</div>
        </span>
    </article>
</template>

<script>
    export default {
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
            reportUrl () {
                const report = this.reportList.find((report, index) => (index === this.reportIndex)) || {}
                return report.indexFileUrl
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
                this.$store.dispatch('soda/requestReportList', postData).then((res) => {
                    this.reportList = res || []
                }).catch((err) => {
                    this.$bkMessage({ theme: 'error', message: err.message || err })
                }).finally(() => {
                    this.isLoading = false
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
    }
    .bk-table-empty-text {
        width: 100%;
        text-align: center;
    }
</style>
