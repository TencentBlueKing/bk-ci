<template>
    <div>
        <div v-if="!isEditable" class="from-pipeline">
            <div class="to-pipeline">
                <span>{{$t('修改扫描触发配置，请前往流水线')}} <a @click="hanldeToPipeline" href="javascript:;">{{$t('立即前往>>')}}</a></span>
            </div>
            <div>
                <span class="pipeline-label">{{$t('扫描方式')}}
                    <bk-popover>
                        <i class="codecc-icon icon-tips"></i>
                        <div slot="content">
                            <div>{{$t('增量扫描：扫描本次构建与上次构建的差异代码。')}}</div>
                            <div>{{$t('MR/PR扫描：扫描MR/PR的源分支与目标分支的差异代码。源分支代码需拉取到工作空间。')}}</div>
                            <div>{{$t('全量扫描：扫描全部代码。Coverity、Klocwork、Pinpoint、Gometalinter、重复率仅支持该扫描方式。')}}</div>
                        </div>
                    </bk-popover>

                </span>
                <span>
                    <span class="fs14" v-if="taskDetail.scanType === 1">{{ $t('增量扫描') }}</span>
                    <span class="fs14" v-else-if="taskDetail.scanType === 0">{{$t('全量扫描')}}</span>
                    <span class="fs14" v-else-if="taskDetail.scanType === 2">{{$t('MR/PR扫描')}}</span>
                </span>
            </div>
            <div>
                <span class="pipeline-label">{{$t('触发方式')}}</span>
                <span class="fs14">{{ formatExecuteDate || '--' }}</span>
                <span class="fs14">{{ taskDetail.executeTime }}</span>
            </div>
            <!-- <div>
                <span class="pipeline-label">{{$t('新问题判定')}}</span>
                <span class="fs14">{{ fromDate || '--' }}</span>
                <span class="fs14" v-if="fromDate">{{$t('之后产生的问题为新问题')}}</span>
            </div> -->
            <div>
                <span class="pipeline-label">{{$t('处理人更替')}}</span>
                <div v-if="authorList" class="handler-replace">
                    <div v-for="(item, index) in authorList" :key="index">
                        <span class="fs14">{{ item.targetAuthor.join() || '--' }}</span>
                        <span class="fs14 ml10" v-if="item.targetAuthor.join()">{{'(原处理人 ' + item.sourceAuthor + ')'}}</span>
                    </div>
                </div>
                
            </div>
        </div>
        <div v-else>
            <bk-form :label-width="190" :model="taskDetail" onkeydown="if(event.keyCode==13){return false;}">
                <div class="settings-header">
                    <b class="settings-header-title">{{$t('扫描方式')}}</b>
                    <span class="fs12 ml5">{{$t('支持除Coverity、Klocwork、Gometalinter、重复率之外所有工具')}}</span>
                </div>
                <div class="settings-body">
                    <bk-form-item :label-width="110">
                        <bk-radio-group v-model="taskDetail.scanType">
                            <bk-radio :value="1" class="pr30">{{$t('增量扫描')}}</bk-radio>
                            <bk-radio :value="0">{{$t('全量扫描')}}</bk-radio>
                        </bk-radio-group>
                    </bk-form-item>
                </div>
                <div class="settings-header">
                    <b v-if="taskDetail.createFrom === 'bs_pipeline'" class="settings-header-title">{{$t('触发方式')}}</b>
                    <b v-else class="settings-header-title">{{$t('定时触发')}}</b>
                </div>

                <div class="settings-body" v-if="taskDetail.createFrom !== 'bs_pipeline'">
                    <bk-form-item :label-width="110">
                        <!-- 周选择器 -->
                        <div>
                            <ul>
                                <li
                                    :class="taskDetail.executeDate && taskDetail.executeDate.includes(week.id) ? 'active' : ''"
                                    @click="selectedWeek(week.id)"
                                    class="settings-trigger-week"
                                    v-for="week in weekList"
                                    :key="week.label">
                                    {{week.name}}
                                </li>
                            </ul>
                        </div>
                        <!-- /周选择器 -->
                    </bk-form-item>
                    <bk-form-item :label-width="110" property="time">
                        <bk-time-picker
                            style="width:293px"
                            v-model="taskDetail.executeTime"
                            :placeholder="$t('选择时间')"
                            :format="'HH:mm'">
                        </bk-time-picker>
                    </bk-form-item>
                </div>
                <div class="settings-body" v-else>
                    <bk-form-item :label-width="110">
                        <span class="fs14">{{$t('请前往流水线修改触发方式。')}} <a @click="hanldeToPipeline" href="javascript:;">{{$t('立即前往>>')}}</a></span>
                    </bk-form-item>
                </div>

                <!-- <div class="settings-header">
                    <b class="settings-header-title">{{$t('新问题判定')}}</b>
                </div>
                <div class="settings-body">
                    <bk-form-item :label-width="110">
                        <bk-date-picker
                            :placeholder="$t('选择日期')"
                            style="width: 200px"
                            class="date-picker"
                            @change="handleTimeChange"
                            v-model="fromDate"
                            value-format="yyyy-MM-dd">
                        </bk-date-picker>{{$t('之后产生的问题为新问题')}}
                    </bk-form-item>
                </div> -->
                <div class="settings-header">
                    <b class="settings-header-title">{{$t('处理人转换')}}</b>
                    <span class="fs12 pl10">{{$t('各工具原处理人的问题都将自动转给新处理人')}}</span>
                </div>
                <div class="settings-body">
                    <bk-form-item :label-width="110" class="input" v-for="(item, index) in authorList" :key="index">
                        <bk-input v-model="item.sourceAuthor"></bk-input>
                        <!-- <bk-input class="compile-version" v-model="item.targetAuthor" :palceholder="'新处理人'"></bk-input> -->
                        <!-- <bk-member-selector v-model="item.sourceAuthor"></bk-member-selector> -->
                        <bk-input class="compile-version" v-model="item.targetAuthor" :palceholder="'新处理人'"></bk-input>
                        <!-- <bk-member-selector class="compile-version" v-model="item.targetAuthor" :palceholder="'新处理人'"></bk-member-selector> -->
                        <div class="tool-icon">
                            <i class="bk-icon icon-plus" @click="addTool(index)" v-if="index === authorList.length - 1"></i>
                            <i class="bk-icon icon-close" @click="deleteTool(index)" v-if="authorList.length > 1"></i>
                        </div>
                    </bk-form-item>
                    <bk-form-item :label-width="110">
                        <span v-if="!isEditable" class="fs14">{{$t('请前往流水线修改。')}} <a @click="hanldeToPipeline" href="javascript:;">{{$t('立即前往>>')}}</a></span>
                        <bk-button v-else :disabled="ableHandlerConversion()" theme="primary" @click="saveHandler">{{$t('保存')}}</bk-button>
                    </bk-form-item>
                </div>
            </bk-form>
        </div>
        
    </div>
