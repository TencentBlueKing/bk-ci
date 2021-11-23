<template>
    <bk-table
        size="small"
        style="margin-bottom: 10px;"
        :data="listWithConfig"
        :outer-border="false">
        <bk-table-column
            :label="$t('pipelineName')"
            prop="pipelineName"
            key="pipelineName"
            width="400">
            <template slot-scope="{ row }">
                <div class="table-list-name text-overflow"
                    :class="row.feConfig && row.feConfig.status">
                    <div
                        v-if="row.latestBuildStatus"
                        class="build-status-tips"
                        v-bk-tooltips="{ content: getStatusTips(row.latestBuildStatus), disabled: !row.latestBuildStatus }"
                    ></div>
                    <a
                        v-if="row.hasPermission"
                        :href="getHistoryURL(row.pipelineId)"
                        :title="row.pipelineName" class="text-link"
                        @click.prevent.stop="e => goHistory(e, row.pipelineId)">
                        {{ row.pipelineName }}
                    </a>
                    <span v-else>{{ row.pipelineName }}</span>
                </div>
            </template>
        </bk-table-column>
        <bk-table-column
            prop="hasPermission"
            key="hasPermission"
            width="400">
            <template slot-scope="{ row }">
                <div v-if="row.hasPermission">
                    <progress-bar
                        class="table-list-progress mr15"
                        v-if="row.feConfig && row.feConfig.status !== 'success' && row.feConfig.status !== 'not_built' && row.feConfig.status !== 'known_error' && row.feConfig.status !== 'known_cancel'"
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
                    <span
                        class="row-item-desc text-overflow mr15"
                        v-if="row.feConfig && row.feConfig.runningInfo.log && row.feConfig.status === 'running'">
                        {{ row.feConfig.runningInfo.log }}
                    </span>
                    <a
                        href="javascript:;"
                        class="text-link item-text-btn"
                        v-if="row.feConfig && row.feConfig.status === 'running'"
                        @click.stop.prevent="emitEventHandler('terminate-pipeline', row.pipelineId)">
                        {{ $t('terminate') }}
                    </a>
                    <a
                        href="javascript:;"
                        class="text-link item-text-btn noticed"
                        v-if="(row.feConfig && row.feConfig.status === 'error') || (row.feConfig && row.feConfig.status === 'cancel')"
                        @click.stop.prevent="emitEventHandler('error-noticed', row.pipelineId)">
                        {{ $t('newlist.known') }}
                        <i class="devops-icon icon-check-1"></i>
                    </a>
                </div>
                <div v-else></div>
            </template>
        </bk-table-column>
        <bk-table-column
            :label="$t('newlist.totalAtomNums')"
            prop="taskCount"
            key="taskCount">
            <template slot-scope="{ row }">
                <a
                    v-if="row.hasPermission"
                    href="javascript:;"
                    class="text-link row-task-count"
                    @click.stop.prevent="$router.push({
                        name: 'pipelinesEdit',
                        params: {
                            pipelineId: row.pipelineId
                        }
                    })">
                    {{ row.taskCount }}
                </a>
                <span v-else>{{ row.taskCount }}</span>
            </template>
        </bk-table-column>
        <bk-table-column
            :label="$t('newlist.execTimes')"
            prop="buildCount"
            key="buildCount">
            <template slot-scope="{ row }">
                <a
                    v-if="row.hasPermission"
                    href="javascript:;"
                    class="text-link row-build-count"
                    @click.stop.prevent="$router.push({
                        name: 'pipelinesHistory',
                        params: {
                            pipelineId: row.pipelineId
                        }
                    })">
                    {{ row.buildCount }}
                </a>
                <span v-else>{{ row.buildCount }}</span>
            </template>
        </bk-table-column>
        <bk-table-column
            :label="$t('lastBuildNum')"
            prop="lastExecTime"
            key="lastExecTime">
            <template slot-scope="{ row }">
                {{ row.feConfig && row.feConfig.content[0].value }}
            </template>
        </bk-table-column>
        <bk-table-column
            :label="$t('lastExecTime')"
            prop="lastExecTime"
            key="lastExecTime">
            <template slot-scope="{ row }">
                <div>{{ calcLatestStartBuildTime(row) }}</div>
            </template>
        </bk-table-column>
        <bk-table-column
            :label="$t('creator')"
            prop="creator"
            key="creator" />
        <bk-table-column
            :label="$t('operate')"
            prop="action"
            key="action"
            class-name="option-menu">
            <template slot-scope="{ row }">
                <ext-menu
                    v-if="row.hasPermission"
                    :config="row.feConfig">
                </ext-menu>
                <div class="option-text-link" v-else>
                    <a href="javascript:;"
                        class="text-link"
                        @click.stop.prevent="applyPermission(row)">
                        {{ $t('newlist.applyPerm') }}
                    </a>
                </div>
            </template>
        </bk-table-column>
    </bk-table>
</template>

<script>
    import { convertMStoString } from '@/utils/util'
    import mixins from '../pipeline-list-mixins'
    export default {
        mixins: [mixins],
        props: {
            list: {
                type: Array,
                default: () => []
            },
            pipelineFeConfMap: {
                type: Object,
                deafult: () => ({})
            }
        },
        data () {
            return {
                isShowExtMenu: false
            }
        },
        computed: {
            listWithConfig () {
                return this.list.map(item => ({
                    ...item,
                    feConfig: this.pipelineFeConfMap[item.pipelineId] || {}
                }))
            }
        },
        methods: {
            localConvertMStoString (num) {
                return convertMStoString(num)
            },
            calcLatestStartBuildTime (row) {
                if (row.latestBuildStartTime) {
                    try {
                        const timeLocaleString = this.localConvertMStoString(row.currentTimestamp - row.latestBuildStartTime)
                        if (window.pipelineVue.$i18n && window.pipelineVue.$i18n.locale === 'en-US') {
                            return timeLocaleString
                        } else {
                            let result = timeLocaleString.match(/^[0-9]{1,}([\u4e00-\u9fa5]){1,}/)[0]
                            if (result.indexOf('分') > 0) {
                                result += '钟'
                            }
                            return `${result}前`
                        }
                    } catch (err) {
                        return '---'
                    }
                } else {
                    return '--'
                }
            },
            getStatusTips (status) {
                const statusTipsMap = {
                    SUCCEED: this.$t('newlist.success'),
                    FAILED: this.$t('newlist.failed'),
                    CANCELED: this.$t('newlist.cancel'),
                    STAGE_SUCCESS: this.$t('newlist.stageSuccess')
                }
                return statusTipsMap[status] || ''
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
        .option-menu .cell {
            overflow: visible;
        }
        .option-text-link {
            overflow: hidden;
        }
        .bk-table th>.cell {
            height: 60px;
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
    .latest-build-number {
        max-width: 110px;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
        padding-right: 25px !important;
    }
</style>
