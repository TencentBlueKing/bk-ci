<template>
    <div class="main-content-outer main-content-task">
        <div v-if="!isFetched">
            <tool-bar :task-count="renderList.length" :search-info="searchInfo" @changeOrder="changeOrder"></tool-bar>
            <div class="task-list-inuse" v-if="!isEmpty">
                <div class="task-card-list">
                    <div class="task-card-item"
                        v-for="(task, taskIndex) in renderList"
                        :key="taskIndex">
                        <task-card :task="task" :get-task-link="getTaskLink" :handle-task="handleTask"></task-card>
                    </div>
                </div>
            </div>
        </div>
        <div v-else-if="projectId && (!isEmpty || isSearch)">
            <tool-bar :task-count="renderList.length" :search-info="searchInfo" @changeOrder="changeOrder"></tool-bar>
            <div class="task-list-inuse" v-if="!isEmpty || isSearch">
                <div class="task-card-list">
                    <div class="task-card-item"
                        v-for="(task, taskIndex) in renderList"
                        :key="taskIndex">
                        <task-card :task="task" :get-task-link="getTaskLink" :handle-task="handleTask"></task-card>
                    </div>
                </div>
            </div>
            <div slot="empty" v-else>
                <div class="codecc-table-empty-text">
                    <img src="../../images/empty.png" class="empty-img">
                    <div>{{$t('暂无数据')}}</div>
                </div>
            </div>
        </div>
        <div v-else-if="!projectId" class="no-task" v-show="!projectId">
            <empty :title="$t('暂无项目')" :desc="$t('你可以通过按钮跳转至项目管理，来创建新项目')">
                <template v-slot:action>
                    <bk-button size="large" theme="primary" @click="createProject">{{$t('项目管理')}}</bk-button>
                </template>
            </empty>
        </div>
        <div v-else-if="projectId && isEmpty && !isSearch && !mainContentLoading" class="no-task" v-show="isEmpty">
            <empty :title="$t('暂无任务')" :desc="$t('你可以通过新增按钮，来创建代码检查任务')">
                <template v-slot:action>
                    <bk-button size="large" theme="primary" @click="$router.push({ name: 'task-new' })">{{$t('新增任务')}}</bk-button>
                </template>
            </empty>
        </div>
        <bk-dialog v-model="dialogVisible"
            :theme="'primary'"
            :mask-close="false"
            @confirm="reAnalyse"
            :title="$t('重新检查')">
            {{this.$t('任务正在分析中，是否中断并重新分析？')}}
        </bk-dialog>
    </div>
</template>

