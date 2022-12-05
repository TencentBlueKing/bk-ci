<template>
    <bk-sideslider class="bkci-property-panel" :width="876" :is-show.sync="visible" :quick-close="true">
        <header :title="stageTitle" class="stage-panel-header" slot="header">
            {{ stageTitle }}
        </header>

        <component
            v-bind="$props"
            v-on="$listeners"
            :is="reviewComponent"
            :stage-control="stageControl"
            :stage-review-type="stageReviewType"
            :stage="stage"
            :disabled="stageReviewDisabled"
            slot="content"
            class="stage-review-content"
        ></component>
    </bk-sideslider>
</template>

<script>
    import { mapState, mapActions } from 'vuex'
    import reviewShow from './show'
    import reviewApprove from './approve'
    import reviewEnable from './enable-review'

    export default {
        name: 'StageReviewPanel',
        components: {
            reviewShow,
            reviewApprove,
            reviewEnable
        },
        computed: {
            ...mapState([
                'showStageReviewPanel'
            ]),
            stage () {
                return this.showStageReviewPanel.stage || {}
            },
            stageTitle () {
                const stageType = this.stageReviewType === 'checkIn' ? 'CheckIn' : 'CheckOut'
                return `${stageType} ${this.stage.name}`
            },
            stageReviewType () {
                return this.showStageReviewPanel.type
            },
            visible: {
                get () {
                    return this.showStageReviewPanel.isShow
                },
                set (value) {
                    this.toggleStageReviewPanel({
                        isShow: value
                    })
                }
            },
            reviewComponent () {
                let reviewComponent = 'reviewShow'
                if (this.isStagePause) reviewComponent = 'reviewApprove'
                if (!this.stageControl.manualTrigger && !this.stageControl.ruleIds) reviewComponent = 'reviewEnable'
                return reviewComponent
            },
            stageReviewDisabled () {
                return !this.editable && !this.isStagePause
            },
            isStageWait () {
                return this.stageControl.status === undefined
            },
            isStagePause () {
                try {
                    return this.stageControl.status === 'REVIEWING'
                } catch (error) {
                    return false
                }
            },
            stageControl () {
                return this.stage[this.stageReviewType] || {}
            }
        },
        methods: {
            ...mapActions([
                'toggleStageReviewPanel'
            ])
        }
    }
</script>

<style lang="postcss" scoped>
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
