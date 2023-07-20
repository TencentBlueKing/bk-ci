<template>
    <article class="task-list-home">
        <template v-if="hasPermission">
            <header class="task-head">
                <bk-button theme="primary" @click="addTask"> {{ $t('turbo.新增方案') }} </bk-button>
                <span class="g-turbo-gray-font task-head-title">{{ $t('turbo.共') }} {{ turboPlanCount }} {{ $t('turbo.个方案') }} </span>
            </header>

            <main v-bkloading="{ isLoading }">
                <section v-for="task in taskList" :key="task" class="g-turbo-box task-card">
                    <span :class="['open-icon', { 'is-open': task.topStatus === 'true' }]" @click="modifyTurboPlanTopStatus(task)">
                        <logo name="thumbtack" class="icon-thumbtack"></logo>
                    </span>

                    <h3 :class="['card-head', { 'disabled': !task.openStatus }]" @click="toggleShowCard(task)">
                        <p class="task-name">
                            <span class="g-turbo-deep-black-font name-desc" @click.stop="$router.push({ name: 'taskDetail', params: { id: task.planId } })">
                                <span v-bk-overflow-tips class="g-turbo-text-overflow plan-name">{{ task.planName }}</span>
                                <span class="name-detail">{{ task.engineName }}</span>
                            </span>
                            <span class="g-turbo-gray-font name-hash g-turbo-text-overflow">
                                {{ task.planId }}
                                <logo name="copy" class="icon-copy" size="16" @click.native.stop="copy(task.planId)"></logo>
                            </span>
                        </p>
                        <span class="task-line"></span>
                        <p class="task-rate">
                            <span class="rate-num g-turbo-deep-black-font">{{ task.instanceNum }}</span>
                            <span class="rate-title g-turbo-gray-font"> {{ $t('turbo.实例数') }} </span>
                        </p>
                        <p class="task-rate">
                            <span class="rate-num g-turbo-deep-black-font">{{ task.executeCount }}</span>
                            <span class="rate-title g-turbo-gray-font"> {{ $t('turbo.加速次数') }} </span>
                        </p>
                        <p class="task-rate">
                            <span class="rate-num g-turbo-deep-black-font">{{ task.estimateTimeHour }}</span>
                            <span class="rate-title g-turbo-gray-font"> {{ $t('turbo.未加速耗时(h)') }} </span>
                        </p>
                        <p class="task-rate">
                            <span class="rate-num g-turbo-deep-black-font">{{ task.executeTimeHour }}</span>
                            <span class="rate-title g-turbo-gray-font"> {{ $t('turbo.实际耗时(h)') }} </span>
                        </p>
                        <p class="task-rate">
                            <span class="rate-num g-turbo-deep-black-font">{{ task.turboRatio }}</span>
                            <span class="rate-title g-turbo-gray-font"> {{ $t('turbo.节省率') }} </span>
                        </p>
                        <logo name="right-shape" size="16" :class="showIds.includes(task.planId) ? 'task-right-down task-right-shape' : 'task-right-shape'"></logo>
                    </h3>

                    <bk-table class="task-records g-turbo-scroll-table" v-if="showIds.includes(task.planId)"
                        :data="task.tableList"
                        :outer-border="false"
                        :header-border="false"
                        :header-cell-style="{ background: '#f5f6fa' }"
                        :pagination="task.pagination"
                        v-bkloading="{ isLoading: task.loading }"
                        :empty-text="$t('turbo.暂无数据')"
                        @page-change="(page) => pageChanged(page, task)"
                        @page-limit-change="(currentLimit) => pageLimitChange(currentLimit, task)"
                        @sort-change="(sort) => sortChange(sort, task)"
                        @row-click="(row) => rowClick(row, task)"
                    >
                        <bk-table-column :label="$t('turbo.流水线/构建机')" prop="pipeline_name" sortable show-overflow-tooltip>
                            <template slot-scope="props">
                                <span v-if="props.row.pipelineName">
                                    {{ props.row.pipelineName }}
                                    <a @click.stop :href="`/console/pipeline/${projectId}/${props.row.pipelineId}/edit`" target="_blank" class="g-turbo-click-text"><logo name="cc-jump-link" class="jump-link" size="14"></logo></a>
                                </span>
                                <span v-else>{{ props.row.clientIp }}</span>
                            </template>
                        </bk-table-column>
                        <bk-table-column :label="$t('turbo.加速次数')" prop="executeCount" sortable></bk-table-column>
                        <bk-table-column :label="$t('turbo.平均耗时')" prop="averageExecuteTimeValue" sortable></bk-table-column>
                        <bk-table-column :label="$t('turbo.节省率')" prop="turboRatio" sortable></bk-table-column>
                        <bk-table-column :label="$t('turbo.最新开始时间')" prop="latestStartTime" sortable></bk-table-column>
                        <bk-table-column :label="$t('turbo.最新状态')" prop="latestStatus" sortable>
                            <template slot-scope="props">
                                <task-status :status="props.row.latestStatus" :message="props.row.message"></task-status>
                            </template>
                        </bk-table-column>
                    </bk-table>
                </section>
            </main>
        </template>
        <permission-exception v-else :message="errMessage" />
    </article>
