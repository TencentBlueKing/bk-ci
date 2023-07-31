<template>
    <div class="pipeline-history-header">
        <div class="pipeline-history-left-aside">
            <pipeline-bread-crumb />
            <pac-tag />
            <badge
                class="pipeline-latest-exec-badge"
                :project-id="$route.params.projectId"
                :pipeline-id="$route.params.pipelineId"
            />
        </div>
        <aside class="pipeline-history-right-aside">
            <bk-button theme="primary" @click="goExecPreview">
                {{ $t("exec") }}
            </bk-button>
            <more-actions />
        </aside>
    </div>
</template>

<script>
    import { mapGetters } from 'vuex'
    import PipelineBreadCrumb from './PipelineBreadCrumb.vue'
    import MoreActions from './MoreActions.vue'
    import PacTag from '@/components/PacTag.vue'
    import Badge from '@/components/Badge.vue'

    export default {
        components: {
            PipelineBreadCrumb,
            PacTag,
            Badge,
            MoreActions
        },
        computed: {
            ...mapGetters({
                curPipeline: 'pipelines/getCurPipeline'
            }),
            editRouteName () {
                return { name: 'pipelinesEdit' }
            }
        },
        created () {
            console.log(this.curPipeline)
        },
        methods: {
            goExecPreview () {
                this.$router.push({
                    name: 'pipelinesPreview'
                })
            }
        }
    }
</script>

<style lang="scss">
.pipeline-history-header {
    width: 100%;
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 0 24px 0 14px;
    .pipeline-history-left-aside {
        display: grid;
        grid-auto-flow: column;
        align-items: center;
        .pipeline-latest-exec-badge {
            margin-left: 22px;
        }
    }

    .pipeline-history-right-aside {
        flex-shrink: 0;
        display: grid;
        align-items: center;
        grid-gap: 10px;
        grid-auto-flow: column;
    }
}
</style>
