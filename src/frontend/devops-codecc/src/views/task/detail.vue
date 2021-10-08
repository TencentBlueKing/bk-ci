<template>
    <div class="task-detail">
        <div>
            <!-- <bk-button :loading="buttonLoading.complete" class="tool-analys" theme="primary" @click="triggerAnalyse">{{$t('开始检查')}}</bk-button> -->
            <!-- <bk-button theme="primary" @click="addTool">{{$t('添加工具')}}</bk-button> -->
            <!-- <a class="tool-add" href="javascript:;" @click="addTool">{{$t('添加工具')}}</a> -->
            <!-- <span class="tool-count">{{$t('质量星级：')}}<i :class="['bk-icon codecc-icon icon-star-gray']" v-for="i in 5" :key="i"></i></span>
            <span class="tool-count">{{$t('语言：')}} {{ formatLang(taskDetail.codeLang) }}</span>
            <span class="tool-count">{{$t('代码量：')}} {{ '--' }}</span> -->
        </div>
        <container class="task-main" :col="3" :margin="0" :gutter="10">
            <bk-row>
                <bk-col class="main-left" v-if="selectedTypeData && selectedTypeData.length">
                    <!-- <div class="type-box" v-if="showType" v-bk-clickoutside="closeSelectType">
                        <bk-checkbox-group class="checkbox is-others" v-model="selectedType">
                            <bk-checkbox style="padding-top: 10px;" v-for="item in toolType" :key="item.key" :value="item.key">{{ item.name }}</bk-checkbox>
                        </bk-checkbox-group>
                    </div> -->
                    <div class="tool-cards">
                        <!-- <i @click="showSelectType" :class="['bk-icon codecc-icon icon-filter']"></i> -->
                        <tool-status-card
                            v-for="(item, index) in selectedTypeData"
                            :key="index"
                            :data="item"
                        >
                        </tool-status-card>
                    </div>
                </bk-col>
                <bk-col class="main-left" v-else-if="taskDetail">
                    <tool-status-card :data="{}"></tool-status-card>
                </bk-col>
                <bk-col class="main-right">
                    <section class="task-info" v-if="taskDetail">
                        <div class="info-header">
                            <strong>{{$t('基础信息')}}</strong>
                        </div>
                        <dl class="info-content">
                            <div class="item">
                                <dt>{{$t('任务名称')}}</dt>
                                <dd>{{ taskDetail.nameCn }}<i class="codecc-icon icon-pipeline-2" v-if="taskDetail.createFrom === 'bs_pipeline'"></i></dd>
                            </div>
                            <div class="item">
                                <dt>{{$t('任务语言')}}</dt>
                                <dd>{{ formatLang(taskDetail.codeLang) }}</dd>
                            </div>
                            <div class="item">
                                <dt>{{$t('规则集')}}</dt>
                                <dd>
                                    <bk-popover class="msg-popover" v-if="taskDetail.checkerSetName">
                                        <span class="checkerset">{{ taskDetail.checkerSetName || '--' }}</span>
                                        <div slot="content">
                                            <p class="msg-content" v-for="(item, msgIndex) in taskDetail.checkerSetName.split(',')" :key="msgIndex">{{ item }}</p>
                                        </div>
                                    </bk-popover>
                                </dd>
                                <!-- <dd v-bk-tooltips="{ content: taskDetail.checkerSetName }">{{ taskDetail.checkerSetName || '--' }}</dd> -->
                            </div>
                            <div class="item">
                                <dt>{{$t('规则数')}}</dt>
                                <dd>{{ taskDetail.checkerCount ? $t('x条', { num: taskDetail.checkerCount }) : '--' }} ({{$t('共x个工具', { num: lastAnalysisResultList.length })}})</dd>
                            </div>
                            <!-- <div class="item">
                                <dt>{{$t('英文名')}}</dt>
                                <dd>{{ taskDetail.nameEn }}</dd>
                            </div> -->
                            <!-- <div class="item">
                                <dt>{{$t('工具数')}}</dt>
                                <dd>
                                    {{$t('共x个工具', { num: lastAnalysisResultList.length })}}
                                    <a @click="addTool" class="tool-add-link">{{$t('添加工具')}}
                                        <i class="codecc-icon icon-link fs12"></i>
                                    </a>
                                </dd>
                            </div> -->
                            <!-- <div class="item">
                                <dt>{{$t('管理员')}}</dt>
                                <dd :title="roleList">{{ roleList }}</dd>
                            </div> -->
                            <div class="item">
                                <dt>{{$t('创建时间')}}</dt>
                                <dd>{{ formatDate(taskDetail.createdDate) }}</dd>
                            </div>
                        </dl>
                    </section>
                    <section class="task-info" v-if="taskDetail">
                        <div class="info-header">
                            <strong>{{$t('操作记录')}}</strong>
                            <a class="log-more" @click="openSlider">{{$t('更多')}}</a>
                        </div>
                        <dl class="info-content" v-if="operateRecords.length > 0">
                            <div class="item" v-for="param in operateRecords.slice(0, 8)" :key="param.index">
                                <dd :title="param.operMsg">{{param.operMsg}}</dd>
                                <dt class="time">{{formatDate(param.time)}}</dt>
                            </div>
                        </dl>
                        <dl v-else>
                            <div style="padding-top: 10px;">{{$t('暂无操作记录')}}</div>
                        </dl>
                    </section>
                </bk-col>
            </bk-row>
        </container>
        <Record :visiable.sync="show" :func-id="funcId" :data="this.$route.name" />
        <bk-dialog v-model="dialogVisible"
            :theme="'primary'"
            :mask-close="false"
            @confirm="reAnalyse"
            :title="$t('重新分析')">
            {{this.$t('任务正在分析中，是否中断并重新分析？')}}
        </bk-dialog>
    </div>
