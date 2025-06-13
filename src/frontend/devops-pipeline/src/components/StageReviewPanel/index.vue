<template>
    <bk-sideslider
        class="bkci-property-panel"
        :class="{ 'with-variable-open': showVariable }"
        :z-index="2016"
        width="876"
        :is-show.sync="visible"
        :quick-close="true"
    >
        <header
            :title="stageTitle"
            class="stage-panel-header"
            slot="header"
        >
            {{ stageTitle }}
        </header>

        <component
            v-bind="$props"
            v-on="$listeners"
            :is="reviewComponent"
            :stage-control="stageControl"
            :stage-review-type="stageReviewType"
            :disabled="stageReviewDisabled"
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
            editable: {
                type: Boolean,
                default: false
            }
        },
        computed: {
            ...mapState('atom', [
                'showVariable',
                'showStageReviewPanel'
            ]),
            stageTitle () {
                return `${this.$t('stageInTitle')}${typeof this.stage !== 'undefined' && this.stage.name ? this.stage.name : 'stage'}`
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
                        showStageReviewPanel: {
                            isShow: value
                        }
                    })
                }
            },
            reviewComponent () {
                let reviewComponent = 'reviewShow'
                if (this.isStagePause) reviewComponent = 'reviewApprove'
                if (this.editable || this.isStageWait) reviewComponent = 'reviewEdit'
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
        mounted () {
            this.preZIndex = window.__bk_zIndex_manager.zIndex
            console.log(this.preZIndex)
            window.__bk_zIndex_manager.zIndex = 2020
        },
        beforeDestroy () {
            window.__bk_zIndex_manager.zIndex = this.preZIndex
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
        position: relative;
        min-height: 100%;
    }
    ::v-deep .review-subtitle {
        display: block;
        margin: 24px 0 8px;
        font-size: 12px;
    }
</style>
