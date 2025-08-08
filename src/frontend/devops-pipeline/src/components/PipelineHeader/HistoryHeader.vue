<template>
    <div class="pipeline-history-header">
        <pipeline-bread-crumb :is-loading="isSwitchPipeline || switchingVersion">
            <bk-popover :delay="[666, 0]">
                <VersionSelector
                    :value="currentVersion"
                    ref="versionSelectorInstance"
                    @change="handleVersionChange"
                    @showAllVersion="showVersionSideSlider"
                    :include-draft="false"
                    refresh-list-on-expand
                />
                <div slot="content">
                    <p>{{ $t('versionRuleP') }}</p>
                    <p>{{ $t('versionRuleT') }}</p>
                    <p>{{ $t('versionRuleA') }}</p>
                </div>
            </bk-popover>
            <bk-button
                v-if="!isReleaseVersion && isReleasePipeline"
                text
                size="small"
                theme="primary"
                @click="switchToReleaseVersion"
            >
                <i class="devops-icon icon-shift"></i>
                {{ $t("switchToReleaseVersion") }}
            </bk-button>
            <badge
                v-if="isReleaseVersion"
                class="pipeline-exec-badge"
                :project-id="projectId"
                :pipeline-id="pipelineId"
            />
        </pipeline-bread-crumb>

        <aside
            v-show="!(isSwitchPipeline || switchingVersion)"
            class="pipeline-history-right-aside"
        >
            <VersionDiffEntry
                v-if="!isTemplatePipeline && !editAndExecutable && !archiveFlag"
                :text="false"
                outline
                :version="currentVersion"
                :latest-version="releaseVersion"
            >
                {{ $t("diff") }}
            </VersionDiffEntry>
            <RollbackEntry
                v-if="showRollback && (isReleasePipeline || onlyBranchPipeline) && !archiveFlag"
                :text="false"
                :has-permission="canEdit"
                :version="currentVersion"
                :draft-version="pipelineInfo?.version"
                :pipeline-id="pipelineId"
                :project-id="projectId"
                :version-name="activePipelineVersion?.versionName"
                :draft-base-version-name="draftBaseVersionName"
                :is-active-draft="activePipelineVersion?.isDraft"
                :is-active-branch-version="isActiveBranchVersion"
                :draft-creator="activePipelineVersion?.creator"
                :draft-create-time="activePipelineVersion?.createTime"
            >
                {{ operateName }}
            </RollbackEntry>
            <bk-button
                v-else-if="onlyBranchPipeline && activePipelineVersion?.version === releaseVersion && !archiveFlag"
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
            <template v-if="editAndExecutable">
                <span
                    v-if="!archiveFlag"
                    v-bk-tooltips="tooltip"
                >
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
                <more-actions />
            </template>
        </aside>
        <VersionHistorySideSlider
            :show-version-sideslider="showVersionSideslider"
            @close="closeVersionSideSlider"
        />
    </div>
</template>

