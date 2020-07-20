<template>
    <div class="task-detail">
        <div>
            <bk-button :loading="buttonLoading.complete" class="tool-analys" theme="primary" @click="triggerAnalyse">{{$t('detail.立即分析')}}</bk-button>
            <a class="tool-add" href="javascript:;" @click="addTool">{{$t('detail.添加工具')}}</a>
            <span class="tool-count">{{$t('detail.共x个工具', { num: lastAnalysisResultList.length })}}</span>
        </div>
        <container class="task-main" :col="3" :margin="0" :gutter="10">
            <bk-row>
                <bk-col class="main-left">
                    <tool-status-card
                        v-for="(item, index) in lastAnalysisResultList"
                        :key="index"
                        :data="item"
                    >
                    </tool-status-card>
                </bk-col>
                <bk-col class="main-right">
                    <section class="task-info" v-if="taskDetail">
                        <div class="info-header">
                            <strong>{{$t('detail.基础信息')}}</strong>
                        </div>
                        <dl>
                            <div class="item">
                                <dt>{{$t('basic.中文名')}}</dt>
                                <dd>{{ taskDetail.nameCn }}</dd>
                            </div>
                            <div class="item">
                                <dt>{{$t('basic.英文名')}}</dt>
                                <dd>{{ taskDetail.nameEn }}</dd>
                            </div>
                            <div class="item">
                                <dt>{{$t('basic.任务语言')}}</dt>
                                <dd>{{ formatLang(taskDetail.codeLang) }}</dd>
                            </div>
                            <div class="item">
                                <dt>{{$t('basic.管理员')}}</dt>
                                <dd>{{ taskDetail.taskOwner.join(';') }}</dd>
                            </div>
                            <div class="item">
                                <dt>{{$t('basic.创建时间')}}</dt>
                                <dd>{{ formatDate(taskDetail.createdDate) }}</dd>
                            </div>
                        </dl>
                    </section>
                    <section class="task-info" v-if="taskDetail">
                        <div class="info-header">
                            <strong>{{$t('nav.操作记录')}}</strong>
                            <a class="log-more" @click="openSlider">{{$t('st.更多')}}</a>
                        </div>
                        <dl v-if="operateRecords.length > 0">
                            <div class="item" v-for="param in operateRecords.slice(0, 5)" :key="param.index">
                                <dd :title="param.operMsg">{{param.operMsg}}</dd>
                                <dt class="time">{{param.time}}</dt>
                            </div>
                        </dl>
                        <dl v-else>
                            <div style="padding-top: 10px;">{{$t('records.暂无操作记录')}}</div>
                        </dl>
                    </section>
                </bk-col>
            </bk-row>
        </container>
        <Record :visiable.sync="show" :data="this.$route.name" />
        <bk-dialog v-model="dialogVisible"
            :theme="'primary'"
            :mask-close="false"
            @confirm="reAnalyse"
            :title="$t('detail.重新分析')">
            {{this.$t('detail.项目有任务正在分析中，是否中断该任务并创建新的扫描任务？')}}
        </bk-dialog>
    </div>
</template>

