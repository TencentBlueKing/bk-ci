<template>
    <div>
        <div class="breadcrumb">
            <div class="breadcrumb-name"><i class="bk-icon icon-arrows-left back" @click="backToDetail"></i>{{$t(`toolName.${toolDisplayName}`)}} {{$t('detail.分析记录')}}</div>
        </div>
        <div class="main-container">
            <bk-table
                :data="taskLogContent"
                :size="size"
                row-key="buildId"
                :expand-row-keys="[taskLogContent[0].buildId]"
                :pagination="pagination"
                @page-change="handlePageChange"
                @page-limit-change="handlePageLimitChange">
                <bk-table-column type="expand" width="30">
                    <template slot-scope="props">
                        <bk-table
                            :data="props.row.stepArray"
                            :outer-border="false"
                            :header-cell-style="{ borderRight: 'none' }">
                            <bk-table-column :label="$t('st.步骤')">
                                <template slot-scope="scope">
                                    <i class="step" :class="{ status: scope.row.index === 1, success: scope.row.flag === '成功', fail: scope.row.flag === '失败' || scope.row.flag === '中断' }"></i>
                                    <span>{{ scope.row.stepNum }}</span>
                                </template>
                            </bk-table-column>
                            <bk-table-column :label="$t('st.状态')">
                                <template slot-scope="scope">
                                    <span :class="{ status: scope.row.index === 1, success: scope.row.flag === '成功', fail: scope.row.flag === '失败' || scope.row.flag === '中断' }">
                                        {{ scope.row.flag }}
                                    </span>
                                </template>
                            </bk-table-column>
                            <bk-table-column :label="$t('st.信息')">
                                <template slot-scope="scope">
                                    <span :title="scope.row.msg">{{ scope.row.msg }}</span>
                                </template>
                            </bk-table-column>
                            <bk-table-column :label="$t('st.耗时')" prop="elapseTime"></bk-table-column>
                            <bk-table-column :label="$t('st.开始时间')" prop="startTime"></bk-table-column>
                            <bk-table-column :label="$t('st.结束时间')" prop="endTime"></bk-table-column>
                        </bk-table>
                    </template>
                </bk-table-column>
                <bk-table-column :label="$t('st.序号')" type="index"></bk-table-column>
                <bk-table-column :label="$t('st.构建号')" prop="buildNum"></bk-table-column>
                <bk-table-column :label="$t('st.状态')">
                    <template slot-scope="scope">
                        <span class="status" :class="{ success: scope.row.flag === '成功', fail: scope.row.flag === '失败' || scope.row.flag === '中断' }">
                            {{ scope.row.flag }}
                        </span>
                    </template>
                </bk-table-column>
                <bk-table-column :label="$t('st.耗时')" prop="elapseTime"></bk-table-column>
                <bk-table-column :label="$t('st.开始时间')" prop="startTime"></bk-table-column>
                <bk-table-column :label="$t('st.结束时间')" prop="endTime"></bk-table-column>
                <bk-table-column :label="$t('st.操作')" width="150">
                    <template slot-scope="scope">
                        <bk-button theme="primary" text @click="showLog(scope.row.buildId)">{{$t('st.查看日志')}}</bk-button>
                    </template>
                </bk-table-column>
            </bk-table>
            <bk-sideslider
                style="margin-top: 50px;z-index: 4;"
                class="sideslider"
                width="800"
                @hidden="_destroyLog()"
                :is-show.sync="slider.isShow"
                :title="slider.title"
                :quick-close="true"
            >
                <div slot="content">
                    <div class="sideslider-header-extra">
                        <bk-button theme="default" @click="showLog('', time = true)">{{$t('st.显示时间')}}</bk-button>
                        <a class="bk-button bk-button-normal export-log" download="" :href="downloadUrl">{{$t('st.导出日志')}}</a>
                    </div>
                    <div id="logContainer" ref="logContainer">
                        自定义显示的内容
                    </div>
                </div>
            </bk-sideslider>
        </div>
    </div>
