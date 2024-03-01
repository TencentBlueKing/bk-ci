<template>
    <div class="pipeline-history-header">
        <div class="pipeline-history-left-aside">
            <pipeline-bread-crumb />
            <pac-tag class="pipeline-pac-indicator" v-if="pacEnabled" :info="yamlInfo" />
            <badge
                :project-id="projectId"
                :pipeline-id="pipelineId"
            />
        </div>
        <aside class="pipeline-history-right-aside">
            <bk-button
                theme="primary"
                outline
                v-perm="{
                    hasPermission: canEdit,
                    disablePermissionApi: true,
                    permissionData: {
                        projectId,
                        resourceType: 'pipeline',
                        resourceCode: pipelineId,
                        action: RESOURCE_ACTION.EDIT
                    }
                }"
                @click="goEdit"
            >
                {{ $t("pipelineEdit") }}
            </bk-button>
            <span v-bk-tooltips="tooltip">
                <bk-button
                    :disabled="!executable"
                    theme="primary"
                    v-perm="{
                        hasPermission: canExecute,
                        disablePermissionApi: true,
                        permissionData: {
                            projectId,
                            resourceType: 'pipeline',
                            resourceCode: pipelineId,
                            action: RESOURCE_ACTION.EXECUTE
                        }
                    }"
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
    import {
        RESOURCE_ACTION
    } from '@/utils/permission'
    import MoreActions from './MoreActions.vue'
    import PipelineBreadCrumb from './PipelineBreadCrumb.vue'
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
            projectId () {
                return this.$route.params.projectId
            },
            pipelineId () {
                return this.$route.params.pipelineId
            },
            yamlInfo () {
                return this.pipelineInfo?.yamlInfo
            },
            canEdit () {
                return this.pipelineInfo?.permissions?.canEdit ?? true
            },
            canExecute () {
                return this.pipelineInfo?.permissions?.canExecute ?? true
            },
            executable () {
                return !this.isCurPipelineLocked && this.canManualStartup && this.isReleasePipeline
            },
            canManualStartup () {
                return this.pipelineInfo?.canManualStartup ?? true
            },
            tooltip () {
                return this.executable
                    ? {
                        disabled: true
                    }
                    : {
                        content: this.$t(!this.isReleasePipeline ? 'draftPipelineExecTips' : this.isCurPipelineLocked ? 'pipelineLockTips' : 'pipelineManualDisable'),
                        delay: [300, 0]
                    }
            },
            RESOURCE_ACTION () {
                return RESOURCE_ACTION
            }
        },
        methods: {
            goEdit () {
                this.$router.push({
                    name: 'pipelinesEdit'
                })
            },
            goExecPreview () {
                this.$router.push({
                    name: 'executePreview',
                    params: {
                        ...this.$route.params,
                        version: this.pipelineInfo?.releaseVersion
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
