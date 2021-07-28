<template>
    <div class="card">
        <div class="card-head">
            <span class="card-tool" v-if="data.toolName">{{$t(`${(toolMap[data.toolName] && toolMap[data.toolName]['displayName']) || ''}`)}}</span>
            <!-- <span v-if="data.lastAnalysisResult" class="card-split">|</span> -->
            <!-- <a class="card-analys" href="javascript:;" @click.stop="toLogs">{{$t('分析详情')}}>></a> -->
            <span v-if="data.lastAnalysisResult" class="card-type">{{ toolMap[data.toolName] && toolType[toolMap[data.toolName].type] }}</span>
            <span v-if="data.lastAnalysisResult" class="card-time">
                <span v-if="stepStatus === 3">{{$t('开始于')}} {{formatDate(data.lastAnalysisTime)}}</span>
                <span v-else>{{$t('耗时')}} {{formatSeconds(data.elapseTime)}}</span>
            </span>
            <span v-if="data.lastAnalysisResult" class="card-step"
                :class="{ 'success': stepStatus === 0, 'fail': stepStatus === 1, 'loading': stepStatus === 3 }"
                @click.stop="toLogs">
                <i class="bk-icon card-tool-status"
                    :class="{ 'icon-check-circle-shape': stepStatus === 0, 'icon-close-circle-shape': stepStatus === 1 }"
                >
                </i>
                <li class="cc-fading-circle" v-if="stepStatus === 3">
                    <div class="cc-circle1 cc-circle"></div>
                    <div class="cc-circle2 cc-circle"></div>
                    <div class="cc-circle3 cc-circle"></div>
                    <div class="cc-circle4 cc-circle"></div>
                    <div class="cc-circle5 cc-circle"></div>
                    <div class="cc-circle6 cc-circle"></div>
                    <div class="cc-circle7 cc-circle"></div>
                    <div class="cc-circle8 cc-circle"></div>
                    <div class="cc-circle9 cc-circle"></div>
                    <div class="cc-circle10 cc-circle"></div>
                    <div class="cc-circle11 cc-circle"></div>
                    <div class="cc-circle12 cc-circle"></div>
                </li>
                <!-- <i class="bk-icon card-tool-status icon-circle-2-1 spin-icon" v-if="stepStatus === 3"></i> -->
                <span class="card-step-txt">{{$t(`${getToolStatus(data.curStep, data.toolName)}`)}}</span>
            </span>
        </div>
        <div v-if="data.lastAnalysisResult">
            <bk-container class="card-content" :col="4" v-if="data.toolName === 'CCN'">
                <bk-row class="card-num">
                    <bk-col><a @click="toList">{{data.lastAnalysisResult.defectCount === undefined ? '--' : data.lastAnalysisResult.defectCount}}</a></bk-col>
                    <bk-col><a @click="toList">{{data.lastAnalysisResult.defectChange === undefined ? '--' : Math.abs(data.lastAnalysisResult.defectChange)}}</a>
                        <i class="bk-icon"
                            :class="{ 'icon-arrows-up up': data.lastAnalysisResult.defectChange > 0,
                                      'icon-arrows-down down': data.lastAnalysisResult.defectChange < 0 }"
                        >
                        </i>
                    </bk-col>
                    <bk-col><a @click="toList">{{data.lastAnalysisResult.averageCCN === undefined ? '--' : (data.lastAnalysisResult.averageCCN).toFixed(2)}}</a></bk-col>
                    <bk-col><a @click="toList">{{data.lastAnalysisResult.averageCCNChange === undefined ? '--' : Math.abs(data.lastAnalysisResult.averageCCNChange).toFixed(2)}}</a>
                        <i class="bk-icon"
                            :class="{ 'icon-arrows-up up': data.lastAnalysisResult.averageCCNChange > 0,
                                      'icon-arrows-down down': data.lastAnalysisResult.averageCCNChange < 0 }"
                        >
                        </i>
                    </bk-col>
                </bk-row>
                <bk-row class="card-txt">
                    <bk-col><i class="codecc-icon icon-risky-function"></i>{{$t('风险函数')}}</bk-col>
                    <bk-col><i class="codecc-icon icon-trend"></i>{{$t('风险函数趋势')}}</bk-col>
                    <bk-col><i class="codecc-icon icon-pie"></i>{{$t('平均圈复杂度')}}</bk-col>
                    <bk-col><i class="codecc-icon icon-trend"></i>{{$t('圈复杂度趋势')}}</bk-col>
                </bk-row>
            </bk-container>

            <bk-container class="card-content" :col="4" v-else-if="data.toolName === 'DUPC'">
                <bk-row class="card-num">
                    <bk-col><a @click="toList">{{data.lastAnalysisResult.defectCount === undefined ? '--' : data.lastAnalysisResult.defectCount}}</a></bk-col>
                    <bk-col><a @click="toList">{{data.lastAnalysisResult.defectChange === undefined ? '--' : Math.abs(data.lastAnalysisResult.defectChange)}}</a>
                        <i class="bk-icon"
                            :class="{ 'icon-arrows-up up': data.lastAnalysisResult.defectChange > 0,
                                      'icon-arrows-down down': data.lastAnalysisResult.defectChange < 0 }"
                        >
                        </i>
                    </bk-col>
                    <bk-col><a @click="toList">{{data.lastAnalysisResult.defectChange === undefined ? '--' : (data.lastAnalysisResult.dupRate).toFixed(2)}}<span v-if="data.lastAnalysisResult.defectChange !== undefined">%</span></a></bk-col>
                    <bk-col><a @click="toList">{{data.lastAnalysisResult.dupRateChange === undefined ? '--' : Math.abs(data.lastAnalysisResult.dupRateChange).toFixed(2)}}<span v-if="data.lastAnalysisResult.dupRateChange !== undefined">%</span></a>
                        <i class="bk-icon"
                            :class="{ 'icon-arrows-up up': data.lastAnalysisResult.dupRateChange > 0,
                                      'icon-arrows-down down': data.lastAnalysisResult.dupRateChange < 0 }"
                        >
                        </i>
                    </bk-col>
                </bk-row>
                <bk-row class="card-txt">
                    <bk-col><i class="codecc-icon icon-risky-file"></i>{{$t('重复文件')}}</bk-col>
                    <bk-col><i class="codecc-icon icon-trend"></i>{{$t('重复文件趋势')}}</bk-col>
                    <bk-col><i class="codecc-icon icon-pie"></i>{{$t('平均重复率')}}</bk-col>
                    <bk-col><i class="codecc-icon icon-trend"></i>{{$t('重复率趋势')}}</bk-col>
                </bk-row>
            </bk-container>

            <bk-container class="card-content" :col="4" v-else-if="data.toolName === 'COVERITY' || data.toolName === 'KLOCWORK' || data.toolName === 'PINPOINT'">
                <bk-row class="card-num">
                    <bk-col><a @click="toList">{{data.lastAnalysisResult.existCount === undefined ? '--' : data.lastAnalysisResult.existCount}}</a></bk-col>
                    <bk-col><a @click="toList">{{data.lastAnalysisResult.newCount === undefined ? '--' : Math.abs(data.lastAnalysisResult.newCount)}}</a></bk-col>
                    <bk-col><a @click="toCovList({ status: 2 })">{{data.lastAnalysisResult.fixedCount === undefined ? '--' : data.lastAnalysisResult.fixedCount}}</a></bk-col>
                    <bk-col><a @click="toList">{{data.lastAnalysisResult.excludeCount === undefined ? '--' : data.lastAnalysisResult.excludeCount}}</a></bk-col>
                </bk-row>
                <bk-row class="card-txt">
                    <bk-col><i class="codecc-icon icon-danger"></i>{{$t('遗留问题')}}</bk-col>
                    <bk-col><i class="codecc-icon icon-trend"></i>{{$t('新增问题')}}</bk-col>
                    <bk-col><i class="codecc-icon icon-danger"></i>{{$t('修复问题')}}</bk-col>
                    <bk-col><i class="codecc-icon icon-danger"></i>{{$t('屏蔽问题')}}</bk-col>
                </bk-row>
            </bk-container>

            <bk-container class="card-content" :col="4" v-else-if="data.toolName === 'CLOC'">
                <bk-row class="card-num">
                    <bk-col><a @click="toList">{{data.lastAnalysisResult.totalLines === undefined ? '--' : data.lastAnalysisResult.totalLines}}</a></bk-col>
                    <bk-col>
                        <a @click="toList">{{data.lastAnalysisResult.linesChange === undefined ? '--' : Math.abs(data.lastAnalysisResult.linesChange)}}</a>
                        <i class="bk-icon"
                            :class="{ 'icon-arrows-up up': data.lastAnalysisResult.linesChange > 0,
                                      'icon-arrows-down down': data.lastAnalysisResult.linesChange < 0 }">
                        </i>
                    </bk-col>
                    <bk-col><a @click="toList">{{data.lastAnalysisResult.fileNum === undefined ? '--' : data.lastAnalysisResult.fileNum}}</a></bk-col>
                    <bk-col>
                        <a @click="toList">{{data.lastAnalysisResult.fileNumChange === undefined ? '--' : Math.abs(data.lastAnalysisResult.fileNumChange)}}</a>
                        <i class="bk-icon"
                            :class="{ 'icon-arrows-up up': data.lastAnalysisResult.fileNumChange > 0,
                                      'icon-arrows-down down': data.lastAnalysisResult.fileNumChange < 0 }">
                        </i>
                    </bk-col>
                </bk-row>
                <bk-row class="card-txt">
                    <bk-col><i class="codecc-icon icon-task-line"></i>{{$t('总行数')}}</bk-col>
                    <bk-col><i class="codecc-icon icon-trend"></i>{{$t('行数趋势')}}</bk-col>
                    <bk-col><i class="codecc-icon icon-risky-file"></i>{{$t('文件数')}}</bk-col>
                    <bk-col><i class="codecc-icon icon-trend"></i>{{$t('文件数趋势')}}</bk-col>
                </bk-row>
            </bk-container>

            <bk-container class="card-content" :col="4" v-else>
                <bk-row class="card-num">
                    <bk-col><a @click="toList">{{data.lastAnalysisResult.defectCount === undefined ? '--' : data.lastAnalysisResult.defectCount}}</a></bk-col>
                    <bk-col><a @click="toList">{{data.lastAnalysisResult.defectChange === undefined ? '--' : Math.abs(data.lastAnalysisResult.defectChange)}}</a>
                        <i class="bk-icon"
                            :class="{ 'icon-arrows-up up': data.lastAnalysisResult.defectChange > 0,
                                      'icon-arrows-down down': data.lastAnalysisResult.defectChange < 0 }">
                        </i>
                    </bk-col>
                    <bk-col><a @click="toList">{{data.lastAnalysisResult.fileCount === undefined ? '--' : data.lastAnalysisResult.fileCount}}</a></bk-col>
                    <bk-col><a @click="toList">{{data.lastAnalysisResult.fileChange === undefined ? '--' : Math.abs(data.lastAnalysisResult.fileChange)}}</a>
                        <i class="bk-icon"
                            :class="{ 'icon-arrows-up up': data.lastAnalysisResult.fileChange > 0,
                                      'icon-arrows-down down': data.lastAnalysisResult.fileChange < 0 }">
                        </i>
                    </bk-col>
                </bk-row>
                <bk-row class="card-txt">
                    <bk-col><i class="codecc-icon icon-danger"></i>{{$t('问题数')}}</bk-col>
                    <bk-col><i class="codecc-icon icon-trend"></i>{{$t('问题趋势')}}</bk-col>
                    <bk-col><i class="codecc-icon icon-risky-file"></i>{{$t('文件数')}}</bk-col>
                    <bk-col><i class="codecc-icon icon-trend"></i>{{$t('文件数趋势')}}</bk-col>
                </bk-row>
            </bk-container>
        </div>
        <div class="card-empty" v-if="!data.lastAnalysisResult">
            <empty :title="data.toolName ? $t('暂无分析结果') : $t('暂无工具')" size="small" />
        </div>
    </div>