<script>
    import { mapState, mapGetters } from 'vuex'
    import Empty from '@/components/empty'
    import TaskCard from './task-card'
    import ToolBar from './tool-bar'
    import projectWebSocket from '@/common/projectWebSocket'

    export default {
        components: {
            Empty,
            TaskCard,
            ToolBar
        },
        data () {
            return {
                retryTask: {},
                isShowDisused: false,
                dialogVisible: false,
                isSearch: false,
                isFetched: false,
                orderType: 'CREATE_DATE',
                list: {
                    enableTasks: [],
                    disableTasks: []
                },
                searchInfo: {
                    taskStatus: '',
                    taskSource: ''
                    // codelib: ''
                }
            }
        },
        computed: {
            ...mapState([
                'toolMeta'
            ]),
            ...mapGetters(['mainContentLoading']),
            renderList () {
                if (!this.list) {
                    return []
                }
                return this.list.enableTasks.concat(this.list.disableTasks)
            },
            isEmpty () {
                return !this.renderList.length
            },
            projectId () {
                return this.$route.params.projectId
            }
        },
        watch: {
            searchInfo: {
                deep: true,
                handler (newVal) {
                    this.isSearch = true
                    this.fetchPageData(true)
                }
            }
        },
        mounted () {
            this.initWebSocket()
        },
        beforeDestroy () {
            projectWebSocket.disconnect()
        },
        methods: {
            async fetchPageData (query = {}) {
                const params = Object.assign({}, this.searchInfo, { orderType: this.orderType, showLoading: true })
                const res = await this.$store.dispatch('task/list', params)
                this.isFetched = true
                const idList = []
                this.list = res
                this.list.enableTasks.map(task => {
                    if (task.createFrom === 'bs_pipeline') idList.push(task.taskId)
                })
                this.requestMatchCodelib(idList)
            },
            async handleTask (task, type) {
                if (type === 'top') {
                    this.toggleTaskTop(task)
                } else if (type === 'execute') {
                    this.analyse(task)
                } else if (type === 'retry') {
                    this.dialogVisible = true
                    this.retryTask = task
                } else if (type) {
                    this.enableTask(task)
                }
            },
            toggleTaskTop (task) {
                const params = {
                    taskId: task.taskId,
                    topFlag: task.topFlag !== 1
                }
                this.$store.dispatch('task/editTaskTop', params).then(res => {
                    if (res.code === '0') {
                        this.fetchPageData()
                    }
                }).catch(e => {
                    this.$bkMessage({ theme: 'error', message: this.$t('置顶失败') })
                })
            },
            enableTask (task) {
                if (task.createFrom === 'bs_codecc') {
                    this.$store.commit('updateTaskId', task.taskId)
                    this.$store.dispatch('task/startManage', task.taskId).then(res => {
                        if (res.data === true) {
                            this.$bkMessage({
                                theme: 'success',
                                message: this.$t('启用任务成功')
                            })
                        }
                        this.fetchPageData()
                    }).catch(e => {
                        console.error(e)
                    })
                } else {
                    window.open(`${window.DEVOPS_SITE_URL}/console/pipeline/${task.projectId}/${task.pipelineId}/edit`, '_blank')
                }
            },
            async analyse (task) {
                if (task.createFrom.indexOf('pipeline') !== -1) {
                    const { projectId, pipelineId } = task
                    this.$bkInfo({
                        title: this.$t('开始检查'),
                        subTitle: this.$t('此代码检查任务需要到流水线启动，是否前往流水线？'),
                        maskClose: true,
                        confirmFn (name) {
                            window.open(`${window.DEVOPS_SITE_URL}/console/pipeline/${projectId}/${pipelineId}/edit`, '_blank')
                        }
                    })
                } else {
                    this.$store.commit('updateTaskId', task.taskId)
                    await this.$store.dispatch('task/triggerAnalyse').then(res => {
                        if (res.code === '0') {
                            this.$bkMessage({
                                theme: 'success',
                                message: this.$t('触发成功')
                            })
                            // this.fetchPageData()
                            this.retryTask = {}
                        }
                    })
                }
            },
            reAnalyse () {
                this.analyse(this.retryTask, 1)
            },
            requestMatchCodelib (list) {
                if (list) {
                    this.$store.dispatch('task/requestRepolist', list).then(res => {
                        const codelibMapList = res || {}
                        this.updateTaskList(codelibMapList)
                    })
                } else {
                    this.updateTaskList()
                }
            },
            updateTaskList (codelibList) {
                this.list.enableTasks = this.list.enableTasks.map(item => {
                    return {
                        ...item,
                        codelib: item.createFrom === 'bs_pipeline' ? codelibList && codelibList[item.taskId] ? codelibList[item.taskId] : [] : `${item.aliasName}@${item.branch}`
                    }
                })
            },
            changeOrder (order) {
                if (order !== this.orderType) {
                    this.orderType = order
                    this.fetchPageData()
                }
            },
            toggleDisused () {
                this.isShowDisused = !this.isShowDisused
            },
            getTaskLink (task, type) {
                const link = { params: { projectId: this.projectId, taskId: task.taskId } }
                if (['detail', 'setting'].includes(type)) {
                    link.name = type === 'setting' ? 'task-settings-code' : 'task-detail'
                    // if (!task.toolConfigInfoList.length) {
                    //     link.name = 'task-new'
                    //     link.query = { step: 'tools' }
                    // }
                } else if (type === 'logs') {
                    link.name = 'task-detail-logs'
                    link.params.toolId = task.displayToolName
                }

                this.$router.push(link)
            },
            createProject () {
                window.open(`${window.DEVOPS_SITE_URL}/console/pm/`)
            },
            initWebSocket () {
                const subscribe = `/topic/analysisProgress/projectId/${this.projectId}`
                
                projectWebSocket.connect(this.projectId, subscribe, {
                    success: (res) => {
                        const data = JSON.parse(res.body)
                        console.log(data)
                        this.list.enableTasks.forEach(task => {
                            if (task.taskId === data.taskId) {
                                Object.assign(task, data)
                            }
                        })
                    },
                    error: (message) => this.$showTips({ message, theme: 'error' })
                })
            }
        }
    }
</script>

<style lang="postcss" scoped>
    @import '../../css/mixins.css';
    @import '../../css/main-content-outer.css';

    .main-content-outer {
        width: 1236px;
    }
    .main-content-task {
        .toolbar {
            margin-bottom: 16px;
        }
        .codecc-table-empty-text {
            text-align: center;
            padding-top: 120px;
        }
    }

    .task-card-list {
        display: flex;
        flex-wrap: wrap;
        .task-card-item {
            margin: 0 20px 16px 0;
            float: left;
        }
    }

    .task-list-disused {
        .disused-head {
            border-bottom: 1px solid #dcdee5;
            padding: 8px 0;
            .title {
                display: inline-block;
                color: #313238;
                cursor: pointer;
                .arrow-icon {
                    font-size: 11px;
                    color: #313238;
                    @mixin transition-rotate 0, 90;
                }
            }
        }
        .disused-body {
            margin-top: 12px;
        }
    }

    .no-task {
        display: flex;
        align-items: center;
        justify-content: center;
        height: 100%;
    }
</style>
