<template>
    <bk-permission
        class="pipeline-list-auth"
        :resource-type="resourceType"
        :resource-code="resourceCode"
        :project-code="projectCode"
        :show-create-group="false"
        :project-name="projectName"
    />
</template>

<script>
    export default {
        name: 'auth-tab',
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