</template>
<script>
    import { mapState } from 'vuex'
    import { format } from 'date-fns'
    import { getToolStatus, formatSeconds } from '@/common/util'
    import Empty from '@/components/empty'

    export default {
        components: {
            Empty
        },
        props: {
            data: {
                type: Object,
                default () {
                    return {
                        lastAnalysisTime: 0,
                        elapseTime: 0,
                        lastAnalysisResult: {},
                        toolName: ''
                    }
                }
            }
        },
        data () {
            return {
            }
        },
        computed: {
            ...mapState('tool', {
                toolMap: 'mapList'
            }),
            ...mapState([
                'toolMeta'
            ]),
            toolType () {
                const obj = {}
                this.toolMeta.TOOL_TYPE.forEach(item => {
                    if (item.name === '代码安全') item.name = '安全漏洞'
                    obj[item.key] = item.name
                })
                return obj
            },
            stepStatus () {
                let stepStatus = 0
                if (this.data.curStep && this.data.curStep < 5 && this.data.curStep > 0 && this.data.stepStatus !== 1) {
                    stepStatus = 3
                } else if (this.data.stepStatus === 1) {
                    stepStatus = 1
                }
                return stepStatus
            }
        },
        created () {
        },
        methods: {
            formatDate (date) {
                return format(date, 'YYYY-MM-DD HH:mm:ss')
            },
            formatSeconds (s) {
                return formatSeconds(s)
            },
            getToolStatus (num, tool) {
                return getToolStatus(num, tool)
            },
            toLogs () {
                const params = this.$route.params
                params.toolId = this.data.toolName
                this.$router.push({
                    name: 'task-detail-logs',
                    params
                })
            },
            toDefaultList () {
                const params = this.$route.params
                params.toolId = this.data.toolName
                this.$router.push({
                    name: 'defect-lint-list',
                    params
                })
            },
            toCCNList () {
                const params = this.$route.params
                params.toolId = this.data.toolName
                this.$router.push({
                    name: 'defect-ccn-list',
                    params
                })
            },
            toDupcList () {
                const params = this.$route.params
                params.toolId = this.data.toolName
                this.$router.push({
                    name: 'defect-dupc-list',
                    params
                })
            },
            toClocList () {
                this.$router.push({
                    name: 'defect-cloc-list'
                })
            },
            toCovList (query) {
                const params = this.$route.params
                params.toolId = this.data.toolName
                this.$router.push({
                    name: 'defect-coverity-list',
                    params,
                    query
                })
            },
            toList () {
                if (this.data.toolName === 'CCN') {
                    return this.toCCNList()
                } else if (this.data.toolName === 'DUPC') {
                    return this.toDupcList()
                } else if (this.data.toolName === 'CLOC') {
                    return this.toClocList()
                } else if (this.data.toolName === 'COVERITY' || this.data.toolName === 'KLOCWORK' || this.data.toolName === 'PINPOINT') {
                    return this.toCovList()
                } else {
                    return this.toDefaultList()
                }
            }
        }
    }
