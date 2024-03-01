<template>
    <div
        :class="{
            'stage-check-icon': true,
            'pointer': true,
            [reviewStatausIcon]: true,
            'is-readonly-check-icon': !isExecDetail && !editable
        }"
        v-bk-tooltips.top="reviewTooltip"
        @click.stop="handleStageCheckIn"
    >
        <Logo :name="reviewStatausIcon" size="28" />
        <span class="stage-check-txt" v-if="checkTxt">{{ t(checkTxt) }}</span>
    </div>
</template>

<script>
    import { eventBus } from './util'
    import { localeMixins } from './locale'
    import Logo from './Logo'
    import {
        STATUS_MAP,
        STAGE_CHECK
    } from './constants'
    export default {
        name: 'stage-check-icon',
        components: {
            Logo
        },
        mixins: [localeMixins],
        props: {
            editable: Boolean,
            stageCheck: Object,
            stageIndex: Number,
            userName: String,
            stageStatus: {
                type: String
            },
            isExecDetail: Boolean,
            checkType: {
                type: String,
                default: 'checkIn'
            }
        },
        computed: {
            hasRuleId () {
                const { stageCheck = {} } = this
                return Array.isArray(stageCheck.ruleIds) && stageCheck.ruleIds.length > 0
            },
            reviewTooltip () {
                const { stageCheck = {} } = this
                const reviewGroups = stageCheck.reviewGroups || []
                const curReviewGroup = reviewGroups.find((review) => (review.status === undefined)) || {}
                const canTriggerStage = (curReviewGroup.reviewers || []).includes(this.userName)
                const isStagePause = stageCheck.status !== STATUS_MAP.REVIEWING
                return {
                    content: canTriggerStage ? this.t('toCheck') : this.t('noAuthToCheck'),
                    disabled: isStagePause
                }
            },
            checkTxt () {
                const { stageCheck = {}, hasRuleId } = this
                let txt = hasRuleId ? 'quality' : ''
                if (stageCheck.manualTrigger) {
                    txt = 'stageCheck'
                }
                
                if (!stageCheck.status) return txt

                switch (stageCheck.status) {
                    case STATUS_MAP.QUALITY_CHECK_FAIL:
                    case STATUS_MAP.QUALITY_CHECK_PASS:
                    case STATUS_MAP.QUALITY_CHECK_WAIT:
                        txt = 'quality'
                        break
                    case STATUS_MAP.REVIEW_PROCESSED:
                    case STATUS_MAP.REVIEW_ABORT:
                    case STATUS_MAP.REVIEWING:
                        txt = 'stageCheck'
                        break
                }
                return txt
            },
            reviewStatausIcon () {
                const { stageCheck = {} } = this
                try {
                    if (stageCheck.isReviewError) return 'review-error'
                    switch (true) {
                        case stageCheck.status === STATUS_MAP.REVIEWING:
                        case stageCheck.status === STATUS_MAP.QUALITY_CHECK_WAIT:
                            return 'reviewing'
                        case stageCheck.status === STATUS_MAP.QUEUE:
                            return 'review-pause'
                        case stageCheck.status === STATUS_MAP.REVIEW_ABORT:
                        case stageCheck.status === STATUS_MAP.QUALITY_CHECK_FAIL:
                            return 'quality-check-error'
                        case stageCheck.status === STATUS_MAP.REVIEW_PROCESSED:
                        case stageCheck.status === STATUS_MAP.QUALITY_CHECK_PASS:
                            return 'quality-check'
                        case this.stageStatus === STATUS_MAP.SKIP:
                        case !this.stageStatus && this.isExecDetail:
                        case stageCheck.status === undefined && this.isExecDetail && (!this.stageStatus || this.checkType === 'checkOut'):
                            return stageCheck.manualTrigger || this.hasRuleId ? 'review-pause' : 'review-auto-gray'
                        case !!this.stageStatus:
                            return 'review-auto-pass'
                        default:
                            return stageCheck.manualTrigger ? 'review-enable' : 'review-auto'
                    }
                } catch (e) {
                    console.warn('get review icon error: ', e)
                    return 'review-auto'
                }
            }
        },
        methods: {
            handleStageCheckIn () {
                eventBus.$emit(STAGE_CHECK, {
                    type: this.checkType,
                    stageIndex: this.stageIndex
                })
            }
        }
    }
</script>

<style lang="scss">
  @use "sass:math";
    @import './conf';

  .stage-check-icon {
    border-radius: 100px;
    border: 1px solid #d0d8ea;
    display: flex;
    align-items: center;
    background: #fff;
    font-size: 12px;
    z-index: 3;
    
    color: $fontWeightColor;

    &.is-readonly-check-icon {
        filter: grayscale(100%);
    }
    &.reviewing {
        color: $primaryColor;
        border-color: $primaryColor;
    }
    
    &.quality-check {
      color: $successColor;
      border-color: $successColor;
    }
    &.quality-check-error,
    &.review-error {
      color: $failColor;
      border-color: $failColor;
    }

    .stage-check-txt {
        padding-right: 10px;
    }
  }
</style>
