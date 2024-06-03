<template>
    <div class="pipeline-list-wrapper">
        <bk-table
            size="small"
            class="pipeline-table"
            :outer-border="false"
            :data="pipelineList"
            :pagination="pagination"
            @page-change="handlePageChange"
            @page-limit-change="handlePageLimitChange"
            :empty-text="$t('environment.noData')">
            <bk-table-column :label="$t('environment.pipeline')" prop="pipelineName" min-width="200">
                <template slot-scope="props">
                    <a class="item-pipelinename" :title="props.row.pipelineName"
                        target="_blank"
                        :href="`/console/pipeline/${props.row.projectId}/${props.row.pipelineId}/detail/${props.row.buildId}`">{{ props.row.pipelineName }}
                    </a>
                </template>
            </bk-table-column>
            <bk-table-column :label="$t('environment.nodeInfo.buildNo')" prop="buildNumber">
                <template slot-scope="props">
                    <span>{{ props.row.buildNumber }}</span>
                </template>
            </bk-table-column>
            <bk-table-column :label="$t('environment.nodeInfo.ownJob')" prop="taskName" min-width="160">
                <template slot-scope="props">
                    <span :title="props.row.taskName">{{ props.row.taskName }}</span>
                </template>
            </bk-table-column>
            <bk-table-column :label="$t('environment.nodeInfo.buildTaskStatus')" prop="status">
                <template slot-scope="props">
                    <span :class="{
                        'is-success': props.row.status === 'DONE',
                        'is-fail': props.row.status === 'FAIL'
                    }">{{ statusMap[props.row.status] }}</span>
                    <span v-if="props.row.agentTask && props.row.agentTask.status === 'RUNNING'">{{`（${$t('environment.nodeInfo.agentTaskRunning')}）`}}</span>
                </template>
            </bk-table-column>
            <bk-table-column :label="$t('environment.envInfo.creationTime')" prop="createdTime" min-width="160">
                <template slot-scope="props">
                    {{ localConvertTime(props.row.createdTime) }}
                </template>
            </bk-table-column>
            <bk-table-column :label="$t('environment.updateTime')" prop="updatedTime" min-width="160">
                <template slot-scope="props">
                    {{ localConvertTime(props.row.updatedTime) }}
                </template>
            </bk-table-column>
        </bk-table>
    </div>
</template>

<script>
    import { convertTime } from '@/utils/util'
    import { bus } from '@/utils/bus'

    export default {
        data () {
            return {
                loopTimer: '',
                pipelineList: [],
                pagination: {
                    current: 1,
                    count: 30,
                    limitList: [10, 20, 50, 100],
                    limit: 10
                },
                statusMap: {
                    QUEUE: this.$t('environment.nodeInfo.queuing'),
                    RUNNING: this.$t('environment.nodeInfo.running'),
                    DONE: this.$t('environment.nodeInfo.succeed'),
                    FAIL: this.$t('environment.nodeInfo.fail')
                }
            }
        },
        computed: {
            projectId () {
                return this.$route.params.projectId
            },
            nodeHashId () {
                return this.$route.params.nodeHashId
            }
        },
        created () {
            bus.$off('refreshBuild')
            bus.$on('refreshBuild', () => {
                this.requestBuildList(this.pagination.current, this.pagination.limit)
                this.loopCheck()
            })
        },
        async mounted () {
            await this.requestBuildList(this.pagination.current, this.pagination.limit)
            this.loopCheck()
        },
        beforeDestroy () {
            clearTimeout(this.loopTimer)
            this.loopTimer = null
        },
        methods: {
            async requestBuildList (page, pageSize) {
                try {
                    const res = await this.$store.dispatch('environment/requestBuildList', {
                        projectId: this.projectId,
                        nodeHashId: this.nodeHashId,
                        page: page,
                        pageSize: pageSize
                    })
                    this.pipelineList.splice(0, this.pipelineList.length, ...res.records || [])
                    this.pagination.count = res.count
                } catch (err) {
                    const message = err.message ? err.message : err
                    const theme = 'error'

                    this.$bkMessage({
                        message,
                        theme
                    })
                }
            },
            async handlePageLimitChange (limit) {
                this.pagination.current = 1
                this.pagination.limit = limit
                await this.requestBuildList(1, limit)
                this.loopCheck()
            },
            async handlePageChange (curPage) {
                this.pagination.current = curPage
                await this.requestBuildList(curPage, this.pagination.limit)
                this.loopCheck()
            },
            /**
             *  轮询检查状态
             */
            loopCheck () {
                clearTimeout(this.loopTimer)

                const needLoop = this.pipelineList.some(item => item.status === 'RUNNING' || (item.agentTask && item.agentTask.status === 'RUNNING'))
                if (needLoop) {
                    this.loopTimer = setTimeout(async () => {
                        try {
                            await this.requestBuildList(this.pagination.current, this.pagination.limit)
                            this.loopCheck()
                        } catch (err) {
                            this.$bkMessage({
                                message: err.message || err,
                                theme: 'error'
                            })
                        }
                    }, 5000)
                }
            },
            /**
             * 处理时间格式
             */
            localConvertTime (timestamp) {
                return convertTime(timestamp * 1000)
            }
        }
    }
</script>

<style lang="scss">
    @import './../../../scss/conf';
    .pipeline-list-wrapper {
        padding: 20px 0;
        .pipeline-table {
            .item-pipelinename {
                color: $primaryColor;
            }
            .no-data {
                padding: 20px 0;
                text-align: center;
            }
            .is-success {
                color: #30D878;
            }
            .is-fail {
                color: #FF5656;
            }
        }
        .ci-paging {
            margin: 20px 20px 10px;
        }
    }
</style>
