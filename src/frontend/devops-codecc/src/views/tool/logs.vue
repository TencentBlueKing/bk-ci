<template>
    <div>
        <div class="main-container" v-bkloading="{ isLoading: loading, opacity: 0.1 }">
            <bk-table
                v-show="!loading"
                row-key="buildIndex"
                :data="taskLogContent"
                :size="size"
                :expand-row-keys="[taskLogContent[0].buildIndex]"
                :pagination="pagination"
                @page-change="handlePageChange"
                @page-limit-change="handlePageLimitChange">
                <bk-table-column type="expand" width="30">
                    <template slot-scope="props">
                        <bk-table
                            :data="props.row.stepArray"
                            :outer-border="false"
                            :header-cell-style="{ borderRight: 'none' }">
                            <bk-table-column :label="$t('步骤')">
                                <template slot-scope="scope">
                                    <i class="step" :class="{ status: scope.row.index === 1, 'status-success': scope.row.flag === '成功', 'status-fail': scope.row.flag === '失败' || scope.row.flag === '中断' }"></i>
                                    <span>{{ scope.row.stepNum }}</span>
                                </template>
                            </bk-table-column>
                            <bk-table-column width="80" :label="$t('状态')">
                                <template slot-scope="scope">
                                    <span :class="{ status: scope.row.index === 1, 'status-success': scope.row.flag === '成功', 'status-fail': scope.row.flag === '失败' || scope.row.flag === '中断' }">
                                        {{ scope.row.flag }}
                                    </span>
                                </template>
                            </bk-table-column>
                            <bk-table-column min-width="120" :label="$t('信息')">
                                <template slot-scope="scope">
                                    <bk-popover theme="light" class="msg-popover" v-if="scope.row.msg">
                                        <span v-if="formatMsg(scope.row.msg)" style="color: #ff9c01">
                                            {{$t('x个大文件y个问题', { X: formatMsg(scope.row.msg)['fileCount'], Y: formatMsg(scope.row.msg)['defectCount'] })}}，
                                            <a download target="_blank" :href="getDownloadUrl(formatMsg(scope.row.msg))">{{$t('点击下载详情文件')}}</a>
                                        </span>
                                        <span v-else>{{ scope.row.msg }}</span>
                                        <div slot="content">
                                            <span v-if="formatMsg(scope.row.msg)">
                                                <p>{{$t('x个大文件y个问题', { X: formatMsg(scope.row.msg)['fileCount'], Y: formatMsg(scope.row.msg)['defectCount'] })}}，
                                                    <a download target="_blank" :href="getDownloadUrl(formatMsg(scope.row.msg))">{{$t('点击下载详情文件')}}</a>
                                                </p>
                                                <p>{{$t('其中：')}}</p>
                                                <p v-for="(item, index) in formatMsg(scope.row.msg)['gatherFileList']" :key="index">
                                                    {{item.relPath}}{{$t('共x个问题', { num: item.total })}}
                                                </p>
                                            </span>
                                            <p v-else class="msg-content" v-for="(item, msgIndex) in scope.row.msg.split(/[,，]+/)" :key="msgIndex">{{ item }}</p>
                                        </div>
                                    </bk-popover>
                                </template>
                            </bk-table-column>
                            <bk-table-column width="80" :label="$t('耗时')" prop="elapseTime"></bk-table-column>
                            <bk-table-column :label="$t('开始时间')" prop="startTime"></bk-table-column>
                            <bk-table-column :label="$t('结束时间')" prop="endTime"></bk-table-column>
                            <div slot="empty">
                                <div class="codecc-table-empty-text">
                                    <img src="../../images/empty.png" class="empty-img">
                                    <div>{{$t('暂无数据')}}</div>
                                </div>
                            </div>
                        </bk-table>
                    </template>
                </bk-table-column>
                <bk-table-column :label="$t('构建号')" prop="buildNum"></bk-table-column>
                <bk-table-column :label="$t('状态')">
                    <template slot-scope="scope">
                        <span class="status" :class="{ 'status-success': scope.row.flag === '成功', 'status-fail': scope.row.flag === '失败' || scope.row.flag === '中断' }">
                            {{ scope.row.flag }}
                        </span>
                        <bk-popover theme="light" class="msg-popover" v-if="handleStepArray(scope.row.stepArray)">
                            <i class="codecc-icon icon-tips"></i>
                            <div slot="content">
                                <span>
                                    <p>{{$t('x个大文件y个问题', { X: handleStepArray(scope.row.stepArray)['fileCount'], Y: handleStepArray(scope.row.stepArray)['defectCount'] } )}}，
                                        <a download target="_blank" :href="getDownloadUrl(handleStepArray(scope.row.stepArray))">{{$t('点击下载详情文件')}}</a>
                                    </p>
                                    <p>{{$t('其中：')}}</p>
                                    <p v-for="(item, index) in handleStepArray(scope.row.stepArray)['gatherFileList']" :key="index">
                                        {{item.relPath}}{{$t('共x个问题', { num: item.total })}}
                                    </p>
                                </span>
                            </div>
                        </bk-popover>
                    </template>
                </bk-table-column>
                <bk-table-column :label="$t('耗时')" prop="elapseTime"></bk-table-column>
                <bk-table-column :label="$t('开始时间')" prop="startTime"></bk-table-column>
                <bk-table-column :label="$t('结束时间')" prop="endTime"></bk-table-column>
                <bk-table-column :label="$t('操作')" width="150">
                    <template slot-scope="scope">
                        <bk-button theme="primary" text @click="showLog(scope.row.buildId, scope.row.buildNum)">{{$t('查看日志')}}</bk-button>
                    </template>
                </bk-table-column>
                <div slot="empty">
                    <div class="codecc-table-empty-text">
                        <img src="../../images/empty.png" class="empty-img">
                        <div>{{$t('暂无数据')}}</div>
                    </div>
                </div>
            </bk-table>
            <article class="cc-log-home" v-if="slider.isShow">
                <section class="cc-log-main" v-bk-clickoutside="closeLog">
                    <section class="cc-log-head">
                        <span>CodeCC {{buildNum}}</span>
                        <bk-log-search :down-load-link="downloadUrl"></bk-log-search>
                    </section>
 
                    <bk-log class="cc-log" ref="bkLog"></bk-log>
                </section>
            </article>
        </div>
    </div>
