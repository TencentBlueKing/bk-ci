<template>
    <bk-permission
        :resource-type="resourceType"
        :resource-code="resourceCode"
        :project-code="projectCode"
        :show-create-group="false"
        :resource-name="resourceName"
    />
</template>

<script>
    import { mapActions, mapState } from 'vuex'
    export default {
        data () {
            return {
                resourceType: 'pipeline_template'
            }
        },
        computed: {
            ...mapState('atom', [
                'template'
            ]),

            projectCode () {
                return this.$route.params.projectId
            },
            resourceCode () {
                return this.$route.params.templateId
            },
            resourceName () {
                return this.template?.templateName
            }
        },
        created () {
            this.requestTemplate({
                projectId: this.projectCode,
                templateId: this.resourceCode
            })
        },
        methods: {
            ...mapActions('atom', [
                'requestTemplate'
            ])
        }
    }
</script>
