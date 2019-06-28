<template>
    <div class="pipeline-list-wrapper">
        <bk-table
            size="small"
            class="pipeline-table"
            :outer-border="false"
            :data="pipelineList"
            :empty-text="'暂无数据'">
            <bk-table-column label="流水线" prop="pipelineName" min-width="200">
                <template slot-scope="props">
                    <a class="item-pipelinename" :title="props.row.pipelineName"
                        target="_blank"
                        :href="`/console/pipeline/${projectId}/${props.row.pipelineId}/detail/${props.row.buildId}`">{{ props.row.pipelineName }}
                    </a>
                </template>
            </bk-table-column>
            <bk-table-column label="构建号" prop="buildNumber">
                <template slot-scope="props">
                    <span>{{ props.row.buildNumber }}</span>
                </template>
            </bk-table-column>
            <bk-table-column label="所属Job" prop="taskName" min-width="160">
                <template slot-scope="props">
                    <span :title="props.row.taskName">{{ props.row.taskName }}</span>
                </template>
            </bk-table-column>
            <bk-table-column label="构建任务状态" prop="status">
                <template slot-scope="props">
                    <span :class="{
                        'is-success': props.row.status === 'DONE',
                        'is-fail': props.row.status === 'FAIL'
                    }">{{ statusMap[props.row.status] }}</span>
                    <span v-if="props.row.agentTask && props.row.agentTask.status === 'RUNNING'">（agent任务运行中）</span>
                </template>
            </bk-table-column>
            <bk-table-column label="创建时间" prop="createdTime" min-width="160">
                <template slot-scope="props">
                    {{ localConvertTime(props.row.createdTime) }}
                </template>
            </bk-table-column>
            <bk-table-column label="更新时间" prop="updatedTime" min-width="160">
                <template slot-scope="props">
                    {{ localConvertTime(props.row.updatedTime) }}
                </template>
            </bk-table-column>
        </bk-table>
        <full-paging v-if="pipelineList.length"
            :size="'small'"
            :page-count-config.sync="pageCountConfig"
            :paging-config.sync="pagingConfig"
            @page-count-changed="pageCountChanged"
            @page-changed="pageChanged">
        </full-paging>
    </div>
</template>

<script>
    import fullPaging from '@/components/common/full-paging'
    import { convertTime } from '@/utils/util'
    import { bus } from '@/utils/bus'

    export default {
        components: {
            fullPaging
        },
        data () {
            return {
                loopTimer: '',
                pipelineList: [],
                pageCountConfig: {
                    totalCount: 30,
                    list: [
                        { id: 10, name: 10 },
                        { id: 20, name: 20 },
                        { id: 50, name: 50 },
                        { id: 100, name: 100 }
                    ],
                    perPageCountSelected: 10
                },
                pagingConfig: {
                    totalPage: 10,
                    curPage: 1
                },
                statusMap: {
                    'QUEUE': '排队中',
                    'RUNNING': '执行中',
                    'DONE': '已完成',
                    'FAIL': '执行失败'
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
                this.requestBuildList(this.pagingConfig.curPage, this.pageCountConfig.perPageCountSelected)
                this.loopCheck()
            })
        },
        async mounted () {
            await this.requestBuildList(1, 10)
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
                    this.pageCountConfig.totalCount = res.count
                    this.pagingConfig.totalPage = Math.ceil(this.pageCountConfig.totalCount / pageSize)
                } catch (err) {
                    const message = err.message ? err.message : err
                    const theme = 'error'

                    this.$bkMessage({
                        message,
                        theme
                    })
                }
            },
            async pageCountChanged () {
                this.pagingConfig.curPage = 1
                await this.requestBuildList(this.pagingConfig.curPage, this.pageCountConfig.perPageCountSelected)
                this.loopCheck()
            },
            async pageChanged () {
                await this.requestBuildList(this.pagingConfig.curPage, this.pageCountConfig.perPageCountSelected)
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
                            await this.requestBuildList(this.pagingConfig.curPage, this.pageCountConfig.perPageCountSelected)
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
