<template>
    <div class="turbo-record-wrapper" v-bkloading="{ isLoading: loading.isloading }">
        <header-process :process-head="title"></header-process>
        <div class="record-container sub-view-port">
            <div class="filter-wrapper">
                <div class="filter-item">
                    <label class="filter-label">流水线</label>
                    <bk-select
                        v-model="searchParam.bsPipelineId"
                        :searchable="true"
                        :loading="pipelineLoading"
                        :clearable="true">
                        <bk-option v-for="(option, index) in pipelineList"
                            :key="index"
                            :id="option.pipelineId"
                            :name="option.pipelineName">
                        </bk-option>
                    </bk-select>
                </div>
                <div class="filter-item">
                    <label class="filter-label">状态</label>
                    <bk-select
                        v-model="searchParam.taskStatus"
                        :searchable="true"
                        :clearable="true">
                        <bk-option v-for="(option, index) in statusList"
                            :key="index"
                            :id="option.paramCode"
                            :name="option.paramName">
                        </bk-option>
                    </bk-select>
                </div>
                <div class="filter-item">
                    <label class="filter-label">时间</label>
                    <!-- <bk-date-range
                        :range-separator="'-'"
                        :disabled="false"
                        :placeholder="'起止时间'"
                        :quick-select="true"
                        :position="'bottom-left'"
                        :start-date="dateDefault.startDate"
                        :end-date="dateDefault.endDate"
                        @change="cahngeDate">
                    </bk-date-range> -->
                    <bk-date-picker
                        :placement="'bottom-end'"
                        :placeholder="'起止时间'"
                        :type="'daterange'"
                        @change="cahngeDate"
                    ></bk-date-picker>
                </div>
                <div class="filter-button">
                    <bk-button theme="primary" @click="query(1)">查询</bk-button>
                </div>
            </div>
            <div class="table-wrapper">
                <bk-table
                    size="small"
                    class="turbo-table"
                    :data="tableList"
                    :pagination="pageConf"
                    :empty-text="'暂无数据'"
                    @page-change="pageChange"
                    @page-limit-change="pageSelectChanged">
                    <bk-table-column label="编号" width="60">
                        <template slot-scope="props">
                            <span>{{ pageConf.count - props.$index - (pageConf.current - 1) * pageConf.limit }}</span>
                        </template>
                    </bk-table-column>
                    <bk-table-column label="任务名称" prop="taskName">
                        <template slot-scope="props">
                            <span :title="props.row.taskName">{{ props.row.taskName }}</span>
                        </template>
                    </bk-table-column>
                    <bk-table-column label="流水线/本地构建路径" prop="bsPipelineName" min-width="120">
                        <template slot-scope="props">
                            <div :title="props.row.bsPipelineName" class="turbo-td task-program">
                                <span v-if="props.row.bsPipelineId === '-1' || !props.row.bsPipelineId">{{ props.row.bsPipelineName || '--' }}</span>
                                <a v-else :href="`/console/pipeline/${projectId}/${props.row.bsPipelineId}/history`" target="_blank" class="text-link">{{ props.row.bsPipelineName || '--' }}</a>
                            </div>
                        </template>
                    </bk-table-column>
                    <bk-table-column label="状态">
                        <template slot-scope="props">
                            <div :class="['turbo-td status', statuClass(props.row.status)]">{{ props.row.statusName }}</div>
                        </template>
                    </bk-table-column>
                    <bk-table-column label="开始时间" min-width="110">
                        <template slot-scope="props">
                            <span>{{ props.row.compileStartTime }}</span>
                        </template>
                    </bk-table-column>
                    <bk-table-column label="耗时">
                        <template slot-scope="props">
                            <span>{{ props.row.timeConsume }}</span>
                        </template>
                    </bk-table-column>
                    <bk-table-column label="实际加速资源">
                        <template slot-scope="props">
                            <span>{{ props.row.resourceConsume }}</span>
                        </template>
                    </bk-table-column>
                    <bk-table-column label="预计节约时间">
                        <template slot-scope="props">
                            <span>{{ props.row.timeSave }}</span>
                        </template>
                    </bk-table-column>
                </bk-table>
            </div>
        </div>
    </div>
</template>

