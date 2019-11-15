<template>
    <div></div>
</template>
<script>
    export default {
        data () {
            this.$router.replace({
                path: `/project/${this.$route.params.projectId}/task/${this.$route.params.taskId}/settings/manage`,
                name: 'task-settings-manage'
            })
            return {

            }
        },
        methods: {

        }
    }
</script>
<style scoped>

</style>
