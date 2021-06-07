<template>
    <div :class="['task-card', { 'disabled-bg': isDisableTask }]" :style="{ height: cardHeight }">
        <i class="codecc-icon icon-top-nail" v-if="task.topFlag === 1"></i>
        <section class="task-info">
            <main>
                <div :class="['task-icon', getTaskIconColorClass(task.taskId)]">{{(task.nameCn || '')[0]}}</div>
                <hgroup>
                    <h5 class="task-title" :title="task.nameCn">
                        <span @click="getTaskLink(task, 'detail')" :style="{ 'max-width': isPipelineTask ? '170px' : '190px' }">{{task.nameCn}}</span>
                        <span class="codecc-icon icon-pipeline" v-if="isPipelineTask"></span>
                    </h5>
                    <p class="task-desc default-blank-line" v-if="!task.codelib"></p>
                    <bk-popover theme="light" class="codelib-popover" :delay="[600, 10]" placement="bottom"
                        v-if="task.codelib && ((isPipelineTask && task.codelib.length) || (!isPipelineTask && task.aliasName))">
                        <section ref="codelibContent">
                            <p class="task-desc" v-if="!isPipelineTask && task.aliasName">{{ task.codelib }}</p>
                            <!-- <i :class="['bk-icon icon-angle-down',{ 'icon-flip': isDropdownShow }]" @click.stop="isDropdownShow = !isDropdownShow"></i> -->
                            <section v-if="isPipelineTask">
                                <p class="task-desc" v-for="(lib, libIndex) in task.codelib.slice(0,5)" :key="libIndex">{{ formatterCodelib(lib) }}</p>
                            </section>
                        </section>
                        <div slot="content">
                            <p class="codelib-item" v-if="!isPipelineTask && task.aliasName">{{ task.codelib }}</p>
                            <section v-if="isPipelineTask">
                                <p class="codelib-item" v-for="(lib, libIndex) in task.codelib" :key="libIndex">{{ formatterCodelib(lib) }}</p>
                            </section>
                        </div>
                    </bk-popover>
                </hgroup>
                <div class="task-status">
                    <i class="bk-icon status-icon" v-if="stepStatus !== 3 || isDisableTask"
                        :class="{
                            'icon-check-circle success': stepStatus === 0,
                            'bk-icon icon-close-circle fail': stepStatus === 1,
                            'bk-icon icon-clock waiting': isWaitingTask,
                            'bk-icon icon-minus-circle disable': isDisableTask
                        }"
                    ></i>
                    <div class="bk-spin-loading bk-spin-loading-mini bk-spin-loading-primary" v-else>
                        <div class="rotate rotate1"></div>
                        <div class="rotate rotate2"></div>
                        <div class="rotate rotate3"></div>
                        <div class="rotate rotate4"></div>
                        <div class="rotate rotate5"></div>
                        <div class="rotate rotate6"></div>
                        <div class="rotate rotate7"></div>
                        <div class="rotate rotate8"></div>
                    </div>
                    <span class="status-name" v-if="isDisableTask">{{$t('已停用')}}</span>
                    <span class="status-name" v-else-if="isWaitingTask">{{$t('待分析')}}</span>
                    <span class="status-name" :class="{ 'success': stepStatus === 0,'fail': stepStatus === 1 }" v-else>{{ taskStatus[stepStatus] }}</span>
                </div>
            </main>
        </section>
        <section class="analysis-contnet">
            <div class="progress-content" v-if="[1, 3].includes(stepStatus) && !isDisableTask">
                <p class="progress-desc">
                    <span class="display-step">
                        <span>{{task.displayName}}</span>：
                        <span>{{ $t(`${getToolStatus(task.displayStep, task.displayToolName)}`) }}</span>
                    </span>
                    <span class="progress-precent">{{task.displayProgress}}%</span>
                </p>
                <bk-progress :percent="(task.displayProgress / 100)" :show-text="false" :color="stepStatus === 1 ? '#ff5656' : '#3a84ff'" stroke-width="6"></bk-progress>
                <sapn class="codecc-icon icon-log-view view-btn" v-if="[1, 3].includes(stepStatus)" @click="getTaskLink(task, 'logs')"></sapn>
            </div>
            <div class="analysis-statistics" v-else>
                <div class="col-item">
                    <p :class="['col-value', { 'disabled': task.status === 3 }]">0</p>
                    <p :class="['col-label', { 'disabled': task.status === 3 }]">{{this.$t('代码缺陷')}}</p>
                    <!-- <div class="trend-chart" :ref="`defect-trendChart-${task.taskId}`" v-if="task.status === 0"></div> -->
                    <img src="../../images/trend-rise.png" class="trend-chart">
                </div>
                <div class="col-item">
                    <p :class="['col-value', { 'disabled': task.status === 3 }]">0</p>
                    <p :class="['col-label', { 'disabled': task.status === 3 }]">{{this.$t('安全漏洞')}}</p>
                    <!-- <div class="trend-chart" :ref="`bug-trendChart-${task.taskId}`" v-if="task.status === 0"></div> -->
                    <img src="../../images/trend-rise.png" class="trend-chart">
                </div>
                <div class="col-item">
                    <p :class="['col-value', { 'disabled': task.status === 3 }]">0</p>
                    <p :class="['col-label', { 'disabled': task.status === 3 }]">{{$t('代码规范')}}</p>
                    <!-- <div class="trend-chart" :ref="`standard-trendChart-${task.taskId}`" v-if="task.status === 0"></div> -->
                    <img src="../../images/trend-rise.png" class="trend-chart">
                </div>
            </div>
        </section>
        <div class="score-group">
            <!-- <bk-popover v-if="!isDisableTask" :delay="300">
                <div class="star-container"><i :class="['bk-icon', 'codecc-icon', 'icon-star-gray', { 'active': i <= 0 }]" v-for="i in 5" :key="i"></i></div>
                <div slot="content">
                    <div class="quality-star">
                        <h5 class="label">{{$t('综合质量星级')}}<span class="level">{{$t('x星', { num: 4 })}}</span></h5>
                        <p class="label item-label">{{$t('提示缺陷x个', { num: 1 })}}<span>{{$t('x星', { num: 4 })}}</span></p>
                        <p class="label item-label">{{$t('提示漏洞x个', { num: 1 })}}<span>{{$t('x星', { num: 4 })}}</span></p>
                        <p class="label item-label">{{$t('规范问题密度小于x/千行', { num: 10 })}}<span>{{$t('x星', { num: 4 })}}</span></p>
                        <p class="learn-more"><a>{{$t('了解评星规则')}}<i class="bk-icon icon-angle-right"></i></a></p>
                    </div>
                </div>
            </bk-popover>
            <i v-else :class="['bk-icon codecc-icon icon-star-gray disabled']" v-for="i in 5" :key="i"></i> -->
            <div class="star-container"><i :class="['bk-icon codecc-icon icon-star-gray', { 'disable-star': isDisableTask }]" v-for="i in 5" :key="i"></i></div>
        </div>
        <div :class="['last-update-time', { 'disabled': task.status === 3 }]">{{task.minStartTime ? formatAnlsTime(task.minStartTime) : '--'}}</div>
        <section class="handle-content">
            <template v-if="!isDisableTask">
                <i class="bk-icon icon-play-circle-shape fs20" v-if="[0, 1].includes(stepStatus) || isWaitingTask" @click="handleTask(task, 'execute')"></i>
                <i class="bk-icon icon-refresh" v-if="stepStatus === 3" @click="handleTask(task, 'retry')"></i>
                <bk-popover theme="light" placement="bottom-top" ref="moreMenu" style="margin-left: 46px;" trigger="click">
                    <i class="bk-icon icon-more"></i>
                    <div slot="content" class="handle-menu-tips">
                        <p @click="handleMore('toggleTop')">{{task.topFlag === 1 ? $t('取消置顶') : $t('置顶')}}</p>
                        <p @click="handleMore('setting')">{{$t('设置')}}</p>
                    </div>
                </bk-popover>
            </template>
            <div class="enable-btn" v-else @click="handleTask(task, 'enable')">{{$t('去启用')}}<i class="bk-icon icon-angle-right"></i></div>
        </section>
    </div>
