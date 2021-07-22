<template>
    <div>
        <span v-if="!isEditable" class="fs12">{{$t('修改通知报告，请前往流水线')}}
            <a @click="hanldeToPipeline" href="javascript:;">{{$t('立即前往>>')}}</a>
        </span>
        <bk-collapse v-model="activeName">
            <bk-collapse-item content-hidden-type="hidden" name="message">
                <div>
                    <b class="hoverStyle">{{$t('消息提醒')}}</b>
                    <span class="header-tips">{{$t('所有工具分析结果和异常通过企业微信消息提醒实时反馈')}}</span>
                </div>
                <div slot="content">
                    <div v-if="!isEditable">
                        <label class="pipeline-item bk-label">{{ $t('接收人') }}</label>
                        <label class="pipeline-content">{{ recieveList[formData.rtxReceiverType].name }}</label>
                        <div v-if="formData.rtxReceiverType === '2'" class="member-list">
                            <span class="receiver-list" v-for="(item, index) in formData.rtxReceiverList" :key="index">{{item}}</span>
                        </div>
                    </div>
                    <bk-form v-else ref="message" :model="formData">
                        <bk-form-item :label="$t('接收人')" :required="true" :property="'rtxReceiverType'" :rules="rules.message">
                            <bk-select v-model="formData.rtxReceiverType" @selected="handleCustomReciever">
                                <bk-option v-for="option in recieveList"
                                    :key="option.id"
                                    :id="option.id"
                                    :name="option.name">
                                </bk-option>
                            </bk-select>
                        </bk-form-item>
                        <bk-form-item label="" v-if="formData.rtxReceiverType === '2'">
                            <span class="edit-receiver-list" @click="isCustomRecieverShow = true"><i class="bk-icon icon-edit2 fs18"></i>{{$t('修改接收人')}}</span>
                            <span class="receiver-list" v-for="(item, index) in formData.rtxReceiverList" :key="index">{{item}}</span>
                        </bk-form-item>
                        <bk-dialog :position="{ top: 50, left: 5 }" v-model="isCustomRecieverShow" width="720">
                            <custom-receiver ref="reportMember" :target-list="formData.rtxReceiverList" :list="taskMemberData" :visiable="isCustomRecieverShow"></custom-receiver>
                        </bk-dialog>
                    </bk-form>
                </div>
            </bk-collapse-item>

            <bk-collapse-item content-hidden-type="hidden" name="robot">
                <div>
                    <b class="hoverStyle">{{$t('群机器人通知')}}</b>
                    <span class="header-tips">{{$t('工具分析结果通过企业微信群机器人实时通知')}}</span>
                </div>
                <div slot="content">
                    <div v-if="!isEditable">
                        <label class="pipeline-item bk-label">{{ $t('Webhook地址') }}</label>
                        <label class="pipeline-content">{{ formData.botWebhookUrl || '--' }}</label>
                        <div style="height: 20px;"></div>
                        <label class="pipeline-item bk-label">{{ $t('通知内容') }}</label>
                        <label class="pipeline-content">{{ $t('问题：') }}{{ botRemindRangeList[formData.botRemindRange] }}{{ botRemindSeverityList[formData.botRemindSeverity] }}</label>
                        <div style="height: 15px;"></div>
                        <label class="pipeline-item bk-label"> </label>
                        <label class="pipeline-content">{{ $t('工具：') }}{{ botToolListPicked.length ? botToolListPicked.join() : $t('目前仅支持Coverity、Klocwork、Pinpoint') }}</label>
                    </div>
                    <bk-form v-else ref="robot" :model="formData">
                        <bk-form-item :label="$t('Webhook地址')" :desc="robotDesc">
                            <bk-input v-model="formData.botWebhookUrl"></bk-input>
                        </bk-form-item>
                        <bk-form-item :label="$t('通知内容')">
                            <span class="label-tips">
                                <span class="bk-icon icon-info-circle"></span>
                                {{$t('工具分析完成后，如果问题数量未清零，就立即通过企业微信群机器人通知分析结果并@问题作者')}}
                            </span>
                            <bk-form-item class="form-item-moved" :label="$t('问题数量')" :label-width="$t('100')">
                                <bk-radio-group class="pb10" v-model="formData.botRemindRange">
                                    <bk-radio-button :value="2">{{$t('遗留')}}</bk-radio-button>
                                    <bk-radio-button :value="1">{{$t('新增')}}</bk-radio-button>
                                </bk-radio-group>
                                <bk-radio-group v-model="formData.botRemindSeverity">
                                    <bk-radio :value="7">{{$t('总问题数')}}</bk-radio>
                                    <bk-radio :value="3">{{$t('严重 + 一般问题数')}}</bk-radio>
                                    <bk-radio :value="1">{{$t('严重问题数')}}</bk-radio>
                                </bk-radio-group>
                            </bk-form-item>
                            <bk-form-item class="form-item-moved" :label="$t('工具选择')" :label-width="$t('100')">
                                <bk-select v-if="botToolList.length" multiple v-model="formData.botRemaindTools">
                                    <bk-option v-for="option in botToolList"
                                        :key="option.id"
                                        :id="option.id"
                                        :name="option.name">
                                    </bk-option>
                                </bk-select>
                                <!-- <tool-selector v-if="botToolList.length" :list="botToolList" :picked="formData.botRemaindTools" @select="selecteBotTools"></tool-selector> -->
                                <span class="label-tips" v-else>{{$t('目前仅支持Coverity、Klocwork、Pinpoint')}}</span>
                            </bk-form-item>
                        </bk-form-item>
                    </bk-form>
                </div>
            </bk-collapse-item>
            <bk-collapse-item content-hidden-type="hidden" name="mail">
                <div>
                    <b class="hoverStyle">{{$t('邮件报告')}}</b>
                    <span class="header-tips">{{$t('展示项目问题的作者分布和遗留趋势等')}}</span>
                </div>
                <div slot="content">
                    <div v-if="!isEditable">
                        <label class="pipeline-item bk-label">{{ $t('接收人') }}</label>
                        <label class="pipeline-content">{{ recieveList[formData.emailReceiverType].name }}</label>
                        <div v-if="formData.emailReceiverType === '2'" class="member-list">
                            <span class="receiver-list" v-for="(item, index) in formData.emailReceiverList" :key="index">{{item}}</span>
                        </div>
                        <div style="height: 15px;"></div>
                        <label class="pipeline-item bk-label">{{ $t('抄送人') }}</label>
                        <label class="pipeline-content">{{ formData.emailCCReceiverList.length ? formData.emailCCReceiverList.join() : '--' }}</label>
                        <div style="height: 15px;"></div>
                        <label class="pipeline-item bk-label">{{ $t('即时报告') }}</label>
                        <label class="pipeline-content">{{ $t('工具：') }}{{ formData.instantReportStatus === '2' ? '--' : $t('所有工具') }}</label>
                        <div style="height: 20px;"></div>
                        <label class="pipeline-item bk-label">{{ $t('定时报告') }}</label>
                        <label class="pipeline-content">{{ $t('时间：') }}{{ reportText }}</label>
                        <div style="height: 15px;"></div>
                        <label class="pipeline-item bk-label"> </label>
                        <label class="pipeline-content">{{ $t('工具：') }}{{ reportToolListPicked.length ? reportToolListPicked.join() : '--' }}</label>
                    </div>
                    <bk-form v-else ref="mail" :model="formData">
                        <bk-form-item :label="$t('收件人')" :required="true" :property="'emailReceiverType'" :rules="rules.message">
                            <bk-select v-model="formData.emailReceiverType" @selected="handleEmailReciever">
                                <bk-option v-for="option in recieveList"
                                    :key="option.id"
                                    :id="option.id"
                                    :name="option.name">
                                </bk-option>
                            </bk-select>
                        </bk-form-item>
                        <bk-form-item label="" v-if="formData.emailReceiverType === '2'">
                            <span class="edit-receiver-list" @click="isEmailRecieverShow = true"><i class="bk-icon icon-edit2 fs18"></i>{{$t('修改收件人')}}</span>
                            <span class="receiver-list" v-for="(item, index) in formData.emailReceiverList" :key="index">{{item}}</span>
                        </bk-form-item>
                        <bk-dialog :position="{ top: 50, left: 5 }" v-model="isEmailRecieverShow" width="720">
                            <custom-receiver ref="emailMember" :target-list="formData.emailReceiverList" :list="taskMemberData" :visiable="isEmailRecieverShow"></custom-receiver>
                        </bk-dialog>
                        <bk-form-item :label="$t('抄送人')">
                            <bk-input v-model="formData.emailCCReceiverList" type="all"></bk-input>
                            <!-- <bk-member-selector v-model="formData.emailCCReceiverList" type="all"></bk-member-selector> -->
                        </bk-form-item>
                        <divider></divider>
                        <bk-form-item :label="$t('定时报告')">
                            <span class="label-tips">
                                <span class="bk-icon icon-info-circle"></span>
                                {{$t('集成多个工具的近期分析结果定时发送，且问题清零后自动不再发送')}}
                            </span>
                            <bk-form-item class="form-item-moved" :label="$t('发送日期')" :label-width="$t('100')">
                                <week-selector :picked="formData.reportDate" @select="selectedWeek"></week-selector>
                            </bk-form-item>
                            <bk-form-item class="form-item-moved" :label="$t('发送时间')" :label-width="$t('100')">
                                <bk-time-picker
                                    style="width:272px"
                                    v-model="formData.reportTime"
                                    :steps="[1, 60]"
                                    :placeholder="$t('选择时间')"
                                    :format="'HH:mm'">
                                </bk-time-picker>
                            </bk-form-item>
                            <bk-form-item class="form-item-moved" :label="$t('报告内容')" :label-width="$t('100')">
                                <bk-select multiple v-model="formData.reportTools">
                                    <bk-option v-for="option in reportToolList"
                                        :key="option.id"
                                        :id="option.id"
                                        :name="option.name">
                                    </bk-option>
                                </bk-select>
                                <!-- <tool-selector :list="reportToolList" :picked="formData.reportTools" @select="selectedReportTools"></tool-selector> -->
                            </bk-form-item>
                        </bk-form-item>
                        <divider></divider>
                        <bk-form-item :label="$t('即时报告')">
                            <span class="label-tips">
                                <span class="bk-icon icon-info-circle"></span>
                                {{$t('工具分析完成后立即发送一封相关工具的分析结果邮件')}}
                            </span>
                            <div class="switcher">
                                <bk-switcher :value="!!instantReportStatus" @click.native="instantReport" size="min" theme="primary"></bk-switcher>{{$t('所有工具')}}
                            </div>
                        </bk-form-item>
                        <!-- <divider></divider>
                        <bk-form-item :label="$t('开源规范报告')">
                            <span class="label-tips">
                                <span class="bk-icon icon-info-circle"></span>
                                {{$t('根据项目语言，发送相应腾讯开源代码规范检查结果邮件')}}
                            </span>
                            <bk-form-item class="form-item-moved" :label="$t('发送日期')" :label-width="$t('100')">
                                <week-selector :picked="formData.tosaReportDate" @select="selectedTosaWeek"></week-selector>
                            </bk-form-item>
                            <bk-form-item class="form-item-moved" :label="$t('发送时间')" :label-width="$t('100')">
                                <bk-time-picker
                                    style="width:272px"
                                    v-model="formData.tosaReportTime"
                                    :steps="[1, 60]"
                                    :placeholder="$t('选择时间')"
                                    :format="'HH:mm'">
                                </bk-time-picker>
                                <bk-button theme="default" @click="send">{{$t('立即发送一封')}}</bk-button>
                            </bk-form-item>
                        </bk-form-item> -->
                    </bk-form>
                </div>
            </bk-collapse-item>
        </bk-collapse>
        <bk-button v-if="isEditable" theme="primary" class="mt10" :loading="buttonLoading" @click="save">{{$t('保存配置')}}</bk-button>
    </div>