</template>

<script>
    import { mapState } from 'vuex'

    export default {
        components: {
        },
        data () {
            return {
                weekList: [
                    {
                        id: '1',
                        name: this.$t('一'),
                        label: 'Mon'
                    },
                    {
                        id: '2',
                        name: this.$t('二'),
                        label: 'Tues'
                    },
                    {
                        id: '3',
                        name: this.$t('三'),
                        label: 'Wed'
                    },
                    {
                        id: '4',
                        name: this.$t('四'),
                        label: 'Thur'
                    },
                    {
                        id: '5',
                        name: this.$t('五'),
                        label: 'Fri'
                    },
                    {
                        id: '6',
                        name: this.$t('六'),
                        label: 'Sat'
                    },
                    {
                        id: '7',
                        name: this.$t('日'),
                        label: 'Sun'
                    }
                ],
                authorList: [{ sourceAuthor: '', targetAuthor: [] }],
                date: '',
                weekform: ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
            }
        },
        computed: {
            ...mapState('task', {
                taskDetail: 'detail'
            }),
            isEditable () {
                // 最老的v1插件atomCode为空，但createFrom === 'bs_pipeline', 也可编辑
                return !this.taskDetail.atomCode || this.taskDetail.createFrom !== 'bs_pipeline' || this.taskDetail.createFrom === 'gongfeng_scan'
            },
            fromDate () {
                return this.taskDetail.newDefectJudge ? this.taskDetail.newDefectJudge.fromDate : ''
            },
            formatExecuteDate () {
                const execute = this.taskDetail.executeDate ? this.taskDetail.executeDate.sort() : []
                if (execute.length) {
                    let str = '每'
                    for (const i in execute) {
                        str += this.weekform[execute[i] - 1]
                        if (i < execute.length - 1) {
                            str += '、'
                        }
                    }
                    return str
                }
                return ''
            }
        },
        created () {
            this.init()
        },
        methods: {
            init () {
                this.$store.dispatch('defect/getTransferAuthorList').then(res => {
                    // this.authorList = res.transferAuthorList ? res.transferAuthorList : [{ sourceAuthor: [], targetAuthor: [] }]
                    this.authorList = res.transferAuthorList ? this.fomatterAuthor(res.transferAuthorList, 1) : [{ sourceAuthor: '', targetAuthor: [] }]
                    this.date = this.fromDate
                })
            },
            saveTime () {
                const taskId = this.$route.params.taskId
                const { scanType } = this.taskDetail
                let { executeDate, executeTime } = this.taskDetail
                executeDate = executeDate === undefined ? [] : executeDate
                executeTime = executeTime === undefined ? '' : executeTime
                const newDefectJudge = { judgeBy: 1, fromDate: this.date }
                const data = { taskId, scanType, timeAnalysisConfig: { executeDate, executeTime }, newDefectJudge }
                this.$store.dispatch('task/trigger', data).then(res => {
                    if (res === true) {
                        this.$bkMessage({ theme: 'success', message: this.$t('保存成功') })
                        this.$store.dispatch('task/detail', { showLoading: true })
                    }
                }).catch(e => {
                    this.$bkMessage({ theme: 'error', message: this.$t('保存失败') })
                    this.$store.dispatch('task/detail', { showLoading: true })
                })
            },
            // 保存选择的周
            selectedWeek (id) {
                if (!this.taskDetail.executeDate) this.$set(this.taskDetail, 'executeDate', [])
                if (!this.taskDetail.executeDate.includes(id)) {
                    this.taskDetail.executeDate.push(id)
                } else if (this.taskDetail.executeDate.includes(id)) {
                    const i = this.taskDetail.executeDate.indexOf(id)
                    this.taskDetail.executeDate.splice(i, 1)
                }
            },
            handleTimeChange (date) {
                this.date = date
            },
            addTool (index) {
                this.authorList.push({
                    sourceAuthor: '',
                    targetAuthor: []
                })
            },
            deleteTool (index) {
                if (this.authorList.length > 1) {
                    this.authorList.splice(index, 1)
                }
            },
            fomatterAuthor (authorObj, type) {
                return authorObj.map(item => {
                    return {
                        sourceAuthor: item.sourceAuthor ? item.sourceAuthor : '',
                        targetAuthor: item.targetAuthor ? type === 1 ? item.targetAuthor.split(',') : item.targetAuthor.join(',') : []
                    }
                })
            },
            saveHandlerConversion () {
                const transferAuthorList = this.fomatterAuthor(this.authorList, 2)
                const taskId = this.$route.params.taskId
                const { scanType } = this.taskDetail
                let { executeDate, executeTime } = this.taskDetail
                executeDate = executeDate === undefined ? [] : executeDate
                executeTime = executeTime === undefined ? '' : executeTime
                const newDefectJudge = { judgeBy: 1, fromDate: this.date }
                const data = { taskId, scanType, timeAnalysisConfig: { executeDate, executeTime }, transferAuthorList, newDefectJudge }
                this.$store.dispatch('task/trigger', data).then(res => {
                    if (res === true) {
                        this.$bkMessage({ theme: 'success', message: this.$t('保存成功') })
                        this.$store.dispatch('task/detail', { showLoading: true })
                    }
                }).catch(e => {
                    this.$bkMessage({ theme: 'error', message: this.$t('保存失败') })
                    this.$store.dispatch('task/detail', { showLoading: true })
                })
            },
            ableHandlerConversion () {
                if (this.authorList.length === 1 && this.authorList[0].sourceAuthor === '' && !this.authorList[0].targetAuthor.length) {
                    return false
                }
                for (let index = 0; index < this.authorList.length; index++) {
                    const element = this.authorList[index]
                    if (element.sourceAuthor === '' || !element.targetAuthor.length) {
                        return true
                    }
                }
                return false
            },
            saveHandler () {
                if (this.authorList.length === 1 && this.authorList[0].sourceAuthor === '' && !this.authorList[0].targetAuthor.length) {
                    return this.saveTime()
                } else {
                    return this.saveHandlerConversion()
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
    /* 标题与分隔线 start */
    .settings-header {
        margin: 19px 0px 19px 45px;
        .settings-header-title {
            display: inline-block;
            text-align: left;
            font-size: 14px;
            color: #63656e
        }
    }
    /* 标题与分隔线 end */
    .settings-body {
        border-bottom: 1px solid $bgHoverColor;
        padding-bottom: 20px;
        font-size: 14px;
        &:last-of-type {
            border-bottom: none;
        }
        /* 星期列表 start */
        .settings-trigger-week {
            margin-right: 8px;
            display: inline-block;
            width: 35px;
            height: 32px;
            border-radius: 2px;
            border: 1px solid $itemBorderColor;
            cursor: pointer;
            line-height: 32px;
            text-align: center;
        }
        /* 星期列表 end */
        .active {
            border: 1px solid $goingColor;
            color: $goingColor;
        }
        .save-button {
            width: 86px
        }
        .date-picker {
            margin-right: 15px;
        }
        .input {
            width: 300px;
            height: 32px;
            .compile-version {
                position: relative;
                top: -32px;
                left: 115%;
            }
            .compile-version::before {
                position: absolute;
                content: '>>';
                font-size: 14px;
                line-height: 32px;
                left: -24px;
                top: 0;
                color: #C4C6CC;
            }
            .tool-icon {
                position: relative;
                top: -63px;
                left: 220%;
                .bk-icon {
                    cursor: pointer;
                    font-size: 20px;
                }
            }
        }
    }
    .from-pipeline {
        padding: 0px 35px 0px 20px;
        .to-pipeline {
            font-size: 12px;
            border-bottom: 1px solid #e3e3e3;
            margin-bottom: 20px;
            height: 32px;
            a {
                margin-left: 12px;
            }
        }
        .pipeline-label {
            display: inline-block;
            width: 104px;
            text-align: left;
            font-size: 14px;
            line-height: 14px;
            height: 46px;
            font-weight: 600;
        }
        .handler-replace {
            display: inline-block;
            vertical-align: top;
        }
    }
</style>