</script>
<style lang="postcss" scoped>
    @import '../../css/variable.css';

    .up {
        margin-left: -10px;
        color: $failColor;
    }
    .down {
        margin-left: -10px;
        color: $successColor;
    }
    .card {
        height: 160px;
        width: 100%;
        border: 1px solid $borderColor;
        padding: 12px;
        margin-bottom: 10px;
        background: #fff;
        &:hover {
            /* box-shadow: 0 3px 8px 0 rgba(0, 0, 0, 0.2), 0 0 0 1px rgba(0, 0, 0, 0.08); */
            /* cursor: pointer; */
            /* .card-head .card-time {
                transition: opacity 0s ease-in-out;
                opacity: 0;
                font-size: 0;
            } */
            /* .card-head .card-step {
                transition: opacity 0s ease-in-out;
                opacity: 0;
                font-size: 0;
            } */
            /* .card-head .card-analys {
                display: block;
                transition: opacity 500ms ease-in-out;
                opacity: 1;
                font-size: 12px;
            } */
            .card-step-txt {
                cursor: pointer;
                /* text-decoration: underline; */
            }
            a {
                cursor: pointer;
            }
            .card-content {
                .card-num {
                    a {
                        color: #3a84ff;
                    }
                }
            }
        }
        .card-head {
            .card-tool {
                font-size: 16px;
                font-weight: bolder;
                color: #63656E;
                padding: 0 9px 0 3px;
            }
            .card-split {
                color: #DCDEE5;
                padding: 0 6px 0 3px;
            }
            .card-type {
                display: inline-block;
                background-color: #c9dffa;
                margin-top: 4px;
                margin-left: 4px;
                padding: 2px 6px;
                font-size: 12px;
                color: #737987;
                border-radius: 2px;
            }
            .card-step {
                font-size: 14px;
                transition: opacity 500ms ease-in-out;
                opacity: 1;
                float: right;
                padding-right: 10px;
                .card-tool-status {
                    font-size: 14px;
                    position: relative;
                    /* top: 1px; */
                }
            }
            .card-time {
                font-size: 14px;
                color: #979BA5;
                float: right;
                /* width: 80px; */
                text-align: right;
                transition: opacity 500ms ease-in-out;
                opacity: 1;
            }
            .card-analys {
                float: right;
                opacity: 0;
                font-size: 0;
                /* width: 80px; */
                text-align: right;
            }
        }
        .card-content {
            width: 100%;
            text-align: center;
            .card-num {
                font-size: 24px;
                padding: 24px 0 4px 0;
                a {
                    color: #313238;
                }
                span {
                    font-size: 16px;
                }
                .bk-icon {
                    font-size: 24px;
                }
            }
            .card-txt {
                i {
                    padding: 0 3px;
                }
            }
        }
        .card-num {
            display: flex;
        }
        .card-empty {
            padding-top: 30px;
            >>>.empty .title {
                color: #737987;
            }
        }
    }
    .spin-icon {
        font-size: 14px;
        display:inline-block;
        animation: loading .8s linear infinite;
        color: $primaryColor;
    }
    @keyframes loading {
        from {
            transform: rotate(0);
        }
        to {
            transform: rotate(360deg)
        }
    }
</style>