</template>

<script>
    import { mapState } from 'vuex'
    import divider from '@/components/divider'
    import WeekSelector from '@/components/week-selector'
    // import ToolSelector from '@/components/tool-selector'
    import CustomReceiver from '@/components/custom-receiver'
    import axios from 'axios'

    export default {
        components: {
            divider,
            WeekSelector,
            // ToolSelector,
            CustomReceiver
        },
        data () {
            return {
                formData: {
                    rtxReceiverType: '1',
                    rtxReceiverList: [],
                    botWebhookUrl: '',
                    botRemindRange: 1,
                    botRemindSeverity: 7,
                    botRemaindTools: [],
                    emailReceiverType: '1',
                    emailReceiverList: [],
                    emailCCReceiverList: [],
                    reportStatus: 1,
                    reportDate: [],
                    reportTime: '00:00',
                    reportTools: [],
                    instantReportStatus: 1,
                    tosaReportDate: [],
                    tosaReportTime: '00:00'
                },
                rules: {
                    message: [
                        {
                            required: true,
                            message: this.$t('必填项'),
                            trigger: 'blur'
                        }
                    ]
                },
                activeName: ['message', 'robot', 'mail'],
                recieveList: [
                    { id: '0', name: this.$t('所有人员') },
                    { id: '1', name: this.$t('仅管理员') },
                    { id: '2', name: this.$t('自定义') },
                    { id: '3', name: this.$t('无（不发送）') }
                ],
                isCustomRecieverShow: false,
                isEmailRecieverShow: false,
                reportDate: [],
                reportTools: [],
                taskMemberData: [],
                buttonLoading: false,
                instantReportStatus: 1,
                botRemindRangeList: {
                    '1': this.$t('新增'),
                    '2': this.$t('遗留')
                },
                botRemindSeverityList: {
                    '7': this.$t('总问题数'),
                    '3': this.$t('严重+一般问题数'),
                    '1': this.$t('严重问题数')
                },
                weekList: {
                    '1': '周一',
                    '2': '周二',
                    '3': '周三',
                    '4': '周四',
                    '5': '周五',
                    '6': '周六',
                    '7': '周日'
                }
            }
        },
        computed: {
            ...mapState('task', {
                taskDetail: 'detail'
            }),
            ...mapState('tool', {
                toolMap: 'mapList'
            }),
            projectId () {
                return this.$route.params.projectId
            },
            isEditable () {
                return !this.taskDetail.atomCode || this.taskDetail.createFrom !== 'bs_pipeline' || this.taskDetail.createFrom === 'gongfeng_scan'
            },
            botToolList () {
                const { enableToolList } = this.taskDetail
                const toolList = ['COVERITY', 'KLOCWORK', 'SPOTBUGS', 'PINPOINT']
                const list = []
                enableToolList.forEach(tool => {
                    if (toolList.includes(tool.toolName)) {
                        const id = tool.toolName
                        const name = tool.toolDisplayName
                        const logo = this.toolMap[id] ? this.toolMap[id].logo : ''
                        list.push({ id, name, logo })
                    }
                })
                return list
            },
            botToolListPicked () {
                const list = []
                this.botToolList.map(tool => {
                    if (this.formData.botRemaindTools.includes(tool.id)) {
                        list.push(tool.name)
                    }
                })
                return list
            },
            reportToolList () {
                const { enableToolList } = this.taskDetail
                const list = []
                enableToolList.forEach(tool => {
                    const id = tool.toolName
                    if (id !== 'CLOC') {
                        const name = tool.toolDisplayName
                        const logo = this.toolMap[id] ? this.toolMap[id].logo : ''
                        list.push({ id, name, logo })
                    }
                })
                return list
            },
            reportToolListPicked () {
                const list = []
                this.reportToolList.map(tool => {
                    if (this.formData.reportTools.includes(tool.id)) {
                        list.push(tool.name)
                    }
                })
                return list
            },
            reportText () {
                const { weekList } = this
                let week = this.formData.reportDate.map(item => {
                    return weekList[item]
                })
                if (week.length === 7) week = ['每天']
                week = week.length ? week.join() + this.formData.reportTime : '--'
                return week
            },
            robotDesc () {
                return `到企业微信群添加群机器人，复制Webhook地址到这里。`
            }
        },
        watch: {
            'isEmailRecieverShow': {
                handler (newVal, oldVal) {
                    if (oldVal) {
                        this.formData.emailReceiverList = this.$refs['emailMember'].selectedList
                    }
                },
                deep: true
            },
            'isCustomRecieverShow': {
                handler (newVal, oldVal) {
                    if (oldVal) {
                        this.formData.rtxReceiverList = this.$refs['reportMember'].selectedList
                    }
                },
                deep: true
            }
        },
        methods: {
            getUserList () {
                axios
                    .get(`${window.DEVOPS_API_URL}/project/api/user/users/projects/${this.projectId}/list`,
                         { withCredentials: true
                         })
                    .then(res => {
                        this.taskMemberData = res.data.data
                    })
            },
            async fetchPageData () {
                await this.getUserList()
                if (!this.taskDetail.notifyCustomInfo) {
                    await this.$store.dispatch('task/detail')
                }
                this.formData = Object.assign(this.formData, this.taskDetail.notifyCustomInfo)
                this.formData.reportTime = this.formTime(this.formData.reportTime)
                // this.formData.tosaReportTime = this.formTime(this.formData.tosaReportTime)
                this.instantReportStatus = 2 - this.formData.instantReportStatus
            },
            handleCustomReciever (value, option) {
                if (value === '2') {
                    this.isCustomRecieverShow = true
                }
            },
            handleEmailReciever (value, option) {
                if (value === '2') {
                    this.isEmailRecieverShow = true
                }
            },
            selectedWeek (id) {
                if (!this.formData.reportDate.includes(id)) {
                    this.formData.reportDate.push(id)
                } else if (this.formData.reportDate.includes(id)) {
                    const i = this.formData.reportDate.indexOf(id)
                    this.formData.reportDate.splice(i, 1)
                }
            },
            selectedTosaWeek (id) {
                if (!this.formData.tosaReportDate.includes(id)) {
                    this.formData.tosaReportDate.push(id)
                } else if (this.formData.tosaReportDate.includes(id)) {
                    const i = this.formData.tosaReportDate.indexOf(id)
                    this.formData.tosaReportDate.splice(i, 1)
                }
            },
            selectedBotTools (tool) {
                if (!this.formData.botRemaindTools.includes(tool)) {
                    this.formData.botRemaindTools.push(tool)
                } else if (this.formData.botRemaindTools.includes(tool)) {
                    const i = this.formData.botRemaindTools.indexOf(tool)
                    this.formData.botRemaindTools.splice(i, 1)
                }
            },
            selectedReportTools (tool) {
                if (!this.formData.reportTools.includes(tool)) {
                    this.formData.reportTools.push(tool)
                } else if (this.formData.reportTools.includes(tool)) {
                    const i = this.formData.reportTools.indexOf(tool)
                    this.formData.reportTools.splice(i, 1)
                }
            },
            selecteBotTools (tool) {
                if (!this.formData.botRemaindTools.includes(tool)) {
                    this.formData.botRemaindTools.push(tool)
                } else if (this.formData.botRemaindTools.includes(tool)) {
                    const i = this.formData.botRemaindTools.indexOf(tool)
                    this.formData.botRemaindTools.splice(i, 1)
                }
            },
            instantReport () {
                this.instantReportStatus = !this.instantReportStatus
            },
            formTime (time) {
                return time + ':00'
            },
            send () {},
            save () {
                const activeName = this.activeName[0]
                if (activeName) {
                    this.$refs[activeName].validate().then(validator => {
                        this.buttonLoading = true
                        this.formData.reportTime = parseInt(this.formData.reportTime)
                        // this.formData.tosaReportTime = parseInt(this.formData.tosaReportTime)
                        this.formData.instantReportStatus = 2 - this.instantReportStatus
                        this.$store.dispatch('task/saveReport', this.formData).then(res => {
                            if (res) {
                                this.$bkMessage({ theme: 'success', message: this.$t('保存成功') })
                            }
                        }).catch(e => {
                            this.$bkMessage({ theme: 'error', message: this.$t('保存失败') })
                        }).finally(() => {
                            this.$store.dispatch('task/detail', { showLoading: true }).finally(() => {
                                this.formData = Object.assign(this.formData, this.taskDetail.notifyCustomInfo)
                                this.formData.reportTime = this.formTime(this.formData.reportTime)
                                // this.formData.tosaReportTime = this.formTime(this.formData.tosaReportTime)
                                // this.instantReportStatus = 2 - this.formData.instantReportStatus
                            })
                            this.buttonLoading = false
                        })
                    })
                }
            },
            hanldeToPipeline () {
                window.open(`${window.DEVOPS_SITE_URL}/console/pipeline/${this.taskDetail.projectId}/${this.taskDetail.pipelineId}/edit#${this.taskDetail.atomCode}`, '_blank')
            }
        }
    }