</template>

<script>
    import { mapState } from 'vuex'
    import taskAnalysisOption from '@/mixins/task-analysis-option'
    import { format } from 'date-fns'
    import { getToolStatus } from '@/common/util'
    import echarts from 'echarts/lib/echarts'
    import 'echarts/lib/chart/bar'
    import 'echarts/lib/chart/line'
    import 'echarts/lib/component/tooltip'
    import 'echarts/lib/component/title'
    import 'echarts/lib/component/legend'

    export default {
        mixins: [taskAnalysisOption],
        props: {
            task: {
                type: Object,
                default () {
                    return {}
                }
            },
            getTaskLink: Function,
            handleTask: Function
        },
        data () {
            return {
                trendChart: null,
                isDropdownShow: false,
                problemList: [
                    { key: 'defect', label: this.$t('缺陷'), value: 12 },
                    { key: 'vulnerability', label: this.$t('漏洞'), value: 2 },
                    { key: 'normativeProblem', label: this.$t('规范问题'), value: 5000 }
                ],
                taskStatus: { 0: this.$t('成功'), 1: this.$t('失败'), 3: this.$t('分析中') }
            }
        },
        computed: {
            ...mapState('tool', {
                toolMap: 'mapList'
            }),
            taskTools () {
                for (const i in this.task.toolConfigInfoList) {
                    if (this.task.toolConfigInfoList[i].curStep < 5 && this.task.toolConfigInfoList[i].curStep > 0 && this.task.toolConfigInfoList[i].stepStatus !== 1) {
                        this.task.toolConfigInfoList[i].stepStatus = 3
                    }
                }
                // 过滤掉停用工具并且只取前3个
                const taskTools = (this.task.toolConfigInfoList || []).filter(task => task.followStatus !== 6)
                return taskTools.slice(0, 3)
            },
            cardHeight () {
                let baseHeight = 80
                if (this.isPipelineTask && this.task.codelib && this.task.codelib.length > 1) {
                    let num = this.task.codelib.length
                    if (num > 4) num = 5
                    baseHeight += (num - 1) * 16
                }
                return `${baseHeight}px`
            },
            isDisableTask () {
                return this.task.status
            },
            isPipelineTask () {
                return this.task.createFrom === 'bs_pipeline'
            },
            isWaitingTask () {
                return this.task.displayStepStatus === undefined || (this.task.displayStepStatus === 0 && !this.task.displayStep)
            },
            // 自定义状态枚举： 0-成功 1-失败 2-等待中 3-分析中
            stepStatus () {
                let stepStatus = 0
                if (this.task.displayStep && this.task.displayStep < 5 && this.task.displayStep > 0 && this.task.displayStepStatus !== 1) {
                    stepStatus = 3
                } else if (this.task.displayStepStatus === 1) {
                    stepStatus = 1
                } else if (this.task.displayStepStatus === 0 && !this.task.displayStep) {
                    stepStatus = 2
                }

                return stepStatus
            }
        },
        mounted () {
            // setTimeout(() => {
            //     if (!this.isDisableTask && (this.stepStatus === 0 || this.isWaitingTask)) this.drawChart()
            // }, 600)
        },
        methods: {
            handleMore (type) {
                if (type === 'toggleTop') {
                    this.$refs.moreMenu.instance.hide()
                    this.handleTask(this.task, 'top')
                } else {
                    this.getTaskLink(this.task, 'setting')
                }
            },
            getTaskIconColorClass (taskId) {
                return `c${(taskId % 6) + 1}`
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
            },
            formatterCodelib (codelib) {
                return codelib.aliasName ? `${codelib.aliasName}@${codelib.branch}` : ''
            },
            getToolStatus (num, tool) {
                return getToolStatus(num, tool)
            },
            drawChart () {
                const defectChart = echarts.init(this.$refs[`defect-trendChart-${this.task.taskId}`])
                const bugChart = echarts.init(this.$refs[`bug-trendChart-${this.task.taskId}`])
                const standardChart = echarts.init(this.$refs[`standard-trendChart-${this.task.taskId}`])
                defectChart.setOption(this.taskAnalysisOption)
                bugChart.setOption(this.taskAnalysisOption)
                standardChart.setOption(this.taskAnalysisOption)
            }
        }
    }
