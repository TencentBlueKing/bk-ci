<template>
    <div class="import-pipeline-edit-wrapper">
        <ImportPipelineHeader />
        <router-view class="pipeline-import-edit-main"></router-view>
    </div>
</template>

<script>
    import ImportPipelineHeader from '@/components/PipelineHeader/ImportPipelineHeader'
    import { SET_PIPELINE_INFO } from '@/store/modules/atom/constants'
    import { mapState } from 'vuex'
    export default {
        components: {
            ImportPipelineHeader
        },
        computed: {
            ...mapState('atom', [
                'editfromImport'
            ])
        },
        created (to, from, next) {
            if (!this.editfromImport) {
                this.$router.replace({
                    name: 'pipelineList'
                })
            }
        },
        beforeDestroy () {
            this.$store.dispatch('atom/setPipeline', null)
            this.$store.dispatch('atom/setPipelineWithoutTrigger', null)
            this.$store.dispatch('atom/setPipelineYaml', '')
            this.$store.dispatch('atom/selectPipelineVersion', null)
            this.$store.commit('atom/resetPipelineSetting', null)
            this.$store.commit(`atom/${SET_PIPELINE_INFO}`, null)
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
