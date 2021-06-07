<template>
    <bk-sideslider class="bkci-property-panel" width="640" :is-show.sync="visible" :quick-close="true">
        <header :title="stageTitle" class="stage-panel-header" slot="header">
            {{ stageTitle }}
        </header>
        <stage-review-control slot="content" v-bind="$props"></stage-review-control>
    </bk-sideslider>
</template>

<script>
    import { mapState, mapActions } from 'vuex'
    import StageReviewControl from './StageReviewControl'

    export default {
        name: 'stage-review-panel',
        components: {
            StageReviewControl
        },
        props: {
            stage: {
                type: Object,
                default: () => ({})
            },
            disabled: {
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
            }
        },
        methods: {
            ...mapActions('atom', [
                'toggleStageReviewPanel'
            ])
        }
    }
</script>
