<template>
    <permission-component
        class="pipeline-list-auth"
        :resource-type="resourceType"
        :resource-code="resourceCode"
        :project-code="projectCode"
        :show-create-group="false"
    />
</template>

<script>
    import { PermissionComponent } from 'bk-permission'

    export default {
        name: 'pipeline-list-auth',
        components: {
            PermissionComponent
        },
        data () {
            return {
                resourceType: 'pipeline_group',
            }
        },
        computed: {
            projectCode () {
                return this.$route.params.projectId
            },
            resourceCode () {
                return this.$route.params.id
            }
        },
    }
</script>

<style lang="scss" scoped>
    .pipeline-list-auth {
        padding: 30px;
    }
</style>
