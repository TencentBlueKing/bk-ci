<template>
    <bk-sideslider class="bkci-property-panel" width="876" :is-show.sync="visible" :quick-close="true">
        <header :title="stageTitle" class="stage-panel-header" slot="header">
            {{ stageTitle }}
        </header>

        <component
            v-bind="$props"
            :is="reviewComponent"
            :stage-control="stageControl"
            slot="content"
            class="stage-review-content"
        ></component>
    </bk-sideslider>
</template>

<script>
    import { mapState, mapActions } from 'vuex'
    import reviewEdit from './edit'
    import reviewShow from './show'
    import reviewApprove from './approve'

    export default {
        name: 'stage-review-panel',
        components: {
            reviewEdit,
            reviewShow,
            reviewApprove
        },
        props: {
            stage: {
                type: Object,
                default: () => ({})
            },
            disabled: {
                type: Boolean,
                default: false
            },
            editable: {
                type: Boolean,
                default: false
            }
        },
        computed: {
            ...mapState('atom', [
                'showStageReviewPanel'
            ]),
            stageTitle () {
                return `${this.$t('stageInTitle')}${typeof this.stage !== 'undefined' ? this.stage.name : 'stage'}`
            },
            visible: {
                get () {
                    return this.showStageReviewPanel
                },
                set (value) {
                    this.toggleStageReviewPanel({
                        isShow: value
                    })
                }
            },
            reviewComponent () {
                let reviewComponent = 'reviewShow'
                if (this.editable) reviewComponent = 'reviewEdit'
                if (this.canTriggerStage && this.isStagePause) reviewComponent = 'reviewApprove'
                return reviewComponent
            },
            canTriggerStage () {
                try {
                    const reviewGroups = this.stageControl.reviewGroups || []
                    const curReviewGroup = reviewGroups.find((review) => (review.status === undefined))
                    return curReviewGroup.reviewers.includes(this.$userInfo.username)
                } catch (e) {
                    return false
                }
            },
            isStagePause () {
                try {
                    return this.stage.reviewStatus === 'REVIEWING'
                } catch (error) {
                    return false
                }
            },
            stageControl () {
                if (this.stage && this.stage.stageControlOption) {
                    return this.stage.stageControlOption
                }
                return {}
            }
        },
        methods: {
            ...mapActions('atom', [
                'toggleStageReviewPanel'
            ])
        }
    }
</script>

<style lang="scss" scoped>
    .stage-review-content {
        padding: 23px 33px;
        font-size: 12px;
    }
    /deep/ .review-subtitle {
        display: block;
        margin: 24px 0 8px;
        font-size: 12px;
    }
</style>
