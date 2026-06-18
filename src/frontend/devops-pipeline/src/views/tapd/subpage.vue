<template>
    <component
        v-if="bkComponent"
        :is="bkComponent"
        class="biz-content"
        ref="pipeline"
        v-bind="$attrs"
        v-on="$listeners"
    />
</template>

<script>
    import PipelineEdit from '../subpages/edit.vue'
    import PipelineExecDetail from '../subpages/ExecDetail.vue'
    import PipelinesHistory from '../subpages/History.vue'
    import PipelinePreview from '../subpages/preview.vue'

    const COMPONENTS = Object.freeze({
        tapdPipelinesEdit: PipelineEdit,
        tapdPipelinesStdDetail: PipelineExecDetail,
        tapdPipelinesHistory: PipelinesHistory,
        tapdExecutePreview: PipelinePreview
    })

    export default {
        name: 'tapdPipelineSubpage',
        inheritAttrs: false,
        computed: {
            bkComponent () {
                return COMPONENTS[this.$route.name] || null
            }
        }
    }
</script>
