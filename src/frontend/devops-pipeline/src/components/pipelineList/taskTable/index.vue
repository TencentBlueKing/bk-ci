<template>
    <table class="bk-table pipeline-list-table">
        <thead>
            <tr>
                <th width="20%" class="pl30">任务名称</th>
                <th width="30%"></th>
                <th width="8%">插件总数</th>
                <th width="8%">已执行次数</th>
                <th width="8%">最新构建号</th>
                <th width="15%">执行开始时间</th>
                <th width="10%">操作</th>
            </tr>
        </thead>
        <tbody>
            <template
                v-for="(row, index) of list">
                <tr
                    style="position: relative"
                    :key="`taskTable${index}_re`">
                    <td class="table-list-name text-overflow"
                        :class="row.feConfig && row.feConfig.status">
                        <a v-if="row.hasPermission" href="javascript:;" class="text-link" @click="emitEventHandler('title-click', row.pipelineId)">
                            {{ row.pipelineName }}
                        </a>
                        <span v-else>{{ row.pipelineName }}</span>
                    </td>
                    <td v-if="row.hasPermission">
                        <progress-bar class="table-list-progress mr15"
                            v-if="row.feConfig && row.feConfig.status !== 'success' && row.feConfig.status !== 'not_built' && row.feConfig.status !== 'known_error'"
                            :percentage="row.feConfig && row.feConfig.runningInfo.percentage"
                            :status="row.feConfig && row.feConfig.status"
                            :has-icon="true">
                        </progress-bar>
                        <triggers
                            v-else
                            class="inline-component"
                            :pipeline-id="row.pipelineId"
                            :can-manual-startup="row.canManualStartup"
                            @exec="triggersExec">
                        </triggers>
                        <span class="row-item-desc text-overflow mr15"
                            v-if="row.feConfig && row.feConfig.runningInfo.log && row.feConfig.status === 'running'">
                            {{ row.feConfig.runningInfo.log }}
                        </span>
                        <a href="javascript:;" class="text-link item-text-btn"
                            v-if="row.feConfig && row.feConfig.status === 'running'"
                            @click.stop.prevent="emitEventHandler('terminate-pipeline', row.pipelineId)">
                            终止
                        </a>
                        <a href="javascript:;" class="text-link item-text-btn noticed"
                            v-if="row.feConfig && row.feConfig.status === 'error'"
                            @click.stop.prevent="emitEventHandler('error-noticed', row.pipelineId)">
                            知道了
                            <i class="bk-icon icon-check-1"></i>
                        </a>
                    </td>
                    <td v-else></td>
                    <td>
                        <a v-if="row.hasPermission" href="javascript:;" class="text-link row-task-count"
                            @click.stop.prevent="$router.push({
                                name: 'pipelinesEdit',
                                params: {
                                    pipelineId: row.pipelineId
                                }
                            })">
                            {{ row.taskCount }}
                        </a>
                        <span v-else>{{ row.taskCount }}</span>
                    </td>
                    <td>
                        <a v-if="row.hasPermission" href="javascript:;" class="text-link row-build-count"
                            @click.stop.prevent="$router.push({
                                name: 'pipelinesHistory',
                                params: {
                                    pipelineId: row.pipelineId
                                }
                            })">
                            {{ row.buildCount }}
                        </a>
                        <span v-else>{{ row.buildCount }}</span>
                    </td>
                    <td>{{ row.feConfig && row.feConfig.content[0].value }}</td>
                    <td>{{ calcLatestStartBuildTime(row) }}</td>
                    <td>
                        <ext-menu
                            v-if="row.hasPermission"
                            :config="row.feConfig">
                        </ext-menu>
                        <a v-else href="javascript:;" class="text-link"
                            @click.stop.prevent="applyPermission(row.pipelineName, row.pipelineId)">
                            申请权限
                        </a>
                    </td>
                </tr>
            </template>
        </tbody>
    </table>
</template>

<script>
    import progressBar from '@/components/devops/progressBar'
    import triggers from '@/components/pipeline/triggers'
    import extMenu from '@/components/pipelineList/extMenu'
    import { bus } from '@/utils/bus'
    import { convertMStoString } from '@/utils/util'
    export default {
        components: {
            progressBar,
            triggers,
            extMenu
        },
        props: {
            list: {
                type: Array,
                default: []
            }
        },
        data () {
            return {
                isShowExtMenu: false
            }
        },
        methods: {
            localConvertMStoString (num) {
                return convertMStoString(num)
            },
            calcLatestStartBuildTime (row) {
                if (row.latestBuildStartTime) {
                    try {
                        let result = this.localConvertMStoString(row.currentTimestamp - row.latestBuildStartTime).match(/^[0-9]{1,}([\u4e00-\u9fa5]){1,}/)[0]
                        if (result.indexOf('分') > 0) {
                            result += '钟'
                        }
                        return `${result}前`
                    } catch (err) {
                        return '---'
                    }
                } else {
                    return '--'
                }
            },
            triggersExec ({ pipelineId, ...params }) {
                bus.$emit('triggers-exec', params, pipelineId)
            },
            /**
             *  参数为pipelineId的触发全局bus事件
             */
            emitEventHandler (eventName, pipelineId) {
                bus.$emit(eventName, pipelineId)
            },
            applyPermission (pipelineName, pipelineId) {
                bus.$emit('set-permission', `流水线：${pipelineName}`, '查看', pipelineId)
            }
        }
    }
</script>

<style lang="scss">
    @import './../../../scss/conf';
    .pipeline-list-table {
        :before {
            height: 0px;
        }
        overflow: visible;
        border-top: 0;
        tr {
            background-color: transparent;
            position: relative;
            height: 60px;
            width: 100%;
            &:hover {
                background-color: transparent;
            }
        }
        >thead>tr>th {
            border: 0;
        }
     }
    .table-tr-overflow {
        position: absolute;
        width: calc(100% - 60px);
        border-top: 1px solid #fff;
        .apply-button {
            position: absolute;
            top: calc(((100% - 36px) / 2) - 60px);
            right: 0;
            bottom: 0;
            left: calc((100% - 96px) / 2);
            z-index: 4;
            opacity: 1;
        }
         .table-list-overflow {
            width: 100%;
            height: 100%;
            position: absolute;
            top: -100%;
            right: 0;
            bottom: 0;
            left: 0;
            background-color: #333C48;
            z-index: 2;
            opacity: 0.6;
        }
    }
</style>
