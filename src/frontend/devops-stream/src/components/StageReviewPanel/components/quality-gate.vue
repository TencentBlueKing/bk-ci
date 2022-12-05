<template>
    <section v-if="(stageControl.ruleIds || []).length">
        <span class="review-title">{{$t('pipeline.qualityGate')}}</span>
        <section class="review-quality" v-bkloading="{ isLoading }">
            <bk-collapse v-model="activeName">
                <bk-collapse-item v-for="(qualityItem, index) in qualityList" :key="index" :name="qualityItem.hashId">
                    <span class="quality-summary">
                        <span class="text-ellipsis summary-left">
                            <i
                                :class="[
                                    'mr5',
                                    'stream-icon',
                                    qualityItem.interceptResult === 'WAIT' ? 'stream-current-status' : 'stream-not-start',
                                    qualityItem.interceptResult
                                ]"
                            ></i>
                            <span class="quality-name text-ellipsis" v-bk-overflow-tips>{{ qualityItem.ruleName }}</span>
                            <span :class="{ 'wait-color': qualityItem.interceptResult === 'WAIT' }">{{ getInterceptValue(qualityItem.interceptResult) }}</span>
                            <span :class="{ 'wait-color': qualityItem.interceptResult === 'WAIT' }" v-if="qualityItem.interceptResult !== 'UNCHECK'">{{ getInterceptNum(qualityItem.interceptList) }}</span>
                        </span>
                        <span v-if="qualityItem.interceptResult === 'WAIT'" class="summary-right">
                            <bk-button
                                size="small"
                                class="mr10"
                                theme="primary"
                                :disabled="isNotGateKeeper(qualityItem)"
                                @click.stop="changeGateWayStatus(true, qualityItem.ruleHashId)"
                            >
                                <span
                                    v-bk-tooltips="{
                                        content: $t('pipeline.gateTips', [getGateKeeper(qualityItem).join(',')])
                                    }"
                                >{{$t('pipeline.continue')}}</span>
                            </bk-button>
                            <bk-button
                                size="small"
                                :disabled="isNotGateKeeper(qualityItem)"
                                @click.stop="changeGateWayStatus(false, qualityItem.ruleHashId)"
                            >
                                <span
                                    v-bk-tooltips="{
                                        content: $t('pipeline.gateTips', [getGateKeeper(qualityItem).join(',')])
                                    }"
                                >{{$t('pipeline.stop')}}</span>
                            </bk-button>
                        </span>
                        <span v-if="['INTERCEPT', 'INTERCEPT_PASS'].includes(qualityItem.interceptResult)" class="text-ellipsis summary-right" v-bk-overflow-tips>
                            {{ getOptValue(qualityItem) }}
                        </span>
                    </span>
                    <section slot="content">
                        <ul>
                            <li class="quality-content text-ellipsis" v-for="(intercept, interceptIndex) in qualityItem.interceptList" :key="interceptIndex">
                                <span :class="{ 'quality-icon': true, 'success': intercept.pass }" v-if="qualityItem.interceptResult !== 'UNCHECK'">
                                    <i :class="`bk-icon ${ intercept.pass ? 'icon-check-1' : 'icon-close' }`"></i>
                                </span>
                                <span class="text-ellipsis mr5" v-bk-overflow-tips>{{ getRuleName(intercept, qualityItem.interceptResult) }}</span>
                                <bk-link
                                    v-if="intercept.logPrompt"
                                    :href="intercept.logPrompt"
                                    theme="primary"
                                    class="quality-link"
                                    target="_blank"
                                >{{$t('pipeline.details')}}</bk-link>
                            </li>
                        </ul>
                    </section>
                </bk-collapse-item>
            </bk-collapse>
        </section>
        <bk-divider class="review-quality-divider"></bk-divider>
    </section>
</template>