</template>
<script>
    import { format } from 'date-fns'
    import { getToolStatus, getLogFlag, formatSeconds } from '@/common/util'
    import { mapState, mapActions } from 'vuex'
    import taskWebsocket from '@/common/taskWebSocket'
    import log from '@blueking/log'
    import Vue from 'vue'

    Vue.use(log)
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
                    title: this.$t('查看日志')
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
                hasTime: false,
                buildNum: '',
                loading: true
            }
        },
        computed: {
            ...mapState('task', {
                taskInfo: 'detail'
            }),
            toolId () {
                return this.$route.params.toolId
            },
            taskLogContent () {
                const content = this.taskLog.taskLogPage.content || []
                const newContent = []
                if (content.length) {
                    content.forEach((log, i) => {
                        let { currStep, flag, startTime, endTime, elapseTime, buildNum } = log
                        const { count, current, limit } = this.pagination
                        const buildIndex = count - (current - 1) * limit - i
                        currStep = getToolStatus(currStep)
                        flag = getLogFlag(flag)
                        startTime = this.formatDate(startTime)
                        endTime = this.formatDate(endTime)
                        elapseTime = formatSeconds(elapseTime)
                        buildNum = '#' + buildNum
                        const { buildId, stepArray } = log
                        const newLog = { currStep, flag, startTime, endTime, elapseTime, buildNum, buildId, buildIndex }

                        const newStepArray = []
                        let index = stepArray.length + 1
                        stepArray.forEach(step => {
                            let { startTime, endTime, elapseTime, flag, stepNum } = step
                            const { msg } = step
                            startTime = this.formatDate(startTime)
                            endTime = this.formatDate(endTime)
                            elapseTime = formatSeconds(elapseTime)
                            flag = getLogFlag(flag)
                            stepNum = getToolStatus(stepNum, this.toolId)
                            stepNum = this.$t(`${stepNum}`)
                            index--

                            const newStep = { startTime, endTime, elapseTime, flag, stepNum, msg, index }
                            newStepArray.unshift(newStep)
                        })
                        newLog.stepArray = newStepArray
                        newContent.push(newLog)
                    })
                } else {
                    newContent.push({})
                }

                return newContent
            },
            logPostData () {
                return {
                    projectId: this.projectId,
                    pipelineId: this.taskInfo && this.taskInfo.pipelineId,
                    buildId: '',
                    lineNo: 0,
                    id: undefined
                }
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
            this.subscribeMsg()
        },
        methods: {
            ...mapActions('devops', [
                'getInitLog',
                'getAfterLog'
            ]),
            async init () {
                this.loading = true
                const res = await this.$store.dispatch('tool/toolLog', this.params)
                this.loading = false
                if (res.taskId) {
                    this.taskLog = res
                    this.pagination.count = res.taskLogPage.totalElements
                }
            },
            formatDate (dateNum) {
                return dateNum ? format(dateNum, 'YYYY-MM-DD HH:mm:ss') : '--'
            },
            formatSeconds (s) {
                return formatSeconds(s)
            },
            showLog (buildId, buildNum) {
                this.buildNum = buildNum
                window.scrollTo(0, 0)
                if (buildId) {
                    this.logPostData.buildId = buildId
                }
                this.slider.isShow = true
                const logUrl = `${window.DEVOPS_API_URL}/log/api/user/logs/${this.projectId}/${this.taskInfo.pipelineId}`
                this.downloadUrl = `${logUrl}/${this.logPostData.buildId}/download`
                
                this.getInitLog(this.logPostData).then(res => {
                    this.handleLogRes(res)
                })
            },
            handleLogRes (res) {
                if (this.$refs.bkLog === undefined) return
                const logs = res.data.logs || []
                this.$refs.bkLog.addLogData(logs)
                
                const lastLog = logs[logs.length - 1] || {}
                const lastLogNo = lastLog.lineNo || this.logPostData.lineNo - 1 || -1
                this.logPostData.lineNo = +lastLogNo + 1
                if (res.data.finished) {
                    if (res.data.hasMore) {
                        this.getAfterLogApi(100)
                    }
                } else {
                    this.getAfterLogApi(1000)
                }
            },
            getAfterLogApi (mis) {
                this.logPostData.id = setTimeout(() => {
                    if (this.$refs.bkLog === undefined) return
                    this.getAfterLog(this.logPostData).then((res) => {
                        this.handleLogRes(res)
                    })
                }, mis)
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
            subscribeMsg () {
                const subscribe = `/topic/analysisDetail/taskId/${this.taskId}/toolName/${this.toolName}`
                if (taskWebsocket.stompClient.connected) {
                    taskWebsocket.subscribeMsg(subscribe, {
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
                } else { // websocket还没连接的话，1s后重试
                    setTimeout(() => {
                        console.log('websocket reconnect')
                        this.subscribeMsg()
                    }, 1000)
                }
            },
            formatMsg (msg) {
                try {
                    const gatherFile = JSON.parse(msg)
                    console.log('gatherFile: ', gatherFile)
                    return gatherFile
                } catch (error) {
                    return false
                }
            },
            getDownloadUrl (gatherFile) {
                return `${window.AJAX_URL_PREFIX}/schedule/api/user/fs/download/type/GATHER/filename/${gatherFile.fileName}`
            },
            closeLog (event) {
                this.slider.isShow = false
                clearTimeout(this.logPostData.id)
            },
            handleStepArray (stepArray = []) {
                const newArray = stepArray.map(item => this.formatMsg(item.msg)).filter(item => item)
                return newArray[0]
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

                            &.status-success {
                                background-color: $successColor;
                            }
                            &.status-fail {
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

                &.status-success {
                    color: $successColor;
                }
                &.status-fail {
                    color: $failColor;
                }
            }
        }
        .msg-popover {
            max-width: 100%;
            .bk-tooltip-ref {
                max-width: 100%;
            }
            span {
                display: inline-block;
                max-width: 100%;
                white-space: nowrap;
                overflow: hidden;
                text-overflow: ellipsis;
            }
        }
    }
    .sideslider {
        .bk-sideslider-wrapper {
            overflow: hidden;
        }
    }
    .msg-content {
        max-width: 300px;
        word-break: break-all;
    }
</style>
<style lang="postcss" scoped>
    >>>.bk-sideslider-content {
        height: 96%;
        overflow: initial;
        >div {
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
    .cc-log-home {
        position: fixed;
        top: 0;
        left: 0;
        bottom: 0;
        right: 0;
        background-color: rgba(0,0,0,0.2);
        z-index: 1000;
        .cc-log-main {
            position: relative;
            width: 80%;
            height: calc(100% - 32px);
            float: right;
            display: flex;
            flex-direction: column;
            margin: 16px;
            border-radius: 6px;
            overflow: hidden;
            transition-property: transform, opacity;
            transition: transform 200ms cubic-bezier(0.165, 0.84, 0.44, 1),opacity 100ms cubic-bezier(0.215, 0.61, 0.355, 1);
            background: #1e1e1e;
            .cc-log-head {
                line-height: 48px;
                padding: 5px 20px;
                border-bottom: 1px solid;
                border-bottom-color: #2b2b2b;
                display: flex;
                align-items: center;
                justify-content: space-between;
                color: #d4d4d4;
            }
            .cc-log {
                height: calc(100% - 60px)
            }
        }
    }
    .icon-tips {
        color: #ff9c01;
    }
</style>
