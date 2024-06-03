<template>
    <section class="review-show">
        <quality-gate :stage-control="stageControl"></quality-gate>

        <template v-if="stageControl.manualTrigger">
            <review-describe :desc="stageControl.reviewDesc"></review-describe>
            <review-flow-show :review-groups="stageControl.reviewGroups"></review-flow-show>
            <params-show :params="stageControl.reviewParams"></params-show>
        </template>
    </section>
</template>

<script>
    import ParamsShow from './components/params/show'
    import ReviewFlowShow from './components/reviewFlow/show'
    import ReviewDescribe from './components/describe'
    import QualityGate from './components/quality-gate'

    export default {
        components: {
            ParamsShow,
            ReviewFlowShow,
            ReviewDescribe,
            QualityGate
        },

        props: {
            stageControl: Object
        }
    }
</script>
