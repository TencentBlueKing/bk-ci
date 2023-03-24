<template>
    <permission-component
        :resource-type="resourceType"
        :resource-code="resourceCode"
        :project-code="projectCode"
        :show-create-group="false"
        :project-name="projectName"
    />
</template>

<script>
    import {
        PermissionComponent
    } from 'bk-permission'
    import pipelineOperateMixin from '@/mixins/pipeline-operate-mixin'

    export default {
        name: 'auth-tab',
        components: {
            PermissionComponent
        },
        mixins: [pipelineOperateMixin],
        data () {
            return {
                resourceType: 'pipeline_group'
            }
        },
        computed: {
            projectCode () {
                return this.$route.params.projectId
            },
            resourceCode () {
                return this.$route.params.id
            },
            projectName () {
                return this.curProject.projectName
            }
        }
    }
</script>
