<template>
    <section class="review-show">
        <review-describe :desc="stageControl.reviewDesc"></review-describe>
        <review-flow-show :review-groups="stageControl.reviewGroups"></review-flow-show>
        <params-show :params="stageControl.reviewParams"></params-show>
    </section>
</template>

<script>
    import ParamsShow from './components/params/show'
    import ReviewFlowShow from './components/reviewFlow/show'
    import ReviewDescribe from './components/describe'

    export default {
        components: {
            ParamsShow,
            ReviewFlowShow,
            ReviewDescribe
        },

        props: {
            stageControl: Object
        }
    }
</script>
