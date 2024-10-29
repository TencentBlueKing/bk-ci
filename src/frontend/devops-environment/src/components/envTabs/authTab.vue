<template>
    <bk-permission
        :key="resourceCode"
        :resource-type="resourceType"
        :resource-code="resourceCode"
        :project-code="projectCode"
        :show-create-group="false"
        :resource-name="resourceName"
    />
</template>

<script>
    export default {
        name: 'auth-tab',
        props: {
            curEnvDetail: Object
        },
        data () {
            return {
                resourceType: 'environment'
            }
        },
        computed: {
            projectCode () {
                console.log(this.$route.params, 1111)
                return this.$route.params.projectId
            },
            resourceCode () {
                return this.$route.params.envId
            },
            resourceName () {
                return this.curEnvDetail.name || ''
            }

        }
    }
</script>
