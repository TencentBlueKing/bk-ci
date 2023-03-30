<template>
    <bk-permission
        :resource-type="resourceType"
        :resource-code="resourceCode"
        :project-code="projectCode"
        :resource-name="groupName"
        :show-create-group="false"
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
            },
            groupName () {
                return this.$route.params.groupName
            }
        }
    }
</script>
