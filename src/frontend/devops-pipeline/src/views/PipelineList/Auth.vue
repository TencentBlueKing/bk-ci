<template>
    <permission-component
        class="pipeline-list-auth"
        :resource-type="resourceType"
        :resource-code="resourceCode"
        :project-code="projectCode"
        :show-create-group="false"
        :project-name="projectName"
    />
</template>

<script>
    import { PermissionComponent } from 'bk-permission'

    export default {
        name: 'auth-tab',
        components: {
            PermissionComponent
        },
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
            }
        }
    }
</script>
