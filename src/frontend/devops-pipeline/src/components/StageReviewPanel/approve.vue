<template>
    <section class="review-approve-home">
        <review-describe :desc="stageControl.reviewDesc"></review-describe>

        <review-flow-approve
            ref="flowApprove"
            :show-review-group.sync="showReviewGroup"
            :disabled="disabled"
            :review-groups="stageControl.reviewGroups"
            :timeout="stageControl.timeout"
            :stage="stage"
        ></review-flow-approve>

        <params-approve
            ref="paramsApprove"
            :show-review-group.sync="showReviewGroup"
            :disabled="disabled"
            :review-params="stageControl.reviewParams"
        ></params-approve>

        <section class="approve-footer">
            <bk-button
                theme="primary"
                class="approve-button"
                @click="confirmApprove"
                :loading="isApproving"
                :disabled="disabled"
            >
                {{ $t('confirm') }}
            </bk-button>
            <bk-button
                @click="cancelApprove"
                :disabled="isApproving"
            >
                {{ $t('cancel') }}
            </bk-button>
        </section>
    </section>
</template>

<script>
    import { mapActions } from 'vuex'
    import ParamsApprove from './components/params/approve'
    import ReviewFlowApprove from './components/reviewFlow/approve'
    import ReviewDescribe from './components/describe'

    export default {
        components: {
            ParamsApprove,
            ReviewFlowApprove,
            ReviewDescribe
        },

        props: {
            stageControl: Object,
            stage: Object
        },

        data () {
            return {
                showReviewGroup: this.getCurReviewGroup(),
                isApproving: false
            }
        },

        computed: {
            execReviewGroup () {
                return this.getCurReviewGroup()
            },

            isShowExecGroup () {
                return this.showReviewGroup === this.execReviewGroup
            },

            canTriggerStage () {
                const reviewGroups = this.stageControl.reviewGroups || []
                const curReviewGroup = reviewGroups.find((review) => (review.status === undefined))
                return curReviewGroup.reviewers.includes(this.$userInfo ? this.$userInfo.username : '')
            },

            disabled () {
                return !this.isShowExecGroup || !this.canTriggerStage
            }
        },

        methods: {
            ...mapActions('atom', [
                'triggerStage',
                'toggleStageReviewPanel'
            ]),

            getCurReviewGroup () {
                const reviewGroups = this.stageControl.reviewGroups || []
                const curReviewGroup = reviewGroups.find((review) => (review.status === undefined))
                return curReviewGroup
            },

            confirmApprove () {
                const { flowApprove, paramsApprove } = this.$refs
                this.isApproving = true
                Promise.all([flowApprove.getApproveData(), paramsApprove.getApproveData()]).then(([flowData, reviewParams]) => {
                    return this.triggerStage({
                        ...this.$route.params,
                        stageId: this.stage.id,
                        cancel: flowData.isCancel,
                        suggest: flowData.suggest,
                        id: flowData.id,
                        reviewParams
                    }).then(() => {
                        this.cancelApprove()
                        this.$emit('approve')
                        this.$bkMessage({ theme: 'success', message: this.$t('editPage.operateSuc'), limit: 1 })
                    })
                }).catch((err) => {
                    this.$bkMessage({ theme: 'error', message: err.message || err })
                }).finally(() => {
                    this.isApproving = false
                })
            },

            cancelApprove () {
                this.toggleStageReviewPanel({
                    showStageReviewPanel: {
                        isShow: false
                    }
                })
            }
        }
    }
</script>

<style lang="scss" scoped>
    .stage-review-content.review-approve-home {
        padding-bottom: 54px;
    }
    .approve-footer {
        position: absolute;
        right: 0;
        left: 0;
        bottom: 0;
        border-top: 1px solid transparent;
        background-color: rgb(250, 251, 253);
        height: 54px;
        display: flex;
        align-items: center;
    }
    .approve-button {
        margin: 0 8px 0 24px;
    }
</style>
