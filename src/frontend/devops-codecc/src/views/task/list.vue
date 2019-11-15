<template>
    <div class="main-content-outer main-content-task">
        <div v-if="!isEmpty && projectId">
            <div class="toolbar">
                <bk-button icon="plus" theme="primary" @click="$router.push({ name: 'task-new' })">{{$t("nav.新建任务")}}</bk-button>
            </div>
            <div class="task-list-inuse">
                <div class="task-card-list">
                    <router-link
                        tag="div"
                        class="task-card-item"
                        v-for="(task, taskIndex) in list.enableTasks"
                        :to="getTaskLink(task)"
                        :key="taskIndex"
                    >
                        <task-card :task="task"></task-card>
                    </router-link>
                </div>
            </div>
            <div class="task-list-disused">
                <div class="disused-head">
                    <div class="title" @click="toggleDisused">{{$t("nav.已停用任务")}} <i :class="['bk-icon icon-angle-right arrow-icon', { open: isShowDisused }]"></i></div>
                </div>
                <div class="disused-body" v-show="isShowDisused">
                    <div class="task-card-list">
                        <router-link
                            tag="div"
                            :to="{ name: 'task-detail', params: { projectId, taskId: task.taskId } }"
                            class="task-card-item"
                            v-for="(task, index) in list.disableTasks"
                            :key="index"
                        >
                            <task-card :task="task"></task-card>
                        </router-link>
                    </div>
                </div>
            </div>
        </div>
        <div v-else-if="!projectId" class="no-task" v-show="!projectId">
            <empty :title="$t('st.暂无项目')" :desc="$t('st.你可以通过按钮跳转至项目管理，来创建新项目')">
                <template v-slot:action>
                    <bk-button size="large" theme="primary" @click="createProject">{{$t("st.项目管理")}}</bk-button>
                </template>
            </empty>
        </div>
        <div v-else-if="projectId && isEmpty" class="no-task" v-show="isEmpty">
            <empty :title="$t('st.暂无任务')" :desc="$t('st.你可以通过新增按钮，来创建代码检查任务')">
                <template v-slot:action>
                    <bk-button size="large" theme="primary" @click="$router.push({ name: 'task-new' })">{{$t("nav.新建任务")}}</bk-button>
                </template>
            </empty>
        </div>
    </div>
</template>

<script>
    import { mapState } from 'vuex'
    import Empty from '@/components/empty'
    import TaskCard from './task-card'

    export default {
        components: {
            Empty,
            TaskCard
        },
        data () {
            return {
                isShowDisused: false,
                list: {
                    enableTasks: [],
                    disableTasks: []
                }
            }
        },
        computed: {
            ...mapState([
                'toolMeta'
            ]),
            isEmpty () {
                if (!this.list) {
                    return
                }
                return !this.list.enableTasks.length && !this.list.disableTasks.length
            },
            projectId () {
                return this.$route.params.projectId
            }
        },
        methods: {
            async fetchPageData () {
                const res = await this.$store.dispatch('task/list')
                this.list = res
            },
            getTaskIconColorClass (taskId) {
                return `c${(taskId % 4) + 1}`
            },
            toggleDisused () {
                this.isShowDisused = !this.isShowDisused
            },
            getTaskLink (task) {
                const link = { name: 'task-detail', params: { projectId: this.projectId, taskId: task.taskId } }
                if (!task.toolConfigInfoList.length) {
                    link.name = 'task-new'
                    link.query = { step: 'tools' }
                }

                return link
            },
            createProject () {
                window.open(`${window.DEVOPS_SITE_URL}/console/pm/`)
            }
        }
    }
</script>

<style lang="postcss">
    @import '../../css/mixins.css';
    @import '../../css/main-content-outer.css';

    .main-content-task {
        .toolbar {
            margin-bottom: 16px;
        }
    }

    .task-card-list {
        display: flex;
        flex-wrap: wrap;
        .task-card-item {
            margin: 0 16px 16px 0;
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

    @media only screen and (max-width: 860px) {
        .main-content-task {
            width: 788px!important;
        }
        .main-content-task .task-card-list .task-card-item:nth-child(3n) {
            margin-right: 0;
        }
    }
    @media only screen and (min-width: 860px) and (max-width: 1694px) {
        .main-content-task {
            width: 1056px!important;
        }
        .main-content-task .task-card-list .task-card-item:nth-child(4n) {
            margin-right: 0;
        }
    }
    @media only screen and (min-width: 1695px) and (max-width: 2025px) {
        .main-content-task {
            width: 1592px!important;;
        }
        .main-content-task .task-card-list .task-card-item:nth-child(6n) {
            margin-right: 0;
        }
    }
</style>
