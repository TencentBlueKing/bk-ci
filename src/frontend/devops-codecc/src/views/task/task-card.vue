<template>
    <div class="task-card">
        <div class="card-head">
            <div :class="['task-icon', getTaskIconColorClass(task.taskId)]">{{(task.nameCn || '')[0]}}</div>
            <div class="task-title" :title="task.nameCn">{{task.nameCn}}</div>
            <i class="bk-icon icon-pipeline" v-if="task.createFrom === 'bs_pipeline'"></i>
        </div>
        <div class="card-body">
            <div class="tool-anls-list" v-if="task.toolConfigInfoList.length">
                <div class="anls-item" v-for="(tool, toolIndex) in taskTools" :key="toolIndex">
                    <i class="bk-icon status-icon"
                        :class="{ 'icon-check-circle success': tool.stepStatus === 0, 'icon-close-circle-shape fail': tool.stepStatus === 1 }"
                    ></i>
                    <div class="bk-spin-loading bk-spin-loading-mini bk-spin-loading-primary" v-if="tool.stepStatus === 3">
                        <div class="rotate rotate1"></div>
                        <div class="rotate rotate2"></div>
                        <div class="rotate rotate3"></div>
                        <div class="rotate rotate4"></div>
                        <div class="rotate rotate5"></div>
                        <div class="rotate rotate6"></div>
                        <div class="rotate rotate7"></div>
                        <div class="rotate rotate8"></div>
                    </div>
                    <div class="toolname" :title="tool.displayName">
                        <router-link :to="getDefectListRoute(tool)">{{tool.displayName}}</router-link>
                    </div>
                    <div class="anlstime">{{formatAnlsTime(tool.updatedDate)}}</div>
                </div>
            </div>
            <div class="no-tool-anls" v-else>{{$t('task.暂未添加工具')}}</div>
        </div>
    </div>
</template>

<script>
    import { mapState } from 'vuex'
    import { format } from 'date-fns'

    export default {
        props: {
            task: {
                type: Object,
                default () {
                    return {}
                }
            }
        },
        computed: {
            ...mapState('tool', {
                toolMap: 'mapList'
            }),
            taskTools () {
                for (let i in this.task.toolConfigInfoList) {
                    if (this.task.toolConfigInfoList[i].curStep < 5 && this.task.toolConfigInfoList[i].curStep > 0 && this.task.toolConfigInfoList[i].stepStatus !== 1)
                    this.task.toolConfigInfoList[i].stepStatus = 3
                }
                // 过滤掉停用工具并且只取前3个
                const taskTools = (this.task.toolConfigInfoList || []).filter(task => task.followStatus !== 6)
                return taskTools.slice(0, 3)
            }
        },
        methods: {
            getTaskIconColorClass (taskId) {
                return `c${(taskId % 4) + 1}`
            },
            formatAnlsTime (time) {
                return format(time, 'YYYY-MM-DD HH:mm:ss')
            },
            getDefectListRoute (tool) {
                const toolMore = this.toolMap[tool.toolName] || {}
                return {
                    name: toolMore.routes ? toolMore.routes.defectList : '',
                    params: { ...this.$route.params, taskId: tool.taskId, toolId: tool.toolName }
                }
            }
        }
    }
</script>

<style lang="postcss">
    @import '../../css/mixins.css';
    @import '../../css/variable.css';
    @import '../../assets/bk_icon_font/style.css';

    .task-card {
        flex: none;
        width: 252px;
        height: 160px;
        border: 1px solid #dcdee5;
        background: #fff;
        border-radius: 2px;
        cursor: pointer;
        box-shadow: rgba(0, 0, 0, 0.04) 0px 3px 6px;
        transition: all 0.3s ease;
        .card-head {
            display: flex;
            height: 68px;
            align-items: center;
            padding: 0 20px;
            background: #fafbfd;
            border-radius: 2px 2px 0px 0px;

            .icon-pipeline {
                color: #a3c5fd;
                margin-left: 4px;
                margin-top: 3px;
            }
        }
        .card-body {
            padding: 12px 20px 0;
        }
        .task-icon {
            flex: none;
            width: 38px;
            height: 38px;
            font-size: 16px;
            margin-right: 12px;
            text-align: center;
            line-height: 38px;
            color: #fff;
            border-radius: 2px;
            &.c1 { background: #ffb848; }
            &.c2 { background: #cc9240; }
            &.c3 { background: #9fc06c; }
            &.c4 { background: #bd5f7d; }
        }
        .task-title {
            @mixin ellipsis;
        }

        &:hover {
            box-shadow: 0 3px 8px 0 rgba(0, 0, 0, 0.2), 0 0 0 1px rgba(0, 0, 0, 0.08);
        }
    }

    .tool-anls-list {
        .anls-item {
            display: flex;
            font-size: 12px;
            align-items: center;
            margin: 8px 0;

            &:first-child {
                margin-top: 0;
            }
            &:last-child {
                margin-bottom: 0;
            }

            .status-icon {
                margin-right: 8px;
                &.success {
                    color: #45e35f;
                }
                &.analyzing {
                    width: 12px;
                    height: 12px;
                    background: url(../../images/processing.png) no-repeat;
                    background-size: contain;
                }
                &.fail {
                    color: #ff5656;
                }
            }
            .toolname {
                flex: 1;
                color: #63656E;
                margin-right: 4px;
                @mixin ellipsis;
            }
            .anlstime {
                color: $itemBorderColor;
            }
        }
    }

    .no-tool-anls {
        text-align: center;
        font-size: 12px;
        color: #979ba5;
        margin-top: 24px;
    }
</style>
