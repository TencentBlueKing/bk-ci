<template>
    <bk-permission
        :resource-type="resourceType"
        :resource-code="resourceCode"
        :project-code="projectCode"
        :resource-name="groupName"
        :ajax-prefix="ajaxPrefix"
        :show-create-group="false"
    />
</template>

<script>
    export default {
        name: 'auth-tab',
        data () {
            return {
                resourceType: 'pipeline_group',
                ajaxPrefix: window.PUBLIC_URL_PREFIX,
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