</template>

<script>
    import { getPlanList, getPlanInstanceDetail, modifyTurboPlanTopStatus } from '@/api'
    import { copyText } from '@/assets/js/util'
    import logo from '@/components/logo'
    import taskStatus from '@/components/task-status'
    import permissionException from '@/components/exception/permission.vue'

    export default {
        components: {
            logo,
            taskStatus,
            permissionException
        },

        data () {
            return {
                taskList: [],
                showIds: [],
                pageNum: 1,
                loadEnd: false,
                isLoadingMore: false,
                turboPlanCount: 0,
                isLoading: false,
                hasPermission: true,
                errMessage: ''
            }
        },

        computed: {
            projectId () {
                return this.$route.params.projectId
            }
        },

        watch: {
            projectId () {
                this.initData()
            }
        },

        created () {
            this.initData()
        },

        mounted () {
            const scrollMain = document.querySelector('.task-list-home')
            if (scrollMain) {
                scrollMain.addEventListener('scroll', this.scrollLoadMore, { passive: true })
                this.$once('hook:beforeDestroy', () => {
                    scrollMain.removeEventListener('scroll', this.scrollLoadMore, { passive: true })
                })
            }
        },

        methods: {
            initData () {
                this.isLoading = true
                this.pageNum = 1
                this.getPlanList().then(() => {
                    const firstTask = this.taskList[0]
                    if (firstTask) {
                        this.getPlanInstanceDetail(firstTask)
                        this.showIds = [firstTask.planId]
                    }
                }).finally(() => {
                    this.isLoading = false
                })
            },

            copy (value) {
                copyText(value, this.$t.bind(this))
            },

            modifyTurboPlanTopStatus (row) {
                const topStatus = row.topStatus === 'true' ? 'false' : 'true'
                modifyTurboPlanTopStatus(row.planId, topStatus).then(() => {
                    row.topStatus = topStatus
                }).catch((err) => {
                    this.$bkMessage({ theme: 'error', message: err.message || err })
                })
            },

            getPlanList (isLoadMore) {
                this.isLoadingMore = true
                return getPlanList(this.projectId, this.pageNum).then((res) => {
                    const taskList = (res.turboPlanList || []).map((item) => {
                        item.pagination = { current: 1, count: 1, limit: 10 }
                        item.tableList = []
                        item.loading = false
                        item.sortField = undefined
                        item.sortType = undefined
                        return item
                    })
                    if (isLoadMore) this.taskList.push(...taskList)
                    else this.taskList = taskList
                    this.turboPlanCount = res.turboPlanCount
                    this.loadEnd = (this.pageNum * 40) >= res.turboPlanCount
                    this.pageNum++
                    if (res.turboPlanCount <= 0) this.$router.replace({ name: 'taskInit' })
                }).catch((err) => {
                    if (err.code === 2300017) {
                        this.hasPermission = false
                        this.errMessage = err.message
                    } else {
                        this.$bkMessage({
                            message: err.message || err,
                            theme: 'error'
                        })
                    }
                }).finally(() => {
                    this.isLoadingMore = false
                })
            },

            toggleShowCard (task) {
                const index = this.showIds.findIndex(x => x === task.planId)
                if (index > -1) {
                    this.showIds.splice(index, 1)
                } else {
                    this.getPlanInstanceDetail(task)
                    this.showIds.push(task.planId)
                }
            },

            getPlanInstanceDetail (task) {
                const pagination = task.pagination || {}
                task.loading = true
                const queryData = {
                    pageNum: pagination.current,
                    pageSize: pagination.limit,
                    sortField: task.sortField,
                    sortType: task.sortType
                }
                getPlanInstanceDetail(task.planId, queryData).then((res) => {
                    task.tableList = res.records || []
                    if (task.pagination) {
                        task.pagination.count = res.count
                        if (task.pagination.count <= task.pagination.limit) task.pagination = undefined
                    }
                }).catch((err) => {
                    this.$bkMessage({ theme: 'error', message: err.message || err })
                }).finally(() => {
                    task.loading = false
                })
            },

            pageChanged (page, task) {
                task.pagination.current = page
                this.getPlanInstanceDetail(task)
            },

            pageLimitChange (currentLimit, task) {
                if (currentLimit === task.pagination.limit) return

                task.pagination.current = 1
                task.pagination.limit = currentLimit
                this.getPlanInstanceDetail(task)
            },

            sortChange (sort, task) {
                const sortMap = {
                    ascending: 'ASC',
                    descending: 'DESC'
                }
                task.sortField = sort.prop
                task.sortType = sortMap[sort.order]
                this.getPlanInstanceDetail(task)
            },

            rowClick (row, task) {
                const pipelineId = row.pipelineId
                const clientIp = row.clientIp
                const planId = task.planId
                this.$router.push({
                    name: 'history',
                    query: {
                        pipelineId,
                        planId,
                        clientIp
                    }
                })
            },

            addTask () {
                this.$router.push({
                    name: 'taskCreate'
                })
            },

            scrollLoadMore (event) {
                const target = event.target
                const bottomDis = target.scrollHeight - target.clientHeight - target.scrollTop
                if (bottomDis <= 500 && !this.loadEnd && !this.isLoadingMore) this.getPlanList(true)
            }
        }
    }
