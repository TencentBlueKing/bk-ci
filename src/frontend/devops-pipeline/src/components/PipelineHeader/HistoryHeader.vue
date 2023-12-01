<template>
    <div class="pipeline-history-header">
        <div class="pipeline-history-left-aside">
            <pipeline-bread-crumb />
            <pac-tag class="pipeline-pac-indicator" v-if="pacEnabled" :info="yamlInfo" />
            <badge
                :project-id="$route.params.projectId"
                :pipeline-id="$route.params.pipelineId"
            />
        </div>
        <aside class="pipeline-history-right-aside">
            <router-link :to="editRouteName">
                <bk-button>{{ $t("edit") }}</bk-button>
            </router-link>
            <span v-bk-tooltips="tooltip">
                <bk-button
                    theme="primary"
                    :disabled="!executable"
                    @click="goExecPreview"
                >
                    {{ $t("exec") }}
                </bk-button>
            </span>
            <more-actions />
        </aside>
    </div>
</template>

<script>
    import { mapGetters, mapState } from 'vuex'
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
            ...mapState('atom', ['pipelineInfo']),
            ...mapGetters({
                isCurPipelineLocked: 'atom/isCurPipelineLocked',
                isReleasePipeline: 'atom/isReleasePipeline',
                pacEnabled: 'atom/pacEnabled'
            }),
            yamlInfo () {
                return this.pipelineInfo?.yamlInfo
            },
            executable () {
                return !this.isCurPipelineLocked && this.canManualStartup && this.isReleasePipeline
            },
            canManualStartup () {
                return this.pipelineInfo?.canManualStartup ?? true
            },
            tooltip () {
                console.log(this.executable)
                return this.executable
                    ? {
                        disabled: true
                    }
                    : {
                        content: this.$t(this.isCurPipelineLocked ? 'pipelineLockTips' : 'pipelineManualDisable'),
                        delay: [300, 0]
                    }
            },
            editRouteName () {
                return {
                    name: 'pipelinesEdit',
                    params: this.$route.params
                }
            }
        },
        methods: {
            goExecPreview () {
                this.$router.push({
                    name: 'executePreview',
                    params: {
                        ...this.$route.params,
                        version: this.pipelineInfo?.version
                    }
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
        .pipeline-pac-indicator {
            margin-right: 22px;
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
