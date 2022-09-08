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
            :empty-text="$t('noData')">
            <bk-table-column :label="$t('pipeline.pipeline')" prop="pipelineName" min-width="200">
                <template slot-scope="props">
                    <a class="item-pipelinename" :title="props.row.pipelineName"
                        target="_blank"
                        :href="`/pipeline/${props.row.pipelineId}/detail/${props.row.buildId}/${projectName}`">{{ props.row.pipelineName }}
                    </a>
                </template>
            </bk-table-column>
            <bk-table-column :label="$t('setting.nodeInfo.buildNo')" prop="buildNumber">
                <template slot-scope="props">
                    <span>{{ props.row.buildNumber }}</span>
                </template>
            </bk-table-column>
            <bk-table-column :label="$t('setting.nodeInfo.ownJob')" prop="taskName" min-width="160">
                <template slot-scope="props">
                    <span :title="props.row.taskName">{{ props.row.taskName }}</span>
                </template>
            </bk-table-column>
            <bk-table-column :label="$t('status')" prop="status">
                <template slot-scope="props">
                    <span :class="{
                        'is-success': props.row.status === 'DONE',
                        'is-fail': props.row.status === 'FAILURE'
                    }">{{ statusMap[props.row.status] || props.row.status }}</span>
                    <span v-if="props.row.agentTask && props.row.agentTask.status === 'RUNNING'">{{`（${$t('setting.nodeInfo.agentTaskRunning')}）`}}</span>
                </template>
            </bk-table-column>
            <bk-table-column :label="$t('createTime')" prop="createdTime" min-width="160">
                <template slot-scope="props">
                    {{ localConvertTime(props.row.createdTime) }}
                </template>
            </bk-table-column>
            <bk-table-column :label="$t('updateTime')" prop="updatedTime" min-width="160">
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
    import { setting } from '@/http'
    import { mapState } from 'vuex'

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
                    'QUEUE': this.$t('setting.nodeInfo.queuing'),
                    'RUNNING': this.$t('setting.nodeInfo.running'),
                    'DONE': this.$t('setting.nodeInfo.succeed'),
                    'FAILURE': this.$t('setting.nodeInfo.fail')
                }
            }
        },
        computed: {
            ...mapState(['projectId']),
            nodeHashId () {
                return this.$route.params.agentId
            },
            projectName () {
                return this.$route.hash
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
                    const res = await setting.requestBuildList({
                        projectId: this.projectId,
                        nodeHashId: this.nodeHashId,
                        page: page || 1,
                        pageSize: pageSize || 10
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

<style lang="postcss">
    @import '@/css/conf';
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
