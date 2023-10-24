<template>
    <div class="import-pipeline-edit-wrapper">
        <ImportPipelineHeader />
        <router-view class="pipeline-import-edit-main"></router-view>
    </div>
</template>

<script>
    import { mapState } from 'vuex'
    import ImportPipelineHeader from '@/components/PipelineHeader/ImportPipelineHeader'
    export default {
        components: {
            ImportPipelineHeader
        },
        computed: {
            ...mapState('atom', [
                'editfromImport',
                'pipeline',
                'pipelineWithoutTrigger',
                'pipelineSetting',
                'pipelineYaml'
            ])
        },
        created (to, from, next) {
            if (!this.editfromImport) {
                this.$router.replace({
                    name: 'pipelineList'
                })
            }
        }
    }
</script>

<style lang="scss">
    .import-pipeline-edit-wrapper {
        height: 100%;
        display: flex;
        flex-direction: column;
        .pipeline-import-edit-main {
            flex: 1;
        }
    }
</style>
