<template>
    <div class="quality-gate-tips">
        <div class="tips-icon">
            <logo name="logo-gate" size="19" style="fill:#fff;" />
        </div>
        <div class="tips-content">
            <div class="rule-detail" v-for="entry in relativeRuleList" :key="entry.taskId">
                <p class="title" v-if="entry.ruleList.length">
                    <span class="caveat">{{ $t('details.quality.warning') }}</span>：{{ $t('details.quality.ruleNameTips') }}
                    <span
                        class="rule-name"
                        v-for="(rule, index) in (entry.ruleList)"
                        :key="index"
                        @click="toLinkRule(rule.ruleHashId)"
                    >
                        {{ rule.ruleName }}
                        <span>（{{ entry.controlStage.name === 'BEFORE' ? $t('details.quality.allowEnter') : $t('details.quality.allowLeave') }}）</span>
                    </span>{{ $t('details.quality.affect') }}
                    <span>{{ entry.taskName }}</span>{{ $t('details.quality.execConditions') }}
                    <span v-if="entry.controlStage.name === 'BEFORE'">{{ $t('details.quality.execCurrent') }}</span>
                    <span v-if="entry.controlStage.name === 'AFTER'">{{ $t('details.quality.execAfter') }}</span>
                </p>
                <div class="threshold-list" v-if="entry.thresholdList.length">
                    <p
                        class="threshold-item"
                        v-for="(threshold, index) in (entry.thresholdList)"
                        :key="index"
                    >
                        <span>-</span>
                        <span>{{ threshold.indicatorName }}</span>
                        <span>{{ indexHandlerConf[threshold.operation] }}</span>
                        <span>{{ threshold.value }}</span>
                    </p>
                </div>
                <p class="notice-type">
                    <span v-if="!entry.auditUserList.length">{{ $t('details.quality.stopConditions') }}</span>
                    <span v-if="entry.auditUserList.length">
                        {{ $t('details.quality.by') }}
                        <span
                            v-for="(reviewr, index) in (entry.auditUserList)"
                            :key="index"
                        >
                            {{ reviewr }}
                            <span v-if="entry.auditUserList.length === 2 && index === 0">;</span>
                        </span>{{ $t('details.quality.toCheck') }}
                    </span>
                </p>
            </div>
        </div>
    </div>
</template>

<script>
    import atomFieldMixin from '../atomFieldMixin'
    import Logo from '@/components/Logo'

    export default {
        components: {
            Logo
        },
        mixins: [atomFieldMixin],
        props: {
            relativeRuleList: {
                type: Array,
                default: []
            }
        },
        data () {
            return {
                indexHandlerConf: {
                    LT: '<',
                    LE: '<=',
                    GT: '>',
                    GE: '>=',
                    EQ: '='
                }
            }
        },
        methods: {
            toLinkRule (id) {
                const url = `${WEB_URL_PREFIX}/quality/${
                    this.$route.params.projectId
                }/ruleList?linkId=${id}`
                window.open(url, '_blank')
            }
        }
    }
</script>

<style lang="scss">
@import "../../../scss/conf.scss";
.quality-gate-tips {
    display: flex;
    margin-top: 8px;
    border: 1px solid #e6e6e6;
    .tips-icon {
        display: flex;
        justify-content: center;
        align-items: center;
        min-width: 44px;
        background-color: #ffb400;
        i {
            display: inline-block;
            font-size: 18px;
            color: #fff;
        }
    }
    .tips-content {
        padding: 10px 20px;
        .title {
            .caveat {
                font-weight: bold;
            }
            .rule-name {
                color: $primaryColor;
                cursor: pointer;
            }
        }
        .notice-type {
            margin-top: 20px;
        }
    }
    .rule-detail {
        margin-bottom: 30px;
        &:last-child {
            margin-bottom: 0;
        }
    }
}
</style>