</script>

<style lang="scss" scoped>
    @import '../../assets/scss/var.scss';

    .task-list-home {
        padding: 20px;
        margin: 0 auto;
        .task-head {
            margin-bottom: 20px;
        }
        .task-head-title {
            margin-left: 14px;
            font-size: 12px;
            line-height: 32px;
        }
    }
    .task-card {
        margin-bottom: 20px;
        cursor: pointer;
        position: relative;
        overflow: hidden;
        &:hover {
            box-shadow: 0 2px 3px 0 rgba(0,0,0,0.15);
            .open-icon {
                display: block;
            }
        }
        .open-icon {
            display: none;
            position: absolute;
            left: -22px;
            top: -22px;
            width: 45px;
            height: 45px;
            transform: rotate(45deg);
            background: #DCDEE5;
            &.is-open {
                display: block;
                background: #3a84ff;
            }
            .icon-thumbtack {
                color: #fff;
                position: absolute;
                right: 3px;
                top: 16px;
                transform: rotate(-45deg);
            }
        }
        .card-head {
            border-bottom: 1px solid #f0f1f5;
            display: flex;
            align-items: center;
            padding: 0 28px 0 43px;
            font-weight: normal;
            height: 110px;
            &.disabled {
                .plan-name, .name-detail, .rate-num, .rate-title {
                    color: #c4c6cc;
                }
            }
        }
        .task-name {
            flex: 1;
            max-width: calc(100% - 380px - 3.76rem);
            .name-desc {
                line-height: 22px;
                display: inline-block;
                width: 90%;
                overflow: hidden;
                text-overflow: ellipsis;
            }
            .name-hash {
                font-size: 12px;
                line-height: 20px;
                margin-top: 5px;
                display: flex;
                align-items: center;
                .icon-copy {
                    margin-left: 4px;
                }
            }
            .name-detail {
                display: inline-block;
                background: #e1ecff;
                color: #3a84ff;
                border-radius: 2px;
                padding: 0 9px;
                font-size: 12px;
                line-height: 20px;
                margin: 0 6px;
                vertical-align: bottom;
            }
        }
        .task-line {
            width: 1px;
            height: round(54px * $designToPx);
            background: #e7e8ed;
            margin-right: .52rem;
        }
        .task-rate {
            width: 145px;
            margin-right: .2rem;
            text-align: center;
            &:nth-child(7) {
                margin-right: .36rem;
            }
            .rate-num {
                display: block;
                font-size: 24px;
                line-height: 32px;
                margin-bottom: 2px;
            }
            .rate-title {
                line-height: 20px;
                font-size: 12px;
            }
        }
        .task-right-shape {
            transition: 200ms transform;
            transform: rotate(90deg);
            color: #979ba5;
            &.task-right-down {
                transform: rotate(-90deg);
            }
        }
        .task-records {
            margin: 19px 28px 0;
            padding-bottom: 27px;
            width: calc(100% - 56px);
        }
        .jump-link {
            vertical-align: sub;
        }
        ::v-deep .bk-table-row {
            cursor: pointer;
        }
    }
</style>