</script>

<style lang="postcss" scoped>
    @import '../../css/mixins.css';
    @import '../../css/variable.css';
    @import '../../assets/bk_icon_font/style.css';

    .task-card {
        display: flex;
        align-items: center;
        position: relative;
        padding: 26px 40px;
        width: 1234px;
        border-radius: 2px;
        background-color: #fff;
        border: solid 1px $borderColor;
        &.disabled-bg {
        background-color: #f7f7f7;
        }
        .icon-top-nail {
            position: absolute;
            top: 0;
            left: 0;
            width: 0;
            height: 0;
            color: #54cad1;
            font-size: 20px;
            z-index: 1;
        }
        .task-info {
            display: flex;
            align-items: center;
            position: absolute;
            left: 0;
            padding: 26px 0 26px 40px;
            width: 396px;
            height: 100%;
            /* cursor: pointer; */
            z-index: 0;
            main {
                display: flex;
                align-items: center;
                padding-right: 24px;
                border-right: 1px solid #eff1f1;
                hgroup {
                    width: 190px;
                }
            }
        }
        .task-icon {
            flex: none;
            width: 48px;
            height: 48px;
            font-size: 20px;
            font-weight: bold;
            margin-right: 10px;
            text-align: center;
            line-height: 46px;
            color: #fff;
            border-radius: 2px;
            &.c1 { background: #7c90ef; }
            &.c2 { background: #5a9bf9; }
            &.c3 { background: #8f84e0; }
            &.c4 { background: #41d7e3; },
            &.c5 { background: #5bd5aa; },
            &.c6 { background: #ffca2b; }
        }
        .task-title {
            display: flex;
            align-items: center;
            margin: 0;
            font-weight: normal;
            white-space: nowrap;
            font-size: 18px;
            color: #63656e;
            span:first-child {
                display: inline-block;
                overflow: hidden;
                text-overflow: ellipsis;
                white-space: nowrap;
                cursor: pointer;
                &:hover {
                    color: $iconGoingColor;
                }
            }
            .icon-pipeline {
                margin-top: 2px;
                margin-left: 6px;
                font-size: 12px;
                color: #BABDC3;
            }
        }
        .task-desc {
            position: relative;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
            font-size: 12px;
            color: #999999;
        }
        .default-blank-line {
            height: 18px;
        }
        .codelib-popover {
            width: 100%;
            >>>.bk-tooltip-ref {
                width: 100%;
            }
        }
        .icon-angle-down {
            display: inline-block;
            position: absolute;
            top: 4px;
            left: 144px;
            font-size: 12px;
            cursor: pointer;
            transition: all .2s ease;
            &.icon-flip {
                transform: rotate(180deg);
            }
        }
        .task-status {
            display: flex;
            align-items: center;
            justify-content: center;
            margin-left: 12px;
            width: 72px;
            height: 26px;
            border-radius: 12px;
            border: 1px solid $borderColor;
            font-size: 12px;
            color:  #63656E;
        }
        .status-icon {
            margin-right: 6px;
            font-size: 12px;
            &.success {
                color: #2dcb56;
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
            &.waiting {
                color: #b0bdd5;
            }
            &.disable {
                color: #c3cdd7;
            }
        }
        .bk-spin-loading {
            width: 12px;
            height: 12px;
            margin: 0 6px 0 0;
        }
        .analysis-contnet {
            margin-left: 356px;
            padding: 0 28px 0 24px;
            width: 436px;
            .progress-content {
                width: 384px;
                &:hover {
                    .view-btn {
                        opacity: 1;
                    }
                }
            }
        }
        .progress-desc {
            display: flex;
            justify-content: space-between;
            margin-bottom: 4px;
            font-size: 12px;
            color: #b8bdc3;
            .progress-precent {
                color: #333;
            }
        }
        .view-btn {
            position: relative;
            top: 0px;
            margin-left: 338px;
            width: 48px;
            height: 16px;
            font-size: 12px;
            color: #3a84ff;
            border-radius: 8px;
            padding-left: 6px;
            font-size: 14px;
            cursor: pointer;
            opacity: 0;
            /* &:hover {
                color: #3a84ff;
            } */
        }
        .analysis-statistics {
            display: flex;
            justify-content: flex;
            .col-item {
                position: relative;
                margin-right: 8px;
                /* padding-top: 6px; */
                width: 122px;
                height: 50px;
                text-align: center;
            }
            .col-value {
                margin-bottom: 6px;
                font-size: 18px;
                color: #333;
            }
            .col-label {
                font-size: 12px;
                color: #63656e;
            }
            .col-value,
            .col-label {
                position: relative;
                z-index: 2;
                color: #bbbbbb;
                &.disabled {
                    color: #c4c6cc;
                }
            }
        }
        .trend-chart {
            position: absolute;
            top: 6px;
            left: 0;
            width: 120px;
            height: 44px;
            z-index: 1;
        }
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
        .disable-star {
            color: #c4c6cc;
        }
        .score-group {
            .bk-tooltip,
            .star-container {
                display: flex;
                align-items: center;
            }
        }
        .last-update-time {
            margin-left: 26px;
            width: 140px;
            font-size: 12px;
            color: #999;
        }
        .handle-content {
            display: flex;
            /* justify-content: center; */
            width: 140px;
            .bk-icon {
                cursor: pointer;
                &:hover {
                    color: $iconGoingColor;
                }
            }
            .icon-angle-right:hover {
                color: #fff;
            }
        }
        .icon-refresh,
        .icon-play-circle-shape {
            margin-left: 40px;
            color: #8f9aae;
        }
        .icon-play-circle-shape {
            color: $iconGoingColor;
            &:hover {
                opacity: .8;
            }
        }
        .icon-refresh {
            font-size: 16px;
        }
        .icon-more {
            color: #8f9aae;
        }
        .enable-btn {
            margin-left: 52px;
            padding: 4px 6px 4px 10px;
            border-radius: 2px;
            color: #fff;
            background-color: $iconGoingColor;
            font-size: 12px;
            cursor: pointer;
        }
        .disabled {
            color: #e6e6e6;
        }
    }
    .quality-star {
        padding: 8px 2px;
        min-width: 160px;
        font-size: 12px;
        color: #fff;
        h5 {
            margin: 0 0 4px;
            font-weight: normal;
            padding-bottom: 6px;
            border-bottom: 1px solid #87898c;
            span {
                color: #ffe148;
            }
        }
        .item-label {
            line-height: 22px;
            color: #e3e5e6;
        }
        span {
            margin-left: 8px;
        }
        .learn-more {
            width: 100%;
            display: flex;
            justify-content: flex-end;
            margin-top: 4px;
            a {
                cursor: pointer;
            }
            .bk-icon {
                position: relative;
                top: 2px;
                cursor: pointer;
            }
        }
    }
    .handle-menu-tips {
        padding: 0 10px;
        p {
            line-height: 30px;
            text-align: center;
            cursor: pointer;
            &:hover {
                color: #3a84ff;
            }
        }
    }
</style>
