<template>
    <div>
        <h1>defect / list Coverity 工具</h1>
        <p>params: {{this.$route.params}}</p>
    </div>
</template>

<script>
    export default {
        data () {
            return {
                editor: null
            }
        },
        computed: {
            projectId () {
                return this.$route.params.projectId
            },
            taskId () {
                return this.$route.params.taskId
            },
            toolName () {
                return this.$route.params.tool
            }
        },
        created () {
        },
        mounted () {
        },
        methods: {
        }
    }
</script>

<style>
    @import './codemirror.css';
</style>

<style lang="postcss" scoped>
    @import './index.css';
</style>