<script>
    import headerProcess from '@/components/turbo/headerProcess'

    export default {
        components: {
            headerProcess
        },
        data () {
            return {
                title: {
                    title: '加速记录',
                    list: [],
                    hasLink: false
                },
                loading: {
                    isloading: true,
                    title: ''
                },
                pipelineLoading: false,
                dateDefault: {
                    startTime: '',
                    endTime: ''
                },
                statusList: [],
                pageSelectConfig: {
                    count: 0,
                    list: [
                        { id: 10, name: 10 },
                        { id: 20, name: 20 },
                        { id: 50, name: 50 },
                        { id: 100, name: 100 }
                    ],
                    pageSelected: 10,
                    hasSelect: true
                },
                pageTabConf: {
                    page: 1,
                    totalPage: 1
                },
                pipelineList: [],
                tableList: [],
                searchParam: {
                    bsPipelineId: '',
                    taskStatus: '',
                    page: '',
                    pageSize: '',
                    startDate: '',
                    endDate: ''
                },
                pageConf: {
                    totalPage: 1,
                    limit: 10,
                    current: 1,
                    show: false,
                    limitList: [10, 20, 50, 100],
                    count: 0
                }
            }
        },
        computed: {
            projectId () {
                return this.$route.params.projectId
            }
        },
        watch: {
            projectId: async function () {
                await this.pageSelectChanged(10)
                this.requestPipelineList()
            }
        },
        created () {
            this.requestRecordStatus()
            this.pageSelectChanged(10)
            this.requestPipelineList()
        },
        methods: {
            cahngeDate (date) {
                // let date = data.split(' - ')
                this.searchParam.startDate = date[0]
                this.searchParam.endDate = date[1]
            },
            timeConsume (timeConsume) {
                const timeArr = timeConsume.split(':')
                return timeArr[0] + '小时' + timeArr[1] + '分'
            },
            statuClass (latestStatus) {
                let statusClass = ''
                this.statusList.forEach(status => {
                    (status.paramCode === latestStatus) && (statusClass = status.paramValue)
                })
                return statusClass
            },
            async query (pageParam) {
                const { searchParam, pageConf, projectId, loading } = this
                if (pageParam === 1) {
                    this.pageConf.current = 1
                }
                loading.isloading = true
                try {
                    const res = await this.$store.dispatch('turbo/requestRecord', {
                        projectId: projectId,
                        params: Object.assign(searchParam, { page: pageConf.current }, { pageSize: pageConf.limit })
                    })
                    if (res) {
                        this.tableList = res.records.concat()
                        this.pageConf.count = res.count
                        this.pageConf.totalPage = res.totalPages || 1
                    }
                } catch (err) {
                    this.$bkMessage({
                        message: err.message ? err.message : err,
                        theme: 'error'
                    })
                } finally {
                    loading.isloading = false
                }
            },
            async requestPipelineList () {
                const { $store } = this
                this.pipelineLoading = true
                try {
                    const res = await $store.dispatch('turbo/requestPipelineList', {
                        projectId: this.projectId
                    })
                    if (res) {
                        this.pipelineList.splice(0, this.pipelineList.length)
                        res.map(item => {
                            this.pipelineList.push(item)
                        })
                    }
                } catch (err) {
                    this.$bkMessage({
                        message: err.message ? err.message : err,
                        theme: 'error'
                    })
                } finally {
                    this.pipelineLoading = false
                }
            },
            pageChange (page) {
                this.pageConf.current = page
                this.query()
            },
            /**
             *  每页条数下拉框改变的回调函数
             */
            async pageSelectChanged (limit) {
                this.pageConf.limit = limit
                this.pageConf.current = 1
                this.query()
            },
            async requestRecordStatus () {
                try {
                    const res = await this.$store.dispatch('turbo/requestRecordStatus')
                    if (res) {
                        res.forEach(item => {
                            this.statusList.push(item)
                        })
                    }
                } catch (err) {
                    this.$bkMessage({
                        message: err.message ? err.message : err,
                        theme: 'error'
                    })
                }
            }
        }
    }
</script>

<style lang="scss" scoped>
    @import '../assets/scss/conf.scss';

    .turbo-record-wrapper {
        height: 100%;
        overflow: hidden;
        .number {
            min-width: 40px;
        }
        .task-name {
            min-width: 135px;
        }
        .pipeline-name {
            min-width: 135px;
        }
        .status {
            min-width: 70px;
        }
        .time-start {
            min-width: 135px;
        }
        .time-spend {
            min-width: 80px;
        }
        .resource {
            min-width: 140px;
        }
        .time-save {
            min-width: 95px;
        }
        .turbo-table {
            .pipeline-name {
                div {
                    overflow: hidden;
                    text-overflow: ellipsis;
                    white-space: nowrap;
                    display: inline-block;
                }
            }
        }
    }
</style>