<script>
    import { mapState } from 'vuex'
    import { pipelines } from '@/http'
    import { timeFormatter } from '@/utils'

    export default {
        props: {
            stageControl: {
                type: Object,
                default: () => ({})
            }
        },

        data () {
            return {
                activeName: [],
                isLoading: false,
                qualityList: []
            }
        },

        computed: {
            ...mapState(['projectId', 'user'])
        },

        created () {
            this.requestQualityLineFromApi()
        },

        methods: {
            getRuleName (intercept, interceptResult) {
                const { indicatorName, operation, actualValue, value } = intercept
                const operationMap = {
                    GT: '>',
                    GE: '>=',
                    LT: '<',
                    LE: '<=',
                    EQ: '='
                }
                return `${indicatorName}当前值(${interceptResult !== 'UNCHECK' ? (actualValue || 'null') : ''})，期望${operationMap[operation]}${value || 'null'}`
            },

            getGateKeeper (qualityItem) {
                const { qualityRuleBuildHisOpt: { gateKeepers = [] } } = qualityItem
                return gateKeepers
            },

            isNotGateKeeper (qualityItem) {
                const gateKeepers = this.getGateKeeper(qualityItem)
                return gateKeepers.length <= 0 || !gateKeepers.includes(this.user.username)
            },

            getOptValue (qualityItem) {
                const {
                    interceptResult,
                    qualityRuleBuildHisOpt: {
                        gateOptUser,
                        gateOptTime
                    }
                } = qualityItem
                const optNameMap = {
                    INTERCEPT: 'Stopped',
                    INTERCEPT_PASS: 'Passed'
                }
                return `${optNameMap[interceptResult]} by ${gateOptUser} at ${timeFormatter(gateOptTime)}`
            },

            getInterceptNum (interceptList = []) {
                const paasNum = interceptList.filter(x => x.pass)
                return `（${paasNum.length}/${interceptList.length}）`
            },

            getInterceptValue (val) {
                const resultMap = {
                    PASS: this.$t('pipeline.passed'),
                    FAIL: this.$t('pipeline.blocked'),
                    WAIT: this.$t('pipeline.waitForApproval'),
                    INTERCEPT: this.$t('pipeline.blocked'),
                    INTERCEPT_PASS: this.$t('pipeline.passed')
                }
                return resultMap[val]
            },

            requestQualityLineFromApi () {
                if ((this.stageControl.ruleIds || []).length <= 0) return

                const params = [
                    this.projectId,
                    this.$route.params.pipelineId,
                    this.$route.params.buildId,
                    this.stageControl.ruleIds,
                    this.stageControl.checkTimes || 1
                ]

                this.isLoading = true
                pipelines.requestQualityGate(...params).then((res) => {
                    this.qualityList = res || []
                    this.activeName.push(...this.qualityList.map(x => x.hashId))
                }).catch((err) => {
                    this.$bkMessage({ theme: 'error', message: err.message || err })
                }).finally(() => {
                    this.isLoading = false
                })
            },

            changeGateWayStatus (val, hashId) {
                this.isLoading = true
                pipelines.changeGateWayStatus(val, hashId).then(() => {
                    return this.requestQualityLineFromApi()
                }).catch((err) => {
                    this.$bkMessage({ theme: 'error', message: err.message || err })
                }).finally(() => {
                    this.isLoading = false
                })
            }
        }
    }
</script>

<style lang="postcss" scoped>
    .review-title {
        color: #666770;
        line-height: 20px;
        padding-left: 7px;
        position: relative;
        display: block;
        font-size: 14px;
        font-weight: bold;
        &:before {
            content: '';
            position: absolute;
            width: 2px;
            height: 16px;
            background: #3a84ff;
            left: 0;
            top: 2px;
        }
    }
    .review-quality {
        margin-top: 16px;
        /deep/ .bk-collapse {
            display: flex;
            flex-wrap: wrap;
            font-size: 12px;
            .bk-collapse-item {
                width: 100%;
                margin-bottom: 20px;
                .bk-collapse-item-header {
                    line-height: 28px;
                    height: 28px;
                    font-size: 12px;
                    &:hover {
                        color: inherit;
                    }
                }
            }
            .quality-title {
                margin-top: 8px;
                color: #666770;
            }
            .quality-content {
                margin-top: 12px;
                margin-left: 25px;
                color: #666770;
                display: flex;
                align-items: center;
            }
        }
        .quality-summary {
            display: flex;
            align-items: center;
            justify-content: space-between;
            .summary-left {
                width: 60%;
                display: flex;
                align-items: center;
                .quality-name {
                    max-width: calc(100% - 120px);
                    margin-right: 12px;
                }
                .wait-color {
                    color: #ff9c01;
                }
            }
            .summary-right {
                width: 36%;
                ::v-deep .bk-button-text {
                    font-size: 12px;
                }
            }
        }
        .quality-icon {
            width: 14px;
            height: 14px;
            border-radius: 100%;
            background: #ffdddd;
            box-sizing: border-box;
            display: inline-block;
            line-height: 12px;
            text-align: center;
            margin-right: 6px;
            &.success {
                background: #e5f6ea;
            }
            .bk-icon {
                font-size: 14px;
            }
        }
        .icon-close, .error {
            color: #ea3636;
        }
        .icon-check-1, .success {
            color: #3fc06d;
        }
    }
    .review-quality-divider {
        margin-bottom: 24px !important;
        margin-top: 12px !important;
    }
    .WAIT {
        color: #3a84ff;
    }
    .PASS {
        color: #3fc06d;
    }
    .FAIL {
        color: #ea3636;
    }
    .INTERCEPT {
        color: #ea3636;
    }
    .INTERCEPT_PASS {
        color: #3fc06d;
    }

    .quality-link {
        margin-left: 5px;
        /deep/ .bk-link-text {
            font-size: 12px;
        }
    }
</style>
