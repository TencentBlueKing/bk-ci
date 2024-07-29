<template>
    <BuildHistoryTab
        is-debug
        :pipeline-version="pipelineVersion"
    />
</template>

<script>
    import BuildHistoryTab from '@/components/PipelineDetailTabs/BuildHistoryTab'
    import '@blueking/search-select/dist/styles/index.css'
    export default {
        components: {
            BuildHistoryTab
        },
        computed: {
            pipelineVersion () {
                return this.$route.params.version
            }
        }
    }
</script>

<style lang="scss">

</style>
