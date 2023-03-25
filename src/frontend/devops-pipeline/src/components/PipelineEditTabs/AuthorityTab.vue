<template>
    <bk-permission
        :resource-type="resourceType"
        :resource-code="resourceCode"
        :project-code="projectCode"
        :show-create-group="false"
        :project-name="projectName"
    />
</template>

<script>
    import pipelineOperateMixin from '@/mixins/pipeline-operate-mixin'

    export default {
        name: 'auth-tab',
        mixins: [pipelineOperateMixin],
        data () {
            return {
                resourceType: 'pipeline'
            }
        },
        computed: {
            projectCode () {
                return this.$route.params.projectId
            },
            resourceCode () {
                return this.$route.params.pipelineId
            },
            projectName () {
                return this.curProject.projectName
            }
        }
    }
</script>
