<template>
    <div class="pipeline-history-header">
        <div class="pipeline-history-left-aside">
            <pipeline-bread-crumb />
            <pac-tag class="pipeline-pac-indicator" v-if="pacEnabled" :info="yamlInfo" />
            <VersionSideslider
                :value="activePipelineVersion?.version"
                @change="handleVersionChange"
            />
            <bk-button
                v-if="isOutdatedVersion"
                text
                size="small"
                theme="primary"
                @click="switchToReleaseVersion"
            >
                <i class="devops-icon icon-shift"></i>
                {{ $t("switchToReleaseVersion") }}
            </bk-button>
            <badge
                class="pipeline-exec-badge"
                :project-id="projectId"
                :pipeline-id="pipelineId"
            />
        </div>
        <aside class="pipeline-history-right-aside">
            <VersionDiffEntry
                v-if="!isReleaseVersion"
                :text="false"
                outline
                :version="activePipelineVersion?.version"
                :latest-version="releaseVersion"
            >
                {{ $t("diff") }}
            </VersionDiffEntry>
            <RollbackEntry
                v-if="showRollback"
                :text="false"
                :has-permission="canEdit"
                :version="activePipelineVersion?.version"
                :pipeline-id="pipelineId"
                :project-id="projectId"
                :version-name="activePipelineVersion?.versionName"
                :draft-base-version-name="draftBaseVersionName"
                :is-active-draft="activePipelineVersion?.isDraft"
            />
            <template v-else>
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
                    {{ $t("edit") }}
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
                        {{ $t(isActiveDraftVersion ? 'debug' : 'exec') }}

                    </bk-button>
                </span>
            </template>
            <more-actions />
        </aside>
    </div>
</template>

<script>
    import { mapGetters, mapState, mapActions } from 'vuex'

    import {
        RESOURCE_ACTION
    } from '@/utils/permission'
    import MoreActions from './MoreActions.vue'
    import PipelineBreadCrumb from './PipelineBreadCrumb.vue'
    import PacTag from '@/components/PacTag.vue'
    import Badge from '@/components/Badge.vue'
    import VersionSideslider from '@/components/PipelineDetailTabs/VersionSideslider'
    import VersionDiffEntry from '@/components/PipelineDetailTabs/VersionDiffEntry'
    import RollbackEntry from '@/components/PipelineDetailTabs/RollbackEntry'

    export default {
        components: {
            PipelineBreadCrumb,
            PacTag,
            Badge,
            MoreActions,
            VersionSideslider,
            VersionDiffEntry,
            RollbackEntry
        },
        props: {
            updatePipeline: Function
        },
        computed: {
            ...mapState('atom', ['pipelineInfo', 'activePipelineVersion']),
            ...mapGetters({
                isCurPipelineLocked: 'atom/isCurPipelineLocked',
                isReleasePipeline: 'atom/isReleasePipeline',
                isReleaseVersion: 'atom/isReleaseVersion',
                isActiveDraftVersion: 'atom/isActiveDraftVersion',
                isOutdatedVersion: 'atom/isOutdatedVersion',
                draftBaseVersionName: 'atom/getDraftBaseVersionName',
                pacEnabled: 'atom/pacEnabled'
            }),
            showRollback () {
                return !this.isActiveDraftVersion && !this.isReleaseVersion
            },
            releaseVersion () {
                return this.pipelineInfo?.releaseVersion
            },
            releaseVersionName () {
                return this.pipelineInfo?.releaseVersionName
            },
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
                return (!this.isCurPipelineLocked && this.canManualStartup && this.isReleasePipeline) || this.isActiveDraftVersion
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
            ...mapActions('atom', ['selectPipelineVersion']),
            goEdit () {
                this.$router.push({
                    name: 'pipelinesEdit'
                })
            },
            goExecPreview () {
                this.$router.push({
                    name: 'executePreview',
                    query: {
                        ...(this.isActiveDraftVersion ? { debug: '' } : {})
                    },
                    params: {
                        ...this.$route.params,
                        version: this.activePipelineVersion?.version
                    }
                })
            },
            switchToReleaseVersion () {
                this.selectPipelineVersion({
                    version: this.releaseVersion
                })
            },
            handleVersionChange (versionId, version) {
                this.selectPipelineVersion(version)
                console.log('this.handleVersionChange', versionId, version)
                this.updatePipeline?.()
                if (['history', 'triggerEvent'].includes(this.$route.params.type) && versionId < this.releaseVersion) {
                    this.$nextTick(() => {
                        this.$router.push({
                            name: 'pipelinesHistory',
                            params: {
                                ...this.$route.params,
                                type: 'pipeline'
                            }
                        })
                    })
                }
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
        .pipeline-pac-indicator{
            margin-right: 16px;
        }
        .pipeline-exec-badge  {
            margin-left: 4px;
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