</script>

<style lang="postcss" scoped>
    @import '../../css/variable.css';

    .header-tips {
        font-size: 12px;
        color: $fontLightColor;
        padding-left: 10px;
    }
    .hoverStyle {
        color: $fontWeightColor;
    }

    .bk-collapse {
        .bk-collapse-item {
            padding-bottom: 10px;
            >>>.bk-collapse-item-header {
                border: 1px solid $borderColor;
                background: $bgLightColor;
            }
            >>>.bk-collapse-item-content {
                border: 1px solid $borderColor;
                border-top: none;
                padding: 30px 0;
            }
        }
    }
    .bk-form-radio {
        margin-right: 30px;
    }
    .label-tips {
        display: inline-block;
        height: 16px;
        line-height: 16px;
        font-size: 12px;
        padding: 8px 0 35px 10px;
        .bk-icon {
            padding: 0 3px;
        }
    }
    >>>.divider {
        margin: 20px;
        width: calc(100% - 40px);
    }
    .report-immediate {
        padding-left: 10px;
        .li {
            display: inline-block;
            width: 200px;
            padding-bottom: 10px;
        }
    }
    .edit-receiver-list {
        font-size: 12px;
        padding-right: 10px;
        display: block;
        cursor: pointer;
        .bk-icon {
            padding-right: 5px;
        }
        &:hover {
            color: $goingColor;
        }
    }
    .receiver-list {
        font-size: 12px;
        padding-right: 10px;
        display: inline-block;
    }
    .form-width {
        width: 570px;
    }
    >>>.bk-label {
        text-align: left;
        padding: 0 19px;
    }
    >>>.bk-form-item {
        margin-right: 20px;
    }
    .switcher {
        display: block;
        padding-left: 10px;
    }
    .form-item-moved {
        position: relative;
        right: 100px;
    }
    .pipeline-item {
        color: #63656e;
        width: 150px;
        line-height: 30px;
        display: inline-block;
        font-size: 14px;
    }
    .member-list {
        padding-left: 150px;
    }
    .pipeline-content {
        color: #63656e;
        line-height: 30px;
        display: inline-block;
        font-size: 14px;
    }
    .fs12 {
        display: block;
        height: 36px;
        a {
            margin-left: 12px;
        }
    }
</style>