</template>
<script>
    import { format } from 'date-fns'
    import { getToolStatus, getLogFlag, formatSeconds } from '@/common/util'
    import { mapState } from 'vuex'
    import taskWebsocket from '@/common/taskWebSocket'
    // const SodaLog = require('@/common/log.1.0.5.min')

    export default {
        props: {
            taskLog: {
                type: Object,
                default () {
                    return {
                        taskLogPage: {}
                    }
                }
            }
        },
        data () {
            return {
                size: 'small',
                pagination: {
                    current: 1,
                    count: 100,
                    limit: 10
                },
                slider: {
                    isShow: false,
                    title: this.$t('st.查看日志')
                },
                params: {
                    toolName: this.$route.params.toolId,
                    page: 1,
                    pageSize: 10
                },
                projectId: this.$route.params.projectId,
                taskId: this.$route.params.taskId,
                toolName: this.$route.params.toolId,
                downloadUrl: '',
                buildId: '',
                hasTime: false
            }
        },
        computed: {
            ...mapState('tool', {
                toolMap: 'mapList'
            }),
            ...mapState('task', {
                taskInfo: 'detail'
            }),
            toolId () {
                return this.$route.params.toolId
            },
            toolDisplayName () {
                return this.toolMap[this.toolId] && this.toolMap[this.toolId].displayName
            },
            taskLogContent () {
                const content = this.taskLog.taskLogPage.content || []
                const newContent = []
                if (content.length) {
                    content.forEach(log => {
                        let { currStep, flag, startTime, endTime, elapseTime, buildNum } = log
                        currStep = getToolStatus(currStep)
                        flag = getLogFlag(flag)
                        startTime = this.formatDate(startTime)
                        endTime = this.formatDate(endTime)
                        elapseTime = formatSeconds(elapseTime)
                        buildNum = '#' + buildNum
                        const { buildId, stepArray } = log
                        const newLog = { currStep, flag, startTime, endTime, elapseTime, buildNum, buildId }

                        const newStepArray = []
                        let index = 0
                        stepArray.forEach(step => {
                            let { startTime, endTime, elapseTime, flag, stepNum } = step
                            const { msg } = step
                            startTime = this.formatDate(startTime)
                            endTime = this.formatDate(endTime)
                            elapseTime = formatSeconds(elapseTime)
                            flag = getLogFlag(flag)
                            stepNum = getToolStatus(stepNum)
                            stepNum = this.$t(`detail.${stepNum}`)
                            index++

                            const newStep = { startTime, endTime, elapseTime, flag, stepNum, msg, index }
                            newStepArray.push(newStep)
                        })
                        newLog.stepArray = newStepArray
                        newContent.push(newLog)
                    })
                } else {
                    newContent.push({})
                }

                return newContent
            }
        },
        watch: {
            params: {
                handler () {
                    this.init()
                },
                deep: true
            }
        },
        created () {
            this.init()
        },
        mounted () {
            this.initWebSocket()
        },
        beforeDestroy () {
            taskWebsocket.disconnect()
        },
        methods: {
            async init () {
                const res = await this.$store.dispatch('tool/toolLog', this.params)
                if (res.taskId) {
                    this.taskLog = res
                    this.pagination.count = res.taskLogPage.totalElements
                }
            },
            backToDetail () {
                this.$router.push({
                    name: 'task-detail'
                })
            },
            formatDate (dateNum) {
                return dateNum ? format(dateNum, 'YYYY-MM-DD HH:mm:ss') : '--'
            },
            formatSeconds (s) {
                return formatSeconds(s)
            },
            showLog (buildId, time, download) {
                window.scrollTo(0, 0)
                if (buildId) {
                    this.buildId = buildId
                }
                this.slider.isShow = true
                const logUrl = `${window.AJAX_URL_PREFIX}/defect/api/user/tasklog/analysis/logs/${this.projectId}/${this.taskInfo.pipelineId}`
                this.downloadUrl = `${logUrl}/${this.buildId}/download`
                let buildNo = ''
                if (download) {
                    buildNo = `${this.buildId}/download`
                } else {
                    buildNo = this.buildId
                }
                if (time) {
                    this.hasTime = !this.hasTime
                    SodaLog.render(this.$refs.logContainer, logUrl, buildNo, this.hasTime, '')
                } else {
                    SodaLog.render(this.$refs.logContainer, logUrl, buildNo, false, '')
                }
            },
            handlePageChange (page) {
                this.pagination.current = page
                this.params = { ...this.params, page }
            },
            handlePageLimitChange (pageSize) {
                this.params.page = 1
                this.pagination.current = 1
                this.params = { ...this.params, pageSize }
            },
            initWebSocket () {
                const subscribe = `/topic/analysisDetail/taskId/${this.taskId}/toolName/${this.toolName}`

                taskWebsocket.connect(this.projectId, this.taskId, subscribe, {
                    success: (res) => {
                        const data = JSON.parse(res.body)
                        let hasNewLog = 1
                        this.taskLog.taskLogPage.content.forEach(item => {
                            if (item.buildId === data.buildId) {
                                Object.assign(item, data)
                                hasNewLog = 0
                            }
                        })
                        if (hasNewLog) this.init()
                    },
                    error: (message) => this.$showTips({ message, theme: 'error' })
                })
            },
            _destroyLog () {
                SodaLog.unMount(this.$refs.logContainer)
            }
        }
    }
</script>

<style lang="postcss">
    @import '../../css/variable.css';
    @import '../../css/log.1.0.5.min.css';

    .back {
        padding: 0 3px;
        color: #3A84FF;
        cursor: pointer;
    }

    .last-analys {
        font-size: 14px;
        margin-bottom: 18px;

        .analys-txt {
            color: $fontLightColor;
        }
        .analys-span {
            padding-right: 20px;
        }
        .analys-time {
            font-size: 12px;
            color: $fontLightColor;
        }
    }

    .bk-table {
        .bk-table-body {
            td.bk-table-expanded-cell {
                padding: 0 60px;
                background-color: $bgLightColor;

                tr,
                th {
                    background-color: $bgLightColor;
                }
                td {
                    .step {
                        display: inline-block;
                        width: 6px;
                        height: 6px;
                        background-color: #c4c6cc;
                        border-radius: 6px;

                        &.status {
                            background-color: $goingColor;

                            &.success {
                                background-color: $successColor;
                            }
                            &.fail {
                                background-color: $failColor;
                            }
                        }
                    }
                }

                &:hover {
                    background-color: $bgLightColor;
                }
            }
        }
        td {
            .status {
                color: $goingColor;

                &.success {
                    color: $successColor;
                }
                &.fail {
                    color: $failColor;
                }
            }
        }
    }
    .sideslider {
        .bk-sideslider-wrapper {
            overflow: hidden;
        }
    }
</style>
<style lang="postcss" scoped>
    >>>.bk-sideslider-content {
        height: 95%;
        overflow: initial;
        div {
            height: 100%;
        }
        #logContainer {
            height: 100%;
        }
        .sideslider-header-extra {
            position: absolute;
            right: 16px;
            top: -60px;
            height: 60px;
            line-height: 60px;
            font-size: 12px;
        }
    }
</style>
