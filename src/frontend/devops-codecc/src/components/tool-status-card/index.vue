<template>
    <div class="card">
        <div class="card-head">
            <span class="card-tool">{{$t(`toolName.${toolMap[data.toolName]['displayName']}`)}}</span>
            <span v-if="data.lastAnalysisResult" class="card-split">|</span>
            <span v-if="data.lastAnalysisResult" class="card-step" :class="{ 'success': stepStatus === 0, 'fail': stepStatus === 1, 'loading': stepStatus === 3 }">
                <i class="bk-icon card-tool-status"
                    :class="{ 'icon-check-circle-shape': stepStatus === 0, 'icon-close-circle-shape': stepStatus === 1 }"
                >
                </i>
                <div class="bk-spin-loading bk-spin-loading-mini bk-spin-loading-primary" v-if="stepStatus === 3">
                    <div class="rotate rotate1"></div>
                    <div class="rotate rotate2"></div>
                    <div class="rotate rotate3"></div>
                    <div class="rotate rotate4"></div>
                    <div class="rotate rotate5"></div>
                    <div class="rotate rotate6"></div>
                    <div class="rotate rotate7"></div>
                    <div class="rotate rotate8"></div>
                </div>
                {{$t(`detail.${getToolStatus(data.curStep)}`)}}
            </span>
            
            <span v-if="data.lastAnalysisResult" class="card-time">{{$t('detail.最近分析时间')}}: {{formatDate(data.lastAnalysisTime)}} {{$t('detail.耗时')}} {{formatSeconds(data.elapseTime)}}</span>
            <a class="card-analys" href="javascript:;" @click="toLogs">{{$t('detail.分析详情')}}>></a>
        </div>
        <div v-if="data.lastAnalysisResult">
            <bk-container class="card-content" :col="4" v-if="data.toolName === 'CCN'">
                <bk-row class="card-num">
                    <bk-col><a @click="toCCNList">{{data.lastAnalysisResult.defectCount}}</a></bk-col>
                    <bk-col><a @click="toCCNList">{{Math.abs(data.lastAnalysisResult.defectChange)}}</a>
                        <i class="bk-icon"
                            :class="{ 'icon-arrows-up up': data.lastAnalysisResult.defectChange > 0,
                                      'icon-arrows-down down': data.lastAnalysisResult.defectChange < 0 }"
                        >
                        </i>
                    </bk-col>
                    <bk-col><a @click="toCCNList">{{(data.lastAnalysisResult.averageCCN).toFixed(2)}}</a></bk-col>
                    <bk-col><a @click="toCCNList">{{Math.abs(data.lastAnalysisResult.averageCCNChange).toFixed(2)}}</a>
                        <i class="bk-icon"
                            :class="{ 'icon-arrows-up up': data.lastAnalysisResult.averageCCNChange > 0,
                                      'icon-arrows-down down': data.lastAnalysisResult.averageCCNChange < 0 }"
                        >
                        </i>
                    </bk-col>
                </bk-row>
                <bk-row class="card-txt">
                    <bk-col><i class="bk-iconcool bk-icon-fengxianhanshu-shi"></i>{{$t('detail.风险函数')}}</bk-col>
                    <bk-col><i class="bk-iconcool bk-icon-qushi"></i>{{$t('detail.风险函数趋势')}}</bk-col>
                    <bk-col><i class="bk-iconcool bk-icon-pingjuzhi-shi"></i>{{$t('detail.平均圈复杂度')}}</bk-col>
                    <bk-col><i class="bk-iconcool bk-icon-qushi"></i>{{$t('detail.圈复杂度趋势')}}</bk-col>
                </bk-row>
            </bk-container>
            <bk-container class="card-content" :col="4" v-else-if="data.toolName === 'DUPC'">
                <bk-row class="card-num">
                    <bk-col><a @click="toDupcList">{{data.lastAnalysisResult.defectCount}}</a></bk-col>
                    <bk-col><a @click="toDupcList">{{Math.abs(data.lastAnalysisResult.defectChange)}}</a>
                        <i class="bk-icon"
                            :class="{ 'icon-arrows-up up': data.lastAnalysisResult.defectChange > 0,
                                      'icon-arrows-down down': data.lastAnalysisResult.defectChange < 0 }"
                        >
                        </i>
                    </bk-col>
                    <bk-col><a @click="toDupcList">{{(data.lastAnalysisResult.dupRate).toFixed(2)}}<span>%</span></a></bk-col>
                    <bk-col><a @click="toDupcList">{{Math.abs(data.lastAnalysisResult.dupRateChange).toFixed(2)}}<span>%</span></a>
                        <i class="bk-icon"
                            :class="{ 'icon-arrows-up up': data.lastAnalysisResult.dupRateChange > 0,
                                      'icon-arrows-down down': data.lastAnalysisResult.dupRateChange < 0 }"
                        >
                        </i>
                    </bk-col>
                </bk-row>
                <bk-row class="card-txt">
                    <bk-col><i class="bk-iconcool bk-icon-fengxianwenjian-shi"></i>{{$t('detail.重复文件')}}</bk-col>
                    <bk-col><i class="bk-iconcool bk-icon-qushi"></i>{{$t('detail.重复文件趋势')}}</bk-col>
                    <bk-col><i class="bk-iconcool bk-icon-pingjuzhi-shi"></i>{{$t('detail.平均重复率')}}</bk-col>
                    <bk-col><i class="bk-iconcool bk-icon-qushi"></i>{{$t('detail.重复率趋势')}}</bk-col>
                </bk-row>
            </bk-container>
            <bk-container class="card-content" :col="4" v-else>
                <bk-row class="card-num">
                    <bk-col><a @click="toDefaultList">{{data.lastAnalysisResult.defectCount}}</a></bk-col>
                    <bk-col><a @click="toDefaultList">{{Math.abs(data.lastAnalysisResult.defectChange)}}</a>
                        <i class="bk-icon"
                            :class="{ 'icon-arrows-up up': data.lastAnalysisResult.defectChange > 0,
                                      'icon-arrows-down down': data.lastAnalysisResult.defectChange < 0 }"
                        >
                        </i>
                    </bk-col>
                    <bk-col><a @click="toDefaultList">{{data.lastAnalysisResult.fileCount}}</a></bk-col>
                    <bk-col><a @click="toDefaultList">{{Math.abs(data.lastAnalysisResult.fileChange)}}</a>
                        <i class="bk-icon"
                            :class="{ 'icon-arrows-up up': data.lastAnalysisResult.fileChange > 0,
                                      'icon-arrows-down down': data.lastAnalysisResult.fileChange < 0 }"
                        >
                        </i>
                    </bk-col>
                </bk-row>
                <bk-row class="card-txt">
                    <bk-col><i class="bk-iconcool bk-icon-weixian"></i>{{$t('detail.告警数')}}</bk-col>
                    <bk-col><i class="bk-iconcool bk-icon-qushi"></i>{{$t('detail.告警趋势')}}</bk-col>
                    <bk-col><i class="bk-iconcool bk-icon-fengxianwenjian-shi"></i>{{$t('detail.文件数')}}</bk-col>
                    <bk-col><i class="bk-iconcool bk-icon-qushi"></i>{{$t('detail.文件数趋势')}}</bk-col>
                </bk-row>
            </bk-container>
        </div>
        <div class="card-empty" v-if="!data.lastAnalysisResult">
            <empty :title="$t('st.暂无分析结果')" size="small" />
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
            stepStatus () {
                let stepStatus = 0
                if (this.data.curStep && this.data.curStep < 5 && this.data.curStep > 0 && this.data.stepStatus !== 1) {
                    stepStatus = 3
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
            getToolStatus (num) {
                return getToolStatus(num)
            },
            toLogs () {
                const params = this.$route.params
                params.toolId = this.data.toolName
                this.$router.push({
                    name: 'tool-logs',
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
            }
        }
    }
</script>
<style lang="postcss" scoped>
    @import '../../css/variable.css';
    @import '../../assets/bk_icon_font/style.css';

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
            box-shadow: 0 3px 8px 0 rgba(0, 0, 0, 0.2), 0 0 0 1px rgba(0, 0, 0, 0.08);
            .card-head .card-time {
                transition: opacity 0s ease-in-out;
                opacity: 0;
                font-size: 0;
            }
            .card-head .card-analys {
                display: block;
                transition: opacity 1s ease-in-out;
                opacity: 1;
                font-size: 14px;
            }
            .card-content {
                .card-num {
                    a {
                        color: #3a84ff;
                        cursor: pointer;
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
            .card-step {
                font-size: 14px;
                .bk-spin-loading-mini {
                    vertical-align: sub;
                }
            }
            .card-time {
                font-size: 12px;
                color: #979BA5;
                float: right;
                transition: opacity 1s ease-in-out;
                opacity: 1;
            }
            .card-analys {
                float: right;
                opacity: 0;
                font-size: 0;
            }
        }
        .card-content {
            width: 100%;
            text-align: center;
            .card-num {
                font-size: 32px;
                padding: 24px 0 4px 0;
                a {
                    color: #313238;
                }
                i, span {
                    font-size: 16px;
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
</style>