</template>

<script>
    import { mapState } from 'vuex'
    import { format } from 'date-fns'
    import toolStatusCard from '@/components/tool-status-card'
    import Record from '@/components/operate-record/index'
    import taskWebsocket from '@/common/taskWebSocket'
    import axios from 'axios'

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
                dialogAnalyseVisible: false,
                neverShow: false,
                buttonLoading: {
                    complete: false
                },
                funcId: [
                    'register_tool',
                    'tool_switch',
                    'task_info',
                    'task_switch',
                    'task_code',
                    'checker_config',
                    'scan_schedule',
                    'filter_path',
                    'defect_manage',
                    'trigger_analysis'
                ],
                roleList: '--',
                showType: false,
                selectedType: [],
                selectedTypeData: []
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
            toolType () {
                const arr = []
                this.toolMeta.TOOL_TYPE.forEach(item => {
                    arr.push({ 'key': item.key, 'name': item.name })
                })
                this.selectedType = arr.map(item => {
                    return item.key
                })
                return arr
            },
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
            },
            pipelineCondition () {
                return this.detail.createFrom === 'bs_pipeline'
            },
            pipelineId () {
                return this.detail.pipelineId
            }
        },
        watch: {
            'selectedType': {
                handler (newVal, oldVal) {
                    const selectedTool = []
                    const allTool = this.lastAnalysisResultList.map(item => {
                        this.detail.enableToolList.forEach(i => {
                            if (item.toolName === i.toolName) {
                                return Object.assign(item, i)
                            }
                        })
                        return item
                    })
                    if (newVal.length) {
                        newVal.forEach(i => {
                            const arr = allTool.filter(item => {
                                return item.toolType === i
                            })
                            selectedTool.push(...arr)
                        })
                        this.selectedTypeData = selectedTool
                    } else {
                        this.selectedTypeData = allTool
                    }
                }
            }
        },
        created () {
            this.init()
            this.recordData()
        },
        mounted () {
            this.subscribeMsg()
            const neverShow = JSON.parse(window.localStorage.getItem('neverShow'))
            neverShow === null ? this.neverShow = false : this.neverShow = neverShow
        },
        methods: {
            async init () {
                const res = await this.$store.dispatch('task/overView', { taskId: this.taskId, showLoading: true })
                if (res.taskId) {
                    if (!this.detail.nameEn) {
                        await this.$store.dispatch('task/detail')
                    }
                    this.taskDetail = this.detail
                    this.lastAnalysisResultList = res.lastAnalysisResultList || []
                    this.selectedTypeData = res.lastAnalysisResultList
                    if (this.detail.nameEn.indexOf('LD_') === 0 || this.taskDetail.nameEn.indexOf('DEVOPS_') === 0) {
                        if (this.$route.params.hasOwnProperty('dialogAnalyseVisible')) {
                            this.dialogAnalyseVisible = this.$route.params.dialogAnalyseVisible
                            this.triggerAnalyse()
                        } else {
                            this.dialogAnalyseVisible = !this.neverShow
                        }
                    }
                    // 去掉管理员列表
                    // if (this.detail.createFrom !== 'gongfeng_scan') {
                    //     this.getUserList()
                    // }
                }
            },
            getUserList () {
                axios
                    .get(`${window.DEVOPS_API_URL}/project/api/user/projects/${this.projectId}`,
                         { withCredentials: true,
                           headers:
                               { 'X-DEVOPS-PROJECT-ID': this.projectId }
                         })
                    .then(res => {
                        this.params = this.pipelineCondition
                            ? { 'projectId': res.data.data.projectId, 'pipelineId': this.pipelineId }
                            : { 'projectId': res.data.data.projectId, 'taskId': this.taskId }
                    }).finally(() => {
                        this.pipelineCondition ? this.getPipeLineAuth() : this.getCodeccAuth()
                    })
            },
            getPipeLineAuth () {
                axios
                    .get(`${window.DEVOPS_API_URL}/backend/api/perm/service/pipeline/mgr_resource/permission/?project_id=${this.params.projectId}&resource_type_code=pipeline&resource_code=${this.pipelineId}`,
                         { withCredentials: true
                         })
                    .then(res => {
                        this.roleList = res ? res.data.data.role.find((role) =>
                            role.role_code === 'manager'
                        ).user_list.join()
                            : '--'
                    })
            },
            getCodeccAuth () {
                axios
                    .get(`${window.DEVOPS_API_URL}/backend/api/perm/service/codecc/mgr_resource/permission/?project_id=${this.params.projectId}&resource_type_code=task&resource_code=${this.params.taskId}`,
                         { withCredentials: true
                         })
                    .then(res => {
                        this.roleList = res ? res.data.data.role.find((role) =>
                            role.role_code === 'manager'
                        ).user_list.join()
                            : '--'
                    })
            },
            addTool () {
                if (this.detail.createFrom.indexOf('pipeline') !== -1) {
                    const that = this
                    this.$bkInfo({
                        title: this.$t('添加工具'),
                        subTitle: this.$t('此代码检查任务为流水线创建，工具需前往相应流水线添加。'),
                        maskClose: true,
                        confirmFn (name) {
                            window.open(`${window.DEVOPS_SITE_URL}/console/pipeline/${that.detail.projectId}/${that.detail.pipelineId}/edit#${that.taskDetail.atomCode}`, '_blank')
                        }
                    })
                } else {
                    this.$router.push({ name: 'task-settings-tools', query: { add: '1' } })
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
                    const that = this
                    this.$bkInfo({
                        title: this.$t('开始检查'),
                        subTitle: this.$t('此代码检查任务需要到流水线启动，是否前往流水线？'),
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
                await this.init()
                this.recordData()
            },
            reAnalyse () {
                this.analyse(1)
            },
            async recordData () {
                const postData = {
                    taskId: this.$route.params.taskId,
                    funcId: this.funcId
                }
                await this.$store.dispatch('defect/getOperatreRecords', postData)
            },
            openSlider () {
                this.show = true
            },
            subscribeMsg () {
                const subscribe = `/topic/analysisInfo/taskId/${this.taskId}`
                if (taskWebsocket.stompClient.connected) {
                    taskWebsocket.subscribeMsg(subscribe, {
                        success: (res) => {
                            const data = JSON.parse(res.body)
                            let hasNewTool = 1
                            this.lastAnalysisResultList.forEach(item => {
                                if (item.toolName === data.toolName) {
                                    Object.assign(item, data)
                                    hasNewTool = item.lastAnalysisResult ? 0 : 1
                                }
                            })
                            if (hasNewTool) this.init()
                        },
                        error: (message) => this.$showTips({ message, theme: 'error' })
                    })
                } else { // websocket还没连接的话，1s后重试
                    setTimeout(() => {
                        this.subscribeMsg()
                    }, 1000)
                }
            },
            newAnalyse () {
                this.dialogAnalyseVisible = false
                this.triggerAnalyse()
            },
            changeItem (data) {
                this.neverShow = data
            },
            showSelectType () {
                this.showType = !this.showType
            },
            closeSelectType () {
                setTimeout(() => {
                    this.showType = false
                }, 0)
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
            display: inline-flex;
            align-items: center;
            line-height: 32px;
            margin-top: -12px;
            padding-right: 30px;
            .icon-star-gray {
                margin-right: 2px;
                line-height: 10px;
                font-size: 14px;
                /* color: #D4D9DD; */
                color: #ffe148;
                &.active {
                    color: #ffe148;
                }
            }
        }

        .task-main {
            padding: 0 0 10px 0;
            display: block;
            overflow-x: hidden;
            .main-left {
                width: calc(100% - 350px);
                padding-right: 10px;
                /* .tool-cards {
                    padding: 10px 8px 5px 8px;
                    background-color: white;
                    border: 1px solid $borderColor;
                    .icon-filter {
                        font-size: 16px;
                        position: relative;
                        bottom: 3px;
                        left: calc(100% - 20px);
                        cursor: pointer;
                        z-index: 10;
                    }
                }
                .type-box {
                    height: 0;
                    position: relative;
                    top: 31px;
                    left: calc(100% - 109px);
                    z-index: 10;
                    .checkbox {
                        background-color: #ffffff;
                        width: 100px;
                        padding: 10px;
                        border: 1px solid $borderColor;
                        box-shadow: 0 3px 8px 0 rgba(0,0,0,.2), 0 0 0 1px rgba(0,0,0,.08);
                    }
                    .is-others:before {
                        content: '';
                        width: 0;
                        height: 0;
                        border: 8px solid transparent;
                        border-bottom-color: $borderColor;
                        position: absolute;
                        left: 79px;
                        top: -16px;
                    }
                    .is-others:after {
                        content: "";
                        width: 0;
                        height: 0;
                        border: 8px solid transparent;
                        border-bottom-color: #ffffff;
                        position: absolute;
                        left: 79px;
                        top: 5px;
                        margin-top: -18px;
                    }
                } */
            }
            .hidden-scroll {
                margin-left: -9px;
            }
            .main-right {
                width: 350px;
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
            .info-content {
                overflow: auto;
                line-height: 19px;
            }
            .item {
                display: flex;
                padding: 15px 0 0 0;
                height: 34px;
                dt {
                    width: 110px;
                    padding-right: 20px;
                    text-align: right;
                    &.time {
                        width: 230px;
                    }
                }
                dd {
                    width: 210px;
                    color: #313238;
                    overflow: hidden;
                    text-overflow:ellipsis;
                    white-space: nowrap;
                    .codecc-icon {
                        color: #a3c5fd;
                        margin-left: 4px;
                        font-size: 16px;
                        &.icon-link {
                            font-size: 12px;
                        }
                    }
                    .tool-add-link {
                        font-size: 12px;
                        padding-left: 10px;
                        cursor: pointer;
                    }
                    .checkerset {
                        display: inline-block;
                        max-width: 210px;
                        overflow: hidden;
                        text-overflow:ellipsis;
                        white-space: nowrap;
                    }
                }
            }
        }
    }
    >>>.bk-checkbox-text {
        font-size: 12px;
    }
</style>
