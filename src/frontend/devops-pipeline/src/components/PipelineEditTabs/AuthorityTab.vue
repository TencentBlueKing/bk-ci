<template>
    <bk-permission
        :key="resourceCode"
        :resource-type="resourceType"
        :resource-code="resourceCode"
        :project-code="projectCode"
        :show-create-group="false"
        :resource-name="pipelineName"
    />
</template>

<script>
    import pipelineOperateMixin from '@/mixins/pipeline-operate-mixin'
    import { mapState, mapGetters } from 'vuex'

    export default {
        name: 'auth-tab',
        mixins: [pipelineOperateMixin],
        computed: {
            ...mapState('atom', ['pipelineInfo']),
            ...mapGetters('atom', ['isTemplate']),
            projectCode () {
                return this.$route.params.projectId
            },
            resourceCode () {
                return this.isTemplate ? this.$route.params.templateId : this.$route.params.pipelineId
            },
            pipelineName () {
                return (this.isTemplate ? this.pipelineInfo?.name : this.pipelineInfo?.pipelineName) ?? ''
            },
            resourceType () {
                return this.isTemplate ? 'template' : 'pipeline'
            }
        }
    }
</script>