<script>
    import { mapState } from 'vuex'
    import { format } from 'date-fns'
    import toolStatusCard from '@/components/tool-status-card'
    import Record from '@/components/operate-record/index'
    import taskWebsocket from '@/common/taskWebSocket'

    export default {
        components: {
            toolStatusCard,
            Record
        },
        data () {
            return {
                lastAnalysisResultList: [],
                show: false,
                dialogVisible: false,
                buttonLoading: {
                    complete: false
                }
            }
        },
        computed: {
            ...mapState([
                'toolMeta'
            ]),
            ...mapState('tool', {
                toolMap: 'mapList'
            }),
            ...mapState('task', {
                detail: 'detail'
            }),
            ...mapState('defect', {
                operateRecords: 'records'
            }),
            projectId () {
                return this.$route.params.projectId
            },
            taskId () {
                return this.$route.params.taskId
            },
            toolId () {
                return this.$route.query.toolId
            },
            isAnalysing () {
                let isAnalysing = 0
                this.lastAnalysisResultList.forEach(result => {
                    if (result.curStep < 5 && result.curStep > 0 && result.stepStatus !== 1) isAnalysing = 1
                })
                return isAnalysing
            }
        },
        created () {
            this.recordData()
        },
        mounted () {
            this.initWebSocket()
        },
        beforeDestroy () {
            taskWebsocket.disconnect()
        },
        methods: {
            async fetchPageData () {
                const res = await this.$store.dispatch('task/overView', { taskId: this.taskId })
                if (res.taskId) {
                    this.taskDetail = this.detail
                    this.lastAnalysisResultList = res.lastAnalysisResultList
                }
            },
            addTool () {
                if (this.detail.createFrom.indexOf('pipeline') !== -1) {
                    let that = this
                    this.$bkInfo({
                        title: this.$t('detail.添加工具'),
                        subTitle: this.$t('st.此代码检查任务为流水线创建，工具需前往相应流水线添加。'),
                        maskClose: true,
                        confirmFn (name) {
                            window.open(`${window.DEVOPS_SITE_URL}/console/pipeline/${that.detail.projectId}/${that.detail.pipelineId}/edit#codecc`, '_blank')
                        }
                    })
                } else {
                    this.$router.push({ name: 'task-settings-tools' })
                }
            },
            formatDate (dateNum, time) {
                return time ? format(dateNum, 'HH:mm:ss') : format(dateNum, 'YYYY-MM-DD HH:mm:ss')
            },
            formatLang (num) {
                return this.toolMeta.LANG.map(lang => lang.key & num ? lang.name : '').filter(name => name).join('; ')
            },
            triggerAnalyse () {
                if (this.detail.createFrom.indexOf('pipeline') !== -1) {
                    let that = this
                    this.$bkInfo({
                        title: this.$t('detail.立即分析'),
                        subTitle: this.$t('st.此代码检查任务需要到流水线启动，是否前往流水线？'),
                        maskClose: true,
                        confirmFn (name) {
                            window.open(`${window.DEVOPS_SITE_URL}/console/pipeline/${that.detail.projectId}/${that.detail.pipelineId}/edit`, '_blank')
                        }
                    })
                } else {
                    this.isAnalysing ? this.dialogVisible = true : this.analyse()
                }
            },
            async analyse (isAnalysing = 0) {
                this.buttonLoading.complete = true
                await this.$store.dispatch('task/triggerAnalyse').finally(() => {
                    this.buttonLoading.complete = false
                })
                await this.fetchPageData()
                this.recordData()
            },
            reAnalyse () {
                this.analyse(1)
            },
            async recordData () {
                const postData = {
                    taskId: this.$route.params.taskId,
                    funcId: ['trigger_analysis'],
                    toolName: ''
                }
                await this.$store.dispatch('defect/getOperatreRecords', postData).then(res => {
                    if (res) {
                        for (const i in this.operateRecords) {
                            this.operateRecords[i].time = format(this.operateRecords[i].time, 'YYYY-MM-DD HH:mm:ss')
                        }
                    }
                })
            },
            openSlider () {
                this.show = true
            },
            initWebSocket () {
                const subscribe = `/topic/analysisInfo/taskId/${this.taskId}`

                taskWebsocket.connect(this.projectId, this.taskId, subscribe, {
                    success: (res) => {
                        const data = JSON.parse(res.body)
                        let hasNewTool = 1
                        this.lastAnalysisResultList.forEach(item => {
                            if (item.toolName === data.toolName) {
                                Object.assign(item, data)
                                hasNewTool = 0
                            }
                        })
                        if (hasNewTool) this.fetchPageData()
                    },
                    error: (message) => this.$showTips({ message, theme: 'error' })
                })
            }
        }
    }
</script>

<style scoped lang="postcss">
    @import '../../css/variable.css';

    .task-detail {
        font-size: 14px;

        .tool-analys {
            width: 120px;
        }
        .tool-add {
            padding: 0 15px;
        }
        .tool-count {
            float: right;
        }

        .task-main {
            padding: 10px 0;
            display: block;
            .main-left {
                width: calc(100% - 400px);
                padding-right: 10px;
            }
            .main-right {
                width: 400px;
            }
        }

        .task-info {
            padding: 19px;
            margin-bottom: 10px;
            border: 1px solid $borderColor;
            background: #fff;
            .info-header {
                padding-bottom: 5px;
                border-bottom: 1px solid $borderColor;
                strong {
                    color: #63656E;
                }
                .log-more {
                    float: right;
                    cursor: pointer;
                }
            }
            .item {
                display: flex;
                padding: 15px 0 0 0;
                dt {
                    width: 110px;
                    padding-right: 20px;
                    text-align: right;
                    &.time {
                        width: 200px;
                    }
                }
                dd {
                    width: 240px;
                    color: #313238;
                    word-break: break-all;
                }
            }
        }
    }
</style>