<script>
    import Badge from '@/components/Badge.vue'

    import RollbackEntry from '@/components/PipelineDetailTabs/RollbackEntry'
    import VersionDiffEntry from '@/components/PipelineDetailTabs/VersionDiffEntry'
    import VersionHistorySideSlider from '@/components/PipelineDetailTabs/VersionHistorySideSlider'
    import VersionSelector from '@/components/PipelineDetailTabs/VersionSelector'
    import {
        RESOURCE_ACTION
    } from '@/utils/permission'
    import { pipelineTabIdMap } from '@/utils/pipelineConst'
    import { mapActions, mapGetters, mapState } from 'vuex'
    import MoreActions from './MoreActions.vue'
    import PipelineBreadCrumb from './PipelineBreadCrumb.vue'

    export default {
        components: {
            PipelineBreadCrumb,

            Badge,
            MoreActions,
            VersionSelector,
            VersionHistorySideSlider,
            VersionDiffEntry,
            RollbackEntry
        },
        props: {
            isSwitchPipeline: Boolean
        },
        data () {
            return {
                showVersionSideslider: false,
                isPipelineIdChanged: false
            }
        },
        computed: {
            ...mapState('atom', [
                'pipeline',
                'pipelineInfo',
                'activePipelineVersion',
                'switchingVersion'
            ]),
            ...mapGetters({
                isCurPipelineLocked: 'atom/isCurPipelineLocked',
                isReleasePipeline: 'atom/isReleasePipeline',
                isReleaseVersion: 'atom/isReleaseVersion',
                isActiveDraftVersion: 'atom/isActiveDraftVersion',
                isOutdatedVersion: 'atom/isOutdatedVersion',
                draftBaseVersionName: 'atom/getDraftBaseVersionName',
                pipelineHistoryViewable: 'atom/pipelineHistoryViewable',
                onlyBranchPipeline: 'atom/onlyBranchPipeline'
            }),
            editAndExecutable () {
                return this.isReleaseVersion || this.activePipelineVersion?.isBranchVersion
            },
            isActiveBranchVersion () {
                return this.activePipelineVersion?.isBranchVersion ?? false
            },
            showRollback () {
                return this.editAndExecutable || !this.pipelineInfo?.baseVersion || this.activePipelineVersion?.baseVersion !== this.pipelineInfo?.baseVersion
            },
            currentVersion () {
                return this.$route.params.version ? parseInt(this.$route.params.version) : this.releaseVersion
            },
            isTemplatePipeline () {
                return this.pipelineInfo?.instanceFromTemplate ?? false
            },
            releaseVersion () {
                return this.pipelineInfo?.releaseVersion
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
                return (!this.isCurPipelineLocked && this.canManualStartup && this.editAndExecutable) || this.isActiveDraftVersion
            },
            canManualStartup () {
                return this.pipelineInfo?.canManualStartup ?? true
            },
            operateName () {
                switch (true) {
                    case this.editAndExecutable:
                        return this.$t('edit')
                    case this.pipelineInfo?.baseVersion && this.activePipelineVersion?.version === this.pipelineInfo?.baseVersion:
                        return this.$t('editCurDraft')
                    default:
                        return this.$t('rollback')
                }
            },
            tooltip () {
                return this.executable
                    ? {
                        disabled: true
                    }
                    : {
                        content: this.$t(this.isCurPipelineLocked ? 'pipelineLockTips' : !(this.isReleasePipeline || this.onlyBranchPipeline) ? 'draftPipelineExecTips' : 'pipelineManualDisable'),
                        delay: [300, 0]
                    }
            },
            RESOURCE_ACTION () {
                return RESOURCE_ACTION
            },
            archiveFlag () {
                return this.$route.query.archiveFlag
            }
        },
        watch: {
            currentVersion () {
                this.$nextTick(this.init)
            }
        },
        created () {
            if (!this.pipelineHistoryViewable) {
                this.$router.replace({
                    name: 'pipelinesEdit'
                })
            }
            if (this.releaseVersion !== this.currentVersion) {
                this.handleVersionChange(this.releaseVersion)
            } else {
                this.init()
            }
        },
        methods: {
            ...mapActions('atom', [
                'selectPipelineVersion',
                'requestPipeline',
                'setSwitchingPipelineVersion',
                'setShowVariable'
            ]),
            goEdit () {
                this.$router.push({
                    name: 'pipelinesEdit',
                    query: {
                        tab: pipelineTabIdMap[this.$route.params.type] ?? 'pipeline'
                    }
                })
            },
            showVersionSideSlider () {
                this.setShowVariable(false)
                this.$refs?.versionSelectorInstance?.close?.()
                this.showVersionSideslider = true
            },
            closeVersionSideSlider () {
                this.showVersionSideslider = false
            },
            async init () {
                try {
                    if (this.currentVersion) {
                        this.setSwitchingPipelineVersion(true)
                        const urlParams = {
                            projectId: this.projectId,
                            pipelineId: this.pipelineId,
                            version: this.currentVersion,
                            archiveFlag: this.archiveFlag
                        }
                        await this.requestPipeline(urlParams)
                    }
                } catch (error) {
                    this.$bkMessage({
                        theme: 'error',
                        message: error.message
                    })
                } finally {
                    this.setSwitchingPipelineVersion(false)
                }
            },
            goExecPreview () {
                this.$router.push({
                    name: 'executePreview',
                    query: {
                        ...(this.isActiveDraftVersion ? { debug: '' } : {})
                    },
                    params: {
                        ...this.$route.params,
                        version: this.currentVersion
                    }
                })
            },
            switchToReleaseVersion () {
                this.handleVersionChange(this.releaseVersion)
            },
            handleVersionChange (versionId, version) {
                let routeType = this.$route.params.type || 'history'

                if (version) {
                    this.selectPipelineVersion(version)
                    if (this.releaseVersion) {
                        const noRecordVersion = ['history', 'triggerEvent'].includes(this.$route.params.type) && !(versionId === this.releaseVersion || version.isBranchVersion)
                        routeType = noRecordVersion ? pipelineTabIdMap.pipeline : this.$route.params.type
                    }
                }
                console.log('handleVersionChange', this.pipelineInfo, this.pipelineHistoryViewable, versionId)
                this.$router.replace({
                    query: this.$route.query,
                    params: {
                        ...this.$route.params,
                        version: versionId,
                        type: routeType
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

    .pipeline-history-right-aside {
        flex-shrink: 0;
        display: grid;
        align-items: center;
        grid-gap: 10px;
        grid-auto-flow: column;
    }
}
</style>
