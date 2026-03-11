<template>
    <section>
        <span class="review-subtitle">
            {{$t('pipeline.approveFlow')}}
            <span class="review-clock" v-bk-tooltips="{ width: 500, content: $t('pipeline.approvalTimeTips') }">
                <i class="bk-icon icon-clock"></i>
                {{ computedTime }}
            </span>
        </span>

        <bk-steps
            controllable
            class="review-steps"
            :steps="computedReviewSteps"
            :cur-step="curStep"
            @step-changed="stepChange"
        ></bk-steps>
        <bk-divider></bk-divider>

        <span class="review-subtitle mt12">
            {{$t('pipeline.currentStatus')}}<span class="gray-color ml20">{{ computedStatusTxt }}</span>
        </span>
        <bk-radio-group v-model="isCancel" class="review-result">
            <bk-radio :value="false" :disabled="disabled">
                {{$t('pipeline.approve')}} <span class="gray-color">（{{$t('pipeline.approveResult')}}）</span>
            </bk-radio>
            <bk-radio :value="true" :disabled="disabled" class="ml40">
                {{$t('pipeline.reject')}} <span class="gray-color">（{{$t('pipeline.rejectResult')}}）</span>
            </bk-radio>
        </bk-radio-group>

        <span class="review-subtitle">{{$t('pipeline.approvalOpinion')}}</span>
        <bk-input
            :placeholder="$t('pipeline.opinionPlaceholder')"
            type="textarea"
            :rows="3"
            :maxlength="200"
            :disabled="disabled"
            v-model="suggest">
        </bk-input>
        <span class="error-message">{{ errMessage }}</span>
    </section>
</template>

<script>
    import { convertTime } from '@/utils/util'

    export default {
        props: {
            reviewGroups: Array,
            showReviewGroup: Object,
            disabled: Boolean,
            stage: Object,
            timeout: Number
        },

        data () {
            return {
                curStep: this.reviewGroups.findIndex(x => x === this.showReviewGroup) + 1,
                isCancel: false,
                suggest: '',
                errMessage: ''
            }
        },

        computed: {
            computedReviewSteps () {
                const getStatus = (item, index) => {
                    const statusMap = {
                        ABORT: 'error',
                        PROCESS: 'done'
                    }
                    let status = statusMap[item.status]

                    const curExecIndex = this.reviewGroups.findIndex(x => x.status === undefined)
                    if (curExecIndex === index) status = 'loading'

                    let icon = index + 1
                    if (!status) icon = ` stream-${index + 1} stream-icon`

                    return { status, title: item.name, icon }
                }

                return this.reviewGroups.map((item, index) => getStatus(item, index))
            },

            computedTime () {
                try {
                    const hour2Ms = 60 * 60 * 1000
                    return convertTime(this.stage.startEpoch + this.timeout * hour2Ms)
                } catch (e) {
                    return '--'
                }
            },

            computedStatusTxt () {
                const curExecIndex = this.reviewGroups.findIndex(x => x.status === undefined) + 1
                const { reviewers, operator } = this.showReviewGroup

                let statusTxt = this.$t('pipeline.approvedStatusDesc') + `: ${operator}`
                if (curExecIndex < this.curStep) statusTxt = this.$t('pipeline.previousStatusDesc') + `: ${reviewers.join(', ')}`
                if (curExecIndex === this.curStep) statusTxt = this.$t('pipeline.pendingArrStatusDesc') + `: ${reviewers.join(', ')}`

                return statusTxt
            }
        },

        watch: {
            showReviewGroup: {
                handler () {
                    this.suggest = this.showReviewGroup.suggest || ''
                    this.isCancel = this.showReviewGroup.status === 'ABORT'
                },
                immediate: true
            }
        },

        methods: {
            stepChange (index) {
                this.curStep = index
                const showReviewGroup = this.reviewGroups[index - 1]
                this.$emit('update:showReviewGroup', showReviewGroup)
            },

            getApproveData () {
                return new Promise((resolve, reject) => {
                    if (this.isCancel && this.suggest === '') {
                        this.errMessage = this.$t('pipeline.rejectOpinionTips')
                        reject(new Error(this.errMessage))
                    } else {
                        this.errMessage = ''
                        resolve({
                            isCancel: this.isCancel,
                            suggest: this.suggest,
                            id: this.showReviewGroup.id
                        })
                    }
                })
            }
        }
    }
</script>

<style lang="postcss" scoped>
    .review-steps {
        margin: 25px 0 32px;
        /deep/ .bk-step {
            max-width: 367.56px;
            .stream-icon {
                font-family: 'stream' !important;
            }
        }
        /deep/ .bk-devops-icon {
            font-family: 'bk-devops' !important;
        }
    }
    .review-result {
        margin-top: 2px;
    }
    .gray-color {
        color: #979BA5;
    }
    .mt12 {
        margin-top: 12px !important;
    }
    .error-message {
        font-size: 12px;
        color: #ea3636;
        line-height: 18px;
    }
    .review-clock {
        color: #3a84ff;
        margin-left: 24px;
        display: inline-flex;
        align-items: center;
        .bk-icon {
            margin-right: 3px;
        }
    }
</style>
